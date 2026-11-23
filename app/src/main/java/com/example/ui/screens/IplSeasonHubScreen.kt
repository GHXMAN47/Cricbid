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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Ipl2026Catalog
import com.example.data.model.IplAiResultsBatch
import com.example.data.model.IplPlayMode
import com.example.data.model.IplRules
import com.example.data.model.IplScheduledMatch
import com.example.data.model.IplSeasonPhase
import com.example.data.model.IplSeasonState
import com.example.engine.IplSeasonEngine
import com.example.engine.SeasonAwardsEngine
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
import com.example.viewmodel.IplSeasonUiState

/**
 * Top-level router for the complete IPL 2026 Game Mode:
 * Setup (Solo / Friend Room + 10 Official 2026 Franchises) → Retentions & RTM → Mega Auction → Playing XI & 5 Impact Subs → 70-Match League & Top-4 Playoffs Hub.
 */
@Composable
fun IplSeasonRootScreen(
    uiState: IplSeasonUiState,
    defaultManagerName: String,
    onBackToHome: () -> Unit,
    onStartNewSeason: (IplPlayMode, List<Pair<String, String>>, String) -> Unit,
    onResetToSetup: () -> Unit,
    onSelectActiveHuman: (String) -> Unit,
    onSubmitRetentions: (String, List<String>) -> Unit,
    onToggleAuctionPause: () -> Unit,
    onPlaceBid: (String) -> Unit,
    onTriggerHammerLoot: (String) -> Unit,
    onUseRtm: (String) -> Unit,
    onPassLot: (String) -> Unit,
    onFastForwardLot: () -> Unit,
    onAutoCompleteAuction: () -> Unit,
    onSavePlayingXi: (String, List<String>, List<String>, String, Boolean) -> Unit,
    onOpenXiEditorInSeason: (String) -> Unit,
    onCloseXiEditorInSeason: () -> Unit,
    onLaunchFixtureMatch: (IplScheduledMatch) -> Unit,
    onDismissAiResultsPopup: () -> Unit,
    onOpenTrophyCabinet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val season = uiState.season

    Box(modifier = modifier.fillMaxSize()) {
        when {
            season == null || season.phase == IplSeasonPhase.SETUP -> {
                IplSeasonSetupScreen(
                    defaultManagerName = defaultManagerName,
                    onBack = onBackToHome,
                    onStartSeason = onStartNewSeason
                )
            }

            season.phase == IplSeasonPhase.RETENTION -> {
                IplRetentionScreen(
                    season = season,
                    onSelectActiveHuman = onSelectActiveHuman,
                    onSubmitRetentions = onSubmitRetentions,
                    onBack = onBackToHome
                )
            }

            season.phase == IplSeasonPhase.AUCTION -> {
                IplLiveAuctionScreen(
                    season = season,
                    countdownSeconds = uiState.countdownSeconds,
                    countdownProgress = uiState.countdownProgress,
                    paused = uiState.auctionPaused,
                    onTogglePause = onToggleAuctionPause,
                    onSelectActiveHuman = onSelectActiveHuman,
                    onPlaceBid = onPlaceBid,
                    onTriggerHammerLoot = onTriggerHammerLoot,
                    onUseRtm = onUseRtm,
                    onPass = onPassLot,
                    onFastForwardLot = onFastForwardLot,
                    onAutoComplete = onAutoCompleteAuction,
                    onBack = onBackToHome
                )
            }

            season.phase == IplSeasonPhase.SQUAD_XI || uiState.editingXiDuringSeason -> {
                IplPlayingXiScreen(
                    season = season,
                    isSeasonEdit = uiState.editingXiDuringSeason,
                    onSelectActiveHuman = onSelectActiveHuman,
                    onSaveXi = onSavePlayingXi,
                    onBack = {
                        if (uiState.editingXiDuringSeason) onCloseXiEditorInSeason() else onBackToHome()
                    }
                )
            }

            else -> {
                IplLeaguePlayoffsHubScreen(
                    season = season,
                    onBack = onBackToHome,
                    onPlayFixture = onLaunchFixtureMatch,
                    onEditXi = onOpenXiEditorInSeason,
                    onOpenTrophyCabinet = onOpenTrophyCabinet,
                    onStartNewSeason = onResetToSetup
                )
            }
        }

        // "Other Match Results" Popup whenever AI vs AI matches are auto-simulated between human fixtures
        val aiBatch = uiState.aiResultsPopup
        if (aiBatch != null) {
            OtherMatchResultsDialog(
                batch = aiBatch,
                onDismiss = onDismissAiResultsPopup
            )
        }
    }
}

