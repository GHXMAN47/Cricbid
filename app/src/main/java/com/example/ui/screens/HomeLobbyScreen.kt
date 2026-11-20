package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountProfile
import com.example.data.model.AuctionRoom
import com.example.data.model.RoomStatus
import com.example.ui.components.IndianPlayerAvatar
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
import com.example.viewmodel.UiState

@Composable
fun HomeLobbyScreen(
    profile: AccountProfile,
    isCloudConnected: Boolean,
    roomsState: UiState<List<AuctionRoom>>,
    busy: Boolean,
    joinStatusError: String? = null,
    joinStatusInfo: String? = null,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenInternational: () -> Unit,
    onOpenCareerMode: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onClearJoinStatus: () -> Unit = {},
    onConnectCloudSync: () -> Unit,
    onDisconnectCloudSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var createTeamName by rememberSaveable(profile.favoriteTeamName) {
        mutableStateOf(profile.favoriteTeamName.ifBlank { "Royal Strikers" })
    }
    var joinRoomCode by rememberSaveable { mutableStateOf("") }
    var joinTeamName by rememberSaveable { mutableStateOf("Super Kings") }
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Create, 1 = Join

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero_1795078716390),
                        contentDescription = "CricBid floodlit cricket stadium",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        NavyMidnight.copy(alpha = 0.45f),
                                        NavyMidnight.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AuctionGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "₹100 CR • 120 STARS • 1 RTM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AuctionGold,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCloudConnected) {
                                Surface(
                                    color = TurfEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onDisconnectCloudSync)
                                        .testTag("disconnect_cloud_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Cloud, contentDescription = "Google account linked", tint = TurfEmerald, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Profile Linked", style = MaterialTheme.typography.labelSmall.copy(color = TurfEmerald, fontWeight = FontWeight.Bold))
                                    }
                                }
                            } else {
                                Surface(
                                    color = NavySurfaceHigh.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, NavyOutline),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onConnectCloudSync)
                                        .testTag("connect_cloud_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Cloud, contentDescription = "Optional profile backup", tint = BroadcastSilver, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Backup Sync", style = MaterialTheme.typography.labelSmall.copy(color = BroadcastSilver))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CRICBID ARENA",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.5.sp,
                                    color = BroadcastWhite
                                )
                            )
                            Text(
                                text = "${profile.matchesWon}W / ${profile.matchesPlayed}M • ${profile.trophiesWon} Trophies",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastSilver
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            color = NavySurface.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenProfile)
                                .testTag("open_profile_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IndianPlayerAvatar(
                                    avatarId = profile.avatarId,
                                    size = 40.dp,
                                    selected = true,
                                    showJerseyPill = false
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = profile.username,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = BroadcastWhite,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (profile.hasPassword) "Profile • Protected" else "Edit Profile",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AuctionGold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // International Cricket Entry Card (T20I • ODI • Test • Series & World Cup)
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, BoundarySky.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenInternational)
                        .testTag("open_international_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BoundarySky.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.45f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = stringResource(R.string.intl_hub_title),
                                    tint = BoundarySky,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.intl_hub_title).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 1.sp,
                                        color = BroadcastWhite
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = TurfEmerald.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "T20I • ODI • TEST",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TurfEmerald,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.intl_hub_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open International Cricket",
                            tint = BoundarySky
                        )
                    }
                }
            }

            // Career Mode (Road to Legend) Entry Card
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenCareerMode)
                        .testTag("open_career_mode_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AuctionGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.5f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = stringResource(R.string.career_mode_title),
                                    tint = AuctionGold,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.career_mode_title).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 1.sp,
                                        color = BroadcastWhite
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = AuctionGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ROAD TO LEGEND",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AuctionGold,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.career_mode_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Career Mode",
                            tint = AuctionGold
                        )
                    }
                }
            }

            // Create / Join Mode Switcher + Action Card
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NavySurfaceHigh)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ModeSegmentButton(
                                label = "Create Room",
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_create_room")
                            )
                            ModeSegmentButton(
                                label = "Join Code",
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tab_join_room")
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        if (selectedTab == 0) {
                            Text(
                                text = stringResource(R.string.create_room_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = BroadcastWhite
                            )
                            Text(
                                text = "Get a 4-digit room code to invite friends or add AI Bot managers for solo play.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                            )
                            OutlinedTextField(
                                value = createTeamName,
                                onValueChange = { if (it.length <= 32) createTeamName = it },
                                label = { Text(stringResource(R.string.team_name_label)) },
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("create_team_name_input")
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onCreateRoom(createTeamName) },
                                enabled = !busy && createTeamName.trim().length >= 2,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurfEmerald,
                                    contentColor = OnEmeraldDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("create_room_button")
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(Modifier.size(20.dp), OnEmeraldDark, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.AddCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.create_room_cta),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.join_room_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = BroadcastWhite
                            )
                            Text(
                                text = "Enter your friend's 4-digit room code and pick your franchise name — no login required.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastMuted,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            val normalizedJoinCode = com.example.data.model.RoomCodeNormalizer.normalize(joinRoomCode)
                            OutlinedTextField(
                                value = joinRoomCode,
                                onValueChange = { raw ->
                                    if (raw.length <= 12) {
                                        joinRoomCode = raw
                                        onClearJoinStatus()
                                    }
                                },
                                label = { Text(stringResource(R.string.room_code_label)) },
                                placeholder = { Text("e.g. 4829") },
                                supportingText = if (joinRoomCode.isNotBlank() && normalizedJoinCode != joinRoomCode) {
                                    { Text("Normalized code: $normalizedJoinCode", color = TurfEmerald) }
                                } else null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_room_code_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = joinTeamName,
                                onValueChange = {
                                    if (it.length <= 32) {
                                        joinTeamName = it
                                        onClearJoinStatus()
                                    }
                                },
                                label = { Text(stringResource(R.string.team_name_label)) },
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("join_team_name_input")
                            )
                            if (!joinStatusInfo.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = BoundarySky.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("join_status_info_banner")
                                ) {
                                    Text(
                                        text = joinStatusInfo,
                                        style = MaterialTheme.typography.bodySmall.copy(color = BroadcastWhite),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            if (!joinStatusError.isNullOrBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    color = WicketCrimson.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("join_status_error_banner")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = joinStatusError,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BroadcastWhite,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onJoinRoom(normalizedJoinCode, joinTeamName) },
                                enabled = !busy && normalizedJoinCode.isNotBlank() && joinTeamName.trim().length >= 2,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuctionGold,
                                    contentColor = OnGoldDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("join_room_button")
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(Modifier.size(20.dp), OnGoldDark, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.MeetingRoom, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.join_room_cta),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Your Active Rooms Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR AUCTION ROOMS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = BroadcastSilver
                        )
                    )
                }
            }

            when (roomsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TurfEmerald)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = WicketCrimson.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = roomsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BroadcastWhite,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (roomsState.data.isEmpty()) {
                        item {
                            Surface(
                                color = NavySurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, NavyOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = BroadcastMuted,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "No active rooms yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BroadcastWhite
                                    )
                                    Text(
                                        text = "Create a room above and add AI Bots or invite friends with your 4-digit code.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BroadcastMuted,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(roomsState.data, key = { it.roomCode }) { room ->
                            ActiveRoomCard(
                                room = room,
                                onClick = { onOpenRoom(room.roomCode) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ModeSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) TurfEmerald else NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (selected) OnEmeraldDark else BroadcastSilver,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun ActiveRoomCard(
    room: AuctionRoom,
    onClick: () -> Unit
) {
    val (badgeLabel, badgeColor) = when (room.roomStatus) {
        RoomStatus.LOBBY -> "LOBBY" to AuctionGold
        RoomStatus.RETENTION -> "RETENTION" to AuctionGold
        RoomStatus.AUCTION -> "LIVE AUCTION" to WicketCrimson
        RoomStatus.SQUAD_SELECTION -> "PICK XI" to TurfEmerald
        RoomStatus.MATCH_HUB -> "MATCH HUB" to TurfEmerald
        RoomStatus.COMPLETED -> "FINISHED" to BroadcastMuted
    }

    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NavyOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("room_card_${room.roomCode}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = NavySurfaceHigh,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NavyOutline)
            ) {
                Text(
                    text = room.roomCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = MonoFontFamily,
                        color = AuctionGold,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = BroadcastMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${room.teams.size} Teams",
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastMuted
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = room.teams.joinToString(" • ") { it.teamName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = BroadcastWhite,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open room",
                tint = BroadcastMuted
            )
        }
    }
}

@Composable
fun cricBidTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TurfEmerald,
    unfocusedBorderColor = NavyOutline,
    focusedLabelColor = TurfEmerald,
    unfocusedLabelColor = BroadcastMuted,
    focusedTextColor = BroadcastWhite,
    unfocusedTextColor = BroadcastWhite,
    cursorColor = TurfEmerald
)
