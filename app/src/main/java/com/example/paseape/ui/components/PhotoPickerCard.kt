package com.example.paseape.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paseape.data.model.ArchivoPendiente
import com.example.paseape.data.model.MAX_FOTO_BYTES
import com.example.paseape.data.model.esFotoPermitida
import com.example.paseape.data.model.formatearTamanoArchivo
import com.example.paseape.ui.theme.PrimaryDark

private val ErrorRed = Color(0xFFB3261E)
private val HintGray = Color(0xFF64748B)
private val PreviewBackground = Color(0xFFF1F5F9)

/**
 * Selector de foto con vista previa, pensado para perfil de cliente, perfil de
 * paseador y ficha de mascota. Valida el formato (JPG/PNG/WEBP) y el tamaño
 * máximo declarado por el backend (10 MB) antes de aceptar la imagen.
 *
 * @param fotoActualUrl foto ya guardada en el backend (URL prefirmada).
 * @param fotoPendiente foto elegida y todavía no subida.
 * @param onFotoSeleccionada se llama con la imagen ya validada.
 */
@Composable
fun PhotoPickerCard(
    fotoActualUrl: String?,
    fotoPendiente: ArchivoPendiente?,
    onFotoSeleccionada: (ArchivoPendiente) -> Unit,
    onQuitarFoto: () -> Unit,
    modifier: Modifier = Modifier,
    subiendo: Boolean = false,
    progreso: Int? = null,
    habilitado: Boolean = true,
    error: String? = null,
    titulo: String = "Foto de perfil",
    descripcion: String = "Elige una imagen JPG, PNG o WEBP. Máximo 10 MB.",
    forma: Shape = CircleShape,
    tamanoPreview: Dp = 88.dp,
    iconoVacio: ImageVector = Icons.Default.Person,
    textoBoton: String = "Elegir foto"
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
                onFotoSeleccionada(archivo)
            }
        }
    }

    val mensajeError = errorSeleccion ?: error
    val modelo: Any? = fotoPendiente?.uri ?: fotoActualUrl?.takeIf { it.isNotBlank() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(tamanoPreview)
                        .clip(forma)
                        .background(PreviewBackground)
                        .border(1.dp, PrimaryDark.copy(alpha = 0.15f), forma),
                    contentAlignment = Alignment.Center
                ) {
                    if (modelo != null) {
                        AsyncImage(
                            model = modelo,
                            contentDescription = titulo,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = iconoVacio,
                            contentDescription = null,
                            tint = PrimaryDark.copy(alpha = 0.55f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            errorSeleccion = null
                            selector.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        enabled = habilitado && !subiendo,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (modelo == null) textoBoton else "Cambiar foto",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (fotoPendiente != null) {
                        Text(
                            text = "${fotoPendiente.nombre} · ${formatearTamanoArchivo(fotoPendiente.tamanoBytes)}",
                            fontSize = 11.sp,
                            color = HintGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = onQuitarFoto,
                            enabled = habilitado && !subiendo,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Quitar selección",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryDark
                            )
                        }
                    } else {
                        Text(
                            text = descripcion,
                            fontSize = 11.sp,
                            color = HintGray
                        )
                    }
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
                        color = PrimaryDark,
                        trackColor = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$porcentaje%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                }
                Text(
                    text = "Subiendo foto… no cierres la pantalla.",
                    fontSize = 11.sp,
                    color = HintGray
                )
            }

            mensajeError?.let { mensaje ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = mensaje,
                        fontSize = 12.sp,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}
