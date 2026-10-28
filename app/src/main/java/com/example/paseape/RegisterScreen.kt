package com.example.paseape

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.paseape.data.network.ApiErrorContext
import com.example.paseape.data.network.ApiErrorMapper
import com.example.paseape.ui.components.CatalogDropdownField
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.PrimaryDark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private val ErrorRed = Color(0xFFB3261E)

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: (Int?, String?) -> Unit,
    onLoginClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var step by remember { mutableIntStateOf(1) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var googleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registroExitoso) {
        if (uiState.registroExitoso) {
            onRegisterSuccess(uiState.authRole, uiState.authUuid)
        }
    }

    // Si el token de Google expiró, el backend lo rechaza y volvemos al paso 1.
    LaunchedEffect(uiState.volverAPaso1) {
        if (uiState.volverAPaso1) {
            step = 1
            viewModel.onVolverAPaso1Aplicado()
        }
    }

    /**
     * Flujo de Google para el registro: obtiene el id_token, comprueba si ese
     * correo ya tiene cuenta y, si es nuevo, prellena el formulario. La pantalla
     * se queda en el paso 1 para que el usuario complete el teléfono.
     */
    fun launchGoogleRegisterFlow() {
        if (googleLoading || uiState.isSubmitting) return
        googleLoading = true

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

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    when (val chequeo = viewModel.chequearCuentaGoogle(
                        googleIdTokenCredential.idToken
                    )) {
                        ChequeoCuentaGoogle.NoRegistrada -> viewModel.onGoogleCredential(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id,
                            nombreCompleto = googleIdTokenCredential.displayName
                        )

                        ChequeoCuentaGoogle.Registrada -> viewModel.mostrarErrorGoogle(
                            "Ya existe una cuenta con ese correo. Inicia Sesión"
                        )

                        is ChequeoCuentaGoogle.Error ->
                            viewModel.mostrarErrorGoogle(chequeo.mensaje)
                    }
                } else {
                    viewModel.mostrarErrorGoogle(
                        "No pudimos obtener las credenciales de Google. Inténtalo de nuevo."
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                // El usuario cerró el selector: no mostramos error.
            } catch (e: GetCredentialException) {
                Log.e("GOOGLE_REGISTER", "Error CredentialManager: ${e.type} - ${e.message}", e)
                viewModel.mostrarErrorGoogle(
                    "No se pudo conectar con Google. Verifica tu cuenta o configuración OAuth."
                )
            } catch (e: Exception) {
                Log.e("GOOGLE_REGISTER", "Error al obtener credenciales de Google", e)
                viewModel.mostrarErrorGoogle(
                    ApiErrorMapper.fromThrowable(e, ApiErrorContext.REGISTER)
                )
            } finally {
                googleLoading = false
            }
        }
    }

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
            text = if (step == 1) {
                "Crea una cuenta"
            } else if (uiState.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
                "Datos de Dueño"
            } else {
                "Perfil de Paseador"
            },
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
            Paso1(
                uiState = uiState,
                viewModel = viewModel,
                onLoginClick = onLoginClick,
                googleLoading = googleLoading,
                onGoogleClick = { launchGoogleRegisterFlow() },
                onSiguiente = {
                    if (viewModel.validarPaso1()) {
                        step = 2
                    }
                }
            )
        } else {
            Paso2(
                uiState = uiState,
                viewModel = viewModel,
                onAtras = { step = 1 }
            )
        }
    }
}

