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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val isSaving: Boolean = false
    ) : DisponibilidadUiState {

        val sinGuardar: Boolean
            get() = capacidadBase != capacidadBaseGuardada || slots != slotsGuardados

        val estaVacia: Boolean
            get() = slots.isEmpty()

        val puedeGuardar: Boolean
            get() = !isSaving && (slots.isNotEmpty() || slotsGuardados.isNotEmpty())
    }
}

/** Mensaje para el Snackbar. retry = true cuando conviene ofrecer "Reintentar". */
data class DisponibilidadMessage(
    val text: String,
    val retry: Boolean = false
)

class DisponibilidadViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DisponibilidadUiState>(DisponibilidadUiState.Loading)
    val uiState: StateFlow<DisponibilidadUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<DisponibilidadMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<DisponibilidadMessage> = _messages.asSharedFlow()

    private var tokenCargado: String? = null

    /**
     * Carga catálogos y disponibilidad. Si ya se cargó con el mismo token y el
     * estado sigue siendo exitoso, no vuelve a consultar (conserva cambios sin
     * guardar al cambiar de pestaña).
     */
    fun cargar(token: String, force: Boolean = false) {
        val authHeader = construirAuthHeader(token)
        if (authHeader.isBlank()) {
            Log.e(TAG, "No hay token para consultar la disponibilidad del paseador")
            val message = ApiErrorMapper.fromThrowable(
                IllegalStateException("Token vacío"),
                ApiErrorContext.WALKER
            )
            setError(message, retry = true)
            return
        }

        if (!force &&
            authHeader == tokenCargado &&
            _uiState.value is DisponibilidadUiState.Success
        ) {
            return
        }

        tokenCargado = authHeader
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
                    withContext(Dispatchers.Main) { setError(message, retry = true) }
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
                    withContext(Dispatchers.Main) { setError(message, retry = true) }
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
                    withContext(Dispatchers.Main) { setError(message, retry = true) }
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
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar la disponibilidad semanal", e)
                withContext(Dispatchers.Main) {
                    setError(
                        ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER),
                        retry = true
                    )
                }
            }
        }
    }

    fun activarSlot(slot: SlotKey) {
        updateSuccess { state ->
            if (state.isSaving || state.slots.containsKey(slot)) {
                state
            } else {
                state.copy(slots = state.slots + (slot to state.capacidadBase))
            }
        }
    }

    fun actualizarCupo(slot: SlotKey, cupo: Int) {
        val cupoValido = cupo.coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
        updateSuccess { state ->
            if (state.isSaving) {
                state
            } else {
                state.copy(slots = state.slots + (slot to cupoValido))
            }
        }
    }

    fun quitarSlot(slot: SlotKey) {
        updateSuccess { state ->
            if (state.isSaving) state else state.copy(slots = state.slots - slot)
        }
    }

    /**
     * Cambia la capacidad base. Las celdas que estaban en estado BASE (su cupo
     * era igual al base anterior) pasan a mostrar el nuevo valor; las celdas
     * personalizadas conservan su cupo.
     */
    fun ajustarCapacidadBase(delta: Int) {
        updateSuccess { state ->
            if (state.isSaving) return@updateSuccess state

            val nuevaCapacidad = (state.capacidadBase + delta)
                .coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
            if (nuevaCapacidad == state.capacidadBase) {
                state
            } else {
                val slotsActualizados = state.slots.mapValues { (_, cupo) ->
                    if (cupo == state.capacidadBase) nuevaCapacidad else cupo
                }
                state.copy(
                    capacidadBase = nuevaCapacidad,
                    slots = slotsActualizados
                )
            }
        }
    }

    fun marcarLunesAViernes() {
        aplicarChip { dias -> dias.take(5).map { it.id }.toSet() }
    }

    fun marcarFinesDeSemana() {
        aplicarChip { dias -> dias.drop(5).map { it.id }.toSet() }
    }

    fun limpiarSemana() {
        updateSuccess { state ->
            if (state.isSaving) state else state.copy(slots = emptyMap())
        }
    }

    /**
     * Descarta los cambios pendientes y vuelve al último estado confirmado por
     * el backend. No realiza llamadas a la API.
     */
    fun descartarCambios() {
        val state = _uiState.value as? DisponibilidadUiState.Success ?: return
        if (state.isSaving || !state.sinGuardar) return

        _uiState.value = state.copy(
            capacidadBase = state.capacidadBaseGuardada,
            slots = state.slotsGuardados
        )
        _messages.tryEmit(DisponibilidadMessage("Cambios descartados"))
    }

    /**
     * Guarda la semana:
     * - con bloques: PUT completo + PATCH de capacidad base si cambió.
     * - sin bloques y con bloques guardados: PATCH CANCELAR por cada bloque activo
     *   (el backend rechaza un PUT vacío). Si algún bloque tiene reservas, se
     *   conserva marcado y se muestra el mensaje del backend.
     */
    fun guardar(token: String) {
        val state = _uiState.value as? DisponibilidadUiState.Success ?: return
        if (state.isSaving) return

        if (state.slots.isEmpty() && state.slotsGuardados.isEmpty()) {
            _messages.tryEmit(
                DisponibilidadMessage("Selecciona al menos un bloque")
            )
            return
        }

        val authHeader = construirAuthHeader(token)
        if (authHeader.isBlank()) {
            Log.e(TAG, "No hay token para guardar la disponibilidad del paseador")
            _messages.tryEmit(
                DisponibilidadMessage(
                    ApiErrorMapper.fromThrowable(
                        IllegalStateException("Token vacío"),
                        ApiErrorContext.WALKER
                    )
                )
            )
            return
        }

        updateSuccess { it.copy(isSaving = true) }

        viewModelScope.launch(Dispatchers.IO) {
            var baseGuardada = state.capacidadBaseGuardada
            try {
                if (state.capacidadBase != state.capacidadBaseGuardada) {
                    val capacidadResponse =
                        RetrofitClient.apiService.actualizarCapacidadBase(
                            token = authHeader,
                            request = ActualizarCapacidadBaseRequestDto(state.capacidadBase)
                        )

                    if (!capacidadResponse.isSuccessful) {
                        Log.e(
                            TAG,
                            "HTTP ${capacidadResponse.code()} al actualizar capacidad base"
                        )
                        val message = ApiErrorMapper.fromResponse(
                            capacidadResponse,
                            ApiErrorContext.WALKER
                        )
                        withContext(Dispatchers.Main) {
                            updateSuccess { it.copy(isSaving = false) }
                            _messages.tryEmit(DisponibilidadMessage(message))
                        }
                        return@launch
                    }

                    baseGuardada = state.capacidadBase
                    withContext(Dispatchers.Main) {
                        updateSuccess { it.copy(capacidadBaseGuardada = baseGuardada) }
                    }
                    Log.d(TAG, "Capacidad base actualizada: $baseGuardada")
                }

                if (state.slots.isEmpty()) {
                    cancelarTodos(authHeader, state, baseGuardada)
                } else {
                    sincronizar(authHeader, state, baseGuardada)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al guardar la disponibilidad", e)
                withContext(Dispatchers.Main) {
                    updateSuccess { it.copy(isSaving = false) }
                    _messages.tryEmit(
                        DisponibilidadMessage(
                            ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALKER)
                        )
                    )
                }
            }
        }
    }

    private suspend fun sincronizar(
        authHeader: String,
        snapshot: DisponibilidadUiState.Success,
        baseGuardada: Int
    ) {
        val bloques = snapshot.slots.entries
            .sortedBy { it.key.diaSemanaId * 100 + it.key.bloqueHorarioId }
            .map { (slot, cupo) ->
                DisponibilidadBloqueRequestDto(
                    diaSemanaId = slot.diaSemanaId,
                    bloqueHorarioId = slot.bloqueHorarioId,
                    maxMascotas = cupo
                )
            }

        val response = RetrofitClient.apiService.sincronizarDisponibilidad(
            token = authHeader,
            request = GuardarDisponibilidadRequestDto(bloques = bloques)
        )

        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP ${response.code()} al sincronizar disponibilidad")
            val message = ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
            withContext(Dispatchers.Main) {
                updateSuccess { it.copy(isSaving = false) }
                _messages.tryEmit(DisponibilidadMessage(message))
            }
            return
        }

        val data = response.body()?.response
        withContext(Dispatchers.Main) {
            if (data != null && data.slots != null) {
                val base = (data.maxMascotasPredeterminado ?: baseGuardada)
                    .coerceIn(MIN_CAPACIDAD, MAX_CAPACIDAD)
                val slotsConfirmados = mapearSlots(data, base)
                updateSuccess {
                    it.copy(
                        isSaving = false,
                        capacidadBase = base,
                        capacidadBaseGuardada = base,
                        slots = slotsConfirmados,
                        slotsGuardados = slotsConfirmados
                    )
                }
            } else {
                updateSuccess {
                    it.copy(
                        isSaving = false,
                        capacidadBaseGuardada = snapshot.capacidadBase,
                        slotsGuardados = snapshot.slots
                    )
                }
            }
            _messages.tryEmit(DisponibilidadMessage("Disponibilidad guardada"))
        }
        Log.d(TAG, "Disponibilidad sincronizada: ${bloques.size} bloques")
    }

    private suspend fun cancelarTodos(
        authHeader: String,
        snapshot: DisponibilidadUiState.Success,
        baseGuardada: Int
    ) {
        var primerError: String? = null
        val bloquesQueSiguenActivos = mutableMapOf<SlotKey, Int>()

        for ((slot, cupo) in snapshot.slotsGuardados) {
            val response = RetrofitClient.apiService.actualizarBloqueDisponibilidad(
                token = authHeader,
                request = ActualizarBloqueDisponibilidadRequestDto(
                    diaSemanaId = slot.diaSemanaId,
                    bloqueHorarioId = slot.bloqueHorarioId,
                    accion = ACCION_CANCELAR
                )
            )

            if (response.isSuccessful) {
                Log.d(
                    TAG,
                    "Bloque cancelado: día ${slot.diaSemanaId}, bloque ${slot.bloqueHorarioId}"
                )
            } else {
                val message = ApiErrorMapper.fromResponse(response, ApiErrorContext.WALKER)
                Log.e(
                    TAG,
                    "No se pudo cancelar el bloque ${slot.diaSemanaId}-${slot.bloqueHorarioId}: " +
                        "HTTP ${response.code()} - $message"
                )
                bloquesQueSiguenActivos[slot] = cupo
                if (primerError == null) primerError = message
            }
        }

        withContext(Dispatchers.Main) {
            updateSuccess {
                it.copy(
                    isSaving = false,
                    capacidadBaseGuardada = baseGuardada,
                    slots = bloquesQueSiguenActivos,
                    slotsGuardados = bloquesQueSiguenActivos
                )
            }

            val error = primerError
            if (error == null) {
                _messages.tryEmit(DisponibilidadMessage("Disponibilidad guardada"))
            } else {
                _messages.tryEmit(DisponibilidadMessage(error))
            }
        }
    }

    private fun aplicarChip(seleccionarDias: (List<DiaSemanaParamDto>) -> Set<Int>) {
        updateSuccess { state ->
            if (state.isSaving) return@updateSuccess state

            val diasSeleccionados = seleccionarDias(state.diasSemana)
            if (diasSeleccionados.isEmpty()) return@updateSuccess state

            val slotsActualizados = state.slots.toMutableMap()
            state.diasSemana
                .filter { it.id in diasSeleccionados }
                .forEach { dia ->
                    state.bloquesHorario.forEach { bloque ->
                        val slot = SlotKey(dia.id, bloque.id)
                        if (!slotsActualizados.containsKey(slot)) {
                            slotsActualizados[slot] = state.capacidadBase
                        }
                    }
                }
            state.copy(slots = slotsActualizados)
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

    private fun setError(message: String, retry: Boolean) {
        _uiState.value = DisponibilidadUiState.Error(message)
        _messages.tryEmit(DisponibilidadMessage(message, retry = retry))
    }

    private fun updateSuccess(
        transform: (DisponibilidadUiState.Success) -> DisponibilidadUiState.Success
    ) {
        _uiState.update { current ->
            if (current is DisponibilidadUiState.Success) transform(current) else current
        }
    }

    private fun construirAuthHeader(token: String): String {
        val limpio = token.trim()
        return when {
            limpio.isBlank() -> ""
            limpio.startsWith("Bearer ") -> limpio
            else -> "Bearer $limpio"
        }
    }

    private companion object {
        private const val TAG = "WALKER_DISPONIBILIDAD"
        private const val MIN_CAPACIDAD = 1
        private const val MAX_CAPACIDAD = 6
        private const val CAPACIDAD_BASE_DEFAULT = 3
        private const val ACCION_CANCELAR = "CANCELAR"
    }
}
