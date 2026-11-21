package com.example.data.local

import android.content.Context
import com.example.engine.TrophyCabinetData
import com.example.engine.TrophyCabinetRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent local store for the Trophy Cabinet: tracks every championship won,
 * finals played, and season awards (Orange Cap, Purple Cap, Season MVP) across restarts.
 */
class LocalTrophyCabinetStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCabinet(): TrophyCabinetData {
        return runCatching {
            val raw = prefs.getString(KEY_CABINET_JSON, null) ?: return TrophyCabinetData()
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray("trophies") ?: JSONArray()
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    add(
                        TrophyCabinetRecord(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            competitionType = item.optString("competitionType", "IPL"),
                            championTeamName = item.optString("championTeamName"),
                            runnerUpTeamName = item.optString("runnerUpTeamName"),
                            userWon = item.optBoolean("userWon", false),
                            summary = item.optString("summary"),
                            orangeCapWinner = item.optString("orangeCapWinner"),
                            purpleCapWinner = item.optString("purpleCapWinner"),
                            mvpWinner = item.optString("mvpWinner"),
                            timestampMs = item.optLong("timestampMs", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.timestampMs }
            TrophyCabinetData(
                trophies = list,
                finalsPlayed = obj.optInt("finalsPlayed", list.size),
                championshipsWon = obj.optInt("championshipsWon", list.count { it.userWon })
            )
        }.getOrDefault(TrophyCabinetData())
    }

    fun recordFinalOutcome(record: TrophyCabinetRecord): TrophyCabinetData {
        val current = loadCabinet()
        if (current.trophies.any { it.id == record.id }) return current
        val updatedList = (listOf(record) + current.trophies).take(40)
        val updated = TrophyCabinetData(
            trophies = updatedList,
            finalsPlayed = current.finalsPlayed + 1,
            championshipsWon = current.championshipsWon + if (record.userWon) 1 else 0
        )
        saveCabinet(updated)
        return updated
    }

    fun saveLastSeasonSquad(teamName: String, playerIds: List<String>) {
        if (teamName.isBlank() || playerIds.isEmpty()) return
        runCatching {
            val raw = prefs.getString(KEY_PREV_SQUADS_JSON, null)
            val obj = if (raw.isNullOrBlank()) JSONObject() else JSONObject(raw)
            val arr = JSONArray()
            playerIds.distinct().take(15).forEach { arr.put(it) }
            obj.put(teamName.trim().lowercase(), arr)
            prefs.edit().putString(KEY_PREV_SQUADS_JSON, obj.toString()).apply()
        }
    }

    fun loadLastSeasonSquad(teamName: String): List<String> {
        if (teamName.isBlank()) return emptyList()
        return runCatching {
            val raw = prefs.getString(KEY_PREV_SQUADS_JSON, null) ?: return emptyList()
            val obj = JSONObject(raw)
            val arr = obj.optJSONArray(teamName.trim().lowercase()) ?: return emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) add(id)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun exportCabinetJson(): JSONObject {
        val data = loadCabinet()
        val arr = JSONArray()
        data.trophies.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("competitionType", t.competitionType)
                    put("championTeamName", t.championTeamName)
                    put("runnerUpTeamName", t.runnerUpTeamName)
                    put("userWon", t.userWon)
                    put("summary", t.summary)
                    put("orangeCapWinner", t.orangeCapWinner)
                    put("purpleCapWinner", t.purpleCapWinner)
                    put("mvpWinner", t.mvpWinner)
                    put("timestampMs", t.timestampMs)
                }
            )
        }
        return JSONObject().apply {
            put("finalsPlayed", data.finalsPlayed)
            put("championshipsWon", data.championshipsWon)
            put("trophies", arr)
        }
    }

    fun importCabinetJson(obj: JSONObject?): TrophyCabinetData {
        if (obj == null) return loadCabinet()
        return runCatching {
            val arr = obj.optJSONArray("trophies") ?: JSONArray()
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    add(
                        TrophyCabinetRecord(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            competitionType = item.optString("competitionType", "IPL"),
                            championTeamName = item.optString("championTeamName"),
                            runnerUpTeamName = item.optString("runnerUpTeamName"),
                            userWon = item.optBoolean("userWon", false),
                            summary = item.optString("summary"),
                            orangeCapWinner = item.optString("orangeCapWinner"),
                            purpleCapWinner = item.optString("purpleCapWinner"),
                            mvpWinner = item.optString("mvpWinner"),
                            timestampMs = item.optLong("timestampMs", 0L)
                        )
                    )
                }
            }.sortedByDescending { it.timestampMs }
            val restored = TrophyCabinetData(
                trophies = list,
                finalsPlayed = obj.optInt("finalsPlayed", list.size),
                championshipsWon = obj.optInt("championshipsWon", list.count { it.userWon })
            )
            saveCabinet(restored)
            restored
        }.getOrDefault(loadCabinet())
    }

    private fun saveCabinet(data: TrophyCabinetData) {
        runCatching {
            val arr = JSONArray()
            data.trophies.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("competitionType", t.competitionType)
                        put("championTeamName", t.championTeamName)
                        put("runnerUpTeamName", t.runnerUpTeamName)
                        put("userWon", t.userWon)
                        put("summary", t.summary)
                        put("orangeCapWinner", t.orangeCapWinner)
                        put("purpleCapWinner", t.purpleCapWinner)
                        put("mvpWinner", t.mvpWinner)
                        put("timestampMs", t.timestampMs)
                    }
                )
            }
            val root = JSONObject().apply {
                put("finalsPlayed", data.finalsPlayed)
                put("championshipsWon", data.championshipsWon)
                put("trophies", arr)
            }
            prefs.edit().putString(KEY_CABINET_JSON, root.toString()).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "cricbid_trophy_cabinet_v1"
        private const val KEY_CABINET_JSON = "cabinet_json"
        private const val KEY_PREV_SQUADS_JSON = "previous_season_squads_json"
    }
}
