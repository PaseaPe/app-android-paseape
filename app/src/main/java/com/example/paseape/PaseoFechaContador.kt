package com.example.paseape

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.SolicitudPaseoResDto
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")

/** Estado del backend: solicitud inmediata aún buscando paseador. */
internal const val ESTADO_SOLICITUD_BUSCANDO = 8

/**
 * El backend (api-paseape) corre con `user.timezone=UTC` y serializa
 * `creado_el` y `hora_vencimiento` como LocalDateTime sin zona. Los pasamos
 * a la zona del dispositivo antes de mostrarlos.
 */
internal fun fechaHoraUtcALocal(texto: String?): LocalDateTime? {
    val valor = texto?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        LocalDateTime.parse(valor)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }.getOrNull()
}

/** `fecha_programada` llega como LocalDate ISO ("2026-10-27"). */
internal fun formatearFechaPaseo(texto: String?): String? {
    val valor = texto?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        LocalDate.parse(valor).format(FORMATO_FECHA)
    }.getOrNull() ?: fechaHoraUtcALocal(valor)?.format(FORMATO_FECHA)
}

internal fun formatearHoraPaseo(texto: String?): String? =
    fechaHoraUtcALocal(texto)?.format(FORMATO_HORA)

/** "B06_07" -> "06:00 - 07:00"; cualquier otro código se deja tal cual. */
internal fun formatearBloqueHorario(codigo: String?): String? {
    val valor = codigo?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val match = Regex("""^B(\d{2})_(\d{2})$""").find(valor) ?: return valor
    return "${match.groupValues[1]}:00 - ${match.groupValues[2]}:00"
}

/**
 * Línea "📅 fecha · 🕒 hora" de una tarjeta de paseo.
 * - Programado: `fecha_programada` + bloque horario.
 * - Inmediato: `creado_el` (UTC -> hora local).
 */
internal fun textoFechaHoraPaseo(
    fechaProgramada: String?,
    bloqueHorarioCodigo: String?,
    creadoEl: String?
): String? {
    val fecha = formatearFechaPaseo(fechaProgramada)
    if (fecha != null) {
        val bloque = formatearBloqueHorario(bloqueHorarioCodigo)
        return if (bloque != null) "📅 $fecha · 🕒 $bloque" else "📅 $fecha"
    }
    val fechaCreacion = fechaHoraUtcALocal(creadoEl)?.format(FORMATO_FECHA)
    val horaCreacion = formatearHoraPaseo(creadoEl)
    return when {
        fechaCreacion != null && horaCreacion != null -> "📅 $fechaCreacion · 🕒 $horaCreacion"
        fechaCreacion != null -> "📅 $fechaCreacion"
        else -> null
    }
}

private fun instanteVencimiento(horaVencimiento: String?): Instant? {
    val valor = horaVencimiento?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { LocalDateTime.parse(valor).toInstant(ZoneOffset.UTC) }.getOrNull()
}

/**
 * "dd/MM/yyyy · HH:mm" en la hora local del dispositivo a partir de `creado_el`
 * (el backend lo guarda en UTC).
 */
internal fun textoFechaHoraNotificacion(creadoEl: String?): String? =
    fechaHoraUtcALocal(creadoEl)?.format(FORMATO_FECHA_HORA)

/**
 * Una solicitud inmediata está expirada cuando pasó su `hora_vencimiento`
 * (creación + 5 minutos). Solo aplica a las que siguen buscando paseador,
 * así se filtran del listado en cuanto el contador llega a 0.
 */
internal fun solicitudExpirada(solicitud: SolicitudPaseoResDto, ahora: Instant = Instant.now()): Boolean {
    if (solicitud.estadoId != ESTADO_SOLICITUD_BUSCANDO) return false
    val vencimiento = instanteVencimiento(solicitud.horaVencimiento) ?: return false
    return !vencimiento.isAfter(ahora)
}

/**
 * "Reloj vivo" para ocultar tarjetas que acaban de vencer.
 * Solo hace tick cuando [activo] es true para no recomponer la pantalla sin necesidad.
 */
@Composable
internal fun recordarAhora(activo: Boolean): Instant {
    var ahora by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(activo) {
        if (!activo) return@LaunchedEffect
        ahora = Instant.now()
        while (true) {
            delay(1_000L)
            ahora = Instant.now()
        }
    }

    return ahora
}

/** Lo más reciente primero: ordena por `creado_el` real (UTC -> local). */
internal fun ordenarPorCreacionDesc(lista: List<SolicitudPaseoResDto>): List<SolicitudPaseoResDto> =
    lista.sortedByDescending { fechaHoraUtcALocal(it.creadoEl) ?: LocalDateTime.MIN }

/**
 * Contador regresivo de una solicitud inmediata. El backend la marca VENCIDA
 * a los 5 minutos (`hora_vencimiento`), así que al llegar a 0 muestra "Expirada".
 */
@Composable
internal fun ContadorVencimiento(
    horaVencimiento: String?,
    modifier: Modifier = Modifier
) {
    val vencimiento = remember(horaVencimiento) { instanteVencimiento(horaVencimiento) } ?: return

    var restante by remember(vencimiento) {
        mutableStateOf(Duration.between(Instant.now(), vencimiento).seconds.coerceAtLeast(0L))
    }

    LaunchedEffect(vencimiento) {
        while (true) {
            val segundos = Duration.between(Instant.now(), vencimiento).seconds
            restante = segundos.coerceAtLeast(0L)
            if (segundos <= 0L) break
            delay(1_000L)
        }
    }

    val vencida = restante <= 0L
    val texto = if (vencida) {
        "⏳ Expirada"
    } else {
        String.format(Locale.US, "⏳ Expira en %d:%02d", restante / 60, restante % 60)
    }

    Surface(
        color = if (vencida) Color(0xFFFEF2F2) else Color(0xFFFFF3E0),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = texto,
            color = if (vencida) Color(0xFFB91C1C) else Color(0xFFE65100),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
