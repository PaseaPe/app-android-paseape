package com.example.paseape

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.paseape.data.model.RegistrarUbicacionesBatchReqDto
import com.example.paseape.data.model.UbicacionPuntoReqDto
import com.example.paseape.data.network.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Mantiene el GPS y el envío de ubicaciones de un paseo en curso aunque la
 * pantalla esté bloqueada o el paseador navegue a otra pantalla.
 *
 * Se ejecuta como servicio en primer plano de tipo `location`, por lo que basta
 * con el permiso de ubicación normal (no requiere ubicación en segundo plano).
 * La notificación es permanente y silenciosa: al tocarla se abre la pantalla
 * "Paseo Activo". No tiene acciones.
 *
 * La Activity solo observa [ubicacionActual] y [servicioActivo]; el servicio es
 * el único dueño del GPS, del búfer y de los envíos a
 * `POST /paseador/solicitudes/{uuid}/ubicaciones`.
 */
class PaseoUbicacionForegroundService : Service() {

    companion object {
        const val ACTION_INICIAR = "com.example.paseape.action.INICIAR_UBICACION"
        const val ACTION_DETENER = "com.example.paseape.action.DETENER_UBICACION"
        const val EXTRA_SOLICITUD_UUID = "extra_solicitud_uuid"
        const val EXTRA_AUTH_TOKEN = "extra_auth_token"

        private const val TAG = "PASEO_UBICACION"
        private const val CANAL_ID = "paseo_en_curso"
        private const val NOTIFICACION_ID = 4101
        private const val INTERVALO_ACTUALIZACION_MS = 8_000L
        private const val INTERVALO_ENVIO_MS = 25_000L
        private const val MAX_PUNTOS_POR_LOTE = 50
        private const val TIEMPO_MAXIMO_ENVIO_FINAL_MS = 6_000L

        private val _ubicacionActual = MutableStateFlow<LatLng?>(null)
        val ubicacionActual: StateFlow<LatLng?> = _ubicacionActual.asStateFlow()

        private val _precisionMetros = MutableStateFlow<Double?>(null)
        val precisionMetros: StateFlow<Double?> = _precisionMetros.asStateFlow()

        private val _servicioActivo = MutableStateFlow(false)
        val servicioActivo: StateFlow<Boolean> = _servicioActivo.asStateFlow()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val envioMutex = Mutex()
    private val bufferLock = Any()
    private val puntosPendientes = mutableListOf<UbicacionPuntoReqDto>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var envioJob: Job? = null
    private var solicitudUuid: String = ""
    private var authHeader: String = ""
    @Volatile
    private var deteniendo = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DETENER -> detenerServicio()
            ACTION_INICIAR -> iniciar(intent)
            else -> detenerServicio()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        detenerSeguimiento()
        scope.cancel()
        _servicioActivo.value = false
        _ubicacionActual.value = null
        _precisionMetros.value = null
        super.onDestroy()
    }

