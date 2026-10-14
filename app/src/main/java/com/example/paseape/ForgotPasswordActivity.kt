package com.example.paseape

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paseape.data.model.ForgotPasswordRequest
import com.example.paseape.data.model.ResetPasswordRequest
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                ForgotPasswordScreen(
                    onBackClick = { finish() },
                    onPasswordResetSuccess = {
                        Toast.makeText(this, "¡Contraseña actualizada correctamente!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onPasswordResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isSendingCode by remember { mutableStateOf(false) }
    var isResettingPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundOffWhite)
            )
        },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera descriptiva
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Recupera el acceso a tu cuenta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Te ayudamos a restablecer el acceso a tu cuenta",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            // PASO 1: Ingresa tu correo
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFECE0)
                        ) {
                            Text(
                                text = "PASO 1",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Text(text = "Solicitar código", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryDark)
                    }

                    Text(text = "Correo electrónico", fontSize = 13.sp, color = Color.DarkGray)

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Ingresa cualquier correo", color = Color.LightGray) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Mail, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val trimmedEmail = email.trim()
                            if (trimmedEmail.isBlank()) {
                                Toast.makeText(context, "Ingresa un correo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isSendingCode) return@Button
                            isSendingCode = true

                            coroutineScope.launch {
                                try {
                                    val request = ForgotPasswordRequest(correo = trimmedEmail)
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.forgotPassword(request)
                                    }

                                    isSendingCode = false
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "¡Petición enviada con éxito!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error en el servidor (${response.code()})", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isSendingCode = false
                                    Toast.makeText(context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF27B35)),
                        enabled = !isSendingCode
                    ) {
                        if (isSendingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(text = "Enviar Código", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // PASO 2: Confirma el código
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFECE0)
                        ) {
                            Text(
                                text = "PASO 2",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Text(text = "Confirma el código", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryDark)
                    }

                    Text(text = "Código OTP / Token", fontSize = 13.sp, color = Color.DarkGray)

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        placeholder = { Text("Pega el código o token aquí", color = Color.LightGray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "Revisa tu bandeja de entrada o spam.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // PASO 3: Crea una contraseña nueva
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFECE0)
                        ) {
                            Text(
                                text = "PASO 3",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Text(text = "Crea una contraseña nueva", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryDark)
                    }

                    Text(text = "Nueva Contraseña", fontSize = 13.sp, color = Color.DarkGray)
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = { Text("Mínimo 8 caracteres", color = Color.LightGray) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(text = "Confirmar Nueva Contraseña", fontSize = 13.sp, color = Color.DarkGray)
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Repite la contraseña", color = Color.LightGray) },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val trimmedCode = code.trim()
                            val trimmedNewPassword = newPassword.trim()
                            val trimmedConfirmPassword = confirmPassword.trim()

                            if (trimmedCode.isBlank() || trimmedNewPassword.isBlank() || trimmedConfirmPassword.isBlank()) {
                                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (trimmedNewPassword != trimmedConfirmPassword) {
                                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isResettingPassword) return@Button
                            isResettingPassword = true

                            coroutineScope.launch {
                                try {
                                    val request = ResetPasswordRequest(
                                        token = trimmedCode,
                                        nueva_contrasena = trimmedNewPassword
                                    )
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.apiService.resetPassword(request)
                                    }

                                    isResettingPassword = false
                                    if (response.isSuccessful) {
                                        onPasswordResetSuccess()
                                    } else {
                                        Toast.makeText(context, "Error al restablecer (${response.code()})", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isResettingPassword = false
                                    Toast.makeText(context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF27B35)),
                        enabled = !isResettingPassword
                    ) {
                        if (isResettingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(text = "Restablecer Contraseña", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}