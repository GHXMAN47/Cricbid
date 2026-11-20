package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.FirebaseSafeInit
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

/**
 * Safely resolves the Google Services generated `default_web_client_id` at runtime so
 * Kotlin compilation and runtime startup succeed even in builds without `google-services.json`,
 * while still enabling full Google Sign-In whenever Firebase config is bundled.
 */
fun resolveDefaultWebClientId(context: Context): String? {
    return try {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) {
            null
        } else {
            context.getString(resId)
                .trim()
                .takeIf { it.isNotEmpty() && !it.startsWith("placeholder", ignoreCase = true) }
        }
    } catch (_: Throwable) {
        null
    }
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    try {
        addAuthStateListener(listener)
    } catch (t: Throwable) {
        Log.w(AUTH_TAG, "Failed to attach FirebaseAuth listener: ${t.message}")
        trySend(null)
    }
    awaitClose {
        runCatching { removeAuthStateListener(listener) }
    }
}

/**
 * Crash-safe wrapper around [FirebaseAuth.authStateFlow] that immediately emits `null`
 * when Firebase is unavailable (offline/local Guest mode).
 */
fun safeAuthStateFlow(auth: FirebaseAuth?): Flow<FirebaseUser?> {
    if (auth == null) return flowOf(null)
    return try {
        auth.authStateFlow()
    } catch (t: Throwable) {
        Log.w(AUTH_TAG, "Falling back to offline Guest auth flow: ${t.message}")
        flowOf(null)
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = FirebaseSafeInit.safeFirebaseAuth(context)
    if (auth == null || credentialManager == null) {
        onUnauthenticated()
        return
    }
    val existingUser = runCatching { auth.currentUser }.getOrNull()
    if (existingUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onUnauthenticated()
        return
    }
    val request = try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    } catch (_: Throwable) {
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Throwable) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val auth = FirebaseSafeInit.safeFirebaseAuth(context)
    if (auth == null || credentialManager == null) {
        onAuthError("Cloud sync is unavailable in this build. Enter any username to play immediately as a Guest.")
        return
    }
    val clientId = resolveDefaultWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In is unavailable in this build. You can play immediately as a Guest.")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Unable to launch sign-in from current context")
        return
    }
    val request = try {
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
    } catch (t: Throwable) {
        onAuthError(t.localizedMessage ?: "Google Sign-In is unavailable. Continue as Guest.")
        return
    }

    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            Log.e(AUTH_TAG, "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "Sign in failed. Continue playing as Guest.")
        }
    }
}

fun signOutFromGoogle(
    context: Context,
    credentialManager: CredentialManager?,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (t: Throwable) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${t.message}")
            } finally {
                runCatching { FirebaseSafeInit.safeFirebaseAuth(context)?.signOut() }
                onSignOutComplete()
            }
        }
    }
}
