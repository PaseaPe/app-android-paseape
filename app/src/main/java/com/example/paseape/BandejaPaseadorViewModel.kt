package com.example.paseape

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paseape.data.model.PageResponseDto
import com.example.paseape.data.model.RechazarSolicitudRequest
import com.example.paseape.data.model.SolicitudPaseoResDto
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.data.network.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

/** Pestañas de la bandeja de paseos. El valor de [valorDeTabAPI] viaja tal cual al backend. */
enum class TabPaseos(val valorDeTabAPI: String, val etiquetaUI: String) {
    INMEDIATOS("INMEDIATAS", "Inmediatos"),
    MIS_PASEOS("MIS_PASEOS", "Mis paseos"),
    EN_CURSO("EN_CURSO", "En Curso"),
    HISTORIAL("HISTORIAL", "Historial")
}

/** Acción en curso sobre una solicitud, para bloquear botones y mostrar el spinner correcto. */
enum class AccionPaseo {
    TOMAR,
    DESCARTAR,
    ACEPTAR,
    RECHAZAR
}

data class TabPaseosData(
    val items: List<SolicitudPaseoResDto>,
    val page: Int,
    val hasNext: Boolean,
    val totalElements: Long,
    val cargando: Boolean = false,
    val cargandoMas: Boolean = false,
    val error: String? = null
)

sealed interface PaseosUiState {

    data object Loading : PaseosUiState

    data class Error(val message: String) : PaseosUiState

    data class Success(
        val tabs: Map<TabPaseos, TabPaseosData>
    ) : PaseosUiState
}

/**
 * Mensaje para el Snackbar. Si [retry] no es null, la pantalla muestra la acción
 * "Reintentar" y ejecuta ese bloque cuando el usuario la pulsa.
 */
data class PaseosMessage(
    val text: String,
    val retry: (() -> Unit)? = null
)

/**
 * Bandeja "Gestión de Paseos" del paseador.
 *
 * - Carga las 4 pestañas en paralelo para pintar todos los contadores.
 * - Cada pestaña cachea su página actual y su estado de paginación.
 * - Las escrituras (tomar/descartar/aceptar/rechazar) se serializan con un Mutex
 *   y, si fallan, revierten el cambio visual mínimo y ofrecen "Reintentar".
 */
class BandejaPaseadorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PaseosUiState>(PaseosUiState.Loading)
    val uiState: StateFlow<PaseosUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<PaseosMessage>(extraBufferCapacity = 1)
    val messages: SharedFlow<PaseosMessage> = _messages.asSharedFlow()

    private val _accionesEnCurso = MutableStateFlow<Map<String, AccionPaseo>>(emptyMap())
    val accionesEnCurso: StateFlow<Map<String, AccionPaseo>> = _accionesEnCurso.asStateFlow()

    private var tokenCargado: String? = null
    private var authHeaderActual: String = ""
    private var jobCarga: Job? = null

    private val mutexEscritura = Mutex()

    /**
     * Carga las 4 pestañas (página 0). Si ya se cargaron con el mismo token no
     * se vuelve a consultar, de modo que navegar entre pestañas conserva datos.
     */
    fun cargar(token: String, force: Boolean = false) {
        val header = construirAuthHeader(token)
        if (header.isBlank()) {
            Log.e(TAG, "No hay token para consultar la bandeja del paseador")
            val mensaje = ApiErrorMapper.fromThrowable(
                IllegalStateException("Token vacío"),
                ApiErrorContext.WALK
            )
            _uiState.value = PaseosUiState.Error(mensaje)
            _messages.tryEmit(PaseosMessage(mensaje) { cargar(token, force = true) })
            return
        }

        authHeaderActual = header

        if (!force &&
            header == tokenCargado &&
            _uiState.value !is PaseosUiState.Error
        ) {
            return
        }

        tokenCargado = header
        jobCarga?.cancel()
        _uiState.value = PaseosUiState.Loading

        jobCarga = viewModelScope.launch {
            val resultados = cargarTabsEnParalelo(header, TabPaseos.entries)
            val tabs = mutableMapOf<TabPaseos, TabPaseosData>()
            var primerError: String? = null

            resultados.forEach { (tab, resultado) ->
                when (resultado) {
                    is ResultadoCarga.Exito -> tabs[tab] = resultado.data
                    is ResultadoCarga.Error -> {
                        tabs[tab] = TabPaseosData(
                            items = emptyList(),
                            page = 0,
                            hasNext = false,
                            totalElements = 0L,
                            error = resultado.mensaje
                        )
                        if (primerError == null) primerError = resultado.mensaje
                    }
                }
            }

            if (tabs.values.all { it.error != null }) {
                val mensaje = primerError ?: ApiErrorMapper.fromThrowable(
                    IllegalStateException("Bandeja vacía"),
                    ApiErrorContext.WALK
                )
                _uiState.value = PaseosUiState.Error(mensaje)
                _messages.tryEmit(PaseosMessage(mensaje) { cargar(header, force = true) })
            } else {
                _uiState.value = PaseosUiState.Success(tabs)
            }
        }
    }

    /** Recarga la página 0 de las 4 pestañas (botón de refresco de la top bar). */
    fun refrescarTodo() {
        val header = authHeaderActual.ifBlank { return }
        if (successActual() == null) {
            cargar(header, force = true)
            return
        }

        updateSuccess { state ->
            state.copy(
                tabs = state.tabs.mapValues { (_, data) ->
                    data.copy(cargando = data.items.isEmpty(), error = null)
                }
            )
        }

        viewModelScope.launch {
            val resultados = cargarTabsEnParalelo(header, TabPaseos.entries)
            var primerError: String? = null

            resultados.forEach { (tab, resultado) ->
                when (resultado) {
                    is ResultadoCarga.Exito -> updateSuccess { state ->
                        state.copy(tabs = state.tabs + (tab to resultado.data))
                    }

                    is ResultadoCarga.Error -> {
                        if (primerError == null) primerError = resultado.mensaje
                        updateSuccess { state ->
                            val previo = state.tabs[tab] ?: return@updateSuccess state
                            state.copy(
                                tabs = state.tabs + (
                                    tab to previo.copy(
                                        cargando = false,
                                        cargandoMas = false,
                                        error = resultado.mensaje
                                    )
                                    )
                            )
                        }
                    }
                }
            }

            primerError?.let { mensaje ->
                _messages.tryEmit(PaseosMessage(mensaje) { refrescarTodo() })
            }
        }
    }

    /** Pide la página siguiente de una pestaña y la agrega al final de la lista. */
    fun cargarMas(tab: TabPaseos) {
        val actual = successActual()?.tabs?.get(tab) ?: return
        if (actual.cargandoMas || !actual.hasNext) return
        val header = authHeaderActual.ifBlank { return }

        updateSuccess { state ->
            val data = state.tabs[tab] ?: return@updateSuccess state
            state.copy(tabs = state.tabs + (tab to data.copy(cargandoMas = true, error = null)))
        }

        viewModelScope.launch {
            when (val resultado = consultarPagina(header, tab, actual.page + 1)) {
                is ResultadoCarga.Exito -> updateSuccess { state ->
                    val previo = state.tabs[tab] ?: return@updateSuccess state
                    val nuevos = resultado.data.items.filter { item ->
                        previo.items.none { it.uuid == item.uuid }
                    }
                    state.copy(
                        tabs = state.tabs + (
                            tab to previo.copy(
                                items = previo.items + nuevos,
                                page = resultado.data.page,
                                hasNext = resultado.data.hasNext,
                                totalElements = resultado.data.totalElements,
                                cargando = false,
                                cargandoMas = false,
                                error = null
                            )
                            )
                    )
                }

                is ResultadoCarga.Error -> {
                    updateSuccess { state ->
                        val previo = state.tabs[tab] ?: return@updateSuccess state
                        state.copy(
                            tabs = state.tabs + (
                                tab to previo.copy(cargandoMas = false)
                                )
                        )
                    }
                    emitirError(resultado.mensaje) { cargarMas(tab) }
                }
            }
        }
    }

    /**
     * Acepta una solicitud inmediata: PATCH /tomar. Se quita de Inmediatos de
     * forma optimista y, al confirmar, se refrescan Inmediatos y Mis paseos.
     */
    fun tomarInmediata(uuid: String) {
        val existe = successActual()?.tabs?.get(TabPaseos.INMEDIATOS)
            ?.items?.any { it.uuid == uuid } ?: false
        if (!existe) return

        val header = authHeaderActual.ifBlank { return }
        if (!marcarAccion(uuid, AccionPaseo.TOMAR)) return

        val removido = quitarLocal(TabPaseos.INMEDIATOS, uuid)

        viewModelScope.launch {
            val resultado = llamarApi("tomar la solicitud") {
                RetrofitClient.apiService.tomarSolicitud(header, uuid)
            }
            desmarcarAccion(uuid)

            when (resultado) {
                is ResultadoApi.Exito -> {
                    ajustarContador(TabPaseos.INMEDIATOS)
                    emitirInfo("Solicitud aceptada")
                    refrescarTabs(setOf(TabPaseos.INMEDIATOS, TabPaseos.MIS_PASEOS))
                }

                is ResultadoApi.Error -> {
                    removido?.let(::restaurarLocal)
                    emitirError(resultado.mensaje) { tomarInmediata(uuid) }
                }
            }
        }
    }

    /** Descarta una solicitud inmediata: PATCH /descartar. Solo se oculta para este paseador. */
    fun descartarInmediata(uuid: String) {
        val existe = successActual()?.tabs?.get(TabPaseos.INMEDIATOS)
            ?.items?.any { it.uuid == uuid } ?: false
        if (!existe) return

        val header = authHeaderActual.ifBlank { return }
        if (!marcarAccion(uuid, AccionPaseo.DESCARTAR)) return

        val removido = quitarLocal(TabPaseos.INMEDIATOS, uuid)

        viewModelScope.launch {
            val resultado = llamarApi("descartar la solicitud") {
                RetrofitClient.apiService.descartarSolicitud(header, uuid)
            }
            desmarcarAccion(uuid)

            when (resultado) {
                is ResultadoApi.Exito -> {
                    ajustarContador(TabPaseos.INMEDIATOS)
                    emitirInfo("Solicitud descartada")
                    refrescarTabs(setOf(TabPaseos.INMEDIATOS))
                }

                is ResultadoApi.Error -> {
                    removido?.let(::restaurarLocal)
                    emitirError(resultado.mensaje) { descartarInmediata(uuid) }
                }
            }
        }
    }

    /**
     * Acepta una solicitud programada: PATCH /aceptar. El badge cambia a
     * ACEPTADA de inmediato y se revierte si la escritura falla.
     */
    fun aceptarProgramada(uuid: String) {
        val original = successActual()?.tabs?.get(TabPaseos.MIS_PASEOS)
            ?.items?.firstOrNull { it.uuid == uuid } ?: return
        if (original.estadoId != ESTADO_PENDIENTE) return

        val header = authHeaderActual.ifBlank { return }
        if (!marcarAccion(uuid, AccionPaseo.ACEPTAR)) return

        actualizarItem(TabPaseos.MIS_PASEOS, uuid) {
            it.copy(estadoId = ESTADO_ACEPTADA, estadoDescripcion = "ACEPTADA")
        }

        viewModelScope.launch {
            val resultado = llamarApi("aceptar la solicitud") {
                RetrofitClient.apiService.aceptarSolicitud(header, uuid)
            }
            desmarcarAccion(uuid)

            when (resultado) {
                is ResultadoApi.Exito -> {
                    emitirInfo("Paseo aceptado")
                    refrescarTabs(setOf(TabPaseos.MIS_PASEOS))
                }

                is ResultadoApi.Error -> {
                    actualizarItem(TabPaseos.MIS_PASEOS, uuid) { original }
                    emitirError(resultado.mensaje) { aceptarProgramada(uuid) }
                }
            }
        }
    }

    /** Rechaza una solicitud programada: PATCH /rechazar con motivo opcional. */
    fun rechazarProgramada(uuid: String, motivo: String) {
        val existe = successActual()?.tabs?.get(TabPaseos.MIS_PASEOS)
            ?.items?.any { it.uuid == uuid } ?: false
        if (!existe) return

        val header = authHeaderActual.ifBlank { return }
        if (!marcarAccion(uuid, AccionPaseo.RECHAZAR)) return

        val removido = quitarLocal(TabPaseos.MIS_PASEOS, uuid)

        viewModelScope.launch {
            val motivoLimpio = motivo.trim().take(MAX_MOTIVO_RECHAZO).ifBlank { null }
            val resultado = llamarApi("rechazar la solicitud") {
                RetrofitClient.apiService.rechazarSolicitud(
                    token = header,
                    uuid = uuid,
                    body = RechazarSolicitudRequest(motivoRechazo = motivoLimpio)
                )
            }
            desmarcarAccion(uuid)

            when (resultado) {
                is ResultadoApi.Exito -> {
                    ajustarContador(TabPaseos.MIS_PASEOS)
                    emitirInfo("Paseo rechazado")
                    refrescarTabs(setOf(TabPaseos.MIS_PASEOS, TabPaseos.HISTORIAL))
                }

                is ResultadoApi.Error -> {
                    removido?.let(::restaurarLocal)
                    emitirError(resultado.mensaje) { rechazarProgramada(uuid, motivo) }
                }
            }
        }
    }

    /**
     * Refresca en segundo plano la página 0 de las pestañas indicadas para
     * actualizar contadores y membresía tras una acción exitosa.
     */
    private fun refrescarTabs(tabs: Set<TabPaseos>) {
        val header = authHeaderActual.ifBlank { return }
        if (tabs.isEmpty()) return

        updateSuccess { state ->
            state.copy(
                tabs = state.tabs.mapValues { (tab, data) ->
                    if (tab in tabs) {
                        data.copy(cargando = data.items.isEmpty(), error = null)
                    } else {
                        data
                    }
                }
            )
        }

        viewModelScope.launch {
            val resultados = cargarTabsEnParalelo(header, tabs)
            resultados.forEach { (tab, resultado) ->
                when (resultado) {
                    is ResultadoCarga.Exito -> updateSuccess { state ->
                        state.copy(tabs = state.tabs + (tab to resultado.data))
                    }

                    is ResultadoCarga.Error -> {
                        Log.e(TAG, "No se pudo refrescar ${tab.valorDeTabAPI}: ${resultado.mensaje}")
                        updateSuccess { state ->
                            val previo = state.tabs[tab] ?: return@updateSuccess state
                            state.copy(
                                tabs = state.tabs + (
                                    tab to previo.copy(cargando = false, cargandoMas = false)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun cargarTabsEnParalelo(
        header: String,
        tabs: Collection<TabPaseos>
    ): List<Pair<TabPaseos, ResultadoCarga>> = coroutineScope {
        tabs.map { tab ->
            async { tab to consultarPagina(header, tab, 0) }
        }.awaitAll()
    }

    private suspend fun consultarPagina(
        header: String,
        tab: TabPaseos,
        page: Int
    ): ResultadoCarga = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.apiService.obtenerBandejaPaseador(
                token = header,
                tab = tab.valorDeTabAPI,
                page = page,
                size = TAMANO_PAGINA
            )
            if (!response.isSuccessful) {
                Log.e(TAG, "HTTP ${response.code()} al consultar la bandeja ${tab.valorDeTabAPI}")
                ResultadoCarga.Error(
                    ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK)
                )
            } else {
                val data = response.body()?.response
                if (data == null) {
                    Log.e(TAG, "Respuesta vacía al consultar la bandeja ${tab.valorDeTabAPI}")
                    ResultadoCarga.Error(
                        ApiErrorMapper.fromThrowable(
                            IllegalStateException("Respuesta vacía"),
                            ApiErrorContext.WALK
                        )
                    )
                } else {
                    ResultadoCarga.Exito(mapearPagina(data, tab))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error de red al consultar la bandeja ${tab.valorDeTabAPI}", e)
            ResultadoCarga.Error(ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK))
        }
    }

    private fun mapearPagina(
        page: PageResponseDto<SolicitudPaseoResDto>,
        tab: TabPaseos
    ): TabPaseosData {
        val items = page.items.orEmpty()
        Log.d(
            TAG,
            "${tab.valorDeTabAPI}: ${items.size} items, total=${page.totalElements ?: 0L}, " +
                "hasNext=${page.hasNext ?: false}"
        )
        return TabPaseosData(
            items = items,
            page = page.page ?: 0,
            hasNext = page.hasNext ?: false,
            totalElements = page.totalElements ?: 0L
        )
    }

    /** Serializa todas las escrituras para que no se crucen entre sí. */
    private suspend fun llamarApi(
        descripcion: String,
        llamada: suspend () -> Response<*>
    ): ResultadoApi = withContext(Dispatchers.IO) {
        mutexEscritura.withLock {
            try {
                val response = llamada()
                if (response.isSuccessful) {
                    ResultadoApi.Exito
                } else {
                    Log.e(TAG, "HTTP ${response.code()} al $descripcion")
                    ResultadoApi.Error(
                        ApiErrorMapper.fromResponse(response, ApiErrorContext.WALK)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error de red al $descripcion", e)
                ResultadoApi.Error(
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.WALK)
                )
            }
        }
    }

    private fun marcarAccion(uuid: String, accion: AccionPaseo): Boolean {
        if (_accionesEnCurso.value.containsKey(uuid)) return false
        _accionesEnCurso.update { it + (uuid to accion) }
        return true
    }

    private fun desmarcarAccion(uuid: String) {
        _accionesEnCurso.update { it - uuid }
    }

    private fun quitarLocal(tab: TabPaseos, uuid: String): ItemRemovido? {
        val actual = successActual()?.tabs?.get(tab) ?: return null
        val indice = actual.items.indexOfFirst { it.uuid == uuid }
        if (indice < 0) return null

        val removido = ItemRemovido(tab, indice, actual.items[indice])
        updateSuccess { state ->
            val data = state.tabs[tab] ?: return@updateSuccess state
            state.copy(
                tabs = state.tabs + (
                    tab to data.copy(items = data.items.filterNot { it.uuid == uuid })
                    )
            )
        }
        return removido
    }

    private fun restaurarLocal(removido: ItemRemovido) {
        updateSuccess { state ->
            val data = state.tabs[removido.tab] ?: return@updateSuccess state
            if (data.items.any { it.uuid == removido.item.uuid }) {
                return@updateSuccess state
            }
            val items = data.items.toMutableList()
            items.add(
                removido.indice.coerceIn(0, items.size),
                removido.item
            )
            state.copy(tabs = state.tabs + (removido.tab to data.copy(items = items)))
        }
    }

    private fun ajustarContador(tab: TabPaseos) {
        updateSuccess { state ->
            val data = state.tabs[tab] ?: return@updateSuccess state
            state.copy(
                tabs = state.tabs + (
                    tab to data.copy(
                        totalElements = (data.totalElements - 1).coerceAtLeast(0L)
                    )
                    )
            )
        }
    }

    private fun actualizarItem(
        tab: TabPaseos,
        uuid: String,
        transform: (SolicitudPaseoResDto) -> SolicitudPaseoResDto
    ) {
        updateSuccess { state ->
            val data = state.tabs[tab] ?: return@updateSuccess state
            state.copy(
                tabs = state.tabs + (
                    tab to data.copy(
                        items = data.items.map { item ->
                            if (item.uuid == uuid) transform(item) else item
                        }
                    )
                    )
            )
        }
    }

    private fun emitirInfo(mensaje: String) {
        _messages.tryEmit(PaseosMessage(mensaje))
    }

    private fun emitirError(mensaje: String, retry: (() -> Unit)? = null) {
        _messages.tryEmit(PaseosMessage(mensaje, retry))
    }

    private fun successActual(): PaseosUiState.Success? =
        _uiState.value as? PaseosUiState.Success

    private fun updateSuccess(transform: (PaseosUiState.Success) -> PaseosUiState.Success) {
        _uiState.update { current ->
            if (current is PaseosUiState.Success) transform(current) else current
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

    private data class ItemRemovido(
        val tab: TabPaseos,
        val indice: Int,
        val item: SolicitudPaseoResDto
    )

    private sealed interface ResultadoCarga {
        data class Exito(val data: TabPaseosData) : ResultadoCarga
        data class Error(val mensaje: String) : ResultadoCarga
    }

    private sealed interface ResultadoApi {
        data object Exito : ResultadoApi
        data class Error(val mensaje: String) : ResultadoApi
    }

    companion object {
        private const val TAG = "WALKER_PASEOS"
        private const val TAMANO_PAGINA = 20
        private const val MAX_MOTIVO_RECHAZO = 500

        // IDs de estado de solicitud (Constant.java del backend).
        internal const val ESTADO_PENDIENTE = 1
        internal const val ESTADO_ACEPTADA = 2
        internal const val ESTADO_RECHAZADA = 3
        internal const val ESTADO_CANCELADA = 4
        internal const val ESTADO_EN_CURSO = 5
        internal const val ESTADO_FINALIZADA = 6
        internal const val ESTADO_VENCIDA = 7
        internal const val ESTADO_BUSCANDO = 8
    }
}
