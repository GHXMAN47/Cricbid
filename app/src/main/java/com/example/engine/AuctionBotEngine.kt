package com.example.engine

import com.example.data.model.AuctionRoom
import com.example.data.model.Cricketer
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Smart AI Bot Manager engine for 8-team auctions:
 * - Realistic budget pacing across 12-player squads (11 XI + 1 Impact Sub)
 * - Role-aware valuation (Batter, WK, All-Rounder, Seamer, Spinner)
 * - Marquee Retention selection & strategic Right to Match (RTM) card usage
 * - Smart AI Fallback so teams running low on purse/time never stall below 11 players
 */
object AuctionBotEngine {

    val botFranchisePresets: List<String> = listOf(
        "Mumbai Mavericks",
        "Chennai Kings",
        "Bengaluru Challengers",
        "Kolkata Knights",
        "Hyderabad Sunrisers",
        "Delhi Capitals",
        "Jaipur Royals",
        "Gujarat Titans"
    )

    data class BotBidDecision(
        val teamId: String,
        val teamName: String,
        val bidLakhs: Long
    )

    /**
     * Populates unfilled team slots up to targetCount (default 8) with named AI franchises.
     */
    fun ensureMinimumTeamsWithBots(
        existingTeams: List<TeamState>,
        targetCount: Int = SquadRules.DEFAULT_TARGET_TEAMS
    ): List<TeamState> {
        if (existingTeams.size >= targetCount) return existingTeams
        val result = existingTeams.toMutableList()
        val usedNames = result.map { it.teamName.lowercase() }.toMutableSet()
        var botSeq = result.count { it.bot } + 1

        while (result.size < targetCount && result.size < SquadRules.MAX_TEAMS) {
            val nextName = botFranchisePresets.firstOrNull { it.lowercase() !in usedNames }
                ?: "AI Franchise ${result.size + 1}"
            usedNames.add(nextName.lowercase())
            result.add(
                TeamState(
                    teamId = "bot_${botSeq}_${Random.nextInt(100, 999)}",
                    ownerUid = "ai_bot",
                    teamName = nextName,
                    bot = true,
                    budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
                    rtmRemaining = SquadRules.INITIAL_RTM_CARDS
                )
            )
            botSeq++
        }
        return result
    }

    /**
     * Ensures every franchise entering a Mega Auction Retention Round has a distinct 7-player
     * previous/core squad so each team can retain up to 4 players (tiered purse deductions)
     * AND use its 1 RTM card on any unretained former squad player during live bidding.
     */
    fun assignFormerSquadsIfMissing(
        teams: List<TeamState>,
        previousSquadLookup: (String) -> List<String> = { emptyList() }
    ): List<TeamState> {
        val claimedIds = mutableSetOf<String>()
        teams.forEach { t ->
            claimedIds.addAll(t.formerSquadPlayerIds)
            claimedIds.addAll(t.squadPlayerIds)
        }

        val allPool = com.example.data.model.CricketerDatabase.allPlayers
        val marqueeIds = com.example.data.model.CricketerDatabase.marqueeRetentionPool().map { it.id }.toSet()

        return teams.map { team ->
            if (team.formerSquadPlayerIds.isNotEmpty()) {
                team
            } else {
                val chosen = mutableListOf<String>()
                // 1. Honor persisted previous-season squad if available and unclaimed
                val savedPrev = previousSquadLookup(team.teamName)
                    .filter { it !in claimedIds && com.example.data.model.CricketerDatabase.getById(it) != null }
                for (id in savedPrev.take(7)) {
                    chosen.add(id)
                    claimedIds.add(id)
                }

                // 2. Otherwise seed a realistic 7-player franchise core (2 marquee, 1 WK, 1 AR, 2 bowlers, 1 batter)
                if (chosen.size < 7) {
                    val remaining = allPool.filter { it.id !in claimedIds }
                    fun pickOne(predicate: (Cricketer) -> Boolean) {
                        if (chosen.size >= 7) return
                        val candidate = remaining
                            .filter { it.id !in claimedIds && predicate(it) }
                            .sortedByDescending { it.overallRating }
                            .take(4)
                            .randomOrNull()
                        if (candidate != null) {
                            chosen.add(candidate.id)
                            claimedIds.add(candidate.id)
                        }
                    }
                    // 2 marquee stars (at least 1 domestic)
                    pickOne { it.id in marqueeIds && !it.overseas }
                    pickOne { it.id in marqueeIds }
                    // Balanced supporting core for realistic retention + RTM decisions
                    pickOne { it.role == PlayerRole.WICKETKEEPER }
                    pickOne { it.role == PlayerRole.ALL_ROUNDER }
                    pickOne { it.role == PlayerRole.SEAMER }
                    pickOne { it.role == PlayerRole.SPINNER }
                    pickOne { !it.overseas && it.role == PlayerRole.BATTER }
                    while (chosen.size < 7) {
                        val fallback = remaining.firstOrNull { it.id !in claimedIds } ?: break
                        chosen.add(fallback.id)
                        claimedIds.add(fallback.id)
                    }
                }
                team.copy(formerSquadPlayerIds = chosen)
            }
        }
    }

