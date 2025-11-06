package com.example.glpimobile.model

import com.google.gson.annotations.SerializedName

data class Ticket(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("date_creation")
    val creationDate: String
)
