package com.example.data.local

import android.content.Context
import com.example.data.model.BatEquipmentPreset
import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerDifficulty
import com.example.data.model.CareerMatchLog
import com.example.data.model.CareerMatchObjective
import com.example.data.model.CareerPlayerAppearance
import com.example.data.model.CareerPlayerAttributes
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerStage
import com.example.data.model.CricketerDatabase
import com.example.data.model.FacialFeaturePreset
import com.example.data.model.HairstylePreset
import com.example.data.model.HallOfFameLegend
import com.example.data.model.KitThemePreset
import com.example.data.model.PlayerRole
import com.example.data.model.ProT20League
import com.example.ui.components.IndianPlayerAvatars
import org.json.JSONArray
import org.json.JSONObject

/**
 * Crash-safe local persistence for Player Career Mode ("Road to Legend")
 * and the Hall of Fame Legends Pool (automatically synced into CricketerDatabase for future auctions).
 */
class LocalCareerStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadActiveCareer(): CareerSaveState? {
        val raw = runCatching { prefs.getString(KEY_ACTIVE_CAREER, null) }.getOrNull() ?: return null
        return try {
            val career = deserializeCareer(JSONObject(raw))
            CricketerDatabase.registerActiveCareerPlayer(career.toCricketer())
            career
        } catch (_: Exception) {
            null
        }
    }

    fun saveActiveCareer(career: CareerSaveState?) {
        runCatching {
            if (career == null) {
                prefs.edit().remove(KEY_ACTIVE_CAREER).apply()
                CricketerDatabase.registerActiveCareerPlayer(null)
                return
            }
            CricketerDatabase.registerActiveCareerPlayer(career.toCricketer())
            prefs.edit().putString(KEY_ACTIVE_CAREER, serializeCareer(career).toString()).apply()
        }
    }

    fun loadHallOfFame(): List<HallOfFameLegend> {
        val raw = runCatching { prefs.getString(KEY_HALL_OF_FAME, null) }.getOrNull() ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<HallOfFameLegend>()
            for (i in 0 until arr.length()) {
                list.add(deserializeLegend(arr.getJSONObject(i)))
            }
            syncLegendsToAuctionPool(list)
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveHallOfFame(legends: List<HallOfFameLegend>) {
        runCatching {
            val arr = JSONArray()
            legends.forEach { arr.put(serializeLegend(it)) }
            prefs.edit().putString(KEY_HALL_OF_FAME, arr.toString()).apply()
            syncLegendsToAuctionPool(legends)
        }
    }

    fun syncLegendsToAuctionPool(legends: List<HallOfFameLegend> = loadHallOfFame()) {
        val activeLegends = legends
            .filter { it.includeInAuctions }
            .map { it.toAuctionCricketer() }
        CricketerDatabase.registerHallOfFameLegends(activeLegends)
    }

    private fun serializeAppearance(a: CareerPlayerAppearance): JSONObject = JSONObject().apply {
        put("hair", a.hairstyle.name)
        put("face", a.facialFeature.name)
        put("kit", a.kitTheme.name)
        put("bat", a.batStyle.name)
        put("jersey", a.jerseyNumber)
    }

    private fun deserializeAppearance(o: JSONObject?): CareerPlayerAppearance {
        if (o == null) return CareerPlayerAppearance()
        return CareerPlayerAppearance(
            hairstyle = HairstylePreset.fromNameSafe(o.optString("hair")),
            facialFeature = FacialFeaturePreset.fromNameSafe(o.optString("face")),
            kitTheme = KitThemePreset.fromNameSafe(o.optString("kit")),
            batStyle = BatEquipmentPreset.fromNameSafe(o.optString("bat")),
            jerseyNumber = o.optInt("jersey", 18).coerceIn(1, 99)
        )
    }

    private fun serializeObjective(obj: CareerMatchObjective): JSONObject = JSONObject().apply {
        put("id", obj.id)
        put("title", obj.title)
        put("desc", obj.description)
        put("runs", obj.targetRuns)
        put("wkts", obj.targetWickets)
        if (obj.maxEconomy != null) put("maxEcon", obj.maxEconomy)
        put("sp", obj.rewardSkillPoints)
        put("sel", obj.rewardSelectionBonus)
    }

    private fun deserializeObjective(o: JSONObject?): CareerMatchObjective? {
        if (o == null) return null
        val title = o.optString("title", "")
        if (title.isBlank()) return null
        return CareerMatchObjective(
            id = o.optString("id", "obj_1"),
            title = title,
            description = o.optString("desc", ""),
            targetRuns = o.optInt("runs", 0),
            targetWickets = o.optInt("wkts", 0),
            maxEconomy = if (o.has("maxEcon")) o.optDouble("maxEcon") else null,
            rewardSkillPoints = o.optInt("sp", 1),
            rewardSelectionBonus = o.optInt("sel", 12)
        )
    }

    private fun serializeCareer(c: CareerSaveState): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("startingAge", c.startingAge)
        put("age", c.age)
        put("season", c.season)
        put("nationality", c.nationality)
        put("countryCode", c.countryCode)
        put("avatarId", c.avatarId)
        put("appearance", serializeAppearance(c.appearance))
        put("role", c.role.name)
        put("battingStyle", c.battingStyle.name)
        put("bowlingStyle", c.bowlingStyle.name)
        put("batTech", c.attributes.battingTechnique)
        put("powerHit", c.attributes.powerHitting)
        put("bowlSkill", c.attributes.bowlingSkill)
        put("fielding", c.attributes.fielding)
        put("batPow", c.attributes.battingPower)
        put("batTim", c.attributes.battingTiming)
        put("batCon", c.attributes.battingConsistency)
        put("bwlPs", c.attributes.bowlingPaceOrSpin)
        put("bwlAcc", c.attributes.bowlingAccuracy)
        put("bwlWkt", c.attributes.bowlingWicketTaking)
        put("fldCat", c.attributes.fieldingCatching)
        put("fldThr", c.attributes.fieldingThrowing)
        put("fldStp", c.attributes.fieldingStopping)
        put("xp", c.xp)
        put("level", c.level)
        put("skillPoints", c.skillPoints)
        put("reputation", c.reputation)
        put("selectionMeter", c.selectionMeter)
        put("droppedMatches", c.droppedMatchesRemaining)
        put("poorStreak", c.consecutivePoorMatches)
        c.currentObjective?.let { put("objective", serializeObjective(it)) }
        put("objectivesCompleted", c.objectivesCompleted)
        put("achievements", JSONArray(c.unlockedAchievements))
        put("form", c.form)
        put("fatigue", c.fatigue)
        put("injuryMatches", c.injuryMatchesRemaining)
        put("difficulty", c.difficulty.name)
        put("stage", c.stage.name)
        put("clubTeamName", c.clubTeamName)
        put("domesticTeamName", c.domesticTeamName)
        put("activeProLeague", c.activeProLeague.name)
        put("signedProLeagues", JSONArray(c.signedProLeagues))
        put("iplTeamName", c.iplTeamName)
        put("iplBoughtPriceLakhs", c.iplBoughtPriceLakhs)
        put("nationalTeamName", c.nationalTeamName)
        put("isIntlCaptain", c.isInternationalCaptain)
        put("matchesInSeason", c.matchesInSeason)
        put("matchesPlayed", c.matchesPlayed)
        put("totalRuns", c.totalRuns)
        put("highestScore", c.highestScore)
        put("fifties", c.fifties)
        put("centuries", c.centuries)
        put("totalWickets", c.totalWickets)
        put("bestBowlingWickets", c.bestBowlingWickets)
        put("bestBowlingRuns", c.bestBowlingRuns)
        put("potmAwards", c.potmAwards)
        put("domesticTrophies", c.domesticTrophies)
        put("iplTrophies", c.iplTrophies)
        put("worldCupTrophies", c.worldCupTrophies)
        put("seriesWins", c.seriesWins)
        put("peakOverall", c.peakOverall)
        put("trainingDone", c.trainingDoneBeforeMatch)
        put("awards", JSONArray(c.awards))
        val logsArr = JSONArray()
        c.recentLogs.takeLast(15).forEach { log ->
            logsArr.put(
                JSONObject().apply {
                    put("season", log.season)
                    put("type", log.matchTypeLabel)
                    put("opp", log.opponent)
                    put("runs", log.runs)
                    put("balls", log.balls)
                    put("wkts", log.wickets)
                    put("rc", log.runsConceded)
                    put("won", log.teamWon)
                    put("potm", log.potm)
                    put("ms", log.milestoneNote)
                    put("xp", log.xpEarned)
                    if (log.objectivePassed != null) put("objPass", log.objectivePassed)
                }
            )
        }
        put("recentLogs", logsArr)
    }

    private fun deserializeCareer(o: JSONObject): CareerSaveState {
        val awardsArr = o.optJSONArray("awards") ?: JSONArray()
        val awards = (0 until awardsArr.length()).mapNotNull { awardsArr.optString(it).takeIf { s -> s.isNotBlank() } }
        val achArr = o.optJSONArray("achievements") ?: JSONArray()
        val achievements = (0 until achArr.length()).mapNotNull { achArr.optString(it).takeIf { s -> s.isNotBlank() } }
        val proLeaguesArr = o.optJSONArray("signedProLeagues") ?: JSONArray()
        val signedProLeagues = (0 until proLeaguesArr.length()).mapNotNull { proLeaguesArr.optString(it).takeIf { s -> s.isNotBlank() } }

        val logsArr = o.optJSONArray("recentLogs") ?: JSONArray()
        val logs = (0 until logsArr.length()).map { i ->
            val l = logsArr.getJSONObject(i)
            CareerMatchLog(
                season = l.optInt("season", 1),
                matchTypeLabel = l.optString("type", "Match"),
                opponent = l.optString("opp", "Rivals"),
                runs = l.optInt("runs", 0),
                balls = l.optInt("balls", 0),
                wickets = l.optInt("wkts", 0),
                runsConceded = l.optInt("rc", 0),
                teamWon = l.optBoolean("won", false),
                potm = l.optBoolean("potm", false),
                milestoneNote = l.optString("ms", ""),
                xpEarned = l.optInt("xp", 0),
                objectivePassed = if (l.has("objPass")) l.optBoolean("objPass") else null
            )
        }

        val batTech = o.optInt("batTech", 58)
        val powerHit = o.optInt("powerHit", 56)
        val bowlSkill = o.optInt("bowlSkill", 25)
        val fielding = o.optInt("fielding", 58)

        return CareerSaveState(
            id = o.optString("id", "career_1"),
            name = o.optString("name", "Rookie Star"),
            startingAge = o.optInt("startingAge", 18),
            age = o.optInt("age", 18),
            season = o.optInt("season", 1),
            nationality = o.optString("nationality", "India"),
            countryCode = o.optString("countryCode", "IND"),
            avatarId = o.optString("avatarId", IndianPlayerAvatars.DEFAULT_ID),
            appearance = deserializeAppearance(o.optJSONObject("appearance")),
            role = runCatching { PlayerRole.valueOf(o.optString("role")) }.getOrDefault(PlayerRole.BATTER),
            battingStyle = runCatching { BattingStyle.valueOf(o.optString("battingStyle")) }.getOrDefault(BattingStyle.RIGHT_HAND_BAT),
            bowlingStyle = runCatching { BowlingStyle.valueOf(o.optString("bowlingStyle")) }.getOrDefault(BowlingStyle.OCCASIONAL_MEDIUM),
            attributes = CareerPlayerAttributes(
                battingTechnique = batTech,
                powerHitting = powerHit,
                bowlingSkill = bowlSkill,
                fielding = fielding,
                battingPower = o.optInt("batPow", powerHit),
                battingTiming = o.optInt("batTim", batTech),
                battingConsistency = o.optInt("batCon", batTech),
                bowlingPaceOrSpin = o.optInt("bwlPs", bowlSkill),
                bowlingAccuracy = o.optInt("bwlAcc", bowlSkill),
                bowlingWicketTaking = o.optInt("bwlWkt", bowlSkill),
                fieldingCatching = o.optInt("fldCat", fielding),
                fieldingThrowing = o.optInt("fldThr", fielding),
                fieldingStopping = o.optInt("fldStp", fielding)
            ),
            xp = o.optInt("xp", 0),
            level = o.optInt("level", 1),
            skillPoints = o.optInt("skillPoints", 2),
            reputation = o.optInt("reputation", 10),
            selectionMeter = o.optInt("selectionMeter", 65),
            droppedMatchesRemaining = o.optInt("droppedMatches", 0),
            consecutivePoorMatches = o.optInt("poorStreak", 0),
            currentObjective = deserializeObjective(o.optJSONObject("objective")),
            objectivesCompleted = o.optInt("objectivesCompleted", 0),
            unlockedAchievements = achievements,
            form = o.optInt("form", 62),
            fatigue = o.optInt("fatigue", 15),
            injuryMatchesRemaining = o.optInt("injuryMatches", 0),
            difficulty = CareerDifficulty.fromNameSafe(o.optString("difficulty", CareerDifficulty.NORMAL.name)),
            stage = runCatching { CareerStage.valueOf(o.optString("stage")) }.getOrDefault(CareerStage.CLUB_CRICKET),
            clubTeamName = o.optString("clubTeamName", "Hometown Cricket Club"),
            domesticTeamName = o.optString("domesticTeamName", "Mumbai Emerging XI"),
            activeProLeague = ProT20League.fromNameSafe(o.optString("activeProLeague")),
            signedProLeagues = signedProLeagues,
            iplTeamName = o.optString("iplTeamName", ""),
            iplBoughtPriceLakhs = o.optLong("iplBoughtPriceLakhs", 0L),
            nationalTeamName = o.optString("nationalTeamName", "India"),
            isInternationalCaptain = o.optBoolean("isIntlCaptain", false),
            matchesInSeason = o.optInt("matchesInSeason", 0),
            matchesPlayed = o.optInt("matchesPlayed", 0),
            totalRuns = o.optInt("totalRuns", 0),
            highestScore = o.optInt("highestScore", 0),
            fifties = o.optInt("fifties", 0),
            centuries = o.optInt("centuries", 0),
            totalWickets = o.optInt("totalWickets", 0),
            bestBowlingWickets = o.optInt("bestBowlingWickets", 0),
            bestBowlingRuns = o.optInt("bestBowlingRuns", 0),
            potmAwards = o.optInt("potmAwards", 0),
            domesticTrophies = o.optInt("domesticTrophies", 0),
            iplTrophies = o.optInt("iplTrophies", 0),
            worldCupTrophies = o.optInt("worldCupTrophies", 0),
            seriesWins = o.optInt("seriesWins", 0),
            peakOverall = o.optInt("peakOverall", 58),
            trainingDoneBeforeMatch = o.optBoolean("trainingDone", false),
            awards = awards,
            recentLogs = logs
        )
    }

    private fun serializeLegend(l: HallOfFameLegend): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("name", l.name)
        put("nationality", l.nationality)
        put("countryCode", l.countryCode)
        put("avatarId", l.avatarId)
        put("appearance", serializeAppearance(l.appearance))
        put("role", l.role.name)
        put("battingStyle", l.battingStyle.name)
        put("bowlingStyle", l.bowlingStyle.name)
        put("peakOverall", l.peakOverall)
        put("bat", l.battingRating)
        put("bowl", l.bowlingRating)
        put("fld", l.fieldingRating)
        put("seasons", l.seasonsPlayed)
        put("retiredAge", l.retiredAge)
        put("matches", l.matchesPlayed)
        put("runs", l.totalRuns)
        put("hs", l.highestScore)
        put("fifties", l.fifties)
        put("centuries", l.centuries)
        put("wkts", l.totalWickets)
        put("potm", l.potmAwards)
        put("trophies", l.totalTrophies)
        put("captain", l.wasCaptain)
        put("iplTeam", l.iplTeamName)
        put("awards", JSONArray(l.awards))
        put("difficulty", l.difficulty.name)
        put("includeInAuctions", l.includeInAuctions)
        put("achievementsCount", l.achievementsCount)
    }

    private fun deserializeLegend(o: JSONObject): HallOfFameLegend {
        val awardsArr = o.optJSONArray("awards") ?: JSONArray()
        val awards = (0 until awardsArr.length()).mapNotNull { awardsArr.optString(it).takeIf { s -> s.isNotBlank() } }
        return HallOfFameLegend(
            id = o.optString("id"),
            name = o.optString("name"),
            nationality = o.optString("nationality", "India"),
            countryCode = o.optString("countryCode", "IND"),
            avatarId = o.optString("avatarId", IndianPlayerAvatars.DEFAULT_ID),
            appearance = deserializeAppearance(o.optJSONObject("appearance")),
            role = runCatching { PlayerRole.valueOf(o.optString("role")) }.getOrDefault(PlayerRole.BATTER),
            battingStyle = runCatching { BattingStyle.valueOf(o.optString("battingStyle")) }.getOrDefault(BattingStyle.RIGHT_HAND_BAT),
            bowlingStyle = runCatching { BowlingStyle.valueOf(o.optString("bowlingStyle")) }.getOrDefault(BowlingStyle.OCCASIONAL_MEDIUM),
            peakOverall = o.optInt("peakOverall", 82),
            battingRating = o.optInt("bat", 82),
            bowlingRating = o.optInt("bowl", 40),
            fieldingRating = o.optInt("fld", 82),
            seasonsPlayed = o.optInt("seasons", 10),
            retiredAge = o.optInt("retiredAge", 37),
            matchesPlayed = o.optInt("matches", 60),
            totalRuns = o.optInt("runs", 2500),
            highestScore = o.optInt("hs", 112),
            fifties = o.optInt("fifties", 16),
            centuries = o.optInt("centuries", 4),
            totalWickets = o.optInt("wkts", 20),
            potmAwards = o.optInt("potm", 8),
            totalTrophies = o.optInt("trophies", 2),
            wasCaptain = o.optBoolean("captain", false),
            iplTeamName = o.optString("iplTeam", ""),
            awards = awards,
            difficulty = CareerDifficulty.fromNameSafe(o.optString("difficulty", CareerDifficulty.NORMAL.name)),
            includeInAuctions = o.optBoolean("includeInAuctions", true),
            achievementsCount = o.optInt("achievementsCount", 0)
        )
    }

    companion object {
        private const val PREFS_NAME = "cricbid_career_vault"
        private const val KEY_ACTIVE_CAREER = "active_career_json"
        private const val KEY_HALL_OF_FAME = "hall_of_fame_json"
    }
}
