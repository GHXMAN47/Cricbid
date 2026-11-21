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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AccountProfile
import com.example.ui.components.IndianPlayerAvatar
import com.example.ui.components.IndianPlayerAvatars
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BoundarySky
import com.example.ui.theme.BroadcastMuted
import com.example.ui.theme.BroadcastSilver
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

@Composable
fun WelcomeScreen(
    activeProfile: AccountProfile?,
    savedUsernames: List<String>,
    isCloudConnected: Boolean,
    cloudSignInLoading: Boolean,
    authErrorMessage: String?,
    cloudErrorMessage: String?,
    onContinueActiveProfile: () -> Unit,
    onQuickGuestPlay: (username: String, avatarId: String) -> Unit,
    onSignInWithPassword: (username: String, password: String) -> Unit,
    onCreateAccount: (username: String, password: String, avatarId: String) -> Unit,
    onRestoreBackupCode: (code: String, password: String) -> Unit,
    onOptionalGoogleCloudSync: () -> Unit
) {
    // 0 = Quick Play, 1 = Sign In, 2 = Create Account
    var selectedMode by rememberSaveable { mutableStateOf(0) }
    var usernameInput by rememberSaveable(activeProfile?.username) {
        mutableStateOf(activeProfile?.username.orEmpty())
    }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var selectedAvatarId by rememberSaveable(activeProfile?.avatarId) {
        mutableStateOf(activeProfile?.avatarId ?: IndianPlayerAvatars.DEFAULT_ID)
    }
    val context = LocalContext.current
    var showPassword by remember { mutableStateOf(false) }
    var showBackupRestore by rememberSaveable { mutableStateOf(false) }
    var backupCodeInput by rememberSaveable { mutableStateOf("") }
    var localFileError by remember { mutableStateOf<String?>(null) }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        localFileError = null
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                require(bytes.size <= 512_000) { "Backup file is too large (max 512 KB)." }
                String(bytes, Charsets.UTF_8)
            } ?: throw IllegalStateException("Could not open selected backup file.")
        }.fold(
            onSuccess = { payload ->
                onRestoreBackupCode(payload, passwordInput)
            },
            onFailure = { err ->
                localFileError = err.message ?: "Failed to read backup file."
            }
        )
    }

    val trimmedUser = usernameInput.trim()
    val displayAuthError = authErrorMessage ?: localFileError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight)
    ) {
        // Floodlight Stadium Hero Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_stadium_hero_1795078716390),
                contentDescription = "Cricket stadium under night floodlights",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NavyMidnight.copy(alpha = 0.25f),
                                NavyMidnight.copy(alpha = 0.78f),
                                NavyMidnight
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Broadcast Pill Tag
            Surface(
                color = TurfEmerald.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Gavel,
                        contentDescription = null,
                        tint = AuctionGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tagline_broadcast),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TurfEmerald,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "CRICBID",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = DisplayFontFamily,
                    fontSize = 50.sp,
                    letterSpacing = 3.sp,
                    color = BroadcastWhite
                )
            )

            Text(
                text = "Enter a username to play live multiplayer auctions as a Guest — no login required.",
                style = MaterialTheme.typography.bodySmall,
                color = BroadcastSilver,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            // Returning Manager One-Tap Resume Banner
            if (activeProfile != null) {
                val avatarSpec = IndianPlayerAvatars.byId(activeProfile.avatarId)
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, TurfEmerald.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IndianPlayerAvatar(
                            avatarId = activeProfile.avatarId,
                            size = 52.dp,
                            selected = true
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WELCOME BACK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TurfEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = activeProfile.username,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = BroadcastWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${avatarSpec.shortName} #${avatarSpec.jerseyNumber} • ${activeProfile.matchesWon}W / ${activeProfile.matchesPlayed}M",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastSilver
                            )
                        }
                        Button(
                            onClick = onContinueActiveProfile,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = OnEmeraldDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("resume_active_profile_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Main Entry Card (Quick Play / Sign In / Create Account)
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 3-Mode Segment Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavySurfaceHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WelcomeModeChip("Quick Play", selectedMode == 0, { selectedMode = 0 }, Modifier.weight(1f).testTag("mode_quick_play"))
                        WelcomeModeChip("Sign In", selectedMode == 1, { selectedMode = 1 }, Modifier.weight(1f).testTag("mode_sign_in"))
                        WelcomeModeChip("Create", selectedMode == 2, { selectedMode = 2 }, Modifier.weight(1f).testTag("mode_create_account"))
                    }

                    if (!displayAuthError.isNullOrBlank()) {
                        Surface(
                            color = WicketCrimson.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayAuthError,
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastWhite,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    when (selectedMode) {
                        // 0: Quick Guest Play (Zero Friction)
                        0 -> {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { if (it.length <= 28) usernameInput = it },
                                label = { Text(stringResource(R.string.welcome_username_label)) },
                                placeholder = { Text("e.g. CaptainVirat") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TurfEmerald) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (trimmedUser.length >= 2) {
                                            onQuickGuestPlay(trimmedUser, selectedAvatarId)
                                        }
                                    }
                                ),
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("welcome_username_input")
                            )

                            CompactAvatarStrip(
                                selectedAvatarId = selectedAvatarId,
                                onSelect = { selectedAvatarId = it }
                            )

                            Button(
                                onClick = {
                                    if (trimmedUser.length >= 2) onQuickGuestPlay(trimmedUser, selectedAvatarId)
                                },
                                enabled = trimmedUser.length >= 2,
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
                                    .testTag("continue_play_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.continue_play_cta),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // 1: Username + Password Sign In (plus Reinstall Backup Code Restore)
                        1 -> {
                            if (savedUsernames.isNotEmpty() && !showBackupRestore) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "SAVED ACCOUNTS ON THIS DEVICE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastMuted
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(savedUsernames, key = { it }) { savedName ->
                                            Surface(
                                                color = if (usernameInput.equals(savedName, ignoreCase = true)) {
                                                    TurfEmerald.copy(alpha = 0.2f)
                                                } else NavySurfaceHigh,
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (usernameInput.equals(savedName, ignoreCase = true)) TurfEmerald else NavyOutline
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { usernameInput = savedName }
                                            ) {
                                                Text(
                                                    text = savedName,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = BroadcastWhite),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (!showBackupRestore) {
                                OutlinedTextField(
                                    value = usernameInput,
                                    onValueChange = { if (it.length <= 28) usernameInput = it },
                                    label = { Text("Account Username") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TurfEmerald) },
                                    singleLine = true,
                                    colors = cricBidTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signin_username_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { if (it.length <= 32) passwordInput = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AuctionGold) },
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
                                        .testTag("signin_password_input")
                                )

                                Button(
                                    onClick = {
                                        localFileError = null
                                        onSignInWithPassword(trimmedUser, passwordInput)
                                    },
                                    enabled = trimmedUser.length >= 2 && passwordInput.length >= 4,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TurfEmerald,
                                        contentColor = OnEmeraldDark
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("account_sign_in_button")
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Sign In / Continue",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Text(
                                    text = "Fresh install? Entering your username & password sets up your profile immediately — or restore a saved backup below.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BroadcastMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = backupCodeInput,
                                    onValueChange = { backupCodeInput = it },
                                    label = { Text("Backup Code (CB2-... / CB1-...) or JSON") },
                                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = AuctionGold) },
                                    singleLine = false,
                                    maxLines = 3,
                                    colors = cricBidTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("restore_backup_code_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { if (it.length <= 32) passwordInput = it },
                                    label = { Text("Password (optional — only for locked CB1 codes)") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = BoundarySky) },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = cricBidTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("restore_backup_password_input")
                                )

                                val trimmedCode = backupCodeInput.trim()
                                val canSubmitCode = trimmedCode.startsWith("CB2-") ||
                                    trimmedCode.startsWith("CB1-") ||
                                    trimmedCode.startsWith("{")

                                Button(
                                    onClick = {
                                        localFileError = null
                                        onRestoreBackupCode(backupCodeInput, passwordInput)
                                    },
                                    enabled = canSubmitCode,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AuctionGold,
                                        contentColor = OnGoldDark
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("restore_backup_submit_button")
                                ) {
                                    Text(
                                        text = "Restore Account & Career",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        localFileError = null
                                        openBackupFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                    },
                                    border = BorderStroke(1.dp, BoundarySky.copy(alpha = 0.65f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("restore_backup_json_button")
                                ) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Load Backup .JSON File",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = BoundarySky,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    localFileError = null
                                    showBackupRestore = !showBackupRestore
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .testTag("toggle_backup_restore_button")
                            ) {
                                Text(
                                    text = if (showBackupRestore) "← Back to Username + Password Sign In" else "Reinstalled? Restore Backup Code or .JSON File",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BoundarySky
                                )
                            }
                        }

                        // 2: Create Reinstall-Safe Password Account + Pick Indian Avatar
                        else -> {
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { if (it.length <= 28) usernameInput = it },
                                label = { Text("Choose Username") },
                                placeholder = { Text("e.g. Hitman45") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TurfEmerald) },
                                singleLine = true,
                                colors = cricBidTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_username_input")
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { if (it.length <= 32) passwordInput = it },
                                label = { Text("Create Password (Min 4 chars)") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = AuctionGold) },
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
                                    .testTag("register_password_input")
                            )

                            CompactAvatarStrip(
                                selectedAvatarId = selectedAvatarId,
                                onSelect = { selectedAvatarId = it }
                            )

                            Button(
                                onClick = { onCreateAccount(trimmedUser, passwordInput, selectedAvatarId) },
                                enabled = trimmedUser.length >= 2 && passwordInput.length >= 4,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuctionGold,
                                    contentColor = OnGoldDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("create_account_submit_button")
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Create Account & Enter Arena",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Compact Feature Highlights Grid (2x2)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WelcomeFeatureTile(
                        icon = Icons.Filled.Groups,
                        title = "8-Team Arena",
                        subtitle = "Guest Multiplayer Rooms",
                        accent = TurfEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeFeatureTile(
                        icon = Icons.Filled.Gavel,
                        title = "120 Stars",
                        subtitle = "Retention + RTM Card",
                        accent = AuctionGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WelcomeFeatureTile(
                        icon = Icons.Filled.SportsCricket,
                        title = "Super Over T20",
                        subtitle = "4-Over Cap & Rotation",
                        accent = BoundarySky,
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeFeatureTile(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Intl & IPL",
                        subtitle = "Series, WC & Playoffs",
                        accent = AuctionGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (!cloudErrorMessage.isNullOrBlank()) {
                Surface(
                    color = WicketCrimson.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = cloudErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastWhite,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Optional Cross-Device Cloud Multiplayer Sync
            if (!isCloudConnected) {
                OutlinedButton(
                    onClick = onOptionalGoogleCloudSync,
                    enabled = !cloudSignInLoading,
                    border = BorderStroke(1.dp, NavyOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("optional_google_cloud_button")
                ) {
                    if (cloudSignInLoading) {
                        CircularProgressIndicator(Modifier.size(18.dp), BoundarySky, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting Cloud Sync...", style = MaterialTheme.typography.labelLarge, color = BroadcastSilver)
                    } else {
                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = BoundarySky, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.connect_cloud_sync),
                            style = MaterialTheme.typography.labelMedium,
                            color = BroadcastSilver
                        )
                    }
                }
            } else {
                Surface(
                    color = TurfEmerald.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "✓ Optional Google Profile Backup Connected",
                        style = MaterialTheme.typography.labelSmall.copy(color = TurfEmerald, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WelcomeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        color = if (selected) TurfEmerald else NavySurfaceHigh,
        shape = RoundedCornerShape(9.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (selected) OnEmeraldDark else BroadcastSilver,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactAvatarStrip(
    selectedAvatarId: String,
    onSelect: (String) -> Unit
) {
    val activeSpec = IndianPlayerAvatars.byId(selectedAvatarId)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PICK INDIAN CRICKET AVATAR",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BroadcastSilver,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${activeSpec.shortName} #${activeSpec.jerseyNumber}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AuctionGold,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(IndianPlayerAvatars.all, key = { it.id }) { spec ->
                val isSelected = spec.id == selectedAvatarId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(spec.id) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("welcome_avatar_${spec.id}")
                ) {
                    IndianPlayerAvatar(
                        avatarId = spec.id,
                        size = 48.dp,
                        selected = isSelected
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = spec.shortName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) AuctionGold else BroadcastMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = NavySurfaceHigh,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NavyOutline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = BroadcastWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = BroadcastMuted
                )
            }
        }
    }
}
