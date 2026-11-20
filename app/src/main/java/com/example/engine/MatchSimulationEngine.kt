package com.example.engine

import com.example.data.model.Cricketer
import com.example.data.model.CricketerDatabase
import com.example.data.model.MatchFormat
import com.example.data.model.MatchRecord
import com.example.data.model.MatchStage
import com.example.data.model.PlayerRole
import com.example.data.model.SquadRuleValidator
import com.example.data.model.SquadRules
import com.example.data.model.TeamState
import java.util.Locale
import kotlin.random.Random

enum class BallOutcomeType {
    DOT, SINGLE, DOUBLE, TRIPLE, FOUR, SIX, WICKET, WIDE, NO_BALL
}

data class BallEvent(
    val inningsNumber: Int,
    val overLabel: String,
    val batterName: String,
    val bowlerName: String,
    val runsScored: Int,
    val outcome: BallOutcomeType,
    val commentary: String,
    val scoreAfterBall: String,
    val isInjuryAlert: Boolean = false,
    val isSuperOver: Boolean = false
)

data class BatterScoreEntry(
    val playerId: String,
    val playerName: String,
    val roleLabel: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val out: Boolean = false,
    val injured: Boolean = false,
    val isCaptain: Boolean = false,
    val confidence: Int = 40,
    val dismissalText: String = "not out"
) {
    val strikeRate: Double
        get() = if (balls == 0) 0.0 else (runs * 100.0) / balls
}

data class BowlerScoreEntry(
    val playerId: String,
    val playerName: String,
    val roleLabel: String = "PACE",
    val legalBalls: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0,
    val currentOverRuns: Int = 0,
    val stamina: Int = 100,
    val injured: Boolean = false,
    val isCaptain: Boolean = false
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val economy: Double
        get() = if (legalBalls == 0) 0.0 else (runsConceded * 6.0) / legalBalls
}

data class FallOfWicket(
    val wicketNumber: Int,
    val scoreAtFall: Int,
    val overLabel: String,
    val batterName: String
)

data class SuperOverInnings(
    val roundNumber: Int,
    val inningsNumber: Int, // 1 or 2 inside this Super Over round
    val battingTeamId: String,
    val battingTeamName: String,
    val bowlingTeamId: String,
    val bowlingTeamName: String,
    val batters: List<Cricketer>,
    val bowler: Cricketer,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val extras: Int = 0,
    val strikerIdx: Int = 0,
    val nonStrikerIdx: Int = 1,
    val nextBatterIdx: Int = 2,
    val battingCard: List<BatterScoreEntry> = emptyList(),
    val bowlingEntry: BowlerScoreEntry,
    val recentBalls: List<BallEvent> = emptyList(),
    val completed: Boolean = false
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val striker: Cricketer?
        get() = batters.getOrNull(strikerIdx)
}

data class SuperOverRoundSummary(
    val roundNumber: Int,
    val team1Id: String,
    val team1Name: String,
    val team1Score: Int,
    val team1Wickets: Int,
    val team1Overs: String,
    val team2Id: String,
    val team2Name: String,
    val team2Score: Int,
    val team2Wickets: Int,
    val team2Overs: String,
    val outcomeNote: String
)

data class InningsState(
    val format: MatchFormat = MatchFormat.T20I,
    val inningsNumber: Int,
    val battingTeamId: String,
    val battingTeamName: String,
    val battingCaptainId: String = "",
    val bowlingTeamId: String,
    val bowlingTeamName: String,
    val bowlingCaptainId: String = "",
    val battingLineup: List<Cricketer>,
    val bowlingOptions: List<Cricketer>,
    val bowlingTeamFullXi: List<Cricketer> = bowlingOptions,
    val battingChemistry: Int,
    val bowlingChemistry: Int,
    val fieldingAverage: Int,
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val legalBalls: Int = 0,
    val extras: Int = 0,
    val partnershipRuns: Int = 0,
    val strikerIdx: Int = 0,
    val nonStrikerIdx: Int = 1,
    val nextBatterIdx: Int = 2,
    val currentBowlerIdx: Int = 0,
    val lastOverBowlerId: String = "",
    val battingCard: List<BatterScoreEntry> = emptyList(),
    val bowlingCard: List<BowlerScoreEntry> = emptyList(),
    val wormByOver: List<Int> = emptyList(),
    val wicketOvers: List<Int> = emptyList(),
    val fallOfWickets: List<FallOfWicket> = emptyList(),
    val recentBalls: List<BallEvent> = emptyList(),
    val injuredPlayerIds: Set<String> = emptySet(),
    val completed: Boolean = false,
    val battingTeamReviewsRemaining: Int = 2,
    val bowlingTeamReviewsRemaining: Int = 2,
    val wagonWheelShots: List<ShotTelemetry> = emptyList(),
    val pitchMapPoints: List<PitchMapPoint> = emptyList()
) {
    val oversFormatted: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val currentRunRate: Double
        get() = if (legalBalls == 0) 0.0 else (totalRuns * 6.0) / legalBalls

    val isStartOfOver: Boolean
        get() = !completed && legalBalls % 6 == 0

    val phaseLabel: String
        get() {
            val ov = legalBalls / 6
            return when (format) {
                MatchFormat.T20I -> when {
                    ov < 6 -> "POWERPLAY"
                    ov < 15 -> "MIDDLE OVERS"
                    else -> "DEATH OVERS"
                }
                MatchFormat.ODI -> when {
                    ov < 10 -> "POWERPLAY 1"
                    ov < 40 -> "MIDDLE OVERS"
                    else -> "SLOG OVERS"
                }
                MatchFormat.TEST -> {
                    val session = (ov / 30).coerceIn(0, 2) + 1
                    val day = if (inningsNumber == 1) 1 + (ov / 45) else 3 + (ov / 45)
                    "DAY $day • SESSION $session"
                }
            }
        }

    val striker: Cricketer?
        get() = battingLineup.getOrNull(strikerIdx)

    val nonStriker: Cricketer?
        get() = battingLineup.getOrNull(nonStrikerIdx)

    val currentBowler: Cricketer?
        get() = bowlingOptions.getOrNull(currentBowlerIdx)

    val currentBowlerStamina: Int
        get() = bowlingCard.getOrNull(currentBowlerIdx)?.stamina ?: 100

    val currentBatterConfidence: Int
        get() = battingCard.getOrNull(strikerIdx)?.confidence ?: 40

    /**
     * Returns null if this bowler is strictly eligible to bowl the next over,
     * otherwise a short human-readable block reason.
     */
    fun bowlerBlockReason(entry: BowlerScoreEntry): String? {
        if (entry.injured) return "Injured"
        if (format != MatchFormat.TEST && (entry.legalBalls >= format.maxBallsPerBowler || entry.legalBalls + 6 > format.maxBallsPerBowler)) {
            return "Max ${format.maxOversPerBowler}.0 ov"
        }
        if (lastOverBowlerId.isNotBlank() && entry.playerId == lastOverBowlerId) {
            return "Last over"
        }
        return null
    }
}

data class LiveMatchState(
    val format: MatchFormat = MatchFormat.T20I,
    val stage: MatchStage = MatchStage.LEAGUE,
    val pitchCondition: String = "Balanced Deck",
    val pitchType: PitchType = PitchType.BALANCED,
    val weatherCondition: WeatherCondition = WeatherCondition.CLEAR_NIGHT,
    val dewFactor2ndInnings: Boolean = false,
    val fieldPreset: FieldPreset = FieldPreset.BALANCED,
    val pendingDrs: DrsPendingDecision? = null,
    val tossSummary: String = "",
    val teamA: TeamState,
    val teamB: TeamState,
    val teamACaptainId: String = "",
    val teamBCaptainId: String = "",
    val teamAChemistry: Int,
    val teamBChemistry: Int,
    val teamAImpactUsed: Boolean = false,
    val teamBImpactUsed: Boolean = false,
    val teamAImpactLog: String? = null,
    val teamBImpactLog: String? = null,
    val injuryAlerts: List<String> = emptyList(),
    val innings1: InningsState,
    val innings2: InningsState,
    val activeInningsNumber: Int = 1,
    val superOverRound: Int = 0,
    val activeSuperOverInningsNumber: Int = 1,
    val superOverInnings1: SuperOverInnings? = null,
    val superOverInnings2: SuperOverInnings? = null,
    val superOverHistory: List<SuperOverRoundSummary> = emptyList(),
    val commentaryFeed: List<BallEvent> = emptyList(),
    val finished: Boolean = false,
    val winnerTeamId: String = "",
    val winnerTeamName: String = "",
    val resultSummary: String = "",
    val playerOfTheMatch: String = "",
    val savedToCloud: Boolean = false
) {
    val activeInnings: InningsState
        get() = if (activeInningsNumber == 1) innings1 else innings2

    val isSuperOverActive: Boolean
        get() = superOverRound > 0 && !finished

    val isDewActiveNow: Boolean
        get() = dewFactor2ndInnings && activeInningsNumber == 2 && !isSuperOverActive && !finished

    val activeSuperOverInnings: SuperOverInnings?
        get() = if (!isSuperOverActive) null else if (activeSuperOverInningsNumber == 1) superOverInnings1 else superOverInnings2

    val target: Int?
        get() = when {
            isSuperOverActive && activeSuperOverInningsNumber == 2 -> (superOverInnings1?.totalRuns ?: 0) + 1
            isSuperOverActive -> null
            activeInningsNumber == 2 || innings1.completed -> innings1.totalRuns + 1
            else -> null
        }

    val requiredRunRate: Double?
        get() {
            if (finished) return null
            if (isSuperOverActive) {
                if (activeSuperOverInningsNumber != 2) return null
                val so1 = superOverInnings1 ?: return null
                val so2 = superOverInnings2 ?: return null
                val runsNeeded = (so1.totalRuns + 1) - so2.totalRuns
                val ballsLeft = (6 - so2.legalBalls).coerceAtLeast(1)
                return if (runsNeeded <= 0) 0.0 else (runsNeeded * 6.0) / ballsLeft
            }
            if (activeInningsNumber != 2) return null
            val runsNeeded = (innings1.totalRuns + 1) - innings2.totalRuns
            val ballsLeft = (format.maxBalls - innings2.legalBalls).coerceAtLeast(1)
            return if (runsNeeded <= 0) 0.0 else (runsNeeded * 6.0) / ballsLeft
        }

    fun toMatchRecord(roomCode: String, hostUid: String, memberUids: List<String>): MatchRecord {
        val fullBalls = format.maxBalls.toLong()
        val effectiveBallsA = if (innings1.wickets >= 10) fullBalls else innings1.legalBalls.toLong().coerceAtLeast(1L)
        val effectiveBallsB = if (innings2.wickets >= 10) fullBalls else innings2.legalBalls.toLong().coerceAtLeast(1L)
        return MatchRecord(
            roomCode = roomCode,
            hostUid = hostUid,
            memberUids = memberUids,
            stage = stage.name,
            teamAId = teamA.teamId,
            teamAName = teamA.teamName,
            teamBId = teamB.teamId,
            teamBName = teamB.teamName,
            teamAScore = innings1.totalRuns.toLong(),
            teamAWickets = innings1.wickets.toLong(),
            teamAOvers = innings1.oversFormatted,
            teamABalls = effectiveBallsA,
            teamBScore = innings2.totalRuns.toLong(),
            teamBWickets = innings2.wickets.toLong(),
            teamBOvers = innings2.oversFormatted,
            teamBBalls = effectiveBallsB,
            winnerTeamId = winnerTeamId.ifBlank { "TIE" },
            winnerTeamName = winnerTeamName.ifBlank { "Match Tied" },
            resultSummary = resultSummary.ifBlank { "Match Completed" }.take(195),
            playerOfTheMatch = playerOfTheMatch.ifBlank { "Match Star" }.take(115),
            innings1Worm = innings1.wormByOver.map { it.toLong() },
            innings2Worm = innings2.wormByOver.map { it.toLong() },
            playerPerformances = SeasonAwardsEngine.extractPerformances(this),
            status = "COMPLETED"
        )
    }
}

/**
 * Pomp-Style Multi-Format Ball-by-Ball Match Simulation Engine:
 * - Strict 4-Over Cap per bowler in T20/T20I (hard-enforced for both manual and AI selection).
 * - Strict No Consecutive Overs rule (bowlerId != lastOverBowlerId across all formats).
 * - Repeatable Super Over on Tied T20/T20I matches (1 over / 6 balls, max 2 wickets per side).
 * - ODI (50 overs, max 10 overs/bowler) & Test (90 overs, uncapped overs, stamina/fatigue, Draw).
 * - Realistic mid-match player injuries & Impact Player substitution (T20/T20I only).
 */
