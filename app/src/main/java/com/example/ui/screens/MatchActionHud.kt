package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchFormat
import com.example.engine.ActionAnimationSnapshot
import com.example.engine.DeliveryType
import com.example.engine.FieldPreset
import com.example.engine.InningsState
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePhaseResolver
import com.example.engine.InteractivePlayFilter
import com.example.engine.InteractivePlayRole
import com.example.engine.LiveMatchState
import com.example.engine.PitchType
import com.example.engine.ShotDirection
import com.example.engine.WeatherCondition
import com.example.ui.components.CricketActionPitchCanvas
import com.example.ui.components.LeftThumbControlPod
import com.example.ui.components.MatchSoundEffects
import com.example.ui.components.PlayModeFilterStrip
import com.example.ui.components.RightThumbTimingPod
import com.example.ui.components.SkippedPhaseAutoSimCard
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
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson
import com.example.viewmodel.PlaybackSpeed
import java.util.Locale

/**
 * Full-bleed Cricket 26-inspired Landscape Broadcast Action View.
 * Automatically activates when the screen is wide/landscape (or when the user toggles Landscape Action Mode),
 * placing ergonomic Left/Right thumb controls cleanly along the sides of the animated pitch
 * with one-tap Side/Phase Play Filter (Bat Only / Bowl Only / Both), Sim Over, Sim Innings,
 * Scorecards/Stats, Bowler Picker, Impact Sub, Auto-Sim, and Sound mute.
 */
