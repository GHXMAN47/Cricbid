package com.example.data

import com.example.data.model.AuctionRoom
import com.example.data.model.MatchRecord
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Unified backend contract implemented by both LocalAuctionRepository (instant username-only play)
 * and AuctionRepository (optional Google-authenticated Firebase Firestore real-time cloud sync).
 */
interface AuctionDataSource {
    suspend fun ensureUserProfile(displayName: String): Result<UserProfile>
    fun observeMyRooms(uid: String): Flow<List<AuctionRoom>>
    fun observeRoom(roomCode: String): Flow<AuctionRoom?>
    fun observeRoomMatches(roomCode: String, uid: String): Flow<List<MatchRecord>>
    suspend fun createRoom(teamNameRaw: String, autoFillEightTeams: Boolean = true): Result<String>
    suspend fun joinRoom(roomCodeRaw: String, teamNameRaw: String): Result<String>
    suspend fun toggleAutoFillEightTeams(roomCode: String, enabled: Boolean): Result<Unit>
    suspend fun addAiBot(roomCode: String): Result<Unit>
    suspend fun removeAiBot(roomCode: String, botTeamId: String): Result<Unit>
    suspend fun enterRetentionRound(roomCode: String): Result<Unit>
    suspend fun retainOrSkipMarquee(roomCode: String, teamId: String, playerId: String?): Result<Unit>
    suspend fun beginLiveAuctionFromRetention(roomCode: String): Result<Unit>
    suspend fun placeBid(roomCode: String, teamId: String): Result<Unit>
    suspend fun useRtmCard(roomCode: String, teamId: String): Result<Unit>
    suspend fun finalizeCurrentLot(roomCode: String): Result<Unit>
    suspend fun fastForwardAuction(roomCode: String): Result<Unit>
    suspend fun savePlayingXi(
        roomCode: String,
        teamId: String,
        selectedIds: List<String>,
        captainPlayerId: String = ""
    ): Result<Unit>
    suspend fun enterMatchHub(roomCode: String): Result<Unit>
    suspend fun recordMatchResult(room: AuctionRoom, record: MatchRecord): Result<String>
}
