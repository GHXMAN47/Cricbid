package com.example.data.model

import com.example.ui.components.IndianPlayerAvatars

enum class CareerDifficulty(
    val label: String,
    val shortBadge: String,
    val summary: String,
    val opponentRatingOffset: Int,
    val xpMultiplier: Double,
    val drillAttrGain: Int,
    val fatigueMultiplier: Double,
    val injuryRiskMultiplier: Double,
    val injuryFatigueThreshold: Int
) {
    EASY(
        label = "Easy",
        shortBadge = "EASY",
        summary = "Forgiving AI (-7 OVR) • 1.25× XP • +3 Drill Boost • Low Injury Risk",
        opponentRatingOffset = -7,
        xpMultiplier = 1.25,
        drillAttrGain = 3,
        fatigueMultiplier = 0.80,
        injuryRiskMultiplier = 0.45,
        injuryFatigueThreshold = 86
    ),
    NORMAL(
        label = "Normal",
        shortBadge = "NORMAL",
        summary = "Balanced AI • 1.0× XP • +2 Drill Boost • Standard Fatigue & Injury",
        opponentRatingOffset = 0,
        xpMultiplier = 1.00,
        drillAttrGain = 2,
        fatigueMultiplier = 1.00,
        injuryRiskMultiplier = 1.00,
        injuryFatigueThreshold = 78
    ),
    HARD(
        label = "Hard",
        shortBadge = "HARD",
        summary = "Tough AI (+6 OVR) • 0.78× Earned XP • +1 Drill • Strict Fatigue & Injury",
        opponentRatingOffset = 6,
        xpMultiplier = 0.78,
        drillAttrGain = 1,
        fatigueMultiplier = 1.25,
        injuryRiskMultiplier = 1.65,
        injuryFatigueThreshold = 68
    ),
    LEGEND(
        label = "Legend",
        shortBadge = "LEGEND",
        summary = "Elite AI (+10 OVR) • 0.65× XP • +1 Drill • Brutal Fitness & Injury Test",
        opponentRatingOffset = 10,
        xpMultiplier = 0.65,
        drillAttrGain = 1,
        fatigueMultiplier = 1.38,
        injuryRiskMultiplier = 1.95,
        injuryFatigueThreshold = 62
    );

    companion object {
        fun fromNameSafe(raw: String?): CareerDifficulty =
            entries.firstOrNull { it.name == raw } ?: NORMAL
    }
}

enum class HairstylePreset(val label: String) {
    SHORT_FADE("Short Fade"),
    SPIKY_MODERN("Spiky"),
    CLASSIC_PART("Classic Part"),
    LONG_FLOWING("Long Hair"),
    BUZZ_CUT("Buzz Cut");

    companion object {
        fun fromNameSafe(raw: String?): HairstylePreset =
            entries.firstOrNull { it.name == raw } ?: SHORT_FADE
    }
}

enum class FacialFeaturePreset(val label: String) {
    CLEAN_SHAVEN("Clean Shaven"),
    LIGHT_STUBBLE("Light Stubble"),
    FULL_BEARD("Full Beard"),
    MUSTACHE("Mustache");

    companion object {
        fun fromNameSafe(raw: String?): FacialFeaturePreset =
            entries.firstOrNull { it.name == raw } ?: LIGHT_STUBBLE
    }
}

enum class KitThemePreset(
    val label: String,
    val primaryArgb: Long,
    val accentArgb: Long,
    val trimArgb: Long
) {
    ROYAL_BLUE("Royal Blue", 0xFF1565C0, 0xFFF5C451, 0xFF64B5F6),
    EMERALD_TURF("Emerald Turf", 0xFF1B8A5A, 0xFFF5C451, 0xFF69F0AE),
    CRIMSON_GOLD("Crimson Gold", 0xFFB71C1C, 0xFFFFD54F, 0xFFFF8A80),
    SUNSET_ORANGE("Sunset Orange", 0xFFE65100, 0xFF212121, 0xFFFFCC80),
    CLASSIC_WHITES("Test Whites", 0xFFE8EEF5, 0xFF1B5E20, 0xFFB0BEC5),
    MIDNIGHT_NEON("Midnight Neon", 0xFF283593, 0xFF00E5FF, 0xFFCE93D8);

    companion object {
        fun fromNameSafe(raw: String?): KitThemePreset =
            entries.firstOrNull { it.name == raw } ?: ROYAL_BLUE
    }
}

