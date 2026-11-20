package com.example.engine

/**
 * Realistic Pitch & Weather conditions, Custom Field Presets, DRS Review state,
 * and lightweight Ball-by-Ball Wagon Wheel & Pitch Map telemetry models.
 */
enum class PitchType(
    val label: String,
    val shortLabel: String,
    val summary: String,
    val seamBonus: Int,
    val spinBonus: Int,
    val batMod: Double,
    val bounceFactor: Float
) {
    GREEN(
        label = "Green Seamer",
        shortLabel = "GREEN",
        summary = "Extra swing & seam movement for fast bowlers",
        seamBonus = 7,
        spinBonus = -2,
        batMod = -0.045,
        bounceFactor = 1.15f
    ),
    DUSTY(
        label = "Dusty Turner",
        shortLabel = "DUSTY",
        summary = "Sharp spin turn & grip for spinners",
        seamBonus = -2,
        spinBonus = 8,
        batMod = -0.040,
        bounceFactor = 0.86f
    ),
    FLAT(
        label = "Flat Batting Deck",
        shortLabel = "FLAT",
        summary = "True bounce, fast outfield & high-scoring batting",
        seamBonus = -4,
        spinBonus = -3,
        batMod = 0.055,
        bounceFactor = 1.0f
    ),
    BALANCED(
        label = "Balanced Deck",
        shortLabel = "BALANCED",
        summary = "Even contest between bat, pace & spin",
        seamBonus = 1,
        spinBonus = 1,
        batMod = 0.0,
        bounceFactor = 1.0f
    );

    companion object {
        fun fromNameSafe(raw: String?): PitchType =
            entries.firstOrNull { it.name == raw } ?: BALANCED
    }
}

enum class WeatherCondition(
    val label: String,
    val shortLabel: String,
    val swingBonus: Int,
    val dewLikelihood: Double
) {
    CLEAR_NIGHT(
        label = "Clear Night Floodlights",
        shortLabel = "CLEAR NIGHT",
        swingBonus = 1,
        dewLikelihood = 0.65
    ),
    OVERCAST(
        label = "Overcast & Humid",
        shortLabel = "OVERCAST",
        swingBonus = 4,
        dewLikelihood = 0.50
    ),
    DRY_HEAT(
        label = "Dry Stadium Heat",
        shortLabel = "DRY HEAT",
        swingBonus = 0,
        dewLikelihood = 0.18
    );

    companion object {
        fun fromNameSafe(raw: String?): WeatherCondition =
            entries.firstOrNull { it.name == raw } ?: CLEAR_NIGHT
    }
}

/**
 * Custom bowling field presets that visibly reposition all 9 outfielders on the stadium canvas
 * and dynamically adjust catch/wicket and boundary probabilities.
 */
