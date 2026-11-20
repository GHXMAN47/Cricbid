package com.example.engine

import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.TeamState
import java.util.Locale

data class TeamStanding(
    val teamId: String,
    val teamName: String,
    val bot: Boolean,
    val played: Int,
    val won: Int,
    val lost: Int,
    val tied: Int,
    val points: Int,
    val nrr: Double,
    val runsFor: Long = 0L,
    val ballsFor: Long = 0L,
    val runsAgainst: Long = 0L,
    val ballsAgainst: Long = 0L,
    val recentForm: List<String> = emptyList()
) {
    val nrrFormatted: String
        get() = String.format(Locale.US, "%+.3f", nrr)

    val oversForFormatted: String
        get() = "${ballsFor / 6}.${ballsFor % 6}"

    val oversAgainstFormatted: String
        get() = "${ballsAgainst / 6}.${ballsAgainst % 6}"

    val winPercentage: Double
        get() = if (played > 0) (won * 100.0) / played else 0.0
}

data class SeriesSummary(
    val teamA: TeamState,
    val teamB: TeamState,
    val winsA: Int,
    val winsB: Int,
    val ties: Int,
    val matchesPlayed: Int,
    val seriesCompleted: Boolean,
    val seriesWinnerName: String?,
    val playerOfTheSeries: String?
) {
    val nextMatchNumber: Int
        get() = (matchesPlayed + 1).coerceAtMost(5)
}

data class PlayoffSlot(
    val stage: MatchStage,
    val title: String,
    val subtitle: String,
    val teamA: TeamState?,
    val teamB: TeamState?,
    val completedMatch: MatchRecord?
) {
    val isPlayable: Boolean
        get() = completedMatch == null && teamA != null && teamB != null
}

data class TournamentOverview(
    val isSeriesMode: Boolean,
    val seriesSummary: SeriesSummary?,
    val standings: List<TeamStanding>,
    val allLeagueFixtures: List<Pair<TeamState, TeamState>>,
    val unplayedLeagueFixtures: List<Pair<TeamState, TeamState>>,
    val leagueCompleted: Boolean,
    val playoffSlots: List<PlayoffSlot>,
    val championTeamName: String?
)

/**
 * Dynamic Tournament Engine:
 * - 2 Active Teams -> 5-Match T20 Series (Scoreline, Series Winner, Player of the Series)
 * - 4..8 Active Teams -> Round-Robin League (Points Table + exact Net Run Rate) + IPL Playoffs
 *   (Qualifier 1, Eliminator, Qualifier 2, Grand Final).
 */
object TournamentEngine {

