package com.example.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadEngine
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.remote.BackendApi
import com.example.data.remote.ExtractRequest
import com.example.data.remote.FormatInfo
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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

    var totalSimulatedDownloads = MutableStateFlow(3240)
        private set

    var adSettingsEnabled = MutableStateFlow(true)
        private set

    var appAnnouncement = MutableStateFlow("Welcome to OpenDoor v1.0! Tap, download, and enjoy videos offline with dynamic speed controls.")
        private set

    private val sampleVideos = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    private val sampleAudios = listOf(
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
    )

    private val backendApi: BackendApi?
    private var backendAvailable = false
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

        // Try to read backend URL from BuildConfig (set via .env / Secrets plugin)
        val backendUrl = try {
            val url = com.example.BuildConfig.BACKEND_URL
            if (url.isNotEmpty() && !url.contains("your-backend")) url else null
        } catch (_: Exception) {
            null
        }

        if (backendUrl != null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            backendApi = Retrofit.Builder()
                .baseUrl(if (backendUrl.endsWith("/")) backendUrl else "$backendUrl/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(BackendApi::class.java)

            viewModelScope.launch {
                try {
                    backendApi.health()
                    backendAvailable = true
                    Log.i("DownloadViewModel", "Backend available at $backendUrl")
                } catch (e: Exception) {
                    backendAvailable = false
                    Log.w("DownloadViewModel", "Backend unreachable, using simulation: ${e.message}")
                }
            }
        } else {
            backendApi = null
            backendAvailable = false
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

            if (backendAvailable && backendApi != null) {
                try {
                    val response = backendApi.extractVideo(ExtractRequest(url))
                    formatUrlMap.clear()

                    response.formats.forEach { f ->
                        formatUrlMap[f.quality] = f.downloadUrl
                        formatUrlMap["${f.quality}.${f.ext}"] = f.downloadUrl
                    }

                    val videos = response.videoFormats.ifEmpty {
                        response.formats.filter { it.hasVideo }
                    }.map { ResolutionOption(it.quality, it.fileSize) }
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
                            duration = response.duration,
                            thumbnailUrl = response.thumbnailUrl,
                            platform = response.platform,
                            uploadDate = response.uploadDate,
                            availableResolutions = videos,
                            availableAudios = audios
                        )
                    )
                    return@launch
                } catch (e: Exception) {
                    Log.w("DownloadViewModel", "Backend extract failed: ${e.message}")
                }
            }

            // Simulation fallback
            delay(1500)
            val platform = detectPlatform(url)
            if (platform == "Unknown") {
                _metadataState.value = MetadataState.Error("Unsupported website. Please paste a valid link from YouTube, Vimeo, Facebook, X, TikTok, or Instagram.")
            } else {
                _metadataState.value = MetadataState.Success(generateSimulatedMetadata(url, platform))
            }
        }
    }

    fun startDownload(metadata: VideoMetadata, optionName: String, isAudio: Boolean, sizeEstimate: Long) {
        viewModelScope.launch {
            val directLower = metadata.url.lowercase()
            val isDirectLink = metadata.platform == "Direct Link" && (directLower.startsWith("http://") || directLower.startsWith("https://"))

            val downloadUrl = formatUrlMap[optionName]
            val useRealDownload = downloadUrl != null && !downloadUrl.isNullOrEmpty() && backendAvailable && !isDirectLink

            val streamUrl: String
            val localFilePath: String

            if (useRealDownload) {
                streamUrl = downloadUrl
                val ext = detectExtension(downloadUrl, isAudio)
                val fileName = "${sanitizeFileName(metadata.title)}_${optionName.replace(" ", "_")}.$ext"
                val destFile = File(downloadDir, fileName)
                val tempFile = File(downloadDir, "$fileName.tmp")
                var counter = 1
                val baseName = fileName.substringBeforeLast(".")
                val extension = fileName.substringAfterLast(".")
                while (destFile.exists()) {
                    val uniqueName = "${baseName}_($counter).$extension"
                    destFile.renameTo(File(downloadDir, uniqueName))
                    counter++
                }

                localFilePath = destFile.absolutePath

                val newItem = DownloadItem(
                    url = metadata.url,
                    title = metadata.title,
                    author = metadata.author,
                    duration = metadata.duration,
                    thumbnailUrl = metadata.thumbnailUrl,
                    platform = metadata.platform,
                    fileType = if (isAudio) "audio" else "video",
                    quality = optionName,
                    filePath = streamUrl,
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
                            repository.updateProgress(
                                id = downloadId,
                                progress = progress.fraction,
                                speed = progress.speed,
                                eta = progress.eta,
                                status = if (progress.fraction < 1f) "Downloading" else "Completed"
                            )
                            if (progress.fraction >= 1f) {
                                repository.getDownloadById(downloadId)?.let { item ->
                                    repository.update(item.copy(filePath = localFilePath))
                                }
                            }
                        }
                    }
                )

                activeJobs[downloadId] = job

                job.invokeOnCompletion { cause ->
                    if (cause is CancellationException) {
                        viewModelScope.launch {
                            val item = repository.getDownloadById(downloadId)
                            if (item != null && item.status != "Completed") {
                                repository.update(item.copy(status = "Paused", downloadSpeed = "0 KB/s", eta = "Paused"))
                            }
                        }
                    } else if (cause != null) {
                        viewModelScope.launch {
                            repository.updateProgress(downloadId, 0.0f, "0 KB/s", "error", "Failed")
                        }
                    }
                }
            } else {
                // Simulation mode
                val randomStreamUrl = if (isDirectLink) {
                    metadata.url
                } else {
                    if (isAudio) sampleAudios[Random().nextInt(sampleAudios.size)]
                    else sampleVideos[Random().nextInt(sampleVideos.size)]
                }
                localFilePath = randomStreamUrl

                val newItem = DownloadItem(
                    url = metadata.url,
                    title = metadata.title,
                    author = metadata.author,
                    duration = metadata.duration,
                    thumbnailUrl = metadata.thumbnailUrl,
                    platform = metadata.platform,
                    fileType = if (isAudio) "audio" else "video",
                    quality = optionName,
                    filePath = randomStreamUrl,
                    fileSize = sizeEstimate,
                    downloadProgress = 0.0f,
                    downloadSpeed = "0 KB/s",
                    eta = "calculating...",
                    status = "Pending",
                    isFavorite = false,
                    timestamp = System.currentTimeMillis()
                )

                val downloadId = repository.insert(newItem).toInt()
                launchDownloadSimulation(downloadId)
            }

            urlInput.value = ""
            _metadataState.value = MetadataState.Idle
            totalSimulatedDownloads.value += 1
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

    private fun launchDownloadSimulation(downloadId: Int) {
        activeJobs[downloadId]?.cancel()

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateProgress(downloadId, 0.0f, "0 KB/s", "calculating...", "Downloading")

                var progress = 0.0f
                val existing = repository.getDownloadById(downloadId)
                if (existing != null) {
                    progress = existing.downloadProgress
                }

                val totalSize = existing?.fileSize ?: (50 * 1024 * 1024L)

                while (progress < 1.0f) {
                    delay(800)

                    val current = repository.getDownloadById(downloadId) ?: break
                    if (current.status == "Paused") break

                    val speedValue = 1.2 + Random().nextDouble() * 7.5
                    val speedStr = String.format(Locale.getDefault(), "%.1f MB/s", speedValue)

                    val bytesPerSec = (speedValue * 1024 * 1024).toLong()
                    val remainingBytes = (totalSize * (1.0f - progress)).toLong()
                    val remainingSeconds = if (bytesPerSec > 0) remainingBytes / bytesPerSec else 0

                    val etaStr = if (remainingSeconds > 60) {
                        "${remainingSeconds / 60}m ${remainingSeconds % 60}s"
                    } else {
                        "${remainingSeconds}s"
                    }

                    val progressDelta = (bytesPerSec.toFloat() / totalSize.toFloat()).coerceAtLeast(0.04f).coerceAtMost(0.12f)
                    progress = (progress + progressDelta).coerceAtMost(1.0f)

                    if (progress >= 1.0f) {
                        repository.updateProgress(downloadId, 1.0f, "0 KB/s", "0s", "Completed")
                    } else {
                        repository.updateProgress(downloadId, progress, speedStr, etaStr, "Downloading")
                    }
                }
            } catch (e: CancellationException) {
                Log.d("DownloadViewModel", "Download job $downloadId cancelled")
            } catch (e: Exception) {
                repository.updateProgress(downloadId, 0.0f, "0 KB/s", "error", "Failed")
            }
        }

        activeJobs[downloadId] = job
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
            val item = repository.getDownloadById(id) ?: return@launch
            val downloadUrl = formatUrlMap[item.quality]
            if (downloadUrl != null && backendAvailable && item.filePath.startsWith("http")) {
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
                        }
                    }
                )
                activeJobs[id] = job
            } else {
                launchDownloadSimulation(id)
            }
        }
    }

    fun cancelDownload(id: Int) {
        viewModelScope.launch {
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
            // Clean up any temp files
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
            // Delete the actual file if it's a local file
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

    private fun detectPlatform(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("vimeo.com") -> "Vimeo"
            lower.contains("facebook.com") || lower.contains("fb.watch") -> "Facebook"
            lower.contains("x.com") || lower.contains("twitter.com") -> "X (Twitter)"
            lower.contains("tiktok.com") -> "TikTok"
            lower.contains("instagram.com") -> "Instagram"
            lower.endsWith(".mp4") || lower.endsWith(".mp3") || lower.contains(".mp4?") || lower.contains(".mp3?") -> "Direct Link"
            else -> "Unknown"
        }
    }

    private fun generateSimulatedMetadata(url: String, platform: String): VideoMetadata {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = format.format(calendar.time)

        return when (platform) {
            "YouTube" -> VideoMetadata(
                url = url,
                title = "Exploring Cosmic Mysteries: Beyond the Event Horizon",
                author = "SpaceVids Hub",
                duration = 742,
                thumbnailUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=600&auto=format&fit=crop",
                platform = "YouTube",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("1080p Full HD", 145 * 1024 * 1024L),
                    ResolutionOption("720p HD", 78 * 1024 * 1024L),
                    ResolutionOption("480p SD", 36 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("MP3 (320kbps)", 12 * 1024 * 1024L),
                    AudioOption("M4A (128kbps)", 6 * 1024 * 1024L)
                )
            )
            "Vimeo" -> VideoMetadata(
                url = url,
                title = "Cinematic Waves: A Coastal Expedition",
                author = "Vanguard Films",
                duration = 240,
                thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop",
                platform = "Vimeo",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("4K Ultra HD", 412 * 1024 * 1024L),
                    ResolutionOption("1080p Full HD", 82 * 1024 * 1024L),
                    ResolutionOption("720p HD", 44 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("AAC (256kbps)", 8 * 1024 * 1024L)
                )
            )
            "TikTok" -> VideoMetadata(
                url = url,
                title = "Insane Basketball Trick Shot! 🏀🔥 #viral #sports",
                author = "TrickShotPro",
                duration = 45,
                thumbnailUrl = "https://images.unsplash.com/photo-1546519638-68e109498ffc?q=80&w=600&auto=format&fit=crop",
                platform = "TikTok",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("720p HD", 15 * 1024 * 1024L),
                    ResolutionOption("576p SD", 8 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("MP3 Audio Only", 1 * 1024 * 1024L)
                )
            )
            "Instagram" -> VideoMetadata(
                url = url,
                title = "Slow Roast Espresso Brew Guide ☕️✨",
                author = "RoasteryLab",
                duration = 60,
                thumbnailUrl = "https://images.unsplash.com/photo-1507133750040-4a8f57021571?q=80&w=600&auto=format&fit=crop",
                platform = "Instagram",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("1080p HD", 22 * 1024 * 1024L),
                    ResolutionOption("720p SD", 11 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("M4A Soundtrack", 1 * 1024 * 1024L)
                )
            )
            "Facebook" -> VideoMetadata(
                url = url,
                title = "Epic Backyard Treehouse Build Time-lapse 🔨🌳",
                author = "DIY Craftsman",
                duration = 480,
                thumbnailUrl = "https://images.unsplash.com/photo-1510251173-857e28d1170b?q=80&w=600&auto=format&fit=crop",
                platform = "Facebook",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("1080p Full HD", 95 * 1024 * 1024L),
                    ResolutionOption("720p HD", 52 * 1024 * 1024L),
                    ResolutionOption("360p Mobile", 18 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("MP3 (192kbps)", 7 * 1024 * 1024L)
                )
            )
            "X (Twitter)" -> VideoMetadata(
                url = url,
                title = "Breaking: Smart AI Robot Solves Complex Maze in Seconds 🤖",
                author = "TechWorldNews",
                duration = 112,
                thumbnailUrl = "https://images.unsplash.com/photo-1485827404703-89b55fcc595e?q=80&w=600&auto=format&fit=crop",
                platform = "X (Twitter)",
                uploadDate = dateStr,
                availableResolutions = listOf(
                    ResolutionOption("720p HD", 28 * 1024 * 1024L),
                    ResolutionOption("480p SD", 14 * 1024 * 1024L)
                ),
                availableAudios = listOf(
                    AudioOption("AAC Audio Only", 2 * 1024 * 1024L)
                )
            )
            else -> {
                val fileName = url.substringAfterLast("/").substringBefore("?")
                VideoMetadata(
                    url = url,
                    title = if (fileName.length > 5) fileName else "Direct Download File",
                    author = "Web Server",
                    duration = 320,
                    thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600&auto=format&fit=crop",
                    platform = "Direct Link",
                    uploadDate = dateStr,
                    availableResolutions = listOf(
                        ResolutionOption("Original Quality", 55 * 1024 * 1024L)
                    ),
                    availableAudios = listOf(
                        AudioOption("MP3 Extract", 5 * 1024 * 1024L)
                    )
                )
            }
        }
    }
}
