package com.example.streambox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamboxTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Netflix & Disney+ mostly use Dark themes for TV
    val colorScheme = darkColorScheme(
        primary = NetflixRed,
        onPrimary = White,
        secondary = DisneyBlue,
        onSecondary = White,
        surface = NetflixBlack,
        onSurface = White,
        background = NetflixDarkGray,
        onBackground = White,
        surfaceVariant = NetflixLightGray,
        onSurfaceVariant = Gray
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
