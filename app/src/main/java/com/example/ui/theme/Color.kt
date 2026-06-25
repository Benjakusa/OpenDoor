package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dynamic Theme State for Sleek Interface
object ThemeState {
    var isDark: Boolean = false
}

// Sleek Interface Color Palette (Dynamic Getters)
val VaultBlack: Color
    get() = if (ThemeState.isDark) Color(0xFF141218) else Color(0xFFFDF8FD)

val VaultDarkGray: Color
    get() = if (ThemeState.isDark) Color(0xFF1D1B20) else Color(0xFFF3EDF7)

val VaultMediumGray: Color
    get() = if (ThemeState.isDark) Color(0xFF49454F) else Color(0xFFE7E0EB)

val VaultLightGray: Color
    get() = if (ThemeState.isDark) Color(0xFFCAC4D0) else Color(0xFF49454F)

val VaultWhite: Color
    get() = if (ThemeState.isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

// Primary Accent
val VaultBlue: Color
    get() = if (ThemeState.isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4)

// Functional UI Colors
val VaultEmerald = Color(0xFF00875A) // Sleek green for active downloads
val VaultAmber = Color(0xFFF59E0B)   // Amber for warnings/favorites/premium
val VaultRed = Color(0xFFB3261E)     // Sleek red for errors/actions
