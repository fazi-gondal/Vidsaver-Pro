package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_logs ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadLog>>

    @Query("SELECT * FROM download_logs WHERE platform = :platform ORDER BY timestamp DESC")
    fun getDownloadsByPlatform(platform: String): Flow<List<DownloadLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(log: DownloadLog): Long

    @Query("DELETE FROM download_logs WHERE id = :id")
    suspend fun deleteDownloadById(id: Int)

    @Query("DELETE FROM download_logs")
    suspend fun clearAll()
}
