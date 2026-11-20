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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuctionRoom
import com.example.data.model.CurrencyFormatter
import com.example.data.model.SquadRuleValidator
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
import com.example.ui.components.AuctionCountdownRing
import com.example.ui.components.EraTagChip
import com.example.ui.components.OverseasBadge
import com.example.ui.components.RatingMeterBar
import com.example.ui.components.RolePillBadge
import com.example.ui.components.roleAccentColor
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
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAuctionScreen(
    room: AuctionRoom,
    currentUid: String,
    countdownSeconds: Int,
    countdownProgress: Float,
    busy: Boolean,
    onBack: () -> Unit,
    onPlaceBid: () -> Unit,
    onUseRtm: () -> Unit,
    onHammerNow: () -> Unit,
    onFastForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showSquadsSheet by remember { mutableStateOf(false) }
    val myTeam = room.teamForUser(currentUid)
    val player = room.currentCricketer
    val isHost = room.hostUid == currentUid
    val proposedBid = room.nextBidAmountLakhs()

    val eligibility = if (myTeam != null && player != null) {
        SquadRuleValidator.checkBidEligibility(
            team = myTeam,
            player = player,
            proposedBidLakhs = proposedBid,
            currentHighestBidderTeamId = room.highestBidderTeamId
        )
    } else null

    val rtmEligibility = if (myTeam != null && player != null) {
        SquadRuleValidator.checkRtmEligibility(
            team = myTeam,
            player = player,
            currentBidLakhs = room.currentBidLakhs,
            currentHighestBidderTeamId = room.highestBidderTeamId
        )
    } else null

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
            // Broadcast Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave auction screen",
                        tint = BroadcastWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = WicketCrimson.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LIVE • ROOM ${room.roomCode}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WicketCrimson,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LOT ${(room.currentPlayerIndex + 1).coerceAtMost(room.auctionOrder.size.toLong())}/${room.auctionOrder.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                    }
                    Text(
                        text = "Sold ${room.soldCount} • Unsold ${room.unsoldCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                }
                OutlinedButton(
                    onClick = { showSquadsSheet = true },
                    border = BorderStroke(1.dp, NavyOutline),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("view_squads_button")
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Squads", style = MaterialTheme.typography.labelMedium, color = BroadcastWhite)
                }
            }

            // My Team Live Purse & Squad Compliance Strip
            if (myTeam != null) {
                MyPurseStrip(team = myTeam, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Main Scrollable Auction Stage
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (player != null) {
                    val roleAccent = roleAccentColor(player.role)

                    // Spotlight Cricketer Card
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.5.dp, roleAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            roleAccent.copy(alpha = 0.14f),
                                            NavySurface
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        RolePillBadge(role = player.role)
                                        OverseasBadge(overseas = player.overseas, countryCode = player.countryCode)
                                        EraTagChip(eraTag = player.eraTag)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = player.name.uppercase(),
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontFamily = DisplayFontFamily,
                                            fontSize = 32.sp,
                                            color = BroadcastWhite
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${player.country} • ${player.specialty}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BroadcastSilver
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Base Price: ${CurrencyFormatter.formatLakhs(player.basePriceLakhs)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = MonoFontFamily,
                                            color = AuctionGold
                                        )
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                // 10-Second Live Countdown Ring
                                AuctionCountdownRing(
                                    secondsLeft = countdownSeconds,
                                    progress = countdownProgress,
                                    size = 76.dp,
                                    modifier = Modifier.testTag("auction_countdown_ring")
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // Three Rating Meters: BAT, BOWL, FIELD
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RatingMeterBar(
                                    label = "BATTING",
                                    rating = player.battingRating,
                                    accentColor = BoundarySky,
                                    modifier = Modifier.weight(1f)
                                )
                                RatingMeterBar(
                                    label = "BOWLING",
                                    rating = player.bowlingRating,
                                    accentColor = WicketCrimson,
                                    modifier = Modifier.weight(1f)
                                )
                                RatingMeterBar(
                                    label = "FIELDING",
                                    rating = player.fieldingRating,
                                    accentColor = TurfEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Highest Bid Podium
                    val hasBid = room.highestBidderTeamId.isNotBlank() && room.currentBidLakhs > 0L
                    Surface(
                        color = if (hasBid) AuctionGold.copy(alpha = 0.14f) else NavySurfaceHigh,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (hasBid) AuctionGold.copy(alpha = 0.7f) else NavyOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasBid) "CURRENT HIGHEST BID" else "OPENING BID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasBid) AuctionGold else BroadcastMuted
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (hasBid) room.highestBidderName else "No bids yet — Be first!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BroadcastWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatLakhs(
                                    if (hasBid) room.currentBidLakhs else player.basePriceLakhs
                                ),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = MonoFontFamily,
                                    color = if (hasBid) AuctionGold else BroadcastSilver,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.testTag("current_bid_amount")
                            )
                        }
                    }

                    // Broadcast Event Ticker
                    if (room.lastEventMessage.isNotBlank()) {
                        Surface(
                            color = NavySurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NavyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = room.lastEventMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = TurfEmerald,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Host Utility Controls (Hammer Now + Auto-Complete Remaining)
                    if (isHost) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onHammerNow,
                                enabled = !busy,
                                border = BorderStroke(1.dp, NavyOutline),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hammer_now_button")
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, tint = BroadcastSilver, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hammer Lot", style = MaterialTheme.typography.labelLarge, color = BroadcastSilver)
                            }
                            OutlinedButton(
                                onClick = onFastForward,
                                enabled = !busy,
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("fast_forward_auction_button")
                            ) {
                                Icon(Icons.Filled.FastForward, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Auto-Fill Rest", style = MaterialTheme.typography.labelLarge, color = TurfEmerald)
                            }
                        }
                    }
                }
            }

            // Giant Tactile BID & RTM Button Footer
            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Right to Match (RTM) Card Action — lights up when a rival holds top bid & you have RTM card
                    if (myTeam != null && myTeam.rtmRemaining > 0 && room.highestBidderTeamId.isNotBlank() && room.highestBidderTeamId != myTeam.teamId) {
                        val canRtm = (rtmEligibility?.allowed == true) && !busy
                        OutlinedButton(
                            onClick = onUseRtm,
                            enabled = canRtm,
                            border = BorderStroke(1.5.dp, if (canRtm) BoundarySky else NavyOutline),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("use_rtm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = if (canRtm) BoundarySky else BroadcastMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (canRtm) {
                                    "USE RTM CARD • MATCH ${CurrencyFormatter.formatLakhs(room.currentBidLakhs)}"
                                } else {
                                    rtmEligibility?.reason ?: "RTM UNAVAILABLE"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 0.7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canRtm) BoundarySky else BroadcastMuted
                                )
                            )
                        }
                    }

                    val canBid = (eligibility?.allowed == true) && !busy
                    Button(
                        onClick = onPlaceBid,
                        enabled = canBid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuctionGold,
                            contentColor = OnGoldDark,
                            disabledContainerColor = NavySurfaceHigh,
                            disabledContentColor = BroadcastMuted
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("place_bid_button")
                    ) {
                        Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        val label = when {
                            eligibility == null -> "WAITING..."
                            eligibility.allowed -> "BID ${CurrencyFormatter.formatLakhs(proposedBid)}  (RESET 10s)"
                            else -> eligibility.reason ?: "BID LOCKED"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                fontSize = 20.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // All Teams & Squads Bottom Sheet
        if (showSquadsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSquadsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = NavySurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "FRANCHISE PURSES & SQUADS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = BroadcastWhite
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        modifier = Modifier.height(420.dp)
                    ) {
                        items(room.teams, key = { it.teamId }) { team ->
                            TeamSummarySheetCard(team = team)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPurseStrip(team: TeamState, modifier: Modifier = Modifier) {
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = team.teamName,
                    style = MaterialTheme.typography.labelLarge,
                    color = BroadcastWhite,
                    maxLines = 1
                )
                Text(
                    text = CurrencyFormatter.formatLakhs(team.budgetLakhs),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MonoFontFamily,
                        color = TurfEmerald,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ComplianceChip(
                    label = "SQUAD",
                    value = "${team.squadPlayerIds.size}/${SquadRules.MAX_SQUAD_SIZE}",
                    ok = team.squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE
                )
                ComplianceChip(
                    label = "OS",
                    value = "${team.overseasCount}/${SquadRules.MAX_OVERSEAS_IN_SQUAD}",
                    ok = team.overseasCount <= SquadRules.MAX_OVERSEAS_IN_SQUAD
                )
                ComplianceChip(
                    label = "WK",
                    value = "${team.wkCount}/1+",
                    ok = team.wkCount >= SquadRules.MIN_WICKETKEEPERS
                )
                ComplianceChip(
                    label = "RTM",
                    value = if (team.rtmRemaining > 0) "${team.rtmRemaining}" else "0",
                    ok = team.rtmRemaining > 0
                )
            }
        }
    }
}

@Composable
private fun ComplianceChip(label: String, value: String, ok: Boolean) {
    val accent = if (ok) TurfEmerald else AuctionGold
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BroadcastMuted)
            Text(
                value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
        }
    }
}

@Composable
private fun TeamSummarySheetCard(team: TeamState) {
    Surface(
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(14.dp),
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
                    text = if (team.bot) "${team.teamName} (AI)" else team.teamName,
                    style = MaterialTheme.typography.titleMedium,
                    color = BroadcastWhite
                )
                Text(
                    text = CurrencyFormatter.formatLakhs(team.budgetLakhs),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = MonoFontFamily,
                        color = TurfEmerald
                    )
                )
            }
            Text(
                text = "Players: ${team.squadPlayerIds.size}/${SquadRules.MAX_SQUAD_SIZE}  •  OS: ${team.overseasCount}/${SquadRules.MAX_OVERSEAS_IN_SQUAD} (4 XI)  •  WK: ${team.wkCount}  •  RTM: ${team.rtmRemaining}",
                style = MaterialTheme.typography.labelSmall,
                color = BroadcastMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (team.players.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = team.players.joinToString(", ") { "${it.name} (${it.role.shortLabel})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = BroadcastSilver
                )
            }
        }
    }
}
