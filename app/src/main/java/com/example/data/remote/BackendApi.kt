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
    val error: Boolean? = null,
    val message: String? = null,
    val title: String? = null,
    val author: String? = null,
    val duration: Double? = null,
    val thumbnailUrl: String? = null,
    val platform: String? = null,
    val uploadDate: String? = null,
    val formats: List<FormatInfo>? = null,
    val videoFormats: List<FormatInfo>? = null,
    val audioFormats: List<FormatInfo>? = null
)

@JsonClass(generateAdapter = true)
data class HealthResponse(val status: String)

interface BackendApi {
    @POST("/api/extract")
    suspend fun extractVideo(@Body request: ExtractRequest): ExtractResponse

    @GET("/api/health")
    suspend fun health(): HealthResponse
}
