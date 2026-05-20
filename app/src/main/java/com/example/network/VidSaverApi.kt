package com.example.network

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

data class VideoLinkResponse(
    val quality: String, // e.g. "1080p", "720p", "audio"
    val url: String,
    val size: Long?,
    val ext: String? // "mp4", "mp3"
)

data class VideoResolutionResponse(
    val title: String?,
    val thumbnail: String?,
    val author: String?,
    val duration: Long?, // seconds
    val links: List<VideoLinkResponse>?
)

interface VidSaverApi {
    @GET("resolve")
    suspend fun resolveVideo(
        @Query("url") url: String
    ): VideoResolutionResponse

    // Allow user to supply a fully custom absolute URL at runtime
    @GET
    suspend fun resolveVideoByAbsoluteUrl(
        @Url absoluteUrl: String
    ): VideoResolutionResponse
}
