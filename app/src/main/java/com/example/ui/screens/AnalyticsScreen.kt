package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadItem
import com.example.ui.theme.*
import com.example.viewmodel.AdminUser
import com.example.viewmodel.DownloadViewModel

@Composable
fun AnalyticsScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        StorageManagementTab(viewModel)
    }
}

@Composable
fun StorageManagementTab(viewModel: DownloadViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    val appUsedBytes = remember(downloads) { viewModel.getDownloadedFilesSizeSum() }
    val systemUsedBytes = 45 * 1024 * 1024 * 1024L
    val totalBytes = viewModel.totalStorageBytes
    val freeBytes = (totalBytes - systemUsedBytes - appUsedBytes).coerceAtLeast(0)

    val recs = remember(downloads) { viewModel.getStorageRecommendations() }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Storage Visualizer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Device Storage Overview",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-segmented bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(VaultMediumGray)
                    ) {
                        val sysFrac = systemUsedBytes.toFloat() / totalBytes.toFloat()
                        val appFrac = appUsedBytes.toFloat() / totalBytes.toFloat()

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(sysFrac)
                                .background(VaultLightGray)
                        )
                        if (appFrac > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(appFrac)
                                    .background(VaultEmerald)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((1f - sysFrac - appFrac).coerceAtLeast(0.01f))
                                .background(VaultMediumGray)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StorageLegendItem(color = VaultLightGray, name = "Other & System", sizeStr = formatFileSize(systemUsedBytes))
                        StorageLegendItem(color = VaultEmerald, name = "OpenDoor Files", sizeStr = formatFileSize(appUsedBytes))
                        StorageLegendItem(color = VaultMediumGray, name = "Available Free", sizeStr = formatFileSize(freeBytes))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = VaultMediumGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vault Footprint", color = VaultLightGray, fontSize = 11.sp)
                            Text(formatFileSize(appUsedBytes), color = VaultEmerald, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.clearAllCompletedDownloads()
                                Toast.makeText(context, "Cleared all finished downloads!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultRed.copy(alpha = 0.15f), contentColor = VaultRed),
                            shape = RoundedCornerShape(8.dp),
                            enabled = appUsedBytes > 0
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clean Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Notification Alerts panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notification Rules",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationOptionRow(title = "Download Started alert", desc = "Alert when background downloading starts")
                    NotificationOptionRow(title = "Download Complete status", desc = "Vibrate and alert when files are ready")
                    NotificationOptionRow(title = "Storage space warning", desc = "Warn if system free space falls below 2 GB")
                }
            }
        }

        // Cleanup Recommendations
        item {
            Text(
                text = "Cleanup Suggestions",
                color = VaultWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (recs.isEmpty()) {
            item {
                Surface(
                    color = VaultDarkGray,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your vault has no completed downloads to clean.",
                            color = VaultLightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recs, key = { it.id }) { item ->
                CleanupItemRow(item, onDelete = {
                    viewModel.deleteDownload(item)
                    Toast.makeText(context, "Deleted download!", Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, name: String, sizeStr: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(name, color = VaultLightGray, fontSize = 9.sp)
            Text(sizeStr, color = VaultWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationOptionRow(title: String, desc: String) {
    var checked by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = VaultWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = VaultLightGray, fontSize = 11.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = VaultEmerald,
                uncheckedTrackColor = VaultMediumGray
            )
        )
    }
}

@Composable
fun CleanupItemRow(item: DownloadItem, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "${item.platform}  •  ${formatDuration(item.duration)}",
                    color = VaultLightGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatFileSize(item.fileSize),
                    color = VaultRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Large File",
                    color = VaultLightGray,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultRed)
            }
        }
    }
}

// Admin Portal Tab
@Composable
fun AdminPortalTab(viewModel: DownloadViewModel) {
    var passwordInput by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (!isAuthenticated) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(VaultBlue.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = VaultBlue, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Secure Admin Portal",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Authorized access only. Enter password below to view system statistics and user lists.",
                        color = VaultLightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Enter Admin Password", color = VaultLightGray) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultBlue,
                            unfocusedBorderColor = VaultMediumGray,
                            focusedTextColor = VaultWhite,
                            unfocusedTextColor = VaultWhite
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (passwordInput == "admin123") {
                                    isAuthenticated = true
                                    passwordInput = ""
                                } else {
                                    Toast.makeText(context, "Invalid credentials. Hint: admin123", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (passwordInput == "admin123") {
                                isAuthenticated = true
                                passwordInput = ""
                            } else {
                                Toast.makeText(context, "Invalid credentials. Hint: admin123", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultBlue),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_submit")
                    ) {
                        Text("Authenticate Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Default Credentials: admin123",
                        color = VaultLightGray.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    } else {
        AdminDashboardConsole(viewModel, onLogout = { isAuthenticated = false })
    }
}

@Composable
fun AdminDashboardConsole(viewModel: DownloadViewModel, onLogout: () -> Unit) {
    val adminUsers by viewModel.adminUsers.collectAsState()
    val totalDownloadsCount by viewModel.totalSimulatedDownloads.collectAsState()
    val adEnabled by viewModel.adSettingsEnabled.collectAsState()
    val currentAnnouncement by viewModel.appAnnouncement.collectAsState()

    var editingAnnouncement by remember { mutableStateOf(currentAnnouncement) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_dashboard_console"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Logout & Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(VaultEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Secure Connection Active", color = VaultEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = VaultRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exit Admin", color = VaultRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Stats Badges Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminStatBadge(
                    icon = Icons.Default.People,
                    color = VaultBlue,
                    title = "Total Users",
                    value = "5,420",
                    modifier = Modifier.weight(1f)
                )
                AdminStatBadge(
                    icon = Icons.Default.CloudDownload,
                    color = VaultEmerald,
                    title = "Downloads",
                    value = totalDownloadsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Platform popularity chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Platform Download Statistics",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    PlatformUsageBar(name = "YouTube", percentage = 0.45f, color = Color(0xFFFF0000))
                    PlatformUsageBar(name = "TikTok", percentage = 0.22f, color = VaultWhite)
                    PlatformUsageBar(name = "Instagram", percentage = 0.18f, color = Color(0xFFE1306C))
                    PlatformUsageBar(name = "Vimeo", percentage = 0.10f, color = Color(0xFF1AB7EA))
                    PlatformUsageBar(name = "Facebook", percentage = 0.05f, color = Color(0xFF1877F2))
                }
            }
        }

        // App Configuration & Ads Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VaultMediumGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Administration",
                        color = VaultWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Integrate Advertisements", color = VaultWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Toggle ads on search & selection screen", color = VaultLightGray, fontSize = 11.sp)
                        }

                        Switch(
                            checked = adEnabled,
                            onCheckedChange = { viewModel.updateAdSettings(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VaultEmerald
                            ),
                            modifier = Modifier.testTag("admin_ad_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = VaultMediumGray)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Publish System Announcement", color = VaultWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Updates global notifications banner in Home", color = VaultLightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editingAnnouncement,
                        onValueChange = { editingAnnouncement = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_announcement_input"),
                        textStyle = TextStyle(fontSize = 12.sp, color = VaultWhite),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultBlue,
                            unfocusedBorderColor = VaultMediumGray
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.updateAnnouncement(editingAnnouncement)
                            Toast.makeText(context, "Global announcement published!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("admin_announcement_publish")
                    ) {
                        Text("Publish Announcements", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // User Management Table
        item {
            Text(
                text = "Registered Accounts Management (${adminUsers.size})",
                color = VaultWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(adminUsers, key = { it.id }) { user ->
            AdminUserRow(user, viewModel)
        }
    }
}

@Composable
fun AdminStatBadge(
    icon: Any,
    color: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(title, color = VaultLightGray, fontSize = 10.sp)
                Text(value, color = VaultWhite, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun PlatformUsageBar(name: String, percentage: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = VaultWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${(percentage * 100).toInt()}%", color = VaultLightGray, fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VaultMediumGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun AdminUserRow(user: AdminUser, viewModel: DownloadViewModel) {
    var showSubscriptionDropdown by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkGray),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, VaultMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_${user.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Name + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(user.name, color = VaultWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(user.email, color = VaultLightGray, fontSize = 11.sp)
                }

                Surface(
                    color = if (user.status == "Active") VaultEmerald.copy(alpha = 0.15f) else VaultRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = user.status,
                        color = if (user.status == "Active") VaultEmerald else VaultRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info rows: Join Date & Sub level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tier: ${user.subscription}  •  Joined: ${user.joinDate}",
                    color = VaultLightGray,
                    fontSize = 11.sp
                )

                // Quick control actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Subscription adjustment
                    Box {
                        Surface(
                            onClick = { showSubscriptionDropdown = true },
                            color = VaultMediumGray,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Plan ▾",
                                color = VaultWhite,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSubscriptionDropdown,
                            onDismissRequest = { showSubscriptionDropdown = false },
                            modifier = Modifier.background(VaultMediumGray)
                        ) {
                            val tiers = listOf("Free", "Premium", "Ad-Free")
                            tiers.forEach { tier ->
                                DropdownMenuItem(
                                    text = { Text(tier, color = VaultWhite, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.changeUserSubscription(user.id, tier)
                                        showSubscriptionDropdown = false
                                        Toast.makeText(context, "${user.name} upgraded to $tier!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Suspend / Activate toggle
                    Surface(
                        onClick = {
                            if (user.status == "Active") {
                                viewModel.suspendAdminUser(user.id)
                                Toast.makeText(context, "${user.name} suspended", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.activateAdminUser(user.id)
                                Toast.makeText(context, "${user.name} activated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        color = if (user.status == "Active") VaultRed.copy(alpha = 0.15f) else VaultEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (user.status == "Active") "Suspend" else "Activate",
                            color = if (user.status == "Active") VaultRed else VaultEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Delete User account
                    IconButton(
                        onClick = {
                            viewModel.deleteAdminUser(user.id)
                            Toast.makeText(context, "User account deleted", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete user", tint = VaultRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