@Composable
internal fun LandscapeActionBroadcastView(
    match: LiveMatchState,
    activeInn: InningsState,
    snapshot: ActionAnimationSnapshot?,
    activeRole: InteractivePlayRole,
    playFilter: InteractivePlayFilter,
    autoSimmingSkippedPhase: Boolean,
    selectedShot: ShotDirection,
    selectedDelivery: DeliveryType,
    autoPlaying: Boolean,
    speed: PlaybackSpeed,
    sfxMuted: Boolean,
    canPickBowler: Boolean,
    canUseImpactSub: Boolean,
    soundEffects: MatchSoundEffects,
    onBack: () -> Unit,
    onToggleLandscapeLock: () -> Unit,
    onToggleSfxMute: () -> Unit,
    onOpenStatsTab: () -> Unit,
    onOpenBowlerPicker: () -> Unit,
    onOpenImpactSub: () -> Unit,
    onChangePlayFilter: (InteractivePlayFilter) -> Unit,
    onToggleRole: (InteractivePlayRole) -> Unit,
    onSelectShot: (ShotDirection) -> Unit,
    onSelectDelivery: (DeliveryType) -> Unit,
    onPlayInteractiveBall: (InteractiveBallInput) -> Unit,
    onNextOver: () -> Unit,
    onSimInnings: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onCycleSpeed: () -> Unit,
    onSimFullMatch: () -> Unit,
    onSelectFieldPreset: (FieldPreset) -> Unit = {},
    onStartDrsReview: () -> Unit = {},
    onDismissDrs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strikerName = activeInn.striker?.name ?: "Striker"
    val bowlerName = activeInn.currentBowler?.name ?: "Bowler"
    val battingTeamName = if (match.isSuperOverActive) {
        match.activeSuperOverInnings?.battingTeamName ?: activeInn.battingTeamName
    } else {
        activeInn.battingTeamName
    }
    val scoreText = if (match.isSuperOverActive) {
        val so = match.activeSuperOverInnings
        "${so?.battingTeamName ?: activeInn.battingTeamName}  ${so?.totalRuns ?: 0}/${so?.wickets ?: 0} (${so?.oversFormatted ?: "0.0"})"
    } else {
        "${activeInn.battingTeamName}  ${activeInn.totalRuns}/${activeInn.wickets} (${activeInn.oversFormatted})"
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("landscape_action_view_root")
    ) {
        val podWidth = (maxWidth * 0.28f).coerceIn(182.dp, 244.dp)

        // Full-bleed Widescreen Cricket 26 Animated Stadium Canvas
        CricketActionPitchCanvas(
            snapshot = snapshot,
            selectedShot = selectedShot,
            selectedDelivery = selectedDelivery,
            activeRole = activeRole,
            strikerName = strikerName,
            bowlerName = bowlerName,
            isRedBall = match.format == MatchFormat.TEST,
            pitchType = match.pitchType,
            dewActive = match.isDewActiveNow,
            fieldPreset = match.fieldPreset,
            soundEffects = soundEffects,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Top Broadcast Scorebug & Quick Action Strip
        Surface(
            color = NavyMidnight.copy(alpha = 0.86f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (match.isSuperOverActive) WicketCrimson else NavyOutline),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).testTag("landscape_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = BroadcastWhite
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dewChip = if (match.isDewActiveNow) " • 💧DEW" else ""
                    val drsChip = " • DRS ${activeInn.battingTeamReviewsRemaining}/${activeInn.bowlingTeamReviewsRemaining}"
                    val targetSub = match.target?.let { tgt ->
                        val rrr = match.requiredRunRate?.let { String.format(Locale.US, " • RRR %.2f", it) }.orEmpty()
                        "Target $tgt$rrr • ${match.pitchType.shortLabel}$dewChip$drsChip"
                    } ?: "${match.format.shortLabel} • CRR ${String.format(Locale.US, "%.2f", activeInn.currentRunRate)} • ${match.pitchType.shortLabel}$dewChip$drsChip"
                    Text(
                        text = if (match.isSuperOverActive) "🔥 SUPER OVER #${match.superOverRound} • $targetSub" else targetSub,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (match.isSuperOverActive) WicketCrimson else AuctionGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canPickBowler && !autoSimmingSkippedPhase) {
                    OutlinedButton(
                        onClick = onOpenBowlerPicker,
                        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Bowler", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                    }
                }
                if (canUseImpactSub) {
                    OutlinedButton(
                        onClick = onOpenImpactSub,
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(14.dp))
                        Text("Impact", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                    }
                }
                OutlinedButton(
                    onClick = onOpenStatsTab,
                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp).testTag("landscape_open_stats_button")
                ) {
                    Text("Stats", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                }
                IconButton(
                    onClick = onToggleSfxMute,
                    modifier = Modifier.size(36.dp).testTag("landscape_sfx_toggle")
                ) {
                    Icon(
                        imageVector = if (sfxMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (sfxMuted) "Unmute SFX" else "Mute SFX",
                        tint = if (sfxMuted) BroadcastSilver else TurfEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onToggleLandscapeLock,
                    modifier = Modifier.size(36.dp).testTag("landscape_toggle_orientation_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = "Switch View Layout",
                        tint = AuctionGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Floating DRS Call Banner in Landscape (when a close LBW/Caught-Behind appeal is reviewable)
        match.pendingDrs?.let { drs ->
            Surface(
                color = NavySurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.4.dp, AuctionGold),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                    .testTag("landscape_drs_prompt_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val drsReviewsLeft = if (drs.isBattingTeamReview) activeInn.battingTeamReviewsRemaining else activeInn.bowlingTeamReviewsRemaining
                    Text(
                        text = "DRS: ${drs.dismissalLabel} (${if (drs.originalDecisionOut) "Given OUT" else "NOT OUT"}) • $drsReviewsLeft left",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AuctionGold
                    )
                    Button(
                        onClick = onStartDrsReview,
                        colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = NavyMidnight),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("drs_trigger_button")
                    ) {
                        Text("Review", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    OutlinedButton(
                        onClick = onDismissDrs,
                        border = BorderStroke(1.dp, NavyOutline),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("drs_dismiss_button")
                    ) {
                        Text("Accept", style = MaterialTheme.typography.labelSmall, color = BroadcastSilver)
                    }
                }
            }
        }

        // Left Thumb Pod (Play Mode Filter + Shot/Delivery Selector + Hybrid Auto-Sim strip)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(podWidth)
                .padding(start = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PlayModeFilterStrip(
                playFilter = playFilter,
                onSelectFilter = onChangePlayFilter,
                compact = true
            )
            LeftThumbControlPod(
                activeRole = activeRole,
                selectedShot = selectedShot,
                selectedDelivery = selectedDelivery,
                onToggleRole = onToggleRole,
                onSelectShot = onSelectShot,
                onSelectDelivery = onSelectDelivery,
                playFilter = playFilter,
                fieldPreset = match.fieldPreset,
                onSelectFieldPreset = onSelectFieldPreset,
                batterConfidence = activeInn.currentBatterConfidence,
                bowlerStamina = activeInn.currentBowlerStamina,
                dewActive = match.isDewActiveNow,
                compact = true,
                modifier = Modifier.fillMaxWidth()
            )
            // Compact Hybrid Playback Controls right under Left Pod
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onToggleAutoPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (autoPlaying) WicketCrimson else NavySurface.copy(alpha = 0.92f),
                        contentColor = BroadcastWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Icon(
                        imageVector = if (autoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(if (autoPlaying) "Pause" else "Auto", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onCycleSpeed,
                    border = BorderStroke(1.dp, NavyOutline),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(speed.label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = AuctionGold)
                }
                OutlinedButton(
                    onClick = onNextOver,
                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+1Ov", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                }
            }
        }

        // Right Thumb Pod (Timing / Release Gauge OR Skipped-Phase Auto-Sim Card + Sim Inn / Match)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(podWidth)
                .padding(end = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (autoSimmingSkippedPhase) {
                SkippedPhaseAutoSimCard(
                    battingTeamName = battingTeamName,
                    playFilter = playFilter,
                    onSkipInningsNow = onSimInnings,
                    onSwitchToBoth = { onChangePlayFilter(InteractivePlayFilter.BOTH) },
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                RightThumbTimingPod(
                    activeRole = activeRole,
                    selectedShot = selectedShot,
                    selectedDelivery = selectedDelivery,
                    enabled = !match.finished,
                    onExecuteBall = onPlayInteractiveBall,
                    batterConfidence = activeInn.currentBatterConfidence,
                    bowlerStamina = activeInn.currentBowlerStamina,
                    fieldPreset = match.fieldPreset,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onSimInnings,
                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.65f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("landscape_sim_innings_button")
                ) {
                    Text("Sim Inn", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                }
                OutlinedButton(
                    onClick = onSimFullMatch,
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.65f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("landscape_sim_match_button")
                ) {
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Sim Match", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                }
            }
        }
    }
}

/**
 * Portrait / Rotation-Locked Action Tab Content:
 * Displays a wide 16:9 Cricket 26 Animated Stadium Canvas on top, a 3-way Side/Phase Play Filter
 * strip (BAT ONLY | BOWL ONLY | BOTH), ergonomic side-by-side Left/Right thumb pods (or Auto-Sim
 * status banner when skipping opponent/batting innings), plus latest ball commentary ticker.
 */
@Composable
internal fun PortraitActionTabContent(
    match: LiveMatchState,
    activeInn: InningsState,
    snapshot: ActionAnimationSnapshot?,
    activeRole: InteractivePlayRole,
    playFilter: InteractivePlayFilter,
    autoSimmingSkippedPhase: Boolean,
    selectedShot: ShotDirection,
    selectedDelivery: DeliveryType,
    soundEffects: MatchSoundEffects,
    onChangePlayFilter: (InteractivePlayFilter) -> Unit,
    onToggleRole: (InteractivePlayRole) -> Unit,
    onSelectShot: (ShotDirection) -> Unit,
    onSelectDelivery: (DeliveryType) -> Unit,
    onPlayInteractiveBall: (InteractiveBallInput) -> Unit,
    onSimInnings: () -> Unit,
    onSelectFieldPreset: (FieldPreset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strikerName = activeInn.striker?.name ?: "Striker"
    val bowlerName = activeInn.currentBowler?.name ?: "Bowler"
    val battingTeamName = if (match.isSuperOverActive) {
        match.activeSuperOverInnings?.battingTeamName ?: activeInn.battingTeamName
    } else {
        activeInn.battingTeamName
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Wide 16:9 Cricket 26 Animated Pitch View
        CricketActionPitchCanvas(
            snapshot = snapshot,
            selectedShot = selectedShot,
            selectedDelivery = selectedDelivery,
            activeRole = activeRole,
            strikerName = strikerName,
            bowlerName = bowlerName,
            isRedBall = match.format == MatchFormat.TEST,
            pitchType = match.pitchType,
            dewActive = match.isDewActiveNow,
            fieldPreset = match.fieldPreset,
            soundEffects = soundEffects,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.2f)
        )

        // Side / Phase Play Filter Selector (Batting Only | Bowling Only | Both)
        if (!match.finished) {
            PlayModeFilterStrip(
                playFilter = playFilter,
                onSelectFilter = onChangePlayFilter,
                compact = false
            )
        }

        // Side-by-side Left & Right Thumb Controls (work cleanly even with rotation locked!)
        if (!match.finished) {
            if (autoSimmingSkippedPhase) {
                SkippedPhaseAutoSimCard(
                    battingTeamName = battingTeamName,
                    playFilter = playFilter,
                    onSkipInningsNow = onSimInnings,
                    onSwitchToBoth = { onChangePlayFilter(InteractivePlayFilter.BOTH) },
                    compact = false,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    LeftThumbControlPod(
                        activeRole = activeRole,
                        selectedShot = selectedShot,
                        selectedDelivery = selectedDelivery,
                        onToggleRole = onToggleRole,
                        onSelectShot = onSelectShot,
                        onSelectDelivery = onSelectDelivery,
                        playFilter = playFilter,
                        fieldPreset = match.fieldPreset,
                        onSelectFieldPreset = onSelectFieldPreset,
                        batterConfidence = activeInn.currentBatterConfidence,
                        bowlerStamina = activeInn.currentBowlerStamina,
                        dewActive = match.isDewActiveNow,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    RightThumbTimingPod(
                        activeRole = activeRole,
                        selectedShot = selectedShot,
                        selectedDelivery = selectedDelivery,
                        enabled = !match.finished,
                        onExecuteBall = onPlayInteractiveBall,
                        batterConfidence = activeInn.currentBatterConfidence,
                        bowlerStamina = activeInn.currentBowlerStamina,
                        fieldPreset = match.fieldPreset,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Latest 1-line Ball Commentary Ticker right under controls
        val latestBall = match.commentaryFeed.firstOrNull()
        if (latestBall != null) {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = latestBall.overLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = latestBall.commentary,
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Lightweight Pre-Match Kickoff Play Mode Selector Overlay:
 * Lets the player pick Batting Only, Bowling Only, or Both Batting & Bowling right before
 * the first delivery (or fast-sim the entire match), clearly showing their team & toss result.
 */
@Composable
internal fun PreMatchPlayModeOverlay(
    match: LiveMatchState,
    userTeamId: String,
    initialFilter: InteractivePlayFilter,
    onConfirmStart: (InteractivePlayFilter, PitchType, WeatherCondition, Boolean) -> Unit,
    onQuickAutoSimMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chosenFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var chosenPitch by remember(match.pitchType) { mutableStateOf(match.pitchType) }
    var chosenWeather by remember(match.weatherCondition) { mutableStateOf(match.weatherCondition) }
    var chosenDew by remember(match.dewFactor2ndInnings) { mutableStateOf(match.dewFactor2ndInnings) }
    val userTeamName = if (match.teamB.teamId == userTeamId) match.teamB.teamName else match.teamA.teamName
    val userBattingFirst = InteractivePhaseResolver.isUserTeamBatting(match, userTeamId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight.copy(alpha = 0.90f))
            .padding(16.dp)
            .testTag("pre_match_play_mode_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.4.dp, TurfEmerald.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CHOOSE PLAY MODE",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = TurfEmerald
                        )
                    )
                    Text(
                        text = "${match.teamA.teamName} vs ${match.teamB.teamName} • ${match.format.shortLabel}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BroadcastWhite
                    )
                    if (match.tossSummary.isNotBlank()) {
                        Text(
                            text = match.tossSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = AuctionGold
                        )
                    }
                    Text(
                        text = "Your Team: $userTeamName (${if (userBattingFirst) "Batting 1st" else "Bowling 1st"})",
                        style = MaterialTheme.typography.labelMedium,
                        color = BoundarySky
                    )
                }

                // 3 Play Filter Cards
                val options = listOf(
                    Triple(
                        InteractivePlayFilter.BATTING_ONLY,
                        Icons.Filled.SportsCricket,
                        TurfEmerald
                    ),
                    Triple(
                        InteractivePlayFilter.BOWLING_ONLY,
                        Icons.Filled.SportsBaseball,
                        BoundarySky
                    ),
                    Triple(
                        InteractivePlayFilter.BOTH,
                        Icons.Filled.Bolt,
                        AuctionGold
                    )
                )

                options.forEach { (filter, icon, accent) ->
                    val selected = chosenFilter == filter
                    Surface(
                        color = if (selected) accent.copy(alpha = 0.18f) else NavySurfaceHigh,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) accent else NavyOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { chosenFilter = filter }
                            .testTag("setup_mode_${filter.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) accent else BroadcastSilver,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) accent else BroadcastWhite
                                )
                                Text(
                                    text = filter.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Pitch & Weather Conditions Selector
                Surface(
                    color = NavySurfaceHigh,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PITCH & MATCH CONDITIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AuctionGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PitchType.entries.forEach { pt ->
                                val selected = pt == chosenPitch
                                val accent = when (pt) {
                                    PitchType.GREEN -> TurfEmerald
                                    PitchType.DUSTY -> AuctionGold
                                    PitchType.FLAT -> BoundarySky
                                    PitchType.BALANCED -> BroadcastSilver
                                }
                                Surface(
                                    color = if (selected) accent.copy(alpha = 0.22f) else NavyMidnight,
                                    shape = RoundedCornerShape(9.dp),
                                    border = BorderStroke(if (selected) 1.4.dp else 1.dp, if (selected) accent else NavyOutline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { chosenPitch = pt }
                                        .testTag("pitch_select_${pt.name.lowercase()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = pt.shortLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (selected) accent else BroadcastSilver
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${chosenPitch.label}: ${chosenPitch.summary}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = BroadcastSilver
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WeatherCondition.entries.forEach { w ->
                                val selected = w == chosenWeather
                                Surface(
                                    color = if (selected) BoundarySky.copy(alpha = 0.2f) else NavyMidnight,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (selected) BoundarySky else NavyOutline),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            chosenWeather = w
                                            if (w == WeatherCondition.CLEAR_NIGHT) chosenDew = true
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 2.dp)) {
                                        Text(
                                            text = w.label.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = if (selected) BoundarySky else BroadcastMuted,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        // 2nd-Innings Dew Factor Toggle
                        Surface(
                            color = if (chosenDew) BoundarySky.copy(alpha = 0.16f) else NavyMidnight,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (chosenDew) BoundarySky else NavyOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chosenDew = !chosenDew }
                                .testTag("toggle_dew_factor_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💧 2nd-Innings Dew Factor (aids chasing)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (chosenDew) BroadcastWhite else BroadcastMuted
                                )
                                Text(
                                    text = if (chosenDew) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (chosenDew) BoundarySky else BroadcastMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // Confirm & Start Interactive Match Button
                val startHint = when {
                    chosenFilter == InteractivePlayFilter.BATTING_ONLY && !userBattingFirst ->
                        "Start (Auto-Sim Opponent 1st Inn → Bat)"
                    chosenFilter == InteractivePlayFilter.BOWLING_ONLY && userBattingFirst ->
                        "Start (Auto-Sim Your 1st Inn → Bowl)"
                    else -> "Start Match (${chosenFilter.label})"
                }
                Button(
                    onClick = { onConfirmStart(chosenFilter, chosenPitch, chosenWeather, chosenDew) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = OnEmeraldDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_play_mode_start_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = startHint,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onQuickAutoSimMatch,
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("setup_quick_sim_match_button")
                ) {
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Quick Auto-Sim Full Match", style = MaterialTheme.typography.labelLarge, color = AuctionGold)
                }
            }
        }
    }
}
