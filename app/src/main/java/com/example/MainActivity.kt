package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuctionDataSource
import com.example.data.firebase.AuctionRepository
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.safeAuthStateFlow
import com.example.data.firebase.signOutFromGoogle
import com.example.data.local.AccountProfile
import com.example.data.local.LocalAuctionRepository
import com.example.data.local.LocalCareerStore
import com.example.data.local.LocalInternationalStore
import com.example.data.local.LocalMatchResumeStore
import com.example.data.local.LocalProfileStore
import com.example.data.local.LocalSaveBackupManager
import com.example.data.local.LocalTrophyCabinetStore
import com.example.data.local.SavedMatchResumeSnapshot
import com.example.data.local.SavedMatchSource
import com.example.data.model.CareerMatchType
import com.example.data.model.MatchStage
import com.example.data.model.RoomStatus
import com.example.engine.SeasonAwardsEngine
import com.example.engine.TrophyCabinetData
import com.example.engine.TrophyCabinetRecord
import com.example.ui.screens.CareerScreen
import com.example.ui.screens.HomeLobbyScreen
import com.example.ui.screens.InternationalScreen
import com.example.ui.screens.LiveAuctionScreen
import com.example.ui.screens.MatchHubScreen
import com.example.ui.screens.MatchSimScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RetentionScreen
import com.example.ui.screens.RoomLobbyScreen
import com.example.ui.screens.SquadSelectionScreen
import com.example.ui.screens.StandingsScreen
import com.example.ui.screens.TrophyCabinetScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BroadcastSilver
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.CricBidTheme
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavySurface
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson
import com.example.viewmodel.CareerViewModel
import com.example.viewmodel.CricBidViewModel
import com.example.viewmodel.InternationalViewModel
import com.example.viewmodel.MatchSimViewModel
import com.google.firebase.auth.FirebaseAuth

private const val MAIN_ACTIVITY_TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { enableEdgeToEdge() }
            .onFailure { Log.w(MAIN_ACTIVITY_TAG, "enableEdgeToEdge skipped: ${it.message}") }

        var initialStartupError: String? = null
        try {
            FirebaseSafeInit.ensureInitialized(applicationContext)
        } catch (t: Throwable) {
            Log.e(MAIN_ACTIVITY_TAG, "Unexpected error during early startup", t)
            initialStartupError = t.localizedMessage ?: t.toString()
        }

        try {
            setContent {
                CricBidTheme {
                    var retryAttempt by remember { mutableIntStateOf(0) }
                    var forcedSafeGuest by remember { mutableStateOf(false) }
                    var startupError by remember(retryAttempt) { mutableStateOf(initialStartupError) }

                    if (startupError != null && !forcedSafeGuest) {
                        StartupErrorFallbackScreen(
                            errorMessage = startupError ?: "Unknown startup error",
                            onRetry = {
                                initialStartupError = null
                                startupError = null
                                retryAttempt += 1
                            },
                            onContinueSafeGuest = {
                                initialStartupError = null
                                startupError = null
                                forcedSafeGuest = true
                            }
                        )
                    } else {
                        key(retryAttempt, forcedSafeGuest) {
                            CricBidAppRoot(
                                forceOfflineGuestMode = forcedSafeGuest,
                                onStartupFatalError = { err ->
                                    startupError = err
                                }
                            )
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(MAIN_ACTIVITY_TAG, "Critical UI setup failure", t)
            runCatching {
                setContent {
                    CricBidTheme {
                        StartupErrorFallbackScreen(
                            errorMessage = t.localizedMessage ?: t.toString(),
                            onRetry = { recreate() },
                            onContinueSafeGuest = { recreate() }
                        )
                    }
                }
            }
        }
    }
}

private data class StartupBootstrap(
    val profileStore: LocalProfileStore,
    val saveBackupManager: LocalSaveBackupManager,
    val credentialManager: CredentialManager?,
    val auth: FirebaseAuth?,
    val initialProfile: AccountProfile?,
    val initialSavedUsernames: List<String>
)

@Composable
private fun StartupErrorFallbackScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onContinueSafeGuest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyMidnight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .testTag("startup_error_fallback_screen"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = NavySurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.2.dp, WicketCrimson.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CRICBID STARTUP RECOVERY",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = AuctionGold
                )
                Text(
                    text = "CricBid hit an unexpected launch error instead of closing. You can continue immediately in offline Guest mode or retry launch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BroadcastSilver
                )
                Surface(
                    color = NavyMidnight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = BroadcastWhite,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onContinueSafeGuest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = OnEmeraldDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("startup_fallback_guest_button")
                ) {
                    Text("Continue in Safe Guest Mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                OutlinedButton(
                    onClick = onRetry,
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("startup_fallback_retry_button")
                ) {
                    Text("Retry Launch", style = MaterialTheme.typography.labelLarge, color = AuctionGold)
                }
            }
        }
    }
}

