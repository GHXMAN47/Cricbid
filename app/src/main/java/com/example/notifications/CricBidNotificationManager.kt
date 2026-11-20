package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AuctionRoom
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.MatchRecord
import com.example.data.model.RoomStatus
import com.example.engine.TournamentEngine

data class NotificationPreferences(
    val enabled: Boolean = true,
    val auctionTurnAlerts: Boolean = true,
    val matchStartAlerts: Boolean = true
)

enum class LocalAlertKind {
    AUCTION_TURN,
    MATCH_START
}

data class LocalAlertPayload(
    val key: String,
    val kind: LocalAlertKind,
    val title: String,
    val body: String
)

/**
 * Pure Kotlin evaluator determining when to fire local alerts for:
 * 1) User's auction turn starting (new lot open, outbid counter-turn, or marquee retention turn).
 * 2) A match involving the user's team being ready / about to begin.
 */
object NotificationAlertEvaluator {

    fun evaluateAuctionTurnAlert(
        room: AuctionRoom,
        currentUserId: String,
        previousHighestBidderTeamId: String?,
        previousLotId: String?
    ): LocalAlertPayload? {
        val myTeam = room.teamForUser(currentUserId) ?: return null

        // 1. Marquee Retention Turn
        if (room.roomStatus == RoomStatus.RETENTION && !myTeam.retentionDone) {
            return LocalAlertPayload(
                key = "retention_${room.roomCode}_${myTeam.teamId}",
                kind = LocalAlertKind.AUCTION_TURN,
                title = "Marquee Retention Turn • Room ${room.roomCode}",
                body = "${myTeam.teamName}: Pick 1 marquee star or skip before the live auction begins!"
            )
        }

        // 2. Live Auction Turn (new lot or outbid)
        if (room.roomStatus == RoomStatus.AUCTION) {
            val player = room.currentCricketer ?: return null
            val lotId = player.id
            val sameLot = lotId == previousLotId
            val wasHighest = previousHighestBidderTeamId == myTeam.teamId
            val isNowOtherHighest = room.highestBidderTeamId.isNotBlank() && room.highestBidderTeamId != myTeam.teamId

            if (sameLot && wasHighest && isNowOtherHighest) {
                val rivalName = room.highestBidderName.ifBlank { "A rival manager" }
                return LocalAlertPayload(
                    key = "outbid_${room.roomCode}_${lotId}_${room.currentBidLakhs}_${room.highestBidderTeamId}",
                    kind = LocalAlertKind.AUCTION_TURN,
                    title = "You Were Outbid on ${player.name}!",
                    body = "$rivalName bid ${CurrencyFormatter.formatLakhs(room.currentBidLakhs)} — your turn to bid now!"
                )
            }

            if (!sameLot && room.highestBidderTeamId != myTeam.teamId) {
                val priceLabel = if (room.currentBidLakhs > 0L) {
                    CurrencyFormatter.formatLakhs(room.currentBidLakhs)
                } else {
                    CurrencyFormatter.formatLakhs(player.basePriceLakhs)
                }
                return LocalAlertPayload(
                    key = "lot_${room.roomCode}_$lotId",
                    kind = LocalAlertKind.AUCTION_TURN,
                    title = "Auction Turn: ${player.name} On The Block",
                    body = "${player.role.displayName} (${player.countryCode}) • $priceLabel • Bid before the 10s hammer!"
                )
            }
        }

        return null
    }

    fun evaluateUpcomingMatchAlert(
        room: AuctionRoom,
        matches: List<MatchRecord>,
        currentUserId: String
    ): LocalAlertPayload? {
        if (room.roomStatus != RoomStatus.MATCH_HUB) return null
        val myTeam = room.teamForUser(currentUserId) ?: return null
        val overview = TournamentEngine.buildOverview(room.teams, matches)

        // 1. 2-Team Series Mode
        if (overview.isSeriesMode) {
            val summary = overview.seriesSummary ?: return null
            if (summary.seriesCompleted) return null
            val opponent = if (summary.teamA.teamId == myTeam.teamId) summary.teamB.teamName else summary.teamA.teamName
            return LocalAlertPayload(
                key = "upcoming_series_${room.roomCode}_${summary.nextMatchNumber}",
                kind = LocalAlertKind.MATCH_START,
                title = "Match About to Begin: ${myTeam.teamName} vs $opponent",
                body = "T20 Series Match #${summary.nextMatchNumber} is ready for toss & first ball!"
            )
        }

        // 2. Playoff Slot involving user
        val myPlayoff = overview.playoffSlots.firstOrNull { slot ->
            slot.isPlayable && (slot.teamA?.teamId == myTeam.teamId || slot.teamB?.teamId == myTeam.teamId)
        }
        if (myPlayoff != null) {
            val oppName = if (myPlayoff.teamA?.teamId == myTeam.teamId) {
                myPlayoff.teamB?.teamName ?: "Rival"
            } else {
                myPlayoff.teamA?.teamName ?: "Rival"
            }
            return LocalAlertPayload(
                key = "upcoming_playoff_${room.roomCode}_${myPlayoff.stage.name}_${matches.size}",
                kind = LocalAlertKind.MATCH_START,
                title = "Match About to Begin: ${myTeam.teamName} vs $oppName",
                body = "${myPlayoff.title} (${myPlayoff.stage.label}) is ready for toss & first ball!"
            )
        }

        // 3. Next Unplayed League Fixture involving user
        val myLeagueFixture = overview.unplayedLeagueFixtures.firstOrNull { (a, b) ->
            a.teamId == myTeam.teamId || b.teamId == myTeam.teamId
        } ?: return null
        val opponent = if (myLeagueFixture.first.teamId == myTeam.teamId) {
            myLeagueFixture.second.teamName
        } else {
            myLeagueFixture.first.teamName
        }
        val fixtureKey = "upcoming_league_${room.roomCode}_${myLeagueFixture.first.teamId}_${myLeagueFixture.second.teamId}_${matches.size}"
        return LocalAlertPayload(
            key = fixtureKey,
            kind = LocalAlertKind.MATCH_START,
            title = "Match About to Begin: ${myTeam.teamName} vs $opponent",
            body = "IPL League fixture is ready for toss & first ball!"
        )
    }

