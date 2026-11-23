package com.example.data.model

/**
 * Official 2026 IPL Franchise metadata, rosters, captains, and extended auction player pool.
 * Guarantees 240+ realistic capped Indian, uncapped Indian, and overseas players so all 10
 * franchises can cleanly draft 18–25 player squads (max 8 overseas, ≥1 WK) under ₹120 Cr purses.
 */
data class IplFranchiseBlueprint(
    val id: String,
    val shortCode: String,
    val name: String,
    val homeCity: String,
    val homeVenue: String,
    val captainId: String,
    val captainName: String,
    val defendingChampion: Boolean = false,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val coreRosterIds: List<String>
)

data class IplPlayerMeta(
    val isUncappedIndian: Boolean = false,
    val isEmergingEligible: Boolean = false
)

object Ipl2026Catalog {

    private fun p(
        id: String,
        name: String,
        country: String,
        code: String,
        role: PlayerRole,
        era: String,
        bat: Int,
        bowl: Int,
        fld: Int,
        base: Long,
        spec: String
    ) = Cricketer(id, name, country, code, country != "India", role, era, bat, bowl, fld, base, spec)

    /**
     * Supplemental 2026 IPL capped Indian, uncapped Indian, and overseas stars so all 10 franchises
     * have authentic 2026 squads and a deep 245+ player mega-auction pool (10 x 18..25 squads).
     */
    val extraPlayers: List<Cricketer> = listOf(
        // --- 2026 FEATURED & UNCAPPED INDIAN BATTERS ---
        p("ipl001", "Vaibhav Sooryavanshi", "India", "IND", PlayerRole.BATTER, "Emerging", 82, 18, 84, 30L, "14yo Wonderkid Opener"),
        p("ipl002", "Priyansh Arya", "India", "IND", PlayerRole.BATTER, "Emerging", 81, 20, 82, 30L, "Powerplay Left-Hander"),
        p("ipl003", "Ayush Badoni", "India", "IND", PlayerRole.BATTER, "Emerging", 81, 32, 85, 50L, "360° Middle Finisher"),
        p("ipl004", "Abdul Samad", "India", "IND", PlayerRole.BATTER, "IPL Finisher", 80, 34, 86, 50L, "Big-Six Finisher"),
        p("ipl005", "Naman Dhir", "India", "IND", PlayerRole.BATTER, "Emerging", 80, 44, 85, 30L, "Clean Ball Striker"),
        p("ipl006", "Shahrukh Khan", "India", "IND", PlayerRole.BATTER, "IPL Finisher", 81, 28, 84, 50L, "Power Finisher"),
        p("ipl007", "Abhinav Manohar", "India", "IND", PlayerRole.BATTER, "IPL Finisher", 80, 18, 84, 50L, "Long-Handle Hitter"),
        p("ipl008", "Sameer Rizvi", "India", "IND", PlayerRole.BATTER, "Emerging", 79, 24, 83, 30L, "Spin-Smashing Youth"),
        p("ipl009", "Aniket Verma", "India", "IND", PlayerRole.BATTER, "Emerging", 79, 20, 82, 30L, "Fearless Middle-Order"),
        p("ipl010", "Shubham Dubey", "India", "IND", PlayerRole.BATTER, "Emerging", 78, 20, 82, 30L, "Left-Hand Slogger"),
        p("ipl011", "Nitish Rana", "India", "IND", PlayerRole.BATTER, "IPL Star", 84, 48, 83, 100L, "Left-Hand Spin Hitter"),
        p("ipl012", "Karun Nair", "India", "IND", PlayerRole.BATTER, "IPL Veteran", 81, 22, 84, 50L, "Composed #3 Anchor"),
        p("ipl013", "Mayank Agarwal", "India", "IND", PlayerRole.BATTER, "IPL Veteran", 82, 16, 85, 75L, "Aggressive Opener"),
        p("ipl014", "Prithvi Shaw", "India", "IND", PlayerRole.BATTER, "IPL Star", 82, 14, 80, 75L, "Powerplay Dasher"),
        p("ipl015", "Sarfaraz Khan", "India", "IND", PlayerRole.BATTER, "INTL Star", 82, 16, 81, 75L, "Sweep Specialist"),
        p("ipl016", "Shaik Rasheed", "India", "IND", PlayerRole.BATTER, "Emerging", 77, 18, 85, 30L, "U19 Classical Bat"),
        p("ipl017", "Andre Siddarth", "India", "IND", PlayerRole.BATTER, "Emerging", 76, 16, 82, 30L, "Young Stroke Maker"),
        p("ipl018", "Swastik Chikara", "India", "IND", PlayerRole.BATTER, "Emerging", 78, 18, 81, 30L, "Explosive Top-Order"),
        p("ipl019", "Himmat Singh", "India", "IND", PlayerRole.BATTER, "IPL Domestic", 77, 22, 82, 30L, "Domestic Power Bat"),
        p("ipl020", "Atharva Taide", "India", "IND", PlayerRole.BATTER, "Emerging", 78, 24, 82, 30L, "Left-Hand Opener"),
        p("ipl021", "Harnoor Pannu", "India", "IND", PlayerRole.BATTER, "Emerging", 76, 15, 81, 30L, "U19 Opener"),
        p("ipl022", "Sachin Baby", "India", "IND", PlayerRole.BATTER, "IPL Domestic", 77, 20, 83, 30L, "Left-Hand Anchor"),
        p("ipl023", "Luvnith Sisodia", "India", "IND", PlayerRole.BATTER, "Emerging", 76, 12, 82, 30L, "Hard-Hitting Leftie"),
        p("ipl024", "Bevon Jacobs", "New Zealand", "NZ", PlayerRole.BATTER, "Emerging", 79, 18, 84, 30L, "Power Finisher"),
        p("ipl025", "Tristan Stubbs", "South Africa", "RSA", PlayerRole.BATTER, "T20 Ace", 88, 36, 91, 150L, "360° Death Finisher"),
        p("ipl026", "Sherfane Rutherford", "West Indies", "WI", PlayerRole.BATTER, "T20 Ace", 84, 24, 86, 100L, "Left-Hand Six Machine"),
        p("ipl027", "Dewald Brevis", "South Africa", "RSA", PlayerRole.BATTER, "Emerging", 83, 38, 87, 75L, "Baby AB Power Bat"),
        p("ipl028", "Matthew Breetzke", "South Africa", "RSA", PlayerRole.BATTER, "INTL Star", 82, 16, 85, 75L, "Top-Order Stroke Maker"),

        // --- 2026 FEATURED WICKETKEEPERS (INDIAN & OVERSEAS) ---
        p("ipl029", "Robin Minz", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 79, 10, 84, 30L, "Explosive Left-Hand WK"),
        p("ipl030", "Anuj Rawat", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Domestic", 78, 10, 84, 30L, "Left-Hand Finisher WK"),
        p("ipl031", "Kumar Kushagra", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 78, 10, 85, 30L, "Young Keeper-Bat"),
        p("ipl032", "Vishnu Vinod", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Domestic", 78, 10, 83, 30L, "Powerplay Hitter WK"),
        p("ipl033", "Aryan Juyal", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 77, 10, 84, 30L, "U19 Keeper-Bat"),
        p("ipl034", "Vansh Bedi", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 77, 10, 83, 30L, "Clean-Striking WK"),
        p("ipl035", "Kunal Singh Rathore", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 76, 10, 83, 30L, "Left-Hand Domestic WK"),
        p("ipl036", "KS Bharat", "India", "IND", PlayerRole.WICKETKEEPER, "INTL Star", 78, 10, 87, 50L, "Safe Glovework WK"),
        p("ipl037", "Ryan Rickelton", "South Africa", "RSA", PlayerRole.WICKETKEEPER, "T20 Ace", 86, 12, 87, 100L, "Fearless Opener WK"),
        p("ipl038", "Josh Inglis", "Australia", "AUS", PlayerRole.WICKETKEEPER, "INTL Star", 86, 12, 89, 150L, "360° T20 Keeper"),
        p("ipl039", "Rahmanullah Gurbaz", "Afghanistan", "AFG", PlayerRole.WICKETKEEPER, "T20 Ace", 85, 10, 86, 100L, "Powerplay Blitzer WK"),
        p("ipl040", "Donovan Ferreira", "South Africa", "RSA", PlayerRole.WICKETKEEPER, "T20 Ace", 81, 34, 86, 75L, "Utility Finisher WK"),

        // --- 2026 FEATURED ALL-ROUNDERS (INDIAN & OVERSEAS) ---
        p("ipl041", "Mahipal Lomror", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 80, 56, 84, 50L, "Spin-Hitting Leftie AR"),
        p("ipl042", "Swapnil Singh", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 72, 79, 82, 30L, "Powerplay Left-Arm Spin"),
        p("ipl043", "Anshul Kamboj", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 68, 82, 82, 30L, "Hit-the-Deck Seam AR"),
        p("ipl044", "Vipraj Nigam", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 74, 80, 83, 30L, "Attacking Leg-Spin AR"),
        p("ipl045", "Vijay Shankar", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Veteran", 78, 66, 83, 50L, "Middle-Order Seam AR"),
        p("ipl046", "Raj Angad Bawa", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 75, 76, 84, 30L, "U19 Pace All-Rounder"),
        p("ipl047", "Suryansh Shedge", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 77, 76, 84, 30L, "Explosive Pace AR"),
        p("ipl048", "Musheer Khan", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 78, 72, 85, 30L, "Clutch All-Rounder"),
        p("ipl049", "Arshin Kulkarni", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 76, 74, 83, 30L, "Power-Hitting Seam AR"),
        p("ipl050", "Nishant Sindhu", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 75, 76, 83, 30L, "Left-Arm Spin AR"),
        p("ipl051", "Anukul Roy", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 72, 77, 89, 30L, "Electric Fielder Spin AR"),
        p("ipl052", "Manoj Bhandage", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 76, 74, 83, 30L, "Finisher Seam AR"),
        p("ipl053", "Yudhvir Singh", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 66, 79, 81, 30L, "145 kph Swing AR"),
        p("ipl054", "Madhav Tiwari", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 72, 75, 82, 30L, "Domestic Pace AR"),
        p("ipl055", "Ajay Mandal", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 71, 76, 83, 30L, "Left-Arm Utility AR"),
        p("ipl056", "Tripurana Vijay", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 72, 76, 82, 30L, "Off-Spin All-Rounder"),
        p("ipl057", "Mitchell Marsh", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "INTL Star", 88, 78, 86, 200L, "Powerplay Bison AR"),
        p("ipl058", "Will Jacks", "England", "ENG", PlayerRole.ALL_ROUNDER, "T20 Ace", 87, 72, 87, 150L, "41-Ball Ton Off-Spin AR"),
        p("ipl059", "Romario Shepherd", "West Indies", "WI", PlayerRole.ALL_ROUNDER, "T20 Ace", 82, 81, 85, 100L, "Death-Over Heavy Hitter"),
        p("ipl060", "Jacob Bethell", "England", "ENG", PlayerRole.ALL_ROUNDER, "Emerging", 83, 72, 88, 100L, "Fearless Left-Hand Prodigy"),
        p("ipl061", "Azmatullah Omarzai", "Afghanistan", "AFG", PlayerRole.ALL_ROUNDER, "INTL Star", 80, 82, 84, 100L, "New-Ball Seam AR"),
        p("ipl062", "Aaron Hardie", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "INTL Star", 81, 79, 85, 100L, "Bouncy Seam AR"),
        p("ipl063", "Moeen Ali", "England", "ENG", PlayerRole.ALL_ROUNDER, "IPL Veteran", 82, 81, 84, 100L, "Spin-Hitting Offie AR"),
        p("ipl064", "Jamie Overton", "England", "ENG", PlayerRole.ALL_ROUNDER, "T20 Ace", 78, 83, 85, 100L, "148 kph Pace Finisher"),
        p("ipl065", "Corbin Bosch", "South Africa", "RSA", PlayerRole.ALL_ROUNDER, "INTL Star", 76, 83, 86, 75L, "Express Seam AR"),
        p("ipl066", "Kamindu Mendis", "Sri Lanka", "SL", PlayerRole.ALL_ROUNDER, "INTL Star", 84, 74, 87, 75L, "Ambidextrous Spin AR"),

        // --- 2026 FEATURED SEAMERS (INDIAN & OVERSEAS) ---
        p("ipl067", "Yash Dayal", "India", "IND", PlayerRole.SEAMER, "IPL Star", 24, 85, 79, 75L, "Left-Arm Swing & Death"),
        p("ipl068", "Rasikh Salam", "India", "IND", PlayerRole.SEAMER, "Emerging", 28, 83, 80, 50L, "Death Slower-Ball Ace"),
        p("ipl069", "Vaibhav Arora", "India", "IND", PlayerRole.SEAMER, "IPL Star", 24, 84, 79, 50L, "New-Ball Outswinger"),
        p("ipl070", "Tushar Deshpande", "India", "IND", PlayerRole.SEAMER, "IPL Star", 28, 84, 79, 75L, "Strike Wicket-Taker"),
        p("ipl071", "Mohsin Khan", "India", "IND", PlayerRole.SEAMER, "IPL Star", 26, 85, 79, 50L, "Tall Left-Arm Bounce"),
        p("ipl072", "Akash Deep", "India", "IND", PlayerRole.SEAMER, "INTL Star", 32, 86, 80, 100L, "Skiddy Seam Spearhead"),
        p("ipl073", "Akash Madhwal", "India", "IND", PlayerRole.SEAMER, "IPL Star", 22, 83, 78, 50L, "Pinpoint Death Yorkers"),
        p("ipl074", "Simarjeet Singh", "India", "IND", PlayerRole.SEAMER, "Emerging", 24, 82, 79, 30L, "146 kph Hard-Length Pace"),
        p("ipl075", "Vijaykumar Vyshak", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 26, 82, 81, 50L, "Wide-Yorker Specialist"),
        p("ipl076", "Yash Thakur", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 22, 82, 79, 50L, "5-For Slower-Ball Pace"),
        p("ipl077", "Gurjapneet Singh", "India", "IND", PlayerRole.SEAMER, "Emerging", 20, 81, 78, 30L, "6'3 Left-Arm Seam"),
        p("ipl078", "Umran Malik", "India", "IND", PlayerRole.SEAMER, "IPL Star", 22, 83, 82, 75L, "154 kph Raw Express"),
        p("ipl079", "Ishant Sharma", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 24, 82, 77, 75L, "Veteran Seam Bounce"),
        p("ipl080", "Jaydev Unadkat", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 32, 82, 80, 75L, "Left-Arm Off-Cutters"),
        p("ipl081", "Umesh Yadav", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 30, 82, 78, 75L, "Powerplay Raw Pace"),
        p("ipl082", "Chetan Sakariya", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 24, 81, 80, 50L, "Left-Arm Slower Balls"),
        p("ipl083", "Kuldeep Sen", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 20, 81, 78, 50L, "145 kph Skiddy Pace"),
        p("ipl084", "Arjun Tendulkar", "India", "IND", PlayerRole.SEAMER, "Emerging", 42, 78, 80, 30L, "Left-Arm New-Ball Swing"),
        p("ipl085", "Ashwani Kumar", "India", "IND", PlayerRole.SEAMER, "Emerging", 22, 80, 79, 30L, "Left-Arm Death Seam"),
        p("ipl086", "Prince Yadav", "India", "IND", PlayerRole.SEAMER, "Emerging", 22, 80, 78, 30L, "143 kph Yorker Bowler"),
        p("ipl087", "Abhinandan Singh", "India", "IND", PlayerRole.SEAMER, "Emerging", 20, 79, 78, 30L, "Young Express Seamer"),
        p("ipl088", "Mukesh Choudhary", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 22, 81, 78, 30L, "Left-Arm Powerplay Swing"),
        p("ipl089", "Kamlesh Nagarkoti", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 34, 80, 85, 30L, "Express Pace & Field"),
        p("ipl090", "Gurnoor Brar", "India", "IND", PlayerRole.SEAMER, "Emerging", 24, 79, 78, 30L, "Tall Hit-the-Deck Pace"),
        p("ipl091", "Gerald Coetzee", "South Africa", "RSA", PlayerRole.SEAMER, "T20 Ace", 42, 87, 83, 150L, "150 kph Enforcer"),
        p("ipl092", "Spencer Johnson", "Australia", "AUS", PlayerRole.SEAMER, "T20 Ace", 24, 86, 82, 150L, "Left-Arm 148 kph Pace"),
        p("ipl093", "Fazalhaq Farooqi", "Afghanistan", "AFG", PlayerRole.SEAMER, "T20 Ace", 20, 87, 80, 100L, "Left-Arm Swing & Yorkers"),
        p("ipl094", "Reece Topley", "England", "ENG", PlayerRole.SEAMER, "INTL Star", 24, 86, 79, 100L, "6'7 Left-Arm Bounce"),
        p("ipl095", "Brydon Carse", "England", "ENG", PlayerRole.SEAMER, "INTL Star", 48, 86, 84, 100L, "Hit-the-Deck Seam"),
        p("ipl096", "Nathan Ellis", "Australia", "AUS", PlayerRole.SEAMER, "T20 Ace", 30, 87, 84, 150L, "Death Slower-Ball Master"),
        p("ipl097", "Xavier Bartlett", "Australia", "AUS", PlayerRole.SEAMER, "INTL Star", 36, 85, 82, 100L, "New-Ball Outswing"),
        p("ipl098", "Kwena Maphaka", "South Africa", "RSA", PlayerRole.SEAMER, "Emerging", 20, 84, 81, 75L, "Teenage 148 kph Leftie"),
        p("ipl099", "Nuwan Thushara", "Sri Lanka", "SL", PlayerRole.SEAMER, "T20 Ace", 20, 85, 80, 100L, "Slingy Yorker Specialist"),
        p("ipl100", "Lungi Ngidi", "South Africa", "RSA", PlayerRole.SEAMER, "INTL Star", 24, 86, 80, 100L, "Dipping Slower-Ball Pace"),
        p("ipl101", "Dushmantha Chameera", "Sri Lanka", "SL", PlayerRole.SEAMER, "INTL Star", 24, 85, 80, 75L, "Skiddy Express Seam"),

        // --- 2026 FEATURED SPINNERS (INDIAN & OVERSEAS) ---
        p("ipl102", "Vignesh Puthur", "India", "IND", PlayerRole.SPINNER, "Emerging", 20, 82, 79, 30L, "Left-Arm Chinaman Prodigy"),
        p("ipl103", "Digvesh Rathi", "India", "IND", PlayerRole.SPINNER, "Emerging", 22, 82, 80, 30L, "Mystery Leg-Spinner"),
        p("ipl104", "Zeeshan Ansari", "India", "IND", PlayerRole.SPINNER, "Emerging", 22, 81, 79, 30L, "Attacking Wrist-Spinner"),
        p("ipl105", "Karn Sharma", "India", "IND", PlayerRole.SPINNER, "IPL Veteran", 40, 83, 79, 50L, "Lucky Charm Leggie"),
        p("ipl106", "Shreyas Gopal", "India", "IND", PlayerRole.SPINNER, "IPL Veteran", 46, 83, 81, 50L, "Hat-Trick Googly Ace"),
        p("ipl107", "Manimaran Siddharth", "India", "IND", PlayerRole.SPINNER, "Emerging", 24, 81, 80, 30L, "Powerplay Left-Arm Spin"),
        p("ipl108", "Praveen Dubey", "India", "IND", PlayerRole.SPINNER, "IPL Domestic", 28, 80, 80, 30L, "Skiddy Leg-Break"),
        p("ipl109", "Kumar Kartikeya", "India", "IND", PlayerRole.SPINNER, "IPL Domestic", 26, 81, 80, 30L, "Dual-Action Mystery Spin"),
        p("ipl110", "Manav Suthar", "India", "IND", PlayerRole.SPINNER, "Emerging", 36, 82, 82, 30L, "Classical Left-Arm Spin"),
        p("ipl111", "Mohit Rathee", "India", "IND", PlayerRole.SPINNER, "Emerging", 32, 79, 80, 30L, "Domestic Leg-Spinner"),
        p("ipl112", "Allah Ghazanfar", "Afghanistan", "AFG", PlayerRole.SPINNER, "Emerging", 24, 86, 80, 100L, "Teen Mystery Carrom Spin"),
        p("ipl113", "Mujeeb Ur Rahman", "Afghanistan", "AFG", PlayerRole.SPINNER, "T20 Ace", 32, 87, 81, 100L, "Powerplay Mystery Spin"),
        p("ipl114", "Akeal Hosein", "West Indies", "WI", PlayerRole.SPINNER, "T20 Ace", 44, 86, 86, 100L, "Arm-Ball Powerplay Spin"),
        p("ipl115", "Keshav Maharaj", "South Africa", "RSA", PlayerRole.SPINNER, "INTL Star", 48, 87, 82, 100L, "Control Left-Arm Spin"),

        // --- EXTRA DOMESTIC INDIAN DEPTH TO ENSURE 10 x 25 SQUADS ALWAYS SOLVABLE ---
        p("ipl116", "Arshad Khan", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 68, 81, 81, 30L, "Left-Arm Swing & Sixes"),
        p("ipl117", "Lalit Yadav", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 74, 76, 84, 30L, "Off-Spin Finisher AR"),
        p("ipl118", "Prerak Mankad", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 75, 72, 82, 30L, "Utility Seam AR"),
        p("ipl119", "Rishi Dhawan", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 72, 78, 82, 30L, "Swing All-Rounder"),
        p("ipl120", "Sanvir Singh", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 76, 70, 82, 30L, "Death-Over Striker"),
        p("ipl121", "Saurav Chauhan", "India", "IND", PlayerRole.BATTER, "Emerging", 77, 15, 81, 30L, "Fastest-50 Left-Hander"),
        p("ipl122", "Nehal Kulkarni", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 76, 10, 83, 30L, "Agile Domestic WK"),
        p("ipl123", "Urvil Patel", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 80, 10, 84, 30L, "28-Ball T20 Centurion WK"),
        p("ipl124", "Upendra Yadav", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Domestic", 77, 10, 85, 30L, "Clean Keeper-Bat"),
        p("ipl125", "Suyash Prabhudessai", "India", "IND", PlayerRole.BATTER, "IPL Domestic", 78, 22, 86, 30L, "Gun Outfielder Bat"),
        p("ipl126", "Rohan Kunnummal", "India", "IND", PlayerRole.BATTER, "Emerging", 78, 15, 83, 30L, "Attacking Opener"),
        p("ipl127", "Riki Bhui", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Domestic", 77, 10, 83, 30L, "Middle-Order WK"),
        p("ipl128", "Shivam Mavi", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 42, 82, 82, 50L, "Skiddy 145 kph Seam"),
        p("ipl129", "Kartik Tyagi", "India", "IND", PlayerRole.SEAMER, "IPL Domestic", 24, 82, 80, 50L, "Death-Over Yorker Pace"),
        p("ipl130", "Navdeep Saini", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 28, 82, 81, 50L, "148 kph Sharp Bouncer"),
        p("ipl131", "Vidwath Kaverappa", "India", "IND", PlayerRole.SEAMER, "Emerging", 20, 81, 79, 30L, "Seam-Movement Bowler"),
        p("ipl132", "Rajvardhan Hangargekar", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 68, 80, 82, 30L, "U19 144 kph Seam AR"),
        p("ipl133", "Shams Mulani", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Domestic", 73, 79, 84, 30L, "Mumbai Left-Arm Spin AR"),
        p("ipl134", "Tanush Kotian", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 75, 80, 83, 30L, "Ranji MVP Off-Spin AR"),
        p("ipl135", "Murugan Ashwin", "India", "IND", PlayerRole.SPINNER, "IPL Domestic", 26, 80, 80, 30L, "Quick Googly Leggie"),
        p("ipl136", "Raghav Goyal", "India", "IND", PlayerRole.SPINNER, "Emerging", 20, 79, 78, 30L, "Left-Arm Wrist Spin")
    )

    /**
     * Uncapped Indian IDs under official IPL rules:
     * Indian player who has NOT played international cricket in the last 5 years AND holds no BCCI central contract.
     * (Includes veterans like MS Dhoni & Sandeep Sharma under the official 5-year uncapped rule, plus domestic/emerging Indian stars!)
     */
    private val uncappedIndianIds: Set<String> = setOf(
        // Base database uncapped Indians (no international cricket in last 5 years & no central contract)
        "p015", // Shashank Singh
        "p016", // Ashutosh Sharma
        "p017", // Prabhsimran Singh
        "p018", // Nehal Wadhera
        "p020", // Rahul Tripathi (no central contract)
        "p021", // Angkrish Raghuvanshi
        "p022", // Manish Pandey (>5 yrs)
        "p035", // MS Dhoni (official IPL 5-year uncapped Indian rule!)
        "p043", // Abishek Porel
        "p060", // Rahul Tewatia
        "p061", // Shahbaz Ahmed
        "p087", // Sandeep Sharma (>5 yrs since last intl!)
        "p090", // Mohit Sharma (>5 yrs since last intl!)
        "p107", // Amit Mishra
        "p108", // Piyush Chawla
        "p111", // Suyash Sharma
        "p112", // Mayank Markande
        "p113", // Harpreet Brar
        // Extra 2026 uncapped Indians
        "ipl001", "ipl002", "ipl003", "ipl004", "ipl005", "ipl006", "ipl007", "ipl008",
        "ipl009", "ipl010", "ipl012", "ipl016", "ipl017", "ipl018", "ipl019", "ipl020",
        "ipl021", "ipl022", "ipl023", "ipl029", "ipl030", "ipl031", "ipl032", "ipl033",
        "ipl034", "ipl035", "ipl041", "ipl042", "ipl043", "ipl044", "ipl045", "ipl046",
        "ipl047", "ipl048", "ipl049", "ipl050", "ipl051", "ipl052", "ipl053", "ipl054",
        "ipl055", "ipl056", "ipl067", "ipl068", "ipl069", "ipl070", "ipl071", "ipl073",
        "ipl074", "ipl075", "ipl076", "ipl077", "ipl082", "ipl083", "ipl084", "ipl085",
        "ipl086", "ipl087", "ipl088", "ipl089", "ipl090", "ipl102", "ipl103", "ipl104",
        "ipl105", "ipl106", "ipl107", "ipl108", "ipl109", "ipl110", "ipl111", "ipl116",
        "ipl117", "ipl118", "ipl119", "ipl120", "ipl121", "ipl122", "ipl123", "ipl124",
        "ipl125", "ipl126", "ipl127", "ipl128", "ipl129", "ipl131", "ipl132", "ipl133",
        "ipl134", "ipl135", "ipl136"
    )

    /**
     * Emerging Player of the Season eligibility (young breakthrough IPL talents, aged <=25 / ≤5 Tests / ≤25 IPL games style).
     */
    private val emergingEligibleIds: Set<String> = setOf(
        "p010", "p016", "p017", "p018", "p021", "p042", "p043", "p057", "p064",
        "p081", "p082", "p110", "p111", "p117",
        "ipl001", "ipl002", "ipl003", "ipl005", "ipl008", "ipl009", "ipl010",
        "ipl016", "ipl017", "ipl018", "ipl020", "ipl021", "ipl023", "ipl024", "ipl027",
        "ipl029", "ipl031", "ipl033", "ipl034", "ipl035", "ipl043", "ipl044", "ipl046",
        "ipl047", "ipl048", "ipl049", "ipl050", "ipl052", "ipl053", "ipl054", "ipl056",
        "ipl060", "ipl068", "ipl074", "ipl077", "ipl084", "ipl085", "ipl086", "ipl087",
        "ipl090", "ipl098", "ipl102", "ipl103", "ipl104", "ipl107", "ipl110", "ipl111",
        "ipl112", "ipl116", "ipl121", "ipl122", "ipl123", "ipl126", "ipl131", "ipl132",
        "ipl134", "ipl136"
    )

    fun isUncappedIndian(player: Cricketer): Boolean =
        !player.overseas && (player.id in uncappedIndianIds || player.eraTag.equals("Emerging", ignoreCase = true))

    fun isUncappedIndian(playerId: String): Boolean {
        val player = CricketerDatabase.getById(playerId) ?: return false
        return isUncappedIndian(player)
    }

    fun isEmergingEligible(playerId: String): Boolean {
        if (playerId in emergingEligibleIds) return true
        val player = CricketerDatabase.getById(playerId) ?: return false
        return player.eraTag.equals("Emerging", ignoreCase = true)
    }

    /**
     * All 10 Official 2026 IPL Franchises with Captains, Defending Champions badge (RCB),
     * and realistic 2026 core former-squad pools (capped Indian, uncapped Indian, overseas)
     * eligible for Pre-Auction Retentions & RTM cards!
     */
    val franchises: List<IplFranchiseBlueprint> = listOf(
        IplFranchiseBlueprint(
            id = "ipl_rcb",
            shortCode = "RCB",
            name = "Royal Challengers Bengaluru",
            homeCity = "Bengaluru",
            homeVenue = "M. Chinnaswamy Stadium",
            captainId = "p011",
            captainName = "Rajat Patidar",
            defendingChampion = true,
            primaryColorHex = 0xFFD71920L,
            accentColorHex = 0xFFFFC107L,
            coreRosterIds = listOf(
                "p011", // Rajat Patidar (C)
                "p001", // Virat Kohli
                "p056", // Venkatesh Iyer
                "p034", // Tim David (OS)
                "p041", // Jitesh Sharma (WK)
                "p058", // Krunal Pandya
                "p079", // Bhuvneshwar Kumar
                "p097", // Josh Hazlewood (OS)
                "p048", // Phil Salt (OS WK)
                "ipl067", // Yash Dayal (Uncapped IND)
                "p111", // Suyash Sharma (Uncapped IND)
                "p073", // Liam Livingstone (OS)
                "ipl059", // Romario Shepherd (OS)
                "ipl068", // Rasikh Salam (Uncapped IND)
                "ipl042", // Swapnil Singh (Uncapped IND)
                "ipl060"  // Jacob Bethell (OS)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_gt",
            shortCode = "GT",
            name = "Gujarat Titans",
            homeCity = "Ahmedabad",
            homeVenue = "Narendra Modi Stadium",
            captainId = "p004",
            captainName = "Shubman Gill",
            primaryColorHex = 0xFF1B2133L,
            accentColorHex = 0xFFDFB962L,
            coreRosterIds = listOf(
                "p004", // Shubman Gill (C)
                "p114", // Rashid Khan (OS)
                "p044", // Jos Buttler (OS WK)
                "p095", // Kagiso Rabada (OS)
                "p078", // Mohammed Siraj
                "p055", // Washington Sundar
                "p089", // Prasidh Krishna
                "p010", // Sai Sudharsan
                "p060", // Rahul Tewatia (Uncapped IND)
                "ipl006", // Shahrukh Khan (Uncapped IND)
                "p110", // Sai Kishore
                "ipl026", // Sherfane Rutherford (OS)
                "ipl091", // Gerald Coetzee (OS)
                "ipl041", // Mahipal Lomror (Uncapped IND)
                "ipl116", // Arshad Khan (Uncapped IND)
                "ipl030"  // Anuj Rawat (Uncapped IND WK)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_csk",
            shortCode = "CSK",
            name = "Chennai Super Kings",
            homeCity = "Chennai",
            homeVenue = "M. A. Chidambaram Stadium",
            captainId = "p006",
            captainName = "Ruturaj Gaikwad",
            primaryColorHex = 0xFFF9CD05L,
            accentColorHex = 0xFF0081E9L,
            coreRosterIds = listOf(
                "p006", // Ruturaj Gaikwad (C)
                "p038", // Sanju Samson (WK)
                "p035", // MS Dhoni (Uncapped IND WK!)
                "p050", // Ravindra Jadeja
                "p053", // Shivam Dube
                "p096", // Matheesha Pathirana (OS)
                "p117", // Noor Ahmad (OS)
                "p105", // Ravichandran Ashwin
                "p086", // Khaleel Ahmed
                "p063", // Deepak Hooda
                "p020", // Rahul Tripathi (Uncapped IND)
                "ipl043", // Anshul Kamboj (Uncapped IND)
                "ipl077", // Gurjapneet Singh (Uncapped IND)
                "ipl096", // Nathan Ellis (OS)
                "ipl064", // Jamie Overton (OS)
                "ipl016"  // Shaik Rasheed (Uncapped IND)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_mi",
            shortCode = "MI",
            name = "Mumbai Indians",
            homeCity = "Mumbai",
            homeVenue = "Wankhede Stadium",
            captainId = "p049",
            captainName = "Hardik Pandya",
            primaryColorHex = 0xFF004BA0L,
            accentColorHex = 0xFFD1AB3EL,
            coreRosterIds = listOf(
                "p049", // Hardik Pandya (C)
                "p002", // Rohit Sharma
                "p003", // Suryakumar Yadav
                "p075", // Jasprit Bumrah
                "p009", // Tilak Varma
                "p094", // Trent Boult (OS)
                "p083", // Deepak Chahar
                "p120", // Mitchell Santner (OS)
                "ipl058", // Will Jacks (OS)
                "ipl037", // Ryan Rickelton (OS WK)
                "ipl005", // Naman Dhir (Uncapped IND)
                "ipl029", // Robin Minz (Uncapped IND WK)
                "ipl102", // Vignesh Puthur (Uncapped IND)
                "ipl094", // Reece Topley (OS)
                "ipl105", // Karn Sharma (Uncapped IND)
                "ipl085"  // Ashwani Kumar (Uncapped IND)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_kkr",
            shortCode = "KKR",
            name = "Kolkata Knight Riders",
            homeCity = "Kolkata",
            homeVenue = "Eden Gardens",
            captainId = "p014",
            captainName = "Ajinkya Rahane",
            primaryColorHex = 0xFF3A225DL,
            accentColorHex = 0xFFD4AF37L,
            coreRosterIds = listOf(
                "p014", // Ajinkya Rahane (C)
                "p008", // Rinku Singh
                "p103", // Varun Chakravarthy
                "p066", // Sunil Narine (OS)
                "p065", // Andre Russell (OS)
                "p081", // Harshit Rana
                "p064", // Ramandeep Singh
                "p047", // Quinton de Kock (OS WK)
                "p021", // Angkrish Raghuvanshi (Uncapped IND)
                "ipl069", // Vaibhav Arora (Uncapped IND)
                "ipl092", // Spencer Johnson (OS)
                "ipl039", // Rahmanullah Gurbaz (OS WK)
                "p022", // Manish Pandey (Uncapped IND)
                "ipl051", // Anukul Roy (Uncapped IND)
                "ipl063", // Moeen Ali (OS)
                "ipl078"  // Umran Malik
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_pbks",
            shortCode = "PBKS",
            name = "Punjab Kings",
            homeCity = "New Chandigarh",
            homeVenue = "Maharaja Yadavindra Singh Stadium",
            captainId = "p007",
            captainName = "Shreyas Iyer",
            primaryColorHex = 0xFFDD1F2DL,
            accentColorHex = 0xFFD7D7D7L,
            coreRosterIds = listOf(
                "p007", // Shreyas Iyer (C)
                "p077", // Arshdeep Singh
                "p102", // Yuzvendra Chahal
                "p072", // Marcus Stoinis (OS)
                "p067", // Glenn Maxwell (OS)
                "p017", // Prabhsimran Singh (Uncapped IND)
                "p015", // Shashank Singh (Uncapped IND)
                "p018", // Nehal Wadhera (Uncapped IND)
                "p100", // Lockie Ferguson (OS)
                "ipl038", // Josh Inglis (OS WK)
                "p113", // Harpreet Brar (Uncapped IND)
                "ipl002", // Priyansh Arya (Uncapped IND)
                "ipl075", // Vijaykumar Vyshak (Uncapped IND)
                "ipl061", // Azmatullah Omarzai (OS)
                "ipl047", // Suryansh Shedge (Uncapped IND)
                "ipl097"  // Xavier Bartlett (OS)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_lsg",
            shortCode = "LSG",
            name = "Lucknow Super Giants",
            homeCity = "Lucknow",
            homeVenue = "BRSABV Ekana Stadium",
            captainId = "p036",
            captainName = "Rishabh Pant",
            primaryColorHex = 0xFF0057E2L,
            accentColorHex = 0xFFFF6F00L,
            coreRosterIds = listOf(
                "p036", // Rishabh Pant (C WK)
                "p046", // Nicholas Pooran (OS WK)
                "p082", // Mayank Yadav
                "p104", // Ravi Bishnoi
                "p028", // David Miller (OS)
                "ipl057", // Mitchell Marsh (OS)
                "p033", // Aiden Markram (OS)
                "p084", // Avesh Khan
                "ipl071", // Mohsin Khan (Uncapped IND)
                "ipl003", // Ayush Badoni (Uncapped IND)
                "ipl004", // Abdul Samad (Uncapped IND)
                "ipl072", // Akash Deep
                "p061", // Shahbaz Ahmed (Uncapped IND)
                "ipl103", // Digvesh Rathi (Uncapped IND)
                "ipl028", // Matthew Breetzke (OS)
                "ipl033"  // Aryan Juyal (Uncapped IND WK)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_dc",
            shortCode = "DC",
            name = "Delhi Capitals",
            homeCity = "New Delhi",
            homeVenue = "Arun Jaitley Stadium",
            captainId = "p051",
            captainName = "Axar Patel",
            primaryColorHex = 0xFF17479EL,
            accentColorHex = 0xFFEF1B23L,
            coreRosterIds = listOf(
                "p051", // Axar Patel (C)
                "p037", // KL Rahul (WK)
                "p101", // Kuldeep Yadav
                "p092", // Mitchell Starc (OS)
                "p080", // T Natarajan
                "p032", // Jake Fraser-McGurk (OS)
                "p031", // Harry Brook (OS)
                "ipl025", // Tristan Stubbs (OS)
                "p027", // Faf du Plessis (OS)
                "p043", // Abishek Porel (Uncapped IND WK)
                "p016", // Ashutosh Sharma (Uncapped IND)
                "p088", // Mukesh Kumar
                "ipl008", // Sameer Rizvi (Uncapped IND)
                "ipl044", // Vipraj Nigam (Uncapped IND)
                "p090", // Mohit Sharma (Uncapped IND)
                "ipl012"  // Karun Nair (Uncapped IND)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_rr",
            shortCode = "RR",
            name = "Rajasthan Royals",
            homeCity = "Jaipur",
            homeVenue = "Sawai Mansingh Stadium",
            captainId = "p059",
            captainName = "Riyan Parag",
            primaryColorHex = 0xFFEA1A85L,
            accentColorHex = 0xFF254AA5L,
            coreRosterIds = listOf(
                "p059", // Riyan Parag (C)
                "ipl001", // Vaibhav Sooryavanshi (Uncapped IND Prodigy!)
                "p005", // Yashasvi Jaiswal
                "p042", // Dhruv Jurel (WK)
                "p030", // Shimron Hetmyer (OS)
                "p098", // Jofra Archer (OS)
                "p115", // Wanindu Hasaranga (OS)
                "p118", // Maheesh Theekshana (OS)
                "p087", // Sandeep Sharma (Uncapped IND)
                "ipl070", // Tushar Deshpande (Uncapped IND)
                "ipl011", // Nitish Rana
                "ipl073", // Akash Madhwal (Uncapped IND)
                "ipl010", // Shubham Dubey (Uncapped IND)
                "ipl093", // Fazalhaq Farooqi (OS)
                "ipl098", // Kwena Maphaka (OS)
                "ipl035"  // Kunal Singh Rathore (Uncapped IND WK)
            )
        ),
        IplFranchiseBlueprint(
            id = "ipl_srh",
            shortCode = "SRH",
            name = "Sunrisers Hyderabad",
            homeCity = "Hyderabad",
            homeVenue = "Rajiv Gandhi Intl Stadium",
            captainId = "p093",
            captainName = "Pat Cummins",
            primaryColorHex = 0xFFF26522L,
            accentColorHex = 0xFF1A1A1AL,
            coreRosterIds = listOf(
                "p093", // Pat Cummins (C OS)
                "p054", // Abhishek Sharma
                "p039", // Ishan Kishan (WK)
                "p025", // Travis Head (OS)
                "p045", // Heinrich Klaasen (OS WK)
                "p057", // Nitish Kumar Reddy
                "p076", // Mohammed Shami
                "p085", // Harshal Patel
                "p116", // Adam Zampa (OS)
                "p109", // Rahul Chahar
                "ipl007", // Abhinav Manohar (Uncapped IND)
                "ipl074", // Simarjeet Singh (Uncapped IND)
                "ipl009", // Aniket Verma (Uncapped IND)
                "ipl104", // Zeeshan Ansari (Uncapped IND)
                "ipl095", // Brydon Carse (OS)
                "ipl020"  // Atharva Taide (Uncapped IND)
            )
        )
    )

    fun byId(id: String): IplFranchiseBlueprint? = franchises.firstOrNull { it.id == id }

    /**
     * Full 250+ player auction pool for the 10-team IPL Mega Auction:
     * Excludes retired legends that break realism if desired, OR keeps modern IPL + 2026 stars first
     * so every single one of the 10 teams finishes with 18–25 authentic players!
     */
    fun fullIplAuctionPool(excludedIds: Set<String> = emptySet()): List<Cricketer> {
        val modernBase = CricketerDatabase.allPlayers.filter { p ->
            p.id !in excludedIds && p.id !in setOf("p012", "p023", "p024", "p052", "p069", "p070", "p091", "p099", "p106")
        }
        val combined = (modernBase + extraPlayers.filter { it.id !in excludedIds })
            .distinctBy { it.id }

        val marquee = combined.filter { it.basePriceLakhs >= 200L }.shuffled()
        val tier1 = combined.filter { it.basePriceLakhs in 100L..199L }.shuffled()
        val tier2 = combined.filter { it.basePriceLakhs in 50L..99L }.shuffled()
        val uncapped = combined.filter { it.basePriceLakhs < 50L }.shuffled()
        return marquee + tier1 + tier2 + uncapped
    }
}
