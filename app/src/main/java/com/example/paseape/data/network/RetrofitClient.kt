package com.example.paseape.data.network

import android.util.Log
import com.example.paseape.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d("RETROFIT_DEBUG", "--> ${request.method} ${request.url}")
                val response = chain.proceed(request)
                Log.d("RETROFIT_DEBUG", "<-- HTTP ${response.code} ${response.message} [${request.url}]")
                response
            }
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}