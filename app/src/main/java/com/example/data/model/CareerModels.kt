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
    );

    companion object {
        fun fromNameSafe(raw: String?): CareerDifficulty =
            entries.firstOrNull { it.name == raw } ?: NORMAL
    }
}

enum class CareerStage(val label: String, val stepNumber: Int) {
    DOMESTIC("Stage 1 • Domestic League", 1),
    IPL_AUCTION("Stage 2 • IPL Auction Pool", 2),
    IPL_FRANCHISE("Stage 2 • IPL Franchise", 2),
    INTERNATIONAL("Stage 3 • International Arena", 3),
    RETIRED("Hall of Fame Legend", 4)
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
        subtitle = "+Technique & Timing • +30 XP",
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
        subtitle = "+Catching & Throws • +28 XP",
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
    val stage: MatchStage
) {
    DOMESTIC_T20("Domestic Emerging T20", MatchFormat.T20I, MatchStage.LEAGUE),
    IPL_T20("IPL Franchise T20", MatchFormat.T20I, MatchStage.LEAGUE),
    INTL_T20I("International T20I", MatchFormat.T20I, MatchStage.SERIES),
    INTL_ODI("International ODI", MatchFormat.ODI, MatchStage.SERIES),
    INTL_TEST("International Test", MatchFormat.TEST, MatchStage.SERIES),
    WORLD_CUP("ICC World Cup Clash", MatchFormat.T20I, MatchStage.FINAL)
}

data class CareerPlayerAttributes(
    val battingTechnique: Int,
    val powerHitting: Int,
    val bowlingSkill: Int,
    val fielding: Int
) {
    val battingRating: Int
        get() = ((battingTechnique * 0.55) + (powerHitting * 0.45)).toInt().coerceIn(25, 99)

    val bowlingRating: Int
        get() = bowlingSkill.coerceIn(15, 99)

    val fieldingRating: Int
        get() = fielding.coerceIn(45, 99)

    fun overallFor(role: PlayerRole): Int = when (role) {
        PlayerRole.BATTER -> (battingRating * 0.8 + fieldingRating * 0.2).toInt()
        PlayerRole.WICKETKEEPER -> (battingRating * 0.7 + fieldingRating * 0.3).toInt()
        PlayerRole.ALL_ROUNDER -> (battingRating * 0.45 + bowlingRating * 0.45 + fieldingRating * 0.1).toInt()
        PlayerRole.SEAMER, PlayerRole.SPINNER -> (bowlingRating * 0.8 + fieldingRating * 0.1 + battingRating * 0.1).toInt()
    }.coerceIn(40, 99)
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
    val xpEarned: Int = 0
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
    val role: PlayerRole = PlayerRole.BATTER,
    val battingStyle: BattingStyle = BattingStyle.RIGHT_HAND_BAT,
    val bowlingStyle: BowlingStyle = BowlingStyle.OCCASIONAL_MEDIUM,
    val attributes: CareerPlayerAttributes,
    val xp: Int = 0,
    val level: Int = 1,
    val reputation: Int = 10,
    val form: Int = 62,
    val fatigue: Int = 15,
    val injuryMatchesRemaining: Int = 0,
    val difficulty: CareerDifficulty = CareerDifficulty.NORMAL,
    val stage: CareerStage = CareerStage.DOMESTIC,
    val domesticTeamName: String = "Mumbai Emerging XI",
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

    val canEnterIplAuction: Boolean
        get() = stage == CareerStage.DOMESTIC && (reputation >= 30 || matchesPlayed >= 4)

    val canEarnInternationalCallUp: Boolean
        get() = stage == CareerStage.IPL_FRANCHISE && (reputation >= 58 || overallRating >= 72 || matchesPlayed >= 9)

    val canRetirePrompt: Boolean
        get() = stage != CareerStage.RETIRED && age >= 36

    val isInjured: Boolean
        get() = injuryMatchesRemaining > 0

    val activeClubOrTeam: String
        get() = when (stage) {
            CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> domesticTeamName
            CareerStage.IPL_FRANCHISE -> iplTeamName.ifBlank { domesticTeamName }
            CareerStage.INTERNATIONAL -> "$nationalTeamName${if (isInternationalCaptain) " (C)" else ""}"
            CareerStage.RETIRED -> "Hall of Fame Legend"
        }

    fun toCricketer(): Cricketer {
        val formBoost = ((form - 60) / 10).coerceIn(-4, 4)
        val fatiguePenalty = if (fatigue >= 80) -4 else if (fatigue >= 65) -2 else 0
        val effectiveBat = (attributes.battingRating + formBoost + fatiguePenalty).coerceIn(25, 99)
        val effectiveBowl = (attributes.bowlingRating + formBoost + fatiguePenalty).coerceIn(15, 99)
        val effectiveFld = (attributes.fieldingRating + (formBoost / 2)).coerceIn(45, 99)
        val era = when (stage) {
            CareerStage.DOMESTIC, CareerStage.IPL_AUCTION -> "Emerging"
            CareerStage.IPL_FRANCHISE -> "IPL Star"
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
    val includeInAuctions: Boolean = true
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
