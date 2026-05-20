package com.example.model

enum class DownloadStatus {
    PENDING,
    RESOLVING,
    DOWNLOADING,
    CONVERTING,
    COMPLETED,
    FAILED,
    PAUSED
}

data class MediaFormat(
    val id: String,
    val qualityLabel: String, // e.g., "HD 1080p (Video)", "SD 720p (Video)", "MP4 (Audio Extracted)", "High Quality MP3 (Audio)"
    val extension: String,    // "mp4", "mp3", "m4a"
    val sizeBytes: Long,
    val downloadUrl: String
)

data class ResolvedVideoInfo(
    val title: String,
    val platform: String, // "TIKTOK", "INSTAGRAM", "TWITTER", "FACEBOOK", "GENERIC"
    val originalUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long?,
    val authorName: String?,
    val formats: List<MediaFormat>
)

data class DownloadQueueItem(
    val id: String, // unique id, e.g., UUID
    val title: String,
    val platform: String,
    val originalUrl: String,
    val format: MediaFormat,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val speedKbps: Double = 0.0,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f

    val progressPercentageStr: String
        get() = "${(progress * 100).toInt()}%"

    val sizeStr: String
        get() {
            val downloadedMb = downloadedBytes.toDouble() / (1024 * 1024)
            val totalMb = totalBytes.toDouble() / (1024 * 1024)
            return String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
        }

    val speedStr: String
        get() {
            return when {
                speedKbps > 1024 -> String.format("%.1f MB/s", speedKbps / 1024)
                else -> String.format("%.0f KB/s", speedKbps)
            }
        }
}
