package com.example.paseape.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.MAX_FOTO_BYTES
import com.example.paseape.data.model.esFotoPermitida
import com.example.paseape.ui.theme.PrimaryOrange

private val VisorFondo = Color(0xFF0B0F14)
private val VisorBarra = Color(0xFF111827)
private val VisorTextoSuave = Color(0xFFCBD5E1)
private val VisorErrorFondo = Color(0xFF3B1212)
private val VisorErrorTexto = Color(0xFFFCA5A5)
private const val ESCALA_MAXIMA = 5f
private const val ESCALA_DOBLE_TOQUE = 2.5f

/**
 * Visor de foto a pantalla completa con zoom
 *
 * Se usa en el perfil del cliente, el perfil del paseador y el detalle de
 * mascota. La foto se cambia desde aquí: al elegir una imagen queda pendiente
 * y se sube con "Guardar foto" (el padre maneja la subida y el progreso).
 *
 * @param fotoUrl foto ya guardada en el backend.
 * @param inicial inicial que se muestra cuando no hay foto (perfiles).
 * @param iconoVacio icono que se muestra cuando no hay foto ni inicial (mascotas).
 * @param fotoPendiente imagen elegida y todavía no subida.
 * @param onSeleccionarFoto se llama con la imagen ya validada.
 * @param onGuardarFoto sube la [fotoPendiente] al backend.
 * @param onDescartarPendiente descarta la selección y vuelve a la foto guardada.
 */
@Composable
fun VisorFotoDialog(
    titulo: String,
    fotoUrl: String?,
    fotoPendiente: ArchivoPendiente?,
    subiendo: Boolean,
    progreso: Int?,
    error: String?,
    onSeleccionarFoto: (ArchivoPendiente) -> Unit,
    onGuardarFoto: () -> Unit,
    onDescartarPendiente: () -> Unit,
    onDismiss: () -> Unit,
    inicial: String? = null,
    iconoVacio: ImageVector = Icons.Default.Person,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorSeleccion by remember { mutableStateOf<String?>(null) }

    val selector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val archivo = leerArchivoSeleccionado(context, uri)
        when {
            !esFotoPermitida(archivo.nombre, archivo.contentType) ->
                errorSeleccion = "${archivo.nombre}: formato no permitido (solo JPG, PNG o WEBP)."

            archivo.tamanoBytes > MAX_FOTO_BYTES ->
                errorSeleccion = "${archivo.nombre}: supera el límite de 10 MB."

            else -> {
                errorSeleccion = null
                onSeleccionarFoto(archivo)
            }
        }
    }

    val modelo: Any? = fotoPendiente?.uri ?: fotoUrl?.takeIf { it.isNotBlank() }
    val hayFotoGuardada = !fotoUrl.isNullOrBlank()
    // El error de subida solo aplica mientras hay una selección pendiente.
    val mensajeError = errorSeleccion ?: error.takeIf { fotoPendiente != null }

    var escala by remember(modelo) { mutableFloatStateOf(1f) }
    var desplazamiento by remember(modelo) { mutableStateOf(Offset.Zero) }

    fun reiniciarZoom() {
        escala = 1f
        desplazamiento = Offset.Zero
    }

    fun abrirSelector() {
        errorSeleccion = null
        selector.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Dialog(
        onDismissRequest = { if (!subiendo) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(VisorFondo)
        ) {
            // Zona de la imagen con zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 176.dp)
                    .clipToBounds()
                    .pointerInput(modelo) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nuevaEscala = (escala * zoom).coerceIn(1f, ESCALA_MAXIMA)
                            escala = nuevaEscala
                            desplazamiento = if (nuevaEscala > 1f) {
                                val maxX = (size.width * (nuevaEscala - 1f)) / 2f
                                val maxY = (size.height * (nuevaEscala - 1f)) / 2f
                                Offset(
                                    x = (desplazamiento.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (desplazamiento.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .pointerInput(modelo) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (escala > 1f) {
                                    reiniciarZoom()
                                } else {
                                    escala = ESCALA_DOBLE_TOQUE
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (modelo != null) {
                    AsyncImage(
                        model = modelo,
                        contentDescription = titulo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = escala
                                scaleY = escala
                                translationX = desplazamiento.x
                                translationY = desplazamiento.y
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!inicial.isNullOrBlank()) {
                            Text(
                                text = inicial,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = iconoVacio,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }

            // Barra superior
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (!subiendo) onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }

            // Barra inferior de acciones
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = VisorBarra
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    mensajeError?.let { mensaje ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(VisorErrorFondo)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mensaje,
                                fontSize = 12.sp,
                                color = VisorErrorTexto
                            )
                        }
                    }

                    if (subiendo || progreso != null) {
                        val porcentaje = (progreso ?: 0).coerceIn(0, 100)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { porcentaje / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = PrimaryOrange,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$porcentaje%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Subiendo foto… no cierres la pantalla.",
                            fontSize = 11.sp,
                            color = VisorTextoSuave
                        )
                    }

                    if (fotoPendiente != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDescartarPendiente,
                                enabled = !subiendo,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                                )
                            ) {
                                Text("Cancelar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onGuardarFoto,
                                enabled = !subiendo,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryOrange,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Guardar foto",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { abrirSelector() },
                            enabled = !subiendo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hayFotoGuardada) "Cambiar foto" else "Agregar foto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

//                    Text(
//                        text = "Pellizca o haz doble toque para acercar",
//                        fontSize = 11.sp,
//                        color = VisorTextoSuave,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.fillMaxWidth()
//                    )
                }
            }
        }
    }
}
