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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onSubmitRetentions: (List<String>) -> Unit,
    onSkipRetention: () -> Unit,
    onHostLaunchAuction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val myTeam = room.teamForUser(currentUid)
    val isHost = room.hostUid == currentUid
    val takenByOthers = remember(room.teams, myTeam?.teamId) {
        room.teams.filter { it.teamId != myTeam?.teamId }.flatMap { it.squadPlayerIds }.toSet()
    }

    // Use franchise's former/core squad when present; fall back to available marquee pool
    val retentionPool = remember(myTeam?.formerSquadPlayerIds, takenByOthers) {
        val former = myTeam?.formerSquadPlayerIds.orEmpty()
            .filter { it !in takenByOthers }
            .mapNotNull { CricketerDatabase.getById(it) }
            .sortedByDescending { it.overallRating }
        if (former.isNotEmpty()) {
            former
        } else {
            CricketerDatabase.marqueeRetentionPool().filter { it.id !in takenByOthers }
        }
    }
    val hasFormerSquad = remember(myTeam?.formerSquadPlayerIds) {
        myTeam?.formerSquadPlayerIds?.isNotEmpty() == true
    }

    var selectedIds by remember(room.roomCode, myTeam?.teamId) {
        mutableStateOf<List<String>>(emptyList())
    }

    val totalDeductionLakhs = SquadRules.totalRetentionCost(selectedIds.size)
    val basePurse = myTeam?.budgetLakhs ?: SquadRules.INITIAL_BUDGET_LAKHS
    val projectedPurse = if (myTeam?.retentionDone == true) basePurse else (basePurse - totalDeductionLakhs).coerceAtLeast(0L)

    val retainedSummary = remember(room.teams) {
        room.teams.mapNotNull { team ->
            val ids = team.retainedPlayerIds.ifEmpty { listOfNotNull(team.retainedPlayerId.takeIf { it.isNotBlank() }) }
            val names = ids.mapNotNull { CricketerDatabase.getById(it)?.name }
            if (names.isNotEmpty()) team.teamName to names else null
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
                        text = "MEGA AUCTION RETENTIONS",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "Retain up to ${SquadRules.MAX_RETENTIONS} stars • Unretained core stays RTM-eligible",
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
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "PURSE LEFT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = BroadcastMuted
                            )
                            Text(
                                text = CurrencyFormatter.formatLakhs(projectedPurse),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    color = TurfEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tiered Retention Purse Breakdown + Status Banner
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
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        val retainedIds = myTeam.retainedPlayerIds.ifEmpty {
                                            listOfNotNull(myTeam.retainedPlayerId.takeIf { it.isNotBlank() })
                                        }
                                        if (retainedIds.isNotEmpty()) {
                                            "Retained ${retainedIds.size} star(s) • Ready for Auction"
                                        } else {
                                            "Skipped Retentions (Full ₹100 Cr Purse)"
                                        }
                                    } else {
                                        "Tiered Retention Slabs (${selectedIds.size}/${SquadRules.MAX_RETENTIONS} Selected)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BroadcastWhite
                                )
                            }

                            // 4 Tiered Slot Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SquadRules.RETENTION_SLOTS_LAKHS.forEachIndexed { idx, price ->
                                    val filled = if (myTeam?.retentionDone == true) {
                                        idx < myTeam.retainedPlayerIds.size
                                    } else {
                                        idx < selectedIds.size
                                    }
                                    val accent = if (filled) AuctionGold else BroadcastMuted
                                    Surface(
                                        color = if (filled) AuctionGold.copy(alpha = 0.16f) else NavySurfaceHigh,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, if (filled) AuctionGold else NavyOutline),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "SLOT ${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (filled) AuctionGold else BroadcastMuted
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatLakhs(price),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = MonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accent
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = BoundarySky,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "1 Right to Match (RTM) card included — match the winning bid on any unretained former squad player in the live auction!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver
                                )
                            }

                            val doneCount = room.teams.count { it.retentionDone }
                            Text(
                                text = "Franchises Ready: $doneCount / ${room.teams.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald
                            )
                        }
                    }
                }

                // Rival / AI Retentions Summary
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
                                    text = "LOCKED FRANCHISE RETENTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 1.sp,
                                        color = BoundarySky
                                    )
                                )
                                retainedSummary.forEach { (teamName, playerNames) ->
                                    Text(
                                        text = "• $teamName (${playerNames.size}): ${playerNames.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BroadcastSilver,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Selectable Core / Former Squad Header
                item {
                    Text(
                        text = if (hasFormerSquad) {
                            "YOUR FORMER / CORE SQUAD (${retentionPool.size})"
                        } else {
                            "AVAILABLE MARQUEE POOL (${retentionPool.size})"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(retentionPool, key = { it.id }) { player ->
                    val slotIdx = selectedIds.indexOf(player.id)
                    val isSelected = slotIdx >= 0
                    val nextSlotPrice = SquadRules.retentionPriceForSlot(selectedIds.size)
                    val canToggle = myTeam != null && !myTeam.retentionDone && !busy &&
                        (isSelected || (selectedIds.size < SquadRules.MAX_RETENTIONS && (projectedPurse >= nextSlotPrice)))

                    MultiRetentionPlayerCard(
                        player = player,
                        slotIndex = slotIdx,
                        nextSlotPriceLakhs = nextSlotPrice,
                        isFormerSquadPlayer = hasFormerSquad,
                        canToggle = canToggle,
                        onToggle = {
                            if (isSelected) {
                                selectedIds = selectedIds.filterNot { it == player.id }
                            } else if (selectedIds.size < SquadRules.MAX_RETENTIONS) {
                                selectedIds = selectedIds + player.id
                            }
                        }
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
                        Button(
                            onClick = { onSubmitRetentions(selectedIds) },
                            enabled = !busy && selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuctionGold,
                                contentColor = OnGoldDark,
                                disabledContainerColor = NavySurfaceHigh,
                                disabledContentColor = BroadcastMuted
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_retentions_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(20.dp), OnGoldDark, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (selectedIds.isEmpty()) {
                                        "Select 1–4 Players to Retain"
                                    } else {
                                        "Confirm ${selectedIds.size} Retention(s) • -${CurrencyFormatter.formatLakhs(totalDeductionLakhs)}"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

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
                                text = "Skip All Retentions (Keep Full ₹100 Cr + 1 RTM)",
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
private fun MultiRetentionPlayerCard(
    player: Cricketer,
    slotIndex: Int,
    nextSlotPriceLakhs: Long,
    isFormerSquadPlayer: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit
) {
    val selected = slotIndex >= 0
    val slotPrice = if (selected) SquadRules.retentionPriceForSlot(slotIndex) else nextSlotPriceLakhs

    Surface(
        color = if (selected) AuctionGold.copy(alpha = 0.12f) else NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) AuctionGold else NavyOutline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canToggle, onClick = onToggle)
            .testTag("retain_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected for retention" else "Not selected",
                tint = if (selected) AuctionGold else BroadcastMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
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
                if (!selected && isFormerSquadPlayer) {
                    Text(
                        text = "⚡ Unretained -> RTM Eligible in Auction",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BoundarySky,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                color = if (selected) AuctionGold else NavySurfaceHigh,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selected) AuctionGold else NavyOutline)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (selected) "SLOT #${slotIndex + 1}" else "RETAIN",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = if (selected) OnGoldDark else BroadcastMuted
                    )
                    Text(
                        text = CurrencyFormatter.formatLakhs(slotPrice),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) OnGoldDark else BroadcastWhite
                        )
                    )
                }
            }
        }
    }
}
