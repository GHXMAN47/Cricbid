package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Ipl2026Catalog
import com.example.data.model.IplFranchiseState
import com.example.data.model.IplRules
import com.example.data.model.IplSeasonState
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import com.example.engine.IplSeasonEngine
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
 * Step 1 of IPL Season: Official Pre-Auction Retention & RTM Selection.
 * Max 6 total (Direct Retentions + RTM Cards), max 5 capped, max 2 uncapped Indian.
 */
@Composable
fun IplRetentionScreen(
    season: IplSeasonState,
    onSelectActiveHuman: (String) -> Unit,
    onSubmitRetentions: (franchiseId: String, selectedIds: List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val activeFranchise = season.activeHumanFranchise ?: return
    val humanFranchises = remember(season.franchises) { season.franchises.filter { it.isHuman } }
    val formerPlayers = remember(activeFranchise.franchiseId, activeFranchise.formerSquadIds) {
        activeFranchise.formerSquadIds.mapNotNull { CricketerDatabase.getById(it) }
    }

    var selectedIds by remember(activeFranchise.franchiseId) {
        mutableStateOf(activeFranchise.retainedPlayerIds.ifEmpty {
            formerPlayers.take(3).map { it.id }
        })
    }

    val cappedSelected = selectedIds.count { !Ipl2026Catalog.isUncappedIndian(it) }
    val uncappedSelected = selectedIds.count { Ipl2026Catalog.isUncappedIndian(it) }
    val rtmRemaining = (IplRules.MAX_RETENTION_PLUS_RTM - selectedIds.size).coerceAtLeast(0)
    val retentionCost = IplRules.calculateRetentionCostLakhs(selectedIds)
    val remainingPurse = (IplRules.PURSE_LAKHS - retentionCost).coerceAtLeast(0L)
    val validationError = IplSeasonEngine.validateRetentionSelection(activeFranchise, selectedIds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Top bar
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
                        .testTag("ipl_retention_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IPL ${season.seasonYear} RETENTIONS & RTM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "${activeFranchise.teamName} • ₹120 Cr Purse • Max 6 (≤5 Capped, ≤2 Uncapped IND)",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (humanFranchises.size > 1) {
                HumanFranchiseSwitcherBar(
                    humanFranchises = humanFranchises,
                    activeFranchiseId = activeFranchise.franchiseId,
                    onSelect = onSelectActiveHuman,
                    doneBadge = { it.retentionDone }
                )
            }

            // Purse & RTM summary card
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, Color(activeFranchise.primaryColorHex).copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PurseStatPill("RETAINED", "${selectedIds.size}/6", TurfEmerald)
                        PurseStatPill("RTM CARDS", "$rtmRemaining", AuctionGold)
                        PurseStatPill("CAPPED", "$cappedSelected/5", BoundarySky)
                        PurseStatPill("UNCAPPED IND", "$uncappedSelected/2", Color(0xFFA855F7))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deduction: ${CurrencyFormatter.formatLakhs(retentionCost)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = WicketCrimson
                        )
                        Text(
                            text = "Auction Purse Left: ${CurrencyFormatter.formatLakhs(remainingPurse)}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = TurfEmerald
                            )
                        )
                    }
                    Text(
                        text = "Official Slabs: Capped ₹18 / ₹14 / ₹11 / ₹18 / ₹14 Cr • Uncapped Indian ₹4 Cr. Unfilled slots become Auction RTM Cards!",
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastMuted
                    )
                    if (validationError != null) {
                        Text(
                            text = validationError,
                            style = MaterialTheme.typography.labelSmall.copy(color = WicketCrimson, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formerPlayers, key = { it.id }) { player ->
                    val isSelected = player.id in selectedIds
                    val isUncappedInd = Ipl2026Catalog.isUncappedIndian(player.id)
                    val slabPrice = if (isSelected) {
                        if (isUncappedInd) IplRules.UNCAPPED_INDIAN_RETENTION_LAKHS
                        else {
                            val cappedIdx = selectedIds.filter { !Ipl2026Catalog.isUncappedIndian(it) }.indexOf(player.id)
                            IplRules.cappedRetentionPrice(cappedIdx)
                        }
                    } else null

                    Surface(
                        color = if (isSelected) TurfEmerald.copy(alpha = 0.14f) else NavySurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) TurfEmerald else NavyOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !activeFranchise.retentionDone) {
                                selectedIds = if (isSelected) {
                                    selectedIds - player.id
                                } else {
                                    val candidate = selectedIds + player.id
                                    if (IplSeasonEngine.validateRetentionSelection(activeFranchise, candidate) == null) {
                                        candidate
                                    } else selectedIds
                                }
                            }
                            .testTag("ipl_retain_player_${player.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (player.overseas) {
                                        MiniTag("✈ OS", BoundarySky)
                                    }
                                    if (isUncappedInd) {
                                        MiniTag("UNCAPPED ₹4Cr", Color(0xFFA855F7))
                                    }
                                    if (player.id == activeFranchise.captainPlayerId) {
                                        MiniTag("CAPTAIN", AuctionGold)
                                    }
                                }
                                Text(
                                    text = "${player.role.shortLabel} • BAT ${player.battingRating} • BOWL ${player.bowlingRating} • ${player.specialty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (slabPrice != null) {
                                Text(
                                    text = CurrencyFormatter.formatLakhs(slabPrice),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = MonoFontFamily,
                                        color = AuctionGold,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Retained", tint = TurfEmerald, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Bottom confirm CTA
            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedIds = emptyList() },
                        enabled = !activeFranchise.retentionDone && selectedIds.isNotEmpty(),
                        border = BorderStroke(1.dp, NavyOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("ipl_clear_retentions_button")
                    ) {
                        Text("0 Retain (6 RTM)", style = MaterialTheme.typography.labelMedium, color = BroadcastSilver)
                    }
                    Button(
                        onClick = { onSubmitRetentions(activeFranchise.franchiseId, selectedIds) },
                        enabled = !activeFranchise.retentionDone && validationError == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfEmerald,
                            contentColor = OnEmeraldDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ipl_lock_retentions_button")
                    ) {
                        Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (activeFranchise.retentionDone) "LOCKED • PICK NEXT TEAM" else "LOCK & ENTER MEGA AUCTION",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 2 of IPL Season: Live IPL 2026 Mega Auction Arena.
 * Features ₹120 Cr purses, 18–25 squad tracker, RTM Card trigger, and Hammer Loot capped strictly at 2/2 per team!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplLiveAuctionScreen(
    season: IplSeasonState,
    countdownSeconds: Int,
    countdownProgress: Float,
    paused: Boolean,
    onTogglePause: () -> Unit,
    onSelectActiveHuman: (String) -> Unit,
    onPlaceBid: (String) -> Unit,
    onTriggerHammerLoot: (String) -> Unit,
    onUseRtm: (String) -> Unit,
    onPass: (String) -> Unit,
    onFastForwardLot: () -> Unit,
    onAutoComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val activeFranchise = season.activeHumanFranchise ?: return
    val humanFranchises = remember(season.franchises) { season.franchises.filter { it.isHuman } }
    val auction = season.auction
    val currentPlayer = auction.currentPlayer
    var showSquadsSheet by remember { mutableStateOf(false) }

    val nextBid = auction.nextBidLakhs()
    val canBid = currentPlayer != null &&
        auction.highestBidderFranchiseId != activeFranchise.franchiseId &&
        IplSeasonEngine.canFranchiseBid(activeFranchise, currentPlayer, nextBid) == null

    val hammerLeft = activeFranchise.hammerLootRemaining.coerceIn(0, IplRules.MAX_HAMMER_LOOT_PER_TEAM)
    val canHammerLoot = currentPlayer != null &&
        hammerLeft > 0 &&
        IplSeasonEngine.canFranchiseBid(activeFranchise, currentPlayer, nextBid) == null

    val canRtm = currentPlayer != null &&
        auction.highestBidderFranchiseId.isNotBlank() &&
        auction.highestBidderFranchiseId != activeFranchise.franchiseId &&
        activeFranchise.canUseRtmOnPlayer(currentPlayer.id) &&
        IplSeasonEngine.canFranchiseBid(activeFranchise, currentPlayer, auction.currentBidLakhs) == null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyMidnight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
        ) {
            // Header
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
                        .testTag("ipl_auction_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IPL ${season.seasonYear} MEGA AUCTION",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "Lot ${auction.currentIndex + 1}/${auction.auctionOrder.size} • Sold ${auction.soldCount} • Unsold ${auction.unsoldCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = BroadcastSilver
                    )
                }
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ipl_auction_pause_button")
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (paused) "Resume Auction" else "Pause Auction",
                        tint = AuctionGold
                    )
                }
                IconButton(
                    onClick = { showSquadsSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ipl_auction_squads_button")
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = "View All 10 Squads", tint = BoundarySky)
                }
            }

            if (humanFranchises.size > 1) {
                HumanFranchiseSwitcherBar(
                    humanFranchises = humanFranchises,
                    activeFranchiseId = activeFranchise.franchiseId,
                    onSelect = onSelectActiveHuman,
                    doneBadge = { it.squadPlayerIds.size >= IplRules.MIN_SQUAD_SIZE }
                )
            }

            // Active Franchise Telemetry Strip
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, Color(activeFranchise.primaryColorHex).copy(alpha = 0.75f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PurseStatPill(activeFranchise.shortCode + " PURSE", CurrencyFormatter.formatLakhs(activeFranchise.purseRemainingLakhs), TurfEmerald)
                    PurseStatPill("SQUAD", "${activeFranchise.squadPlayerIds.size}/${IplRules.MAX_SQUAD_SIZE}", BroadcastWhite)
                    PurseStatPill("OS", "${activeFranchise.overseasCount}/${IplRules.MAX_OVERSEAS_IN_SQUAD}", BoundarySky)
                    PurseStatPill("RTM", "${activeFranchise.rtmCardsRemaining}", AuctionGold)
                    PurseStatPill(
                        "HAMMER",
                        "$hammerLeft/${IplRules.MAX_HAMMER_LOOT_PER_TEAM}",
                        if (hammerLeft > 0) AuctionGold else WicketCrimson
                    )
                }
            }

            // Countdown bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    progress = { countdownProgress.coerceIn(0f, 1f) },
                    color = if (countdownSeconds <= 3) WicketCrimson else TurfEmerald,
                    trackColor = NavySurfaceHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = auction.lastEventMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (paused) "PAUSED" else "${countdownSeconds}s",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (countdownSeconds <= 3) WicketCrimson else AuctionGold
                        )
                    )
                }
            }

            // Current Player Spotlight Card
            if (currentPlayer != null) {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, AuctionGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ipl_current_lot_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniTag(currentPlayer.role.shortLabel, TurfEmerald)
                                MiniTag(if (currentPlayer.overseas) "✈ ${currentPlayer.countryCode}" else "🇮🇳 IND", BoundarySky)
                                if (Ipl2026Catalog.isUncappedIndian(currentPlayer.id)) {
                                    MiniTag("UNCAPPED", Color(0xFFA855F7))
                                }
                                if (Ipl2026Catalog.isEmergingEligible(currentPlayer.id)) {
                                    MiniTag("EMERGING", AuctionGold)
                                }
                            }
                            Text(
                                text = "BASE ${CurrencyFormatter.formatLakhs(currentPlayer.basePriceLakhs)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = MonoFontFamily,
                                    color = BroadcastSilver
                                )
                            )
                        }

                        Text(
                            text = currentPlayer.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DisplayFontFamily,
                                color = BroadcastWhite
                            )
                        )
                        Text(
                            text = "${currentPlayer.specialty} • ${currentPlayer.eraTag}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox("BAT", "${currentPlayer.battingRating}", TurfEmerald, Modifier.weight(1f))
                            StatBox("BOWL", "${currentPlayer.bowlingRating}", BoundarySky, Modifier.weight(1f))
                            StatBox("FLD", "${currentPlayer.fieldingRating}", AuctionGold, Modifier.weight(1f))
                            StatBox("OVR", "${currentPlayer.overallRating}", BroadcastWhite, Modifier.weight(1f))
                        }

                        Surface(
                            color = NavySurfaceHigh,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NavyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CURRENT BID", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                                    Text(
                                        text = if (auction.highestBidderFranchiseId.isBlank()) {
                                            "Opening Bid"
                                        } else {
                                            CurrencyFormatter.formatLakhs(auction.currentBidLakhs)
                                        },
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = MonoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AuctionGold
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("LEADING FRANCHISE", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                                    Text(
                                        text = auction.highestBidderName.ifBlank { "No bids yet" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (auction.highestBidderFranchiseId == activeFranchise.franchiseId) TurfEmerald else BroadcastWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Primary Auction Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlaceBid(activeFranchise.franchiseId) },
                    enabled = canBid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = OnEmeraldDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("ipl_place_bid_button")
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (auction.highestBidderFranchiseId == activeFranchise.franchiseId) {
                            "YOU HOLD HIGHEST BID"
                        } else {
                            "BID ${CurrencyFormatter.formatLakhs(nextBid)} (${activeFranchise.shortCode})"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hammer Loot strictly capped at 2 per team per auction!
                    Button(
                        onClick = { onTriggerHammerLoot(activeFranchise.franchiseId) },
                        enabled = canHammerLoot,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuctionGold,
                            contentColor = OnGoldDark,
                            disabledContainerColor = NavySurfaceHigh,
                            disabledContentColor = BroadcastMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.25f)
                            .height(50.dp)
                            .testTag("ipl_hammer_loot_button")
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (hammerLeft > 0) "HAMMER LOOT ($hammerLeft/2)" else "HAMMER LOOT (0/2)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { onUseRtm(activeFranchise.franchiseId) },
                        enabled = canRtm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BoundarySky,
                            contentColor = NavyMidnight,
                            disabledContainerColor = NavySurfaceHigh,
                            disabledContentColor = BroadcastMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ipl_rtm_button")
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "RTM (${activeFranchise.rtmCardsRemaining})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPass(activeFranchise.franchiseId) },
                        border = BorderStroke(1.dp, NavyOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ipl_pass_lot_button")
                    ) {
                        Text("Pass Lot", style = MaterialTheme.typography.labelMedium, color = BroadcastSilver)
                    }
                    OutlinedButton(
                        onClick = onFastForwardLot,
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ipl_next_lot_button")
                    ) {
                        Text("Hammer Now", style = MaterialTheme.typography.labelMedium, color = BoundarySky)
                    }
                    OutlinedButton(
                        onClick = onAutoComplete,
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("ipl_auto_complete_auction_button")
                    ) {
                        Icon(Icons.Filled.FastForward, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto-Draft Rest", style = MaterialTheme.typography.labelMedium, color = AuctionGold, maxLines = 1)
                    }
                }
            }
        }
    }

    if (showSquadsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSquadsSheet = false },
            containerColor = NavySurface
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "ALL 10 IPL FRANCHISE PURSES & SQUADS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = AuctionGold
                        )
                    )
                }
                items(season.franchises, key = { it.franchiseId }) { f ->
                    Surface(
                        color = NavySurfaceHigh,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(f.primaryColorHex).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${f.shortCode} • ${f.teamName}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BroadcastWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${f.squadPlayerIds.size} Players (OS ${f.overseasCount}/8, WK ${f.wkCount}) • Hammer ${f.hammerLootRemaining}/2 • RTM ${f.rtmCardsRemaining}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BroadcastSilver
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatLakhs(f.purseRemainingLakhs),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TurfEmerald
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 3 of IPL Season (also accessible mid-season): Pick Playing XI (max 4 overseas, ≥1 WK), Captain,
 * and nominate 5 Impact Player Substitutes from the bench.
 */
@Composable
fun IplPlayingXiScreen(
    season: IplSeasonState,
    isSeasonEdit: Boolean,
    onSelectActiveHuman: (String) -> Unit,
    onSaveXi: (franchiseId: String, xiIds: List<String>, impactNomineeIds: List<String>, captainId: String, enterLeague: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val activeFranchise = season.activeHumanFranchise ?: return
    val humanFranchises = remember(season.franchises) { season.franchises.filter { it.isHuman } }
    val squadPlayers = remember(activeFranchise.franchiseId, activeFranchise.squadPlayerIds) {
        activeFranchise.players.sortedByDescending { it.overallRating }
    }

    var selectedXi by remember(activeFranchise.franchiseId) {
        mutableStateOf(
            activeFranchise.playingXiIds.takeIf { it.size == 11 }
                ?: SquadRuleValidator.autoPickBestPlayingXi(activeFranchise.squadPlayerIds)
        )
    }
    var captainId by remember(activeFranchise.franchiseId) {
        mutableStateOf(
            activeFranchise.captainPlayerId.takeIf { it in selectedXi }
                ?: SquadRuleValidator.autoPickCaptain(selectedXi)
        )
    }
    var selectedImpactNominees by remember(activeFranchise.franchiseId) {
        mutableStateOf(
            activeFranchise.impactNomineeIds.filter { it !in selectedXi && it in activeFranchise.squadPlayerIds }
                .take(IplRules.IMPACT_NOMINEES_COUNT)
                .ifEmpty {
                    activeFranchise.squadPlayerIds.filter { it !in selectedXi }.take(IplRules.IMPACT_NOMINEES_COUNT)
                }
        )
    }

    val xiPlayers = selectedXi.mapNotNull { CricketerDatabase.getById(it) }
    val xiOverseas = xiPlayers.count { it.overseas }
    val xiWks = xiPlayers.count { it.role == PlayerRole.WICKETKEEPER }
    val xiValidation = SquadRuleValidator.validatePlayingXi(
        selectedIds = selectedXi,
        ownedIds = activeFranchise.squadPlayerIds,
        captainPlayerId = captainId
    )

    Box(
        modifier = modifier
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
                        .testTag("ipl_xi_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BroadcastWhite)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${activeFranchise.shortCode} PLAYING XI & 5 IMPACT SUBS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = AuctionGold
                        )
                    )
                    Text(
                        text = "Pick 11 Starters (Max 4 OS, ≥1 WK), Captain, & 5 Impact Substitutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver
                    )
                }
            }

            if (humanFranchises.size > 1) {
                HumanFranchiseSwitcherBar(
                    humanFranchises = humanFranchises,
                    activeFranchiseId = activeFranchise.franchiseId,
                    onSelect = onSelectActiveHuman,
                    doneBadge = { it.playingXiIds.size == 11 }
                )
            }

            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (xiValidation.allowed) TurfEmerald.copy(alpha = 0.7f) else WicketCrimson.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        PurseStatPill("XI", "${selectedXi.size}/11", if (selectedXi.size == 11) TurfEmerald else WicketCrimson)
                        PurseStatPill("OVERSEAS", "$xiOverseas/4", if (xiOverseas <= 4) BoundarySky else WicketCrimson)
                        PurseStatPill("WK", "$xiWks", if (xiWks >= 1) TurfEmerald else WicketCrimson)
                        PurseStatPill("IMPACT SUBS", "${selectedImpactNominees.size}/5", AuctionGold)
                    }
                    Text(
                        text = if (xiOverseas >= 4) {
                            "4 Overseas in Starting XI → Only an Indian Impact Sub can enter during the match."
                        } else {
                            "$xiOverseas Overseas in Starting XI → Either an Indian or Overseas Impact Sub can enter."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver
                    )
                    if (!xiValidation.allowed && xiValidation.reason != null) {
                        Text(
                            text = xiValidation.reason,
                            style = MaterialTheme.typography.labelSmall.copy(color = WicketCrimson, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(squadPlayers, key = { it.id }) { player ->
                    val inXi = player.id in selectedXi
                    val isImpactSub = player.id in selectedImpactNominees
                    val isCaptain = player.id == captainId
                    val borderColor = when {
                        inXi -> TurfEmerald
                        isImpactSub -> AuctionGold
                        else -> NavyOutline
                    }

                    Surface(
                        color = when {
                            inXi -> TurfEmerald.copy(alpha = 0.12f)
                            isImpactSub -> AuctionGold.copy(alpha = 0.12f)
                            else -> NavySurface
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (inXi) {
                                    selectedXi = selectedXi - player.id
                                    if (captainId == player.id) {
                                        captainId = selectedXi.firstOrNull().orEmpty()
                                    }
                                } else if (selectedXi.size < 11) {
                                    selectedXi = selectedXi + player.id
                                    selectedImpactNominees = selectedImpactNominees - player.id
                                    if (captainId.isBlank()) captainId = player.id
                                }
                            }
                            .testTag("ipl_xi_player_${player.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MiniTag(player.role.shortLabel, BroadcastSilver)
                                    if (player.overseas) MiniTag("✈ OS", BoundarySky)
                                }
                                Text(
                                    text = "BAT ${player.battingRating} • BOWL ${player.bowlingRating} • OVR ${player.overallRating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BroadcastMuted
                                )
                            }
                            if (inXi) {
                                Surface(
                                    color = if (isCaptain) AuctionGold else NavySurfaceHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { captainId = player.id }
                                        .testTag("ipl_cap_${player.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Set Captain",
                                            tint = if (isCaptain) OnGoldDark else AuctionGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (isCaptain) "CAP" else "C",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isCaptain) OnGoldDark else AuctionGold
                                            )
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = if (isImpactSub) AuctionGold else NavySurfaceHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedImpactNominees = if (isImpactSub) {
                                                selectedImpactNominees - player.id
                                            } else if (selectedImpactNominees.size < IplRules.IMPACT_NOMINEES_COUNT) {
                                                selectedImpactNominees + player.id
                                            } else {
                                                selectedImpactNominees.drop(1) + player.id
                                            }
                                        }
                                        .testTag("ipl_impact_nom_${player.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.SwapHoriz,
                                            contentDescription = "Nominate Impact Sub",
                                            tint = if (isImpactSub) OnGoldDark else AuctionGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (isImpactSub) "IMPACT" else "SUB",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isImpactSub) OnGoldDark else AuctionGold
                                            )
                                        )
                                    }
                                }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val autoXi = SquadRuleValidator.autoPickBestPlayingXi(activeFranchise.squadPlayerIds)
                            selectedXi = autoXi
                            captainId = SquadRuleValidator.autoPickCaptain(autoXi)
                            selectedImpactNominees = activeFranchise.squadPlayerIds.filter { it !in autoXi }.take(5)
                        },
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("ipl_auto_pick_xi_button")
                    ) {
                        Text("Auto Best XI", style = MaterialTheme.typography.labelMedium, color = BoundarySky)
                    }
                    Button(
                        onClick = {
                            val finalNominees = if (selectedImpactNominees.size >= IplRules.IMPACT_NOMINEES_COUNT) {
                                selectedImpactNominees.take(IplRules.IMPACT_NOMINEES_COUNT)
                            } else {
                                (selectedImpactNominees + activeFranchise.squadPlayerIds.filter { it !in selectedXi && it !in selectedImpactNominees })
                                    .take(IplRules.IMPACT_NOMINEES_COUNT)
                            }
                            onSaveXi(activeFranchise.franchiseId, selectedXi, finalNominees, captainId, !isSeasonEdit)
                        },
                        enabled = xiValidation.allowed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfEmerald,
                            contentColor = OnEmeraldDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ipl_save_xi_button")
                    ) {
                        Text(
                            text = if (isSeasonEdit) "SAVE XI & IMPACT SUBS" else "START 70-MATCH IPL SEASON",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HumanFranchiseSwitcherBar(
    humanFranchises: List<IplFranchiseState>,
    activeFranchiseId: String,
    onSelect: (String) -> Unit,
    doneBadge: (IplFranchiseState) -> Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        humanFranchises.forEach { f ->
            val selected = f.franchiseId == activeFranchiseId
            val accent = Color(f.primaryColorHex)
            Surface(
                color = if (selected) accent.copy(alpha = 0.24f) else NavySurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, if (selected) accent else NavyOutline),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(f.franchiseId) }
                    .testTag("ipl_switch_human_${f.shortCode}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "${f.shortCode} (${f.ownerName})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selected) BroadcastWhite else BroadcastSilver
                        )
                    )
                    if (doneBadge(f)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = TurfEmerald, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PurseStatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BroadcastMuted)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        )
    }
}

@Composable
internal fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = BroadcastMuted)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
        }
    }
}

@Composable
internal fun MiniTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
