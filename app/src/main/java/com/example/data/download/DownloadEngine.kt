package com.example.data.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speed: String,
    val eta: String
) {
    val fraction: Float
        get() = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

class DownloadEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    fun download(
        url: String,
        destination: File,
        tempFile: File,
        onProgress: (DownloadProgress) -> Unit,
        scope: CoroutineScope
    ): Job {
        return scope.launch(Dispatchers.IO) {
            val startBytes = if (tempFile.exists()) tempFile.length() else 0L

            val request = Request.Builder()
                .url(url)
                .apply {
                    if (startBytes > 0) {
                        header("Range", "bytes=$startBytes-")
                    }
                }
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful && response.code != 206) {
                response.close()
                throw Exception("Download failed: HTTP ${response.code}")
            }

            val totalBytes = when {
                startBytes > 0 -> {
                    response.header("Content-Range")
                        ?.substringAfter("/")
                        ?.toLongOrNull()
                        ?: response.body?.contentLength()?.let { startBytes + it }
                        ?: -1L
                }
                else -> response.body?.contentLength() ?: -1L
            }

            val body = response.body ?: throw Exception("Empty response body")

            try {
                FileOutputStream(tempFile, startBytes > 0).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesDownloaded = startBytes
                    val startTime = System.nanoTime()

                    body.byteStream().use { input ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) throw CancellationException("Download cancelled")

                            bytesDownloaded += bytesRead
                            outputStream.write(buffer, 0, bytesRead)

                            val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
                            val downloadedSinceStart = bytesDownloaded - startBytes
                            val speed = if (elapsed > 0) downloadedSinceStart / elapsed else 0.0

                            val remainingBytes = if (totalBytes > 0) totalBytes - bytesDownloaded else -1L
                            val eta = when {
                                remainingBytes > 0 && speed > 0 -> {
                                    val secs = (remainingBytes / speed).toLong()
                                    when {
                                        secs > 3600 -> "${secs / 3600}h ${(secs % 3600) / 60}m"
                                        secs > 60 -> "${secs / 60}m ${secs % 60}s"
                                        else -> "${secs}s"
                                    }
                                }
                                else -> "unknown"
                            }

                            val speedStr = when {
                                speed >= 1_000_000 -> String.format("%.1f MB/s", speed / 1_000_000)
                                speed >= 1_000 -> String.format("%.0f KB/s", speed / 1_000)
                                else -> String.format("%.0f B/s", speed)
                            }

                            onProgress(
                                DownloadProgress(
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    speed = speedStr,
                                    eta = eta
                                )
                            )
                        }
                    }
                }

                // Rename temp to final on completion
                if (totalBytes < 0 || bytesDownloaded >= totalBytes) {
                    tempFile.renameTo(destination)
                }
            } catch (e: CancellationException) {
                body.close()
                throw e
            } catch (e: Exception) {
                body.close()
                throw e
            }
        }
    }
}
