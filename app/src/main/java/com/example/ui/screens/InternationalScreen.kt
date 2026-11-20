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
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InternationalCampaignState
import com.example.data.model.InternationalMode
import com.example.data.model.InternationalNation
import com.example.data.model.InternationalNations
import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.TeamState
import com.example.engine.CustomSeriesSummary
import com.example.engine.InternationalTournamentEngine
import com.example.engine.PlayoffSlot
import com.example.engine.TeamStanding
import com.example.engine.WorldCupOverview
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

@Composable
fun InternationalScreen(
    campaign: InternationalCampaignState,
    busy: Boolean,
    onBack: () -> Unit,
    onSelectFormat: (MatchFormat) -> Unit,
    onSelectMode: (InternationalMode) -> Unit,
    onSelectUserNation: (String) -> Unit,
    onSelectOpponentNation: (String) -> Unit,
    onSelectSeriesLength: (Int) -> Unit,
    onStartMatch: (TeamState, TeamState, MatchStage, MatchFormat) -> Unit,
    onQuickSimWorldCup: (List<Pair<TeamState, TeamState>>) -> Unit,
    onOpenStandings: () -> Unit = {},
    onResetCampaign: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val allTeamStates = remember(campaign.userNationId) {
        InternationalNations.buildTeamStates(campaign.userNationId)
    }
    val userTeam = remember(allTeamStates, campaign.userNationId) {
        allTeamStates.firstOrNull { it.teamId == campaign.userNationId } ?: allTeamStates.first()
    }
    val opponentTeam = remember(allTeamStates, campaign.opponentNationId) {
        allTeamStates.firstOrNull { it.teamId == campaign.opponentNationId }
            ?: allTeamStates.first { it.teamId != userTeam.teamId }
    }

    val seriesSummary = remember(campaign, userTeam, opponentTeam) {
        InternationalTournamentEngine.buildCustomSeriesSummary(
            format = campaign.format,
            teamA = userTeam,
            teamB = opponentTeam,
            totalMatches = campaign.seriesTotalMatches,
            matches = campaign.seriesMatches
        )
    }

    val worldCupOverview = remember(campaign, allTeamStates) {
        InternationalTournamentEngine.buildWorldCupOverview(
            format = campaign.format,
            nations = allTeamStates,
            matches = campaign.worldCupMatches
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            item(key = "intl_top_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("intl_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = BroadcastWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INTERNATIONAL ARENA",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = BroadcastWhite
                            )
                        )
                        Text(
                            text = "${campaign.format.title} • 8 ICC Nations",
                            style = MaterialTheme.typography.labelSmall,
                            color = BoundarySky
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenStandings,
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.65f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("intl_open_standings_button")
                    ) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Standings", style = MaterialTheme.typography.labelMedium, color = AuctionGold)
                    }
                    IconButton(
                        onClick = onResetCampaign,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("intl_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset current campaign",
                            tint = AuctionGold
                        )
                    }
                }
            }

            // 1. Format Selection (T20I | ODI | TEST) + Rules Pill
            item(key = "format_selector") {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "SELECT FORMAT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = BroadcastSilver
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavySurfaceHigh)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MatchFormat.entries.forEach { fmt ->
                                val selected = fmt == campaign.format
                                Surface(
                                    color = if (selected) TurfEmerald else NavySurfaceHigh,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectFormat(fmt) }
                                        .testTag("format_${fmt.name.lowercase()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = fmt.shortLabel,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) OnEmeraldDark else BroadcastSilver
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            color = BoundarySky.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.SportsCricket, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = campaign.format.rulesSummary,
                                    style = MaterialTheme.typography.labelSmall.copy(color = BroadcastWhite)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Mode Choice: Bilateral Series vs World Cup / WTC
            item(key = "mode_selector") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NavySurfaceHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isSeries = campaign.mode == InternationalMode.SERIES
                    IntlModePill(
                        label = "Bilateral Series",
                        selected = isSeries,
                        accent = AuctionGold,
                        selectedText = OnGoldDark,
                        onClick = { onSelectMode(InternationalMode.SERIES) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intl_mode_series")
                    )
                    IntlModePill(
                        label = if (campaign.format == MatchFormat.TEST) "World Test Champ." else "${campaign.format.shortLabel} World Cup",
                        selected = !isSeries,
                        accent = BoundarySky,
                        selectedText = NavyMidnight,
                        onClick = { onSelectMode(InternationalMode.WORLD_CUP) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intl_mode_world_cup")
                    )
                }
            }

            // 3. Content based on Series vs World Cup
            if (campaign.mode == InternationalMode.SERIES) {
                item(key = "series_setup") {
                    SeriesSetupCard(
                        campaign = campaign,
                        onSelectUserNation = onSelectUserNation,
                        onSelectOpponentNation = onSelectOpponentNation,
                        onSelectSeriesLength = onSelectSeriesLength
                    )
                }
                item(key = "series_scoreboard") {
                    BilateralSeriesScorecard(
                        summary = seriesSummary,
                        onPlayNextMatch = {
                            onStartMatch(userTeam, opponentTeam, MatchStage.SERIES, campaign.format)
                        },
                        onResetSeries = onResetCampaign
                    )
                }
                if (campaign.seriesMatches.isNotEmpty()) {
                    item(key = "series_results_header") {
                        Text(
                            text = "SERIES SCORES (${campaign.seriesMatches.size}/${campaign.seriesTotalMatches})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = BroadcastSilver
                            )
                        )
                    }
                    items(campaign.seriesMatches.reversed(), key = { it.id.ifBlank { "${it.teamAId}_${it.teamBId}_${it.teamAScore}_${it.teamBScore}" } }) { rec ->
                        IntlMatchHistoryRow(rec)
                    }
                }
            } else {
                // World Cup / WTC Mode
                item(key = "wc_nation_picker") {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, NavyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "YOUR NATION (${InternationalNations.byId(campaign.userNationId).name.uppercase()})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald
                            )
                            NationChipRow(
                                nations = InternationalNations.all,
                                selectedId = campaign.userNationId,
                                disabledId = null,
                                accent = TurfEmerald,
                                onSelect = onSelectUserNation
                            )
                        }
                    }
                }

                // Champion Banner
                if (worldCupOverview.championTeamName != null) {
                    item(key = "wc_champion") {
                        Surface(
                            color = AuctionGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.5.dp, AuctionGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(34.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${worldCupOverview.title.uppercase()} CHAMPIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = worldCupOverview.championTeamName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        color = BroadcastWhite
                                    )
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onResetCampaign,
                                    border = BorderStroke(1.dp, AuctionGold),
                                    modifier = Modifier.testTag("new_world_cup_button")
                                ) {
                                    Text("Start New ${campaign.format.shortLabel} Tournament", color = AuctionGold)
                                }
                            }
                        }
                    }
                }

                // Group Stage Action Card
                item(key = "wc_group_actions") {
                    WorldCupActionCard(
                        overview = worldCupOverview,
                        userNationId = campaign.userNationId,
                        busy = busy,
                        onPlayFixture = { a, b -> onStartMatch(a, b, MatchStage.LEAGUE, campaign.format) },
                        onQuickSimFixtures = onQuickSimWorldCup
                    )
                }

                // Knockout Bracket Card
                item(key = "wc_knockout_bracket") {
                    WorldCupBracketCard(
                        overview = worldCupOverview,
                        onPlayKnockout = { slot ->
                            val a = slot.teamA
                            val b = slot.teamB
                            if (a != null && b != null) {
                                onStartMatch(a, b, slot.stage, campaign.format)
                            }
                        }
                    )
                }

                // Points Table
                item(key = "wc_points_table") {
                    WorldCupStandingsCard(
                        format = campaign.format,
                        standings = worldCupOverview.standings,
                        qualifierCount = worldCupOverview.qualifierCount,
                        userNationId = campaign.userNationId,
                        onOpenFullStandings = onOpenStandings
                    )
                }

                if (campaign.worldCupMatches.isNotEmpty()) {
                    item(key = "wc_recent_header") {
                        Text(
                            text = "RECENT TOURNAMENT RESULTS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = BroadcastSilver
                            )
                        )
                    }
                    items(campaign.worldCupMatches.takeLast(8).reversed(), key = { it.id.ifBlank { "${it.stage}_${it.teamAId}_${it.teamBId}_${it.teamAScore}" } }) { rec ->
                        IntlMatchHistoryRow(rec)
                    }
                }
            }

            item(key = "intl_bottom_spacer") { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun IntlModePill(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    selectedText: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent else NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) selectedText else BroadcastSilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NationChipRow(
    nations: List<InternationalNation>,
    selectedId: String,
    disabledId: String?,
    accent: androidx.compose.ui.graphics.Color,
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(nations, key = { it.id }) { nation ->
            val selected = nation.id == selectedId
            val disabled = nation.id == disabledId
            Surface(
                color = when {
                    selected -> accent.copy(alpha = 0.22f)
                    disabled -> NavySurfaceHigh.copy(alpha = 0.45f)
                    else -> NavySurfaceHigh
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, if (selected) accent else NavyOutline),
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !disabled) { onSelect(nation.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${nation.flag} ${nation.code}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                selected -> accent
                                disabled -> BroadcastMuted
                                else -> BroadcastWhite
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesSetupCard(
    campaign: InternationalCampaignState,
    onSelectUserNation: (String) -> Unit,
    onSelectOpponentNation: (String) -> Unit,
    onSelectSeriesLength: (Int) -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR NATION", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
            NationChipRow(
                nations = InternationalNations.all,
                selectedId = campaign.userNationId,
                disabledId = null,
                accent = TurfEmerald,
                onSelect = onSelectUserNation
            )
            Text("OPPONENT NATION", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
            NationChipRow(
                nations = InternationalNations.all,
                selectedId = campaign.opponentNationId,
                disabledId = campaign.userNationId,
                accent = AuctionGold,
                onSelect = onSelectOpponentNation
            )
            Text("SERIES MATCH COUNT", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InternationalNations.seriesLengthOptions.forEach { count ->
                    val selected = campaign.seriesTotalMatches == count
                    Surface(
                        color = if (selected) BoundarySky else NavySurfaceHigh,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) BoundarySky else NavyOutline),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSeriesLength(count) }
                            .testTag("series_len_$count")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (count == 1) "1 Match" else "$count Matches",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) NavyMidnight else BroadcastSilver
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BilateralSeriesScorecard(
    summary: CustomSeriesSummary,
    onPlayNextMatch: () -> Unit,
    onResetSeries: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.4.dp, if (summary.seriesCompleted) AuctionGold else TurfEmerald.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.totalMatches}-MATCH ${summary.format.shortLabel} SERIES",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = 1.sp,
                        color = AuctionGold
                    )
                )
                Text(
                    text = "${summary.matchesPlayed}/${summary.totalMatches} Played",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = BroadcastSilver
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavySurfaceHigh)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.teamA.teamName, style = MaterialTheme.typography.titleSmall, color = TurfEmerald, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("You", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.winsA}  —  ${summary.winsB}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = BroadcastWhite
                        )
                    )
                    if (summary.drawsOrTies > 0) {
                        Text(
                            text = if (summary.format == MatchFormat.TEST) "${summary.drawsOrTies} Drawn" else "${summary.drawsOrTies} Tied",
                            style = MaterialTheme.typography.labelSmall,
                            color = BoundarySky
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(summary.teamB.teamName, style = MaterialTheme.typography.titleSmall, color = AuctionGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("AI", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                }
            }

            if (summary.seriesCompleted) {
                Surface(
                    color = AuctionGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Series Winner: ${summary.seriesWinnerName}",
                                style = MaterialTheme.typography.titleSmall.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                            )
                        }
                        summary.playerOfTheSeries?.let { pots ->
                            Text(
                                text = "Player of the Series: $pots",
                                style = MaterialTheme.typography.bodySmall,
                                color = TurfEmerald,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onResetSeries,
                    border = BorderStroke(1.dp, AuctionGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_new_series_button")
                ) {
                    Text("Start New Series", style = MaterialTheme.typography.titleSmall, color = AuctionGold)
                }
            } else {
                Button(
                    onClick = onPlayNextMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_intl_series_match_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Play ${summary.format.shortLabel} Match ${summary.nextMatchNumber} of ${summary.totalMatches}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldCupActionCard(
    overview: WorldCupOverview,
    userNationId: String,
    busy: Boolean,
    onPlayFixture: (TeamState, TeamState) -> Unit,
    onQuickSimFixtures: (List<Pair<TeamState, TeamState>>) -> Unit
) {
    val total = overview.allGroupFixtures.size
    val completed = total - overview.unplayedGroupFixtures.size
    val myNext = overview.unplayedGroupFixtures.firstOrNull { (a, b) ->
        a.teamId == userNationId || b.teamId == userNationId
    }
    val nextAny = myNext ?: overview.unplayedGroupFixtures.firstOrNull()
    val aiOnly = overview.unplayedGroupFixtures.filter { (a, b) ->
        a.teamId != userNationId && b.teamId != userNationId
    }

    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = overview.title.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DisplayFontFamily,
                            color = BroadcastWhite
                        )
                    )
                }
                Text(
                    text = "$completed/$total Group",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = BoundarySky
                )
            }

            if (nextAny != null) {
                Button(
                    onClick = { onPlayFixture(nextAny.first, nextAny.second) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_next_wc_match_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Play: ${nextAny.first.teamName} vs ${nextAny.second.teamName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (aiOnly.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { onQuickSimFixtures(aiOnly) },
                            enabled = !busy,
                            border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("sim_ai_wc_matches_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(16.dp), BoundarySky, strokeWidth = 2.dp)
                            } else {
                                Text("Sim AI (${aiOnly.size})", style = MaterialTheme.typography.labelMedium, color = BoundarySky)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { onQuickSimFixtures(overview.unplayedGroupFixtures) },
                        enabled = !busy,
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("sim_to_wc_knockouts_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), AuctionGold, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("To Knockouts", style = MaterialTheme.typography.labelMedium, color = AuctionGold)
                        }
                    }
                }
            } else {
                Text(
                    text = "Group stage complete! Top ${overview.qualifierCount} nations advance to the Knockout Stage below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TurfEmerald
                )
            }
        }
    }
}

@Composable
private fun WorldCupBracketCard(
    overview: WorldCupOverview,
    onPlayKnockout: (PlayoffSlot) -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (overview.format == MatchFormat.TEST) "WTC GRAND FINAL" else "WORLD CUP KNOCKOUT BRACKET",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = 1.sp,
                        color = AuctionGold
                    )
                )
            }
            overview.knockoutSlots.forEach { slot ->
                Surface(
                    color = NavySurfaceHigh,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            slot.completedMatch != null -> TurfEmerald.copy(alpha = 0.5f)
                            slot.isPlayable -> AuctionGold.copy(alpha = 0.7f)
                            else -> NavyOutline
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = slot.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AuctionGold)
                            )
                            Text(
                                text = if (slot.teamA != null && slot.teamB != null) {
                                    "${slot.teamA.teamName} vs ${slot.teamB.teamName}"
                                } else {
                                    slot.subtitle
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            slot.completedMatch?.let { m ->
                                Text(
                                    text = m.resultSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurfEmerald
                                )
                            }
                        }
                        if (slot.isPlayable) {
                            Button(
                                onClick = { onPlayKnockout(slot) },
                                colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("play_wc_${slot.stage.name.lowercase()}")
                            ) {
                                Text("Play", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        } else if (slot.completedMatch == null) {
                            Text("LOCKED", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldCupStandingsCard(
    format: MatchFormat,
    standings: List<TeamStanding>,
    qualifierCount: Int,
    userNationId: String,
    onOpenFullStandings: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (format == MatchFormat.TEST) "WTC POINTS TABLE" else "WORLD CUP STANDINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = 1.sp,
                        color = BroadcastWhite
                    )
                )
                Text(
                    text = "Full Table →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TurfEmerald,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenFullStandings)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("intl_full_standings_link")
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(22.dp))
                Text("NATION", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.weight(1f))
                Text("P", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("W", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("L", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("PTS", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(34.dp))
                Text("NRR", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(46.dp))
            }
            HorizontalDivider(color = NavyOutline, modifier = Modifier.padding(vertical = 6.dp))
            standings.forEachIndexed { idx, row ->
                val qualifies = idx < qualifierCount
                val isUser = row.teamId == userNationId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                        color = if (qualifies) TurfEmerald else BroadcastMuted,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(
                        text = if (isUser) "${row.teamName} ★" else row.teamName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isUser) FontWeight.Bold else FontWeight.Medium),
                        color = if (isUser) AuctionGold else BroadcastWhite,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${row.played}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(24.dp))
                    Text("${row.won}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = TurfEmerald, modifier = Modifier.width(24.dp))
                    Text("${row.lost}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastMuted, modifier = Modifier.width(24.dp))
                    Text("${row.points}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = AuctionGold, modifier = Modifier.width(34.dp))
                    Text(row.nrrFormatted, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BoundarySky, modifier = Modifier.width(46.dp))
                }
            }
        }
    }
}

@Composable
private fun IntlMatchHistoryRow(rec: MatchRecord) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${rec.teamAName} vs ${rec.teamBName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = BroadcastWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(rec.matchStage.label, style = MaterialTheme.typography.labelSmall, color = BoundarySky)
            }
            Text(
                text = "${rec.teamAScore}/${rec.teamAWickets} (${rec.teamAOvers})  •  ${rec.teamBScore}/${rec.teamBWickets} (${rec.teamBOvers})",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = BroadcastSilver
            )
            Text(
                text = rec.resultSummary,
                style = MaterialTheme.typography.labelSmall,
                color = AuctionGold
            )
            if (rec.playerOfTheMatch.isNotBlank()) {
                Text(
                    text = "POTM: ${rec.playerOfTheMatch}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurfEmerald
                )
            }
        }
    }
}
