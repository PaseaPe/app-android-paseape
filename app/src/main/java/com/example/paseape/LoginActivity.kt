package com.example.paseape

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
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
                    onLoginSuccess = { role, uuid ->
                        if (role == 99) {
                            val intent = Intent(this, AdminActivity::class.java).apply {
                                putExtra("USER_ROLE", role)
                                putExtra("EXTRA_USER_UUID", uuid)
                                putExtra("user_uuid", uuid)
                                putExtra("USER_UUID", uuid)
                            }
                            startActivity(intent)
                        } else if (role == 2) {
                            val intent = Intent(this, PaseadoresActivity::class.java).apply {
                                putExtra("USER_ROLE", role)
                                putExtra("EXTRA_USER_UUID", uuid)
                                putExtra("user_uuid", uuid)
                                putExtra("USER_UUID", uuid)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(this, DashboardActivity::class.java).apply {
                                putExtra("USER_ROLE", role)
                                putExtra("EXTRA_USER_UUID", uuid)
                                putExtra("user_uuid", uuid)
                                putExtra("USER_UUID", uuid)
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
    onLoginSuccess: (Int, String?) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }

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

                    val googleRequest = GoogleAuthReq(idToken = idToken)

                    val apiResponse = withContext(Dispatchers.IO) {
                        RetrofitClient.apiService.loginWithGoogle(googleRequest)
                    }

                    val tokenJwt = apiResponse.token ?: ""

                    if (tokenJwt.isNotEmpty()) {
                        val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                        sharedPref.edit()
                            .putString("auth_token", tokenJwt)
                            .putString("JWT_TOKEN", tokenJwt)
                            .putInt("USER_ROLE", selectedRole)
                            .commit()

                        val paseaPrefs = context.getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
                        paseaPrefs.edit()
                            .putString("auth_token", tokenJwt)
                            .putString("JWT_TOKEN", tokenJwt)
                            .putInt("USER_ROLE", selectedRole)
                            .commit()
                    }

                    isLoading = false
                    Toast.makeText(context, "¡Ingreso con Google exitoso!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(selectedRole, null)

                } else {
                    isLoading = false
                    Toast.makeText(context, "Tipo de credencial no soportado", Toast.LENGTH_SHORT).show()
                }

            } catch (e: GetCredentialCancellationException) {
                isLoading = false
            } catch (e: GetCredentialException) {
                isLoading = false
                Toast.makeText(context, "Error Credenciales Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                isLoading = false
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
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description, tint = PrimaryDark)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDark,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        val intent = Intent(context, ForgotPasswordActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontSize = 12.sp,
                        color = PrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val trimmedEmail = emailInput.trim()
                        val trimmedPassword = passwordInput.trim()

                        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
                            Toast.makeText(context, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isLoading) return@Button
                        isLoading = true

                        coroutineScope.launch {
                            try {
                                val loginReq = LoginRequest(
                                    correo = trimmedEmail,
                                    contrasena = trimmedPassword
                                )

                                val apiResponse = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.iniciarSesion(loginReq)
                                }

                                Log.d("LOGIN_TEST", "Objeto response recibido: $apiResponse")
                                val tokenJwt = apiResponse.response?.token ?: ""
                                val userUuid = apiResponse.userUuid ?: apiResponse.response?.uuid ?: apiResponse.uuid ?: ""
                                Log.d("LOGIN_TEST", "Token extraído: [$tokenJwt], UUID: [$userUuid]")

                                if (tokenJwt.isNotEmpty()) {
                                    val userRole = if (trimmedEmail.contains("admin")) 99 else 1

                                    // Guardar en SharedPreferences "paseape_prefs" (user_uuid, auth_token, USER_ROLE)
                                    val paseaPrefs = context.getSharedPreferences("paseape_prefs", Context.MODE_PRIVATE)
                                    paseaPrefs.edit()
                                        .putString("user_uuid", userUuid)
                                        .putString("auth_token", tokenJwt)
                                        .putString("JWT_TOKEN", tokenJwt)
                                        .putInt("USER_ROLE", userRole)
                                        .commit()

                                    // Guardar en PaseaPePrefs para retrocompatibilidad
                                    val sharedPref = context.getSharedPreferences("PaseaPePrefs", Context.MODE_PRIVATE)
                                    sharedPref.edit()
                                        .putString("user_uuid", userUuid)
                                        .putString("auth_token", tokenJwt)
                                        .putString("JWT_TOKEN", tokenJwt)
                                        .putInt("USER_ROLE", userRole)
                                        .commit()

                                    Log.d("LOGIN_TEST", "¡Token (auth_token), UUID (user_uuid) y Rol ($userRole) guardados exitosamente!")

                                    isLoading = false
                                    Toast.makeText(context, "¡Bienvenido a PaseaPE!", Toast.LENGTH_SHORT).show()

                                    onLoginSuccess(userRole, userUuid)
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Credenciales incorrectas o token vacío", Toast.LENGTH_LONG).show()
                                }

                            } catch (e: retrofit2.HttpException) {
                                isLoading = false
                                val errorBody = e.response()?.errorBody()?.string() ?: "Sin detalle"
                                Log.e("LOGIN_ERROR", "HTTP ${e.code()} - Body: $errorBody", e)
                                Toast.makeText(context, "Error servidor (${e.code()}): $errorBody", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("LOGIN_ERROR", "Error de conexión: ${e.localizedMessage}", e)
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
                    Text(
                        text = "Continuar con Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryDark
                    )
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