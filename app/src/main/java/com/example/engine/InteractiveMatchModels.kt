package com.example.engine

enum class InteractivePlayRole(val label: String) {
    BATTING("BAT"),
    BOWLING("BOWL")
}

enum class InteractivePlayFilter(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    BOTH(
        label = "Both Bat & Bowl",
        shortLabel = "BOTH",
        description = "Control both batting shots and bowling deliveries"
    ),
    BATTING_ONLY(
        label = "Batting Only",
        shortLabel = "BAT ONLY",
        description = "Play your team's batting; opponent innings auto-sims"
    ),
    BOWLING_ONLY(
        label = "Bowling Only",
        shortLabel = "BOWL ONLY",
        description = "Control your team's bowling; your batting auto-sims"
    )
}

object InteractivePhaseResolver {
    /**
     * Resolves which team in a [LiveMatchState] belongs to the active user.
     */
    fun resolveUserTeamId(match: LiveMatchState, preferredUserTeamId: String?): String {
        if (!preferredUserTeamId.isNullOrBlank()) {
            if (preferredUserTeamId == match.teamA.teamId) return match.teamA.teamId
            if (preferredUserTeamId == match.teamB.teamId) return match.teamB.teamId
        }
        return when {
            !match.teamA.bot && match.teamB.bot -> match.teamA.teamId
            match.teamA.bot && !match.teamB.bot -> match.teamB.teamId
            else -> match.teamA.teamId
        }
    }

    /**
     * Returns true when the user's team is currently batting (supports both regulation innings and Super Overs).
     */
    fun isUserTeamBatting(match: LiveMatchState, userTeamId: String): Boolean {
        val battingTeamId = if (match.isSuperOverActive) {
            match.activeSuperOverInnings?.battingTeamId ?: match.activeInnings.battingTeamId
        } else {
            match.activeInnings.battingTeamId
        }
        return battingTeamId == userTeamId
    }

    /**
     * Returns true when the current match phase should be auto-simulated based on [filter].
     * - [InteractivePlayFilter.BATTING_ONLY]: auto-sim when opponent is batting.
     * - [InteractivePlayFilter.BOWLING_ONLY]: auto-sim when user's own team is batting.
     * - [InteractivePlayFilter.BOTH]: never auto-skip phases.
     */
    fun shouldAutoSimCurrentPhase(
        match: LiveMatchState,
        userTeamId: String,
        filter: InteractivePlayFilter
    ): Boolean {
        if (match.finished) return false
        val userBatting = isUserTeamBatting(match, userTeamId)
        return when (filter) {
            InteractivePlayFilter.BOTH -> false
            InteractivePlayFilter.BATTING_ONLY -> !userBatting
            InteractivePlayFilter.BOWLING_ONLY -> userBatting
        }
    }

    /**
     * Computes which interactive control role (BAT or BOWL) should be active for the current phase.
     */
    fun expectedActiveRole(
        match: LiveMatchState,
        userTeamId: String,
        filter: InteractivePlayFilter
    ): InteractivePlayRole = when (filter) {
        InteractivePlayFilter.BATTING_ONLY -> InteractivePlayRole.BATTING
        InteractivePlayFilter.BOWLING_ONLY -> InteractivePlayRole.BOWLING
        InteractivePlayFilter.BOTH -> {
            if (isUserTeamBatting(match, userTeamId)) InteractivePlayRole.BATTING else InteractivePlayRole.BOWLING
        }
    }
}

enum class ShotDirection(
    val label: String,
    val shortLabel: String,
    val angleDeg: Float,
    val isLofted: Boolean = false
) {
    OFF_DRIVE(label = "Off Drive", shortLabel = "OFF", angleDeg = -36f, isLofted = false),
    STRAIGHT(label = "Straight", shortLabel = "STR", angleDeg = 0f, isLofted = false),
    LEG_GLANCE(label = "Leg Flick", shortLabel = "LEG", angleDeg = 36f, isLofted = false),
    LOFTED(label = "Loft / Six", shortLabel = "LOFT", angleDeg = 12f, isLofted = true)
}

