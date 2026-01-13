package com.example.glpimobile.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_actions")
data class OfflineAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "SOLUTION", "CONSUMABLE", "DOCUMENT"
    val ticketId: Int,
    val payloadJson: String,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)
