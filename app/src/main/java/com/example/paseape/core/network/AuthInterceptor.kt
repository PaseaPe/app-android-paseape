package com.example.paseape.core.network

import com.example.paseape.core.util.AppConstants
import com.example.paseape.data.local.session.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Rutas públicas que no requieren token
        val isPublicEndpoint = AppConstants.PUBLIC_ENDPOINTS.any { path.contains(it) }

        val token = if (!isPublicEndpoint) {
            runBlocking {
                sessionManager.authToken.firstOrNull()
            }
        } else {
            null
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
