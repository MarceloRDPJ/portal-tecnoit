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
        var wellFormedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        // If the URL already contains apirest.php, do not append it again.
        // Some users might enter "http://glpi/apirest.php" or "http://glpi/apirest.php/"
        if (!wellFormedBaseUrl.contains("apirest.php")) {
             wellFormedBaseUrl += "apirest.php/"
        } else {
             // Ensure it ends with / so Retrofit treats it as a directory
             if (!wellFormedBaseUrl.endsWith("/")) {
                 wellFormedBaseUrl += "/"
             }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(wellFormedBaseUrl)
            .client(getHttpClient(context, appToken))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(GlpiApiService::class.java)
    }
}