object MatchSimulationEngine {

    private val pitchPresetsT20 = listOf("Batting Paradise", "Balanced T20 Deck", "Seam & Bounce", "Spin-Friendly Turn")
    private val pitchPresetsOdi = listOf("True ODI Pitch", "Batting Paradise", "Seam & Bounce", "Spin-Friendly Turn")
    private val pitchPresetsTest = listOf("Green Seamer", "Dusty Day-4 Turner", "True Test Surface", "Hard Bounce Deck")

    private val injuryDescriptions = listOf(
        "hamstring tweak",
        "side strain",
        "calf cramp",
        "groin niggle",
        "rolled ankle"
    )

    fun calculateTeamChemistry(xi: List<Cricketer>): Int {
        if (xi.isEmpty()) return 70
        val hasWk = xi.any { it.role == PlayerRole.WICKETKEEPER }
        val seamers = xi.count { it.role == PlayerRole.SEAMER }
        val spinners = xi.count { it.role == PlayerRole.SPINNER }
        val allRounders = xi.count { it.role == PlayerRole.ALL_ROUNDER }
        val batters = xi.count { it.role == PlayerRole.BATTER }
        val avgField = xi.map { it.fieldingRating }.average()

        var chem = 68
        if (hasWk) chem += 6
        if (seamers >= 2) chem += 6 else if (seamers == 1) chem += 3
        if (spinners >= 1) chem += 6
        if (allRounders >= 2) chem += 6 else if (allRounders == 1) chem += 3
        if (batters >= 3) chem += 4
        chem += ((avgField - 80.0) * 0.4).toInt()
        return chem.coerceIn(65, 99)
    }

    fun createMatch(
        teamA: TeamState,
        teamB: TeamState,
        stage: MatchStage = MatchStage.LEAGUE,
        format: MatchFormat = MatchFormat.T20I,
        random: Random = Random.Default,
        teamBRatingOffset: Int = 0,
        pitchType: PitchType? = null,
        weatherCondition: WeatherCondition? = null,
        dewFactor2ndInnings: Boolean? = null
    ): LiveMatchState {
        val xiA = resolvePlayingXi(teamA)
        val rawXiB = resolvePlayingXi(teamB)
        val xiB = if (teamBRatingOffset == 0) {
            rawXiB
        } else {
            rawXiB.map { p ->
                p.copy(
                    battingRating = (p.battingRating + teamBRatingOffset).coerceIn(20, 99),
                    bowlingRating = (p.bowlingRating + teamBRatingOffset).coerceIn(15, 99),
                    fieldingRating = (p.fieldingRating + (teamBRatingOffset / 2)).coerceIn(45, 99)
                )
            }
        }
        val capAId = resolveCaptainId(teamA, xiA)
        val capBId = resolveCaptainId(teamB, xiB)
        val capAName = xiA.firstOrNull { it.id == capAId }?.name ?: xiA.firstOrNull()?.name ?: "Captain"
        val capBName = xiB.firstOrNull { it.id == capBId }?.name ?: xiB.firstOrNull()?.name ?: "Captain"
        val resolvedTeamA = teamA.copy(playingXiIds = xiA.map { it.id }, captainPlayerId = capAId)
        val resolvedTeamB = teamB.copy(playingXiIds = xiB.map { it.id }, captainPlayerId = capBId)
        val chemA = calculateTeamChemistry(xiA)
        val chemB = calculateTeamChemistry(xiB)
        val resolvedPitch = pitchType ?: PitchType.entries.random(random)
        val resolvedWeather = weatherCondition ?: WeatherCondition.entries.random(random)
        val resolvedDew = dewFactor2ndInnings ?: (format != MatchFormat.TEST && random.nextDouble() < resolvedWeather.dewLikelihood)
        val pitchLabel = buildPitchConditionLabel(resolvedPitch, resolvedWeather, resolvedDew)
        val tossSummary = if (random.nextBoolean()) {
            "🪙 Toss: ${teamA.teamName} ($capAName (C)) won the toss & chose to bat first vs $capBName (C)"
        } else {
            "🪙 Toss: ${teamB.teamName} ($capBName (C)) won the toss & chose to bowl first vs $capAName (C)"
        }
        val innings1 = buildInitialInnings(format, 1, resolvedTeamA, resolvedTeamB, xiA, xiB, chemA, chemB, capAId, capBId)
        val innings2 = buildInitialInnings(format, 2, resolvedTeamB, resolvedTeamA, xiB, xiA, chemB, chemA, capBId, capAId)
        return LiveMatchState(
            format = format,
            stage = stage,
            pitchCondition = pitchLabel,
            pitchType = resolvedPitch,
            weatherCondition = resolvedWeather,
            dewFactor2ndInnings = resolvedDew,
            fieldPreset = FieldPreset.BALANCED,
            tossSummary = tossSummary,
            teamA = resolvedTeamA,
            teamB = resolvedTeamB,
            teamACaptainId = capAId,
            teamBCaptainId = capBId,
            teamAChemistry = chemA,
            teamBChemistry = chemB,
            innings1 = innings1,
            innings2 = innings2,
            activeInningsNumber = 1
        )
    }

    fun buildPitchConditionLabel(pitch: PitchType, weather: WeatherCondition, dew: Boolean): String {
        val dewTag = if (dew) " • 2nd-Inn Dew" else ""
        return "${pitch.label} • ${weather.shortLabel}$dewTag"
    }

    fun updatePreMatchConditions(
        state: LiveMatchState,
        pitchType: PitchType,
        weatherCondition: WeatherCondition,
        dewFactor2ndInnings: Boolean
    ): LiveMatchState {
        return state.copy(
            pitchType = pitchType,
            weatherCondition = weatherCondition,
            dewFactor2ndInnings = dewFactor2ndInnings,
            pitchCondition = buildPitchConditionLabel(pitchType, weatherCondition, dewFactor2ndInnings)
        )
    }

    fun setFieldPreset(state: LiveMatchState, preset: FieldPreset): LiveMatchState {
        return state.copy(fieldPreset = preset)
    }

    fun dismissPendingDrs(state: LiveMatchState): LiveMatchState {
        return state.copy(pendingDrs = null)
    }

    /**
     * Resolves an active DRS review:
     * - Overturned OUT -> restores batter at crease, rolls back wicket/FOW/bowler wicket, retains review!
     * - Overturned NOT OUT -> awards wicket to bowler, updates FOW/next batter, retains review!
     * - Umpire's Call -> original decision stands, reviewing team RETAINS their review!
     * - Decision Upheld -> original decision stands, reviewing team LOSES 1 review!
     */
    fun resolveDrsReview(state: LiveMatchState): LiveMatchState {
        val drs = state.pendingDrs ?: return state
        val inn = if (drs.inningsNumber == 1) state.innings1 else state.innings2
        val deductReview = !drs.overturned && !drs.umpiresCall

        var updatedInn = inn.copy(
            battingTeamReviewsRemaining = if (drs.isBattingTeamReview && deductReview) {
                (inn.battingTeamReviewsRemaining - 1).coerceAtLeast(0)
            } else inn.battingTeamReviewsRemaining,
            bowlingTeamReviewsRemaining = if (!drs.isBattingTeamReview && deductReview) {
                (inn.bowlingTeamReviewsRemaining - 1).coerceAtLeast(0)
            } else inn.bowlingTeamReviewsRemaining
        )

        val verdictHeadline: String
        if (drs.originalDecisionOut && drs.overturned) {
            // Restore dismissed batter back to the crease
            val restoredIdx = updatedInn.battingCard.indexOfFirst { it.playerId == drs.batterId }
            val newBatCard = updatedInn.battingCard.toMutableList()
            val newBowlCard = updatedInn.bowlingCard.toMutableList()
            var nextStriker = updatedInn.strikerIdx
            var nextInLine = updatedInn.nextBatterIdx

            if (restoredIdx >= 0) {
                // Demote newly entered batter if they haven't faced a ball yet
                if (nextStriker != restoredIdx && nextStriker in newBatCard.indices && newBatCard[nextStriker].balls == 0) {
                    newBatCard[nextStriker] = newBatCard[nextStriker].copy(dismissalText = "yet to bat")
                    nextInLine = (nextInLine - 1).coerceAtLeast(restoredIdx + 1)
                }
                newBatCard[restoredIdx] = newBatCard[restoredIdx].copy(
                    out = false,
                    dismissalText = "batting",
                    confidence = (newBatCard[restoredIdx].confidence + 8).coerceAtMost(100)
                )
                nextStriker = restoredIdx
            }

            val bowlerIdx = newBowlCard.indexOfFirst { it.playerName.removeSuffix(" (IP)") == drs.bowlerName }
            if (bowlerIdx >= 0 && newBowlCard[bowlerIdx].wickets > 0) {
                newBowlCard[bowlerIdx] = newBowlCard[bowlerIdx].copy(wickets = newBowlCard[bowlerIdx].wickets - 1)
            }
            val updatedFow = if (updatedInn.fallOfWickets.isNotEmpty() && updatedInn.fallOfWickets.last().batterName == drs.batterName) {
                updatedInn.fallOfWickets.dropLast(1)
            } else updatedInn.fallOfWickets

            updatedInn = updatedInn.copy(
                wickets = (updatedInn.wickets - 1).coerceAtLeast(0),
                strikerIdx = nextStriker,
                nextBatterIdx = nextInLine,
                battingCard = newBatCard,
                bowlingCard = newBowlCard,
                fallOfWickets = updatedFow,
                wicketOvers = updatedInn.wicketOvers.dropLast(1),
                completed = false
            )
            verdictHeadline = "📺 DRS OVERTURNED — NOT OUT! ${drs.batterName} survives (${drs.mode.label}) • Review Retained!"
        } else if (!drs.originalDecisionOut && drs.overturned) {
            // Bowling review successful: dismiss the batter!
            val targetIdx = updatedInn.battingCard.indexOfFirst { it.playerId == drs.batterId }
            val newBatCard = updatedInn.battingCard.toMutableList()
            val newBowlCard = updatedInn.bowlingCard.toMutableList()
            var nextStriker = updatedInn.strikerIdx
            var nextInLine = updatedInn.nextBatterIdx
            val newWickets = (updatedInn.wickets + 1).coerceAtMost(10)

            if (targetIdx >= 0) {
                newBatCard[targetIdx] = newBatCard[targetIdx].copy(
                    out = true,
                    dismissalText = drs.dismissalLabel
                )
                if (nextInLine < updatedInn.battingLineup.size) {
                    if (nextStriker == targetIdx) {
                        nextStriker = nextInLine
                    }
                    newBatCard[nextInLine] = newBatCard[nextInLine].copy(dismissalText = "batting")
                    nextInLine += 1
                }
            }
            val bowlerIdx = newBowlCard.indexOfFirst { it.playerName.removeSuffix(" (IP)") == drs.bowlerName }
            if (bowlerIdx >= 0) {
                newBowlCard[bowlerIdx] = newBowlCard[bowlerIdx].copy(wickets = newBowlCard[bowlerIdx].wickets + 1)
            }
            val newFow = updatedInn.fallOfWickets + FallOfWicket(
                wicketNumber = newWickets,
                scoreAtFall = updatedInn.totalRuns,
                overLabel = drs.overLabel,
                batterName = drs.batterName
            )
            val allOut = newWickets >= 10
            updatedInn = updatedInn.copy(
                wickets = newWickets,
                partnershipRuns = 0,
                strikerIdx = nextStriker,
                nextBatterIdx = nextInLine,
                battingCard = newBatCard,
                bowlingCard = newBowlCard,
                fallOfWickets = newFow,
                wicketOvers = updatedInn.wicketOvers + ((updatedInn.legalBalls - 1).coerceAtLeast(0) / 6 + 1),
                completed = updatedInn.completed || allOut
            )
            verdictHeadline = "📺 DRS OVERTURNED — OUT! ${drs.batterName} ${drs.dismissalLabel} (${drs.mode.label}) • Review Retained!"
        } else if (drs.umpiresCall) {
            val call = if (drs.originalDecisionOut) "OUT" else "NOT OUT"
            verdictHeadline = "📺 DRS UMPIRE'S CALL — $call stands (${drs.wicketsLabel}) • Review Retained!"
        } else {
            val call = if (drs.originalDecisionOut) "OUT" else "NOT OUT"
            val left = if (drs.isBattingTeamReview) updatedInn.battingTeamReviewsRemaining else updatedInn.bowlingTeamReviewsRemaining
            verdictHeadline = "📺 DRS UPHELD — $call (${drs.wicketsLabel}) • ${drs.reviewingTeamName} has $left review(s) left."
        }

        val drsEvent = BallEvent(
            inningsNumber = drs.inningsNumber,
            overLabel = drs.overLabel,
            batterName = drs.batterName,
            bowlerName = drs.bowlerName,
            runsScored = 0,
            outcome = if ((!drs.originalDecisionOut && drs.overturned) || (drs.originalDecisionOut && !drs.overturned)) {
                BallOutcomeType.WICKET
            } else BallOutcomeType.DOT,
            commentary = verdictHeadline,
            scoreAfterBall = "${updatedInn.totalRuns}/${updatedInn.wickets} (${updatedInn.oversFormatted})"
        )

        val nextFeed = (listOf(drsEvent) + state.commentaryFeed).take(220)
        return if (drs.inningsNumber == 1) {
            val advanceTo2 = updatedInn.completed
            state.copy(
                innings1 = updatedInn,
                activeInningsNumber = if (advanceTo2) 2 else state.activeInningsNumber,
                pendingDrs = null,
                commentaryFeed = nextFeed
            )
        } else {
            val updated = state.copy(
                innings2 = updatedInn,
                pendingDrs = null,
                commentaryFeed = nextFeed
            )
            if (updatedInn.completed && !updated.finished) finalizeOrTriggerSuperOver(updated, Random.Default) else updated
        }
    }

