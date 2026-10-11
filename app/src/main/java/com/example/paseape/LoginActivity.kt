package com.example.paseape

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.paseape.data.model.GoogleAuthReq
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.paseape.data.model.LoginRequest
import com.example.paseape.data.network.RetrofitClient
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.CardSurface
import com.example.paseape.ui.theme.PaseaPETheme
import com.example.paseape.ui.theme.PrimaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaseaPETheme {
                LoginContent(
                    onLoginSuccess = { role ->
                        if (role == 2) {
                            // Redirecciona a la pantalla del Paseador trabajador
                            val intent = Intent(this, PaseadoresActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Redirecciona al Dashboard principal del Cliente
                            val intent = Intent(this, DashboardActivity::class.java).apply {
                                putExtra("USER_ROLE", role)
                            }

                            startActivity(intent)
                        }
                        finish()
                    },
                    onNavigateToRegister = {
                        val intent = Intent(this, RegisterActivity::class.java)
                        startActivity(intent)
                    }

                )
            }
        }
    }
}

@Composable
fun LoginContent(
    onLoginSuccess: (Int) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    fun launchGoogleLoginFlow(selectedRole: Int) {
        if (isLoading) return
        isLoading = true

        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = "1076479424045-i2m2bjuakkfc3233ssd20epbctf03b81.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result: GetCredentialResponse = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    Log.d("PaseaPE_Auth", "idToken obtenido exitosamente: $idToken")

                    val googleRequest = GoogleAuthReq(
                        idToken = idToken
                    )

                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.loginWithGoogle(googleRequest)
                    }

                    isLoading = false
                    Log.d("PaseaPE_Auth", "Respuesta exitosa Google Auth: $response")
                    Toast.makeText(context, "¡Ingreso con Google exitoso!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(selectedRole)

                } else {
                    isLoading = false
                    Toast.makeText(context, "Tipo de credencial no soportado", Toast.LENGTH_SHORT).show()
                }

            } catch (e: GetCredentialCancellationException) {
                isLoading = false
                Log.d("PaseaPE_Auth", "Selección de cuenta cancelada por el usuario")
            } catch (e: GetCredentialException) {
                isLoading = false
                Log.e("PaseaPE_Auth", "Error en CredentialManager: ${e.type} ${e.message}", e)
                Toast.makeText(context, "Error Credenciales Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isLoading = false
                Log.e("PaseaPE_Auth", "Error durante el flujo de inicio de sesión", e)
                Toast.makeText(context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.perritologoapp),
                    contentDescription = "Logo PaseaPE",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bienvenido de nuevo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
                Text(
                    text = "Ingresa tus datos o continúa con Google",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { showForgotPasswordDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontSize = 12.sp,
                        color = PrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showForgotPasswordDialog) {
                    ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false })
                }

                Spacer(modifier = Modifier.height(16.dp))

                /*
                // CÓDIGO DE PRUEBA DIRECTO (COMENTADO PARA NO BORRARLO):
                Button(
                    onClick = {
                        Toast.makeText(context, "Ingreso directo", Toast.LENGTH_SHORT).show()
                        onLoginSuccess(1) // Pasa rol 1 (Cliente)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                ) {
                    Text(text = "Iniciar Sesión (Directo)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                */

                // BOTÓN INICIAR SESIÓN USANDO TUS MÉTODOS DE RETROFIT REALES
                Button(
                    onClick = {
                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                            Toast.makeText(context, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isLoading) return@Button
                        isLoading = true

                        coroutineScope.launch {
                            try {
                                val loginReq = LoginRequest(
                                    correo = emailInput.trim(),
                                    contrasena = passwordInput.trim()
                                )

                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.iniciarSesion(loginReq)
                                }

                                isLoading = false
                                Log.d("PaseaPE_Auth", "Login local exitoso: $response")
                                Toast.makeText(context, "¡Bienvenido a PaseaPE!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(1)

                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("PaseaPE_Auth", "Error al iniciar sesión local", e)
                                Toast.makeText(context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = "Iniciar Sesión", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "o", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showRoleDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryDark, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Continuar con Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryDark
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(text = "¿No tienes una cuenta?", fontSize = 13.sp, color = Color.Gray)
                TextButton(onClick = { onNavigateToRegister() }) {
                    Text(text = "Regístrate", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryDark)
                }
            }
        }
    }

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(text = "¿Cómo deseas ingresar?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryDark)
            },
            text = {
                Text(text = "Selecciona el tipo de cuenta para tu perfil en PaseaPE:", fontSize = 14.sp, color = Color.DarkGray)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRoleDialog = false
                        launchGoogleLoginFlow(selectedRole = 1)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
                ) {
                    Text("Cliente (Dueño)", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRoleDialog = false
                        launchGoogleLoginFlow(selectedRole = 2)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDark)
                ) {
                    Text("Paseador")
                }
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) } // 1: Enviar Email, 2: Código y Nueva Contraseña
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = if (step == 1) "Recuperar Contraseña" else "Restablecer Contraseña",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PrimaryDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == 1) {
                    Text(
                        text = "Ingresa tu correo registrado para enviarte un código de verificación:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Text(
                        text = "Ingresa el código enviado a $email y tu nueva contraseña:",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código de 6 dígitos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (email.isBlank()) {
                            Toast.makeText(context, "Ingresa tu correo", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // TODO: Llamar al backend para enviar el código por correo (p. ej., /auth/forgot-password)
                        Toast.makeText(context, "Código enviado a tu correo", Toast.LENGTH_SHORT).show()
                        step = 2
                    } else {
                        if (code.isBlank() || newPassword.isBlank()) {
                            Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        // TODO: Llamar al backend para validar código y cambiar clave (p. ej., /auth/reset-password)
                        Toast.makeText(context, "¡Contraseña actualizada correctamente!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark)
            ) {
                Text(if (step == 1) "Enviar Código" else "Guardar Contraseña", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}