enum class BatEquipmentPreset(
    val label: String,
    val bladeArgb: Long,
    val stickerArgb: Long,
    val gripArgb: Long
) {
    ENGLISH_WILLOW("English Willow", 0xFFEAD2AC, 0xFF1565C0, 0xFF263238),
    NEON_POWER("Neon Power", 0xFFF2DFC1, 0xFF00E676, 0xFF00BFA5),
    GOLD_PRO("Gold Edition", 0xFFF5E1B9, 0xFFFFC107, 0xFFB8860B),
    CARBON_STEALTH("Carbon Stealth", 0xFF37474F, 0xFFFF5252, 0xFF111722);

    companion object {
        fun fromNameSafe(raw: String?): BatEquipmentPreset =
            entries.firstOrNull { it.name == raw } ?: ENGLISH_WILLOW
    }
}

data class CareerPlayerAppearance(
    val hairstyle: HairstylePreset = HairstylePreset.SHORT_FADE,
    val facialFeature: FacialFeaturePreset = FacialFeaturePreset.LIGHT_STUBBLE,
    val kitTheme: KitThemePreset = KitThemePreset.ROYAL_BLUE,
    val batStyle: BatEquipmentPreset = BatEquipmentPreset.ENGLISH_WILLOW,
    val jerseyNumber: Int = 18
)

enum class ProT20League(
    val shortCode: String,
    val displayName: String,
    val franchises: List<String>
) {
    IPL(
        shortCode = "IPL",
        displayName = "IPL",
        franchises = listOf(
            "Mumbai Mavericks",
            "Chennai Super Chargers",
            "Bengaluru Challengers",
            "Kolkata Knights",
            "Hyderabad Sunrisers",
            "Rajasthan Royals XI",
            "Delhi Capitals XI",
            "Gujarat Titans XI"
        )
    ),
    PSL(
        shortCode = "PSL",
        displayName = "PSL",
        franchises = listOf(
            "Lahore Qalandars XI",
            "Karachi Kings XI",
            "Islamabad United XI",
            "Multan Sultans XI",
            "Peshawar Zalmi XI",
            "Quetta Gladiators XI"
        )
    ),
    BBL(
        shortCode = "BBL",
        displayName = "BBL",
        franchises = listOf(
            "Sydney Sixers XI",
            "Melbourne Stars XI",
            "Perth Scorchers XI",
            "Brisbane Heat XI",
            "Adelaide Strikers XI",
            "Hobart Hurricanes XI"
        )
    ),
    THE_HUNDRED(
        shortCode = "100",
        displayName = "The Hundred",
        franchises = listOf(
            "Oval Invincibles XI",
            "London Spirit XI",
            "Trent Rockets XI",
            "Manchester Originals XI",
            "Southern Brave XI",
            "Birmingham Phoenix XI"
        )
    );

    fun toMatchType(): CareerMatchType = when (this) {
        IPL -> CareerMatchType.IPL_T20
        PSL -> CareerMatchType.PSL_T20
        BBL -> CareerMatchType.BBL_T20
        THE_HUNDRED -> CareerMatchType.HUNDRED_T20
    }

    companion object {
        fun fromNameSafe(raw: String?): ProT20League =
            entries.firstOrNull { it.name == raw } ?: IPL
    }
}

enum class CareerStage(val label: String, val stepNumber: Int) {
    CLUB_CRICKET("Stage 1 • Hometown Club", 1),
    DOMESTIC("Stage 2 • Domestic Cricket", 2),
    IPL_AUCTION("Stage 3 • Pro T20 Draft", 3),
    IPL_FRANCHISE("Stage 3 • Pro T20 Leagues", 3),
    INTERNATIONAL("Stage 4 • International Arena", 4),
    RETIRED("Hall of Fame Legend", 5)
}

enum class BattingStyle(val label: String) {
    RIGHT_HAND_BAT("Right-Hand Bat"),
    LEFT_HAND_BAT("Left-Hand Bat")
}

