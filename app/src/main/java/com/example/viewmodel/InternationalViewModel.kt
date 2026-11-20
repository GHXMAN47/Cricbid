package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalInternationalStore
import com.example.data.model.InternationalCampaignState
import com.example.data.model.InternationalMode
import com.example.data.model.InternationalNations
import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.TeamState
import com.example.engine.MatchSimulationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InternationalUiState(
    val campaign: InternationalCampaignState = InternationalCampaignState(),
    val busy: Boolean = false,
    val snackbarMessage: String? = null
)

class InternationalViewModel(
    private val store: LocalInternationalStore,
    private val currentUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InternationalUiState(
            campaign = store.loadCampaign(store.loadActiveFormat())
        )
    )
    val uiState: StateFlow<InternationalUiState> = _uiState.asStateFlow()

    fun selectFormat(format: MatchFormat) {
        if (_uiState.value.campaign.format == format) return
        store.saveActiveFormat(format)
        val loaded = store.loadCampaign(format)
        _uiState.update { it.copy(campaign = loaded) }
    }

    fun selectMode(mode: InternationalMode) {
        updateCampaign { it.copy(mode = mode) }
    }

    fun selectUserNation(nationId: String) {
        val current = _uiState.value.campaign
        if (current.userNationId == nationId) return
        val newOpponent = if (current.opponentNationId == nationId) {
            InternationalNations.all.first { it.id != nationId }.id
        } else {
            current.opponentNationId
        }
        val resetSeries = current.mode == InternationalMode.SERIES && current.seriesMatches.isNotEmpty()
        updateCampaign {
            it.copy(
                userNationId = nationId,
                opponentNationId = newOpponent,
                seriesMatches = if (resetSeries) emptyList() else it.seriesMatches
            )
        }
    }

    fun selectOpponentNation(nationId: String) {
        val current = _uiState.value.campaign
        if (current.opponentNationId == nationId || current.userNationId == nationId) return
        updateCampaign {
            it.copy(
                opponentNationId = nationId,
                seriesMatches = emptyList()
            )
        }
    }

    fun selectSeriesLength(matchesCount: Int) {
        if (matchesCount !in InternationalNations.seriesLengthOptions) return
        val current = _uiState.value.campaign
        if (current.seriesTotalMatches == matchesCount) return
        updateCampaign {
            it.copy(
                seriesTotalMatches = matchesCount,
                seriesMatches = emptyList()
            )
        }
    }

    fun recordMatchResult(record: MatchRecord) {
        val mode = _uiState.value.campaign.mode
        updateCampaign { current ->
            if (mode == InternationalMode.SERIES || record.matchStage == MatchStage.SERIES) {
                current.copy(seriesMatches = current.seriesMatches + record)
            } else {
                current.copy(worldCupMatches = current.worldCupMatches + record)
            }
        }
    }

    fun quickSimWorldCupFixtures(fixtures: List<Pair<TeamState, TeamState>>) {
        if (fixtures.isEmpty() || _uiState.value.busy) return
        val format = _uiState.value.campaign.format
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            val simulatedRecords = withContext(Dispatchers.Default) {
                fixtures.mapIndexed { idx, (teamA, teamB) ->
                    val sim = MatchSimulationEngine.simulateFullMatch(
                        MatchSimulationEngine.createMatch(
                            teamA = teamA,
                            teamB = teamB,
                            stage = MatchStage.LEAGUE,
                            format = format
                        )
                    )
                    sim.toMatchRecord(
                        roomCode = "INTL_${format.name}",
                        hostUid = currentUserId,
                        memberUids = listOf(currentUserId)
                    ).copy(id = "wc_${System.currentTimeMillis()}_$idx")
                }
            }
            updateCampaign { current ->
                current.copy(worldCupMatches = current.worldCupMatches + simulatedRecords)
            }
            _uiState.update {
                it.copy(
                    busy = false,
                    snackbarMessage = "Simulated ${simulatedRecords.size} ${format.shortLabel} World Cup matches!"
                )
            }
        }
    }

    fun resetCurrentModeCampaign() {
        val current = _uiState.value.campaign
        if (current.mode == InternationalMode.SERIES) {
            updateCampaign { it.copy(seriesMatches = emptyList()) }
            _uiState.update { it.copy(snackbarMessage = "Started a fresh ${current.format.shortLabel} Series!") }
        } else {
            updateCampaign { it.copy(worldCupMatches = emptyList()) }
            _uiState.update { it.copy(snackbarMessage = "Started a fresh ${current.format.worldCupTitle}!") }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun updateCampaign(transform: (InternationalCampaignState) -> InternationalCampaignState) {
        val updated = transform(_uiState.value.campaign)
        store.saveCampaign(updated)
        _uiState.update { it.copy(campaign = updated) }
    }
}
