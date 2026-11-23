package com.example.engine

import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.Ipl2026Catalog
import com.example.data.model.IplAiResultsBatch
import com.example.data.model.IplAuctionLiveState
import com.example.data.model.IplFranchiseState
import com.example.data.model.IplPlayMode
import com.example.data.model.IplRules
import com.example.data.model.IplScheduledMatch
import com.example.data.model.IplSeasonPhase
import com.example.data.model.IplSeasonState
import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import kotlin.random.Random

/**
 * Official IPL 2026 Season, Mega Auction, Match-Fee Ledger, Auto-Sim AI Engine, and Top-4 Playoffs Engine.
 */
object IplSeasonEngine {

    /**
     * Initializes a brand-new 10-franchise IPL 2026 season starting at the Pre-Auction Retention & RTM stage.
     * @param humanSelections List of (franchiseId, managerName) pairs (1 for Solo, 2+ for Friend Room).
     */
    fun createNewSeason(
        playMode: IplPlayMode,
        humanSelections: List<Pair<String, String>>,
        roomCode: String = "",
        seasonNumber: Int = 1,
        random: Random = Random.Default
    ): IplSeasonState {
        val humanMap = humanSelections.associate { it.first to it.second.trim().ifBlank { "Manager" } }
        val normalizedHumans = if (humanMap.isEmpty()) mapOf("ipl_rcb" to "Manager") else humanMap

        val franchises = Ipl2026Catalog.franchises.map { bp ->
            val owner = normalizedHumans[bp.id]
            IplFranchiseState(
                franchiseId = bp.id,
                shortCode = bp.shortCode,
                teamName = bp.name,
                defendingChampion = bp.defendingChampion,
                primaryColorHex = bp.primaryColorHex,
                accentColorHex = bp.accentColorHex,
                isHuman = owner != null,
                ownerName = owner ?: "AI Franchise",
                purseRemainingLakhs = IplRules.PURSE_LAKHS,
                matchFeesSpentLakhs = 0.0,
                matchesPaidCount = 0,
                hammerLootRemaining = IplRules.MAX_HAMMER_LOOT_PER_TEAM,
                formerSquadIds = bp.coreRosterIds.distinct(),
                captainPlayerId = bp.captainId
            )
        }

        val humanIds = franchises.filter { it.isHuman }.map { it.franchiseId }
        val resolvedRoomCode = if (playMode == IplPlayMode.FRIEND_ROOM) {
            roomCode.filter { it.isDigit() }.take(4).ifBlank { (1000 + random.nextInt(9000)).toString() }
        } else ""

        return IplSeasonState(
            seasonNumber = seasonNumber,
            seasonYear = 2026 + (seasonNumber - 1),
            playMode = playMode,
            roomCode = resolvedRoomCode,
            phase = IplSeasonPhase.RETENTION,
            activeHumanFranchiseId = humanIds.firstOrNull() ?: "ipl_rcb",
            humanFranchiseIds = humanIds,
            franchises = franchises
        )
    }

    /**
     * Validates official IPL retention rules:
     * - Max 6 total retentions (remaining slots become RTM cards for the auction)
     * - Max 5 capped players
     * - Max 2 uncapped Indian players
     * - Total slab deduction leaves enough purse to reach 18 players at ₹30L min reserve.
     */
    fun validateRetentionSelection(
        franchise: IplFranchiseState,
        selectedIds: List<String>
    ): String? {
        val unique = selectedIds.distinct()
        if (unique.size > IplRules.MAX_RETENTION_PLUS_RTM) {
            return "Max ${IplRules.MAX_RETENTION_PLUS_RTM} players can be retained (retentions + RTM combined)."
        }
        val invalid = unique.firstOrNull { it !in franchise.formerSquadIds }
        if (invalid != null) {
            return "Only players from ${franchise.shortCode}'s previous squad can be retained."
        }
        val uncappedCount = unique.count { Ipl2026Catalog.isUncappedIndian(it) }
        val cappedCount = unique.size - uncappedCount
        if (cappedCount > IplRules.MAX_CAPPED_RETENTION_RTM) {
            return "Max ${IplRules.MAX_CAPPED_RETENTION_RTM} capped players can be retained."
        }
        if (uncappedCount > IplRules.MAX_UNCAPPED_INDIAN_RETENTION_RTM) {
            return "Max ${IplRules.MAX_UNCAPPED_INDIAN_RETENTION_RTM} uncapped Indian players can be retained."
        }
        val overseasCount = unique.mapNotNull { CricketerDatabase.getById(it) }.count { it.overseas }
        if (overseasCount > IplRules.MAX_OVERSEAS_IN_SQUAD) {
            return "Max ${IplRules.MAX_OVERSEAS_IN_SQUAD} overseas players allowed in squad."
        }
        val cost = IplRules.calculateRetentionCostLakhs(unique)
        val remainingPurse = IplRules.PURSE_LAKHS - cost
        val slotsStillNeeded = (IplRules.MIN_SQUAD_SIZE - unique.size).coerceAtLeast(0)
        if (remainingPurse < slotsStillNeeded * IplRules.MIN_RESERVE_PER_SLOT_LAKHS) {
            return "Insufficient purse remaining to complete an 18-player squad."
        }
        return null
    }

