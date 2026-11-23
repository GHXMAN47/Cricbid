package com.example

import com.example.data.model.CricketerDatabase
import com.example.data.model.Ipl2026Catalog
import com.example.data.model.IplPlayMode
import com.example.data.model.IplRules
import com.example.data.model.IplSeasonPhase
import com.example.data.model.MatchFormat
import com.example.data.model.MatchStage
import com.example.data.model.PlayerRole
import com.example.engine.BallOutcomeType
import com.example.engine.DeliveryType
import com.example.engine.DrsMode
import com.example.engine.DrsPendingDecision
import com.example.engine.DrsReviewType
import com.example.engine.FieldPreset
import com.example.engine.InteractiveBallInput
import com.example.engine.InteractivePlayRole
import com.example.engine.IplScheduleGenerator
import com.example.engine.IplSeasonEngine
import com.example.engine.MatchSimulationEngine
import com.example.engine.TimingGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IplModeAndRulesUnitTest {

    @Test
    fun ipl2026Catalog_has10OfficialFranchisesCaptainsAndRcbDefendingChampion() {
        val franchises = Ipl2026Catalog.franchises
        assertEquals(10, franchises.size)

        val byCode = franchises.associateBy { it.shortCode }
        assertEquals(setOf("RCB", "GT", "CSK", "MI", "KKR", "PBKS", "LSG", "DC", "RR", "SRH"), byCode.keys)

        val rcb = byCode.getValue("RCB")
        assertTrue(rcb.defendingChampion)
        assertEquals("Rajat Patidar", rcb.captainName)
        assertEquals(1, franchises.count { it.defendingChampion })

        assertEquals("Shubman Gill", byCode.getValue("GT").captainName)
        assertEquals("Ruturaj Gaikwad", byCode.getValue("CSK").captainName)
        assertEquals("Hardik Pandya", byCode.getValue("MI").captainName)
        assertEquals("Ajinkya Rahane", byCode.getValue("KKR").captainName)
        assertEquals("Shreyas Iyer", byCode.getValue("PBKS").captainName)
        assertEquals("Rishabh Pant", byCode.getValue("LSG").captainName)
        assertEquals("Axar Patel", byCode.getValue("DC").captainName)
        assertEquals("Riyan Parag", byCode.getValue("RR").captainName)
        assertEquals("Pat Cummins", byCode.getValue("SRH").captainName)

        // Vaibhav Sooryavanshi in RR roster & Emerging eligible
        val rrRosterNames = byCode.getValue("RR").coreRosterIds.mapNotNull { CricketerDatabase.getById(it)?.name }
        assertTrue("Vaibhav Sooryavanshi should be in RR roster", "Vaibhav Sooryavanshi" in rrRosterNames)
        assertTrue(Ipl2026Catalog.isEmergingEligible("ipl001"))

        // Deep auction pool supports 10 full 18–25 squads
        assertTrue(Ipl2026Catalog.fullIplAuctionPool().size >= 240)
    }

    @Test
    fun iplRetentionRules_enforceMax6TotalMax5CappedMax2UncappedAnd120CrPurse() {
        val season = IplSeasonEngine.createNewSeason(
            playMode = IplPlayMode.SOLO,
            humanSelections = listOf("ipl_rcb" to "Tester"),
            random = Random(42)
        )
        val rcb = season.franchiseById("ipl_rcb")!!
        val cappedIds = rcb.formerSquadIds.filter { !Ipl2026Catalog.isUncappedIndian(it) }
        val uncappedIds = rcb.formerSquadIds.filter { Ipl2026Catalog.isUncappedIndian(it) }

        assertTrue("RCB should have >= 6 capped players in former squad", cappedIds.size >= 6)
        assertTrue("RCB should have >= 1 uncapped Indian player", uncappedIds.isNotEmpty())

        // 6 capped must fail (max 5 capped)
        assertNotNull(IplSeasonEngine.validateRetentionSelection(rcb, cappedIds.take(6)))

        // 5 capped + 1 uncapped = 6 total must succeed
        val validSix = cappedIds.take(5) + uncappedIds.take(1)
        assertNull(IplSeasonEngine.validateRetentionSelection(rcb, validSix))

        // Check ₹18 + ₹14 + ₹11 + ₹18 + ₹14 Cr + ₹4 Cr = ₹79 Cr = 7900L deduction -> ₹41 Cr left
        val expectedCost = 1_800L + 1_400L + 1_100L + 1_800L + 1_400L + 400L
        assertEquals(expectedCost, IplRules.calculateRetentionCostLakhs(validSix))

        val afterRetain = IplSeasonEngine.submitRetentions(season, "ipl_rcb", validSix, Random(42)).getOrThrow()
        assertEquals(IplSeasonPhase.AUCTION, afterRetain.phase)
        val updatedRcb = afterRetain.franchiseById("ipl_rcb")!!
        assertEquals(IplRules.PURSE_LAKHS - expectedCost, updatedRcb.purseRemainingLakhs)
        assertEquals(0, updatedRcb.rtmCardsRemaining)
    }

    @Test
    fun iplMegaAuction_enforcesHammerLootCapOf2AndDraftsValid18To25Squads() {
        var season = IplSeasonEngine.createNewSeason(
            playMode = IplPlayMode.SOLO,
            humanSelections = listOf("ipl_mi" to "Host"),
            random = Random(7)
        )
        // Lock 2 retentions -> 4 RTM cards left, enters live auction
        val mi = season.franchiseById("ipl_mi")!!
        season = IplSeasonEngine.submitRetentions(season, "ipl_mi", mi.formerSquadIds.take(2), Random(7)).getOrThrow()
        assertEquals(IplSeasonPhase.AUCTION, season.phase)
        assertEquals(2, season.franchiseById("ipl_mi")!!.hammerLootRemaining)

        // Use Hammer Loot #1
        season = IplSeasonEngine.triggerHammerLoot(season, "ipl_mi").getOrThrow()
        assertEquals(1, season.franchiseById("ipl_mi")!!.hammerLootRemaining)

        // Use Hammer Loot #2
        season = IplSeasonEngine.triggerHammerLoot(season, "ipl_mi").getOrThrow()
        assertEquals(0, season.franchiseById("ipl_mi")!!.hammerLootRemaining)

        // Attempt Hammer Loot #3 -> must fail cleanly!
        val thirdAttempt = IplSeasonEngine.triggerHammerLoot(season, "ipl_mi")
        assertTrue("3rd Hammer Loot must be blocked at 0/2", thirdAttempt.isFailure)

        // Auto-complete remaining auction -> all 10 teams must have 18..25 players, <= 8 OS, >= 1 WK
        val drafted = IplSeasonEngine.autoCompleteRemainingAuction(season, Random(7))
        assertEquals(IplSeasonPhase.SQUAD_XI, drafted.phase)
        drafted.franchises.forEach { f ->
            assertTrue("${f.shortCode} squad size ${f.squadPlayerIds.size} must be >= 18", f.squadPlayerIds.size >= IplRules.MIN_SQUAD_SIZE)
            assertTrue("${f.shortCode} squad size ${f.squadPlayerIds.size} must be <= 25", f.squadPlayerIds.size <= IplRules.MAX_SQUAD_SIZE)
            assertTrue("${f.shortCode} OS ${f.overseasCount} must be <= 8", f.overseasCount <= IplRules.MAX_OVERSEAS_IN_SQUAD)
            assertTrue("${f.shortCode} must have >= 1 WK", f.wkCount >= 1)
            assertEquals(11, f.playingXiIds.size)
            assertEquals(5, f.impactNomineeIds.size)
        }
    }

    @Test
    fun iplScheduleGenerator_creates70Matches14PerTeamAndZeroBackToBack() {
        val season = IplSeasonEngine.createNewSeason(
            playMode = IplPlayMode.SOLO,
            humanSelections = listOf("ipl_rcb" to "Tester"),
            random = Random(123)
        )
        val schedule = IplScheduleGenerator.generate70MatchSchedule(
            franchises = season.franchises,
            humanFranchiseIds = season.humanFranchiseIds,
            random = Random(123)
        )
        assertEquals(70, schedule.size)

        val counts = mutableMapOf<String, Int>()
        schedule.forEach { fx ->
            counts[fx.teamAId] = (counts[fx.teamAId] ?: 0) + 1
            counts[fx.teamBId] = (counts[fx.teamBId] ?: 0) + 1
        }
        assertEquals(10, counts.size)
        counts.values.forEach { assertEquals(14, it) }

        // Verify zero back-to-back team games
        for (i in 1 until schedule.size) {
            val prev = schedule[i - 1]
            val curr = schedule[i]
            val prevTeams = setOf(prev.teamAId, prev.teamBId)
            val currTeams = setOf(curr.teamAId, curr.teamBId)
            assertTrue(
                "Match ${curr.matchNumber} should not have back-to-back teams with Match ${prev.matchNumber}",
                prevTeams.intersect(currTeams).isEmpty()
            )
        }
    }

    @Test
    fun iplFullSeasonSoloLifecycle_autoSimsAiMatchesTracksMatchFeesAndAwardsChampion() {
        val rng = Random(99)
        var season = IplSeasonEngine.createNewSeason(
            playMode = IplPlayMode.SOLO,
            humanSelections = listOf("ipl_rcb" to "ViratFan"),
            random = rng
        )
        val rcb = season.franchiseById("ipl_rcb")!!
        season = IplSeasonEngine.submitRetentions(season, "ipl_rcb", rcb.formerSquadIds.take(3), rng).getOrThrow()
        season = IplSeasonEngine.autoCompleteRemainingAuction(season, rng)
        val (leagueStarted, _) = IplSeasonEngine.enterLeagueStage(season, rng)
        season = leagueStarted

        assertEquals(IplSeasonPhase.LEAGUE, season.phase)
        assertEquals(70, season.schedule.size)

        // Play all human fixtures until season completes (league + playoffs if qualified, or auto-simmed playoffs)
        var humanMatchesPlayed = 0
        var safety = 0
        while (season.phase != IplSeasonPhase.COMPLETED && safety < 30) {
            safety++
            val nextFx = IplSeasonEngine.nextHumanFixture(season) ?: break
            assertTrue("Next playable fixture must involve human team", season.involvesHuman(nextFx))
            val fA = season.franchiseById(nextFx.teamAId)!!
            val fB = season.franchiseById(nextFx.teamBId)!!
            val simmed = MatchSimulationEngine.simulateFullMatch(
                MatchSimulationEngine.createMatch(
                    teamA = fA.toTeamState(),
                    teamB = fB.toTeamState(),
                    stage = nextFx.stage,
                    format = MatchFormat.T20I,
                    isIplRules = true,
                    teamAImpactNominees = fA.impactNomineeIds,
                    teamBImpactNominees = fB.impactNomineeIds,
                    random = rng
                ),
                rng
            )
            val record = simmed.toMatchRecord("IPL_2026_M${nextFx.matchNumber}", "u1", listOf("u1"))
            val (updated, _) = IplSeasonEngine.recordMatchAndAutoSimAi(season, nextFx.matchNumber, record, rng)
            season = updated
            humanMatchesPlayed++
        }

        assertTrue("Human should play at least 14 league matches", humanMatchesPlayed >= 14)
        assertEquals(IplSeasonPhase.COMPLETED, season.phase)
        assertEquals(70, season.leagueCompletedCount)
        assertEquals(4, season.playoffs.size)
        assertTrue(season.championTeamName.isNotBlank())

        // Match fee ledger check: every team played >= 14 games -> >= 14 * ₹90L = 1260L
        season.franchises.forEach { f ->
            assertTrue(f.matchesPaidCount >= 14)
            assertEquals(f.matchesPaidCount * 90.0, f.matchFeesSpentLakhs, 0.01)
        }

        val trophy = IplSeasonEngine.buildSeasonTrophyRecord(season)
        assertNotNull(trophy)
        assertTrue(trophy!!.orangeCapWinner.isNotBlank())
        assertTrue(trophy.purpleCapWinner.isNotBlank())
        assertTrue(trophy.mvpWinner.isNotBlank())
    }

    @Test
    fun officialIplMatchRules_powerplayCircleImpactOverseas2BouncersWideDrsAndUnlimitedSuperOver() {
        val rng = Random(2026)
        var season = IplSeasonEngine.createNewSeason(
            playMode = IplPlayMode.SOLO,
            humanSelections = listOf("ipl_rcb" to "Coach"),
            random = rng
        )
        season = IplSeasonEngine.submitRetentions(season, "ipl_rcb", emptyList(), rng).getOrThrow()
        season = IplSeasonEngine.autoCompleteRemainingAuction(season, rng)

        val fA = season.franchiseById("ipl_rcb")!!
        val fB = season.franchiseById("ipl_csk")!!

        // Craft a Starting XI with EXACTLY 4 overseas players and verify overseas Impact Sub is BLOCKED
        val allA = fA.players
        val osA = allA.filter { it.overseas }
        val indA = allA.filter { !it.overseas }
        val wkA = allA.first { it.role == PlayerRole.WICKETKEEPER }
        val fourOsXi = (listOf(wkA) + osA.filter { it.id != wkA.id }.take(if (wkA.overseas) 3 else 4) +
            indA.filter { it.id != wkA.id }).distinctBy { it.id }.take(11)
        val benchOs = osA.firstOrNull { p -> fourOsXi.none { it.id == p.id } }
        val benchInd = indA.first { p -> fourOsXi.none { it.id == p.id } }
        val nomineesA = listOfNotNull(benchOs?.id, benchInd.id) + allA.map { it.id }.filter { id -> fourOsXi.none { it.id == id } }
        val teamAWith4Os = fA.copy(
            playingXiIds = fourOsXi.map { it.id },
            impactNomineeIds = nomineesA.distinct().take(5)
        ).toTeamState()

        var match = MatchSimulationEngine.createMatch(
            teamA = teamAWith4Os,
            teamB = fB.toTeamState(),
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I,
            isIplRules = true,
            teamAImpactNominees = nomineesA.distinct().take(5),
            teamBImpactNominees = fB.impactNomineeIds,
            random = rng
        )

        // 1. Powerplay (Overs 1–6) max 2 fielders outside 30-yard circle
        assertEquals(2, match.innings1.maxFieldersOutsideCircle(isIplRules = true))
        val ppCoords = FieldPreset.DEFENSIVE.coordsForCircleLimit(2)
        assertTrue(FieldPreset.DEFENSIVE.countOutsideCircle(ppCoords) <= 2)

        // Overs 7–20 max 5 outside; under Slow Over-Rate Penalty max 4 outside
        val over8Inn = match.innings1.copy(legalBalls = 42)
        assertEquals(5, over8Inn.maxFieldersOutsideCircle(isIplRules = true))
        val penalizedMatch = MatchSimulationEngine.applySlowOverRatePenalty(
            match.copy(innings1 = over8Inn)
        )
        assertTrue(penalizedMatch.innings1.slowOverRatePenaltyActive)
        assertEquals(4, penalizedMatch.innings1.maxFieldersOutsideCircle(isIplRules = true))

        // 2. Impact Player strict overseas rule: with 4 OS in Starting XI, overseas sub must be REJECTED
        if (benchOs != null) {
            val blocked = MatchSimulationEngine.applyImpactSubstitution(
                state = match,
                teamId = match.teamA.teamId,
                benchPlayerId = benchOs.id,
                outPlayerId = fourOsXi.last().id
            )
            assertTrue("Overseas Impact Sub must be blocked when starting XI has 4 overseas", blocked.isFailure)
        }
        // Indian Impact Sub at innings start window must SUCCEED
        val allowedInd = MatchSimulationEngine.applyImpactSubstitution(
            state = match,
            teamId = match.teamA.teamId,
            benchPlayerId = benchInd.id,
            outPlayerId = fourOsXi.last().id
        )
        assertTrue("Indian Impact Sub should succeed at innings start", allowedInd.isSuccess)
        assertTrue(allowedInd.getOrThrow().teamAImpactUsed)

        // 3. Two Bouncers Per Over -> 3rd bouncer by pace bowler triggers No-Ball + Free Hit!
        val paceMatch = match.copy(
            innings1 = match.innings1.copy(
                bouncersThisOver = 2,
                bowlingOptions = match.innings1.bowlingOptions.mapIndexed { idx, c ->
                    if (idx == match.innings1.currentBowlerIdx) c.copy(specialty = "148 kph Express Fast") else c
                }
            )
        )
        val after3rdBouncer = MatchSimulationEngine.simulateNextBall(
            state = paceMatch,
            interactiveInput = InteractiveBallInput(
                role = InteractivePlayRole.BOWLING,
                deliveryType = DeliveryType.BOUNCER,
                timingGrade = TimingGrade.GOOD
            ),
            random = rng
        )
        assertTrue("3rd bouncer in over must trigger Free Hit on next ball", after3rdBouncer.innings1.isFreeHitNextBall)
        assertEquals(BallOutcomeType.NO_BALL, after3rdBouncer.innings1.recentBalls.last().outcome)

        // 4. Expanded DRS: Wide Call review overturned awards +1 run & keeps review
        val wideDrsState = match.copy(
            pendingDrs = DrsPendingDecision(
                decisionId = 101L,
                mode = DrsMode.WIDE_LINE_CHECK,
                originalDecisionOut = false,
                overturned = true,
                umpiresCall = false,
                reviewingTeamId = match.innings1.battingTeamId,
                reviewingTeamName = match.innings1.battingTeamName,
                isBattingTeamReview = true,
                batterId = match.innings1.striker?.id.orEmpty(),
                batterName = match.innings1.striker?.name ?: "Batter",
                bowlerName = match.innings1.currentBowler?.name ?: "Bowler",
                dismissalLabel = "Wide Ball Review (On-Field: Fair Delivery)",
                pitchingLabel = "Outside Wide Tramline",
                impactLabel = "Wide Confirmed",
                wicketsLabel = "WIDE +1 RUN",
                spikeIntensity = 0f,
                ballPitchX = 0.92f,
                ballPitchY = 0.5f,
                inningsNumber = 1,
                overLabel = "0.1",
                reviewType = DrsReviewType.WIDE_CALL,
                originalExtraCalled = false
            )
        )
        val afterWideDrs = MatchSimulationEngine.resolveDrsReview(wideDrsState)
        assertEquals(match.innings1.totalRuns + 1, afterWideDrs.innings1.totalRuns)
        assertEquals(2, afterWideDrs.innings1.battingTeamReviewsRemaining)

        // 5. Unfair Fielder Movement Penalty -> +5 penalty runs
        val afterPenalty = MatchSimulationEngine.applyUnfairFielderMovementPenalty(match)
        assertEquals(match.innings1.totalRuns + 5, afterPenalty.innings1.totalRuns)

        // 6. Unlimited Super Over nomination excludes dismissed batters & previous bowler
        val soState = MatchSimulationEngine.startSuperOverRound(
            state = match.copy(
                usedSuperOverDismissedIds = mapOf(match.teamA.teamId to setOf(match.innings1.battingLineup.first().id)),
                usedSuperOverBowlerIds = mapOf(match.teamA.teamId to setOf(match.innings1.bowlingOptions.first().id))
            ),
            roundNumber = 2,
            interactiveTrigger = true
        )
        assertTrue(soState.isSuperOverActive)
        assertTrue(soState.awaitingSuperOverNomination)

        val blockedBatterId = match.innings1.battingLineup.first().id
        val blockedBowlerId = match.innings1.bowlingOptions.first().id
        val validBatters = match.innings1.battingLineup.filter { it.id != blockedBatterId }.take(3).map { it.id }
        val validBowler = match.innings1.bowlingOptions.first { it.id != blockedBowlerId }.id

        // Trying to pick dismissed batter must fail
        val badBatNom = MatchSimulationEngine.confirmSuperOverNomination(
            state = soState,
            teamId = match.teamA.teamId,
            batterIds = listOf(blockedBatterId) + validBatters.take(2),
            bowlerId = validBowler
        )
        assertTrue("Dismissed Super Over batter must be blocked", badBatNom.isFailure)

        // Trying to pick prior Super Over bowler must fail
        val badBowlNom = MatchSimulationEngine.confirmSuperOverNomination(
            state = soState,
            teamId = match.teamA.teamId,
            batterIds = validBatters,
            bowlerId = blockedBowlerId
        )
        assertTrue("Previous Super Over bowler must be blocked", badBowlNom.isFailure)

        // Fresh 3 batters + fresh bowler must succeed!
        val goodNom = MatchSimulationEngine.confirmSuperOverNomination(
            state = soState,
            teamId = match.teamA.teamId,
            batterIds = validBatters,
            bowlerId = validBowler
        )
        assertTrue("Valid Super Over nomination must succeed", goodNom.isSuccess)
        assertFalse(goodNom.getOrThrow().awaitingSuperOverNomination)
    }
}
