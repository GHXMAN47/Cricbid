package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ActionAnimationSnapshot
import com.example.engine.BallOutcomeType
import com.example.engine.DeliveryType
import com.example.engine.FieldPreset
import com.example.engine.InteractivePlayRole
import com.example.engine.PitchType
import com.example.engine.ShotDirection
import com.example.engine.TimingGrade
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BoundarySky
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cricket 26-inspired widescreen broadcast stadium action canvas.
 * Renders animated bowler run-up & arm release, pitch bounce, animated batter stance/swing,
 * ball trajectory, sprinting fielders, flying bails on wickets, and dynamic camera zoom
 * using lightweight Jetpack Compose Canvas primitives (zero heavy bitmaps).
 */
@Composable
fun CricketActionPitchCanvas(
    snapshot: ActionAnimationSnapshot?,
    selectedShot: ShotDirection,
    selectedDelivery: DeliveryType,
    activeRole: InteractivePlayRole,
    strikerName: String,
    bowlerName: String,
    isRedBall: Boolean,
    soundEffects: MatchSoundEffects,
    pitchType: PitchType = PitchType.BALANCED,
    dewActive: Boolean = false,
    fieldPreset: FieldPreset = FieldPreset.BALANCED,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val animProgress = remember { Animatable(1f) }
    val idleTransition = rememberInfiniteTransition(label = "batter_idle")
    val idleBob by idleTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_bob"
    )

    LaunchedEffect(snapshot?.sequenceId) {
        val snap = snapshot ?: return@LaunchedEffect
        animProgress.snapTo(0f)
        soundEffects.playReleaseCue()
        animProgress.animateTo(
            targetValue = 0.54f,
            animationSpec = tween(durationMillis = 480, easing = LinearEasing)
        )
        soundEffects.playOutcomeCue(snap.outcome, snap.timingGrade)
        if (snap.outcome == BallOutcomeType.SIX || snap.outcome == BallOutcomeType.FOUR || snap.outcome == BallOutcomeType.WICKET) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
    }

    val t = animProgress.value
    val isActiveBall = snapshot != null && t < 0.999f
    val isBigEvent = snapshot != null && (
        snapshot.outcome == BallOutcomeType.SIX ||
            snapshot.outcome == BallOutcomeType.FOUR ||
            snapshot.outcome == BallOutcomeType.WICKET
        )

    // Dynamic Camera Focus & Zoom on big shots or wickets
    val cameraZoom = when {
        snapshot == null -> 1f
        !isBigEvent -> 1f
        t < 0.50f -> 1f
        t < 0.86f -> {
            val zt = ((t - 0.50f) / 0.36f).coerceIn(0f, 1f)
            val peak = when (snapshot.outcome) {
                BallOutcomeType.WICKET -> 1.24f
                BallOutcomeType.SIX -> 1.20f
                else -> 1.14f
            }
            1f + (peak - 1f) * sin(zt * PI.toFloat())
        }
        else -> 1f
    }

    val cameraPanX = if (snapshot != null && isBigEvent && t in 0.54f..0.92f) {
        val pt = ((t - 0.54f) / 0.38f).coerceIn(0f, 1f)
        val env = sin(pt * PI.toFloat())
        when (snapshot.outcome) {
            BallOutcomeType.WICKET -> 0f
            else -> -sin(Math.toRadians(snapshot.shotDirection.angleDeg.toDouble())).toFloat() * 44f * env
        }
    } else 0f

    val cameraPanY = if (snapshot != null && isBigEvent && t in 0.52f..0.92f) {
        val pt = ((t - 0.52f) / 0.40f).coerceIn(0f, 1f)
        val env = sin(pt * PI.toFloat())
        when (snapshot.outcome) {
            BallOutcomeType.WICKET -> -24f * env // focus near batting stumps
            BallOutcomeType.SIX -> 28f * env // follow lofted ball skyward
            else -> 12f * env
        }
    } else 0f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF071A14))
            .testTag("cricket_action_arena_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = cameraZoom,
                    scaleY = cameraZoom,
                    translationX = cameraPanX,
                    translationY = cameraPanY
                )
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // 1. Stadium Night Sky & Floodlight Glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF0F2D24), Color(0xFF0B3B28)),
                    startY = 0f,
                    endY = h
                )
            )
            // Stadium Crowd Rim Arc (top horizon)
            drawOval(
                color = Color(0xFF13263E),
                topLeft = Offset(-w * 0.12f, h * 0.04f),
                size = Size(w * 1.24f, h * 1.15f)
            )
            // Floodlight Soft Beams (top-left & top-right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x30FFF6D1), Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.06f),
                    radius = w * 0.30f
                ),
                radius = w * 0.30f,
                center = Offset(w * 0.12f, h * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x30FFF6D1), Color.Transparent),
                    center = Offset(w * 0.88f, h * 0.06f),
                    radius = w * 0.30f
                ),
                radius = w * 0.30f,
                center = Offset(w * 0.88f, h * 0.06f)
            )

            // 2. Lush Outfield Oval & Mowed Stripes
            val fieldLeft = w * 0.04f
            val fieldTop = h * 0.14f
            val fieldW = w * 0.92f
            val fieldH = h * 0.96f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1C7C47), Color(0xFF125A34), Color(0xFF0B3F24)),
                    center = Offset(cx, h * 0.56f),
                    radius = w * 0.58f
                ),
                topLeft = Offset(fieldLeft, fieldTop),
                size = Size(fieldW, fieldH)
            )
            // Subtle mowed grass bands
            for (stripe in 0..5) {
                val bandY = h * (0.22f + stripe * 0.13f)
                drawRect(
                    color = if (stripe % 2 == 0) Color(0x14FFFFFF) else Color(0x10000000),
                    topLeft = Offset(w * 0.08f, bandY),
                    size = Size(w * 0.84f, h * 0.065f)
                )
            }

            // Boundary Rope (Glowing Broadcast Edge)
            drawOval(
                color = AuctionGold.copy(alpha = 0.80f),
                topLeft = Offset(fieldLeft + 6f, fieldTop + 6f),
                size = Size(fieldW - 12f, fieldH - 12f),
                style = Stroke(width = 3.2f)
            )
            // 30-Yard Inner Circle (Dashed White)
            drawOval(
                color = BroadcastWhite.copy(alpha = 0.32f),
                topLeft = Offset(w * 0.19f, h * 0.28f),
                size = Size(w * 0.62f, h * 0.60f),
                style = Stroke(
                    width = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            )

            // Evening Dew sheen across outfield when 2nd-innings dew is active
            if (dewActive) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2238BDF8), Color(0x0E7DD3FC), Color.Transparent),
                        center = Offset(cx, h * 0.56f),
                        radius = w * 0.54f
                    ),
                    topLeft = Offset(fieldLeft, fieldTop),
                    size = Size(fieldW, fieldH)
                )
            }

            // 3. Perspective 22-Yard Cricket Pitch Strip (tinted by PitchType!)
            val pitchTopY = h * 0.24f
            val pitchBotY = h * 0.86f
            val pitchTopHalfW = w * 0.042f
            val pitchBotHalfW = w * 0.088f
            val pitchPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx - pitchTopHalfW, pitchTopY)
                lineTo(cx + pitchTopHalfW, pitchTopY)
                lineTo(cx + pitchBotHalfW, pitchBotY)
                lineTo(cx - pitchBotHalfW, pitchBotY)
                close()
            }
            val pitchColors = when (pitchType) {
                PitchType.GREEN -> listOf(Color(0xFF9AB87B), Color(0xFFB4CC91), Color(0xFF99B67A))
                PitchType.DUSTY -> listOf(Color(0xFFD5B184), Color(0xFFC59B6D), Color(0xFFB5895A))
                PitchType.FLAT -> listOf(Color(0xFFE3CEA8), Color(0xFFEDD9B6), Color(0xFFD9BE92))
                PitchType.BALANCED -> listOf(Color(0xFFCBB082), Color(0xFFDFC79A), Color(0xFFC6A572))
            }
            drawPath(
                path = pitchPath,
                brush = Brush.verticalGradient(
                    colors = pitchColors,
                    startY = pitchTopY,
                    endY = pitchBotY
                )
            )
            // Dry spin cracks on Dusty pitches
            if (pitchType == PitchType.DUSTY) {
                drawLine(
                    color = Color(0x554E342E),
                    start = Offset(cx - pitchTopHalfW * 0.35f, pitchTopY + (pitchBotY - pitchTopY) * 0.45f),
                    end = Offset(cx + pitchBotHalfW * 0.22f, pitchTopY + (pitchBotY - pitchTopY) * 0.62f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0x444E342E),
                    start = Offset(cx + pitchTopHalfW * 0.3f, pitchTopY + (pitchBotY - pitchTopY) * 0.34f),
                    end = Offset(cx - pitchBotHalfW * 0.25f, pitchTopY + (pitchBotY - pitchTopY) * 0.56f),
                    strokeWidth = 1.2f
                )
            }
            // Bowling & Popping Creases
            val farCreaseY = pitchTopY + (pitchBotY - pitchTopY) * 0.12f
            val nearCreaseY = pitchBotY - (pitchBotY - pitchTopY) * 0.14f
            drawLine(
                color = BroadcastWhite.copy(alpha = 0.88f),
                start = Offset(cx - pitchTopHalfW * 1.15f, farCreaseY),
                end = Offset(cx + pitchTopHalfW * 1.15f, farCreaseY),
                strokeWidth = 2f
            )
            drawLine(
                color = BroadcastWhite.copy(alpha = 0.92f),
                start = Offset(cx - pitchBotHalfW * 1.12f, nearCreaseY),
                end = Offset(cx + pitchBotHalfW * 1.12f, nearCreaseY),
                strokeWidth = 2.6f
            )

            // 4. Tactical Pre-Delivery Visual Guides (Aim Arrow / Bowling Pitch Marker)
            val activeDelivery = snapshot?.deliveryType ?: selectedDelivery
            val bounceY = farCreaseY + (nearCreaseY - farCreaseY) * activeDelivery.pitchYRatio
            val bounceX = cx + activeDelivery.lateralCurve * w * 0.12f

            if (!isActiveBall) {
                if (activeRole == InteractivePlayRole.BOWLING) {
                    // Glowing Pitch Target Ring for selected delivery
                    val markerColor = when (selectedDelivery) {
                        DeliveryType.YORKER -> WicketCrimson
                        DeliveryType.BOUNCER -> AuctionGold
                        DeliveryType.SPIN_TURN -> BoundarySky
                        else -> TurfEmerald
                    }
                    val targetY = farCreaseY + (nearCreaseY - farCreaseY) * selectedDelivery.pitchYRatio
                    val targetX = cx + selectedDelivery.lateralCurve * w * 0.12f
                    drawCircle(
                        color = markerColor.copy(alpha = 0.28f),
                        radius = 15f + idleBob * 2.5f,
                        center = Offset(targetX, targetY)
                    )
                    drawCircle(
                        color = markerColor,
                        radius = 7f,
                        center = Offset(targetX, targetY),
                        style = Stroke(width = 2.2f)
                    )
                } else {
                    // Directional Batting Shot Preview Arrow
                    val rad = Math.toRadians(selectedShot.angleDeg.toDouble())
                    val arrowLen = if (selectedShot.isLofted) h * 0.36f else h * 0.28f
                    val endX = cx + sin(rad).toFloat() * arrowLen * 1.25f
                    val endY = nearCreaseY - cos(rad).toFloat() * arrowLen
                    drawLine(
                        color = if (selectedShot.isLofted) AuctionGold.copy(alpha = 0.72f) else TurfEmerald.copy(alpha = 0.65f),
                        start = Offset(cx, nearCreaseY - 8f),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                    drawCircle(
                        color = if (selectedShot.isLofted) AuctionGold else TurfEmerald,
                        radius = 5.5f,
                        center = Offset(endX, endY)
                    )
                }
            }

            // 5. Far-End Stumps (Bowler End)
            drawStumps(cx = cx, baseY = pitchTopY + 10f, scale = 0.62f, shatteredProgress = 0f)

            // 6. Animated Fielders (9 dynamic outfielders positioned by active FieldPreset + Wicketkeeper!)
            val ballHitDirection = snapshot?.shotDirection ?: selectedShot
            val ballHitRad = Math.toRadians(ballHitDirection.angleDeg.toDouble())
            val chaseFactor = if (snapshot != null && t > 0.60f && snapshot.outcome != BallOutcomeType.WICKET && snapshot.outcome != BallOutcomeType.DOT) {
                ((t - 0.60f) / 0.40f).coerceIn(0f, 1f)
            } else 0f
            val landingTarget = Offset(
                x = (cx + sin(ballHitRad).toFloat() * w * 0.34f).coerceIn(w * 0.12f, w * 0.88f),
                y = (nearCreaseY - cos(ballHitRad).toFloat() * h * 0.44f).coerceIn(h * 0.20f, h * 0.76f)
            )
            val dynamicOutfielders = fieldPreset.fielderCoords.map { (fx, fy) ->
                Offset(w * fx, h * fy)
            }
            dynamicOutfielders.forEach { base ->
                val pos = if (chaseFactor > 0f) {
                    val dist = kotlin.math.hypot((landingTarget.x - base.x).toDouble(), (landingTarget.y - base.y).toDouble())
                    if (dist < w * 0.28f) {
                        Offset(
                            x = base.x + (landingTarget.x - base.x) * chaseFactor * 0.52f,
                            y = base.y + (landingTarget.y - base.y) * chaseFactor * 0.52f
                        )
                    } else base
                } else base
                drawFielderSprite(
                    center = pos,
                    scale = 0.66f,
                    jerseyColor = Color(0xFF1F6FEB),
                    runningPhase = if (chaseFactor > 0f) chaseFactor * 10f else 0f
                )
            }
            // Wicketkeeper behind near stumps
            drawFielderSprite(
                center = Offset(w * 0.56f, h * 0.90f),
                scale = 0.90f,
                jerseyColor = Color(0xFF1F6FEB),
                runningPhase = 0f
            )

            // 7. Animated Bowler (Run-up 0..0.34, Release Leap 0.34..0.46, Follow-through 0.46..0.70)
            val runupT = (t / 0.36f).coerceIn(0f, 1f)
            val bowlerStartY = pitchTopY - h * 0.08f
            val bowlerReleaseY = farCreaseY + 2f
            val bowlerY = if (isActiveBall) {
                bowlerStartY + (bowlerReleaseY - bowlerStartY) * runupT + if (t > 0.36f) ((t - 0.36f) * 18f).coerceAtMost(12f) else 0f
            } else {
                bowlerStartY + idleBob * 1.5f
            }
            val bowlerArmAngle = when {
                !isActiveBall -> -25f
                t < 0.34f -> -25f + sin(runupT * 14f) * 35f
                t < 0.48f -> -165f + ((t - 0.34f) / 0.14f) * 240f
                else -> 75f
            }
            drawBowlerSprite(
                center = Offset(cx + w * 0.015f, bowlerY),
                scale = 0.76f + runupT * 0.08f,
                jerseyColor = Color(0xFF1F6FEB),
                stridePhase = if (isActiveBall && t < 0.42f) runupT * 18f else 0f,
                armAngleDeg = bowlerArmAngle
            )

            // 8. Near-End Stumps (shatter on WICKET!)
            val wicketBreakT = if (snapshot?.outcome == BallOutcomeType.WICKET && t > 0.54f) {
                ((t - 0.54f) / 0.46f).coerceIn(0f, 1f)
            } else 0f
            drawStumps(cx = cx, baseY = nearCreaseY + 12f, scale = 1.0f, shatteredProgress = wicketBreakT)

            // 9. Animated Batter (Idle breathing stance -> Backlift -> Directional Swing & Follow-Through)
            val swingT = if (isActiveBall && t >= 0.46f) ((t - 0.46f) / 0.24f).coerceIn(0f, 1f) else 0f
            val shotSign = when (ballHitDirection) {
                ShotDirection.OFF_DRIVE -> -1f
                ShotDirection.LEG_GLANCE -> 1f
                ShotDirection.LOFTED -> 0.5f
                ShotDirection.STRAIGHT -> 0.15f
            }
            drawBatterSprite(
                center = Offset(cx - w * 0.025f, nearCreaseY - 4f),
                scale = 1.05f,
                jerseyColor = Color(0xFF00C982),
                idleBob = if (isActiveBall) 0f else idleBob,
                swingProgress = swingT,
                shotSign = shotSign,
                isLofted = ballHitDirection.isLofted,
                dismissed = wicketBreakT > 0.15f
            )

            // 10. Ball Flight Trajectory & Impact VFX
            if (snapshot != null && t >= 0.34f) {
                val ballColor = if (isRedBall || snapshot.isRedBall) Color(0xFFD62839) else Color(0xFFF8FAFC)
                val releasePos = Offset(cx + w * 0.02f, farCreaseY - 12f)
                val bouncePos = Offset(bounceX, bounceY)
                val contactPos = Offset(cx - w * 0.005f, nearCreaseY - 10f)

                if (t < 0.46f) {
                    // Bowler hand -> Pitch bounce
                    val p = ((t - 0.34f) / 0.12f).coerceIn(0f, 1f)
                    val bx = releasePos.x + (bouncePos.x - releasePos.x) * p
                    val by = releasePos.y + (bouncePos.y - releasePos.y) * p - sin(p * PI.toFloat()) * 14f
                    drawLine(
                        color = ballColor.copy(alpha = 0.35f),
                        start = releasePos,
                        end = Offset(bx, by),
                        strokeWidth = 2.5f
                    )
                    drawCircle(color = ballColor, radius = 4.2f + p * 1.2f, center = Offset(bx, by))
                } else if (t < 0.56f) {
                    // Pitch bounce dust ring + bounce -> batter contact
                    val p = ((t - 0.46f) / 0.10f).coerceIn(0f, 1f)
                    drawCircle(
                        color = AuctionGold.copy(alpha = (1f - p) * 0.65f),
                        radius = 5f + p * 14f,
                        center = bouncePos,
                        style = Stroke(width = 2f)
                    )
                    val bx = bouncePos.x + (contactPos.x - bouncePos.x) * p
                    val by = bouncePos.y + (contactPos.y - bouncePos.y) * p - sin(p * PI.toFloat()) * 10f
                    drawCircle(color = ballColor, radius = 5.4f, center = Offset(bx, by))
                } else {
                    // Post-contact outcome trajectory!
                    val p = ((t - 0.56f) / 0.44f).coerceIn(0f, 1f)
                    when (snapshot.outcome) {
                        BallOutcomeType.SIX -> {
                            val endX = cx + sin(ballHitRad).toFloat() * w * 0.46f
                            val endY = h * 0.12f
                            val bx = contactPos.x + (endX - contactPos.x) * p
                            val by = contactPos.y + (endY - contactPos.y) * p - sin(p * PI.toFloat()) * h * 0.28f
                            drawLine(
                                color = AuctionGold.copy(alpha = 0.72f),
                                start = contactPos,
                                end = Offset(bx, by),
                                strokeWidth = 3.5f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = AuctionGold.copy(alpha = 0.35f), radius = 14f * (1f - p * 0.3f), center = Offset(bx, by))
                            drawCircle(color = ballColor, radius = 6.2f + sin(p * PI.toFloat()) * 3f, center = Offset(bx, by))
                            if (p > 0.75f) {
                                drawCircle(
                                    color = AuctionGold.copy(alpha = (1f - p) * 2.5f),
                                    radius = 12f + (p - 0.75f) * 90f,
                                    center = Offset(endX, endY),
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                        BallOutcomeType.FOUR -> {
                            val endX = (cx + sin(ballHitRad).toFloat() * w * 0.44f).coerceIn(w * 0.06f, w * 0.94f)
                            val endY = (nearCreaseY - cos(ballHitRad).toFloat() * h * 0.62f).coerceIn(h * 0.18f, h * 0.72f)
                            val bx = contactPos.x + (endX - contactPos.x) * p
                            val by = contactPos.y + (endY - contactPos.y) * p
                            drawLine(
                                color = BoundarySky.copy(alpha = 0.75f),
                                start = contactPos,
                                end = Offset(bx, by),
                                strokeWidth = 3.2f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = ballColor, radius = 5.4f, center = Offset(bx, by))
                        }
                        BallOutcomeType.SINGLE, BallOutcomeType.DOUBLE, BallOutcomeType.TRIPLE -> {
                            val endX = (cx + sin(ballHitRad).toFloat() * w * 0.26f).coerceIn(w * 0.14f, w * 0.86f)
                            val endY = (nearCreaseY - cos(ballHitRad).toFloat() * h * 0.34f).coerceIn(h * 0.28f, h * 0.74f)
                            val bx = contactPos.x + (endX - contactPos.x) * p
                            val by = contactPos.y + (endY - contactPos.y) * p
                            drawLine(
                                color = TurfEmerald.copy(alpha = 0.55f),
                                start = contactPos,
                                end = Offset(bx, by),
                                strokeWidth = 2.2f
                            )
                            drawCircle(color = ballColor, radius = 4.8f, center = Offset(bx, by))
                        }
                        BallOutcomeType.WICKET -> {
                            // Red impact burst at batting stumps
                            drawCircle(
                                color = WicketCrimson.copy(alpha = (1f - p) * 0.75f),
                                radius = 10f + p * 38f,
                                center = Offset(cx, nearCreaseY + 6f),
                                style = Stroke(width = 3.5f)
                            )
                            drawCircle(
                                color = ballColor,
                                radius = 5f,
                                center = Offset(cx + p * 22f, nearCreaseY + 14f + p * 18f)
                            )
                        }
                        BallOutcomeType.DOT, BallOutcomeType.WIDE, BallOutcomeType.NO_BALL -> {
                            val keeperPos = Offset(w * 0.54f, h * 0.89f)
                            val bx = contactPos.x + (keeperPos.x - contactPos.x) * p
                            val by = contactPos.y + (keeperPos.y - contactPos.y) * p
                            drawCircle(color = ballColor, radius = 4.6f, center = Offset(bx, by))
                        }
                    }
                }
            }
        }

        // Top-Left Mini Pitch Matchup Tag (Striker vs Bowler)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = NavyMidnight.copy(alpha = 0.78f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "🏏 $strikerName  vs  ⚡ $bowlerName",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = BroadcastWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Top-Right Speed / Timing Broadcast Badge
        if (snapshot != null && t >= 0.36f) {
            val timingBadgeColor = when (snapshot.timingGrade) {
                TimingGrade.PERFECT -> TurfEmerald
                TimingGrade.GOOD -> AuctionGold
                TimingGrade.EARLY, TimingGrade.LATE -> WicketCrimson
                null -> BoundarySky
            }
            val labelText = if (snapshot.timingGrade != null) {
                "${snapshot.timingGrade.label} • ${snapshot.deliveryType.speedKph} KPH"
            } else {
                "${snapshot.deliveryType.shortLabel} • ${snapshot.deliveryType.speedKph} KPH"
            }
            Surface(
                color = NavyMidnight.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, timingBadgeColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = timingBadgeColor
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Center-Bottom Dramatic Outcome Broadcast Callout (SIX / FOUR / WICKET / RUNS)
        if (snapshot != null && t >= 0.56f) {
            val (bannerText, bannerColor) = when (snapshot.outcome) {
                BallOutcomeType.SIX -> "SIX! MAXIMUM +6" to AuctionGold
                BallOutcomeType.FOUR -> "FOUR! CRACKING BOUNDARY" to BoundarySky
                BallOutcomeType.WICKET -> "OUT! TIMBER SHATTERED" to WicketCrimson
                BallOutcomeType.WIDE -> "WIDE BALL +1" to AuctionGold
                BallOutcomeType.NO_BALL -> "NO BALL +1" to WicketCrimson
                BallOutcomeType.DOT -> "DOT BALL • TIGHT LINE" to BroadcastWhite
                else -> "${snapshot.runsScored} RUN${if (snapshot.runsScored == 1) "" else "S"}" to TurfEmerald
            }
            Surface(
                color = NavySurface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.4.dp, bannerColor),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = bannerText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = bannerColor
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawStumps(cx: Float, baseY: Float, scale: Float, shatteredProgress: Float) {
    val stumpH = 22f * scale
    val spacing = 5.5f * scale
    val wood = Color(0xFFF3D58D)

    // Leg & Middle Stumps
    for (offsetIdx in listOf(-1, 0)) {
        val sx = cx + offsetIdx * spacing
        drawLine(
            color = wood,
            start = Offset(sx, baseY),
            end = Offset(sx, baseY - stumpH),
            strokeWidth = 2.4f * scale,
            cap = StrokeCap.Round
        )
    }
    // Off Stump (tilts & flies back when bowled!)
    val offX = cx + spacing
    val offTopX = offX + shatteredProgress * 18f * scale
    val offTopY = baseY - stumpH + shatteredProgress * 10f * scale
    drawLine(
        color = wood,
        start = Offset(offX, baseY),
        end = Offset(offTopX, offTopY),
        strokeWidth = 2.4f * scale,
        cap = StrokeCap.Round
    )
    // Bails (fly outward on wicket)
    val bailY = baseY - stumpH - 1.5f * scale
    if (shatteredProgress <= 0.02f) {
        drawLine(
            color = Color(0xFFFFE5A8),
            start = Offset(cx - spacing, bailY),
            end = Offset(cx + spacing, bailY),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
        )
    } else {
        drawLine(
            color = WicketCrimson,
            start = Offset(cx - spacing - shatteredProgress * 18f, bailY - shatteredProgress * 16f),
            end = Offset(cx - shatteredProgress * 12f, bailY - shatteredProgress * 22f),
            strokeWidth = 2.2f * scale
        )
        drawLine(
            color = AuctionGold,
            start = Offset(cx + spacing + shatteredProgress * 14f, bailY - shatteredProgress * 18f),
            end = Offset(cx + spacing + shatteredProgress * 24f, bailY - shatteredProgress * 12f),
            strokeWidth = 2.2f * scale
        )
    }
}

private fun DrawScope.drawBowlerSprite(
    center: Offset,
    scale: Float,
    jerseyColor: Color,
    stridePhase: Float,
    armAngleDeg: Float
) {
    val legSwing = sin(stridePhase) * 6f * scale
    // Shadow
    drawOval(
        color = Color(0x45000000),
        topLeft = Offset(center.x - 9f * scale, center.y + 8f * scale),
        size = Size(18f * scale, 6f * scale)
    )
    // Legs
    drawLine(
        color = Color(0xFF0E2A47),
        start = Offset(center.x - 2f * scale, center.y + 1f * scale),
        end = Offset(center.x - 4f * scale - legSwing, center.y + 11f * scale),
        strokeWidth = 3f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF0E2A47),
        start = Offset(center.x + 2f * scale, center.y + 1f * scale),
        end = Offset(center.x + 4f * scale + legSwing, center.y + 11f * scale),
        strokeWidth = 3f * scale,
        cap = StrokeCap.Round
    )
    // Torso
    drawRoundRect(
        color = jerseyColor,
        topLeft = Offset(center.x - 5.5f * scale, center.y - 11f * scale),
        size = Size(11f * scale, 13f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale)
    )
    // Bowling Arm (rotates through delivery release)
    val rad = Math.toRadians(armAngleDeg.toDouble())
    drawLine(
        color = Color(0xFFF2C6A0),
        start = Offset(center.x + 4f * scale, center.y - 9f * scale),
        end = Offset(
            center.x + 4f * scale + cos(rad).toFloat() * 12f * scale,
            center.y - 9f * scale + sin(rad).toFloat() * 12f * scale
        ),
        strokeWidth = 2.6f * scale,
        cap = StrokeCap.Round
    )
    // Head
    drawCircle(
        color = Color(0xFFF2C6A0),
        radius = 4.2f * scale,
        center = Offset(center.x, center.y - 15f * scale)
    )
}

private fun DrawScope.drawBatterSprite(
    center: Offset,
    scale: Float,
    jerseyColor: Color,
    idleBob: Float,
    swingProgress: Float,
    shotSign: Float,
    isLofted: Boolean,
    dismissed: Boolean
) {
    val bobY = idleBob * 1.4f * scale + if (dismissed) 4f * scale else 0f
    // Ground Shadow
    drawOval(
        color = Color(0x50000000),
        topLeft = Offset(center.x - 12f * scale, center.y + 10f * scale),
        size = Size(24f * scale, 7f * scale)
    )
    // White Batting Pads / Legs
    drawLine(
        color = Color(0xFFF1F5F9),
        start = Offset(center.x - 3f * scale, center.y + 1f * scale + bobY),
        end = Offset(center.x - 6f * scale, center.y + 13f * scale),
        strokeWidth = 4.2f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFF1F5F9),
        start = Offset(center.x + 3f * scale, center.y + 1f * scale + bobY),
        end = Offset(center.x + 6f * scale + swingProgress * 4f * shotSign, center.y + 13f * scale),
        strokeWidth = 4.2f * scale,
        cap = StrokeCap.Round
    )
    // Jersey Torso
    drawRoundRect(
        color = jerseyColor,
        topLeft = Offset(center.x - 6.5f * scale, center.y - 13f * scale + bobY),
        size = Size(13f * scale, 15f * scale),
        cornerRadius = CornerRadius(4.5f * scale, 4.5f * scale)
    )
    // Helmet + Grill
    drawCircle(
        color = Color(0xFF0A2239),
        radius = 5.4f * scale,
        center = Offset(center.x, center.y - 18f * scale + bobY)
    )
    drawLine(
        color = AuctionGold,
        start = Offset(center.x - 4f * scale, center.y - 18f * scale + bobY),
        end = Offset(center.x + 4f * scale, center.y - 18f * scale + bobY),
        strokeWidth = 1.6f * scale
    )

    // Animated Willow Cricket Bat (Stance -> Backlift -> Follow-Through Swing!)
    val handOffset = Offset(center.x + 4f * scale, center.y - 4f * scale + bobY)
    val batAngleDeg = if (swingProgress <= 0.01f) {
        72f + idleBob * 4f // poised batting stance
    } else {
        val targetFollowThrough = when {
            isLofted -> -125f
            shotSign < 0f -> -148f // off-side drive follow-through
            shotSign > 0.4f -> -35f // leg-side whip
            else -> -95f // straight drive
        }
        72f + (targetFollowThrough - 72f) * swingProgress
    }
    val batRad = Math.toRadians(batAngleDeg.toDouble())
    val batLen = 19f * scale
    val batTip = Offset(
        x = handOffset.x + cos(batRad).toFloat() * batLen,
        y = handOffset.y + sin(batRad).toFloat() * batLen
    )
    // Bat Blade (thick willow)
    drawLine(
        color = Color(0xFFE7C792),
        start = handOffset,
        end = batTip,
        strokeWidth = 4.4f * scale,
        cap = StrokeCap.Round
    )
    // Swing motion arc flash at peak swing!
    if (swingProgress in 0.25f..0.85f && !dismissed) {
        drawLine(
            color = if (isLofted) AuctionGold.copy(alpha = 0.65f) else BroadcastWhite.copy(alpha = 0.45f),
            start = Offset(center.x - 8f * scale, center.y - 8f * scale),
            end = batTip,
            strokeWidth = 2f * scale
        )
    }
}

private fun DrawScope.drawFielderSprite(
    center: Offset,
    scale: Float,
    jerseyColor: Color,
    runningPhase: Float
) {
    val stride = sin(runningPhase) * 3.5f * scale
    drawCircle(color = Color(0x3A000000), radius = 5.5f * scale, center = Offset(center.x, center.y + 6f * scale))
    drawLine(
        color = Color(0xFF0E2A47),
        start = Offset(center.x, center.y),
        end = Offset(center.x - 2.5f * scale - stride, center.y + 7f * scale),
        strokeWidth = 2.2f * scale
    )
    drawLine(
        color = Color(0xFF0E2A47),
        start = Offset(center.x, center.y),
        end = Offset(center.x + 2.5f * scale + stride, center.y + 7f * scale),
        strokeWidth = 2.2f * scale
    )
    drawCircle(color = jerseyColor, radius = 4.8f * scale, center = Offset(center.x, center.y - 3f * scale))
    drawCircle(color = Color(0xFFF2C6A0), radius = 3.1f * scale, center = Offset(center.x, center.y - 9f * scale))
}