    fun submitRetentions(
        state: IplSeasonState,
        franchiseId: String,
        selectedIds: List<String>,
        random: Random = Random.Default
    ): Result<IplSeasonState> {
        val target = state.franchiseById(franchiseId)
            ?: return Result.failure(IllegalArgumentException("Franchise not found"))
        val unique = selectedIds.distinct()
        val error = validateRetentionSelection(target, unique)
        if (error != null) return Result.failure(IllegalStateException(error))

        val updatedFranchise = applyRetentionsToFranchise(target, unique)
        var updatedList = state.franchises.map { if (it.franchiseId == franchiseId) updatedFranchise else it }

        // Check if all human franchises have finished retention
        val nextPendingHuman = updatedList.firstOrNull { it.isHuman && !it.retentionDone }
        if (nextPendingHuman != null) {
            return Result.success(
                state.copy(
                    franchises = updatedList,
                    activeHumanFranchiseId = nextPendingHuman.franchiseId
                )
            )
        }

        // All human franchises submitted! Auto-apply smart AI retentions (3-5 stars, keeping 1-3 RTM cards)
        val alreadyClaimed = updatedList.flatMap { it.retainedPlayerIds }.toMutableSet()
        updatedList = updatedList.map { f ->
            if (f.retentionDone) f
            else {
                val aiPicks = pickSmartAiRetentions(f, alreadyClaimed, random)
                alreadyClaimed.addAll(aiPicks)
                applyRetentionsToFranchise(f, aiPicks)
            }
        }

        // Build full 245+ Mega Auction pool excluding all retained players
        val allRetained = updatedList.flatMap { it.retainedPlayerIds }.toSet()
        val auctionOrder = Ipl2026Catalog.fullIplAuctionPool(excludedIds = allRetained).map { it.id }

        return Result.success(
            state.copy(
                phase = IplSeasonPhase.AUCTION,
                activeHumanFranchiseId = state.humanFranchiseIds.firstOrNull() ?: state.activeHumanFranchiseId,
                franchises = updatedList,
                auction = IplAuctionLiveState(
                    auctionOrder = auctionOrder,
                    currentIndex = 0,
                    lastEventMessage = "Mega Auction LIVE! ${allRetained.size} players retained • ${auctionOrder.size} stars in the pool • ₹120 Cr purse!"
                )
            )
        )
    }

    private fun applyRetentionsToFranchise(
        franchise: IplFranchiseState,
        selectedIds: List<String>
    ): IplFranchiseState {
        var cappedSlot = 0
        val prices = linkedMapOf<String, Long>()
        var totalCost = 0L
        selectedIds.forEach { pid ->
            val price = if (Ipl2026Catalog.isUncappedIndian(pid)) {
                IplRules.UNCAPPED_INDIAN_RETENTION_LAKHS
            } else {
                val p = IplRules.cappedRetentionPrice(cappedSlot)
                cappedSlot++
                p
            }
            prices[pid] = price
            totalCost += price
        }
        val capId = franchise.captainPlayerId.takeIf { it in selectedIds } ?: selectedIds.firstOrNull().orEmpty()
        return franchise.copy(
            purseRemainingLakhs = (IplRules.PURSE_LAKHS - totalCost).coerceAtLeast(0L),
            retainedPlayerIds = selectedIds,
            retentionDone = true,
            squadPlayerIds = selectedIds,
            squadPrices = prices,
            captainPlayerId = capId
        )
    }

    private fun pickSmartAiRetentions(
        franchise: IplFranchiseState,
        alreadyClaimed: Set<String>,
        random: Random
    ): List<String> {
        val candidates = franchise.formerSquadIds
            .filter { it !in alreadyClaimed }
            .mapNotNull { CricketerDatabase.getById(it) }
            .sortedByDescending { p ->
                val capBonus = if (p.id == franchise.captainPlayerId) 25 else 0
                p.overallRating + capBonus
            }

        val desiredCount = random.nextInt(3, 6) // 3, 4, or 5 retentions -> keeps 1 to 3 RTM cards!
        val picked = mutableListOf<String>()
        var capped = 0
        var uncapped = 0
        var overseas = 0

        for (p in candidates) {
            if (picked.size >= desiredCount) break
            val isUncappedInd = Ipl2026Catalog.isUncappedIndian(p.id)
            if (isUncappedInd && uncapped >= IplRules.MAX_UNCAPPED_INDIAN_RETENTION_RTM) continue
            if (!isUncappedInd && capped >= IplRules.MAX_CAPPED_RETENTION_RTM) continue
            if (p.overseas && overseas >= 2) continue // Leave OS slots open for auction

            val trial = picked + p.id
            if (validateRetentionSelection(franchise, trial) == null) {
                picked.add(p.id)
                if (isUncappedInd) uncapped++ else capped++
                if (p.overseas) overseas++
            }
        }
        return picked
    }

    // --- LIVE IPL MEGA AUCTION OPERATIONS ---

    fun maxAllowedBidLakhs(franchise: IplFranchiseState): Long {
        if (franchise.squadPlayerIds.size >= IplRules.MAX_SQUAD_SIZE) return 0L
        val slotsStillNeededAfterThis = (IplRules.MIN_SQUAD_SIZE - (franchise.squadPlayerIds.size + 1)).coerceAtLeast(0)
        val reserve = slotsStillNeededAfterThis * IplRules.MIN_RESERVE_PER_SLOT_LAKHS
        return (franchise.purseRemainingLakhs - reserve).coerceAtLeast(0L)
    }

    fun canFranchiseBid(franchise: IplFranchiseState, player: Cricketer, bidAmountLakhs: Long): String? {
        if (franchise.squadPlayerIds.size >= IplRules.MAX_SQUAD_SIZE) {
            return "${franchise.shortCode} squad is full (${IplRules.MAX_SQUAD_SIZE}/${IplRules.MAX_SQUAD_SIZE})."
        }
        if (player.overseas && franchise.overseasCount >= IplRules.MAX_OVERSEAS_IN_SQUAD) {
            return "${franchise.shortCode} already has ${IplRules.MAX_OVERSEAS_IN_SQUAD} overseas players."
        }
        val maxBid = maxAllowedBidLakhs(franchise)
        if (bidAmountLakhs > maxBid) {
            return "Exceeds max safe bid (${CurrencyFormatter.formatLakhs(maxBid)}) to keep ₹30L reserve for 18 players."
        }
        return null
    }

