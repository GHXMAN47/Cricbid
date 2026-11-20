package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RoomStatus {
    LOBBY,
    RETENTION,
    AUCTION,
    SQUAD_SELECTION,
    MATCH_HUB,
    COMPLETED;

    companion object {
        fun fromNameSafe(raw: String?): RoomStatus =
            entries.firstOrNull { it.name == raw } ?: LOBBY
    }
}

enum class MatchStage(val label: String) {
    SERIES("Series Match"),
    LEAGUE("League Match"),
    QUALIFIER_1("Qualifier 1"),
    ELIMINATOR("Eliminator"),
    QUALIFIER_2("Qualifier 2"),
    SEMI_FINAL_1("Semi-Final 1"),
    SEMI_FINAL_2("Semi-Final 2"),
    WTC_FINAL("WTC Grand Final"),
    FINAL("Grand Final");

    companion object {
        fun fromNameSafe(raw: String?): MatchStage =
            entries.firstOrNull { it.name == raw } ?: LEAGUE
    }
}

object SquadRules {
    const val INITIAL_BUDGET_LAKHS: Long = 10_000L // ₹100 Cr
    const val RETENTION_PRICE_LAKHS: Long = 1_500L // Slot 1 ₹15 Cr marquee retention
    const val MAX_RETENTIONS: Int = 4
    val RETENTION_SLOTS_LAKHS: List<Long> = listOf(1_500L, 1_100L, 800L, 600L) // ₹15Cr, ₹11Cr, ₹8Cr, ₹6Cr
    const val MIN_SQUAD_SIZE: Int = 11
    const val MAX_SQUAD_SIZE: Int = 15 // Allows up to 8 overseas + 7 Indian players in full squad
    const val MAX_OVERSEAS_IN_SQUAD: Int = 8 // IPL Auction squad cap
    const val MAX_OVERSEAS_IN_XI: Int = 4 // Starting Playing XI strict cap
    const val MAX_OVERSEAS: Int = MAX_OVERSEAS_IN_XI // Starting XI alias
    const val MIN_DOMESTIC_FOR_XI: Int = MIN_SQUAD_SIZE - MAX_OVERSEAS_IN_XI // At least 7 domestic needed for valid XI
    const val MIN_WICKETKEEPERS: Int = 1
    const val DEFAULT_TARGET_TEAMS: Int = 8
    const val MAX_TEAMS: Int = 8
    const val MIN_TEAMS: Int = 2
    const val MIN_RESERVE_PER_SLOT_LAKHS: Long = 50L
    const val AUCTION_TIMER_SECONDS: Int = 10
    const val INITIAL_RTM_CARDS: Int = 1

    fun retentionPriceForSlot(slotIndex: Int): Long =
        RETENTION_SLOTS_LAKHS.getOrElse(slotIndex.coerceAtLeast(0)) { 600L }

    fun totalRetentionCost(count: Int): Long =
        (0 until count.coerceIn(0, MAX_RETENTIONS)).sumOf { retentionPriceForSlot(it) }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val favoriteTeamName: String? = null,
    val matchesPlayed: Long = 0L,
    val matchesWon: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TeamState(
    val teamId: String = "",
    val ownerUid: String = "",
    val teamName: String = "",
    val bot: Boolean = false,
    val budgetLakhs: Long = SquadRules.INITIAL_BUDGET_LAKHS,
    val rtmRemaining: Int = SquadRules.INITIAL_RTM_CARDS,
    val retainedPlayerId: String = "",
    val retainedPlayerIds: List<String> = emptyList(),
    val formerSquadPlayerIds: List<String> = emptyList(),
    val retentionDone: Boolean = false,
    val squadPlayerIds: List<String> = emptyList(),
    val squadPrices: Map<String, Long> = emptyMap(),
    val playingXiIds: List<String> = emptyList(),
    val captainPlayerId: String = "",
    val xiReady: Boolean = false,
    val overseasCount: Int = 0,
    val wkCount: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "teamId" to teamId,
        "ownerUid" to ownerUid,
        "teamName" to teamName,
        "bot" to bot,
        "budgetLakhs" to budgetLakhs,
        "rtmRemaining" to rtmRemaining,
        "retainedPlayerId" to retainedPlayerId,
        "retainedPlayerIds" to retainedPlayerIds,
        "formerSquadPlayerIds" to formerSquadPlayerIds,
        "retentionDone" to retentionDone,
        "squadPlayerIds" to squadPlayerIds,
        "squadPrices" to squadPrices,
        "playingXiIds" to playingXiIds,
        "captainPlayerId" to captainPlayerId,
        "xiReady" to xiReady,
        "overseasCount" to overseasCount,
        "wkCount" to wkCount
    )

    val players: List<Cricketer>
        get() = squadPlayerIds.mapNotNull { CricketerDatabase.getById(it) }