    /**
     * AI Bots each retain 2–3 core stars from their former squad using tiered purse deductions
     * (₹15 Cr, ₹11 Cr, ₹8 Cr, ₹6 Cr). Unretained former players remain in [TeamState.formerSquadPlayerIds]
     * so they are eligible for that franchise's Right to Match (RTM) card during the live auction.
     */
    fun applyBotRetentions(
        teams: List<TeamState>,
        marqueePool: List<Cricketer>,
        previousSquadLookup: (String) -> List<String> = { emptyList() }
    ): Pair<List<TeamState>, Set<String>> {
        val seededTeams = assignFormerSquadsIfMissing(teams, previousSquadLookup)
        val takenIds = seededTeams.flatMap { it.retainedPlayerIds + listOfNotNull(it.retainedPlayerId.takeIf { id -> id.isNotBlank() }) }
            .toMutableSet()
        val fallbackMarquee = marqueePool.filter { it.id !in takenIds }.shuffled().toMutableList()

        val updated = seededTeams.map { team ->
            if (!team.bot || team.retentionDone) {
                team
            } else {
                val formerPlayers = team.formerSquadPlayerIds
                    .mapNotNull { com.example.data.model.CricketerDatabase.getById(it) }
                    .filter { it.id !in takenIds }
                    .sortedByDescending { it.overallRating }

                val desiredCount = Random.nextInt(2, 4) // Bots retain 2 or 3 stars, leaving 4-5 former players for RTM
                var working = team
                val retainedList = mutableListOf<String>()

                val candidates = if (formerPlayers.isNotEmpty()) formerPlayers else fallbackMarquee.toList()
                for (candidate in candidates) {
                    if (retainedList.size >= desiredCount || retainedList.size >= SquadRules.MAX_RETENTIONS) break
                    if (candidate.id in takenIds) continue
                    if (candidate.overseas && working.overseasCount >= 2) continue // Keep overseas balance
                    val slotPrice = SquadRules.retentionPriceForSlot(retainedList.size)
                    if (working.budgetLakhs - slotPrice < 3_500L) break // Keep healthy auction purse
                    working = applyPurchase(working, candidate, slotPrice)
                    retainedList.add(candidate.id)
                    takenIds.add(candidate.id)
                    fallbackMarquee.removeAll { it.id == candidate.id }
                }

                val remainingFormerForRtm = working.formerSquadPlayerIds.filterNot { it in retainedList }
                working.copy(
                    retainedPlayerId = retainedList.firstOrNull().orEmpty(),
                    retainedPlayerIds = retainedList,
                    formerSquadPlayerIds = remainingFormerForRtm,
                    retentionDone = true
                )
            }
        }
        return updated to takenIds
    }

