package com.example.glpimobile.network

import com.example.glpimobile.model.Ticket
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface GlpiApiService {

    @GET("initSession")
    suspend fun initSession(
        @Header("Authorization") authorization: String,
        @Header("App-Token") appToken: String
    ): Response<SessionResponse>

    @GET("Ticket")
    suspend fun getTickets(): Response<List<Ticket>>
}

data class SessionResponse(
    @SerializedName("session_token")
    val sessionToken: String
)