    private fun iniciar(intent: Intent) {
        val uuid = intent.getStringExtra(EXTRA_SOLICITUD_UUID)?.trim().orEmpty()
        val token = intent.getStringExtra(EXTRA_AUTH_TOKEN)?.trim().orEmpty()

        if (uuid.isBlank() || token.isBlank() || !tienePermisoUbicacion()) {
            Log.w(TAG, "No se puede iniciar el seguimiento: faltan datos o permiso.")
            detenerServicio()
            return
        }

        if (solicitudUuid.isNotBlank() && solicitudUuid != uuid) {
            // Nunca se mezclan puntos de dos paseos distintos.
            synchronized(bufferLock) { puntosPendientes.clear() }
            _ubicacionActual.value = null
            _precisionMetros.value = null
        }

        solicitudUuid = uuid
        authHeader = token
        deteniendo = false

        // Debe llamarse de inmediato: Android exige el primer plano en pocos segundos.
        startForeground(
            NOTIFICACION_ID,
            construirNotificacion(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
        _servicioActivo.value = true

        if (locationCallback != null) return
        iniciarSeguimiento()
        iniciarEnvioPeriodico()
    }

    private fun detenerServicio() {
        if (deteniendo) return
        deteniendo = true

        scope.launch {
            detenerEnvioPeriodico()
            detenerSeguimiento()

            // Último lote antes de soltar el servicio (el paseo aún está EN_CURSO).
            withTimeoutOrNull(TIEMPO_MAXIMO_ENVIO_FINAL_MS) {
                enviarPuntosPendientes()
            }

            // Si mientras tanto se pidió reanudar el paseo, no se apaga.
            if (!deteniendo) return@launch

            _servicioActivo.value = false
            _ubicacionActual.value = null
            _precisionMetros.value = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun iniciarEnvioPeriodico() {
        if (envioJob != null) return
        envioJob = scope.launch {
            while (isActive) {
                delay(INTERVALO_ENVIO_MS)
                enviarPuntosPendientes()
            }
        }
    }

    private fun detenerEnvioPeriodico() {
        envioJob?.cancel()
        envioJob = null
    }

    private fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    private fun iniciarSeguimiento() {
        if (locationCallback != null) return
        if (!tienePermisoUbicacion()) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            INTERVALO_ACTUALIZACION_MS
        )
            .setMinUpdateIntervalMillis(4_000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    onNuevaUbicacion(it.latitude, it.longitude, it.accuracy, it.time)
                }
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onNuevaUbicacion(
                        location.latitude,
                        location.longitude,
                        location.accuracy,
                        location.time
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso para solicitar actualizaciones de ubicación", e)
            locationCallback = null
        }
    }

    private fun detenerSeguimiento() {
        val callback = locationCallback ?: return
        runCatching { fusedLocationClient.removeLocationUpdates(callback) }
        locationCallback = null
    }

    private fun onNuevaUbicacion(
        latitud: Double,
        longitud: Double,
        precision: Float,
        tiempoGpsMillis: Long
    ) {
        val ahora = System.currentTimeMillis()
        val tiempo = if (tiempoGpsMillis in 1..ahora) tiempoGpsMillis else ahora
        val fechaGps = LocalDateTime
            .ofInstant(Instant.ofEpochMilli(tiempo), ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        agregarPunto(
            UbicacionPuntoReqDto(
                latitud = latitud,
                longitud = longitud,
                precisionM = precision.toDouble().takeIf { it >= 0 },
                fechaHoraGps = fechaGps
            )
        )

        _ubicacionActual.value = LatLng(latitud, longitud)
        _precisionMetros.value = precision.toDouble().takeIf { it >= 0 }
    }

    private suspend fun enviarPuntosPendientes() {
        val uuid = solicitudUuid
        val token = authHeader
        if (uuid.isBlank() || token.isBlank()) return

        envioMutex.withLock {
            val lote = tomarLote()
            if (lote.isEmpty()) return@withLock

            val response = try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.registrarUbicaciones(
                        token = token,
                        uuid = uuid,
                        body = RegistrarUbicacionesBatchReqDto(ubicaciones = lote)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo enviar el lote de ubicaciones", e)
                recortarBuffer()
                return@withLock
            }

            if (response.isSuccessful) {
                confirmarLote(lote)
            } else {
                Log.w(TAG, "Telemetría rechazada (HTTP ${response.code()})")
                recortarBuffer()
            }
        }
    }

    private fun agregarPunto(punto: UbicacionPuntoReqDto) {
        synchronized(bufferLock) {
            puntosPendientes.add(punto)
            while (puntosPendientes.size > MAX_PUNTOS_POR_LOTE) {
                puntosPendientes.removeAt(0)
            }
        }
    }

    private fun tomarLote(): List<UbicacionPuntoReqDto> = synchronized(bufferLock) {
        if (puntosPendientes.isEmpty()) emptyList() else puntosPendientes.toList()
    }

    private fun confirmarLote(lote: List<UbicacionPuntoReqDto>) {
        synchronized(bufferLock) { puntosPendientes.removeAll(lote.toSet()) }
    }

    private fun recortarBuffer() {
        synchronized(bufferLock) {
            while (puntosPendientes.size > MAX_PUNTOS_POR_LOTE) {
                puntosPendientes.removeAt(0)
            }
        }
    }

    private fun crearCanalNotificacion() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CANAL_ID) != null) return

        val canal = NotificationChannel(
            CANAL_ID,
            "Paseo en curso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Indica que tu ubicación se comparte con el cliente durante el paseo."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(canal)
    }

    private fun construirNotificacion(): Notification {
        val abrirPaseo = Intent(this, PaseoActivoPaseadorActivity::class.java).apply {
            putExtra(PaseoActivoPaseadorActivity.EXTRA_SOLICITUD_UUID, solicitudUuid)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            abrirPaseo,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Paseo en curso")
            .setContentText("Compartiendo tu ubicación con el cliente.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
