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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TeamState
import com.example.engine.TeamStanding
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
import java.util.Locale

enum class StandingsSortColumn(val label: String) {
    OFFICIAL("Rank"),
    POINTS("Points"),
    NRR("NRR"),
    WINS("Wins"),
    LOSSES("Losses"),
    PLAYED("Played")
}

@Composable
fun StandingsScreen(
    tournamentTitle: String,
    tournamentSubtitle: String,
    standings: List<TeamStanding>,
    teams: List<TeamState>,
    matchesCompleted: Int,
    totalScheduledMatches: Int,
    highlightTeamId: String?,
    qualifierCount: Int,
    winPoints: Int = 2,
    tiePoints: Int = 1,
    tieColumnLabel: String = "T",
    championTeamName: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var sortColumn by rememberSaveable { mutableStateOf(StandingsSortColumn.OFFICIAL) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var expandedTeamId by rememberSaveable { mutableStateOf<String?>(null) }

    val teamById = remember(teams) { teams.associateBy { it.teamId } }

    // Official rank map (1-based) so every team keeps its true tournament rank badge even when sorted by another column
    val officialRankByTeamId = remember(standings) {
        standings.mapIndexed { idx, row -> row.teamId to (idx + 1) }.toMap()
    }

    val displayedStandings = remember(standings, sortColumn, sortDescending) {
        val base = when (sortColumn) {
            StandingsSortColumn.OFFICIAL -> standings
            StandingsSortColumn.POINTS -> standings.sortedWith(
                compareByDescending<TeamStanding> { it.points }
                    .thenByDescending { it.nrr }
                    .thenByDescending { it.won }
            )
            StandingsSortColumn.NRR -> standings.sortedWith(
                compareByDescending<TeamStanding> { it.nrr }
                    .thenByDescending { it.points }
            )
            StandingsSortColumn.WINS -> standings.sortedWith(
                compareByDescending<TeamStanding> { it.won }
                    .thenByDescending { it.nrr }
            )
            StandingsSortColumn.LOSSES -> standings.sortedWith(
                compareByDescending<TeamStanding> { it.lost }
                    .thenBy { it.nrr }
            )
            StandingsSortColumn.PLAYED -> standings.sortedWith(
                compareByDescending<TeamStanding> { it.played }
                    .thenByDescending { it.points }
            )
        }
        if (sortColumn == StandingsSortColumn.OFFICIAL || sortDescending) base else base.reversed()
    }

    val leader = standings.firstOrNull()
    val showCutoffLine = sortColumn == StandingsSortColumn.OFFICIAL &&
        qualifierCount in 1 until displayedStandings.size

    fun toggleSort(column: StandingsSortColumn) {
        if (column == StandingsSortColumn.OFFICIAL) {
            sortColumn = StandingsSortColumn.OFFICIAL
            sortDescending = true
        } else if (sortColumn == column) {
            sortDescending = !sortDescending
        } else {
            sortColumn = column
            sortDescending = true
        }
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
                .widthIn(max = 660.dp)
                .testTag("standings_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bar
            item(key = "standings_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("standings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BroadcastWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Leaderboard,
                                contentDescription = null,
                                tint = AuctionGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.standings_title).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.sp,
                                    color = BroadcastWhite
                                )
                            )
                        }
                        Text(
                            text = "$tournamentTitle • $tournamentSubtitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = TurfEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Champion Banner (if crowned)
            if (!championTeamName.isNullOrBlank()) {
                item(key = "standings_champion") {
                    Surface(
                        color = AuctionGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, AuctionGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Tournament Champion",
                                tint = AuctionGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "TOURNAMENT CHAMPIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuctionGold
                                )
                                Text(
                                    text = championTeamName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BroadcastWhite
                                )
                            }
                        }
                    }
                }
            }

            // Tournament Summary KPI Strip
            item(key = "standings_kpis") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StandingsKpiCard(
                        label = "MATCHES",
                        value = if (totalScheduledMatches > 0) "$matchesCompleted/$totalScheduledMatches" else "$matchesCompleted",
                        accent = BoundarySky,
                        modifier = Modifier.weight(1f)
                    )
                    StandingsKpiCard(
                        label = "LEADER",
                        value = if (leader != null && leader.played > 0) leader.teamName else "TBD",
                        subValue = if (leader != null && leader.played > 0) "${leader.points} pts • ${leader.nrrFormatted}" else "0 played",
                        accent = AuctionGold,
                        modifier = Modifier.weight(1.35f)
                    )
                    StandingsKpiCard(
                        label = "QUALIFY",
                        value = if (qualifierCount > 0) "Top $qualifierCount" else "Series",
                        accent = TurfEmerald,
                        modifier = Modifier.weight(0.95f)
                    )
                }
            }

            // Sort Controls Ribbon
            item(key = "standings_sort_chips") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(StandingsSortColumn.entries) { col ->
                        val active = sortColumn == col
                        Surface(
                            color = if (active) TurfEmerald else NavySurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) TurfEmerald else NavyOutline),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { toggleSort(col) }
                                .testTag("sort_${col.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = col.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) OnEmeraldDark else BroadcastSilver
                                    )
                                )
                                if (active && col != StandingsSortColumn.OFFICIAL) {
                                    Icon(
                                        imageVector = if (sortDescending) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = OnEmeraldDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabular Data View Card
            item(key = "standings_table") {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("standings_table_card")
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        // Table Header Row (Tappable sortable columns)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavySurfaceHigh)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastMuted,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = "TEAM",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastMuted,
                                modifier = Modifier.weight(1f)
                            )
                            SortableHeaderCell(
                                label = "P",
                                active = sortColumn == StandingsSortColumn.PLAYED,
                                width = 26.dp,
                                onClick = { toggleSort(StandingsSortColumn.PLAYED) }
                            )
                            SortableHeaderCell(
                                label = "W",
                                active = sortColumn == StandingsSortColumn.WINS,
                                activeColor = TurfEmerald,
                                width = 26.dp,
                                onClick = { toggleSort(StandingsSortColumn.WINS) }
                            )
                            SortableHeaderCell(
                                label = "L",
                                active = sortColumn == StandingsSortColumn.LOSSES,
                                activeColor = WicketCrimson,
                                width = 26.dp,
                                onClick = { toggleSort(StandingsSortColumn.LOSSES) }
                            )
                            Text(
                                text = tieColumnLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BroadcastMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(24.dp)
                            )
                            SortableHeaderCell(
                                label = "NRR",
                                active = sortColumn == StandingsSortColumn.NRR,
                                activeColor = BoundarySky,
                                width = 54.dp,
                                onClick = { toggleSort(StandingsSortColumn.NRR) }
                            )
                            SortableHeaderCell(
                                label = "PTS",
                                active = sortColumn == StandingsSortColumn.POINTS || sortColumn == StandingsSortColumn.OFFICIAL,
                                activeColor = AuctionGold,
                                width = 36.dp,
                                onClick = { toggleSort(StandingsSortColumn.POINTS) }
                            )
                        }

                        HorizontalDivider(color = NavyOutline)

                        if (displayedStandings.isEmpty()) {
                            Text(
                                text = "No teams registered in this tournament yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastMuted,
                                modifier = Modifier.padding(20.dp)
                            )
                        } else {
                            displayedStandings.forEachIndexed { displayIdx, row ->
                                val officialRank = officialRankByTeamId[row.teamId] ?: (displayIdx + 1)
                                val inQualificationZone = qualifierCount > 0 && officialRank <= qualifierCount
                                val isUserTeam = row.teamId == highlightTeamId
                                val isExpanded = expandedTeamId == row.teamId
                                val teamMeta = teamById[row.teamId]

                                StandingsTableRow(
                                    rank = officialRank,
                                    row = row,
                                    inQualificationZone = inQualificationZone,
                                    isUserTeam = isUserTeam,
                                    isEvenRow = displayIdx % 2 == 0,
                                    isExpanded = isExpanded,
                                    captainName = teamMeta?.captainPlayer?.name,
                                    onClick = {
                                        expandedTeamId = if (isExpanded) null else row.teamId
                                    }
                                )

                                // Qualification Cutoff Line right below the last qualifying rank
                                if (showCutoffLine && displayIdx + 1 == qualifierCount) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(TurfEmerald.copy(alpha = 0.08f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(
                                            color = TurfEmerald.copy(alpha = 0.55f),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "TOP $qualifierCount QUALIFY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = TurfEmerald
                                        )
                                        HorizontalDivider(
                                            color = TurfEmerald.copy(alpha = 0.55f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else if (displayIdx < displayedStandings.lastIndex) {
                                    HorizontalDivider(
                                        color = NavyOutline.copy(alpha = 0.55f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tap-hint & Scoring Rules Legend
            item(key = "standings_legend") {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "POINTS & TIE-BREAKER RULES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 0.8.sp,
                                color = BroadcastSilver
                            )
                        )
                        Text(
                            text = "Win = +$winPoints pts  •  Tie/Draw = +$tiePoints pt${if (tiePoints == 1) "" else "s"}  •  Loss = 0 pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastWhite
                        )
                        Text(
                            text = "Tie-breaker: Points → Net Run Rate (NRR) → Total Wins. Tap any team row to inspect Runs For/Against, Win %, Captain, and Last 5 Form.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsKpiCard(
    label: String,
    value: String,
    accent: Color,
    subValue: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = BroadcastMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = if (subValue == null) MonoFontFamily else MaterialTheme.typography.titleSmall.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subValue != null) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp
                    ),
                    color = BroadcastSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SortableHeaderCell(
    label: String,
    active: Boolean,
    width: androidx.compose.ui.unit.Dp,
    activeColor: Color = TurfEmerald,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = if (active) activeColor else BroadcastMuted
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun StandingsTableRow(
    rank: Int,
    row: TeamStanding,
    inQualificationZone: Boolean,
    isUserTeam: Boolean,
    isEvenRow: Boolean,
    isExpanded: Boolean,
    captainName: String?,
    onClick: () -> Unit
) {
    val rowBg = when {
        isUserTeam -> AuctionGold.copy(alpha = 0.11f)
        isExpanded -> NavySurfaceHigh.copy(alpha = 0.7f)
        isEvenRow -> Color.Transparent
        else -> NavySurfaceHigh.copy(alpha = 0.28f)
    }
    val nrrColor = when {
        row.nrr > 0.0005 -> TurfEmerald
        row.nrr < -0.0005 -> WicketCrimson
        else -> BoundarySky
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .testTag("standings_row_${row.teamId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank cell with qualification accent
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = when {
                        isUserTeam -> AuctionGold
                        inQualificationZone -> TurfEmerald
                        else -> BroadcastMuted
                    }
                )
            }

            // Team Name + compact badges
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = row.teamName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUserTeam || inQualificationZone) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isUserTeam) AuctionGold else BroadcastWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isUserTeam) {
                    Surface(
                        color = AuctionGold,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "YOU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnGoldDark
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else if (inQualificationZone && row.played > 0) {
                    Surface(
                        color = TurfEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Q",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TurfEmerald
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Played
            Text(
                text = "${row.played}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                color = BroadcastSilver,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp)
            )
            // Won
            Text(
                text = "${row.won}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TurfEmerald,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp)
            )
            // Lost
            Text(
                text = "${row.lost}",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                color = if (row.lost > 0) WicketCrimson.copy(alpha = 0.9f) else BroadcastMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(26.dp)
            )
            // Tied / Draw
            Text(
                text = "${row.tied}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = BroadcastMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
            // Net Run Rate
            Text(
                text = row.nrrFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = nrrColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(54.dp)
            )
            // Points
            Text(
                text = "${row.points}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = AuctionGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(36.dp)
            )
        }

        // Expandable inline stats drawer (Last 5 Form, Runs For/Against, Win %, Captain)
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyMidnight.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOR: ${row.runsFor} (${row.oversForFormatted} ov)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = TurfEmerald
                    )
                    Text(
                        text = "AGAINST: ${row.runsAgainst} (${row.oversAgainstFormatted} ov)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = WicketCrimson
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append("Win Rate: ${String.format(Locale.US, "%.0f%%", row.winPercentage)}")
                            if (!captainName.isNullOrBlank()) {
                                append("  •  Captain: $captainName (C)")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (row.recentForm.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.recentForm.forEach { res ->
                                val chipColor = when (res) {
                                    "W" -> TurfEmerald
                                    "L" -> WicketCrimson
                                    else -> AuctionGold
                                }
                                Surface(
                                    color = chipColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.8.dp, chipColor.copy(alpha = 0.65f))
                                ) {
                                    Text(
                                        text = res,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = chipColor
                                        ),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
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
