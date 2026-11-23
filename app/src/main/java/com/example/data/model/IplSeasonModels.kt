package com.example.data.model

/**
 * Official IPL 2026 Season & Mega Auction Rules, Models, and State.
 */
object IplRules {
    const val TOTAL_FRANCHISES: Int = 10
    const val LEAGUE_MATCHES_PER_TEAM: Int = 14
    const val TOTAL_LEAGUE_MATCHES: Int = 70
    const val PURSE_LAKHS: Long = 12_000L // ₹120 Crore
    const val MATCH_FEE_PER_PLAYER_LAKHS_X10: Long = 75L // ₹7.5 Lakh per player per game (in 0.1L units)
    const val PLAYERS_PAID_PER_MATCH: Int = 12 // Playing XI (11) + Impact Player (1)
    const val MATCH_FEE_PER_TEAM_GAME_LAKHS: Long = 90L // 12 x ₹7.5L = ₹90 Lakh per match per franchise

    const val MAX_RETENTION_PLUS_RTM: Int = 6
    const val MAX_CAPPED_RETENTION_RTM: Int = 5
    const val MAX_UNCAPPED_INDIAN_RETENTION_RTM: Int = 2

    val CAPPED_RETENTION_SLABS_LAKHS: List<Long> = listOf(1_800L, 1_400L, 1_100L, 1_800L, 1_400L) // ₹18,14,11,18,14 Cr
    const val UNCAPPED_INDIAN_RETENTION_LAKHS: Long = 400L // ₹4 Cr

    const val MIN_SQUAD_SIZE: Int = 18
    const val MAX_SQUAD_SIZE: Int = 25
    const val MAX_OVERSEAS_IN_SQUAD: Int = 8
    const val MAX_OVERSEAS_IN_XI: Int = 4
    const val IMPACT_NOMINEES_COUNT: Int = 5
    const val MAX_HAMMER_LOOT_PER_TEAM: Int = 2
    const val MIN_RESERVE_PER_SLOT_LAKHS: Long = 30L
    const val AUCTION_TIMER_SECONDS: Int = 10

    fun cappedRetentionPrice(cappedSlotIndex: Int): Long =
        CAPPED_RETENTION_SLABS_LAKHS.getOrElse(cappedSlotIndex.coerceAtLeast(0)) { 1_100L }

    fun calculateRetentionCostLakhs(playerIds: List<String>): Long {
        var cappedCount = 0
        var total = 0L
        playerIds.distinct().forEach { pid ->
            if (Ipl2026Catalog.isUncappedIndian(pid)) {
                total += UNCAPPED_INDIAN_RETENTION_LAKHS
            } else {
                total += cappedRetentionPrice(cappedCount)
                cappedCount++
            }
        }
        return total
    }

    fun formatMatchFeeLakhs(totalLakhs: Double): String {
        return if (totalLakhs >= 100.0) {
            String.format(java.util.Locale.US, "₹%.2f Cr", totalLakhs / 100.0)
        } else if (totalLakhs % 1.0 == 0.0) {
            "₹${totalLakhs.toLong()} L"
        } else {
            String.format(java.util.Locale.US, "₹%.1f L", totalLakhs)
        }
    }
}

enum class IplPlayMode(val label: String, val subtitle: String) {
    SOLO("Solo IPL Season", "Pick 1 franchise • Play your matches • AI auto-sims rest"),
    FRIEND_ROOM("Friend Room IPL", "2+ Human Friends + AI teams • 4-Digit Room Code • Shared Schedule");

    companion object {
        fun fromNameSafe(raw: String?): IplPlayMode =
            entries.firstOrNull { it.name == raw } ?: SOLO
    }
}

enum class IplSeasonPhase(val label: String) {
    SETUP("Franchise Selection"),
    RETENTION("Pre-Auction Retentions & RTM"),
    AUCTION("Mega Auction (₹120 Cr)"),
    SQUAD_XI("Playing XI & Impact Subs"),
    LEAGUE("70-Match League Stage"),
    PLAYOFFS("Top-4 IPL Playoffs"),
    COMPLETED("IPL Season Complete");

