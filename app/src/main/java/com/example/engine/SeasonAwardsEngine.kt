package com.example.engine

import com.example.data.model.MatchRecord

/**
 * Per-player performance snapshot embedded inside a completed [MatchRecord]
 * for tournament-wide Orange Cap, Purple Cap, and Season MVP aggregation.
 */
data class MatchPlayerPerf(
    val playerId: String = "",
    val playerName: String = "",
    val teamName: String = "",
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val wickets: Int = 0,
    val runsConceded: Int = 0,
    val legalBallsBowled: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "playerId" to playerId,
        "playerName" to playerName,
        "teamName" to teamName,
        "runs" to runs,
        "ballsFaced" to ballsFaced,
        "fours" to fours,
        "sixes" to sixes,
        "wickets" to wickets,
        "runsConceded" to runsConceded,
        "legalBallsBowled" to legalBallsBowled
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): MatchPlayerPerf = MatchPlayerPerf(
            playerId = map["playerId"] as? String ?: "",
            playerName = map["playerName"] as? String ?: "",
            teamName = map["teamName"] as? String ?: "",
            runs = (map["runs"] as? Number)?.toInt() ?: 0,
            ballsFaced = (map["ballsFaced"] as? Number)?.toInt() ?: 0,
            fours = (map["fours"] as? Number)?.toInt() ?: 0,
            sixes = (map["sixes"] as? Number)?.toInt() ?: 0,
            wickets = (map["wickets"] as? Number)?.toInt() ?: 0,
            runsConceded = (map["runsConceded"] as? Number)?.toInt() ?: 0,
            legalBallsBowled = (map["legalBallsBowled"] as? Number)?.toInt() ?: 0
        )
    }
}

data class SeasonPlayerLeader(
    val playerId: String,
    val playerName: String,
    val teamName: String,
    val matches: Int,
    val runs: Int,
    val ballsFaced: Int,
    val fours: Int,
    val sixes: Int,
    val highestScore: Int,
    val wickets: Int,
    val runsConceded: Int,
    val legalBallsBowled: Int,
    val mvpPoints: Double
) {
    val strikeRate: Double
        get() = if (ballsFaced == 0) 0.0 else (runs * 100.0) / ballsFaced

    val economy: Double
        get() = if (legalBallsBowled == 0) 0.0 else (runsConceded * 6.0) / legalBallsBowled
}

data class SeasonLeaderboardSummary(
    val orangeCapTable: List<SeasonPlayerLeader> = emptyList(),
    val purpleCapTable: List<SeasonPlayerLeader> = emptyList(),
    val mvpTable: List<SeasonPlayerLeader> = emptyList(),
    val emergingTable: List<SeasonPlayerLeader> = emptyList()
) {
    val orangeCapHolder: SeasonPlayerLeader? get() = orangeCapTable.firstOrNull { it.runs > 0 }
    val purpleCapHolder: SeasonPlayerLeader? get() = purpleCapTable.firstOrNull { it.wickets > 0 }
    val mvpHolder: SeasonPlayerLeader? get() = mvpTable.firstOrNull { it.mvpPoints > 0.0 }
    val emergingPlayerHolder: SeasonPlayerLeader? get() = emergingTable.firstOrNull { it.mvpPoints > 0.0 }

    val orangeCapPlayerId: String? get() = orangeCapHolder?.playerId
    val purpleCapPlayerId: String? get() = purpleCapHolder?.playerId
    val hasData: Boolean
        get() = orangeCapTable.isNotEmpty() || purpleCapTable.isNotEmpty() || mvpTable.isNotEmpty() || emergingTable.isNotEmpty()
}

data class TrophyCabinetRecord(
    val id: String,
    val title: String,
    val competitionType: String, // "IPL", "SERIES", "WORLD_CUP", "WTC", "CAREER"
    val championTeamName: String,
    val runnerUpTeamName: String,
    val userWon: Boolean,
    val summary: String,
    val orangeCapWinner: String = "",
    val purpleCapWinner: String = "",
    val mvpWinner: String = "",
    val emergingPlayerWinner: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)

data class TrophyCabinetData(
    val trophies: List<TrophyCabinetRecord> = emptyList(),
    val finalsPlayed: Int = 0,
    val championshipsWon: Int = 0
)

/**
 * Dynamically aggregates tournament-wide Orange Cap, Purple Cap, and Season MVP leaderboards
 * from all completed [MatchRecord]s in the active tournament.
 */
object SeasonAwardsEngine {