    /**
     * Preserves the EXACT user-defined batting order (#1 to #11) when playingXiIds has 11 valid players.
     * Only falls back to auto-pick/default ordering when a team has not yet finalized an 11-player XI.
     */
    private fun resolvePlayingXi(team: TeamState): List<Cricketer> {
        val explicitXi = team.playingXiIds.mapNotNull { CricketerDatabase.getById(it) }
        if (explicitXi.size == SquadRules.MIN_SQUAD_SIZE) {
            return explicitXi
        }
        val autoIds = SquadRuleValidator.autoPickBestPlayingXi(team.squadPlayerIds)
        return autoIds.mapNotNull { CricketerDatabase.getById(it) }
    }

    private fun resolveCaptainId(team: TeamState, xi: List<Cricketer>): String {
        val xiIds = xi.map { it.id }
        return team.captainPlayerId.takeIf { it.isNotBlank() && it in xiIds }
            ?: SquadRuleValidator.autoPickCaptain(xiIds)
    }

    private fun buildInitialInnings(
        format: MatchFormat,
        inningsNumber: Int,
        battingTeam: TeamState,
        bowlingTeam: TeamState,
        battingXi: List<Cricketer>,
        bowlingXi: List<Cricketer>,
        battingChem: Int,
        bowlingChem: Int,
        battingCaptainId: String,
        bowlingCaptainId: String
    ): InningsState {
        // Always include 6 initial bowling options (5 main + 1 backup) so even with injuries the 4-over / 10-over caps stay strictly enforceable
        val bowlers = pickBowlingAttack(bowlingXi, 6)
        val fieldingAvg = if (bowlingXi.isEmpty()) 80 else bowlingXi.map { it.fieldingRating }.average().toInt()
        val initialBattingCard = battingXi.mapIndexed { idx, p ->
            BatterScoreEntry(
                playerId = p.id,
                playerName = p.name,
                roleLabel = p.role.shortLabel,
                isCaptain = p.id == battingCaptainId,
                dismissalText = if (idx < 2) "batting" else "yet to bat"
            )
        }
        val initialBowlingCard = bowlers.map { b ->
            BowlerScoreEntry(
                playerId = b.id,
                playerName = b.name,
                roleLabel = b.role.shortLabel,
                stamina = 100,
                isCaptain = b.id == bowlingCaptainId
            )
        }
        return InningsState(
            format = format,
            inningsNumber = inningsNumber,
            battingTeamId = battingTeam.teamId,
            battingTeamName = battingTeam.teamName,
            battingCaptainId = battingCaptainId,
            bowlingTeamId = bowlingTeam.teamId,
            bowlingTeamName = bowlingTeam.teamName,
            bowlingCaptainId = bowlingCaptainId,
            battingLineup = battingXi,
            bowlingOptions = bowlers,
            bowlingTeamFullXi = bowlingXi,
            battingChemistry = battingChem,
            bowlingChemistry = bowlingChem,
            fieldingAverage = fieldingAvg,
            battingCard = initialBattingCard,
            bowlingCard = initialBowlingCard
        )
    }

    private fun pickBowlingAttack(bowlingXi: List<Cricketer>, count: Int): List<Cricketer> =
        bowlingXi
            .sortedByDescending {
                val roleBonus = when (it.role) {
                    PlayerRole.SEAMER, PlayerRole.SPINNER -> 22
                    PlayerRole.ALL_ROUNDER -> 12
                    else -> 0
                }
                it.bowlingRating + roleBonus
            }
            .take(count.coerceAtMost(bowlingXi.size))

    /**
     * Manual bowler selection at the start of any over (legalBalls % 6 == 0).
     * Hard-enforces:
     * 1. oversBowled < 4 in T20/T20I (and < 10 in ODI).
     * 2. No consecutive overs (bowlerId != lastOverBowlerId).
     * 3. Bowler not injured.
     */
    fun selectBowlerForNextOver(state: LiveMatchState, bowlerPlayerId: String): Result<LiveMatchState> {
        if (state.finished) return Result.failure(IllegalStateException("Match is already finished"))
        if (state.isSuperOverActive) return Result.failure(IllegalStateException("Super Over bowler is locked"))
        val inn = state.activeInnings
        if (inn.legalBalls % 6 != 0) {
            return Result.failure(IllegalStateException("Wait until the current over finishes to change bowler"))
        }
        val idx = inn.bowlingCard.indexOfFirst { it.playerId == bowlerPlayerId }
        if (idx < 0) return Result.failure(IllegalArgumentException("Bowler not found in bowling attack"))
        val entry = inn.bowlingCard[idx]
        val blockReason = inn.bowlerBlockReason(entry)
        if (blockReason != null) {
            val friendly = when (blockReason) {
                "Last over" -> "${entry.playerName} bowled the last over — consecutive overs are not allowed"
                "Injured" -> "${entry.playerName} is injured and unavailable to bowl"
                else -> "${entry.playerName} has completed the max ${inn.format.maxOversPerBowler}.0-over quota"
            }
            return Result.failure(IllegalStateException(friendly))
        }
        val updatedInn = inn.copy(currentBowlerIdx = idx)
        return Result.success(
            if (state.activeInningsNumber == 1) state.copy(innings1 = updatedInn) else state.copy(innings2 = updatedInn)
        )
    }

    /**
     * Impact Player Rule (IPL & T20I only): Allows a team to substitute 1 bench player into the XI.
     */
    fun applyImpactSubstitution(
        state: LiveMatchState,
        teamId: String,
        benchPlayerId: String,
        outPlayerId: String
    ): Result<LiveMatchState> {
        if (!state.format.impactSubAllowed) {
            return Result.failure(IllegalStateException("Impact Player rule applies to T20/T20I only (not ${state.format.shortLabel})"))
        }
        if (state.finished || state.isSuperOverActive) {
            return Result.failure(IllegalStateException("Impact Sub unavailable right now"))
        }
        val isTeamA = state.teamA.teamId == teamId
        val team = if (isTeamA) state.teamA else state.teamB
        val alreadyUsed = if (isTeamA) state.teamAImpactUsed else state.teamBImpactUsed
        if (alreadyUsed) return Result.failure(IllegalStateException("Impact Player already used for ${team.teamName}"))

        val benchPlayer = CricketerDatabase.getById(benchPlayerId)
            ?: return Result.failure(IllegalArgumentException("Bench player not found"))
        val outPlayer = CricketerDatabase.getById(outPlayerId)
            ?: return Result.failure(IllegalArgumentException("Outgoing player not found"))

        val isInternationalTeam = team.teamId.startsWith("nat_")
        if (!isInternationalTeam) {
            val currentXi = if (isTeamA) state.innings1.battingLineup else state.innings2.battingLineup
            val overseasNow = currentXi.count { it.overseas }
            val overseasAfter = overseasNow - (if (outPlayer.overseas) 1 else 0) + (if (benchPlayer.overseas) 1 else 0)
            if (overseasAfter > SquadRules.MAX_OVERSEAS_IN_XI) {
                return Result.failure(IllegalStateException("Max ${SquadRules.MAX_OVERSEAS_IN_XI} overseas players allowed in Playing XI (swap an overseas player instead)"))
            }
        }

        val updatedInn1 = applySubToInnings(state.innings1, teamId, benchPlayer, outPlayer)
        val updatedInn2 = applySubToInnings(state.innings2, teamId, benchPlayer, outPlayer)

        val logText = "⚡ IMPACT SUB (${team.teamName}): ${benchPlayer.name} IN for ${outPlayer.name}!"
        val activeInn = if (state.activeInningsNumber == 1) updatedInn1 else updatedInn2
        val subEvent = BallEvent(
            inningsNumber = state.activeInningsNumber,
            overLabel = activeInn.oversFormatted,
            batterName = benchPlayer.name,
            bowlerName = outPlayer.name,
            runsScored = 0,
            outcome = BallOutcomeType.DOT,
            commentary = logText,
            scoreAfterBall = "${activeInn.totalRuns}/${activeInn.wickets} (${activeInn.oversFormatted})"
        )

        val nextState = state.copy(
            innings1 = updatedInn1,
            innings2 = updatedInn2,
            teamAImpactUsed = if (isTeamA) true else state.teamAImpactUsed,
            teamBImpactUsed = if (!isTeamA) true else state.teamBImpactUsed,
            teamAImpactLog = if (isTeamA) "${benchPlayer.name} in for ${outPlayer.name}" else state.teamAImpactLog,
            teamBImpactLog = if (!isTeamA) "${benchPlayer.name} in for ${outPlayer.name}" else state.teamBImpactLog,
            commentaryFeed = (listOf(subEvent) + state.commentaryFeed).take(220)
        )
        return Result.success(nextState)
    }

    private fun applySubToInnings(
        inn: InningsState,
        teamId: String,
        inPlayer: Cricketer,
        outPlayer: Cricketer
    ): InningsState {
        var result = inn
        if (inn.battingTeamId == teamId) {
            val outIdx = inn.battingLineup.indexOfFirst { it.id == outPlayer.id }
            if (outIdx >= 0) {
                val cardEntry = inn.battingCard.getOrNull(outIdx)
                if (cardEntry != null && cardEntry.dismissalText == "yet to bat") {
                    val newLineup = inn.battingLineup.toMutableList().apply { set(outIdx, inPlayer) }
                    val newCard = inn.battingCard.toMutableList().apply {
                        set(
                            outIdx,
                            BatterScoreEntry(
                                playerId = inPlayer.id,
                                playerName = "${inPlayer.name} (IP)",
                                roleLabel = inPlayer.role.shortLabel,
                                dismissalText = "yet to bat"
                            )
                        )
                    }
                    result = result.copy(battingLineup = newLineup, battingCard = newCard)
                }
            }
        }
        if (inn.bowlingTeamId == teamId) {
            val existingIdx = inn.bowlingOptions.indexOfFirst { it.id == outPlayer.id }
            if (existingIdx >= 0 && inn.bowlingCard[existingIdx].legalBalls == 0) {
                val newBowlers = inn.bowlingOptions.toMutableList().apply { set(existingIdx, inPlayer) }
                val newBowlCard = inn.bowlingCard.toMutableList().apply {
                    set(existingIdx, BowlerScoreEntry(inPlayer.id, "${inPlayer.name} (IP)", inPlayer.role.shortLabel))
                }
                result = result.copy(bowlingOptions = newBowlers, bowlingCard = newBowlCard)
            } else if (inPlayer.bowlingRating >= 60 && inn.bowlingOptions.none { it.id == inPlayer.id }) {
                val newBowlers = inn.bowlingOptions + inPlayer
                val newBowlCard = inn.bowlingCard + BowlerScoreEntry(inPlayer.id, "${inPlayer.name} (IP)", inPlayer.role.shortLabel)
                result = result.copy(bowlingOptions = newBowlers, bowlingCard = newBowlCard)
            }
        }
        return result
    }

