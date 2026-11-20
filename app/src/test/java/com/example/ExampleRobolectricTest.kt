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
}
