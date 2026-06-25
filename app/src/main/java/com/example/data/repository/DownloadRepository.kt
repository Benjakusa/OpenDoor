package com.example.data.repository

import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun getDownloadById(id: Int): DownloadItem? {
        return downloadDao.getDownloadById(id)
    }

    suspend fun insert(download: DownloadItem): Long {
        return downloadDao.insertDownload(download)
    }

    suspend fun update(download: DownloadItem) {
        downloadDao.updateDownload(download)
    }

    suspend fun delete(download: DownloadItem) {
        downloadDao.deleteDownload(download)
    }

    suspend fun deleteById(id: Int) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun updateFavorite(id: Int, isFavorite: Boolean) {
        downloadDao.updateFavorite(id, isFavorite)
    }

    suspend fun updateProgress(id: Int, progress: Float, speed: String, eta: String, status: String) {
        downloadDao.updateProgress(id, progress, speed, eta, status)
    }
}