    fun simulateNextBall(
        state: LiveMatchState,
        random: Random = Random.Default,
        interactiveInput: InteractiveBallInput? = null
    ): LiveMatchState {
        if (state.finished) return state

        // Route directly to Super Over ball engine if a Super Over is active
        if (state.isSuperOverActive) {
            return simulateSuperOverBall(state, random)
        }

        var current = state.activeInnings
        if (current.completed) {
            return if (state.activeInningsNumber == 1) {
                state.copy(activeInningsNumber = 2)
            } else {
                finalizeOrTriggerSuperOver(state, random)
            }
        }

        val format = state.format
        val maxLegalBalls = format.maxBalls

        // Hard safety guard at the start of any over: guarantee strict quota & no consecutive overs!
        if (current.legalBalls % 6 == 0) {
            val curCard = current.bowlingCard.getOrNull(current.currentBowlerIdx)
            if (curCard == null || current.bowlerBlockReason(curCard) != null) {
                val (promotedOptions, promotedCard, validIdx) = resolveValidNextBowler(
                    format = format,
                    nextOverIndex = current.legalBalls / 6,
                    lastOverBowlerId = current.lastOverBowlerId,
                    currentOptions = current.bowlingOptions,
                    currentCard = current.bowlingCard,
                    fullXi = current.bowlingTeamFullXi,
                    injuredIds = current.injuredPlayerIds
                )
                current = current.copy(
                    bowlingOptions = promotedOptions,
                    bowlingCard = promotedCard,
                    currentBowlerIdx = validIdx
                )
            }
        }

        val batter = current.striker ?: return finalizeOrTriggerSuperOver(state, random)
        val bowler = current.currentBowler ?: return finalizeOrTriggerSuperOver(state, random)
        val overNumber = current.legalBalls / 6
        val ballInOver = (current.legalBalls % 6) + 1
        val overLabel = "$overNumber.$ballInOver"

        val bowlerEntryBefore = current.bowlingCard[current.currentBowlerIdx]
        val batterEntryBefore = current.battingCard[current.strikerIdx]
        val activePreset = interactiveInput?.fieldPreset ?: state.fieldPreset
        val dewActive = state.dewFactor2ndInnings && state.activeInningsNumber == 2

        val staminaPenalty = when {
            bowlerEntryBefore.stamina < 30 -> if (format == MatchFormat.TEST) -14 else -10
            bowlerEntryBefore.stamina < 45 -> if (format == MatchFormat.TEST) -8 else -6
            bowlerEntryBefore.stamina < 60 -> -3
            else -> 0
        }

        val phaseBowlingBonus = when (format) {
            MatchFormat.T20I -> when {
                overNumber < 6 && bowler.role == PlayerRole.SEAMER -> 5
                overNumber in 6..14 && bowler.role == PlayerRole.SPINNER -> 6
                overNumber >= 15 && bowler.role == PlayerRole.SEAMER && bowler.bowlingRating >= 88 -> 5
                else -> 0
            }
            MatchFormat.ODI -> when {
                overNumber < 10 && bowler.role == PlayerRole.SEAMER -> 5
                overNumber in 10..39 && bowler.role == PlayerRole.SPINNER -> 5
                overNumber >= 40 && bowler.role == PlayerRole.SEAMER && bowler.bowlingRating >= 88 -> 4
                else -> 0
            }
            MatchFormat.TEST -> when {
                overNumber < 25 && bowler.role == PlayerRole.SEAMER -> 7
                overNumber >= 45 && bowler.role == PlayerRole.SPINNER -> 8
                else -> 2
            }
        }

        val pitchBowlBonus = when (bowler.role) {
            PlayerRole.SEAMER -> state.pitchType.seamBonus + state.weatherCondition.swingBonus
            PlayerRole.SPINNER -> state.pitchType.spinBonus
            else -> (state.pitchType.seamBonus + state.pitchType.spinBonus) / 3
        }
        val dewBowlPenalty = if (dewActive) {
            if (bowler.role == PlayerRole.SPINNER) -6 else -3
        } else 0
        val dewBatBoost = if (dewActive) 0.032 else 0.0
        val confidenceBatBonus = ((batterEntryBefore.confidence - 40) / 60.0 * 0.045).coerceIn(-0.02, 0.045)

        val pitchBatMod = state.pitchType.batMod + dewBatBoost + confidenceBatBonus
        val chemEdge = (current.battingChemistry - current.bowlingChemistry) / 300.0
        val partnershipMomentum = if (current.partnershipRuns >= 35) 0.025 else 0.0

        val chasePressure = if (state.activeInningsNumber == 2 && format != MatchFormat.TEST) {
            val needed = (state.innings1.totalRuns + 1) - current.totalRuns
            val ballsLeft = (maxLegalBalls - current.legalBalls).coerceAtLeast(1)
            val rrr = (needed * 6.0) / ballsLeft
            val parRate = if (format == MatchFormat.T20I) 8.5 else 6.0
            ((rrr - parRate) / 10.0).coerceIn(-0.15, 0.35)
        } else 0.0

        val effectiveBowlRating = (bowler.bowlingRating + phaseBowlingBonus + pitchBowlBonus + staminaPenalty + dewBowlPenalty).coerceIn(42, 99)
        val baseRatingDiff = ((batter.battingRating - effectiveBowlRating) / 100.0) + chemEdge + pitchBatMod + partnershipMomentum
        val interactiveRatingShift = when (interactiveInput?.role) {
            InteractivePlayRole.BATTING -> interactiveInput.timingGrade.batEdge + if (interactiveInput.shotDirection.isLofted) 0.07 else 0.0
            InteractivePlayRole.BOWLING -> -interactiveInput.timingGrade.bowlEdge
            null -> 0.0
        }
        val ratingDiff = (baseRatingDiff + interactiveRatingShift).coerceIn(-0.48, 0.52)
        val fieldingFactor = (current.fieldingAverage - 80) / 200.0

        val outcome = rollBallOutcome(
            format = format,
            overNumber = overNumber,
            ratingDiff = ratingDiff,
            fieldingFactor = fieldingFactor,
            chasePressure = chasePressure,
            random = random,
            interactiveInput = interactiveInput,
            fieldPreset = activePreset,
            dewActive = dewActive,
            lowBowlerStamina = bowlerEntryBefore.stamina < 45
        )

        val isExtra = outcome == BallOutcomeType.WIDE || outcome == BallOutcomeType.NO_BALL
        val runsFromBall = when (outcome) {
            BallOutcomeType.DOT, BallOutcomeType.WICKET -> 0
            BallOutcomeType.SINGLE, BallOutcomeType.WIDE, BallOutcomeType.NO_BALL -> 1
            BallOutcomeType.DOUBLE -> 2
            BallOutcomeType.TRIPLE -> 3
            BallOutcomeType.FOUR -> 4
            BallOutcomeType.SIX -> 6
        }

        val newTotalRuns = current.totalRuns + runsFromBall
        val newExtras = current.extras + if (isExtra) 1 else 0
        val newLegalBalls = current.legalBalls + if (isExtra) 0 else 1
        val newWickets = current.wickets + if (outcome == BallOutcomeType.WICKET) 1 else 0
        val newPartnership = if (outcome == BallOutcomeType.WICKET) 0 else current.partnershipRuns + runsFromBall

        val updatedBattingCard = current.battingCard.toMutableList()
        val strikerEntry = updatedBattingCard[current.strikerIdx]
        val dismissalDesc = if (outcome == BallOutcomeType.WICKET) {
            generateDismissal(bowler, current.bowlingOptions, random)
        } else {
            strikerEntry.dismissalText
        }
        val timingConfidenceBonus = when {
            interactiveInput?.role == InteractivePlayRole.BATTING && interactiveInput.timingGrade == TimingGrade.PERFECT -> 8
            interactiveInput?.role == InteractivePlayRole.BATTING && interactiveInput.timingGrade == TimingGrade.GOOD -> 4
            interactiveInput?.role == InteractivePlayRole.BATTING &&
                (interactiveInput.timingGrade == TimingGrade.EARLY || interactiveInput.timingGrade == TimingGrade.LATE) -> -4
            else -> 0
        }
        val outcomeConfidenceDelta = when (outcome) {
            BallOutcomeType.SIX -> 16
            BallOutcomeType.FOUR -> 12
            BallOutcomeType.TRIPLE -> 8
            BallOutcomeType.DOUBLE -> 6
            BallOutcomeType.SINGLE -> 4
            BallOutcomeType.DOT -> -3
            else -> 0
        }
        val updatedConfidence = (strikerEntry.confidence + outcomeConfidenceDelta + timingConfidenceBonus).coerceIn(15, 100)

        updatedBattingCard[current.strikerIdx] = strikerEntry.copy(
            runs = strikerEntry.runs + if (isExtra) 0 else runsFromBall,
            balls = strikerEntry.balls + if (outcome == BallOutcomeType.WIDE) 0 else 1,
            fours = strikerEntry.fours + if (outcome == BallOutcomeType.FOUR) 1 else 0,
            sixes = strikerEntry.sixes + if (outcome == BallOutcomeType.SIX) 1 else 0,
            out = outcome == BallOutcomeType.WICKET,
            confidence = updatedConfidence,
            dismissalText = if (outcome == BallOutcomeType.WICKET) dismissalDesc else "batting"
        )

        val updatedBowlingCard = current.bowlingCard.toMutableList()
        val updatedOverRuns = bowlerEntryBefore.currentOverRuns + runsFromBall
        val completedThisOver = !isExtra && (newLegalBalls % 6 == 0)
        val isDeathPhase = (format == MatchFormat.T20I && overNumber >= 15) || (format == MatchFormat.ODI && overNumber >= 40)
        val staminaDrain = if (completedThisOver) {
            val baseDrain = when (format) {
                MatchFormat.TEST -> if (bowler.role == PlayerRole.SEAMER) 15 else 9
                MatchFormat.ODI -> if (bowler.role == PlayerRole.SEAMER) 10 else 6
                MatchFormat.T20I -> if (bowler.role == PlayerRole.SEAMER) 14 else 9
            }
            val deathExtra = if (isDeathPhase) 7 else 0
            val workloadExtra = if (bowlerEntryBefore.legalBalls >= 12) 4 else 0
            baseDrain + deathExtra + workloadExtra
        } else 0

        updatedBowlingCard[current.currentBowlerIdx] = bowlerEntryBefore.copy(
            legalBalls = bowlerEntryBefore.legalBalls + if (isExtra) 0 else 1,
            runsConceded = bowlerEntryBefore.runsConceded + runsFromBall,
            wickets = bowlerEntryBefore.wickets + if (outcome == BallOutcomeType.WICKET) 1 else 0,
            maidens = bowlerEntryBefore.maidens + if (completedThisOver && updatedOverRuns == 0) 1 else 0,
            currentOverRuns = if (completedThisOver) 0 else updatedOverRuns,
            stamina = (bowlerEntryBefore.stamina - staminaDrain).coerceIn(18, 100)
        )

        // Recover resting bowlers' stamina cleanly at end of every over across all formats
        if (completedThisOver) {
            val recovery = when (format) {
                MatchFormat.TEST -> 12
                MatchFormat.ODI -> 8
                MatchFormat.T20I -> 7
            }
            for (idx in updatedBowlingCard.indices) {
                if (idx != current.currentBowlerIdx && !updatedBowlingCard[idx].injured) {
                    val b = updatedBowlingCard[idx]
                    updatedBowlingCard[idx] = b.copy(stamina = (b.stamina + recovery).coerceAtMost(100))
                }
            }
        }

        var nextStrikerIdx = current.strikerIdx
        var nextNonStrikerIdx = current.nonStrikerIdx
        var nextInLine = current.nextBatterIdx
        val updatedFow = current.fallOfWickets.toMutableList()
        val updatedWicketOvers = current.wicketOvers.toMutableList()
        val updatedInjuredIds = current.injuredPlayerIds.toMutableSet()
        val updatedInjuryAlerts = state.injuryAlerts.toMutableList()
        var injuryEvent: BallEvent? = null

        if (outcome == BallOutcomeType.WICKET) {
            updatedFow.add(FallOfWicket(newWickets, newTotalRuns, overLabel, batter.name))
            updatedWicketOvers.add((newLegalBalls - 1) / 6 + 1)
            if (nextInLine < current.battingLineup.size) {
                nextStrikerIdx = nextInLine
                updatedBattingCard[nextInLine] = updatedBattingCard[nextInLine].copy(dismissalText = "batting")
                nextInLine += 1
            }
        } else if (runsFromBall == 1 || runsFromBall == 3) {
            val tmp = nextStrikerIdx
            nextStrikerIdx = nextNonStrikerIdx
            nextNonStrikerIdx = tmp
        }

        // Realistic Player Injury System (capped to max 2 injuries per innings so squads remain playable)
        if (!isExtra && outcome != BallOutcomeType.WICKET && updatedInjuredIds.size < 2) {
            val bowlerFatigueRisk = if (format == MatchFormat.TEST && updatedBowlingCard[current.currentBowlerIdx].stamina < 35) 0.006 else 0.0014
            val batterInjuryRisk = 0.0014
            val injRoll = random.nextDouble()
            if (injRoll < bowlerFatigueRisk && updatedBowlingCard.count { !it.injured } > 3 && !updatedBowlingCard[current.currentBowlerIdx].injured) {
                val issue = injuryDescriptions.random(random)
                updatedInjuredIds.add(bowler.id)
                updatedBowlingCard[current.currentBowlerIdx] = updatedBowlingCard[current.currentBowlerIdx].copy(injured = true)
                val alert = "${bowler.name} ($issue)"
                updatedInjuryAlerts.add(alert)
                injuryEvent = BallEvent(
                    inningsNumber = state.activeInningsNumber,
                    overLabel = overLabel,
                    batterName = batter.name,
                    bowlerName = bowler.name,
                    runsScored = 0,
                    outcome = BallOutcomeType.DOT,
                    commentary = "🚑 INJURY ALERT: ${bowler.name} pulls up with a $issue and leaves the attack!",
                    scoreAfterBall = "$newTotalRuns/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6})",
                    isInjuryAlert = true
                )
            } else if (injRoll < bowlerFatigueRisk + batterInjuryRisk && nextInLine < current.battingLineup.size && !updatedBattingCard[current.strikerIdx].injured) {
                val issue = injuryDescriptions.random(random)
                updatedInjuredIds.add(batter.id)
                updatedBattingCard[current.strikerIdx] = updatedBattingCard[current.strikerIdx].copy(
                    injured = true,
                    dismissalText = "retired hurt ($issue)"
                )
                val replacement = current.battingLineup[nextInLine]
                if (nextStrikerIdx == current.strikerIdx) {
                    nextStrikerIdx = nextInLine
                } else {
                    nextNonStrikerIdx = nextInLine
                }
                updatedBattingCard[nextInLine] = updatedBattingCard[nextInLine].copy(dismissalText = "batting")
                nextInLine += 1
                val alert = "${batter.name} (retired hurt)"
                updatedInjuryAlerts.add(alert)
                injuryEvent = BallEvent(
                    inningsNumber = state.activeInningsNumber,
                    overLabel = overLabel,
                    batterName = batter.name,
                    bowlerName = bowler.name,
                    runsScored = 0,
                    outcome = BallOutcomeType.DOT,
                    commentary = "🚑 INJURY ALERT: ${batter.name} suffers a $issue and retires hurt! ${replacement.name} walks in.",
                    scoreAfterBall = "$newTotalRuns/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6})",
                    isInjuryAlert = true
                )
            }
        }

        val updatedWorm = current.wormByOver.toMutableList()
        var nextBowlingOptions = current.bowlingOptions
        var nextBowlingCard: List<BowlerScoreEntry> = updatedBowlingCard
        var nextBowlerIdx = current.currentBowlerIdx
        var nextLastOverBowlerId = current.lastOverBowlerId

        val bowlerJustInjured = updatedBowlingCard[current.currentBowlerIdx].injured
        if (completedThisOver || bowlerJustInjured) {
            if (completedThisOver) {
                updatedWorm.add(newTotalRuns)
                val tmp = nextStrikerIdx
                nextStrikerIdx = nextNonStrikerIdx
                nextNonStrikerIdx = tmp
                // Mandatory rotation: record the bowler who just completed this over!
                nextLastOverBowlerId = bowler.id
            }
            val ballsNeededNext = if (completedThisOver) 6 else (6 - (newLegalBalls % 6)).coerceIn(1, 6)
            val (resolvedOptions, resolvedCard, resolvedIdx) = resolveValidNextBowler(
                format = format,
                nextOverIndex = newLegalBalls / 6,
                lastOverBowlerId = bowler.id,
                currentOptions = nextBowlingOptions,
                currentCard = nextBowlingCard,
                fullXi = current.bowlingTeamFullXi,
                injuredIds = updatedInjuredIds,
                ballsNeededInOver = ballsNeededNext
            )
            nextBowlingOptions = resolvedOptions
            nextBowlingCard = resolvedCard
            nextBowlerIdx = resolvedIdx
        }

        val chaseWon = state.activeInningsNumber == 2 && newTotalRuns > state.innings1.totalRuns
        val inningsEnded = newWickets >= 10 || newLegalBalls >= maxLegalBalls || chaseWon
        if (inningsEnded && !completedThisOver && updatedWorm.size < format.maxOvers) {
            updatedWorm.add(newTotalRuns)
        }

        val finalBattingCard = if (inningsEnded) {
            updatedBattingCard.map { entry ->
                if (entry.dismissalText == "batting") entry.copy(dismissalText = "not out") else entry
            }
        } else updatedBattingCard

        val scoreAfter = "$newTotalRuns/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6})"
        val baseCommentary = buildCommentary(
            overLabel = overLabel,
            batter = batter,
            bowler = bowler,
            outcome = outcome,
            dismissalDesc = dismissalDesc,
            batterRunsAfter = finalBattingCard[current.strikerIdx].runs,
            batterBallsAfter = finalBattingCard[current.strikerIdx].balls,
            random = random
        )
        val commentary = if (interactiveInput != null) {
            val tag = when (interactiveInput.role) {
                InteractivePlayRole.BATTING -> "[${interactiveInput.timingGrade.shortLabel} • ${interactiveInput.shotDirection.label}]"
                InteractivePlayRole.BOWLING -> "[${interactiveInput.timingGrade.shortLabel} • ${interactiveInput.deliveryType.label} ${interactiveInput.deliveryType.speedKph}kph]"
            }
            "$tag $baseCommentary"
        } else {
            baseCommentary
        }

        val event = BallEvent(
            inningsNumber = state.activeInningsNumber,
            overLabel = overLabel,
            batterName = batter.name,
            bowlerName = bowler.name,
            runsScored = runsFromBall,
            outcome = outcome,
            commentary = commentary,
            scoreAfterBall = scoreAfter
        )

        val newEvents = if (injuryEvent != null) listOf(injuryEvent, event) else listOf(event)

        // Lightweight Wagon Wheel & Pitch Map telemetry recording
        val newWagonShots = if (!isExtra && runsFromBall > 0) {
            val baseAngle = when (interactiveInput?.shotDirection) {
                ShotDirection.OFF_DRIVE -> -48f + (random.nextFloat() - 0.5f) * 44f
                ShotDirection.STRAIGHT -> (random.nextFloat() - 0.5f) * 34f
                ShotDirection.LEG_GLANCE -> 58f + (random.nextFloat() - 0.5f) * 56f
                ShotDirection.LOFTED -> (random.nextFloat() - 0.5f) * 110f
                null -> (random.nextFloat() * 340f) - 170f
            }
            val dist = when (outcome) {
                BallOutcomeType.SIX -> 1.03f
                BallOutcomeType.FOUR -> 0.94f
                BallOutcomeType.TRIPLE -> 0.78f
                BallOutcomeType.DOUBLE -> 0.64f
                else -> 0.44f + random.nextFloat() * 0.22f
            }
            (current.wagonWheelShots + ShotTelemetry(
                batterId = batter.id,
                batterName = batter.name,
                angleDeg = baseAngle,
                distanceRatio = dist,
                runs = runsFromBall,
                overLabel = overLabel
            )).takeLast(140)
        } else current.wagonWheelShots

        val pitchY = when (interactiveInput?.deliveryType) {
            DeliveryType.YORKER -> 0.08f + random.nextFloat() * 0.08f
            DeliveryType.BOUNCER -> 0.78f + random.nextFloat() * 0.16f
            DeliveryType.GOOD_LENGTH -> 0.44f + random.nextFloat() * 0.16f
            DeliveryType.SLOWER -> 0.34f + random.nextFloat() * 0.22f
            DeliveryType.SPIN_TURN -> 0.36f + random.nextFloat() * 0.22f
            null -> random.nextFloat().coerceIn(0.06f, 0.92f)
        }
        val pitchX = when {
            outcome == BallOutcomeType.WIDE -> if (random.nextBoolean()) -0.92f else 0.92f
            bowler.role == PlayerRole.SPINNER -> ((random.nextFloat() - 0.45f) * 1.1f).coerceIn(-0.85f, 0.85f)
            else -> ((random.nextFloat() - 0.55f) * 1.0f).coerceIn(-0.85f, 0.85f)
        }
        val newPitchPoints = (current.pitchMapPoints + PitchMapPoint(
            bowlerId = bowler.id,
            bowlerName = bowler.name,
            lineX = pitchX,
            lengthY = pitchY,
            outcome = outcome,
            isSpin = bowler.role == PlayerRole.SPINNER,
            overLabel = overLabel
        )).takeLast(140)

        // Generate a reviewable DRS decision on close LBW or caught-behind calls during interactive play
        val generatedDrs: DrsPendingDecision? = if (interactiveInput != null && !inningsEnded) {
            buildReviewableDrsIfClose(
                outcome = outcome,
                dismissalDesc = dismissalDesc,
                inningsNumber = state.activeInningsNumber,
                overLabel = overLabel,
                batter = batter,
                bowler = bowler,
                battingTeamId = current.battingTeamId,
                battingTeamName = current.battingTeamName,
                bowlingTeamId = current.bowlingTeamId,
                bowlingTeamName = current.bowlingTeamName,
                battingReviewsLeft = current.battingTeamReviewsRemaining,
                bowlingReviewsLeft = current.bowlingTeamReviewsRemaining,
                interactiveRole = interactiveInput.role,
                pitchX = pitchX,
                pitchY = pitchY,
                random = random
            )
        } else null

        val updatedInnings = current.copy(
            totalRuns = newTotalRuns,
            wickets = newWickets,
            legalBalls = newLegalBalls,
            extras = newExtras,
            partnershipRuns = newPartnership,
            strikerIdx = nextStrikerIdx,
            nonStrikerIdx = nextNonStrikerIdx,
            nextBatterIdx = nextInLine,
            currentBowlerIdx = nextBowlerIdx,
            lastOverBowlerId = nextLastOverBowlerId,
            bowlingOptions = nextBowlingOptions,
            battingCard = finalBattingCard,
            bowlingCard = nextBowlingCard,
            wormByOver = updatedWorm,
            wicketOvers = updatedWicketOvers,
            fallOfWickets = updatedFow,
            recentBalls = (current.recentBalls + event).takeLast(12),
            injuredPlayerIds = updatedInjuredIds,
            completed = inningsEnded,
            wagonWheelShots = newWagonShots,
            pitchMapPoints = newPitchPoints
        )

        val updatedFeed = (newEvents + state.commentaryFeed).take(220)

        return if (state.activeInningsNumber == 1) {
            state.copy(
                innings1 = updatedInnings,
                activeInningsNumber = if (inningsEnded) 2 else 1,
                fieldPreset = activePreset,
                pendingDrs = generatedDrs,
                injuryAlerts = updatedInjuryAlerts.takeLast(4),
                commentaryFeed = updatedFeed
            )
        } else {
            val nextState = state.copy(
                innings2 = updatedInnings,
                fieldPreset = activePreset,
                pendingDrs = generatedDrs,
                injuryAlerts = updatedInjuryAlerts.takeLast(4),
                commentaryFeed = updatedFeed
            )
            if (inningsEnded) finalizeOrTriggerSuperOver(nextState, random) else nextState
        }
    }

