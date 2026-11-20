package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeliveryType
import com.example.engine.FieldPreset
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePlayFilter
import com.example.engine.InteractivePlayRole
import com.example.engine.ShotDirection
import com.example.engine.TimingGrade
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

/**
 * Left thumb ergonomic control pod:
 * - Batting: Shot Direction / Power selector (OFF, STRAIGHT, LEG, LOFT 6)
 * - Bowling: Delivery Type selector (YORKER, BOUNCER, LENGTH, SLOWER, SPIN)
 */
@Composable
fun LeftThumbControlPod(
    activeRole: InteractivePlayRole,
    selectedShot: ShotDirection,
    selectedDelivery: DeliveryType,
    onToggleRole: (InteractivePlayRole) -> Unit,
    onSelectShot: (ShotDirection) -> Unit,
    onSelectDelivery: (DeliveryType) -> Unit,
    playFilter: InteractivePlayFilter = InteractivePlayFilter.BOTH,
    fieldPreset: FieldPreset = FieldPreset.BALANCED,
    onSelectFieldPreset: (FieldPreset) -> Unit = {},
    batterConfidence: Int = 45,
    bowlerStamina: Int = 100,
    dewActive: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Role Switch Pill (BOTH) or Active Phase Badge (BAT ONLY / BOWL ONLY)
            if (playFilter == InteractivePlayFilter.BOTH) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleMiniPill(
                        label = "BAT",
                        selected = activeRole == InteractivePlayRole.BATTING,
                        accent = TurfEmerald,
                        onClick = { onToggleRole(InteractivePlayRole.BATTING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_role_bat")
                    )
                    RoleMiniPill(
                        label = "BOWL",
                        selected = activeRole == InteractivePlayRole.BOWLING,
                        accent = BoundarySky,
                        onClick = { onToggleRole(InteractivePlayRole.BOWLING) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_role_bowl")
                    )
                }
            } else {
                val badgeAccent = if (playFilter == InteractivePlayFilter.BATTING_ONLY) TurfEmerald else BoundarySky
                Surface(
                    color = badgeAccent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, badgeAccent.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (playFilter == InteractivePlayFilter.BATTING_ONLY) "🏏 BATTING ONLY" else "🎯 BOWLING ONLY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeAccent
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (activeRole == InteractivePlayRole.BATTING) {
                PlayerVitalMiniBar(
                    label = "BAT CONFIDENCE",
                    percent = batterConfidence,
                    accent = when {
                        batterConfidence >= 70 -> AuctionGold
                        batterConfidence >= 40 -> TurfEmerald
                        else -> BoundarySky
                    }
                )
                Text(
                    text = "SHOT DIRECTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                    color = BroadcastMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ThumbChoiceButton(
                        label = ShotDirection.OFF_DRIVE.shortLabel,
                        selected = selectedShot == ShotDirection.OFF_DRIVE,
                        accent = TurfEmerald,
                        onClick = { onSelectShot(ShotDirection.OFF_DRIVE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shot_off_drive")
                    )
                    ThumbChoiceButton(
                        label = ShotDirection.STRAIGHT.shortLabel,
                        selected = selectedShot == ShotDirection.STRAIGHT,
                        accent = TurfEmerald,
                        onClick = { onSelectShot(ShotDirection.STRAIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shot_straight")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ThumbChoiceButton(
                        label = ShotDirection.LEG_GLANCE.shortLabel,
                        selected = selectedShot == ShotDirection.LEG_GLANCE,
                        accent = TurfEmerald,
                        onClick = { onSelectShot(ShotDirection.LEG_GLANCE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shot_leg_glance")
                    )
                    ThumbChoiceButton(
                        label = "LOFT 6",
                        selected = selectedShot == ShotDirection.LOFTED,
                        accent = AuctionGold,
                        onClick = { onSelectShot(ShotDirection.LOFTED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shot_lofted")
                    )
                }
            } else {
                PlayerVitalMiniBar(
                    label = "BOWLER STAMINA",
                    percent = bowlerStamina,
                    accent = when {
                        bowlerStamina < 40 -> WicketCrimson
                        bowlerStamina < 68 -> AuctionGold
                        else -> TurfEmerald
                    }
                )
                val effSpeed = selectedDelivery.effectiveSpeedKph(bowlerStamina, dewActive)
                Text(
                    text = "DELIVERY • $effSpeed KPH${if (dewActive) " • DEW" else ""}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                    color = BoundarySky,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ThumbChoiceButton(
                        label = DeliveryType.YORKER.shortLabel,
                        selected = selectedDelivery == DeliveryType.YORKER,
                        accent = WicketCrimson,
                        onClick = { onSelectDelivery(DeliveryType.YORKER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delivery_yorker")
                    )
                    ThumbChoiceButton(
                        label = DeliveryType.BOUNCER.shortLabel,
                        selected = selectedDelivery == DeliveryType.BOUNCER,
                        accent = AuctionGold,
                        onClick = { onSelectDelivery(DeliveryType.BOUNCER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delivery_bouncer")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThumbChoiceButton(
                        label = DeliveryType.GOOD_LENGTH.shortLabel,
                        selected = selectedDelivery == DeliveryType.GOOD_LENGTH,
                        accent = TurfEmerald,
                        onClick = { onSelectDelivery(DeliveryType.GOOD_LENGTH) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delivery_length")
                    )
                    ThumbChoiceButton(
                        label = DeliveryType.SLOWER.shortLabel,
                        selected = selectedDelivery == DeliveryType.SLOWER,
                        accent = BoundarySky,
                        onClick = { onSelectDelivery(DeliveryType.SLOWER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delivery_slower")
                    )
                    ThumbChoiceButton(
                        label = DeliveryType.SPIN_TURN.shortLabel,
                        selected = selectedDelivery == DeliveryType.SPIN_TURN,
                        accent = BoundarySky,
                        onClick = { onSelectDelivery(DeliveryType.SPIN_TURN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delivery_spin")
                    )
                }
                // One-Tap Bowling Field Presets
                Text(
                    text = "FIELD PRESET",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.6.sp),
                    color = BroadcastMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    FieldPreset.entries.forEach { preset ->
                        val active = preset == fieldPreset
                        val presetAccent = when (preset) {
                            FieldPreset.ATTACKING -> WicketCrimson
                            FieldPreset.BALANCED -> TurfEmerald
                            FieldPreset.DEFENSIVE -> BoundarySky
                            FieldPreset.DEATH_OVERS -> AuctionGold
                        }
                        Surface(
                            color = if (active) presetAccent.copy(alpha = 0.24f) else NavySurfaceHigh,
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(1.dp, if (active) presetAccent else NavyOutline),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { onSelectFieldPreset(preset) }
                                .testTag("field_preset_${preset.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
                                Text(
                                    text = preset.shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (active) presetAccent else BroadcastSilver
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerVitalMiniBar(
    label: String,
    percent: Int,
    accent: Color
) {
    val clamped = percent.coerceIn(0, 100)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.5.sp),
                color = BroadcastMuted
            )
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(NavyMidnight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped / 100f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
    }
}

/**
 * Right thumb ergonomic control pod:
 * - Smooth live Timing / Accuracy Gauge (EARLY / GOOD / PERFECT / LATE)
 * - Big tactile PLAY SHOT / BOWL BALL release button capturing needle timing
 */
@Composable
fun RightThumbTimingPod(
    activeRole: InteractivePlayRole,
    selectedShot: ShotDirection,
    selectedDelivery: DeliveryType,
    enabled: Boolean,
    onExecuteBall: (InteractiveBallInput) -> Unit,
    batterConfidence: Int = 45,
    bowlerStamina: Int = 100,
    fieldPreset: FieldPreset = FieldPreset.BALANCED,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sweetSpotBonus = if (activeRole == InteractivePlayRole.BATTING) {
        TimingGrade.confidenceWindowBonus(batterConfidence)
    } else {
        TimingGrade.staminaWindowPenalty(bowlerStamina)
    }
    val perfectHalf = (0.08f + sweetSpotBonus).coerceIn(0.045f, 0.145f)
    val goodHalf = (0.24f + sweetSpotBonus * 1.4f).coerceIn(0.16f, 0.34f)

    val gaugeTransition = rememberInfiniteTransition(label = "timing_gauge")
    val cursor by gaugeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (activeRole == InteractivePlayRole.BATTING) 1150 else 1050,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timing_cursor"
    )
    val liveGrade = TimingGrade.fromGaugeCursor(cursor, sweetSpotBonus)
    val gradeColor = when (liveGrade) {
        TimingGrade.PERFECT -> TurfEmerald
        TimingGrade.GOOD -> AuctionGold
        TimingGrade.EARLY, TimingGrade.LATE -> WicketCrimson
    }

    Surface(
        color = NavySurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeRole == InteractivePlayRole.BATTING) "TIMING GAUGE" else "RELEASE METER",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.6.sp),
                    color = BroadcastMuted
                )
                Text(
                    text = liveGrade.shortLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        color = gradeColor
                    )
                )
            }

            // Dynamic Color-Coded Timing Bar (widens with Batter Confidence / narrows with low Bowler Stamina)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NavyMidnight)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                    val w = size.width
                    val h = size.height
                    val leftGoodStart = (0.5f - goodHalf).coerceIn(0.08f, 0.42f)
                    val leftPerfectStart = (0.5f - perfectHalf).coerceIn(leftGoodStart, 0.48f)
                    val rightPerfectEnd = (0.5f + perfectHalf).coerceIn(0.52f, 0.92f)
                    val rightGoodEnd = (0.5f + goodHalf).coerceIn(rightPerfectEnd, 0.96f)

                    drawRect(color = WicketCrimson.copy(alpha = 0.55f), topLeft = Offset.Zero, size = Size(w * leftGoodStart, h))
                    drawRect(color = AuctionGold.copy(alpha = 0.65f), topLeft = Offset(w * leftGoodStart, 0f), size = Size(w * (leftPerfectStart - leftGoodStart), h))
                    drawRect(color = TurfEmerald, topLeft = Offset(w * leftPerfectStart, 0f), size = Size(w * (rightPerfectEnd - leftPerfectStart), h))
                    drawRect(color = AuctionGold.copy(alpha = 0.65f), topLeft = Offset(w * rightPerfectEnd, 0f), size = Size(w * (rightGoodEnd - rightPerfectEnd), h))
                    drawRect(color = WicketCrimson.copy(alpha = 0.55f), topLeft = Offset(w * rightGoodEnd, 0f), size = Size(w * (1f - rightGoodEnd), h))

                    // Bright White Sweeping Needle
                    val needleX = (cursor * w).coerceIn(3f, w - 3f)
                    drawRoundRect(
                        color = BroadcastWhite,
                        topLeft = Offset(needleX - 2.5f, 1f),
                        size = Size(5f, h - 2f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("EARLY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = BroadcastMuted)
                Text("PERFECT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = TurfEmerald)
                Text("LATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = BroadcastMuted)
            }

            // Primary Action Trigger Button (min 48dp accessible touch height!)
            val isBat = activeRole == InteractivePlayRole.BATTING
            Button(
                onClick = {
                    val grade = TimingGrade.fromGaugeCursor(cursor, sweetSpotBonus)
                    onExecuteBall(
                        InteractiveBallInput(
                            role = activeRole,
                            shotDirection = selectedShot,
                            deliveryType = selectedDelivery,
                            timingGrade = grade,
                            fieldPreset = fieldPreset
                        )
                    )
                },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBat) TurfEmerald else AuctionGold,
                    contentColor = if (isBat) OnEmeraldDark else OnGoldDark
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("execute_interactive_ball_button")
            ) {
                Icon(
                    imageVector = if (isBat) Icons.Filled.SportsCricket else Icons.Filled.SportsBaseball,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isBat) "SWING • ${selectedShot.shortLabel}" else "BOWL • ${selectedDelivery.shortLabel}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RoleMiniPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.24f) else NavySurfaceHigh,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) accent else NavyOutline),
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) accent else BroadcastSilver
                )
            )
        }
    }
}

@Composable
private fun ThumbChoiceButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.24f) else NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, if (selected) accent else NavyOutline),
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) accent else BroadcastWhite
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact 3-way Side/Phase Play Mode Switcher (Batting Only | Bowling Only | Both).
 * Works cleanly inside both Portrait Action Tab and Landscape Broadcast HUD.
 */
@Composable
fun PlayModeFilterStrip(
    playFilter: InteractivePlayFilter,
    onSelectFilter: (InteractivePlayFilter) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 3.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                Triple(InteractivePlayFilter.BATTING_ONLY, "BAT ONLY", TurfEmerald),
                Triple(InteractivePlayFilter.BOWLING_ONLY, "BOWL ONLY", BoundarySky),
                Triple(InteractivePlayFilter.BOTH, "BOTH", AuctionGold)
            )
            items.forEach { (filter, label, accent) ->
                val selected = playFilter == filter
                Surface(
                    color = if (selected) accent.copy(alpha = 0.24f) else NavySurfaceHigh.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selected) accent else NavyOutline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 32.dp else 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectFilter(filter) }
                        .testTag("play_filter_${filter.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (compact) 10.sp else 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) accent else BroadcastSilver
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shown in place of the Timing/Release Pod whenever the current innings is outside
 * the user's selected [InteractivePlayFilter] and is automatically fast-simulating.
 */
@Composable
fun SkippedPhaseAutoSimCard(
    battingTeamName: String,
    playFilter: InteractivePlayFilter,
    onSkipInningsNow: () -> Unit,
    onSwitchToBoth: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = if (playFilter == InteractivePlayFilter.BATTING_ONLY) TurfEmerald else BoundarySky
    val headline = if (playFilter == InteractivePlayFilter.BATTING_ONLY) {
        "AUTO-SIMMING OPPONENT"
    } else {
        "AUTO-SIMMING YOUR BATTING"
    }
    val sub = if (playFilter == InteractivePlayFilter.BATTING_ONLY) {
        "$battingTeamName batting • You take over when your team bats"
    } else {
        "$battingTeamName batting • You take over when your team bowls"
    }

    Surface(
        color = NavySurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.75f)),
        modifier = modifier.testTag("skipped_phase_autosim_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = if (compact) 10.sp else 11.sp),
                color = BroadcastSilver,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onSkipInningsNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuctionGold,
                    contentColor = OnGoldDark
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("skip_innings_now_button")
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "SKIP INNINGS NOW",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onSwitchToBoth,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("take_control_both_button")
            ) {
                Text(
                    text = "Play Both Sides",
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastWhite
                )
            }
        }
    }
}
