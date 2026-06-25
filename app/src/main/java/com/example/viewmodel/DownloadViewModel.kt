package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Data structures for fetched info
data class ResolutionOption(val quality: String, val fileSizeEstimate: Long)
data class AudioOption(val format: String, val fileSizeEstimate: Long)

data class VideoMetadata(
    val url: String,
    val title: String,
    val author: String,
    val duration: Long, // in seconds
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

// User representation for Admin Dashboard
data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val subscription: String, // "Free", "Premium", "Ad-Free"
    val status: String, // "Active", "Suspended"
    val joinDate: String
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DownloadRepository
    val downloads: StateFlow<List<DownloadItem>>

    // Active downloading Coroutines
    private val activeJobs = mutableMapOf<Int, Job>()

    // UI States
    var urlInput = MutableStateFlow("")
        private set

    private val _metadataState = MutableStateFlow<MetadataState>(MetadataState.Idle)
    val metadataState: StateFlow<MetadataState> = _metadataState.asStateFlow()

    // Storage info states
    private val _storageUsedBytes = MutableStateFlow(45 * 1024 * 1024 * 1024L) // Simulated 45 GB used by other files
    val totalStorageBytes = 128 * 1024 * 1024 * 1024L // Simulated 128 GB total device storage

    // Theme state
    var isDarkTheme = MutableStateFlow(false)
        private set

    // Admin Dashboard simulated states
    private val _adminUsers = MutableStateFlow<List<AdminUser>>(emptyList())
    val adminUsers: StateFlow<List<AdminUser>> = _adminUsers.asStateFlow()

    var totalSimulatedDownloads = MutableStateFlow(3240)
        private set

    var adSettingsEnabled = MutableStateFlow(true)
        private set

    var appAnnouncement = MutableStateFlow("Welcome to OpenDoor v1.0! Tap, download, and enjoy videos offline with dynamic speed controls.")
        private set

    // Sample video URLs for media streaming mapping
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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DownloadRepository(database.downloadDao())
        
        downloads = repository.allDownloads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed some admin users
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

    // URL validation and platform extraction
    fun fetchVideoInfo() {
        val url = urlInput.value.trim()
        if (url.isEmpty()) {
            _metadataState.value = MetadataState.Error("Please paste a video link first.")
            return
        }

        viewModelScope.launch {
            _metadataState.value = MetadataState.Fetching
            delay(1500) // Realistic network look-up delay

            val platform = detectPlatform(url)
            if (platform == "Unknown") {
                _metadataState.value = MetadataState.Error("Unsupported website. Please paste a valid link from YouTube, Vimeo, Facebook, X, TikTok, or Instagram.")
            } else {
                // Generate clean, platform-specific metadata simulation
                val metadata = generateSimulatedMetadata(url, platform)
                _metadataState.value = MetadataState.Success(metadata)
            }
        }
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
            // If it's a direct mp4/mp3 url, treat as Generic
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
                duration = 742, // 12m 22s
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
                duration = 240, // 4m
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
                duration = 45, // 45s
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
                duration = 60, // 1m
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
                duration = 480, // 8m
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
                duration = 112, // 1m 52s
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
            else -> { // Direct Link
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

    // Start video/audio download
    fun startDownload(metadata: VideoMetadata, optionName: String, isAudio: Boolean, sizeEstimate: Long) {
        viewModelScope.launch {
            // Find a random sample URL to use for actual streaming in the media player, or use direct link if applicable
            val directLower = metadata.url.lowercase()
            val isDirectLink = metadata.platform == "Direct Link" && (directLower.startsWith("http://") || directLower.startsWith("https://"))
            val randomStreamUrl = if (isDirectLink) {
                metadata.url
            } else {
                if (isAudio) {
                    sampleAudios[Random().nextInt(sampleAudios.size)]
                } else {
                    sampleVideos[Random().nextInt(sampleVideos.size)]
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
                filePath = randomStreamUrl, // Store real playable URL here
                fileSize = sizeEstimate,
                downloadProgress = 0.0f,
                downloadSpeed = "0 KB/s",
                eta = "calculating...",
                status = "Pending",
                isFavorite = false,
                timestamp = System.currentTimeMillis()
            )

            // Insert into local DB
            val downloadId = repository.insert(newItem).toInt()
            
            // Start download thread simulator
            launchDownloadSimulation(downloadId)

            // Clean inputs
            urlInput.value = ""
            _metadataState.value = MetadataState.Idle
            
            // Increment simulated dashboard count
            totalSimulatedDownloads.value += 1
        }
    }

    // Download processing core supporting Pause/Resume/Cancel/Retry
    fun launchDownloadSimulation(downloadId: Int) {
        // If there's an existing job running, cancel it first
        activeJobs[downloadId]?.cancel()

        val job = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update status to Downloading
                repository.updateProgress(downloadId, 0.0f, "0 KB/s", "calculating...", "Downloading")

                var progress = 0.0f
                // Check if we already had progress (in case of resume)
                val existing = repository.getDownloadById(downloadId)
                if (existing != null) {
                    progress = existing.downloadProgress
                }

                val totalSize = existing?.fileSize ?: (50 * 1024 * 1024L)
                val isAudio = existing?.fileType == "audio"

                while (progress < 1.0f) {
                    delay(800) // Delay between progress updates

                    // Re-query status to check if user paused or cancelled in between
                    val current = repository.getDownloadById(downloadId) ?: break
                    if (current.status == "Paused") {
                        break
                    }

                    // Simulate a download chunk
                    val speedValue = 1.2 + Random().nextDouble() * 7.5 // 1.2 to 8.7 MB/s
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
                        repository.updateProgress(
                            id = downloadId,
                            progress = 1.0f,
                            speed = "0 KB/s",
                            eta = "0s",
                            status = "Completed"
                        )
                    } else {
                        repository.updateProgress(
                            id = downloadId,
                            progress = progress,
                            speed = speedStr,
                            eta = etaStr,
                            status = "Downloading"
                        )
                    }
                }
            } catch (e: CancellationException) {
                // Job was cancelled externally (pause or delete)
                Log.d("DownloadViewModel", "Download job $downloadId cancelled")
            } catch (e: Exception) {
                repository.updateProgress(downloadId, 0.0f, "0 KB/s", "error", "Failed")
            }
        }

        activeJobs[downloadId] = job
    }

    fun pauseDownload(id: Int) {
        viewModelScope.launch {
            activeJobs[id]?.cancel()
            val item = repository.getDownloadById(id)
            if (item != null) {
                repository.update(item.copy(status = "Paused", downloadSpeed = "0 KB/s", eta = "Paused"))
            }
        }
    }

    fun resumeDownload(id: Int) {
        launchDownloadSimulation(id)
    }

    fun cancelDownload(id: Int) {
        viewModelScope.launch {
            activeJobs[id]?.cancel()
            repository.deleteById(id)
        }
    }

    fun toggleFavorite(id: Int, isFav: Boolean) {
        viewModelScope.launch {
            repository.updateFavorite(id, isFav)
        }
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
            repository.delete(downloadItem)
        }
    }

    // Storage analysis
    fun getDownloadedFilesSizeSum(): Long {
        return downloads.value.filter { it.status == "Completed" }.sumOf { it.fileSize }
    }

    fun getStorageRecommendations(): List<DownloadItem> {
        // Recommendations are Completed items sorted by size (largest first)
        return downloads.value.filter { it.status == "Completed" }.sortedByDescending { it.fileSize }
    }

    fun clearAllCompletedDownloads() {
        viewModelScope.launch {
            downloads.value.filter { it.status == "Completed" }.forEach {
                repository.delete(it)
            }
        }
    }

    // Admin dashboard mock actions
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
        // Cancel all active coroutine download loops to prevent resource leaks
        activeJobs.values.forEach { it.cancel() }
    }
}
