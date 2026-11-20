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
     * AI Bots each retain 1 unique Marquee Star at ₹15 Cr (1500L) prior to the main auction.
     */
    fun applyBotRetentions(
        teams: List<TeamState>,
        marqueePool: List<Cricketer>
    ): Pair<List<TeamState>, Set<String>> {
        val takenIds = teams.mapNotNull { it.retainedPlayerId.takeIf { id -> id.isNotBlank() } }.toMutableSet()
        val available = marqueePool.filter { it.id !in takenIds }.shuffled().toMutableList()

        val updated = teams.map { team ->
            if (!team.bot || team.retentionDone) {
                team
            } else {
                val pick = available.firstOrNull { !it.overseas || team.overseasCount < SquadRules.MAX_OVERSEAS_IN_SQUAD }
                if (pick != null) {
                    available.remove(pick)
                    takenIds.add(pick.id)
                    applyPurchase(team, pick, SquadRules.RETENTION_PRICE_LAKHS).copy(
                        retainedPlayerId = pick.id,
                        retentionDone = true
                    )
                } else {
                    team.copy(retentionDone = true)
                }
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
