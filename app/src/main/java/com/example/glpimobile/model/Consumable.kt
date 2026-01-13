package com.example.glpimobile.model

import com.google.gson.annotations.SerializedName

data class Consumable(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("ref")
    val reference: String?,

    @SerializedName("entities_id")
    val entityId: Int
)
