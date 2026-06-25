package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Announcement
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.DownloadItem
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.*
import com.example.viewmodel.DownloadViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoVaultApp(
    viewModel: DownloadViewModel = viewModel()
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var showOnboarding by rememberSaveable { mutableStateOf(true) }
    var activeTab by rememberSaveable { mutableStateOf("Downloader") } // "Downloader", "Library", "Analytics"
    var activePlayMedia by remember { mutableStateOf<DownloadItem?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val darkTheme by viewModel.isDarkTheme.collectAsState()
    val announcement by viewModel.appAnnouncement.collectAsState()

    val context = LocalContext.current

    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = false) {
        if (showSplash) {
            SplashScreen(onFinish = { showSplash = false })
        } else if (showOnboarding) {
            OnboardingScreen(
                onFinish = { showOnboarding = false }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                             title = {
                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                     Box(
                                         modifier = Modifier
                                             .size(40.dp)
                                             .background(VaultBlue, RoundedCornerShape(12.dp)),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Icon(
                                             painter = painterResource(id = R.drawable.ic_open_door),
                                             contentDescription = null,
                                             tint = Color.White,
                                             modifier = Modifier.size(24.dp)
                                         )
                                     }
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Text(
                                         "OpenDoor",
                                         fontWeight = FontWeight.ExtraBold,
                                         fontSize = 20.sp,
                                         color = MaterialTheme.colorScheme.onBackground
                                     )
                                    Surface(
                                        color = VaultBlue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            "PRO",
                                            color = VaultBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            actions = {
                                // Manual Theme Toggle Icon
                                IconButton(onClick = { viewModel.toggleTheme() }) {
                                    Icon(
                                        imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme",
                                        tint = if (darkTheme) VaultAmber else VaultLightGray
                                    )
                                }

                                // Interactive Profile / Account Dialog Trigger
                                IconButton(
                                    onClick = { showProfileDialog = true },
                                    modifier = Modifier.testTag("profile_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(VaultMediumGray, CircleShape)
                                            .border(1.dp, VaultBlue, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = VaultBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = VaultDarkGray,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "Downloader",
                            onClick = { activeTab = "Downloader" },
                            label = { Text("Downloader", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Downloading, contentDescription = "Downloader") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultBlue,
                                selectedTextColor = VaultBlue,
                                indicatorColor = VaultMediumGray,
                                unselectedIconColor = VaultLightGray,
                                unselectedTextColor = VaultLightGray
                            ),
                            modifier = Modifier.testTag("nav_tab_downloader")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Library",
                            onClick = { activeTab = "Library" },
                            label = { Text("My Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = "Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultBlue,
                                selectedTextColor = VaultBlue,
                                indicatorColor = VaultMediumGray,
                                unselectedIconColor = VaultLightGray,
                                unselectedTextColor = VaultLightGray
                            ),
                            modifier = Modifier.testTag("nav_tab_library")
                        )

                        NavigationBarItem(
                            selected = activeTab == "Analytics",
                            onClick = { activeTab = "Analytics" },
                            label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Analytics & Admin") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultBlue,
                                selectedTextColor = VaultBlue,
                                indicatorColor = VaultMediumGray,
                                unselectedIconColor = VaultLightGray,
                                unselectedTextColor = VaultLightGray
                            ),
                            modifier = Modifier.testTag("nav_tab_analytics")
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "MainTabsNavigation"
                    ) { tab ->
                        when (tab) {
                            "Downloader" -> DownloaderScreen(viewModel)
                            "Library" -> LibraryScreen(
                                viewModel = viewModel,
                                onPlayMedia = { item -> activePlayMedia = item }
                            )
                            "Analytics" -> AnalyticsScreen(viewModel)
                        }
                    }
                }
            }
        }

        // Integrated Media Player Bottom Sheet
        if (activePlayMedia != null) {
            MediaPlayerBottomSheet(
                downloadItem = activePlayMedia!!,
                onDismiss = { activePlayMedia = null }
            )
        }

        // Profile dialog (Google Accounts, sign out, subscription details)
        if (showProfileDialog) {
            UserProfileDialog(
                onDismiss = { showProfileDialog = false },
                onLogout = {
                    showProfileDialog = false
                    showOnboarding = true
                    Toast.makeText(context, "Signed out of OpenDoor", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(8000)
        onFinish()
    }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(VaultBlue.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, VaultBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_open_door),
                    contentDescription = null,
                    tint = VaultBlue,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "OpenDoor",
                color = VaultWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = VaultBlue,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Immersive decorative background logo circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(VaultBlue.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, VaultBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_open_door),
                    contentDescription = null,
                    tint = VaultBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OpenDoor",
                color = VaultWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Extract videos and audio files in maximum resolutions. Play your files offline in our built-in customized media player.",
                color = VaultLightGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
            )

            // Dynamic feature cards list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingFeatureRow(icon = Icons.Default.OfflineBolt, title = "High-Speed Offline Extractor")
                OnboardingFeatureRow(icon = Icons.Default.Storage, title = "Smart Device Storage Cleanups")
                OnboardingFeatureRow(icon = Icons.Default.MusicVideo, title = "Built-in Media Controller Dashboard")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = VaultBlue),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_start_button")
            ) {
                Text(
                    text = "Open the Door",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultMediumGray.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .border(1.dp, VaultMediumGray.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = VaultBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = VaultWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun UserProfileDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    var userSubscriptionState by remember { mutableStateOf("Gold VIP Plan") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkGray,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = VaultBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("My Account", color = VaultWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .background(VaultBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = VaultBlue, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("benjakusa@gmail.com", color = VaultWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Google Sign-In Account", color = VaultLightGray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = VaultMediumGray)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Membership details", color = VaultWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultMediumGray, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tier", color = VaultLightGray, fontSize = 9.sp)
                        Text(userSubscriptionState, color = VaultAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            userSubscriptionState = "Platinum Supreme Pro"
                            Toast.makeText(context, "Upgraded plan!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAmber, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Upgrade", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = VaultRed),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Logout Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultLightGray, fontSize = 12.sp)
            }
        }
    )
}
