package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class ExtractRequest(val url: String)

@JsonClass(generateAdapter = true)
data class FormatInfo(
    val quality: String,
    val ext: String,
    val fileSize: Long,
    val downloadUrl: String,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

@JsonClass(generateAdapter = true)
data class ExtractResponse(
    val title: String,
    val author: String,
    val duration: Double,
    val thumbnailUrl: String,
    val platform: String,
    val uploadDate: String,
    val formats: List<FormatInfo>,
    val videoFormats: List<FormatInfo>,
    val audioFormats: List<FormatInfo>
)

@JsonClass(generateAdapter = true)
data class HealthResponse(val status: String)

interface BackendApi {
    @POST("/api/extract")
    suspend fun extractVideo(@Body request: ExtractRequest): ExtractResponse

    @GET("/api/health")
    suspend fun health(): HealthResponse
}
