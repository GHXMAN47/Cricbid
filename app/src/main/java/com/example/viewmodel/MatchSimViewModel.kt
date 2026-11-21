package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedMatchResumeSnapshot
import com.example.data.model.AuctionRoom
import com.example.data.model.MatchFormat
import com.example.data.model.TeamState
import com.example.engine.ActionAnimationSnapshot
import com.example.engine.BallOutcomeType
import com.example.engine.CareerControlMode
import com.example.engine.DeliveryType
import com.example.engine.FieldPreset
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePhaseResolver
import com.example.engine.InteractivePlayFilter
import com.example.engine.InteractivePlayRole
import com.example.engine.LiveMatchState
import com.example.engine.MatchSimulationEngine
import com.example.engine.PitchType
import com.example.engine.ShotDirection
import com.example.engine.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val autoSimmingSkippedPhase: Boolean = false,
    val showDrsOverlay: Boolean = false,
    val careerPlayerId: String? = null,
    val careerPlayerName: String? = null,
    val careerControlMode: CareerControlMode = CareerControlMode.FULL_TEAM,
    val autoJumpLandscapeSignal: Long = 0L
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
        userTeamId: String? = null,
        careerPlayerId: String? = null,
        careerPlayerName: String? = null,
        careerControlMode: CareerControlMode = if (careerPlayerId != null) CareerControlMode.PLAYER_ONLY else CareerControlMode.FULL_TEAM
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
        val resolvedFilter = if (careerPlayerId != null && careerControlMode == CareerControlMode.PLAYER_ONLY) {
            InteractivePlayFilter.BOTH
        } else {
            _uiState.value.playFilter
        }
        _uiState.value = MatchSimUiState(
            match = initial,
            autoPlaying = false,
            speed = PlaybackSpeed.NORMAL,
            userTeamId = resolvedUserTeamId,
            playFilter = resolvedFilter,
            showPlayModeSetup = true,
            autoSimmingSkippedPhase = false,
            careerPlayerId = careerPlayerId,
            careerPlayerName = careerPlayerName,
            careerControlMode = if (careerPlayerId != null) careerControlMode else CareerControlMode.FULL_TEAM,
            autoJumpLandscapeSignal = 0L
        )
    }

    fun confirmPlayModeSetup(
        filter: InteractivePlayFilter,
        pitchType: PitchType? = null,
        weatherCondition: WeatherCondition? = null,
        dewFactor2ndInnings: Boolean? = null,
        careerControlMode: CareerControlMode? = null,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        var shouldJumpLandscapeNow = false
        _uiState.update { state ->
            val resolvedControlMode = if (state.careerPlayerId != null) {
                careerControlMode ?: state.careerControlMode
            } else {
                CareerControlMode.FULL_TEAM
            }
            val effectiveFilter = if (state.careerPlayerId != null && resolvedControlMode == CareerControlMode.PLAYER_ONLY) {
                InteractivePlayFilter.BOTH
            } else {
                filter
            }
            val updatedMatch = state.match?.let { m ->
                if (pitchType != null || weatherCondition != null || dewFactor2ndInnings != null) {
                    MatchSimulationEngine.updatePreMatchConditions(
                        state = m,
                        pitchType = pitchType ?: m.pitchType,
                        weatherCondition = weatherCondition ?: m.weatherCondition,
                        dewFactor2ndInnings = dewFactor2ndInnings ?: m.dewFactor2ndInnings
                    )
                } else m
            }
            if (updatedMatch != null &&
                state.careerPlayerId != null &&
                resolvedControlMode == CareerControlMode.PLAYER_ONLY &&
                InteractivePhaseResolver.isCareerPlayerActiveTurn(updatedMatch, state.userTeamId, state.careerPlayerId)
            ) {
                shouldJumpLandscapeNow = true
            }
            val modeLabel = if (state.careerPlayerId != null) {
                "${resolvedControlMode.shortLabel} (${state.careerPlayerName ?: "My Pro"})"
            } else {
                effectiveFilter.label
            }
            state.copy(
                match = updatedMatch,
                playFilter = effectiveFilter,
                careerControlMode = resolvedControlMode,
                showPlayModeSetup = false,
                autoJumpLandscapeSignal = if (shouldJumpLandscapeNow) state.autoJumpLandscapeSignal + 1L else state.autoJumpLandscapeSignal,
                statusNotice = "Mode: $modeLabel • ${updatedMatch?.pitchType?.shortLabel ?: ""}"
            )
        }
        triggerSkippedPhaseAutoSimIfNeeded(onFinished)
    }

    fun resumeSavedMatch(
        snapshot: SavedMatchResumeSnapshot,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        val resolvedControlMode = if (!snapshot.careerPlayerId.isNullOrBlank()) {
            snapshot.careerControlMode
        } else {
            CareerControlMode.FULL_TEAM
        }
        val jumpNow = !snapshot.careerPlayerId.isNullOrBlank() &&
            resolvedControlMode == CareerControlMode.PLAYER_ONLY &&
            InteractivePhaseResolver.isCareerPlayerActiveTurn(snapshot.match, snapshot.userTeamId, snapshot.careerPlayerId)
        _uiState.value = MatchSimUiState(
            match = snapshot.match,
            autoPlaying = false,
            speed = PlaybackSpeed.NORMAL,
            userTeamId = snapshot.userTeamId,
            playFilter = if (resolvedControlMode == CareerControlMode.PLAYER_ONLY) InteractivePlayFilter.BOTH else snapshot.playFilter,
            showPlayModeSetup = false,
            autoSimmingSkippedPhase = false,
            showDrsOverlay = false,
            careerPlayerId = snapshot.careerPlayerId,
            careerPlayerName = snapshot.careerPlayerName,
            careerControlMode = resolvedControlMode,
            autoJumpLandscapeSignal = if (jumpNow) 1L else 0L,
            statusNotice = "Resumed saved match (${snapshot.headline})"
        )
        triggerSkippedPhaseAutoSimIfNeeded(onFinished)
    }

    fun setFieldPreset(preset: FieldPreset) {
        val current = _uiState.value.match ?: return
        if (current.fieldPreset == preset) return
        val updated = MatchSimulationEngine.setFieldPreset(current, preset)
        _uiState.update {
            it.copy(
                match = updated,
                statusNotice = "Field Set: ${preset.label} (${preset.summary})"
            )
        }
    }

    fun startDrsReview() {
        val current = _uiState.value.match ?: return
        if (current.pendingDrs == null) return
        stopAutoPlay()
        _uiState.update { it.copy(showDrsOverlay = true) }
    }

    fun completeDrsReview(onFinished: ((LiveMatchState) -> Unit)? = null) {
        val current = _uiState.value.match ?: return
        val resolved = MatchSimulationEngine.resolveDrsReview(current)
        _uiState.update {
            it.copy(
                match = resolved,
                showDrsOverlay = false,
                statusNotice = resolved.commentaryFeed.firstOrNull()?.commentary ?: "DRS decision finalized"
            )
        }
        if (resolved.finished) {
            onFinished?.invoke(resolved)
        } else {
            triggerSkippedPhaseAutoSimIfNeeded(onFinished)
        }
    }

    fun dismissDrs() {
        val current = _uiState.value.match ?: return
        val cleared = MatchSimulationEngine.dismissPendingDrs(current)
        _uiState.update { it.copy(match = cleared, showDrsOverlay = false) }
    }

    fun setPlayFilter(
        filter: InteractivePlayFilter,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        val state = _uiState.value
        if (state.careerControlMode == CareerControlMode.PLAYER_ONLY && !state.careerPlayerId.isNullOrBlank()) {
            // Strict Player-Only mode stays on BOTH so user always plays their player's batting & bowling turns
            return
        }
        if (state.playFilter == filter && !state.showPlayModeSetup) return
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

    fun setCareerControlMode(
        mode: CareerControlMode,
        onFinished: ((LiveMatchState) -> Unit)? = null
    ) {
        val state = _uiState.value
        if (state.careerPlayerId.isNullOrBlank()) return
        if (state.careerControlMode == mode && !state.showPlayModeSetup) return
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        _uiState.update {
            it.copy(
                careerControlMode = mode,
                playFilter = if (mode == CareerControlMode.PLAYER_ONLY) InteractivePlayFilter.BOTH else it.playFilter,
                showPlayModeSetup = false,
                statusNotice = if (mode == CareerControlMode.PLAYER_ONLY) {
                    "★ Strict Player-Only active for ${it.careerPlayerName ?: "My Pro"}"
                } else {
                    "Switched to Full-Team Gameplay"
                }
            )
        }
        triggerSkippedPhaseAutoSimIfNeeded(onFinished)
    }

    /**
     * Immediately fast-forwards past all non-created-player deliveries/overs and lands
     * on the exact ball where the created career player faces a delivery or bowls an over.
     */
    fun skipToCareerPlayerTurn(onFinished: ((LiveMatchState) -> Unit)? = null) {
        val state = _uiState.value
        val startMatch = state.match ?: return
        if (startMatch.finished) return
        val careerId = state.careerPlayerId
        if (careerId.isNullOrBlank() || state.careerControlMode != CareerControlMode.PLAYER_ONLY) {
            simInnings(onFinished)
            return
        }
        stopAutoPlay()
        stopSkippedPhaseAutoSim()
        viewModelScope.launch {
            val advanced = withContext(Dispatchers.Default) {
                var cur = startMatch
                var guard = 0
                while (
                    !cur.finished &&
                    guard < 4000 &&
                    InteractivePhaseResolver.shouldAutoSimAnyReason(
                        match = cur,
                        userTeamId = state.userTeamId,
                        filter = state.playFilter,
                        careerPlayerId = careerId,
                        controlMode = state.careerControlMode
                    )
                ) {
                    cur = stepAutoSimForPlayerOnly(cur, state.userTeamId, careerId)
                    guard++
                }
                cur
            }
            val snap = buildSnapshot(advanced, null)
            val isActiveTurn = !advanced.finished && InteractivePhaseResolver.isCareerPlayerActiveTurn(
                match = advanced,
                userTeamId = state.userTeamId,
                careerPlayerId = careerId
            )
            val handoffMsg = buildPlayerTurnHandoffMessage(
                match = advanced,
                userTeamId = state.userTeamId,
                careerPlayerName = state.careerPlayerName
            )
            _uiState.update {
                it.copy(
                    match = advanced,
                    lastActionSnapshot = snap ?: it.lastActionSnapshot,
                    autoSimmingSkippedPhase = false,
                    autoJumpLandscapeSignal = if (isActiveTurn) it.autoJumpLandscapeSignal + 1L else it.autoJumpLandscapeSignal,
                    statusNotice = if (advanced.finished) advanced.resultSummary else handoffMsg
                )
            }
            if (advanced.finished) {
                onFinished?.invoke(advanced)
            }
        }
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
        // Strict Player-Only guard: never allow interactive play for teammates
        if (state.careerControlMode == CareerControlMode.PLAYER_ONLY && !state.careerPlayerId.isNullOrBlank()) {
            if (!InteractivePhaseResolver.isCareerPlayerActiveTurn(current, state.userTeamId, state.careerPlayerId)) {
                triggerSkippedPhaseAutoSimIfNeeded(onFinished)
                return
            }
        }
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
            isRedBall = updated.format == MatchFormat.TEST,
            pitchType = updated.pitchType,
            dewActive = updated.isDewActiveNow,
            fieldPreset = updated.fieldPreset
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

    private fun stepAutoSimForPlayerOnly(
        match: LiveMatchState,
        userTeamId: String,
        careerPlayerId: String
    ): LiveMatchState {
        if (match.isSuperOverActive) {
            return MatchSimulationEngine.simulateNextBall(match)
        }
        val userBatting = InteractivePhaseResolver.isUserTeamBatting(match, userTeamId)
        return if (userBatting) {
            // If career player is already dismissed in this innings, fast-sim full overs cleanly
            val alreadyOut = match.activeInnings.battingCard
                .firstOrNull { it.playerId == careerPlayerId }
                ?.out == true
            if (alreadyOut) {
                MatchSimulationEngine.simulateNextOver(match)
            } else {
                // Step 1 ball at a time before coming in or at non-striker end so we pause on their exact first ball
                MatchSimulationEngine.simulateNextBall(match)
            }
        } else {
            // Opponent batting & teammate bowling: step 1 full over at start-of-over, else finish current over ball-by-ball
            if (match.activeInnings.legalBalls % 6 == 0) {
                MatchSimulationEngine.simulateNextOver(match)
            } else {
                MatchSimulationEngine.simulateNextBall(match)
            }
        }
    }

    private fun buildPlayerTurnHandoffMessage(
        match: LiveMatchState,
        userTeamId: String,
        careerPlayerName: String?
    ): String {
        val proLabel = careerPlayerName?.takeIf { it.isNotBlank() } ?: "Your Pro"
        val userBat = InteractivePhaseResolver.isUserTeamBatting(match, userTeamId)
        return if (userBat) {
            val tgtText = match.target?.let { " • Target $it" }.orEmpty()
            "🏏 $proLabel's turn to BAT$tgtText!"
        } else {
            val overNum = (match.activeInnings.legalBalls / 6) + 1
            "🎯 $proLabel's turn to BOWL (Over $overNum)!"
        }
    }

    /**
     * Automatically fast-simulates any innings or deliveries outside the user's control:
     * - In Strict Player-Only Career Mode: fast-simulates all deliveries/overs whenever the
     *   created player is not on strike batting and not bowling, then pauses and signals
     *   interactive Landscape gameplay the exact moment their turn begins.
     * - In Team Bat-Only / Bowl-Only modes: fast-simulates the skipped innings.
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
        val shouldAutoSim = InteractivePhaseResolver.shouldAutoSimAnyReason(
            match = currentMatch,
            userTeamId = currentState.userTeamId,
            filter = currentState.playFilter,
            careerPlayerId = currentState.careerPlayerId,
            controlMode = currentState.careerControlMode
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
                val stillSkipped = InteractivePhaseResolver.shouldAutoSimAnyReason(
                    match = m,
                    userTeamId = st.userTeamId,
                    filter = st.playFilter,
                    careerPlayerId = st.careerPlayerId,
                    controlMode = st.careerControlMode
                )
                if (!stillSkipped) {
                    val isPlayerOnly = st.careerControlMode == CareerControlMode.PLAYER_ONLY &&
                        !st.careerPlayerId.isNullOrBlank()
                    val handoffMsg = if (isPlayerOnly) {
                        buildPlayerTurnHandoffMessage(m, st.userTeamId, st.careerPlayerName)
                    } else {
                        val userBat = InteractivePhaseResolver.isUserTeamBatting(m, st.userTeamId)
                        if (userBat) {
                            val tgtText = m.target?.let { " • Target $it" }.orEmpty()
                            "🏏 Your turn to BAT$tgtText!"
                        } else {
                            val tgtText = m.target?.let { " • Defend ${it - 1}" }.orEmpty()
                            "🎯 Your turn to BOWL$tgtText!"
                        }
                    }
                    _uiState.update {
                        it.copy(
                            autoSimmingSkippedPhase = false,
                            autoJumpLandscapeSignal = if (isPlayerOnly) it.autoJumpLandscapeSignal + 1L else it.autoJumpLandscapeSignal,
                            statusNotice = handoffMsg
                        )
                    }
                    break
                }

                val isPlayerOnly = st.careerControlMode == CareerControlMode.PLAYER_ONLY &&
                    !st.careerPlayerId.isNullOrBlank()
                val updated = if (isPlayerOnly) {
                    stepAutoSimForPlayerOnly(m, st.userTeamId, st.careerPlayerId!!)
                } else if (m.isSuperOverActive) {
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
                val delayMs = when {
                    !isPlayerOnly -> 200L
                    InteractivePhaseResolver.isUserTeamBatting(updated, st.userTeamId) -> {
                        val outNow = updated.activeInnings.battingCard
                            .firstOrNull { it.playerId == st.careerPlayerId }?.out == true
                        if (outNow) 130L else 70L
                    }
                    else -> 130L
                }
                delay(delayMs)
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
