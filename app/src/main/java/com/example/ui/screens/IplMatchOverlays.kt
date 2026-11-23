package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.Cricketer
import com.example.engine.LiveMatchState
import com.example.engine.StrategicTimeoutPrompt
import com.example.engine.StrategicTimeoutSide
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
 * Brief, skippable official IPL Strategic Time-Out overlay (Overs 6–9 bowling side, Overs 13–16 batting side).
 */
@Composable
fun StrategicTimeoutOverlay(
    prompt: StrategicTimeoutPrompt,
    onResolve: (String) -> Unit
) {
    val accent = if (prompt.side == StrategicTimeoutSide.BOWLING_SIDE) BoundarySky else AuctionGold
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight.copy(alpha = 0.80f))
            .padding(20.dp)
            .testTag("strategic_timeout_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Strategic Time-Out",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "IPL STRATEGIC TIME-OUT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = accent
                            )
                        )
                    }
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "END OF OVER ${prompt.overCompleted}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "${prompt.teamName} • ${prompt.side.label}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = BroadcastWhite
                )
                Text(
                    text = "${prompt.side.windowLabel} • Choose a quick 2.5-minute huddle instruction or skip straight back to live action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastSilver
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResolve("BOWL_RECHARGE") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BoundarySky,
                            contentColor = NavyMidnight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("timeout_bowl_recharge_button")
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bowl Plan (+Stamina)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                    Button(
                        onClick = { onResolve("BAT_FOCUS") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuctionGold,
                            contentColor = OnGoldDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("timeout_bat_focus_button")
                    ) {
                        Icon(Icons.Filled.SportsCricket, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bat Plan (+Focus)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = { onResolve("SKIP") },
                    border = BorderStroke(1.dp, NavyOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("timeout_skip_button")
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Skip Time-Out", tint = BroadcastSilver, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Skip Strategic Time-Out", style = MaterialTheme.typography.labelLarge, color = BroadcastWhite)
                }
            }
        }
    }
}

/**
 * Unlimited Super Over Tie-Breaker lineup selector:
 * Lets the user nominate 3 batters (max 2 wickets) & 1 bowler for the current Super Over round,
 * strictly blocking batters dismissed in any previous Super Over and bowlers who bowled in any previous Super Over.
 */
@Composable
fun SuperOverNominationOverlay(
    match: LiveMatchState,
    userTeamId: String,
    onConfirm: (teamId: String, batterIds: List<String>, bowlerId: String) -> Unit
) {
    val targetTeam = remember(match.teamA.teamId, match.teamB.teamId, userTeamId) {
        when (userTeamId) {
            match.teamB.teamId -> match.teamB
            else -> match.teamA
        }
    }
    val xiPlayers: List<Cricketer> = remember(targetTeam, match.innings1.battingLineup, match.innings2.battingLineup) {
        if (match.innings1.battingTeamId == targetTeam.teamId) {
            match.innings1.battingLineup
        } else {
            match.innings2.battingLineup
        }
    }

    val excludedBatters = match.usedSuperOverDismissedIds[targetTeam.teamId].orEmpty()
    val excludedBowlers = match.usedSuperOverBowlerIds[targetTeam.teamId].orEmpty()

    val eligibleBatters = remember(xiPlayers, excludedBatters) {
        val fresh = xiPlayers.filter { it.id !in excludedBatters }
        if (fresh.size >= 3) fresh else xiPlayers
    }
    val eligibleBowlers = remember(xiPlayers, excludedBowlers) {
        val fresh = xiPlayers.filter { it.id !in excludedBowlers }
        if (fresh.isNotEmpty()) fresh else xiPlayers
    }

    var selectedBatters by remember(match.superOverRound, targetTeam.teamId) {
        mutableStateOf(
            eligibleBatters
                .sortedByDescending { it.battingRating }
                .take(3)
                .map { it.id }
        )
    }
    var selectedBowler by remember(match.superOverRound, targetTeam.teamId) {
        mutableStateOf(
            eligibleBowlers
                .maxByOrNull { it.bowlingRating }
                ?.id
                ?: xiPlayers.firstOrNull()?.id.orEmpty()
        )
    }

    val canConfirm = selectedBatters.size == 3 && selectedBowler.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight.copy(alpha = 0.88f))
            .padding(16.dp)
            .testTag("super_over_nomination_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, WicketCrimson.copy(alpha = 0.85f)),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Super Over",
                            tint = WicketCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SUPER OVER #${match.superOverRound}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = AuctionGold
                            )
                        )
                    }
                    Surface(
                        color = WicketCrimson.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "6 BALLS • 2 WKTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MonoFontFamily,
                                color = WicketCrimson,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "${targetTeam.teamName}: Nominate 3 Batters & 1 Bowler. Batters dismissed or bowlers used in an earlier Super Over cannot repeat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastSilver
                )

                Text(
                    text = "SELECT 3 BATTERS (${selectedBatters.size}/3)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TurfEmerald,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    xiPlayers.sortedByDescending { it.battingRating }.forEach { player ->
                        val isBlocked = player.id in excludedBatters && xiPlayers.count { it.id !in excludedBatters } >= 3
                        val isPicked = player.id in selectedBatters
                        val border = when {
                            isBlocked -> NavyOutline.copy(alpha = 0.4f)
                            isPicked -> TurfEmerald
                            else -> NavyOutline
                        }
                        Surface(
                            color = when {
                                isBlocked -> NavyMidnight.copy(alpha = 0.6f)
                                isPicked -> TurfEmerald.copy(alpha = 0.16f)
                                else -> NavySurfaceHigh
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isBlocked) {
                                    selectedBatters = if (isPicked) {
                                        selectedBatters - player.id
                                    } else if (selectedBatters.size < 3) {
                                        selectedBatters + player.id
                                    } else {
                                        selectedBatters.drop(1) + player.id
                                    }
                                }
                                .testTag("so_batter_${player.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isBlocked) BroadcastMuted else BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBlocked) "OUT IN PREVIOUS SUPER OVER (LOCKED)" else "BAT ${player.battingRating} • ${player.role.shortLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBlocked) WicketCrimson else BroadcastSilver
                                    )
                                }
                                if (isPicked) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = TurfEmerald, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "SELECT 1 BOWLER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = BoundarySky,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    xiPlayers.sortedByDescending { it.bowlingRating }.take(7).forEach { player ->
                        val isBlocked = player.id in excludedBowlers && xiPlayers.any { it.id !in excludedBowlers }
                        val isPicked = selectedBowler == player.id
                        Surface(
                            color = when {
                                isBlocked -> NavyMidnight.copy(alpha = 0.6f)
                                isPicked -> BoundarySky.copy(alpha = 0.16f)
                                else -> NavySurfaceHigh
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isPicked) BoundarySky else NavyOutline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isBlocked) { selectedBowler = player.id }
                                .testTag("so_bowler_${player.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isBlocked) BroadcastMuted else BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBlocked) "BOWLED PREVIOUS SUPER OVER (LOCKED)" else "BOWL ${player.bowlingRating} • ${player.specialty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isBlocked) WicketCrimson else BroadcastSilver
                                    )
                                }
                                if (isPicked) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected Bowler", tint = BoundarySky, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { onConfirm(targetTeam.teamId, selectedBatters, selectedBowler) },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = OnEmeraldDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_super_over_lineup_button")
                ) {
                    Text(
                        text = "LOCK SUPER OVER #${match.superOverRound} LINEUP",
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
}
