package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DrsMode
import com.example.engine.DrsPendingDecision
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
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Lightweight animated DRS Review broadcast overlay:
 * - BALL_TRACKING (LBW): Animated Hawk-Eye pitch, bounce point, impact, and projected path to stumps.
 * - ULTRA_EDGE (Caught Behind): Animated oscilloscope audio waveform spiking at the bat edge.
 */
@Composable
fun DrsReviewOverlay(
    decision: DrsPendingDecision,
    soundEffects: MatchSoundEffects?,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(decision.decisionId) { Animatable(0f) }

    LaunchedEffect(decision.decisionId) {
        soundEffects?.playDrsRadarCue()
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 2200, easing = LinearEasing))
        soundEffects?.playDrsDecisionCue(decision.overturned)
    }

    val p = progress.value
    val verdictReady = p >= 0.96f
    val finalOut = if (decision.overturned) !decision.originalDecisionOut else decision.originalDecisionOut
    val verdictColor = when {
        !verdictReady -> BoundarySky
        finalOut -> WicketCrimson
        else -> TurfEmerald
    }
    val verdictTitle = when {
        !verdictReady -> "THIRD UMPIRE REVIEWING..."
        finalOut -> "OUT!"
        else -> "NOT OUT!"
    }
    val verdictSub = when {
        !verdictReady -> "Checking ${decision.mode.label.lowercase()} telemetry..."
        decision.overturned -> "DECISION OVERTURNED! ${decision.reviewingTeamName} retains their review."
        decision.umpiresCall -> "UMPIRE'S CALL — Original decision stands & review retained."
        else -> "ORIGINAL DECISION UPHELD — ${decision.reviewingTeamName} loses 1 review."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.6.dp, verdictColor.copy(alpha = 0.85f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("drs_review_overlay")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (decision.mode == DrsMode.BALL_TRACKING) Icons.Filled.Radar else Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = BoundarySky,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DRS • ${decision.mode.label}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.sp,
                                    color = BoundarySky
                                )
                            )
                            Text(
                                text = "${decision.batterName} vs ${decision.bowlerName} • ${decision.overLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastMuted
                            )
                        }
                    }
                    Surface(
                        color = NavySurfaceHigh,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "ON-FIELD: ${if (decision.originalDecisionOut) "OUT" else "NOT OUT"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = AuctionGold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Animated Visualization Canvas
                Surface(
                    color = NavyMidnight,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    if (decision.mode == DrsMode.BALL_TRACKING) {
                        HawkEyeBallTrackingCanvas(
                            decision = decision,
                            progress = p,
                            finalOut = finalOut,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        UltraEdgeWaveformCanvas(
                            spikeIntensity = decision.spikeIntensity,
                            progress = p,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Live Telemetry Status Chips (appear sequentially)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DrsCheckChip(
                        title = if (decision.mode == DrsMode.BALL_TRACKING) "PITCHING" else "AUDIO",
                        value = if (p >= 0.30f) decision.pitchingLabel else "...",
                        accent = if (p >= 0.30f) TurfEmerald else BroadcastMuted,
                        modifier = Modifier.weight(1f)
                    )
                    DrsCheckChip(
                        title = if (decision.mode == DrsMode.BALL_TRACKING) "IMPACT" else "BAT EDGE",
                        value = if (p >= 0.62f) decision.impactLabel else "...",
                        accent = if (p >= 0.62f) {
                            if (decision.impactLabel.contains("OUTSIDE", true) || decision.impactLabel.contains("NO", true)) TurfEmerald else AuctionGold
                        } else BroadcastMuted,
                        modifier = Modifier.weight(1f)
                    )
                    DrsCheckChip(
                        title = if (decision.mode == DrsMode.BALL_TRACKING) "WICKETS" else "VERDICT",
                        value = if (p >= 0.90f) decision.wicketsLabel else "...",
                        accent = if (p >= 0.90f) {
                            when {
                                decision.wicketsLabel.contains("HITTING", true) || decision.wicketsLabel.contains("OUT", true) -> WicketCrimson
                                decision.wicketsLabel.contains("UMPIRE", true) -> AuctionGold
                                else -> TurfEmerald
                            }
                        } else BroadcastMuted,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Big Screen Decision Banner
                Surface(
                    color = verdictColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, verdictColor.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = verdictTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.6.sp,
                                color = verdictColor,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = verdictSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onComplete,
                    enabled = verdictReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (finalOut) AuctionGold else TurfEmerald,
                        contentColor = if (finalOut) OnGoldDark else OnEmeraldDark,
                        disabledContainerColor = NavySurfaceHigh,
                        disabledContentColor = BroadcastMuted
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("drs_continue_button")
                ) {
                    Text(
                        text = if (verdictReady) "Continue Match" else "Analyzing Telemetry...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrsCheckChip(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = BroadcastMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HawkEyeBallTrackingCanvas(
    decision: DrsPendingDecision,
    progress: Float,
    finalOut: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Stadium turf background
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0A192A), Color(0xFF0E3A2A), Color(0xFF124934))
            )
        )

        // Perspective pitch trapezoid
        val topHalfW = w * 0.14f
        val botHalfW = w * 0.34f
        val pitchTopY = h * 0.14f
        val pitchBotY = h * 0.92f
        val centerX = w * 0.5f

        val pitchPath = Path().apply {
            moveTo(centerX - topHalfW, pitchTopY)
            lineTo(centerX + topHalfW, pitchTopY)
            lineTo(centerX + botHalfW, pitchBotY)
            lineTo(centerX - botHalfW, pitchBotY)
            close()
        }
        drawPath(pitchPath, color = Color(0xFFD2B48C).copy(alpha = 0.85f))

        // Stump-to-stump Hawk-Eye purple corridor
        val stumpTopW = w * 0.048f
        val stumpBotW = w * 0.088f
        val corridor = Path().apply {
            moveTo(centerX - stumpTopW, pitchTopY)
            lineTo(centerX + stumpTopW, pitchTopY)
            lineTo(centerX + stumpBotW, pitchBotY)
            lineTo(centerX - stumpBotW, pitchBotY)
            close()
        }
        drawPath(corridor, color = Color(0xFF7C4DFF).copy(alpha = 0.28f))

        // Far-end stumps (target stumps)
        val stumpY = pitchTopY + 12f
        val stumpH = 26f
        for (i in -1..1) {
            val sx = centerX + i * (stumpTopW * 0.72f)
            drawLine(
                color = BroadcastWhite,
                start = Offset(sx, stumpY),
                end = Offset(sx, stumpY - stumpH),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }
        drawLine(
            color = AuctionGold,
            start = Offset(centerX - stumpTopW * 0.75f, stumpY - stumpH),
            end = Offset(centerX + stumpTopW * 0.75f, stumpY - stumpH),
            strokeWidth = 3f
        )

        // Key trajectory points: Release -> Pitch Bounce -> Pad Impact -> Projected Stumps
        val releasePt = Offset(centerX + decision.ballPitchX * w * 0.08f, pitchBotY - 12f)
        val bouncePt = Offset(
            centerX + decision.ballPitchX * w * 0.13f,
            pitchBotY - (pitchBotY - pitchTopY) * (0.32f + (1f - decision.ballPitchY) * 0.24f)
        )
        val impactPt = Offset(
            centerX + decision.ballPitchX * w * 0.08f,
            pitchTopY + (pitchBotY - pitchTopY) * 0.30f
        )
        val missOffset = if (finalOut) 0.32f else if (decision.umpiresCall) 0.78f else 1.55f
        val targetX = centerX + (if (decision.ballPitchX >= 0f) 1f else -1f) * stumpTopW * missOffset
        val targetPt = Offset(targetX, stumpY - stumpH * 0.45f)

        // Segment 1: Release -> Bounce (progress 0..0.38)
        val seg1 = (progress / 0.38f).coerceIn(0f, 1f)
        val cur1 = Offset(
            releasePt.x + (bouncePt.x - releasePt.x) * seg1,
            releasePt.y + (bouncePt.y - releasePt.y) * seg1
        )
        drawLine(BoundarySky, releasePt, cur1, strokeWidth = 5f, cap = StrokeCap.Round)

        // Segment 2: Bounce -> Impact (progress 0.38..0.68)
        if (progress > 0.38f) {
            drawCircle(AuctionGold, radius = 6.5f, center = bouncePt)
            val seg2 = ((progress - 0.38f) / 0.30f).coerceIn(0f, 1f)
            val cur2 = Offset(
                bouncePt.x + (impactPt.x - bouncePt.x) * seg2,
                bouncePt.y + (impactPt.y - bouncePt.y) * seg2
            )
            drawLine(BoundarySky, bouncePt, cur2, strokeWidth = 5f, cap = StrokeCap.Round)
        }

        // Segment 3: Projected Path Impact -> Stumps (progress 0.68..1.0)
        if (progress > 0.68f) {
            drawCircle(Color.White, radius = 7f, center = impactPt)
            val seg3 = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
            val cur3 = Offset(
                impactPt.x + (targetPt.x - impactPt.x) * seg3,
                impactPt.y + (targetPt.y - impactPt.y) * seg3
            )
            val projColor = if (finalOut) WicketCrimson else TurfEmerald
            drawLine(
                color = projColor,
                start = impactPt,
                end = cur3,
                strokeWidth = 5.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            )
            if (seg3 >= 0.92f) {
                drawCircle(projColor.copy(alpha = 0.35f), radius = 14f, center = targetPt)
                drawCircle(projColor, radius = 7f, center = targetPt)
            }
        }
    }
}

@Composable
private fun UltraEdgeWaveformCanvas(
    spikeIntensity: Float,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.54f

        // Oscilloscope grid
        drawRect(Color(0xFF071320))
        val gridColor = Color(0xFF183652)
        for (i in 1..4) {
            val y = h * (i / 5f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        for (i in 1..7) {
            val x = w * (i / 8f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }

        // Bat-passing marker zone in the center
        val batZoneX = w * 0.54f
        drawRect(
            color = AuctionGold.copy(alpha = 0.14f),
            topLeft = Offset(batZoneX - 24f, 0f),
            size = Size(48f, h)
        )
        drawLine(
            color = AuctionGold.copy(alpha = 0.65f),
            start = Offset(batZoneX, 8f),
            end = Offset(batZoneX, h - 8f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        // Animated audio waveform scrolling left to right
        val sampleCount = 72
        val maxDrawIndex = (sampleCount * progress.coerceIn(0.05f, 1f)).toInt().coerceAtLeast(2)
        val wavePath = Path()
        for (i in 0..maxDrawIndex) {
            val frac = i / sampleCount.toFloat()
            val x = frac * w
            val distFromBat = abs(frac - 0.54f)
            val noise = sin(i * 1.9).toFloat() * 4f
            val spike = if (distFromBat < 0.08f) {
                val envelope = (1f - distFromBat / 0.08f)
                sin(i * 2.8 + PI * 0.5).toFloat() * (h * 0.38f * spikeIntensity * envelope)
            } else {
                0f
            }
            val y = midY + noise + spike
            if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
        }

        val waveColor = if (spikeIntensity >= 0.45f && progress >= 0.55f) WicketCrimson else TurfEmerald
        drawPath(
            path = wavePath,
            color = waveColor,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )

        // Glowing scan head
        val headX = (maxDrawIndex / sampleCount.toFloat()) * w
        drawCircle(color = waveColor.copy(alpha = 0.35f), radius = 10f, center = Offset(headX, midY))
        drawCircle(color = BroadcastWhite, radius = 4f, center = Offset(headX, midY))
    }
}
