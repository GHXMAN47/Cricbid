package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Leaderboard
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AuctionRoom
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.TeamState
import com.example.engine.PlayoffSlot
import com.example.engine.SeriesSummary
import com.example.engine.TeamStanding
import com.example.engine.TournamentEngine
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
fun MatchHubScreen(
    room: AuctionRoom,
    currentUid: String,
    matches: List<MatchRecord>,
    busy: Boolean,
    onBack: () -> Unit,
    onEditMyXi: () -> Unit,
    onOpenStandings: () -> Unit = {},
    onStartMatch: (TeamState, TeamState, MatchStage) -> Unit,
    onQuickSimLeague: (List<Pair<TeamState, TeamState>>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val myTeam = room.teamForUser(currentUid)
    val overview = remember(room.teams, matches) {
        TournamentEngine.buildOverview(room.teams, matches)
    }

    val defaultTeamA = myTeam ?: room.teams.firstOrNull()
    val defaultTeamB = room.teams.firstOrNull { it.teamId != defaultTeamA?.teamId } ?: room.teams.lastOrNull()
    var teamAId by remember(room.teams) { mutableStateOf(defaultTeamA?.teamId ?: "") }
    var teamBId by remember(room.teams) { mutableStateOf(defaultTeamB?.teamId ?: "") }
    val selectedA = room.teams.firstOrNull { it.teamId == teamAId } ?: defaultTeamA
    val selectedB = room.teams.firstOrNull { it.teamId == teamBId } ?: defaultTeamB

    // Prioritize user's next unplayed league match if available
    val nextUserFixture = remember(overview.unplayedLeagueFixtures, myTeam?.teamId) {
        if (myTeam != null) {
            overview.unplayedLeagueFixtures.firstOrNull {
                it.first.teamId == myTeam.teamId || it.second.teamId == myTeam.teamId
            } ?: overview.unplayedLeagueFixtures.firstOrNull()
        } else {
            overview.unplayedLeagueFixtures.firstOrNull()
        }
    }
    val aiOnlyUnplayed = remember(overview.unplayedLeagueFixtures) {
        overview.unplayedLeagueFixtures.filter { it.first.bot && it.second.bot }
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
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Nav Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("match_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to rooms",
                            tint = BroadcastWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (overview.isSeriesMode) "5-MATCH T20 SERIES" else "LEAGUE & IPL PLAYOFFS",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DisplayFontFamily,
                                color = BroadcastWhite
                            )
                        )
                        Text(
                            text = "Room ${room.roomCode} • ${room.teams.size} Franchises",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastMuted
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenStandings,
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.65f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("open_standings_button")
                    ) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Standings", style = MaterialTheme.typography.labelMedium, color = AuctionGold)
                    }
                    if (myTeam != null) {
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(
                            onClick = onEditMyXi,
                            border = BorderStroke(1.dp, NavyOutline),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("edit_my_xi_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("XI", style = MaterialTheme.typography.labelMedium, color = BroadcastWhite)
                        }
                    }
                }
            }

            // Championship Trophy Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_trophy_banner_1795078739465),
                        contentDescription = "Cricket championship trophy",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        NavyMidnight.copy(alpha = 0.92f),
                                        NavyMidnight.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = when {
                                overview.championTeamName != null -> "CHAMPIONS CROWNED"
                                overview.isSeriesMode -> "BILATERAL T20 SERIES"
                                overview.leagueCompleted -> "IPL PLAYOFFS LIVE"
                                else -> "LEAGUE STAGE • TOP 4 QUALIFY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = AuctionGold
                        )
                        Text(
                            text = overview.championTeamName?.uppercase()
                                ?: if (overview.isSeriesMode) "BEST OF 5 T20 CLASH" else "ROAD TO THE FINAL",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = DisplayFontFamily,
                                color = BroadcastWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Pomp-Style Phase Engine • Chemistry • Impact Player Rule",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )
                    }
                }
            }

            // Your Franchise Captain & Playing XI Quick Status Strip
            if (myTeam != null) {
                item {
                    val xiPlayers = myTeam.playingXiPlayers
                    val xiOverseas = xiPlayers.count { it.overseas }
                    val xiValid = xiPlayers.size == com.example.data.model.SquadRules.MIN_SQUAD_SIZE &&
                        xiOverseas <= com.example.data.model.SquadRules.MAX_OVERSEAS_IN_XI
                    val captainName = myTeam.captainPlayer?.name ?: "Auto Captain"
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (xiValid) AuctionGold.copy(alpha = 0.45f) else com.example.ui.theme.WicketCrimson
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onEditMyXi)
                            .testTag("match_hub_captain_strip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = AuctionGold,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "C",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OnGoldDark
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${myTeam.teamName} • $captainName",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "XI OS: $xiOverseas/4 max  •  Squad OS: ${myTeam.overseasCount}/8  •  Tap to reorder 1–11",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (xiValid) BroadcastSilver else com.example.ui.theme.WicketCrimson,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = "EDIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TurfEmerald
                                )
                            )
                        }
                    }
                }
            }

            // MODE A: 2-Team 5-Match Series Dashboard
            if (overview.isSeriesMode && overview.seriesSummary != null) {
                item {
                    SeriesDashboardCard(
                        summary = overview.seriesSummary,
                        onPlayNext = {
                            onStartMatch(overview.seriesSummary.teamA, overview.seriesSummary.teamB, MatchStage.SERIES)
                        }
                    )
                }
            } else {
                // MODE B: 4-8 Team League Stage + IPL Playoffs Bracket
                if (overview.allLeagueFixtures.isNotEmpty() && !overview.leagueCompleted) {
                    item {
                        LeagueActionCard(
                            totalFixtures = overview.allLeagueFixtures.size,
                            remainingCount = overview.unplayedLeagueFixtures.size,
                            nextFixture = nextUserFixture,
                            aiOnlyCount = aiOnlyUnplayed.size,
                            busy = busy,
                            onPlayNext = { pair -> onStartMatch(pair.first, pair.second, MatchStage.LEAGUE) },
                            onSimAiFixtures = { onQuickSimLeague(aiOnlyUnplayed) },
                            onSimAllRemaining = { onQuickSimLeague(overview.unplayedLeagueFixtures) }
                        )
                    }
                }

                // IPL Playoffs Bracket (for 4+ Teams)
                if (overview.playoffSlots.isNotEmpty()) {
                    item {
                        PlayoffsBracketCard(
                            slots = overview.playoffSlots,
                            leagueCompleted = overview.leagueCompleted,
                            onPlayPlayoff = { slot ->
                                if (slot.teamA != null && slot.teamB != null) {
                                    onStartMatch(slot.teamA, slot.teamB, slot.stage)
                                }
                            }
                        )
                    }
                }

                // Points Table with Exact NRR
                item {
                    LeaguePointsTableCard(
                        standings = overview.standings,
                        showTopFourHighlight = room.teams.size >= 4,
                        onOpenFullStandings = onOpenStandings
                    )
                }
            }

            // Custom Exhibition Match Selector (always available for quick matchups)
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CUSTOM EXHIBITION MATCH",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = BroadcastSilver
                            )
                        )
                        Text("BATTING FIRST", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(room.teams, key = { "a_${it.teamId}" }) { team ->
                                TeamChipSelector(
                                    name = team.teamName,
                                    selected = team.teamId == selectedA?.teamId,
                                    accent = TurfEmerald,
                                    onClick = {
                                        teamAId = team.teamId
                                        if (teamBId == team.teamId) {
                                            teamBId = room.teams.firstOrNull { it.teamId != team.teamId }?.teamId ?: ""
                                        }
                                    }
                                )
                            }
                        }
                        Text("CHASING SIDE", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(room.teams, key = { "b_${it.teamId}" }) { team ->
                                TeamChipSelector(
                                    name = team.teamName,
                                    selected = team.teamId == selectedB?.teamId,
                                    accent = AuctionGold,
                                    onClick = {
                                        teamBId = team.teamId
                                        if (teamAId == team.teamId) {
                                            teamAId = room.teams.firstOrNull { it.teamId != team.teamId }?.teamId ?: ""
                                        }
                                    }
                                )
                            }
                        }
                        val canSim = selectedA != null && selectedB != null && selectedA.teamId != selectedB.teamId
                        OutlinedButton(
                            onClick = {
                                if (selectedA != null && selectedB != null) {
                                    val stage = if (overview.isSeriesMode) MatchStage.SERIES else MatchStage.LEAGUE
                                    onStartMatch(selectedA, selectedB, stage)
                                }
                            },
                            enabled = canSim && !busy,
                            border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("simulate_match_button")
                        ) {
                            Icon(Icons.Filled.SportsCricket, contentDescription = null, tint = AuctionGold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Simulate Custom T20 Match",
                                style = MaterialTheme.typography.titleSmall.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Recent Match Results (last 12 for lightweight scrolling)
            if (matches.isNotEmpty()) {
                item {
                    Text(
                        text = "RECENT RESULTS (${matches.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = BroadcastSilver
                        )
                    )
                }
                items(matches.asReversed().take(12), key = { it.id.ifBlank { "${it.teamAId}_${it.teamAScore}_${it.teamBScore}_${it.stage}" } }) { rec ->
                    MatchHistoryCard(rec)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SeriesDashboardCard(
    summary: SeriesSummary,
    onPlayNext: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, AuctionGold.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "5-MATCH SERIES SCORELINE",
                    style = MaterialTheme.typography.labelSmall.copy(color = AuctionGold, letterSpacing = 1.sp)
                )
                Text(
                    text = "${summary.matchesPlayed}/5 Played",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, color = BroadcastMuted)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.teamA.teamName, style = MaterialTheme.typography.titleMedium, color = TurfEmerald, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("WINS", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                }
                Text(
                    text = "${summary.winsA}  —  ${summary.winsB}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = BroadcastWhite
                    )
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(summary.teamB.teamName, style = MaterialTheme.typography.titleMedium, color = AuctionGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("WINS", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                }
            }

            if (summary.seriesCompleted) {
                Surface(
                    color = TurfEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🏆 Series Winner: ${summary.seriesWinnerName}",
                            style = MaterialTheme.typography.titleSmall.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                        )
                        summary.playerOfTheSeries?.let { pots ->
                            Text(
                                text = "⭐ Player of the Series: $pots",
                                style = MaterialTheme.typography.bodySmall,
                                color = TurfEmerald
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = onPlayNext,
                    colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_next_series_match_button")
                ) {
                    Icon(Icons.Filled.SportsCricket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Play Match #${summary.nextMatchNumber} of 5",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeagueActionCard(
    totalFixtures: Int,
    remainingCount: Int,
    nextFixture: Pair<TeamState, TeamState>?,
    aiOnlyCount: Int,
    busy: Boolean,
    onPlayNext: (Pair<TeamState, TeamState>) -> Unit,
    onSimAiFixtures: () -> Unit,
    onSimAllRemaining: () -> Unit
) {
    val played = totalFixtures - remainingCount
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, TurfEmerald.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "LEAGUE STAGE",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily, color = TurfEmerald)
                )
                Text(
                    text = "$played / $totalFixtures Matches",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily, color = BroadcastSilver)
                )
            }

            if (nextFixture != null) {
                Button(
                    onClick = { onPlayNext(nextFixture) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_next_league_match_button")
                ) {
                    Icon(Icons.Filled.SportsCricket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Play: ${nextFixture.first.teamName} vs ${nextFixture.second.teamName}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (aiOnlyCount > 0) {
                    OutlinedButton(
                        onClick = onSimAiFixtures,
                        enabled = !busy,
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("sim_ai_league_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), BoundarySky, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.FastForward, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sim $aiOnlyCount AI", style = MaterialTheme.typography.labelMedium, color = BoundarySky)
                        }
                    }
                }
                OutlinedButton(
                    onClick = onSimAllRemaining,
                    enabled = !busy && remainingCount > 0,
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("sim_all_league_button")
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), TurfEmerald, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.FastForward, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sim to Playoffs", style = MaterialTheme.typography.labelMedium, color = TurfEmerald)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayoffsBracketCard(
    slots: List<PlayoffSlot>,
    leagueCompleted: Boolean,
    onPlayPlayoff: (PlayoffSlot) -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, if (leagueCompleted) AuctionGold.copy(alpha = 0.65f) else NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "IPL PLAYOFFS BRACKET",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = 1.sp,
                        color = AuctionGold
                    )
                )
            }
            if (!leagueCompleted) {
                Text(
                    text = "Finish League Stage to lock Top 4 seeds for Qualifier 1, Eliminator, Qualifier 2 & Final.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastMuted
                )
            }
            slots.forEach { slot ->
                Surface(
                    color = NavySurfaceHigh,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            slot.completedMatch != null -> TurfEmerald.copy(alpha = 0.4f)
                            slot.isPlayable -> AuctionGold.copy(alpha = 0.65f)
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
                                text = "${slot.title} • ${slot.subtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slot.stage == MatchStage.FINAL) AuctionGold else BoundarySky
                            )
                            Spacer(Modifier.height(3.dp))
                            val matchup = if (slot.teamA != null && slot.teamB != null) {
                                "${slot.teamA.teamName} vs ${slot.teamB.teamName}"
                            } else {
                                "TBD vs TBD"
                            }
                            Text(
                                text = matchup,
                                style = MaterialTheme.typography.titleSmall,
                                color = BroadcastWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            slot.completedMatch?.let { m ->
                                Text(
                                    text = m.resultSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TurfEmerald
                                )
                            }
                        }
                        if (slot.isPlayable) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onPlayPlayoff(slot) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (slot.stage == MatchStage.FINAL) AuctionGold else TurfEmerald,
                                    contentColor = if (slot.stage == MatchStage.FINAL) OnGoldDark else OnEmeraldDark
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("play_${slot.stage.name.lowercase()}")
                            ) {
                                Text("Play", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaguePointsTableCard(
    standings: List<TeamStanding>,
    showTopFourHighlight: Boolean,
    onOpenFullStandings: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "POINTS TABLE & NRR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = BroadcastWhite
                        )
                    )
                }
                Text(
                    text = "Full Table →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TurfEmerald,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onOpenFullStandings)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("full_standings_link")
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(20.dp))
                Text("TEAM", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.weight(1f))
                Text("P", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("W", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("L", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted, modifier = Modifier.width(24.dp))
                Text("PTS", style = MaterialTheme.typography.labelSmall, color = AuctionGold, modifier = Modifier.width(30.dp))
                Text("NRR", style = MaterialTheme.typography.labelSmall, color = BoundarySky, modifier = Modifier.width(46.dp))
            }
            HorizontalDivider(color = NavyOutline)
            standings.forEachIndexed { idx, row ->
                val isPlayoffZone = showTopFourHighlight && idx < 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold),
                        color = if (isPlayoffZone) TurfEmerald else BroadcastMuted,
                        modifier = Modifier.width(20.dp)
                    )
                    Text(
                        row.teamName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isPlayoffZone) FontWeight.SemiBold else FontWeight.Normal),
                        color = BroadcastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${row.played}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastSilver, modifier = Modifier.width(24.dp))
                    Text("${row.won}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = TurfEmerald, modifier = Modifier.width(24.dp))
                    Text("${row.lost}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily), color = BroadcastMuted, modifier = Modifier.width(24.dp))
                    Text("${row.points}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = AuctionGold, modifier = Modifier.width(30.dp))
                    Text(
                        row.nrrFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = if (row.nrr >= 0.0) BoundarySky else BroadcastMuted,
                        modifier = Modifier.width(46.dp)
                    )
                }
                if (showTopFourHighlight && idx == 3 && standings.size > 4) {
                    HorizontalDivider(color = TurfEmerald.copy(alpha = 0.45f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun TeamChipSelector(
    name: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.2f) else NavySurfaceHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.2.dp, if (selected) accent else NavyOutline),
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) accent else BroadcastSilver,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun MatchHistoryCard(record: MatchRecord) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Surface(
                color = NavySurfaceHigh,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = record.matchStage.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(color = BoundarySky, fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.teamAName, style = MaterialTheme.typography.titleSmall, color = BroadcastWhite)
                Text(
                    "${record.teamAScore}/${record.teamAWickets} (${record.teamAOvers})",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily, color = TurfEmerald)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.teamBName, style = MaterialTheme.typography.titleSmall, color = BroadcastWhite)
                Text(
                    "${record.teamBScore}/${record.teamBWickets} (${record.teamBOvers})",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily, color = AuctionGold)
                )
            }
            Text(
                text = record.resultSummary,
                style = MaterialTheme.typography.labelMedium,
                color = AuctionGold
            )
            Text(
                text = "POTM: ${record.playerOfTheMatch}",
                style = MaterialTheme.typography.bodySmall,
                color = BroadcastMuted
            )
        }
    }
}