enum class BowlingStyle(val label: String) {
    RIGHT_ARM_FAST("Right-Arm Fast"),
    LEFT_ARM_FAST("Left-Arm Fast"),
    RIGHT_ARM_OFFSPIN("Right-Arm Off-Spin"),
    RIGHT_ARM_LEGSPIN("Right-Arm Leg-Spin"),
    LEFT_ARM_ORTHODOX("Left-Arm Orthodox"),
    OCCASIONAL_MEDIUM("Part-Time Medium")
}

enum class TrainingDrill(
    val title: String,
    val subtitle: String,
    val fatigueDelta: Int,
    val riskLabel: String
) {
    BATTING_TECHNIQUE(
        title = "Batting Technique",
        subtitle = "+Timing & Consistency • +30 XP",
        fatigueDelta = 16,
        riskLabel = "Low Risk"
    ),
    POWER_HITTING(
        title = "Power Hitting",
        subtitle = "+Six-Hitting Power • +35 XP",
        fatigueDelta = 22,
        riskLabel = "Med Risk"
    ),
    BOWLING_SKILL(
        title = "Bowling Craft",
        subtitle = "+Pace / Spin / Accuracy • +35 XP",
        fatigueDelta = 20,
        riskLabel = "Med Risk"
    ),
    FIELDING_AGILITY(
        title = "Fielding & Reflex",
        subtitle = "+Catching, Throwing & Stopping • +28 XP",
        fatigueDelta = 14,
        riskLabel = "Low Risk"
    ),
    FITNESS_RECOVERY(
        title = "Fitness & Recovery",
        subtitle = "-38 Fatigue • Heals Strain • +15 XP",
        fatigueDelta = -38,
        riskLabel = "Safe"
    )
}

enum class CareerMatchType(
    val label: String,
    val format: MatchFormat,
    val stage: MatchStage,
    val shortChip: String = label
) {
    CLUB_T20("Club T20 Bash", MatchFormat.T20I, MatchStage.LEAGUE, "T20"),
    CLUB_ONE_DAY("Club 50-Over One-Day", MatchFormat.ODI, MatchStage.LEAGUE, "50-Over"),
    CLUB_FIRST_CLASS("Club First-Class", MatchFormat.TEST, MatchStage.LEAGUE, "First-Class"),
    DOMESTIC_T20("Domestic T20 Trophy", MatchFormat.T20I, MatchStage.LEAGUE, "Dom T20"),
    DOMESTIC_ONE_DAY("Domestic 50-Over Cup", MatchFormat.ODI, MatchStage.LEAGUE, "50-Over"),
    DOMESTIC_FIRST_CLASS("Domestic First-Class", MatchFormat.TEST, MatchStage.LEAGUE, "First-Class"),
    IPL_T20("IPL Franchise T20", MatchFormat.T20I, MatchStage.LEAGUE, "IPL"),
    PSL_T20("PSL Franchise T20", MatchFormat.T20I, MatchStage.LEAGUE, "PSL"),
    BBL_T20("BBL Franchise T20", MatchFormat.T20I, MatchStage.LEAGUE, "BBL"),
    HUNDRED_T20("The Hundred Clash", MatchFormat.T20I, MatchStage.LEAGUE, "The Hundred"),
    INTL_T20I("International T20I", MatchFormat.T20I, MatchStage.SERIES, "T20I"),
    INTL_ODI("International 50-Over ODI", MatchFormat.ODI, MatchStage.SERIES, "ODI"),
    INTL_TEST("International Test", MatchFormat.TEST, MatchStage.SERIES, "Test"),
    WORLD_CUP("T20 World Championship", MatchFormat.T20I, MatchStage.FINAL, "T20 WC"),
    ODI_WORLD_CUP("50-Over World Championship", MatchFormat.ODI, MatchStage.FINAL, "ODI WC");

    val isWorldChampionship: Boolean
        get() = this == WORLD_CUP || this == ODI_WORLD_CUP

    val isProLeague: Boolean
        get() = this == IPL_T20 || this == PSL_T20 || this == BBL_T20 || this == HUNDRED_T20
}

enum class CareerSkillCategory(val label: String) {
    BATTING("Batting"),
    BOWLING("Bowling"),
    FIELDING("Fielding")
}