enum class FieldPreset(
    val label: String,
    val shortLabel: String,
    val summary: String,
    val wicketDelta: Double,
    val boundaryDelta: Double,
    val fielderCoords: List<Pair<Float, Float>>
) {
    ATTACKING(
        label = "Attacking",
        shortLabel = "ATTACK",
        summary = "Close slips & catching ring • +Wickets, +Boundaries",
        wicketDelta = 0.022,
        boundaryDelta = 0.032,
        fielderCoords = listOf(
            0.41f to 0.78f, // 1st Slip
            0.35f to 0.74f, // Gully
            0.29f to 0.60f, // Short Cover
            0.34f to 0.45f, // Mid-Off
            0.66f to 0.45f, // Mid-On
            0.70f to 0.60f, // Short Mid-Wicket
            0.63f to 0.74f, // Short Leg
            0.22f to 0.36f, // Deep Cover
            0.78f to 0.36f  // Deep Mid-Wicket
        )
    ),
    BALANCED(
        label = "Balanced",
        shortLabel = "BALANCED",
        summary = "Standard 30-yard ring + 4 boundary riders",
        wicketDelta = 0.0,
        boundaryDelta = 0.0,
        fielderCoords = listOf(
            0.24f to 0.68f, // Point
            0.21f to 0.46f, // Extra Cover
            0.36f to 0.30f, // Long-Off
            0.64f to 0.30f, // Long-On
            0.78f to 0.46f, // Deep Mid-Wicket
            0.74f to 0.68f, // Square Leg
            0.36f to 0.56f, // Mid-Off Ring
            0.64f to 0.56f, // Mid-On Ring
            0.72f to 0.80f  // Fine Leg
        )
    ),
    DEFENSIVE(
        label = "Defensive",
        shortLabel = "DEFEND",
        summary = "5 boundary riders deep • Cuts 4s & 6s",
        wicketDelta = -0.012,
        boundaryDelta = -0.034,
        fielderCoords = listOf(
            0.16f to 0.64f, // Deep Point
            0.17f to 0.42f, // Deep Cover
            0.34f to 0.25f, // Long-Off Rope
            0.66f to 0.25f, // Long-On Rope
            0.83f to 0.42f, // Cow Corner
            0.83f to 0.64f, // Deep Square Leg
            0.26f to 0.79f, // Deep Third Man
            0.74f to 0.80f, // Deep Fine Leg
            0.38f to 0.52f  // Inside Mid-Off
        )
    ),
    DEATH_OVERS(
        label = "Death Overs",
        shortLabel = "DEATH",
        summary = "Straight & square boundary net • Max Six protection",
        wicketDelta = 0.008,
        boundaryDelta = -0.044,
        fielderCoords = listOf(
            0.30f to 0.23f, // Straight Long-Off
            0.70f to 0.23f, // Straight Long-On
            0.15f to 0.38f, // Deep Extra Cover
            0.85f to 0.38f, // Deep Mid-Wicket
            0.16f to 0.58f, // Sweeper Cover
            0.84f to 0.58f, // Deep Backward Square
            0.25f to 0.82f, // Fine Third Man
            0.75f to 0.82f, // Fine Leg Rope
            0.50f to 0.36f  // Mid-On Catcher
        )
    );

    companion object {
        fun fromNameSafe(raw: String?): FieldPreset =
            entries.firstOrNull { it.name == raw } ?: BALANCED
    }
}

enum class DrsMode(val label: String) {
    BALL_TRACKING("HAWK-EYE BALL TRACKING"),
    ULTRA_EDGE("ULTRAEDGE SNICKO")
}

/**
 * Represents a close LBW or Caught-Behind decision eligible for DRS review.
 */
data class DrsPendingDecision(
    val decisionId: Long,
    val mode: DrsMode,
    val originalDecisionOut: Boolean,
    val overturned: Boolean,
    val umpiresCall: Boolean,
    val reviewingTeamId: String,
    val reviewingTeamName: String,
    val isBattingTeamReview: Boolean,
    val batterId: String,
    val batterName: String,
    val bowlerName: String,
    val dismissalLabel: String,
    val pitchingLabel: String,
    val impactLabel: String,
    val wicketsLabel: String,
    val spikeIntensity: Float, // 0f..1f for UltraEdge audio waveform
    val ballPitchX: Float,     // -1f (off) .. 1f (leg)
    val ballPitchY: Float,     // 0f (yorker) .. 1f (short)
    val inningsNumber: Int,
    val overLabel: String
)

/**
 * Lightweight shot telemetry for Wagon Wheel visualization.
 */
data class ShotTelemetry(
    val batterId: String,
    val batterName: String,
    val angleDeg: Float,       // -180..180 where 0 = straight down ground, -90 = off, +90 = leg
    val distanceRatio: Float,  // 0.25..1.05 of boundary radius
    val runs: Int,
    val overLabel: String
)

/**
 * Lightweight pitch impact point for Pitch Map visualization.
 */
data class PitchMapPoint(
    val bowlerId: String,
    val bowlerName: String,
    val lineX: Float,          // -1f (wide off) .. 0f (middle stump) .. +1f (leg side)
    val lengthY: Float,        // 0f (yorker/full toss) .. 1f (bouncer/short)
    val outcome: BallOutcomeType,
    val isSpin: Boolean,
    val overLabel: String
)
