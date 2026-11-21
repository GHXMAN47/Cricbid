package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalCareerStore
import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerDifficulty
import com.example.data.model.CareerMatchType
import com.example.data.model.CareerPlayerAppearance
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerSkillStat
import com.example.data.model.CareerStage
import com.example.data.model.HallOfFameLegend
import com.example.data.model.PlayerRole
import com.example.data.model.ProT20League
import com.example.data.model.TeamState
import com.example.data.model.TrainingDrill
import com.example.engine.CareerEngine
import com.example.engine.IplAuctionDraftOutcome
import com.example.engine.LiveMatchState
import com.example.engine.MatchSimulationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CareerUiState(
    val activeCareer: CareerSaveState? = null,
    val hallOfFame: List<HallOfFameLegend> = emptyList(),
    val latestAuctionDraft: IplAuctionDraftOutcome? = null,
    val latestRetiredLegend: HallOfFameLegend? = null,
    val lastEventBanner: String? = null,
    val snackbarMessage: String? = null,
    val busy: Boolean = false
)

class CareerViewModel(
    private val store: LocalCareerStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CareerUiState(
            activeCareer = store.loadActiveCareer(),
            hallOfFame = store.loadHallOfFame()
        )
    )
    val uiState: StateFlow<CareerUiState> = _uiState.asStateFlow()

    fun reloadFromStore() {
        _uiState.update {
            it.copy(
                activeCareer = store.loadActiveCareer(),
                hallOfFame = store.loadHallOfFame(),
                latestAuctionDraft = null,
                latestRetiredLegend = null
            )
        }
    }

    fun createRookieCareer(
        name: String,
        startingAge: Int,
        nationality: String,
        countryCode: String,
        avatarId: String,
        role: PlayerRole,
        battingStyle: BattingStyle,
        bowlingStyle: BowlingStyle,
        difficulty: CareerDifficulty = CareerDifficulty.NORMAL,
        appearance: CareerPlayerAppearance = CareerPlayerAppearance()
    ) {
        val rookie = CareerEngine.createRookie(
            name = name,
            startingAge = startingAge,
            nationality = nationality,
            countryCode = countryCode,
            avatarId = avatarId,
            role = role,
            battingStyle = battingStyle,
            bowlingStyle = bowlingStyle,
            difficulty = difficulty,
            appearance = appearance
        )
        store.saveActiveCareer(rookie)
        _uiState.update {
            it.copy(
                activeCareer = rookie,
                latestAuctionDraft = null,
                latestRetiredLegend = null,
                lastEventBanner = "Welcome to Hometown Club Cricket (${difficulty.label} Mode)! Complete Match Objectives to earn a Domestic Call-Up.",
                snackbarMessage = "${rookie.name} (${rookie.overallRating} OVR) signed for ${rookie.clubTeamName}!"
            )
        }
    }

    fun updateDifficulty(difficulty: CareerDifficulty) {
        val current = _uiState.value.activeCareer ?: return
        if (current.difficulty == difficulty) return
        val updated = current.copy(difficulty = difficulty)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = "Difficulty switched to ${difficulty.label} (${difficulty.summary})",
                snackbarMessage = "Career Difficulty set to ${difficulty.label}"
            )
        }
    }

    fun refreshObjectiveForMatchType(matchType: CareerMatchType) {
        val current = _uiState.value.activeCareer ?: return
        val newObjective = CareerEngine.generateMatchObjective(current, matchType)
        if (current.currentObjective?.title == newObjective.title) return
        val updated = current.copy(currentObjective = newObjective)
        store.saveActiveCareer(updated)
        _uiState.update { it.copy(activeCareer = updated) }
    }

    fun spendSkillPoint(stat: CareerSkillStat) {
        val current = _uiState.value.activeCareer ?: return
        val (updated, message) = CareerEngine.spendSkillPoint(current, stat)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = message,
                snackbarMessage = message
            )
        }
    }

    fun completeSelectionTrial() {
        val current = _uiState.value.activeCareer ?: return
        val (updated, message) = CareerEngine.runSelectionTrial(current)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = message,
                snackbarMessage = message
            )
        }
    }

    fun promoteToDomestic() {
        val current = _uiState.value.activeCareer ?: return
        if (current.stage != CareerStage.CLUB_CRICKET) return
        val updated = CareerEngine.promoteToDomestic(current)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = "Promoted to Domestic Cricket! Compete in First-Class, 50-Over One-Day & Domestic T20 for ${updated.domesticTeamName}.",
                snackbarMessage = "Promoted to ${updated.domesticTeamName} (+1 Skill Point)!"
            )
        }
    }

    fun runTrainingDrill(drill: TrainingDrill) {
        val current = _uiState.value.activeCareer ?: return
        val outcome = CareerEngine.applyTrainingDrill(current, drill)
        store.saveActiveCareer(outcome.updatedCareer)
        _uiState.update {
            it.copy(
                activeCareer = outcome.updatedCareer,
                lastEventBanner = outcome.message,
                snackbarMessage = outcome.message
            )
        }
    }

    fun enterIplAuction(league: ProT20League = ProT20League.IPL) {
        val current = _uiState.value.activeCareer ?: return
        if (current.stage != CareerStage.DOMESTIC) return
        val draft = CareerEngine.runIplAuctionDraftEvent(current, league = league)
        store.saveActiveCareer(draft.updatedCareer)
        _uiState.update {
            it.copy(
                activeCareer = draft.updatedCareer,
                latestAuctionDraft = draft,
                lastEventBanner = draft.biddingHighlights.lastOrNull(),
                snackbarMessage = "SOLD! ${draft.winningFranchise} signed ${current.name} (${league.displayName})!"
            )
        }
    }

    fun selectProLeague(league: ProT20League) {
        val current = _uiState.value.activeCareer ?: return
        if (current.stage != CareerStage.IPL_FRANCHISE || current.activeProLeague == league) return
        val updated = CareerEngine.switchProLeague(current, league)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = "Signed ${league.displayName} contract with ${updated.iplTeamName}!",
                snackbarMessage = "Joined ${updated.iplTeamName} (${league.displayName})"
            )
        }
    }

    fun dismissAuctionDraftSummary() {
        _uiState.update { it.copy(latestAuctionDraft = null) }
    }

    fun acceptInternationalCallUp() {
        val current = _uiState.value.activeCareer ?: return
        val updated = CareerEngine.acceptInternationalCallUp(current)
        store.saveActiveCareer(updated)
        _uiState.update {
            it.copy(
                activeCareer = updated,
                lastEventBanner = "Selected for ${updated.nationalTeamName}! Play T20I, 50-Over ODI, Test & World Championships.",
                snackbarMessage = "Maiden International Call-Up for ${updated.nationalTeamName}!"
            )
        }
    }

    fun prepareLiveMatch(matchType: CareerMatchType): Triple<TeamState, TeamState, Int>? {
        val current = _uiState.value.activeCareer ?: return null
        if (current.isInjured) {
            _uiState.update {
                it.copy(snackbarMessage = "You have a minor strain. Run Fitness & Recovery before playing!")
            }
            return null
        }
        if (current.isDropped) {
            _uiState.update {
                it.copy(snackbarMessage = "You were dropped from the XI! Complete a Selection Net Trial first.")
            }
            return null
        }
        val withObjective = current.copy(
            currentObjective = CareerEngine.generateMatchObjective(current, matchType)
        )
        store.saveActiveCareer(withObjective)
        _uiState.update { it.copy(activeCareer = withObjective) }
        val (myTeam, oppTeam) = CareerEngine.buildCareerMatchTeams(withObjective, matchType)
        return Triple(myTeam, oppTeam, withObjective.difficulty.opponentRatingOffset)
    }

    fun recordLiveMatchResult(match: LiveMatchState, matchType: CareerMatchType) {
        val current = _uiState.value.activeCareer ?: return
        val resolution = CareerEngine.resolveCompletedMatch(current, match, matchType)
        handleResolution(resolution)
    }

    fun quickSimCareerMatch(matchType: CareerMatchType) {
        val current = _uiState.value.activeCareer ?: return
        if (_uiState.value.busy) return
        if (current.isInjured) {
            _uiState.update {
                it.copy(snackbarMessage = "You have a minor strain. Run Fitness & Recovery before playing!")
            }
            return
        }
        if (current.isDropped) {
            _uiState.update {
                it.copy(snackbarMessage = "You were dropped from the XI! Complete a Selection Net Trial first.")
            }
            return
        }
        val preparedCareer = current.copy(
            currentObjective = CareerEngine.generateMatchObjective(current, matchType)
        )
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, activeCareer = preparedCareer) }
            val resolution = withContext(Dispatchers.Default) {
                val (myTeam, oppTeam) = CareerEngine.buildCareerMatchTeams(preparedCareer, matchType)
                val initial = MatchSimulationEngine.createMatch(
                    teamA = myTeam,
                    teamB = oppTeam,
                    stage = matchType.stage,
                    format = matchType.format,
                    teamBRatingOffset = preparedCareer.difficulty.opponentRatingOffset
                )
                val finished = MatchSimulationEngine.simulateFullMatch(initial)
                CareerEngine.resolveCompletedMatch(preparedCareer, finished, matchType)
            }
            handleResolution(resolution)
            _uiState.update { it.copy(busy = false) }
        }
    }

    private fun handleResolution(resolution: com.example.engine.CareerMatchResolution) {
        val career = resolution.updatedCareer
        // Mandatory retirement at age 40 -> automatic Hall of Fame induction
        if (career.age >= 40 && career.stage != CareerStage.RETIRED) {
            val (retired, legend) = CareerEngine.retireToHallOfFame(career)
            val updatedHof = (listOf(legend) + _uiState.value.hallOfFame.filterNot { it.id == legend.id })
            store.saveActiveCareer(null)
            store.saveHallOfFame(updatedHof)
            _uiState.update {
                it.copy(
                    activeCareer = null,
                    hallOfFame = updatedHof,
                    latestRetiredLegend = legend,
                    lastEventBanner = "${retired.name} retired at age ${retired.age} into the Hall of Fame!",
                    snackbarMessage = "Legend Inducted into Hall of Fame & Future Auction Pool!"
                )
            }
            return
        }

        store.saveActiveCareer(career)
        val banner = listOfNotNull(
            resolution.newlyUnlockedStage,
            resolution.milestoneBanner,
            resolution.summaryMessage
        ).joinToString(" • ")

        _uiState.update {
            it.copy(
                activeCareer = career,
                lastEventBanner = banner,
                snackbarMessage = resolution.summaryMessage
            )
        }
    }

    fun retireActiveCareerToHallOfFame() {
        val current = _uiState.value.activeCareer ?: return
        val (_, legend) = CareerEngine.retireToHallOfFame(current)
        val updatedHof = (listOf(legend) + _uiState.value.hallOfFame.filterNot { it.id == legend.id })
        store.saveActiveCareer(null)
        store.saveHallOfFame(updatedHof)
        _uiState.update {
            it.copy(
                activeCareer = null,
                hallOfFame = updatedHof,
                latestRetiredLegend = legend,
                lastEventBanner = "${legend.name} inducted into the Hall of Fame & Future Auction Pool!",
                snackbarMessage = "${legend.name} added to Hall of Fame Legends!"
            )
        }
    }

    fun dismissRetirementSummary() {
        _uiState.update { it.copy(latestRetiredLegend = null) }
    }

    fun toggleLegendInAuctions(legendId: String, include: Boolean) {
        val updated = _uiState.value.hallOfFame.map { l ->
            if (l.id == legendId) l.copy(includeInAuctions = include) else l
        }
        store.saveHallOfFame(updated)
        _uiState.update {
            it.copy(
                hallOfFame = updated,
                snackbarMessage = if (include) "Legend enabled for future IPL Auctions!" else "Legend paused from future auctions."
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