enum class CareerSkillStat(
    val label: String,
    val description: String,
    val category: CareerSkillCategory
) {
    BAT_POWER("Power", "Boundary clearing & six-hitting distance", CareerSkillCategory.BATTING),
    BAT_TIMING("Timing", "Sweet-spot window & crisp stroke placement", CareerSkillCategory.BATTING),
    BAT_CONSISTENCY("Consistency", "Strike rotation & surviving tough spells", CareerSkillCategory.BATTING),
    BOWL_PACE_SPIN("Pace / Spin", "Raw speed, seam bite, or sharp turn", CareerSkillCategory.BOWLING),
    BOWL_ACCURACY("Accuracy", "Pinpoint yorkers & tight economy lines", CareerSkillCategory.BOWLING),
    BOWL_WICKET_TAKING("Wicket-Taking", "Breakthrough edges, swing & deception", CareerSkillCategory.BOWLING),
    FIELD_CATCHING("Catching", "Safe hands in slips, ring & deep boundary", CareerSkillCategory.FIELDING),
    FIELD_THROWING("Throwing", "Rocket arm & direct-hit run-out accuracy", CareerSkillCategory.FIELDING),
    FIELD_STOPPING("Stopping", "Diving saves that cut off boundaries", CareerSkillCategory.FIELDING)
}

data class CareerPlayerAttributes(
    val battingTechnique: Int,
    val powerHitting: Int,
    val bowlingSkill: Int,
    val fielding: Int,
    val battingPower: Int = powerHitting,
    val battingTiming: Int = battingTechnique,
    val battingConsistency: Int = battingTechnique,
    val bowlingPaceOrSpin: Int = bowlingSkill,
    val bowlingAccuracy: Int = bowlingSkill,
    val bowlingWicketTaking: Int = bowlingSkill,
    val fieldingCatching: Int = fielding,
    val fieldingThrowing: Int = fielding,
    val fieldingStopping: Int = fielding
) {
    val battingRating: Int
        get() = ((battingPower * 0.34) + (battingTiming * 0.34) + (battingConsistency * 0.32)).toInt().coerceIn(25, 99)

    val bowlingRating: Int
        get() = ((bowlingPaceOrSpin * 0.34) + (bowlingAccuracy * 0.33) + (bowlingWicketTaking * 0.33)).toInt().coerceIn(15, 99)

    val fieldingRating: Int
        get() = ((fieldingCatching * 0.35) + (fieldingThrowing * 0.32) + (fieldingStopping * 0.33)).toInt().coerceIn(45, 99)

    fun valueOf(stat: CareerSkillStat): Int = when (stat) {
        CareerSkillStat.BAT_POWER -> battingPower
        CareerSkillStat.BAT_TIMING -> battingTiming
        CareerSkillStat.BAT_CONSISTENCY -> battingConsistency
        CareerSkillStat.BOWL_PACE_SPIN -> bowlingPaceOrSpin
        CareerSkillStat.BOWL_ACCURACY -> bowlingAccuracy
        CareerSkillStat.BOWL_WICKET_TAKING -> bowlingWicketTaking
        CareerSkillStat.FIELD_CATCHING -> fieldingCatching
        CareerSkillStat.FIELD_THROWING -> fieldingThrowing
        CareerSkillStat.FIELD_STOPPING -> fieldingStopping
    }

    fun upgrade(stat: CareerSkillStat, delta: Int = 2): CareerPlayerAttributes {
        val next = when (stat) {
            CareerSkillStat.BAT_POWER -> copy(
                battingPower = (battingPower + delta).coerceIn(20, 99),
                powerHitting = (powerHitting + delta).coerceIn(20, 99)
            )
            CareerSkillStat.BAT_TIMING -> copy(
                battingTiming = (battingTiming + delta).coerceIn(20, 99),
                battingTechnique = (((battingTiming + delta).coerceIn(20, 99) + battingConsistency) / 2).coerceIn(20, 99)
            )
            CareerSkillStat.BAT_CONSISTENCY -> copy(
                battingConsistency = (battingConsistency + delta).coerceIn(20, 99),
                battingTechnique = ((battingTiming + (battingConsistency + delta).coerceIn(20, 99)) / 2).coerceIn(20, 99)
            )
            CareerSkillStat.BOWL_PACE_SPIN -> copy(bowlingPaceOrSpin = (bowlingPaceOrSpin + delta).coerceIn(15, 99))
            CareerSkillStat.BOWL_ACCURACY -> copy(bowlingAccuracy = (bowlingAccuracy + delta).coerceIn(15, 99))
            CareerSkillStat.BOWL_WICKET_TAKING -> copy(bowlingWicketTaking = (bowlingWicketTaking + delta).coerceIn(15, 99))
            CareerSkillStat.FIELD_CATCHING -> copy(fieldingCatching = (fieldingCatching + delta).coerceIn(40, 99))
            CareerSkillStat.FIELD_THROWING -> copy(fieldingThrowing = (fieldingThrowing + delta).coerceIn(40, 99))
            CareerSkillStat.FIELD_STOPPING -> copy(fieldingStopping = (fieldingStopping + delta).coerceIn(40, 99))
        }
        return next.copy(
            battingTechnique = ((next.battingTiming + next.battingConsistency) / 2).coerceIn(20, 99),
            powerHitting = next.battingPower.coerceIn(20, 99),
            bowlingSkill = next.bowlingRating,
            fielding = next.fieldingRating
        )
    }

    fun overallFor(role: PlayerRole): Int = when (role) {
        PlayerRole.BATTER -> (battingRating * 0.8 + fieldingRating * 0.2).toInt()
        PlayerRole.WICKETKEEPER -> (battingRating * 0.7 + fieldingRating * 0.3).toInt()
        PlayerRole.ALL_ROUNDER -> (battingRating * 0.45 + bowlingRating * 0.45 + fieldingRating * 0.1).toInt()
        PlayerRole.SEAMER, PlayerRole.SPINNER -> (bowlingRating * 0.8 + fieldingRating * 0.1 + battingRating * 0.1).toInt()
    }.coerceIn(40, 99)
}

