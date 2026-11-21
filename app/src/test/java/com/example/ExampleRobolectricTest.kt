package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.safeAuthStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CricBid", appName)
    }

    @Test
    fun `startup Firebase safe init and MainActivity launch never crash`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Must not throw even when Firebase is unavailable or unconfigured in JVM/CI environment
        FirebaseSafeInit.ensureInitialized(context)
        FirebaseSafeInit.safeFirebaseAuth(context)
        FirebaseSafeInit.safeFirestore(context)

        val nullAuthUser = runBlocking { safeAuthStateFlow(null).first() }
        assertNull(nullAuthUser)

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }

    @Test
    fun `fresh install sign in never fails with not found and creates profile immediately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("cricbid_local_profile", Context.MODE_PRIVATE).edit().clear().commit()

        val store = com.example.data.local.LocalProfileStore(context)
        // Fresh install: entering a brand-new username + password on Sign In must succeed immediately
        val result = store.signInWithPassword("FreshCaptain", "pass1234")
        org.junit.Assert.assertTrue("Fresh install Sign In must not fail with not found: ${result.exceptionOrNull()}", result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals("FreshCaptain", profile.username)
        org.junit.Assert.assertTrue(profile.hasPassword)

        // Wrong password on the now-existing local account must still be rejected cleanly
        val wrongPass = store.signInWithPassword("FreshCaptain", "wrong999")
        org.junit.Assert.assertTrue("Wrong password on existing account should fail", wrongPass.isFailure)

        // Correct password signs right back in
        val okAgain = store.signInWithPassword("FreshCaptain", "pass1234")
        org.junit.Assert.assertTrue(okAgain.isSuccess)
        assertEquals(profile.accountId, okAgain.getOrThrow().accountId)
    }

    @Test
    fun `full backup code and JSON export restore account career hall of fame and trophies`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("cricbid_local_profile", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_career_vault", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_trophy_cabinet_v1", Context.MODE_PRIVATE).edit().clear().commit()

        val profileStore = com.example.data.local.LocalProfileStore(context)
        val careerStore = com.example.data.local.LocalCareerStore(context)
        val trophyStore = com.example.data.local.LocalTrophyCabinetStore(context)
        val backupManager = com.example.data.local.LocalSaveBackupManager(profileStore, careerStore, trophyStore)

        // 1. Seed Account + Career + Hall of Fame + Trophy Cabinet
        var profile = profileStore.registerAccount("LegendOwner", "secret42", "virat").getOrThrow()
        profile = profileStore.recordMatchOutcome(won = true, wonTrophy = true)!!
        val rookie = com.example.engine.CareerEngine.createRookie(
            name = "Aarav Verma",
            startingAge = 20,
            nationality = "India",
            countryCode = "IND",
            avatarId = "rohit",
            role = com.example.data.model.PlayerRole.BATTER,
            battingStyle = com.example.data.model.BattingStyle.RIGHT_HAND_BAT,
            bowlingStyle = com.example.data.model.BowlingStyle.OCCASIONAL_MEDIUM
        ).copy(level = 5, totalRuns = 1420, matchesPlayed = 18)
        careerStore.saveActiveCareer(rookie)

        val (_, legend) = com.example.engine.CareerEngine.retireToHallOfFame(rookie.copy(id = "hof_1", name = "Old Legend"))
        careerStore.saveHallOfFame(listOf(legend))

        trophyStore.recordFinalOutcome(
            com.example.engine.TrophyCabinetRecord(
                id = "t_1",
                title = "IPL Final",
                competitionType = "IPL",
                championTeamName = "Mumbai Titans",
                runnerUpTeamName = "Chennai Kings",
                userWon = true,
                summary = "Won by 14 runs"
            )
        )

        // 2. Export both CB2-... Backup Code and .JSON string
        val cb2Code = backupManager.exportFullBackupCode(profile)
        val jsonBackup = backupManager.exportFullBackupJson(profile)
        org.junit.Assert.assertTrue("Exported code must start with CB2-", cb2Code.startsWith("CB2-"))
        org.junit.Assert.assertTrue("Exported JSON must start with {", jsonBackup.trim().startsWith("{"))

        // 3. Simulate uninstall/reinstall by wiping all local stores
        context.getSharedPreferences("cricbid_local_profile", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_career_vault", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_trophy_cabinet_v1", Context.MODE_PRIVATE).edit().clear().commit()
        assertNull(profileStore.loadActiveProfile())
        assertNull(careerStore.loadActiveCareer())
        assertEquals(0, trophyStore.loadCabinet().trophies.size)

        // 4. Restore from CB2-... code and verify every store is restored
        val restoredFromCode = backupManager.importBackup(cb2Code).getOrThrow()
        assertEquals("LegendOwner", restoredFromCode.profile.username)
        assertEquals(1, restoredFromCode.profile.matchesWon)
        assertEquals("Aarav Verma", restoredFromCode.careerPlayerName)
        assertEquals(5, restoredFromCode.careerLevel)
        assertEquals(1, restoredFromCode.hallOfFameCount)
        assertEquals(1, restoredFromCode.trophiesCount)
        assertEquals(1420, careerStore.loadActiveCareer()?.totalRuns)

        // 5. Wipe again & restore from raw .JSON file payload
        context.getSharedPreferences("cricbid_local_profile", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_career_vault", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("cricbid_trophy_cabinet_v1", Context.MODE_PRIVATE).edit().clear().commit()

        val restoredFromJson = backupManager.importBackup(jsonBackup).getOrThrow()
        assertEquals("LegendOwner", restoredFromJson.profile.username)
        assertEquals("Aarav Verma", restoredFromJson.careerPlayerName)
        assertEquals(1, restoredFromJson.trophiesCount)
        assertEquals("Mumbai Titans", trophyStore.loadCabinet().trophies.first().championTeamName)
    }
}