enum class TimingGrade(
    val label: String,
    val shortLabel: String,
    val batEdge: Double,
    val bowlEdge: Double,
    val riskDelta: Double
) {
    EARLY(
        label = "EARLY",
        shortLabel = "EARLY",
        batEdge = -0.11,
        bowlEdge = -0.10,
        riskDelta = 0.045
    ),
    GOOD(
        label = "GOOD",
        shortLabel = "GOOD",
        batEdge = 0.10,
        bowlEdge = 0.10,
        riskDelta = -0.015
    ),
    PERFECT(
        label = "PERFECT!",
        shortLabel = "PERFECT",
        batEdge = 0.28,
        bowlEdge = 0.25,
        riskDelta = -0.045
    ),
    LATE(
        label = "LATE",
        shortLabel = "LATE",
        batEdge = -0.14,
        bowlEdge = -0.13,
        riskDelta = 0.060
    );

    companion object {
        /**
         * Evaluates the oscillating timing gauge needle in [0f, 1f].
         * Base sweet spot: 0.42..0.58 -> PERFECT; Good zone: 0.26..0.74 -> GOOD.
         * High batter confidence (0..100) gently widens both the PERFECT and GOOD windows,
         * while low bowler stamina slightly tightens the bowling sweet spot.
         */
        fun fromGaugeCursor(cursor: Float, sweetSpotHalfWidthBonus: Float = 0f): TimingGrade {
            val c = cursor.coerceIn(0f, 1f)
            val bonus = sweetSpotHalfWidthBonus.coerceIn(-0.03f, 0.055f)
            val perfectHalf = (0.08f + bonus).coerceIn(0.055f, 0.135f)
            val goodHalf = (0.24f + bonus * 1.3f).coerceIn(0.19f, 0.31f)
            return when {
                c in (0.5f - perfectHalf)..(0.5f + perfectHalf) -> PERFECT
                c in (0.5f - goodHalf)..(0.5f + goodHalf) -> GOOD
                c < (0.5f - goodHalf) -> EARLY
                else -> LATE
            }
        }

        fun confidenceWindowBonus(batterConfidence: Int): Float {
            // Confidence 40 = 0 bonus; 100 = +0.05f wider PERFECT half-width; <25 = -0.015f
            return ((batterConfidence - 40) / 60f * 0.05f).coerceIn(-0.015f, 0.05f)
        }

        fun staminaWindowPenalty(bowlerStamina: Int): Float {
            // Stamina >= 60 = 0; Stamina 20 = -0.025f narrower sweet spot
            return if (bowlerStamina >= 60) 0f else -((60 - bowlerStamina) / 40f * 0.025f).coerceIn(0f, 0.025f)
        }
    }
}

enum class DeliveryType(
    val label: String,
    val shortLabel: String,
    val pitchYRatio: Float, // 0f = bowler end, 1f = batter crease
    val lateralCurve: Float,
    val speedKph: Int,
    val wicketBonus: Double
) {
    YORKER(
        label = "Yorker",
        shortLabel = "YORKER",
        pitchYRatio = 0.90f,
        lateralCurve = 0.03f,
        speedKph = 144,
        wicketBonus = 0.030
    ),
    BOUNCER(
        label = "Bouncer",
        shortLabel = "BOUNCE",
        pitchYRatio = 0.44f,
        lateralCurve = -0.03f,
        speedKph = 146,
        wicketBonus = 0.025
    ),
    GOOD_LENGTH(
        label = "Good Length",
        shortLabel = "LENGTH",
        pitchYRatio = 0.68f,
        lateralCurve = 0.05f,
        speedKph = 138,
        wicketBonus = 0.018
    ),
    SLOWER(
        label = "Slower Ball",
        shortLabel = "SLOWER",
        pitchYRatio = 0.74f,
        lateralCurve = 0.02f,
        speedKph = 114,
        wicketBonus = 0.024
    ),
    SPIN_TURN(
        label = "Spin / Turn",
        shortLabel = "SPIN",
        pitchYRatio = 0.71f,
        lateralCurve = 0.14f,
        speedKph = 88,
        wicketBonus = 0.026
    );

    fun effectiveSpeedKph(bowlerStamina: Int, dewActive: Boolean): Int {
        val staminaDrop = if (bowlerStamina < 60) ((60 - bowlerStamina) * 0.22f).toInt() else 0
        val dewDrop = if (dewActive) 3 else 0
        return (speedKph - staminaDrop - dewDrop).coerceAtLeast(72)
    }
}

data class InteractiveBallInput(
    val role: InteractivePlayRole,
    val shotDirection: ShotDirection = ShotDirection.STRAIGHT,
    val deliveryType: DeliveryType = DeliveryType.GOOD_LENGTH,
    val timingGrade: TimingGrade = TimingGrade.GOOD,
    val fieldPreset: FieldPreset? = null
)

data class ActionAnimationSnapshot(
    val sequenceId: Long,
    val outcome: BallOutcomeType,
    val runsScored: Int,
    val shotDirection: ShotDirection,
    val deliveryType: DeliveryType,
    val timingGrade: TimingGrade?,
    val role: InteractivePlayRole,
    val batterName: String,
    val bowlerName: String,
    val overLabel: String,
    val isSuperOver: Boolean,
    val isRedBall: Boolean,
    val pitchType: PitchType = PitchType.BALANCED,
    val dewActive: Boolean = false,
    val fieldPreset: FieldPreset = FieldPreset.BALANCED
)
