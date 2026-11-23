package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.IplAuctionLiveState
import com.example.data.model.IplFranchiseState
import com.example.data.model.IplPlayMode
import com.example.data.model.IplScheduledMatch
import com.example.data.model.IplSeasonPhase
import com.example.data.model.IplSeasonState
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.engine.MatchPlayerPerf
import org.json.JSONArray
import org.json.JSONObject

/**
 * Crash-safe local SharedPreferences persistence for the complete IPL 2026 Season
 * (franchises, ₹120 Cr purses, ₹7.5L match fees, 2/2 Hammer Loots, retentions/RTMs,
 * 18–25 squads, Playing XI, 5 Impact Nominees, 70-match schedule, playoffs & leaderboards).
 */
class LocalIplSeasonStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSeason(state: IplSeasonState) {
        try {
            val json = serializeSeason(state).toString()
            prefs.edit().putString(KEY_SEASON_JSON, json).apply()
        } catch (_: Throwable) {
            // Never crash UI on storage write edge cases
        }
    }

    fun loadSeason(): IplSeasonState? {
        val raw = prefs.getString(KEY_SEASON_JSON, null) ?: return null
        return try {
            deserializeSeason(JSONObject(raw))
        } catch (_: Throwable) {
            null
        }
    }

    fun clearSeason() {
        prefs.edit().remove(KEY_SEASON_JSON).apply()
    }

    private fun serializeSeason(s: IplSeasonState): JSONObject = JSONObject().apply {
        put("seasonNumber", s.seasonNumber)
        put("seasonYear", s.seasonYear)
        put("playMode", s.playMode.name)
        put("roomCode", s.roomCode)
        put("phase", s.phase.name)
        put("activeHumanFranchiseId", s.activeHumanFranchiseId)
        put("humanFranchiseIds", JSONArray(s.humanFranchiseIds))
        put("championFranchiseId", s.championFranchiseId)
        put("championTeamName", s.championTeamName)
        put("runnerUpTeamName", s.runnerUpTeamName)
        put("trophyRecorded", s.trophyRecorded)

        val fArr = JSONArray()
        s.franchises.forEach { f ->
            fArr.put(JSONObject().apply {
                put("franchiseId", f.franchiseId)
                put("shortCode", f.shortCode)
                put("teamName", f.teamName)
                put("defendingChampion", f.defendingChampion)
                put("primaryColorHex", f.primaryColorHex)
                put("accentColorHex", f.accentColorHex)
                put("isHuman", f.isHuman)
                put("ownerName", f.ownerName)
                put("purseRemainingLakhs", f.purseRemainingLakhs)
                put("matchFeesSpentLakhs", f.matchFeesSpentLakhs)
                put("matchesPaidCount", f.matchesPaidCount)
                put("hammerLootRemaining", f.hammerLootRemaining)
                put("retainedPlayerIds", JSONArray(f.retainedPlayerIds))
                put("rtmUsedPlayerIds", JSONArray(f.rtmUsedPlayerIds))
                put("formerSquadIds", JSONArray(f.formerSquadIds))
                put("retentionDone", f.retentionDone)
                put("squadPlayerIds", JSONArray(f.squadPlayerIds))
                put("squadPrices", JSONObject().apply { f.squadPrices.forEach { (k, v) -> put(k, v) } })
                put("playingXiIds", JSONArray(f.playingXiIds))
                put("impactNomineeIds", JSONArray(f.impactNomineeIds))
                put("captainPlayerId", f.captainPlayerId)
            })
        }
        put("franchises", fArr)

        put("auction", JSONObject().apply {
            put("auctionOrder", JSONArray(s.auction.auctionOrder))
            put("currentIndex", s.auction.currentIndex)
            put("currentBidLakhs", s.auction.currentBidLakhs)
            put("highestBidderFranchiseId", s.auction.highestBidderFranchiseId)
            put("highestBidderName", s.auction.highestBidderName)
            put("soldCount", s.auction.soldCount)
            put("unsoldCount", s.auction.unsoldCount)
            put("passedFranchiseIds", JSONArray(s.auction.passedFranchiseIds.toList()))
            put("lastEventMessage", s.auction.lastEventMessage)
        })

        put("schedule", serializeFixtures(s.schedule))
        put("playoffs", serializeFixtures(s.playoffs))
    }

    private fun serializeFixtures(list: List<IplScheduledMatch>): JSONArray {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("matchNumber", m.matchNumber)
                put("matchday", m.matchday)
                put("stage", m.stage.name)
                put("teamAId", m.teamAId)
                put("teamBId", m.teamBId)
                put("venue", m.venue)
                m.completedRecord?.let { put("completedRecord", serializeMatchRecord(it)) }
            })
        }
        return arr
    }

    private fun serializeMatchRecord(r: MatchRecord): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("roomCode", r.roomCode)
        put("stage", r.stage)
        put("teamAId", r.teamAId)
        put("teamAName", r.teamAName)
        put("teamBId", r.teamBId)
        put("teamBName", r.teamBName)
        put("teamAScore", r.teamAScore)
        put("teamAWickets", r.teamAWickets)
        put("teamAOvers", r.teamAOvers)
        put("teamABalls", r.teamABalls)
        put("teamBScore", r.teamBScore)
        put("teamBWickets", r.teamBWickets)
        put("teamBOvers", r.teamBOvers)
        put("teamBBalls", r.teamBBalls)
        put("winnerTeamId", r.winnerTeamId)
        put("winnerTeamName", r.winnerTeamName)
        put("resultSummary", r.resultSummary)
        put("playerOfTheMatch", r.playerOfTheMatch)
        val perfArr = JSONArray()
        r.playerPerformances.forEach { p ->
            perfArr.put(JSONObject().apply {
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
            })
        }
        put("playerPerformances", perfArr)
    }

    private fun deserializeSeason(o: JSONObject): IplSeasonState {
        val fArr = o.optJSONArray("franchises") ?: JSONArray()
        val franchises = (0 until fArr.length()).map { idx ->
            val f = fArr.getJSONObject(idx)
            val pricesObj = f.optJSONObject("squadPrices") ?: JSONObject()
            val priceMap = linkedMapOf<String, Long>()
            val keys = pricesObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                priceMap[k] = pricesObj.optLong(k, 30L)
            }
            IplFranchiseState(
                franchiseId = f.optString("franchiseId"),
                shortCode = f.optString("shortCode"),
                teamName = f.optString("teamName"),
                defendingChampion = f.optBoolean("defendingChampion", false),
                primaryColorHex = f.optLong("primaryColorHex", 0xFF00C982L),
                accentColorHex = f.optLong("accentColorHex", 0xFFFFC107L),
                isHuman = f.optBoolean("isHuman", false),
                ownerName = f.optString("ownerName", "AI Franchise"),
                purseRemainingLakhs = f.optLong("purseRemainingLakhs", 12_000L),
                matchFeesSpentLakhs = f.optDouble("matchFeesSpentLakhs", 0.0),
                matchesPaidCount = f.optInt("matchesPaidCount", 0),
                hammerLootRemaining = f.optInt("hammerLootRemaining", 2),
                retainedPlayerIds = readStringList(f.optJSONArray("retainedPlayerIds")),
                rtmUsedPlayerIds = readStringList(f.optJSONArray("rtmUsedPlayerIds")),
                formerSquadIds = readStringList(f.optJSONArray("formerSquadIds")),
                retentionDone = f.optBoolean("retentionDone", false),
                squadPlayerIds = readStringList(f.optJSONArray("squadPlayerIds")),
                squadPrices = priceMap,
                playingXiIds = readStringList(f.optJSONArray("playingXiIds")),
                impactNomineeIds = readStringList(f.optJSONArray("impactNomineeIds")),
                captainPlayerId = f.optString("captainPlayerId")
            )
        }

        val aObj = o.optJSONObject("auction") ?: JSONObject()
        val auction = IplAuctionLiveState(
            auctionOrder = readStringList(aObj.optJSONArray("auctionOrder")),
            currentIndex = aObj.optInt("currentIndex", 0),
            currentBidLakhs = aObj.optLong("currentBidLakhs", 0L),
            highestBidderFranchiseId = aObj.optString("highestBidderFranchiseId"),
            highestBidderName = aObj.optString("highestBidderName"),
            soldCount = aObj.optInt("soldCount", 0),
            unsoldCount = aObj.optInt("unsoldCount", 0),
            passedFranchiseIds = readStringList(aObj.optJSONArray("passedFranchiseIds")).toSet(),
            lastEventMessage = aObj.optString("lastEventMessage")
        )

        return IplSeasonState(
            seasonNumber = o.optInt("seasonNumber", 1),
            seasonYear = o.optInt("seasonYear", 2026),
            playMode = IplPlayMode.fromNameSafe(o.optString("playMode")),
            roomCode = o.optString("roomCode"),
            phase = IplSeasonPhase.fromNameSafe(o.optString("phase")),
            activeHumanFranchiseId = o.optString("activeHumanFranchiseId", "ipl_rcb"),
            humanFranchiseIds = readStringList(o.optJSONArray("humanFranchiseIds")),
            franchises = franchises,
            auction = auction,
            schedule = deserializeFixtures(o.optJSONArray("schedule")),
            playoffs = deserializeFixtures(o.optJSONArray("playoffs")),
            championFranchiseId = o.optString("championFranchiseId"),
            championTeamName = o.optString("championTeamName"),
            runnerUpTeamName = o.optString("runnerUpTeamName"),
            trophyRecorded = o.optBoolean("trophyRecorded", false)
        )
    }

    private fun deserializeFixtures(arr: JSONArray?): List<IplScheduledMatch> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { idx ->
            val m = arr.getJSONObject(idx)
            val recObj = m.optJSONObject("completedRecord")
            IplScheduledMatch(
                matchNumber = m.optInt("matchNumber", idx + 1),
                matchday = m.optInt("matchday", 1),
                stage = MatchStage.fromNameSafe(m.optString("stage")),
                teamAId = m.optString("teamAId"),
                teamBId = m.optString("teamBId"),
                venue = m.optString("venue"),
                completedRecord = recObj?.let { deserializeMatchRecord(it) }
            )
        }
    }

    private fun deserializeMatchRecord(r: JSONObject): MatchRecord {
        val perfArr = r.optJSONArray("playerPerformances") ?: JSONArray()
        val perfs = (0 until perfArr.length()).map { idx ->
            val p = perfArr.getJSONObject(idx)
            MatchPlayerPerf(
                playerId = p.optString("playerId"),
                playerName = p.optString("playerName"),
                teamName = p.optString("teamName"),
                runs = p.optInt("runs", 0),
                ballsFaced = p.optInt("ballsFaced", 0),
                fours = p.optInt("fours", 0),
                sixes = p.optInt("sixes", 0),
                wickets = p.optInt("wickets", 0),
                runsConceded = p.optInt("runsConceded", 0),
                legalBallsBowled = p.optInt("legalBallsBowled", 0)
            )
        }
        return MatchRecord(
            id = r.optString("id"),
            roomCode = r.optString("roomCode"),
            stage = r.optString("stage", MatchStage.LEAGUE.name),
            teamAId = r.optString("teamAId"),
            teamAName = r.optString("teamAName"),
            teamBId = r.optString("teamBId"),
            teamBName = r.optString("teamBName"),
            teamAScore = r.optLong("teamAScore", 0L),
            teamAWickets = r.optLong("teamAWickets", 0L),
            teamAOvers = r.optString("teamAOvers", "0.0"),
            teamABalls = r.optLong("teamABalls", 120L),
            teamBScore = r.optLong("teamBScore", 0L),
            teamBWickets = r.optLong("teamBWickets", 0L),
            teamBOvers = r.optString("teamBOvers", "0.0"),
            teamBBalls = r.optLong("teamBBalls", 120L),
            winnerTeamId = r.optString("winnerTeamId"),
            winnerTeamName = r.optString("winnerTeamName"),
            resultSummary = r.optString("resultSummary"),
            playerOfTheMatch = r.optString("playerOfTheMatch"),
            playerPerformances = perfs,
            status = "COMPLETED"
        )
    }

    private fun readStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }

    companion object {
        private const val PREFS_NAME = "cricbid_ipl_2026_season_store"
        private const val KEY_SEASON_JSON = "ipl_season_state_v1"
    }
}