    fun placeBid(state: IplSeasonState, franchiseId: String): Result<IplSeasonState> {
        if (state.phase != IplSeasonPhase.AUCTION) return Result.failure(IllegalStateException("Auction not active"))
        val player = state.auction.currentPlayer ?: return Result.failure(IllegalStateException("No active player lot"))
        val franchise = state.franchiseById(franchiseId) ?: return Result.failure(IllegalArgumentException("Franchise not found"))
        if (state.auction.highestBidderFranchiseId == franchiseId) {
            return Result.failure(IllegalStateException("${franchise.shortCode} already holds the highest bid"))
        }
        val nextBid = state.auction.nextBidLakhs()
        val reason = canFranchiseBid(franchise, player, nextBid)
        if (reason != null) return Result.failure(IllegalStateException(reason))

        return Result.success(
            state.copy(
                auction = state.auction.copy(
                    currentBidLakhs = nextBid,
                    highestBidderFranchiseId = franchise.franchiseId,
                    highestBidderName = franchise.teamName,
                    passedFranchiseIds = state.auction.passedFranchiseIds - franchiseId,
                    lastEventMessage = "${franchise.shortCode} bids ${CurrencyFormatter.formatLakhs(nextBid)} for ${player.name}!"
                )
            )
        )
    }

    /**
     * Hammer Loot (STRICT MAX 2 USES PER TEAM PER AUCTION):
     * Instantly seals the player at a quick-steal price (next bid or current bid + ₹20L),
     * decrements `hammerLootRemaining` (2 -> 1 -> 0), and locks out further Hammer Loots at 0/2!
     */
    fun triggerHammerLoot(state: IplSeasonState, franchiseId: String): Result<IplSeasonState> {
        if (state.phase != IplSeasonPhase.AUCTION) return Result.failure(IllegalStateException("Auction not active"))
        val player = state.auction.currentPlayer ?: return Result.failure(IllegalStateException("No active lot"))
        val franchise = state.franchiseById(franchiseId) ?: return Result.failure(IllegalArgumentException("Franchise not found"))
        if (franchise.hammerLootRemaining <= 0) {
            return Result.failure(IllegalStateException("Hammer Loot limit reached (0/${IplRules.MAX_HAMMER_LOOT_PER_TEAM}) for ${franchise.shortCode}!"))
        }
        val stealPrice = if (state.auction.highestBidderFranchiseId.isBlank()) {
            player.basePriceLakhs
        } else {
            state.auction.nextBidLakhs()
        }
        val reason = canFranchiseBid(franchise, player, stealPrice)
        if (reason != null) return Result.failure(IllegalStateException(reason))

        val updatedFranchises = state.franchises.map { f ->
            if (f.franchiseId != franchiseId) f
            else f.copy(
                purseRemainingLakhs = (f.purseRemainingLakhs - stealPrice).coerceAtLeast(0L),
                hammerLootRemaining = (f.hammerLootRemaining - 1).coerceAtLeast(0),
                squadPlayerIds = f.squadPlayerIds + player.id,
                squadPrices = f.squadPrices + (player.id to stealPrice)
            )
        }
        val left = (franchise.hammerLootRemaining - 1).coerceAtLeast(0)
        val msg = "🔨 HAMMER LOOT! ${franchise.shortCode} steals ${player.name} for ${CurrencyFormatter.formatLakhs(stealPrice)}! ($left/${IplRules.MAX_HAMMER_LOOT_PER_TEAM} left)"
        return Result.success(
            advanceAuctionLot(
                state.copy(
                    franchises = updatedFranchises,
                    auction = state.auction.copy(
                        soldCount = state.auction.soldCount + 1,
                        lastEventMessage = msg
                    )
                )
            )
        )
    }

    /**
     * Official Right-To-Match (RTM) card:
     * Matches the current highest bid for a former franchise player (strictly enforcing max 6 total,
     * max 5 capped, max 2 uncapped Indian!).
     */
    fun useRtmCard(state: IplSeasonState, franchiseId: String): Result<IplSeasonState> {
        if (state.phase != IplSeasonPhase.AUCTION) return Result.failure(IllegalStateException("Auction not active"))
        val player = state.auction.currentPlayer ?: return Result.failure(IllegalStateException("No active lot"))
        val franchise = state.franchiseById(franchiseId) ?: return Result.failure(IllegalArgumentException("Franchise not found"))
        if (!franchise.canUseRtmOnPlayer(player.id)) {
            return Result.failure(IllegalStateException("RTM not eligible for ${player.name} (check former squad & 5 capped / 2 uncapped limits)."))
        }
        val matchPrice = if (state.auction.currentBidLakhs > 0L) state.auction.currentBidLakhs else player.basePriceLakhs
        val reason = canFranchiseBid(franchise, player, matchPrice)
        if (reason != null) return Result.failure(IllegalStateException(reason))

        val updatedFranchises = state.franchises.map { f ->
            if (f.franchiseId != franchiseId) f
            else f.copy(
                purseRemainingLakhs = (f.purseRemainingLakhs - matchPrice).coerceAtLeast(0L),
                rtmUsedPlayerIds = f.rtmUsedPlayerIds + player.id,
                squadPlayerIds = f.squadPlayerIds + player.id,
                squadPrices = f.squadPrices + (player.id to matchPrice)
            )
        }
        val msg = "🃏 RTM EXERCISED! ${franchise.shortCode} matches ${CurrencyFormatter.formatLakhs(matchPrice)} to bring back ${player.name}!"
        return Result.success(
            advanceAuctionLot(
                state.copy(
                    franchises = updatedFranchises,
                    auction = state.auction.copy(
                        soldCount = state.auction.soldCount + 1,
                        lastEventMessage = msg
                    )
                )
            )
        )
    }

