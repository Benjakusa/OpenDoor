package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.ui.theme.*
import com.example.viewmodel.DownloadViewModel
import com.example.viewmodel.MetadataState
import com.example.viewmodel.VideoMetadata
import java.util.Locale

@Composable
fun DownloaderScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val metadataState by viewModel.metadataState.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    
    val activeDownloads = remember(downloads) {
        downloads.filter { it.status != "Completed" }
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var autoDownloadOnSuccess by remember { mutableStateOf(false) }
    var lastAutofetchedUrl by remember { mutableStateOf("") }

    // Automatic preview metadata fetching when a valid URL is pasted
    LaunchedEffect(urlInput) {
        val trimmed = urlInput.trim()
        if (trimmed.isNotEmpty() && trimmed != lastAutofetchedUrl) {
            val lower = trimmed.lowercase()
            val isHttp = lower.startsWith("http://") || lower.startsWith("https://")
            val isSupported = lower.contains("youtube.com") || lower.contains("youtu.be") ||
                    lower.contains("facebook.com") || lower.contains("fb.watch") ||
                    lower.contains("x.com") || lower.contains("twitter.com") ||
                    lower.contains("tiktok.com") || lower.contains("instagram.com") ||
                    lower.endsWith(".mp4") || lower.endsWith(".mp3")
            
            if (isHttp && isSupported) {
                lastAutofetchedUrl = trimmed
                viewModel.fetchVideoInfo()
            }
        }
    }

    // Direct download trigger as soon as metadata is fetched successfully (if requested)
    LaunchedEffect(metadataState) {
        if (metadataState is MetadataState.Success && autoDownloadOnSuccess) {
            autoDownloadOnSuccess = false
            val metadata = (metadataState as MetadataState.Success).metadata
            val highestRes = metadata.availableResolutions.firstOrNull()
            if (highestRes != null) {
                viewModel.startDownload(metadata, highestRes.quality, false, highestRes.fileSizeEstimate)
                Toast.makeText(context, "Downloading highest quality: ${highestRes.quality}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // Welcome Header is removed completely as requested (Requirement 7)

        // URL Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Paste Video Link",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.updateUrlInput(it) },
                        placeholder = { Text("Paste your URL", color = VaultLightGray) }, // Requirement 10
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultBlue,
                            unfocusedBorderColor = VaultMediumGray,
                            focusedTextColor = VaultWhite,
                            unfocusedTextColor = VaultWhite
                        ),
                        trailingIcon = {
                            Row {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateUrlInput("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VaultLightGray)
                                    }
                                }
                                IconButton(onClick = {
                                    clipboardManager.getText()?.let {
                                        viewModel.updateUrlInput(it.text)
                                        Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Clipboard", tint = VaultBlue)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val state = metadataState
                                if (state is MetadataState.Success) {
                                    val metadata = state.metadata
                                    val highestRes = metadata.availableResolutions.firstOrNull()
                                    if (highestRes != null) {
                                        viewModel.startDownload(metadata, highestRes.quality, false, highestRes.fileSizeEstimate)
                                        Toast.makeText(context, "Downloading highest quality: ${highestRes.quality}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.fetchVideoInfo()
                                    autoDownloadOnSuccess = true
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val state = metadataState
                            if (state is MetadataState.Success) {
                                val metadata = state.metadata
                                val highestRes = metadata.availableResolutions.firstOrNull()
                                if (highestRes != null) {
                                    viewModel.startDownload(metadata, highestRes.quality, false, highestRes.fileSizeEstimate)
                                    Toast.makeText(context, "Downloading highest quality: ${highestRes.quality}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.fetchVideoInfo()
                                autoDownloadOnSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultBlue),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_button"),
                        enabled = metadataState !is MetadataState.Fetching
                    ) {
                        if (metadataState is MetadataState.Fetching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = "Download", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download", fontWeight = FontWeight.Bold, fontSize = 14.sp) // Requirement 11
                            }
                        }
                    }
                }
            }
        }

        // Platform Presets Row
        item {
            Column {
                Text(
                    text = "Supported Sites",
                    color = VaultWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val presets = listOf(
                        "YouTube",
                        "X (Twitter)", // Requirement 9
                        "TikTok",
                        "Instagram",
                        "Facebook"
                    )

                    presets.forEach { name ->
                        Surface(
                            onClick = {
                                viewModel.updateUrlInput("")
                                Toast.makeText(context, "Please paste your $name video link!", Toast.LENGTH_SHORT).show()
                            },
                            color = VaultDarkGray,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .testTag("platform_preset_${name.lowercase().replace(" ", "_")}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.replace(" (Twitter)", ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (name) {
                                        "YouTube" -> Color(0xFFFF0000)
                                        "X (Twitter)" -> VaultWhite
                                        "TikTok" -> VaultWhite
                                        "Instagram" -> Color(0xFFE1306C)
                                        "Facebook" -> Color(0xFF1877F2)
                                        else -> VaultLightGray
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Downloads section (Download Manager)
        if (activeDownloads.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download Manager (${activeDownloads.size})",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            items(activeDownloads, key = { it.id }) { item ->
                ActiveDownloadItemRow(item, viewModel)
            }
        }

        // Fetched Video metadata result (Video Details Screen)
        item {
            AnimatedContent(
                targetState = metadataState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MetadataDisplay"
            ) { state ->
                when (state) {
                    is MetadataState.Idle -> {
                        if (activeDownloads.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DownloadForOffline,
                                    contentDescription = "Ready",
                                    tint = VaultMediumGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Ready to Download",
                                    color = VaultLightGray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is MetadataState.Error -> {
                        Surface(
                            color = VaultRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VaultRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = VaultRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    color = VaultRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    is MetadataState.Success -> {
                        VideoDetailCard(state.metadata, viewModel)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun VideoDetailCard(
    metadata: VideoMetadata,
    viewModel: DownloadViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_details_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: platform tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (metadata.platform) {
                        "YouTube" -> Color(0xFFFF0000).copy(alpha = 0.15f)
                        "Vimeo" -> Color(0xFF1AB7EA).copy(alpha = 0.15f)
                        "TikTok" -> VaultWhite.copy(alpha = 0.15f)
                        "Instagram" -> Color(0xFFE1306C).copy(alpha = 0.15f)
                        "Facebook" -> Color(0xFF1877F2).copy(alpha = 0.15f)
                        else -> VaultBlue.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = metadata.platform,
                        color = when (metadata.platform) {
                            "YouTube" -> Color(0xFFFF3333)
                            "Vimeo" -> Color(0xFF26C6DA)
                            "TikTok" -> VaultWhite
                            "Instagram" -> Color(0xFFFF5C8D)
                            "Facebook" -> Color(0xFF42A5F5)
                            else -> VaultBlue
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Upload Date: ${metadata.uploadDate}",
                    color = VaultLightGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Thumbnail + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .size(width = 110.dp, height = 75.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.title,
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Creator: ${metadata.author}",
                        color = VaultLightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Duration: ${formatDuration(metadata.duration)}",
                        color = VaultEmerald,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = VaultMediumGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Download Selection
            Text(
                text = "Select Video Quality",
                color = VaultWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            metadata.availableResolutions.forEach { res ->
                QualitySelectionRow(
                    title = res.quality,
                    fileSize = res.fileSizeEstimate,
                    isAudio = false,
                    onClick = {
                        viewModel.startDownload(metadata, res.quality, false, res.fileSizeEstimate)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Extract Audio Only",
                color = VaultWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            metadata.availableAudios.forEach { aud ->
                QualitySelectionRow(
                    title = aud.format,
                    fileSize = aud.fileSizeEstimate,
                    isAudio = true,
                    onClick = {
                        viewModel.startDownload(metadata, aud.format, true, aud.fileSizeEstimate)
                    }
                )
            }
        }
    }
}

@Composable
fun QualitySelectionRow(
    title: String,
    fileSize: Long,
    isAudio: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = VaultMediumGray,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("quality_option_${title.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = if (isAudio) VaultAmber else VaultBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = VaultWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatFileSize(fileSize),
                    color = VaultLightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = VaultEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadItemRow(
    item: DownloadItem,
    viewModel: DownloadViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_download_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header info: Title + Type icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.fileType == "audio") Icons.Default.MusicNote else Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = if (item.fileType == "audio") VaultAmber else VaultBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.cancelDownload(item.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel task", tint = VaultRed, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = item.downloadProgress,
                color = if (item.status == "Paused") VaultAmber else VaultEmerald,
                trackColor = VaultMediumGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .testTag("download_progress_bar_${item.id}")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-info: Percentage, Speed, ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(item.downloadProgress * 100).toInt()}%",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.downloadSpeed,
                        color = VaultLightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ETA: ${item.eta}",
                        color = VaultLightGray,
                        fontSize = 11.sp
                    )
                }

                // Pause/Resume Actions
                Surface(
                    onClick = {
                        if (item.status == "Paused") {
                            viewModel.resumeDownload(item.id)
                        } else {
                            viewModel.pauseDownload(item.id)
                        }
                    },
                    color = if (item.status == "Paused") VaultEmerald.copy(alpha = 0.15f) else VaultAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (item.status == "Paused") "Resume" else "Pause",
                        color = if (item.status == "Paused") VaultEmerald else VaultAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%dm %02ds", m, s)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