    companion object {
        fun fromNameSafe(raw: String?): IplSeasonPhase =
            entries.firstOrNull { it.name == raw } ?: SETUP
    }
}

data class IplFranchiseState(
    val franchiseId: String,
    val shortCode: String,
    val teamName: String,
    val defendingChampion: Boolean = false,
    val primaryColorHex: Long = 0xFF00C982L,
    val accentColorHex: Long = 0xFFFFC107L,
    val isHuman: Boolean = false,
    val ownerName: String = "AI Manager",
    val purseRemainingLakhs: Long = IplRules.PURSE_LAKHS,
    val matchFeesSpentLakhs: Double = 0.0,
    val matchesPaidCount: Int = 0,
    val hammerLootRemaining: Int = IplRules.MAX_HAMMER_LOOT_PER_TEAM,
    val retainedPlayerIds: List<String> = emptyList(),
    val rtmUsedPlayerIds: List<String> = emptyList(),
    val formerSquadIds: List<String> = emptyList(),
    val retentionDone: Boolean = false,
    val squadPlayerIds: List<String> = emptyList(),
    val squadPrices: Map<String, Long> = emptyMap(),
    val playingXiIds: List<String> = emptyList(),
    val impactNomineeIds: List<String> = emptyList(),
    val captainPlayerId: String = ""
) {
    val players: List<Cricketer>
        get() = squadPlayerIds.mapNotNull { CricketerDatabase.getById(it) }

    val overseasCount: Int
        get() = players.count { it.overseas }

    val indianCount: Int
        get() = players.count { !it.overseas }

    val wkCount: Int
        get() = players.count { it.role == PlayerRole.WICKETKEEPER }

    val retainedCappedCount: Int
        get() = (retainedPlayerIds + rtmUsedPlayerIds).distinct().count { !Ipl2026Catalog.isUncappedIndian(it) }

    val retainedUncappedIndianCount: Int
        get() = (retainedPlayerIds + rtmUsedPlayerIds).distinct().count { Ipl2026Catalog.isUncappedIndian(it) }

    val totalRetentionsAndRtmsUsed: Int
        get() = (retainedPlayerIds + rtmUsedPlayerIds).distinct().size

    val rtmCardsRemaining: Int
        get() = (IplRules.MAX_RETENTION_PLUS_RTM - totalRetentionsAndRtmsUsed).coerceAtLeast(0)

    val canUseRtmForCapped: Boolean
        get() = rtmCardsRemaining > 0 && retainedCappedCount < IplRules.MAX_CAPPED_RETENTION_RTM

    val canUseRtmForUncappedIndian: Boolean
        get() = rtmCardsRemaining > 0 && retainedUncappedIndianCount < IplRules.MAX_UNCAPPED_INDIAN_RETENTION_RTM

    fun canUseRtmOnPlayer(playerId: String): Boolean {
        if (rtmCardsRemaining <= 0) return false
        if (playerId !in formerSquadIds) return false
        if (playerId in squadPlayerIds) return false
        return if (Ipl2026Catalog.isUncappedIndian(playerId)) {
            canUseRtmForUncappedIndian
        } else {
            canUseRtmForCapped
        }
    }

    fun toTeamState(): TeamState {
        val xi = playingXiIds.takeIf { it.size == 11 }
            ?: SquadRuleValidator.autoPickBestPlayingXi(squadPlayerIds)
        val cap = captainPlayerId.takeIf { it.isNotBlank() && it in xi }
            ?: SquadRuleValidator.autoPickCaptain(xi)
        return TeamState(
            teamId = franchiseId,
            ownerUid = if (isHuman) "human_$franchiseId" else "ai_$franchiseId",
            teamName = teamName,
            bot = !isHuman,
            budgetLakhs = purseRemainingLakhs,
            rtmRemaining = rtmCardsRemaining,
            retainedPlayerId = retainedPlayerIds.firstOrNull().orEmpty(),
            retainedPlayerIds = retainedPlayerIds,
            formerSquadPlayerIds = formerSquadIds,
            retentionDone = retentionDone,
            squadPlayerIds = squadPlayerIds,
            squadPrices = squadPrices,
            playingXiIds = xi,
            captainPlayerId = cap,
            xiReady = xi.size == 11,
            overseasCount = overseasCount,
            wkCount = wkCount
        )
    }
}

