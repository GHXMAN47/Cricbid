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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AuctionRoom
import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import com.example.data.model.SquadRules
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
fun SquadSelectionScreen(
    room: AuctionRoom,
    currentUid: String,
    busy: Boolean,
    onBack: () -> Unit,
    onSavePlayingXi: (List<String>, String) -> Unit,
    onHostEnterMatchHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val myTeam = room.teamForUser(currentUid)
    val isHost = room.hostUid == currentUid
    val squadPlayers = remember(myTeam?.squadPlayerIds) {
        myTeam?.players?.sortedByDescending { it.overallRating } ?: emptyList()
    }

    var selectedIds by remember(myTeam?.teamId) {
        mutableStateOf(
            when {
                myTeam == null -> emptyList()
                myTeam.playingXiIds.size == SquadRules.MIN_SQUAD_SIZE -> myTeam.playingXiIds
                else -> SquadRuleValidator.autoPickBestPlayingXi(myTeam.squadPlayerIds)
            }
        )
    }

    var captainId by remember(myTeam?.teamId) {
        mutableStateOf(
            myTeam?.effectiveCaptainId?.takeIf { it.isNotBlank() }
                ?: SquadRuleValidator.autoPickCaptain(selectedIds)
        )
    }

    // 0 = Pick XI from full squad (up to 15 / 8 OS), 1 = Batting Order #1..#11 & Captain (C)
    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    var swapSourceIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(myTeam?.squadPlayerIds) {
        if (selectedIds.isEmpty() && myTeam != null && myTeam.squadPlayerIds.isNotEmpty()) {
            val auto = SquadRuleValidator.autoPickBestPlayingXi(myTeam.squadPlayerIds)
            selectedIds = auto
            if (captainId !in auto) {
                captainId = SquadRuleValidator.autoPickCaptain(auto)
            }
        }
    }

    // Keep captain always valid within current selected XI
    LaunchedEffect(selectedIds) {
        if (selectedIds.isNotEmpty() && captainId !in selectedIds) {
            captainId = SquadRuleValidator.autoPickCaptain(selectedIds)
        } else if (selectedIds.isEmpty()) {
            captainId = ""
        }
        swapSourceIndex = null
    }

    val selectedPlayers = remember(selectedIds) {
        selectedIds.mapNotNull { CricketerDatabase.getById(it) }
    }
    val selectedOverseas = selectedPlayers.count { it.overseas }
    val selectedWks = selectedPlayers.count { it.role == PlayerRole.WICKETKEEPER }
    val effectiveCaptainId = captainId.takeIf { it in selectedIds } ?: SquadRuleValidator.autoPickCaptain(selectedIds)
    val captainPlayer = CricketerDatabase.getById(effectiveCaptainId)

    val validation = if (myTeam != null) {
        SquadRuleValidator.validatePlayingXi(
            selectedIds = selectedIds,
            ownedIds = myTeam.squadPlayerIds,
            captainPlayerId = effectiveCaptainId
        )
    } else null

    val overseasViolation = selectedOverseas > SquadRules.MAX_OVERSEAS_IN_XI

    fun movePlayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in selectedIds.indices || toIndex !in selectedIds.indices || fromIndex == toIndex) return
        val mutable = selectedIds.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        selectedIds = mutable
        swapSourceIndex = null
    }

    fun swapPositions(first: Int, second: Int) {
        if (first !in selectedIds.indices || second !in selectedIds.indices || first == second) {
            swapSourceIndex = null
            return
        }
        val mutable = selectedIds.toMutableList()
        val tmp = mutable[first]
        mutable[first] = mutable[second]
        mutable[second] = tmp
        selectedIds = mutable
        swapSourceIndex = null
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BroadcastWhite
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYING XI & ORDER",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = BroadcastWhite
                        )
                    )
                    Text(
                        text = "${myTeam?.teamName ?: "Squad"} • Max 4 OS in XI (8 in Squad)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TurfEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (myTeam != null) {
                    OutlinedButton(
                        onClick = {
                            val best = SquadRuleValidator.autoPickBestPlayingXi(myTeam.squadPlayerIds)
                            selectedIds = best
                            if (captainId !in best) {
                                captainId = SquadRuleValidator.autoPickCaptain(best)
                            }
                            swapSourceIndex = null
                        },
                        border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("auto_pick_xi_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Best XI", style = MaterialTheme.typography.labelMedium, color = AuctionGold)
                    }
                }
            }

            // Live Validation Bar
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.2.dp,
                    when {
                        overseasViolation -> WicketCrimson
                        validation?.allowed == true -> TurfEmerald.copy(alpha = 0.6f)
                        else -> AuctionGold.copy(alpha = 0.5f)
                    }
                ),
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
                    RuleStat("XI", "${selectedIds.size}/11", selectedIds.size == SquadRules.MIN_SQUAD_SIZE)
                    RuleStat("OVERSEAS", "$selectedOverseas/4 max", !overseasViolation)
                    RuleStat("WK", "$selectedWks/1+", selectedWks >= SquadRules.MIN_WICKETKEEPERS)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CAPTAIN (C)", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                        Text(
                            text = captainPlayer?.name?.substringAfterLast(" ") ?: "Select",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AuctionGold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Two-Tab Switcher: Select XI vs Batting Order #1-#11 & Captain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NavySurfaceHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SelectionTabButton(
                    label = "1. Pick XI (${squadPlayers.size})",
                    selected = activeTab == 0,
                    onClick = { activeTab = 0; swapSourceIndex = null },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_pick_xi")
                )
                SelectionTabButton(
                    label = "2. Order 1–11 & C",
                    selected = activeTab == 1,
                    onClick = { activeTab = 1; swapSourceIndex = null },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_batting_order")
                )
            }

            // Secondary Status / Helper Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeTab == 0) {
                    val readyCount = room.teams.count { it.xiReady }
                    val benchCount = (squadPlayers.size - selectedIds.size).coerceAtLeast(0)
                    Text(
                        text = "Ready: $readyCount/${room.teams.size} • Squad OS: ${myTeam?.overseasCount ?: 0}/8",
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver
                    )
                    Text(
                        text = "Impact Bench: $benchCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = BoundarySky
                    )
                } else {
                    Text(
                        text = if (swapSourceIndex != null) {
                            "Tap any slot badge to swap with #${swapSourceIndex!! + 1}"
                        } else {
                            "Use ▲/▼ or tap #slot to swap • Tap C for Captain"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (swapSourceIndex != null) AuctionGold else BroadcastSilver,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = NavySurfaceHigh,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, NavyOutline),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val autoOrdered = SquadRuleValidator.orderBattingLineup(selectedPlayers).map { it.id }
                                selectedIds = autoOrdered
                                swapSourceIndex = null
                            }
                            .testTag("auto_order_lineup_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.SwapVert, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Auto", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                        }
                    }
                }
            }

            // Main Content List
            if (activeTab == 0) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(squadPlayers, key = { it.id }) { player ->
                        val orderIdx = selectedIds.indexOf(player.id)
                        val isSelected = orderIdx >= 0
                        val isCaptain = player.id == effectiveCaptainId
                        SquadPlayerSelectableCard(
                            player = player,
                            selected = isSelected,
                            battingPosition = if (isSelected) orderIdx + 1 else null,
                            isCaptain = isCaptain,
                            onToggle = {
                                selectedIds = if (isSelected) {
                                    selectedIds - player.id
                                } else if (selectedIds.size < SquadRules.MIN_SQUAD_SIZE) {
                                    selectedIds + player.id
                                } else {
                                    selectedIds
                                }
                            },
                            onSetCaptain = {
                                if (isSelected) {
                                    captainId = player.id
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedPlayers.isEmpty()) {
                        item {
                            Surface(
                                color = NavySurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NavyOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Select 11 players in Tab 1 to set your batting order and Captain.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BroadcastSilver,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        itemsIndexed(selectedPlayers, key = { _, p -> p.id }) { index, player ->
                            val isCaptain = player.id == effectiveCaptainId
                            val isSwapSource = swapSourceIndex == index
                            BattingOrderRowCard(
                                position = index + 1,
                                player = player,
                                isCaptain = isCaptain,
                                isSwapSource = isSwapSource,
                                canMoveUp = index > 0,
                                canMoveDown = index < selectedPlayers.lastIndex,
                                onTapPositionSlot = {
                                    val src = swapSourceIndex
                                    if (src == null) {
                                        swapSourceIndex = index
                                    } else if (src == index) {
                                        swapSourceIndex = null
                                    } else {
                                        swapPositions(src, index)
                                    }
                                },
                                onMoveUp = { movePlayer(index, index - 1) },
                                onMoveDown = { movePlayer(index, index + 1) },
                                onMakeCaptain = { captainId = player.id }
                            )
                        }
                    }
                }
            }

            // Bottom Lock & Enter Match Hub Footer
            Surface(
                color = NavySurface,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (validation != null && !validation.allowed) {
                        Surface(
                            color = if (overseasViolation) WicketCrimson.copy(alpha = 0.16f) else AuctionGold.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (overseasViolation) WicketCrimson.copy(alpha = 0.7f) else AuctionGold.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("xi_validation_error_banner")
                        ) {
                            Text(
                                text = validation.reason ?: "Adjust selection to meet Playing XI rules",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (overseasViolation) WicketCrimson else AuctionGold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { onSavePlayingXi(selectedIds, effectiveCaptainId) },
                        enabled = (validation?.allowed == true) && !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurfEmerald,
                            contentColor = OnEmeraldDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("lock_playing_xi_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(20.dp), OnEmeraldDark, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.SportsCricket, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.lock_playing_xi),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (isHost && myTeam?.xiReady == true) {
                        OutlinedButton(
                            onClick = onHostEnterMatchHub,
                            enabled = !busy,
                            border = BorderStroke(1.dp, AuctionGold),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enter_match_hub_button")
                        ) {
                            Text(
                                text = "Open T20 Match Hub Now",
                                style = MaterialTheme.typography.labelLarge,
                                color = AuctionGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) TurfEmerald else NavySurfaceHigh,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) OnEmeraldDark else BroadcastSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RuleStat(label: String, value: String, valid: Boolean) {
    val color = if (valid) TurfEmerald else WicketCrimson
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = MonoFontFamily,
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SquadPlayerSelectableCard(
    player: Cricketer,
    selected: Boolean,
    battingPosition: Int?,
    isCaptain: Boolean,
    onToggle: () -> Unit,
    onSetCaptain: () -> Unit
) {
    Surface(
        color = if (selected) TurfEmerald.copy(alpha = 0.13f) else NavySurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = when {
                isCaptain -> AuctionGold.copy(alpha = 0.85f)
                selected -> TurfEmerald.copy(alpha = 0.7f)
                else -> NavyOutline
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("select_player_${player.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (battingPosition != null) {
                Surface(
                    color = TurfEmerald.copy(alpha = 0.22f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, TurfEmerald),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$battingPosition",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = TurfEmerald
                            )
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Not selected",
                    tint = BroadcastMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BroadcastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    RolePillBadge(role = player.role)
                    OverseasBadge(overseas = player.overseas, countryCode = player.countryCode)
                }
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("BAT ${player.battingRating}", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                    Text("BOWL ${player.bowlingRating}", style = MaterialTheme.typography.labelSmall, color = WicketCrimson)
                    Text("OVR ${player.overallRating}", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                }
            }
            if (selected) {
                Spacer(Modifier.width(6.dp))
                CaptainToggleButton(
                    isCaptain = isCaptain,
                    onClick = onSetCaptain,
                    testTagName = "pick_tab_captain_${player.id}"
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NavySurfaceHigh,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BattingOrderRowCard(
    position: Int,
    player: Cricketer,
    isCaptain: Boolean,
    isSwapSource: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTapPositionSlot: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMakeCaptain: () -> Unit
) {
    Surface(
        color = when {
            isSwapSource -> AuctionGold.copy(alpha = 0.18f)
            isCaptain -> AuctionGold.copy(alpha = 0.10f)
            else -> NavySurface
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSwapSource || isCaptain) 1.5.dp else 1.dp,
            color = when {
                isSwapSource -> AuctionGold
                isCaptain -> AuctionGold.copy(alpha = 0.7f)
                else -> NavyOutline
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_row_${position}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tappable #1..#11 position badge for instant tap-to-swap
            Surface(
                color = if (isSwapSource) AuctionGold else NavySurfaceHigh,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isSwapSource) AuctionGold else TurfEmerald.copy(alpha = 0.6f)),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onTapPositionSlot)
                    .testTag("slot_swap_${position}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "#$position",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (isSwapSource) OnGoldDark else TurfEmerald
                        )
                    )
                    if (position <= 2) {
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = if (isSwapSource) OnGoldDark else BroadcastMuted
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BroadcastWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    OverseasBadge(overseas = player.overseas, countryCode = player.countryCode)
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RolePillBadge(role = player.role)
                    Text("BAT ${player.battingRating}", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                    Text("BWL ${player.bowlingRating}", style = MaterialTheme.typography.labelSmall, color = WicketCrimson)
                }
            }

            // Captain (C) selector button
            CaptainToggleButton(
                isCaptain = isCaptain,
                onClick = onMakeCaptain,
                testTagName = "set_captain_${player.id}"
            )

            Spacer(Modifier.width(4.dp))

            // Up / Down arrows for exact 1..11 batting order control
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("move_up_${position}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move ${player.name} up in batting order",
                        tint = if (canMoveUp) BroadcastWhite else BroadcastMuted.copy(alpha = 0.35f)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("move_down_${position}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move ${player.name} down in batting order",
                        tint = if (canMoveDown) BroadcastWhite else BroadcastMuted.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptainToggleButton(
    isCaptain: Boolean,
    onClick: () -> Unit,
    testTagName: String
) {
    Surface(
        color = if (isCaptain) AuctionGold else NavySurfaceHigh,
        shape = CircleShape,
        border = BorderStroke(1.2.dp, if (isCaptain) AuctionGold else AuctionGold.copy(alpha = 0.45f)),
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTagName)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "C",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCaptain) OnGoldDark else AuctionGold
                )
            )
        }
    }
}
