package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SeasonLeaderboardSummary
import com.example.engine.SeasonPlayerLeader
import com.example.engine.TrophyCabinetData
import com.example.engine.TrophyCabinetRecord
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
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

private val OrangeCapAccent = Color(0xFFFF8C00)
private val PurpleCapAccent = Color(0xFFA855F7)

@Composable
fun TrophyCabinetScreen(
    cabinet: TrophyCabinetData,
    seasonLeaderboards: SeasonLeaderboardSummary,
    activeCompetitionTitle: String,
    initialTab: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

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
            // Header
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
                        .testTag("trophy_cabinet_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BroadcastWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TROPHIES & SEASON CAPS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "Persistent Championship Cabinet • Orange Cap • Purple Cap • MVP",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                }
            }

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
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.testTag("tab_trophy_cabinet"),
                    text = { Text("Trophy Cabinet (${cabinet.championshipsWon})", maxLines = 1) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.testTag("tab_season_caps"),
                    text = { Text("Orange / Purple / MVP", maxLines = 1) }
                )
            }

            if (selectedTab == 0) {
                TrophyCabinetTabContent(cabinet = cabinet, modifier = Modifier.weight(1f))
            } else {
                SeasonCapsTabContent(
                    summary = seasonLeaderboards,
                    activeCompetitionTitle = activeCompetitionTitle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TrophyCabinetTabContent(
    cabinet: TrophyCabinetData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CabinetMetricCard(
                    label = "TITLES WON",
                    value = "${cabinet.championshipsWon}",
                    accent = AuctionGold,
                    modifier = Modifier.weight(1f)
                )
                CabinetMetricCard(
                    label = "FINALS PLAYED",
                    value = "${cabinet.finalsPlayed}",
                    accent = TurfEmerald,
                    modifier = Modifier.weight(1f)
                )
                val winRate = if (cabinet.finalsPlayed == 0) 0 else (cabinet.championshipsWon * 100 / cabinet.finalsPlayed)
                CabinetMetricCard(
                    label = "FINAL WIN %",
                    value = "$winRate%",
                    accent = BoundarySky,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (cabinet.trophies.isEmpty()) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = AuctionGold.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Your Trophy Cabinet Awaits",
                            style = MaterialTheme.typography.titleMedium,
                            color = BroadcastWhite
                        )
                        Text(
                            text = "Win an IPL Franchise Final, 5-Match Series, World Cup, or WTC Final to engrave your championship trophy and Season Award winners permanently on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )
                    }
                }
            }
        } else {
            itemsIndexed(cabinet.trophies, key = { _, item -> item.id }) { _, record ->
                TrophyRecordCard(record = record)
            }
        }
    }
}

@Composable
private fun CabinetMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = BroadcastMuted
            )
        }
    }
}

