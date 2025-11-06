package com.example.glpimobile.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private fun getHttpClient(context: Context, appToken: String): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY) // Use Level.NONE for release builds
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor(context, appToken))
            .build()
    }

    fun getApiService(context: Context, baseUrl: String, appToken: String): GlpiApiService {
        // Ensure the base URL ends with a slash
        val wellFormedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(wellFormedBaseUrl + "apirest.php/") // The endpoint for GLPI 10 is typically apirest.php
            .client(getHttpClient(context, appToken))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(GlpiApiService::class.java)
    }
}
