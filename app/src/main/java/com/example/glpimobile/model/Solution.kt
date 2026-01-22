package com.example.glpimobile.model

import com.google.gson.annotations.SerializedName

data class Solution(
    @SerializedName("items_id")
    val ticketId: Int,

    @SerializedName("content")
    val content: String,

    @SerializedName("itemtype")
    val itemType: String = "Ticket",

    @SerializedName("solutiontypes_id")
    val solutionTypeId: Int = 1, // Default

    // Extra field not used by ITILSolution API directly but used by SyncWorker logic
    @SerializedName("status")
    val status: Int? = null
)
