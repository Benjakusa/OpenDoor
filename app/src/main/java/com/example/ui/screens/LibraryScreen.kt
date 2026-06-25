package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DownloadItem
import com.example.ui.theme.*
import com.example.viewmodel.DownloadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: DownloadViewModel,
    onPlayMedia: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloads.collectAsState()
    
    // States
    var selectedCategory by remember { mutableStateOf("All") } // "All", "Videos", "Audio", "Favorites"
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Date Newest") } // "Date Newest", "Date Oldest", "Size Largest", "A-Z"
    var showSortMenu by remember { mutableStateOf(false) }

    // Dialogs
    var itemToRename by remember { mutableStateOf<DownloadItem?>(null) }
    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val completedDownloads = remember(downloads) {
        downloads.filter { it.status == "Completed" }
    }

    val context = LocalContext.current

    // Categorize
    val filteredByCategory = remember(completedDownloads, selectedCategory) {
        when (selectedCategory) {
            "Videos" -> completedDownloads.filter { it.fileType == "video" }
            "Audio" -> completedDownloads.filter { it.fileType == "audio" }
            "Favorites" -> completedDownloads.filter { it.isFavorite }
            else -> completedDownloads
        }
    }

    // Filter by search query
    val filteredBySearch = remember(filteredByCategory, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            filteredByCategory
        } else {
            filteredByCategory.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Sort
    val sortedList = remember(filteredBySearch, sortBy) {
        when (sortBy) {
            "Date Newest" -> filteredBySearch.sortedByDescending { it.timestamp }
            "Date Oldest" -> filteredBySearch.sortedBy { it.timestamp }
            "Size Largest" -> filteredBySearch.sortedByDescending { it.fileSize }
            "A-Z" -> filteredBySearch.sortedBy { it.title.lowercase() }
            else -> filteredBySearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Filter header
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Downloads Library",
            color = VaultWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            modifier = Modifier.testTag("library_screen_header")
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search title, author...", color = VaultLightGray) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("library_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultBlue,
                    unfocusedBorderColor = VaultMediumGray,
                    focusedTextColor = VaultWhite,
                    unfocusedTextColor = VaultWhite
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = VaultLightGray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VaultLightGray)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(VaultDarkGray, RoundedCornerShape(28.dp))
                        .testTag("sort_menu_button")
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort library", tint = VaultWhite)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(VaultMediumGray)
                ) {
                    val sortOptions = listOf("Date Newest", "Date Oldest", "Size Largest", "A-Z")
                    sortOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, color = VaultWhite, fontSize = 13.sp) },
                            onClick = {
                                sortBy = opt
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortBy == opt) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = VaultEmerald)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Categories list Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All", "Videos", "Audio", "Favorites")
            categories.forEach { cat ->
                val selected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    color = if (selected) VaultEmerald else VaultDarkGray,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_tab_$cat")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (selected) Color.Black else VaultWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Downloads List
        if (sortedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.FolderOpen,
                        contentDescription = "Empty",
                        tint = VaultMediumGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results matching search" else "No downloaded files yet",
                        color = VaultLightGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Downloaded videos and audio tracks will appear here.",
                            color = VaultLightGray.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("library_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sortedList, key = { it.id }) { item ->
                    LibraryItemRow(
                        item = item,
                        onPlay = { onPlayMedia(item) },
                        onRename = {
                            itemToRename = item
                            renameInput = item.title
                        },
                        onDelete = { itemToDelete = item },
                        onToggleFav = { viewModel.toggleFavorite(item.id, !item.isFavorite) }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (itemToRename != null) {
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            containerColor = VaultDarkGray,
            title = { Text("Rename File", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        focusedBorderColor = VaultBlue
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToRename?.let {
                            if (renameInput.trim().isNotEmpty()) {
                                viewModel.renameDownload(it.id, renameInput.trim())
                                Toast.makeText(context, "File renamed successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        itemToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                ) {
                    Text("Save", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel", color = VaultLightGray)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = VaultDarkGray,
            title = { Text("Delete Download?", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${itemToDelete?.title}\" from your device storage?",
                    color = VaultWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let {
                            viewModel.deleteDownload(it)
                            Toast.makeText(context, "Deleted download!", Toast.LENGTH_SHORT).show()
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = VaultLightGray)
                }
            }
        )
    }
}

@Composable
fun LibraryItemRow(
    item: DownloadItem,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onToggleFav: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        onClick = onPlay,
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with type overlay
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 64.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Platform or format label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.quality,
                        color = if (item.fileType == "audio") VaultAmber else VaultEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = VaultWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = item.author,
                    color = VaultLightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.fileType == "audio") Icons.Default.MusicNote else Icons.Default.Movie,
                        contentDescription = null,
                        tint = VaultLightGray,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatDuration(item.duration)}  •  ${formatFileSize(item.fileSize)}",
                        color = VaultLightGray,
                        fontSize = 10.sp
                    )
                }
            }

            // Favorites Heart button
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (item.isFavorite) VaultRed else VaultLightGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Action menu
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Actions", tint = VaultLightGray)
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(VaultMediumGray)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Media", color = VaultWhite, fontSize = 13.sp) },
                        onClick = {
                            expandedMenu = false
                            onPlay()
                        },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = VaultWhite, fontSize = 13.sp) },
                        onClick = {
                            expandedMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = VaultBlue, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Link", color = VaultWhite, fontSize = 13.sp) },
                        onClick = {
                            expandedMenu = false
                            // Native share simulation
                            try {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Offline download via OpenDoor: ${item.title}\nSource: ${item.url}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Link shared!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = VaultAmber, modifier = Modifier.size(18.dp)) }
                    )
                    Divider(color = VaultDarkGray)
                    DropdownMenuItem(
                        text = { Text("Delete File", color = VaultRed, fontSize = 13.sp) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = VaultRed, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}
