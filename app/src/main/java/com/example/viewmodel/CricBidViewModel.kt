package com.example.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuctionDataSource
import com.example.data.model.AuctionRoom
import com.example.data.model.MatchRecord
import com.example.data.model.RoomStatus
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
import com.example.engine.AuctionBotEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ActiveRoomUiState(
    val activeRoomCode: String? = null,
    val room: AuctionRoom? = null,
    val matches: List<MatchRecord> = emptyList(),
    val countdownSeconds: Int = 10,
    val countdownProgress: Float = 1f,
    val busy: Boolean = false,
    val snackbarMessage: String? = null,
    val joinStatusError: String? = null,
    val joinStatusInfo: String? = null,
    val editingXiOverride: Boolean = false
)

class CricBidViewModel(
    private val repository: AuctionDataSource,
    val currentUserId: String,
    val currentUserDisplayName: String
) : ViewModel() {

    val myRoomsState: StateFlow<UiState<List<AuctionRoom>>> = repository.observeMyRooms(currentUserId)
        .map<List<AuctionRoom>, UiState<List<AuctionRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing rooms", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load your rooms"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _activeState = MutableStateFlow(ActiveRoomUiState())
    val activeState: StateFlow<ActiveRoomUiState> = _activeState.asStateFlow()

    private var roomObserverJob: Job? = null
    private var matchesObserverJob: Job? = null
    private var auctionTickerJob: Job? = null
    private var lastBotActionAtMs: Long = 0L
    private var lastFinalizedLotKey: String = ""
    private var lastAutoAdvancedStageKey: String = ""
    private var bidInFlight: Boolean = false
    private var anchoredLotKey: String = ""
    private var anchoredLocalDeadlineElapsedMs: Long = 0L

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserDisplayName)
        }
    }

    fun clearSnackbar() {
        _activeState.update { it.copy(snackbarMessage = null) }
    }

    fun clearJoinStatus() {
        _activeState.update { it.copy(joinStatusError = null, joinStatusInfo = null) }
    }

    fun showMessage(msg: String) {
        _activeState.update { it.copy(snackbarMessage = msg) }
    }

    fun createRoom(teamName: String) {
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true, snackbarMessage = null, joinStatusError = null, joinStatusInfo = null) }
            val result = repository.createRoom(teamName)
            result.fold(
                onSuccess = { code ->
                    _activeState.update { it.copy(busy = false) }
                    openRoom(code)
                },
                onFailure = { err ->
                    _activeState.update {
                        it.copy(busy = false, snackbarMessage = err.message ?: "Could not create room")
                    }
                }
            )
        }
    }

    fun joinRoom(roomCode: String, teamName: String) {
        val normalizedCode = com.example.data.model.RoomCodeNormalizer.normalize(roomCode)
        val cleanedTeam = teamName.trim()
        if (!com.example.data.model.RoomCodeNormalizer.isValid(normalizedCode)) {
            val msg = if (normalizedCode.isEmpty()) {
                "Enter a 4-digit room code to join."
            } else {
                "Room code '$normalizedCode' is invalid. Enter a 4-digit code (e.g. 4829)."
            }
            _activeState.update { it.copy(joinStatusError = msg, joinStatusInfo = null, snackbarMessage = msg) }
            return
        }
        if (cleanedTeam.length < 2) {
            val msg = "Enter a franchise name (at least 2 characters)."
            _activeState.update { it.copy(joinStatusError = msg, joinStatusInfo = null, snackbarMessage = msg) }
            return
        }
        viewModelScope.launch {
            _activeState.update {
                it.copy(
                    busy = true,
                    snackbarMessage = null,
                    joinStatusError = null,
                    joinStatusInfo = "Looking up Room $normalizedCode & syncing roster..."
                )
            }
            val result = repository.joinRoom(normalizedCode, cleanedTeam)
            result.fold(
                onSuccess = { code ->
                    _activeState.update {
                        it.copy(
                            busy = false,
                            joinStatusError = null,
                            joinStatusInfo = null,
                            snackbarMessage = "Joined Room $code! Synced with live lobby."
                        )
                    }
                    openRoom(code)
                },
                onFailure = { err ->
                    val errorMsg = err.message ?: "Could not join Room $normalizedCode. Please try again."
                    _activeState.update {
                        it.copy(
                            busy = false,
                            joinStatusError = errorMsg,
                            joinStatusInfo = null,
                            snackbarMessage = errorMsg
                        )
                    }
                }
            )
        }
    }

    fun openRoom(roomCode: String) {
        if (_activeState.value.activeRoomCode == roomCode && roomObserverJob?.isActive == true) return
        closeRoom()
        _activeState.update {
            it.copy(activeRoomCode = roomCode, room = null, matches = emptyList(), editingXiOverride = false)
        }

        roomObserverJob = viewModelScope.launch {
            repository.observeRoom(roomCode)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing room $roomCode", rootError)
                    _activeState.update {
                        it.copy(snackbarMessage = rootError.message ?: "Connection lost to room")
                    }
                }
                .collect { room ->
                    _activeState.update { it.copy(room = room) }
                    if (room != null) {
                        val isHost = room.hostUid == currentUserId
                        when (room.roomStatus) {
                            RoomStatus.RETENTION -> {
                                auctionTickerJob?.cancel()
                                auctionTickerJob = null
                                // Auto-advance to Live Auction if all franchises (including remote guests) completed retention
                                if (isHost && room.teams.isNotEmpty() && room.teams.all { it.retentionDone }) {
                                    val stageKey = "RET_${room.roomCode}_${room.teams.size}"
                                    if (lastAutoAdvancedStageKey != stageKey) {
                                        lastAutoAdvancedStageKey = stageKey
                                        viewModelScope.launch { repository.beginLiveAuctionFromRetention(room.roomCode) }
                                    }
                                }
                            }
                            RoomStatus.AUCTION -> {
                                anchorCountdownForRoom(room)
                                ensureAuctionTickerRunning(roomCode)
                            }
                            RoomStatus.SQUAD_SELECTION -> {
                                auctionTickerJob?.cancel()
                                auctionTickerJob = null
                                // Auto-advance to Match Hub once all franchises (including remote guests) lock Playing XI
                                if (isHost && room.teams.isNotEmpty() && room.teams.all { it.xiReady }) {
                                    val stageKey = "XI_${room.roomCode}_${room.teams.size}"
                                    if (lastAutoAdvancedStageKey != stageKey) {
                                        lastAutoAdvancedStageKey = stageKey
                                        viewModelScope.launch { repository.enterMatchHub(room.roomCode) }
                                    }
                                }
                            }
                            else -> {
                                auctionTickerJob?.cancel()
                                auctionTickerJob = null
                            }
                        }
                    } else {
                        auctionTickerJob?.cancel()
                        auctionTickerJob = null
                    }
                }
        }

        matchesObserverJob = viewModelScope.launch {
            repository.observeRoomMatches(roomCode, currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing room matches", firestoreError ?: error)
                }
                .collect { matchList ->
                    _activeState.update { it.copy(matches = matchList) }
                }
        }
    }

    fun closeRoom() {
        roomObserverJob?.cancel()
        matchesObserverJob?.cancel()
        auctionTickerJob?.cancel()
        roomObserverJob = null
        matchesObserverJob = null
        auctionTickerJob = null
        lastFinalizedLotKey = ""
        lastAutoAdvancedStageKey = ""
        anchoredLotKey = ""
        anchoredLocalDeadlineElapsedMs = 0L
        bidInFlight = false
        _activeState.value = ActiveRoomUiState()
    }

    fun setEditingXi(editing: Boolean) {
        _activeState.update { it.copy(editingXiOverride = editing) }
    }

    // --- LOBBY, RETENTION & AUCTION ACTIONS ---

    fun toggleAutoFillEightTeams(enabled: Boolean) {
        val code = _activeState.value.activeRoomCode ?: return
        viewModelScope.launch {
            val res = repository.toggleAutoFillEightTeams(code, enabled)
            res.onFailure { err -> showMessage(err.message ?: "Failed to update AI mode") }
        }
    }

    fun addAiBot() {
        val code = _activeState.value.activeRoomCode ?: return
        viewModelScope.launch {
            val res = repository.addAiBot(code)
            res.onFailure { err -> showMessage(err.message ?: "Failed to add AI Bot") }
        }
    }

    fun removeAiBot(botTeamId: String) {
        val code = _activeState.value.activeRoomCode ?: return
        viewModelScope.launch {
            val res = repository.removeAiBot(code, botTeamId)
            res.onFailure { err -> showMessage(err.message ?: "Failed to remove AI Bot") }
        }
    }

    fun enterRetentionRound() {
        val code = _activeState.value.activeRoomCode ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.enterRetentionRound(code)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Cannot enter Marquee Retention") }
        }
    }

    fun retainMarqueePlayer(playerId: String) {
        val room = _activeState.value.room ?: return
        val myTeam = room.teamForUser(currentUserId) ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.retainOrSkipMarquee(room.roomCode, myTeam.teamId, playerId)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Retention failed") }
        }
    }

    fun skipMarqueeRetention() {
        val room = _activeState.value.room ?: return
        val myTeam = room.teamForUser(currentUserId) ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.retainOrSkipMarquee(room.roomCode, myTeam.teamId, null)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Could not skip retention") }
        }
    }

    fun beginLiveAuctionFromRetention() {
        val code = _activeState.value.activeRoomCode ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.beginLiveAuctionFromRetention(code)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Cannot start live auction") }
        }
    }

    fun placeMyBid() {
        if (bidInFlight) return
        val room = _activeState.value.room ?: return
        val myTeam = room.teamForUser(currentUserId) ?: return
        bidInFlight = true
        viewModelScope.launch {
            try {
                val res = repository.placeBid(room.roomCode, myTeam.teamId)
                res.onFailure { err -> showMessage(err.message ?: "Bid rejected") }
            } finally {
                bidInFlight = false
            }
        }
    }

    fun useMyRtmCard() {
        val room = _activeState.value.room ?: return
        val myTeam = room.teamForUser(currentUserId) ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.useRtmCard(room.roomCode, myTeam.teamId)
            _activeState.update { it.copy(busy = false) }
            res.fold(
                onSuccess = { showMessage("⚡ RTM Card used! Player added to your squad.") },
                onFailure = { err -> showMessage(err.message ?: "RTM Card unavailable") }
            )
        }
    }

    fun hammerCurrentLotNow() {
        val room = _activeState.value.room ?: return
        if (room.hostUid != currentUserId) return
        viewModelScope.launch {
            val res = repository.finalizeCurrentLot(room.roomCode)
            res.onFailure { err -> showMessage(err.message ?: "Could not advance lot") }
        }
    }

    fun fastForwardAuction() {
        val room = _activeState.value.room ?: return
        if (room.hostUid != currentUserId) return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.fastForwardAuction(room.roomCode)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Auto-complete failed") }
        }
    }

    fun saveMyPlayingXi(selectedIds: List<String>, captainPlayerId: String = "") {
        val room = _activeState.value.room ?: return
        val myTeam = room.teamForUser(currentUserId) ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.savePlayingXi(
                roomCode = room.roomCode,
                teamId = myTeam.teamId,
                selectedIds = selectedIds,
                captainPlayerId = captainPlayerId
            )
            _activeState.update { it.copy(busy = false, editingXiOverride = false) }
            res.fold(
                onSuccess = { showMessage("Playing XI, batting order & Captain locked in!") },
                onFailure = { err -> showMessage(err.message ?: "Invalid Playing XI") }
            )
        }
    }

    fun enterMatchHubAsHost() {
        val room = _activeState.value.room ?: return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            val res = repository.enterMatchHub(room.roomCode)
            _activeState.update { it.copy(busy = false) }
            res.onFailure { err -> showMessage(err.message ?: "Unable to open Match Hub") }
        }
    }

    fun quickSimLeagueMatches(fixtures: List<Pair<TeamState, TeamState>>) {
        val room = _activeState.value.room ?: return
        if (fixtures.isEmpty() || _activeState.value.busy) return
        viewModelScope.launch {
            _activeState.update { it.copy(busy = true) }
            var completed = 0
            for ((teamA, teamB) in fixtures) {
                val simulated = com.example.engine.MatchSimulationEngine.simulateFullMatch(
                    com.example.engine.MatchSimulationEngine.createMatch(
                        teamA = teamA,
                        teamB = teamB,
                        stage = com.example.data.model.MatchStage.LEAGUE
                    )
                )
                val record = simulated.toMatchRecord(
                    roomCode = room.roomCode,
                    hostUid = currentUserId,
                    memberUids = room.memberUids
                )
                val res = repository.recordMatchResult(room, record)
                if (res.isSuccess) completed++
            }
            _activeState.update {
                it.copy(
                    busy = false,
                    snackbarMessage = "Simulated $completed league match(es)! Points & NRR updated."
                )
            }
        }
    }

    suspend fun saveCompletedMatch(record: MatchRecord): Result<String> {
        val room = _activeState.value.room ?: return Result.failure(IllegalStateException("No active room"))
        return repository.recordMatchResult(room, record)
    }

    // --- CLOCK-SKEW-SAFE 10-SECOND COUNTDOWN & AI BOT BIDDER ---

    /**
     * Anchors the countdown to the device's monotonic elapsedRealtime clock whenever a new
     * lot or bid arrives. Prevents cross-device clock skew (e.g., Android vs iOS system clock
     * differences) from causing premature 0s expirations or frozen 10s timers.
     */
    private fun anchorCountdownForRoom(room: AuctionRoom) {
        val maxWindowMs = SquadRules.AUCTION_TIMER_SECONDS * 1000L
        val lotKey = "${room.currentPlayerIndex}_${room.bidCount}_${room.bidDeadlineEpochMs}"
        if (lotKey == anchoredLotKey) return
        anchoredLotKey = lotKey
        val nowWall = System.currentTimeMillis()
        val remainingMs = if (room.hostUid == currentUserId && room.bidDeadlineEpochMs > 0L) {
            (room.bidDeadlineEpochMs - nowWall).coerceIn(0L, maxWindowMs)
        } else {
            val serverUpdatedMs = room.updatedAt?.toDate()?.time
            val estFromServer = if (serverUpdatedMs != null && room.bidDeadlineEpochMs > 0L) {
                (room.bidDeadlineEpochMs - serverUpdatedMs).coerceIn(1_000L, maxWindowMs)
            } else {
                maxWindowMs
            }
            estFromServer
        }
        anchoredLocalDeadlineElapsedMs = SystemClock.elapsedRealtime() + remainingMs
    }

    private fun ensureAuctionTickerRunning(roomCode: String) {
        if (auctionTickerJob?.isActive == true) return
        auctionTickerJob = viewModelScope.launch {
            val maxWindowMs = (SquadRules.AUCTION_TIMER_SECONDS * 1000L).toFloat()
            var lastEmittedSec = -1
            var lastEmittedBucket = -1
            while (isActive) {
                val room = _activeState.value.room
                if (room == null || room.roomCode != roomCode || room.roomStatus != RoomStatus.AUCTION) {
                    delay(300L)
                    continue
                }

                anchorCountdownForRoom(room)
                val nowElapsed = SystemClock.elapsedRealtime()
                val remainingMs = (anchoredLocalDeadlineElapsedMs - nowElapsed).coerceAtLeast(0L)
                val seconds = ((remainingMs + 999L) / 1000L).toInt().coerceIn(0, SquadRules.AUCTION_TIMER_SECONDS)
                val progress = (remainingMs / maxWindowMs).coerceIn(0f, 1f)
                val progressBucket = (progress * 20f).toInt()

                // Emit only when seconds or quantized 5% bar step actually changed (keeps mobile Chrome/Safari lightweight)
                if (seconds != lastEmittedSec || progressBucket != lastEmittedBucket) {
                    lastEmittedSec = seconds
                    lastEmittedBucket = progressBucket
                    _activeState.update {
                        it.copy(countdownSeconds = seconds, countdownProgress = progress)
                    }
                }

                // Host manages AI bot bids and timer expiration authoritatively
                if (room.hostUid == currentUserId && room.bidDeadlineEpochMs > 0L) {
                    val nowWall = System.currentTimeMillis()
                    if (remainingMs == 0L) {
                        val lotKey = "${room.currentPlayerIndex}_${room.bidCount}"
                        if (lastFinalizedLotKey != lotKey) {
                            lastFinalizedLotKey = lotKey
                            repository.finalizeCurrentLot(roomCode)
                            delay(550L)
                            continue
                        }
                    } else if (remainingMs in 2200L..8200L && (nowWall - lastBotActionAtMs) > 1650L) {
                        val botDecision = AuctionBotEngine.decideNextBotBid(room)
                        if (botDecision != null) {
                            val urgencyProb = if (remainingMs < 4500L) 0.85 else 0.45
                            if (Random.nextDouble() < urgencyProb) {
                                lastBotActionAtMs = nowWall
                                repository.placeBid(roomCode, botDecision.teamId)
                            }
                        }
                    }
                }

                delay(250L)
            }
        }
    }

    fun getCurrentUserTeam(): TeamState? = _activeState.value.room?.teamForUser(currentUserId)

    override fun onCleared() {
        super.onCleared()
        closeRoom()
    }

    private companion object {
        const val TAG = "CricBidVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