    fun buildMatchKickoffAlert(
        teamAName: String,
        teamBName: String,
        stageLabel: String,
        formatLabel: String,
        uniqueKey: String
    ): LocalAlertPayload = LocalAlertPayload(
        key = "kickoff_$uniqueKey",
        kind = LocalAlertKind.MATCH_START,
        title = "Match Starting: $teamAName vs $teamBName",
        body = "$formatLabel • $stageLabel is underway — tap to jump to the live pitch!"
    )
}

/**
 * Lightweight Android Local Notification manager for CricBid.
 * Handles Android 8+ NotificationChannels, Android 13+ POST_NOTIFICATIONS permission checks,
 * preference persistence, and duplicate-alert suppression.
 */
class CricBidNotificationManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firedKeys = LinkedHashSet<String>()

    init {
        ensureChannels()
    }

    fun loadPreferences(): NotificationPreferences = NotificationPreferences(
        enabled = prefs.getBoolean(KEY_ENABLED, true),
        auctionTurnAlerts = prefs.getBoolean(KEY_AUCTION_ALERTS, true),
        matchStartAlerts = prefs.getBoolean(KEY_MATCH_ALERTS, true)
    )

    fun savePreferences(updated: NotificationPreferences) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, updated.enabled)
            .putBoolean(KEY_AUCTION_ALERTS, updated.auctionTurnAlerts)
            .putBoolean(KEY_MATCH_ALERTS, updated.matchStartAlerts)
            .apply()
    }

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val auctionChannel = NotificationChannel(
            CHANNEL_AUCTION,
            appContext.getString(R.string.notif_channel_auction_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.notif_channel_auction_desc)
            enableVibration(true)
        }

        val matchChannel = NotificationChannel(
            CHANNEL_MATCH,
            appContext.getString(R.string.notif_channel_match_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = appContext.getString(R.string.notif_channel_match_desc)
            enableVibration(true)
        }

        manager.createNotificationChannel(auctionChannel)
        manager.createNotificationChannel(matchChannel)
    }

    /**
     * Posts a local system notification if enabled and permitted, deduplicating by payload.key.
     * Returns true if a new notification was posted.
     */
    fun postAlert(payload: LocalAlertPayload, forceAllowDuplicate: Boolean = false): Boolean {
        val currentPrefs = loadPreferences()
        if (!currentPrefs.enabled) return false
        when (payload.kind) {
            LocalAlertKind.AUCTION_TURN -> if (!currentPrefs.auctionTurnAlerts) return false
            LocalAlertKind.MATCH_START -> if (!currentPrefs.matchStartAlerts) return false
        }

        if (!forceAllowDuplicate && !rememberKey(payload.key)) return false
        if (!hasNotificationPermission()) return false

        ensureChannels()
        val channelId = when (payload.kind) {
            LocalAlertKind.AUCTION_TURN -> CHANNEL_AUCTION
            LocalAlertKind.MATCH_START -> CHANNEL_MATCH
        }
        val notificationId = when (payload.kind) {
            LocalAlertKind.AUCTION_TURN -> NOTIF_ID_AUCTION
            LocalAlertKind.MATCH_START -> NOTIF_ID_MATCH
        }

        val tapIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(appContext, notificationId, tapIntent, pendingFlags)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendTestAlert(): Boolean {
        val payload = LocalAlertPayload(
            key = "test_${System.currentTimeMillis()}",
            kind = LocalAlertKind.AUCTION_TURN,
            title = "CricBid Alerts Active!",
            body = "You'll be notified when your auction turn starts or a match is about to begin."
        )
        return postAlert(payload, forceAllowDuplicate = true)
    }

    private fun rememberKey(key: String): Boolean {
        if (key in firedKeys) return false
        firedKeys.add(key)
        if (firedKeys.size > 40) {
            val iterator = firedKeys.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
        return true
    }

    companion object {
        const val CHANNEL_AUCTION = "cricbid_auction_turns"
        const val CHANNEL_MATCH = "cricbid_match_alerts"
        private const val NOTIF_ID_AUCTION = 4001
        private const val NOTIF_ID_MATCH = 4002
        private const val PREFS_NAME = "cricbid_notification_prefs"
        private const val KEY_ENABLED = "alerts_enabled"
        private const val KEY_AUCTION_ALERTS = "auction_turn_alerts"
        private const val KEY_MATCH_ALERTS = "match_start_alerts"
    }
}
