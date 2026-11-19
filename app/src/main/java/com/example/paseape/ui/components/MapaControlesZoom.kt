package com.example.paseape.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.paseape.ui.theme.PrimaryDark
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

/**
 * Controles + / - para hacer zoom sobre un [GoogleMap].
 *
 * Se dibuja como hermano posterior al AndroidView del mapa, así que queda
 * encima sin interferir con los gestos nativos de la vista del mapa.
 */
@Composable
fun ControlesZoomMapa(
    googleMap: GoogleMap?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BotonZoomMapa(
            icono = Icons.Default.Add,
            descripcion = "Acercar mapa",
            onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) }
        )
        BotonZoomMapa(
            icono = Icons.Default.Remove,
            descripcion = "Alejar mapa",
            onClick = { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }
        )
    }
}

@Composable
private fun BotonZoomMapa(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(elevation = 4.dp, shape = forma, clip = false)
            .clip(forma)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = PrimaryDark,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Avisa cuando el usuario toca (y mientras mantiene el dedo sobre) el mapa.
 *
 * El mapa vive dentro de una pantalla con `verticalScroll`: sin este aviso, el
 * scroll padre se queda con el arrastre y el mapa no puede moverse. El callback
 * permite desactivar el scroll mientras dure el toque. No consume los eventos,
 * porque si se consumen la vista del mapa deja de recibir el gesto.
 */
@Composable
fun Modifier.observarToqueMapa(onMapaTocado: (Boolean) -> Unit): Modifier {
    val aviso = rememberUpdatedState(onMapaTocado)

    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            aviso.value(true)
            try {
                var evento = awaitPointerEvent()
                while (evento.changes.any { it.pressed }) {
                    evento = awaitPointerEvent()
                }
            } finally {
                aviso.value(false)
            }
        }
    }
}