data class CareerMatchObjective(
    val id: String,
    val title: String,
    val description: String,
    val targetRuns: Int = 0,
    val targetWickets: Int = 0,
    val maxEconomy: Double? = null,
    val rewardSkillPoints: Int = 1,
    val rewardSelectionBonus: Int = 12
)

enum class CareerAchievementId(
    val title: String,
    val description: String,
    val badgeTag: String
) {
    FIRST_STEPS(
        title = "First Steps",
        description = "Play your debut career match",
        badgeTag = "DEBUT"
    ),
    RISING_STAR(
        title = "Rising Star",
        description = "Score 50+ runs or take 3+ wickets in a match",
        badgeTag = "50R / 3W"
    ),
    CAPTAINCY_CALL(
        title = "Captaincy Call",
        description = "Earn appointment as Team Captain (C)",
        badgeTag = "CAPTAIN"
    ),
    NATIONAL_HERO(
        title = "National Hero",
        description = "Earn National Team selection as a rookie (Season ≤ 3 or Age ≤ 21)",
        badgeTag = "ROOKIE CAP"
    ),
    WORLD_CHAMPION(
        title = "World Champion",
        description = "Win a 50-Over or T20 World Championship",
        badgeTag = "WORLD CUP"
    ),
    ALL_ROUNDER(
        title = "All-Rounder",
        description = "Score 75+ runs and take 3+ wickets in one match",
        badgeTag = "75R + 3W"
    )
}

data class CareerMatchLog(
    val season: Int,
    val matchTypeLabel: String,
    val opponent: String,
    val runs: Int,
    val balls: Int,
    val wickets: Int,
    val runsConceded: Int,
    val teamWon: Boolean,
    val potm: Boolean,
    val milestoneNote: String = "",
    val xpEarned: Int = 0,
    val objectivePassed: Boolean? = null
)