    fun passCurrentLot(state: IplSeasonState, franchiseId: String): IplSeasonState {
        return state.copy(
            auction = state.auction.copy(
                passedFranchiseIds = state.auction.passedFranchiseIds + franchiseId
            )
        )
    }

    /**
     * One AI bidding step during the live auction countdown (respects purse, 18-25 size, max 8 OS, and 2/2 Hammer Loot cap).
     */
    fun stepAiAuctionBid(state: IplSeasonState, random: Random = Random.Default): IplSeasonState {
        if (state.phase != IplSeasonPhase.AUCTION) return state
        val player = state.auction.currentPlayer ?: return state
        val nextBid = state.auction.nextBidLakhs()

        val eligibleAi = state.franchises.filter { f ->
            !f.isHuman &&
                f.franchiseId != state.auction.highestBidderFranchiseId &&
                canFranchiseBid(f, player, nextBid) == null &&
                evaluateAiWillingness(f, player, nextBid, random)
        }
        if (eligibleAi.isEmpty()) return state

        val bidder = eligibleAi.maxByOrNull { f ->
            val needWk = if (player.role == PlayerRole.WICKETKEEPER && f.wkCount == 0) 35 else 0
            val needSize = (IplRules.MIN_SQUAD_SIZE - f.squadPlayerIds.size).coerceAtLeast(0) * 6
            needWk + needSize + (f.purseRemainingLakhs / 400L).toInt() + random.nextInt(15)
        } ?: return state

        // Rarely (4% on marquee 88+ stars), AI may trigger one of its 2 Hammer Loots!
        if (bidder.hammerLootRemaining > 0 && player.overallRating >= 88 && nextBid <= 1_400L && random.nextDouble() < 0.04) {
            return triggerHammerLoot(state, bidder.franchiseId).getOrDefault(state)
        }

        return placeBid(state, bidder.franchiseId).getOrDefault(state)
    }

    private fun evaluateAiWillingness(
        franchise: IplFranchiseState,
        player: Cricketer,
        bidLakhs: Long,
        random: Random
    ): Boolean {
        val slotsToMin = (IplRules.MIN_SQUAD_SIZE - franchise.squadPlayerIds.size).coerceAtLeast(0)
        val fairValue = when {
            player.overallRating >= 91 -> 1_650L
            player.overallRating >= 87 -> 1_150L
            player.overallRating >= 83 -> 650L
            player.overallRating >= 79 -> 280L
            else -> 120L
        }
        val wkBoost = if (player.role == PlayerRole.WICKETKEEPER && franchise.wkCount == 0) 350L else 0L
        val urgentBoost = if (slotsToMin >= 6) 140L else 0L
        val cap = ((fairValue + wkBoost + urgentBoost) * (0.85 + random.nextDouble() * 0.35)).toLong()
        return bidLakhs <= cap
    }

    fun finalizeCurrentLot(state: IplSeasonState, random: Random = Random.Default): IplSeasonState {
        if (state.phase != IplSeasonPhase.AUCTION) return state
        val player = state.auction.currentPlayer ?: return finalizeAuctionAndMoveToXi(state, random)
        val winnerId = state.auction.highestBidderFranchiseId
        val price = state.auction.currentBidLakhs

        if (winnerId.isBlank() || price <= 0L) {
            return advanceAuctionLot(
                state.copy(
                    auction = state.auction.copy(
                        unsoldCount = state.auction.unsoldCount + 1,
                        lastEventMessage = "UNSOLD: ${player.name} goes unsold at ${CurrencyFormatter.formatLakhs(player.basePriceLakhs)}."
                    )
                )
            )
        }

        // Check if an AI former franchise wants to exercise an RTM card on a star player!
        val rtmAi = state.franchises.firstOrNull { f ->
            !f.isHuman &&
                f.franchiseId != winnerId &&
                f.canUseRtmOnPlayer(player.id) &&
                canFranchiseBid(f, player, price) == null &&
                player.overallRating >= 84 &&
                price <= 1_450L &&
                random.nextDouble() < 0.35
        }
        if (rtmAi != null) {
            return useRtmCard(state, rtmAi.franchiseId).getOrDefault(state)
        }

        val winner = state.franchiseById(winnerId)
        val updatedFranchises = state.franchises.map { f ->
            if (f.franchiseId != winnerId) f
            else f.copy(
                purseRemainingLakhs = (f.purseRemainingLakhs - price).coerceAtLeast(0L),
                squadPlayerIds = f.squadPlayerIds + player.id,
                squadPrices = f.squadPrices + (player.id to price)
            )
        }
        val msg = "SOLD! ${player.name} to ${winner?.shortCode ?: winnerId} for ${CurrencyFormatter.formatLakhs(price)}!"
        return advanceAuctionLot(
            state.copy(
                franchises = updatedFranchises,
                auction = state.auction.copy(
                    soldCount = state.auction.soldCount + 1,
                    lastEventMessage = msg
                )
            )
        )
    }

    private fun advanceAuctionLot(state: IplSeasonState): IplSeasonState {
        val nextIdx = state.auction.currentIndex + 1
        val allHaveMinSquad = state.franchises.all { it.squadPlayerIds.size >= IplRules.MIN_SQUAD_SIZE }
        val allFull = state.franchises.all { it.squadPlayerIds.size >= IplRules.MAX_SQUAD_SIZE }
        val poolExhausted = nextIdx >= state.auction.auctionOrder.size

        if (allFull || poolExhausted || (allHaveMinSquad && nextIdx >= 195)) {
            return finalizeAuctionAndMoveToXi(state)
        }

        return state.copy(
            auction = state.auction.copy(
                currentIndex = nextIdx,
                currentBidLakhs = 0L,
                highestBidderFranchiseId = "",
                highestBidderName = "",
                passedFranchiseIds = emptySet()
            )
        )
    }

