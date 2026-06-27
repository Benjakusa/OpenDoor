package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadEngine
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.remote.BackendApi
import com.example.data.remote.ExtractRequest
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

data class ResolutionOption(val quality: String, val fileSizeEstimate: Long)
data class AudioOption(val format: String, val fileSizeEstimate: Long)

data class VideoMetadata(
    val url: String,
    val title: String,
    val author: String,
    val duration: Long,
    val thumbnailUrl: String,
    val platform: String,
    val uploadDate: String,
    val availableResolutions: List<ResolutionOption>,
    val availableAudios: List<AudioOption>
)

sealed interface MetadataState {
    object Idle : MetadataState
    object Fetching : MetadataState
    data class Success(val metadata: VideoMetadata) : MetadataState
    data class Error(val message: String) : MetadataState
}

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val subscription: String,
    val status: String,
    val joinDate: String
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository
    val downloads: StateFlow<List<DownloadItem>>

    private val activeJobs = mutableMapOf<Int, Job>()
    private val downloadEngine = DownloadEngine()
    private val formatUrlMap = mutableMapOf<String, String>()

    var urlInput = MutableStateFlow("")
        private set

    private val _metadataState = MutableStateFlow<MetadataState>(MetadataState.Idle)
    val metadataState: StateFlow<MetadataState> = _metadataState.asStateFlow()

    private val _storageUsedBytes = MutableStateFlow(45 * 1024 * 1024 * 1024L)
    val totalStorageBytes = 128 * 1024 * 1024 * 1024L

    var isDarkTheme = MutableStateFlow(false)
        private set

    private val _adminUsers = MutableStateFlow<List<AdminUser>>(emptyList())
    val adminUsers: StateFlow<List<AdminUser>> = _adminUsers.asStateFlow()

    var totalSimulatedDownloads = MutableStateFlow(0)
        private set

    var adSettingsEnabled = MutableStateFlow(true)
        private set

    var appAnnouncement = MutableStateFlow("Welcome to OpenDoor v1.0! Tap, download, and enjoy videos offline with dynamic speed controls.")
        private set

    private val backendApi: BackendApi?
    private var backendAvailable = true
    private val downloadDir: File

    init {
        val context = application
        val database = AppDatabase.getDatabase(context)
        repository = DownloadRepository(database.downloadDao())

        downloads = repository.allDownloads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        downloadDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "OpenDoor"
        ).also { it.mkdirs() }

        val backendUrl = try {
            val url = com.example.BuildConfig.BACKEND_URL
            if (url.isNotEmpty() && !url.contains("your-backend")) url else null
        } catch (_: Exception) {
            null
        }

        if (backendUrl != null) {
            val client = OkHttpClient.Builder()
                .connectionPool(ConnectionPool(2, 30, TimeUnit.SECONDS))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            backendApi = Retrofit.Builder()
                .baseUrl(if (backendUrl.endsWith("/")) backendUrl else "$backendUrl/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(BackendApi::class.java)

            Log.i("DownloadViewModel", "Backend configured at $backendUrl")
        } else {
            backendApi = null
            backendAvailable = false
            _metadataState.value = MetadataState.Error("BACKEND_URL not configured. Set it in .env and rebuild.")
        }

        _adminUsers.value = listOf(
            AdminUser("U001", "Alex Smith", "alex@gmail.com", "Premium", "Active", "2026-01-10"),
            AdminUser("U002", "Elena Rostova", "elena@yahoo.com", "Free", "Active", "2026-02-14"),
            AdminUser("U003", "John Doe", "john.doe@live.com", "Ad-Free", "Suspended", "2026-03-01"),
            AdminUser("U004", "Sarah Jenkins", "sarah.j@gmail.com", "Premium", "Active", "2026-05-20"),
            AdminUser("U005", "Michael Chang", "mchang@outlook.com", "Free", "Active", "2026-06-15")
        )
    }

    fun updateUrlInput(url: String) {
        urlInput.value = url
        if (url.isEmpty()) {
            _metadataState.value = MetadataState.Idle
        }
    }

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
    }

    fun fetchVideoInfo() {
        val url = urlInput.value.trim()
        if (url.isEmpty()) {
            _metadataState.value = MetadataState.Error("Please paste a video link first.")
            return
        }

        viewModelScope.launch {
            _metadataState.value = MetadataState.Fetching

            if (backendApi == null) {
                _metadataState.value = MetadataState.Error("Backend is not connected. Check your BACKEND_URL configuration.")
                return@launch
            }

            try {
                val response = backendApi.extractVideo(ExtractRequest(url))
                formatUrlMap.clear()

                response.formats.forEach { f ->
                    formatUrlMap[f.quality] = f.downloadUrl
                    formatUrlMap["${f.quality}.${f.ext}"] = f.downloadUrl
                }

                val videos = response.videoFormats
                    .filter { it.hasAudio }
                    .ifEmpty { response.videoFormats }
                    .map { ResolutionOption(it.quality, it.fileSize) }
                    .ifEmpty { listOf(ResolutionOption("Default Quality", 0)) }

                val audios = response.audioFormats.ifEmpty {
                    response.formats.filter { !it.hasVideo && it.hasAudio }
                }.map { AudioOption(it.quality, it.fileSize) }
                    .ifEmpty { listOf(AudioOption("Audio Only", 0)) }

                _metadataState.value = MetadataState.Success(
                    VideoMetadata(
                        url = url,
                        title = response.title,
                        author = response.author,
                        duration = response.duration.toLong(),
                        thumbnailUrl = response.thumbnailUrl,
                        platform = response.platform,
                        uploadDate = response.uploadDate,
                        availableResolutions = videos,
                        availableAudios = audios
                    )
                )
            } catch (e: Throwable) {
                Log.e("DownloadViewModel", "Extraction failed", e)
                _metadataState.value = MetadataState.Error("Failed to extract video: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun startDownload(metadata: VideoMetadata, optionName: String, isAudio: Boolean, sizeEstimate: Long) {
        viewModelScope.launch {
            try {
                val downloadUrl = formatUrlMap[optionName]
                if (downloadUrl == null) {
                    _metadataState.value = MetadataState.Error("Download URL not available. Try fetching video info again.")
                    return@launch
                }

                val ext = detectExtension(downloadUrl, isAudio)
                val fileName = "${sanitizeFileName(metadata.title)}_${optionName.replace(" ", "_")}.$ext"
                val destFile = File(downloadDir, fileName)
                val tempFile = File(downloadDir, "$fileName.tmp")
                withContext(Dispatchers.IO) {
                    var counter = 0
                    val baseName = fileName.substringBeforeLast(".")
                    val extension = fileName.substringAfterLast(".")
                    while (destFile.exists() && counter < 100) {
                        counter++
                        val uniqueName = "${baseName}_($counter).$extension"
                        destFile.renameTo(File(downloadDir, uniqueName))
                    }
                }

                val newItem = DownloadItem(
                    url = metadata.url,
                    title = metadata.title,
                    author = metadata.author,
                    duration = metadata.duration,
                    thumbnailUrl = metadata.thumbnailUrl,
                    platform = metadata.platform,
                    fileType = if (isAudio) "audio" else "video",
                    quality = optionName,
                    filePath = downloadUrl,
                    fileSize = sizeEstimate.coerceAtLeast(1),
                    downloadProgress = 0.0f,
                    downloadSpeed = "0 KB/s",
                    eta = "starting...",
                    status = "Downloading",
                    isFavorite = false,
                    timestamp = System.currentTimeMillis()
                )

                val downloadId = repository.insert(newItem).toInt()

                val job = downloadEngine.download(
                    url = downloadUrl,
                    destination = destFile,
                    tempFile = tempFile,
                    scope = viewModelScope,
                    onProgress = { progress ->
                        viewModelScope.launch {
                            try {
                                repository.updateProgress(
                                    id = downloadId,
                                    progress = progress.fraction,
                                    speed = progress.speed,
                                    eta = progress.eta,
                                    status = if (progress.fraction < 1f) "Downloading" else "Completed"
                                )
                                if (progress.fraction >= 1f) {
                                    repository.getDownloadById(downloadId)?.let { item ->
                                        val mime = if (isAudio) "audio/mp4" else "video/mp4"
                                        val publicUri = saveToPublicStorage(tempFile, fileName, mime)
                                        repository.update(item.copy(filePath = publicUri ?: destFile.absolutePath))
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e("DownloadViewModel", "Progress update failed", e)
                            }
                        }
                    }
                )

                activeJobs[downloadId] = job

                job.invokeOnCompletion { cause ->
                    if (cause is CancellationException) {
                        viewModelScope.launch {
                            try {
                                val item = repository.getDownloadById(downloadId)
                                if (item != null && item.status != "Completed") {
                                    repository.update(item.copy(status = "Paused", downloadSpeed = "0 KB/s", eta = "Paused"))
                                }
                            } catch (e: Throwable) {
                                Log.e("DownloadViewModel", "Pause handler failed", e)
                            }
                        }
                    } else if (cause != null) {
                        viewModelScope.launch {
                            try {
                                repository.updateProgress(downloadId, 0.0f, "0 KB/s", "error", "Failed")
                            } catch (e: Throwable) {
                                Log.e("DownloadViewModel", "Failure handler failed", e)
                            }
                        }
                    }
                }

                urlInput.value = ""
                _metadataState.value = MetadataState.Idle
                totalSimulatedDownloads.value += 1
            } catch (e: Throwable) {
                Log.e("DownloadViewModel", "Failed to start download", e)
                _metadataState.value = MetadataState.Error("Failed to start download: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun detectExtension(url: String, isAudio: Boolean): String {
        val path = url.substringBefore("?").substringAfterLast("/")
        val ext = path.substringAfterLast(".", "").lowercase()
        if (ext in listOf("mp4", "webm", "mkv", "avi", "mov", "mp3", "m4a", "aac", "ogg", "wav")) return ext
        return if (isAudio) "mp3" else "mp4"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").take(100)
    }

    private fun saveToPublicStorage(file: File, fileName: String, mimeType: String): String? {
        return try {
            val context = getApplication<Application>()
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OpenDoor")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            }
            file.delete()
            uri?.toString()
        } catch (e: Throwable) {
            Log.w("DownloadViewModel", "Failed to save to public storage: ${e.message}")
            null
        }
    }

    fun pauseDownload(id: Int) {
        viewModelScope.launch {
            val job = activeJobs[id]
            if (job != null && job.isActive) {
                job.cancel()
            }
            val item = repository.getDownloadById(id)
            if (item != null) {
                repository.update(item.copy(status = "Paused", downloadSpeed = "0 KB/s", eta = "Paused"))
            }
        }
    }

    fun resumeDownload(id: Int) {
        viewModelScope.launch {
            try {
                val item = repository.getDownloadById(id) ?: return@launch
                val downloadUrl = formatUrlMap[item.quality]
                if (downloadUrl == null || !backendAvailable) {
                    _metadataState.value = MetadataState.Error("Cannot resume: backend unavailable.")
                    return@launch
                }

                val ext = detectExtension(downloadUrl, item.fileType == "audio")
                val fileName = "${sanitizeFileName(item.title)}_${item.quality.replace(" ", "_")}.$ext"
                val destFile = File(downloadDir, fileName)
                val tempFile = File(downloadDir, "$fileName.tmp")

                repository.updateProgress(id, item.downloadProgress, "0 KB/s", "resuming...", "Downloading")

                activeJobs[id]?.cancel()
                val job = downloadEngine.download(
                    url = downloadUrl,
                    destination = destFile,
                    tempFile = tempFile,
                    scope = viewModelScope,
                    onProgress = { progress ->
                        viewModelScope.launch {
                            try {
                                val remaining = 1f - item.downloadProgress
                                val totalProgress = (item.downloadProgress + progress.fraction * remaining).coerceAtMost(1f)
                                repository.updateProgress(
                                    id = id,
                                    progress = totalProgress,
                                    speed = progress.speed,
                                    eta = progress.eta,
                                    status = if (totalProgress < 1f) "Downloading" else "Completed"
                                )
                                if (totalProgress >= 1f) {
                                    repository.getDownloadById(id)?.let { dbItem ->
                                        repository.update(dbItem.copy(filePath = destFile.absolutePath))
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.e("DownloadViewModel", "Resume progress update failed", e)
                            }
                        }
                    }
                )
                activeJobs[id] = job
            } catch (e: Throwable) {
                Log.e("DownloadViewModel", "Failed to resume download", e)
                _metadataState.value = MetadataState.Error("Failed to resume: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun cancelDownload(id: Int) {
        viewModelScope.launch {
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
            val item = repository.getDownloadById(id)
            if (item != null) {
                val tempFile = File(item.filePath + ".tmp")
                if (tempFile.exists()) tempFile.delete()
            }
            repository.deleteById(id)
        }
    }

    fun toggleFavorite(id: Int, isFav: Boolean) {
        viewModelScope.launch { repository.updateFavorite(id, isFav) }
    }

    fun renameDownload(id: Int, newName: String) {
        viewModelScope.launch {
            val item = repository.getDownloadById(id)
            if (item != null) {
                repository.update(item.copy(title = newName))
            }
        }
    }

    fun deleteDownload(downloadItem: DownloadItem) {
        viewModelScope.launch {
            activeJobs[downloadItem.id]?.cancel()
            activeJobs.remove(downloadItem.id)
            if (!downloadItem.filePath.startsWith("http")) {
                val file = File(downloadItem.filePath)
                if (file.exists()) file.delete()
                val tempFile = File(downloadItem.filePath + ".tmp")
                if (tempFile.exists()) tempFile.delete()
            }
            repository.delete(downloadItem)
        }
    }

    fun getDownloadedFilesSizeSum(): Long {
        return downloads.value.filter { it.status == "Completed" }.sumOf { it.fileSize }
    }

    fun getStorageRecommendations(): List<DownloadItem> {
        return downloads.value.filter { it.status == "Completed" }.sortedByDescending { it.fileSize }
    }

    fun clearAllCompletedDownloads() {
        viewModelScope.launch {
            downloads.value.filter { it.status == "Completed" }.forEach {
                if (!it.filePath.startsWith("http")) {
                    val file = File(it.filePath)
                    if (file.exists()) file.delete()
                }
                repository.delete(it)
            }
        }
    }

    fun suspendAdminUser(id: String) {
        _adminUsers.value = _adminUsers.value.map {
            if (it.id == id) it.copy(status = "Suspended") else it
        }
    }

    fun activateAdminUser(id: String) {
        _adminUsers.value = _adminUsers.value.map {
            if (it.id == id) it.copy(status = "Active") else it
        }
    }

    fun changeUserSubscription(id: String, sub: String) {
        _adminUsers.value = _adminUsers.value.map {
            if (it.id == id) it.copy(subscription = sub) else it
        }
    }

    fun deleteAdminUser(id: String) {
        _adminUsers.value = _adminUsers.value.filter { it.id != id }
    }

    fun updateAdSettings(enabled: Boolean) {
        adSettingsEnabled.value = enabled
    }

    fun updateAnnouncement(text: String) {
        appAnnouncement.value = text
    }

    override fun onCleared() {
        super.onCleared()
        activeJobs.values.forEach { it.cancel() }
    }
}