data class CareerSaveState(
    val id: String,
    val name: String,
    val startingAge: Int = 18,
    val age: Int = 18,
    val season: Int = 1,
    val nationality: String = "India",
    val countryCode: String = "IND",
    val avatarId: String = IndianPlayerAvatars.DEFAULT_ID,
    val appearance: CareerPlayerAppearance = CareerPlayerAppearance(),
    val role: PlayerRole = PlayerRole.BATTER,
    val battingStyle: BattingStyle = BattingStyle.RIGHT_HAND_BAT,
    val bowlingStyle: BowlingStyle = BowlingStyle.OCCASIONAL_MEDIUM,
    val attributes: CareerPlayerAttributes,
    val xp: Int = 0,
    val level: Int = 1,
    val skillPoints: Int = 2,
    val reputation: Int = 10,
    val selectionMeter: Int = 65,
    val droppedMatchesRemaining: Int = 0,
    val consecutivePoorMatches: Int = 0,
    val currentObjective: CareerMatchObjective? = null,
    val objectivesCompleted: Int = 0,
    val unlockedAchievements: List<String> = emptyList(),
    val form: Int = 62,
    val fatigue: Int = 15,
    val injuryMatchesRemaining: Int = 0,
    val difficulty: CareerDifficulty = CareerDifficulty.NORMAL,
    val stage: CareerStage = CareerStage.CLUB_CRICKET,
    val clubTeamName: String = "Hometown Cricket Club",
    val domesticTeamName: String = "Mumbai Emerging XI",
    val activeProLeague: ProT20League = ProT20League.IPL,
    val signedProLeagues: List<String> = emptyList(),
    val iplTeamName: String = "",
    val iplBoughtPriceLakhs: Long = 0L,
    val nationalTeamName: String = "India",
    val isInternationalCaptain: Boolean = false,
    val matchesInSeason: Int = 0,
    val matchesPlayed: Int = 0,
    val totalRuns: Int = 0,
    val highestScore: Int = 0,
    val fifties: Int = 0,
    val centuries: Int = 0,
    val totalWickets: Int = 0,
    val bestBowlingWickets: Int = 0,
    val bestBowlingRuns: Int = 0,
    val potmAwards: Int = 0,
    val domesticTrophies: Int = 0,
    val iplTrophies: Int = 0,
    val worldCupTrophies: Int = 0,
    val seriesWins: Int = 0,
    val peakOverall: Int = 58,
    val trainingDoneBeforeMatch: Boolean = false,
    val awards: List<String> = emptyList(),
    val recentLogs: List<CareerMatchLog> = emptyList()
) {
    val overallRating: Int
        get() = attributes.overallFor(role)

    val totalTrophies: Int
        get() = domesticTrophies + iplTrophies + worldCupTrophies + seriesWins

    val xpForNextLevel: Int
        get() = 100 + (level - 1) * 35

    val isDropped: Boolean
        get() = droppedMatchesRemaining > 0

    val selectionStatusLabel: String
        get() = when {
            isDropped -> "DROPPED (RUN TRIAL)"
            selectionMeter >= 82 -> "PROMOTION FAVORITE"
            selectionMeter >= 62 -> "FIRST-CHOICE XI"
            selectionMeter >= 40 -> "HOLDING SPOT"
            else -> "AT RISK OF DROP"
        }

    val currentLadderStep: Int
        get() = when {
            stage == CareerStage.RETIRED -> 5
            isInternationalCaptain -> 5
            stage == CareerStage.INTERNATIONAL -> 4
            stage == CareerStage.IPL_FRANCHISE || stage == CareerStage.IPL_AUCTION -> 3
            stage == CareerStage.DOMESTIC -> 2
            else -> 1
        }

    val canPromoteToDomestic: Boolean
        get() = stage == CareerStage.CLUB_CRICKET && !isDropped && (selectionMeter >= 76 || reputation >= 22 || matchesPlayed >= 2)

    val canEnterIplAuction: Boolean
        get() = stage == CareerStage.DOMESTIC && !isDropped && (selectionMeter >= 78 || reputation >= 34 || matchesPlayed >= 4)

    val canEarnInternationalCallUp: Boolean
        get() = stage == CareerStage.IPL_FRANCHISE && !isDropped && (selectionMeter >= 80 || reputation >= 56 || overallRating >= 72 || matchesPlayed >= 8)

    val canRetirePrompt: Boolean
        get() = stage != CareerStage.RETIRED && age >= 36

    val isInjured: Boolean
        get() = injuryMatchesRemaining > 0

    val activeClubOrTeam: String
        get() = when (stage) {
            CareerStage.CLUB_CRICKET -> clubTeamName
            CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> domesticTeamName
            CareerStage.IPL_FRANCHISE -> iplTeamName.ifBlank { "${activeProLeague.shortCode} XI" }
            CareerStage.INTERNATIONAL -> "$nationalTeamName${if (isInternationalCaptain) " (C)" else ""}"
            CareerStage.RETIRED -> "Hall of Fame Legend"
        }

    fun availableMatchTypesForCurrentStage(): List<CareerMatchType> = when (stage) {
        CareerStage.CLUB_CRICKET -> listOf(
            CareerMatchType.CLUB_T20,
            CareerMatchType.CLUB_ONE_DAY,
            CareerMatchType.CLUB_FIRST_CLASS
        )
        CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> listOf(
            CareerMatchType.DOMESTIC_T20,
            CareerMatchType.DOMESTIC_ONE_DAY,
            CareerMatchType.DOMESTIC_FIRST_CLASS
        )
        CareerStage.IPL_FRANCHISE -> listOf(
            CareerMatchType.IPL_T20,
            CareerMatchType.PSL_T20,
            CareerMatchType.BBL_T20,
            CareerMatchType.HUNDRED_T20
        )
        CareerStage.INTERNATIONAL, CareerStage.RETIRED -> listOf(
            CareerMatchType.INTL_T20I,
            CareerMatchType.INTL_ODI,
            CareerMatchType.INTL_TEST,
            CareerMatchType.WORLD_CUP,
            CareerMatchType.ODI_WORLD_CUP
        )
    }

    fun hasAchievement(id: CareerAchievementId): Boolean = id.name in unlockedAchievements

    fun toCricketer(): Cricketer {
        val formBoost = ((form - 60) / 10).coerceIn(-4, 4)
        val fatiguePenalty = if (fatigue >= 80) -4 else if (fatigue >= 65) -2 else 0
        val effectiveBat = (attributes.battingRating + formBoost + fatiguePenalty).coerceIn(25, 99)
        val effectiveBowl = (attributes.bowlingRating + formBoost + fatiguePenalty).coerceIn(15, 99)
        val effectiveFld = (attributes.fieldingRating + (formBoost / 2)).coerceIn(45, 99)
        val era = when (stage) {
            CareerStage.CLUB_CRICKET -> "Club Prospect"
            CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> "Domestic Star"
            CareerStage.IPL_FRANCHISE -> "${activeProLeague.shortCode} Star"
            CareerStage.INTERNATIONAL -> if (isInternationalCaptain) "INTL Captain" else "INTL Star"
            CareerStage.RETIRED -> "HOF Legend"
        }
        return Cricketer(
            id = id,
            name = name,
            country = nationality,
            countryCode = countryCode,
            overseas = countryCode != "IND",
            role = role,
            eraTag = era,
            battingRating = effectiveBat,
            bowlingRating = effectiveBowl,
            fieldingRating = effectiveFld,
            basePriceLakhs = when {
                overallRating >= 85 -> 200L
                overallRating >= 74 -> 150L
                overallRating >= 65 -> 75L
                else -> 30L
            },
            specialty = "${battingStyle.label} • ${bowlingStyle.label}"
        )
    }
}

