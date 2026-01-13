package com.example.glpimobile.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OfflineActionDao {
    @Insert
    suspend fun insert(action: OfflineAction)

    @Query("SELECT * FROM offline_actions WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingActions(): List<OfflineAction>

    @Query("UPDATE offline_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
}
