package com.example.paseape.data.local.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("key_auth_token")
        private val KEY_USER_EMAIL = stringPreferencesKey("key_user_email")
        private val KEY_USER_NAME = stringPreferencesKey("key_user_name")
        private val KEY_USER_ROLE = stringPreferencesKey("key_user_role")
        private val KEY_USER_ID = intPreferencesKey("key_user_id")
        private val KEY_USER_TYPE_ID = intPreferencesKey("key_user_type_id")
    }

    val authToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_AUTH_TOKEN]
    }

    val userEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val userName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    val userRole: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]
    }

    val userId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }

    val userTypeId: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[KEY_USER_TYPE_ID]
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun saveUserSession(
        token: String,
        id: Int? = null,
        nombre: String? = null,
        email: String? = null,
        rol: String? = null,
        tipoUsuarioId: Int? = null
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            id?.let { preferences[KEY_USER_ID] = it }
            nombre?.let { preferences[KEY_USER_NAME] = it }
            email?.let { preferences[KEY_USER_EMAIL] = it }
            rol?.let { preferences[KEY_USER_ROLE] = it }
            tipoUsuarioId?.let { preferences[KEY_USER_TYPE_ID] = it }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
