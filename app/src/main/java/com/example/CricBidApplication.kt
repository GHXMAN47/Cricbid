package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val STARTUP_TAG = "CricBidStartup"

/**
 * Crash-safe Firebase bootstrap helper.
 * Ensures that standalone/CI builds without `google-services.json` (or with placeholder values)
 * NEVER throw `IllegalStateException: Default FirebaseApp is not initialized` and instead fall
 * back cleanly to local/offline Guest mode.
 */
object FirebaseSafeInit {

    @Volatile
    private var checked: Boolean = false

    @Volatile
    private var available: Boolean = false

    val isAvailable: Boolean
        get() = available

    fun ensureInitialized(context: Context): Boolean {
        if (checked) return available
        synchronized(this) {
            if (checked) return available
            available = try {
                val appCtx = context.applicationContext ?: context
                val existing = FirebaseApp.getApps(appCtx)
                if (existing.isNotEmpty()) {
                    val defaultApp = runCatching { FirebaseApp.getInstance() }.getOrNull() ?: existing.firstOrNull()
                    val opts = runCatching { defaultApp?.options }.getOrNull()
                    val appId = opts?.applicationId.orEmpty()
                    val projectId = opts?.projectId.orEmpty()
                    val isPlaceholder = appId.isBlank() ||
                        appId.contains("placeholder", ignoreCase = true) ||
                        projectId.contains("placeholder", ignoreCase = true) ||
                        projectId.contains("remixed-project-id", ignoreCase = true)
                    if (isPlaceholder) {
                        Log.w(STARTUP_TAG, "FirebaseApp has placeholder options; using local/offline Guest mode.")
                        false
                    } else {
                        true
                    }
                } else {
                    val googleAppIdRes = appCtx.resources.getIdentifier(
                        "google_app_id",
                        "string",
                        appCtx.packageName
                    )
                    val googleAppId = if (googleAppIdRes != 0) {
                        runCatching { appCtx.getString(googleAppIdRes).trim() }.getOrNull()
                    } else {
                        null
                    }
                    if (googleAppId.isNullOrBlank() || googleAppId.contains("placeholder", ignoreCase = true)) {
                        Log.w(STARTUP_TAG, "Firebase config missing or placeholder; running in local/offline Guest mode.")
                        false
                    } else {
                        val initialized = FirebaseApp.initializeApp(appCtx)
                        initialized != null || FirebaseApp.getApps(appCtx).isNotEmpty()
                    }
                }
            } catch (t: Throwable) {
                Log.w(STARTUP_TAG, "Firebase initialization skipped safely; falling back to local Guest mode: ${t.message}")
                false
            }
            checked = true
            return available
        }
    }

    fun safeFirebaseAuth(context: Context): FirebaseAuth? {
        return try {
            if (!ensureInitialized(context)) return null
            Firebase.auth
        } catch (t: Throwable) {
            Log.w(STARTUP_TAG, "FirebaseAuth unavailable; using Guest mode: ${t.message}")
            null
        }
    }

    fun safeFirestore(context: Context): FirebaseFirestore? {
        return try {
            if (!ensureInitialized(context)) return null
            val appCtx = context.applicationContext ?: context
            val dbIdRes = appCtx.resources.getIdentifier(
                "firestore_database_id",
                "string",
                appCtx.packageName
            )
            val databaseId = if (dbIdRes != 0) {
                runCatching { appCtx.getString(dbIdRes).trim() }.getOrNull().orEmpty()
            } else {
                ""
            }
            if (databaseId.isNotBlank() && databaseId != "(default)") {
                FirebaseFirestore.getInstance(databaseId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (t: Throwable) {
            Log.w(STARTUP_TAG, "FirebaseFirestore unavailable; using local auction store: ${t.message}")
            null
        }
    }
}

class CricBidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseSafeInit.ensureInitialized(this)
        } catch (t: Throwable) {
            Log.w(STARTUP_TAG, "Application startup Firebase init caught safely: ${t.message}")
        }
    }
}
