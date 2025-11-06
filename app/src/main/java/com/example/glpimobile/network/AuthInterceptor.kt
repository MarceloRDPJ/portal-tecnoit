package com.example.glpimobile.network

import android.content.Context
import com.example.glpimobile.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context, private val appToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sessionToken = SessionManager.getSessionToken(context)

        // Do not add headers to the initSession request, as it uses different auth.
        if (request.url.encodedPath.endsWith("initSession")) {
            return chain.proceed(request)
        }

        val requestBuilder = request.newBuilder()
            .addHeader("App-Token", appToken)

        if (sessionToken != null) {
            requestBuilder.addHeader("Session-Token", sessionToken)
        }

        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}
