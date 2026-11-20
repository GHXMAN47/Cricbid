package com.example.data.model

enum class MatchFormat(
    val shortLabel: String,
    val title: String,
    val maxOvers: Int,
    val maxOversPerBowler: Int,
    val impactSubAllowed: Boolean,
    val rulesSummary: String,
    val worldCupTitle: String
) {
    T20I(
        shortLabel = "T20I",
        title = "T20 International",
        maxOvers = 20,
        maxOversPerBowler = 4,
        impactSubAllowed = true,
        rulesSummary = "20 Overs • Max 4 Ov/Bowler • Impact Sub",
        worldCupTitle = "ICC T20I World Cup"
    ),
    ODI(
        shortLabel = "ODI",
        title = "One-Day International",
        maxOvers = 50,
        maxOversPerBowler = 10,
        impactSubAllowed = false,
        rulesSummary = "50 Overs • Max 10 Ov/Bowler • Powerplay 1–10",
        worldCupTitle = "ICC ODI World Cup"
    ),
    TEST(
        shortLabel = "TEST",
        title = "Test Match (Red Ball)",
        maxOvers = 90,
        maxOversPerBowler = Int.MAX_VALUE,
        impactSubAllowed = false,
        rulesSummary = "90 Ov/Inn • Uncapped Overs • Stamina & Fatigue",
        worldCupTitle = "World Test Championship"
    );

    val maxBalls: Int
        get() = maxOvers * 6

    val maxBallsPerBowler: Int
        get() = if (maxOversPerBowler == Int.MAX_VALUE) Int.MAX_VALUE else maxOversPerBowler * 6

    companion object {
        fun fromNameSafe(raw: String?): MatchFormat =
            entries.firstOrNull { it.name == raw } ?: T20I
    }
}

enum class InternationalMode(val label: String) {
    SERIES("Bilateral Series"),
    WORLD_CUP("World Cup");

    companion object {
        fun fromNameSafe(raw: String?): InternationalMode =
            entries.firstOrNull { it.name == raw } ?: SERIES
    }
}

data class InternationalNation(
    val id: String,
    val name: String,
    val code: String,
    val flag: String,
    val squadIds: List<String>
) {
    fun toTeamState(isUserTeam: Boolean): TeamState {
        val xi = SquadRuleValidator.orderBattingLineup(
            squadIds.take(11).mapNotNull { CricketerDatabase.getById(it) }
        ).map { it.id }
        val players = squadIds.mapNotNull { CricketerDatabase.getById(it) }
        return TeamState(
            teamId = id,
            ownerUid = if (isUserTeam) "intl_user" else "intl_ai_$id",
            teamName = "$flag $name",
            bot = !isUserTeam,
            budgetLakhs = 0L,
            rtmRemaining = 0,
            squadPlayerIds = squadIds,
            playingXiIds = xi,
            captainPlayerId = SquadRuleValidator.autoPickCaptain(xi),
            xiReady = true,
            overseasCount = players.count { it.overseas },
            wkCount = players.count { it.role == PlayerRole.WICKETKEEPER }
        )
    }
}

object InternationalNations {
    val seriesLengthOptions = listOf(1, 3, 5, 7)

    val all: List<InternationalNation> = listOf(
        InternationalNation(
            id = "nat_ind",
            name = "India",
            code = "IND",
            flag = "🇮🇳",
            squadIds = listOf(
                "p002", "p005", "p001", "p004", "p003", "p036",
                "p049", "p050", "p051", "p075", "p076", "p101"
            )
        ),
        InternationalNation(
            id = "nat_aus",
            name = "Australia",
            code = "AUS",
            flag = "🇦🇺",
            squadIds = listOf(
                "p025", "p026", "i013", "i014", "p067", "i015",
                "p074", "p093", "p092", "p097", "i016", "p116"
            )
        ),
        InternationalNation(
            id = "nat_eng",
            name = "England",
            code = "ENG",
            flag = "🏴",
            squadIds = listOf(
                "p048", "i018", "i017", "p031", "p044", "p068",
                "p073", "i020", "p098", "i019", "p119", "i021"
            )
        ),
        InternationalNation(
            id = "nat_rsa",
            name = "South Africa",
            code = "RSA",
            flag = "🇿🇦",
            squadIds = listOf(
                "p047", "i022", "p033", "p045", "p028", "p023",
                "i023", "p095", "i024", "i025", "i026", "p027"
            )
        ),
        InternationalNation(
            id = "nat_nz",
            name = "New Zealand",
            code = "NZ",
            flag = "🇳🇿",
            squadIds = listOf(
                "i027", "i028", "p029", "i029", "i030", "i031",
                "p120", "i032", "p094", "i033", "p100", "i034"
            )
        ),
        InternationalNation(
            id = "nat_pak",
            name = "Pakistan",
            code = "PAK",
            flag = "🇵🇰",
            squadIds = listOf(
                "i003", "i002", "i001", "i005", "i004", "i007",
                "i006", "i009", "i010", "i011", "i012", "i008"
            )
        ),
        InternationalNation(
            id = "nat_wi",
            name = "West Indies",
            code = "WI",
            flag = "🌴",
            squadIds = listOf(
                "p024", "i035", "p046", "p030", "i036", "p065",
                "i037", "p066", "i038", "i039", "i040", "p099"
            )
        ),
        InternationalNation(
            id = "nat_sl",
            name = "Sri Lanka",
            code = "SL",
            flag = "🇱🇰",
            squadIds = listOf(
                "i041", "i042", "i043", "i044", "i045", "i046",
                "p115", "i049", "p118", "p096", "i047", "i048"
            )
        )
    )

    fun byId(id: String): InternationalNation = all.firstOrNull { it.id == id } ?: all.first()

    fun buildTeamStates(userNationId: String): List<TeamState> =
        all.map { it.toTeamState(isUserTeam = it.id == userNationId) }
}

data class InternationalCampaignState(
    val format: MatchFormat = MatchFormat.T20I,
    val mode: InternationalMode = InternationalMode.SERIES,
    val userNationId: String = "nat_ind",
    val opponentNationId: String = "nat_aus",
    val seriesTotalMatches: Int = 3,
    val seriesMatches: List<MatchRecord> = emptyList(),
    val worldCupMatches: List<MatchRecord> = emptyList()
)
