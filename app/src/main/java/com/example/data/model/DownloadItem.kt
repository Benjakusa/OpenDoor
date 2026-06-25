package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val author: String,
    val duration: Long, // in seconds
    val thumbnailUrl: String,
    val platform: String, // "YouTube", "Facebook", "Twitter", "TikTok", "Instagram", "Vimeo", "Other"
    val fileType: String, // "video" or "audio"
    val quality: String, // "1080p Full HD", "720p HD", "MP3", "M4A", etc.
    val filePath: String,
    val fileSize: Long, // in bytes
    val downloadProgress: Float, // 0.0 to 1.0
    val downloadSpeed: String = "0 KB/s",
    val eta: String = "00:00",
    val status: String, // "Pending", "Downloading", "Paused", "Completed", "Failed"
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