    /**
     * Auto-completes the rest of the IPL Mega Auction cleanly so every single one of the 10 franchises
     * finishes with 18–25 players (max 8 overseas, ≥1 Wicketkeeper) inside their ₹120 Cr purse.
     */
    fun autoCompleteRemainingAuction(state: IplSeasonState, random: Random = Random.Default): IplSeasonState {
        if (state.phase != IplSeasonPhase.AUCTION) return state
        val franchises = state.franchises.toMutableList()
        val ownedIds = franchises.flatMap { it.squadPlayerIds }.toMutableSet()
        val remainingIds = state.auction.auctionOrder.drop(state.auction.currentIndex).filter { it !in ownedIds }
        val unsoldPool = mutableListOf<Cricketer>()

        // Pass 1: Competitive bidding across the remaining pool
        for (pid in remainingIds) {
            val player = CricketerDatabase.getById(pid) ?: continue
            val interested = franchises.indices.filter { idx ->
                val f = franchises[idx]
                f.squadPlayerIds.size < IplRules.MAX_SQUAD_SIZE &&
                    canFranchiseBid(f, player, player.basePriceLakhs) == null &&
                    (f.squadPlayerIds.size < 20 || (player.overallRating >= 84 && f.purseRemainingLakhs >= 350L))
            }
            if (interested.isEmpty()) {
                unsoldPool.add(player)
                continue
            }
            // Pick highest-need franchise and compute realistic market price
            val winnerIdx = interested.maxByOrNull { idx ->
                val f = franchises[idx]
                val needMin = (IplRules.MIN_SQUAD_SIZE - f.squadPlayerIds.size).coerceAtLeast(0) * 18
                val needWk = if (player.role == PlayerRole.WICKETKEEPER && f.wkCount == 0) 60 else 0
                val needBowl = if ((player.role == PlayerRole.SEAMER || player.role == PlayerRole.SPINNER) &&
                    f.players.count { it.role == PlayerRole.SEAMER || it.role == PlayerRole.SPINNER } < 6
                ) 22 else 0
                val formerBonus = if (player.id in f.formerSquadIds) 10 else 0
                needMin + needWk + needBowl + formerBonus + (f.purseRemainingLakhs / 350L).toInt() + random.nextInt(18)
            } ?: continue

            val winner = franchises[winnerIdx]
            val maxSafe = maxAllowedBidLakhs(winner)
            val targetPrice = when {
                player.overallRating >= 90 -> (player.basePriceLakhs + random.nextLong(250L, 950L))
                player.overallRating >= 85 -> (player.basePriceLakhs + random.nextLong(100L, 550L))
                player.overallRating >= 80 -> (player.basePriceLakhs + random.nextLong(20L, 220L))
                else -> player.basePriceLakhs
            }.coerceIn(player.basePriceLakhs, maxSafe.coerceAtLeast(player.basePriceLakhs))

            val finalPrice = if (targetPrice <= maxSafe) targetPrice else player.basePriceLakhs
            if (finalPrice <= maxSafe) {
                franchises[winnerIdx] = winner.copy(
                    purseRemainingLakhs = (winner.purseRemainingLakhs - finalPrice).coerceAtLeast(0L),
                    squadPlayerIds = winner.squadPlayerIds + player.id,
                    squadPrices = winner.squadPrices + (player.id to finalPrice)
                )
                ownedIds.add(player.id)
            } else {
                unsoldPool.add(player)
            }
        }

        // Pass 2: Accelerated round — strictly guarantee EVERY franchise reaches at least 18 players & ≥1 WK!
        val fullFallbackPool = (unsoldPool + Ipl2026Catalog.fullIplAuctionPool(excludedIds = ownedIds))
            .distinctBy { it.id }
            .filter { it.id !in ownedIds }
            .toMutableList()

        // First guarantee every team has ≥1 WK
        for (idx in franchises.indices) {
            val f = franchises[idx]
            if (f.wkCount == 0 && f.squadPlayerIds.size < IplRules.MAX_SQUAD_SIZE) {
                val wk = fullFallbackPool.firstOrNull {
                    it.role == PlayerRole.WICKETKEEPER && (!it.overseas || f.overseasCount < IplRules.MAX_OVERSEAS_IN_SQUAD)
                }
                if (wk != null) {
                    fullFallbackPool.remove(wk)
                    val price = wk.basePriceLakhs.coerceAtMost(f.purseRemainingLakhs).coerceAtLeast(0L)
                    franchises[idx] = f.copy(
                        purseRemainingLakhs = (f.purseRemainingLakhs - price).coerceAtLeast(0L),
                        squadPlayerIds = f.squadPlayerIds + wk.id,
                        squadPrices = f.squadPrices + (wk.id to price)
                    )
                    ownedIds.add(wk.id)
                }
            }
        }

        // Fill every team up to MIN_SQUAD_SIZE (18 players), preferring Indian players when OS cap is reached
        for (idx in franchises.indices) {
            while (franchises[idx].squadPlayerIds.size < IplRules.MIN_SQUAD_SIZE && fullFallbackPool.isNotEmpty()) {
                val f = franchises[idx]
                val pick = fullFallbackPool.firstOrNull { p ->
                    !p.overseas || f.overseasCount < IplRules.MAX_OVERSEAS_IN_SQUAD
                } ?: break
                fullFallbackPool.remove(pick)
                val price = IplRules.MIN_RESERVE_PER_SLOT_LAKHS.coerceAtMost(f.purseRemainingLakhs).coerceAtLeast(0L)
                franchises[idx] = f.copy(
                    purseRemainingLakhs = (f.purseRemainingLakhs - price).coerceAtLeast(0L),
                    squadPlayerIds = f.squadPlayerIds + pick.id,
                    squadPrices = f.squadPrices + (pick.id to price)
                )
                ownedIds.add(pick.id)
            }
        }

        return finalizeAuctionAndMoveToXi(state.copy(franchises = franchises), random)
    }

