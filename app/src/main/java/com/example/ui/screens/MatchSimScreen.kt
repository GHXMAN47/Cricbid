package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchFormat
import com.example.engine.ActionAnimationSnapshot
import com.example.engine.BallEvent
import com.example.engine.BallOutcomeType
import com.example.engine.CareerControlMode
import com.example.engine.DeliveryType
import com.example.engine.FieldPreset
import com.example.engine.InningsState
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePhaseResolver
import com.example.engine.InteractivePlayFilter
import com.example.engine.InteractivePlayRole
import com.example.engine.LiveMatchState
import com.example.engine.MatchSimulationEngine
import com.example.engine.PitchType
import com.example.engine.ShotDirection
import com.example.engine.WeatherCondition
import com.example.ui.components.BallOutcomeChip
import com.example.ui.components.DrsReviewOverlay
import com.example.ui.components.MatchSoundEffects
import com.example.ui.components.PitchMapChartCard
import com.example.ui.components.RolePillBadge
import com.example.ui.components.RunRateWormGraph
import com.example.ui.components.WagonWheelChartCard
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSimScreen(
    match: LiveMatchState,
    autoPlaying: Boolean,
    speed: PlaybackSpeed,
    onBack: () -> Unit,
    onNextBall: () -> Unit,
    onNextOver: () -> Unit,
    onNextFiveOvers: () -> Unit = {},
    onSimInnings: () -> Unit,
    onSimFullMatch: () -> Unit,
    onToggleAutoPlay: () -> Unit,
    onCycleSpeed: () -> Unit,
    onApplyImpactSub: (teamId: String, benchPlayerId: String, outPlayerId: String) -> Unit,
    onSelectBowler: (bowlerPlayerId: String) -> Unit = {},
    lastActionSnapshot: ActionAnimationSnapshot? = null,
    onPlayInteractiveBall: (InteractiveBallInput) -> Unit = { onNextBall() },
    userTeamId: String = "",
    playFilter: InteractivePlayFilter = InteractivePlayFilter.BOTH,
    showPlayModeSetup: Boolean = false,
    autoSimmingSkippedPhase: Boolean = false,
    onConfirmPlayModeSetup: (InteractivePlayFilter, PitchType, WeatherCondition, Boolean, CareerControlMode?) -> Unit = { _, _, _, _, _ -> },
    onChangePlayFilter: (InteractivePlayFilter) -> Unit = {},
    showDrsOverlay: Boolean = false,
    onSelectFieldPreset: (FieldPreset) -> Unit = {},
    onStartDrsReview: () -> Unit = {},
    onCompleteDrsReview: () -> Unit = {},
    onDismissDrs: () -> Unit = {},
    orangeCapPlayerId: String? = null,
    purpleCapPlayerId: String? = null,
    careerPlayerId: String? = null,
    careerPlayerName: String? = null,
    careerControlMode: CareerControlMode = CareerControlMode.FULL_TEAM,
    autoJumpLandscapeSignal: Long = 0L,
    onChangeCareerControlMode: (CareerControlMode) -> Unit = {},
    onSkipToMyTurn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var activeTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Action, 1 = Comm, 2 = Worm, 3 = Card
    var scorecardInningsTab by rememberSaveable { mutableIntStateOf(1) }
    var showImpactSheet by remember { mutableStateOf(false) }
    var showBowlerSheet by remember { mutableStateOf(false) }
    var preferLandscapeAction by rememberSaveable { mutableStateOf(false) }
    var showStatsInLandscape by rememberSaveable { mutableStateOf(false) }
    var selectedShot by rememberSaveable { mutableStateOf(ShotDirection.STRAIGHT) }
    var selectedDelivery by rememberSaveable { mutableStateOf(DeliveryType.GOOD_LENGTH) }
    var sfxMuted by rememberSaveable { mutableStateOf(false) }

    val activeInn = match.activeInnings
    val resolvedUserTeamId = remember(match.teamA.teamId, match.teamB.teamId, userTeamId) {
        InteractivePhaseResolver.resolveUserTeamId(match, userTeamId)
    }
    val expectedRole = remember(
        activeInn.battingTeamId,
        match.isSuperOverActive,
        match.activeSuperOverInnings?.battingTeamId,
        resolvedUserTeamId,
        playFilter
    ) {
        InteractivePhaseResolver.expectedActiveRole(match, resolvedUserTeamId, playFilter)
    }
    var activeRole by rememberSaveable { mutableStateOf(expectedRole) }
    LaunchedEffect(expectedRole) {
        activeRole = expectedRole
    }

    // Automatically jump straight into interactive Landscape Broadcast gameplay when the created player's turn arrives
    LaunchedEffect(autoJumpLandscapeSignal) {
        if (autoJumpLandscapeSignal > 0L && !match.finished) {
            activeTab = 0
            showStatsInLandscape = false
            preferLandscapeAction = true
        }
    }

    // Automatically jump to Scorecard when match concludes so summary & POTM are front-and-center
    LaunchedEffect(match.finished) {
        if (match.finished) {
            preferLandscapeAction = false
            if (activeTab == 0) {
                activeTab = 3
            }
        }
    }

    val context = LocalContext.current

    // Lightweight synthesized cricket sound effects + haptics with clean resource disposal
    val soundEffects = remember(context) { MatchSoundEffects(context) }
    LaunchedEffect(sfxMuted) {
        soundEffects.muted = sfxMuted
    }
    DisposableEffect(Unit) {
        onDispose { soundEffects.release() }
    }

    // Optional Landscape Broadcast lock toggle (always restores UNSPECIFIED cleanly on exit)
    DisposableEffect(preferLandscapeAction) {
        val activity = context.findActivity()
        if (preferLandscapeAction) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val hasAnyBench = match.teamA.benchPlayers.isNotEmpty() || match.teamB.benchPlayers.isNotEmpty()
    val canUseImpactSub = match.format.impactSubAllowed &&
        !match.finished &&
        !match.isSuperOverActive &&
        hasAnyBench &&
        (!match.teamAImpactUsed || !match.teamBImpactUsed)
    val canPickBowler = !match.finished && !match.isSuperOverActive && activeInn.isStartOfOver

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isLandscapeViewport = maxWidth > maxHeight
            val showFullLandscapeAction = isLandscapeViewport && !match.finished && !showStatsInLandscape

            if (showFullLandscapeAction) {
                LandscapeActionBroadcastView(
                    match = match,
                    activeInn = activeInn,
                    snapshot = lastActionSnapshot,
                    activeRole = activeRole,
                    playFilter = playFilter,
                    autoSimmingSkippedPhase = autoSimmingSkippedPhase,
                    selectedShot = selectedShot,
                    selectedDelivery = selectedDelivery,
                    autoPlaying = autoPlaying,
                    speed = speed,
                    sfxMuted = sfxMuted,
                    canPickBowler = canPickBowler,
                    canUseImpactSub = canUseImpactSub,
                    soundEffects = soundEffects,
                    onBack = onBack,
                    onToggleLandscapeLock = {
                        if (preferLandscapeAction) {
                            preferLandscapeAction = false
                        } else {
                            showStatsInLandscape = true
                        }
                    },
                    onToggleSfxMute = { sfxMuted = !sfxMuted },
                    onOpenStatsTab = {
                        activeTab = 1
                        showStatsInLandscape = true
                    },
                    onOpenBowlerPicker = { showBowlerSheet = true },
                    onOpenImpactSub = { showImpactSheet = true },
                    onChangePlayFilter = onChangePlayFilter,
                    onToggleRole = { activeRole = it },
                    onSelectShot = { selectedShot = it },
                    onSelectDelivery = { selectedDelivery = it },
                    onPlayInteractiveBall = onPlayInteractiveBall,
                    onNextOver = onNextOver,
                    onSimInnings = onSimInnings,
                    onToggleAutoPlay = onToggleAutoPlay,
                    onCycleSpeed = onCycleSpeed,
                    onSimFullMatch = onSimFullMatch,
                    onSelectFieldPreset = onSelectFieldPreset,
                    onStartDrsReview = onStartDrsReview,
                    onDismissDrs = onDismissDrs,
                    userTeamId = resolvedUserTeamId,
                    careerPlayerId = careerPlayerId,
                    careerPlayerName = careerPlayerName,
                    careerControlMode = careerControlMode,
                    onChangeCareerControlMode = onChangeCareerControlMode,
                    onSkipToMyTurn = onSkipToMyTurn
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (isLandscapeViewport) 880.dp else 620.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("match_sim_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Match Hub",
                                tint = BroadcastWhite
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${match.teamA.teamName} vs ${match.teamB.teamName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = BroadcastWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    match.finished -> "${match.format.shortLabel} • ${match.stage.label.uppercase()} • FULL TIME"
                                    match.isSuperOverActive -> "🔥 SUPER OVER #${match.superOverRound} • 1 OV • MAX 2 WKTS"
                                    else -> "${match.format.shortLabel} • ${playFilter.shortLabel} • ${activeInn.phaseLabel}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    match.isSuperOverActive -> WicketCrimson
                                    match.finished -> AuctionGold
                                    else -> TurfEmerald
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { sfxMuted = !sfxMuted },
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("match_sfx_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (sfxMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (sfxMuted) "Unmute Match Sound" else "Mute Match Sound",
                                tint = if (sfxMuted) BroadcastSilver else TurfEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (!match.finished) {
                            IconButton(
                                onClick = {
                                    if (isLandscapeViewport && showStatsInLandscape) {
                                        showStatsInLandscape = false
                                    } else {
                                        showStatsInLandscape = false
                                        preferLandscapeAction = !preferLandscapeAction
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("match_landscape_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ScreenRotation,
                                    contentDescription = "Toggle Landscape Action View",
                                    tint = if (preferLandscapeAction) TurfEmerald else AuctionGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (canPickBowler && !autoSimmingSkippedPhase && !(careerControlMode == CareerControlMode.PLAYER_ONLY && !careerPlayerId.isNullOrBlank())) {
                            OutlinedButton(
                                onClick = { showBowlerSheet = true },
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("open_bowler_picker_button")
                            ) {
                                Text("Bowler", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (canUseImpactSub) {
                            OutlinedButton(
                                onClick = { showImpactSheet = true },
                                border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("open_impact_sub_button")
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Impact", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        if (!match.finished) {
                            OutlinedButton(
                                onClick = onSimFullMatch,
                                border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("sim_full_match_button")
                            ) {
                                Text("Finish", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                            }
                        }
                    }

                    // Live Scorebug Card
                    LiveScorebugCard(
                        match = match,
                        activeInn = activeInn,
                        orangeCapPlayerId = orangeCapPlayerId,
                        purpleCapPlayerId = purpleCapPlayerId,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // DRS Review Prompt Banner (close LBW / Caught Behind decision)
                    val pendingDrs = match.pendingDrs
                    if (pendingDrs != null && !showDrsOverlay) {
                        Surface(
                            color = AuctionGold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.4.dp, AuctionGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("portrait_drs_prompt_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val drsReviewsLeft = if (pendingDrs.isBattingTeamReview) activeInn.battingTeamReviewsRemaining else activeInn.bowlingTeamReviewsRemaining
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DRS AVAILABLE • ${pendingDrs.dismissalLabel.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AuctionGold,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Text(
                                        text = "On-field: ${if (pendingDrs.originalDecisionOut) "OUT" else "NOT OUT"} (${pendingDrs.batterName}) • $drsReviewsLeft reviews left",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = onStartDrsReview,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AuctionGold,
                                        contentColor = NavyMidnight
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("drs_trigger_button")
                                ) {
                                    Text("Review", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                                }
                                OutlinedButton(
                                    onClick = onDismissDrs,
                                    border = BorderStroke(1.dp, NavyOutline),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("drs_dismiss_button")
                                ) {
                                    Text("Accept", style = MaterialTheme.typography.labelSmall, color = BroadcastSilver)
                                }
                            }
                        }
                    }

                    // Playback & Quick-Skip Controls Ribbon (when match not finished)
                    if (!match.finished) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onToggleAutoPlay,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (autoPlaying) WicketCrimson else TurfEmerald,
                                    contentColor = if (autoPlaying) BroadcastWhite else OnEmeraldDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("match_autoplay_button")
                            ) {
                                Icon(
                                    imageVector = if (autoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (autoPlaying) "Pause" else "Auto Sim",
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(if (autoPlaying) "Pause" else "Auto", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onCycleSpeed,
                                border = BorderStroke(1.dp, NavyOutline),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("match_speed_button")
                            ) {
                                Text(speed.label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = AuctionGold)
                            }

                            OutlinedButton(
                                onClick = onNextBall,
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("next_ball_button")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(15.dp))
                                Text("+1B", style = MaterialTheme.typography.labelSmall, color = BroadcastWhite)
                            }

                            OutlinedButton(
                                onClick = onNextOver,
                                border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("next_over_button")
                            ) {
                                Icon(Icons.Filled.FastForward, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(15.dp))
                                Text("+1Ov", style = MaterialTheme.typography.labelSmall, color = BroadcastWhite)
                            }

                            OutlinedButton(
                                onClick = onSimInnings,
                                border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("sim_innings_ribbon_button")
                            ) {
                                Text("Sim Inn", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AuctionGold)
                            }

                            if (match.format != MatchFormat.T20I) {
                                OutlinedButton(
                                    onClick = onNextFiveOvers,
                                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("next_five_overs_button")
                                ) {
                                    Text("+5Ov", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                                }
                            }
                        }
                    }

                    // Tab Switcher: Action | Comm | Worm | Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavySurfaceHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SimTabButton(
                            label = "Action",
                            selected = activeTab == 0,
                            onClick = {
                                activeTab = 0
                                if (isLandscapeViewport && !match.finished) {
                                    showStatsInLandscape = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("tab_action")
                        )
                        SimTabButton("Comm", activeTab == 1, { activeTab = 1 }, Modifier.weight(1f).testTag("tab_commentary"))
                        SimTabButton("Worm", activeTab == 2, { activeTab = 2 }, Modifier.weight(1f).testTag("tab_worm_graph"))
                        SimTabButton("Card", activeTab == 3, { activeTab = 3 }, Modifier.weight(1f).testTag("tab_scorecard"))
                    }

                    // Tab Content
                    when (activeTab) {
                        0 -> Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            PortraitActionTabContent(
                                match = match,
                                activeInn = activeInn,
                                snapshot = lastActionSnapshot,
                                activeRole = activeRole,
                                playFilter = playFilter,
                                autoSimmingSkippedPhase = autoSimmingSkippedPhase,
                                selectedShot = selectedShot,
                                selectedDelivery = selectedDelivery,
                                soundEffects = soundEffects,
                                onChangePlayFilter = onChangePlayFilter,
                                onToggleRole = { activeRole = it },
                                onSelectShot = { selectedShot = it },
                                onSelectDelivery = { selectedDelivery = it },
                                onPlayInteractiveBall = onPlayInteractiveBall,
                                onSimInnings = onSimInnings,
                                onSelectFieldPreset = onSelectFieldPreset,
                                userTeamId = resolvedUserTeamId,
                                careerPlayerId = careerPlayerId,
                                careerPlayerName = careerPlayerName,
                                careerControlMode = careerControlMode,
                                onChangeCareerControlMode = onChangeCareerControlMode,
                                onSkipToMyTurn = onSkipToMyTurn
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        1 -> CommentaryTabContent(
                            feed = match.commentaryFeed,
                            onSimInnings = onSimInnings,
                            finished = match.finished,
                            modifier = Modifier.weight(1f)
                        )
                        2 -> WormGraphTabContent(
                            match = match,
                            modifier = Modifier.weight(1f)
                        )
                        3 -> ScorecardTabContent(
                            match = match,
                            selectedInnings = scorecardInningsTab,
                            onSelectInnings = { scorecardInningsTab = it },
                            orangeCapPlayerId = orangeCapPlayerId,
                            purpleCapPlayerId = purpleCapPlayerId,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Animated DRS Hawk-Eye Ball-Tracking / UltraEdge Overlay
        val drsDecision = match.pendingDrs
        if (showDrsOverlay && drsDecision != null) {
            DrsReviewOverlay(
                decision = drsDecision,
                soundEffects = soundEffects,
                onComplete = onCompleteDrsReview
            )
        }

        // Pre-Match Play Mode Setup Overlay (Player-Only default in Career, or Bat/Bowl/Both)
        if (showPlayModeSetup && !match.finished) {
            PreMatchPlayModeOverlay(
                match = match,
                userTeamId = resolvedUserTeamId,
                initialFilter = playFilter,
                onConfirmStart = onConfirmPlayModeSetup,
                onQuickAutoSimMatch = onSimFullMatch,
                careerPlayerId = careerPlayerId,
                careerPlayerName = careerPlayerName,
                initialCareerControlMode = careerControlMode
            )
        }

        // IPL Impact Player Substitution Bottom Sheet
        if (showImpactSheet) {
            ImpactSubBottomSheet(
                match = match,
                onDismiss = { showImpactSheet = false },
                onConfirmSub = { teamId, benchId, outId ->
                    onApplyImpactSub(teamId, benchId, outId)
                    showImpactSheet = false
                }
            )
        }

        // Manual Bowler Selection Sheet (strict 4-over cap & no consecutive overs)
        if (showBowlerSheet && canPickBowler) {
            BowlerPickerBottomSheet(
                innings = activeInn,
                onDismiss = { showBowlerSheet = false },
                onSelectBowler = { bowlerId ->
                    onSelectBowler(bowlerId)
                    showBowlerSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BowlerPickerBottomSheet(
    innings: InningsState,
    onDismiss: () -> Unit,
    onSelectBowler: (String) -> Unit
) {
    val capText = if (innings.format == MatchFormat.TEST) {
        "No over cap • Consecutive overs blocked"
    } else {
        "Max ${innings.format.maxOversPerBowler}.0 overs/bowler • No consecutive overs"
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NavySurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SELECT BOWLER — OVER ${(innings.legalBalls / 6) + 1}",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DisplayFontFamily, color = TurfEmerald)
            )
            Text(
                text = capText,
                style = MaterialTheme.typography.bodySmall,
                color = BroadcastSilver
            )
            Spacer(Modifier.height(4.dp))
            innings.bowlingCard.forEachIndexed { idx, entry ->
                val blockReason = innings.bowlerBlockReason(entry)
                val isCurrent = idx == innings.currentBowlerIdx
                val eligible = blockReason == null
                Surface(
                    color = when {
                        !eligible -> NavySurfaceHigh.copy(alpha = 0.5f)
                        isCurrent -> TurfEmerald.copy(alpha = 0.2f)
                        else -> NavySurfaceHigh
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.2.dp,
                        when {
                            !eligible -> WicketCrimson.copy(alpha = 0.35f)
                            isCurrent -> TurfEmerald
                            else -> NavyOutline
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = eligible) { onSelectBowler(entry.playerId) }
                        .testTag("pick_bowler_${entry.playerId}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = entry.playerName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (eligible) BroadcastWhite else BroadcastMuted
                                )
                                if (entry.isCaptain) {
                                    CaptainTagBadge()
                                }
                            }
                            Text(
                                text = "${entry.oversFormatted} ov • ${entry.wickets}/${entry.runsConceded} (Econ ${MatchSimulationEngine.formatRate(entry.economy)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = BroadcastSilver
                            )
                        }
                        if (blockReason != null) {
                            Surface(
                                color = WicketCrimson.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = blockReason.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(color = WicketCrimson, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else if (isCurrent) {
                            Surface(
                                color = TurfEmerald.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SELECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TurfEmerald, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        } else {
                            Text("BOWL", style = MaterialTheme.typography.labelMedium.copy(color = BoundarySky, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImpactSubBottomSheet(
    match: LiveMatchState,
    onDismiss: () -> Unit,
    onConfirmSub: (teamId: String, benchPlayerId: String, outPlayerId: String) -> Unit
) {
    val defaultTeam = if (!match.teamAImpactUsed && match.teamA.benchPlayers.isNotEmpty()) match.teamA else match.teamB
    var selectedTeamId by remember { mutableStateOf(defaultTeam.teamId) }
    val selectedTeam = if (selectedTeamId == match.teamA.teamId) match.teamA else match.teamB
    val teamAlreadyUsed = if (selectedTeamId == match.teamA.teamId) match.teamAImpactUsed else match.teamBImpactUsed
    val bench = selectedTeam.benchPlayers
    val activeXi = if (selectedTeamId == match.teamA.teamId) match.innings1.battingLineup else match.innings2.battingLineup

    var selectedBenchId by remember(selectedTeamId) { mutableStateOf(bench.firstOrNull()?.id ?: "") }
    var selectedOutId by remember(selectedTeamId) { mutableStateOf(activeXi.lastOrNull()?.id ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NavySurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ IPL IMPACT PLAYER RULE (1 SUB)",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DisplayFontFamily, color = BoundarySky)
            )
            Text(
                text = "Swap 1 bench player into the match XI (respecting max 4 overseas rule).",
                style = MaterialTheme.typography.bodySmall,
                color = BroadcastSilver
            )

            // Team switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SimTabButton(
                    label = match.teamA.teamName + if (match.teamAImpactUsed) " (Used)" else "",
                    selected = selectedTeamId == match.teamA.teamId,
                    onClick = { selectedTeamId = match.teamA.teamId },
                    modifier = Modifier.weight(1f)
                )
                SimTabButton(
                    label = match.teamB.teamName + if (match.teamBImpactUsed) " (Used)" else "",
                    selected = selectedTeamId == match.teamB.teamId,
                    onClick = { selectedTeamId = match.teamB.teamId },
                    modifier = Modifier.weight(1f)
                )
            }

            if (teamAlreadyUsed) {
                Text(
                    text = "Impact Sub already activated for ${selectedTeam.teamName}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuctionGold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else if (bench.isEmpty()) {
                Text(
                    text = "No bench players available for ${selectedTeam.teamName} (11-player squad).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BroadcastMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Text("IMPACT PLAYER IN (BENCH)", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bench, key = { it.id }) { p ->
                        val chosen = p.id == selectedBenchId
                        Surface(
                            color = if (chosen) TurfEmerald.copy(alpha = 0.2f) else NavySurfaceHigh,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, if (chosen) TurfEmerald else NavyOutline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedBenchId = p.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(p.name, style = MaterialTheme.typography.labelMedium, color = BroadcastWhite)
                                RolePillBadge(role = p.role)
                            }
                        }
                    }
                }

                Text("REPLACE PLAYER OUT (XI)", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activeXi, key = { it.id }) { p ->
                        val chosen = p.id == selectedOutId
                        Surface(
                            color = if (chosen) AuctionGold.copy(alpha = 0.2f) else NavySurfaceHigh,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, if (chosen) AuctionGold else NavyOutline),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedOutId = p.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(p.name, style = MaterialTheme.typography.labelMedium, color = BroadcastWhite)
                                RolePillBadge(role = p.role)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (selectedBenchId.isNotBlank() && selectedOutId.isNotBlank()) {
                            onConfirmSub(selectedTeam.teamId, selectedBenchId, selectedOutId)
                        }
                    },
                    enabled = selectedBenchId.isNotBlank() && selectedOutId.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BoundarySky, contentColor = NavyMidnight),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_impact_sub_button")
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Activate Impact Player", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LiveScorebugCard(
    match: LiveMatchState,
    activeInn: InningsState,
    orangeCapPlayerId: String? = null,
    purpleCapPlayerId: String? = null,
    modifier: Modifier = Modifier
) {
    val activeSo = match.activeSuperOverInnings
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.4.dp,
            when {
                match.isSuperOverActive -> WicketCrimson.copy(alpha = 0.85f)
                match.finished -> AuctionGold.copy(alpha = 0.7f)
                else -> NavyOutline
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (match.tossSummary.isNotBlank()) {
                Text(
                    text = match.tossSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = AuctionGold.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .testTag("match_toss_banner")
                )
            }
            if (match.finished) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = match.resultSummary,
                        style = MaterialTheme.typography.titleMedium.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "Player of the Match: ${match.playerOfTheMatch}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TurfEmerald,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(match.teamA.teamName, style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                        Text(
                            "${match.innings1.totalRuns}/${match.innings1.wickets} (${match.innings1.oversFormatted})",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, color = TurfEmerald)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(match.teamB.teamName, style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                        Text(
                            "${match.innings2.totalRuns}/${match.innings2.wickets} (${match.innings2.oversFormatted})",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, color = AuctionGold)
                        )
                    }
                }
                if (match.superOverHistory.isNotEmpty()) {
                    val lastSo = match.superOverHistory.last()
                    Text(
                        text = "🔥 Super Over #${lastSo.roundNumber}: ${lastSo.team1Name} ${lastSo.team1Score}/${lastSo.team1Wickets} vs ${lastSo.team2Name} ${lastSo.team2Score}/${lastSo.team2Wickets}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = WicketCrimson,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else if (activeSo != null) {
                // Live Super Over Scorebug
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "🔥 SUPER OVER #${activeSo.roundNumber} • ${activeSo.battingTeamName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AuctionGold
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${activeSo.totalRuns}/${activeSo.wickets}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    color = BroadcastWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("live_score_text")
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(${activeSo.oversFormatted}/1.0 ov • max 2 wkts)",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, color = BroadcastSilver),
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        match.target?.let { tgt ->
                            val needed = (tgt - activeSo.totalRuns).coerceAtLeast(0)
                            val ballsLeft = (6 - activeSo.legalBalls).coerceAtLeast(0)
                            Text(
                                text = "Need $needed off $ballsLeft",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                color = AuctionGold
                            )
                        } ?: Text(
                            text = "Regulation Tied ${match.innings1.totalRuns}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BoundarySky
                        )
                        Text(
                            text = "Bowler: ${activeSo.bowler.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WicketCrimson
                        )
                    }
                }
                if (activeSo.recentBalls.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeSo.recentBalls.takeLast(8)) { ball ->
                            BallOutcomeChip(outcome = ball.outcome)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = activeInn.battingTeamName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TurfEmerald
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${activeInn.totalRuns}/${activeInn.wickets}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    color = BroadcastWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("live_score_text")
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "(${activeInn.oversFormatted} ov)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    color = BroadcastSilver
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "CRR ${MatchSimulationEngine.formatRate(activeInn.currentRunRate)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, color = BoundarySky)
                        )
                        match.target?.let { tgt ->
                            val needed = (tgt - match.innings2.totalRuns).coerceAtLeast(0)
                            val ballsLeft = (match.format.maxBalls - match.innings2.legalBalls).coerceAtLeast(0)
                            val targetNote = if (match.format == MatchFormat.TEST) {
                                "Need $needed (${ballsLeft / 6} ov left)"
                            } else {
                                "Need $needed off $ballsLeft (RRR ${MatchSimulationEngine.formatRate(match.requiredRunRate ?: 0.0)})"
                            }
                            Text(
                                text = targetNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = AuctionGold
                            )
                        } ?: Text(
                            text = "vs ${activeInn.bowlingTeamName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastMuted
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Current Batters & Bowler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val strikerCard = activeInn.battingCard.getOrNull(activeInn.strikerIdx)
                    val nonStrikerCard = activeInn.battingCard.getOrNull(activeInn.nonStrikerIdx)
                    val bowlerCard = activeInn.bowlingCard.getOrNull(activeInn.currentBowlerIdx)
                    Column(modifier = Modifier.weight(1f)) {
                        strikerCard?.let {
                            val capTag = if (it.isCaptain) " (C)" else ""
                            val orangeTag = if (orangeCapPlayerId != null && it.playerId == orangeCapPlayerId) " 🟠" else ""
                            val confColor = when {
                                it.confidence >= 75 -> TurfEmerald
                                it.confidence >= 45 -> AuctionGold
                                else -> BroadcastSilver
                            }
                            Text(
                                text = "🏏 ${it.playerName}$capTag$orangeTag*  ${it.runs}(${it.balls})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "   Confidence ${it.confidence}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontSize = 10.sp),
                                color = confColor
                            )
                        }
                        nonStrikerCard?.let {
                            val capTag = if (it.isCaptain) " (C)" else ""
                            val orangeTag = if (orangeCapPlayerId != null && it.playerId == orangeCapPlayerId) " 🟠" else ""
                            Text(
                                text = "   ${it.playerName}$capTag$orangeTag  ${it.runs}(${it.balls})",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    bowlerCard?.let {
                        val staminaColor = when {
                            it.stamina < 40 -> WicketCrimson
                            it.stamina < 65 -> AuctionGold
                            else -> TurfEmerald
                        }
                        val bowlerCapTag = if (it.isCaptain) " (C)" else ""
                        val purpleTag = if (purpleCapPlayerId != null && it.playerId == purpleCapPlayerId) " 🟣" else ""
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${it.playerName}$bowlerCapTag$purpleTag",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = BroadcastSilver
                            )
                            Text(
                                text = "${it.wickets}-${it.runsConceded} (${it.oversFormatted})",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, color = WicketCrimson)
                            )
                            Text(
                                text = "Stamina ${it.stamina}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = staminaColor
                            )
                        }
                    }
                }

                // Pomp-Style Pitch / Dew / DRS / Partnership Telemetry Strip
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dewLabel = if (match.isDewActiveNow) " • 💧DEW" else ""
                    Text(
                        text = "${match.pitchType.shortLabel} • ${match.weatherCondition.label.split(" ").first()}$dewLabel • P'ship ${activeInn.partnershipRuns}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.isDewActiveNow) BoundarySky else BroadcastMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "DRS ${activeInn.battingTeamReviewsRemaining}/${activeInn.bowlingTeamReviewsRemaining} • ${match.fieldPreset.shortLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = AuctionGold
                    )
                }

                val impactBanner = listOfNotNull(match.teamAImpactLog, match.teamBImpactLog).lastOrNull()
                if (impactBanner != null) {
                    Text(
                        text = "⚡ Impact Sub: $impactBanner",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuctionGold,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val latestInjury = match.injuryAlerts.lastOrNull()
                if (latestInjury != null) {
                    Text(
                        text = "🚑 Injury Update: $latestInjury",
                        style = MaterialTheme.typography.labelSmall,
                        color = WicketCrimson,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Recent Balls Ribbon
                if (activeInn.recentBalls.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeInn.recentBalls.takeLast(8)) { ball ->
                            BallOutcomeChip(outcome = ball.outcome)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) TurfEmerald else NavySurfaceHigh,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) OnEmeraldDark else BroadcastSilver,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun CommentaryTabContent(
    feed: List<BallEvent>,
    onSimInnings: () -> Unit,
    finished: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (feed.isEmpty()) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Players are walking out to the crease!",
                            style = MaterialTheme.typography.titleMedium,
                            color = BroadcastWhite
                        )
                        Text(
                            text = "Tap Play, +1 Ball, or +1 Ov above to begin live T20 commentary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        } else {
            if (!finished) {
                item {
                    OutlinedButton(
                        onClick = onSimInnings,
                        border = BorderStroke(1.dp, NavyOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text("Simulate Rest of Current Innings", style = MaterialTheme.typography.labelMedium, color = BroadcastSilver)
                    }
                }
            }
            items(feed) { event ->
                val borderAccent = when {
                    event.isSuperOver -> AuctionGold.copy(alpha = 0.85f)
                    event.isInjuryAlert -> WicketCrimson.copy(alpha = 0.85f)
                    event.outcome == BallOutcomeType.SIX -> AuctionGold.copy(alpha = 0.65f)
                    event.outcome == BallOutcomeType.FOUR -> BoundarySky.copy(alpha = 0.6f)
                    event.outcome == BallOutcomeType.WICKET -> WicketCrimson.copy(alpha = 0.7f)
                    else -> NavyOutline
                }
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BallOutcomeChip(outcome = event.outcome)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.commentary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastWhite
                            )
                            Text(
                                text = "Inn ${event.inningsNumber} • Score ${event.scoreAfterBall}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastMuted,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WormGraphTabContent(
    match: LiveMatchState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RunRateWormGraph(
                teamAName = match.teamA.teamName,
                teamBName = match.teamB.teamName,
                innings1Worm = match.innings1.wormByOver,
                innings2Worm = match.innings2.wormByOver,
                innings1WicketOvers = match.innings1.wicketOvers,
                innings2WicketOvers = match.innings2.wicketOvers,
                maxOvers = match.format.maxOvers
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InningsRateCard(
                    teamName = match.teamA.teamName,
                    score = "${match.innings1.totalRuns}/${match.innings1.wickets}",
                    overs = match.innings1.oversFormatted,
                    runRate = MatchSimulationEngine.formatRate(match.innings1.currentRunRate),
                    accent = TurfEmerald,
                    modifier = Modifier.weight(1f)
                )
                InningsRateCard(
                    teamName = match.teamB.teamName,
                    score = "${match.innings2.totalRuns}/${match.innings2.wickets}",
                    overs = match.innings2.oversFormatted,
                    runRate = MatchSimulationEngine.formatRate(match.innings2.currentRunRate),
                    accent = AuctionGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InningsRateCard(
    teamName: String,
    score: String,
    overs: String,
    runRate: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(teamName, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "$score ($overs)",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, color = BroadcastWhite)
            )
            Text("Run Rate: $runRate", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
        }
    }
}

@Composable
private fun ScorecardTabContent(
    match: LiveMatchState,
    selectedInnings: Int,
    onSelectInnings: (Int) -> Unit,
    orangeCapPlayerId: String? = null,
    purpleCapPlayerId: String? = null,
    modifier: Modifier = Modifier
) {
    val inn = if (selectedInnings == 1) match.innings1 else match.innings2
    var subTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Scorecard, 1 = Wagon Wheel, 2 = Pitch Map

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SimTabButton(
                    label = "${match.teamA.teamName} (${match.innings1.totalRuns}/${match.innings1.wickets})",
                    selected = selectedInnings == 1,
                    onClick = { onSelectInnings(1) },
                    modifier = Modifier.weight(1f)
                )
                SimTabButton(
                    label = "${match.teamB.teamName} (${match.innings2.totalRuns}/${match.innings2.wickets})",
                    selected = selectedInnings == 2,
                    onClick = { onSelectInnings(2) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sub-Tab Switcher: Scorecard | Wagon Wheel | Pitch Map
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavySurfaceHigh)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SimTabButton(
                    label = "Scorecard",
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    modifier = Modifier.weight(1f).testTag("subtab_scorecard")
                )
                SimTabButton(
                    label = "Wagon Wheel",
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    modifier = Modifier.weight(1f).testTag("subtab_wagon_wheel")
                )
                SimTabButton(
                    label = "Pitch Map",
                    selected = subTab == 2,
                    onClick = { subTab = 2 },
                    modifier = Modifier.weight(1f).testTag("subtab_pitch_map")
                )
            }
        }

        if (subTab == 1) {
            item {
                WagonWheelChartCard(
                    shots = inn.wagonWheelShots,
                    batters = inn.battingCard,
                    teamName = inn.battingTeamName
                )
            }
        } else if (subTab == 2) {
            item {
                PitchMapChartCard(
                    points = inn.pitchMapPoints,
                    bowlers = inn.bowlingCard,
                    teamName = inn.bowlingTeamName
                )
            }
        }

        if (subTab == 0) {
        // Batting Scorecard Table
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BATTING — ${inn.battingTeamName.uppercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily, color = TurfEmerald)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("BATTER", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.weight(1f))
                        Text("R", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(32.dp))
                        Text("B", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(30.dp))
                        Text("4s", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(26.dp))
                        Text("6s", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(26.dp))
                        Text("SR", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(46.dp))
                    }
                    HorizontalDivider(color = NavyOutline, modifier = Modifier.padding(vertical = 6.dp))
                    inn.battingCard.forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${entry.playerName}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (entry.dismissalText == "batting") TurfEmerald else BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (entry.isCaptain) {
                                        CaptainTagBadge()
                                    }
                                    if (orangeCapPlayerId != null && entry.playerId == orangeCapPlayerId) {
                                        CapLeaderBadge(label = "🟠", color = androidx.compose.ui.graphics.Color(0xFFFF8C00))
                                    }
                                }
                                Text(
                                    text = entry.dismissalText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = BroadcastMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text("${entry.runs}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = BroadcastWhite, modifier = Modifier.width(32.dp))
                            Text("${entry.balls}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(30.dp))
                            Text("${entry.fours}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BoundarySky, modifier = Modifier.width(26.dp))
                            Text("${entry.sixes}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = AuctionGold, modifier = Modifier.width(26.dp))
                            Text(MatchSimulationEngine.formatRate(entry.strikeRate), style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastMuted, modifier = Modifier.width(46.dp))
                        }
                    }
                    HorizontalDivider(color = NavyOutline, modifier = Modifier.padding(vertical = 6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Extras: ${inn.extras}", style = MaterialTheme.typography.labelMedium, color = BroadcastSilver)
                        Text(
                            "Total: ${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted} ov)",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily, color = AuctionGold)
                        )
                    }
                }
            }
        }

        // Bowling Scorecard Table
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BOWLING — ${inn.bowlingTeamName.uppercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily, color = AuctionGold)
                    )
                    Spacer(Modifier.height(8.dp))
                    val showStamina = match.format == MatchFormat.TEST
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("BOWLER", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.weight(1f))
                        Text("O", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(34.dp))
                        Text("M", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                        Text("R", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(32.dp))
                        Text("W", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                        Text(
                            if (showStamina) "STM" else "ECON",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastMuted,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                    HorizontalDivider(color = NavyOutline, modifier = Modifier.padding(vertical = 6.dp))
                    inn.bowlingCard.forEach { bw ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (bw.injured) "${bw.playerName} 🚑" else bw.playerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (bw.injured) WicketCrimson else BroadcastWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (bw.isCaptain) {
                                    CaptainTagBadge()
                                }
                                if (purpleCapPlayerId != null && bw.playerId == purpleCapPlayerId) {
                                    CapLeaderBadge(label = "🟣", color = androidx.compose.ui.graphics.Color(0xFFA855F7))
                                }
                            }
                            Text(bw.oversFormatted, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(34.dp))
                            Text("${bw.maidens}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastMuted, modifier = Modifier.width(24.dp))
                            Text("${bw.runsConceded}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(32.dp))
                            Text("${bw.wickets}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = WicketCrimson, modifier = Modifier.width(24.dp))
                            if (showStamina) {
                                val stColor = when {
                                    bw.stamina < 40 -> WicketCrimson
                                    bw.stamina < 65 -> AuctionGold
                                    else -> TurfEmerald
                                }
                                Text("${bw.stamina}%", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = stColor, modifier = Modifier.width(44.dp))
                            } else {
                                Text(MatchSimulationEngine.formatRate(bw.economy), style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BoundarySky, modifier = Modifier.width(44.dp))
                            }
                        }
                    }
                }
            }
        }

        // Fall of Wickets
        if (inn.fallOfWickets.isNotEmpty()) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FALL OF WICKETS",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily, color = WicketCrimson)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = inn.fallOfWickets.joinToString("  •  ") {
                                "${it.scoreAtFall}-${it.wicketNumber} (${it.batterName}, ${it.overLabel})"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )
                    }
                }
            }
        }

        // Super Over Scorecard Summary (when triggered on tied T20/T20I)
        if (match.superOverHistory.isNotEmpty() || match.superOverInnings1 != null) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🔥 SUPER OVER SHOOTOUT",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily, color = AuctionGold)
                        )
                        match.superOverHistory.forEach { so ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Super Over #${so.roundNumber}: ${so.team1Name} ${so.team1Score}/${so.team1Wickets} (${so.team1Overs})  vs  ${so.team2Name} ${so.team2Score}/${so.team2Wickets} (${so.team2Overs})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    color = BroadcastWhite
                                )
                                Text(
                                    text = so.outcomeNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurfEmerald
                                )
                            }
                        }
                        if (match.isSuperOverActive) {
                            val so1 = match.superOverInnings1
                            val so2 = match.superOverInnings2
                            if (so1 != null) {
                                Text(
                                    text = "Live SO #${match.superOverRound}: ${so1.battingTeamName} ${so1.totalRuns}/${so1.wickets} (${so1.oversFormatted})" +
                                        if (so2 != null && (so2.legalBalls > 0 || match.activeSuperOverInningsNumber == 2)) {
                                            "  •  ${so2.battingTeamName} ${so2.totalRuns}/${so2.wickets} (${so2.oversFormatted})"
                                        } else "",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                    color = AuctionGold
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CapLeaderBadge(
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun CaptainTagBadge() {
    Surface(
        color = AuctionGold,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = "C",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyMidnight
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

