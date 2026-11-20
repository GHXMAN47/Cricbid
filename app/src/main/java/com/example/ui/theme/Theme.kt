package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ColorOutlineSubtle = androidx.compose.ui.graphics.Color(0xFF1F334D)

private val CricBidDarkColorScheme = darkColorScheme(
    primary = TurfEmerald,
    onPrimary = OnEmeraldDark,
    primaryContainer = TurfEmeraldContainer,
    onPrimaryContainer = BroadcastWhite,
    secondary = AuctionGold,
    onSecondary = OnGoldDark,
    secondaryContainer = AuctionGoldContainer,
    onSecondaryContainer = AuctionGoldBright,
    tertiary = BoundarySky,
    onTertiary = NavyMidnight,
    tertiaryContainer = BoundarySkyContainer,
    onTertiaryContainer = BroadcastWhite,
    background = NavyMidnight,
    onBackground = BroadcastWhite,
    surface = NavySurface,
    onSurface = BroadcastWhite,
    surfaceVariant = NavySurfaceHigh,
    onSurfaceVariant = BroadcastSilver,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavySurfaceHigh,
    surfaceContainerHigh = NavySurfaceHighest,
    outline = NavyOutline,
    outlineVariant = ColorOutlineSubtle,
    error = WicketCrimson,
    onError = BroadcastWhite,
    errorContainer = WicketCrimsonContainer,
    onErrorContainer = BroadcastWhite,
)

private val CricBidShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CricBidTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CricBidDarkColorScheme,
        typography = Typography,
        shapes = CricBidShapes,
        content = content
    )
}
