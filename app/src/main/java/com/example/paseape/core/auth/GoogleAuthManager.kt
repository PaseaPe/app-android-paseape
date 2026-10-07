package com.example.paseape.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.paseape.core.util.AppConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(activityContext: Context): Result<String> {
        return runCatching {
            val serverClientId = AppConfig.googleWebClientId
            if (serverClientId.isBlank()) {
                throw IllegalStateException("GOOGLE_WEB_CLIENT_ID no está configurado en local.properties")
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                googleIdTokenCredential.idToken
            } else {
                throw Exception("Tipo de credencial no soportado: ${credential.type}")
            }
        }.recoverCatching { exception ->
            if (exception is GetCredentialCancellationException) {
                throw Exception("Inicio de sesión cancelado")
            } else if (exception is GetCredentialException) {
                throw Exception("Error de Google Credential Manager: ${exception.message}")
            }
            throw exception
        }
    }
}