package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerRole
import com.example.engine.BallOutcomeType
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BoundarySky
import com.example.ui.theme.BroadcastMuted
import com.example.ui.theme.BroadcastSilver
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.NavySurfaceHighest
import com.example.ui.theme.RoleAllRounderColor
import com.example.ui.theme.RoleBatsmanColor
import com.example.ui.theme.RoleSeamerColor
import com.example.ui.theme.RoleSpinnerColor
import com.example.ui.theme.RoleWicketkeeperColor
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

fun roleAccentColor(role: PlayerRole): Color = when (role) {
    PlayerRole.BATTER -> RoleBatsmanColor
    PlayerRole.SEAMER -> RoleSeamerColor
    PlayerRole.SPINNER -> RoleSpinnerColor
    PlayerRole.ALL_ROUNDER -> RoleAllRounderColor
    PlayerRole.WICKETKEEPER -> RoleWicketkeeperColor
}

@Composable
fun RolePillBadge(role: PlayerRole, modifier: Modifier = Modifier) {
    val accent = roleAccentColor(role)
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Text(
            text = role.shortLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accent
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun OverseasBadge(overseas: Boolean, countryCode: String, modifier: Modifier = Modifier) {
    val accent = if (overseas) AuctionGold else TurfEmerald
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = if (overseas) "$countryCode • OS" else countryCode,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accent
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EraTagChip(eraTag: String, modifier: Modifier = Modifier) {
    if (eraTag.isBlank()) return
    Surface(
        modifier = modifier,
        color = NavySurfaceHighest,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NavyOutline)
    ) {
        Text(
            text = eraTag,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = BroadcastSilver
            ),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun RatingMeterBar(
    label: String,
    rating: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (rating / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 380),
        label = "rating_$label"
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BroadcastMuted
            )
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(NavySurfaceHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.65f), accentColor)
                        )
                    )
            )
        }
    }
}

@Composable
fun AuctionCountdownRing(
    secondsLeft: Int,
    progress: Float,
    size: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val ringColor = when {
        secondsLeft <= 3 -> WicketCrimson
        secondsLeft <= 6 -> AuctionGold
        else -> TurfEmerald
    }
    val smoothProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 140),
        label = "countdown_arc"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = NavySurfaceHighest,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * smoothProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${secondsLeft}s",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            )
        }
    }
}