    private fun buildReviewableDrsIfClose(
        outcome: BallOutcomeType,
        dismissalDesc: String,
        inningsNumber: Int,
        overLabel: String,
        batter: Cricketer,
        bowler: Cricketer,
        battingTeamId: String,
        battingTeamName: String,
        bowlingTeamId: String,
        bowlingTeamName: String,
        battingReviewsLeft: Int,
        bowlingReviewsLeft: Int,
        interactiveRole: InteractivePlayRole,
        pitchX: Float,
        pitchY: Float,
        random: Random
    ): DrsPendingDecision? {
        if (outcome == BallOutcomeType.WICKET && battingReviewsLeft > 0) {
            val isLbw = dismissalDesc.startsWith("lbw", ignoreCase = true)
            val isCaughtBehind = dismissalDesc.contains("c ", ignoreCase = true) && !dismissalDesc.contains("c & b", ignoreCase = true)
            if (isLbw || (isCaughtBehind && random.nextDouble() < 0.55)) {
                val mode = if (isLbw) DrsMode.BALL_TRACKING else DrsMode.ULTRA_EDGE
                val roll = random.nextDouble()
                val overturned = roll < 0.38
                val umpiresCall = !overturned && roll < 0.58
                val pitching = if (overturned && mode == DrsMode.BALL_TRACKING && random.nextBoolean()) "OUTSIDE LEG" else "IN LINE"
                val impact = if (overturned && mode == DrsMode.BALL_TRACKING && pitching == "IN LINE") "OUTSIDE OFF" else "IN LINE"
                val wickets = when {
                    overturned && mode == DrsMode.BALL_TRACKING -> "MISSING STUMPS"
                    overturned && mode == DrsMode.ULTRA_EDGE -> "FLAT LINE (NO EDGE)"
                    umpiresCall -> "UMPIRE'S CALL"
                    mode == DrsMode.ULTRA_EDGE -> "CLEAR SPIKE ON BAT"
                    else -> "HITTING STUMPS"
                }
                return DrsPendingDecision(
                    decisionId = System.nanoTime(),
                    mode = mode,
                    originalDecisionOut = true,
                    overturned = overturned,
                    umpiresCall = umpiresCall,
                    reviewingTeamId = battingTeamId,
                    reviewingTeamName = battingTeamName,
                    isBattingTeamReview = true,
                    batterId = batter.id,
                    batterName = batter.name,
                    bowlerName = bowler.name,
                    dismissalLabel = dismissalDesc,
                    pitchingLabel = pitching,
                    impactLabel = impact,
                    wicketsLabel = wickets,
                    spikeIntensity = if (mode == DrsMode.ULTRA_EDGE && !overturned) 0.88f else 0.12f,
                    ballPitchX = pitchX,
                    ballPitchY = pitchY,
                    inningsNumber = inningsNumber,
                    overLabel = overLabel
                )
            }
        } else if (outcome == BallOutcomeType.DOT && interactiveRole == InteractivePlayRole.BOWLING && bowlingReviewsLeft > 0 && random.nextDouble() < 0.16) {
            // Close Not-Out appeal for bowling team to review (LBW or Caught-Behind edge)
            val isLbwAppeal = random.nextBoolean()
            val mode = if (isLbwAppeal) DrsMode.BALL_TRACKING else DrsMode.ULTRA_EDGE
            val roll = random.nextDouble()
            val overturned = roll < 0.42
            val umpiresCall = !overturned && roll < 0.65
            val wickets = when {
                overturned && mode == DrsMode.BALL_TRACKING -> "3 REDS • HITTING!"
                overturned && mode == DrsMode.ULTRA_EDGE -> "SHARP SPIKE • EDGED!"
                umpiresCall -> "UMPIRE'S CALL"
                mode == DrsMode.ULTRA_EDGE -> "NO SPIKE (FLAT)"
                else -> "MISSING LEG STUMP"
            }
            return DrsPendingDecision(
                decisionId = System.nanoTime(),
                mode = mode,
                originalDecisionOut = false,
                overturned = overturned,
                umpiresCall = umpiresCall,
                reviewingTeamId = bowlingTeamId,
                reviewingTeamName = bowlingTeamName,
                isBattingTeamReview = false,
                batterId = batter.id,
                batterName = batter.name,
                bowlerName = bowler.name,
                dismissalLabel = if (isLbwAppeal) "lbw b ${bowler.name}" else "c †WK b ${bowler.name}",
                pitchingLabel = "IN LINE",
                impactLabel = if (overturned || umpiresCall) "IN LINE" else "OUTSIDE OFF",
                wicketsLabel = wickets,
                spikeIntensity = if (mode == DrsMode.ULTRA_EDGE && overturned) 0.92f else 0.10f,
                ballPitchX = pitchX,
                ballPitchY = pitchY,
                inningsNumber = inningsNumber,
                overLabel = overLabel
            )
        }
        return null
    }