    fun buildOverview(teams: List<TeamState>, matches: List<MatchRecord>): TournamentOverview {
        if (teams.size == 2) {
            val teamA = teams[0]
            val teamB = teams[1]
            val seriesMatches = matches.filter {
                it.matchStage == MatchStage.SERIES || it.matchStage == MatchStage.LEAGUE
            }.take(5)
            val winsA = seriesMatches.count { it.winnerTeamId == teamA.teamId }
            val winsB = seriesMatches.count { it.winnerTeamId == teamB.teamId }
            val ties = seriesMatches.count { it.winnerTeamId == "TIE" }
            val completed = winsA >= 3 || winsB >= 3 || seriesMatches.size >= 5
            val winnerName = when {
                !completed -> null
                winsA > winsB -> teamA.teamName
                winsB > winsA -> teamB.teamName
                else -> "Series Shared"
            }
            val pots = computePlayerOfTheSeries(seriesMatches)
            val standings = computeStandings(teams, seriesMatches)
            return TournamentOverview(
                isSeriesMode = true,
                seriesSummary = SeriesSummary(
                    teamA = teamA,
                    teamB = teamB,
                    winsA = winsA,
                    winsB = winsB,
                    ties = ties,
                    matchesPlayed = seriesMatches.size,
                    seriesCompleted = completed,
                    seriesWinnerName = winnerName,
                    playerOfTheSeries = pots
                ),
                standings = standings,
                allLeagueFixtures = emptyList(),
                unplayedLeagueFixtures = emptyList(),
                leagueCompleted = false,
                playoffSlots = emptyList(),
                championTeamName = winnerName
            )
        }

        // League + IPL Playoffs Mode (3..8 teams; Playoffs unlock for 4+ teams)
        val leagueMatches = matches.filter { it.matchStage == MatchStage.LEAGUE }
        val standings = computeStandings(teams, leagueMatches)
        val allFixtures = buildSingleRoundRobin(teams)
        val playedKeys = leagueMatches.map { pairKey(it.teamAId, it.teamBId) }.toMutableList()
        val unplayed = mutableListOf<Pair<TeamState, TeamState>>()
        for (fixture in allFixtures) {
            val k = pairKey(fixture.first.teamId, fixture.second.teamId)
            val idx = playedKeys.indexOf(k)
            if (idx >= 0) {
                playedKeys.removeAt(idx)
            } else {
                unplayed.add(fixture)
            }
        }

        val leagueCompleted = teams.size >= 4 && unplayed.isEmpty()
        val teamById = teams.associateBy { it.teamId }

        val q1Match = matches.lastOrNull { it.matchStage == MatchStage.QUALIFIER_1 }
        val elimMatch = matches.lastOrNull { it.matchStage == MatchStage.ELIMINATOR }
        val q2Match = matches.lastOrNull { it.matchStage == MatchStage.QUALIFIER_2 }
        val finalMatch = matches.lastOrNull { it.matchStage == MatchStage.FINAL }

        val top1 = if (leagueCompleted) standings.getOrNull(0)?.teamId?.let { teamById[it] } else null
        val top2 = if (leagueCompleted) standings.getOrNull(1)?.teamId?.let { teamById[it] } else null
        val top3 = if (leagueCompleted) standings.getOrNull(2)?.teamId?.let { teamById[it] } else null
        val top4 = if (leagueCompleted) standings.getOrNull(3)?.teamId?.let { teamById[it] } else null

        val q1Winner = resolveWinnerTeam(q1Match, teamById)
        val q1Loser = resolveLoserTeam(q1Match, teamById)
        val elimWinner = resolveWinnerTeam(elimMatch, teamById)
        val q2Winner = resolveWinnerTeam(q2Match, teamById)
        val champion = resolveWinnerTeam(finalMatch, teamById)?.teamName

        val playoffSlots = if (teams.size >= 4) {
            listOf(
                PlayoffSlot(
                    stage = MatchStage.QUALIFIER_1,
                    title = "QUALIFIER 1",
                    subtitle = "#1 vs #2 • Winner enters Final",
                    teamA = q1Match?.teamAId?.let { teamById[it] } ?: top1,
                    teamB = q1Match?.teamBId?.let { teamById[it] } ?: top2,
                    completedMatch = q1Match
                ),
                PlayoffSlot(
                    stage = MatchStage.ELIMINATOR,
                    title = "ELIMINATOR",
                    subtitle = "#3 vs #4 • Knockout clash",
                    teamA = elimMatch?.teamAId?.let { teamById[it] } ?: top3,
                    teamB = elimMatch?.teamBId?.let { teamById[it] } ?: top4,
                    completedMatch = elimMatch
                ),
                PlayoffSlot(
                    stage = MatchStage.QUALIFIER_2,
                    title = "QUALIFIER 2",
                    subtitle = "Loser Q1 vs Winner Eliminator",
                    teamA = q2Match?.teamAId?.let { teamById[it] } ?: q1Loser,
                    teamB = q2Match?.teamBId?.let { teamById[it] } ?: elimWinner,
                    completedMatch = q2Match
                ),
                PlayoffSlot(
                    stage = MatchStage.FINAL,
                    title = "GRAND FINAL",
                    subtitle = "Winner Q1 vs Winner Q2 • Trophy Decider",
                    teamA = finalMatch?.teamAId?.let { teamById[it] } ?: q1Winner,
                    teamB = finalMatch?.teamBId?.let { teamById[it] } ?: q2Winner,
                    completedMatch = finalMatch
                )
            )
        } else {
            emptyList()
        }

        return TournamentOverview(
            isSeriesMode = false,
            seriesSummary = null,
            standings = standings,
            allLeagueFixtures = allFixtures,
            unplayedLeagueFixtures = unplayed,
            leagueCompleted = leagueCompleted,
            playoffSlots = playoffSlots,
            championTeamName = champion
        )
    }

