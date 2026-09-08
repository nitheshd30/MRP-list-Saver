package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MrpHistory
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MrpHistoryDao {
    @Query("SELECT * FROM mrp_history WHERE productId = :productId ORDER BY changeDate DESC")
    fun getHistoryForProduct(productId: Long): Flow<List<MrpHistory>>

    @Query("SELECT * FROM mrp_history ORDER BY changeDate DESC")
    fun getAllHistory(): Flow<List<MrpHistory>>

    @Query("SELECT * FROM mrp_history WHERE barcode = :barcode ORDER BY changeDate DESC")
    fun getHistoryForBarcode(barcode: String): Flow<List<MrpHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: MrpHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<MrpHistory>)

    @Query("SELECT * FROM mrp_history WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncHistory(): List<MrpHistory>

    @Query("UPDATE mrp_history SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Query("SELECT COUNT(*) FROM mrp_history")
    suspend fun getHistoryCount(): Int
}
