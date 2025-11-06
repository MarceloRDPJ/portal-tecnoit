package com.example.glpimobile.network

import com.example.glpimobile.model.Ticket
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GlpiApiService {

    @POST("token")
    suspend fun getAccessToken(@Body tokenRequest: TokenRequest): Response<TokenResponse>

    @GET("Ticket")
    suspend fun getTickets(): Response<List<Ticket>>
}

data class TokenRequest(
    val grant_type: String = "authorization_code",
    val client_id: String,
    val client_secret: String, // Note: In a real app, the client_secret should be handled more securely.
    val code: String
)

data class TokenResponse(
    val token_type: String,
    val expires_in: Int,
    val access_token: String,
    val refresh_token: String
)