@Composable
private fun Paso1(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    onLoginClick: () -> Unit,
    googleLoading: Boolean,
    onGoogleClick: () -> Unit,
    onSiguiente: () -> Unit
) {
    val habilitado = !uiState.isSubmitting

    if (uiState.modoGoogle) {
        Surface(
            color = Color(0xFFE8F5E9),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "✓ Registrando con Google",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = uiState.googleEmail,
                    fontSize = 13.sp,
                    color = PrimaryDark
                )
                Text(
                    text = "Tu cuenta se creará sin contraseña.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    RegisterTextField(
        value = uiState.nombres,
        onValueChange = { viewModel.onFieldChange(RegisterField.NOMBRES, it) },
        label = "Nombres *",
        placeholder = "Ej. Ana María",
        error = uiState.fieldErrors[RegisterField.NOMBRES],
        enabled = habilitado
    )

    Spacer(modifier = Modifier.height(8.dp))

    RegisterTextField(
        value = uiState.apellidos,
        onValueChange = { viewModel.onFieldChange(RegisterField.APELLIDOS, it) },
        label = "Apellidos *",
        placeholder = "Ej. Pérez Gómez",
        error = uiState.fieldErrors[RegisterField.APELLIDOS],
        enabled = habilitado
    )

    if (!uiState.modoGoogle) {
        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.correo,
            onValueChange = { viewModel.onFieldChange(RegisterField.CORREO, it) },
            label = "Correo electrónico *",
            placeholder = "tucorreo@ejemplo.com",
            error = uiState.fieldErrors[RegisterField.CORREO],
            enabled = habilitado,
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.contrasena,
            onValueChange = { viewModel.onFieldChange(RegisterField.CONTRASENA, it) },
            label = "Contraseña *",
            error = uiState.fieldErrors[RegisterField.CONTRASENA],
            supportingText = "Entre 6 y 50 caracteres.",
            enabled = habilitado,
            keyboardType = KeyboardType.Password,
            password = true
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    RegisterTextField(
        value = uiState.telefono,
        onValueChange = { viewModel.onFieldChange(RegisterField.TELEFONO, it) },
        label = "Teléfono *",
        placeholder = "987 654 321",
        error = uiState.fieldErrors[RegisterField.TELEFONO],
        enabled = habilitado,
        keyboardType = KeyboardType.Phone
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
            modifier = Modifier.clickable(enabled = habilitado) {
                viewModel.onTipoUsuarioChange(TIPO_USUARIO_CLIENTE)
            }
        ) {
            RadioButton(
                selected = uiState.tipoUsuarioId == TIPO_USUARIO_CLIENTE,
                onClick = { viewModel.onTipoUsuarioChange(TIPO_USUARIO_CLIENTE) },
                enabled = habilitado
            )
            Text("Dueño")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = habilitado) {
                viewModel.onTipoUsuarioChange(TIPO_USUARIO_PASEADOR)
            }
        ) {
            RadioButton(
                selected = uiState.tipoUsuarioId == TIPO_USUARIO_PASEADOR,
                onClick = { viewModel.onTipoUsuarioChange(TIPO_USUARIO_PASEADOR) },
                enabled = habilitado
            )
            Text("Paseador")
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onSiguiente,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
        shape = RoundedCornerShape(24.dp),
        enabled = habilitado
    ) {
        Text("Siguiente")
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (uiState.modoGoogle) {
        TextButton(
            onClick = { viewModel.salirDeModoGoogle() },
            enabled = habilitado
        ) {
            Text(
                text = "Usar correo y contraseña",
                color = PrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        OutlinedButton(
            onClick = onGoogleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = PrimaryDark
            ),
            enabled = habilitado && !googleLoading
        ) {
            if (googleLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = PrimaryDark,
                    strokeWidth = 2.dp
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEA4335).copy(alpha = 0.12f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA4335)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continuar con Google",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark
                )
            }
        }
    }

    uiState.errorGeneral?.let { mensaje ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = mensaje,
            color = ErrorRed,
            fontSize = 13.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(modifier = Modifier.clickable(onClick = onLoginClick)) {
        Text(text = "¿Ya tienes una cuenta? ", color = Color.Gray, fontSize = 14.sp)
        Text(
            text = "Inicia sesión",
            color = PrimaryDark,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Paso2(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    onAtras: () -> Unit
) {
    val habilitado = !uiState.isSubmitting
    val catalogsReady = uiState.distritos.isNotEmpty()
    val catalogosListos = if (uiState.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
        catalogsReady
    } else {
        catalogsReady && uiState.tiposDocumento.isNotEmpty()
    }

    if (uiState.isLoadingCatalogos) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = PrimaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.catalogosError?.let { mensaje ->
        Text(
            text = mensaje,
            color = ErrorRed,
            fontSize = 12.sp
        )
        TextButton(
            onClick = { viewModel.cargarCatalogos() },
            enabled = !uiState.isLoadingCatalogos
        ) {
            Text("Reintentar")
        }
    }

    if (uiState.tipoUsuarioId == TIPO_USUARIO_CLIENTE) {
        val placeholderDistrito = when {
            uiState.isLoadingCatalogos -> "Cargando distritos..."
            uiState.distritos.isEmpty() -> "Distritos no disponibles"
            else -> "Selecciona tu distrito"
        }

        CatalogDropdownField(
            label = "Distrito *",
            items = uiState.distritos,
            selectedItem = uiState.distritoSeleccionado,
            itemLabel = { it.descripcion },
            placeholder = placeholderDistrito,
            enabled = habilitado && uiState.distritos.isNotEmpty(),
            onItemSelected = { viewModel.onDistritoSelected(it) },
            errorText = uiState.fieldErrors[RegisterField.DISTRITO]
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.direccionReferencia,
            onValueChange = { viewModel.onFieldChange(RegisterField.DIRECCION_REFERENCIA, it) },
            label = "Dirección de referencia",
            placeholder = "Ej. Av. Los Álamos 123, Dpto. 302",
            enabled = habilitado
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.contactoEmergenciaNombre,
            onValueChange = { viewModel.onFieldChange(RegisterField.CONTACTO_NOMBRE, it) },
            label = "Contacto de emergencia (nombre)",
            placeholder = "Ej. María Gómez",
            enabled = habilitado
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.contactoEmergenciaTelefono,
            onValueChange = { viewModel.onFieldChange(RegisterField.CONTACTO_TELEFONO, it) },
            label = "Contacto de emergencia (teléfono)",
            placeholder = "+51 987 654 321",
            enabled = habilitado,
            keyboardType = KeyboardType.Phone
        )
    } else {
        val tipoDocumentoId = uiState.tipoDocumentoSeleccionado?.id

        val placeholderTipoDocumento = when {
            uiState.isLoadingCatalogos -> "Cargando tipos..."
            uiState.tiposDocumento.isEmpty() -> "Tipos no disponibles"
            else -> "Selecciona el tipo de documento"
        }

        CatalogDropdownField(
            label = "Tipo de documento *",
            items = uiState.tiposDocumento,
            selectedItem = uiState.tipoDocumentoSeleccionado,
            itemLabel = { it.descripcion },
            placeholder = placeholderTipoDocumento,
            enabled = habilitado && uiState.tiposDocumento.isNotEmpty(),
            onItemSelected = { viewModel.onTipoDocumentoSelected(it) },
            errorText = uiState.fieldErrors[RegisterField.TIPO_DOCUMENTO]
        )

        Spacer(modifier = Modifier.height(8.dp))

        val labelDocumento = when (tipoDocumentoId) {
            1 -> "Número de DNI *"
            2 -> "Número de carnet de extranjería *"
            3 -> "Número de pasaporte *"
            else -> "Número de documento *"
        }
        val placeholderDocumento = when (tipoDocumentoId) {
            1 -> "8 dígitos"
            2 -> "8 o 9 dígitos"
            3 -> "9 caracteres alfanuméricos"
            else -> "Selecciona primero el tipo de documento"
        }

        RegisterTextField(
            value = uiState.numeroDocumento,
            onValueChange = { viewModel.onNumeroDocumentoChange(it) },
            label = labelDocumento,
            placeholder = placeholderDocumento,
            error = uiState.fieldErrors[RegisterField.NUMERO_DOCUMENTO],
            enabled = habilitado && tipoDocumentoId != null,
            keyboardType = if (tipoDocumentoId == 3) KeyboardType.Text else KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(8.dp))

        val placeholderCobertura = when {
            uiState.isLoadingCatalogos -> "Cargando distritos..."
            !catalogsReady -> "Distritos no disponibles"
            else -> "Selecciona el distrito de cobertura"
        }

        CatalogDropdownField(
            label = "Distrito de cobertura *",
            items = uiState.distritos,
            selectedItem = uiState.distritoCoberturaSeleccionado,
            itemLabel = { it.descripcion },
            placeholder = placeholderCobertura,
            enabled = habilitado && catalogsReady,
            onItemSelected = { viewModel.onDistritoCoberturaSelected(it) },
            errorText = uiState.fieldErrors[RegisterField.DISTRITO_COBERTURA]
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.experienciaAnos,
            onValueChange = { viewModel.onFieldChange(RegisterField.EXPERIENCIA, it) },
            label = "Años de experiencia",
            placeholder = "Ej. 2",
            error = uiState.fieldErrors[RegisterField.EXPERIENCIA],
            enabled = habilitado,
            keyboardType = KeyboardType.Number
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.tarifaHora,
            onValueChange = { viewModel.onFieldChange(RegisterField.TARIFA, it) },
            label = "Tarifa por hora (S/)",
            placeholder = "Ej. 20.00",
            error = uiState.fieldErrors[RegisterField.TARIFA],
            enabled = habilitado,
            keyboardType = KeyboardType.Decimal
        )

        Spacer(modifier = Modifier.height(8.dp))

        RegisterTextField(
            value = uiState.biografia,
            onValueChange = { viewModel.onFieldChange(RegisterField.BIOGRAFIA, it) },
            label = "Biografía / presentación",
            placeholder = "Cuéntanos sobre tu experiencia con mascotas",
            enabled = habilitado,
            singleLine = false,
            minLines = 3
        )
    }

    uiState.errorGeneral?.let { mensaje ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = mensaje,
            color = ErrorRed,
            fontSize = 13.sp
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onAtras,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            enabled = !uiState.isSubmitting
        ) {
            Text("Atrás")
        }

        Button(
            onClick = { viewModel.registrar() },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
            shape = RoundedCornerShape(24.dp),
            enabled = !uiState.isSubmitting && !uiState.isLoadingCatalogos && catalogosListos
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text("Registrar")
            }
        }
    }
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, fontSize = 13.sp, color = Color.Gray) }
        } else {
            null
        },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        isError = error != null,
        supportingText = when {
            error != null -> {
                { Text(error, fontSize = 11.sp, color = ErrorRed) }
            }
            supportingText != null -> {
                { Text(supportingText, fontSize = 11.sp, color = Color.Gray) }
            }
            else -> null
        },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}
