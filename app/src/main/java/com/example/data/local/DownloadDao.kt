package com.example.data.local

import androidx.room.*
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_items ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM download_items WHERE id = :id")
    suspend fun getDownloadById(id: Int): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem): Long

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun deleteDownloadById(id: Int)

    @Query("UPDATE download_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE download_items SET downloadProgress = :progress, downloadSpeed = :speed, eta = :eta, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: Float, speed: String, eta: String, status: String)
}