    fun computeStandings(teams: List<TeamState>, matches: List<MatchRecord>): List<TeamStanding> {
        return teams.map { team ->
            var played = 0
            var won = 0
            var tied = 0
            var runsFor = 0L
            var ballsFor = 0L
            var runsAgainst = 0L
            var ballsAgainst = 0L
            val form = mutableListOf<String>()

            matches.forEach { m ->
                val isA = m.teamAId == team.teamId
                val isB = m.teamBId == team.teamId
                if (isA || isB) {
                    played++
                    when (m.winnerTeamId) {
                        team.teamId -> {
                            won++
                            form.add("W")
                        }
                        "TIE" -> {
                            tied++
                            form.add("T")
                        }
                        else -> form.add("L")
                    }
                    if (isA) {
                        runsFor += m.teamAScore
                        ballsFor += m.teamABalls.coerceAtLeast(1L)
                        runsAgainst += m.teamBScore
                        ballsAgainst += m.teamBBalls.coerceAtLeast(1L)
                    } else {
                        runsFor += m.teamBScore
                        ballsFor += m.teamBBalls.coerceAtLeast(1L)
                        runsAgainst += m.teamAScore
                        ballsAgainst += m.teamABalls.coerceAtLeast(1L)
                    }
                }
            }

            val lost = (played - won - tied).coerceAtLeast(0)
            val points = won * 2 + tied
            val nrr = if (ballsFor > 0L && ballsAgainst > 0L) {
                (runsFor.toDouble() / (ballsFor / 6.0)) - (runsAgainst.toDouble() / (ballsAgainst / 6.0))
            } else {
                0.0
            }

            TeamStanding(
                teamId = team.teamId,
                teamName = team.teamName,
                bot = team.bot,
                played = played,
                won = won,
                lost = lost,
                tied = tied,
                points = points,
                nrr = nrr,
                runsFor = runsFor,
                ballsFor = ballsFor,
                runsAgainst = runsAgainst,
                ballsAgainst = ballsAgainst,
                recentForm = form.takeLast(5)
            )
        }.sortedWith(
            compareByDescending<TeamStanding> { it.points }
                .thenByDescending { it.nrr }
                .thenByDescending { it.won }
                .thenBy { it.teamName }
        )
    }

    private fun buildSingleRoundRobin(teams: List<TeamState>): List<Pair<TeamState, TeamState>> {
        val fixtures = mutableListOf<Pair<TeamState, TeamState>>()
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                fixtures.add(teams[i] to teams[j])
            }
        }
        return fixtures
    }

    private fun pairKey(idA: String, idB: String): String =
        if (idA <= idB) "${idA}__${idB}" else "${idB}__${idA}"

    private fun resolveWinnerTeam(match: MatchRecord?, teamById: Map<String, TeamState>): TeamState? {
        if (match == null) return null
        if (match.winnerTeamId == "TIE") {
            // Tie-breaker in knockouts goes to teamA (higher league seed)
            return teamById[match.teamAId]
        }
        return teamById[match.winnerTeamId]
    }

    private fun resolveLoserTeam(match: MatchRecord?, teamById: Map<String, TeamState>): TeamState? {
        if (match == null) return null
        val winner = resolveWinnerTeam(match, teamById) ?: return null
        return if (winner.teamId == match.teamAId) teamById[match.teamBId] else teamById[match.teamAId]
    }

    private fun computePlayerOfTheSeries(matches: List<MatchRecord>): String? {
        if (matches.isEmpty()) return null
        val counts = mutableMapOf<String, Int>()
        matches.forEach { m ->
            val rawName = m.playerOfTheMatch.substringBefore(" (").trim()
            if (rawName.isNotBlank() && rawName != "Match Star") {
                counts[rawName] = (counts[rawName] ?: 0) + 1
            }
        }
        val best = counts.entries.maxByOrNull { it.value } ?: return null
        return if (best.value > 1) "${best.key} (${best.value}x POTM)" else best.key
    }
}
