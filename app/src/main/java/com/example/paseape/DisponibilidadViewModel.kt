package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.ActualizarBloqueDisponibilidadRequestDto
import com.example.paseape.data.model.ActualizarCapacidadBaseRequestDto
import com.example.paseape.data.model.BloqueHorarioParamDto
import com.example.paseape.data.model.DiaSemanaParamDto
import com.example.paseape.data.model.DisponibilidadBloqueRequestDto
import com.example.paseape.data.model.DisponibilidadDataDto
import com.example.paseape.data.model.GuardarDisponibilidadRequestDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response

/** Identifica una celda del grid: día de la semana + bloque horario. */
data class SlotKey(
    val diaSemanaId: Int,
    val bloqueHorarioId: Int
)

sealed interface DisponibilidadUiState {

    data object Loading : DisponibilidadUiState

    data class Error(val message: String) : DisponibilidadUiState

    data class Success(
        val diasSemana: List<DiaSemanaParamDto>,
        val bloquesHorario: List<BloqueHorarioParamDto>,
        val capacidadBase: Int,
        val capacidadBaseGuardada: Int,
        val slots: Map<SlotKey, Int>,
        val slotsGuardados: Map<SlotKey, Int>,
        /** Bloques agregados localmente que aún no se han persistido. */
        val slotsPendientes: Set<SlotKey> = emptySet(),
        val isSaving: Boolean = false
    ) : DisponibilidadUiState {

        val estaVacia: Boolean
            get() = slots.isEmpty()

        val hayCambiosSinGuardar: Boolean
            get() = slotsPendientes.isNotEmpty()
    }
}

/**
 * Mensaje para el Snackbar. Si [retry] no es null, la pantalla muestra la acción
 * "Reintentar" y ejecuta ese bloque cuando el usuario la pulsa.
 */
data class DisponibilidadMessage(
    val text: String,
    val retry: (() -> Unit)? = null
)

/**
 * Disponibilidad semanal:
 * - Capacidad base: PATCH propio, con debounce, sin tocar los bloques.
 * - Editar un bloque: PATCH GUARDAR_CUPO automático solo para esa celda.
 * - Quitar un bloque: PATCH CANCELAR inmediato (valida reservas).
 * - Agregar celdas y chips: solo estado local hasta pulsar "Guardar Cambios",
 *   que sincroniza todo con un PUT.
 * - Limpiar semana: PATCH CANCELAR por cada bloque activo.
 */
class DisponibilidadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DisponibilidadUiState>(DisponibilidadUiState.Loading)
    val uiState: StateFlow<DisponibilidadUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<DisponibilidadMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<DisponibilidadMessage> = _messages.asSharedFlow()

    private var tokenCargado: String? = null
    private var authHeaderActual: String = ""

    private val mutexEscritura = Mutex()
    private var jobCapacidad: Job? = null
    private var jobGuardado: Job? = null
    private val jobsSlot = mutableMapOf<SlotKey, Job>()

    private val operacionesActivas = mutableSetOf<Long>()
    private var secuenciaOperacion = 0L

    /**
     * Carga catálogos y disponibilidad. Si ya se cargó con el mismo token y el
     * estado sigue siendo exitoso, no vuelve a consultar (conserva el estado
     * mientras se navega entre pestañas).
     */
    fun cargar(token: String, force: Boolean = false) {
        val authHeader = construirAuthHeader(token)
        if (authHeader.isBlank()) {
            Log.e(TAG, "No hay token para consultar la disponibilidad del paseador")
            val message = ApiErrorMapper.fromThrowable(
                IllegalStateException("Token vacío"),
                ApiErrorContext.WALKER
            )
            setError(message) { cargar(token, force = true) }
            return
        }

        authHeaderActual = authHeader

        if (!force &&
            authHeader == tokenCargado &&
            _uiState.value is DisponibilidadUiState.Success
        ) {
            return
        }

        tokenCargado = authHeader
        cancelarOperacionesPendientes()
        _uiState.value = DisponibilidadUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parametrosResponse = RetrofitClient.apiService.getApplicationParameters(authHeader)
                if (!parametrosResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "HTTP ${parametrosResponse.code()} al consultar application-parameters"
                    )
                    val message = ApiErrorMapper.fromResponse(
                        parametrosResponse,
                        ApiErrorContext.WALKER
                    )
                    withContext(Dispatchers.Main) {
                        setError(message) { cargar(token, force = true) }
                    }
                    return@launch
                }

                var dias = parametrosResponse.body()?.response?.diasSemana.orEmpty()
                var bloques = parametrosResponse.body()?.response?.bloquesHorario.orEmpty()

                // Compatibilidad: el backend actual expone los catálogos de horario
                // en /application-parameters/horario.
                if (dias.isEmpty() || bloques.isEmpty()) {
                    val horarioResponse = RetrofitClient.apiService.getHorarioParameters(authHeader)
                    if (horarioResponse.isSuccessful) {
                        val horario = horarioResponse.body()?.response
                        if (dias.isEmpty()) dias = horario?.diasSemana.orEmpty()
                        if (bloques.isEmpty()) bloques = horario?.bloquesHorario.orEmpty()
                    } else {
                        Log.e(
                            TAG,
                            "HTTP ${horarioResponse.code()} al consultar application-parameters/horario"
                        )
                    }
                }

                val disponibilidadResponse = RetrofitClient.apiService.getMiDisponibilidad(authHeader)
                if (!disponibilidadResponse.isSuccessful) {
                    Log.e(
                        TAG,
                        "HTTP ${disponibilidadResponse.code()} al consultar paseador/disponibilidad"
                    )
                    val message = ApiErrorMapper.fromResponse(
                        disponibilidadResponse,
                        ApiErrorContext.WALKER
                    )
                    withContext(Dispatchers.Main) {
                        setError(message) { cargar(token, force = true) }
                    }
                    return@launch
                }

                val data = disponibilidadResponse.body()?.response
                val diasOrdenados = dias.distinctBy { it.id }.sortedBy { it.id }
                val bloquesOrdenados = bloques.distinctBy { it.id }.sortedBy { it.id }

                if (data == null || diasOrdenados.isEmpty() || bloquesOrdenados.isEmpty()) {
                    Log.e(
                        TAG,
                        "Respuesta incompleta de disponibilidad: dias=${diasOrdenados.size}, " +
                            "bloques=${bloquesOrdenados.size}, data=${data != null}"
                    )
                    val message = ApiErrorMapper.fromThrowable(
                        IllegalStateException("Catálogos o disponibilidad vacíos"),
                        ApiErrorContext.WALKER
                    )
                    withContext(Dispatchers.Main) {
                        setError(message) { cargar(token, force = true) }
                    }
                    return@launch
                }

                val base = (data.maxMascotasPredeterminado ?: CAPACIDAD_BASE_DEFAULT)
                    .coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
                val slots = mapearSlots(data, base)

                withContext(Dispatchers.Main) {
                    _uiState.value = DisponibilidadUiState.Success(
                        diasSemana = diasOrdenados,
                        bloquesHorario = bloquesOrdenados,
                        capacidadBase = base,
                        capacidadBaseGuardada = base,
                        slots = slots,
                        slotsGuardados = slots
                    )
                }

                Log.d(
                    TAG,
                    "Disponibilidad cargada: ${slots.size} bloques, capacidad base=$base"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar la disponibilidad semanal", e)
                withContext(Dispatchers.Main) {
                    setError(ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)) {
                        cargar(token, force = true)
                    }
                }
            }
        }
    }

    /**
     * Activa una celda vacía con el cupo base. Queda pendiente hasta que el
     * usuario pulse "Guardar Cambios".
     */
    fun activarSlot(slot: SlotKey) {
        val state = successActual() ?: return
        if (state.slots.containsKey(slot)) return

        val cupo = state.capacidadBase
        updateSuccess {
            it.copy(
                slots = it.slots + (slot to cupo),
                slotsPendientes = it.slotsPendientes + slot
            )
        }
    }

    /** Cambia el cupo de un bloque marcado; solo ese bloque se persiste. */
    fun actualizarCupo(slot: SlotKey, cupo: Int) {
        val state = successActual() ?: return
        if (!state.slots.containsKey(slot)) return

        val cupoValido = cupo.coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
        if (state.slots[slot] == cupoValido) return

        updateSuccess { it.copy(slots = it.slots + (slot to cupoValido)) }
        programarGuardadoSlot(slot, cupoValido)
    }

    /**
     * Quita el bloque. Si ya estaba guardado en el servidor se cancela al
     * instante con PATCH CANCELAR; si solo era local, se descarta sin llamar a la API.
     */
    fun quitarSlot(slot: SlotKey) {
        val state = successActual() ?: return
        if (!state.slots.containsKey(slot)) return

        val estabaGuardado = state.slotsGuardados.containsKey(slot)
        jobsSlot.remove(slot)?.cancel()
        updateSuccess {
            it.copy(
                slots = it.slots - slot,
                slotsPendientes = it.slotsPendientes - slot
            )
        }

        if (!estabaGuardado) return

        val id = nuevaOperacion()
        viewModelScope.launch {
            try {
                persistirCancelacion(slot)
            } finally {
                terminarOperacion(id)
            }
        }
    }

    /**
     * Cambia la capacidad base y la guarda con debounce. No modifica los bloques
     * existentes: el nuevo valor solo se usa al activar bloques nuevos.
     */
    fun ajustarCapacidadBase(delta: Int) {
        val state = successActual() ?: return

        val nuevaCapacidad = (state.capacidadBase + delta)
            .coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
        if (nuevaCapacidad == state.capacidadBase) return

        updateSuccess { it.copy(capacidadBase = nuevaCapacidad) }
        programarGuardadoCapacidad(nuevaCapacidad)
    }

    fun marcarLunesAViernes() {
        rellenarDias { dias -> dias.take(5).map { it.id }.toSet() }
    }

    fun marcarFinesDeSemana() {
        rellenarDias { dias -> dias.drop(5).map { it.id }.toSet() }
    }

    /** Limpia la semana y cancela cada bloque guardado con PATCH CANCELAR. */
    fun limpiarSemana() {
        val state = successActual() ?: return
        val objetivos = state.slotsGuardados.keys
        if (objetivos.isEmpty() && state.slots.isEmpty()) return

        jobsSlot.values.forEach { it.cancel() }
        jobsSlot.clear()
        jobGuardado?.cancel()
        jobGuardado = null

        updateSuccess {
            it.copy(
                slots = emptyMap(),
                slotsPendientes = emptySet()
            )
        }

        if (objetivos.isEmpty()) return

        val id = nuevaOperacion()
        viewModelScope.launch {
            try {
                cancelarBloques(objetivos)
            } finally {
                terminarOperacion(id)
            }
        }
    }

    private fun rellenarDias(seleccionar: (List<DiaSemanaParamDto>) -> Set<Int>) {
        val state = successActual() ?: return

        val diasSeleccionados = seleccionar(state.diasSemana)
        if (diasSeleccionados.isEmpty()) return

        val nuevos = mutableMapOf<SlotKey, Int>()
        state.diasSemana
            .filter { it.id in diasSeleccionados }
            .forEach { dia ->
                state.bloquesHorario.forEach { bloque ->
                    val slot = SlotKey(dia.id, bloque.id)
                    if (!state.slots.containsKey(slot)) {
                        nuevos[slot] = state.capacidadBase
                    }
                }
            }

        if (nuevos.isEmpty()) return
        updateSuccess {
            it.copy(
                slots = it.slots + nuevos,
                slotsPendientes = it.slotsPendientes + nuevos.keys
            )
        }
    }

    /**
     * Guarda manualmente todos los bloques con un PUT. Solo se ejecuta si hay
     * celdas agregadas localmente y no hay otra escritura en curso.
     */
    fun guardarCambios() {
        val state = successActual() ?: return
        if (state.isSaving || !state.hayCambiosSinGuardar) return

        jobGuardado?.cancel()
        val objetivo = state.slots

        val id = nuevaOperacion()
        jobGuardado = viewModelScope.launch {
            try {
                persistirMapa(objetivo)
            } finally {
                terminarOperacion(id)
            }
        }
    }

    private suspend fun persistirMapa(objetivo: Map<SlotKey, Int>) {
        val header = headerActual() ?: return

        val bloques = objetivo.entries
            .sortedWith(compareBy({ it.key.diaSemanaId }, { it.key.bloqueHorarioId }))
            .map { (slot, cupo) ->
                DisponibilidadBloqueRequestDto(
                    diaSemanaId = slot.diaSemanaId,
                    bloqueHorarioId = slot.bloqueHorarioId,
                    maxMascotas = cupo
                )
            }

        val resultado = llamarApi("guardar la disponibilidad") {
            RetrofitClient.apiService.sincronizarDisponibilidad(
                token = header,
                request = GuardarDisponibilidadRequestDto(bloques = bloques)
            )
        }

        when (resultado) {
            is ResultadoApi.Error -> {
                // Se conservan los cambios locales para poder reintentar.
                emitirError(resultado.mensaje) { guardarCambios() }
            }

            is ResultadoApi.Exito -> {
                val base = successActual()?.capacidadBase ?: CAPACIDAD_BASE_DEFAULT
                val data = resultado.response.body()?.response
                val confirmados = if (data != null && data.slots != null) {
                    mapearSlots(data, base)
                } else {
                    objetivo
                }
                updateSuccess { state ->
                    state.copy(
                        slotsGuardados = confirmados,
                        // Solo siguen pendientes las celdas agregadas durante el envío.
                        slotsPendientes = (state.slotsPendientes - objetivo.keys)
                            .filter { it in state.slots }
                            .toSet()
                    )
                }
                emitirInfo("Disponibilidad guardada")
                Log.d(TAG, "Disponibilidad sincronizada: ${bloques.size} bloques")
            }
        }
    }

    private fun programarGuardadoSlot(slot: SlotKey, cupo: Int) {
        jobsSlot.remove(slot)?.cancel()

        val id = nuevaOperacion()
        val job = viewModelScope.launch {
            try {
                delay(DEBOUNCE_GUARDADO_MS)
                persistirSlot(slot, cupo)
            } finally {
                terminarOperacion(id)
            }
        }
        jobsSlot[slot] = job
    }

    private suspend fun persistirSlot(slot: SlotKey, cupo: Int) {
        val header = headerActual() ?: return

        val resultado = llamarApi("guardar el bloque") {
            RetrofitClient.apiService.actualizarBloqueDisponibilidad(
                token = header,
                request = ActualizarBloqueDisponibilidadRequestDto(
                    diaSemanaId = slot.diaSemanaId,
                    bloqueHorarioId = slot.bloqueHorarioId,
                    accion = ACCION_GUARDAR_CUPO,
                    maxMascotas = cupo
                )
            )
        }

        when (resultado) {
            is ResultadoApi.Error -> {
                revertirSlot(slot, cupo)
                emitirError(resultado.mensaje) { reintentarSlot(slot, cupo) }
            }

            is ResultadoApi.Exito -> {
                updateSuccess { state ->
                    if (state.slots[slot] == cupo) {
                        state.copy(
                            slotsGuardados = state.slotsGuardados + (slot to cupo),
                            slotsPendientes = state.slotsPendientes - slot
                        )
                    } else {
                        state
                    }
                }
                Log.d(
                    TAG,
                    "Bloque guardado: día ${slot.diaSemanaId}, bloque ${slot.bloqueHorarioId}, cupo $cupo"
                )
            }
        }
    }

    private suspend fun persistirCancelacion(slot: SlotKey) {
        val header = headerActual() ?: return

        val resultado = llamarApi("cancelar el bloque") {
            RetrofitClient.apiService.actualizarBloqueDisponibilidad(
                token = header,
                request = ActualizarBloqueDisponibilidadRequestDto(
                    diaSemanaId = slot.diaSemanaId,
                    bloqueHorarioId = slot.bloqueHorarioId,
                    accion = ACCION_CANCELAR
                )
            )
        }

        when (resultado) {
            is ResultadoApi.Error -> {
                // Si el bloque ya se volvió a activar, no pisamos el nuevo estado.
                val confirmado = successActual()?.slotsGuardados?.get(slot)
                if (confirmado != null) {
                    updateSuccess { state ->
                        if (state.slots.containsKey(slot)) {
                            state
                        } else {
                            state.copy(slots = state.slots + (slot to confirmado))
                        }
                    }
                }
                emitirError(resultado.mensaje) { quitarSlot(slot) }
            }

            is ResultadoApi.Exito -> {
                updateSuccess { it.copy(slotsGuardados = it.slotsGuardados - slot) }
                Log.d(
                    TAG,
                    "Bloque cancelado: día ${slot.diaSemanaId}, bloque ${slot.bloqueHorarioId}"
                )
            }
        }
    }

    private fun programarGuardadoCapacidad(valor: Int) {
        jobCapacidad?.cancel()

        val id = nuevaOperacion()
        jobCapacidad = viewModelScope.launch {
            try {
                delay(DEBOUNCE_GUARDADO_MS)
                persistirCapacidad(valor)
            } finally {
                terminarOperacion(id)
            }
        }
    }

    private suspend fun persistirCapacidad(valor: Int) {
        val header = headerActual() ?: return

        val resultado = llamarApi("actualizar la capacidad base") {
            RetrofitClient.apiService.actualizarCapacidadBase(
                token = header,
                request = ActualizarCapacidadBaseRequestDto(valor)
            )
        }

        when (resultado) {
            is ResultadoApi.Error -> {
                updateSuccess { state ->
                    if (state.capacidadBase == valor) {
                        state.copy(capacidadBase = state.capacidadBaseGuardada)
                    } else {
                        state
                    }
                }
                emitirError(resultado.mensaje) { reintentarCapacidad(valor) }
            }

            is ResultadoApi.Exito -> {
                updateSuccess { state ->
                    if (state.capacidadBase == valor) {
                        state.copy(capacidadBaseGuardada = valor)
                    } else {
                        state
                    }
                }
                Log.d(TAG, "Capacidad base actualizada: $valor")
            }
        }
    }

    private fun reintentarCapacidad(valor: Int) {
        if (successActual() == null) return
        updateSuccess { it.copy(capacidadBase = valor) }
        programarGuardadoCapacidad(valor)
    }

    private fun reintentarSlot(slot: SlotKey, cupo: Int) {
        if (successActual() == null) return
        updateSuccess { it.copy(slots = it.slots + (slot to cupo)) }
        programarGuardadoSlot(slot, cupo)
    }

    private suspend fun revertirSlot(slot: SlotKey, cupoEnviado: Int) {
        updateSuccess { state ->
            if (state.slots[slot] != cupoEnviado) {
                state
            } else {
                val confirmado = state.slotsGuardados[slot]
                state.copy(
                    slots = if (confirmado == null) {
                        state.slots - slot
                    } else {
                        state.slots + (slot to confirmado)
                    },
                    slotsPendientes = if (confirmado == null) {
                        state.slotsPendientes - slot
                    } else {
                        state.slotsPendientes
                    }
                )
            }
        }
    }

    private suspend fun cancelarBloques(objetivos: Set<SlotKey>) {
        val header = headerActual() ?: return

        val guardados = successActual()?.slotsGuardados.orEmpty()
        val locales = successActual()?.slots.orEmpty()
        val base = successActual()?.capacidadBase ?: CAPACIDAD_BASE_DEFAULT
        val fallidos = mutableMapOf<SlotKey, Int>()
        var primerError: String? = null

        for (slot in objetivos.sortedWith(compareBy({ it.diaSemanaId }, { it.bloqueHorarioId }))) {
            val resultado = llamarApi("cancelar el bloque") {
                RetrofitClient.apiService.actualizarBloqueDisponibilidad(
                    token = header,
                    request = ActualizarBloqueDisponibilidadRequestDto(
                        diaSemanaId = slot.diaSemanaId,
                        bloqueHorarioId = slot.bloqueHorarioId,
                        accion = ACCION_CANCELAR
                    )
                )
            }

            if (resultado is ResultadoApi.Error) {
                fallidos[slot] = guardados[slot] ?: locales[slot] ?: base
                if (primerError == null) primerError = resultado.mensaje
            }
        }

        updateSuccess {
            it.copy(
                slots = fallidos,
                slotsGuardados = fallidos
            )
        }

        val error = primerError
        if (error == null) {
            emitirInfo("Disponibilidad guardada")
            Log.d(TAG, "Semana despejada: ${objetivos.size} bloques cancelados")
        } else {
            emitirError(error) { limpiarSemana() }
        }
    }

    private fun mapearSlots(
        data: DisponibilidadDataDto,
        capacidadBase: Int
    ): Map<SlotKey, Int> {
        return data.slots.orEmpty()
            .filter { it.activo != false }
            .mapNotNull { slot ->
                val diaSemanaId = slot.diaSemanaId ?: return@mapNotNull null
                val bloqueHorarioId = slot.bloqueHorarioId ?: return@mapNotNull null
                val cupo = (slot.maxMascotas ?: capacidadBase)
                    .coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
                SlotKey(diaSemanaId, bloqueHorarioId) to cupo
            }
            .toMap()
    }

    /**
     * Ejecuta una escritura serializada: un PUT de guardado nunca se cruza con
     * un PATCH de bloque o de capacidad base.
     */
    private suspend fun <T> llamarApi(
        descripcion: String,
        llamada: suspend () -> Response<T>
    ): ResultadoApi<T> = withContext(Dispatchers.IO) {
        mutexEscritura.withLock {
            try {
                val response = llamada()
                if (response.isSuccessful) {
                    ResultadoApi.Exito(response)
                } else {
                    Log.e(TAG, "HTTP ${response.code()} al $descripcion")
                    ResultadoApi.Error(
                        ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al $descripcion", e)
                ResultadoApi.Error(
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                )
            }
        }
    }

    private fun headerActual(): String? {
        if (authHeaderActual.isNotBlank()) return authHeaderActual

        Log.e(TAG, "No hay token para guardar la disponibilidad del paseador")
        _messages.tryEmit(
            DisponibilidadMessage(
                ApiErrorMapper.fromThrowable(
                    IllegalStateException("Token vacío"),
                    ApiErrorContext.WALKER
                )
            )
        )
        return null
    }

    private fun emitirInfo(mensaje: String) {
        _messages.tryEmit(DisponibilidadMessage(mensaje))
    }

    private fun emitirError(mensaje: String, retry: (() -> Unit)? = null) {
        _messages.tryEmit(DisponibilidadMessage(mensaje, retry))
    }

    private fun setError(message: String, retry: (() -> Unit)? = null) {
        _uiState.value = DisponibilidadUiState.Error(message)
        _messages.tryEmit(DisponibilidadMessage(message, retry))
    }

    private fun successActual(): DisponibilidadUiState.Success? =
        _uiState.value as? DisponibilidadUiState.Success

    private fun updateSuccess(
        transform: (DisponibilidadUiState.Success) -> DisponibilidadUiState.Success
    ) {
        _uiState.update { current ->
            if (current is DisponibilidadUiState.Success) transform(current) else current
        }
    }

    private fun nuevaOperacion(): Long {
        val id = ++secuenciaOperacion
        operacionesActivas += id
        actualizarIndicador()
        return id
    }

    private fun terminarOperacion(id: Long) {
        operacionesActivas -= id
        actualizarIndicador()
    }

    private fun actualizarIndicador() {
        val activas = operacionesActivas.isNotEmpty()
        updateSuccess { it.copy(isSaving = activas) }
    }

    private fun cancelarOperacionesPendientes() {
        jobCapacidad?.cancel()
        jobCapacidad = null
        jobGuardado?.cancel()
        jobGuardado = null
        jobsSlot.values.forEach { it.cancel() }
        jobsSlot.clear()
        operacionesActivas.clear()
        actualizarIndicador()
    }

    private fun construirAuthHeader(token: String): String {
        val limpio = token.trim()
        return when {
            limpio.isBlank() -> ""
            limpio.startsWith("Bearer ") -> limpio
            else -> "Bearer $limpio"
        }
    }

    private sealed interface ResultadoApi<out T> {
        data class Exito<T>(val response: Response<T>) : ResultadoApi<T>
        data class Error(val mensaje: String) : ResultadoApi<Nothing>
    }

    private companion object {
        private const val TAG = "WALKER_DISPONIBILIDAD"
        private const val MIN_CAPACIDAD = 1
        private const val MAX_CAPACIDAD = 6
        private const val CAPACIDAD_BASE_DEFAULT = 3
        private const val DEBOUNCE_GUARDADO_MS = 600L
        private const val ACCION_GUARDAR_CUPO = "GUARDAR_CUPO"
        private const val ACCION_CANCELAR = "CANCELAR"
    }
}