data class IplScheduledMatch(
    val matchNumber: Int, // 1..70 for league; 71=Q1, 72=Elim, 73=Q2, 74=Final
    val matchday: Int,
    val stage: MatchStage = MatchStage.LEAGUE,
    val teamAId: String,
    val teamBId: String,
    val venue: String = "",
    val completedRecord: MatchRecord? = null
) {
    val isCompleted: Boolean get() = completedRecord != null
}

data class IplAuctionLiveState(
    val auctionOrder: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val currentBidLakhs: Long = 0L,
    val highestBidderFranchiseId: String = "",
    val highestBidderName: String = "",
    val soldCount: Int = 0,
    val unsoldCount: Int = 0,
    val passedFranchiseIds: Set<String> = emptySet(),
    val lastEventMessage: String = "IPL 2026 Mega Auction ready! ₹120 Cr purse • 18–25 squad • 2 Hammer Loots/team"
) {
    val currentPlayer: Cricketer?
        get() = auctionOrder.getOrNull(currentIndex)?.let { CricketerDatabase.getById(it) }

    fun nextBidLakhs(): Long {
        val p = currentPlayer ?: return 30L
        return if (highestBidderFranchiseId.isBlank()) {
            p.basePriceLakhs
        } else {
            currentBidLakhs + CurrencyFormatter.nextBidIncrementLakhs(currentBidLakhs)
        }
    }
}

data class IplAiResultsBatch(
    val title: String,
    val results: List<MatchRecord>,
    val shownAtMs: Long = System.currentTimeMillis()
)

data class IplSeasonState(
    val seasonNumber: Int = 1,
    val seasonYear: Int = 2026,
    val playMode: IplPlayMode = IplPlayMode.SOLO,
    val roomCode: String = "",
    val phase: IplSeasonPhase = IplSeasonPhase.SETUP,
    val activeHumanFranchiseId: String = "ipl_rcb",
    val humanFranchiseIds: List<String> = listOf("ipl_rcb"),
    val franchises: List<IplFranchiseState> = emptyList(),
    val auction: IplAuctionLiveState = IplAuctionLiveState(),
    val schedule: List<IplScheduledMatch> = emptyList(),
    val playoffs: List<IplScheduledMatch> = emptyList(),
    val championFranchiseId: String = "",
    val championTeamName: String = "",
    val runnerUpTeamName: String = "",
    val trophyRecorded: Boolean = false
) {
    val activeHumanFranchise: IplFranchiseState?
        get() = franchises.firstOrNull { it.franchiseId == activeHumanFranchiseId }
            ?: franchises.firstOrNull { it.isHuman }

    val allCompletedMatches: List<MatchRecord>
        get() = (schedule + playoffs).mapNotNull { it.completedRecord }

    val leagueCompletedCount: Int
        get() = schedule.count { it.isCompleted }

    val isLeagueFinished: Boolean
        get() = schedule.isNotEmpty() && schedule.all { it.isCompleted }

    fun franchiseById(id: String): IplFranchiseState? = franchises.firstOrNull { it.franchiseId == id }

    fun involvesHuman(fixture: IplScheduledMatch): Boolean =
        fixture.teamAId in humanFranchiseIds || fixture.teamBId in humanFranchiseIds
}
