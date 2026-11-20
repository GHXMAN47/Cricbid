package com.example.engine

import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerDifficulty
import com.example.data.model.CareerMatchLog
import com.example.data.model.CareerMatchType
import com.example.data.model.CareerPlayerAttributes
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerStage
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.HallOfFameLegend
import com.example.data.model.InternationalNations
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import com.example.data.model.TeamState
import com.example.data.model.TrainingDrill
import java.util.UUID
import kotlin.random.Random

data class TrainingOutcome(
    val updatedCareer: CareerSaveState,
    val message: String,
    val injured: Boolean = false,
    val levelsGained: Int = 0
)

data class IplAuctionDraftOutcome(
    val updatedCareer: CareerSaveState,
    val winningFranchise: String,
    val finalBidLakhs: Long,
    val biddingHighlights: List<String>
)

data class CareerMatchResolution(
    val updatedCareer: CareerSaveState,
    val summaryMessage: String,
    val milestoneBanner: String? = null,
    val newlyUnlockedStage: String? = null,
    val retirementPromptTriggered: Boolean = false
)

/**
 * Pure Kotlin engine for single-player "Career Mode (Road to Legend)".
 * Drives rookie creation (55-62 OVR), training drills with fatigue/injury tradeoffs,
 * Stage 1 (Domestic) -> Stage 2 (IPL Auction & Franchise) -> Stage 3 (International + Captaincy),
 * real MatchSimulationEngine team construction, and Retirement -> Hall of Fame Legend induction.
 */
object CareerEngine {

    private val domesticSidesByCountry = mapOf(
        "IND" to listOf("Mumbai Emerging XI", "Karnataka Challengers", "Delhi Colts", "Tamil Nadu XI", "Punjab Rising", "Bengal Tigers"),
        "AUS" to listOf("NSW Blues Emerging", "Victoria Bushrangers", "Queensland Bulls XI", "Western Warriors"),
        "ENG" to listOf("Surrey Academy XI", "Yorkshire Vikings", "Lancashire Lightning", "Kent Spitfires"),
        "RSA" to listOf("Gauteng Lions XI", "Western Province", "Dolphins Emerging", "Titans Academy"),
        "NZ" to listOf("Auckland Aces XI", "Canterbury Kings", "Wellington Firebirds", "Otago Volts"),
        "WI" to listOf("Barbados Pride XI", "Trinidad Red Force", "Jamaica Tallawahs XI", "Guyana Jaguars"),
        "SL" to listOf("Colombo Stars XI", "Kandy Falcons", "Galle Gladiators", "Dambulla Aura"),
        "PAK" to listOf("Lahore Blues XI", "Karachi Whites", "Multan Emerging", "Rawalpindi Rams")
    )

    private val iplFranchises = listOf(
        "Mumbai Mavericks",
        "Chennai Super Chargers",
        "Bengaluru Challengers",
        "Kolkata Knights",
        "Hyderabad Sunrisers",
        "Rajasthan Royals XI",
        "Delhi Capitals XI",
        "Gujarat Titans XI"
    )