    fun decideNextBotBid(room: AuctionRoom): BotBidDecision? {
        val player = room.currentCricketer ?: return null
        val proposedBid = room.nextBidAmountLakhs()
        val eligibleBots = room.teams.filter { team ->
            team.bot &&
                SquadRuleValidator.checkBidEligibility(
                    team = team,
                    player = player,
                    proposedBidLakhs = proposedBid,
                    currentHighestBidderTeamId = room.highestBidderTeamId
                ).allowed
        }
        if (eligibleBots.isEmpty()) return null

        val interested = eligibleBots.mapNotNull { bot ->
            val maxWilling = computeMaxWillingBidLakhs(bot, player, room.teams.size)
            if (proposedBid <= maxWilling) {
                val marginScore = (maxWilling - proposedBid) + Random.nextLong(0L, 125L)
                bot to marginScore
            } else {
                null
            }
        }
        val chosen = interested.maxByOrNull { it.second }?.first ?: return null
        return BotBidDecision(
            teamId = chosen.teamId,
            teamName = chosen.teamName,
            bidLakhs = proposedBid
        )
    }

    /**
     * Checks whether any AI Bot wants to deploy its single Right to Match (RTM) card
     * right before the hammer falls on a high-value marquee star.
     */
    fun decideBotRtm(room: AuctionRoom): TeamState? {
        val player = room.currentCricketer ?: return null
        if (player.overallRating < 92 || room.highestBidderTeamId.isBlank() || room.currentBidLakhs <= 0L) return null
        // Only trigger occasionally (~25%) so bots don't burn all RTMs on lot #1
        if (Random.nextDouble() > 0.25) return null

        return room.teams.firstOrNull { bot ->
            bot.bot &&
                SquadRuleValidator.checkRtmEligibility(
                    team = bot,
                    player = player,
                    currentBidLakhs = room.currentBidLakhs,
                    currentHighestBidderTeamId = room.highestBidderTeamId
                ).allowed &&
                room.currentBidLakhs <= computeMaxWillingBidLakhs(bot, player, room.teams.size) + 150L
        }
    }

    fun computeMaxWillingBidLakhs(team: TeamState, player: Cricketer, totalTeams: Int): Long {
        val slotsNeeded = maxOf(0, SquadRules.MIN_SQUAD_SIZE - team.squadPlayerIds.size)
        val reserveAfterBuy = maxOf(0, slotsNeeded - 1) * SquadRules.MIN_RESERVE_PER_SLOT_LAKHS
        val maxAffordable = (team.budgetLakhs - reserveAfterBuy).coerceAtLeast(0L)
        if (maxAffordable < player.basePriceLakhs) return 0L

        val ratingFactor = when {
            player.overallRating >= 94 -> 1550L
            player.overallRating >= 91 -> 1225L
            player.overallRating >= 88 -> 925L
            player.overallRating >= 85 -> 625L
            else -> 375L
        }

        val existingRoleCount = team.players.count { it.role == player.role }
        val roleMultiplier = when (player.role) {
            PlayerRole.WICKETKEEPER -> when {
                team.wkCount == 0 -> 1.45
                team.wkCount == 1 -> 0.55
                else -> 0.25
            }
            PlayerRole.ALL_ROUNDER -> when {
                existingRoleCount < 2 -> 1.28
                existingRoleCount < 4 -> 0.92
                else -> 0.45
            }
            PlayerRole.SEAMER -> when {
                existingRoleCount < 3 -> 1.22
                existingRoleCount < 4 -> 0.85
                else -> 0.40
            }
            PlayerRole.SPINNER -> when {
                existingRoleCount < 2 -> 1.22
                existingRoleCount < 3 -> 0.85
                else -> 0.40
            }
            PlayerRole.BATTER -> when {
                existingRoleCount < 4 -> 1.15
                existingRoleCount < 5 -> 0.80
                else -> 0.42
            }
        }

        val slotUrgency = if (slotsNeeded > 0) 1.0 + (slotsNeeded * 0.028) else 0.42
        val domesticBonus = if (!player.overseas) 1.14 else 0.94
        val poolScale = if (totalTeams <= 2) 0.85 else 1.0
        val affinityJitter = 0.82 + (((team.teamId + player.id).hashCode().and(0xFF)) / 255.0) * 0.36

        val target = (ratingFactor * roleMultiplier * slotUrgency * domesticBonus * poolScale * affinityJitter)
            .roundToInt()
            .toLong()
            .coerceAtLeast(player.basePriceLakhs)

        val budgetShareCap = if (slotsNeeded > 2) {
            (team.budgetLakhs * 0.28).toLong().coerceAtLeast(player.basePriceLakhs)
        } else {
            maxAffordable
        }

        return minOf(target, budgetShareCap, maxAffordable)
    }

