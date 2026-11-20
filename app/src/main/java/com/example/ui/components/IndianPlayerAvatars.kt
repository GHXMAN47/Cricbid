package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily

data class IndianPlayerAvatarSpec(
    val id: String,
    val playerName: String,
    val shortName: String,
    val jerseyNumber: String,
    val nickname: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val skinTone: Color,
    val hasBeard: Boolean,
    val badgeSymbol: String
)

object IndianPlayerAvatars {
    const val DEFAULT_ID = "vk18"

    val all: List<IndianPlayerAvatarSpec> = listOf(
        IndianPlayerAvatarSpec(
            id = "vk18",
            playerName = "Virat Kohli",
            shortName = "Kohli",
            jerseyNumber = "18",
            nickname = "King Kohli",
            primaryColor = Color(0xFF0A4DA2),
            secondaryColor = Color(0xFF052452),
            accentColor = Color(0xFFFFC107),
            skinTone = Color(0xFFD5A078),
            hasBeard = true,
            badgeSymbol = "👑"
        ),
        IndianPlayerAvatarSpec(
            id = "rs45",
            playerName = "Rohit Sharma",
            shortName = "Rohit",
            jerseyNumber = "45",
            nickname = "The Hitman",
            primaryColor = Color(0xFF0072CE),
            secondaryColor = Color(0xFF003262),
            accentColor = Color(0xFFFF9800),
            skinTone = Color(0xFFD09770),
            hasBeard = true,
            badgeSymbol = "💥"
        ),
        IndianPlayerAvatarSpec(
            id = "msd7",
            playerName = "MS Dhoni",
            shortName = "Dhoni",
            jerseyNumber = "7",
            nickname = "Captain Cool",
            primaryColor = Color(0xFFE6A800),
            secondaryColor = Color(0xFF102A63),
            accentColor = Color(0xFFFFEB3B),
            skinTone = Color(0xFFC98E64),
            hasBeard = false,
            badgeSymbol = "⭐"
        ),
        IndianPlayerAvatarSpec(
            id = "jb93",
            playerName = "Jasprit Bumrah",
            shortName = "Bumrah",
            jerseyNumber = "93",
            nickname = "Yorker King",
            primaryColor = Color(0xFF00838F),
            secondaryColor = Color(0xFF06253A),
            accentColor = Color(0xFF26E8C8),
            skinTone = Color(0xFFCB9168),
            hasBeard = true,
            badgeSymbol = "⚡"
        ),
        IndianPlayerAvatarSpec(
            id = "sg77",
            playerName = "Shubman Gill",
            shortName = "Gill",
            jerseyNumber = "77",
            nickname = "The Prince",
            primaryColor = Color(0xFF1565C0),
            secondaryColor = Color(0xFF0B1F47),
            accentColor = Color(0xFF4DD0E1),
            skinTone = Color(0xFFDDB08A),
            hasBeard = false,
            badgeSymbol = "🏏"
        ),
        IndianPlayerAvatarSpec(
            id = "hp33",
            playerName = "Hardik Pandya",
            shortName = "Hardik",
            jerseyNumber = "33",
            nickname = "Clutch AR",
            primaryColor = Color(0xFF1D4ED8),
            secondaryColor = Color(0xFF111827),
            accentColor = Color(0xFFFF6D00),
            skinTone = Color(0xFFB57C56),
            hasBeard = true,
            badgeSymbol = "🔥"
        ),
        IndianPlayerAvatarSpec(
            id = "rj8",
            playerName = "Ravindra Jadeja",
            shortName = "Jadeja",
            jerseyNumber = "8",
            nickname = "Sir Jadeja",
            primaryColor = Color(0xFF00695C),
            secondaryColor = Color(0xFF072B24),
            accentColor = Color(0xFFFFD54F),
            skinTone = Color(0xFFC78D64),
            hasBeard = true,
            badgeSymbol = "⚔️"
        ),
        IndianPlayerAvatarSpec(
            id = "sky63",
            playerName = "Suryakumar Yadav",
            shortName = "SKY",
            jerseyNumber = "63",
            nickname = "Mr. 360°",
            primaryColor = Color(0xFF0288D1),
            secondaryColor = Color(0xFF0A1E3D),
            accentColor = Color(0xFFFF4081),
            skinTone = Color(0xFFC88F67),
            hasBeard = true,
            badgeSymbol = "🎯"
        ),
        IndianPlayerAvatarSpec(
            id = "rp17",
            playerName = "Rishabh Pant",
            shortName = "Pant",
            jerseyNumber = "17",
            nickname = "Spidey WK",
            primaryColor = Color(0xFF283593),
            secondaryColor = Color(0xFF0D1438),
            accentColor = Color(0xFFFF5252),
            skinTone = Color(0xFFD6A47E),
            hasBeard = false,
            badgeSymbol = "🧤"
        ),
        IndianPlayerAvatarSpec(
            id = "srt10",
            playerName = "Sachin Tendulkar",
            shortName = "Sachin",
            jerseyNumber = "10",
            nickname = "Master Blaster",
            primaryColor = Color(0xFF1E88E5),
            secondaryColor = Color(0xFF0B3C78),
            accentColor = Color(0xFFFFD700),
            skinTone = Color(0xFFD39D75),
            hasBeard = false,
            badgeSymbol = "🏆"
        ),
        IndianPlayerAvatarSpec(
            id = "ys12",
            playerName = "Yuvraj Singh",
            shortName = "Yuvraj",
            jerseyNumber = "12",
            nickname = "Sixer King",
            primaryColor = Color(0xFF0D47A1),
            secondaryColor = Color(0xFF071B3E),
            accentColor = Color(0xFFFF7043),
            skinTone = Color(0xFFD4A27C),
            hasBeard = true,
            badgeSymbol = "6️⃣"
        ),
        IndianPlayerAvatarSpec(
            id = "kd83",
            playerName = "Kapil Dev",
            shortName = "Kapil Dev",
            jerseyNumber = "83",
            nickname = "83 WC Legend",
            primaryColor = Color(0xFF37474F),
            secondaryColor = Color(0xFF102027),
            accentColor = Color(0xFFFFCA28),
            skinTone = Color(0xFFC68A62),
            hasBeard = true,
            badgeSymbol = "🏅"
        )
    )