    private fun finalizeAuctionAndMoveToXi(
        state: IplSeasonState,
        random: Random = Random.Default
    ): IplSeasonState {
        val readyFranchises = state.franchises.map { f ->
            val xi = SquadRuleValidator.autoPickBestPlayingXi(f.squadPlayerIds)
            val xiSet = xi.toSet()
            val nominees = autoPickImpactNominees(f.squadPlayerIds, xiSet)
            val cap = f.captainPlayerId.takeIf { it in xiSet } ?: SquadRuleValidator.autoPickCaptain(xi)
            f.copy(
                playingXiIds = xi,
                impactNomineeIds = nominees,
                captainPlayerId = cap
            )
        }
        return state.copy(
            phase = IplSeasonPhase.SQUAD_XI,
            activeHumanFranchiseId = state.humanFranchiseIds.firstOrNull() ?: state.activeHumanFranchiseId,
            franchises = readyFranchises,
            auction = state.auction.copy(
                lastEventMessage = "Mega Auction Complete! All 10 franchises drafted 18–25 player squads. Lock Playing XI & 5 Impact Subs!"
            )
        )
    }

    fun autoPickImpactNominees(squadIds: List<String>, xiSet: Set<String>): List<String> {
        val bench = squadIds.filter { it !in xiSet }.mapNotNull { CricketerDatabase.getById(it) }
        // Prioritize Indian specialists + 1 overseas impact option so the 4-OS rule always has strong Indian choices
        val indianBench = bench.filter { !it.overseas }.sortedByDescending { it.overallRating }
        val overseasBench = bench.filter { it.overseas }.sortedByDescending { it.overallRating }
        return (indianBench.take(4) + overseasBench.take(1) + indianBench.drop(4) + overseasBench.drop(1))
            .distinctBy { it.id }
            .take(IplRules.IMPACT_NOMINEES_COUNT)
            .map { it.id }
    }

    /**
     * Saves a franchise's custom Playing XI (strictly 11 players, max 4 overseas, ≥1 WK),
     * Captain, and 5 Impact Player substitutes.
     */
    fun savePlayingXiAndNominees(
        state: IplSeasonState,
        franchiseId: String,
        xiIds: List<String>,
        impactNomineeIds: List<String>,
        captainId: String
    ): Result<IplSeasonState> {
        val franchise = state.franchiseById(franchiseId)
            ?: return Result.failure(IllegalArgumentException("Franchise not found"))
        val distinctXi = xiIds.distinct()
        if (distinctXi.size != 11) {
            return Result.failure(IllegalStateException("Select exactly 11 players for Playing XI (${distinctXi.size}/11)."))
        }
        val xiPlayers = distinctXi.mapNotNull { CricketerDatabase.getById(it) }
        val osCount = xiPlayers.count { it.overseas }
        if (osCount > IplRules.MAX_OVERSEAS_IN_XI) {
            return Result.failure(IllegalStateException("Max ${IplRules.MAX_OVERSEAS_IN_XI} overseas players allowed in Playing XI (selected $osCount)."))
        }
        if (xiPlayers.none { it.role == PlayerRole.WICKETKEEPER }) {
            return Result.failure(IllegalStateException("Playing XI must include at least 1 Wicketkeeper."))
        }
        val xiSet = distinctXi.toSet()
        val validNominees = impactNomineeIds
            .filter { it in franchise.squadPlayerIds && it !in xiSet }
            .distinct()
            .take(IplRules.IMPACT_NOMINEES_COUNT)
            .ifEmpty { autoPickImpactNominees(franchise.squadPlayerIds, xiSet) }
        val validCaptain = captainId.takeIf { it in xiSet } ?: SquadRuleValidator.autoPickCaptain(distinctXi)

        val updated = state.franchises.map { f ->
            if (f.franchiseId != franchiseId) f
            else f.copy(
                playingXiIds = distinctXi,
                impactNomineeIds = validNominees,
                captainPlayerId = validCaptain
            )
        }
        return Result.success(state.copy(franchises = updated))
    }

    // --- 70-MATCH LEAGUE SCHEDULE, AUTO-SIM AI, & TOP-4 PLAYOFFS ---

    /**
     * Enters the 70-match League Stage, generates the balanced schedule, and auto-simulates
     * any initial AI vs AI matches scheduled before the first human matchday!
     */
    fun enterLeagueStage(
        state: IplSeasonState,
        random: Random = Random.Default
    ): Pair<IplSeasonState, IplAiResultsBatch?> {
        val schedule = if (state.schedule.size == IplRules.TOTAL_LEAGUE_MATCHES) {
            state.schedule
        } else {
            IplScheduleGenerator.generate70MatchSchedule(
                franchises = state.franchises,
                humanFranchiseIds = state.humanFranchiseIds,
                random = random
            )
        }
        val leagueState = state.copy(
            phase = IplSeasonPhase.LEAGUE,
            schedule = schedule
        )
        return autoSimPendingAiMatches(leagueState, random, batchTitle = "Opening Matchday AI Results")
    }

