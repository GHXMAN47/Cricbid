package com.example.data.local

import android.content.Context
import com.example.data.model.InternationalCampaignState
import com.example.data.model.InternationalMode
import com.example.data.model.InternationalNations
import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight local JSON persistence for International cricket campaigns (per format: T20I, ODI, Test).
 */
class LocalInternationalStore(context: Context) {

    private val prefs = context.getSharedPreferences("cricbid_international_v1", Context.MODE_PRIVATE)

    fun loadActiveFormat(): MatchFormat =
        MatchFormat.fromNameSafe(prefs.getString("active_format", MatchFormat.T20I.name))

    fun saveActiveFormat(format: MatchFormat) {
        prefs.edit().putString("active_format", format.name).apply()
    }

    fun loadCampaign(format: MatchFormat): InternationalCampaignState {
        val raw = prefs.getString(keyFor(format), null)
        if (raw.isNullOrBlank()) {
            return InternationalCampaignState(format = format)
        }
        return runCatching {
            val o = JSONObject(raw)
            val userId = o.optString("userNationId", "nat_ind")
            val oppId = o.optString("opponentNationId", "nat_aus").let {
                if (it == userId) InternationalNations.all.first { n -> n.id != userId }.id else it
            }
            val totalMatches = o.optInt("seriesTotalMatches", 3).let {
                if (it in InternationalNations.seriesLengthOptions) it else 3
            }
            InternationalCampaignState(
                format = format,
                mode = InternationalMode.fromNameSafe(o.optString("mode", InternationalMode.SERIES.name)),
                userNationId = userId,
                opponentNationId = oppId,
                seriesTotalMatches = totalMatches,
                seriesMatches = parseMatches(o.optJSONArray("seriesMatches")),
                worldCupMatches = parseMatches(o.optJSONArray("worldCupMatches"))
            )
        }.getOrElse {
            InternationalCampaignState(format = format)
        }
    }

    fun saveCampaign(state: InternationalCampaignState) {
        val obj = JSONObject().apply {
            put("format", state.format.name)
            put("mode", state.mode.name)
            put("userNationId", state.userNationId)
            put("opponentNationId", state.opponentNationId)
            put("seriesTotalMatches", state.seriesTotalMatches)
            put("seriesMatches", serializeMatches(state.seriesMatches))
            put("worldCupMatches", serializeMatches(state.worldCupMatches))
        }
        prefs.edit()
            .putString("active_format", state.format.name)
            .putString(keyFor(state.format), obj.toString())
            .apply()
    }

    private fun keyFor(format: MatchFormat): String = "campaign_${format.name}"

    private fun serializeMatches(matches: List<MatchRecord>): JSONArray {
        val arr = JSONArray()
        matches.takeLast(45).forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("stage", m.stage)
                    put("teamAId", m.teamAId)
                    put("teamAName", m.teamAName)
                    put("teamBId", m.teamBId)
                    put("teamBName", m.teamBName)
                    put("teamAScore", m.teamAScore)
                    put("teamAWickets", m.teamAWickets)
                    put("teamAOvers", m.teamAOvers)
                    put("teamABalls", m.teamABalls)
                    put("teamBScore", m.teamBScore)
                    put("teamBWickets", m.teamBWickets)
                    put("teamBOvers", m.teamBOvers)
                    put("teamBBalls", m.teamBBalls)
                    put("winnerTeamId", m.winnerTeamId)
                    put("winnerTeamName", m.winnerTeamName)
                    put("resultSummary", m.resultSummary)
                    put("playerOfTheMatch", m.playerOfTheMatch)
                }
            )
        }
        return arr
    }

    private fun parseMatches(arr: JSONArray?): List<MatchRecord> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MatchRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                MatchRecord(
                    id = o.optString("id"),
                    roomCode = "INTL",
                    stage = o.optString("stage", "LEAGUE"),
                    teamAId = o.optString("teamAId"),
                    teamAName = o.optString("teamAName"),
                    teamBId = o.optString("teamBId"),
                    teamBName = o.optString("teamBName"),
                    teamAScore = o.optLong("teamAScore"),
                    teamAWickets = o.optLong("teamAWickets"),
                    teamAOvers = o.optString("teamAOvers"),
                    teamABalls = o.optLong("teamABalls", 120L),
                    teamBScore = o.optLong("teamBScore"),
                    teamBWickets = o.optLong("teamBWickets"),
                    teamBOvers = o.optString("teamBOvers"),
                    teamBBalls = o.optLong("teamBBalls", 120L),
                    winnerTeamId = o.optString("winnerTeamId"),
                    winnerTeamName = o.optString("winnerTeamName"),
                    resultSummary = o.optString("resultSummary"),
                    playerOfTheMatch = o.optString("playerOfTheMatch")
                )
            )
        }
        return list
    }
}
