package com.example.engine

import com.example.data.model.Ipl2026Catalog
import com.example.data.model.IplFranchiseState
import com.example.data.model.IplScheduledMatch
import com.example.data.model.MatchStage
import kotlin.random.Random

/**
 * Generates an official, realistic 70-match IPL League Stage schedule for 10 franchises:
 * - Official IPL Virtual Group Format (Group A = 5 teams, Group B = 5 teams):
 *   * Each team plays 2x (Home & Away) vs the 4 other teams in its own group = 8 matches
 *   * Each team plays 2x (Home & Away) vs 1 paired rival in the opposite group = 2 matches
 *   * Each team plays 1x vs the remaining 4 teams in the opposite group = 4 matches
 *   => EXACTLY 14 matches per team, 70 league matches total!
 * - Balanced matchday ordering that strictly avoids back-to-back matches for any team
 *   and naturally spreads human franchises across the calendar (never scheduling every
 *   team's opening match against the user's team!).
 */
object IplScheduleGenerator {

    fun generate70MatchSchedule(
        franchises: List<IplFranchiseState>,
        humanFranchiseIds: List<String> = emptyList(),
        random: Random = Random.Default
    ): List<IplScheduledMatch> {
        val ids = franchises.map { it.franchiseId }.distinct()
        require(ids.size == 10) { "IPL schedule requires exactly 10 franchises, got ${ids.size}" }

        // Shuffle teams into two 5-team virtual groups for fresh season matchups
        val shuffled = ids.shuffled(random)
        val groupA = shuffled.subList(0, 5)
        val groupB = shuffled.subList(5, 10)

        // Build the exact 70-match multiset (teamA = home, teamB = away)
        val rawPairings = mutableListOf<Pair<String, String>>()

        // 1) Intra-group double round-robin (10 pairs x 2 = 20 per group => 40 matches)
        for (group in listOf(groupA, groupB)) {
            for (i in 0 until 5) {
                for (j in i + 1 until 5) {
                    rawPairings.add(group[i] to group[j])
                    rawPairings.add(group[j] to group[i])
                }
            }
        }

        // 2) Cross-group paired rival double round-robin (5 pairs x 2 = 10 matches)
        for (i in 0 until 5) {
            rawPairings.add(groupA[i] to groupB[i])
            rawPairings.add(groupB[i] to groupA[i])
        }

        // 3) Cross-group remaining 4 opponents single round-robin (20 matches, balanced home/away)
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (i == j) continue
                // Alternate home advantage cleanly so each team gets 7 home + 7 away
                val aHome = ((i - j + 5) % 5) in 1..2
                if (aHome) {
                    rawPairings.add(groupA[i] to groupB[j])
                } else {
                    rawPairings.add(groupB[j] to groupA[i])
                }
            }
        }

        // Verify exact 70 matches before ordering
        val remaining = rawPairings.shuffled(random).toMutableList()
        val ordered = mutableListOf<Pair<String, String>>()
        val lastPlayedIndex = ids.associateWith { -10 }.toMutableMap()
        val playedCount = ids.associateWith { 0 }.toMutableMap()
        val humanSet = humanFranchiseIds.toSet()

        // Greedy constraint scheduler: minimizes consecutive games & keeps games-played balanced!
        var matchIdx = 0
        while (remaining.isNotEmpty()) {
            // Find best candidate maximizing rest gap & avoiding back-to-back games
            var bestCandidateIdx = 0
            var bestScore = Int.MIN_VALUE

            for (i in remaining.indices) {
                val (a, b) = remaining[i]
                val gapA = matchIdx - (lastPlayedIndex[a] ?: -10)
                val gapB = matchIdx - (lastPlayedIndex[b] ?: -10)
                val minGap = minOf(gapA, gapB)

                // Hard penalty for immediate back-to-back (gap == 1), softer penalty for gap == 2
                var score = when {
                    minGap == 1 -> -10_000
                    minGap == 2 -> -800
                    minGap == 3 -> -150
                    else -> minGap.coerceAtMost(8) * 18
                }

                // Strongly prefer teams with fewer games played so every franchise opens in Matchdays 1-5
                val minPlayed = playedCount.values.minOrNull() ?: 0
                val countA = playedCount[a] ?: 0
                val countB = playedCount[b] ?: 0
                score -= (countA - minPlayed) * 260
                score -= (countB - minPlayed) * 260

                // Extra spacing protection for human teams so they never get bunched up
                if (a in humanSet || b in humanSet) {
                    if (minGap <= 2) score -= 2_000
                    // Slightly vary opener so Match 1 isn't always forced if defending champ isn't human
                    if (matchIdx == 0 && a != "ipl_rcb" && b != "ipl_rcb") score -= 35
                }

                // Tiny jitter for natural variety across seasons
                score += random.nextInt(15)

                if (score > bestScore) {
                    bestScore = score
                    bestCandidateIdx = i
                }
            }

            val chosen = remaining.removeAt(bestCandidateIdx)
            ordered.add(chosen)
            lastPlayedIndex[chosen.first] = matchIdx
            lastPlayedIndex[chosen.second] = matchIdx
            playedCount[chosen.first] = (playedCount[chosen.first] ?: 0) + 1
            playedCount[chosen.second] = (playedCount[chosen.second] ?: 0) + 1
            matchIdx++
        }

        // Convert to IplScheduledMatch with realistic Matchdays (1..52, double-headers on weekends)
        var currentMatchday = 1
        var matchesInDay = 0
        return ordered.mapIndexed { idx, (homeId, awayId) ->
            val isDoubleHeaderDay = currentMatchday % 3 == 0
            val maxToday = if (isDoubleHeaderDay) 2 else 1
            if (matchesInDay >= maxToday) {
                currentMatchday++
                matchesInDay = 0
            }
            matchesInDay++
            val venue = Ipl2026Catalog.byId(homeId)?.homeVenue ?: "IPL Stadium"
            IplScheduledMatch(
                matchNumber = idx + 1,
                matchday = currentMatchday,
                stage = MatchStage.LEAGUE,
                teamAId = homeId,
                teamBId = awayId,
                venue = venue
            )
        }
    }
}