    fun byId(id: String?): IndianPlayerAvatarSpec =
        all.firstOrNull { it.id == id } ?: all.first()
}

/**
 * Lightweight, vector-drawn Indian Cricket Player Avatar portrait badge.
 * Uses pure Compose Canvas (zero external URLs, zero heavy bitmaps, zero broken image risk).
 */
@Composable
fun IndianPlayerAvatar(
    avatarId: String?,
    size: Dp = 56.dp,
    selected: Boolean = false,
    showJerseyPill: Boolean = true,
    modifier: Modifier = Modifier
) {
    val spec = IndianPlayerAvatars.byId(avatarId)
    val borderColor = if (selected) spec.accentColor else spec.accentColor.copy(alpha = 0.55f)
    val borderWidth = if (selected) 2.5.dp else 1.5.dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(spec.primaryColor, spec.secondaryColor)
                    )
                )
                .border(borderWidth, borderColor, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                // Subtle stadium spotlight ring
                drawCircle(
                    color = spec.accentColor.copy(alpha = 0.14f),
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.34f)
                )

                // Jersey shoulders silhouette
                val shoulderPath = Path().apply {
                    moveTo(w * 0.10f, h * 1.02f)
                    quadraticTo(w * 0.22f, h * 0.68f, w * 0.50f, h * 0.66f)
                    quadraticTo(w * 0.78f, h * 0.68f, w * 0.90f, h * 1.02f)
                    close()
                }
                drawPath(path = shoulderPath, color = spec.primaryColor)

                // Tricolor / Accent collar V-stripe
                val collarPath = Path().apply {
                    moveTo(w * 0.38f, h * 0.68f)
                    lineTo(w * 0.50f, h * 0.80f)
                    lineTo(w * 0.62f, h * 0.68f)
                }
                drawPath(
                    path = collarPath,
                    color = spec.accentColor,
                    style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
                )

                // Neck
                drawRect(
                    color = spec.skinTone.copy(alpha = 0.92f),
                    topLeft = Offset(w * 0.43f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.14f)
                )

                // Stylized cricketer head
                drawCircle(
                    color = spec.skinTone,
                    radius = w * 0.19f,
                    center = Offset(w * 0.50f, h * 0.41f)
                )

                // Hair / Cap crest
                val hairPath = Path().apply {
                    moveTo(w * 0.31f, h * 0.38f)
                    quadraticTo(w * 0.34f, h * 0.18f, w * 0.50f, h * 0.19f)
                    quadraticTo(w * 0.66f, h * 0.18f, w * 0.69f, h * 0.38f)
                    quadraticTo(w * 0.58f, h * 0.28f, w * 0.50f, h * 0.30f)
                    quadraticTo(w * 0.42f, h * 0.28f, w * 0.31f, h * 0.38f)
                    close()
                }
                drawPath(path = hairPath, color = Color(0xFF1A1A24))

                // Optional iconic beard / moustache accent
                if (spec.hasBeard) {
                    val beardPath = Path().apply {
                        moveTo(w * 0.35f, h * 0.44f)
                        quadraticTo(w * 0.50f, h * 0.64f, w * 0.65f, h * 0.44f)
                        quadraticTo(w * 0.50f, h * 0.53f, w * 0.35f, h * 0.44f)
                        close()
                    }
                    drawPath(path = beardPath, color = Color(0xFF1F1E28).copy(alpha = 0.85f))
                }
            }

            // Jersey number badge centered on chest
            if (showJerseyPill && size >= 44.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (size.value * 0.06f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC060D1C))
                        .border(0.8.dp, spec.accentColor.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "#${spec.jerseyNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (size >= 64.dp) 10.sp else 8.sp,
                            color = spec.accentColor
                        )
                    )
                }
            }
        }

        // Top-right mini emblem for larger avatars
        if (size >= 60.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A1426))
                    .border(1.dp, spec.accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = spec.badgeSymbol,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun IndianPlayerAvatarLabel(
    avatarId: String?,
    modifier: Modifier = Modifier
) {
    val spec = IndianPlayerAvatars.byId(avatarId)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = spec.shortName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = DisplayFontFamily,
                color = BroadcastWhite
            )
        )
    }
}
