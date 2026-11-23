package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalIplSeasonStore
import com.example.data.model.IplAiResultsBatch
import com.example.data.model.IplPlayMode
import com.example.data.model.IplRules
import com.example.data.model.IplScheduledMatch
import com.example.data.model.IplSeasonPhase
import com.example.data.model.IplSeasonState
import com.example.data.model.MatchRecord
import com.example.engine.IplSeasonEngine
import com.example.engine.TrophyCabinetRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IplSeasonUiState(
    val season: IplSeasonState? = null,
    val aiResultsPopup: IplAiResultsBatch? = null,
    val countdownSeconds: Int = IplRules.AUCTION_TIMER_SECONDS,
    val countdownProgress: Float = 1f,
    val auctionPaused: Boolean = false,
    val editingXiDuringSeason: Boolean = false,
    val snackbarMessage: String? = null
)

class IplSeasonViewModel(
    private val store: LocalIplSeasonStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(IplSeasonUiState(season = store.loadSeason()))
    val uiState: StateFlow<IplSeasonUiState> = _uiState.asStateFlow()

    private var auctionTimerJob: Job? = null

    init {
        if (_uiState.value.season?.phase == IplSeasonPhase.AUCTION) {
            startAuctionLoop()
        }
    }

    fun startNewSeason(
        playMode: IplPlayMode,
        humanSelections: List<Pair<String, String>>,
        roomCode: String = ""
    ) {
        stopAuctionLoop()
        val prevNum = _uiState.value.season?.seasonNumber ?: 0
        val created = IplSeasonEngine.createNewSeason(
            playMode = playMode,
            humanSelections = humanSelections,
            roomCode = roomCode,
            seasonNumber = if (prevNum > 0 && _uiState.value.season?.phase == IplSeasonPhase.COMPLETED) prevNum + 1 else 1
        )
        persistAndEmit(created, clearPopup = true)
    }

    fun resetToSetup() {
        stopAuctionLoop()
        store.clearSeason()
        _uiState.update { IplSeasonUiState(season = null) }
    }

    fun setActiveHumanFranchise(franchiseId: String) {
        val current = _uiState.value.season ?: return
        if (franchiseId !in current.humanFranchiseIds) return
        persistAndEmit(current.copy(activeHumanFranchiseId = franchiseId))
    }

    fun submitRetentions(franchiseId: String, selectedIds: List<String>) {
        val current = _uiState.value.season ?: return
        IplSeasonEngine.submitRetentions(current, franchiseId, selectedIds)
            .onSuccess { updated ->
                persistAndEmit(updated)
                if (updated.phase == IplSeasonPhase.AUCTION) {
                    resetAuctionClock()
                    startAuctionLoop()
                }
            }
            .onFailure { err ->
                showMessage(err.message ?: "Unable to lock retentions")
            }
    }

    fun placeBid(franchiseId: String) {
        val current = _uiState.value.season ?: return
        IplSeasonEngine.placeBid(current, franchiseId)
            .onSuccess { updated ->
                persistAndEmit(updated)
                resetAuctionClock()
            }
            .onFailure { err ->
                showMessage(err.message ?: "Cannot place bid")
            }
    }

    fun triggerHammerLoot(franchiseId: String) {
        val current = _uiState.value.season ?: return
        IplSeasonEngine.triggerHammerLoot(current, franchiseId)
            .onSuccess { updated ->
                persistAndEmit(updated)
                if (updated.phase == IplSeasonPhase.AUCTION) {
                    resetAuctionClock()
                } else {
                    stopAuctionLoop()
                }
                showMessage(updated.auction.lastEventMessage)
            }
            .onFailure { err ->
                showMessage(err.message ?: "Hammer Loot unavailable")
            }
    }

    fun useRtmCard(franchiseId: String) {
        val current = _uiState.value.season ?: return
        IplSeasonEngine.useRtmCard(current, franchiseId)
            .onSuccess { updated ->
                persistAndEmit(updated)
                if (updated.phase == IplSeasonPhase.AUCTION) {
                    resetAuctionClock()
                } else {
                    stopAuctionLoop()
                }
                showMessage(updated.auction.lastEventMessage)
            }
            .onFailure { err ->
                showMessage(err.message ?: "RTM unavailable")
            }
    }

    fun passCurrentLot(franchiseId: String) {
        val current = _uiState.value.season ?: return
        val passed = IplSeasonEngine.passCurrentLot(current, franchiseId)
        persistAndEmit(passed)
        // If all human teams passed, fast-resolve the lot cleanly!
        val allHumansPassed = passed.humanFranchiseIds.all { it in passed.auction.passedFranchiseIds }
        if (allHumansPassed) {
            fastFinishCurrentLot()
        }
    }

    fun fastFinishCurrentLot() {
        var current = _uiState.value.season ?: return
        if (current.phase != IplSeasonPhase.AUCTION) return
        repeat(4) {
            current = IplSeasonEngine.stepAiAuctionBid(current)
        }
        val resolved = IplSeasonEngine.finalizeCurrentLot(current)
        persistAndEmit(resolved)
        if (resolved.phase == IplSeasonPhase.AUCTION) {
            resetAuctionClock()
        } else {
            stopAuctionLoop()
        }
    }

    fun toggleAuctionPause() {
        _uiState.update { it.copy(auctionPaused = !it.auctionPaused) }
    }

    fun autoCompleteAuction() {
        val current = _uiState.value.season ?: return
        stopAuctionLoop()
        val finished = IplSeasonEngine.autoCompleteRemainingAuction(current)
        persistAndEmit(finished)
        showMessage("Mega Auction finished! All 10 franchises drafted 18–25 players.")
    }

    fun savePlayingXi(
        franchiseId: String,
        xiIds: List<String>,
        impactNomineeIds: List<String>,
        captainId: String,
        enterLeagueIfReady: Boolean
    ) {
        val current = _uiState.value.season ?: return
        IplSeasonEngine.savePlayingXiAndNominees(current, franchiseId, xiIds, impactNomineeIds, captainId)
            .onSuccess { updated ->
                if (enterLeagueIfReady && updated.phase == IplSeasonPhase.SQUAD_XI) {
                    val (leagueState, openingAiBatch) = IplSeasonEngine.enterLeagueStage(updated)
                    persistAndEmit(leagueState, aiPopup = openingAiBatch)
                    _uiState.update { it.copy(editingXiDuringSeason = false) }
                    showMessage("70-Match IPL 2026 Schedule Generated! Your season starts now.")
                } else {
                    persistAndEmit(updated)
                    _uiState.update { it.copy(editingXiDuringSeason = false) }
                    showMessage("Playing XI & 5 Impact Subs updated!")
                }
            }
            .onFailure { err ->
                showMessage(err.message ?: "Invalid Playing XI")
            }
    }

    fun openXiEditorInSeason(franchiseId: String) {
        val current = _uiState.value.season ?: return
        persistAndEmit(current.copy(activeHumanFranchiseId = franchiseId))
        _uiState.update { it.copy(editingXiDuringSeason = true) }
    }

    fun closeXiEditorInSeason() {
        _uiState.update { it.copy(editingXiDuringSeason = false) }
    }

    fun nextHumanFixture(): IplScheduledMatch? {
        val current = _uiState.value.season ?: return null
        return IplSeasonEngine.nextHumanFixture(current)
    }

    /**
     * Records a completed human match, auto-simulates all AI vs AI matches up to the next human match,
     * shows the "Other Match Results" popup, and returns an optional IPL Trophy Cabinet record if the Final finished!
     */
    fun recordMatchCompleted(
        matchNumber: Int,
        record: MatchRecord,
        onSeasonTrophyWon: (TrophyCabinetRecord) -> Unit
    ) {
        val current = _uiState.value.season ?: return
        val (updated, aiBatch) = IplSeasonEngine.recordMatchAndAutoSimAi(current, matchNumber, record)
        var finalState = updated
        if (updated.phase == IplSeasonPhase.COMPLETED && !updated.trophyRecorded) {
            IplSeasonEngine.buildSeasonTrophyRecord(updated)?.let { trophy ->
                onSeasonTrophyWon(trophy)
                finalState = updated.copy(trophyRecorded = true)
            }
        }
        persistAndEmit(finalState, aiPopup = aiBatch)
    }

    fun dismissAiResultsPopup() {
        _uiState.update { it.copy(aiResultsPopup = null) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    private fun persistAndEmit(
        season: IplSeasonState,
        aiPopup: IplAiResultsBatch? = _uiState.value.aiResultsPopup,
        clearPopup: Boolean = false
    ) {
        store.saveSeason(season)
        _uiState.update {
            it.copy(
                season = season,
                aiResultsPopup = if (clearPopup) null else aiPopup
            )
        }
    }

    private fun resetAuctionClock() {
        _uiState.update {
            it.copy(
                countdownSeconds = IplRules.AUCTION_TIMER_SECONDS,
                countdownProgress = 1f
            )
        }
    }

    private fun startAuctionLoop() {
        auctionTimerJob?.cancel()
        auctionTimerJob = viewModelScope.launch {
            var remainingMs = IplRules.AUCTION_TIMER_SECONDS * 1000L
            var aiTickMs = 0L
            while (true) {
                delay(200L)
                val ui = _uiState.value
                val season = ui.season
                if (season == null || season.phase != IplSeasonPhase.AUCTION) break
                if (ui.auctionPaused) continue

                if (ui.countdownProgress == 1f && ui.countdownSeconds == IplRules.AUCTION_TIMER_SECONDS && remainingMs < 9_200L) {
                    remainingMs = IplRules.AUCTION_TIMER_SECONDS * 1000L
                }

                remainingMs = (remainingMs - 200L).coerceAtLeast(0L)
                aiTickMs += 200L

                val secs = ((remainingMs + 999L) / 1000L).toInt().coerceIn(0, IplRules.AUCTION_TIMER_SECONDS)
                val prog = (remainingMs.toFloat() / (IplRules.AUCTION_TIMER_SECONDS * 1000f)).coerceIn(0f, 1f)
                _uiState.update { it.copy(countdownSeconds = secs, countdownProgress = prog) }

                if (aiTickMs >= 1_200L && remainingMs > 1_000L) {
                    aiTickMs = 0L
                    val beforeBidder = season.auction.highestBidderFranchiseId
                    val beforeIdx = season.auction.currentIndex
                    val stepped = IplSeasonEngine.stepAiAuctionBid(season)
                    if (stepped.auction.highestBidderFranchiseId != beforeBidder || stepped.auction.currentIndex != beforeIdx) {
                        persistAndEmit(stepped)
                        remainingMs = IplRules.AUCTION_TIMER_SECONDS * 1000L
                        resetAuctionClock()
                        if (stepped.phase != IplSeasonPhase.AUCTION) break
                    }
                }

                if (remainingMs <= 0L) {
                    val resolved = IplSeasonEngine.finalizeCurrentLot(season)
                    persistAndEmit(resolved)
                    if (resolved.phase != IplSeasonPhase.AUCTION) break
                    remainingMs = IplRules.AUCTION_TIMER_SECONDS * 1000L
                    resetAuctionClock()
                }
            }
        }
    }

    private fun stopAuctionLoop() {
        auctionTimerJob?.cancel()
        auctionTimerJob = null
    }

    override fun onCleared() {
        stopAuctionLoop()
        super.onCleared()
    }
}