    /**
     * Returns the next unplayed match involving a human franchise (either League or Playoffs).
     */
    fun nextHumanFixture(state: IplSeasonState): IplScheduledMatch? {
        return when (state.phase) {
            IplSeasonPhase.LEAGUE -> state.schedule.firstOrNull { !it.isCompleted && state.involvesHuman(it) }
            IplSeasonPhase.PLAYOFFS -> state.playoffs.firstOrNull { !it.isCompleted && state.involvesHuman(it) }
            else -> null
        }
    }

    /**
     * Records a completed human match, charges official ₹7.5L x 12 = ₹90L fixed match fees to both teams,
     * and immediately auto-simulates all AI vs AI matches up to the next human match (or Top-4 Playoffs)!
     */
    fun recordMatchAndAutoSimAi(
        state: IplSeasonState,
        matchNumber: Int,
        record: MatchRecord,
        random: Random = Random.Default
    ): Pair<IplSeasonState, IplAiResultsBatch?> {
        var updated = applyCompletedMatchToState(state, matchNumber, record)
        updated = maybeAdvanceToOrUpdatePlayoffs(updated)
        val (afterAiSim, aiBatch) = autoSimPendingAiMatches(
            state = updated,
            random = random,
            batchTitle = "Other IPL Match Results"
        )
        return afterAiSim to aiBatch
    }

    private fun applyCompletedMatchToState(
        state: IplSeasonState,
        matchNumber: Int,
        record: MatchRecord
    ): IplSeasonState {
        val updatedSchedule = state.schedule.map { m ->
            if (m.matchNumber == matchNumber) m.copy(completedRecord = record) else m
        }
        val updatedPlayoffs = state.playoffs.map { m ->
            if (m.matchNumber == matchNumber) m.copy(completedRecord = record) else m
        }
        // Track official ₹7.5 Lakh fixed match fees for Playing XI + Impact Player (12 x ₹7.5L = ₹90L per match per franchise)
        val feeLakhs = IplRules.MATCH_FEE_PER_TEAM_GAME_LAKHS.toDouble()
        val updatedFranchises = state.franchises.map { f ->
            if (f.franchiseId == record.teamAId || f.franchiseId == record.teamBId) {
                f.copy(
                    matchFeesSpentLakhs = f.matchFeesSpentLakhs + feeLakhs,
                    matchesPaidCount = f.matchesPaidCount + 1
                )
            } else f
        }
        return state.copy(
            franchises = updatedFranchises,
            schedule = updatedSchedule,
            playoffs = updatedPlayoffs
        )
    }

    /**
     * Auto-simulates all AI vs AI fixtures sequentially until either:
     * - The next unplayed match involves a human franchise, OR
     * - The league ends (transitioning cleanly into Top-4 Playoffs), OR
     * - The IPL Final completes!
     */
    fun autoSimPendingAiMatches(
        state: IplSeasonState,
        random: Random = Random.Default,
        batchTitle: String = "Other IPL Match Results"
    ): Pair<IplSeasonState, IplAiResultsBatch?> {
        var current = state
        val simulatedRecords = mutableListOf<MatchRecord>()
        var safety = 0

        while (safety < 80) {
            safety++
            current = maybeAdvanceToOrUpdatePlayoffs(current)
            if (current.phase == IplSeasonPhase.COMPLETED) break

            val nextMatch = when (current.phase) {
                IplSeasonPhase.LEAGUE -> current.schedule.firstOrNull { !it.isCompleted }
                IplSeasonPhase.PLAYOFFS -> current.playoffs.firstOrNull { !it.isCompleted }
                else -> null
            } ?: break

            // Stop as soon as a human franchise is scheduled to play!
            if (current.involvesHuman(nextMatch)) break

            // Fast-simulate AI vs AI match with full official IPL rules
            val fA = current.franchiseById(nextMatch.teamAId) ?: break
            val fB = current.franchiseById(nextMatch.teamBId) ?: break
            val live = MatchSimulationEngine.createMatch(
                teamA = fA.toTeamState(),
                teamB = fB.toTeamState(),
                stage = nextMatch.stage,
                format = MatchFormat.T20I,
                random = random,
                isIplRules = true,
                teamAImpactNominees = fA.impactNomineeIds,
                teamBImpactNominees = fB.impactNomineeIds
            )
            val finished = MatchSimulationEngine.simulateFullMatch(live, random)
            val record = finished.toMatchRecord(
                roomCode = current.roomCode.ifBlank { "IPL2026" },
                hostUid = "ipl_engine",
                memberUids = emptyList()
            ).copy(id = "ipl_s${current.seasonNumber}_m${nextMatch.matchNumber}")

            simulatedRecords.add(record)
            current = applyCompletedMatchToState(current, nextMatch.matchNumber, record)
        }

        current = maybeAdvanceToOrUpdatePlayoffs(current)
        val batch = if (simulatedRecords.isNotEmpty()) {
            IplAiResultsBatch(title = batchTitle, results = simulatedRecords)
        } else null
        return current to batch
    }

    /**
     * Computes live 10-team IPL Points Table with exact Net Run Rate (NRR) from League matches.
     */
    fun computeLeagueStandings(state: IplSeasonState): List<TeamStanding> {
        val teams = state.franchises.map { it.toTeamState() }
        val leagueRecords = state.schedule.mapNotNull { it.completedRecord }
        return TournamentEngine.computeStandings(teams, leagueRecords)
    }