@Composable
fun CricBidAppRoot(
    forceOfflineGuestMode: Boolean = false,
    onStartupFatalError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bootstrapResult = remember(context, forceOfflineGuestMode) {
        try {
            val store = LocalProfileStore(context)
            val backupMgr = LocalSaveBackupManager(context)
            val credMgr = if (forceOfflineGuestMode) null else runCatching { CredentialManager.create(context) }.getOrNull()
            val firebaseAuth = if (forceOfflineGuestMode) null else FirebaseSafeInit.safeFirebaseAuth(context)
            val loadedProfile = runCatching { store.loadActiveProfile() }.getOrNull()
            val loadedNames = runCatching { store.listSavedUsernames() }.getOrDefault(emptyList())
            Result.success(
                StartupBootstrap(
                    profileStore = store,
                    saveBackupManager = backupMgr,
                    credentialManager = credMgr,
                    auth = firebaseAuth,
                    initialProfile = loadedProfile,
                    initialSavedUsernames = loadedNames
                )
            )
        } catch (t: Throwable) {
            Log.e(MAIN_ACTIVITY_TAG, "Startup bootstrap failed", t)
            Result.failure(t)
        }
    }

    val bootstrap = bootstrapResult.getOrNull()
    if (bootstrap == null) {
        val errText = bootstrapResult.exceptionOrNull()?.let { "${it::class.java.simpleName}: ${it.localizedMessage ?: it}" }
            ?: "Unable to initialize local storage."
        LaunchedEffect(errText) { onStartupFatalError(errText) }
        StartupErrorFallbackScreen(
            errorMessage = errText,
            onRetry = { onStartupFatalError(errText) },
            onContinueSafeGuest = { onStartupFatalError(errText) }
        )
        return
    }

    val profileStore = bootstrap.profileStore
    val saveBackupManager = bootstrap.saveBackupManager
    val credentialManager = bootstrap.credentialManager
    val auth = bootstrap.auth

    val authFlow = remember(auth) { safeAuthStateFlow(auth) }
    val initialFirebaseUser = remember(auth) { runCatching { auth?.currentUser }.getOrNull() }
    val firebaseUser by authFlow.collectAsStateWithLifecycle(initialValue = initialFirebaseUser)

    var activeProfile by remember { mutableStateOf(bootstrap.initialProfile) }
    var savedUsernames by remember { mutableStateOf(bootstrap.initialSavedUsernames) }
    var enteredArena by rememberSaveable { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var profileErrorMessage by remember { mutableStateOf<String?>(null) }
    var cloudSignInLoading by remember { mutableStateOf(false) }
    var cloudErrorMessage by remember { mutableStateOf<String?>(null) }

    val triggerOptionalGoogleSignIn: () -> Unit = {
        cloudErrorMessage = null
        cloudSignInLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                cloudSignInLoading = false
                if (activeProfile == null) {
                    val fallback = runCatching { auth?.currentUser?.displayName?.trim().orEmpty() }
                        .getOrDefault("")
                        .ifBlank { "Manager" }
                    activeProfile = profileStore.continueAsGuest(fallback)
                }
            },
            onAuthError = { msg ->
                cloudSignInLoading = false
                cloudErrorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                cloudSignInLoading = false
            }
        )
    }

    if (!enteredArena || activeProfile == null) {
        WelcomeScreen(
            activeProfile = activeProfile,
            savedUsernames = savedUsernames,
            isCloudConnected = firebaseUser != null,
            cloudSignInLoading = cloudSignInLoading,
            authErrorMessage = authErrorMessage,
            cloudErrorMessage = cloudErrorMessage,
            onContinueActiveProfile = {
                authErrorMessage = null
                if (activeProfile != null) enteredArena = true
            },
            onQuickGuestPlay = { username, avatarId ->
                authErrorMessage = null
                activeProfile = profileStore.continueAsGuest(username, avatarId)
                savedUsernames = profileStore.listSavedUsernames()
                enteredArena = true
            },
            onSignInWithPassword = { username, password ->
                authErrorMessage = null
                profileStore.signInWithPassword(username, password).fold(
                    onSuccess = { loaded ->
                        activeProfile = loaded
                        savedUsernames = profileStore.listSavedUsernames()
                        enteredArena = true
                    },
                    onFailure = { err -> authErrorMessage = err.message ?: "Sign in failed" }
                )
            },
            onCreateAccount = { username, password, avatarId ->
                authErrorMessage = null
                profileStore.registerAccount(username, password, avatarId).fold(
                    onSuccess = { created ->
                        activeProfile = created
                        savedUsernames = profileStore.listSavedUsernames()
                        enteredArena = true
                    },
                    onFailure = { err -> authErrorMessage = err.message ?: "Could not create account" }
                )
            },
            onRestoreBackupCode = { code, password ->
                authErrorMessage = null
                saveBackupManager.importBackup(code, password).fold(
                    onSuccess = { summary ->
                        activeProfile = summary.profile
                        savedUsernames = profileStore.listSavedUsernames()
                        enteredArena = true
                    },
                    onFailure = { err -> authErrorMessage = err.message ?: "Could not restore backup" }
                )
            },
            onOptionalGoogleCloudSync = triggerOptionalGoogleSignIn
        )
    } else {
        val currentProfile = activeProfile!!
        val activeUid = firebaseUser?.uid ?: currentProfile.accountId
        val isCloudConnected = firebaseUser != null
        val sessionKey = if (isCloudConnected) "cloud_$activeUid" else "guest_${currentProfile.accountId}"

        key(sessionKey) {
            CricBidArenaFlow(
                sessionKey = sessionKey,
                currentUserId = activeUid,
                profile = currentProfile,
                saveBackupManager = saveBackupManager,
                initialRecentRoomCodes = profileStore.loadRecentRoomCodes(),
                onRememberRoomCode = { code -> profileStore.rememberRoomCode(code) },
                profileErrorMessage = profileErrorMessage,
                isCloudConnected = isCloudConnected,
                forceOfflineGuestMode = forceOfflineGuestMode,
                onProfileRestored = { restored ->
                    activeProfile = restored
                    savedUsernames = profileStore.listSavedUsernames()
                },
                onSaveProfile = { username, avatarId, favTeam, newPassword ->
                    profileErrorMessage = null
                    val result = profileStore.updateActiveProfile(
                        newUsername = username,
                        newAvatarId = avatarId,
                        newFavoriteTeam = favTeam,
                        newPassword = newPassword
                    )
                    result.fold(
                        onSuccess = { updated ->
                            activeProfile = updated
                            savedUsernames = profileStore.listSavedUsernames()
                            true
                        },
                        onFailure = { err ->
                            profileErrorMessage = err.message ?: "Could not update profile"
                            false
                        }
                    )
                },
                onRecordMatchOutcome = { won, wonTrophy ->
                    profileStore.recordMatchOutcome(won = won, wonTrophy = wonTrophy)?.let { updated ->
                        activeProfile = updated
                    }
                },
                onSignOutToWelcome = {
                    profileStore.signOutActiveSession()
                    activeProfile = null
                    profileErrorMessage = null
                    authErrorMessage = null
                    enteredArena = false
                },
                onConnectCloudSync = triggerOptionalGoogleSignIn,
                onDisconnectCloudSync = {
                    signOutFromGoogle(context, credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun CricBidArenaFlow(
    sessionKey: String,
    currentUserId: String,
    profile: AccountProfile,
    saveBackupManager: LocalSaveBackupManager,
    initialRecentRoomCodes: List<String>,
    onRememberRoomCode: (String) -> Unit,
    profileErrorMessage: String?,
    isCloudConnected: Boolean,
    forceOfflineGuestMode: Boolean,
    onProfileRestored: (AccountProfile) -> Unit,
    onSaveProfile: (username: String, avatarId: String, favoriteTeam: String, newPassword: String?) -> Boolean,
    onRecordMatchOutcome: (won: Boolean, wonTrophy: Boolean) -> Unit,
    onSignOutToWelcome: () -> Unit,
    onConnectCloudSync: () -> Unit,
    onDisconnectCloudSync: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val matchResumeStore = remember(context) { runCatching { LocalMatchResumeStore(context) }.getOrNull() }
    val trophyCabinetStore = remember(context) { runCatching { LocalTrophyCabinetStore(context) }.getOrNull() }

    val viewModel: CricBidViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource: AuctionDataSource = if (forceOfflineGuestMode) {
                    LocalAuctionRepository(app, currentUserId)
                } else {
                    try {
                        val db = FirebaseSafeInit.safeFirestore(app)
                        val firebaseAuth = FirebaseSafeInit.safeFirebaseAuth(app)
                        if (db != null && firebaseAuth != null) {
                            AuctionRepository(
                                db = db,
                                activeActorId = currentUserId,
                                initialRecentRoomCodes = initialRecentRoomCodes,
                                onRememberRoomCode = onRememberRoomCode,
                                auth = firebaseAuth
                            )
                        } else {
                            LocalAuctionRepository(app, currentUserId)
                        }
                    } catch (t: Throwable) {
                        Log.w(MAIN_ACTIVITY_TAG, "Falling back to LocalAuctionRepository: ${t.message}")
                        LocalAuctionRepository(app, currentUserId)
                    }
                }
                CricBidViewModel(
                    repository = dataSource,
                    currentUserId = currentUserId,
                    currentUserDisplayName = profile.username
                )
            }
        }
    )

    val matchSimViewModel: MatchSimViewModel = viewModel(key = "match_sim_$sessionKey")
    val internationalViewModel: InternationalViewModel = viewModel(
        key = "intl_$sessionKey",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                InternationalViewModel(
                    store = LocalInternationalStore(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val careerViewModel: CareerViewModel = viewModel(
        key = "career_$sessionKey",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                CareerViewModel(store = LocalCareerStore(app))
            }
        }
    )

    var inInternationalHub by rememberSaveable { mutableStateOf(false) }
    var inCareerMode by rememberSaveable { mutableStateOf(false) }
    var inProfileScreen by rememberSaveable { mutableStateOf(false) }
    var showTrophyCabinet by rememberSaveable { mutableStateOf(false) }
    var trophyCabinetInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var viewingStandingsMode by rememberSaveable { mutableStateOf<String?>(null) } // "IPL" or "INTERNATIONAL"
    var activeMatchIsInternational by rememberSaveable { mutableStateOf(false) }
    var activeCareerMatchType by rememberSaveable { mutableStateOf<CareerMatchType?>(null) }
    var careerMatchRecordedKey by remember { mutableStateOf<String?>(null) }
    var recordedMatchStatKeys by remember { mutableStateOf(emptySet<String>()) }
    var savedMatchSnapshot by remember(sessionKey) {
        mutableStateOf(runCatching { matchResumeStore?.loadOngoingMatch() }.getOrNull())
    }
    var lastActiveRoomCode by remember(sessionKey) {
        mutableStateOf(runCatching { matchResumeStore?.loadLastActiveRoomCode() }.getOrNull())
    }
    var trophyCabinetData by remember(sessionKey) {
        mutableStateOf(runCatching { trophyCabinetStore?.loadCabinet() }.getOrNull() ?: TrophyCabinetData())
    }

    val myRoomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val activeState by viewModel.activeState.collectAsStateWithLifecycle()
    val matchSimState by matchSimViewModel.uiState.collectAsStateWithLifecycle()
    val internationalState by internationalViewModel.uiState.collectAsStateWithLifecycle()
    val careerState by careerViewModel.uiState.collectAsStateWithLifecycle()

    val iplLeaderboards = remember(activeState.matches) {
        SeasonAwardsEngine.buildLeaderboards(activeState.matches)
    }
    val intlLeaderboards = remember(
        internationalState.campaign.worldCupMatches,
        internationalState.campaign.seriesMatches
    ) {
        SeasonAwardsEngine.buildLeaderboards(
            internationalState.campaign.worldCupMatches + internationalState.campaign.seriesMatches
        )
    }

    // Auto-save ongoing match locally so relaunching can resume right where the player left off
    LaunchedEffect(
        matchSimState.match,
        matchSimState.playFilter,
        matchSimState.showPlayModeSetup,
        matchSimState.careerPlayerId,
        matchSimState.careerPlayerName,
        matchSimState.careerControlMode,
        activeMatchIsInternational,
        activeCareerMatchType,
        activeState.activeRoomCode
    ) {
        val m = matchSimState.match
        if (m != null) {
            if (m.finished) {
                matchResumeStore?.clearOngoingMatch()
                savedMatchSnapshot = null
            } else if (!matchSimState.showPlayModeSetup || m.innings1.legalBalls > 0 || m.innings2.legalBalls > 0) {
                val src = when {
                    activeCareerMatchType != null -> SavedMatchSource.CAREER
                    activeMatchIsInternational -> SavedMatchSource.INTERNATIONAL
                    else -> SavedMatchSource.IPL_ROOM
                }
                val snap = SavedMatchResumeSnapshot(
                    match = m,
                    userTeamId = matchSimState.userTeamId,
                    playFilter = matchSimState.playFilter,
                    source = src,
                    roomCode = activeState.activeRoomCode,
                    careerMatchType = activeCareerMatchType,
                    careerPlayerId = matchSimState.careerPlayerId,
                    careerPlayerName = matchSimState.careerPlayerName,
                    careerControlMode = matchSimState.careerControlMode
                )
                matchResumeStore?.saveOngoingMatch(snap)
                savedMatchSnapshot = snap
            }
        }
    }

    // Auto-save last active auction room code for quick resume
    LaunchedEffect(activeState.activeRoomCode) {
        val code = activeState.activeRoomCode
        if (!code.isNullOrBlank()) {
            matchResumeStore?.saveLastActiveRoomCode(code)
            lastActiveRoomCode = code
        }
    }

    LaunchedEffect(activeState.snackbarMessage) {
        val msg = activeState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(matchSimState.statusNotice) {
        val notice = matchSimState.statusNotice
        if (!notice.isNullOrBlank()) {
            snackbarHostState.showSnackbar(notice)
            matchSimViewModel.clearStatusNotice()
        }
    }

    LaunchedEffect(internationalState.snackbarMessage) {
        val msg = internationalState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            internationalViewModel.clearSnackbar()
        }
    }

    LaunchedEffect(careerState.snackbarMessage) {
        val msg = careerState.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            careerViewModel.clearSnackbar()
        }
    }

    Scaffold(
        containerColor = NavyMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        val activeMatch = matchSimState.match
        val activeRoom = activeState.room

        when {
            // 1. Live Career Mode Ball-by-Ball Match Simulation
            activeMatch != null && activeCareerMatchType != null -> {
                val careerMatchType = activeCareerMatchType!!
                val onFinishCareerMatch: () -> Unit = {
                    val m = matchSimViewModel.uiState.value.match
                    if (m != null && m.finished) {
                        val key = "career_${m.teamA.teamName}_${m.teamB.teamName}_${m.innings1.totalRuns}_${m.innings2.totalRuns}_${m.resultSummary}"
                        if (careerMatchRecordedKey != key) {
                            careerMatchRecordedKey = key
                            careerViewModel.recordLiveMatchResult(m, careerMatchType)
                            val won = m.winnerTeamId == "career_user_team"
                            val wonTrophy = won && careerMatchType.isWorldChampionship
                            onRecordMatchOutcome(won, wonTrophy)
                            if (careerMatchType.isWorldChampionship && trophyCabinetStore != null) {
                                val runnerUp = if (m.winnerTeamId == m.teamA.teamId) m.teamB.teamName else m.teamA.teamName
                                val winner = if (m.winnerTeamId == m.teamA.teamId) m.teamA.teamName else m.teamB.teamName
                                trophyCabinetData = trophyCabinetStore.recordFinalOutcome(
                                    TrophyCabinetRecord(
                                        id = key,
                                        title = if (careerMatchType == CareerMatchType.ODI_WORLD_CUP) {
                                            "Career 50-Over World Championship"
                                        } else {
                                            "Career T20 World Championship"
                                        },
                                        competitionType = "CAREER",
                                        championTeamName = winner,
                                        runnerUpTeamName = runnerUp,
                                        userWon = won,
                                        summary = m.resultSummary,
                                        mvpWinner = m.playerOfTheMatch
                                    )
                                )
                            }
                        }
                    }
                }
                LaunchedEffect(activeMatch.finished) {
                    if (activeMatch.finished) onFinishCareerMatch()
                }
                MatchSimScreen(
                    match = activeMatch,
                    autoPlaying = matchSimState.autoPlaying,
                    speed = matchSimState.speed,
                    onBack = {
                        matchSimViewModel.closeMatch()
                        activeCareerMatchType = null
                    },
                    onNextBall = { matchSimViewModel.nextBall { onFinishCareerMatch() } },
                    onNextOver = { matchSimViewModel.nextOver { onFinishCareerMatch() } },
                    onNextFiveOvers = { matchSimViewModel.nextFiveOvers { onFinishCareerMatch() } },
                    onSimInnings = { matchSimViewModel.simInnings { onFinishCareerMatch() } },
                    onSimFullMatch = { matchSimViewModel.simFullMatch { onFinishCareerMatch() } },
                    onToggleAutoPlay = { matchSimViewModel.toggleAutoPlay { onFinishCareerMatch() } },
                    onCycleSpeed = { matchSimViewModel.cycleSpeed() },
                    onApplyImpactSub = { teamId, benchId, outId ->
                        matchSimViewModel.applyImpactSub(teamId, benchId, outId)
                    },
                    onSelectBowler = { bowlerId ->
                        matchSimViewModel.selectBowlerForNextOver(bowlerId)
                    },
                    lastActionSnapshot = matchSimState.lastActionSnapshot,
                    onPlayInteractiveBall = { input ->
                        matchSimViewModel.playInteractiveBall(input) { onFinishCareerMatch() }
                    },
                    userTeamId = matchSimState.userTeamId,
                    playFilter = matchSimState.playFilter,
                    showPlayModeSetup = matchSimState.showPlayModeSetup,
                    autoSimmingSkippedPhase = matchSimState.autoSimmingSkippedPhase,
                    onConfirmPlayModeSetup = { filter, pitch, weather, dew, controlMode ->
                        matchSimViewModel.confirmPlayModeSetup(filter, pitch, weather, dew, controlMode) { onFinishCareerMatch() }
                    },
                    onChangePlayFilter = { filter ->
                        matchSimViewModel.setPlayFilter(filter) { onFinishCareerMatch() }
                    },
                    careerPlayerId = matchSimState.careerPlayerId,
                    careerPlayerName = matchSimState.careerPlayerName,
                    careerControlMode = matchSimState.careerControlMode,
                    autoJumpLandscapeSignal = matchSimState.autoJumpLandscapeSignal,
                    onChangeCareerControlMode = { mode ->
                        matchSimViewModel.setCareerControlMode(mode) { onFinishCareerMatch() }
                    },
                    onSkipToMyTurn = {
                        matchSimViewModel.skipToCareerPlayerTurn { onFinishCareerMatch() }
                    },
                    showDrsOverlay = matchSimState.showDrsOverlay,
                    onSelectFieldPreset = { preset -> matchSimViewModel.setFieldPreset(preset) },
                    onStartDrsReview = { matchSimViewModel.startDrsReview() },
                    onCompleteDrsReview = { matchSimViewModel.completeDrsReview { onFinishCareerMatch() } },
                    onDismissDrs = { matchSimViewModel.dismissDrs() },
                    modifier = contentModifier
                )
            }

            // 1A. Live International Ball-by-Ball Match Simulation (T20I / ODI / Test + Super Over + Bowler Rotation)
            activeMatch != null && activeMatchIsInternational -> {
                val onFinishInternational: () -> Unit = {
                    matchSimViewModel.persistInternationalResultIfNeeded(
                        currentUid = currentUserId,
                        onSaveRecord = { rec ->
                            internationalViewModel.recordMatchResult(rec)
                            val statKey = "intl_${rec.teamAId}_${rec.teamBId}_${rec.teamAScore}_${rec.teamBScore}_${rec.resultSummary}"
                            if (statKey !in recordedMatchStatKeys) {
                                recordedMatchStatKeys = recordedMatchStatKeys + statKey
                                val userNationId = internationalState.campaign.userNationId
                                val won = rec.winnerTeamId == userNationId
                                val isFinal = rec.matchStage == MatchStage.FINAL || rec.matchStage == MatchStage.WTC_FINAL
                                val wonTrophy = won && isFinal
                                onRecordMatchOutcome(won, wonTrophy)
                                if (isFinal && trophyCabinetStore != null) {
                                    val updatedMatches = internationalState.campaign.worldCupMatches + rec
                                    val lb = SeasonAwardsEngine.buildLeaderboards(updatedMatches)
                                    val runnerUp = if (rec.winnerTeamId == rec.teamAId) rec.teamBName else rec.teamAName
                                    val winner = if (rec.winnerTeamId == rec.teamAId) rec.teamAName else rec.teamBName
                                    trophyCabinetData = trophyCabinetStore.recordFinalOutcome(
                                        TrophyCabinetRecord(
                                            id = statKey,
                                            title = "${internationalState.campaign.format.shortLabel} World Cup Final",
                                            competitionType = "WORLD_CUP",
                                            championTeamName = winner,
                                            runnerUpTeamName = runnerUp,
                                            userWon = won,
                                            summary = rec.resultSummary,
                                            orangeCapWinner = lb.orangeCapHolder?.let { "${it.playerName} (${it.runs})" }.orEmpty(),
                                            purpleCapWinner = lb.purpleCapHolder?.let { "${it.playerName} (${it.wickets})" }.orEmpty(),
                                            mvpWinner = lb.mvpHolder?.playerName ?: rec.playerOfTheMatch
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
                LaunchedEffect(activeMatch.finished) {
                    if (activeMatch.finished) onFinishInternational()
                }
                MatchSimScreen(
                    match = activeMatch,
                    autoPlaying = matchSimState.autoPlaying,
                    speed = matchSimState.speed,
                    onBack = {
                        matchSimViewModel.closeMatch()
                        activeMatchIsInternational = false
                    },
                    onNextBall = { matchSimViewModel.nextBall { onFinishInternational() } },
                    onNextOver = { matchSimViewModel.nextOver { onFinishInternational() } },
                    onNextFiveOvers = { matchSimViewModel.nextFiveOvers { onFinishInternational() } },
                    onSimInnings = { matchSimViewModel.simInnings { onFinishInternational() } },
                    onSimFullMatch = { matchSimViewModel.simFullMatch { onFinishInternational() } },
                    onToggleAutoPlay = { matchSimViewModel.toggleAutoPlay { onFinishInternational() } },
                    onCycleSpeed = { matchSimViewModel.cycleSpeed() },
                    onApplyImpactSub = { teamId, benchId, outId ->
                        matchSimViewModel.applyImpactSub(teamId, benchId, outId)
                    },
                    onSelectBowler = { bowlerId ->
                        matchSimViewModel.selectBowlerForNextOver(bowlerId)
                    },
                    lastActionSnapshot = matchSimState.lastActionSnapshot,
                    onPlayInteractiveBall = { input ->
                        matchSimViewModel.playInteractiveBall(input) { onFinishInternational() }
                    },
                    userTeamId = matchSimState.userTeamId,
                    playFilter = matchSimState.playFilter,
                    showPlayModeSetup = matchSimState.showPlayModeSetup,
                    autoSimmingSkippedPhase = matchSimState.autoSimmingSkippedPhase,
                    onConfirmPlayModeSetup = { filter, pitch, weather, dew, _ ->
                        matchSimViewModel.confirmPlayModeSetup(filter, pitch, weather, dew) { onFinishInternational() }
                    },
                    onChangePlayFilter = { filter ->
                        matchSimViewModel.setPlayFilter(filter) { onFinishInternational() }
                    },
                    showDrsOverlay = matchSimState.showDrsOverlay,
                    onSelectFieldPreset = { preset -> matchSimViewModel.setFieldPreset(preset) },
                    onStartDrsReview = { matchSimViewModel.startDrsReview() },
                    onCompleteDrsReview = { matchSimViewModel.completeDrsReview { onFinishInternational() } },
                    onDismissDrs = { matchSimViewModel.dismissDrs() },
                    orangeCapPlayerId = intlLeaderboards.orangeCapPlayerId,
                    purpleCapPlayerId = intlLeaderboards.purpleCapPlayerId,
                    modifier = contentModifier
                )
            }

            // 1B. Live Auction Room Ball-by-Ball Match Simulation (Pomp-Style + Impact Player + Super Over + Bowler Rotation)
            activeMatch != null && activeRoom != null -> {
                val onFinishPersist: () -> Unit = {
                    matchSimViewModel.persistResultIfNeeded(
                        room = activeRoom,
                        currentUid = currentUserId,
                        saveAction = { rec ->
                            val res = viewModel.saveCompletedMatch(rec)
                            val statKey = "room_${activeRoom.roomCode}_${rec.teamAId}_${rec.teamBId}_${rec.teamAScore}_${rec.teamBScore}"
                            if (statKey !in recordedMatchStatKeys) {
                                recordedMatchStatKeys = recordedMatchStatKeys + statKey
                                val myTeam = activeRoom.teamForUser(currentUserId)
                                val myTeamId = myTeam?.teamId
                                if (myTeamId != null && (rec.teamAId == myTeamId || rec.teamBId == myTeamId)) {
                                    val won = rec.winnerTeamId == myTeamId
                                    val wonTrophy = won && rec.matchStage == MatchStage.FINAL
                                    onRecordMatchOutcome(won, wonTrophy)
                                }
                                if (rec.matchStage == MatchStage.FINAL && trophyCabinetStore != null) {
                                    val updatedMatches = activeState.matches + rec
                                    val lb = SeasonAwardsEngine.buildLeaderboards(updatedMatches)
                                    val winner = if (rec.winnerTeamId == rec.teamAId) rec.teamAName else rec.teamBName
                                    val runnerUp = if (rec.winnerTeamId == rec.teamAId) rec.teamBName else rec.teamAName
                                    val userWonFinal = myTeamId != null && rec.winnerTeamId == myTeamId
                                    trophyCabinetData = trophyCabinetStore.recordFinalOutcome(
                                        TrophyCabinetRecord(
                                            id = statKey,
                                            title = "CricBid IPL Final (Room ${activeRoom.roomCode})",
                                            competitionType = "IPL",
                                            championTeamName = winner,
                                            runnerUpTeamName = runnerUp,
                                            userWon = userWonFinal,
                                            summary = rec.resultSummary,
                                            orangeCapWinner = lb.orangeCapHolder?.let { "${it.playerName} (${it.runs})" }.orEmpty(),
                                            purpleCapWinner = lb.purpleCapHolder?.let { "${it.playerName} (${it.wickets})" }.orEmpty(),
                                            mvpWinner = lb.mvpHolder?.playerName ?: rec.playerOfTheMatch
                                        )
                                    )
                                    if (myTeam != null) {
                                        trophyCabinetStore.saveLastSeasonSquad(myTeam.teamName, myTeam.squadPlayerIds)
                                    }
                                }
                            }
                            res
                        }
                    )
                }
                LaunchedEffect(activeMatch.finished) {
                    if (activeMatch.finished) onFinishPersist()
                }
                MatchSimScreen(
                    match = activeMatch,
                    autoPlaying = matchSimState.autoPlaying,
                    speed = matchSimState.speed,
                    onBack = { matchSimViewModel.closeMatch() },
                    onNextBall = { matchSimViewModel.nextBall { onFinishPersist() } },
                    onNextOver = { matchSimViewModel.nextOver { onFinishPersist() } },
                    onNextFiveOvers = { matchSimViewModel.nextFiveOvers { onFinishPersist() } },
                    onSimInnings = { matchSimViewModel.simInnings { onFinishPersist() } },
                    onSimFullMatch = { matchSimViewModel.simFullMatch { onFinishPersist() } },
                    onToggleAutoPlay = { matchSimViewModel.toggleAutoPlay { onFinishPersist() } },
                    onCycleSpeed = { matchSimViewModel.cycleSpeed() },
                    onApplyImpactSub = { teamId, benchId, outId ->
                        matchSimViewModel.applyImpactSub(teamId, benchId, outId)
                    },
                    onSelectBowler = { bowlerId ->
                        matchSimViewModel.selectBowlerForNextOver(bowlerId)
                    },
                    lastActionSnapshot = matchSimState.lastActionSnapshot,
                    onPlayInteractiveBall = { input ->
                        matchSimViewModel.playInteractiveBall(input) { onFinishPersist() }
                    },
                    userTeamId = matchSimState.userTeamId,
                    playFilter = matchSimState.playFilter,
                    showPlayModeSetup = matchSimState.showPlayModeSetup,
                    autoSimmingSkippedPhase = matchSimState.autoSimmingSkippedPhase,
                    onConfirmPlayModeSetup = { filter, pitch, weather, dew, _ ->
                        matchSimViewModel.confirmPlayModeSetup(filter, pitch, weather, dew) { onFinishPersist() }
                    },
                    onChangePlayFilter = { filter ->
                        matchSimViewModel.setPlayFilter(filter) { onFinishPersist() }
                    },
                    showDrsOverlay = matchSimState.showDrsOverlay,
                    onSelectFieldPreset = { preset -> matchSimViewModel.setFieldPreset(preset) },
                    onStartDrsReview = { matchSimViewModel.startDrsReview() },
                    onCompleteDrsReview = { matchSimViewModel.completeDrsReview { onFinishPersist() } },
                    onDismissDrs = { matchSimViewModel.dismissDrs() },
                    orangeCapPlayerId = iplLeaderboards.orangeCapPlayerId,
                    purpleCapPlayerId = iplLeaderboards.purpleCapPlayerId,
                    modifier = contentModifier
                )
            }

            // 1C. Trophy Cabinet & Season Leaderboards (Orange Cap / Purple Cap / MVP)
            showTrophyCabinet -> {
                val activeLb = when {
                    activeRoom != null -> iplLeaderboards
                    inInternationalHub -> intlLeaderboards
                    iplLeaderboards.hasData -> iplLeaderboards
                    else -> intlLeaderboards
                }
                val compTitle = when {
                    activeRoom != null -> "IPL Season • Room ${activeRoom.roomCode}"
                    inInternationalHub -> "${internationalState.campaign.format.shortLabel} International Season"
                    else -> "Active Tournament Caps"
                }
                TrophyCabinetScreen(
                    cabinet = trophyCabinetData,
                    seasonLeaderboards = activeLb,
                    activeCompetitionTitle = compTitle,
                    initialTab = trophyCabinetInitialTab,
                    onBack = { showTrophyCabinet = false },
                    modifier = contentModifier
                )
            }

            // 2A. Manager Profile, Indian Player Avatars & Full Save Backup/Restore Screen
            inProfileScreen -> {
                val fullBackupCode = remember(profile, careerState.activeCareer, careerState.hallOfFame, trophyCabinetData) {
                    runCatching { saveBackupManager.exportFullBackupCode(profile) }.getOrDefault("")
                }
                val backupSummaryLine = remember(profile, careerState.activeCareer, careerState.hallOfFame, trophyCabinetData) {
                    runCatching { saveBackupManager.buildBackupSummaryLine(profile) }.getOrDefault("")
                }
                ProfileScreen(
                    profile = profile,
                    backupCode = fullBackupCode,
                    backupSummaryLine = backupSummaryLine,
                    isCloudConnected = isCloudConnected,
                    errorMessage = profileErrorMessage,
                    onBack = { inProfileScreen = false },
                    onSaveProfile = { newUsername, newAvatarId, newFavTeam, newPassword ->
                        val ok = onSaveProfile(newUsername, newAvatarId, newFavTeam, newPassword)
                        if (ok) {
                            viewModel.showMessage("Profile saved! Avatar & credentials updated.")
                        }
                    },
                    onCopyBackupCode = {
                        viewModel.showMessage("Full Save Backup Code (CB2-...) copied! Includes Account, Career & Trophies.")
                    },
                    onExportBackupJson = {
                        saveBackupManager.exportFullBackupJson(profile)
                    },
                    onImportBackupPayload = { rawPayload ->
                        saveBackupManager.importBackup(rawPayload).fold(
                            onSuccess = { summary ->
                                onProfileRestored(summary.profile)
                                careerViewModel.reloadFromStore()
                                trophyCabinetData = runCatching { trophyCabinetStore?.loadCabinet() }.getOrNull() ?: trophyCabinetData
                                viewModel.showMessage("✓ ${summary.toUserMessage()}")
                            },
                            onFailure = { err ->
                                viewModel.showMessage(err.message ?: "Could not restore backup save")
                            }
                        )
                    },
                    onExportFileResult = { msg -> viewModel.showMessage(msg) },
                    onConnectCloudSync = onConnectCloudSync,
                    onSignOutToWelcome = {
                        viewModel.closeRoom()
                        matchSimViewModel.closeMatch()
                        inProfileScreen = false
                        inInternationalHub = false
                        inCareerMode = false
                        viewingStandingsMode = null
                        activeMatchIsInternational = false
                        activeCareerMatchType = null
                        onSignOutToWelcome()
                    },
                    modifier = contentModifier
                )
            }

            // 2A-2. Dedicated Dynamic Tournament Standings Screen (IPL Room or International)
            viewingStandingsMode == "IPL" && activeRoom != null -> {
                val overview = remember(activeRoom.teams, activeState.matches) {
                    com.example.engine.TournamentEngine.buildOverview(activeRoom.teams, activeState.matches)
                }
                val myTeamId = activeRoom.teamForUser(currentUserId)?.teamId
                val matchesDone = if (overview.isSeriesMode) {
                    overview.seriesSummary?.matchesPlayed ?: 0
                } else {
                    activeState.matches.count { it.matchStage == MatchStage.LEAGUE }
                }
                val totalScheduled = if (overview.isSeriesMode) 5 else overview.allLeagueFixtures.size
                StandingsScreen(
                    tournamentTitle = if (overview.isSeriesMode) "5-Match T20 Series" else "IPL League Table",
                    tournamentSubtitle = "Room ${activeRoom.roomCode} • ${activeRoom.teams.size} Franchises",
                    standings = overview.standings,
                    teams = activeRoom.teams,
                    matchesCompleted = matchesDone,
                    totalScheduledMatches = totalScheduled,
                    highlightTeamId = myTeamId,
                    qualifierCount = if (!overview.isSeriesMode && activeRoom.teams.size >= 4) 4 else 0,
                    winPoints = 2,
                    tiePoints = 1,
                    tieColumnLabel = "T",
                    championTeamName = overview.championTeamName,
                    onBack = { viewingStandingsMode = null },
                    modifier = contentModifier
                )
            }

            viewingStandingsMode == "INTERNATIONAL" && inInternationalHub -> {
                val campaign = internationalState.campaign
                val allNations = remember(campaign.userNationId) {
                    com.example.data.model.InternationalNations.buildTeamStates(campaign.userNationId)
                }
                val isWorldCup = campaign.mode == com.example.data.model.InternationalMode.WORLD_CUP
                if (isWorldCup) {
                    val wcOverview = remember(campaign, allNations) {
                        com.example.engine.InternationalTournamentEngine.buildWorldCupOverview(
                            format = campaign.format,
                            nations = allNations,
                            matches = campaign.worldCupMatches
                        )
                    }
                    val isTest = campaign.format == com.example.data.model.MatchFormat.TEST
                    StandingsScreen(
                        tournamentTitle = wcOverview.title,
                        tournamentSubtitle = "${campaign.format.title} • 8 ICC Nations",
                        standings = wcOverview.standings,
                        teams = allNations,
                        matchesCompleted = campaign.worldCupMatches.count { it.matchStage == MatchStage.LEAGUE },
                        totalScheduledMatches = wcOverview.allGroupFixtures.size,
                        highlightTeamId = campaign.userNationId,
                        qualifierCount = wcOverview.qualifierCount,
                        winPoints = if (isTest) 12 else 2,
                        tiePoints = if (isTest) 4 else 1,
                        tieColumnLabel = if (isTest) "D" else "T",
                        championTeamName = wcOverview.championTeamName,
                        onBack = { viewingStandingsMode = null },
                        modifier = contentModifier
                    )
                } else {
                    val userTeam = allNations.firstOrNull { it.teamId == campaign.userNationId } ?: allNations.first()
                    val oppTeam = allNations.firstOrNull { it.teamId == campaign.opponentNationId }
                        ?: allNations.first { it.teamId != userTeam.teamId }
                    val seriesTeams = listOf(userTeam, oppTeam)
                    val seriesSummary = remember(campaign, userTeam, oppTeam) {
                        com.example.engine.InternationalTournamentEngine.buildCustomSeriesSummary(
                            format = campaign.format,
                            teamA = userTeam,
                            teamB = oppTeam,
                            totalMatches = campaign.seriesTotalMatches,
                            matches = campaign.seriesMatches
                        )
                    }
                    val seriesStandings = remember(campaign, seriesTeams) {
                        com.example.engine.InternationalTournamentEngine.computeInternationalStandings(
                            format = campaign.format,
                            nations = seriesTeams,
                            matches = campaign.seriesMatches
                        )
                    }
                    val isTest = campaign.format == com.example.data.model.MatchFormat.TEST
                    StandingsScreen(
                        tournamentTitle = "${campaign.format.shortLabel} Bilateral Series",
                        tournamentSubtitle = "${userTeam.teamName} vs ${oppTeam.teamName} (${campaign.seriesTotalMatches} Matches)",
                        standings = seriesStandings,
                        teams = seriesTeams,
                        matchesCompleted = seriesSummary.matchesPlayed,
                        totalScheduledMatches = campaign.seriesTotalMatches,
                        highlightTeamId = campaign.userNationId,
                        qualifierCount = 0,
                        winPoints = if (isTest) 12 else 2,
                        tiePoints = if (isTest) 4 else 1,
                        tieColumnLabel = if (isTest) "D" else "T",
                        championTeamName = seriesSummary.seriesWinnerName,
                        onBack = { viewingStandingsMode = null },
                        modifier = contentModifier
                    )
                }
            }

            // 2B. International Cricket Hub (T20I / ODI / Test • Series 1/3/5/7 & World Cup / WTC)
            inInternationalHub -> {
                InternationalScreen(
                    campaign = internationalState.campaign,
                    busy = internationalState.busy,
                    onBack = {
                        viewingStandingsMode = null
                        inInternationalHub = false
                    },
                    onSelectFormat = { fmt -> internationalViewModel.selectFormat(fmt) },
                    onSelectMode = { mode -> internationalViewModel.selectMode(mode) },
                    onSelectUserNation = { id -> internationalViewModel.selectUserNation(id) },
                    onSelectOpponentNation = { id -> internationalViewModel.selectOpponentNation(id) },
                    onSelectSeriesLength = { count -> internationalViewModel.selectSeriesLength(count) },
                    onStartMatch = { teamA, teamB, stage, format ->
                        activeMatchIsInternational = true
                        matchSimViewModel.startNewMatch(
                            teamA = teamA,
                            teamB = teamB,
                            stage = stage,
                            format = format,
                            userTeamId = internationalState.campaign.userNationId
                        )
                    },
                    onQuickSimWorldCup = { fixtures -> internationalViewModel.quickSimWorldCupFixtures(fixtures) },
                    onOpenStandings = { viewingStandingsMode = "INTERNATIONAL" },
                    onResetCampaign = { internationalViewModel.resetCurrentModeCampaign() },
                    modifier = contentModifier
                )
            }

            // 2B-2. Single-Player Career Mode (Cricket 26-style Road to Legend + Hall of Fame)
            inCareerMode -> {
                CareerScreen(
                    uiState = careerState,
                    onBack = { inCareerMode = false },
                    onCreateCareer = { name, age, nat, code, avId, role, batS, bowlS, diff, appearance ->
                        careerViewModel.createRookieCareer(name, age, nat, code, avId, role, batS, bowlS, diff, appearance)
                    },
                    onChangeDifficulty = { diff -> careerViewModel.updateDifficulty(diff) },
                    onRunDrill = { drill -> careerViewModel.runTrainingDrill(drill) },
                    onPromoteToDomestic = { careerViewModel.promoteToDomestic() },
                    onEnterIplAuction = { league -> careerViewModel.enterIplAuction(league) },
                    onSelectProLeague = { league -> careerViewModel.selectProLeague(league) },
                    onDismissAuctionDraft = { careerViewModel.dismissAuctionDraftSummary() },
                    onAcceptIntlCallUp = { careerViewModel.acceptInternationalCallUp() },
                    onSpendSkillPoint = { stat -> careerViewModel.spendSkillPoint(stat) },
                    onRunSelectionTrial = { careerViewModel.completeSelectionTrial() },
                    onRefreshObjective = { mt -> careerViewModel.refreshObjectiveForMatchType(mt) },
                    onPlayLiveMatch = { matchType, controlMode ->
                        val prepared = careerViewModel.prepareLiveMatch(matchType)
                        val activeCareer = careerState.activeCareer
                        if (prepared != null) {
                            val (myTeam, oppTeam, oppOffset) = prepared
                            careerMatchRecordedKey = null
                            activeCareerMatchType = matchType
                            activeMatchIsInternational = false
                            matchSimViewModel.startNewMatch(
                                teamA = myTeam,
                                teamB = oppTeam,
                                stage = matchType.stage,
                                format = matchType.format,
                                opponentRatingOffset = oppOffset,
                                userTeamId = myTeam.teamId,
                                careerPlayerId = activeCareer?.id,
                                careerPlayerName = activeCareer?.name,
                                careerControlMode = controlMode
                            )
                        }
                    },
                    onQuickSimMatch = { matchType -> careerViewModel.quickSimCareerMatch(matchType) },
                    onRetireToHallOfFame = { careerViewModel.retireActiveCareerToHallOfFame() },
                    onDismissRetirement = { careerViewModel.dismissRetirementSummary() },
                    onToggleLegendInAuctions = { id, include -> careerViewModel.toggleLegendInAuctions(id, include) },
                    modifier = contentModifier
                )
            }

            // 2C. Home Room Creator / Joiner when no active room selected
            activeState.activeRoomCode == null -> {
                val resumableAuctionRoom = remember(myRoomsState, lastActiveRoomCode) {
                    val rooms = (myRoomsState as? com.example.viewmodel.UiState.Success)?.data.orEmpty()
                    rooms.firstOrNull {
                        it.roomCode == lastActiveRoomCode &&
                            (it.roomStatus == RoomStatus.AUCTION || it.roomStatus == RoomStatus.RETENTION)
                    } ?: rooms.firstOrNull {
                        it.roomStatus == RoomStatus.AUCTION || it.roomStatus == RoomStatus.RETENTION
                    }
                }
                HomeLobbyScreen(
                    profile = profile,
                    isCloudConnected = isCloudConnected,
                    roomsState = myRoomsState,
                    busy = activeState.busy,
                    joinStatusError = activeState.joinStatusError,
                    joinStatusInfo = activeState.joinStatusInfo,
                    onCreateRoom = { name -> viewModel.createRoom(name) },
                    onJoinRoom = { code, name -> viewModel.joinRoom(code, name) },
                    onOpenRoom = { code -> viewModel.openRoom(code) },
                    onOpenInternational = { inInternationalHub = true },
                    onOpenCareerMode = { inCareerMode = true },
                    onOpenProfile = { inProfileScreen = true },
                    onClearJoinStatus = { viewModel.clearJoinStatus() },
                    onConnectCloudSync = onConnectCloudSync,
                    onDisconnectCloudSync = {
                        viewModel.closeRoom()
                        matchSimViewModel.closeMatch()
                        activeMatchIsInternational = false
                        activeCareerMatchType = null
                        inInternationalHub = false
                        inCareerMode = false
                        inProfileScreen = false
                        showTrophyCabinet = false
                        onDisconnectCloudSync()
                    },
                    savedMatchSnapshot = savedMatchSnapshot,
                    onResumeSavedMatch = {
                        val snap = savedMatchSnapshot ?: return@HomeLobbyScreen
                        when (snap.source) {
                            SavedMatchSource.CAREER -> {
                                activeCareerMatchType = snap.careerMatchType ?: CareerMatchType.IPL_T20
                                activeMatchIsInternational = false
                                inCareerMode = true
                            }
                            SavedMatchSource.INTERNATIONAL -> {
                                activeMatchIsInternational = true
                                activeCareerMatchType = null
                                inInternationalHub = true
                            }
                            SavedMatchSource.IPL_ROOM -> {
                                activeMatchIsInternational = false
                                activeCareerMatchType = null
                                snap.roomCode?.let { viewModel.openRoom(it) }
                            }
                        }
                        matchSimViewModel.resumeSavedMatch(snap)
                    },
                    onDiscardSavedMatch = {
                        matchResumeStore?.clearOngoingMatch()
                        savedMatchSnapshot = null
                    },
                    trophyCabinet = trophyCabinetData,
                    onOpenTrophyCabinet = {
                        trophyCabinetInitialTab = 0
                        showTrophyCabinet = true
                    },
                    resumableRoom = resumableAuctionRoom,
                    activeCareer = careerState.activeCareer,
                    modifier = contentModifier
                )
            }

            // 3. Loading active room snapshot
            activeRoom == null -> {
                Box(
                    modifier = contentModifier.background(NavyMidnight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TurfEmerald)
                }
            }

            // 4. Playing XI Selection (or Editing XI from Match Hub)
            activeRoom.roomStatus == RoomStatus.SQUAD_SELECTION || activeState.editingXiOverride -> {
                SquadSelectionScreen(
                    room = activeRoom,
                    currentUid = currentUserId,
                    busy = activeState.busy,
                    onBack = {
                        if (activeState.editingXiOverride) {
                            viewModel.setEditingXi(false)
                        } else {
                            viewModel.closeRoom()
                        }
                    },
                    onSavePlayingXi = { ids, capId -> viewModel.saveMyPlayingXi(ids, capId) },
                    onHostEnterMatchHub = { viewModel.enterMatchHubAsHost() },
                    modifier = contentModifier
                )
            }

            // 5. Waiting Room Lobby
            activeRoom.roomStatus == RoomStatus.LOBBY -> {
                RoomLobbyScreen(
                    room = activeRoom,
                    currentUid = currentUserId,
                    isCloudConnected = isCloudConnected,
                    busy = activeState.busy,
                    onBack = { viewModel.closeRoom() },
                    onCopyCode = { code -> viewModel.showMessage("Copied Room Code $code!") },
                    onToggleAutoFillEightTeams = { enabled -> viewModel.toggleAutoFillEightTeams(enabled) },
                    onAddAiBot = { viewModel.addAiBot() },
                    onRemoveAiBot = { botId -> viewModel.removeAiBot(botId) },
                    onStartAuction = { viewModel.enterRetentionRound() },
                    modifier = contentModifier
                )
            }

            // 6. Marquee Retention Round (Up to 4 Tiered Retentions + Former Squad RTM Eligibility)
            activeRoom.roomStatus == RoomStatus.RETENTION -> {
                RetentionScreen(
                    room = activeRoom,
                    currentUid = currentUserId,
                    busy = activeState.busy,
                    onBack = { viewModel.closeRoom() },
                    onSubmitRetentions = { playerIds -> viewModel.submitMultiRetentions(playerIds) },
                    onSkipRetention = { viewModel.skipMarqueeRetention() },
                    onHostLaunchAuction = { viewModel.beginLiveAuctionFromRetention() },
                    modifier = contentModifier
                )
            }

            // 7. Live Auction Arena (10s Timer + Right to Match Card + Smart AI Fallback)
            activeRoom.roomStatus == RoomStatus.AUCTION -> {
                LiveAuctionScreen(
                    room = activeRoom,
                    currentUid = currentUserId,
                    countdownSeconds = activeState.countdownSeconds,
                    countdownProgress = activeState.countdownProgress,
                    busy = activeState.busy,
                    onBack = { viewModel.closeRoom() },
                    onPlaceBid = { viewModel.placeMyBid() },
                    onUseRtm = { viewModel.useMyRtmCard() },
                    onHammerNow = { viewModel.hammerCurrentLotNow() },
                    onFastForward = { viewModel.fastForwardAuction() },
                    modifier = contentModifier
                )
            }

            // 8. Dynamic Tournament Hub (5-Match Series OR League + NRR + IPL Playoffs)
            else -> {
                MatchHubScreen(
                    room = activeRoom,
                    currentUid = currentUserId,
                    matches = activeState.matches,
                    busy = activeState.busy,
                    onBack = {
                        viewingStandingsMode = null
                        viewModel.closeRoom()
                    },
                    onEditMyXi = { viewModel.setEditingXi(true) },
                    onOpenStandings = { viewingStandingsMode = "IPL" },
                    onStartMatch = { teamA, teamB, stage ->
                        val myTeam = activeRoom.teamForUser(currentUserId)
                        val myActiveTeam = when (myTeam?.teamId) {
                            teamA.teamId -> teamA
                            teamB.teamId -> teamB
                            else -> null
                        }
                        if (myActiveTeam != null) {
                            val check = com.example.data.model.SquadRuleValidator.validatePlayingXi(
                                selectedIds = myActiveTeam.playingXiIds,
                                ownedIds = myActiveTeam.squadPlayerIds,
                                captainPlayerId = myActiveTeam.effectiveCaptainId
                            )
                            if (!check.allowed) {
                                viewModel.showMessage(
                                    check.reason ?: "Max 4 overseas players allowed in Playing XI. Update your XI before starting."
                                )
                                viewModel.setEditingXi(true)
                                return@MatchHubScreen
                            }
                        }
                        activeMatchIsInternational = false
                        matchSimViewModel.startNewMatch(
                            teamA = teamA,
                            teamB = teamB,
                            stage = stage,
                            userTeamId = myActiveTeam?.teamId ?: activeRoom.teamForUser(currentUserId)?.teamId
                        )
                    },
                    onQuickSimLeague = { fixtures -> viewModel.quickSimLeagueMatches(fixtures) },
                    seasonLeaderboards = iplLeaderboards,
                    onOpenTrophyCabinet = {
                        trophyCabinetInitialTab = 1
                        showTrophyCabinet = true
                    },
                    onStartNewSeasonMegaAuction = if (activeRoom.hostUid == currentUserId || !isCloudConnected) {
                        { viewModel.startNewSeasonMegaAuction() }
                    } else null,
                    modifier = contentModifier
                )
            }
        }
    }
}
