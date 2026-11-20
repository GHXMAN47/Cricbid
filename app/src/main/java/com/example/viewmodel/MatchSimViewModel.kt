package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuctionRoom
import com.example.data.model.MatchFormat
import com.example.data.model.TeamState
import com.example.engine.ActionAnimationSnapshot
import com.example.engine.BallOutcomeType
import com.example.engine.DeliveryType
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePhaseResolver
import com.example.engine.InteractivePlayFilter
import com.example.engine.InteractivePlayRole
import com.example.engine.LiveMatchState
import com.example.engine.MatchSimulationEngine
import com.example.engine.ShotDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlaybackSpeed(val label: String, val delayMs: Long) {
    NORMAL("1x", 850L),
    FAST("2x", 380L),
    TURBO("5x", 110L)
}

data class MatchSimUiState(
    val match: LiveMatchState? = null,
    val autoPlaying: Boolean = false,
    val speed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val savingResult: Boolean = false,
    val statusNotice: String? = null,
    val lastActionSnapshot: ActionAnimationSnapshot? = null,
    val userTeamId: String = "",
    val playFilter: InteractivePlayFilter = InteractivePlayFilter.BOTH,
    val showPlayModeSetup: Boolean = false,
    val autoSimmingSkippedPhase: Boolean = false
)

class MatchSimViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MatchSimUiState())
    val uiState: StateFlow<MatchSimUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    private var skippedPhaseJob: Job? = null
    private var actionSeq: Long = 0L

    fun startNewMatch(
        teamA: TeamState,
        teamB: TeamState,
        stage: com.example.data.model.MatchStage = com.example.data.model.MatchStage.LEAGUE,
        format: com.example.data.model.MatchFormat = com.example.data.model.MatchFormat.T20I,
        opponentRatingOffset: Int = 0,
        userTeamId: String? = null
    ) {
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        val initial = MatchSimulationEngine.createMatch(
            teamA = teamA,
            teamB = teamB,
            stage = stage,
            format = format,
            teamBRatingOffset = opponentRatingOffset
        )
        val resolvedUserTeamId = InteractivePhaseResolver.resolveUserTeamId(initial, userTeamId)
        val previousFilter = _uiState.value.playFilter
        _uiState.value = MatchSimUiState(
            match = initial,
            autoPlaying = false,
            speed = PlaybackSpeed.NORMAL,
            userTeamId = resolvedUserTeamId,
            playFilter = previousFilter,
            showPlayModeSetup = true,
            autoSimmingSkippedPhase = false
        )
    }

    fun confirmPlayModeSetup(
        filter: InteractivePlayFilter,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        _uiState.update {
            it.copy(
                playFilter = filter,
                showPlayModeSetup = false,
                statusNotice = "Mode: ${filter.label}"
            )
        }
        triggerSkippedPhaseAutoSimIfNeeded(onFinished)
    }

    fun setPlayFilter(
        filter: InteractivePlayFilter,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        if (_uiState.value.playFilter == filter && !_uiState.value.showPlayModeSetup) return
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        _uiState.update {
            it.copy(
                playFilter = filter,
                showPlayModeSetup = false,
                statusNotice = "Switched to ${filter.label}"
            )
        }
        triggerSkippedPhaseAutoSimIfNeeded(onFinished)
    }

    fun applyImpactSub(teamId: String, benchPlayerId: String, outPlayerId: String) {
        val current = _uiState.value.match ?: return
        val res = MatchSimulationEngine.applyImpactSubstitution(
            state = current,
            teamId = teamId,
            benchPlayerId = benchPlayerId,
            outPlayerId = outPlayerId
        )
        res.fold(
            onSuccess = { updated ->
                _uiState.update {
                    it.copy(
                        match = updated,
                        statusNotice = "⚡ Impact Player activated!"
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(statusNotice = err.message ?: "Impact Sub not allowed")
                }
            }
        )
    }

    fun selectBowlerForNextOver(bowlerPlayerId: String) {
        val current = _uiState.value.match ?: return
        val res = MatchSimulationEngine.selectBowlerForNextOver(current, bowlerPlayerId)
        res.fold(
            onSuccess = { updated ->
                val bowlerName = updated.activeInnings.currentBowler?.name ?: "Bowler"
                _uiState.update {
                    it.copy(
                        match = updated,
                        statusNotice = "🏏 $bowlerName set to bowl next over"
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(statusNotice = err.message ?: "Bowler not eligible for this over")
                }
            }
        )
    }

    fun clearStatusNotice() {
        _uiState.update { it.copy(statusNotice = null) }
    }

    fun playInteractiveBall(
        input: InteractiveBallInput,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        stopAutoPlay()
        val state = _uiState.value
        if (state.autoSimmingSkippedPhase) return
        val current = state.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateNextBall(current, interactiveInput = input)
        val snap = buildSnapshot(next, input)
        _uiState.update { it.copy(match = next, lastActionSnapshot = snap) }
        if (next.finished) {
            onFinished?.invoke(next)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    fun nextBall(onFinished: ((LiveMatchState) -> Unit)? = null) {
        val current = _uiState.value.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateNextBall(current)
        val snap = buildSnapshot(next, null)
        _uiState.update { it.copy(match = next, lastActionSnapshot = snap) }
        if (next.finished) {
            stopAutoPlay()
            stopSkippedPhaseAutoSim()
            onFinished?.invoke(next)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    private fun buildSnapshot(
        updated: LiveMatchState,
        input: InteractiveBallInput?
    ): ActionAnimationSnapshot? {
        val lastEvent = updated.commentaryFeed.firstOrNull { !it.isInjuryAlert } ?: return null
        actionSeq += 1L
        val direction = input?.shotDirection ?: when (lastEvent.outcome) {
            BallOutcomeType.SIX -> ShotDirection.LOFTED
            BallOutcomeType.FOUR -> if (actionSeq % 2L == 0L) ShotDirection.OFF_DRIVE else ShotDirection.LEG_GLANCE
            else -> ShotDirection.STRAIGHT
        }
        val delivery = input?.deliveryType ?: DeliveryType.entries[(actionSeq % DeliveryType.entries.size).toInt()]
        return ActionAnimationSnapshot(
            sequenceId = actionSeq,
            outcome = lastEvent.outcome,
            runsScored = lastEvent.runsScored,
            shotDirection = direction,
            deliveryType = delivery,
            timingGrade = input?.timingGrade,
            role = input?.role ?: InteractivePlayRole.BATTING,
            batterName = lastEvent.batterName,
            bowlerName = lastEvent.bowlerName,
            overLabel = lastEvent.overLabel,
            isSuperOver = lastEvent.isSuperOver,
            isRedBall = updated.format == MatchFormat.TEST
        )
    }

    fun nextOver(onFinished: ((LiveMatchState) -> Unit)? = null) {
        val current = _uiState.value.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateNextOver(current)
        val snap = buildSnapshot(next, null)
        _uiState.update { it.copy(match = next, lastActionSnapshot = snap ?: it.lastActionSnapshot) }
        if (next.finished) {
            stopAutoPlay()
            stopSkippedPhaseAutoSim()
            onFinished?.invoke(next)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    fun nextFiveOvers(onFinished: ((LiveMatchState) -> Unit)? = null) {
        val current = _uiState.value.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateFiveOvers(current)
        val snap = buildSnapshot(next, null)
        _uiState.update { it.copy(match = next, lastActionSnapshot = snap ?: it.lastActionSnapshot) }
        if (next.finished) {
            stopAutoPlay()
            stopSkippedPhaseAutoSim()
            onFinished?.invoke(next)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    fun simInnings(onFinished: ((LiveMatchState) -> Unit)? = null) {
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        val current = _uiState.value.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateCurrentInnings(current)
        val snap = buildSnapshot(next, null)
        _uiState.update {
            it.copy(
                match = next,
                lastActionSnapshot = snap ?: it.lastActionSnapshot,
                autoSimmingSkippedPhase = false
            )
        }
        if (next.finished) {
            onFinished?.invoke(next)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    fun simFullMatch(onFinished: ((LiveMatchState) -> Unit)? = null) {
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        val current = _uiState.value.match ?: return
        if (current.finished) return
        val next = MatchSimulationEngine.simulateFullMatch(current)
        _uiState.update { it.copy(match = next, showPlayModeSetup = false, autoSimmingSkippedPhase = false) }
        if (next.finished) {
            onFinished?.invoke(next)
        }
    }

    fun toggleAutoPlay(onFinished: ((LiveMatchState) -> Unit)? = null) {
        if (_uiState.value.autoPlaying) {
            stopAutoPlay()
        } else {
            stopSkippedPhaseAutoSim()
            val current = _uiState.value.match ?: return
            if (current.finished) return
            _uiState.update { it.copy(autoPlaying = true, showPlayModeSetup = false) }
            autoPlayJob = viewModelScope.launch {
                while (isActive) {
                    val m = _uiState.value.match
                    if (m == null || m.finished) {
                        _uiState.update { it.copy(autoPlaying = false) }
                        break
                    }
                    val updated = MatchSimulationEngine.simulateNextBall(m)
                    val snap = buildSnapshot(updated, null)
                    _uiState.update { it.copy(match = updated, lastActionSnapshot = snap) }
                    if (updated.finished) {
                        _uiState.update { it.copy(autoPlaying = false) }
                        onFinished?.invoke(updated)
                        break
                    }
                    delay(_uiState.value.speed.delayMs)
                }
            }
        }
    }

    /**
     * Automatically fast-simulates any innings that falls outside the user's selected
     * [InteractivePlayFilter] (e.g., opponent's batting innings in BATTING_ONLY mode,
     * or user's batting innings in BOWLING_ONLY mode) in lightweight 1-over pulses
     * so mobile browsers stay smooth while letting the user watch or instant-skip.
     */
    private fun triggerSkippedPhaseAutoSimIfNeeded(
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        val currentState = _uiState.value
        val currentMatch = currentState.match ?: return
        if (currentState.showPlayModeSetup || currentMatch.finished) {
            stopSkippedPhaseAutoSim()
            return
        }
        val shouldAutoSim = InteractivePhaseResolver.shouldAutoSimCurrentPhase(
            match = currentMatch,
            userTeamId = currentState.userTeamId,
            filter = currentState.playFilter
        )
        if (!shouldAutoSim) {
            stopSkippedPhaseAutoSim()
            return
        }
        if (skippedPhaseJob?.isActive == true) return

        stopAutoPlay()
        _uiState.update { it.copy(autoSimmingSkippedPhase = true) }
        skippedPhaseJob = viewModelScope.launch {
            while (isActive) {
                val st = _uiState.value
                val m = st.match
                if (m == null || m.finished) {
                    _uiState.update { it.copy(autoSimmingSkippedPhase = false) }
                    if (m?.finished == true) onFinished?.invoke(m)
                    break
                }
                val stillSkipped = InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                    match = m,
                    userTeamId = st.userTeamId,
                    filter = st.playFilter
                )
                if (!stillSkipped) {
                    val userBat = InteractivePhaseResolver.isUserTeamBatting(m, st.userTeamId)
                    val handoffMsg = if (userBat) {
                        val tgtText = m.target?.let { " • Target $it" }.orEmpty()
                        "🏏 Your turn to BAT$tgtText!"
                    } else {
                        val tgtText = m.target?.let { " • Defend ${it - 1}" }.orEmpty()
                        "🎯 Your turn to BOWL$tgtText!"
                    }
                    _uiState.update {
                        it.copy(autoSimmingSkippedPhase = false, statusNotice = handoffMsg)
                    }
                    break
                }

                // Step 1 over at a time in regulation (or 1 ball in Super Over) to keep UI lightweight & responsive
                val updated = if (m.isSuperOverActive) {
                    MatchSimulationEngine.simulateNextBall(m)
                } else {
                    MatchSimulationEngine.simulateNextOver(m)
                }
                val snap = buildSnapshot(updated, null)
                _uiState.update {
                    it.copy(
                        match = updated,
                        lastActionSnapshot = snap ?: it.lastActionSnapshot
                    )
                }
                if (updated.finished) {
                    _uiState.update { it.copy(autoSimmingSkippedPhase = false) }
                    onFinished?.invoke(updated)
                    break
                }
                delay(220L)
            }
        }
    }

    fun cycleSpeed() {
        val next = when (_uiState.value.speed) {
            PlaybackSpeed.NORMAL -> PlaybackSpeed.FAST
            PlaybackSpeed.FAST -> PlaybackSpeed.TURBO
            PlaybackSpeed.TURBO -> PlaybackSpeed.NORMAL
        }
        _uiState.update { it.copy(speed = next) }
    }

    fun stopAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        _uiState.update { it.copy(autoPlaying = false) }
    }

    private fun stopSkippedPhaseAutoSim() {
        skippedPhaseJob?.cancel()
        skippedPhaseJob = null
        _uiState.update { it.copy(autoSimmingSkippedPhase = false) }
    }

    fun persistResultIfNeeded(
        room: AuctionRoom,
        currentUid: String,
        saveAction: suspend (com.example.data.model.MatchRecord) -> Result<String>
    ) {
        val current = _uiState.value.match ?: return
        if (!current.finished || current.savedToCloud || _uiState.value.savingResult) return
        viewModelScope.launch {
            _uiState.update { it.copy(savingResult = true) }
            val record = current.toMatchRecord(
                roomCode = room.roomCode,
                hostUid = currentUid,
                memberUids = room.memberUids
            )
            val res = saveAction(record)
            _uiState.update { state ->
                state.copy(
                    savingResult = false,
                    match = state.match?.copy(savedToCloud = res.isSuccess),
                    statusNotice = if (res.isSuccess) "Match result synced to League Table!" else res.exceptionOrNull()?.message
                )
            }
        }
    }

    fun persistInternationalResultIfNeeded(
        currentUid: String,
        onSaveRecord: (com.example.data.model.MatchRecord) -> Unit
    ) {
        val current = _uiState.value.match ?: return
        if (!current.finished || current.savedToCloud || _uiState.value.savingResult) return
        val record = current.toMatchRecord(
            roomCode = "INTL_${current.format.name}",
            hostUid = currentUid,
            memberUids = listOf(currentUid)
        ).copy(id = "intl_${System.currentTimeMillis()}")
        onSaveRecord(record)
        _uiState.update { state ->
            state.copy(
                match = state.match?.copy(savedToCloud = true),
                statusNotice = "${current.format.shortLabel} result recorded!"
            )
        }
    }

    fun closeMatch() {
        val savedFilter = _uiState.value.playFilter
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        _uiState.value = MatchSimUiState(playFilter = savedFilter)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
    }
}