    /**
     * Builds & advances official Top-4 IPL Playoffs (Qualifier 1, Eliminator, Qualifier 2, IPL Final).
     */
    private fun maybeAdvanceToOrUpdatePlayoffs(state: IplSeasonState): IplSeasonState {
        if (state.phase == IplSeasonPhase.COMPLETED) return state
        if (state.phase != IplSeasonPhase.LEAGUE && state.phase != IplSeasonPhase.PLAYOFFS) return state
        if (!state.isLeagueFinished) return state

        val standings = computeLeagueStandings(state)
        if (standings.size < 4) return state

        val top1 = standings[0].teamId
        val top2 = standings[1].teamId
        val top3 = standings[2].teamId
        val top4 = standings[3].teamId

        val existing = state.playoffs.toMutableList()
        // 1) Create Qualifier 1 & Eliminator immediately when all 70 league games finish
        if (existing.none { it.stage == MatchStage.QUALIFIER_1 }) {
            existing.add(
                IplScheduledMatch(
                    matchNumber = 71,
                    matchday = 53,
                    stage = MatchStage.QUALIFIER_1,
                    teamAId = top1,
                    teamBId = top2,
                    venue = Ipl2026Catalog.byId(top1)?.homeVenue ?: "IPL Playoffs Venue"
                )
            )
        }
        if (existing.none { it.stage == MatchStage.ELIMINATOR }) {
            existing.add(
                IplScheduledMatch(
                    matchNumber = 72,
                    matchday = 54,
                    stage = MatchStage.ELIMINATOR,
                    teamAId = top3,
                    teamBId = top4,
                    venue = Ipl2026Catalog.byId(top3)?.homeVenue ?: "IPL Playoffs Venue"
                )
            )
        }

        val q1 = existing.firstOrNull { it.stage == MatchStage.QUALIFIER_1 }?.completedRecord
        val elim = existing.firstOrNull { it.stage == MatchStage.ELIMINATOR }?.completedRecord

        // 2) Create Qualifier 2 (Loser Q1 vs Winner Eliminator)
        if (q1 != null && elim != null && existing.none { it.stage == MatchStage.QUALIFIER_2 }) {
            val q1Loser = if (q1.winnerTeamId == q1.teamAId) q1.teamBId else q1.teamAId
            val elimWinner = if (elim.winnerTeamId == elim.teamBId) elim.teamBId else elim.teamAId
            existing.add(
                IplScheduledMatch(
                    matchNumber = 73,
                    matchday = 55,
                    stage = MatchStage.QUALIFIER_2,
                    teamAId = q1Loser,
                    teamBId = elimWinner,
                    venue = "Narendra Modi Stadium"
                )
            )
        }

        val q2 = existing.firstOrNull { it.stage == MatchStage.QUALIFIER_2 }?.completedRecord

        // 3) Create IPL Final (Winner Q1 vs Winner Q2)
        if (q1 != null && q2 != null && existing.none { it.stage == MatchStage.FINAL }) {
            val q1Winner = if (q1.winnerTeamId == q1.teamBId) q1.teamBId else q1.teamAId
            val q2Winner = if (q2.winnerTeamId == q2.teamBId) q2.teamBId else q2.teamAId
            existing.add(
                IplScheduledMatch(
                    matchNumber = 74,
                    matchday = 56,
                    stage = MatchStage.FINAL,
                    teamAId = q1Winner,
                    teamBId = q2Winner,
                    venue = "M. Chinnaswamy Stadium"
                )
            )
        }

        val finalMatch = existing.firstOrNull { it.stage == MatchStage.FINAL }?.completedRecord
        if (finalMatch != null) {
            val champId = if (finalMatch.winnerTeamId == finalMatch.teamBId) finalMatch.teamBId else finalMatch.teamAId
            val champName = state.franchiseById(champId)?.teamName ?: finalMatch.winnerTeamName
            val runnerUpId = if (champId == finalMatch.teamAId) finalMatch.teamBId else finalMatch.teamAId
            val runnerUpName = state.franchiseById(runnerUpId)?.teamName ?: "Runner-Up"
            return state.copy(
                phase = IplSeasonPhase.COMPLETED,
                playoffs = existing,
                championFranchiseId = champId,
                championTeamName = champName,
                runnerUpTeamName = runnerUpName
            )
        }

        return state.copy(
            phase = IplSeasonPhase.PLAYOFFS,
            playoffs = existing
        )
    }

    /**
     * Builds the final IPL Trophy Cabinet record including IPL Trophy, Orange Cap, Purple Cap,
     * Season MVP, and Emerging Player awards!
     */
    fun buildSeasonTrophyRecord(state: IplSeasonState): TrophyCabinetRecord? {
        if (state.phase != IplSeasonPhase.COMPLETED || state.championTeamName.isBlank()) return null
        val awards = SeasonAwardsEngine.buildLeaderboards(state.allCompletedMatches)
        val userWon = state.championFranchiseId in state.humanFranchiseIds
        val orange = awards.orangeCapHolder?.let { "${it.playerName} (${it.runs} runs)" }.orEmpty()
        val purple = awards.purpleCapHolder?.let { "${it.playerName} (${it.wickets} wkts)" }.orEmpty()
        val mvp = awards.mvpHolder?.let { "${it.playerName} (${it.mvpPoints.toInt()} pts)" }.orEmpty()
        val emerging = awards.emergingPlayerHolder?.let { "${it.playerName} (${it.teamName})" }.orEmpty()
        val finalRec = state.playoffs.firstOrNull { it.stage == MatchStage.FINAL }?.completedRecord

        return TrophyCabinetRecord(
            id = "ipl_${state.seasonYear}_s${state.seasonNumber}_${System.currentTimeMillis()}",
            title = "IPL ${state.seasonYear} Trophy",
            competitionType = "IPL",
            championTeamName = state.championTeamName,
            runnerUpTeamName = state.runnerUpTeamName,
            userWon = userWon,
            summary = finalRec?.resultSummary ?: "${state.championTeamName} crowned IPL ${state.seasonYear} Champions!",
            orangeCapWinner = orange,
            purpleCapWinner = purple,
            mvpWinner = mvp,
            emergingPlayerWinner = emerging
        )
    }
}