@Composable
private fun IplSeasonSetupScreen(
    defaultManagerName: String,
    onBack: () -> Unit,
    onStartSeason: (IplPlayMode, List<Pair<String, String>>, String) -> Unit
) {
    BackHandler(onBack = onBack)
    var playMode by rememberSaveable { mutableStateOf(IplPlayMode.SOLO) }
    var selectedIds by rememberSaveable { mutableStateOf(listOf("ipl_rcb")) }
    var roomCode by rememberSaveable { mutableStateOf((1000 + (System.currentTimeMillis() % 9000).toInt()).toString()) }
    var managerName by rememberSaveable { mutableStateOf(defaultManagerName.ifBlank { "Manager" }) }

    val canLaunch = when (playMode) {
        IplPlayMode.SOLO -> selectedIds.size == 1
        IplPlayMode.FRIEND_ROOM -> selectedIds.size >= 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
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
                        .testTag("ipl_setup_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "INDIAN PREMIER LEAGUE 2026",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "10 Official Franchises • ₹120 Cr Mega Auction • 70 Matches • Top-4 Playoffs",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Solo vs Friend Room Mode Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IplPlayMode.entries.forEach { mode ->
                            val selected = playMode == mode
                            Surface(
                                color = if (selected) TurfEmerald.copy(alpha = 0.18f) else NavySurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) TurfEmerald else NavyOutline),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        playMode = mode
                                        selectedIds = if (mode == IplPlayMode.SOLO) {
                                            listOf(selectedIds.firstOrNull() ?: "ipl_rcb")
                                        } else {
                                            if (selectedIds.size >= 2) selectedIds else listOf("ipl_rcb", "ipl_csk")
                                        }
                                    }
                                    .testTag("ipl_mode_${mode.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selected) TurfEmerald else BroadcastWhite
                                        )
                                    )
                                    Text(
                                        text = mode.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastSilver
                                    )
                                }
                            }
                        }
                    }
                }

                // Manager Name & Optional Friend Room 4-digit code
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { if (it.length <= 24) managerName = it },
                            label = { Text("Host Manager Name") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("ipl_manager_name_input")
                        )
                        if (playMode == IplPlayMode.FRIEND_ROOM) {
                            OutlinedTextField(
                                value = roomCode,
                                onValueChange = { if (it.length <= 4) roomCode = it.filter(Char::isDigit) },
                                label = { Text("4-Digit Room") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("ipl_room_code_input")
                            )
                        }
                    }
                }

                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (playMode == IplPlayMode.SOLO) {
                                "Select your IPL Franchise below. You will enter Retentions & RTM (max 6), bid in the ₹120 Cr Mega Auction (2 Hammer Loots max), and play ONLY your franchise's matches while AI auto-sims the other 9 teams!"
                            } else {
                                "Friend Room IPL: Tap 2 or more franchises below for human friends (remaining franchises filled by AI up to 10). Only human-involved matches are played interactively; AI vs AI games auto-simulate cleanly!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(Ipl2026Catalog.franchises, key = { it.id }) { bp ->
                    val isSelected = bp.id in selectedIds
                    val accent = Color(bp.primaryColorHex)
                    Surface(
                        color = if (isSelected) accent.copy(alpha = 0.20f) else NavySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, if (isSelected) accent else NavyOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedIds = if (playMode == IplPlayMode.SOLO) {
                                    listOf(bp.id)
                                } else {
                                    if (isSelected && selectedIds.size > 1) selectedIds - bp.id
                                    else if (!isSelected) selectedIds + bp.id
                                    else selectedIds
                                }
                            }
                            .testTag("ipl_pick_${bp.shortCode}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, accent),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = bp.shortCode,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BroadcastWhite
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = bp.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (bp.defendingChampion) {
                                        MiniTag("★ DEFENDING CHAMPIONS", AuctionGold)
                                    }
                                }
                                Text(
                                    text = "Captain: ${bp.captainName} • ${bp.homeVenue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = TurfEmerald, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val selections = selectedIds.mapIndexed { idx, fid ->
                            val owner = if (idx == 0) managerName.trim().ifBlank { "Manager" } else "Friend ${idx + 1}"
                            fid to owner
                        }
                        onStartSeason(playMode, selections, roomCode)
                    },
                    enabled = canLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuctionGold,
                        contentColor = OnGoldDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp)
                        .testTag("ipl_start_season_button")
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (playMode == IplPlayMode.SOLO) {
                            "ENTER IPL 2026 RETENTIONS & MEGA AUCTION"
                        } else {
                            "START FRIEND ROOM (${selectedIds.size} HUMANS + ${10 - selectedIds.size} AI)"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun IplLeaguePlayoffsHubScreen(
    season: IplSeasonState,
    onBack: () -> Unit,
    onPlayFixture: (IplScheduledMatch) -> Unit,
    onEditXi: (String) -> Unit,
    onOpenTrophyCabinet: () -> Unit,
    onStartNewSeason: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0=Fixtures, 1=Table, 2=Awards, 3=Finances
    val nextFixture = remember(season) { IplSeasonEngine.nextHumanFixture(season) }
    val standings = remember(season.schedule, season.franchises) { IplSeasonEngine.computeLeagueStandings(season) }
    val awards = remember(season.schedule, season.playoffs) { SeasonAwardsEngine.buildLeaderboards(season.allCompletedMatches) }
    val activeHuman = season.activeHumanFranchise

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 660.dp)
        ) {
            // Top bar
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
                        .testTag("ipl_hub_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IPL ${season.seasonYear} • ${season.phase.label.uppercase()}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append("League ${season.leagueCompletedCount}/${IplRules.TOTAL_LEAGUE_MATCHES}")
                            if (season.playMode == IplPlayMode.FRIEND_ROOM && season.roomCode.isNotBlank()) {
                                append(" • Room ${season.roomCode}")
                            }
                            if (activeHuman != null) append(" • ${activeHuman.shortCode}")
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = BroadcastSilver
                    )
                }
                if (activeHuman != null && season.phase != IplSeasonPhase.COMPLETED) {
                    IconButton(
                        onClick = { onEditXi(activeHuman.franchiseId) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ipl_edit_xi_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Playing XI & Impact Subs", tint = BoundarySky)
                    }
                }
                IconButton(
                    onClick = onOpenTrophyCabinet,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ipl_open_trophies_button")
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophy Cabinet", tint = AuctionGold)
                }
            }

            // Champion Banner OR Next Interactive Human Match Hero Card
            if (season.phase == IplSeasonPhase.COMPLETED) {
                Surface(
                    color = AuctionGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, AuctionGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ipl_champion_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "IPL ${season.seasonYear} CHAMPIONS: ${season.championTeamName.uppercase()}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    color = AuctionGold
                                )
                            )
                        }
                        Text(
                            text = "Runner-Up: ${season.runnerUpTeamName} • Trophy, Orange Cap, Purple Cap, MVP & Emerging Player saved to Trophy Cabinet!",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastWhite
                        )
                        Button(
                            onClick = onStartNewSeason,
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("ipl_new_season_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Next IPL Season", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else if (nextFixture != null) {
                val teamA = season.franchiseById(nextFixture.teamAId)
                val teamB = season.franchiseById(nextFixture.teamBId)
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, TurfEmerald.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ipl_next_match_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniTag(
                                text = if (nextFixture.matchNumber <= 70) "MATCH #${nextFixture.matchNumber} • DAY ${nextFixture.matchday}" else nextFixture.stage.label.uppercase(),
                                color = AuctionGold
                            )
                            Text(
                                text = "OFFICIAL IPL RULES • 20 OV",
                                style = MaterialTheme.typography.labelSmall.copy(color = TurfEmerald, fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = "${teamA?.teamName ?: nextFixture.teamAId} vs ${teamB?.teamName ?: nextFixture.teamBId}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                color = BroadcastWhite
                            )
                        )
                        Text(
                            text = "${nextFixture.venue} • Impact Player • 2 Bouncers/Ov • Wide/NB DRS • Unlimited Super Over",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                        Button(
                            onClick = { onPlayFixture(nextFixture) },
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("ipl_play_next_match_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "PLAY MATCH #${nextFixture.matchNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }
                    }
                }
            }

            // 4 Hub Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavySurface,
                contentColor = AuctionGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AuctionGold
                    )
                }
            ) {
                listOf("Fixtures", "Table", "Awards", "Finances").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        modifier = Modifier.testTag("ipl_tab_${label.lowercase()}"),
                        text = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when (selectedTab) {
                0 -> IplFixturesTab(season = season, onPlayFixture = onPlayFixture, modifier = Modifier.weight(1f))
                1 -> IplStandingsTab(season = season, standings = standings, modifier = Modifier.weight(1f))
                2 -> IplAwardsTab(awards = awards, modifier = Modifier.weight(1f))
                else -> IplFinancesTab(season = season, onEditXi = onEditXi, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IplFixturesTab(
    season: IplSeasonState,
    onPlayFixture: (IplScheduledMatch) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFixtures = remember(season.schedule, season.playoffs) {
        season.playoffs.reversed() + season.schedule
    }
    val nextHumanNum = remember(season) { IplSeasonEngine.nextHumanFixture(season)?.matchNumber }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allFixtures, key = { it.matchNumber }) { fx ->
            val teamA = season.franchiseById(fx.teamAId)
            val teamB = season.franchiseById(fx.teamBId)
            val isHumanMatch = season.involvesHuman(fx)
            val isNextPlayable = fx.matchNumber == nextHumanNum

            Surface(
                color = if (isNextPlayable) TurfEmerald.copy(alpha = 0.12f) else NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isNextPlayable) 1.5.dp else 1.dp,
                    color = when {
                        isNextPlayable -> TurfEmerald
                        fx.matchNumber > 70 -> AuctionGold.copy(alpha = 0.7f)
                        isHumanMatch -> BoundarySky.copy(alpha = 0.55f)
                        else -> NavyOutline
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (fx.matchNumber <= 70) "M${fx.matchNumber} • Day ${fx.matchday}" else "${fx.stage.label.uppercase()} (M${fx.matchNumber})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                color = if (fx.matchNumber > 70) AuctionGold else BroadcastMuted
                            )
                        )
                        if (isHumanMatch) {
                            MiniTag("YOUR MATCH", TurfEmerald)
                        } else {
                            MiniTag("AI AUTO-SIM", BroadcastMuted)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${teamA?.shortCode ?: fx.teamAId} vs ${teamB?.shortCode ?: fx.teamBId}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = BroadcastWhite
                        )
                        if (isNextPlayable) {
                            TextButton(onClick = { onPlayFixture(fx) }) {
                                Text("PLAY →", color = TurfEmerald, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    val rec = fx.completedRecord
                    if (rec != null) {
                        Text(
                            text = "${rec.teamAName} ${rec.teamAScore}/${rec.teamAWickets} (${rec.teamAOvers}) vs ${rec.teamBName} ${rec.teamBScore}/${rec.teamBWickets} (${rec.teamBOvers})",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = BroadcastSilver
                        )
                        Text(
                            text = "${rec.resultSummary} • POTM: ${rec.playerOfTheMatch}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurfEmerald
                        )
                    } else {
                        Text(
                            text = fx.venue,
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IplStandingsTab(
    season: IplSeasonState,
    standings: List<com.example.engine.TeamStanding>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("# TEAM", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.weight(1.8f))
                Text("P", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(26.dp))
                Text("W", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(26.dp))
                Text("L", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(26.dp))
                Text("PTS", style = MaterialTheme.typography.labelSmall, color = AuctionGold, modifier = Modifier.width(34.dp))
                Text("NRR", style = MaterialTheme.typography.labelSmall, color = BoundarySky, modifier = Modifier.width(56.dp))
            }
        }
        itemsIndexed(standings, key = { _, row -> row.teamId }) { index, row ->
            val franchise = season.franchiseById(row.teamId)
            val isTop4 = index < 4
            val isHuman = row.teamId in season.humanFranchiseIds
            Surface(
                color = when {
                    isHuman -> TurfEmerald.copy(alpha = 0.14f)
                    isTop4 -> NavySurface
                    else -> NavySurface.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isHuman) 1.4.dp else 1.dp,
                    color = when {
                        isHuman -> TurfEmerald
                        isTop4 -> AuctionGold.copy(alpha = 0.45f)
                        else -> NavyOutline
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. ${franchise?.shortCode ?: row.teamName.take(3)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTop4) BroadcastWhite else BroadcastSilver
                        ),
                        modifier = Modifier.weight(1.8f)
                    )
                    Text("${row.played}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(26.dp))
                    Text("${row.won}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = TurfEmerald, modifier = Modifier.width(26.dp))
                    Text("${row.lost}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = WicketCrimson, modifier = Modifier.width(26.dp))
                    Text("${row.points}", style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.ExtraBold), color = AuctionGold, modifier = Modifier.width(34.dp))
                    val nrrStr = String.format(java.util.Locale.US, "%+.3f", row.nrr)
                    Text(nrrStr, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = if (row.nrr >= 0) TurfEmerald else WicketCrimson, modifier = Modifier.width(56.dp))
                }
            }
            if (index == 3) {
                Text(
                    text = "▲ TOP 4 QUALIFY FOR IPL PLAYOFFS (Q1 • ELIMINATOR • Q2 • FINAL) ▲",
                    style = MaterialTheme.typography.labelSmall.copy(color = AuctionGold, fontSize = 10.sp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun IplAwardsTab(
    awards: com.example.engine.SeasonLeaderboardSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AwardLeaderBanner(
                title = "🟠 ORANGE CAP (MOST RUNS)",
                holder = awards.orangeCapHolder?.let { "${it.playerName} (${it.teamName})" } ?: "TBD after Match 1",
                stat = awards.orangeCapHolder?.let { "${it.runs} Runs • SR ${String.format(java.util.Locale.US, "%.1f", it.strikeRate)}" } ?: "—",
                accent = Color(0xFFFF8C00)
            )
        }
        item {
            AwardLeaderBanner(
                title = "🟣 PURPLE CAP (MOST WICKETS)",
                holder = awards.purpleCapHolder?.let { "${it.playerName} (${it.teamName})" } ?: "TBD after Match 1",
                stat = awards.purpleCapHolder?.let { "${it.wickets} Wkts • Econ ${String.format(java.util.Locale.US, "%.2f", it.economy)}" } ?: "—",
                accent = Color(0xFFA855F7)
            )
        }
        item {
            AwardLeaderBanner(
                title = "⭐ MOST VALUABLE PLAYER (MVP)",
                holder = awards.mvpHolder?.let { "${it.playerName} (${it.teamName})" } ?: "TBD after Match 1",
                stat = awards.mvpHolder?.let { "${it.mvpPoints.toInt()} MVP Pts • ${it.runs}r / ${it.wickets}w" } ?: "—",
                accent = AuctionGold
            )
        }
        item {
            AwardLeaderBanner(
                title = "🌱 EMERGING PLAYER OF THE SEASON",
                holder = awards.emergingPlayerHolder?.let { "${it.playerName} (${it.teamName})" } ?: "TBD (Young/Uncapped Stars)",
                stat = awards.emergingPlayerHolder?.let { "${it.mvpPoints.toInt()} Pts • ${it.runs}r / ${it.wickets}w" } ?: "—",
                accent = TurfEmerald
            )
        }
    }
}

@Composable
private fun AwardLeaderBanner(title: String, holder: String, stat: String, accent: Color) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.ExtraBold))
            Text(holder, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BroadcastWhite)
            Text(stat, style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily), color = BroadcastSilver)
        }
    }
}

@Composable
private fun IplFinancesTab(
    season: IplSeasonState,
    onEditXi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Official IPL Match Fee Ledger: Every player in the Playing XI + Impact Player (12 players) earns a fixed ₹7.5 Lakh match fee per game (₹90 Lakh per franchise per match), tracked separately from the ₹120 Cr Mega Auction purse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastSilver,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        items(season.franchises, key = { it.franchiseId }) { f ->
            Surface(
                color = if (f.isHuman) TurfEmerald.copy(alpha = 0.12f) else NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(f.primaryColorHex).copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${f.shortCode} • ${f.teamName}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastWhite
                            )
                            if (f.defendingChampion) MiniTag("CHAMPIONS", AuctionGold)
                        }
                        if (f.isHuman && season.phase != IplSeasonPhase.COMPLETED) {
                            TextButton(onClick = { onEditXi(f.franchiseId) }) {
                                Text("Edit XI", color = BoundarySky, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PurseStatPill("PURSE LEFT", CurrencyFormatter.formatLakhs(f.purseRemainingLakhs), TurfEmerald)
                        PurseStatPill("MATCH FEES PAID", IplRules.formatMatchFeeLakhs(f.matchFeesSpentLakhs), AuctionGold)
                        PurseStatPill("GAMES PAID", "${f.matchesPaidCount}", BoundarySky)
                        PurseStatPill("SQUAD", "${f.squadPlayerIds.size} (${f.overseasCount} OS)", BroadcastWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun OtherMatchResultsDialog(
    batch: IplAiResultsBatch,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySurface,
        titleContentColor = AuctionGold,
        textContentColor = BroadcastWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SportsCricket, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = batch.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        color = AuctionGold
                    )
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(batch.results) { rec ->
                    Surface(
                        color = NavySurfaceHigh,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NavyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${rec.teamAName} ${rec.teamAScore}/${rec.teamAWickets} (${rec.teamAOvers})",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = BroadcastWhite
                            )
                            Text(
                                text = "${rec.teamBName} ${rec.teamBScore}/${rec.teamBWickets} (${rec.teamBOvers})",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                                color = BroadcastSilver
                            )
                            Text(
                                text = "${rec.resultSummary} • POTM: ${rec.playerOfTheMatch}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                modifier = Modifier.testTag("dismiss_other_match_results_button")
            ) {
                Text("Continue Season", fontWeight = FontWeight.Bold)
            }
        }
    )
}