@Composable
fun BallOutcomeChip(outcome: BallOutcomeType, modifier: Modifier = Modifier) {
    val (label, bg, fg) = when (outcome) {
        BallOutcomeType.SIX -> Triple("6", AuctionGold, Color(0xFF1A1200))
        BallOutcomeType.FOUR -> Triple("4", BoundarySky, Color(0xFF041C2C))
        BallOutcomeType.WICKET -> Triple("W", WicketCrimson, BroadcastWhite)
        BallOutcomeType.WIDE -> Triple("Wd", NavySurfaceHighest, AuctionGold)
        BallOutcomeType.NO_BALL -> Triple("Nb", NavySurfaceHighest, AuctionGold)
        BallOutcomeType.DOT -> Triple("•", NavySurfaceHighest, BroadcastMuted)
        BallOutcomeType.SINGLE -> Triple("1", TurfEmerald.copy(alpha = 0.22f), TurfEmerald)
        BallOutcomeType.DOUBLE -> Triple("2", TurfEmerald.copy(alpha = 0.30f), TurfEmerald)
        BallOutcomeType.TRIPLE -> Triple("3", TurfEmerald.copy(alpha = 0.38f), TurfEmerald)
    }
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = fg,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun RunRateWormGraph(
    teamAName: String,
    teamBName: String,
    innings1Worm: List<Int>,
    innings2Worm: List<Int>,
    innings1WicketOvers: List<Int>,
    innings2WicketOvers: List<Int>,
    maxOvers: Int = 20,
    modifier: Modifier = Modifier
) {
    val safeMaxOvers = maxOvers.coerceAtLeast(10)
    val maxRuns = maxOf(
        innings1Worm.maxOrNull() ?: 0,
        innings2Worm.maxOrNull() ?: 0,
        if (safeMaxOvers >= 50) 120 else 60
    )
    val yCeiling = ((maxRuns + 29) / 30) * 30 // Round up to clean multiple of 30

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NavySurfaceHigh)
            .border(1.dp, NavyOutline, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUN-RATE WORM GRAPH",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DisplayFontFamily,
                    letterSpacing = 1.sp,
                    color = BroadcastWhite
                )
            )
            Text(
                text = "OVERS 1–$safeMaxOvers",
                style = MaterialTheme.typography.labelSmall,
                color = BroadcastMuted
            )
        }

        Spacer(Modifier.height(10.dp))

        // Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = TurfEmerald, label = teamAName)
            LegendDot(color = AuctionGold, label = teamBName)
            LegendDot(color = WicketCrimson, label = "Wicket")
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val leftPad = 36.dp.toPx()
                val rightPad = 12.dp.toPx()
                val topPad = 10.dp.toPx()
                val bottomPad = 24.dp.toPx()
                val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                fun xForOver(over: Int): Float = leftPad + (over.coerceAtMost(safeMaxOvers) / safeMaxOvers.toFloat()) * chartW
                fun yForRuns(runs: Int): Float = topPad + chartH - ((runs.toFloat() / yCeiling) * chartH)

                // Horizontal grid lines (4 intervals)
                val steps = 4
                for (i in 0..steps) {
                    val y = topPad + (chartH / steps) * i
                    drawLine(
                        color = NavyOutline.copy(alpha = 0.55f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Vertical format-specific phase markers
                val phaseOvers = when (safeMaxOvers) {
                    20 -> listOf(6, 10, 15, 20)
                    50 -> listOf(10, 25, 40, 50)
                    else -> listOf(safeMaxOvers / 4, safeMaxOvers / 2, (safeMaxOvers * 3) / 4, safeMaxOvers)
                }
                phaseOvers.forEach { ov ->
                    val x = xForOver(ov)
                    drawLine(
                        color = NavyOutline.copy(alpha = 0.4f),
                        start = Offset(x, topPad),
                        end = Offset(x, topPad + chartH),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Innings 1 worm
                if (innings1Worm.isNotEmpty()) {
                    val path1 = Path().apply {
                        moveTo(xForOver(0), yForRuns(0))
                        innings1Worm.forEachIndexed { idx, runs ->
                            lineTo(xForOver(idx + 1), yForRuns(runs))
                        }
                    }
                    drawPath(
                        path = path1,
                        color = TurfEmerald,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    innings1WicketOvers.forEach { ov ->
                        val idx = (ov - 1).coerceIn(0, innings1Worm.lastIndex)
                        val pt = Offset(xForOver(idx + 1), yForRuns(innings1Worm[idx]))
                        drawCircle(color = WicketCrimson, radius = 4.5.dp.toPx(), center = pt)
                        drawCircle(color = BroadcastWhite, radius = 2.dp.toPx(), center = pt)
                    }
                }

                // Draw Innings 2 worm
                if (innings2Worm.isNotEmpty()) {
                    val path2 = Path().apply {
                        moveTo(xForOver(0), yForRuns(0))
                        innings2Worm.forEachIndexed { idx, runs ->
                            lineTo(xForOver(idx + 1), yForRuns(runs))
                        }
                    }
                    drawPath(
                        path = path2,
                        color = AuctionGold,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    innings2WicketOvers.forEach { ov ->
                        val idx = (ov - 1).coerceIn(0, innings2Worm.lastIndex)
                        val pt = Offset(xForOver(idx + 1), yForRuns(innings2Worm[idx]))
                        drawCircle(color = WicketCrimson, radius = 4.5.dp.toPx(), center = pt)
                        drawCircle(color = BroadcastWhite, radius = 2.dp.toPx(), center = pt)
                    }
                }
            }

            // Y-axis labels overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(186.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$yCeiling", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${(yCeiling * 3) / 4}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${yCeiling / 2}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${yCeiling / 4}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("0", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
            }

            // X-axis over labels overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 34.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${safeMaxOvers / 4}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${safeMaxOvers / 2}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("${(safeMaxOvers * 3) / 4}", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                Text("$safeMaxOvers ov", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BroadcastSilver,
            maxLines = 1
        )
    }
}