data class HallOfFameLegend(
    val id: String,
    val name: String,
    val nationality: String,
    val countryCode: String,
    val avatarId: String,
    val role: PlayerRole,
    val battingStyle: BattingStyle,
    val bowlingStyle: BowlingStyle,
    val peakOverall: Int,
    val battingRating: Int,
    val bowlingRating: Int,
    val fieldingRating: Int,
    val seasonsPlayed: Int,
    val retiredAge: Int,
    val matchesPlayed: Int,
    val totalRuns: Int,
    val highestScore: Int,
    val fifties: Int,
    val centuries: Int,
    val totalWickets: Int,
    val potmAwards: Int,
    val totalTrophies: Int,
    val wasCaptain: Boolean,
    val iplTeamName: String,
    val awards: List<String>,
    val difficulty: CareerDifficulty = CareerDifficulty.NORMAL,
    val includeInAuctions: Boolean = true,
    val appearance: CareerPlayerAppearance = CareerPlayerAppearance(),
    val achievementsCount: Int = 0
) {
    fun toAuctionCricketer(): Cricketer = Cricketer(
        id = id,
        name = name,
        country = nationality,
        countryCode = countryCode,
        overseas = countryCode != "IND",
        role = role,
        eraTag = "HOF Legend",
        battingRating = battingRating.coerceIn(30, 99),
        bowlingRating = bowlingRating.coerceIn(15, 99),
        fieldingRating = fieldingRating.coerceIn(50, 99),
        basePriceLakhs = if (peakOverall >= 84) 200L else 150L,
        specialty = "HOF Legend • ${totalRuns}R / ${totalWickets}W${if (wasCaptain) " • Captain" else ""}"
    )
}
