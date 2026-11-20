package com.example.data.model

/**
 * Compact, lightweight pool of 120 famous International & IPL cricketers.
 * Guaranteed solvable for 8 full franchises (8 x 11 = 88 minimum slots; 8 x 12 = 96 with Impact Subs):
 * - 76 Indian players, 44 Overseas players
 * - 14 Wicketkeepers (9 Indian, 5 Overseas)
 * - 34 Batters, 26 All-Rounders, 26 Seamers, 20 Spinners
 */
object CricketerDatabase {

    private fun p(
        id: String, name: String, country: String, code: String, role: PlayerRole,
        era: String, bat: Int, bowl: Int, fld: Int, base: Long, spec: String
    ) = Cricketer(id, name, country, code, country != "India", role, era, bat, bowl, fld, base, spec)

    private val basePlayers: List<Cricketer> = listOf(
        // --- BATTERS (34: 22 Indian, 12 Overseas) ---
        p("p001", "Virat Kohli", "India", "IND", PlayerRole.BATTER, "IPL Icon", 97, 32, 93, 200L, "Chase Master"),
        p("p002", "Rohit Sharma", "India", "IND", PlayerRole.BATTER, "IPL Icon", 95, 34, 84, 200L, "Powerplay Hitman"),
        p("p003", "Suryakumar Yadav", "India", "IND", PlayerRole.BATTER, "T20 Ace", 96, 25, 90, 200L, "360° Finisher"),
        p("p004", "Shubman Gill", "India", "IND", PlayerRole.BATTER, "IPL Star", 92, 22, 86, 150L, "Classical Opener"),
        p("p005", "Yashasvi Jaiswal", "India", "IND", PlayerRole.BATTER, "IPL Star", 91, 24, 86, 150L, "Explosive Left-Hander"),
        p("p006", "Ruturaj Gaikwad", "India", "IND", PlayerRole.BATTER, "IPL Star", 89, 20, 85, 150L, "Anchor Opener"),
        p("p007", "Shreyas Iyer", "India", "IND", PlayerRole.BATTER, "IPL Captain", 88, 22, 85, 150L, "Spin Crusher"),
        p("p008", "Rinku Singh", "India", "IND", PlayerRole.BATTER, "IPL Finisher", 90, 20, 89, 100L, "Death-Over Finisher"),
        p("p009", "Tilak Varma", "India", "IND", PlayerRole.BATTER, "T20 Ace", 87, 38, 86, 100L, "Middle-Order Dynamo"),
        p("p010", "Sai Sudharsan", "India", "IND", PlayerRole.BATTER, "IPL Star", 86, 20, 83, 75L, "Consistent Opener"),
        p("p011", "Rajat Patidar", "India", "IND", PlayerRole.BATTER, "IPL Star", 86, 18, 82, 75L, "Spin Enforcer"),
        p("p012", "Suresh Raina", "India", "IND", PlayerRole.BATTER, "IPL Legend", 92, 54, 95, 200L, "Mr. IPL"),
        p("p013", "Shikhar Dhawan", "India", "IND", PlayerRole.BATTER, "IPL Legend", 89, 20, 85, 150L, "Gabbar Opener"),
        p("p014", "Ajinkya Rahane", "India", "IND", PlayerRole.BATTER, "IPL Veteran", 83, 18, 88, 75L, "Classical Timer"),
        p("p015", "Shashank Singh", "India", "IND", PlayerRole.BATTER, "IPL Finisher", 84, 30, 83, 50L, "Clutch Chaser"),
        p("p016", "Ashutosh Sharma", "India", "IND", PlayerRole.BATTER, "Emerging", 83, 22, 82, 50L, "Impact Hitter"),
        p("p017", "Prabhsimran Singh", "India", "IND", PlayerRole.BATTER, "Emerging", 82, 15, 81, 50L, "Powerplay Striker"),
        p("p018", "Nehal Wadhera", "India", "IND", PlayerRole.BATTER, "Emerging", 81, 26, 84, 50L, "Left-Hand Finisher"),
        p("p019", "Devdutt Padikkal", "India", "IND", PlayerRole.BATTER, "IPL Star", 82, 16, 82, 75L, "Tall Left-Hander"),
        p("p020", "Rahul Tripathi", "India", "IND", PlayerRole.BATTER, "IPL Star", 83, 18, 86, 75L, "Fearless #3"),
        p("p021", "Angkrish Raghuvanshi", "India", "IND", PlayerRole.BATTER, "Emerging", 80, 18, 84, 50L, "Young Stroke Maker"),
        p("p022", "Manish Pandey", "India", "IND", PlayerRole.BATTER, "IPL Veteran", 81, 16, 90, 50L, "Gun Outfielder"),
        p("p023", "AB de Villiers", "South Africa", "RSA", PlayerRole.BATTER, "IPL Legend", 97, 24, 96, 200L, "Mr. 360 Legend"),
        p("p024", "Chris Gayle", "West Indies", "WI", PlayerRole.BATTER, "IPL Legend", 95, 45, 76, 200L, "Universe Boss"),
        p("p025", "Travis Head", "Australia", "AUS", PlayerRole.BATTER, "INTL Star", 95, 42, 86, 200L, "Fearless Opener"),
        p("p026", "David Warner", "Australia", "AUS", PlayerRole.BATTER, "IPL Legend", 92, 22, 91, 150L, "3x Orange Cap"),
        p("p027", "Faf du Plessis", "South Africa", "RSA", PlayerRole.BATTER, "IPL Veteran", 90, 20, 93, 150L, "Athletic Captain"),
        p("p028", "David Miller", "South Africa", "RSA", PlayerRole.BATTER, "INTL Star", 90, 18, 91, 150L, "Killer Finisher"),
        p("p029", "Kane Williamson", "New Zealand", "NZ", PlayerRole.BATTER, "INTL Star", 88, 30, 88, 100L, "Tactical Anchor"),
        p("p030", "Shimron Hetmyer", "West Indies", "WI", PlayerRole.BATTER, "IPL Finisher", 87, 15, 85, 100L, "Power Hitter"),
        p("p031", "Harry Brook", "England", "ENG", PlayerRole.BATTER, "INTL Star", 86, 25, 86, 100L, "Stroke Maker"),
        p("p032", "Jake Fraser-McGurk", "Australia", "AUS", PlayerRole.BATTER, "T20 Ace", 87, 16, 85, 100L, "Powerplay Blitzer"),
        p("p033", "Aiden Markram", "South Africa", "RSA", PlayerRole.BATTER, "INTL Star", 86, 52, 91, 100L, "Stylish Leader"),
        p("p034", "Tim David", "Australia", "AUS", PlayerRole.BATTER, "T20 Ace", 87, 28, 86, 100L, "Death-Over Monster"),

        // --- WICKETKEEPERS (14: 9 Indian, 5 Overseas) ---
        p("p035", "MS Dhoni", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Legend", 91, 18, 97, 200L, "Captain Cool Finisher"),
        p("p036", "Rishabh Pant", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Icon", 93, 15, 90, 200L, "Counter-Attack WK"),
        p("p037", "KL Rahul", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Icon", 92, 15, 88, 200L, "Versatile Top-Order WK"),
        p("p038", "Sanju Samson", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Captain", 91, 15, 89, 150L, "Flamboyant Keeper"),
        p("p039", "Ishan Kishan", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Star", 87, 12, 86, 100L, "Pocket Dynamo WK"),
        p("p040", "Dinesh Karthik", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Legend", 86, 12, 89, 100L, "Death Finisher WK"),
        p("p041", "Jitesh Sharma", "India", "IND", PlayerRole.WICKETKEEPER, "IPL Star", 84, 12, 86, 75L, "Lower-Order Striker"),
        p("p042", "Dhruv Jurel", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 83, 12, 88, 75L, "Calm Finisher WK"),
        p("p043", "Abishek Porel", "India", "IND", PlayerRole.WICKETKEEPER, "Emerging", 81, 10, 84, 50L, "Aggressive Left-Hand WK"),
        p("p044", "Jos Buttler", "England", "ENG", PlayerRole.WICKETKEEPER, "INTL Star", 95, 15, 90, 200L, "T20 Superstar WK"),
        p("p045", "Heinrich Klaasen", "South Africa", "RSA", PlayerRole.WICKETKEEPER, "T20 Ace", 95, 15, 88, 200L, "Spin Destroyer WK"),
        p("p046", "Nicholas Pooran", "West Indies", "WI", PlayerRole.WICKETKEEPER, "T20 Ace", 94, 18, 90, 200L, "Six-Hitting Machine"),
        p("p047", "Quinton de Kock", "South Africa", "RSA", PlayerRole.WICKETKEEPER, "INTL Star", 90, 12, 91, 150L, "Left-Hand Opener WK"),
        p("p048", "Phil Salt", "England", "ENG", PlayerRole.WICKETKEEPER, "T20 Ace", 90, 14, 87, 150L, "Powerplay Crusher WK"),

        // --- ALL-ROUNDERS (26: 16 Indian, 10 Overseas) ---
        p("p049", "Hardik Pandya", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Icon", 90, 87, 92, 200L, "Pace All-Rounder"),
        p("p050", "Ravindra Jadeja", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Legend", 85, 89, 98, 200L, "3D Gun Fielder"),
        p("p051", "Axar Patel", "India", "IND", PlayerRole.ALL_ROUNDER, "INTL Star", 84, 88, 88, 150L, "Economical Spin AR"),
        p("p052", "Yuvraj Singh", "India", "IND", PlayerRole.ALL_ROUNDER, "INTL Legend", 92, 76, 92, 200L, "Six-Sixes Legend"),
        p("p053", "Shivam Dube", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Star", 89, 68, 80, 100L, "Spin-Smashing AR"),
        p("p054", "Abhishek Sharma", "India", "IND", PlayerRole.ALL_ROUNDER, "T20 Ace", 90, 66, 84, 150L, "Blistering Opener AR"),
        p("p055", "Washington Sundar", "India", "IND", PlayerRole.ALL_ROUNDER, "INTL Star", 78, 83, 83, 75L, "Powerplay Off-Spin AR"),
        p("p056", "Venkatesh Iyer", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Star", 85, 66, 82, 100L, "Big-Match Clutch AR"),
        p("p057", "Nitish Kumar Reddy", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 84, 76, 85, 75L, "Seam-Bowling Finisher"),
        p("p058", "Krunal Pandya", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Veteran", 77, 83, 85, 75L, "Tight Left-Arm Spin AR"),
        p("p059", "Riyan Parag", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Star", 85, 68, 88, 100L, "Middle-Order Enforcer"),
        p("p060", "Rahul Tewatia", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Finisher", 84, 70, 83, 75L, "Last-Over Miracle AR"),
        p("p061", "Shahbaz Ahmed", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Star", 76, 79, 84, 50L, "Utility Spin AR"),
        p("p062", "Shardul Thakur", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Star", 72, 82, 81, 75L, "Golden-Arm Partnership Breaker"),
        p("p063", "Deepak Hooda", "India", "IND", PlayerRole.ALL_ROUNDER, "IPL Veteran", 79, 66, 85, 50L, "Power-Hitting AR"),
        p("p064", "Ramandeep Singh", "India", "IND", PlayerRole.ALL_ROUNDER, "Emerging", 81, 71, 90, 50L, "Electric Finisher AR"),
        p("p065", "Andre Russell", "West Indies", "WI", PlayerRole.ALL_ROUNDER, "IPL Legend", 93, 86, 89, 200L, "Dre Russ Powerhouse"),
        p("p066", "Sunil Narine", "West Indies", "WI", PlayerRole.ALL_ROUNDER, "IPL Legend", 86, 94, 83, 200L, "Mystery Spin & Opener"),
        p("p067", "Glenn Maxwell", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "INTL Star", 91, 78, 95, 200L, "The Big Show"),
        p("p068", "Ben Stokes", "England", "ENG", PlayerRole.ALL_ROUNDER, "INTL Legend", 89, 84, 93, 200L, "Big-Stage Match Winner"),
        p("p069", "Kieron Pollard", "West Indies", "WI", PlayerRole.ALL_ROUNDER, "IPL Legend", 91, 79, 94, 200L, "Clutch Death Finisher"),
        p("p070", "Shane Watson", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "IPL Legend", 90, 83, 86, 150L, "Finals MVP All-Rounder"),
        p("p071", "Sam Curran", "England", "ENG", PlayerRole.ALL_ROUNDER, "INTL Star", 81, 85, 86, 150L, "Left-Arm Death AR"),
        p("p072", "Marcus Stoinis", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "INTL Star", 87, 79, 86, 150L, "Hulk Finisher AR"),
        p("p073", "Liam Livingstone", "England", "ENG", PlayerRole.ALL_ROUNDER, "T20 Ace", 88, 75, 88, 100L, "Monster Six Hitter"),
        p("p074", "Cameron Green", "Australia", "AUS", PlayerRole.ALL_ROUNDER, "INTL Star", 86, 82, 91, 150L, "Tall Pace All-Rounder"),

        // --- SEAMERS (26: 16 Indian, 10 Overseas) ---
        p("p075", "Jasprit Bumrah", "India", "IND", PlayerRole.SEAMER, "IPL Icon", 34, 98, 83, 200L, "Yorker King"),
        p("p076", "Mohammed Shami", "India", "IND", PlayerRole.SEAMER, "INTL Star", 30, 93, 78, 150L, "Seam Maestro"),
        p("p077", "Arshdeep Singh", "India", "IND", PlayerRole.SEAMER, "T20 Ace", 28, 92, 81, 150L, "Left-Arm Swing & Death"),
        p("p078", "Mohammed Siraj", "India", "IND", PlayerRole.SEAMER, "INTL Star", 26, 90, 83, 150L, "New-Ball Enforcer"),
        p("p079", "Bhuvneshwar Kumar", "India", "IND", PlayerRole.SEAMER, "IPL Legend", 44, 89, 82, 150L, "2x Purple Cap Swing"),
        p("p080", "T Natarajan", "India", "IND", PlayerRole.SEAMER, "IPL Star", 20, 87, 77, 100L, "Death Yorker Specialist"),
        p("p081", "Harshit Rana", "India", "IND", PlayerRole.SEAMER, "Emerging", 42, 86, 82, 75L, "Fiery Pace Prospect"),
        p("p082", "Mayank Yadav", "India", "IND", PlayerRole.SEAMER, "Emerging", 24, 88, 79, 100L, "155 kph Express Pace"),
        p("p083", "Deepak Chahar", "India", "IND", PlayerRole.SEAMER, "IPL Star", 52, 86, 80, 100L, "Powerplay Swing Ace"),
        p("p084", "Avesh Khan", "India", "IND", PlayerRole.SEAMER, "IPL Star", 24, 85, 78, 75L, "Hard-Length Pace"),
        p("p085", "Harshal Patel", "India", "IND", PlayerRole.SEAMER, "IPL Star", 48, 88, 80, 100L, "Slower-Ball Purple Cap"),
        p("p086", "Khaleel Ahmed", "India", "IND", PlayerRole.SEAMER, "IPL Star", 22, 85, 79, 75L, "Left-Arm Powerplay Pace"),
        p("p087", "Sandeep Sharma", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 28, 86, 78, 75L, "Swing & Death Control"),
        p("p088", "Mukesh Kumar", "India", "IND", PlayerRole.SEAMER, "IPL Star", 22, 84, 78, 50L, "Pinpoint Death Seam"),
        p("p089", "Prasidh Krishna", "India", "IND", PlayerRole.SEAMER, "INTL Star", 22, 86, 78, 75L, "Hit-the-Deck Bounce"),
        p("p090", "Mohit Sharma", "India", "IND", PlayerRole.SEAMER, "IPL Veteran", 26, 85, 80, 50L, "Back-of-Hand Specialist"),
        p("p091", "Lasith Malinga", "Sri Lanka", "SL", PlayerRole.SEAMER, "IPL Legend", 28, 97, 79, 200L, "Slinga Yorker Legend"),
        p("p092", "Mitchell Starc", "Australia", "AUS", PlayerRole.SEAMER, "INTL Star", 46, 95, 84, 200L, "Express Left-Arm Pace"),
        p("p093", "Pat Cummins", "Australia", "AUS", PlayerRole.SEAMER, "INTL Star", 58, 93, 87, 200L, "Champion Pace Captain"),
        p("p094", "Trent Boult", "New Zealand", "NZ", PlayerRole.SEAMER, "INTL Star", 34, 94, 87, 200L, "First-Over Wicket King"),
        p("p095", "Kagiso Rabada", "South Africa", "RSA", PlayerRole.SEAMER, "INTL Star", 40, 92, 83, 150L, "Raw Pace Strike Bowler"),
        p("p096", "Matheesha Pathirana", "Sri Lanka", "SL", PlayerRole.SEAMER, "T20 Ace", 20, 92, 80, 150L, "Baby Malinga Death Ace"),
        p("p097", "Josh Hazlewood", "Australia", "AUS", PlayerRole.SEAMER, "INTL Star", 28, 92, 82, 150L, "Metronomic Line & Length"),
        p("p098", "Jofra Archer", "England", "ENG", PlayerRole.SEAMER, "T20 Ace", 48, 92, 88, 150L, "150 kph Bouncer Threat"),
        p("p099", "Dwayne Bravo", "West Indies", "WI", PlayerRole.SEAMER, "IPL Legend", 68, 92, 91, 150L, "Champion Slower-Ball King"),
        p("p100", "Lockie Ferguson", "New Zealand", "NZ", PlayerRole.SEAMER, "INTL Star", 26, 89, 83, 100L, "Thunderbolt Pace"),

        // --- SPINNERS (20: 13 Indian, 7 Overseas) ---
        p("p101", "Kuldeep Yadav", "India", "IND", PlayerRole.SPINNER, "INTL Star", 38, 93, 80, 150L, "Left-Arm Wrist Spin"),
        p("p102", "Yuzvendra Chahal", "India", "IND", PlayerRole.SPINNER, "IPL Legend", 22, 93, 78, 150L, "IPL All-Time Wicket King"),
        p("p103", "Varun Chakravarthy", "India", "IND", PlayerRole.SPINNER, "T20 Ace", 24, 92, 79, 150L, "Mystery Carrom Spinner"),
        p("p104", "Ravi Bishnoi", "India", "IND", PlayerRole.SPINNER, "T20 Ace", 32, 89, 89, 100L, "Skiddy Googly Bowler"),
        p("p105", "Ravichandran Ashwin", "India", "IND", PlayerRole.SPINNER, "IPL Legend", 62, 89, 80, 150L, "Tactical Off-Spin Genius"),
        p("p106", "Harbhajan Singh", "India", "IND", PlayerRole.SPINNER, "IPL Legend", 54, 88, 82, 100L, "Turbanator Doosra"),
        p("p107", "Amit Mishra", "India", "IND", PlayerRole.SPINNER, "IPL Legend", 34, 87, 74, 75L, "3x IPL Hat-Trick Leggie"),
        p("p108", "Piyush Chawla", "India", "IND", PlayerRole.SPINNER, "IPL Veteran", 40, 86, 76, 75L, "Veteran Googly Ace"),
        p("p109", "Rahul Chahar", "India", "IND", PlayerRole.SPINNER, "IPL Star", 30, 85, 81, 75L, "Quick Leg-Spin"),
        p("p110", "Sai Kishore", "India", "IND", PlayerRole.SPINNER, "Emerging", 38, 85, 82, 50L, "Tall Left-Arm Orthodox"),
        p("p111", "Suyash Sharma", "India", "IND", PlayerRole.SPINNER, "Emerging", 20, 84, 79, 50L, "Fearless Impact Leggie"),
        p("p112", "Mayank Markande", "India", "IND", PlayerRole.SPINNER, "IPL Star", 24, 83, 78, 50L, "Sharp Leg-Break"),
        p("p113", "Harpreet Brar", "India", "IND", PlayerRole.SPINNER, "IPL Star", 56, 84, 82, 50L, "Economical Finger Spin"),
        p("p114", "Rashid Khan", "Afghanistan", "AFG", PlayerRole.SPINNER, "T20 Legend", 68, 97, 92, 200L, "T20 Leg-Spin Icon"),
        p("p115", "Wanindu Hasaranga", "Sri Lanka", "SL", PlayerRole.SPINNER, "T20 Ace", 62, 92, 88, 150L, "Wicket-Taking Googly"),
        p("p116", "Adam Zampa", "Australia", "AUS", PlayerRole.SPINNER, "INTL Star", 28, 91, 82, 150L, "World Cup Leg-Spinner"),
        p("p117", "Noor Ahmad", "Afghanistan", "AFG", PlayerRole.SPINNER, "T20 Ace", 26, 90, 83, 100L, "Left-Arm Wrist Mystery"),
        p("p118", "Maheesh Theekshana", "Sri Lanka", "SL", PlayerRole.SPINNER, "T20 Ace", 32, 89, 80, 100L, "Powerplay Mystery Spin"),
        p("p119", "Adil Rashid", "England", "ENG", PlayerRole.SPINNER, "INTL Star", 42, 91, 81, 150L, "Classic Flight & Drift"),
        p("p120", "Mitchell Santner", "New Zealand", "NZ", PlayerRole.SPINNER, "INTL Star", 64, 90, 91, 100L, "Pinpoint Left-Arm Spin")
    )

    /**
     * Compact supplemental international stars to complete authentic 12-player squads across all 8 ICC nations
     * (Pakistan, England, Australia, South Africa, New Zealand, West Indies, Sri Lanka) while keeping
     * the IPL Auction order strictly within the 120-player pool.
     */
    val internationalExtras: List<Cricketer> = listOf(
        // Pakistan (12)
        p("i001", "Babar Azam", "Pakistan", "PAK", PlayerRole.BATTER, "INTL Icon", 93, 22, 86, 200L, "Classical Cover Drive"),
        p("i002", "Fakhar Zaman", "Pakistan", "PAK", PlayerRole.BATTER, "INTL Star", 88, 20, 82, 150L, "Explosive Left-Hander"),
        p("i003", "Saim Ayub", "Pakistan", "PAK", PlayerRole.BATTER, "Emerging", 85, 44, 83, 100L, "Fearless Stroke Opener"),
        p("i004", "Saud Shakeel", "Pakistan", "PAK", PlayerRole.BATTER, "Test Ace", 86, 25, 81, 100L, "Red-Ball Anchor"),
        p("i005", "Mohammad Rizwan", "Pakistan", "PAK", PlayerRole.WICKETKEEPER, "INTL Star", 90, 15, 91, 150L, "Consistent Keeper-Bat"),
        p("i006", "Shadab Khan", "Pakistan", "PAK", PlayerRole.ALL_ROUNDER, "INTL Star", 78, 86, 92, 150L, "Athletic Leg-Spin AR"),
        p("i007", "Salman Ali Agha", "Pakistan", "PAK", PlayerRole.ALL_ROUNDER, "INTL Star", 84, 72, 85, 100L, "Middle-Order Off-Spin AR"),
        p("i008", "Iftikhar Ahmed", "Pakistan", "PAK", PlayerRole.ALL_ROUNDER, "T20 Ace", 82, 70, 82, 75L, "Power-Hitting Finisher"),
        p("i009", "Shaheen Afridi", "Pakistan", "PAK", PlayerRole.SEAMER, "INTL Icon", 46, 94, 82, 200L, "Left-Arm Swing Spearhead"),
        p("i010", "Naseem Shah", "Pakistan", "PAK", PlayerRole.SEAMER, "INTL Star", 38, 91, 80, 150L, "Skiddy Express Seam"),
        p("i011", "Haris Rauf", "Pakistan", "PAK", PlayerRole.SEAMER, "T20 Ace", 24, 89, 80, 150L, "150 kph Death Pace"),
        p("i012", "Abrar Ahmed", "Pakistan", "PAK", PlayerRole.SPINNER, "INTL Star", 22, 87, 76, 100L, "Mystery Carrom Spinner"),

        // Australia additions
        p("i013", "Steve Smith", "Australia", "AUS", PlayerRole.BATTER, "INTL Icon", 95, 36, 92, 200L, "Modern Test Genius"),
        p("i014", "Marnus Labuschagne", "Australia", "AUS", PlayerRole.BATTER, "Test Ace", 89, 44, 90, 150L, "Relentless #3 Anchor"),
        p("i015", "Alex Carey", "Australia", "AUS", PlayerRole.WICKETKEEPER, "INTL Star", 84, 12, 92, 100L, "Clutch Left-Hand WK"),
        p("i016", "Nathan Lyon", "Australia", "AUS", PlayerRole.SPINNER, "Test Legend", 38, 92, 84, 150L, "GOAT Off-Spinner"),

        // England additions
        p("i017", "Joe Root", "England", "ENG", PlayerRole.BATTER, "INTL Icon", 96, 62, 91, 200L, "Fab-4 Run Machine"),
        p("i018", "Ben Duckett", "England", "ENG", PlayerRole.BATTER, "INTL Star", 88, 15, 84, 100L, "Bazball Sweep Master"),
        p("i019", "Mark Wood", "England", "ENG", PlayerRole.SEAMER, "INTL Star", 36, 91, 84, 150L, "155 kph Rocket Pace"),
        p("i020", "Chris Woakes", "England", "ENG", PlayerRole.ALL_ROUNDER, "INTL Star", 72, 89, 85, 150L, "Swing & Control AR"),
        p("i021", "Gus Atkinson", "England", "ENG", PlayerRole.SEAMER, "Emerging", 48, 88, 82, 100L, "Steep Bounce Seamer"),

        // South Africa additions
        p("i022", "Temba Bavuma", "South Africa", "RSA", PlayerRole.BATTER, "INTL Captain", 86, 20, 92, 100L, "Gritty Proteas Captain"),
        p("i023", "Marco Jansen", "South Africa", "RSA", PlayerRole.ALL_ROUNDER, "INTL Star", 74, 90, 86, 150L, "6'8 Left-Arm Bounce AR"),
        p("i024", "Anrich Nortje", "South Africa", "RSA", PlayerRole.SEAMER, "T20 Ace", 22, 90, 81, 150L, "Express Thunderbolts"),
        p("i025", "Keshav Maharaj", "South Africa", "RSA", PlayerRole.SPINNER, "INTL Star", 54, 90, 82, 150L, "Classical Left-Arm Spin"),
        p("i026", "Tabraiz Shamsi", "South Africa", "RSA", PlayerRole.SPINNER, "T20 Ace", 20, 88, 78, 100L, "Left-Arm Wrist Spin"),

        // New Zealand additions
        p("i027", "Devon Conway", "New Zealand", "NZ", PlayerRole.WICKETKEEPER, "INTL Star", 89, 15, 87, 150L, "Classical Left-Hand WK"),
        p("i028", "Rachin Ravindra", "New Zealand", "NZ", PlayerRole.ALL_ROUNDER, "INTL Star", 90, 74, 88, 150L, "World Cup Prodigy AR"),
        p("i029", "Daryl Mitchell", "New Zealand", "NZ", PlayerRole.ALL_ROUNDER, "INTL Star", 89, 68, 87, 150L, "Big-Match #4 Power"),
        p("i030", "Glenn Phillips", "New Zealand", "NZ", PlayerRole.ALL_ROUNDER, "T20 Ace", 87, 72, 97, 150L, "Superman Fielder AR"),
        p("i031", "Tom Latham", "New Zealand", "NZ", PlayerRole.WICKETKEEPER, "Test Ace", 85, 12, 88, 100L, "Composed Opener WK"),
        p("i032", "Matt Henry", "New Zealand", "NZ", PlayerRole.SEAMER, "INTL Star", 34, 91, 82, 150L, "Relentless Seam Length"),
        p("i033", "Tim Southee", "New Zealand", "NZ", PlayerRole.SEAMER, "INTL Legend", 50, 88, 84, 150L, "Outswing Veteran"),
        p("i034", "Will O'Rourke", "New Zealand", "NZ", PlayerRole.SEAMER, "Emerging", 24, 87, 80, 100L, "Tall Steep Bounce"),

        // West Indies additions
        p("i035", "Shai Hope", "West Indies", "WI", PlayerRole.WICKETKEEPER, "INTL Captain", 89, 12, 88, 150L, "ODI Anchor Captain"),
        p("i036", "Rovman Powell", "West Indies", "WI", PlayerRole.BATTER, "T20 Captain", 86, 48, 89, 100L, "Brutal Six-Hitter"),
        p("i037", "Jason Holder", "West Indies", "WI", PlayerRole.ALL_ROUNDER, "INTL Star", 76, 86, 86, 150L, "Tall Seam All-Rounder"),
        p("i038", "Alzarri Joseph", "West Indies", "WI", PlayerRole.SEAMER, "INTL Star", 38, 88, 82, 150L, "148 kph Hit-the-Deck"),
        p("i039", "Shamar Joseph", "West Indies", "WI", PlayerRole.SEAMER, "Emerging", 28, 89, 83, 150L, "Gabba Hero Express"),
        p("i040", "Gudakesh Motie", "West Indies", "WI", PlayerRole.SPINNER, "INTL Star", 42, 88, 81, 100L, "Sharp Left-Arm Spin"),

        // Sri Lanka additions
        p("i041", "Pathum Nissanka", "Sri Lanka", "SL", PlayerRole.BATTER, "INTL Star", 89, 16, 86, 150L, "Double-Ton Opener"),
        p("i042", "Kusal Mendis", "Sri Lanka", "SL", PlayerRole.WICKETKEEPER, "INTL Star", 87, 15, 87, 150L, "Aggressive Top-Order WK"),
        p("i043", "Charith Asalanka", "Sri Lanka", "SL", PlayerRole.BATTER, "INTL Captain", 86, 62, 86, 100L, "Clutch Left-Hand Leader"),
        p("i044", "Kamindu Mendis", "Sri Lanka", "SL", PlayerRole.ALL_ROUNDER, "Test Ace", 88, 72, 87, 150L, "Ambidextrous Spin AR"),
        p("i045", "Angelo Mathews", "Sri Lanka", "SL", PlayerRole.ALL_ROUNDER, "INTL Legend", 84, 78, 83, 150L, "Veteran Match-Winner"),
        p("i046", "Dhananjaya de Silva", "Sri Lanka", "SL", PlayerRole.ALL_ROUNDER, "Test Captain", 84, 76, 86, 100L, "Composed Red-Ball AR"),
        p("i047", "Dushmantha Chameera", "Sri Lanka", "SL", PlayerRole.SEAMER, "INTL Star", 24, 87, 80, 100L, "Skiddy Express Pace"),
        p("i048", "Asitha Fernando", "Sri Lanka", "SL", PlayerRole.SEAMER, "Test Ace", 20, 86, 78, 100L, "Workhorse Test Seamer"),
        p("i049", "Dunith Wellalage", "Sri Lanka", "SL", PlayerRole.ALL_ROUNDER, "Emerging", 76, 86, 85, 100L, "Left-Arm Spin Prodigy")
    )

    @Volatile
    private var hallOfFameLegends: List<Cricketer> = emptyList()

    @Volatile
    private var activeCareerPlayers: Map<String, Cricketer> = emptyMap()

    val allPlayers: List<Cricketer>
        get() = if (hallOfFameLegends.isEmpty()) basePlayers else basePlayers + hallOfFameLegends

    private val baseByIdMap: Map<String, Cricketer> = (basePlayers + internationalExtras).associateBy { it.id }

    fun registerHallOfFameLegends(legends: List<Cricketer>) {
        hallOfFameLegends = legends.distinctBy { it.id }
    }

    fun registerActiveCareerPlayer(player: Cricketer?) {
        activeCareerPlayers = if (player == null) emptyMap() else mapOf(player.id to player)
    }

    fun getById(id: String): Cricketer? =
        activeCareerPlayers[id]
            ?: hallOfFameLegends.firstOrNull { it.id == id }
            ?: baseByIdMap[id]

    /**
     * Top Marquee Icons + Hall of Fame Legends eligible for the Pre-Auction Marquee/Retention Round.
     */
    fun marqueeRetentionPool(): List<Cricketer> =
        allPlayers.filter { it.basePriceLakhs >= 200L }
            .sortedByDescending { it.overallRating }

    /**
     * Generates a dramatic auction order excluding retained players:
     * Marquee Stars & Hall of Fame Legends first, followed by Tier 2 and Value picks.
     */
    fun generateAuctionOrder(excludedIds: Set<String> = emptySet()): List<String> {
        val available = allPlayers.filter { it.id !in excludedIds }
        val marquee = available.filter { it.basePriceLakhs >= 200L }.shuffled()
        val tierTwo = available.filter { it.basePriceLakhs == 150L }.shuffled()
        val tierThree = available.filter { it.basePriceLakhs < 150L }.shuffled()
        return (marquee + tierTwo + tierThree).map { it.id }
    }
}