    /**
     * Hard-enforces BOTH rules for every over transition:
     * 1. Strict over cap (legalBalls < 24 in T20/T20I, < 60 in ODI).
     * 2. No consecutive overs (playerId != lastOverBowlerId).
     * If injuries ever leave zero eligible bowlers in the initial 6-man attack, promotes the best
     * available outfielder from the full Playing XI so the cap & rotation are NEVER broken.
     */
    private fun resolveValidNextBowler(
        format: MatchFormat,
        nextOverIndex: Int,
        lastOverBowlerId: String,
        currentOptions: List<Cricketer>,
        currentCard: List<BowlerScoreEntry>,
        fullXi: List<Cricketer>,
        injuredIds: Set<String>,
        ballsNeededInOver: Int = 6
    ): Triple<List<Cricketer>, List<BowlerScoreEntry>, Int> {
        val maxBallsPerBowler = format.maxBallsPerBowler
        val options = currentOptions.toMutableList()
        val card = currentCard.toMutableList()

        fun fitsQuota(entry: BowlerScoreEntry): Boolean =
            format == MatchFormat.TEST || (entry.legalBalls + ballsNeededInOver <= maxBallsPerBowler)

        fun findEligibleIndices(requireFit: Boolean): List<Int> =
            card.indices.filter { idx ->
                val entry = card[idx]
                val notConsecutive = lastOverBowlerId.isBlank() || entry.playerId != lastOverBowlerId
                val underCap = fitsQuota(entry)
                val fitOk = !requireFit || !entry.injured
                notConsecutive && underCap && fitOk
            }

        var eligible = findEligibleIndices(requireFit = true)
        if (eligible.isEmpty()) {
            // Promote an unbowled fit player from the Playing XI (part-timer cover) so 4-over limit & rotation NEVER break!
            val existingIds = options.map { it.id }.toSet()
            val backupCandidates = fullXi
                .filter { it.id !in existingIds && it.id !in injuredIds && it.id != lastOverBowlerId }
                .sortedByDescending { it.bowlingRating }
            for (candidate in backupCandidates) {
                options.add(candidate)
                card.add(BowlerScoreEntry(candidate.id, candidate.name, candidate.role.shortLabel, stamina = 100))
                eligible = findEligibleIndices(requireFit = true)
                if (eligible.isNotEmpty()) break
            }
        }
        if (eligible.isEmpty()) {
            // Extreme fallback: any under-cap non-consecutive bowler even if carrying a niggle
            eligible = findEligibleIndices(requireFit = false)
        }
        if (eligible.isEmpty()) {
            // Absolute final safety: any under-cap bowler (never exceed maxBallsPerBowler)
            eligible = card.indices.filter { fitsQuota(card[it]) }
        }
        if (eligible.isEmpty()) {
            return Triple(options, card, 0)
        }

        val chosenIdx = when (format) {
            MatchFormat.T20I -> when {
                nextOverIndex in 6..14 -> eligible.maxByOrNull { idx ->
                    val b = options[idx]
                    b.bowlingRating + if (b.role == PlayerRole.SPINNER) 14 else 0
                } ?: eligible.first()
                nextOverIndex >= 15 -> eligible.maxByOrNull { idx ->
                    val b = options[idx]
                    b.bowlingRating + if (b.role == PlayerRole.SEAMER) 12 else 0
                } ?: eligible.first()
                else -> eligible.maxByOrNull { idx ->
                    val b = options[idx]
                    b.bowlingRating + if (b.role == PlayerRole.SEAMER) 10 else 0
                } ?: eligible.first()
            }
            MatchFormat.ODI -> {
                eligible.maxByOrNull { idx ->
                    val b = options[idx]
                    val staminaBonus = (card[idx].stamina - 60) / 4
                    val phaseBonus = when {
                        nextOverIndex < 10 && b.role == PlayerRole.SEAMER -> 12
                        nextOverIndex in 10..39 && b.role == PlayerRole.SPINNER -> 13
                        nextOverIndex >= 40 && b.role == PlayerRole.SEAMER -> 12
                        else -> 0
                    }
                    b.bowlingRating + phaseBonus + staminaBonus
                } ?: eligible.first()
            }
            MatchFormat.TEST -> {
                val freshPool = eligible.filter { card[it].stamina >= 45 }.ifEmpty { eligible }
                freshPool.maxByOrNull { idx ->
                    val b = options[idx]
                    val staminaScore = card[idx].stamina / 2
                    val phaseBonus = when {
                        nextOverIndex < 25 && b.role == PlayerRole.SEAMER -> 14
                        nextOverIndex >= 45 && b.role == PlayerRole.SPINNER -> 16
                        else -> 0
                    }
                    b.bowlingRating + phaseBonus + staminaScore
                } ?: freshPool.first()
            }
        }

        return Triple(options, card, chosenIdx)
    }

    // --- SUPER OVER ENGINE (T20 / T20I Tie-Breaker) ---

    private fun startSuperOverRound(
        state: LiveMatchState,
        roundNumber: Int
    ): LiveMatchState {
        // ICC Rule: Team that batted 2nd in regulation bats 1st in Super Over #1 (alternating on subsequent Super Overs)
        val firstBattingTeam = if (roundNumber % 2 == 1) state.teamB else state.teamA
        val secondBattingTeam = if (roundNumber % 2 == 1) state.teamA else state.teamB

        val firstXi = if (firstBattingTeam.teamId == state.teamA.teamId) state.innings1.battingLineup else state.innings2.battingLineup
        val secondXi = if (secondBattingTeam.teamId == state.teamA.teamId) state.innings1.battingLineup else state.innings2.battingLineup

        val so1 = buildSuperOverInnings(roundNumber, 1, firstBattingTeam, secondBattingTeam, firstXi, secondXi)
        val so2 = buildSuperOverInnings(roundNumber, 2, secondBattingTeam, firstBattingTeam, secondXi, firstXi)

        val announcementText = if (roundNumber == 1) {
            "🔥 SUPER OVER #1! Match tied at ${state.innings1.totalRuns}! 1 over (6 balls), max 2 wickets — ${firstBattingTeam.teamName} to bat first!"
        } else {
            "🔥🔥 SUPER OVER #$roundNumber! Previous Super Over was TIED! Back for another 6-ball shootout — ${firstBattingTeam.teamName} to bat first!"
        }

        val bannerEvent = BallEvent(
            inningsNumber = 2 + roundNumber,
            overLabel = "SO$roundNumber",
            batterName = firstBattingTeam.teamName,
            bowlerName = secondBattingTeam.teamName,
            runsScored = 0,
            outcome = BallOutcomeType.DOT,
            commentary = announcementText,
            scoreAfterBall = "SUPER OVER #$roundNumber",
            isSuperOver = true
        )

        return state.copy(
            superOverRound = roundNumber,
            activeSuperOverInningsNumber = 1,
            superOverInnings1 = so1,
            superOverInnings2 = so2,
            commentaryFeed = (listOf(bannerEvent) + state.commentaryFeed).take(220)
        )
    }

    private fun buildSuperOverInnings(
        roundNumber: Int,
        inningsNumber: Int,
        battingTeam: TeamState,
        bowlingTeam: TeamState,
        battingXi: List<Cricketer>,
        bowlingXi: List<Cricketer>
    ): SuperOverInnings {
        val top3Batters = battingXi.sortedByDescending { it.battingRating }.take(3).ifEmpty { battingXi.take(3) }
        val strikeBowler = bowlingXi.maxByOrNull {
            it.bowlingRating + if (it.role == PlayerRole.SEAMER) 6 else 0
        } ?: bowlingXi.first()

        val battingCard = top3Batters.mapIndexed { idx, p ->
            BatterScoreEntry(
                playerId = p.id,
                playerName = p.name,
                roleLabel = p.role.shortLabel,
                dismissalText = if (idx < 2) "batting" else "yet to bat"
            )
        }
        return SuperOverInnings(
            roundNumber = roundNumber,
            inningsNumber = inningsNumber,
            battingTeamId = battingTeam.teamId,
            battingTeamName = battingTeam.teamName,
            bowlingTeamId = bowlingTeam.teamId,
            bowlingTeamName = bowlingTeam.teamName,
            batters = top3Batters,
            bowler = strikeBowler,
            battingCard = battingCard,
            bowlingEntry = BowlerScoreEntry(strikeBowler.id, strikeBowler.name, strikeBowler.role.shortLabel)
        )
    }

