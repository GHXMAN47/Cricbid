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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AuctionRoom
import com.example.data.model.CurrencyFormatter
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
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
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

@Composable
fun RoomLobbyScreen(
    room: AuctionRoom,
    currentUid: String,
    isCloudConnected: Boolean,
    busy: Boolean,
    onBack: () -> Unit,
    onCopyCode: (String) -> Unit,
    onToggleAutoFillEightTeams: (Boolean) -> Unit,
    onAddAiBot: () -> Unit,
    onRemoveAiBot: (String) -> Unit,
    onStartAuction: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val clipboard = LocalClipboardManager.current
    val isHost = room.hostUid == currentUid

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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("lobby_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to rooms",
                        tint = BroadcastWhite
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUCTION LOBBY",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            color = BroadcastWhite
                        )
                    )
                    Text(
                        text = if (isHost) "You are Host • Play Solo vs 7 AI or Invite Friends" else "Waiting for Host to start auction",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Big 4-Digit Room Code Card
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, AuctionGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SHARE 4-DIGIT ROOM CODE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AuctionGold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = room.roomCode,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = MonoFontFamily,
                                        fontSize = 48.sp,
                                        letterSpacing = 8.sp,
                                        color = BroadcastWhite
                                    ),
                                    modifier = Modifier.testTag("lobby_room_code_text")
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(room.roomCode))
                                        onCopyCode(room.roomCode)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("copy_room_code_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy room code",
                                        tint = AuctionGold
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RulePill("₹100 Cr")
                                RulePill("1 RTM Card")
                                RulePill("120 Stars")
                                RulePill("8 OS / 4 XI")
                            }
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = TurfEmerald.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "● LIVE MULTIPLAYER — Friends Can Join With Code ${room.roomCode}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TurfEmerald,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (room.lastEventMessage.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = room.lastEventMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver
                                )
                            }
                        }
                    }
                }

                // 8-Team AI Auto-Fill Toggle Card
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (room.autoFillEightTeams) TurfEmerald.copy(alpha = 0.55f) else BoundarySky.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Fill to 8 Teams with Smart AI",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BroadcastWhite
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (room.autoFillEightTeams) {
                                        val botsToAdd = (SquadRules.DEFAULT_TARGET_TEAMS - room.teams.size).coerceAtLeast(0)
                                        if (botsToAdd > 0) {
                                            "Adds $botsToAdd Smart AI manager(s) on start → Full League + IPL Playoffs!"
                                        } else {
                                            "8 Franchises ready → Full League + IPL Playoffs!"
                                        }
                                    } else {
                                        "2 teams play a 5-Match Series; 4–8 teams play League + IPL Playoffs."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BroadcastSilver
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Switch(
                                checked = room.autoFillEightTeams,
                                onCheckedChange = { if (isHost && !busy) onToggleAutoFillEightTeams(it) },
                                enabled = isHost && !busy,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnEmeraldDark,
                                    checkedTrackColor = TurfEmerald
                                ),
                                modifier = Modifier.testTag("toggle_autofill_eight_teams")
                            )
                        }
                    }
                }

                // Franchises Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FRANCHISES (${room.teams.size}/${SquadRules.MAX_TEAMS})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                letterSpacing = 1.sp,
                                color = BroadcastSilver
                            )
                        )
                        Text(
                            text = if (room.autoFillEightTeams) "Solo OK • AI fills to 8" else "Min ${SquadRules.MIN_TEAMS} to start",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastMuted
                        )
                    }
                }

                // Team Roster List
                items(room.teams, key = { it.teamId }) { team ->
                    LobbyFranchiseRow(
                        team = team,
                        isHostTeam = team.ownerUid == room.hostUid && !team.bot,
                        canRemove = isHost && team.bot,
                        onRemove = { onRemoveAiBot(team.teamId) }
                    )
                }

                // Host: Add AI Bot Button
                if (isHost && room.teams.size < SquadRules.MAX_TEAMS) {
                    item {
                        OutlinedButton(
                            onClick = onAddAiBot,
                            enabled = !busy,
                            border = BorderStroke(1.2.dp, BoundarySky.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("add_ai_bot_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = null,
                                tint = BoundarySky
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.add_ai_bot),
                                style = MaterialTheme.typography.titleMedium.copy(color = BoundarySky)
                            )
                        }
                    }
                }
            }

            // Bottom Action Bar
            Surface(
                color = NavySurface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isHost) {
                        val canStart = (room.autoFillEightTeams || room.teams.size >= SquadRules.MIN_TEAMS) && !busy
                        val effectiveCount = if (room.autoFillEightTeams) maxOf(room.teams.size, SquadRules.DEFAULT_TARGET_TEAMS) else room.teams.size
                        Button(
                            onClick = onStartAuction,
                            enabled = canStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = OnEmeraldDark
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_auction_button")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(Modifier.size(22.dp), OnEmeraldDark, strokeWidth = 2.5.dp)
                            } else {
                                Icon(Icons.Filled.Gavel, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (!canStart) {
                                        "Add 1+ AI Bot or Friend to Start"
                                    } else {
                                        "Start $effectiveCount-Team Auction (Retention)"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Waiting for the room host to strike the opening gavel...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuctionGold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RulePill(label: String) {
    Surface(
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, NavyOutline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = BroadcastSilver,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LobbyFranchiseRow(
    team: TeamState,
    isHostTeam: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val iconColor = if (team.bot) BoundarySky else TurfEmerald
    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (team.bot) Icons.Filled.SmartToy else Icons.Filled.Person,
                    contentDescription = if (team.bot) "AI Bot Manager" else "Human Manager",
                    tint = iconColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team.teamName,
                        style = MaterialTheme.typography.titleMedium,
                        color = BroadcastWhite
                    )
                    if (isHostTeam) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = AuctionGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "HOST",
                                style = MaterialTheme.typography.labelSmall.copy(color = AuctionGold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (team.bot) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = BoundarySky.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "AI BOT",
                                style = MaterialTheme.typography.labelSmall.copy(color = BoundarySky),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Purse: ${CurrencyFormatter.formatLakhs(team.budgetLakhs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TurfEmerald
                )
            }
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("remove_bot_${team.teamId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Remove AI Bot",
                        tint = WicketCrimson
                    )
                }
            }
        }
    }
}
