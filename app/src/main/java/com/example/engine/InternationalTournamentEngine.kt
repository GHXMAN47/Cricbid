package com.example.engine

import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.TeamState

data class CustomSeriesSummary(
    val format: MatchFormat,
    val teamA: TeamState,
    val teamB: TeamState,
    val totalMatches: Int,
    val winsA: Int,
    val winsB: Int,
    val drawsOrTies: Int,
    val matchesPlayed: Int,
    val seriesCompleted: Boolean,
    val seriesWinnerName: String?,
    val playerOfTheSeries: String?
) {
    val nextMatchNumber: Int
        get() = (matchesPlayed + 1).coerceAtMost(totalMatches)
}

data class WorldCupOverview(
    val format: MatchFormat,
    val title: String,
    val standings: List<TeamStanding>,
    val allGroupFixtures: List<Pair<TeamState, TeamState>>,
    val unplayedGroupFixtures: List<Pair<TeamState, TeamState>>,
    val groupStageCompleted: Boolean,
    val knockoutSlots: List<PlayoffSlot>,
    val championTeamName: String?,
    val qualifierCount: Int
)

/**
 * International Tournament Engine supporting:
 * 1. Custom-length Bilateral Series (1, 3, 5, or 7 matches) across T20I, ODI, and Test.
 * 2. World Cup / WTC Brackets:
 *    - T20I & ODI World Cup: 8-Nation Group Stage -> Semi-Final 1 (#1 vs #4), Semi-Final 2 (#2 vs #3) -> World Cup Final.
 *    - Test (World Test Championship): 8-Nation WTC League -> WTC Grand Final (#1 vs #2).
 */
object InternationalTournamentEngine {

    fun buildCustomSeriesSummary(
        format: MatchFormat,
        teamA: TeamState,
        teamB: TeamState,
        totalMatches: Int,
        matches: List<MatchRecord>
    ): CustomSeriesSummary {
        val relevant = matches.filter {
            (it.teamAId == teamA.teamId && it.teamBId == teamB.teamId) ||
                (it.teamAId == teamB.teamId && it.teamBId == teamA.teamId)
        }.take(totalMatches)

        val winsA = relevant.count { it.winnerTeamId == teamA.teamId }
        val winsB = relevant.count { it.winnerTeamId == teamB.teamId }
        val drawsOrTies = relevant.count { it.winnerTeamId == "TIE" || it.winnerTeamId == "DRAW" }
        val majorityNeeded = (totalMatches / 2) + 1
        val completed = relevant.size >= totalMatches || winsA >= majorityNeeded || winsB >= majorityNeeded

        val winnerName = when {
            !completed -> null
            winsA > winsB -> teamA.teamName
            winsB > winsA -> teamB.teamName
            else -> "Series Drawn"
        }

        val pots = computePlayerOfTheSeries(relevant)

        return CustomSeriesSummary(
            format = format,
            teamA = teamA,
            teamB = teamB,
            totalMatches = totalMatches,
            winsA = winsA,
            winsB = winsB,
            drawsOrTies = drawsOrTies,
            matchesPlayed = relevant.size,
            seriesCompleted = completed,
            seriesWinnerName = winnerName,
            playerOfTheSeries = pots
        )
    }

