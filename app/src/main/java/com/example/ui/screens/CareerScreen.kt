package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerDifficulty
import com.example.data.model.CareerMatchType
import com.example.data.model.CareerPlayerAppearance
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerSkillStat
import com.example.data.model.CareerStage
import com.example.data.model.CurrencyFormatter
import com.example.data.model.PlayerRole
import com.example.data.model.ProT20League
import com.example.data.model.TrainingDrill
import com.example.ui.components.CareerPlayerPortrait
import com.example.ui.components.RolePillBadge
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
import com.example.viewmodel.CareerUiState

@Composable
fun CareerScreen(
    uiState: CareerUiState,
    onBack: () -> Unit,
    onCreateCareer: (String, Int, String, String, String, PlayerRole, BattingStyle, BowlingStyle, CareerDifficulty, CareerPlayerAppearance) -> Unit,
    onChangeDifficulty: (CareerDifficulty) -> Unit,
    onRunDrill: (TrainingDrill) -> Unit,
    onPromoteToDomestic: () -> Unit,
    onEnterIplAuction: (ProT20League) -> Unit,
    onSelectProLeague: (ProT20League) -> Unit,
    onDismissAuctionDraft: () -> Unit,
    onAcceptIntlCallUp: () -> Unit,
    onSpendSkillPoint: (CareerSkillStat) -> Unit,
    onRunSelectionTrial: () -> Unit,
    onRefreshObjective: (CareerMatchType) -> Unit,
    onPlayLiveMatch: (CareerMatchType) -> Unit,
    onQuickSimMatch: (CareerMatchType) -> Unit,
    onRetireToHallOfFame: () -> Unit,
    onDismissRetirement: () -> Unit,
    onToggleLegendInAuctions: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var topTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Career Hub, 1 = Skills & Badges, 2 = Hall of Fame
    var selectedDraftLeague by rememberSaveable { mutableStateOf(ProT20League.IPL) }

    val activeCareer = uiState.activeCareer
    val availableFormats = activeCareer?.availableMatchTypesForCurrentStage() ?: listOf(CareerMatchType.CLUB_T20)
    var selectedMatchType by rememberSaveable { mutableStateOf(availableFormats.first()) }

    LaunchedEffect(activeCareer?.stage, activeCareer?.activeProLeague) {
        if (activeCareer != null) {
            val valid = activeCareer.availableMatchTypesForCurrentStage()
            if (selectedMatchType !in valid) {
                selectedMatchType = if (activeCareer.stage == CareerStage.IPL_FRANCHISE) {
                    activeCareer.activeProLeague.toMatchType()
                } else {
                    valid.first()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("career_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYER CAREER MODE",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = BroadcastWhite
                        )
                    )
                    Text(
                        text = activeCareer?.stage?.label ?: "Club → Domestic → T20 Leagues → Intl → Captain",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuctionGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                activeCareer?.let { active ->
                    val diffColor = careerDifficultyAccent(active.difficulty)
                    Surface(
                        color = diffColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, diffColor.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = active.difficulty.shortBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Tab Switcher: Career Hub | Skills & Badges | Hall of Fame
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurfaceHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CareerTopTab(
                    label = if (activeCareer != null) "Career Hub" else "Create Player",
                    selected = topTab == 0,
                    onClick = { topTab = 0 },
                    modifier = Modifier.weight(1f).testTag("career_tab_main")
                )
                if (activeCareer != null) {
                    val spSuffix = if (activeCareer.skillPoints > 0) " (${activeCareer.skillPoints} SP)" else ""
                    CareerTopTab(
                        label = "Skills$spSuffix",
                        selected = topTab == 1,
                        onClick = { topTab = 1 },
                        modifier = Modifier.weight(1f).testTag("career_tab_skills")
                    )
                }
                CareerTopTab(
                    label = "HOF (${uiState.hallOfFame.size})",
                    selected = topTab == 2,
                    onClick = { topTab = 2 },
                    modifier = Modifier.weight(0.9f).testTag("career_tab_hof")
                )
            }

            // Main View Routing
            when {
                topTab == 2 -> {
                    HallOfFameListContent(
                        legends = uiState.hallOfFame,
                        onToggleAuctionPool = onToggleLegendInAuctions,
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.latestRetiredLegend != null -> {
                    RetirementSummaryCard(
                        legend = uiState.latestRetiredLegend,
                        onContinueToHallOfFame = {
                            onDismissRetirement()
                            topTab = 2
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                activeCareer == null -> {
                    RookieCreatorContent(
                        onCreateCareer = onCreateCareer,
                        modifier = Modifier.weight(1f)
                    )
                }
                topTab == 1 -> {
                    CareerSkillsAndBadgesContent(
                        career = activeCareer,
                        onSpendSkillPoint = onSpendSkillPoint,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    CareerDashboardContent(
                        career = activeCareer,
                        uiState = uiState,
                        selectedMatchType = selectedMatchType,
                        selectedDraftLeague = selectedDraftLeague,
                        onSelectDraftLeague = { selectedDraftLeague = it },
                        onSelectMatchType = { mt ->
                            selectedMatchType = mt
                            if (activeCareer.stage == CareerStage.IPL_FRANCHISE) {
                                val league = when (mt) {
                                    CareerMatchType.IPL_T20 -> ProT20League.IPL
                                    CareerMatchType.PSL_T20 -> ProT20League.PSL
                                    CareerMatchType.BBL_T20 -> ProT20League.BBL
                                    CareerMatchType.HUNDRED_T20 -> ProT20League.THE_HUNDRED
                                    else -> null
                                }
                                if (league != null && league != activeCareer.activeProLeague) {
                                    onSelectProLeague(league)
                                    return@CareerDashboardContent
                                }
                            }
                            onRefreshObjective(mt)
                        },
                        onOpenSkillsTab = { topTab = 1 },
                        onChangeDifficulty = onChangeDifficulty,
                        onRunDrill = onRunDrill,
                        onPromoteToDomestic = onPromoteToDomestic,
                        onEnterIplAuction = { onEnterIplAuction(selectedDraftLeague) },
                        onDismissAuctionDraft = onDismissAuctionDraft,
                        onAcceptIntlCallUp = onAcceptIntlCallUp,
                        onRunSelectionTrial = onRunSelectionTrial,
                        onPlayLiveMatch = onPlayLiveMatch,
                        onQuickSimMatch = onQuickSimMatch,
                        onRetireToHallOfFame = onRetireToHallOfFame,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CareerDashboardContent(
    career: CareerSaveState,
    uiState: CareerUiState,
    selectedMatchType: CareerMatchType,
    selectedDraftLeague: ProT20League,
    onSelectDraftLeague: (ProT20League) -> Unit,
    onSelectMatchType: (CareerMatchType) -> Unit,
    onOpenSkillsTab: () -> Unit,
    onChangeDifficulty: (CareerDifficulty) -> Unit,
    onRunDrill: (TrainingDrill) -> Unit,
    onPromoteToDomestic: () -> Unit,
    onEnterIplAuction: () -> Unit,
    onDismissAuctionDraft: () -> Unit,
    onAcceptIntlCallUp: () -> Unit,
    onRunSelectionTrial: () -> Unit,
    onPlayLiveMatch: (CareerMatchType) -> Unit,
    onQuickSimMatch: (CareerMatchType) -> Unit,
    onRetireToHallOfFame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stageMatchTypes = career.availableMatchTypesForCurrentStage()
    val effectiveMatchType = if (selectedMatchType in stageMatchTypes) selectedMatchType else stageMatchTypes.first()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pro T20 Draft Result Banner
        if (uiState.latestAuctionDraft != null) {
            item(key = "auction_draft_result") {
                IplAuctionDraftSummaryCard(
                    draft = uiState.latestAuctionDraft,
                    onDismiss = onDismissAuctionDraft
                )
            }
        }

        // Hero Player Profile Card with Custom Portrait
        item(key = "career_hero") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, TurfEmerald.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CareerPlayerPortrait(
                            appearance = career.appearance,
                            size = 64.dp,
                            showJerseyNumber = true,
                            isCaptain = career.isInternationalCaptain
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = career.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BroadcastWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (career.isInternationalCaptain) {
                                    Surface(color = AuctionGold, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "C",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, color = OnGoldDark),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                RolePillBadge(role = career.role)
                            }
                            Text(
                                text = "Age ${career.age} • S${career.season} • ${career.activeClubOrTeam}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${career.battingStyle.label} • ${career.bowlingStyle.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastSilver,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (career.iplBoughtPriceLakhs > 0L) {
                                Text(
                                    text = "${career.activeProLeague.displayName}: ${career.iplTeamName} (${CurrencyFormatter.formatLakhs(career.iplBoughtPriceLakhs)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuctionGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = AuctionGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AuctionGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("OVR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AuctionGold)
                                Text(
                                    "${career.overallRating}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.ExtraBold),
                                    color = AuctionGold
                                )
                            }
                        }
                    }

                    // XP Bar + Attributes Row
                    val xpProgress = (career.xp.toFloat() / career.xpForNextLevel.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lv.${career.level} • XP ${career.xp}/${career.xpForNextLevel}", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                            Text(
                                "BAT ${career.attributes.battingRating} • BWL ${career.attributes.bowlingRating} • FLD ${career.attributes.fieldingRating}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = BroadcastSilver
                            )
                        }
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            color = BoundarySky,
                            trackColor = NavySurfaceHigh,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // Form, Reputation, Fatigue, Skill Points Quick Strip
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CareerMeterPill("FORM", "${career.form}%", if (career.form >= 65) TurfEmerald else AuctionGold, Modifier.weight(1f))
                        CareerMeterPill("REP", "${career.reputation}/100", BoundarySky, Modifier.weight(1f))
                        CareerMeterPill(
                            label = if (career.isInjured) "INJURED" else "FATIGUE",
                            value = if (career.isInjured) "${career.injuryMatchesRemaining}m" else "${career.fatigue}%",
                            accent = if (career.isInjured || career.fatigue >= 75) WicketCrimson else TurfEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        CareerMeterPill(
                            label = "SKILL PTS",
                            value = "${career.skillPoints} SP",
                            accent = if (career.skillPoints > 0) AuctionGold else BroadcastMuted,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onOpenSkillsTab)
                        )
                    }
                }
            }
        }

        // Latest Milestone / Event Notice
        if (!uiState.lastEventBanner.isNullOrBlank()) {
            item(key = "career_event_banner") {
                Surface(
                    color = BoundarySky.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.lastEventBanner,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BroadcastWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // 5-Stage Career Progression Ladder & Promotion CTAs
        item(key = "career_roadmap") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CAREER PROGRESSION LADDER", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StageStepPill("1. Club", career.currentLadderStep >= 1, career.currentLadderStep == 1, Modifier.weight(1f))
                        StageStepPill("2. Dom", career.currentLadderStep >= 2, career.currentLadderStep == 2, Modifier.weight(1f))
                        StageStepPill("3. T20", career.currentLadderStep >= 3, career.currentLadderStep == 3, Modifier.weight(1f))
                        StageStepPill("4. Intl", career.currentLadderStep >= 4, career.currentLadderStep == 4, Modifier.weight(1f))
                        StageStepPill("5. (C)", career.currentLadderStep >= 5, career.currentLadderStep == 5, Modifier.weight(1f))
                    }

                    when {
                        career.canPromoteToDomestic -> {
                            Button(
                                onClick = onPromoteToDomestic,
                                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("promote_domestic_button")
                            ) {
                                Icon(Icons.Filled.TrendingUp, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Accept Domestic Call-Up (${career.domesticTeamName})!", fontWeight = FontWeight.Bold)
                            }
                        }
                        career.canEnterIplAuction -> {
                            Text(
                                text = "Choose your target Pro T20 League Draft:",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastSilver
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ProT20League.entries.forEach { league ->
                                    CareerSelectChip(
                                        label = league.displayName,
                                        selected = selectedDraftLeague == league,
                                        accent = AuctionGold,
                                        onClick = { onSelectDraftLeague(league) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Button(
                                onClick = onEnterIplAuction,
                                colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("enter_ipl_auction_button")
                            ) {
                                Icon(Icons.Filled.Gavel, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enter ${selectedDraftLeague.displayName} Auction Draft!", fontWeight = FontWeight.Bold)
                            }
                        }
                        career.canEarnInternationalCallUp -> {
                            Button(
                                onClick = onAcceptIntlCallUp,
                                colors = ButtonDefaults.buttonColors(containerColor = BoundarySky, contentColor = NavyMidnight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("accept_intl_callup_button")
                            ) {
                                Icon(Icons.Filled.Public, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Accept ${career.nationalTeamName} International Call-Up!", fontWeight = FontWeight.Bold)
                            }
                        }
                        career.stage == CareerStage.CLUB_CRICKET -> {
                            Text(
                                text = "Next Promotion: Reach 76% Selection (${career.selectionMeter}%), 22 Rep (${career.reputation}), or play 2 Club matches (${career.matchesPlayed}/2) for Domestic selection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                        }
                        career.stage == CareerStage.DOMESTIC -> {
                            Text(
                                text = "Next Promotion: Reach 78% Selection (${career.selectionMeter}%), 34 Rep (${career.reputation}), or 4 matches (${career.matchesPlayed}/4) to enter IPL/PSL/BBL/The Hundred Draft.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                        }
                        career.stage == CareerStage.IPL_FRANCHISE -> {
                            Text(
                                text = "Next Promotion: Reach 80% Selection (${career.selectionMeter}%), 56 Rep (${career.reputation}), or 72 OVR (${career.overallRating}) for International Debut.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                        }
                        career.stage == CareerStage.INTERNATIONAL && !career.isInternationalCaptain -> {
                            Text(
                                text = "Captaincy Goal: Reach 76 Rep (${career.reputation}/76) & 75 OVR (${career.overallRating}/75) to be named Captain (C) of ${career.nationalTeamName}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                        }
                        career.isInternationalCaptain -> {
                            Text(
                                text = "Leading ${career.nationalTeamName} as Captain (C)! Win the T20 & 50-Over World Championships to cement your Hall of Fame legacy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuctionGold
                            )
                        }
                    }
                }
            }
        }

        // Match Objectives & Team Selection Meter Card
        item(key = "career_objective_selection") {
            CareerObjectiveAndSelectionCard(
                career = career,
                onRunSelectionTrial = onRunSelectionTrial
            )
        }

        // Next Match Play / Quick Sim Card with 3 Cricket Formats / Pro Leagues Selector
        item(key = "career_match_panel") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "NEXT FIXTURE • ${effectiveMatchType.label.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TurfEmerald
                    )
                    Text(
                        text = when (career.stage) {
                            CareerStage.IPL_FRANCHISE -> "Select Pro T20 League (IPL • PSL • BBL • The Hundred):"
                            else -> "Choose Cricket Format (T20 • 50-Over One-Day • Test / First-Class):"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(stageMatchTypes) { mt ->
                            val chipAccent = when {
                                mt.isWorldChampionship -> AuctionGold
                                mt.isProLeague -> AuctionGold
                                mt.format == com.example.data.model.MatchFormat.TEST -> BroadcastWhite
                                mt.format == com.example.data.model.MatchFormat.ODI -> BoundarySky
                                else -> TurfEmerald
                            }
                            CareerSelectChip(
                                label = mt.shortChip,
                                selected = effectiveMatchType == mt,
                                accent = chipAccent,
                                onClick = { onSelectMatchType(mt) },
                                modifier = Modifier.testTag("career_format_${mt.name.lowercase()}")
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onPlayLiveMatch(effectiveMatchType) },
                            enabled = !uiState.busy && !career.isInjured && !career.isDropped,
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("career_play_live_match_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Play Live", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onQuickSimMatch(effectiveMatchType) },
                            enabled = !uiState.busy && !career.isInjured && !career.isDropped,
                            border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("career_quick_sim_button")
                        ) {
                            if (uiState.busy) {
                                CircularProgressIndicator(Modifier.size(18.dp), AuctionGold, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Quick Sim", color = AuctionGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Between-Match Training Drills
        item(key = "career_training") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(18.dp))
                            Text("NETS & TRAINING DRILLS", style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily), color = BroadcastWhite)
                        }
                        Text(
                            text = if (career.trainingDoneBeforeMatch) "1 Drill Done • Play Match" else "1 Drill Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (career.trainingDoneBeforeMatch) AuctionGold else TurfEmerald
                        )
                    }
                    TrainingDrill.entries.forEach { drill ->
                        val isRecovery = drill == TrainingDrill.FITNESS_RECOVERY
                        val enabled = isRecovery || (!career.trainingDoneBeforeMatch && !career.isInjured)
                        val scaledFatigue = if (isRecovery) {
                            when (career.difficulty) {
                                CareerDifficulty.EASY -> drill.fatigueDelta - 6
                                CareerDifficulty.NORMAL -> drill.fatigueDelta
                                CareerDifficulty.HARD -> (drill.fatigueDelta + 6).coerceAtMost(-24)
                                CareerDifficulty.LEGEND -> (drill.fatigueDelta + 10).coerceAtMost(-20)
                            }
                        } else {
                            (drill.fatigueDelta * career.difficulty.fatigueMultiplier).toInt().coerceAtLeast(6)
                        }
                        val accent = when {
                            isRecovery -> TurfEmerald
                            career.fatigue >= career.difficulty.injuryFatigueThreshold - 10 -> WicketCrimson
                            else -> BoundarySky
                        }
                        Surface(
                            color = NavySurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.5f) else NavyOutline.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = enabled) { onRunDrill(drill) }
                                .testTag("career_drill_${drill.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = drill.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (enabled) BroadcastWhite else BroadcastMuted
                                    )
                                    Text(
                                        text = if (isRecovery) {
                                            "$scaledFatigue Fatigue • Heals Strain"
                                        } else {
                                            "+${career.difficulty.drillAttrGain} Sub-Skills • ${(career.difficulty.xpMultiplier * 100).toInt()}% XP"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastSilver
                                    )
                                }
                                Text(
                                    text = if (isRecovery) "RECOVER" else "+$scaledFatigue% FTG",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                                    color = if (enabled) accent else BroadcastMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Career Settings: Switch Difficulty Anytime
        item(key = "career_difficulty_settings") {
            val activeAccent = careerDifficultyAccent(career.difficulty)
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CAREER SETTINGS • DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                        Text(
                            text = career.difficulty.shortBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = activeAccent
                            )
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CareerDifficulty.entries.forEach { diff ->
                            CareerSelectChip(
                                label = diff.label,
                                selected = career.difficulty == diff,
                                accent = careerDifficultyAccent(diff),
                                onClick = { onChangeDifficulty(diff) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("career_settings_difficulty_${diff.name.lowercase()}")
                            )
                        }
                    }
                    Text(career.difficulty.summary, style = MaterialTheme.typography.labelSmall, color = BroadcastSilver)
                }
            }
        }

        // Career Totals & Honors
        item(key = "career_stats") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CAREER STATS & HONORS", style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily), color = AuctionGold)
                        Text(
                            text = "${career.unlockedAchievements.size}/6 Badges",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                            color = TurfEmerald
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CareerStatCell("MAT", "${career.matchesPlayed}")
                        CareerStatCell("RUNS", "${career.totalRuns}")
                        CareerStatCell("HS", "${career.highestScore}")
                        CareerStatCell("50/100", "${career.fifties}/${career.centuries}")
                        CareerStatCell("WKTS", "${career.totalWickets}")
                        CareerStatCell("CUPS", "${career.totalTrophies}")
                    }
                    if (career.awards.isNotEmpty()) {
                        Text(
                            text = "Honors: ${career.awards.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurfEmerald
                        )
                    }
                }
            }
        }

        // Recent Career Match Logs
        if (career.recentLogs.isNotEmpty()) {
            item(key = "career_logs_header") {
                Text("RECENT CAREER PERFORMANCES", style = MaterialTheme.typography.labelMedium, color = BroadcastSilver)
            }
            items(career.recentLogs.reversed().take(6)) { log ->
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (log.teamWon) TurfEmerald.copy(alpha = 0.45f) else NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "S${log.season} • ${log.matchTypeLabel} vs ${log.opponent}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (log.teamWon) "WIN" else "LOSS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (log.teamWon) TurfEmerald else WicketCrimson
                            )
                        }
                        Text(
                            text = "Bat: ${log.runs}(${log.balls})  •  Bowl: ${log.wickets}/${log.runsConceded}  •  +${log.xpEarned} XP",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = BroadcastSilver
                        )
                        if (log.milestoneNote.isNotBlank()) {
                            Text(log.milestoneNote, style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                        }
                    }
                }
            }
        }

        // Retirement & Hall of Fame Induction Control
        item(key = "career_retire") {
            Surface(
                color = if (career.canRetirePrompt) AuctionGold.copy(alpha = 0.14f) else NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (career.canRetirePrompt) AuctionGold else NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (career.canRetirePrompt) "VETERAN FAREWELL (AGE ${career.age})" else "RETIREMENT & HALL OF FAME",
                            style = MaterialTheme.typography.labelMedium,
                            color = AuctionGold
                        )
                    }
                    Text(
                        text = if (career.canRetirePrompt) {
                            "You've reached veteran age (${career.age}). Retire now to cement your Hall of Fame legacy (mandatory retirement at age 40)."
                        } else {
                            "Retire anytime (or at age 36–40) to save ${career.name} into the Hall of Fame Legends pool for future auctions."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver
                    )
                    OutlinedButton(
                        onClick = onRetireToHallOfFame,
                        border = BorderStroke(1.dp, AuctionGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("retire_career_button")
                    ) {
                        Text("Retire & Induct into Hall of Fame", color = AuctionGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerTopTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) AuctionGold else NavySurfaceHigh,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) OnGoldDark else BroadcastSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CareerMeterPill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = BroadcastMuted, maxLines = 1)
            Text(value, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = accent, maxLines = 1)
        }
    }
}

@Composable
private fun StageStepPill(label: String, unlocked: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    val color = when {
        active -> AuctionGold
        unlocked -> TurfEmerald
        else -> BroadcastMuted
    }
    Surface(
        color = color.copy(alpha = if (active) 0.2f else 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CareerStatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = BroadcastWhite)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BroadcastMuted)
    }
}
