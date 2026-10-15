package com.example.paseape

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.RegistroRequest
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) } // 1: Datos Básicos, 2: Datos por Rol
    var tipoUsuarioId by remember { mutableIntStateOf(1) } // 1: Dueño, 2: Paseador

    // Campos Paso 1
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // Campos Paso 2 - Dueño
    var direccionReferencia by remember { mutableStateOf("") }
    var contactoNombre by remember { mutableStateOf("") }
    var contactoTelefono by remember { mutableStateOf("") }

    // Campos Paso 2 - Paseador
    var numeroDocumento by remember { mutableStateOf("") }
    var experienciaAnos by remember { mutableStateOf("") }
    var tarifaHora by remember { mutableStateOf("") }
    var biografia by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.perritologoapp),
                contentDescription = "Logo",
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (step == 1) "Crea una cuenta" else if (tipoUsuarioId == 1) "Datos de Dueño" else "Perfil de Paseador",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryDark
        )

        Text(
            text = "Paso $step de 2",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (step == 1) {
            OutlinedTextField(
                value = nombres,
                onValueChange = { nombres = it },
                label = { Text("Nombres") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                supportingText = {
                    Text(
                        text = "Mínimo 8 caracteres, mayúscula, número y símbolo (*,#,etc.)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono (+51...)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("¿Cómo deseas registrarte?", fontWeight = FontWeight.Bold, color = PrimaryDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { tipoUsuarioId = 1 }
                ) {
                    RadioButton(selected = tipoUsuarioId == 1, onClick = { tipoUsuarioId = 1 })
                    Text("Dueño")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { tipoUsuarioId = 2 }
                ) {
                    RadioButton(selected = tipoUsuarioId == 2, onClick = { tipoUsuarioId = 2 })
                    Text("Paseador")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (nombres.isBlank() || apellidos.isBlank() || correo.isBlank() || contrasena.isBlank() || telefono.isBlank()) {
                        Toast.makeText(context, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                    } else {
                        step = 2
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Siguiente")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.clickable(onClick = onLoginClick)) {
                Text(text = "¿Ya tienes una cuenta? ", color = Color.Gray, fontSize = 14.sp)
                Text(text = "Inicia sesión", color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        } else {
            // PASO 2 - CAMPOS ESPECÍFICOS SEGÚN EL ROL
            if (tipoUsuarioId == 1) {
                OutlinedTextField(
                    value = direccionReferencia,
                    onValueChange = { direccionReferencia = it },
                    label = { Text("Dirección de Referencia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactoNombre,
                    onValueChange = { contactoNombre = it },
                    label = { Text("Contacto de Emergencia (Nombre)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contactoTelefono,
                    onValueChange = { contactoTelefono = it },
                    label = { Text("Contacto de Emergencia (Teléfono)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = numeroDocumento,
                    onValueChange = { numeroDocumento = it },
                    label = { Text("Número de DNI") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = experienciaAnos,
                    onValueChange = { experienciaAnos = it },
                    label = { Text("Años de experiencia") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tarifaHora,
                    onValueChange = { tarifaHora = it },
                    label = { Text("Tarifa por hora (S/)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = biografia,
                    onValueChange = { biografia = it },
                    label = { Text("Biografía / Presentación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { step = 1 },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Atrás")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val request = if (tipoUsuarioId == 1) {
                                    RegistroRequest(
                                        nombres = nombres.trim(),
                                        apellidos = apellidos.trim(),
                                        correo = correo.trim(),
                                        contrasena = contrasena.trim(),
                                        telefono = telefono.trim(),
                                        tipoUsuarioId = 1,
                                        direccionReferencia = direccionReferencia.ifBlank { "Calle Las Begonias 450, Dpto 302" },
                                        distritoId = 31,
                                        contactoEmergenciaNombre = contactoNombre.ifBlank { "Contacto de Emergencia" },
                                        contactoEmergenciaTelefono = contactoTelefono.ifBlank { "+51987654320" },
                                        notasAdicionales = "Registro vía app Android",
                                        mascotas = emptyList()
                                    )
                                } else {
                                    RegistroRequest(
                                        nombres = nombres.trim(),
                                        apellidos = apellidos.trim(),
                                        correo = correo.trim(),
                                        contrasena = contrasena.trim(),
                                        telefono = telefono.trim(),
                                        tipoUsuarioId = 2,
                                        tipoDocumentoId = 1,
                                        numeroDocumento = numeroDocumento.ifBlank { "45678912" },
                                        antecedentesPolicialesUrl = "https://storage.paseape.com/docs/cert-45678912.pdf",
                                        experienciaAnos = experienciaAnos.toIntOrNull() ?: 1,
                                        biografia = biografia.ifBlank { "Paseador de mascotas capacitado." },
                                        tarifaHoraPen = tarifaHora.toDoubleOrNull() ?: 20.0,
                                        distritoCoberturaId = 21
                                    )
                                }

                                RetrofitClient.apiService.registrarUsuario(request)
                                onRegisterSuccess()
                            } catch (e: HttpException) {
                                val err = e.response()?.errorBody()?.string() ?: "Bad Request"
                                Toast.makeText(context, "Error ${e.code()}: $err", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Registrar")
                    }
                }
            }
        }
    }
}