@Composable
private fun TrophyRecordCard(record: TrophyCabinetRecord) {
    val accent = if (record.userWon) AuctionGold else BoundarySky
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (record.userWon) Icons.Filled.EmojiEvents else Icons.Filled.MilitaryTech,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BroadcastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (record.userWon) "CHAMPION" else "RUNNER-UP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = "${record.championTeamName} def. ${record.runnerUpTeamName}",
                style = MaterialTheme.typography.bodyMedium,
                color = TurfEmerald
            )
            if (record.summary.isNotBlank()) {
                Text(
                    text = record.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastSilver
                )
            }
            if (record.orangeCapWinner.isNotBlank() || record.purpleCapWinner.isNotBlank() || record.mvpWinner.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (record.orangeCapWinner.isNotBlank()) {
                        AwardMiniPill("🟠 ${record.orangeCapWinner}", OrangeCapAccent, Modifier.weight(1f))
                    }
                    if (record.purpleCapWinner.isNotBlank()) {
                        AwardMiniPill("🟣 ${record.purpleCapWinner}", PurpleCapAccent, Modifier.weight(1f))
                    }
                }
                if (record.mvpWinner.isNotBlank()) {
                    AwardMiniPill("⭐ MVP: ${record.mvpWinner}", AuctionGold, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AwardMiniPill(text: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SeasonCapsTabContent(
    summary: SeasonLeaderboardSummary,
    activeCompetitionTitle: String,
    modifier: Modifier = Modifier
) {
    var boardIdx by remember { mutableIntStateOf(0) } // 0=Orange, 1=Purple, 2=MVP
    val list = when (boardIdx) {
        0 -> summary.orangeCapTable
        1 -> summary.purpleCapTable
        else -> summary.mvpTable
    }
    val accent = when (boardIdx) {
        0 -> OrangeCapAccent
        1 -> PurpleCapAccent
        else -> AuctionGold
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = activeCompetitionTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = DisplayFontFamily,
                    letterSpacing = 1.sp,
                    color = BoundarySky
                )
            )
            Spacer(Modifier.height(6.dp))
            // Live Cap Holder Spotlight Trio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapSpotlightCard(
                    title = "🟠 ORANGE CAP",
                    leader = summary.orangeCapHolder,
                    statText = summary.orangeCapHolder?.let { "${it.runs} Runs" } ?: "TBD",
                    accent = OrangeCapAccent,
                    selected = boardIdx == 0,
                    onClick = { boardIdx = 0 },
                    modifier = Modifier.weight(1f)
                )
                CapSpotlightCard(
                    title = "🟣 PURPLE CAP",
                    leader = summary.purpleCapHolder,
                    statText = summary.purpleCapHolder?.let { "${it.wickets} Wkts" } ?: "TBD",
                    accent = PurpleCapAccent,
                    selected = boardIdx == 1,
                    onClick = { boardIdx = 1 },
                    modifier = Modifier.weight(1f)
                )
                CapSpotlightCard(
                    title = "⭐ SEASON MVP",
                    leader = summary.mvpHolder,
                    statText = summary.mvpHolder?.let { "${it.mvpPoints.toInt()} pts" } ?: "TBD",
                    accent = AuctionGold,
                    selected = boardIdx == 2,
                    onClick = { boardIdx = 2 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (list.isEmpty()) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Complete at least 1 match in your active tournament to track live Orange Cap, Purple Cap, and Season MVP standings!",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            itemsIndexed(list, key = { _, item -> "${boardIdx}_${item.playerId}" }) { idx, leader ->
                LeaderboardRowCard(
                    rank = idx + 1,
                    leader = leader,
                    mode = boardIdx,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun CapSpotlightCard(
    title: String,
    leader: SeasonPlayerLeader?,
    statText: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) accent.copy(alpha = 0.16f) else NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else NavyOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
            Text(
                text = leader?.playerName ?: "—",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = BroadcastWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    leader: SeasonPlayerLeader,
    mode: Int,
    accent: Color
) {
    Surface(
        color = if (rank == 1) accent.copy(alpha = 0.12f) else NavySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (rank == 1) accent.copy(alpha = 0.65f) else NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (rank == 1) accent else BroadcastMuted
                ),
                modifier = Modifier.width(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rank == 1) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = leader.playerName,
                        style = MaterialTheme.typography.titleSmall,
                        color = BroadcastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${leader.teamName} • ${leader.matches}M",
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                when (mode) {
                    0 -> {
                        Text(
                            text = "${leader.runs} RUNS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        )
                        Text(
                            text = "HS ${leader.highestScore} • SR ${String.format("%.1f", leader.strikeRate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                    }
                    1 -> {
                        Text(
                            text = "${leader.wickets} WKTS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        )
                        Text(
                            text = "Econ ${String.format("%.2f", leader.economy)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                    }
                    else -> {
                        Text(
                            text = "${leader.mvpPoints.toInt()} PTS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        )
                        Text(
                            text = "${leader.runs}r • ${leader.wickets}w",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                    }
                }
            }
        }
    }
}