    private fun simulateSuperOverBall(state: LiveMatchState, random: Random): LiveMatchState {
        val so = state.activeSuperOverInnings ?: return finalizeMatch(state)
        if (so.completed) {
            return if (state.activeSuperOverInningsNumber == 1) {
                state.copy(activeSuperOverInningsNumber = 2)
            } else {
                resolveCompletedSuperOverRound(state)
            }
        }

        val batter = so.striker ?: return resolveCompletedSuperOverRound(state)
        val bowler = so.bowler
        val ballNum = so.legalBalls + 1
        val overLabel = "SO${so.roundNumber}.$ballNum"

        val chasePressure = if (state.activeSuperOverInningsNumber == 2) {
            val needed = ((state.superOverInnings1?.totalRuns ?: 0) + 1) - so.totalRuns
            val ballsLeft = (6 - so.legalBalls).coerceAtLeast(1)
            val rrr = (needed * 6.0) / ballsLeft
            ((rrr - 10.0) / 10.0).coerceIn(-0.1, 0.35)
        } else 0.12

        val ratingDiff = (batter.battingRating - bowler.bowlingRating) / 100.0
        val outcome = rollBallOutcome(
            format = MatchFormat.T20I,
            overNumber = 19, // Death-over intensity!
            ratingDiff = ratingDiff,
            fieldingFactor = 0.0,
            chasePressure = chasePressure,
            random = random
        )

        val isExtra = outcome == BallOutcomeType.WIDE || outcome == BallOutcomeType.NO_BALL
        val runsFromBall = when (outcome) {
            BallOutcomeType.DOT, BallOutcomeType.WICKET -> 0
            BallOutcomeType.SINGLE, BallOutcomeType.WIDE, BallOutcomeType.NO_BALL -> 1
            BallOutcomeType.DOUBLE -> 2
            BallOutcomeType.TRIPLE -> 3
            BallOutcomeType.FOUR -> 4
            BallOutcomeType.SIX -> 6
        }

        val newTotalRuns = so.totalRuns + runsFromBall
        val newExtras = so.extras + if (isExtra) 1 else 0
        val newLegalBalls = so.legalBalls + if (isExtra) 0 else 1
        val newWickets = so.wickets + if (outcome == BallOutcomeType.WICKET) 1 else 0

        val updatedCard = so.battingCard.toMutableList()
        val strikerEntry = updatedCard[so.strikerIdx]
        val dismissalDesc = if (outcome == BallOutcomeType.WICKET) "b ${bowler.name}" else strikerEntry.dismissalText
        updatedCard[so.strikerIdx] = strikerEntry.copy(
            runs = strikerEntry.runs + if (isExtra) 0 else runsFromBall,
            balls = strikerEntry.balls + if (outcome == BallOutcomeType.WIDE) 0 else 1,
            fours = strikerEntry.fours + if (outcome == BallOutcomeType.FOUR) 1 else 0,
            sixes = strikerEntry.sixes + if (outcome == BallOutcomeType.SIX) 1 else 0,
            out = outcome == BallOutcomeType.WICKET,
            dismissalText = if (outcome == BallOutcomeType.WICKET) dismissalDesc else "batting"
        )

        val updatedBowler = so.bowlingEntry.copy(
            legalBalls = so.bowlingEntry.legalBalls + if (isExtra) 0 else 1,
            runsConceded = so.bowlingEntry.runsConceded + runsFromBall,
            wickets = so.bowlingEntry.wickets + if (outcome == BallOutcomeType.WICKET) 1 else 0
        )

        var nextStrikerIdx = so.strikerIdx
        var nextNonStrikerIdx = so.nonStrikerIdx
        var nextInLine = so.nextBatterIdx
        if (outcome == BallOutcomeType.WICKET) {
            if (nextInLine < so.batters.size) {
                nextStrikerIdx = nextInLine
                updatedCard[nextInLine] = updatedCard[nextInLine].copy(dismissalText = "batting")
                nextInLine += 1
            }
        } else if (runsFromBall == 1 || runsFromBall == 3) {
            val tmp = nextStrikerIdx
            nextStrikerIdx = nextNonStrikerIdx
            nextNonStrikerIdx = tmp
        }

        val targetToBeat = if (state.activeSuperOverInningsNumber == 2) state.superOverInnings1?.totalRuns else null
        val chaseWon = targetToBeat != null && newTotalRuns > targetToBeat
        // Super Over Rule: 6 legal balls OR 2 wickets lost ends the Super Over innings!
        val inningsEnded = newWickets >= 2 || newLegalBalls >= 6 || chaseWon

        val finalCard = if (inningsEnded) {
            updatedCard.map { if (it.dismissalText == "batting") it.copy(dismissalText = "not out") else it }
        } else updatedCard

        val scoreAfter = "$newTotalRuns/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6})"
        val commentary = "🔥 [SUPER OVER #${so.roundNumber}] " + buildCommentary(
            overLabel = overLabel,
            batter = batter,
            bowler = bowler,
            outcome = outcome,
            dismissalDesc = dismissalDesc,
            batterRunsAfter = finalCard[so.strikerIdx].runs,
            batterBallsAfter = finalCard[so.strikerIdx].balls,
            random = random
        )

        val event = BallEvent(
            inningsNumber = 2 + so.roundNumber,
            overLabel = overLabel,
            batterName = batter.name,
            bowlerName = bowler.name,
            runsScored = runsFromBall,
            outcome = outcome,
            commentary = commentary,
            scoreAfterBall = scoreAfter,
            isSuperOver = true
        )

        val updatedSo = so.copy(
            totalRuns = newTotalRuns,
            wickets = newWickets,
            legalBalls = newLegalBalls,
            extras = newExtras,
            strikerIdx = nextStrikerIdx,
            nonStrikerIdx = nextNonStrikerIdx,
            nextBatterIdx = nextInLine,
            battingCard = finalCard,
            bowlingEntry = updatedBowler,
            recentBalls = (so.recentBalls + event).takeLast(12),
            completed = inningsEnded
        )

        val updatedFeed = (listOf(event) + state.commentaryFeed).take(220)

        return if (state.activeSuperOverInningsNumber == 1) {
            state.copy(
                superOverInnings1 = updatedSo,
                activeSuperOverInningsNumber = if (inningsEnded) 2 else 1,
                commentaryFeed = updatedFeed
            )
        } else {
            val nextState = state.copy(
                superOverInnings2 = updatedSo,
                commentaryFeed = updatedFeed
            )
            if (inningsEnded) resolveCompletedSuperOverRound(nextState) else nextState
        }
    }

    private fun resolveCompletedSuperOverRound(state: LiveMatchState): LiveMatchState {
        val so1 = state.superOverInnings1 ?: return finalizeMatch(state)
        val so2 = state.superOverInnings2 ?: return finalizeMatch(state)
        val round = state.superOverRound

        val outcomeNote = when {
            so1.totalRuns > so2.totalRuns -> "${so1.battingTeamName} won Super Over #$round (${so1.totalRuns}/${so1.wickets} vs ${so2.totalRuns}/${so2.wickets})"
            so2.totalRuns > so1.totalRuns -> "${so2.battingTeamName} won Super Over #$round (${so2.totalRuns}/${so2.wickets} vs ${so1.totalRuns}/${so1.wickets})"
            else -> "Super Over #$round TIED at ${so1.totalRuns}!"
        }

        val summary = SuperOverRoundSummary(
            roundNumber = round,
            team1Id = so1.battingTeamId,
            team1Name = so1.battingTeamName,
            team1Score = so1.totalRuns,
            team1Wickets = so1.wickets,
            team1Overs = so1.oversFormatted,
            team2Id = so2.battingTeamId,
            team2Name = so2.battingTeamName,
            team2Score = so2.totalRuns,
            team2Wickets = so2.wickets,
            team2Overs = so2.oversFormatted,
            outcomeNote = outcomeNote
        )

        val stateWithHistory = state.copy(superOverHistory = state.superOverHistory + summary)

        // If tied again in Super Over, keep running another Super Over until there is a winner!
        if (so1.totalRuns == so2.totalRuns) {
            return startSuperOverRound(stateWithHistory, roundNumber = round + 1)
        }

        val winnerId = if (so1.totalRuns > so2.totalRuns) so1.battingTeamId else so2.battingTeamId
        val winnerName = if (so1.totalRuns > so2.totalRuns) so1.battingTeamName else so2.battingTeamName
        val fullSummary = "$winnerName won via Super Over #$round (${so1.totalRuns}/${so1.wickets} vs ${so2.totalRuns}/${so2.wickets}, tied ${state.innings1.totalRuns})"
        val potm = pickPlayerOfTheMatch(state.innings1, state.innings2)

        return stateWithHistory.copy(
            finished = true,
            winnerTeamId = winnerId,
            winnerTeamName = winnerName,
            resultSummary = fullSummary,
            playerOfTheMatch = potm
        )
    }

