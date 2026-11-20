package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class AuctionRepository(
    private val db: FirebaseFirestore,
    private val activeActorId: String = "",
    initialRecentRoomCodes: List<String> = emptyList(),
    private val onRememberRoomCode: (String) -> Unit = {},
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) : AuctionDataSource {
    constructor(context: Context, activeActorId: String = "") : this(
        db = com.example.FirebaseSafeInit.safeFirestore(context)
            ?: throw IllegalStateException("Firestore is not initialized in this environment."),
        activeActorId = activeActorId,
        auth = com.example.FirebaseSafeInit.safeFirebaseAuth(context)
    )

    private val recentRoomCodesFlow = MutableStateFlow(
        initialRecentRoomCodes
            .map { it.trim() }
            .filter { it.matches(Regex("^[0-9]{4}$")) }
            .distinct()
            .take(6)
    )

    private fun actorId(): String =
        runCatching { auth?.currentUser?.uid }.getOrNull()
            ?: activeActorId.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Player session ID is missing. Please restart CricBid.")

    private fun rememberCode(roomCode: String) {
        val cleaned = roomCode.trim()
        if (!cleaned.matches(Regex("^[0-9]{4}$"))) return
        val updated = (listOf(cleaned) + recentRoomCodesFlow.value.filterNot { it == cleaned }).take(6)
        recentRoomCodesFlow.value = updated
        onRememberRoomCode(cleaned)
    }

    // --- USER PROFILE (Strictly Authenticated Cloud Profile; Guests stay local) ---

    override suspend fun ensureUserProfile(displayName: String): Result<UserProfile> {
        val safeName = displayName.trim().take(60).ifBlank { "Cricket Manager" }
        val authUid = runCatching { auth?.currentUser?.uid }.getOrNull()
            ?: return Result.success(UserProfile(id = actorId(), userId = actorId(), displayName = safeName))
        val path = "users/$authUid"
        return try {
            val docRef = db.collection("users").document(authUid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (existing != null) return Result.success(existing)
            }
            val payload = mapOf(
                "userId" to authUid,
                "displayName" to safeName,
                "matchesPlayed" to 0L,
                "matchesWon" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val fresh = docRef.get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = authUid, userId = authUid, displayName = safeName)
            Result.success(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- REAL-TIME OBSERVERS ---

    override fun observeMyRooms(uid: String): Flow<List<AuctionRoom>> {
        val authUid = runCatching { auth?.currentUser?.uid }.getOrNull()
        if (authUid != null && authUid == uid) {
            return flow {
                val path = "rooms"
                emitAll(
                    db.collection("rooms")
                        .whereArrayContains("memberUids", authUid)
                        .snapshots()
                        .map { snapshot ->
                            snapshot.documents.mapNotNull { doc -> parseRoomSnapshot(doc) }
                                .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                        }
                        .distinctUntilChanged()
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val exceptionToLog = firestoreError ?: (error as? Exception)
                            if (exceptionToLog != null && runCatching { auth?.currentUser?.uid }.getOrNull() == authUid) {
                                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                            }
                            throw error
                        }
                )
            }
        }

        // Guest mode: observe each remembered 4-digit room document directly (no unauthenticated collection list!)
        return recentRoomCodesFlow.flatMapLatest { codes ->
            if (codes.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    codes.map { code ->
                        observeRoom(code).catch { emit(null) }
                    }
                ) { snapshots ->
                    snapshots.filterNotNull()
                        .filter { room -> uid in room.memberUids || room.hostUid == uid }
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }.distinctUntilChanged()
            }
        }
    }

    override fun observeRoom(roomCode: String): Flow<AuctionRoom?> = flow {
        val path = "rooms/$roomCode"
        emitAll(
            db.collection("rooms").document(roomCode)
                .snapshots()
                .map { doc -> if (doc.exists()) parseRoomSnapshot(doc) else null }
                .distinctUntilChanged()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    override fun observeRoomMatches(roomCode: String, uid: String): Flow<List<MatchRecord>> = flow {
        val path = "rooms/$roomCode/matches"
        emitAll(
            db.collection("rooms").document(roomCode).collection("matches")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MatchRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .distinctUntilChanged()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- ROOM CREATION & LOBBY ---

    override suspend fun createRoom(teamNameRaw: String, autoFillEightTeams: Boolean): Result<String> {
        val uid = actorId()
        val teamName = teamNameRaw.trim().take(36).ifBlank { return Result.failure(IllegalArgumentException("Enter a team name")) }
        var roomCode = generateFourDigitCode()
        val path = "rooms/$roomCode"
        return try {
            var docRef = db.collection("rooms").document(roomCode)
            for (attempt in 0 until 5) {
                val existing = try { docRef.get().await() } catch (_: FirebaseFirestoreException) { null }
                if (existing != null && !existing.exists()) break
                roomCode = generateFourDigitCode()
                docRef = db.collection("rooms").document(roomCode)
            }

            val hostTeam = TeamState(
                teamId = "team_${uid.takeLast(12)}",
                ownerUid = uid,
                teamName = teamName,
                bot = false,
                budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
                rtmRemaining = SquadRules.INITIAL_RTM_CARDS
            )
            val order = CricketerDatabase.generateAuctionOrder()
            val payload = mapOf(
                "roomCode" to roomCode,
                "hostUid" to uid,
                "memberUids" to listOf(uid),
                "status" to RoomStatus.LOBBY.name,
                "autoFillEightTeams" to autoFillEightTeams,
                "teams" to listOf(hostTeam.toMap()),
                "auctionOrder" to order,
                "currentPlayerIndex" to 0L,
                "currentBidLakhs" to 0L,
                "highestBidderTeamId" to "",
                "highestBidderName" to "",
                "bidDeadlineEpochMs" to 0L,
                "bidCount" to 0L,
                "soldCount" to 0L,
                "unsoldCount" to 0L,
                "lastEventMessage" to "$teamName created Room $roomCode (₹100 Cr • 1 RTM)",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            rememberCode(roomCode)
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun joinRoom(roomCodeRaw: String, teamNameRaw: String): Result<String> {
        val uid = actorId()
        val roomCode = com.example.data.model.RoomCodeNormalizer.normalize(roomCodeRaw)
        val teamName = teamNameRaw.trim().take(36)
        if (!com.example.data.model.RoomCodeNormalizer.isValid(roomCode)) {
            return Result.failure(IllegalArgumentException("Enter a valid 4-digit room code (e.g. 4829)."))
        }
        if (teamName.length < 2) {
            return Result.failure(IllegalArgumentException("Franchise name must be at least 2 characters."))
        }
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Room $roomCode not found. Double-check the 4-digit code with your friend and try again.")
                }
                val room = parseRoomSnapshot(snap)
                    ?: throw IllegalStateException("Room $roomCode data could not be read. Please try again.")
                if (uid in room.memberUids) {
                    // Already a participant — allow seamless rejoin across devices!
                    return@runTransaction
                }
                if (room.roomStatus != RoomStatus.LOBBY) {
                    throw IllegalStateException("Room $roomCode has already started (${room.roomStatus.name.replace('_', ' ')}) and is closed to new players.")
                }
                if (room.teams.size >= SquadRules.MAX_TEAMS || room.memberUids.size >= SquadRules.MAX_TEAMS) {
                    throw IllegalStateException("Room $roomCode is full (${room.teams.size}/${SquadRules.MAX_TEAMS} franchises). Ask the host to remove an AI Bot.")
                }
                if (room.teams.any { it.teamName.equals(teamName, ignoreCase = true) }) {
                    throw IllegalStateException("Franchise name '$teamName' is already taken in Room $roomCode. Pick a different name.")
                }
                val baseId = "team_${uid.takeLast(12)}"
                val uniqueTeamId = if (room.teams.none { it.teamId == baseId }) {
                    baseId
                } else {
                    "${baseId}_${room.teams.size}"
                }
                val newTeam = TeamState(
                    teamId = uniqueTeamId,
                    ownerUid = uid,
                    teamName = teamName,
                    bot = false,
                    budgetLakhs = SquadRules.INITIAL_BUDGET_LAKHS,
                    rtmRemaining = SquadRules.INITIAL_RTM_CARDS
                )
                tx.update(
                    docRef,
                    mapOf(
                        "memberUids" to (room.memberUids + uid),
                        "teams" to (room.teams + newTeam).map { it.toMap() },
                        "lastEventMessage" to "$teamName joined the auction lobby!".take(240),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            rememberCode(roomCode)
            Result.success(roomCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            val friendly = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    IllegalStateException("Room $roomCode has already started its auction and is closed to new players.")
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    IllegalStateException("Network issue reaching Room $roomCode. Check your connection and tap Join again.")
                else -> e
            }
            Result.failure(friendly)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleAutoFillEightTeams(roomCode: String, enabled: Boolean): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can change AI settings" }
                check(room.roomStatus == RoomStatus.LOBBY) { "Only editable in lobby" }
                val modeLabel = if (enabled) "8-Team League + Playoffs (AI Auto-Fill ON)" else "Custom / 2-Team 5-Match Series Mode"
                tx.update(
                    docRef,
                    mapOf(
                        "autoFillEightTeams" to enabled,
                        "lastEventMessage" to "Format set: $modeLabel",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun addAiBot(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only the room host can add AI bots" }
                check(room.roomStatus == RoomStatus.LOBBY) { "Can only add bots in lobby" }
                if (room.teams.size >= SquadRules.MAX_TEAMS) {
                    throw IllegalStateException("Max ${SquadRules.MAX_TEAMS} teams reached")
                }
                val updated = AuctionBotEngine.ensureMinimumTeamsWithBots(room.teams, room.teams.size + 1)
                val added = updated.last()
                tx.update(
                    docRef,
                    mapOf(
                        "teams" to updated.map { it.toMap() },
                        "lastEventMessage" to "${added.teamName} (AI Bot) entered the lobby",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun removeAiBot(roomCode: String, botTeamId: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can remove bots" }
                check(room.roomStatus == RoomStatus.LOBBY) { "Can only remove bots in lobby" }
                val updated = room.teams.filterNot { it.bot && it.teamId == botTeamId }
                tx.update(
                    docRef,
                    mapOf(
                        "teams" to updated.map { it.toMap() },
                        "lastEventMessage" to "AI Bot removed from lobby",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- MARQUEE / RETENTION ROUND ---

    override suspend fun enterRetentionRound(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only the host can start the auction" }

                val targetTeams = if (room.autoFillEightTeams) SquadRules.DEFAULT_TARGET_TEAMS else maxOf(room.teams.size, SquadRules.MIN_TEAMS)
                val filledTeams = AuctionBotEngine.ensureMinimumTeamsWithBots(room.teams, targetTeams)
                val marqueePool = CricketerDatabase.marqueeRetentionPool()
                val (teamsWithBotRetentions, retainedIds) = AuctionBotEngine.applyBotRetentions(filledTeams, marqueePool)
                val remainingOrder = CricketerDatabase.generateAuctionOrder(excludedIds = retainedIds)

                tx.update(
                    docRef,
                    mapOf(
                        "status" to RoomStatus.RETENTION.name,
                        "teams" to teamsWithBotRetentions.map { it.toMap() },
                        "auctionOrder" to remainingOrder,
                        "soldCount" to retainedIds.size.toLong(),
                        "lastEventMessage" to "${filledTeams.size} Franchises locked! Retain up to 4 stars (₹15/11/8/6 Cr) + 1 RTM card.",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun retainOrSkipMarquee(roomCode: String, teamId: String, playerId: String?): Result<Unit> =
        submitMultiRetentions(roomCode, teamId, listOfNotNull(playerId))

    override suspend fun submitMultiRetentions(
        roomCode: String,
        teamId: String,
        playerIds: List<String>
    ): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.roomStatus == RoomStatus.RETENTION) { "Retention round is closed" }
                check(uid in room.memberUids) { "Not a member of this room" }
                val myTeam = room.teams.firstOrNull { it.teamId == teamId && it.ownerUid == uid }
                    ?: throw IllegalStateException("Team not found")
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
                    val names = mutableListOf<String>()
                    uniqueIds.forEachIndexed { slotIdx, pid ->
                        if (pid in takenByOthers || pid in workingTeam.squadPlayerIds) {
                            throw IllegalStateException("That star was already retained")
                        }
                        val player = CricketerDatabase.getById(pid)
                            ?: throw IllegalStateException("Player not found")
                        workingTeam = AuctionBotEngine.applyPurchase(
                            workingTeam,
                            player,
                            SquadRules.retentionPriceForSlot(slotIdx)
                        )
                        names.add(player.name)
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
                    message = "${myTeam.teamName} retained ${uniqueIds.size} star(s) (${names.joinToString(", ")}) for ${CurrencyFormatter.formatLakhs(totalCost)}!"
                    room.teams.map { t -> if (t.teamId == teamId) finalizedTeam else t }
                } else {
                    message = "${myTeam.teamName} skipped retentions (kept full ₹100 Cr purse)"
                    room.teams.map { t ->
                        if (t.teamId == teamId) t.copy(retentionDone = true) else t
                    }
                }

                val allDone = updatedTeams.all { it.retentionDone }
                val isHost = room.hostUid == uid

                if (isHost && allDone) {
                    val firstPlayer = updatedOrder.firstOrNull()?.let { CricketerDatabase.getById(it) }
                    val deadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
                    tx.update(
                        docRef,
                        mapOf(
                            "status" to RoomStatus.AUCTION.name,
                            "teams" to updatedTeams.map { it.toMap() },
                            "auctionOrder" to updatedOrder,
                            "currentPlayerIndex" to 0L,
                            "currentBidLakhs" to 0L,
                            "highestBidderTeamId" to "",
                            "highestBidderName" to "",
                            "bidDeadlineEpochMs" to deadline,
                            "bidCount" to 0L,
                            "soldCount" to soldCount,
                            "unsoldCount" to 0L,
                            "lastEventMessage" to "$message • Live Auction starts: ${firstPlayer?.name ?: ""}".take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        docRef,
                        mapOf(
                            "teams" to updatedTeams.map { it.toMap() },
                            "auctionOrder" to updatedOrder,
                            "soldCount" to soldCount,
                            "lastEventMessage" to message.take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun beginLiveAuctionFromRetention(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can launch Live Auction" }
                val firstPlayer = room.auctionOrder.firstOrNull()?.let { CricketerDatabase.getById(it) }
                    ?: throw IllegalStateException("No players remaining")
                val deadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
                val finalizedTeams = room.teams.map { it.copy(retentionDone = true) }
                tx.update(
                    docRef,
                    mapOf(
                        "status" to RoomStatus.AUCTION.name,
                        "teams" to finalizedTeams.map { it.toMap() },
                        "currentPlayerIndex" to 0L,
                        "currentBidLakhs" to 0L,
                        "highestBidderTeamId" to "",
                        "highestBidderName" to "",
                        "bidDeadlineEpochMs" to deadline,
                        "bidCount" to 0L,
                        "unsoldCount" to 0L,
                        "lastEventMessage" to "Gavel up! ${firstPlayer.name} opens at ${CurrencyFormatter.formatLakhs(firstPlayer.basePriceLakhs)}",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun startNewSeasonMegaAuction(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can launch a New Season Mega Auction" }
                val resetTeams = room.teams.map { t ->
                    val carrySquad = t.squadPlayerIds.ifEmpty { t.formerSquadPlayerIds }.distinct().take(15)
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
                tx.update(
                    docRef,
                    mapOf(
                        "status" to RoomStatus.RETENTION.name,
                        "teams" to teamsWithBots.map { it.toMap() },
                        "auctionOrder" to freshOrder,
                        "currentPlayerIndex" to 0L,
                        "currentBidLakhs" to 0L,
                        "highestBidderTeamId" to "",
                        "highestBidderName" to "",
                        "bidDeadlineEpochMs" to 0L,
                        "bidCount" to 0L,
                        "soldCount" to retainedIds.size.toLong(),
                        "unsoldCount" to 0L,
                        "lastEventMessage" to "New Season Mega Auction! Retain up to 4 players + 1 RTM card.",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- LIVE AUCTION: BIDDING, RIGHT TO MATCH (RTM), LOT FINALIZATION & SMART FALLBACK ---

    override suspend fun placeBid(roomCode: String, teamId: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }
                check(uid in room.memberUids) { "Not a member of this room" }
                val player = room.currentCricketer ?: throw IllegalStateException("No active player")
                val team = room.teams.firstOrNull { it.teamId == teamId }
                    ?: throw IllegalStateException("Team not found")
                if (!team.bot && team.ownerUid != uid) {
                    throw IllegalStateException("Cannot bid for another manager's franchise")
                }
                if (team.bot && room.hostUid != uid) {
                    throw IllegalStateException("Only host executes AI bot bids")
                }
                val proposedBid = room.nextBidAmountLakhs()
                val eligibility = SquadRuleValidator.checkBidEligibility(
                    team = team,
                    player = player,
                    proposedBidLakhs = proposedBid,
                    currentHighestBidderTeamId = room.highestBidderTeamId
                )
                if (!eligibility.allowed) throw IllegalStateException(eligibility.reason ?: "Bid not allowed")

                val newDeadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
                tx.update(
                    docRef,
                    mapOf(
                        "currentBidLakhs" to proposedBid,
                        "highestBidderTeamId" to team.teamId,
                        "highestBidderName" to team.teamName.take(50),
                        "bidDeadlineEpochMs" to newDeadline,
                        "bidCount" to (room.bidCount + 1L),
                        "lastEventMessage" to "${team.teamName} bid ${CurrencyFormatter.formatLakhs(proposedBid)} for ${player.name}!".take(240),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Right to Match (RTM) Card: Matches the current highest bid, spends 1 RTM card,
     * immediately awards the player to the RTM team, and advances the auction to the next lot.
     */
    override suspend fun useRtmCard(roomCode: String, teamId: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }
                check(uid in room.memberUids) { "Not a member of this room" }
                val player = room.currentCricketer ?: throw IllegalStateException("No active player")
                val team = room.teams.firstOrNull { it.teamId == teamId }
                    ?: throw IllegalStateException("Team not found")
                if (!team.bot && team.ownerUid != uid) throw IllegalStateException("Not your franchise")
                if (team.bot && room.hostUid != uid) throw IllegalStateException("Only host executes AI RTM")

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

                val nextIdx = room.currentPlayerIndex + 1L
                val allFull = updatedTeams.all { it.squadPlayerIds.size >= SquadRules.MAX_SQUAD_SIZE }
                val reachedEnd = nextIdx >= room.auctionOrder.size || allFull

                if (reachedEnd) {
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
                    tx.update(
                        docRef,
                        mapOf(
                            "status" to RoomStatus.SQUAD_SELECTION.name,
                            "teams" to completedTeams.map { it.toMap() },
                            "currentPlayerIndex" to nextIdx.coerceAtMost(room.auctionOrder.size.toLong()),
                            "currentBidLakhs" to 0L,
                            "highestBidderTeamId" to "",
                            "highestBidderName" to "",
                            "bidDeadlineEpochMs" to 0L,
                            "bidCount" to 0L,
                            "soldCount" to (soldCount + extraSold).coerceAtMost(120L),
                            "unsoldCount" to room.unsoldCount.coerceAtMost(120L),
                            "lastEventMessage" to "$rtmMsg • Auction Complete!".take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val nextPlayer = CricketerDatabase.getById(room.auctionOrder[nextIdx.toInt()])
                    val nextDeadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
                    tx.update(
                        docRef,
                        mapOf(
                            "status" to RoomStatus.AUCTION.name,
                            "teams" to updatedTeams.map { it.toMap() },
                            "currentPlayerIndex" to nextIdx,
                            "currentBidLakhs" to 0L,
                            "highestBidderTeamId" to "",
                            "highestBidderName" to "",
                            "bidDeadlineEpochMs" to nextDeadline,
                            "bidCount" to 0L,
                            "soldCount" to soldCount,
                            "unsoldCount" to room.unsoldCount.coerceAtMost(120L),
                            "lastEventMessage" to "$rtmMsg • Next: ${nextPlayer?.name ?: ""}".take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun finalizeCurrentLot(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                if (room.hostUid != uid || room.roomStatus != RoomStatus.AUCTION) return@runTransaction
                val player = room.currentCricketer ?: return@runTransaction

                // Check if an AI Bot strategically uses its 1 RTM card right at the gavel
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

                val nextIdx = room.currentPlayerIndex + 1L
                val allSquadsFull = updatedTeams.all { it.squadPlayerIds.size >= SquadRules.MAX_SQUAD_SIZE }
                val reachedEnd = nextIdx >= room.auctionOrder.size || allSquadsFull

                if (reachedEnd) {
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

                    tx.update(
                        docRef,
                        mapOf(
                            "status" to RoomStatus.SQUAD_SELECTION.name,
                            "teams" to completedTeams.map { it.toMap() },
                            "currentPlayerIndex" to nextIdx.coerceAtMost(room.auctionOrder.size.toLong()),
                            "currentBidLakhs" to 0L,
                            "highestBidderTeamId" to "",
                            "highestBidderName" to "",
                            "bidDeadlineEpochMs" to 0L,
                            "bidCount" to 0L,
                            "soldCount" to (soldCount + extraSold).coerceAtMost(120L),
                            "unsoldCount" to unsoldCount.coerceAtMost(120L),
                            "lastEventMessage" to "$lotEvent • Smart AI Fallback completed squads! Pick Playing XI.".take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val nextPlayer = CricketerDatabase.getById(room.auctionOrder[nextIdx.toInt()])
                    val nextDeadline = System.currentTimeMillis() + (SquadRules.AUCTION_TIMER_SECONDS * 1000L)
                    tx.update(
                        docRef,
                        mapOf(
                            "teams" to updatedTeams.map { it.toMap() },
                            "currentPlayerIndex" to nextIdx,
                            "currentBidLakhs" to 0L,
                            "highestBidderTeamId" to "",
                            "highestBidderName" to "",
                            "bidDeadlineEpochMs" to nextDeadline,
                            "bidCount" to 0L,
                            "soldCount" to soldCount.coerceAtMost(120L),
                            "unsoldCount" to unsoldCount.coerceAtMost(120L),
                            "lastEventMessage" to "$lotEvent • Next: ${nextPlayer?.name ?: ""}".take(240),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun fastForwardAuction(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can auto-complete auction" }
                check(room.roomStatus == RoomStatus.AUCTION) { "Auction is not active" }

                val alreadyBought = room.teams.flatMap { it.squadPlayerIds }.toSet()
                val remainingPool = CricketerDatabase.allPlayers.filter { it.id !in alreadyBought }
                val (completedTeams, extraSold) = AuctionBotEngine.autoCompleteRemainingAuction(room.teams, remainingPool)

                tx.update(
                    docRef,
                    mapOf(
                        "status" to RoomStatus.SQUAD_SELECTION.name,
                        "teams" to completedTeams.map { it.toMap() },
                        "currentPlayerIndex" to room.auctionOrder.size.toLong(),
                        "currentBidLakhs" to 0L,
                        "highestBidderTeamId" to "",
                        "highestBidderName" to "",
                        "bidDeadlineEpochMs" to 0L,
                        "bidCount" to 0L,
                        "soldCount" to (room.soldCount + extraSold).coerceAtMost(120L),
                        "lastEventMessage" to "Smart AI Fallback built all ${completedTeams.size} squads — select your Playing XI!",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- SQUAD XI & MATCH RECORDS ---

    override suspend fun savePlayingXi(
        roomCode: String,
        teamId: String,
        selectedIds: List<String>,
        captainPlayerId: String
    ): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(uid in room.memberUids) { "Not a member of this room" }
                val team = room.teams.firstOrNull { it.teamId == teamId && it.ownerUid == uid }
                    ?: throw IllegalStateException("Your team was not found")

                val check = SquadRuleValidator.validatePlayingXi(
                    selectedIds = selectedIds,
                    ownedIds = team.squadPlayerIds,
                    captainPlayerId = captainPlayerId
                )
                if (!check.allowed) throw IllegalStateException(check.reason ?: "Invalid Playing XI")

                // Preserve the exact user-defined #1..#11 batting order
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
                val isHost = room.hostUid == uid

                if (isHost && allReady && room.roomStatus == RoomStatus.SQUAD_SELECTION) {
                    tx.update(
                        docRef,
                        mapOf(
                            "status" to RoomStatus.MATCH_HUB.name,
                            "teams" to updatedTeams.map { it.toMap() },
                            "lastEventMessage" to "All Playing XIs locked! Tournament Hub is live.",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        docRef,
                        mapOf(
                            "teams" to updatedTeams.map { it.toMap() },
                            "lastEventMessage" to "${team.teamName} locked their Playing XI & Captain!",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun enterMatchHub(roomCode: String): Result<Unit> {
        val uid = actorId()
        val path = "rooms/$roomCode"
        return try {
            val docRef = db.collection("rooms").document(roomCode)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val room = parseRoomSnapshot(snap) ?: throw IllegalStateException("Room not found")
                check(room.hostUid == uid) { "Only host can open Match Hub" }
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
                tx.update(
                    docRef,
                    mapOf(
                        "status" to RoomStatus.MATCH_HUB.name,
                        "teams" to updatedTeams.map { it.toMap() },
                        "lastEventMessage" to "Championship Match Hub is live!",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordMatchResult(room: AuctionRoom, record: MatchRecord): Result<String> {
        val uid = actorId()
        val docRef = db.collection("rooms").document(room.roomCode).collection("matches").document()
        val path = docRef.path
        return try {
            val payload = mapOf(
                "roomCode" to room.roomCode,
                "hostUid" to uid,
                "memberUids" to room.memberUids,
                "stage" to record.matchStage.name,
                "teamAId" to record.teamAId,
                "teamAName" to record.teamAName.take(50),
                "teamBId" to record.teamBId,
                "teamBName" to record.teamBName.take(50),
                "teamAScore" to record.teamAScore.coerceIn(0L, 500L),
                "teamAWickets" to record.teamAWickets.coerceIn(0L, 10L),
                "teamAOvers" to record.teamAOvers.take(10).ifBlank { "0.0" },
                "teamABalls" to record.teamABalls.coerceIn(0L, 120L),
                "teamBScore" to record.teamBScore.coerceIn(0L, 500L),
                "teamBWickets" to record.teamBWickets.coerceIn(0L, 10L),
                "teamBOvers" to record.teamBOvers.take(10).ifBlank { "0.0" },
                "teamBBalls" to record.teamBBalls.coerceIn(0L, 120L),
                "winnerTeamId" to record.winnerTeamId.take(64).ifBlank { record.teamAId },
                "winnerTeamName" to record.winnerTeamName.take(60).ifBlank { record.teamAName },
                "resultSummary" to record.resultSummary.take(200).ifBlank { "Match completed" },
                "playerOfTheMatch" to record.playerOfTheMatch.take(120).ifBlank { "TBD" },
                "innings1Worm" to record.innings1Worm.take(20),
                "innings2Worm" to record.innings2Worm.take(20),
                "playerPerformances" to record.playerPerformances.take(24).map { it.toMap() },
                "status" to "COMPLETED",
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- SAFE SNAPSHOT PARSER ---

    @Suppress("UNCHECKED_CAST")
    private fun parseRoomSnapshot(doc: DocumentSnapshot): AuctionRoom? {
        val data = doc.data ?: return null
        val rawTeams = data["teams"] as? List<*> ?: emptyList<Any>()
        val teams = rawTeams.mapNotNull { item ->
            (item as? Map<String, Any?>)?.let { TeamState.fromMap(it) }
        }
        val memberUids = (data["memberUids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val auctionOrder = (data["auctionOrder"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

        return AuctionRoom(
            id = doc.id,
            roomCode = data["roomCode"] as? String ?: doc.id,
            hostUid = data["hostUid"] as? String ?: "",
            memberUids = memberUids,
            status = data["status"] as? String ?: RoomStatus.LOBBY.name,
            autoFillEightTeams = data["autoFillEightTeams"] as? Boolean ?: true,
            teams = teams,
            auctionOrder = auctionOrder,
            currentPlayerIndex = (data["currentPlayerIndex"] as? Number)?.toLong() ?: 0L,
            currentBidLakhs = (data["currentBidLakhs"] as? Number)?.toLong() ?: 0L,
            highestBidderTeamId = data["highestBidderTeamId"] as? String ?: "",
            highestBidderName = data["highestBidderName"] as? String ?: "",
            bidDeadlineEpochMs = (data["bidDeadlineEpochMs"] as? Number)?.toLong() ?: 0L,
            bidCount = (data["bidCount"] as? Number)?.toLong() ?: 0L,
            soldCount = (data["soldCount"] as? Number)?.toLong() ?: 0L,
            unsoldCount = (data["unsoldCount"] as? Number)?.toLong() ?: 0L,
            lastEventMessage = data["lastEventMessage"] as? String ?: "",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun generateFourDigitCode(): String =
        Random.nextInt(1000, 10000).toString()
}