    fun createRookie(
        name: String,
        startingAge: Int,
        nationality: String,
        countryCode: String,
        avatarId: String,
        role: PlayerRole,
        battingStyle: BattingStyle,
        bowlingStyle: BowlingStyle,
        difficulty: CareerDifficulty = CareerDifficulty.NORMAL
    ): CareerSaveState {
        val cleanName = name.trim().take(28).ifBlank { "Rookie Star" }
        val clampedAge = startingAge.coerceIn(17, 23)
        val attrs = when (role) {
            PlayerRole.BATTER -> CareerPlayerAttributes(
                battingTechnique = 60,
                powerHitting = 58,
                bowlingSkill = 22,
                fielding = 59
            )
            PlayerRole.WICKETKEEPER -> CareerPlayerAttributes(
                battingTechnique = 59,
                powerHitting = 56,
                bowlingSkill = 16,
                fielding = 62
            )
            PlayerRole.ALL_ROUNDER -> CareerPlayerAttributes(
                battingTechnique = 57,
                powerHitting = 57,
                bowlingSkill = 58,
                fielding = 59
            )
            PlayerRole.SEAMER -> CareerPlayerAttributes(
                battingTechnique = 34,
                powerHitting = 36,
                bowlingSkill = 61,
                fielding = 57
            )
            PlayerRole.SPINNER -> CareerPlayerAttributes(
                battingTechnique = 36,
                powerHitting = 34,
                bowlingSkill = 61,
                fielding = 58
            )
        }
        val domesticList = domesticSidesByCountry[countryCode] ?: domesticSidesByCountry["IND"]!!
        val id = "hof_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val career = CareerSaveState(
            id = id,
            name = cleanName,
            startingAge = clampedAge,
            age = clampedAge,
            season = 1,
            nationality = nationality,
            countryCode = countryCode,
            avatarId = avatarId,
            role = role,
            battingStyle = battingStyle,
            bowlingStyle = bowlingStyle,
            attributes = attrs,
            difficulty = difficulty,
            domesticTeamName = domesticList.first(),
            nationalTeamName = nationality,
            peakOverall = attrs.overallFor(role)
        )
        CricketerDatabase.registerActiveCareerPlayer(career.toCricketer())
        return career
    }

    fun applyTrainingDrill(
        career: CareerSaveState,
        drill: TrainingDrill,
        rng: Random = Random.Default
    ): TrainingOutcome {
        if (career.stage == CareerStage.RETIRED) {
            return TrainingOutcome(career, "Player has already retired to the Hall of Fame.")
        }
        if (career.trainingDoneBeforeMatch && drill != TrainingDrill.FITNESS_RECOVERY) {
            return TrainingOutcome(career, "Play your next match before running another skill drill (or choose Recovery).")
        }
        if (career.isInjured && drill != TrainingDrill.FITNESS_RECOVERY) {
            return TrainingOutcome(career, "You have a strain (${career.injuryMatchesRemaining}m). Run Fitness & Recovery first!")
        }

        val diff = career.difficulty
        val attrs = career.attributes
        var newTech = attrs.battingTechnique
        var newPower = attrs.powerHitting
        var newBowl = attrs.bowlingSkill
        var newFld = attrs.fielding
        var baseXpGain = 0
        val attrGain = diff.drillAttrGain
        val scaledFatigueDelta = if (drill.fatigueDelta > 0) {
            (drill.fatigueDelta * diff.fatigueMultiplier).toInt().coerceAtLeast(6)
        } else {
            // Recovery is slightly stronger on Easy and slightly stricter on Hard
            when (diff) {
                CareerDifficulty.EASY -> (drill.fatigueDelta - 6)
                CareerDifficulty.NORMAL -> drill.fatigueDelta
                CareerDifficulty.HARD -> (drill.fatigueDelta + 6).coerceAtMost(-24)
            }
        }
        val newFatigue = (career.fatigue + scaledFatigueDelta).coerceIn(0, 100)
        var newInjury = career.injuryMatchesRemaining
        var newForm = career.form

        when (drill) {
            TrainingDrill.BATTING_TECHNIQUE -> {
                newTech = (newTech + attrGain).coerceAtMost(99)
                baseXpGain = 30
                newForm = (newForm + 3).coerceAtMost(100)
            }
            TrainingDrill.POWER_HITTING -> {
                newPower = (newPower + attrGain).coerceAtMost(99)
                baseXpGain = 35
                newForm = (newForm + 2).coerceAtMost(100)
            }
            TrainingDrill.BOWLING_SKILL -> {
                newBowl = (newBowl + attrGain).coerceAtMost(99)
                baseXpGain = 35
                newForm = (newForm + 3).coerceAtMost(100)
            }
            TrainingDrill.FIELDING_AGILITY -> {
                newFld = (newFld + attrGain).coerceAtMost(99)
                baseXpGain = 28
                newForm = (newForm + 2).coerceAtMost(100)
            }
            TrainingDrill.FITNESS_RECOVERY -> {
                newInjury = (newInjury - 1).coerceAtLeast(0)
                baseXpGain = 15
                newForm = (newForm + 2).coerceAtMost(100)
            }
        }

        val xpGain = (baseXpGain * diff.xpMultiplier).toInt().coerceAtLeast(10)

        // Balance fatigue & injury risk on intense drills according to Difficulty
        var injuredNow = false
        if (drill != TrainingDrill.FITNESS_RECOVERY && newFatigue >= diff.injuryFatigueThreshold) {
            val baseChance = if (drill == TrainingDrill.POWER_HITTING || drill == TrainingDrill.BOWLING_SKILL) 0.30 else 0.18
            val injuryChance = (baseChance * diff.injuryRiskMultiplier).coerceIn(0.05, 0.72)
            if (rng.nextDouble() < injuryChance) {
                injuredNow = true
                newInjury = if (diff == CareerDifficulty.HARD && newFatigue >= 88) 2 else 1
                newForm = (newForm - 6).coerceAtLeast(35)
            }
        }

        val (leveledAttrs, newXp, newLevel, levelsGained) = applyXpAndLevelUps(
            role = career.role,
            attrs = CareerPlayerAttributes(newTech, newPower, newBowl, newFld),
            currentXp = career.xp + xpGain,
            currentLevel = career.level
        )
        val newPeak = maxOf(career.peakOverall, leveledAttrs.overallFor(career.role))
        val updated = career.copy(
            attributes = leveledAttrs,
            xp = newXp,
            level = newLevel,
            fatigue = newFatigue,
            injuryMatchesRemaining = newInjury,
            form = newForm,
            peakOverall = newPeak,
            trainingDoneBeforeMatch = drill != TrainingDrill.FITNESS_RECOVERY
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())

        val msg = buildString {
            append("${drill.title} complete (+$xpGain XP)")
            if (levelsGained > 0) append(" • LEVEL UP Lv.$newLevel (+attributes)!")
            if (injuredNow) append(" • 🚑 Overworked! Minor strain (${newInjury}m — run Recovery).")
            else if (drill == TrainingDrill.FITNESS_RECOVERY) append(" • Fatigue down to $newFatigue%.")
        }
        return TrainingOutcome(updated, msg, injured = injuredNow, levelsGained = levelsGained)
    }

    fun runIplAuctionDraftEvent(
        career: CareerSaveState,
        rng: Random = Random.Default
    ): IplAuctionDraftOutcome {
        val ovr = career.overallRating
        val baseLakhs = when {
            ovr >= 75 -> 150L
            ovr >= 66 -> 75L
            else -> 30L
        }
        val bidders = iplFranchises.shuffled(rng).take(3)
        val bidSteps = (3 + ((ovr - 55).coerceAtLeast(0) / 4) + (career.reputation / 20)).coerceIn(3, 12)
        var currentBid = baseLakhs
        var highestTeam = bidders.first()
        val highlights = mutableListOf<String>()
        highlights.add("${career.name} enters IPL Auction (Base ${CurrencyFormatter.formatLakhs(baseLakhs)})")

        for (step in 0 until bidSteps) {
            val team = bidders[step % bidders.size]
            if (step > 0) {
                currentBid += when {
                    currentBid < 100L -> 20L
                    currentBid < 500L -> 50L
                    else -> 100L
                }
            }
            highestTeam = team
            if (step == 0 || step == bidSteps / 2 || step == bidSteps - 1) {
                highlights.add("$team bids ${CurrencyFormatter.formatLakhs(currentBid)}")
            }
        }
        highlights.add("SOLD! $highestTeam signs ${career.name} for ${CurrencyFormatter.formatLakhs(currentBid)}!")

        val newAwards = (career.awards + "IPL Drafted: $highestTeam (${CurrencyFormatter.formatLakhs(currentBid)})").distinct()
        val updated = career.copy(
            stage = CareerStage.IPL_FRANCHISE,
            iplTeamName = highestTeam,
            iplBoughtPriceLakhs = currentBid,
            reputation = (career.reputation + 12).coerceAtMost(100),
            form = (career.form + 6).coerceAtMost(100),
            awards = newAwards
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return IplAuctionDraftOutcome(
            updatedCareer = updated,
            winningFranchise = highestTeam,
            finalBidLakhs = currentBid,
            biddingHighlights = highlights
        )
    }

    fun acceptInternationalCallUp(career: CareerSaveState): CareerSaveState {
        val nation = InternationalNations.all.firstOrNull { it.code == career.countryCode }
            ?: InternationalNations.all.first()
        val awards = (career.awards + "International Debut (${nation.name})").distinct()
        val updated = career.copy(
            stage = CareerStage.INTERNATIONAL,
            nationalTeamName = nation.name,
            reputation = (career.reputation + 10).coerceAtMost(100),
            awards = awards
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return updated
    }

    /**
     * Constructs two real TeamState objects for MatchSimulationEngine with the user's custom Career player
     * embedded in their team's Playing XI in their natural batting/bowling position.
     */
    fun buildCareerMatchTeams(
        career: CareerSaveState,
        matchType: CareerMatchType,
        rng: Random = Random.Default
    ): Pair<TeamState, TeamState> {
        val myCricketer = career.toCricketer()
        CricketerDatabase.registerActiveCareerPlayer(myCricketer)

        val userTeamName = when (matchType) {
            CareerMatchType.DOMESTIC_T20 -> career.domesticTeamName
            CareerMatchType.IPL_T20 -> career.iplTeamName.ifBlank { "IPL Emerging XI" }
            else -> career.nationalTeamName
        }

        val opponentName: String
        val myCompanions: List<String>
        val oppPlayerIds: List<String>

        when (matchType) {
            CareerMatchType.DOMESTIC_T20 -> {
                val domesticRivals = (domesticSidesByCountry[career.countryCode] ?: domesticSidesByCountry["IND"]!!)
                    .filter { it != career.domesticTeamName }
                opponentName = domesticRivals.random(rng)
                val emergingPool = CricketerDatabase.allPlayers
                    .filter { it.basePriceLakhs <= 100L }
                    .shuffled(rng)
                myCompanions = pickBalancedTenCompanions(emergingPool, career.role)
                oppPlayerIds = pickBalancedEleven(emergingPool.filter { it.id !in myCompanions })
            }
            CareerMatchType.IPL_T20 -> {
                val rivals = iplFranchises.filter { it != career.iplTeamName }
                opponentName = rivals.random(rng)
                val iplPool = CricketerDatabase.allPlayers.shuffled(rng)
                myCompanions = pickBalancedTenCompanions(iplPool, career.role)
                oppPlayerIds = pickBalancedEleven(iplPool.filter { it.id !in myCompanions })
            }
            else -> {
                val myNation = InternationalNations.all.firstOrNull { it.code == career.countryCode }
                    ?: InternationalNations.all.first()
                val oppNation = InternationalNations.all.filter { it.id != myNation.id }.random(rng)
                opponentName = oppNation.name
                val baseMyXi = myNation.squadIds.take(11)
                myCompanions = baseMyXi.dropLast(1).take(10)
                oppPlayerIds = oppNation.squadIds.take(11)
            }
        }

        val myLineupIds = placeCareerPlayerInOrder(myCricketer, myCompanions)
        val captainId = if (career.stage == CareerStage.INTERNATIONAL && career.isInternationalCaptain) {
            career.id
        } else {
            SquadRuleValidator.autoPickCaptain(myLineupIds)
        }

        val myTeam = TeamState(
            teamId = "career_user_team",
            ownerUid = "career_user",
            teamName = userTeamName,
            bot = false,
            squadPlayerIds = myLineupIds,
            playingXiIds = myLineupIds,
            captainPlayerId = captainId,
            xiReady = true
        )

        val oppOrdered = SquadRuleValidator.orderBattingLineup(
            oppPlayerIds.mapNotNull { CricketerDatabase.getById(it) }
        ).map { it.id }

        val oppTeam = TeamState(
            teamId = "career_opp_team",
            ownerUid = "career_ai",
            teamName = opponentName,
            bot = true,
            squadPlayerIds = oppOrdered,
            playingXiIds = oppOrdered,
            captainPlayerId = SquadRuleValidator.autoPickCaptain(oppOrdered),
            xiReady = true
        )

        return myTeam to oppTeam
    }

    /**
     * Reads the custom Career player's exact scorecard stats from a completed LiveMatchState
     * and applies XP, rating boosts, milestones, fatigue, season aging, captaincy, and unlock checks.
     */
    fun resolveCompletedMatch(
        career: CareerSaveState,
        match: LiveMatchState,
        matchType: CareerMatchType,
        rng: Random = Random.Default
    ): CareerMatchResolution {
        val userWon = match.winnerTeamId == "career_user_team"
        val userBattingCard = match.innings1.battingCard.firstOrNull { it.playerId == career.id }
            ?: match.innings2.battingCard.firstOrNull { it.playerId == career.id }
        val userBowlingCard = match.innings1.bowlingCard.firstOrNull { it.playerId == career.id }
            ?: match.innings2.bowlingCard.firstOrNull { it.playerId == career.id }

        val runs = userBattingCard?.runs ?: 0
        val balls = userBattingCard?.balls ?: 0
        val wickets = userBowlingCard?.wickets ?: 0
        val runsConceded = userBowlingCard?.runsConceded ?: 0
        val isPotm = match.playerOfTheMatch.startsWith(career.name, ignoreCase = true)

        val hitFifty = runs in 50..99
        val hitCentury = runs >= 100
        val tookThreeFor = wickets in 3..4
        val tookFiveFor = wickets >= 5

        // XP + Performance Rating Boosts scaled by CareerDifficulty
        val diff = career.difficulty
        var rawXp = 35
        if (userWon) rawXp += 20
        rawXp += (runs * 1.4).toInt()
        rawXp += wickets * 28
        if (hitFifty) rawXp += 35
        if (hitCentury) rawXp += 85
        if (tookThreeFor) rawXp += 35
        if (tookFiveFor) rawXp += 80
        if (isPotm) rawXp += 55
        val xpEarned = (rawXp * diff.xpMultiplier).toInt().coerceAtLeast(18)

        var tech = career.attributes.battingTechnique
        var power = career.attributes.powerHitting
        var bowl = career.attributes.bowlingSkill
        var fld = career.attributes.fielding
        if (runs >= 50) tech = (tech + 1).coerceAtMost(99)
        if (runs >= 100 || (userBattingCard?.sixes ?: 0) >= 3) power = (power + 1).coerceAtMost(99)
        if (wickets >= 3) bowl = (bowl + 1).coerceAtMost(99)
        if (wickets >= 5) bowl = (bowl + 1).coerceAtMost(99)
        if (isPotm) fld = (fld + 1).coerceAtMost(99)

        val (leveledAttrs, newXp, newLevel, levelsGained) = applyXpAndLevelUps(
            role = career.role,
            attrs = CareerPlayerAttributes(tech, power, bowl, fld),
            currentXp = career.xp + xpEarned,
            currentLevel = career.level
        )

        val repGain = buildRepGain(userWon, runs, wickets, isPotm, matchType)
        val newRep = (career.reputation + repGain).coerceIn(0, 100)
        val formDelta = when {
            hitCentury || tookFiveFor || isPotm -> 10
            hitFifty || tookThreeFor -> 6
            runs >= 25 || wickets >= 1 || userWon -> 2
            else -> if (diff == CareerDifficulty.HARD) -6 else -4
        }
        val newForm = (career.form + formDelta).coerceIn(30, 100)

        val baseFatigueIncrease = when (matchType.format) {
            com.example.data.model.MatchFormat.T20I -> 15
            com.example.data.model.MatchFormat.ODI -> 20
            com.example.data.model.MatchFormat.TEST -> 26
        }
        val fatigueIncrease = (baseFatigueIncrease * diff.fatigueMultiplier).toInt().coerceAtLeast(10)
        val newFatigue = (career.fatigue + fatigueIncrease).coerceIn(0, 100)
        var newInjury = (career.injuryMatchesRemaining - 1).coerceAtLeast(0)
        var injuredInMatch = false
        val matchInjuryThreshold = (diff.injuryFatigueThreshold + 8).coerceAtMost(94)
        val matchInjuryChance = (0.25 * diff.injuryRiskMultiplier).coerceIn(0.08, 0.55)
        if (newFatigue >= matchInjuryThreshold && rng.nextDouble() < matchInjuryChance) {
            newInjury = if (diff == CareerDifficulty.HARD && newFatigue >= 92) 2 else 1
            injuredInMatch = true
        }

        // Season progression (every 5 matches advances 1 full cricket season & +1 age)
        val nextMatchesInSeason = career.matchesInSeason + 1
        val seasonCompleted = nextMatchesInSeason >= 5
        val newSeason = if (seasonCompleted) career.season + 1 else career.season
        val newAge = if (seasonCompleted) career.age + 1 else career.age
        val resetMatchesInSeason = if (seasonCompleted) 0 else nextMatchesInSeason

        // Trophy checks on big matches / season finales
        var domTrophies = career.domesticTrophies
        var iplTrophies = career.iplTrophies
        var wcTrophies = career.worldCupTrophies
        var seriesWins = career.seriesWins
        val awards = career.awards.toMutableList()
        val milestoneParts = mutableListOf<String>()

        if (hitCentury) {
            milestoneParts.add("💯 Century ($runs)")
            if ("Maiden Century" !in awards) awards.add("Maiden Century")
        } else if (hitFifty) {
            milestoneParts.add("⭐ Half-Century ($runs)")
        }
        if (tookFiveFor) {
            milestoneParts.add("🔥 5-Wicket Haul ($wickets/$runsConceded)")
            if ("5-Wicket Haul" !in awards) awards.add("5-Wicket Haul")
        } else if (tookThreeFor) {
            milestoneParts.add("🎯 $wickets Wickets")
        }
        if (isPotm) {
            milestoneParts.add("🏆 Player of the Match")
        }
        if (userWon && matchType == CareerMatchType.WORLD_CUP) {
            wcTrophies += 1
            milestoneParts.add("🏆 WORLD CUP CHAMPION!")
            if ("World Cup Champion" !in awards) awards.add("World Cup Champion")
        } else if (seasonCompleted && userWon) {
            when (career.stage) {
                CareerStage.DOMESTIC -> {
                    domTrophies += 1
                    milestoneParts.add("🏆 Domestic Trophy Won!")
                    if ("Domestic Champion" !in awards) awards.add("Domestic Champion")
                }
                CareerStage.IPL_FRANCHISE -> {
                    iplTrophies += 1
                    milestoneParts.add("🏆 IPL Champion!")
                    if ("IPL Champion" !in awards) awards.add("IPL Champion")
                }
                CareerStage.INTERNATIONAL -> {
                    seriesWins += 1
                    milestoneParts.add("🏆 International Series Win!")
                }
                else -> {}
            }
        }

        // National Captaincy check in Stage 3
        var becameCaptain = career.isInternationalCaptain
        var newlyUnlocked: String? = null
        if (career.stage == CareerStage.INTERNATIONAL && !becameCaptain && newRep >= 78 && leveledAttrs.overallFor(career.role) >= 76) {
            becameCaptain = true
            newlyUnlocked = "👑 Appointed Captain (C) of ${career.nationalTeamName}!"
            awards.add("National Captain (C)")
        }

        val bestWkts: Int
        val bestRuns: Int
        if (wickets > career.bestBowlingWickets || (wickets == career.bestBowlingWickets && wickets > 0 && runsConceded < career.bestBowlingRuns)) {
            bestWkts = wickets
            bestRuns = runsConceded
        } else {
            bestWkts = career.bestBowlingWickets
            bestRuns = career.bestBowlingRuns
        }

        val log = CareerMatchLog(
            season = career.season,
            matchTypeLabel = matchType.label,
            opponent = match.teamB.teamName,
            runs = runs,
            balls = balls,
            wickets = wickets,
            runsConceded = runsConceded,
            teamWon = userWon,
            potm = isPotm,
            milestoneNote = milestoneParts.joinToString(" • "),
            xpEarned = xpEarned
        )

        val newPeak = maxOf(career.peakOverall, leveledAttrs.overallFor(career.role))
        val updated = career.copy(
            age = newAge,
            season = newSeason,
            attributes = leveledAttrs,
            xp = newXp,
            level = newLevel,
            reputation = newRep,
            form = newForm,
            fatigue = newFatigue,
            injuryMatchesRemaining = newInjury,
            isInternationalCaptain = becameCaptain,
            matchesInSeason = resetMatchesInSeason,
            matchesPlayed = career.matchesPlayed + 1,
            totalRuns = career.totalRuns + runs,
            highestScore = maxOf(career.highestScore, runs),
            fifties = career.fifties + if (hitFifty) 1 else 0,
            centuries = career.centuries + if (hitCentury) 1 else 0,
            totalWickets = career.totalWickets + wickets,
            bestBowlingWickets = bestWkts,
            bestBowlingRuns = bestRuns,
            potmAwards = career.potmAwards + if (isPotm) 1 else 0,
            domesticTrophies = domTrophies,
            iplTrophies = iplTrophies,
            worldCupTrophies = wcTrophies,
            seriesWins = seriesWins,
            peakOverall = newPeak,
            trainingDoneBeforeMatch = false,
            awards = awards.distinct(),
            recentLogs = (career.recentLogs + log).takeLast(15)
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())

        if (newlyUnlocked == null) {
            if (updated.canEnterIplAuction && !career.canEnterIplAuction) {
                newlyUnlocked = "🔨 IPL Auction Entry Unlocked! Enter the auction pool now."
            } else if (updated.canEarnInternationalCallUp && !career.canEarnInternationalCallUp) {
                newlyUnlocked = "🌍 International Call-Up Earned! Join ${updated.nationalTeamName}."
            }
        }

        val retirementTriggered = updated.age >= 36
        val summary = buildString {
            append("${if (userWon) "WIN" else "LOSS"} vs ${match.teamB.teamName}: ")
            append("$runs($balls)")
            if (wickets > 0) append(" & $wickets/$runsConceded")
            append(" • +$xpEarned XP")
            if (levelsGained > 0) append(" • Lv.$newLevel!")
            if (injuredInMatch) append(" • 🚑 Strain!")
        }

        return CareerMatchResolution(
            updatedCareer = updated,
            summaryMessage = summary,
            milestoneBanner = milestoneParts.takeIf { it.isNotEmpty() }?.joinToString(" • "),
            newlyUnlockedStage = newlyUnlocked,
            retirementPromptTriggered = retirementTriggered
        )
    }

    fun retireToHallOfFame(career: CareerSaveState): Pair<CareerSaveState, HallOfFameLegend> {
        val peakBoost = (career.peakOverall - career.overallRating).coerceAtLeast(0)
        val finalAwards = if (career.difficulty == CareerDifficulty.HARD) {
            (career.awards + "Hard Mode Veteran").distinct()
        } else {
            career.awards
        }
        val legend = HallOfFameLegend(
            id = career.id,
            name = career.name,
            nationality = career.nationality,
            countryCode = career.countryCode,
            avatarId = career.avatarId,
            role = career.role,
            battingStyle = career.battingStyle,
            bowlingStyle = career.bowlingStyle,
            peakOverall = maxOf(career.peakOverall, career.overallRating),
            battingRating = (career.attributes.battingRating + peakBoost).coerceIn(30, 99),
            bowlingRating = (career.attributes.bowlingRating + peakBoost).coerceIn(15, 99),
            fieldingRating = career.attributes.fieldingRating.coerceIn(55, 99),
            seasonsPlayed = career.season,
            retiredAge = career.age,
            matchesPlayed = career.matchesPlayed,
            totalRuns = career.totalRuns,
            highestScore = career.highestScore,
            fifties = career.fifties,
            centuries = career.centuries,
            totalWickets = career.totalWickets,
            potmAwards = career.potmAwards,
            totalTrophies = career.totalTrophies,
            wasCaptain = career.isInternationalCaptain,
            iplTeamName = career.iplTeamName,
            awards = finalAwards,
            difficulty = career.difficulty,
            includeInAuctions = true
        )
        val retiredCareer = career.copy(stage = CareerStage.RETIRED)
        return retiredCareer to legend
    }

    private data class LevelUpResult(
        val attributes: CareerPlayerAttributes,
        val remainingXp: Int,
        val level: Int,
        val levelsGained: Int
    )

    private fun applyXpAndLevelUps(
        role: PlayerRole,
        attrs: CareerPlayerAttributes,
        currentXp: Int,
        currentLevel: Int
    ): LevelUpResult {
        var xp = currentXp
        var level = currentLevel
        var gained = 0
        var tech = attrs.battingTechnique
        var power = attrs.powerHitting
        var bowl = attrs.bowlingSkill
        var fld = attrs.fielding

        while (xp >= 100 + (level - 1) * 35 && level < 50) {
            xp -= 100 + (level - 1) * 35
            level++
            gained++
            when (role) {
                PlayerRole.BATTER -> {
                    tech = (tech + 2).coerceAtMost(99)
                    power = (power + 2).coerceAtMost(99)
                    fld = (fld + 1).coerceAtMost(99)
                }
                PlayerRole.WICKETKEEPER -> {
                    tech = (tech + 2).coerceAtMost(99)
                    power = (power + 1).coerceAtMost(99)
                    fld = (fld + 2).coerceAtMost(99)
                }
                PlayerRole.ALL_ROUNDER -> {
                    tech = (tech + 1).coerceAtMost(99)
                    power = (power + 2).coerceAtMost(99)
                    bowl = (bowl + 2).coerceAtMost(99)
                    fld = (fld + 1).coerceAtMost(99)
                }
                PlayerRole.SEAMER, PlayerRole.SPINNER -> {
                    bowl = (bowl + 2).coerceAtMost(99)
                    fld = (fld + 1).coerceAtMost(99)
                    tech = (tech + 1).coerceAtMost(99)
                }
            }
        }
        return LevelUpResult(
            attributes = CareerPlayerAttributes(tech, power, bowl, fld),
            remainingXp = xp,
            level = level,
            levelsGained = gained
        )
    }

    private fun buildRepGain(
        won: Boolean,
        runs: Int,
        wickets: Int,
        potm: Boolean,
        matchType: CareerMatchType
    ): Int {
        var gain = if (won) 4 else 1
        if (runs >= 30) gain += 2
        if (runs >= 50) gain += 3
        if (runs >= 100) gain += 6
        if (wickets >= 2) gain += 3
        if (wickets >= 4) gain += 5
        if (potm) gain += 5
        if (matchType == CareerMatchType.WORLD_CUP && won) gain += 8
        return gain
    }

    private fun pickBalancedTenCompanions(pool: List<com.example.data.model.Cricketer>, myRole: PlayerRole): List<String> {
        val picked = mutableListOf<com.example.data.model.Cricketer>()
        if (myRole != PlayerRole.WICKETKEEPER) {
            pool.firstOrNull { it.role == PlayerRole.WICKETKEEPER }?.let { picked.add(it) }
        }
        val bowlersNeeded = if (myRole == PlayerRole.SEAMER || myRole == PlayerRole.SPINNER) 4 else 5
        pool.filter { (it.role == PlayerRole.SEAMER || it.role == PlayerRole.SPINNER || it.role == PlayerRole.ALL_ROUNDER) && it !in picked }
            .take(bowlersNeeded)
            .forEach { picked.add(it) }
        pool.filter { it !in picked }.take(10 - picked.size).forEach { picked.add(it) }
        return picked.take(10).map { it.id }
    }

    private fun pickBalancedEleven(pool: List<com.example.data.model.Cricketer>): List<String> {
        val picked = mutableListOf<com.example.data.model.Cricketer>()
        pool.firstOrNull { it.role == PlayerRole.WICKETKEEPER }?.let { picked.add(it) }
        pool.filter { (it.role == PlayerRole.SEAMER || it.role == PlayerRole.SPINNER) && it !in picked }
            .take(4)
            .forEach { picked.add(it) }
        pool.filter { it !in picked }.take(11 - picked.size).forEach { picked.add(it) }
        return picked.take(11).map { it.id }
    }

    private fun placeCareerPlayerInOrder(
        myCricketer: com.example.data.model.Cricketer,
        companionIds: List<String>
    ): List<String> {
        val orderedCompanions = SquadRuleValidator.orderBattingLineup(
            companionIds.mapNotNull { CricketerDatabase.getById(it) }
        ).map { it.id }.toMutableList()
        val targetSlot = when (myCricketer.role) {
            PlayerRole.BATTER -> 2 // #3 star slot so the user bats almost every single match!
            PlayerRole.WICKETKEEPER -> 3 // #4
            PlayerRole.ALL_ROUNDER -> 4 // #5
            PlayerRole.SEAMER, PlayerRole.SPINNER -> 7 // #8 bowling specialist
        }.coerceIn(0, orderedCompanions.size)
        orderedCompanions.add(targetSlot, myCricketer.id)
        return orderedCompanions.take(11)
    }
}