    private fun rollBallOutcome(
        format: MatchFormat,
        overNumber: Int,
        ratingDiff: Double,
        fieldingFactor: Double,
        chasePressure: Double,
        random: Random,
        interactiveInput: InteractiveBallInput? = null,
        fieldPreset: FieldPreset = FieldPreset.BALANCED,
        dewActive: Boolean = false,
        lowBowlerStamina: Boolean = false
    ): BallOutcomeType {
        var pWide: Double
        var pNoBall: Double
        var pWicket: Double
        var pSix: Double
        var pFour: Double
        val pTriple: Double
        val pDouble: Double
        val pSingle: Double

        when (format) {
            MatchFormat.T20I -> {
                val isPowerplay = overNumber < 6
                val isDeath = overNumber >= 15
                pWide = 0.024
                pNoBall = 0.008
                pWicket = (0.047 - ratingDiff * 0.032 + fieldingFactor * 0.012 +
                    (if (isDeath) 0.019 else 0.0) + maxOf(0.0, chasePressure * 0.03)).coerceIn(0.018, 0.125)
                pSix = (0.058 + ratingDiff * 0.068 + (if (isDeath) 0.044 else if (isPowerplay) 0.020 else 0.0) +
                    maxOf(0.0, chasePressure * 0.04)).coerceIn(0.015, 0.20)
                pFour = (0.124 + ratingDiff * 0.075 - fieldingFactor * 0.024 +
                    (if (isPowerplay) 0.040 else if (isDeath) 0.024 else 0.0)).coerceIn(0.04, 0.24)
                pTriple = 0.008
                pDouble = (0.078 - fieldingFactor * 0.015).coerceIn(0.04, 0.12)
                pSingle = 0.355
            }
            MatchFormat.ODI -> {
                val isPowerplay = overNumber < 10
                val isSlog = overNumber >= 40
                pWide = 0.018
                pNoBall = 0.005
                pWicket = (0.025 - ratingDiff * 0.018 + fieldingFactor * 0.008 +
                    (if (isSlog) 0.016 else 0.0) + maxOf(0.0, chasePressure * 0.02)).coerceIn(0.010, 0.085)
                pSix = (0.022 + ratingDiff * 0.032 + (if (isSlog) 0.042 else if (isPowerplay) 0.012 else 0.0) +
                    maxOf(0.0, chasePressure * 0.025)).coerceIn(0.006, 0.12)
                pFour = (0.094 + ratingDiff * 0.055 - fieldingFactor * 0.020 +
                    (if (isPowerplay) 0.035 else if (isSlog) 0.026 else 0.0)).coerceIn(0.03, 0.18)
                pTriple = 0.009
                pDouble = (0.072 - fieldingFactor * 0.012).coerceIn(0.04, 0.11)
                pSingle = 0.355
            }
            MatchFormat.TEST -> {
                val isNewBall = overNumber < 20
                pWide = 0.006
                pNoBall = 0.007
                pWicket = (0.016 - ratingDiff * 0.014 + fieldingFactor * 0.007 +
                    (if (isNewBall) 0.005 else 0.0)).coerceIn(0.007, 0.055)
                pSix = (0.004 + ratingDiff * 0.009).coerceIn(0.001, 0.022)
                pFour = (0.072 + ratingDiff * 0.042 - fieldingFactor * 0.015).coerceIn(0.025, 0.13)
                pTriple = 0.010
                pDouble = 0.046
                pSingle = 0.215
            }
        }

        // Apply crisp interactive risk/reward modifiers when user uses the Action View controls
        if (interactiveInput != null) {
            when (interactiveInput.role) {
                InteractivePlayRole.BATTING -> {
                    val loftBonus = if (interactiveInput.shotDirection.isLofted) 0.11 else 0.0
                    val loftRisk = if (interactiveInput.shotDirection.isLofted) 0.035 else 0.0
                    when (interactiveInput.timingGrade) {
                        TimingGrade.PERFECT -> {
                            pSix = (pSix + 0.14 + loftBonus).coerceAtMost(0.42)
                            pFour = (pFour + 0.16).coerceAtMost(0.36)
                            pWicket = (pWicket - 0.03 + loftRisk * 0.35).coerceAtLeast(0.008)
                        }
                        TimingGrade.GOOD -> {
                            pFour = (pFour + 0.07).coerceAtMost(0.30)
                            pSix = (pSix + 0.04 + loftBonus * 0.5).coerceAtMost(0.28)
                            pWicket = (pWicket + loftRisk * 0.6).coerceIn(0.01, 0.16)
                        }
                        TimingGrade.EARLY, TimingGrade.LATE -> {
                            pSix = (pSix * 0.35).coerceAtLeast(0.005)
                            pFour = (pFour * 0.45).coerceAtLeast(0.015)
                            pWicket = (pWicket + interactiveInput.timingGrade.riskDelta + loftRisk).coerceAtMost(0.28)
                        }
                    }
                }
                InteractivePlayRole.BOWLING -> {
                    val deliveryWktBonus = interactiveInput.deliveryType.wicketBonus
                    when (interactiveInput.timingGrade) {
                        TimingGrade.PERFECT -> {
                            pWicket = (pWicket + 0.055 + deliveryWktBonus).coerceAtMost(0.24)
                            pSix = (pSix * 0.35).coerceAtLeast(0.005)
                            pFour = (pFour * 0.45).coerceAtLeast(0.02)
                        }
                        TimingGrade.GOOD -> {
                            pWicket = (pWicket + 0.018 + deliveryWktBonus * 0.5).coerceAtMost(0.18)
                            pSix = (pSix * 0.70).coerceAtLeast(0.01)
                        }
                        TimingGrade.EARLY, TimingGrade.LATE -> {
                            pWicket = (pWicket * 0.45).coerceAtLeast(0.006)
                            pFour = (pFour + 0.09).coerceAtMost(0.32)
                            pSix = (pSix + 0.06).coerceAtMost(0.26)
                        }
                    }
                }
            }
        }

        // Apply Field Preset, 2nd-Innings Dew, and Low Bowler Stamina tactical modifiers
        pWicket = (pWicket + fieldPreset.wicketDelta).coerceIn(0.008, 0.26)
        pFour = (pFour + fieldPreset.boundaryDelta * 0.65).coerceIn(0.018, 0.36)
        pSix = (pSix + fieldPreset.boundaryDelta * 0.45).coerceIn(0.005, 0.34)
        if (dewActive) {
            pWide = (pWide + 0.014).coerceAtMost(0.07)
            pFour = (pFour + 0.018).coerceAtMost(0.38)
            pSix = (pSix + 0.012).coerceAtMost(0.36)
        }
        if (lowBowlerStamina) {
            pWide = (pWide + 0.010).coerceAtMost(0.08)
            pNoBall = (pNoBall + 0.006).coerceAtMost(0.03)
            pFour = (pFour + 0.015).coerceAtMost(0.38)
        }

        val roll = random.nextDouble()
        var cursor = 0.0
        return when {
            roll < (cursor + pWide).also { cursor = it } -> BallOutcomeType.WIDE
            roll < (cursor + pNoBall).also { cursor = it } -> BallOutcomeType.NO_BALL
            roll < (cursor + pWicket).also { cursor = it } -> BallOutcomeType.WICKET
            roll < (cursor + pSix).also { cursor = it } -> BallOutcomeType.SIX
            roll < (cursor + pFour).also { cursor = it } -> BallOutcomeType.FOUR
            roll < (cursor + pTriple).also { cursor = it } -> BallOutcomeType.TRIPLE
            roll < (cursor + pDouble).also { cursor = it } -> BallOutcomeType.DOUBLE
            roll < (cursor + pSingle).also { cursor = it } -> BallOutcomeType.SINGLE
            else -> BallOutcomeType.DOT
        }
    }

    fun simulateNextOver(state: LiveMatchState, random: Random = Random.Default): LiveMatchState {
        if (state.finished) return state
        if (state.isSuperOverActive) {
            val startRound = state.superOverRound
            val startSoInn = state.activeSuperOverInningsNumber
            var s = state
            var safety = 0
            while (!s.finished && s.isSuperOverActive && s.superOverRound == startRound && s.activeSuperOverInningsNumber == startSoInn && safety < 24) {
                s = simulateNextBall(s, random)
                safety++
            }
            return s
        }
        val startInnings = state.activeInningsNumber
        val startOver = state.activeInnings.legalBalls / 6
        var s = state
        var safety = 0
        while (!s.finished && !s.isSuperOverActive && s.activeInningsNumber == startInnings && (s.activeInnings.legalBalls / 6) == startOver && safety < 24) {
            s = simulateNextBall(s, random)
            safety++
        }
        return s
    }

    fun simulateFiveOvers(state: LiveMatchState, random: Random = Random.Default): LiveMatchState {
        var s = state
        repeat(5) {
            if (!s.finished) {
                s = simulateNextOver(s, random)
            }
        }
        return s
    }

    fun simulateCurrentInnings(state: LiveMatchState, random: Random = Random.Default): LiveMatchState {
        if (state.finished) return state
        if (state.isSuperOverActive) return simulateNextOver(state, random)
        val targetInnings = state.activeInningsNumber
        var s = state
        var safety = 0
        while (!s.finished && !s.isSuperOverActive && s.activeInningsNumber == targetInnings && safety < 1400) {
            s = simulateNextBall(s, random)
            safety++
        }
        return s
    }

    fun simulateFullMatch(state: LiveMatchState, random: Random = Random.Default): LiveMatchState {
        var s = state
        var safety = 0
        while (!s.finished && safety < 3200) {
            s = simulateNextBall(s, random)
            safety++
        }
        return s
    }

    private fun finalizeOrTriggerSuperOver(state: LiveMatchState, random: Random): LiveMatchState {
        val score1 = state.innings1.totalRuns
        val score2 = state.innings2.totalRuns
        // Strict T20 / T20I Rule: Tied match triggers a Super Over!
        if (state.format == MatchFormat.T20I && score1 == score2) {
            return startSuperOverRound(state, roundNumber = 1)
        }
        return finalizeMatch(state)
    }

    private fun finalizeMatch(state: LiveMatchState): LiveMatchState {
        val score1 = state.innings1.totalRuns
        val score2 = state.innings2.totalRuns
        val winnerId: String
        val winnerName: String
        val summary: String

        val isTestDraw = state.format == MatchFormat.TEST &&
            score1 > score2 &&
            state.innings2.wickets < 10 &&
            state.innings2.legalBalls >= state.format.maxBalls &&
            state.stage != MatchStage.WTC_FINAL

        when {
            isTestDraw -> {
                winnerId = "DRAW"
                winnerName = "Match Drawn"
                summary = "Test Match Drawn! ${state.teamB.teamName} held on at $score2/${state.innings2.wickets}"
            }
            score1 > score2 -> {
                val margin = score1 - score2
                winnerId = state.teamA.teamId
                winnerName = state.teamA.teamName
                summary = "${state.teamA.teamName} won by $margin runs"
            }
            score2 > score1 -> {
                val wicketsLeft = (10 - state.innings2.wickets).coerceAtLeast(1)
                val ballsLeft = (state.format.maxBalls - state.innings2.legalBalls).coerceAtLeast(0)
                winnerId = state.teamB.teamId
                winnerName = state.teamB.teamName
                summary = "${state.teamB.teamName} won by $wicketsLeft wickets ($ballsLeft balls left)"
            }
            else -> {
                winnerId = "TIE"
                winnerName = "Match Tied"
                summary = "Thrilling Tie! Both sides finished on $score1"
            }
        }

        val potm = pickPlayerOfTheMatch(state.innings1, state.innings2)

        return state.copy(
            finished = true,
            winnerTeamId = winnerId,
            winnerTeamName = winnerName,
            resultSummary = summary,
            playerOfTheMatch = potm
        )
    }

    private fun pickPlayerOfTheMatch(inn1: InningsState, inn2: InningsState): String {
        val impactScores = mutableMapOf<String, Pair<String, Double>>()
        (inn1.battingCard + inn2.battingCard).forEach { b ->
            if (b.balls > 0) {
                val pts = b.runs * 1.2 + b.sixes * 3.0 + b.fours * 1.5 +
                    (if (b.runs >= 100) 35.0 else if (b.runs >= 50) 15.0 else 0.0)
                val summary = "${b.playerName} (${b.runs} off ${b.balls})"
                val prev = impactScores[b.playerId]?.second ?: 0.0
                if (pts > prev) impactScores[b.playerId] = summary to pts
            }
        }
        (inn1.bowlingCard + inn2.bowlingCard).forEach { bw ->
            if (bw.legalBalls > 0) {
                val econBonus = if (bw.economy < 6.0 && bw.legalBalls >= 18) 15.0 else 0.0
                val pts = bw.wickets * 26.0 + bw.maidens * 10.0 + econBonus
                val summary = "${bw.playerName} (${bw.wickets}/${bw.runsConceded} in ${bw.oversFormatted})"
                val prev = impactScores[bw.playerId]?.second ?: 0.0
                if (pts > prev) impactScores[bw.playerId] = summary to pts
            }
        }
        return impactScores.values.maxByOrNull { it.second }?.first ?: "All-round Team Performance"
    }

    private fun generateDismissal(bowler: Cricketer, fielders: List<Cricketer>, random: Random): String {
        val fielder = fielders.filter { it.id != bowler.id }.randomOrNull(random)?.name ?: "Sub"
        return when (random.nextInt(5)) {
            0 -> "b ${bowler.name}"
            1 -> "lbw b ${bowler.name}"
            2 -> "c & b ${bowler.name}"
            else -> "c $fielder b ${bowler.name}"
        }
    }

    private fun buildCommentary(
        overLabel: String,
        batter: Cricketer,
        bowler: Cricketer,
        outcome: BallOutcomeType,
        dismissalDesc: String,
        batterRunsAfter: Int,
        batterBallsAfter: Int,
        random: Random
    ): String {
        val lead = "$overLabel • ${bowler.name} to ${batter.name}: "
        return when (outcome) {
            BallOutcomeType.SIX -> lead + listOf(
                "MASSIVE SIX! Picked up early and launched into the second tier!",
                "MAXIMUM! Clean swing through the line, sails over long-on!",
                "OUT OF THE PARK! Crowd erupts as that flies over deep mid-wicket!"
            ).random(random)
            BallOutcomeType.FOUR -> lead + listOf(
                "FOUR! Crisp cover drive pierces the infield like a laser!",
                "BOUNDARY! Width offered and brutally cut past backward point!",
                "FOUR! Timed to perfection down the ground, zero chance for long-off!"
            ).random(random)
            BallOutcomeType.WICKET ->
                "${lead}OUT! Big breakthrough — ${batter.name} $dismissalDesc for $batterRunsAfter ($batterBallsAfter)!"
            BallOutcomeType.WIDE ->
                "${lead}WIDE! Strays down the leg side, extra run added."
            BallOutcomeType.NO_BALL ->
                "${lead}NO BALL! Overstepped the crease — 1 run and pressure on the bowler!"
            BallOutcomeType.TRIPLE ->
                "${lead}THREE RUNS! Placed into the deep gap, electric running!"
            BallOutcomeType.DOUBLE ->
                "${lead}TWO RUNS! Tucked into the leg side and they sprint back for two."
            BallOutcomeType.SINGLE ->
                "${lead}1 run. Rotated smartly into the gap to keep the momentum going."
            BallOutcomeType.DOT -> lead + if (bowler.role == PlayerRole.SPINNER) {
                "Dot ball! Sharp turn and drift beats the outside edge."
            } else {
                "Dot ball! Heavy pace and tight length — dug out back to the bowler."
            }
        }
    }

    fun formatRate(rate: Double): String = String.format(Locale.US, "%.2f", rate)
}
