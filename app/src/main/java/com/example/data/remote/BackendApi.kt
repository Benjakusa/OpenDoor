package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class ExtractRequest(val url: String)

data class FormatInfo(
    val quality: String,
    val ext: String,
    val fileSize: Long,
    val downloadUrl: String,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

data class ExtractResponse(
    val title: String,
    val author: String,
    val duration: Long,
    val thumbnailUrl: String,
    val platform: String,
    val uploadDate: String,
    val formats: List<FormatInfo>,
    val videoFormats: List<FormatInfo>,
    val audioFormats: List<FormatInfo>
)

data class HealthResponse(val status: String)

interface BackendApi {
    @POST("/api/extract")
    suspend fun extractVideo(@Body request: ExtractRequest): ExtractResponse

    @GET("/api/health")
    suspend fun health(): HealthResponse
}
