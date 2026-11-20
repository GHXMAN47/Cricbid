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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountProfile
import com.example.notifications.NotificationPreferences
import com.example.ui.components.IndianPlayerAvatar
import com.example.ui.components.IndianPlayerAvatarSpec
import com.example.ui.components.IndianPlayerAvatars
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
fun ProfileScreen(
    profile: AccountProfile,
    backupCode: String,
    isCloudConnected: Boolean,
    errorMessage: String?,
    notificationPrefs: NotificationPreferences = NotificationPreferences(),
    hasNotificationPermission: Boolean = true,
    onUpdateNotificationPrefs: (NotificationPreferences) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onBack: () -> Unit,
    onSaveProfile: (username: String, avatarId: String, favoriteTeam: String, newPassword: String?) -> Unit,
    onCopyBackupCode: () -> Unit,
    onConnectCloudSync: () -> Unit,
    onSignOutToWelcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val clipboard = LocalClipboardManager.current

    var usernameDraft by rememberSaveable(profile.accountId, profile.username) { mutableStateOf(profile.username) }
    var favoriteTeamDraft by rememberSaveable(profile.accountId, profile.favoriteTeamName) { mutableStateOf(profile.favoriteTeamName) }
    var selectedAvatarId by rememberSaveable(profile.accountId, profile.avatarId) { mutableStateOf(profile.avatarId) }
    var newPasswordDraft by rememberSaveable(profile.accountId) { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val selectedAvatarSpec = remember(selectedAvatarId) { IndianPlayerAvatars.byId(selectedAvatarId) }
    val avatarRows = remember { IndianPlayerAvatars.all.chunked(4) }

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
                        .testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to lobby",
                        tint = BroadcastWhite
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MANAGER PROFILE",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = 1.sp,
                            color = BroadcastWhite
                        )
                    )
                    Text(
                        text = "Avatar • Password Security • Reinstall Backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastMuted
                    )
                }
                IconButton(
                    onClick = onSignOutToWelcome,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Switch account or sign out",
                        tint = WicketCrimson
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Profile Summary + Stats
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IndianPlayerAvatar(
                                    avatarId = selectedAvatarId,
                                    size = 76.dp,
                                    selected = true
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.username,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = DisplayFontFamily,
                                            color = BroadcastWhite
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedAvatarSpec.playerName} • ${selectedAvatarSpec.nickname}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AuctionGold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        color = if (profile.hasPassword) TurfEmerald.copy(alpha = 0.16f) else BoundarySky.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (profile.hasPassword) TurfEmerald.copy(alpha = 0.45f) else BoundarySky.copy(alpha = 0.45f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (profile.hasPassword) Icons.Filled.Shield else Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = if (profile.hasPassword) TurfEmerald else BoundarySky,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = if (profile.hasPassword) "PASSWORD PROTECTED • REINSTALL SAFE" else "GUEST PROFILE • SET PASSWORD BELOW",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (profile.hasPassword) TurfEmerald else BoundarySky,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // 4-Column Career Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatBox("MATCHES", "${profile.matchesPlayed}", BroadcastWhite, Modifier.weight(1f))
                                ProfileStatBox("WINS", "${profile.matchesWon}", TurfEmerald, Modifier.weight(1f))
                                ProfileStatBox("WIN %", "${profile.winRatePercent}%", BoundarySky, Modifier.weight(1f))
                                ProfileStatBox("TROPHIES", "${profile.trophiesWon}", AuctionGold, Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Indian Cricket Player Avatars Grid (Lightweight 4x3 rows, zero nested lazy grids)
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, NavyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "INDIAN CRICKET AVATAR",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 1.sp,
                                        color = BroadcastWhite
                                    )
                                )
                                Text(
                                    text = "${selectedAvatarSpec.shortName} #${selectedAvatarSpec.jerseyNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = MonoFontFamily,
                                        color = AuctionGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            avatarRows.forEach { rowSpecs ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSpecs.forEach { spec ->
                                        AvatarGridTile(
                                            spec = spec,
                                            selected = selectedAvatarId == spec.id,
                                            onSelect = { selectedAvatarId = spec.id },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Account Credentials & Details Form
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, NavyOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "ACCOUNT & PASSWORD",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.sp,
                                    color = BroadcastWhite
                                )
                            )

                            if (!errorMessage.isNullOrBlank()) {
                                Surface(
                                    color = WicketCrimson.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BroadcastWhite,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = usernameDraft,
                                onValueChange = { if (it.length <= 28) usernameDraft = it },
                                label = { Text("Manager Username") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TurfEmerald) },
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_username_input")
                            )

                            OutlinedTextField(
                                value = favoriteTeamDraft,
                                onValueChange = { if (it.length <= 32) favoriteTeamDraft = it },
                                label = { Text("Preferred Franchise Name") },
                                leadingIcon = { Icon(Icons.Filled.SportsCricket, contentDescription = null, tint = AuctionGold) },
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_fav_team_input")
                            )

                            OutlinedTextField(
                                value = newPasswordDraft,
                                onValueChange = { if (it.length <= 32) newPasswordDraft = it },
                                label = {
                                    Text(
                                        if (profile.hasPassword) "Update Password (Optional)"
                                        else "Set Password (Min 4 chars for Account Login)"
                                    )
                                },
                                placeholder = { Text("Leave blank to keep current password") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = BoundarySky) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password",
                                            tint = BroadcastSilver
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_password_input")
                            )

                            Button(
                                onClick = {
                                    onSaveProfile(
                                        usernameDraft.trim(),
                                        selectedAvatarId,
                                        favoriteTeamDraft.trim(),
                                        newPasswordDraft.trim().ifBlank { null }
                                    )
                                    newPasswordDraft = ""
                                },
                                enabled = usernameDraft.trim().length >= 2 &&
                                    (newPasswordDraft.isBlank() || newPasswordDraft.trim().length >= 4),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TurfEmerald,
                                    contentColor = OnEmeraldDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("save_profile_button")
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Save Profile & Avatar",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Reinstall-Safe Backup Code & Cloud Multiplayer Card
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "REINSTALL-SAFE ACCOUNT BACKUP",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = DisplayFontFamily,
                                        letterSpacing = 0.8.sp,
                                        color = BroadcastWhite
                                    )
                                )
                            }
                            Text(
                                text = "Your profile & salted password hash auto-backup with Android Auto Backup. Copy your portable CB1 Backup Code to restore your profile on any device or after a clean reinstall.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )

                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(backupCode))
                                    onCopyBackupCode()
                                },
                                border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("copy_backup_code_button")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Copy Account Backup Code (CB1-...)",
                                    style = MaterialTheme.typography.labelLarge.copy(color = AuctionGold, fontWeight = FontWeight.Bold)
                                )
                            }

                            if (!isCloudConnected) {
                                OutlinedButton(
                                    onClick = onConnectCloudSync,
                                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_connect_cloud_button")
                                ) {
                                    Text(
                                        text = "Optional: Link Google Account for Profile Backup",
                                        style = MaterialTheme.typography.labelMedium.copy(color = BoundarySky, fontWeight = FontWeight.Bold)
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TurfEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Google Account Linked for Optional Profile Backup",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TurfEmerald, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Local Alerts & Notifications Card
                item {
                    Surface(
                        color = NavySurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = BoundarySky,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "LOCAL ALERTS & NOTIFICATIONS",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = BoundarySky,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = if (hasNotificationPermission) "ACTIVE" else "NEED PERMISSION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasNotificationPermission) TurfEmerald else AuctionGold
                                    )
                                )
                            }

                            Text(
                                text = "Get local device alerts when your auction bidding turn starts (new lot / outbid) or a match involving your team is about to begin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )

                            if (!hasNotificationPermission) {
                                Button(
                                    onClick = onRequestNotificationPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = NavyMidnight),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("profile_grant_notif_permission_button")
                                ) {
                                    Text("Allow Local Notifications", fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auction Turn Alerts",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = BroadcastWhite
                                    )
                                    Text(
                                        text = "New lot open, outbid counter-turn & retention turn",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastMuted
                                    )
                                }
                                Switch(
                                    checked = notificationPrefs.enabled && notificationPrefs.auctionTurnAlerts,
                                    onCheckedChange = { checked ->
                                        if (checked && !hasNotificationPermission) {
                                            onRequestNotificationPermission()
                                        }
                                        onUpdateNotificationPrefs(
                                            notificationPrefs.copy(
                                                enabled = checked || notificationPrefs.matchStartAlerts,
                                                auctionTurnAlerts = checked
                                            )
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnEmeraldDark,
                                        checkedTrackColor = TurfEmerald
                                    ),
                                    modifier = Modifier.testTag("toggle_auction_turn_alerts")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Match Kickoff Alerts",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = BroadcastWhite
                                    )
                                    Text(
                                        text = "Upcoming fixture ready & match starting now",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastMuted
                                    )
                                }
                                Switch(
                                    checked = notificationPrefs.enabled && notificationPrefs.matchStartAlerts,
                                    onCheckedChange = { checked ->
                                        if (checked && !hasNotificationPermission) {
                                            onRequestNotificationPermission()
                                        }
                                        onUpdateNotificationPrefs(
                                            notificationPrefs.copy(
                                                enabled = checked || notificationPrefs.auctionTurnAlerts,
                                                matchStartAlerts = checked
                                            )
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnEmeraldDark,
                                        checkedTrackColor = TurfEmerald
                                    ),
                                    modifier = Modifier.testTag("toggle_match_kickoff_alerts")
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!hasNotificationPermission) {
                                        onRequestNotificationPermission()
                                    } else {
                                        onSendTestNotification()
                                    }
                                },
                                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("profile_send_test_notification_button")
                            ) {
                                Text(
                                    text = "Send Test Notification",
                                    style = MaterialTheme.typography.labelMedium.copy(color = TurfEmerald, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NavyOutline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MonoFontFamily,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BroadcastMuted,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
private fun AvatarGridTile(
    spec: IndianPlayerAvatarSpec,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .testTag("avatar_${spec.id}"),
        color = if (selected) spec.primaryColor.copy(alpha = 0.25f) else NavySurfaceHigh,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) spec.accentColor else NavyOutline
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IndianPlayerAvatar(
                avatarId = spec.id,
                size = 50.dp,
                selected = selected
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = spec.shortName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (selected) AuctionGold else BroadcastWhite,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
