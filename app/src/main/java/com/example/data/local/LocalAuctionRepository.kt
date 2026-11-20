package com.example.data.local

import android.content.Context
import com.example.data.AuctionDataSource
import com.example.data.model.AuctionRoom
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.MatchRecord
import com.example.data.model.RoomStatus
import com.example.data.model.SquadRuleValidator
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
import com.example.data.model.UserProfile
import com.example.engine.AuctionBotEngine
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

/**
 * Lightweight local-first repository implementing the full CricBid auction, retention, RTM,
 * Playing XI, 5-Match Series, and 8-Team League + IPL Playoffs flow without requiring Google Sign-In.
 * Persists rooms and match records compactly in SharedPreferences JSON.
 */
class LocalAuctionRepository(
    context: Context,
    private val localUid: String
) : AuctionDataSource {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val trophyStore = runCatching { LocalTrophyCabinetStore(context) }.getOrNull()
    private val mutex = Mutex()

    private val roomsFlow = MutableStateFlow<Map<String, AuctionRoom>>(loadRoomsFromDisk())
    private val matchesFlow = MutableStateFlow<Map<String, List<MatchRecord>>>(loadMatchesFromDisk())

    override suspend fun ensureUserProfile(displayName: String): Result<UserProfile> =
        Result.success(UserProfile(id = localUid, userId = localUid, displayName = displayName.trim().ifBlank { "Manager" }))

    override fun observeMyRooms(uid: String): Flow<List<AuctionRoom>> =
        roomsFlow.map { map ->
            map.values
                .filter { uid in it.memberUids || it.hostUid == uid }
                .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
        }

    override fun observeRoom(roomCode: String): Flow<AuctionRoom?> =
        roomsFlow.map { it[roomCode] }

    override fun observeRoomMatches(roomCode: String, uid: String): Flow<List<MatchRecord>> =
        matchesFlow.map { it[roomCode].orEmpty() }

    override suspend fun createRoom(teamNameRaw: String, autoFillEightTeams: Boolean): Result<String> = mutex.withLock {
        val teamName = teamNameRaw.trim().take(36)
        if (teamName.length < 2) {
            return Result.failure(IllegalArgumentException("Enter a franchise name (2+ chars)"))
        }
        val current = roomsFlow.value.toMutableMap()
        var code = generateFourDigitCode()
        repeat(8) {
            if (!current.containsKey(code)) return@repeat
            code = generateFourDigitCode()
        }
        val now = Timestamp.now()
        val hostTeam = TeamState(
            teamId = "team_${localUid.take(10)}",
            ownerUid = localUid,
            teamName = teamName,
            bot = false,
            budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
            rtmRemaining = SquadRules.INITIAL_RTM_CARDS
        )
        val room = AuctionRoom(
            id = code,
            roomCode = code,
            hostUid = localUid,
            memberUids = listOf(localUid),
            status = RoomStatus.LOBBY.name,
            autoFillEightTeams = autoFillEightTeams,
            teams = listOf(hostTeam),
            auctionOrder = CricketerDatabase.generateAuctionOrder(),
            lastEventMessage = "$teamName created Room $code (₹100 Cr • 1 RTM)",
            createdAt = now,
            updatedAt = now
        )
        current[code] = room
        commitRooms(current)
        Result.success(code)
    }

    override suspend fun joinRoom(roomCodeRaw: String, teamNameRaw: String): Result<String> = mutex.withLock {
        val code = com.example.data.model.RoomCodeNormalizer.normalize(roomCodeRaw)
        val teamName = teamNameRaw.trim().take(36)
        if (!com.example.data.model.RoomCodeNormalizer.isValid(code)) {
            return Result.failure(IllegalArgumentException("Enter a valid 4-digit room code (e.g. 4829)."))
        }
        if (teamName.length < 2) {
            return Result.failure(IllegalArgumentException("Franchise name must be at least 2 characters."))
        }
        val current = roomsFlow.value.toMutableMap()
        val existing = current[code]
            ?: return Result.failure(
                IllegalStateException(
                    "Room $code not found on this device. To join a friend's room on another phone, tap 'Connect Cloud Live Sync' first."
                )
            )
        if (localUid in existing.memberUids) {
            return Result.success(code)
        }
        if (existing.roomStatus != RoomStatus.LOBBY) {
            return Result.failure(
                IllegalStateException("Room $code has already started (${existing.roomStatus.name.replace('_', ' ')}) and is closed to new players.")
            )
        }
        if (existing.teams.size >= SquadRules.MAX_TEAMS) {
            return Result.failure(
                IllegalStateException("Room $code is full (${existing.teams.size}/${SquadRules.MAX_TEAMS} franchises).")
            )
        }
        if (existing.teams.any { it.teamName.equals(teamName, ignoreCase = true) }) {
            return Result.failure(
                IllegalStateException("Franchise name '$teamName' is already taken in Room $code.")
            )
        }
        val newTeam = TeamState(
            teamId = "team_${localUid.take(10)}_${existing.teams.size}",
            ownerUid = localUid,
            teamName = teamName,
            bot = false,
            budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
            rtmRemaining = SquadRules.INITIAL_RTM_CARDS
        )
        current[code] = existing.copy(
            memberUids = (existing.memberUids + localUid).distinct(),
            teams = existing.teams + newTeam,
            lastEventMessage = "$teamName joined Room $code!",
            updatedAt = Timestamp.now()
        )
        commitRooms(current)
        Result.success(code)
    }

    override suspend fun toggleAutoFillEightTeams(roomCode: String, enabled: Boolean): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.LOBBY) { "Only editable in lobby" }
        val modeLabel = if (enabled) "8-Team League + Playoffs (AI Auto-Fill ON)" else "Custom / 2-Team 5-Match Series Mode"
        room.copy(
            autoFillEightTeams = enabled,
            lastEventMessage = "Format set: $modeLabel",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun addAiBot(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.LOBBY) { "Can only add bots in lobby" }
        if (room.teams.size >= SquadRules.MAX_TEAMS) {
            throw IllegalStateException("Max ${SquadRules.MAX_TEAMS} teams reached")
        }
        val updated = AuctionBotEngine.ensureMinimumTeamsWithBots(room.teams, room.teams.size + 1)
        val added = updated.last()
        room.copy(
            teams = updated,
            lastEventMessage = "${added.teamName} (AI Bot) entered the lobby",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun removeAiBot(roomCode: String, botTeamId: String): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.LOBBY) { "Can only remove bots in lobby" }
        val updated = room.teams.filterNot { it.bot && it.teamId == botTeamId }
        room.copy(
            teams = updated,
            lastEventMessage = "AI Bot removed from lobby",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun enterRetentionRound(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        val targetTeams = if (room.autoFillEightTeams) SquadRules.DEFAULT_TARGET_TEAMS else maxOf(room.teams.size, SquadRules.MIN_TEAMS)
        val filledTeams = AuctionBotEngine.ensureMinimumTeamsWithBots(room.teams, targetTeams)
        val marqueePool = CricketerDatabase.marqueeRetentionPool()
        val (teamsWithBotRetentions, retainedIds) = AuctionBotEngine.applyBotRetentions(
            teams = filledTeams,
            marqueePool = marqueePool,
            previousSquadLookup = { teamName -> trophyStore?.loadLastSeasonSquad(teamName).orEmpty() }
        )
        val remainingOrder = CricketerDatabase.generateAuctionOrder(excludedIds = retainedIds)
        room.copy(
            status = RoomStatus.RETENTION.name,
            teams = teamsWithBotRetentions,
            auctionOrder = remainingOrder,
            soldCount = retainedIds.size.toLong(),
            lastEventMessage = "${filledTeams.size} Franchises locked! Retain up to 4 players (₹15/11/8/6 Cr) + 1 RTM card.",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun retainOrSkipMarquee(roomCode: String, teamId: String, playerId: String?): Result<Unit> =
        submitMultiRetentions(roomCode, teamId, listOfNotNull(playerId))

    override suspend fun submitMultiRetentions(
        roomCode: String,
        teamId: String,
        playerIds: List<String>
    ): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.RETENTION) { "Retention round is closed" }
        val myTeam = room.teams.firstOrNull { it.teamId == teamId } ?: throw IllegalStateException("Team not found")
        check(!myTeam.retentionDone) { "Retention already submitted" }

        val uniqueIds = playerIds.distinct().take(SquadRules.MAX_RETENTIONS)
        val totalCost = SquadRules.totalRetentionCost(uniqueIds.size)
        if (totalCost > myTeam.budgetLakhs) {
            throw IllegalStateException("Not enough purse for ${uniqueIds.size} retentions")
        }

        var updatedOrder = room.auctionOrder
        var soldCount = room.soldCount
        val message: String

        val updatedTeams = if (uniqueIds.isNotEmpty()) {
            val takenByOthers = room.teams.filter { it.teamId != teamId }.flatMap { it.squadPlayerIds }.toSet()
            var workingTeam = myTeam
            val retainedNames = mutableListOf<String>()
            uniqueIds.forEachIndexed { slotIdx, pid ->
                if (pid in takenByOthers || pid in workingTeam.squadPlayerIds) {
                    throw IllegalStateException("One of the selected players was already retained")
                }
                val player = CricketerDatabase.getById(pid) ?: throw IllegalStateException("Player not found")
                val slotPrice = SquadRules.retentionPriceForSlot(slotIdx)
                workingTeam = AuctionBotEngine.applyPurchase(workingTeam, player, slotPrice)
                retainedNames.add(player.name)
                updatedOrder = updatedOrder.filterNot { it == pid }
                soldCount += 1L
            }
            val remainingFormer = workingTeam.formerSquadPlayerIds.filterNot { it in uniqueIds }
            val finalizedTeam = workingTeam.copy(
                retainedPlayerId = uniqueIds.first(),
                retainedPlayerIds = uniqueIds,
                formerSquadPlayerIds = remainingFormer,
                retentionDone = true
            )
            message = "${myTeam.teamName} retained ${uniqueIds.size} star(s) (${retainedNames.joinToString(", ")}) for ${CurrencyFormatter.formatLakhs(totalCost)}!"
            room.teams.map { t -> if (t.teamId == teamId) finalizedTeam else t }
        } else {
            message = "${myTeam.teamName} skipped retentions (kept full ₹100 Cr purse • 1 RTM ready)"
            room.teams.map { t -> if (t.teamId == teamId) t.copy(retentionDone = true) else t }
        }

        val allDone = updatedTeams.all { it.retentionDone }
        if (allDone) {
            val firstPlayer = updatedOrder.firstOrNull()?.let { CricketerDatabase.getById(it) }
            val deadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
            room.copy(
                status = RoomStatus.AUCTION.name,
                teams = updatedTeams,
                auctionOrder = updatedOrder,
                currentPlayerIndex = 0L,
                currentBidLakhs = 0L,
                highestBidderTeamId = "",
                highestBidderName = "",
                bidDeadlineEpochMs = deadline,
                bidCount = 0L,
                soldCount = soldCount,
                unsoldCount = 0L,
                lastEventMessage = "$message • Live Auction starts: ${firstPlayer?.name ?: ""}",
                updatedAt = Timestamp.now()
            )
        } else {
            room.copy(
                teams = updatedTeams,
                auctionOrder = updatedOrder,
                soldCount = soldCount,
                lastEventMessage = message,
                updatedAt = Timestamp.now()
            )
        }
    }

    override suspend fun beginLiveAuctionFromRetention(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        val firstPlayer = room.auctionOrder.firstOrNull()?.let { CricketerDatabase.getById(it) }
            ?: throw IllegalStateException("No players remaining")
        val deadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
        room.copy(
            status = RoomStatus.AUCTION.name,
            teams = room.teams.map { it.copy(retentionDone = true) },
            currentPlayerIndex = 0L,
            currentBidLakhs = 0L,
            highestBidderTeamId = "",
            highestBidderName = "",
            bidDeadlineEpochMs = deadline,
            bidCount = 0L,
            unsoldCount = 0L,
            lastEventMessage = "Gavel up! ${firstPlayer.name} opens at ${CurrencyFormatter.formatLakhs(firstPlayer.basePriceLakhs)}",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun startNewSeasonMegaAuction(roomCode: String): Result<Unit> = mutex.withLock {
        try {
            val current = roomsFlow.value.toMutableMap()
            val room = current[roomCode] ?: return Result.failure(IllegalStateException("Room not found"))
            val resetTeams = room.teams.map { t ->
                val carrySquad = t.squadPlayerIds.ifEmpty { t.formerSquadPlayerIds }.distinct().take(15)
                if (!t.bot && carrySquad.isNotEmpty()) {
                    trophyStore?.saveLastSeasonSquad(t.teamName, carrySquad)
                }
                TeamState(
                    teamId = t.teamId,
                    ownerUid = t.ownerUid,
                    teamName = t.teamName,
                    bot = t.bot,
                    budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
                    rtmRemaining = SquadRules.INITIAL_RTM_CARDS,
                    formerSquadPlayerIds = carrySquad,
                    retentionDone = false
                )
            }
            val marqueePool = CricketerDatabase.marqueeRetentionPool()
            val (teamsWithBots, retainedIds) = AuctionBotEngine.applyBotRetentions(resetTeams, marqueePool)
            val freshOrder = CricketerDatabase.generateAuctionOrder(excludedIds = retainedIds)
            current[roomCode] = room.copy(
                status = RoomStatus.RETENTION.name,
                teams = teamsWithBots,
                auctionOrder = freshOrder,
                currentPlayerIndex = 0L,
                currentBidLakhs = 0L,
                highestBidderTeamId = "",
                highestBidderName = "",
                bidDeadlineEpochMs = 0L,
                bidCount = 0L,
                soldCount = retainedIds.size.toLong(),
                unsoldCount = 0L,
                lastEventMessage = "New Season Mega Auction! Retain up to 4 players from last season's squad + 1 RTM card.",
                updatedAt = Timestamp.now()
            )
            commitRooms(current)
            val matchesMap = matchesFlow.value.toMutableMap()
            matchesMap[roomCode] = emptyList()
            commitMatches(matchesMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun placeBid(roomCode: String, teamId: String): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }
        val player = room.currentCricketer ?: throw IllegalStateException("No active player")
        val team = room.teams.firstOrNull { it.teamId == teamId } ?: throw IllegalStateException("Team not found")
        val proposedBid = room.nextBidAmountLakhs()
        val eligibility = SquadRuleValidator.checkBidEligibility(
            team = team,
            player = player,
            proposedBidLakhs = proposedBid,
            currentHighestBidderTeamId = room.highestBidderTeamId
        )
        if (!eligibility.allowed) throw IllegalStateException(eligibility.reason ?: "Bid not allowed")
        val newDeadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
        room.copy(
            currentBidLakhs = proposedBid,
            highestBidderTeamId = team.teamId,
            highestBidderName = team.teamName,
            bidDeadlineEpochMs = newDeadline,
            bidCount = room.bidCount + 1L,
            lastEventMessage = "${team.teamName} bid ${CurrencyFormatter.formatLakhs(proposedBid)} for ${player.name}!",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun useRtmCard(roomCode: String, teamId: String): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }
        val player = room.currentCricketer ?: throw IllegalStateException("No active player")
        val team = room.teams.firstOrNull { it.teamId == teamId } ?: throw IllegalStateException("Team not found")
        val rtmCheck = SquadRuleValidator.checkRtmEligibility(
            team = team,
            player = player,
            currentBidLakhs = room.currentBidLakhs,
            currentHighestBidderTeamId = room.highestBidderTeamId
        )
        if (!rtmCheck.allowed) throw IllegalStateException(rtmCheck.reason ?: "RTM not eligible")

        val matchedPrice = room.currentBidLakhs
        val updatedTeams = room.teams.map { t ->
            if (t.teamId == team.teamId) {
                AuctionBotEngine.applyPurchase(t, player, matchedPrice).copy(
                    rtmRemaining = (t.rtmRemaining - 1).coerceAtLeast(0)
                )
            } else t
        }
        val soldCount = (room.soldCount + 1L).coerceAtMost(120L)
        val rtmMsg = "⚡ RTM CARD! ${team.teamName} matched ${CurrencyFormatter.formatLakhs(matchedPrice)} to claim ${player.name}!"
        advanceOrFinishAuction(room, updatedTeams, soldCount, room.unsoldCount, rtmMsg)
    }

    override suspend fun finalizeCurrentLot(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        if (room.roomStatus != RoomStatus.AUCTION) return@updateRoom room
        val player = room.currentCricketer ?: return@updateRoom room

        val botRtmTeam = AuctionBotEngine.decideBotRtm(room)
        var updatedTeams = room.teams
        var soldCount = room.soldCount
        var unsoldCount = room.unsoldCount
        val lotEvent: String

        if (botRtmTeam != null && room.currentBidLakhs > 0L) {
            updatedTeams = room.teams.map { team ->
                if (team.teamId == botRtmTeam.teamId) {
                    AuctionBotEngine.applyPurchase(team, player, room.currentBidLakhs)
                        .copy(rtmRemaining = (team.rtmRemaining - 1).coerceAtLeast(0))
                } else team
            }
            soldCount += 1L
            lotEvent = "⚡ AI RTM! ${botRtmTeam.teamName} matched ${CurrencyFormatter.formatLakhs(room.currentBidLakhs)} for ${player.name}!"
        } else if (room.highestBidderTeamId.isNotBlank() && room.currentBidLakhs > 0L) {
            updatedTeams = room.teams.map { team ->
                if (team.teamId == room.highestBidderTeamId) {
                    AuctionBotEngine.applyPurchase(team, player, room.currentBidLakhs)
                } else team
            }
            soldCount += 1L
            lotEvent = "SOLD! ${player.name} -> ${room.highestBidderName} (${CurrencyFormatter.formatLakhs(room.currentBidLakhs)})"
        } else {
            unsoldCount += 1L
            lotEvent = "UNSOLD: ${player.name} passes"
        }

        advanceOrFinishAuction(room, updatedTeams, soldCount, unsoldCount, lotEvent)
    }

    override suspend fun fastForwardAuction(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }
        val alreadyBought = room.teams.flatMap { it.squadPlayerIds }.toSet()
        val remainingPool = CricketerDatabase.allPlayers.filter { it.id !in alreadyBought }
        val (completedTeams, extraSold) = AuctionBotEngine.autoCompleteRemainingAuction(room.teams, remainingPool)
        room.copy(
            status = RoomStatus.SQUAD_SELECTION.name,
            teams = completedTeams,
            currentPlayerIndex = room.auctionOrder.size.toLong(),
            currentBidLakhs = 0L,
            highestBidderTeamId = "",
            highestBidderName = "",
            bidDeadlineEpochMs = 0L,
            bidCount = 0L,
            soldCount = (room.soldCount + extraSold).coerceAtMost(120L),
            lastEventMessage = "Smart AI Fallback built all ${completedTeams.size} squads — select your Playing XI!",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun savePlayingXi(
        roomCode: String,
        teamId: String,
        selectedIds: List<String>,
        captainPlayerId: String
    ): Result<Unit> = updateRoom(roomCode) { room ->
        val team = room.teams.firstOrNull { it.teamId == teamId } ?: throw IllegalStateException("Your team was not found")
        val check = SquadRuleValidator.validatePlayingXi(
            selectedIds = selectedIds,
            ownedIds = team.squadPlayerIds,
            captainPlayerId = captainPlayerId
        )
        if (!check.allowed) throw IllegalStateException(check.reason ?: "Invalid Playing XI")

        // Preserve exact user-defined #1..#11 batting order
        val exactOrder = selectedIds.toList()
        val resolvedCaptain = captainPlayerId.takeIf { it.isNotBlank() && it in exactOrder }
            ?: SquadRuleValidator.autoPickCaptain(exactOrder)

        val updatedTeams = room.teams.map { t ->
            when {
                t.teamId == teamId -> t.copy(
                    playingXiIds = exactOrder,
                    captainPlayerId = resolvedCaptain,
                    xiReady = true
                )
                t.bot && !t.xiReady && t.squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE -> {
                    val aiXi = SquadRuleValidator.autoPickBestPlayingXi(t.squadPlayerIds)
                    t.copy(
                        playingXiIds = aiXi,
                        captainPlayerId = SquadRuleValidator.autoPickCaptain(aiXi),
                        xiReady = true
                    )
                }
                t.bot && t.captainPlayerId.isBlank() && t.playingXiIds.isNotEmpty() -> {
                    t.copy(captainPlayerId = SquadRuleValidator.autoPickCaptain(t.playingXiIds))
                }
                else -> t
            }
        }

        val allReady = updatedTeams.all { it.xiReady }
        if (allReady && room.roomStatus == RoomStatus.SQUAD_SELECTION) {
            room.copy(
                status = RoomStatus.MATCH_HUB.name,
                teams = updatedTeams,
                lastEventMessage = "All Playing XIs locked! Tournament Hub is live.",
                updatedAt = Timestamp.now()
            )
        } else {
            room.copy(
                teams = updatedTeams,
                lastEventMessage = "${team.teamName} locked their Playing XI & Captain!",
                updatedAt = Timestamp.now()
            )
        }
    }

    override suspend fun enterMatchHub(roomCode: String): Result<Unit> = updateRoom(roomCode) { room ->
        val updatedTeams = room.teams.map { t ->
            when {
                !t.xiReady && t.squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE -> {
                    val aiXi = SquadRuleValidator.autoPickBestPlayingXi(t.squadPlayerIds)
                    t.copy(
                        playingXiIds = aiXi,
                        captainPlayerId = SquadRuleValidator.autoPickCaptain(aiXi),
                        xiReady = true
                    )
                }
                t.captainPlayerId.isBlank() && t.playingXiIds.isNotEmpty() -> {
                    t.copy(captainPlayerId = SquadRuleValidator.autoPickCaptain(t.playingXiIds))
                }
                else -> t
            }
        }
        room.copy(
            status = RoomStatus.MATCH_HUB.name,
            teams = updatedTeams,
            lastEventMessage = "Championship Match Hub is live!",
            updatedAt = Timestamp.now()
        )
    }

    override suspend fun recordMatchResult(room: AuctionRoom, record: MatchRecord): Result<String> = mutex.withLock {
        val matchId = "m_${UUID.randomUUID().toString().take(10)}"
        val stored = record.copy(id = matchId, roomCode = room.roomCode, createdAt = Timestamp.now())
        val map = matchesFlow.value.toMutableMap()
        val list = map[room.roomCode].orEmpty() + stored
        map[room.roomCode] = list
        commitMatches(map)
        Result.success(matchId)
    }

    // --- HELPERS ---

    private fun advanceOrFinishAuction(
        room: AuctionRoom,
        updatedTeams: List<TeamState>,
        soldCount: Long,
        unsoldCount: Long,
        lotEvent: String
    ): AuctionRoom {
        val nextIdx = room.currentPlayerIndex + 1L
        val allFull = updatedTeams.all { it.squadPlayerIds.size >= SquadRules.MAX_SQUAD_SIZE }
        val reachedEnd = nextIdx >= room.auctionOrder.size || allFull

        return if (reachedEnd) {
            val boughtIds = updatedTeams.flatMap { it.squadPlayerIds }.toSet()
            val remainingUnsold = CricketerDatabase.allPlayers.filter { it.id !in boughtIds }
            val (completedTeams, extraSold) = if (updatedTeams.any { !it.meetsSquadRules } && remainingUnsold.isNotEmpty()) {
                AuctionBotEngine.autoCompleteRemainingAuction(updatedTeams, remainingUnsold)
            } else {
                updatedTeams.map { t ->
                    if (t.bot && t.squadPlayerIds.size >= SquadRules.MIN_SQUAD_SIZE) {
                        val aiXi = SquadRuleValidator.autoPickBestPlayingXi(t.squadPlayerIds)
                        t.copy(
                            playingXiIds = aiXi,
                            captainPlayerId = SquadRuleValidator.autoPickCaptain(aiXi),
                            xiReady = true
                        )
                    } else t
                } to 0
            }
            room.copy(
                status = RoomStatus.SQUAD_SELECTION.name,
                teams = completedTeams,
                currentPlayerIndex = nextIdx.coerceAtMost(room.auctionOrder.size.toLong()),
                currentBidLakhs = 0L,
                highestBidderTeamId = "",
                highestBidderName = "",
                bidDeadlineEpochMs = 0L,
                bidCount = 0L,
                soldCount = (soldCount + extraSold).coerceAtMost(120L),
                unsoldCount = unsoldCount.coerceAtMost(120L),
                lastEventMessage = "$lotEvent • Smart AI Fallback completed squads! Pick Playing XI.",
                updatedAt = Timestamp.now()
            )
        } else {
            val nextPlayer = CricketerDatabase.getById(room.auctionOrder[nextIdx.toInt()])
            val nextDeadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
            room.copy(
                status = RoomStatus.AUCTION.name,
                teams = updatedTeams,
                currentPlayerIndex = nextIdx,
                currentBidLakhs = 0L,
                highestBidderTeamId = "",
                highestBidderName = "",
                bidDeadlineEpochMs = nextDeadline,
                bidCount = 0L,
                soldCount = soldCount.coerceAtMost(120L),
                unsoldCount = unsoldCount.coerceAtMost(120L),
                lastEventMessage = "$lotEvent • Next: ${nextPlayer?.name ?: ""}",
                updatedAt = Timestamp.now()
            )
        }
    }

    private suspend fun updateRoom(roomCode: String, transform: (AuctionRoom) -> AuctionRoom): Result<Unit> = mutex.withLock {
        try {
            val current = roomsFlow.value.toMutableMap()
            val room = current[roomCode] ?: return Result.failure(IllegalStateException("Room not found"))
            current[roomCode] = transform(room)
            commitRooms(current)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun commitRooms(map: Map<String, AuctionRoom>) {
        roomsFlow.value = map
        prefs.edit().putString(KEY_ROOMS_JSON, serializeRooms(map)).apply()
    }

    private fun commitMatches(map: Map<String, List<MatchRecord>>) {
        matchesFlow.value = map
        prefs.edit().putString(KEY_MATCHES_JSON, serializeMatches(map)).apply()
    }

    private fun generateFourDigitCode(): String = Random.nextInt(1000, 10000).toString()

    // --- COMPACT JSON SERIALIZATION ---

    private fun serializeRooms(map: Map<String, AuctionRoom>): String {
        val arr = JSONArray()
        map.values.forEach { r ->
            val obj = JSONObject()
            obj.put("roomCode", r.roomCode)
            obj.put("hostUid", r.hostUid)
            obj.put("memberUids", JSONArray(r.memberUids))
            obj.put("status", r.status)
            obj.put("autoFillEightTeams", r.autoFillEightTeams)
            obj.put("auctionOrder", JSONArray(r.auctionOrder))
            obj.put("currentPlayerIndex", r.currentPlayerIndex)
            obj.put("currentBidLakhs", r.currentBidLakhs)
            obj.put("highestBidderTeamId", r.highestBidderTeamId)
            obj.put("highestBidderName", r.highestBidderName)
            obj.put("bidDeadlineEpochMs", r.bidDeadlineEpochMs)
            obj.put("bidCount", r.bidCount)
            obj.put("soldCount", r.soldCount)
            obj.put("unsoldCount", r.unsoldCount)
            obj.put("lastEventMessage", r.lastEventMessage)
            obj.put("updatedSec", r.updatedAt?.seconds ?: (System.currentTimeMillis() / 1000L))
            val teamsArr = JSONArray()
            r.teams.forEach { t ->
                val tObj = JSONObject()
                tObj.put("teamId", t.teamId)
                tObj.put("ownerUid", t.ownerUid)
                tObj.put("teamName", t.teamName)
                tObj.put("bot", t.bot)
                tObj.put("budgetLakhs", t.budgetLakhs)
                tObj.put("rtmRemaining", t.rtmRemaining)
                tObj.put("retainedPlayerId", t.retainedPlayerId)
                tObj.put("retainedPlayerIds", JSONArray(t.retainedPlayerIds))
                tObj.put("formerSquadPlayerIds", JSONArray(t.formerSquadPlayerIds))
                tObj.put("retentionDone", t.retentionDone)
                tObj.put("squadPlayerIds", JSONArray(t.squadPlayerIds))
                val pricesObj = JSONObject()
                t.squadPrices.forEach { (k, v) -> pricesObj.put(k, v) }
                tObj.put("squadPrices", pricesObj)
                tObj.put("playingXiIds", JSONArray(t.playingXiIds))
                tObj.put("captainPlayerId", t.captainPlayerId)
                tObj.put("xiReady", t.xiReady)
                tObj.put("overseasCount", t.overseasCount)
                tObj.put("wkCount", t.wkCount)
                teamsArr.put(tObj)
            }
            obj.put("teams", teamsArr)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun loadRoomsFromDisk(): Map<String, AuctionRoom> {
        val raw = prefs.getString(KEY_ROOMS_JSON, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val result = mutableMapOf<String, AuctionRoom>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val code = o.optString("roomCode")
                if (code.isBlank()) continue
                val teamsArr = o.optJSONArray("teams") ?: JSONArray()
                val teams = mutableListOf<TeamState>()
                for (j in 0 until teamsArr.length()) {
                    val t = teamsArr.getJSONObject(j)
                    val pricesObj = t.optJSONObject("squadPrices") ?: JSONObject()
                    val prices = mutableMapOf<String, Long>()
                    val keys = pricesObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        prices[k] = pricesObj.optLong(k)
                    }
                    val legacyRetained = t.optString("retainedPlayerId")
                    val retainedList = jsonToStringList(t.optJSONArray("retainedPlayerIds"))
                        .ifEmpty { listOfNotNull(legacyRetained.takeIf { it.isNotBlank() }) }
                    teams.add(
                        TeamState(
                            teamId = t.optString("teamId"),
                            ownerUid = t.optString("ownerUid"),
                            teamName = t.optString("teamName"),
                            bot = t.optBoolean("bot"),
                            budgetLakhs = t.optLong("budgetLakhs", SquadRules.INITIAL_BUDGET_LAKHS),
                            rtmRemaining = t.optInt("rtmRemaining", SquadRules.INITIAL_RTM_CARDS),
                            retainedPlayerId = legacyRetained.ifBlank { retainedList.firstOrNull().orEmpty() },
                            retainedPlayerIds = retainedList,
                            formerSquadPlayerIds = jsonToStringList(t.optJSONArray("formerSquadPlayerIds")),
                            retentionDone = t.optBoolean("retentionDone"),
                            squadPlayerIds = jsonToStringList(t.optJSONArray("squadPlayerIds")),
                            squadPrices = prices,
                            playingXiIds = jsonToStringList(t.optJSONArray("playingXiIds")),
                            captainPlayerId = t.optString("captainPlayerId"),
                            xiReady = t.optBoolean("xiReady"),
                            overseasCount = t.optInt("overseasCount"),
                            wkCount = t.optInt("wkCount")
                        )
                    )
                }
                val ts = Timestamp(o.optLong("updatedSec", System.currentTimeMillis() / 1000L), 0)
                result[code] = AuctionRoom(
                    id = code,
                    roomCode = code,
                    hostUid = o.optString("hostUid"),
                    memberUids = jsonToStringList(o.optJSONArray("memberUids")),
                    status = o.optString("status", RoomStatus.LOBBY.name),
                    autoFillEightTeams = o.optBoolean("autoFillEightTeams", true),
                    teams = teams,
                    auctionOrder = jsonToStringList(o.optJSONArray("auctionOrder")),
                    currentPlayerIndex = o.optLong("currentPlayerIndex"),
                    currentBidLakhs = o.optLong("currentBidLakhs"),
                    highestBidderTeamId = o.optString("highestBidderTeamId"),
                    highestBidderName = o.optString("highestBidderName"),
                    bidDeadlineEpochMs = o.optLong("bidDeadlineEpochMs"),
                    bidCount = o.optLong("bidCount"),
                    soldCount = o.optLong("soldCount"),
                    unsoldCount = o.optLong("unsoldCount"),
                    lastEventMessage = o.optString("lastEventMessage"),
                    createdAt = ts,
                    updatedAt = ts
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeMatches(map: Map<String, List<MatchRecord>>): String {
        val root = JSONObject()
        map.forEach { (code, list) ->
            val arr = JSONArray()
            list.forEach { m ->
                val o = JSONObject()
                o.put("id", m.id)
                o.put("roomCode", m.roomCode)
                o.put("stage", m.stage)
                o.put("teamAId", m.teamAId)
                o.put("teamAName", m.teamAName)
                o.put("teamBId", m.teamBId)
                o.put("teamBName", m.teamBName)
                o.put("teamAScore", m.teamAScore)
                o.put("teamAWickets", m.teamAWickets)
                o.put("teamAOvers", m.teamAOvers)
                o.put("teamABalls", m.teamABalls)
                o.put("teamBScore", m.teamBScore)
                o.put("teamBWickets", m.teamBWickets)
                o.put("teamBOvers", m.teamBOvers)
                o.put("teamBBalls", m.teamBBalls)
                o.put("winnerTeamId", m.winnerTeamId)
                o.put("winnerTeamName", m.winnerTeamName)
                o.put("resultSummary", m.resultSummary)
                o.put("playerOfTheMatch", m.playerOfTheMatch)
                val perfArr = JSONArray()
                m.playerPerformances.forEach { p ->
                    perfArr.put(
                        JSONObject().apply {
                            put("playerId", p.playerId)
                            put("playerName", p.playerName)
                            put("teamName", p.teamName)
                            put("runs", p.runs)
                            put("ballsFaced", p.ballsFaced)
                            put("fours", p.fours)
                            put("sixes", p.sixes)
                            put("wickets", p.wickets)
                            put("runsConceded", p.runsConceded)
                            put("legalBallsBowled", p.legalBallsBowled)
                        }
                    )
                }
                o.put("playerPerformances", perfArr)
                arr.put(o)
            }
            root.put(code, arr)
        }
        return root.toString()
    }

    private fun loadMatchesFromDisk(): Map<String, List<MatchRecord>> {
        val raw = prefs.getString(KEY_MATCHES_JSON, null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, List<MatchRecord>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                val arr = root.optJSONArray(code) ?: continue
                val list = mutableListOf<MatchRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val perfArr = o.optJSONArray("playerPerformances") ?: JSONArray()
                    val perfs = buildList {
                        for (pIdx in 0 until perfArr.length()) {
                            val po = perfArr.optJSONObject(pIdx) ?: continue
                            add(
                                com.example.engine.MatchPlayerPerf(
                                    playerId = po.optString("playerId"),
                                    playerName = po.optString("playerName"),
                                    teamName = po.optString("teamName"),
                                    runs = po.optInt("runs"),
                                    ballsFaced = po.optInt("ballsFaced"),
                                    fours = po.optInt("fours"),
                                    sixes = po.optInt("sixes"),
                                    wickets = po.optInt("wickets"),
                                    runsConceded = po.optInt("runsConceded"),
                                    legalBallsBowled = po.optInt("legalBallsBowled")
                                )
                            )
                        }
                    }
                    list.add(
                        MatchRecord(
                            id = o.optString("id"),
                            roomCode = o.optString("roomCode", code),
                            stage = o.optString("stage", "LEAGUE"),
                            teamAId = o.optString("teamAId"),
                            teamAName = o.optString("teamAName"),
                            teamBId = o.optString("teamBId"),
                            teamBName = o.optString("teamBName"),
                            teamAScore = o.optLong("teamAScore"),
                            teamAWickets = o.optLong("teamAWickets"),
                            teamAOvers = o.optString("teamAOvers", "20.0"),
                            teamABalls = o.optLong("teamABalls", 120L),
                            teamBScore = o.optLong("teamBScore"),
                            teamBWickets = o.optLong("teamBWickets"),
                            teamBOvers = o.optString("teamBOvers", "20.0"),
                            teamBBalls = o.optLong("teamBBalls", 120L),
                            winnerTeamId = o.optString("winnerTeamId"),
                            winnerTeamName = o.optString("winnerTeamName"),
                            resultSummary = o.optString("resultSummary"),
                            playerOfTheMatch = o.optString("playerOfTheMatch"),
                            playerPerformances = perfs
                        )
                    )
                }
                result[code] = list
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun jsonToStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            list.add(arr.optString(i))
        }
        return list
    }

    private companion object {
        const val PREFS_NAME = "cricbid_local_rooms_v1"
        const val KEY_ROOMS_JSON = "rooms_json"
        const val KEY_MATCHES_JSON = "matches_json"
    }
}
