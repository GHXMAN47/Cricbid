package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BallOutcomeType
import com.example.engine.BatterScoreEntry
import com.example.engine.BowlerScoreEntry
import com.example.engine.PitchMapPoint
import com.example.engine.ShotTelemetry
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BoundarySky
import com.example.ui.theme.BroadcastMuted
import com.example.ui.theme.BroadcastSilver
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clean Wagon Wheel shot-direction chart rendered with lightweight Compose Canvas.
 */
@Composable
fun WagonWheelChartCard(
    shots: List<ShotTelemetry>,
    batters: List<BatterScoreEntry>,
    teamName: String,
    modifier: Modifier = Modifier
) {
    var selectedBatterId by remember(teamName) { mutableStateOf<String?>(null) }
    val filteredShots = remember(shots, selectedBatterId) {
        if (selectedBatterId == null) shots else shots.filter { it.batterId == selectedBatterId }
    }
    val activeBatters = remember(batters) { batters.filter { it.balls > 0 || it.runs > 0 } }

    val fours = filteredShots.count { it.runs == 4 }
    val sixes = filteredShots.count { it.runs >= 6 }
    val singlesTwos = filteredShots.count { it.runs in 1..3 }
    val totalRuns = filteredShots.sumOf { it.runs }
    val offSideRuns = filteredShots.filter { it.angleDeg < 0f }.sumOf { it.runs }
    val legSideRuns = filteredShots.filter { it.angleDeg >= 0f }.sumOf { it.runs }

    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("wagon_wheel_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WAGON WHEEL • $teamName",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DisplayFontFamily,
                            color = AuctionGold,
                            letterSpacing = 0.8.sp
                        )
                    )
                    Text(
                        text = "$totalRuns runs • $fours Fours • $sixes Sixes • Off $offSideRuns / Leg $legSideRuns",
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastMuted
                    )
                }
            }

            // Optional batter filter strip
            if (activeBatters.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChipMini(
                        label = "All Team",
                        selected = selectedBatterId == null,
                        onClick = { selectedBatterId = null }
                    )
                    activeBatters.forEach { batter ->
                        FilterChipMini(
                            label = "${batter.playerName.substringAfterLast(' ')} (${batter.runs})",
                            selected = selectedBatterId == batter.playerId,
                            onClick = { selectedBatterId = batter.playerId }
                        )
                    }
                }
            }

            // Circular Stadium Field Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                if (filteredShots.isEmpty()) {
                    Text(
                        text = "No scoring shots recorded yet in this innings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                } else {
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                    ) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = size.minDimension * 0.46f
                        val ringR = outerR * 0.52f

                        // Outfield turf
                        drawCircle(color = Color(0xFF0E3F2C), radius = outerR, center = Offset(cx, cy))
                        drawCircle(color = Color(0xFF135038), radius = outerR * 0.78f, center = Offset(cx, cy))

                        // Boundary rope & 30-yard circle
                        drawCircle(
                            color = BroadcastWhite.copy(alpha = 0.75f),
                            radius = outerR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = BroadcastWhite.copy(alpha = 0.30f),
                            radius = ringR,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )

                        // Zone sector dividers (8 zones)
                        for (deg in 0 until 360 step 45) {
                            val rad = (deg * PI / 180.0)
                            drawLine(
                                color = BroadcastWhite.copy(alpha = 0.12f),
                                start = Offset(cx, cy),
                                end = Offset(cx + cos(rad).toFloat() * outerR, cy + sin(rad).toFloat() * outerR),
                                strokeWidth = 1f
                            )
                        }

                        // Central 22-yard pitch strip
                        drawRect(
                            color = Color(0xFFD2B48C),
                            topLeft = Offset(cx - 5f, cy - 18f),
                            size = Size(10f, 36f)
                        )

                        // Shot spokes radiating from batter's crease (slightly below center)
                        val origin = Offset(cx, cy + 4f)
                        filteredShots.forEach { shot ->
                            // angleDeg: 0 = straight up (-90 in Canvas), -90 = Off (left), +90 = Leg (right)
                            val canvasRad = ((shot.angleDeg - 90f) * PI / 180.0)
                            val dist = (outerR * shot.distanceRatio.coerceIn(0.22f, 1.03f))
                            val end = Offset(
                                origin.x + cos(canvasRad).toFloat() * dist,
                                origin.y + sin(canvasRad).toFloat() * dist
                            )
                            val color = when {
                                shot.runs >= 6 -> AuctionGold
                                shot.runs == 4 -> TurfEmerald
                                else -> BoundarySky.copy(alpha = 0.75f)
                            }
                            val width = when {
                                shot.runs >= 6 -> 3.6f
                                shot.runs == 4 -> 2.8f
                                else -> 1.8f
                            }
                            drawLine(
                                color = color,
                                start = origin,
                                end = end,
                                strokeWidth = width,
                                cap = StrokeCap.Round
                            )
                            if (shot.runs >= 4) {
                                drawCircle(color = color, radius = 4f, center = end)
                            }
                        }
                    }

                    // Side direction labels
                    Text(
                        text = "STRAIGHT",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = BroadcastMuted),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        text = "OFF",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = BroadcastMuted),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                    Text(
                        text = "LEG",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = BroadcastMuted),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "BEHIND",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = BroadcastMuted),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot("1–3 Runs ($singlesTwos)", BoundarySky)
                LegendDot("Fours ($fours)", TurfEmerald)
                LegendDot("Sixes ($sixes)", AuctionGold)
            }
        }
    }
}

/**
 * Clean Pitch Map ball-landing chart showing Yorker / Full / Good Length / Short zones.
 */
