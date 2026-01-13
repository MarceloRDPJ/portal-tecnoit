package com.example.glpimobile.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SessionData(
    @SerializedName("glpimy_entities")
    val myEntities: JsonElement?, // Can be JsonArray or JsonObject

    @SerializedName("glpiactive_entity")
    val activeEntityId: String?
)

data class Entity(
    val id: Int,
    val name: String
)
