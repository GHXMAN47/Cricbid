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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuctionRoom
import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.SquadRules
import com.example.ui.components.EraTagChip
import com.example.ui.components.OverseasBadge
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

@Composable
fun RetentionScreen(
    room: AuctionRoom,
    currentUid: String,
    busy: Boolean,
    onBack: () -> Unit,
    onRetainPlayer: (String) -> Unit,
    onSkipRetention: () -> Unit,
    onHostLaunchAuction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val myTeam = room.teamForUser(currentUid)
    val isHost = room.hostUid == currentUid
    val takenPlayerIds = remember(room.teams) {
        room.teams.flatMap { it.squadPlayerIds }.toSet()
    }
    val availableMarquee = remember(takenPlayerIds) {
        CricketerDatabase.marqueeRetentionPool().filter { it.id !in takenPlayerIds }
    }
    val retainedSummary = remember(room.teams) {
        room.teams.mapNotNull { team ->
            val p = team.retainedPlayerId.takeIf { it.isNotBlank() }?.let { CricketerDatabase.getById(it) }
            if (p != null) team.teamName to p.name else null
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
                .widthIn(max = 600.dp)
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
                        .testTag("retention_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to rooms",
                        tint = BroadcastWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MARQUEE RETENTION",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "${room.teams.size} Franchises • Retain 1 Star for ₹15 Cr or Skip",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                }
                if (myTeam != null) {
                    Surface(
                        color = NavySurfaceHigh,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = CurrencyFormatter.formatLakhs(myTeam.budgetLakhs),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = MonoFontFamily,
                                color = TurfEmerald,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // My Status Banner
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (myTeam?.retentionDone == true) TurfEmerald.copy(alpha = 0.6f) else AuctionGold.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (myTeam?.retentionDone == true) Icons.Filled.CheckCircle else Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (myTeam?.retentionDone == true) TurfEmerald else AuctionGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (myTeam?.retentionDone == true) {
                                        val retained = myTeam.retainedPlayerId.takeIf { it.isNotBlank() }?.let { CricketerDatabase.getById(it)?.name }
                                        if (retained != null) "Retained $retained (₹15 Cr)" else "Skipped Retention (Full ₹100 Cr Purse)"
                                    } else {
                                        "Pick 1 Marquee Icon (₹15 Cr) before Live Bidding"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BroadcastWhite
                                )
                            }
                            Text(
                                text = "Every franchise also gets 1 Right to Match (RTM) card during the live auction.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                            val doneCount = room.teams.count { it.retentionDone }
                            Text(
                                text = "Franchises Ready: $doneCount / ${room.teams.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald
                            )
                        }
                    }
                }

                // Rival / AI Retentions Summary (compact)
                if (retainedSummary.isNotEmpty()) {
                    item {
                        Surface(
                            color = NavySurfaceHigh,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NavyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "LOCKED RETENTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 1.sp,
                                        color = BoundarySky
                                    )
                                )
                                retainedSummary.forEach { (teamName, playerName) ->
                                    Text(
                                        text = "• $teamName → $playerName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BroadcastSilver,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Available Marquee Stars
                item {
                    Text(
                        text = "AVAILABLE MARQUEE POOL (${availableMarquee.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(availableMarquee, key = { it.id }) { player ->
                    MarqueeRetentionCard(
                        player = player,
                        canSelect = myTeam != null && !myTeam.retentionDone && !busy &&
                            myTeam.budgetLakhs >= SquadRules.RETENTION_PRICE_LAKHS,
                        onRetain = { onRetainPlayer(player.id) }
                    )
                }
            }

            // Bottom Action Bar
            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (myTeam != null && !myTeam.retentionDone) {
                        OutlinedButton(
                            onClick = onSkipRetention,
                            enabled = !busy,
                            border = BorderStroke(1.dp, BroadcastSilver.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("skip_retention_button")
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, tint = BroadcastSilver)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Skip Retention (Keep Full ₹100 Cr)",
                                style = MaterialTheme.typography.labelLarge,
                                color = BroadcastWhite
                            )
                        }
                    }

                    if (isHost) {
                        Button(
                            onClick = onHostLaunchAuction,
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = OnEmeraldDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("launch_live_auction_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(20.dp), OnEmeraldDark, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Gavel, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Launch Live Auction Now",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
private fun MarqueeRetentionCard(
    player: Cricketer,
    canSelect: Boolean,
    onRetain: () -> Unit
) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyOutline),
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
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = BroadcastWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RolePillBadge(role = player.role)
                    OverseasBadge(overseas = player.overseas, countryCode = player.countryCode)
                    EraTagChip(eraTag = player.eraTag)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("BAT ${player.battingRating}", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                    Text("BOWL ${player.bowlingRating}", style = MaterialTheme.typography.labelSmall, color = WicketCrimson)
                    Text("FLD ${player.fieldingRating}", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRetain,
                enabled = canSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuctionGold,
                    contentColor = OnGoldDark,
                    disabledContainerColor = NavySurfaceHigh,
                    disabledContentColor = BroadcastMuted
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("retain_${player.id}")
            ) {
                Text(
                    text = "₹15 Cr",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