    /**
     * Smart AI Fallback & Auto-Completer:
     * Ensures every franchise finishes with >= 12 players (11 Playing XI + 1+ Impact Sub bench),
     * <= 8 overseas in squad, >= 7 Indian players (so a <= 4 overseas Playing XI is always possible),
     * >= 1 Wicketkeeper, and auto-assigned Captain (C) for AI teams.
     */
    fun autoCompleteRemainingAuction(
        teams: List<TeamState>,
        remainingPlayers: List<Cricketer>
    ): Pair<List<TeamState>, Int> {
        val working = teams.toMutableList()
        var soldAdded = 0
        val pool = remainingPlayers.toMutableList()

        // Pass 1: Guarantee at least 1 Wicketkeeper for every team that lacks one
        val keepers = pool.filter { it.role == PlayerRole.WICKETKEEPER }.sortedByDescending { it.overallRating }
        for (wk in keepers) {
            val needyIdx = working.indices
                .filter { idx ->
                    val t = working[idx]
                    val overseasOk = !wk.overseas || t.overseasCount < SquadRules.MAX_OVERSEAS_IN_SQUAD
                    t.wkCount == 0 && overseasOk && t.squadPlayerIds.size < SquadRules.MAX_SQUAD_SIZE
                }
                .maxByOrNull { working[it].budgetLakhs }
            if (needyIdx != null) {
                val team = working[needyIdx]
                val safePrice = computeSafeFallbackPrice(team, wk)
                working[needyIdx] = applyPurchase(team, wk, safePrice)
                pool.remove(wk)
                soldAdded++
            }
        }

        // Pass 1B: Guarantee at least 7 Indian players for every team so a max-4-overseas XI is always valid
        val domesticPool = pool.filter { !it.overseas }.sortedByDescending { it.overallRating }
        for (dom in domesticPool) {
            if (working.all { it.domesticCount >= SquadRules.MIN_DOMESTIC_FOR_XI }) break
            val needyIdx = working.indices
                .filter { idx ->
                    val t = working[idx]
                    t.domesticCount < SquadRules.MIN_DOMESTIC_FOR_XI && t.squadPlayerIds.size < SquadRules.MAX_SQUAD_SIZE
                }
                .maxByOrNull { idx ->
                    val t = working[idx]
                    (SquadRules.MIN_DOMESTIC_FOR_XI - t.domesticCount) * 10_000L + t.budgetLakhs
                } ?: break
            val team = working[needyIdx]
            val safePrice = computeSafeFallbackPrice(team, dom)
            working[needyIdx] = applyPurchase(team, dom, safePrice)
            pool.remove(dom)
            soldAdded++
        }

        // Pass 2: Target 12-player squads (11 XI + Impact Sub) across all teams cleanly
        val targetSquadSize = minOf(12, (pool.size + working.sumOf { it.squadPlayerIds.size }) / working.size.coerceAtLeast(1))
            .coerceAtLeast(SquadRules.MIN_SQUAD_SIZE)

        val sortedPool = pool.sortedByDescending { it.overallRating }
        for (player in sortedPool) {
            if (working.all { it.meetsSquadRules && it.squadPlayerIds.size >= targetSquadSize }) break

            val candidates = working.indices.filter { idx ->
                val t = working[idx]
                val needsSlot = !t.meetsSquadRules || t.squadPlayerIds.size < targetSquadSize
                val overseasOk = !player.overseas || (
                    t.overseasCount < SquadRules.MAX_OVERSEAS_IN_SQUAD &&
                        (t.domesticCount + (SquadRules.MAX_SQUAD_SIZE - (t.squadPlayerIds.size + 1)) >= SquadRules.MIN_DOMESTIC_FOR_XI)
                    )
                needsSlot && overseasOk && t.squadPlayerIds.size < SquadRules.MAX_SQUAD_SIZE
            }
            if (candidates.isEmpty()) continue

            val winnerIdx = candidates.maxByOrNull { idx ->
                val t = working[idx]
                val urgentNeed = if (t.squadPlayerIds.size < SquadRules.MIN_SQUAD_SIZE) 20_000L else 0L
                val domNeed = if (!player.overseas && t.domesticCount < SquadRules.MIN_DOMESTIC_FOR_XI) 25_000L else 0L
                val sizePriority = (targetSquadSize - t.squadPlayerIds.size) * 1500L
                val valuation = computeMaxWillingBidLakhs(t, player, working.size)
                urgentNeed + domNeed + sizePriority + valuation
            } ?: continue

            val winnerTeam = working[winnerIdx]
            val safePrice = computeSafeFallbackPrice(winnerTeam, player)
            working[winnerIdx] = applyPurchase(winnerTeam, player, safePrice)
            soldAdded++
        }

        val finalized = working.map { team ->
            if (team.bot && team.squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE) {
                val aiXi = SquadRuleValidator.autoPickBestPlayingXi(team.squadPlayerIds)
                team.copy(
                    playingXiIds = aiXi,
                    captainPlayerId = SquadRuleValidator.autoPickCaptain(aiXi),
                    xiReady = true
                )
            } else {
                team
            }
        }
        return finalized to soldAdded
    }

