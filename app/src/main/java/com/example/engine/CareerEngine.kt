package com.example.engine

import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerAchievementId
import com.example.data.model.CareerDifficulty
import com.example.data.model.CareerMatchLog
import com.example.data.model.CareerMatchObjective
import com.example.data.model.CareerMatchType
import com.example.data.model.CareerPlayerAppearance
import com.example.data.model.CareerPlayerAttributes
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerSkillStat
import com.example.data.model.CareerStage
import com.example.data.model.CricketerDatabase
import com.example.data.model.CurrencyFormatter
import com.example.data.model.HallOfFameLegend
import com.example.data.model.InternationalNations
import com.example.data.model.MatchFormat
import com.example.data.model.PlayerRole
import com.example.data.model.ProT20League
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
    val biddingHighlights: List<String>,
    val league: ProT20League = ProT20League.IPL
)

data class CareerMatchResolution(
    val updatedCareer: CareerSaveState,
    val summaryMessage: String,
    val milestoneBanner: String? = null,
    val newlyUnlockedStage: String? = null,
    val newlyUnlockedAchievements: List<CareerAchievementId> = emptyList(),
    val objectivePassed: Boolean = false,
    val retirementPromptTriggered: Boolean = false
)

/**
 * Pure Kotlin engine for Cricket 26-style single-player "Player Career Mode (Road to Legend)".
 * Drives custom player creation (appearance + styles, 55-62 OVR), 5-stage ladder:
 * Hometown Club -> Domestic (First-Class & Limited-Overs) -> Pro T20 Leagues (IPL/PSL/BBL/The Hundred)
 * -> International Debut (T20I / 50-Over ODI / Test / World Cups) -> Team Captaincy -> Hall of Fame.
 */
object CareerEngine {

    private val clubSidesByCountry = mapOf(
        "IND" to listOf("Shivaji Park Club XI", "Maidan Strikers CC", "Gymkhana Colts CC", "Dadar Union CC", "Chepauk Town CC"),
        "AUS" to listOf("Sydney Grade CC", "Melbourne Club XI", "Brisbane Valley CC", "Perth Suburban CC"),
        "ENG" to listOf("Richmond Green CC", "Yorkshire Town CC", "Surrey Village CC", "Bristol Rovers CC"),
        "RSA" to listOf("Wanderers Club XI", "Cape Town CC", "Durban Bay CC", "Pretoria Colts CC"),
        "NZ" to listOf("Eden Park Club XI", "Wellington Harbour CC", "Christchurch Town CC", "Otago Club XI"),
        "WI" to listOf("Bridgetown Club XI", "Port of Spain CC", "Kingston Town CC", "Georgetown CC"),
        "SL" to listOf("Colombo Colts CC", "Kandy Town CC", "Galle Fort CC", "Moratuwa Club XI"),
        "PAK" to listOf("Lahore Gymkhana CC", "Karachi Club XI", "Pindi Colts CC", "Multan Town CC")
    )

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