@Composable
fun PitchMapChartCard(
    points: List<PitchMapPoint>,
    bowlers: List<BowlerScoreEntry>,
    teamName: String,
    modifier: Modifier = Modifier
) {
    var selectedBowlerId by remember(teamName) { mutableStateOf<String?>(null) }
    val filteredPoints = remember(points, selectedBowlerId) {
        if (selectedBowlerId == null) points else points.filter { it.bowlerId == selectedBowlerId }
    }
    val activeBowlers = remember(bowlers) { bowlers.filter { it.legalBalls > 0 } }

    val yorkers = filteredPoints.count { it.lengthY < 0.22f }
    val fullBalls = filteredPoints.count { it.lengthY in 0.22f..0.44f }
    val goodLength = filteredPoints.count { it.lengthY in 0.44f..0.72f }
    val shortBalls = filteredPoints.count { it.lengthY > 0.72f }

    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pitch_map_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = "PITCH MAP • $teamName",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = DisplayFontFamily,
                        color = BoundarySky,
                        letterSpacing = 0.8.sp
                    )
                )
                Text(
                    text = "${filteredPoints.size} balls • Yorker $yorkers • Full $fullBalls • Good $goodLength • Short $shortBalls",
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastMuted
                )
            }

            if (activeBowlers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChipMini(
                        label = "All Bowlers",
                        selected = selectedBowlerId == null,
                        onClick = { selectedBowlerId = null }
                    )
                    activeBowlers.forEach { bw ->
                        FilterChipMini(
                            label = "${bw.playerName.substringAfterLast(' ')} (${bw.wickets}/${bw.runsConceded})",
                            selected = selectedBowlerId == bw.playerId,
                            onClick = { selectedBowlerId = bw.playerId }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavyMidnight),
                contentAlignment = Alignment.Center
            ) {
                if (filteredPoints.isEmpty()) {
                    Text(
                        text = "No deliveries bowled yet in this innings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val pitchLeft = w * 0.18f
                        val pitchRight = w * 0.82f
                        val pitchW = pitchRight - pitchLeft

                        // Pitch length zones (top = batter stumps/yorker, bottom = short/bouncer)
                        val z1 = h * 0.22f // Yorker
                        val z2 = h * 0.44f // Full
                        val z3 = h * 0.72f // Good Length

                        drawRect(Color(0xFFCFA97E).copy(alpha = 0.28f), Offset(pitchLeft, 0f), Size(pitchW, z1))
                        drawRect(Color(0xFF4CAF50).copy(alpha = 0.16f), Offset(pitchLeft, z1), Size(pitchW, z2 - z1))
                        drawRect(Color(0xFF38BDF8).copy(alpha = 0.16f), Offset(pitchLeft, z2), Size(pitchW, z3 - z2))
                        drawRect(Color(0xFFEF4444).copy(alpha = 0.14f), Offset(pitchLeft, z3), Size(pitchW, h - z3))

                        // Pitch borders & horizontal length lines
                        drawRect(
                            color = BroadcastWhite.copy(alpha = 0.35f),
                            topLeft = Offset(pitchLeft, 0f),
                            size = Size(pitchW, h),
                            style = Stroke(width = 1.5f)
                        )
                        listOf(z1, z2, z3).forEach { y ->
                            drawLine(
                                color = BroadcastWhite.copy(alpha = 0.25f),
                                start = Offset(pitchLeft, y),
                                end = Offset(pitchRight, y),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                            )
                        }

                        // Stump line corridor down the middle
                        val midX = w * 0.5f
                        val stumpHalf = pitchW * 0.12f
                        drawRect(
                            color = BroadcastWhite.copy(alpha = 0.08f),
                            topLeft = Offset(midX - stumpHalf, 0f),
                            size = Size(stumpHalf * 2f, h)
                        )
                        // Batter stumps at top
                        for (i in -1..1) {
                            val sx = midX + i * stumpHalf * 0.72f
                            drawLine(BroadcastWhite, Offset(sx, 0f), Offset(sx, 14f), strokeWidth = 3f)
                        }

                        // Ball landing dots
                        filteredPoints.forEach { pt ->
                            val x = (midX + pt.lineX.coerceIn(-1f, 1f) * (pitchW * 0.45f))
                            val y = (pt.lengthY.coerceIn(0.05f, 0.96f) * h)
                            val dotColor = when (pt.outcome) {
                                BallOutcomeType.WICKET -> WicketCrimson
                                BallOutcomeType.SIX -> AuctionGold
                                BallOutcomeType.FOUR -> TurfEmerald
                                BallOutcomeType.DOT -> BroadcastWhite.copy(alpha = 0.72f)
                                else -> BoundarySky
                            }
                            val r = if (pt.outcome == BallOutcomeType.WICKET) 6.5f else 5f
                            drawCircle(color = dotColor, radius = r, center = Offset(x, y))
                            if (pt.outcome == BallOutcomeType.WICKET) {
                                drawCircle(
                                    color = BroadcastWhite,
                                    radius = r + 1.5f,
                                    center = Offset(x, y),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }
                    }

                    // Zone labels along left margin
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(34.dp)
                    ) {
                        Text("YORKER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = AuctionGold))
                        Text("FULL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TurfEmerald))
                        Text("GOOD", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = BoundarySky))
                        Text("SHORT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = WicketCrimson))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot("Wicket", WicketCrimson)
                LegendDot("Six", AuctionGold)
                LegendDot("Four", TurfEmerald)
                LegendDot("Run", BoundarySky)
                LegendDot("Dot", BroadcastSilver)
            }
        }
    }
}

@Composable
private fun FilterChipMini(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AuctionGold.copy(alpha = 0.2f) else NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) AuctionGold else NavyOutline),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MonoFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) AuctionGold else BroadcastSilver
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = BroadcastSilver
        )
    }
}