    fun buildWorldCupOverview(
        format: MatchFormat,
        nations: List<TeamState>,
        matches: List<MatchRecord>
    ): WorldCupOverview {
        val groupMatches = matches.filter { it.matchStage == MatchStage.LEAGUE }
        val standings = computeInternationalStandings(format, nations, groupMatches)
        val allFixtures = buildSingleRoundRobin(nations)

        val playedKeys = groupMatches.map { pairKey(it.teamAId, it.teamBId) }.toMutableList()
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

        val groupCompleted = nations.size >= 4 && unplayed.isEmpty()
        val teamById = nations.associateBy { it.teamId }

        if (format == MatchFormat.TEST) {
            // World Test Championship: League Table -> WTC Grand Final (#1 vs #2)
            val wtcFinalMatch = matches.lastOrNull { it.matchStage == MatchStage.WTC_FINAL || it.matchStage == MatchStage.FINAL }
            val top1 = if (groupCompleted) standings.getOrNull(0)?.teamId?.let { teamById[it] } else null
            val top2 = if (groupCompleted) standings.getOrNull(1)?.teamId?.let { teamById[it] } else null
            val champion = resolveKnockoutWinner(wtcFinalMatch, teamById)?.teamName

            val slots = listOf(
                PlayoffSlot(
                    stage = MatchStage.WTC_FINAL,
                    title = "WTC GRAND FINAL",
                    subtitle = "#1 vs #2 • Test Mace Decider",
                    teamA = wtcFinalMatch?.teamAId?.let { teamById[it] } ?: top1,
                    teamB = wtcFinalMatch?.teamBId?.let { teamById[it] } ?: top2,
                    completedMatch = wtcFinalMatch
                )
            )

            return WorldCupOverview(
                format = format,
                title = format.worldCupTitle,
                standings = standings,
                allGroupFixtures = allFixtures,
                unplayedGroupFixtures = unplayed,
                groupStageCompleted = groupCompleted,
                knockoutSlots = slots,
                championTeamName = champion,
                qualifierCount = 2
            )
        }

        // T20I & ODI World Cup: League/Group -> Semi-Final 1 (#1 vs #4), Semi-Final 2 (#2 vs #3) -> World Cup Final
        val sf1Match = matches.lastOrNull { it.matchStage == MatchStage.SEMI_FINAL_1 }
        val sf2Match = matches.lastOrNull { it.matchStage == MatchStage.SEMI_FINAL_2 }
        val finalMatch = matches.lastOrNull { it.matchStage == MatchStage.FINAL }

        val top1 = if (groupCompleted) standings.getOrNull(0)?.teamId?.let { teamById[it] } else null
        val top2 = if (groupCompleted) standings.getOrNull(1)?.teamId?.let { teamById[it] } else null
        val top3 = if (groupCompleted) standings.getOrNull(2)?.teamId?.let { teamById[it] } else null
        val top4 = if (groupCompleted) standings.getOrNull(3)?.teamId?.let { teamById[it] } else null

        val sf1Winner = resolveKnockoutWinner(sf1Match, teamById)
        val sf2Winner = resolveKnockoutWinner(sf2Match, teamById)
        val champion = resolveKnockoutWinner(finalMatch, teamById)?.teamName

        val slots = listOf(
            PlayoffSlot(
                stage = MatchStage.SEMI_FINAL_1,
                title = "SEMI-FINAL 1",
                subtitle = "#1 vs #4 • Knockout",
                teamA = sf1Match?.teamAId?.let { teamById[it] } ?: top1,
                teamB = sf1Match?.teamBId?.let { teamById[it] } ?: top4,
                completedMatch = sf1Match
            ),
            PlayoffSlot(
                stage = MatchStage.SEMI_FINAL_2,
                title = "SEMI-FINAL 2",
                subtitle = "#2 vs #3 • Knockout",
                teamA = sf2Match?.teamAId?.let { teamById[it] } ?: top2,
                teamB = sf2Match?.teamBId?.let { teamById[it] } ?: top3,
                completedMatch = sf2Match
            ),
            PlayoffSlot(
                stage = MatchStage.FINAL,
                title = "${format.shortLabel} WORLD CUP FINAL",
                subtitle = "Winner SF1 vs Winner SF2 • World Trophy",
                teamA = finalMatch?.teamAId?.let { teamById[it] } ?: sf1Winner,
                teamB = finalMatch?.teamBId?.let { teamById[it] } ?: sf2Winner,
                completedMatch = finalMatch
            )
        )

        return WorldCupOverview(
            format = format,
            title = format.worldCupTitle,
            standings = standings,
            allGroupFixtures = allFixtures,
            unplayedGroupFixtures = unplayed,
            groupStageCompleted = groupCompleted,
            knockoutSlots = slots,
            championTeamName = champion,
            qualifierCount = 4
        )
    }

    fun computeInternationalStandings(
        format: MatchFormat,
        nations: List<TeamState>,
        matches: List<MatchRecord>
    ): List<TeamStanding> {
        val winPts = if (format == MatchFormat.TEST) 12 else 2
        val drawPts = if (format == MatchFormat.TEST) 4 else 1

        return nations.map { team ->
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
                        "DRAW" -> {
                            tied++
                            form.add("D")
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
            val points = won * winPts + tied * drawPts
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

    private fun resolveKnockoutWinner(match: MatchRecord?, teamById: Map<String, TeamState>): TeamState? {
        if (match == null) return null
        if (match.winnerTeamId == "TIE" || match.winnerTeamId == "DRAW") {
            return teamById[match.teamAId]
        }
        return teamById[match.winnerTeamId]
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