    fun createRookie(
        name: String,
        startingAge: Int,
        nationality: String,
        countryCode: String,
        avatarId: String,
        role: PlayerRole,
        battingStyle: BattingStyle,
        bowlingStyle: BowlingStyle,
        difficulty: CareerDifficulty = CareerDifficulty.NORMAL,
        appearance: CareerPlayerAppearance = CareerPlayerAppearance()
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
        val clubList = clubSidesByCountry[countryCode] ?: clubSidesByCountry["IND"]!!
        val domesticList = domesticSidesByCountry[countryCode] ?: domesticSidesByCountry["IND"]!!
        val id = "hof_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val baseCareer = CareerSaveState(
            id = id,
            name = cleanName,
            startingAge = clampedAge,
            age = clampedAge,
            season = 1,
            nationality = nationality,
            countryCode = countryCode,
            avatarId = avatarId,
            appearance = appearance,
            role = role,
            battingStyle = battingStyle,
            bowlingStyle = bowlingStyle,
            attributes = attrs,
            skillPoints = 2,
            selectionMeter = 65,
            difficulty = difficulty,
            stage = CareerStage.CLUB_CRICKET,
            clubTeamName = clubList.first(),
            domesticTeamName = domesticList.first(),
            nationalTeamName = nationality,
            peakOverall = attrs.overallFor(role)
        )
        val career = baseCareer.copy(
            currentObjective = generateMatchObjective(baseCareer, CareerMatchType.CLUB_T20)
        )
        CricketerDatabase.registerActiveCareerPlayer(career.toCricketer())
        return career
    }

    fun generateMatchObjective(
        career: CareerSaveState,
        matchType: CareerMatchType
    ): CareerMatchObjective {
        val stageTier = when (career.stage) {
            CareerStage.CLUB_CRICKET -> 0
            CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> 1
            CareerStage.IPL_FRANCHISE -> 2
            CareerStage.INTERNATIONAL, CareerStage.RETIRED -> 3
        }
        val formatRunsScale = when (matchType.format) {
            MatchFormat.T20I -> 0
            MatchFormat.ODI -> 12
            MatchFormat.TEST -> 18
        }
        val maxEcon = when (matchType.format) {
            MatchFormat.T20I -> 8.0 - (stageTier * 0.25)
            MatchFormat.ODI -> 5.8 - (stageTier * 0.2)
            MatchFormat.TEST -> 3.8 - (stageTier * 0.15)
        }
        val bonusSp = if (stageTier >= 2 || matchType.isWorldChampionship) 2 else 1

        return when (career.role) {
            PlayerRole.BATTER -> {
                val targetRuns = (28 + stageTier * 5 + formatRunsScale).coerceAtMost(75)
                CareerMatchObjective(
                    id = "obj_bat_${matchType.name}_${career.matchesPlayed}",
                    title = "Anchor & Score $targetRuns+ Runs",
                    description = "Score at least $targetRuns runs in this ${matchType.shortChip} fixture to boost your selection rating.",
                    targetRuns = targetRuns,
                    rewardSkillPoints = bonusSp,
                    rewardSelectionBonus = 13
                )
            }
            PlayerRole.WICKETKEEPER -> {
                val targetRuns = (24 + stageTier * 4 + formatRunsScale).coerceAtMost(65)
                CareerMatchObjective(
                    id = "obj_wk_${matchType.name}_${career.matchesPlayed}",
                    title = "Keeper-Batter: $targetRuns+ Runs",
                    description = "Contribute $targetRuns+ vital runs in this ${matchType.shortChip} match to keep the gloves.",
                    targetRuns = targetRuns,
                    rewardSkillPoints = bonusSp,
                    rewardSelectionBonus = 13
                )
            }
            PlayerRole.ALL_ROUNDER -> {
                val targetRuns = (22 + stageTier * 4 + (formatRunsScale / 2)).coerceAtMost(55)
                val targetWkts = if (matchType.format == MatchFormat.TEST) 2 else 1
                CareerMatchObjective(
                    id = "obj_ar_${matchType.name}_${career.matchesPlayed}",
                    title = "Dual Impact: $targetRuns+ Runs or $targetWkts+ Wkts",
                    description = "Either score $targetRuns+ runs OR claim $targetWkts+ wicket(s) to impress selectors.",
                    targetRuns = targetRuns,
                    targetWickets = targetWkts,
                    rewardSkillPoints = bonusSp,
                    rewardSelectionBonus = 14
                )
            }
            PlayerRole.SEAMER, PlayerRole.SPINNER -> {
                val targetWkts = if (stageTier >= 2 || matchType.format != MatchFormat.T20I) 2 else 1
                val roundedEcon = (Math.round(maxEcon * 10.0) / 10.0)
                CareerMatchObjective(
                    id = "obj_bwl_${matchType.name}_${career.matchesPlayed}",
                    title = "Strike Spell: $targetWkts+ Wkts or ≤$roundedEcon RPO",
                    description = "Claim $targetWkts+ wicket(s) OR keep economy at or below $roundedEcon RPO.",
                    targetWickets = targetWkts,
                    maxEconomy = roundedEcon,
                    rewardSkillPoints = bonusSp,
                    rewardSelectionBonus = 14
                )
            }
        }
    }

    fun evaluateObjective(
        objective: CareerMatchObjective,
        runs: Int,
        wickets: Int,
        runsConceded: Int,
        legalBallsBowled: Int
    ): Boolean {
        val econPassed = objective.maxEconomy != null && legalBallsBowled >= 6 &&
            ((runsConceded.toDouble() / (legalBallsBowled.toDouble() / 6.0)) <= objective.maxEconomy)
        val runsPassed = objective.targetRuns > 0 && runs >= objective.targetRuns
        val wktsPassed = objective.targetWickets > 0 && wickets >= objective.targetWickets

        return when {
            objective.targetRuns > 0 && objective.targetWickets > 0 -> runsPassed || wktsPassed
            objective.targetWickets > 0 && objective.maxEconomy != null -> wktsPassed || econPassed
            objective.targetRuns > 0 -> runsPassed
            objective.targetWickets > 0 -> wktsPassed
            else -> runs >= 20 || wickets >= 1
        }
    }

    fun spendSkillPoint(
        career: CareerSaveState,
        stat: CareerSkillStat
    ): Pair<CareerSaveState, String> {
        if (career.stage == CareerStage.RETIRED) {
            return career to "Player has already retired to the Hall of Fame."
        }
        if (career.skillPoints <= 0) {
            return career to "No Skill Points available. Complete matches & objectives to earn more!"
        }
        val currentVal = career.attributes.valueOf(stat)
        if (currentVal >= 99) {
            return career to "${stat.label} is already maxed at 99!"
        }
        val upgradedAttrs = career.attributes.upgrade(stat, delta = 2)
        val newPeak = maxOf(career.peakOverall, upgradedAttrs.overallFor(career.role))
        val updated = career.copy(
            attributes = upgradedAttrs,
            skillPoints = (career.skillPoints - 1).coerceAtLeast(0),
            peakOverall = newPeak
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return updated to "${stat.label} upgraded to ${upgradedAttrs.valueOf(stat)} (+2) • OVR ${updated.overallRating}"
    }

    fun runSelectionTrial(
        career: CareerSaveState
    ): Pair<CareerSaveState, String> {
        if (!career.isDropped) {
            return career to "You are already in the Playing XI!"
        }
        val updated = career.copy(
            droppedMatchesRemaining = 0,
            consecutivePoorMatches = 0,
            selectionMeter = maxOf(career.selectionMeter, 50),
            fatigue = (career.fatigue + 12).coerceIn(0, 100),
            form = (career.form + 5).coerceIn(30, 100),
            xp = career.xp + 20
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return updated to "Impressive Net Trial! Selectors recalled ${career.name} to the Playing XI (Selection 50%)."
    }

    fun promoteToDomestic(career: CareerSaveState): CareerSaveState {
        if (career.stage != CareerStage.CLUB_CRICKET) return career
        val awards = (career.awards + "Domestic Call-Up (${career.domesticTeamName})").distinct()
        val promoted = career.copy(
            stage = CareerStage.DOMESTIC,
            reputation = (career.reputation + 8).coerceAtMost(100),
            selectionMeter = maxOf(career.selectionMeter, 66),
            skillPoints = career.skillPoints + 1,
            awards = awards
        )
        val withObjective = promoted.copy(
            currentObjective = generateMatchObjective(promoted, CareerMatchType.DOMESTIC_T20)
        )
        CricketerDatabase.registerActiveCareerPlayer(withObjective.toCricketer())
        return withObjective
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
        var attrs = career.attributes
        var baseXpGain = 0
        val attrGain = diff.drillAttrGain
        val scaledFatigueDelta = if (drill.fatigueDelta > 0) {
            (drill.fatigueDelta * diff.fatigueMultiplier).toInt().coerceAtLeast(6)
        } else {
            when (diff) {
                CareerDifficulty.EASY -> (drill.fatigueDelta - 6)
                CareerDifficulty.NORMAL -> drill.fatigueDelta
                CareerDifficulty.HARD -> (drill.fatigueDelta + 6).coerceAtMost(-24)
                CareerDifficulty.LEGEND -> (drill.fatigueDelta + 10).coerceAtMost(-20)
            }
        }
        val newFatigue = (career.fatigue + scaledFatigueDelta).coerceIn(0, 100)
        var newInjury = career.injuryMatchesRemaining
        var newForm = career.form

        when (drill) {
            TrainingDrill.BATTING_TECHNIQUE -> {
                attrs = attrs.upgrade(CareerSkillStat.BAT_TIMING, attrGain)
                    .upgrade(CareerSkillStat.BAT_CONSISTENCY, attrGain)
                baseXpGain = 30
                newForm = (newForm + 3).coerceAtMost(100)
            }
            TrainingDrill.POWER_HITTING -> {
                attrs = attrs.upgrade(CareerSkillStat.BAT_POWER, attrGain)
                baseXpGain = 35
                newForm = (newForm + 2).coerceAtMost(100)
            }
            TrainingDrill.BOWLING_SKILL -> {
                attrs = attrs.upgrade(CareerSkillStat.BOWL_PACE_SPIN, attrGain)
                    .upgrade(CareerSkillStat.BOWL_ACCURACY, attrGain)
                    .upgrade(CareerSkillStat.BOWL_WICKET_TAKING, attrGain)
                baseXpGain = 35
                newForm = (newForm + 3).coerceAtMost(100)
            }
            TrainingDrill.FIELDING_AGILITY -> {
                attrs = attrs.upgrade(CareerSkillStat.FIELD_CATCHING, attrGain)
                    .upgrade(CareerSkillStat.FIELD_THROWING, attrGain)
                    .upgrade(CareerSkillStat.FIELD_STOPPING, attrGain)
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

        var injuredNow = false
        if (drill != TrainingDrill.FITNESS_RECOVERY && newFatigue >= diff.injuryFatigueThreshold) {
            val baseChance = if (drill == TrainingDrill.POWER_HITTING || drill == TrainingDrill.BOWLING_SKILL) 0.30 else 0.18
            val injuryChance = (baseChance * diff.injuryRiskMultiplier).coerceIn(0.05, 0.72)
            if (rng.nextDouble() < injuryChance) {
                injuredNow = true
                newInjury = if ((diff == CareerDifficulty.HARD || diff == CareerDifficulty.LEGEND) && newFatigue >= 86) 2 else 1
                newForm = (newForm - 6).coerceAtLeast(35)
            }
        }

        val (leveledAttrs, newXp, newLevel, levelsGained) = applyXpAndLevelUps(
            role = career.role,
            attrs = attrs,
            currentXp = career.xp + xpGain,
            currentLevel = career.level
        )
        val newPeak = maxOf(career.peakOverall, leveledAttrs.overallFor(career.role))
        val updated = career.copy(
            attributes = leveledAttrs,
            xp = newXp,
            level = newLevel,
            skillPoints = career.skillPoints + levelsGained,
            fatigue = newFatigue,
            injuryMatchesRemaining = newInjury,
            form = newForm,
            peakOverall = newPeak,
            trainingDoneBeforeMatch = drill != TrainingDrill.FITNESS_RECOVERY
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())

        val msg = buildString {
            append("${drill.title} complete (+$xpGain XP)")
            if (levelsGained > 0) append(" • LEVEL UP Lv.$newLevel (+$levelsGained SP)!")
            if (injuredNow) append(" • Overworked! Minor strain (${newInjury}m — run Recovery).")
            else if (drill == TrainingDrill.FITNESS_RECOVERY) append(" • Fatigue down to $newFatigue%.")
        }
        return TrainingOutcome(updated, msg, injured = injuredNow, levelsGained = levelsGained)
    }

    fun runIplAuctionDraftEvent(
        career: CareerSaveState,
        rng: Random = Random.Default,
        league: ProT20League = ProT20League.IPL
    ): IplAuctionDraftOutcome {
        val ovr = career.overallRating
        val baseLakhs = when {
            ovr >= 75 -> 150L
            ovr >= 66 -> 75L
            else -> 30L
        }
        val bidders = league.franchises.shuffled(rng).take(3)
        val bidSteps = (3 + ((ovr - 55).coerceAtLeast(0) / 4) + (career.reputation / 20)).coerceIn(3, 12)
        var currentBid = baseLakhs
        var highestTeam = bidders.first()
        val highlights = mutableListOf<String>()
        highlights.add("${career.name} enters ${league.displayName} Draft (Base ${CurrencyFormatter.formatLakhs(baseLakhs)})")

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

        val newAwards = (career.awards + "${league.displayName} Drafted: $highestTeam (${CurrencyFormatter.formatLakhs(currentBid)})").distinct()
        val updatedLeagues = (career.signedProLeagues + league.name).distinct()
        val baseUpdated = career.copy(
            stage = CareerStage.IPL_FRANCHISE,
            activeProLeague = league,
            signedProLeagues = updatedLeagues,
            iplTeamName = highestTeam,
            iplBoughtPriceLakhs = currentBid,
            reputation = (career.reputation + 12).coerceAtMost(100),
            selectionMeter = maxOf(career.selectionMeter, 68),
            skillPoints = career.skillPoints + 1,
            form = (career.form + 6).coerceAtMost(100),
            awards = newAwards
        )
        val updated = baseUpdated.copy(
            currentObjective = generateMatchObjective(baseUpdated, league.toMatchType())
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return IplAuctionDraftOutcome(
            updatedCareer = updated,
            winningFranchise = highestTeam,
            finalBidLakhs = currentBid,
            biddingHighlights = highlights,
            league = league
        )
    }

    fun switchProLeague(
        career: CareerSaveState,
        league: ProT20League,
        rng: Random = Random.Default
    ): CareerSaveState {
        if (career.stage != CareerStage.IPL_FRANCHISE) return career
        val team = league.franchises.random(rng)
        val updatedLeagues = (career.signedProLeagues + league.name).distinct()
        val updated = career.copy(
            activeProLeague = league,
            signedProLeagues = updatedLeagues,
            iplTeamName = team,
            currentObjective = generateMatchObjective(career.copy(activeProLeague = league), league.toMatchType())
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())
        return updated
    }

    fun acceptInternationalCallUp(career: CareerSaveState): CareerSaveState {
        val nation = InternationalNations.all.firstOrNull { it.code == career.countryCode }
            ?: InternationalNations.all.first()
        val awards = (career.awards + "International Debut (${nation.name})").distinct()
        val achievements = career.unlockedAchievements.toMutableSet()
        if (career.season <= 3 || career.age <= 21) {
            achievements.add(CareerAchievementId.NATIONAL_HERO.name)
        }
        val baseUpdated = career.copy(
            stage = CareerStage.INTERNATIONAL,
            nationalTeamName = nation.name,
            reputation = (career.reputation + 10).coerceAtMost(100),
            selectionMeter = maxOf(career.selectionMeter, 68),
            skillPoints = career.skillPoints + 2,
            awards = awards,
            unlockedAchievements = achievements.toList()
        )
        val updated = baseUpdated.copy(
            currentObjective = generateMatchObjective(baseUpdated, CareerMatchType.INTL_T20I)
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

        val proLeagueForMatch = when (matchType) {
            CareerMatchType.IPL_T20 -> ProT20League.IPL
            CareerMatchType.PSL_T20 -> ProT20League.PSL
            CareerMatchType.BBL_T20 -> ProT20League.BBL
            CareerMatchType.HUNDRED_T20 -> ProT20League.THE_HUNDRED
            else -> null
        }

        val userTeamName = when {
            matchType == CareerMatchType.CLUB_T20 || matchType == CareerMatchType.CLUB_ONE_DAY || matchType == CareerMatchType.CLUB_FIRST_CLASS ->
                career.clubTeamName
            matchType == CareerMatchType.DOMESTIC_T20 || matchType == CareerMatchType.DOMESTIC_ONE_DAY || matchType == CareerMatchType.DOMESTIC_FIRST_CLASS ->
                career.domesticTeamName
            proLeagueForMatch != null ->
                if (career.activeProLeague == proLeagueForMatch && career.iplTeamName.isNotBlank()) {
                    career.iplTeamName
                } else {
                    proLeagueForMatch.franchises.first()
                }
            else -> career.nationalTeamName
        }

        val opponentName: String
        val myCompanions: List<String>
        val oppPlayerIds: List<String>

        when {
            matchType == CareerMatchType.CLUB_T20 || matchType == CareerMatchType.CLUB_ONE_DAY || matchType == CareerMatchType.CLUB_FIRST_CLASS -> {
                val clubRivals = (clubSidesByCountry[career.countryCode] ?: clubSidesByCountry["IND"]!!)
                    .filter { it != career.clubTeamName }
                opponentName = clubRivals.random(rng)
                val clubPool = CricketerDatabase.allPlayers
                    .filter { it.basePriceLakhs <= 75L }
                    .ifEmpty { CricketerDatabase.allPlayers }
                    .shuffled(rng)
                myCompanions = pickBalancedTenCompanions(clubPool, career.role)
                oppPlayerIds = pickBalancedEleven(clubPool.filter { it.id !in myCompanions })
            }
            matchType == CareerMatchType.DOMESTIC_T20 || matchType == CareerMatchType.DOMESTIC_ONE_DAY || matchType == CareerMatchType.DOMESTIC_FIRST_CLASS -> {
                val domesticRivals = (domesticSidesByCountry[career.countryCode] ?: domesticSidesByCountry["IND"]!!)
                    .filter { it != career.domesticTeamName }
                opponentName = domesticRivals.random(rng)
                val emergingPool = CricketerDatabase.allPlayers
                    .filter { it.basePriceLakhs <= 100L }
                    .shuffled(rng)
                myCompanions = pickBalancedTenCompanions(emergingPool, career.role)
                oppPlayerIds = pickBalancedEleven(emergingPool.filter { it.id !in myCompanions })
            }
            proLeagueForMatch != null -> {
                val rivals = proLeagueForMatch.franchises.filter { it != userTeamName }
                opponentName = rivals.random(rng)
                val proPool = CricketerDatabase.allPlayers.shuffled(rng)
                myCompanions = pickBalancedTenCompanions(proPool, career.role)
                oppPlayerIds = pickBalancedEleven(proPool.filter { it.id !in myCompanions })
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
        val captainId = if (career.isInternationalCaptain) {
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
     * and applies Match Objectives, Selection Meter, Skill Points, Achievements, XP, fatigue,
     * captaincy, season aging, and promotion unlock checks.
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
        val legalBallsBowled = userBowlingCard?.legalBalls ?: 0
        val isPotm = match.playerOfTheMatch.startsWith(career.name, ignoreCase = true)

        val hitFifty = runs in 50..99
        val hitCentury = runs >= 100
        val tookThreeFor = wickets in 3..4
        val tookFiveFor = wickets >= 5

        // Evaluate Match Objective & Team Selection Meter
        val activeObj = career.currentObjective ?: generateMatchObjective(career, matchType)
        val objPassed = evaluateObjective(activeObj, runs, wickets, runsConceded, legalBallsBowled)
        val poorPerformance = !objPassed && runs < 12 && wickets == 0

        val selectionDelta = when {
            objPassed && (hitFifty || hitCentury || tookThreeFor || tookFiveFor || isPotm) -> activeObj.rewardSelectionBonus + 5
            objPassed -> activeObj.rewardSelectionBonus
            poorPerformance -> if (career.difficulty == CareerDifficulty.HARD || career.difficulty == CareerDifficulty.LEGEND) -16 else -12
            else -> -6
        }
        val newSelectionMeter = (career.selectionMeter + selectionDelta).coerceIn(0, 100)
        val newPoorStreak = if (poorPerformance) career.consecutivePoorMatches + 1 else 0
        var newDroppedMatches = career.droppedMatchesRemaining
        var droppedJustNow = false
        if (newSelectionMeter <= 22 || newPoorStreak >= 3) {
            newDroppedMatches = 1
            droppedJustNow = true
        }

        // XP + Performance Rating Boosts scaled by CareerDifficulty
        val diff = career.difficulty
        var rawXp = 35
        if (userWon) rawXp += 20
        if (objPassed) rawXp += 30
        rawXp += (runs * 1.4).toInt()
        rawXp += wickets * 28
        if (hitFifty) rawXp += 35
        if (hitCentury) rawXp += 85
        if (tookThreeFor) rawXp += 35
        if (tookFiveFor) rawXp += 80
        if (isPotm) rawXp += 55
        val xpEarned = (rawXp * diff.xpMultiplier).toInt().coerceAtLeast(18)

        var attrs = career.attributes
        if (runs >= 50) attrs = attrs.upgrade(CareerSkillStat.BAT_TIMING, 1)
        if (runs >= 100 || (userBattingCard?.sixes ?: 0) >= 3) attrs = attrs.upgrade(CareerSkillStat.BAT_POWER, 1)
        if (wickets >= 3) attrs = attrs.upgrade(CareerSkillStat.BOWL_WICKET_TAKING, 1)
        if (wickets >= 5) attrs = attrs.upgrade(CareerSkillStat.BOWL_ACCURACY, 1)
        if (isPotm) attrs = attrs.upgrade(CareerSkillStat.FIELD_CATCHING, 1)

        val (leveledAttrs, newXp, newLevel, levelsGained) = applyXpAndLevelUps(
            role = career.role,
            attrs = attrs,
            currentXp = career.xp + xpEarned,
            currentLevel = career.level
        )

        // Skill progression points earned
        var earnedSkillPoints = 1 + levelsGained
        if (objPassed) earnedSkillPoints += activeObj.rewardSkillPoints
        if (hitFifty || hitCentury || tookThreeFor || tookFiveFor || isPotm) earnedSkillPoints += 1

        val repGain = buildRepGain(userWon, runs, wickets, isPotm, matchType, objPassed)
        val newRep = (career.reputation + repGain).coerceIn(0, 100)
        val formDelta = when {
            hitCentury || tookFiveFor || isPotm -> 10
            hitFifty || tookThreeFor || objPassed -> 6
            runs >= 25 || wickets >= 1 || userWon -> 2
            else -> if (diff == CareerDifficulty.HARD || diff == CareerDifficulty.LEGEND) -6 else -4
        }
        val newForm = (career.form + formDelta).coerceIn(30, 100)

        val baseFatigueIncrease = when (matchType.format) {
            MatchFormat.T20I -> 15
            MatchFormat.ODI -> 20
            MatchFormat.TEST -> 26
        }
        val fatigueIncrease = (baseFatigueIncrease * diff.fatigueMultiplier).toInt().coerceAtLeast(10)
        val newFatigue = (career.fatigue + fatigueIncrease).coerceIn(0, 100)
        var newInjury = (career.injuryMatchesRemaining - 1).coerceAtLeast(0)
        var injuredInMatch = false
        val matchInjuryThreshold = (diff.injuryFatigueThreshold + 8).coerceAtMost(94)
        val matchInjuryChance = (0.25 * diff.injuryRiskMultiplier).coerceIn(0.08, 0.55)
        if (newFatigue >= matchInjuryThreshold && rng.nextDouble() < matchInjuryChance) {
            newInjury = if ((diff == CareerDifficulty.HARD || diff == CareerDifficulty.LEGEND) && newFatigue >= 90) 2 else 1
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

        if (objPassed) {
            milestoneParts.add("Objective Met (+${activeObj.rewardSkillPoints} SP)")
        }
        if (hitCentury) {
            milestoneParts.add("Century ($runs)")
            if ("Maiden Century" !in awards) awards.add("Maiden Century")
        } else if (hitFifty) {
            milestoneParts.add("Half-Century ($runs)")
        }
        if (tookFiveFor) {
            milestoneParts.add("5-Wicket Haul ($wickets/$runsConceded)")
            if ("5-Wicket Haul" !in awards) awards.add("5-Wicket Haul")
        } else if (tookThreeFor) {
            milestoneParts.add("$wickets Wickets")
        }
        if (isPotm) {
            milestoneParts.add("Player of the Match")
        }
        if (userWon && matchType.isWorldChampionship) {
            wcTrophies += 1
            val wcLabel = if (matchType == CareerMatchType.ODI_WORLD_CUP) "50-Over World Champion" else "T20 World Champion"
            milestoneParts.add("$wcLabel!")
            if (wcLabel !in awards) awards.add(wcLabel)
        } else if (seasonCompleted && userWon) {
            when (career.stage) {
                CareerStage.CLUB_CRICKET -> {
                    domTrophies += 1
                    milestoneParts.add("Club Shield Champion!")
                    if ("Club Champion" !in awards) awards.add("Club Champion")
                }
                CareerStage.DOMESTIC -> {
                    domTrophies += 1
                    milestoneParts.add("Domestic Trophy Won!")
                    if ("Domestic Champion" !in awards) awards.add("Domestic Champion")
                }
                CareerStage.IPL_FRANCHISE -> {
                    iplTrophies += 1
                    milestoneParts.add("${career.activeProLeague.displayName} Champion!")
                    if ("${career.activeProLeague.displayName} Champion" !in awards) {
                        awards.add("${career.activeProLeague.displayName} Champion")
                    }
                }
                CareerStage.INTERNATIONAL -> {
                    seriesWins += 1
                    milestoneParts.add("International Series Win!")
                }
                else -> {}
            }
        }

        // Team Captaincy check in International stage
        var becameCaptain = career.isInternationalCaptain
        var newlyUnlocked: String? = null
        if (career.stage == CareerStage.INTERNATIONAL && !becameCaptain && newRep >= 76 && leveledAttrs.overallFor(career.role) >= 75) {
            becameCaptain = true
            newlyUnlocked = "Appointed Team Captain (C) of ${career.nationalTeamName}!"
            if ("National Captain (C)" !in awards) awards.add("National Captain (C)")
        }

        // Achievements Evaluation (all 6 Career Achievements)
        val existingAchievements = career.unlockedAchievements.toMutableSet()
        val newlyEarnedAchievements = mutableListOf<CareerAchievementId>()
        fun grant(id: CareerAchievementId) {
            if (existingAchievements.add(id.name)) {
                newlyEarnedAchievements.add(id)
                if (id.title !in awards) awards.add("Badge: ${id.title}")
            }
        }
        grant(CareerAchievementId.FIRST_STEPS)
        if (runs >= 50 || wickets >= 3) grant(CareerAchievementId.RISING_STAR)
        if (becameCaptain) grant(CareerAchievementId.CAPTAINCY_CALL)
        if (career.stage == CareerStage.INTERNATIONAL && (career.season <= 3 || career.age <= 21)) {
            grant(CareerAchievementId.NATIONAL_HERO)
        }
        if (wcTrophies >= 1) grant(CareerAchievementId.WORLD_CHAMPION)
        if (runs >= 75 && wickets >= 3) grant(CareerAchievementId.ALL_ROUNDER)

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
            xpEarned = xpEarned,
            objectivePassed = objPassed
        )

        val newPeak = maxOf(career.peakOverall, leveledAttrs.overallFor(career.role))
        val preObjectiveUpdated = career.copy(
            age = newAge,
            season = newSeason,
            attributes = leveledAttrs,
            xp = newXp,
            level = newLevel,
            skillPoints = career.skillPoints + earnedSkillPoints,
            reputation = newRep,
            selectionMeter = newSelectionMeter,
            droppedMatchesRemaining = newDroppedMatches,
            consecutivePoorMatches = newPoorStreak,
            objectivesCompleted = career.objectivesCompleted + if (objPassed) 1 else 0,
            unlockedAchievements = existingAchievements.toList(),
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
        val updated = preObjectiveUpdated.copy(
            currentObjective = generateMatchObjective(preObjectiveUpdated, matchType)
        )
        CricketerDatabase.registerActiveCareerPlayer(updated.toCricketer())

        if (newlyUnlocked == null) {
            newlyUnlocked = when {
                droppedJustNow -> "Dropped from the XI! Complete a Selection Net Trial to regain your place."
                updated.canPromoteToDomestic && !career.canPromoteToDomestic ->
                    "Domestic Call-Up Unlocked! Graduate from Club Cricket to Domestic First-Class & Limited-Overs."
                updated.canEnterIplAuction && !career.canEnterIplAuction ->
                    "Pro T20 Draft Unlocked! Enter the IPL, PSL, BBL & The Hundred Auction Pool."
                updated.canEarnInternationalCallUp && !career.canEarnInternationalCallUp ->
                    "International Call-Up Earned! Debut for ${updated.nationalTeamName}."
                newlyEarnedAchievements.isNotEmpty() ->
                    "Achievement Unlocked: ${newlyEarnedAchievements.joinToString(", ") { it.title }}!"
                else -> null
            }
        }

        val retirementTriggered = updated.age >= 36
        val summary = buildString {
            append("${if (userWon) "WIN" else "LOSS"} vs ${match.teamB.teamName}: ")
            append("$runs($balls)")
            if (wickets > 0) append(" & $wickets/$runsConceded")
            append(" • +$xpEarned XP • +$earnedSkillPoints SP")
            if (objPassed) append(" • Obj OK")
            if (levelsGained > 0) append(" • Lv.$newLevel!")
            if (injuredInMatch) append(" • Strain!")
        }

        return CareerMatchResolution(
            updatedCareer = updated,
            summaryMessage = summary,
            milestoneBanner = milestoneParts.takeIf { it.isNotEmpty() }?.joinToString(" • "),
            newlyUnlockedStage = newlyUnlocked,
            newlyUnlockedAchievements = newlyEarnedAchievements,
            objectivePassed = objPassed,
            retirementPromptTriggered = retirementTriggered
        )
    }

    fun retireToHallOfFame(career: CareerSaveState): Pair<CareerSaveState, HallOfFameLegend> {
        val peakBoost = (career.peakOverall - career.overallRating).coerceAtLeast(0)
        val finalAwards = when (career.difficulty) {
            CareerDifficulty.HARD -> (career.awards + "Hard Mode Veteran").distinct()
            CareerDifficulty.LEGEND -> (career.awards + "Legend Mode Immortal").distinct()
            else -> career.awards
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
            includeInAuctions = true,
            appearance = career.appearance,
            achievementsCount = career.unlockedAchievements.size
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
        var updatedAttrs = attrs

        while (xp >= 100 + (level - 1) * 35 && level < 50) {
            xp -= 100 + (level - 1) * 35
            level++
            gained++
            updatedAttrs = when (role) {
                PlayerRole.BATTER -> updatedAttrs
                    .upgrade(CareerSkillStat.BAT_TIMING, 2)
                    .upgrade(CareerSkillStat.BAT_POWER, 2)
                    .upgrade(CareerSkillStat.BAT_CONSISTENCY, 1)
                    .upgrade(CareerSkillStat.FIELD_CATCHING, 1)
                PlayerRole.WICKETKEEPER -> updatedAttrs
                    .upgrade(CareerSkillStat.BAT_TIMING, 2)
                    .upgrade(CareerSkillStat.BAT_CONSISTENCY, 1)
                    .upgrade(CareerSkillStat.FIELD_CATCHING, 2)
                    .upgrade(CareerSkillStat.FIELD_STOPPING, 1)
                PlayerRole.ALL_ROUNDER -> updatedAttrs
                    .upgrade(CareerSkillStat.BAT_POWER, 2)
                    .upgrade(CareerSkillStat.BAT_TIMING, 1)
                    .upgrade(CareerSkillStat.BOWL_PACE_SPIN, 2)
                    .upgrade(CareerSkillStat.BOWL_ACCURACY, 1)
                    .upgrade(CareerSkillStat.FIELD_THROWING, 1)
                PlayerRole.SEAMER, PlayerRole.SPINNER -> updatedAttrs
                    .upgrade(CareerSkillStat.BOWL_PACE_SPIN, 2)
                    .upgrade(CareerSkillStat.BOWL_ACCURACY, 2)
                    .upgrade(CareerSkillStat.BOWL_WICKET_TAKING, 1)
                    .upgrade(CareerSkillStat.FIELD_STOPPING, 1)
            }
        }
        return LevelUpResult(
            attributes = updatedAttrs,
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
        matchType: CareerMatchType,
        objectivePassed: Boolean
    ): Int {
        var gain = if (won) 4 else 1
        if (objectivePassed) gain += 3
        if (runs >= 30) gain += 2
        if (runs >= 50) gain += 3
        if (runs >= 100) gain += 6
        if (wickets >= 2) gain += 3
        if (wickets >= 4) gain += 5
        if (potm) gain += 5
        if (matchType.isWorldChampionship && won) gain += 8
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
            PlayerRole.BATTER -> 2
            PlayerRole.WICKETKEEPER -> 3
            PlayerRole.ALL_ROUNDER -> 4
            PlayerRole.SEAMER, PlayerRole.SPINNER -> 7
        }.coerceIn(0, orderedCompanions.size)
        orderedCompanions.add(targetSlot, myCricketer.id)
        return orderedCompanions.take(11)
    }
}
