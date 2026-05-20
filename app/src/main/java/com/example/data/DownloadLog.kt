package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "download_logs")
data class DownloadLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val platform: String, // "TIKTOK", "INSTAGRAM", "TWITTER", "FACEBOOK", "GENERIC"
    val originalUrl: String,
    val downloadUrl: String,
    val filePath: String,
    val thumbnailUrl: String?,
    val quality: String, // e.g. "HD 1080p", "MP3 Audio"
    val fileSize: Long, // in bytes
    val durationSeconds: Long?,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
