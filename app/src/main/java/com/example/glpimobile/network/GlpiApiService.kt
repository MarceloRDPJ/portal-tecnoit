package com.example.glpimobile.network

import com.example.glpimobile.model.SessionData
import com.example.glpimobile.model.Ticket
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface GlpiApiService {

    @GET("initSession")
    suspend fun initSession(
        @Header("Authorization") authorization: String,
        @Header("App-Token") appToken: String,
        @Query("get_full_session") getFullSession: Boolean = true
    ): Response<SessionResponse>

    @GET("Ticket")
    suspend fun getTickets(): Response<List<Ticket>>

    @POST("Ticket")
    suspend fun createTicket(
        @Body ticket: JsonObject
    ): Response<JsonObject>
}

data class SessionResponse(
    @SerializedName("session_token")
    val sessionToken: String,

    @SerializedName("session")
    val session: SessionData?
)
