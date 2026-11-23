package com.example.data.local

import android.content.Context
import com.example.data.model.CareerMatchType
import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.MatchFormat
import com.example.data.model.MatchStage
import com.example.data.model.PlayerRole
import com.example.data.model.TeamState
import com.example.engine.BallEvent
import com.example.engine.BallOutcomeType
import com.example.engine.BatterScoreEntry
import com.example.engine.BowlerScoreEntry
import com.example.engine.FallOfWicket
import com.example.engine.CareerControlMode
import com.example.engine.FieldPreset
import com.example.engine.InningsState
import com.example.engine.InteractivePlayFilter
import com.example.engine.LiveMatchState
import com.example.engine.PitchMapPoint
import com.example.engine.PitchType
import com.example.engine.ShotTelemetry
import com.example.engine.WeatherCondition
import org.json.JSONArray
import org.json.JSONObject

enum class SavedMatchSource {
    IPL_ROOM,
    IPL_SEASON,
    INTERNATIONAL,
    CAREER;

    companion object {
        fun fromNameSafe(raw: String?): SavedMatchSource =
            entries.firstOrNull { it.name == raw } ?: IPL_ROOM
    }
}

data class SavedMatchResumeSnapshot(
    val match: LiveMatchState,
    val userTeamId: String,
    val playFilter: InteractivePlayFilter,
    val source: SavedMatchSource,
    val roomCode: String? = null,
    val careerMatchType: CareerMatchType? = null,
    val careerPlayerId: String? = null,
    val careerPlayerName: String? = null,
    val careerControlMode: CareerControlMode = CareerControlMode.FULL_TEAM,
    val savedAtMs: Long = System.currentTimeMillis()
) {
    val headline: String
        get() {
            val inn = match.activeInnings
            return "${inn.battingTeamName} ${inn.totalRuns}/${inn.wickets} (${inn.oversFormatted})"
        }

    val subtitle: String
        get() = "${match.teamA.teamName} vs ${match.teamB.teamName} • ${match.format.shortLabel} • ${match.pitchType.shortLabel}"
}

/**
 * Lightweight local auto-save store for mid-match state and last active auction room code.
 * Uses SharedPreferences JSON with strict try-catch guards so corrupted/old payloads never crash startup.
 */
class LocalMatchResumeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLastActiveRoomCode(roomCode: String?) {
        runCatching {
            if (roomCode.isNullOrBlank()) {
                prefs.edit().remove(KEY_LAST_ROOM_CODE).apply()
            } else {
                prefs.edit().putString(KEY_LAST_ROOM_CODE, roomCode).apply()
            }
        }
    }

    fun loadLastActiveRoomCode(): String? = runCatching {
        prefs.getString(KEY_LAST_ROOM_CODE, null)?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun saveOngoingMatch(snapshot: SavedMatchResumeSnapshot) {
        if (snapshot.match.finished) {
            clearOngoingMatch()
            return
        }
        runCatching {
            val root = JSONObject().apply {
                put("userTeamId", snapshot.userTeamId)
                put("playFilter", snapshot.playFilter.name)
                put("source", snapshot.source.name)
                put("roomCode", snapshot.roomCode ?: "")
                put("careerMatchType", snapshot.careerMatchType?.name ?: "")
                put("careerPlayerId", snapshot.careerPlayerId ?: "")
                put("careerPlayerName", snapshot.careerPlayerName ?: "")
                put("careerControlMode", snapshot.careerControlMode.name)
                put("savedAtMs", snapshot.savedAtMs)
                put("match", serializeMatch(snapshot.match))
            }
            prefs.edit().putString(KEY_ONGOING_MATCH_JSON, root.toString()).apply()
        }
    }

    fun loadOngoingMatch(): SavedMatchResumeSnapshot? = runCatching {
        val raw = prefs.getString(KEY_ONGOING_MATCH_JSON, null) ?: return null
        val root = JSONObject(raw)
        val matchObj = root.optJSONObject("match") ?: return null
        val match = deserializeMatch(matchObj) ?: return null
        if (match.finished) {
            clearOngoingMatch()
            return null
        }
        val filterName = root.optString("playFilter", InteractivePlayFilter.BOTH.name)
        val filter = InteractivePlayFilter.entries.firstOrNull { it.name == filterName } ?: InteractivePlayFilter.BOTH
        val source = SavedMatchSource.fromNameSafe(root.optString("source"))
        val roomCode = root.optString("roomCode").takeIf { it.isNotBlank() }
        val careerName = root.optString("careerMatchType")
        val careerType = CareerMatchType.entries.firstOrNull { it.name == careerName }
        val careerPlayerId = root.optString("careerPlayerId").takeIf { it.isNotBlank() }
        val careerPlayerName = root.optString("careerPlayerName").takeIf { it.isNotBlank() }
        val controlModeName = root.optString(
            "careerControlMode",
            if (careerPlayerId != null) CareerControlMode.PLAYER_ONLY.name else CareerControlMode.FULL_TEAM.name
        )
        val controlMode = CareerControlMode.entries.firstOrNull { it.name == controlModeName }
            ?: if (careerPlayerId != null) CareerControlMode.PLAYER_ONLY else CareerControlMode.FULL_TEAM
        SavedMatchResumeSnapshot(
            match = match,
            userTeamId = root.optString("userTeamId"),
            playFilter = filter,
            source = source,
            roomCode = roomCode,
            careerMatchType = careerType,
            careerPlayerId = careerPlayerId,
            careerPlayerName = careerPlayerName,
            careerControlMode = controlMode,
            savedAtMs = root.optLong("savedAtMs", 0L)
        )
    }.getOrNull()

    fun clearOngoingMatch() {
        runCatching {
            prefs.edit().remove(KEY_ONGOING_MATCH_JSON).apply()
        }
    }

    private fun serializeMatch(m: LiveMatchState): JSONObject = JSONObject().apply {
        put("format", m.format.name)
        put("stage", m.stage.name)
        put("pitchCondition", m.pitchCondition)
        put("pitchType", m.pitchType.name)
        put("weatherCondition", m.weatherCondition.name)
        put("dewFactor2ndInnings", m.dewFactor2ndInnings)
        put("fieldPreset", m.fieldPreset.name)
        put("tossSummary", m.tossSummary)
        put("teamA", serializeTeam(m.teamA))
        put("teamB", serializeTeam(m.teamB))
        put("teamACaptainId", m.teamACaptainId)
        put("teamBCaptainId", m.teamBCaptainId)
        put("teamAChemistry", m.teamAChemistry)
        put("teamBChemistry", m.teamBChemistry)
        put("teamAImpactUsed", m.teamAImpactUsed)
        put("teamBImpactUsed", m.teamBImpactUsed)
        put("isIplRules", m.isIplRules)
        put("teamAImpactNominees", JSONArray(m.teamAImpactNominees))
        put("teamBImpactNominees", JSONArray(m.teamBImpactNominees))
        put("teamAStartingOverseasCount", m.teamAStartingOverseasCount)
        put("teamBStartingOverseasCount", m.teamBStartingOverseasCount)
        put("activeInningsNumber", m.activeInningsNumber)
        put("innings1", serializeInnings(m.innings1))
        put("innings2", serializeInnings(m.innings2))
        put("commentaryFeed", JSONArray().apply {
            m.commentaryFeed.take(40).forEach { e ->
                put(JSONObject().apply {
                    put("inningsNumber", e.inningsNumber)
                    put("overLabel", e.overLabel)
                    put("batterName", e.batterName)
                    put("bowlerName", e.bowlerName)
                    put("runsScored", e.runsScored)
                    put("outcome", e.outcome.name)
                    put("commentary", e.commentary)
                    put("scoreAfterBall", e.scoreAfterBall)
                    put("isInjuryAlert", e.isInjuryAlert)
                    put("isSuperOver", e.isSuperOver)
                })
            }
        })
    }

    private fun deserializeMatch(o: JSONObject): LiveMatchState? {
        val teamA = deserializeTeam(o.optJSONObject("teamA") ?: return null)
        val teamB = deserializeTeam(o.optJSONObject("teamB") ?: return null)
        val format = MatchFormat.entries.firstOrNull { it.name == o.optString("format") } ?: MatchFormat.T20I
        val inn1 = deserializeInnings(o.optJSONObject("innings1") ?: return null, format) ?: return null
        val inn2 = deserializeInnings(o.optJSONObject("innings2") ?: return null, format) ?: return null
        val commArr = o.optJSONArray("commentaryFeed") ?: JSONArray()
        val comm = buildList {
            for (i in 0 until commArr.length()) {
                val c = commArr.optJSONObject(i) ?: continue
                add(
                    BallEvent(
                        inningsNumber = c.optInt("inningsNumber", 1),
                        overLabel = c.optString("overLabel"),
                        batterName = c.optString("batterName"),
                        bowlerName = c.optString("bowlerName"),
                        runsScored = c.optInt("runsScored", 0),
                        outcome = BallOutcomeType.entries.firstOrNull { it.name == c.optString("outcome") } ?: BallOutcomeType.DOT,
                        commentary = c.optString("commentary"),
                        scoreAfterBall = c.optString("scoreAfterBall"),
                        isInjuryAlert = c.optBoolean("isInjuryAlert", false),
                        isSuperOver = c.optBoolean("isSuperOver", false)
                    )
                )
            }
        }
        return LiveMatchState(
            format = format,
            stage = MatchStage.fromNameSafe(o.optString("stage")),
            pitchCondition = o.optString("pitchCondition", "Balanced Deck"),
            pitchType = PitchType.fromNameSafe(o.optString("pitchType")),
            weatherCondition = WeatherCondition.fromNameSafe(o.optString("weatherCondition")),
            dewFactor2ndInnings = o.optBoolean("dewFactor2ndInnings", false),
            fieldPreset = FieldPreset.fromNameSafe(o.optString("fieldPreset")),
            tossSummary = o.optString("tossSummary"),
            teamA = teamA,
            teamB = teamB,
            teamACaptainId = o.optString("teamACaptainId"),
            teamBCaptainId = o.optString("teamBCaptainId"),
            teamAChemistry = o.optInt("teamAChemistry", 80),
            teamBChemistry = o.optInt("teamBChemistry", 80),
            teamAImpactUsed = o.optBoolean("teamAImpactUsed", false),
            teamBImpactUsed = o.optBoolean("teamBImpactUsed", false),
            innings1 = inn1,
            innings2 = inn2,
            activeInningsNumber = o.optInt("activeInningsNumber", 1).coerceIn(1, 2),
            commentaryFeed = comm,
            isIplRules = o.optBoolean("isIplRules", false),
            teamAImpactNominees = o.optJSONArray("teamAImpactNominees").toStringList(),
            teamBImpactNominees = o.optJSONArray("teamBImpactNominees").toStringList(),
            teamAStartingOverseasCount = o.optInt("teamAStartingOverseasCount", 0),
            teamBStartingOverseasCount = o.optInt("teamBStartingOverseasCount", 0)
        )
    }

    private fun serializeTeam(t: TeamState): JSONObject = JSONObject().apply {
        put("teamId", t.teamId)
        put("ownerUid", t.ownerUid)
        put("teamName", t.teamName)
        put("bot", t.bot)
        put("captainPlayerId", t.captainPlayerId)
        put("squadPlayerIds", JSONArray(t.squadPlayerIds))
        put("playingXiIds", JSONArray(t.playingXiIds))
    }

    private fun deserializeTeam(o: JSONObject): TeamState = TeamState(
        teamId = o.optString("teamId"),
        ownerUid = o.optString("ownerUid"),
        teamName = o.optString("teamName"),
        bot = o.optBoolean("bot", false),
        captainPlayerId = o.optString("captainPlayerId"),
        squadPlayerIds = o.optJSONArray("squadPlayerIds").toStringList(),
        playingXiIds = o.optJSONArray("playingXiIds").toStringList()
    )

    private fun serializeCricketer(c: Cricketer): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("country", c.country)
        put("countryCode", c.countryCode)
        put("overseas", c.overseas)
        put("role", c.role.name)
        put("battingRating", c.battingRating)
        put("bowlingRating", c.bowlingRating)
        put("fieldingRating", c.fieldingRating)
        put("basePriceLakhs", c.basePriceLakhs)
    }

    private fun deserializeCricketer(o: JSONObject): Cricketer {
        val id = o.optString("id")
        val db = CricketerDatabase.getById(id)
        val role = PlayerRole.entries.firstOrNull { it.name == o.optString("role") } ?: db?.role ?: PlayerRole.BATTER
        return Cricketer(
            id = id,
            name = o.optString("name", db?.name ?: "Player"),
            country = o.optString("country", db?.country ?: "India"),
            countryCode = o.optString("countryCode", db?.countryCode ?: "IND"),
            overseas = o.optBoolean("overseas", db?.overseas ?: false),
            role = role,
            battingRating = o.optInt("battingRating", db?.battingRating ?: 75),
            bowlingRating = o.optInt("bowlingRating", db?.bowlingRating ?: 60),
            fieldingRating = o.optInt("fieldingRating", db?.fieldingRating ?: 75),
            basePriceLakhs = o.optLong("basePriceLakhs", db?.basePriceLakhs ?: 100L),
            eraTag = db?.eraTag ?: "Active",
            specialty = db?.specialty ?: "All-Format"
        )
    }

    private fun serializeInnings(inn: InningsState): JSONObject = JSONObject().apply {
        put("inningsNumber", inn.inningsNumber)
        put("battingTeamId", inn.battingTeamId)
        put("battingTeamName", inn.battingTeamName)
        put("battingCaptainId", inn.battingCaptainId)
        put("bowlingTeamId", inn.bowlingTeamId)
        put("bowlingTeamName", inn.bowlingTeamName)
        put("bowlingCaptainId", inn.bowlingCaptainId)
        put("battingLineup", JSONArray().apply { inn.battingLineup.forEach { put(serializeCricketer(it)) } })
        put("bowlingOptions", JSONArray().apply { inn.bowlingOptions.forEach { put(serializeCricketer(it)) } })
        put("battingChemistry", inn.battingChemistry)
        put("bowlingChemistry", inn.bowlingChemistry)
        put("fieldingAverage", inn.fieldingAverage)
        put("totalRuns", inn.totalRuns)
        put("wickets", inn.wickets)
        put("legalBalls", inn.legalBalls)
        put("extras", inn.extras)
        put("partnershipRuns", inn.partnershipRuns)
        put("strikerIdx", inn.strikerIdx)
        put("nonStrikerIdx", inn.nonStrikerIdx)
        put("nextBatterIdx", inn.nextBatterIdx)
        put("currentBowlerIdx", inn.currentBowlerIdx)
        put("lastOverBowlerId", inn.lastOverBowlerId)
        put("completed", inn.completed)
        put("battingReviewsLeft", inn.battingTeamReviewsRemaining)
        put("bowlingReviewsLeft", inn.bowlingTeamReviewsRemaining)
        put("wormByOver", JSONArray(inn.wormByOver))
        put("wicketOvers", JSONArray(inn.wicketOvers))
        put("battingCard", JSONArray().apply {
            inn.battingCard.forEach { b ->
                put(JSONObject().apply {
                    put("playerId", b.playerId)
                    put("playerName", b.playerName)
                    put("roleLabel", b.roleLabel)
                    put("runs", b.runs)
                    put("balls", b.balls)
                    put("fours", b.fours)
                    put("sixes", b.sixes)
                    put("out", b.out)
                    put("injured", b.injured)
                    put("isCaptain", b.isCaptain)
                    put("confidence", b.confidence)
                    put("dismissalText", b.dismissalText)
                })
            }
        })
        put("bowlingCard", JSONArray().apply {
            inn.bowlingCard.forEach { bw ->
                put(JSONObject().apply {
                    put("playerId", bw.playerId)
                    put("playerName", bw.playerName)
                    put("roleLabel", bw.roleLabel)
                    put("legalBalls", bw.legalBalls)
                    put("runsConceded", bw.runsConceded)
                    put("wickets", bw.wickets)
                    put("maidens", bw.maidens)
                    put("currentOverRuns", bw.currentOverRuns)
                    put("stamina", bw.stamina)
                    put("injured", bw.injured)
                    put("isCaptain", bw.isCaptain)
                })
            }
        })
        put("fallOfWickets", JSONArray().apply {
            inn.fallOfWickets.forEach { f ->
                put(JSONObject().apply {
                    put("wicketNumber", f.wicketNumber)
                    put("scoreAtFall", f.scoreAtFall)
                    put("overLabel", f.overLabel)
                    put("batterName", f.batterName)
                })
            }
        })
        put("wagonWheelShots", JSONArray().apply {
            inn.wagonWheelShots.takeLast(80).forEach { s ->
                put(JSONObject().apply {
                    put("batterId", s.batterId)
                    put("batterName", s.batterName)
                    put("angleDeg", s.angleDeg.toDouble())
                    put("distanceRatio", s.distanceRatio.toDouble())
                    put("runs", s.runs)
                    put("overLabel", s.overLabel)
                })
            }
        })
        put("pitchMapPoints", JSONArray().apply {
            inn.pitchMapPoints.takeLast(80).forEach { p ->
                put(JSONObject().apply {
                    put("bowlerId", p.bowlerId)
                    put("bowlerName", p.bowlerName)
                    put("lineX", p.lineX.toDouble())
                    put("lengthY", p.lengthY.toDouble())
                    put("outcome", p.outcome.name)
                    put("isSpin", p.isSpin)
                    put("overLabel", p.overLabel)
                })
            }
        })
    }

    private fun deserializeInnings(o: JSONObject, format: MatchFormat): InningsState? {
        val batArr = o.optJSONArray("battingLineup") ?: return null
        val bowlArr = o.optJSONArray("bowlingOptions") ?: return null
        val lineup = buildList {
            for (i in 0 until batArr.length()) {
                batArr.optJSONObject(i)?.let { add(deserializeCricketer(it)) }
            }
        }
        val bowlers = buildList {
            for (i in 0 until bowlArr.length()) {
                bowlArr.optJSONObject(i)?.let { add(deserializeCricketer(it)) }
            }
        }
        if (lineup.isEmpty() || bowlers.isEmpty()) return null

        val batCardArr = o.optJSONArray("battingCard") ?: JSONArray()
        val batCard = buildList {
            for (i in 0 until batCardArr.length()) {
                val b = batCardArr.optJSONObject(i) ?: continue
                add(
                    BatterScoreEntry(
                        playerId = b.optString("playerId"),
                        playerName = b.optString("playerName"),
                        roleLabel = b.optString("roleLabel"),
                        runs = b.optInt("runs"),
                        balls = b.optInt("balls"),
                        fours = b.optInt("fours"),
                        sixes = b.optInt("sixes"),
                        out = b.optBoolean("out"),
                        injured = b.optBoolean("injured"),
                        isCaptain = b.optBoolean("isCaptain"),
                        confidence = b.optInt("confidence", 40),
                        dismissalText = b.optString("dismissalText", "yet to bat")
                    )
                )
            }
        }

        val bowlCardArr = o.optJSONArray("bowlingCard") ?: JSONArray()
        val bowlCard = buildList {
            for (i in 0 until bowlCardArr.length()) {
                val bw = bowlCardArr.optJSONObject(i) ?: continue
                add(
                    BowlerScoreEntry(
                        playerId = bw.optString("playerId"),
                        playerName = bw.optString("playerName"),
                        roleLabel = bw.optString("roleLabel", "PACE"),
                        legalBalls = bw.optInt("legalBalls"),
                        runsConceded = bw.optInt("runsConceded"),
                        wickets = bw.optInt("wickets"),
                        maidens = bw.optInt("maidens"),
                        currentOverRuns = bw.optInt("currentOverRuns"),
                        stamina = bw.optInt("stamina", 100),
                        injured = bw.optBoolean("injured"),
                        isCaptain = bw.optBoolean("isCaptain")
                    )
                )
            }
        }

        val fowArr = o.optJSONArray("fallOfWickets") ?: JSONArray()
        val fow = buildList {
            for (i in 0 until fowArr.length()) {
                val f = fowArr.optJSONObject(i) ?: continue
                add(
                    FallOfWicket(
                        wicketNumber = f.optInt("wicketNumber"),
                        scoreAtFall = f.optInt("scoreAtFall"),
                        overLabel = f.optString("overLabel"),
                        batterName = f.optString("batterName")
                    )
                )
            }
        }

        val shotsArr = o.optJSONArray("wagonWheelShots") ?: JSONArray()
        val shots = buildList {
            for (i in 0 until shotsArr.length()) {
                val s = shotsArr.optJSONObject(i) ?: continue
                add(
                    ShotTelemetry(
                        batterId = s.optString("batterId"),
                        batterName = s.optString("batterName"),
                        angleDeg = s.optDouble("angleDeg", 0.0).toFloat(),
                        distanceRatio = s.optDouble("distanceRatio", 0.5).toFloat(),
                        runs = s.optInt("runs", 0),
                        overLabel = s.optString("overLabel")
                    )
                )
            }
        }

        val pitchArr = o.optJSONArray("pitchMapPoints") ?: JSONArray()
        val pitchPts = buildList {
            for (i in 0 until pitchArr.length()) {
                val p = pitchArr.optJSONObject(i) ?: continue
                add(
                    PitchMapPoint(
                        bowlerId = p.optString("bowlerId"),
                        bowlerName = p.optString("bowlerName"),
                        lineX = p.optDouble("lineX", 0.0).toFloat(),
                        lengthY = p.optDouble("lengthY", 0.5).toFloat(),
                        outcome = BallOutcomeType.entries.firstOrNull { it.name == p.optString("outcome") } ?: BallOutcomeType.DOT,
                        isSpin = p.optBoolean("isSpin", false),
                        overLabel = p.optString("overLabel")
                    )
                )
            }
        }

        return InningsState(
            format = format,
            inningsNumber = o.optInt("inningsNumber", 1),
            battingTeamId = o.optString("battingTeamId"),
            battingTeamName = o.optString("battingTeamName"),
            battingCaptainId = o.optString("battingCaptainId"),
            bowlingTeamId = o.optString("bowlingTeamId"),
            bowlingTeamName = o.optString("bowlingTeamName"),
            bowlingCaptainId = o.optString("bowlingCaptainId"),
            battingLineup = lineup,
            bowlingOptions = bowlers,
            bowlingTeamFullXi = bowlers,
            battingChemistry = o.optInt("battingChemistry", 80),
            bowlingChemistry = o.optInt("bowlingChemistry", 80),
            fieldingAverage = o.optInt("fieldingAverage", 80),
            totalRuns = o.optInt("totalRuns"),
            wickets = o.optInt("wickets"),
            legalBalls = o.optInt("legalBalls"),
            extras = o.optInt("extras"),
            partnershipRuns = o.optInt("partnershipRuns"),
            strikerIdx = o.optInt("strikerIdx").coerceIn(0, lineup.lastIndex),
            nonStrikerIdx = o.optInt("nonStrikerIdx", 1).coerceIn(0, lineup.lastIndex),
            nextBatterIdx = o.optInt("nextBatterIdx", 2),
            currentBowlerIdx = o.optInt("currentBowlerIdx").coerceIn(0, bowlers.lastIndex),
            lastOverBowlerId = o.optString("lastOverBowlerId"),
            battingCard = batCard,
            bowlingCard = bowlCard,
            wormByOver = o.optJSONArray("wormByOver").toIntList(),
            wicketOvers = o.optJSONArray("wicketOvers").toIntList(),
            fallOfWickets = fow,
            completed = o.optBoolean("completed"),
            battingTeamReviewsRemaining = o.optInt("battingReviewsLeft", 2),
            bowlingTeamReviewsRemaining = o.optInt("bowlingReviewsLeft", 2),
            wagonWheelShots = shots,
            pitchMapPoints = pitchPts
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val s = optString(i)
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) add(optInt(i))
        }
    }

    companion object {
        private const val PREFS_NAME = "cricbid_match_autosave_v1"
        private const val KEY_ONGOING_MATCH_JSON = "ongoing_match_json"
        private const val KEY_LAST_ROOM_CODE = "last_active_room_code"
    }
}