    val playingXiPlayers: List<Cricketer>
        get() = playingXiIds.mapNotNull { CricketerDatabase.getById(it) }

    val effectiveCaptainId: String
        get() {
            val activePool = playingXiIds.ifEmpty { squadPlayerIds }
            return captainPlayerId.takeIf { it.isNotBlank() && it in activePool }
                ?: SquadRuleValidator.autoPickCaptain(activePool)
        }

    val captainPlayer: Cricketer?
        get() = effectiveCaptainId.takeIf { it.isNotBlank() }?.let { CricketerDatabase.getById(it) }

    val benchPlayers: List<Cricketer>
        get() {
            val xiSet = playingXiIds.toSet()
            return players.filter { it.id !in xiSet }
        }

    val domesticCount: Int
        get() = (squadPlayerIds.size - overseasCount).coerceAtLeast(0)

    val meetsSquadRules: Boolean
        get() = squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE &&
            overseasCount <= SquadRules.MAX_OVERSEAS_IN_SQUAD &&
            domesticCount >= SquadRules.MIN_DOMESTIC_FOR_XI &&
            wkCount >= SquadRules.MIN_WICKETKEEPERS

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): TeamState {
            val rawPrices = map["squadPrices"] as? Map<String, Any?> ?: emptyMap()
            val parsedPrices = rawPrices.mapNotNull { (k, v) ->
                val num = (v as? Number)?.toLong()
                if (num != null) k to num else null
            }.toMap()
            val rawSquad = (map["squadPlayerIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val rawXi = (map["playingXiIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val legacyRetained = map["retainedPlayerId"] as? String ?: ""
            val rawRetainedList = (map["retainedPlayerIds"] as? List<*>)?.mapNotNull { it as? String }
                ?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(legacyRetained.takeIf { it.isNotBlank() })
            val rawFormerSquad = (map["formerSquadPlayerIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            return TeamState(
                teamId = map["teamId"] as? String ?: "",
                ownerUid = map["ownerUid"] as? String ?: "",
                teamName = map["teamName"] as? String ?: "",
                bot = map["bot"] as? Boolean ?: false,
                budgetLakhs = (map["budgetLakhs"] as? Number)?.toLong() ?: SquadRules.INITIAL_BUDGET_LAKHS,
                rtmRemaining = (map["rtmRemaining"] as? Number)?.toInt() ?: SquadRules.INITIAL_RTM_CARDS,
                retainedPlayerId = legacyRetained.ifBlank { rawRetainedList.firstOrNull().orEmpty() },
                retainedPlayerIds = rawRetainedList,
                formerSquadPlayerIds = rawFormerSquad,
                retentionDone = map["retentionDone"] as? Boolean ?: false,
                squadPlayerIds = rawSquad,
                squadPrices = parsedPrices,
                playingXiIds = rawXi,
                captainPlayerId = map["captainPlayerId"] as? String ?: "",
                xiReady = map["xiReady"] as? Boolean ?: false,
                overseasCount = (map["overseasCount"] as? Number)?.toInt() ?: 0,
                wkCount = (map["wkCount"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AuctionRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val memberUids: List<String> = emptyList(),
    val status: String = RoomStatus.LOBBY.name,
    val autoFillEightTeams: Boolean = true,
    val teams: List<TeamState> = emptyList(),
    val auctionOrder: List<String> = emptyList(),
    val currentPlayerIndex: Long = 0L,
    val currentBidLakhs: Long = 0L,
    val highestBidderTeamId: String = "",
    val highestBidderName: String = "",
    val bidDeadlineEpochMs: Long = 0L,
    val bidCount: Long = 0L,
    val soldCount: Long = 0L,
    val unsoldCount: Long = 0L,
    val lastEventMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val roomStatus: RoomStatus
        get() = RoomStatus.fromNameSafe(status)

    val currentCricketer: Cricketer?
        get() {
            val idx = currentPlayerIndex.toInt()
            if (idx !in auctionOrder.indices) return null
            return CricketerDatabase.getById(auctionOrder[idx])
        }

    fun nextBidAmountLakhs(): Long {
        val player = currentCricketer ?: return 50L
        return if (highestBidderTeamId.isBlank()) {
            player.basePriceLakhs
        } else {
            currentBidLakhs + CurrencyFormatter.nextBidIncrementLakhs(currentBidLakhs)
        }
    }

    fun teamForUser(uid: String): TeamState? = teams.firstOrNull { !it.bot && it.ownerUid == uid }
}

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val memberUids: List<String> = emptyList(),
    val stage: String = MatchStage.LEAGUE.name,
    val teamAId: String = "",
    val teamAName: String = "",
    val teamBId: String = "",
    val teamBName: String = "",
    val teamAScore: Long = 0L,
    val teamAWickets: Long = 0L,
    val teamAOvers: String = "0.0",
    val teamABalls: Long = 120L,
    val teamBScore: Long = 0L,
    val teamBWickets: Long = 0L,
    val teamBOvers: String = "0.0",
    val teamBBalls: Long = 120L,
    val winnerTeamId: String = "",
    val winnerTeamName: String = "",
    val resultSummary: String = "",
    val playerOfTheMatch: String = "",
    val innings1Worm: List<Long> = emptyList(),
    val innings2Worm: List<Long> = emptyList(),
    val playerPerformances: List<com.example.engine.MatchPlayerPerf> = emptyList(),
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val matchStage: MatchStage
        get() = MatchStage.fromNameSafe(stage)
}

data class BidEligibility(
    val allowed: Boolean,
    val reason: String? = null
)

object SquadRuleValidator {

    fun checkBidEligibility(
        team: TeamState,
        player: Cricketer,
        proposedBidLakhs: Long,
        currentHighestBidderTeamId: String
    ): BidEligibility {
        if (currentHighestBidderTeamId == team.teamId) {
            return BidEligibility(false, "You hold the top bid")
        }
        if (team.squadPlayerIds.size >= SquadRules.MAX_SQUAD_SIZE) {
            return BidEligibility(false, "Squad full (${SquadRules.MAX_SQUAD_SIZE} max)")
        }
        if (player.overseas && team.overseasCount >= SquadRules.MAX_OVERSEAS_IN_SQUAD) {
            return BidEligibility(false, "Max ${SquadRules.MAX_OVERSEAS_IN_SQUAD} overseas in squad reached")
        }
        // Ensure squad always keeps enough open slots for at least 7 Indian players (required for 4-OS Playing XI rule)
        if (player.overseas) {
            val remainingSlotsAfter = SquadRules.MAX_SQUAD_SIZE - (team.squadPlayerIds.size + 1)
            val maxPossibleDomestic = team.domesticCount + remainingSlotsAfter
            if (maxPossibleDomestic < SquadRules.MIN_DOMESTIC_FOR_XI) {
                return BidEligibility(false, "Reserve slots for ${SquadRules.MIN_DOMESTIC_FOR_XI} Indian players (4 OS XI rule)")
            }
        }
        if (team.wkCount == 0 && player.role != PlayerRole.WICKETKEEPER &&
            team.squadPlayerIds.size + 1 >= SquadRules.MAX_SQUAD_SIZE
        ) {
            return BidEligibility(false, "Must reserve slot for Wicketkeeper")
        }
        val remainingMandatorySlots = maxOf(0, SquadRules.MIN_SQUAD_SIZE - (team.squadPlayerIds.size + 1))
        val minReserveLakhs = remainingMandatorySlots * SquadRules.MIN_RESERVE_PER_SLOT_LAKHS
        val maxSpendable = team.budgetLakhs - minReserveLakhs
        if (proposedBidLakhs > maxSpendable) {
            return BidEligibility(
                false,
                "Keep ${CurrencyFormatter.formatLakhs(minReserveLakhs)} for $remainingMandatorySlots slots"
            )
        }
        return BidEligibility(true)
    }

    fun checkRtmEligibility(
        team: TeamState,
        player: Cricketer,
        currentBidLakhs: Long,
        currentHighestBidderTeamId: String
    ): BidEligibility {
        if (team.rtmRemaining <= 0) {
            return BidEligibility(false, "RTM card already used")
        }
        if (team.formerSquadPlayerIds.isNotEmpty() && player.id !in team.formerSquadPlayerIds) {
            return BidEligibility(false, "RTM only valid on your former squad players")
        }
        if (currentHighestBidderTeamId.isBlank() || currentBidLakhs <= 0L) {
            return BidEligibility(false, "Wait for a winning bid to match")
        }
        if (currentHighestBidderTeamId == team.teamId) {
            return BidEligibility(false, "You already lead the bid")
        }
        return checkBidEligibility(team, player, currentBidLakhs, "")
    }

    fun isFormerPlayerForRtm(team: TeamState, playerId: String): Boolean =
        team.rtmRemaining > 0 && team.formerSquadPlayerIds.isNotEmpty() && playerId in team.formerSquadPlayerIds

    fun validatePlayingXi(
        selectedIds: List<String>,
        ownedIds: List<String>,
        captainPlayerId: String = ""
    ): BidEligibility {
        if (selectedIds.size != SquadRules.MIN_SQUAD_SIZE) {
            return BidEligibility(false, "Select exactly 11 players (${selectedIds.size}/11)")
        }
        if (selectedIds.distinct().size != SquadRules.MIN_SQUAD_SIZE) {
            return BidEligibility(false, "Playing XI contains duplicate players")
        }
        if (!ownedIds.containsAll(selectedIds)) {
            return BidEligibility(false, "Selection contains players not in your squad")
        }
        val players = selectedIds.mapNotNull { CricketerDatabase.getById(it) }
        val overseas = players.count { it.overseas }
        if (overseas > SquadRules.MAX_OVERSEAS_IN_XI) {
            val excess = overseas - SquadRules.MAX_OVERSEAS_IN_XI
            return BidEligibility(
                false,
                "Max ${SquadRules.MAX_OVERSEAS_IN_XI} overseas players allowed in Playing XI ($overseas selected). Remove $excess overseas player(s) to start."
            )
        }
        val wks = players.count { it.role == PlayerRole.WICKETKEEPER }
        if (wks < SquadRules.MIN_WICKETKEEPERS) {
            return BidEligibility(false, "At least 1 Wicketkeeper required in Playing XI")
        }
        if (captainPlayerId.isNotBlank() && captainPlayerId !in selectedIds) {
            return BidEligibility(false, "Selected Captain must be inside your Playing XI")
        }
        return BidEligibility(true)
    }

    fun autoPickBestPlayingXi(squadIds: List<String>): List<String> {
        val squad = squadIds.mapNotNull { CricketerDatabase.getById(it) }
        if (squad.size <= SquadRules.MIN_SQUAD_SIZE) return orderBattingLineup(squad).map { it.id }

        val picked = mutableListOf<Cricketer>()
        var overseas = 0

        fun tryAdd(candidate: Cricketer): Boolean {
            if (picked.any { it.id == candidate.id }) return false
            if (candidate.overseas && overseas >= SquadRules.MAX_OVERSEAS_IN_XI) return false
            picked.add(candidate)
            if (candidate.overseas) overseas++
            return true
        }

        // 1. Best Wicketkeeper
        squad.filter { it.role == PlayerRole.WICKETKEEPER }
            .sortedByDescending { it.overallRating }
            .firstOrNull { tryAdd(it) }

        // 2. Top 2 Seamers & Top 2 Spinners (balanced Pomp attack!)
        squad.filter { it.role == PlayerRole.SEAMER }
            .sortedByDescending { it.bowlingRating }
            .forEach { if (picked.count { p -> p.role == PlayerRole.SEAMER } < 2) tryAdd(it) }

        squad.filter { it.role == PlayerRole.SPINNER }
            .sortedByDescending { it.bowlingRating }
            .forEach { if (picked.count { p -> p.role == PlayerRole.SPINNER } < 2) tryAdd(it) }

        // 3. Top 2 All-Rounders
        squad.filter { it.role == PlayerRole.ALL_ROUNDER }
            .sortedByDescending { it.overallRating }
            .forEach { if (picked.count { p -> p.role == PlayerRole.ALL_ROUNDER } < 2) tryAdd(it) }

        // 4. Fill remaining slots with best overall respecting 4-overseas XI cap
        squad.sortedByDescending { it.overallRating }
            .forEach { if (picked.size < SquadRules.MIN_SQUAD_SIZE) tryAdd(it) }

        // 5. Domestic fallback if overseas cap hit
        squad.filter { !it.overseas }
            .sortedByDescending { it.overallRating }
            .forEach { if (picked.size < SquadRules.MIN_SQUAD_SIZE) tryAdd(it) }

        return orderBattingLineup(picked).map { it.id }
    }

    /**
     * Automatically selects the strongest Captain (C) from a Playing XI or squad,
     * preferring marquee captains, icons/legends, wicketkeepers, and highest overall rating.
     */
    fun autoPickCaptain(playerIds: List<String>): String {
        val players = playerIds.mapNotNull { CricketerDatabase.getById(it) }
        if (players.isEmpty()) return ""
        return players.maxByOrNull { p ->
            val leadershipBonus = when {
                p.eraTag.contains("Captain", ignoreCase = true) -> 8
                p.specialty.contains("Captain", ignoreCase = true) -> 8
                p.eraTag.contains("Icon", ignoreCase = true) || p.eraTag.contains("Legend", ignoreCase = true) -> 4
                p.role == PlayerRole.WICKETKEEPER -> 3
                else -> 0
            }
            p.overallRating + leadershipBonus
        }?.id ?: players.first().id
    }

    fun orderBattingLineup(players: List<Cricketer>): List<Cricketer> {
        val batters = players.filter { it.role == PlayerRole.BATTER }.sortedByDescending { it.battingRating }
        val keepers = players.filter { it.role == PlayerRole.WICKETKEEPER }.sortedByDescending { it.battingRating }
        val allRounders = players.filter { it.role == PlayerRole.ALL_ROUNDER }.sortedByDescending { it.battingRating }
        val bowlers = players.filter { it.role.isSpecialistBowler }.sortedByDescending { it.battingRating }
        return batters + keepers + allRounders + bowlers
    }
}
