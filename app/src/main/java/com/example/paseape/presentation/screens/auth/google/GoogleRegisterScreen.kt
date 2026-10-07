package com.example.paseape.presentation.screens.auth.google

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.paseape.R
import com.example.paseape.core.util.AppConstants
import com.example.paseape.presentation.components.AppButton
import com.example.paseape.presentation.components.AppDropdownMenu
import com.example.paseape.presentation.components.AppTextField
import com.example.paseape.presentation.theme.BackgroundOffWhite
import com.example.paseape.presentation.theme.CardSurface
import com.example.paseape.presentation.theme.PrimaryDark

@Composable
fun GoogleRegisterScreen(
    onBack: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: GoogleRegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isRegistrationSuccess) {
        if (uiState.isRegistrationSuccess) {
            onRegistrationSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(GoogleRegisterUiEvent.DismissError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundOffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundOffWhite)
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back),
                        tint = PrimaryDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.google_register_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark
                    )
                    Text(
                        text = stringResource(id = R.string.google_register_subtitle),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (uiState.isParametersLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryDark)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Estado de vinculación con Google
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (uiState.idToken.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.google_account_linked),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryDark
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        (context as? Activity)?.let { act ->
                                            viewModel.onEvent(GoogleRegisterUiEvent.RequestGoogleSignIn(act))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(text = stringResource(id = R.string.google_btn_sign_in), fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector de Rol
                    Text(
                        text = stringResource(id = R.string.select_role_prompt),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = uiState.selectedTipoUsuarioId == AppConstants.TIPO_USUARIO_CLIENTE_ID,
                            onClick = { viewModel.onEvent(GoogleRegisterUiEvent.RoleChanged(AppConstants.TIPO_USUARIO_CLIENTE_ID)) },
                            label = { Text(stringResource(id = R.string.role_client)) }
                        )
                        FilterChip(
                            selected = uiState.selectedTipoUsuarioId == AppConstants.TIPO_USUARIO_PASEADOR_ID,
                            onClick = { viewModel.onEvent(GoogleRegisterUiEvent.RoleChanged(AppConstants.TIPO_USUARIO_PASEADOR_ID)) },
                            label = { Text(stringResource(id = R.string.role_walker)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Formulario de CLIENTE
                    if (uiState.selectedTipoUsuarioId == AppConstants.TIPO_USUARIO_CLIENTE_ID) {
                        AppTextField(
                            value = uiState.telefono,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.TelefonoChanged(it)) },
                            label = stringResource(id = R.string.phone_label),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.direccionReferencia,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.DireccionReferenciaChanged(it)) },
                            label = stringResource(id = R.string.address_reference_label),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppDropdownMenu(
                            label = stringResource(id = R.string.district_label),
                            items = uiState.parameters.distritos,
                            selectedId = uiState.distritoId,
                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.DistritoSelected(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.contactoEmergenciaNombre,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.ContactoEmergenciaNombreChanged(it)) },
                            label = stringResource(id = R.string.emergency_contact_name_label),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.contactoEmergenciaTelefono,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.ContactoEmergenciaTelefonoChanged(it)) },
                            label = stringResource(id = R.string.emergency_contact_phone_label),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.notasAdicionales,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.NotasAdicionalesChanged(it)) },
                            label = stringResource(id = R.string.additional_notes_label),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Pregunta de Mascotas
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.pet_question_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = PrimaryDark
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { viewModel.onEvent(GoogleRegisterUiEvent.SetWantsToRegisterPets(true)) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.wantsToRegisterPets) PrimaryDark else CardSurface,
                                            contentColor = if (uiState.wantsToRegisterPets) Color.White else PrimaryDark
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(text = stringResource(id = R.string.pet_question_yes), fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.onEvent(GoogleRegisterUiEvent.SetWantsToRegisterPets(false)) },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.pet_question_skip),
                                            fontSize = 12.sp,
                                            color = PrimaryDark
                                        )
                                    }
                                }
                            }
                        }

                        // Formulario dinámico de Mascotas
                        if (uiState.wantsToRegisterPets) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.pet_section_title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryDark
                                )
                                Button(
                                    onClick = { viewModel.onEvent(GoogleRegisterUiEvent.AddPetClicked) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDark),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(text = stringResource(id = R.string.pet_add_button), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            for ((index, pet) in uiState.mascotas.withIndex()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Mascota #${index + 1}",
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark
                                            )
                                            if (uiState.mascotas.size > 1) {
                                                OutlinedButton(
                                                    onClick = { viewModel.onEvent(GoogleRegisterUiEvent.RemovePetClicked(pet.tempId)) },
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(id = R.string.pet_remove_button),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        AppTextField(
                                            value = pet.nombre,
                                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetNombreChanged(pet.tempId, it)) },
                                            label = stringResource(id = R.string.pet_name_label),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppDropdownMenu(
                                            label = stringResource(id = R.string.pet_type_label),
                                            items = uiState.parameters.tiposMascota,
                                            selectedId = pet.tipoMascotaId,
                                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.PetTipoMascotaSelected(pet.tempId, it)) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppDropdownMenu(
                                            label = stringResource(id = R.string.pet_breed_label),
                                            items = pet.availableRazas,
                                            selectedId = pet.tipoRazaId,
                                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.PetRazaSelected(pet.tempId, it)) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppDropdownMenu(
                                            label = stringResource(id = R.string.pet_gender_label),
                                            items = uiState.parameters.generosMascota,
                                            selectedId = pet.tipoGeneroMascotaId,
                                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.PetGeneroSelected(pet.tempId, it)) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppDropdownMenu(
                                            label = stringResource(id = R.string.pet_size_label),
                                            items = uiState.parameters.tamanosMascota,
                                            selectedId = pet.tipoTamanoMascotaId,
                                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.PetTamanoSelected(pet.tempId, it)) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppDropdownMenu(
                                            label = stringResource(id = R.string.pet_energy_label),
                                            items = uiState.parameters.nivelesEnergia,
                                            selectedId = pet.tipoNivelEnergiaId,
                                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.PetNivelEnergiaSelected(pet.tempId, it)) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AppTextField(
                                                value = pet.edadAnos,
                                                onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetEdadAnosChanged(pet.tempId, it)) },
                                                label = stringResource(id = R.string.pet_age_years_label),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            AppTextField(
                                                value = pet.edadMeses,
                                                onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetEdadMesesChanged(pet.tempId, it)) },
                                                label = stringResource(id = R.string.pet_age_months_label),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            AppTextField(
                                                value = pet.pesoKg,
                                                onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetPesoChanged(pet.tempId, it)) },
                                                label = stringResource(id = R.string.pet_weight_label),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1.2f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.pet_sterilized_label),
                                                fontSize = 13.sp,
                                                color = PrimaryDark
                                            )
                                            Switch(
                                                checked = pet.esterilizado,
                                                onCheckedChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetEsterilizadoChanged(pet.tempId, it)) }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AppTextField(
                                            value = pet.precaucionesMedicas,
                                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.PetPrecaucionesChanged(pet.tempId, it)) },
                                            label = stringResource(id = R.string.pet_medical_notes_label),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                    } else {
                        // Formulario de PASEADOR
                        AppTextField(
                            value = uiState.telefono,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.TelefonoChanged(it)) },
                            label = stringResource(id = R.string.phone_label),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppDropdownMenu(
                            label = stringResource(id = R.string.doc_type_label),
                            items = uiState.parameters.tiposDocumento,
                            selectedId = uiState.tipoDocumentoId,
                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.TipoDocumentoSelected(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.numeroDocumento,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.NumeroDocumentoChanged(it)) },
                            label = stringResource(id = R.string.doc_number_label),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextField(
                                value = uiState.experienciaAnos,
                                onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.ExperienciaAnosChanged(it)) },
                                label = stringResource(id = R.string.experience_years_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AppTextField(
                                value = uiState.tarifaHoraPen,
                                onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.TarifaHoraChanged(it)) },
                                label = stringResource(id = R.string.hourly_rate_label),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AppDropdownMenu(
                            label = stringResource(id = R.string.coverage_district_label),
                            items = uiState.parameters.distritos,
                            selectedId = uiState.distritoCoberturaId,
                            onItemSelected = { viewModel.onEvent(GoogleRegisterUiEvent.DistritoCoberturaSelected(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.biografia,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.BiografiaChanged(it)) },
                            label = stringResource(id = R.string.biography_label),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppTextField(
                            value = uiState.antecedentesPolicialesUrl,
                            onValueChange = { viewModel.onEvent(GoogleRegisterUiEvent.AntecedentesUrlChanged(it)) },
                            label = stringResource(id = R.string.police_records_url_label),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    AppButton(
                        text = stringResource(id = R.string.btn_complete_registration),
                        onClick = { viewModel.onEvent(GoogleRegisterUiEvent.SubmitRegistration) },
                        isLoading = uiState.isLoading,
                        enabled = uiState.idToken.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}