package com.example

import com.example.data.model.CricketerDatabase
import com.example.data.model.InternationalNations
import com.example.data.model.MatchFormat
import com.example.data.model.MatchStage
import com.example.data.model.TeamState
import com.example.engine.MatchSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MatchRulesUnitTest {

    private fun sampleTeams(): Pair<TeamState, TeamState> {
        val india = InternationalNations.all[0].toTeamState(isUserTeam = true)
        val australia = InternationalNations.all[1].toTeamState(isUserTeam = false)
        return india to australia
    }

    @Test
    fun t20iEnforcesStrictFourOverCapAndNoConsecutiveOvers() {
        val (teamA, teamB) = sampleTeams()

        repeat(20) { seedIdx ->
            val rng = Random(1000L + seedIdx)
            var state = MatchSimulationEngine.createMatch(
                teamA = teamA,
                teamB = teamB,
                stage = MatchStage.SERIES,
                format = MatchFormat.T20I
            )

            var prevInnings1Over = -1
            var prevInnings1BowlerId = ""
            var prevInnings2Over = -1
            var prevInnings2BowlerId = ""

            var safety = 0
            while (!state.finished && safety < 400) {
                safety++
                if (!state.isSuperOverActive) {
                    val active = state.activeInnings
                    if (!active.completed) {
                        val currentOverIdx = active.legalBalls / 6
                        val currentBowlerId = active.currentBowler?.id.orEmpty()
                        if (active.inningsNumber == 1) {
                            if (currentOverIdx != prevInnings1Over) {
                                if (prevInnings1BowlerId.isNotBlank()) {
                                    assertNotEquals(
                                        "Bowler $currentBowlerId bowled consecutive overs in Innings 1!",
                                        prevInnings1BowlerId,
                                        currentBowlerId
                                    )
                                }
                                prevInnings1Over = currentOverIdx
                                prevInnings1BowlerId = currentBowlerId
                            }
                        } else {
                            if (currentOverIdx != prevInnings2Over) {
                                if (prevInnings2BowlerId.isNotBlank()) {
                                    assertNotEquals(
                                        "Bowler $currentBowlerId bowled consecutive overs in Innings 2!",
                                        prevInnings2BowlerId,
                                        currentBowlerId
                                    )
                                }
                                prevInnings2Over = currentOverIdx
                                prevInnings2BowlerId = currentBowlerId
                            }
                        }
                    }
                }
                state = MatchSimulationEngine.simulateNextBall(state, rng)
            }

            assertTrue("Match should finish within safety limit", state.finished)
            state.innings1.bowlingCard.forEach { bowler ->
                assertTrue(
                    "Innings 1 bowler ${bowler.playerName} exceeded 4.0 overs (${bowler.legalBalls} balls)",
                    bowler.legalBalls <= 24
                )
            }
            state.innings2?.bowlingCard?.forEach { bowler ->
                assertTrue(
                    "Innings 2 bowler ${bowler.playerName} exceeded 4.0 overs (${bowler.legalBalls} balls)",
                    bowler.legalBalls <= 24
                )
            }
        }
    }

    @Test
    fun manualBowlerSelectionRejectsConsecutiveOverAndMaxedOutBowler() {
        val (teamA, teamB) = sampleTeams()
        val rng = Random(42L)
        var state = MatchSimulationEngine.createMatch(
            teamA = teamA,
            teamB = teamB,
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I
        )

        // Bowl 1 complete over (6 legal balls)
        val openingBowlerId = state.innings1.currentBowler!!.id
        while (state.innings1.legalBalls < 6 && !state.innings1.completed) {
            state = MatchSimulationEngine.simulateNextBall(state, rng)
        }
        assertEquals(6, state.innings1.legalBalls)
        assertEquals(openingBowlerId, state.innings1.lastOverBowlerId)

        // Selecting the exact bowler who bowled the previous over MUST fail
        val consecutiveResult = MatchSimulationEngine.selectBowlerForNextOver(state, openingBowlerId)
        assertTrue("Consecutive over bowler must be blocked", consecutiveResult.isFailure)

        // Simulate a bowler who has already bowled 4.0 overs (24 legal balls)
        val targetBowler = state.innings1.bowlingCard.first { it.playerId != openingBowlerId }
        val maxedCard = state.innings1.bowlingCard.map { entry ->
            if (entry.playerId == targetBowler.playerId) entry.copy(legalBalls = 24) else entry
        }
        val stateWithMaxedBowler = state.copy(innings1 = state.innings1.copy(bowlingCard = maxedCard))
        val maxedResult = MatchSimulationEngine.selectBowlerForNextOver(stateWithMaxedBowler, targetBowler.playerId)
        assertTrue("Bowler at 4.0 overs must be blocked", maxedResult.isFailure)

        // Selecting an eligible bowler (< 4 overs and != lastOverBowlerId) MUST succeed
        val validBowler = stateWithMaxedBowler.innings1.bowlingCard.first {
            it.playerId != openingBowlerId && it.playerId != targetBowler.playerId
        }
        val okResult = MatchSimulationEngine.selectBowlerForNextOver(stateWithMaxedBowler, validBowler.playerId)
        assertTrue("Eligible bowler should be accepted", okResult.isSuccess)
        assertEquals(validBowler.playerId, okResult.getOrThrow().innings1.currentBowler?.id)
    }

    @Test
    fun tiedT20MatchTriggersSuperOverAndProducesDecisiveWinner() {
        val (teamA, teamB) = sampleTeams()
        val base = MatchSimulationEngine.createMatch(
            teamA = teamA,
            teamB = teamB,
            stage = MatchStage.FINAL,
            format = MatchFormat.T20I
        )

        // Complete innings 1 first so innings 2 is initialized
        val state = MatchSimulationEngine.simulateCurrentInnings(base, Random(99L))
        assertFalse(state.finished)
        val targetRuns = state.innings1.totalRuns
        val inn2 = requireNotNull(state.innings2)

        // Put Innings 2 at scores level with 1 legal ball left (119 balls) and 9 wickets down
        val tiedReadyInn2 = inn2.copy(
            totalRuns = targetRuns,
            wickets = 9,
            legalBalls = 119
        )

        var superOverTriggered = false
        for (attempt in 0 until 60) {
            val rng = Random(500L + attempt)
            val candidate = MatchSimulationEngine.simulateNextBall(state.copy(innings2 = tiedReadyInn2), rng)
            if (candidate.isSuperOverActive) {
                superOverTriggered = true
                assertEquals(1, candidate.superOverRound)

                val finished = MatchSimulationEngine.simulateFullMatch(candidate, rng)
                assertTrue("Super Over match must finish", finished.finished)
                assertTrue("Super Over history must be recorded", finished.superOverHistory.isNotEmpty())
                assertNotEquals("Super Over must produce a decisive winner, never TIE", "TIE", finished.winnerTeamId)
                assertTrue(
                    "Result summary should mention Super Over",
                    finished.resultSummary.contains("Super Over", ignoreCase = true)
                )
                finished.superOverHistory.forEach { round ->
                    assertTrue("Super Over Team 1 max 2 wickets", round.team1Wickets <= 2)
                    assertTrue("Super Over Team 2 max 2 wickets", round.team2Wickets <= 2)
                }
                break
            }
        }
        assertTrue("Tied T20I match should trigger a Super Over", superOverTriggered)
    }

    @Test
    fun roomCodeNormalizerTrimsWhitespaceAndNormalizesCase() {
        assertEquals("4829", com.example.data.model.RoomCodeNormalizer.normalize("  4829  "))
        assertEquals("4829", com.example.data.model.RoomCodeNormalizer.normalize(" 48 29 "))
        assertEquals("4829", com.example.data.model.RoomCodeNormalizer.normalize("#4829"))
        assertEquals("ABCD", com.example.data.model.RoomCodeNormalizer.normalize(" abcd "))
        assertTrue(com.example.data.model.RoomCodeNormalizer.isValid("4829"))
        assertFalse(com.example.data.model.RoomCodeNormalizer.isValid("ABCD"))
        assertFalse(com.example.data.model.RoomCodeNormalizer.isValid("123"))
    }

    @Test
    fun iplOverseasLimitAllowsEightInSquadAndEnforcesFourInPlayingXi() {
        val overseasPlayers = com.example.data.model.CricketerDatabase.allPlayers.filter { it.overseas }
        val domesticPlayers = com.example.data.model.CricketerDatabase.allPlayers.filter { !it.overseas }
        val domesticWk = domesticPlayers.first { it.role == com.example.data.model.PlayerRole.WICKETKEEPER }
        val otherDomestics = domesticPlayers.filter { it.id != domesticWk.id }.take(6)

        // Team starts with 7 overseas + 1 domestic WK, plenty of budget & slots open (8 of 15)
        var squadIds = overseasPlayers.take(7).map { it.id } + domesticWk.id
        var team = TeamState(
            teamId = "team_ipl_test",
            ownerUid = "user_1",
            teamName = "Mumbai Strikers",
            budgetLakhs = 6000L,
            squadPlayerIds = squadIds,
            overseasCount = 7,
            wkCount = 1
        )

        // Buying the 8th overseas player MUST be allowed in auction squad (leaves 6 slots + 1 domestic = 7 domestic possible)
        val eighthOverseas = overseasPlayers[7]
        val eighthCheck = com.example.data.model.SquadRuleValidator.checkBidEligibility(
            team = team,
            player = eighthOverseas,
            proposedBidLakhs = 200L,
            currentHighestBidderTeamId = ""
        )
        assertTrue("8th overseas player should be allowed in IPL auction squad: ${eighthCheck.reason}", eighthCheck.allowed)

        // Now add the 8th overseas + 6 domestic players (15-player full squad: 8 OS + 7 IND)
        squadIds = overseasPlayers.take(8).map { it.id } + domesticWk.id + otherDomestics.map { it.id }
        team = team.copy(
            squadPlayerIds = squadIds,
            overseasCount = 8,
            wkCount = 1
        )

        // Buying a 9th overseas player (even if squad size < 15) MUST be blocked
        val squadWithRoom = team.copy(squadPlayerIds = overseasPlayers.take(8).map { it.id } + domesticWk.id, overseasCount = 8)
        val ninthCheck = com.example.data.model.SquadRuleValidator.checkBidEligibility(
            team = squadWithRoom,
            player = overseasPlayers[8],
            proposedBidLakhs = 200L,
            currentHighestBidderTeamId = ""
        )
        assertFalse("9th overseas player must be blocked in auction squad", ninthCheck.allowed)

        // Playing XI with 5 overseas players MUST be strictly blocked with a clear message
        val fiveOverseasXi = overseasPlayers.take(5).map { it.id } + domesticWk.id + otherDomestics.take(5).map { it.id }
        val invalidXiCheck = com.example.data.model.SquadRuleValidator.validatePlayingXi(
            selectedIds = fiveOverseasXi,
            ownedIds = squadIds,
            captainPlayerId = domesticWk.id
        )
        assertFalse("Playing XI with 5 overseas players must be blocked", invalidXiCheck.allowed)
        assertTrue(
            "Error message should clearly state max 4 overseas rule",
            invalidXiCheck.reason?.contains("4 overseas", ignoreCase = true) == true
        )

        // Playing XI with 4 overseas + 7 domestic players MUST be accepted
        val validFourOverseasXi = overseasPlayers.take(4).map { it.id } + domesticWk.id + otherDomestics.map { it.id }
        val validXiCheck = com.example.data.model.SquadRuleValidator.validatePlayingXi(
            selectedIds = validFourOverseasXi,
            ownedIds = squadIds,
            captainPlayerId = domesticWk.id
        )
        assertTrue("Playing XI with 4 overseas players should be allowed: ${validXiCheck.reason}", validXiCheck.allowed)
    }

    @Test
    fun battingOrderReorderingIsPreservedExactlyInMatchSimulation() {
        val (baseA, baseB) = sampleTeams()
        // Reverse the entire #1..#11 batting order so tailenders open and openers bat #10/#11
        val customOrderIds = baseA.playingXiIds.reversed()
        assertEquals(11, customOrderIds.size)

        val customCaptainId = customOrderIds[4] // Pick #5 batter as Captain
        val reorderedTeamA = baseA.copy(
            playingXiIds = customOrderIds,
            captainPlayerId = customCaptainId
        )

        val match = MatchSimulationEngine.createMatch(
            teamA = reorderedTeamA,
            teamB = baseB,
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I
        )

        // Verify exact 1..11 batting order is preserved verbatim in both battingLineup and battingCard
        assertEquals(customOrderIds, match.innings1.battingLineup.map { it.id })
        assertEquals(customOrderIds, match.innings1.battingCard.map { it.playerId })
        assertEquals(customOrderIds[0], match.innings1.striker?.id)
        assertEquals(customOrderIds[1], match.innings1.nonStriker?.id)
    }

    @Test
    fun selectedCaptainAndAiAutoCaptainAppearInMatchAndScorecard() {
        val (baseA, baseB) = sampleTeams()
        val chosenCaptainId = baseA.playingXiIds[2]
        val chosenCaptainName = com.example.data.model.CricketerDatabase.getById(chosenCaptainId)!!.name

        val userTeam = baseA.copy(captainPlayerId = chosenCaptainId)
        val aiTeamWithoutExplicitCap = baseB.copy(bot = true, captainPlayerId = "")

        val match = MatchSimulationEngine.createMatch(
            teamA = userTeam,
            teamB = aiTeamWithoutExplicitCap,
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I
        )

        assertEquals(chosenCaptainId, match.teamACaptainId)
        assertTrue("AI team must automatically receive a captain", match.teamBCaptainId.isNotBlank())
        assertTrue(
            "AI captain must belong to AI Playing XI",
            match.teamBCaptainId in match.innings2.battingLineup.map { it.id }
        )
        assertTrue(
            "Toss summary should display user's selected captain with (C)",
            match.tossSummary.contains("$chosenCaptainName (C)")
        )

        // Captain badge flag must be set exclusively on the chosen captain in Team A's batting card
        val flaggedBattersA = match.innings1.battingCard.filter { it.isCaptain }
        assertEquals(1, flaggedBattersA.size)
        assertEquals(chosenCaptainId, flaggedBattersA.first().playerId)

        // And AI team's captain must be flagged exclusively in Team B's batting card
        val flaggedBattersB = match.innings2.battingCard.filter { it.isCaptain }
        assertEquals(1, flaggedBattersB.size)
        assertEquals(match.teamBCaptainId, flaggedBattersB.first().playerId)

        // Also verify AI auction auto-completion produces valid squads (<=8 OS, >=7 domestic) & auto-captains
        val emptyBotTeams = (1..8).map { i ->
            TeamState(
                teamId = "bot_$i",
                ownerUid = "bot_$i",
                teamName = "AI Team $i",
                bot = true
            )
        }
        val (completedBots, _) = com.example.engine.AuctionBotEngine.autoCompleteRemainingAuction(
            teams = emptyBotTeams,
            remainingPlayers = com.example.data.model.CricketerDatabase.allPlayers
        )
        completedBots.forEach { bot ->
            assertTrue("${bot.teamName} must have >=11 players", bot.squadPlayerIds.size >= 11)
            assertTrue("${bot.teamName} must have <=8 overseas in squad", bot.overseasCount <= 8)
            assertEquals("${bot.teamName} must have 11 in Playing XI", 11, bot.playingXiIds.size)
            val xiOs = bot.playingXiPlayers.count { it.overseas }
            assertTrue("${bot.teamName} Playing XI must have <=4 overseas (had $xiOs)", xiOs <= 4)
            assertTrue("${bot.teamName} must have an auto-assigned captain in XI", bot.captainPlayerId in bot.playingXiIds)
        }
    }

    @Test
    fun standingsAggregationComputesWinsLossesNrrPointsAndTieBreakers() {
        val t1 = TeamState(teamId = "t1", ownerUid = "u1", teamName = "Mumbai Titans")
        val t2 = TeamState(teamId = "t2", ownerUid = "u2", teamName = "Chennai Kings")
        val t3 = TeamState(teamId = "t3", ownerUid = "u3", teamName = "Bengaluru Royals")
        val t4 = TeamState(teamId = "t4", ownerUid = "u4", teamName = "Kolkata Knights")

        val matches = listOf(
            // Match 1: t1 beats t2 by huge margin (200/4 in 20 ov vs 120/10 in 20 ov -> t1 NRR +4.000, t2 NRR -4.000)
            com.example.data.model.MatchRecord(
                id = "m1",
                stage = MatchStage.LEAGUE.name,
                teamAId = "t1",
                teamAName = "Mumbai Titans",
                teamBId = "t2",
                teamBName = "Chennai Kings",
                teamAScore = 200,
                teamAWickets = 4,
                teamABalls = 120,
                teamBScore = 120,
                teamBWickets = 10,
                teamBBalls = 120,
                winnerTeamId = "t1"
            ),
            // Match 2: t3 beats t4 by smaller margin (160/6 in 20 ov vs 150/8 in 20 ov -> t3 NRR +0.500, t4 NRR -0.500)
            com.example.data.model.MatchRecord(
                id = "m2",
                stage = MatchStage.LEAGUE.name,
                teamAId = "t3",
                teamAName = "Bengaluru Royals",
                teamBId = "t4",
                teamBName = "Kolkata Knights",
                teamAScore = 160,
                teamAWickets = 6,
                teamABalls = 120,
                teamBScore = 150,
                teamBWickets = 8,
                teamBBalls = 120,
                winnerTeamId = "t3"
            ),
            // Match 3: t2 and t4 tie (150/7 in 20 ov each -> +1 pt each)
            com.example.data.model.MatchRecord(
                id = "m3",
                stage = MatchStage.LEAGUE.name,
                teamAId = "t2",
                teamAName = "Chennai Kings",
                teamBId = "t4",
                teamBName = "Kolkata Knights",
                teamAScore = 150,
                teamAWickets = 7,
                teamABalls = 120,
                teamBScore = 150,
                teamBWickets = 7,
                teamBBalls = 120,
                winnerTeamId = "TIE"
            )
        )

        val standings = com.example.engine.TournamentEngine.computeStandings(listOf(t1, t2, t3, t4), matches)
        assertEquals(4, standings.size)

        // Expected order: t1 (2 pts, +4.000), t3 (2 pts, +0.500), t4 (1 pt, -0.250), t2 (1 pt, -2.000)
        assertEquals(listOf("t1", "t3", "t4", "t2"), standings.map { it.teamId })

        val row1 = standings[0]
        assertEquals(1, row1.played)
        assertEquals(1, row1.won)
        assertEquals(0, row1.lost)
        assertEquals(0, row1.tied)
        assertEquals(2, row1.points)
        assertEquals(4.0, row1.nrr, 0.001)
        assertEquals("+4.000", row1.nrrFormatted)
        assertEquals(listOf("W"), row1.recentForm)

        val row4 = standings[3] // t2: 1L, 1T -> 1 pt, runsFor=270/40ov (6.75), runsAgainst=350/40ov (8.75) => NRR -2.000
        assertEquals(2, row4.played)
        assertEquals(0, row4.won)
        assertEquals(1, row4.lost)
        assertEquals(1, row4.tied)
        assertEquals(1, row4.points)
        assertEquals(-2.0, row4.nrr, 0.001)
        assertEquals(listOf("L", "T"), row4.recentForm)

        // Test WTC scoring (12 pts win, 4 pts draw)
        val testMatches = listOf(
            com.example.data.model.MatchRecord(
                id = "tm1",
                stage = MatchStage.LEAGUE.name,
                teamAId = "t1",
                teamBId = "t2",
                teamAScore = 400,
                teamABalls = 600,
                teamBScore = 300,
                teamBBalls = 600,
                winnerTeamId = "t1"
            ),
            com.example.data.model.MatchRecord(
                id = "tm2",
                stage = MatchStage.LEAGUE.name,
                teamAId = "t3",
                teamBId = "t4",
                teamAScore = 350,
                teamABalls = 600,
                teamBScore = 350,
                teamBBalls = 600,
                winnerTeamId = "DRAW"
            )
        )
        val wtcStandings = com.example.engine.InternationalTournamentEngine.computeInternationalStandings(
            format = MatchFormat.TEST,
            nations = listOf(t1, t2, t3, t4),
            matches = testMatches
        )
        assertEquals("t1", wtcStandings[0].teamId)
        assertEquals(12, wtcStandings[0].points)
        assertEquals(4, wtcStandings[1].points)
        assertEquals(listOf("D"), wtcStandings[1].recentForm)
    }

    @Test
    fun careerModeFullJourneyAndHallOfFameAuctionPoolIntegration() {
        // 1. Custom Rookie Creation starts around 55-62 OVR for every role
        com.example.data.model.PlayerRole.entries.forEach { role ->
            val rookie = com.example.engine.CareerEngine.createRookie(
                name = "Rookie ${role.shortLabel}",
                startingAge = 18,
                nationality = "India",
                countryCode = "IND",
                avatarId = "virat",
                role = role,
                battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
                bowlingStyle = com.example.data.model.BowlingStyle.RIGHT_ARM_FAST
            )
            assertTrue(
                "Rookie ${role.displayName} OVR (${rookie.overallRating}) should start in 55..62",
                rookie.overallRating in 55..62
            )
            assertEquals(com.example.data.model.CareerStage.DOMESTIC, rookie.stage)
        }

        // 2. Training Drill upgrades attributes, adds XP & fatigue, and Recovery reduces fatigue
        var career = com.example.engine.CareerEngine.createRookie(
            name = "Vikram Dev",
            startingAge = 18,
            nationality = "India",
            countryCode = "IND",
            avatarId = "rohit",
            role = com.example.data.model.PlayerRole.BATTER,
            battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
            bowlingStyle = com.example.data.model.BowlingStyle.OCCASIONAL_MEDIUM
        )
        val initialTech = career.attributes.battingTechnique
        val initialFatigue = career.fatigue
        val drillOutcome = com.example.engine.CareerEngine.applyTrainingDrill(
            career,
            com.example.data.model.TrainingDrill.BATTING_TECHNIQUE,
            Random(7L)
        )
        career = drillOutcome.updatedCareer
        assertTrue("Batting technique should improve after drill", career.attributes.battingTechnique > initialTech)
        assertTrue("Fatigue should increase after intense drill", career.fatigue > initialFatigue)
        assertTrue("XP should increase after drill", career.xp > 0)

        val recoveryOutcome = com.example.engine.CareerEngine.applyTrainingDrill(
            career,
            com.example.data.model.TrainingDrill.FITNESS_RECOVERY,
            Random(7L)
        )
        career = recoveryOutcome.updatedCareer
        assertTrue("Recovery should reduce fatigue", career.fatigue < drillOutcome.updatedCareer.fatigue)

        // 3. Real Match Simulation with custom Career player embedded in XI
        val (myTeam, oppTeam) = com.example.engine.CareerEngine.buildCareerMatchTeams(
            career,
            com.example.data.model.CareerMatchType.DOMESTIC_T20,
            Random(11L)
        )
        assertTrue("Custom player must be embedded in Playing XI", career.id in myTeam.playingXiIds)
        val simMatch = MatchSimulationEngine.simulateFullMatch(
            MatchSimulationEngine.createMatch(myTeam, oppTeam, MatchStage.LEAGUE, MatchFormat.T20I),
            Random(11L)
        )
        val matchRes = com.example.engine.CareerEngine.resolveCompletedMatch(
            career,
            simMatch,
            com.example.data.model.CareerMatchType.DOMESTIC_T20,
            Random(11L)
        )
        career = matchRes.updatedCareer
        assertEquals(1, career.matchesPlayed)

        // 4. Stage 2 (IPL Auction Entry) & Stage 3 (International Call-Up + Captaincy)
        val draftOutcome = com.example.engine.CareerEngine.runIplAuctionDraftEvent(career, Random(19L))
        career = draftOutcome.updatedCareer
        assertEquals(com.example.data.model.CareerStage.IPL_FRANCHISE, career.stage)
        assertTrue("IPL franchise name must be assigned", career.iplTeamName.isNotBlank())
        assertTrue("IPL price must be positive", career.iplBoughtPriceLakhs >= 30L)

        career = com.example.engine.CareerEngine.acceptInternationalCallUp(career)
        assertEquals(com.example.data.model.CareerStage.INTERNATIONAL, career.stage)
        assertEquals("India", career.nationalTeamName)

        // 5. Retirement -> Hall of Fame Legend -> Appears in future CricketerDatabase Auction Pool!
        val (retired, legend) = com.example.engine.CareerEngine.retireToHallOfFame(career.copy(age = 37, season = 19, totalRuns = 9400, centuries = 24))
        assertEquals(com.example.data.model.CareerStage.RETIRED, retired.stage)
        assertEquals("Vikram Dev", legend.name)
        assertEquals(37, legend.retiredAge)
        assertEquals(24, legend.centuries)

        com.example.data.model.CricketerDatabase.registerHallOfFameLegends(listOf(legend.toAuctionCricketer()))
        assertTrue(
            "Retired Hall of Fame Legend must appear in CricketerDatabase.allPlayers",
            com.example.data.model.CricketerDatabase.allPlayers.any { it.id == legend.id }
        )
        assertTrue(
            "Retired Hall of Fame Legend must appear in generated future auction order",
            legend.id in com.example.data.model.CricketerDatabase.generateAuctionOrder()
        )
        // Clean up static test legend so other tests stay isolated
        com.example.data.model.CricketerDatabase.registerHallOfFameLegends(emptyList())
    }

    @Test
    fun careerModeDifficultyScalingCoversAiXpTrainingAndInjuryRisk() {
        val easyRookie = com.example.engine.CareerEngine.createRookie(
            name = "Easy Star",
            startingAge = 18,
            nationality = "India",
            countryCode = "IND",
            avatarId = "virat",
            role = com.example.data.model.PlayerRole.BATTER,
            battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
            bowlingStyle = com.example.data.model.BowlingStyle.OCCASIONAL_MEDIUM,
            difficulty = com.example.data.model.CareerDifficulty.EASY
        )
        val normalRookie = com.example.engine.CareerEngine.createRookie(
            name = "Normal Star",
            startingAge = 18,
            nationality = "India",
            countryCode = "IND",
            avatarId = "virat",
            role = com.example.data.model.PlayerRole.BATTER,
            battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
            bowlingStyle = com.example.data.model.BowlingStyle.OCCASIONAL_MEDIUM,
            difficulty = com.example.data.model.CareerDifficulty.NORMAL
        )
        val hardRookie = com.example.engine.CareerEngine.createRookie(
            name = "Hard Star",
            startingAge = 18,
            nationality = "India",
            countryCode = "IND",
            avatarId = "virat",
            role = com.example.data.model.PlayerRole.BATTER,
            battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
            bowlingStyle = com.example.data.model.BowlingStyle.OCCASIONAL_MEDIUM,
            difficulty = com.example.data.model.CareerDifficulty.HARD
        )

        // 1. Training drill scaling: Easy > Normal > Hard for XP & attribute gain; Easy < Normal < Hard for fatigue
        val easyDrill = com.example.engine.CareerEngine.applyTrainingDrill(
            easyRookie, com.example.data.model.TrainingDrill.BATTING_TECHNIQUE, Random(1L)
        ).updatedCareer
        val normalDrill = com.example.engine.CareerEngine.applyTrainingDrill(
            normalRookie, com.example.data.model.TrainingDrill.BATTING_TECHNIQUE, Random(1L)
        ).updatedCareer
        val hardDrill = com.example.engine.CareerEngine.applyTrainingDrill(
            hardRookie, com.example.data.model.TrainingDrill.BATTING_TECHNIQUE, Random(1L)
        ).updatedCareer

        assertTrue("Easy XP (${easyDrill.xp}) > Normal XP (${normalDrill.xp})", easyDrill.xp > normalDrill.xp)
        assertTrue("Normal XP (${normalDrill.xp}) > Hard XP (${hardDrill.xp})", normalDrill.xp > hardDrill.xp)
        assertTrue(
            "Easy technique (${easyDrill.attributes.battingTechnique}) > Normal (${normalDrill.attributes.battingTechnique}) > Hard (${hardDrill.attributes.battingTechnique})",
            easyDrill.attributes.battingTechnique > normalDrill.attributes.battingTechnique &&
                normalDrill.attributes.battingTechnique > hardDrill.attributes.battingTechnique
        )
        assertTrue(
            "Fatigue scales Easy (${easyDrill.fatigue}) < Normal (${normalDrill.fatigue}) < Hard (${hardDrill.fatigue})",
            easyDrill.fatigue < normalDrill.fatigue && normalDrill.fatigue < hardDrill.fatigue
        )

        // 2. MatchSimulationEngine AI opposition scaling via teamBRatingOffset
        val (myTeam, oppTeam) = com.example.engine.CareerEngine.buildCareerMatchTeams(
            normalRookie, com.example.data.model.CareerMatchType.DOMESTIC_T20, Random(9L)
        )
        val easyMatch = MatchSimulationEngine.createMatch(
            teamA = myTeam,
            teamB = oppTeam,
            format = MatchFormat.T20I,
            random = Random(9L),
            teamBRatingOffset = com.example.data.model.CareerDifficulty.EASY.opponentRatingOffset
        )
        val hardMatch = MatchSimulationEngine.createMatch(
            teamA = myTeam,
            teamB = oppTeam,
            format = MatchFormat.T20I,
            random = Random(9L),
            teamBRatingOffset = com.example.data.model.CareerDifficulty.HARD.opponentRatingOffset
        )
        val easyOppAvgBat = easyMatch.innings2.battingLineup.map { it.battingRating }.average()
        val hardOppAvgBat = hardMatch.innings2.battingLineup.map { it.battingRating }.average()
        assertTrue("Hard AI avg batting ($hardOppAvgBat) must exceed Easy AI ($easyOppAvgBat)", hardOppAvgBat > easyOppAvgBat + 8.0)

        // 3. Hard mode retirement adds veteran honor & preserves difficulty on legend
        val (_, hardLegend) = com.example.engine.CareerEngine.retireToHallOfFame(hardRookie.copy(age = 38))
        assertEquals(com.example.data.model.CareerDifficulty.HARD, hardLegend.difficulty)
        assertTrue("Hard mode legend should receive Hard Mode Veteran honor", "Hard Mode Veteran" in hardLegend.awards)
    }

    @Test
    fun interactiveTimingGaugeAndShotDeliverySimulationRespectMatchRules() {
        // 1. Verify TimingGrade gauge cursor mapping
        assertEquals(com.example.engine.TimingGrade.EARLY, com.example.engine.TimingGrade.fromGaugeCursor(0.10f))
        assertEquals(com.example.engine.TimingGrade.GOOD, com.example.engine.TimingGrade.fromGaugeCursor(0.34f))
        assertEquals(com.example.engine.TimingGrade.PERFECT, com.example.engine.TimingGrade.fromGaugeCursor(0.50f))
        assertEquals(com.example.engine.TimingGrade.GOOD, com.example.engine.TimingGrade.fromGaugeCursor(0.68f))
        assertEquals(com.example.engine.TimingGrade.LATE, com.example.engine.TimingGrade.fromGaugeCursor(0.91f))

        // 2. Verify interactive batting and bowling tags commentary cleanly & rewards PERFECT over LATE
        val (teamA, teamB) = sampleTeams()
        val base = MatchSimulationEngine.createMatch(
            teamA = teamA,
            teamB = teamB,
            stage = MatchStage.SERIES,
            format = MatchFormat.T20I,
            random = Random(303L)
        )

        var perfectRunsTotal = 0
        var lateRunsTotal = 0
        repeat(60) { seed ->
            val perfectNext = MatchSimulationEngine.simulateNextBall(
                state = base,
                random = Random(1000L + seed),
                interactiveInput = com.example.engine.InteractiveBallInput(
                    role = com.example.engine.InteractivePlayRole.BATTING,
                    shotDirection = com.example.engine.ShotDirection.LOFTED,
                    timingGrade = com.example.engine.TimingGrade.PERFECT
                )
            )
            perfectRunsTotal += perfectNext.innings1.totalRuns
            if (seed == 0) {
                val comm = perfectNext.commentaryFeed.first().commentary
                assertTrue("Batting commentary should include timing grade", comm.contains("PERFECT"))
                assertTrue("Batting commentary should include shot label", comm.contains(com.example.engine.ShotDirection.LOFTED.label))
            }

            val lateNext = MatchSimulationEngine.simulateNextBall(
                state = base,
                random = Random(1000L + seed),
                interactiveInput = com.example.engine.InteractiveBallInput(
                    role = com.example.engine.InteractivePlayRole.BATTING,
                    shotDirection = com.example.engine.ShotDirection.LOFTED,
                    timingGrade = com.example.engine.TimingGrade.LATE
                )
            )
            lateRunsTotal += lateNext.innings1.totalRuns
        }
        assertTrue(
            "PERFECT timed lofted shots ($perfectRunsTotal runs) should outscore LATE mistimed shots ($lateRunsTotal runs)",
            perfectRunsTotal > lateRunsTotal
        )

        // 3. Verify interactive bowling delivery selection tags commentary & still obeys T20 4-over cap & no consecutive overs
        var state = base
        var prevOverIdx = -1
        var prevBowlerId = ""
        val deliveries = com.example.engine.DeliveryType.entries
        var ballCount = 0
        while (!state.finished && ballCount < 350) {
            val active = state.activeInnings
            if (!state.isSuperOverActive && !active.completed && active.inningsNumber == 1) {
                val overIdx = active.legalBalls / 6
                val bowlerId = active.currentBowler?.id.orEmpty()
                if (overIdx != prevOverIdx) {
                    if (prevBowlerId.isNotBlank()) {
                        assertNotEquals("Consecutive overs must remain blocked in interactive bowling", prevBowlerId, bowlerId)
                    }
                    prevOverIdx = overIdx
                    prevBowlerId = bowlerId
                }
            }
            val del = deliveries[ballCount % deliveries.size]
            state = MatchSimulationEngine.simulateNextBall(
                state = state,
                random = Random(2000L + ballCount),
                interactiveInput = com.example.engine.InteractiveBallInput(
                    role = com.example.engine.InteractivePlayRole.BOWLING,
                    deliveryType = del,
                    timingGrade = com.example.engine.TimingGrade.PERFECT
                )
            )
            if (ballCount == 0) {
                val comm = state.commentaryFeed.first().commentary
                assertTrue("Bowling commentary should include delivery label", comm.contains(del.label))
            }
            ballCount++
        }
        assertTrue("Interactive match should complete cleanly", state.finished)
        state.innings1.bowlingCard.forEach { entry ->
            assertTrue("T20 4-over cap must hold in interactive mode (${entry.legalBalls} balls)", entry.legalBalls <= 24)
        }
    }

    @Test
    fun interactivePlayFilterControlsBattingOnlyBowlingOnlyAndBoth() {
        val (india, australia) = sampleTeams()
        val match = MatchSimulationEngine.createMatch(
            teamA = india,
            teamB = australia,
            stage = MatchStage.SERIES,
            format = MatchFormat.T20I,
            random = Random(515L)
        )

        // 1. User team resolution respects explicit userTeamId whether user is teamA or teamB
        assertEquals(india.teamId, com.example.engine.InteractivePhaseResolver.resolveUserTeamId(match, india.teamId))
        assertEquals(australia.teamId, com.example.engine.InteractivePhaseResolver.resolveUserTeamId(match, australia.teamId))

        // In innings 1, teamA (india) bats first
        val userBatsFirst = com.example.engine.InteractivePhaseResolver.isUserTeamBatting(match, india.teamId)
        assertTrue("Team A (India) should be batting in Innings 1", userBatsFirst)

        // 2. When user is India (batting first):
        // - BATTING_ONLY -> user plays now (shouldAutoSim == false), role == BATTING
        assertFalse(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                match, india.teamId, com.example.engine.InteractivePlayFilter.BATTING_ONLY
            )
        )
        assertEquals(
            com.example.engine.InteractivePlayRole.BATTING,
            com.example.engine.InteractivePhaseResolver.expectedActiveRole(
                match, india.teamId, com.example.engine.InteractivePlayFilter.BATTING_ONLY
            )
        )
        // - BOWLING_ONLY -> user's own batting innings auto-sims (shouldAutoSim == true)
        assertTrue(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                match, india.teamId, com.example.engine.InteractivePlayFilter.BOWLING_ONLY
            )
        )
        // - BOTH -> user plays manually (shouldAutoSim == false)
        assertFalse(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                match, india.teamId, com.example.engine.InteractivePlayFilter.BOTH
            )
        )

        // 3. After simulating 1st innings, Australia bats (user India is now bowling!)
        val inn2State = MatchSimulationEngine.simulateCurrentInnings(match, Random(515L))
        assertFalse("Match should now be in Innings 2", inn2State.finished)
        assertFalse(
            "India should now be bowling in Innings 2",
            com.example.engine.InteractivePhaseResolver.isUserTeamBatting(inn2State, india.teamId)
        )
        // - BATTING_ONLY -> opponent batting now, so shouldAutoSim MUST flip to true!
        assertTrue(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                inn2State, india.teamId, com.example.engine.InteractivePlayFilter.BATTING_ONLY
            )
        )
        // - BOWLING_ONLY -> user is bowling now, so shouldAutoSim MUST flip to false and role == BOWLING!
        assertFalse(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                inn2State, india.teamId, com.example.engine.InteractivePlayFilter.BOWLING_ONLY
            )
        )
        assertEquals(
            com.example.engine.InteractivePlayRole.BOWLING,
            com.example.engine.InteractivePhaseResolver.expectedActiveRole(
                inn2State, india.teamId, com.example.engine.InteractivePlayFilter.BOWLING_ONLY
            )
        )
        // - BOTH -> user plays manually with BOWLING role
        assertFalse(
            com.example.engine.InteractivePhaseResolver.shouldAutoSimCurrentPhase(
                inn2State, india.teamId, com.example.engine.InteractivePlayFilter.BOTH
            )
        )
        assertEquals(
            com.example.engine.InteractivePlayRole.BOWLING,
            com.example.engine.InteractivePhaseResolver.expectedActiveRole(
                inn2State, india.teamId, com.example.engine.InteractivePlayFilter.BOTH
            )
        )

        // 4. Finishing skipped 2nd innings via simulateCurrentInnings preserves strict T20 4-over cap
        val finished = MatchSimulationEngine.simulateCurrentInnings(inn2State, Random(515L))
        assertTrue(finished.finished)
        finished.innings2.bowlingCard.forEach { bowler ->
            assertTrue("Bowler ${bowler.playerName} exceeded 4 overs during skipped innings auto-sim", bowler.legalBalls <= 24)
        }
    }

    @Test
    fun pitchDewConfidenceStaminaFieldPresetsAndTelemetryRecording() {
        val (india, australia) = sampleTeams()
        val base = MatchSimulationEngine.createMatch(
            teamA = india,
            teamB = australia,
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I,
            pitchType = com.example.engine.PitchType.FLAT,
            weatherCondition = com.example.engine.WeatherCondition.CLEAR_NIGHT,
            dewFactor2ndInnings = true,
            random = Random(777L)
        )
        assertEquals(com.example.engine.PitchType.FLAT, base.pitchType)
        assertFalse("Dew should not be active in 1st innings", base.isDewActiveNow)

        // Field presets have 9 outfielder coordinates and update cleanly
        val attacking = MatchSimulationEngine.setFieldPreset(base, com.example.engine.FieldPreset.ATTACKING)
        assertEquals(com.example.engine.FieldPreset.ATTACKING, attacking.fieldPreset)
        com.example.engine.FieldPreset.entries.forEach { preset ->
            assertEquals("Field preset ${preset.name} must position 9 outfielders", 9, preset.fielderCoords.size)
        }

        // Confidence widens timing sweet-spot window; low stamina narrows it
        val highConfBonus = com.example.engine.TimingGrade.confidenceWindowBonus(95)
        val lowConfBonus = com.example.engine.TimingGrade.confidenceWindowBonus(20)
        assertTrue("High batter confidence should widen timing window", highConfBonus > lowConfBonus)
        val lowStaminaPenalty = com.example.engine.TimingGrade.staminaWindowPenalty(25)
        assertTrue("Low bowler stamina should narrow bowling timing window", lowStaminaPenalty < 0f)

        // Simulate 1st innings and verify wagon wheel, pitch map, stamina drain, and 2nd-innings dew activation
        val afterInn1 = MatchSimulationEngine.simulateCurrentInnings(attacking, Random(777L))
        assertTrue("Pitch map should record ball landing points", afterInn1.innings1.pitchMapPoints.isNotEmpty())
        assertTrue("Wagon wheel should record scoring shots", afterInn1.innings1.wagonWheelShots.isNotEmpty())
        assertTrue(
            "Bowlers who bowled in T20 should lose stamina",
            afterInn1.innings1.bowlingCard.any { it.legalBalls > 0 && it.stamina < 100 }
        )
        assertTrue("2nd innings should activate evening dew factor", afterInn1.isDewActiveNow)

        val finished = MatchSimulationEngine.simulateFullMatch(afterInn1, Random(777L))
        val perfs = com.example.engine.SeasonAwardsEngine.extractPerformances(finished)
        assertTrue("SeasonAwardsEngine should extract player performances for Orange/Purple Cap & MVP", perfs.isNotEmpty())
    }

    @Test
    fun drsReviewOverturnsOutDeductsUpheldAndPreservesUmpiresCall() {
        val (india, australia) = sampleTeams()
        val match = MatchSimulationEngine.createMatch(
            teamA = india,
            teamB = australia,
            stage = MatchStage.LEAGUE,
            format = MatchFormat.T20I,
            random = Random(888L)
        )
        val striker = match.innings1.striker!!
        val bowler = match.innings1.currentBowler!!

        // 1. Overturned OUT (batter reviews -> restored to crease, review retained!)
        val innWithOut = match.innings1.copy(
            wickets = 1,
            battingCard = match.innings1.battingCard.mapIndexed { idx, b ->
                if (idx == 0) b.copy(out = true, dismissalText = "lbw b ${bowler.name}") else b
            },
            bowlingCard = match.innings1.bowlingCard.mapIndexed { idx, bw ->
                if (idx == 0) bw.copy(wickets = 1) else bw
            },
            battingTeamReviewsRemaining = 2
        )
        val pendingOverturn = com.example.engine.DrsPendingDecision(
            decisionId = 1L,
            mode = com.example.engine.DrsMode.BALL_TRACKING,
            originalDecisionOut = true,
            overturned = true,
            umpiresCall = false,
            reviewingTeamId = india.teamId,
            reviewingTeamName = india.teamName,
            isBattingTeamReview = true,
            batterId = striker.id,
            batterName = striker.name,
            bowlerName = bowler.name,
            dismissalLabel = "LBW",
            pitchingLabel = "IN LINE",
            impactLabel = "IN LINE",
            wicketsLabel = "MISSING",
            spikeIntensity = 0f,
            ballPitchX = 0.05f,
            ballPitchY = 0.35f,
            inningsNumber = 1,
            overLabel = "0.1"
        )
        val afterOverturn = MatchSimulationEngine.resolveDrsReview(
            match.copy(innings1 = innWithOut, pendingDrs = pendingOverturn)
        )
        assertEquals("Overturned OUT must restore team wickets from 1 to 0", 0, afterOverturn.innings1.wickets)
        assertFalse("Overturned batter must be marked not out", afterOverturn.innings1.battingCard[0].out)
        assertEquals("Successful review must keep 2 reviews remaining", 2, afterOverturn.innings1.battingTeamReviewsRemaining)
        assertEquals(null, afterOverturn.pendingDrs)

        // 2. Upheld OUT (not Umpire's Call -> loses 1 review)
        val pendingUpheld = pendingOverturn.copy(
            decisionId = 2L,
            wicketsLabel = "HITTING",
            overturned = false,
            umpiresCall = false
        )
        val afterUpheld = MatchSimulationEngine.resolveDrsReview(
            match.copy(innings1 = innWithOut, pendingDrs = pendingUpheld)
        )
        assertEquals("Upheld OUT keeps 1 wicket", 1, afterUpheld.innings1.wickets)
        assertEquals("Unsuccessful review must deduct 1 review", 1, afterUpheld.innings1.battingTeamReviewsRemaining)

        // 3. Umpire's Call -> decision stands AND review is retained!
        val pendingUmpiresCall = pendingOverturn.copy(
            decisionId = 3L,
            wicketsLabel = "UMPIRE'S CALL",
            overturned = false,
            umpiresCall = true
        )
        val afterUmpiresCall = MatchSimulationEngine.resolveDrsReview(
            match.copy(innings1 = innWithOut, pendingDrs = pendingUmpiresCall)
        )
        assertEquals("Umpire's Call retains original decision", 1, afterUmpiresCall.innings1.wickets)
        assertEquals("Umpire's Call must retain review", 2, afterUmpiresCall.innings1.battingTeamReviewsRemaining)
    }

    @Test
    fun multiPlayerTieredRetentionsAndFormerSquadRtmCard() {
        // Verify tiered retention prices: Slot 1 = 15 Cr, Slot 2 = 11 Cr, Slot 3 = 8 Cr, Slot 4 = 6 Cr -> Total 40 Cr
        assertEquals(1500L, com.example.data.model.SquadRules.retentionPriceForSlot(0))
        assertEquals(1100L, com.example.data.model.SquadRules.retentionPriceForSlot(1))
        assertEquals(800L, com.example.data.model.SquadRules.retentionPriceForSlot(2))
        assertEquals(600L, com.example.data.model.SquadRules.retentionPriceForSlot(3))
        assertEquals(4000L, com.example.data.model.SquadRules.totalRetentionCost(4))

        val players = CricketerDatabase.allPlayers.filter { !it.overseas }.take(8)
        val formerSquadIds = players.take(6).map { it.id }
        val retainedIds = formerSquadIds.take(4)
        val unretainedFormerPlayer = players[4] // Was in former squad, NOT retained -> RTM eligible!
        val nonFormerPlayer = players[7]        // Never in former squad -> RTM blocked!

        val team = TeamState(
            teamId = "team_rtm_test",
            ownerUid = "u1",
            teamName = "Royal Challengers",
            budgetLakhs = 10_000L - com.example.data.model.SquadRules.totalRetentionCost(4),
            squadPlayerIds = retainedIds,
            retainedPlayerIds = retainedIds,
            formerSquadPlayerIds = formerSquadIds,
            rtmRemaining = 1
        )

        val eligibleFormerRtm = com.example.data.model.SquadRuleValidator.checkRtmEligibility(
            team = team,
            player = unretainedFormerPlayer,
            currentBidLakhs = 900L,
            currentHighestBidderTeamId = "other_team"
        )
        assertTrue("Unretained former squad player must be RTM eligible: ${eligibleFormerRtm.reason}", eligibleFormerRtm.allowed)

        val blockedNonFormerRtm = com.example.data.model.SquadRuleValidator.checkRtmEligibility(
            team = team,
            player = nonFormerPlayer,
            currentBidLakhs = 900L,
            currentHighestBidderTeamId = "other_team"
        )
        assertFalse("Player not in former squad must be blocked for RTM", blockedNonFormerRtm.allowed)
    }

    @Test
    fun seasonLeaderboardsComputeOrangeCapPurpleCapAndMvp() {
        val m1 = com.example.data.model.MatchRecord(
            id = "s_m1",
            stage = MatchStage.LEAGUE.name,
            teamAId = "t1",
            teamAName = "Mumbai",
            teamBId = "t2",
            teamBName = "Chennai",
            winnerTeamId = "t1",
            playerPerformances = listOf(
                com.example.engine.MatchPlayerPerf(
                    playerId = "p_bat",
                    playerName = "Virat Star",
                    teamName = "Mumbai",
                    runs = 92,
                    ballsFaced = 50,
                    fours = 8,
                    sixes = 5
                ),
                com.example.engine.MatchPlayerPerf(
                    playerId = "p_bowl",
                    playerName = "Jasprit Ace",
                    teamName = "Mumbai",
                    wickets = 4,
                    runsConceded = 22,
                    legalBallsBowled = 24
                )
            )
        )
        val m2 = com.example.data.model.MatchRecord(
            id = "s_m2",
            stage = MatchStage.LEAGUE.name,
            teamAId = "t1",
            teamAName = "Mumbai",
            teamBId = "t3",
            teamBName = "Delhi",
            winnerTeamId = "t1",
            playerPerformances = listOf(
                com.example.engine.MatchPlayerPerf(
                    playerId = "p_bat",
                    playerName = "Virat Star",
                    teamName = "Mumbai",
                    runs = 68,
                    ballsFaced = 42,
                    fours = 6,
                    sixes = 3
                ),
                com.example.engine.MatchPlayerPerf(
                    playerId = "p_bowl",
                    playerName = "Jasprit Ace",
                    teamName = "Mumbai",
                    wickets = 3,
                    runsConceded = 18,
                    legalBallsBowled = 24
                )
            )
        )

        val summary = com.example.engine.SeasonAwardsEngine.buildLeaderboards(listOf(m1, m2))
        assertTrue(summary.hasData)
        assertEquals("p_bat", summary.orangeCapPlayerId)
        assertEquals(160, summary.orangeCapHolder?.runs)
        assertEquals("p_bowl", summary.purpleCapPlayerId)
        assertEquals(7, summary.purpleCapHolder?.wickets)
        assertNotNull("MVP holder should be computed", summary.mvpHolder)
    }
}



