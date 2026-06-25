package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DownloadItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerBottomSheet(
    downloadItem: DownloadItem,
    onDismiss: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) }

    if (isFullScreen) {
        FullScreenPlayerDialog(
            downloadItem = downloadItem,
            onDismiss = { isFullScreen = false }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = VaultDarkGray,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            PlayerContent(
                downloadItem = downloadItem,
                isFullScreen = false,
                toggleFullScreen = { isFullScreen = true },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun FullScreenPlayerDialog(
    downloadItem: DownloadItem,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            PlayerContent(
                downloadItem = downloadItem,
                isFullScreen = true,
                toggleFullScreen = onDismiss,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun PlayerContent(
    downloadItem: DownloadItem,
    isFullScreen: Boolean,
    toggleFullScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = downloadItem.fileType == "video"

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Subtitle simulation
    val simulatedSubtitles = listOf(
        3 to "[Atmospheric background music playing]",
        8 to "Welcome to the future of high-speed offline offline playback.",
        15 to "We are downloading and managing files in full quality.",
        24 to "OpenDoor gives you secure access to your media assets.",
        35 to "Thank you for exploring this experience!"
    )

    val activeSubtitle = remember(currentPosition) {
        val seconds = currentPosition / 1000
        simulatedSubtitles.lastOrNull { seconds >= it.first && seconds < it.first + 4 }?.second ?: ""
    }

    // Audio animation (spinning disc)
    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylSpinAngle"
    )

    // Position updates coroutine
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                mediaPlayerRef?.let { mp ->
                    if (mp.isPlaying) {
                        currentPosition = mp.currentPosition
                    }
                }
                delay(200)
            }
        }
    }

    // Controls autohide
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && isVideo) {
            delay(4000)
            showControls = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
            .background(if (isFullScreen) Color.Black else VaultDarkGray)
            .clickable(enabled = isVideo) { showControls = !showControls },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isFullScreen) 1.2f else 16 / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isVideo) {
                // Real Video Player using native Android VideoView
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(downloadItem.filePath))
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                duration = mp.duration
                                mp.isLooping = true
                                mp.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // Dynamically apply speed on newer Android versions
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                mediaPlayerRef?.let { mp ->
                                    val params = mp.playbackParams
                                    params.speed = playbackSpeed
                                    mp.playbackParams = params
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        if (isMuted) {
                            mediaPlayerRef?.setVolume(0f, 0f)
                        } else {
                            mediaPlayerRef?.setVolume(1f, 1f)
                        }
                    }
                )

                // Subtitle Overlay
                if (activeSubtitle.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeSubtitle,
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Custom Overlay Controls
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        // Quick bar (top)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.White)
                            }

                            Text(
                                text = downloadItem.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            )

                            IconButton(onClick = toggleFullScreen) {
                                Icon(
                                    if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }

                        // Play/Pause center overlay button
                        IconButton(
                            onClick = {
                                mediaPlayerRef?.let { mp ->
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .align(Alignment.Center)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = VaultEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                // Audio Vinyl Disk player
                AndroidView(
                    factory = { ctx ->
                        // Invisible media player helper for audio streaming
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(downloadItem.filePath))
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                duration = mp.duration
                                mp.isLooping = true
                                mp.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier.size(1.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(VaultDarkGray, VaultBlack)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Vinyl shape
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .rotate(if (isPlaying) rotationAngle else 0f)
                                .background(Color.Black, CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Grooves
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color.Transparent)
                                    .clip(CircleShape)
                            )
                            // Core label
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(VaultEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = "Audio Disc",
                                    tint = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Streaming HQ Audio Only",
                            color = VaultEmerald,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    // Close button top-left
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Audio", tint = VaultLightGray)
                    }
                }
            }
        }

        // Info and Scrubbing Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!isVideo) {
                Text(
                    text = downloadItem.title,
                    color = VaultWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = downloadItem.author,
                    color = VaultLightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Slider scrubbing bar
            Slider(
                value = if (duration > 0) currentPosition.toFloat() else 0f,
                onValueChange = { newValue ->
                    currentPosition = newValue.toInt()
                    mediaPlayerRef?.seekTo(newValue.toInt())
                },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                colors = SliderDefaults.colors(
                    activeTrackColor = VaultEmerald,
                    inactiveTrackColor = VaultMediumGray,
                    thumbColor = VaultEmerald
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Duration timestamps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    color = VaultLightGray,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(duration),
                    color = VaultLightGray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Speed Toggle
                TextButton(
                    onClick = {
                        playbackSpeed = when (playbackSpeed) {
                            0.5f -> 1.0f
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 0.5f
                        }
                    }
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = VaultEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Skip Backward 10s
                IconButton(
                    onClick = {
                        val seek = (currentPosition - 10000).coerceAtLeast(0)
                        currentPosition = seek
                        mediaPlayerRef?.seekTo(seek)
                    }
                ) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Skip Backward 10s",
                        tint = VaultWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause core button
                IconButton(
                    onClick = {
                        mediaPlayerRef?.let { mp ->
                            if (mp.isPlaying) {
                                mp.pause()
                                isPlaying = false
                            } else {
                                mp.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(VaultEmerald, CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play / Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip Forward 10s
                IconButton(
                    onClick = {
                        val seek = (currentPosition + 10000).coerceAtMost(duration)
                        currentPosition = seek
                        mediaPlayerRef?.seekTo(seek)
                    }
                ) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Skip Forward 10s",
                        tint = VaultWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Mute button
                IconButton(onClick = { isMuted = !isMuted }) {
                    Icon(
                        if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Audio",
                        tint = if (isMuted) VaultRed else VaultWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
