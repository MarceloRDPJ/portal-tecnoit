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
import retrofit2.http.Path
import retrofit2.http.PUT
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

    @POST("changeActiveEntities")
    suspend fun changeActiveEntity(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body body: JsonObject
    ): Response<Void>

    // ── Tickets ──────────────────────────────────────────────────────────────
    @GET("Ticket")
    suspend fun getTickets(): Response<List<Ticket>>

    @PUT("Ticket/{id}")
    suspend fun updateTicket(
        @Path("id") id: Int,
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body ticket: JsonObject
    ): Response<JsonObject>

    @POST("Ticket")
    suspend fun createTicket(@Body ticket: JsonObject): Response<JsonObject>

    @POST("ITILSolution")
    suspend fun addSolution(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body solution: JsonObject
    ): Response<JsonObject>

    // ── Consumables ───────────────────────────────────────────────────────────
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

    @POST("Item_Ticket")
    suspend fun linkItemToTicket(
        @Header("Session-Token") sessionToken: String,
        @Header("App-Token") appToken: String,
        @Body payload: JsonObject
    ): Response<JsonObject>

    // ── Inventário — Computadores ─────────────────────────────────────────────
    @GET("Computer")
    suspend fun getComputers(
        @Query("range") range: String = "0-100",
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<List<JsonObject>>

    @POST("Computer")
    suspend fun createComputer(@Body payload: JsonObject): Response<JsonObject>

    // ── Inventário — Equipamentos de Rede ─────────────────────────────────────
    @GET("NetworkEquipment")
    suspend fun getNetworkEquipment(
        @Query("range") range: String = "0-100",
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<List<JsonObject>>

    @POST("NetworkEquipment")
    suspend fun createNetworkEquipment(@Body payload: JsonObject): Response<JsonObject>

    // ── Inventário — Periféricos ──────────────────────────────────────────────
    @GET("Peripheral")
    suspend fun getPeripherals(
        @Query("range") range: String = "0-100",
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<List<JsonObject>>

    @POST("Peripheral")
    suspend fun createPeripheral(@Body payload: JsonObject): Response<JsonObject>

    // ── Inventário — Impressoras ──────────────────────────────────────────────
    @GET("Printer")
    suspend fun getPrinters(
        @Query("range") range: String = "0-100",
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<List<JsonObject>>

    @POST("Printer")
    suspend fun createPrinter(@Body payload: JsonObject): Response<JsonObject>

    // ── Documentos ────────────────────────────────────────────────────────────
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