    private fun computeSafeFallbackPrice(team: TeamState, player: Cricketer): Long {
        val slotsAfter = maxOf(0, SquadRules.MIN_SQUAD_SIZE - (team.squadPlayerIds.size + 1))
        val reserve = slotsAfter * SquadRules.MIN_RESERVE_PER_SLOT_LAKHS
        val maxSpend = (team.budgetLakhs - reserve).coerceAtLeast(0L)
        if (maxSpend < player.basePriceLakhs) {
            // Smart AI Fallback when purse ran low: sign at available remaining funds (or base rescue 25L/0L)
            return minOf(team.budgetLakhs, 50L)
        }
        val estimated = computeMaxWillingBidLakhs(team, player, SquadRules.MAX_TEAMS)
            .coerceIn(player.basePriceLakhs, minOf(maxSpend, maxOf(player.basePriceLakhs, 650L)))
        return ((estimated / 25L) * 25L).coerceAtLeast(player.basePriceLakhs).coerceAtMost(maxSpend)
    }

    fun applyPurchase(team: TeamState, player: Cricketer, priceLakhs: Long): TeamState {
        return team.copy(
            budgetLakhs = (team.budgetLakhs - priceLakhs).coerceAtLeast(0L),
            squadPlayerIds = team.squadPlayerIds + player.id,
            squadPrices = team.squadPrices + (player.id to priceLakhs),
            overseasCount = team.overseasCount + if (player.overseas) 1 else 0,
            wkCount = team.wkCount + if (player.role == PlayerRole.WICKETKEEPER) 1 else 0
        )
    }
}