    fun buildLeaderboards(matches: List<MatchRecord>): SeasonLeaderboardSummary {
        if (matches.isEmpty()) return SeasonLeaderboardSummary()

        data class Accum(
            val playerId: String,
            var playerName: String,
            var teamName: String,
            val matchIds: MutableSet<String> = mutableSetOf(),
            var runs: Int = 0,
            var ballsFaced: Int = 0,
            var fours: Int = 0,
            var sixes: Int = 0,
            var highestScore: Int = 0,
            var wickets: Int = 0,
            var runsConceded: Int = 0,
            var legalBallsBowled: Int = 0
        )

        val map = linkedMapOf<String, Accum>()
        matches.forEachIndexed { mIdx, match ->
            val matchKey = match.id.ifBlank { "m_$mIdx" }
            match.playerPerformances.forEach { perf ->
                if (perf.playerId.isBlank() || perf.playerName.isBlank()) return@forEach
                val acc = map.getOrPut(perf.playerId) {
                    Accum(
                        playerId = perf.playerId,
                        playerName = perf.playerName,
                        teamName = perf.teamName
                    )
                }
                acc.matchIds.add(matchKey)
                acc.runs += perf.runs
                acc.ballsFaced += perf.ballsFaced
                acc.fours += perf.fours
                acc.sixes += perf.sixes
                if (perf.runs > acc.highestScore) acc.highestScore = perf.runs
                acc.wickets += perf.wickets
                acc.runsConceded += perf.runsConceded
                acc.legalBallsBowled += perf.legalBallsBowled
                if (perf.teamName.isNotBlank()) acc.teamName = perf.teamName
            }
        }

        val leaders = map.values.map { acc ->
            val srBonus = if (acc.ballsFaced >= 15) ((acc.runs * 100.0 / acc.ballsFaced) - 120.0).coerceIn(-10.0, 25.0) * 0.4 else 0.0
            val econBonus = if (acc.legalBallsBowled >= 12) (8.5 - (acc.runsConceded * 6.0 / acc.legalBallsBowled)).coerceIn(-8.0, 15.0) * 1.5 else 0.0
            val mvp = (acc.runs * 1.0) + (acc.fours * 1.5) + (acc.sixes * 3.0) + (acc.wickets * 24.0) + srBonus + econBonus
            SeasonPlayerLeader(
                playerId = acc.playerId,
                playerName = acc.playerName,
                teamName = acc.teamName,
                matches = acc.matchIds.size.coerceAtLeast(1),
                runs = acc.runs,
                ballsFaced = acc.ballsFaced,
                fours = acc.fours,
                sixes = acc.sixes,
                highestScore = acc.highestScore,
                wickets = acc.wickets,
                runsConceded = acc.runsConceded,
                legalBallsBowled = acc.legalBallsBowled,
                mvpPoints = mvp.coerceAtLeast(0.0)
            )
        }

        val orange = leaders
            .filter { it.runs > 0 }
            .sortedWith(compareByDescending<SeasonPlayerLeader> { it.runs }.thenByDescending { it.strikeRate })
            .take(12)

        val purple = leaders
            .filter { it.wickets > 0 }
            .sortedWith(compareByDescending<SeasonPlayerLeader> { it.wickets }.thenBy { it.economy })
            .take(12)

        val mvp = leaders
            .filter { it.mvpPoints > 0.0 }
            .sortedByDescending { it.mvpPoints }
            .take(12)

        val emerging = leaders
            .filter { it.mvpPoints > 0.0 && com.example.data.model.Ipl2026Catalog.isEmergingEligible(it.playerId) }
            .sortedByDescending { it.mvpPoints }
            .take(10)

        return SeasonLeaderboardSummary(
            orangeCapTable = orange,
            purpleCapTable = purple,
            mvpTable = mvp,
            emergingTable = emerging
        )
    }

    /**
     * Extracts consolidated per-player performances from a completed [LiveMatchState].
     */
    fun extractPerformances(match: LiveMatchState): List<MatchPlayerPerf> {
        val byPlayer = linkedMapOf<String, MatchPlayerPerf>()

        fun mergeBat(teamName: String, card: List<BatterScoreEntry>) {
            card.forEach { b ->
                if (b.balls > 0 || b.runs > 0) {
                    val prev = byPlayer[b.playerId]
                    byPlayer[b.playerId] = (prev ?: MatchPlayerPerf(
                        playerId = b.playerId,
                        playerName = b.playerName.removeSuffix(" (IP)"),
                        teamName = teamName
                    )).let {
                        it.copy(
                            runs = it.runs + b.runs,
                            ballsFaced = it.ballsFaced + b.balls,
                            fours = it.fours + b.fours,
                            sixes = it.sixes + b.sixes
                        )
                    }
                }
            }
        }

        fun mergeBowl(teamName: String, card: List<BowlerScoreEntry>) {
            card.forEach { bw ->
                if (bw.legalBalls > 0 || bw.wickets > 0) {
                    val prev = byPlayer[bw.playerId]
                    byPlayer[bw.playerId] = (prev ?: MatchPlayerPerf(
                        playerId = bw.playerId,
                        playerName = bw.playerName.removeSuffix(" (IP)"),
                        teamName = teamName
                    )).let {
                        it.copy(
                            wickets = it.wickets + bw.wickets,
                            runsConceded = it.runsConceded + bw.runsConceded,
                            legalBallsBowled = it.legalBallsBowled + bw.legalBalls
                        )
                    }
                }
            }
        }

        mergeBat(match.innings1.battingTeamName, match.innings1.battingCard)
        mergeBowl(match.innings1.bowlingTeamName, match.innings1.bowlingCard)
        mergeBat(match.innings2.battingTeamName, match.innings2.battingCard)
        mergeBowl(match.innings2.bowlingTeamName, match.innings2.bowlingCard)
        return byPlayer.values.toList()
    }
}
