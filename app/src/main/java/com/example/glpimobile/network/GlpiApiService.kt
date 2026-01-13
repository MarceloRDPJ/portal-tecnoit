package com.example.glpimobile.network

import com.example.glpimobile.model.SessionData
import com.example.glpimobile.model.Ticket
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface GlpiApiService {

    @GET("initSession")
    suspend fun initSession(
        @Header("Authorization") authorization: String?,
        @Header("App-Token") appToken: String,
        @Query("get_full_session") getFullSession: Boolean = true,
        @Query("login") login: String? = null,
        @Query("password") password: String? = null
    ): Response<SessionResponse>

    @GET("Ticket")
    suspend fun getTickets(): Response<List<Ticket>>

    @POST("Ticket")
    suspend fun createTicket(
        @Body ticket: JsonObject
    ): Response<JsonObject>

    @POST("ITILSolution")
    suspend fun addSolution(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body solution: JsonObject
    ): Response<JsonObject>

    @GET("ConsumableItem")
    suspend fun getConsumables(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Query("range") range: String = "0-50"
    ): Response<List<JsonObject>>

    @POST("ConsumableItem")
    suspend fun createConsumable(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body payload: JsonObject
    ): Response<JsonObject>

    // Linking Consumable to Ticket usually involves Item_Ticket or similar,
    // but for "Consumables" tab in GLPI, it might be separate.
    // Assuming we use standard GLPI API to link item.
    // For now I will add a generic createItem endpoint that can be used for Ticket_Item or Consumable.
    @POST("Item_Ticket")
    suspend fun linkItemToTicket(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body payload: JsonObject
    ): Response<JsonObject>

    @Multipart
    @POST("Document")
    suspend fun uploadDocument(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Part("uploadManifest") manifest: String,
        @Part file: MultipartBody.Part
    ): Response<JsonObject>
}

data class SessionResponse(
    @SerializedName("session_token")
    val sessionToken: String,

    @SerializedName("session")
    val session: SessionData?
)
