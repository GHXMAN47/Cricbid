package com.example.data.local

import android.content.Context
import android.util.Base64
import com.example.ui.components.IndianPlayerAvatars
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

data class AccountProfile(
    val accountId: String,
    val username: String,
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val avatarId: String = IndianPlayerAvatars.DEFAULT_ID,
    val favoriteTeamName: String = "Royal Strikers",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val trophiesWon: Int = 0,
    val isGuest: Boolean = true
) {
    val hasPassword: Boolean
        get() = passwordHash.isNotBlank() && passwordSalt.isNotBlank() && !isGuest

    val winRatePercent: Int
        get() = if (matchesPlayed <= 0) 0 else ((matchesWon * 100.0) / matchesPlayed).toInt()
}

/**
 * Reinstall-safe local Account & Profile Vault:
 * - Persists Username + salted SHA-256 Password accounts, Indian player avatar choice, favorite franchise, and career stats.
 * - Explicitly backed up via Android Auto Backup & Device Transfer (backup_rules.xml & data_extraction_rules.xml).
 * - Supports portable Account Backup Codes (CB1-...) for instant cross-device/reinstall recovery.
 * - Also preserves zero-friction Guest / Instant Play mode!
 */
class LocalProfileStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadActiveProfile(): AccountProfile? {
        val activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null)
        val accounts = loadAllAccounts()
        if (!activeId.isNullOrBlank()) {
            accounts.firstOrNull { it.accountId == activeId }?.let { return it }
        }
        // Backward compatibility with previous username-only installs
        val legacyUsername = prefs.getString(KEY_USERNAME, "")?.trim().orEmpty()
        if (legacyUsername.isNotBlank()) {
            val uid = getOrCreateLocalUserId()
            val migrated = AccountProfile(
                accountId = uid,
                username = legacyUsername,
                avatarId = IndianPlayerAvatars.DEFAULT_ID,
                favoriteTeamName = "Royal Strikers",
                isGuest = true
            )
            saveAccountToVault(migrated, setActive = true)
            return migrated
        }
        return null
    }

    fun loadUsername(): String = loadActiveProfile()?.username ?: prefs.getString(KEY_USERNAME, "")?.trim().orEmpty()

    fun saveUsername(username: String): String {
        val cleaned = username.trim().take(28)
        prefs.edit().putString(KEY_USERNAME, cleaned).apply()
        val active = loadActiveProfile()
        if (active != null) {
            saveAccountToVault(active.copy(username = cleaned), setActive = true)
        }
        return cleaned
    }

    fun getOrCreateLocalUserId(): String {
        val active = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null)
        if (!active.isNullOrBlank()) return active
        val existing = prefs.getString(KEY_LOCAL_UID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "local_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        prefs.edit().putString(KEY_LOCAL_UID, generated).apply()
        return generated
    }

    fun listSavedUsernames(): List<String> =
        loadAllAccounts().filter { it.hasPassword }.map { it.username }

    fun continueAsGuest(username: String, avatarId: String = IndianPlayerAvatars.DEFAULT_ID): AccountProfile {
        val cleaned = username.trim().take(28).ifBlank { "Manager" }
        val norm = normalizeUsername(cleaned)
        val existingActive = loadActiveProfile()
        val existingMatching = loadAllAccounts().firstOrNull { normalizeUsername(it.username) == norm }
        val profile = when {
            existingMatching != null && existingMatching.isGuest -> existingMatching.copy(
                username = cleaned,
                avatarId = avatarId.ifBlank { existingMatching.avatarId }
            )
            existingActive != null && existingActive.isGuest -> existingActive.copy(
                username = cleaned,
                avatarId = avatarId.ifBlank { existingActive.avatarId }
            )
            else -> AccountProfile(
                accountId = "guest_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                username = cleaned,
                avatarId = IndianPlayerAvatars.byId(avatarId).id,
                favoriteTeamName = "Royal Strikers",
                isGuest = true
            )
        }
        saveAccountToVault(profile, setActive = true)
        return profile
    }

    fun registerAccount(
        username: String,
        password: String,
        avatarId: String = IndianPlayerAvatars.DEFAULT_ID,
        favoriteTeamName: String = "Royal Strikers"
    ): Result<AccountProfile> {
        val cleanedUser = username.trim().take(28)
        if (cleanedUser.length < 2) {
            return Result.failure(IllegalArgumentException("Username must be at least 2 characters"))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }
        val accounts = loadAllAccounts()
        val norm = normalizeUsername(cleanedUser)
        val existing = accounts.firstOrNull { it.hasPassword && normalizeUsername(it.username) == norm }
        if (existing != null) {
            // If the user re-enters the exact same username & password, sign them in cleanly instead of blocking
            val candidateHash = hashPassword(password, existing.passwordSalt)
            return if (constantTimeEquals(candidateHash, existing.passwordHash)) {
                saveAccountToVault(existing, setActive = true)
                Result.success(existing)
            } else {
                Result.failure(IllegalStateException("Username '$cleanedUser' already exists on this device — use Sign In with your password"))
            }
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val deterministicId = "acct_${sha256Hex(norm).take(14)}"
        val created = AccountProfile(
            accountId = deterministicId,
            username = cleanedUser,
            passwordHash = hash,
            passwordSalt = salt,
            avatarId = IndianPlayerAvatars.byId(avatarId).id,
            favoriteTeamName = favoriteTeamName.trim().take(32).ifBlank { "Royal Strikers" },
            matchesPlayed = 0,
            matchesWon = 0,
            trophiesWon = 0,
            isGuest = false
        )
        saveAccountToVault(created, setActive = true)
        return Result.success(created)
    }

    /**
     * Signs into an existing local account OR cleanly creates it on a fresh install
     * so entering a username never gets stuck with a "not found" error after reinstalling.
     */
    fun signInWithPassword(
        username: String,
        password: String,
        avatarId: String = IndianPlayerAvatars.DEFAULT_ID
    ): Result<AccountProfile> {
        val cleanedUser = username.trim().take(28)
        if (cleanedUser.length < 2) {
            return Result.failure(IllegalArgumentException("Username must be at least 2 characters"))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
        }
        val norm = normalizeUsername(cleanedUser)
        val allAccounts = loadAllAccounts()
        val existingProtected = allAccounts.firstOrNull { it.hasPassword && normalizeUsername(it.username) == norm }
        if (existingProtected != null) {
            val candidateHash = hashPassword(password, existingProtected.passwordSalt)
            if (!constantTimeEquals(candidateHash, existingProtected.passwordHash)) {
                return Result.failure(IllegalArgumentException("Incorrect password for '$cleanedUser'"))
            }
            saveAccountToVault(existingProtected, setActive = true)
            return Result.success(existingProtected)
        }

        // If a guest profile with this username exists locally, upgrade & preserve its stats
        val existingGuest = allAccounts.firstOrNull { normalizeUsername(it.username) == norm }
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val freshOrUpgraded = if (existingGuest != null) {
            existingGuest.copy(
                username = cleanedUser,
                passwordHash = hash,
                passwordSalt = salt,
                isGuest = false
            )
        } else {
            AccountProfile(
                accountId = "acct_${sha256Hex(norm).take(14)}",
                username = cleanedUser,
                passwordHash = hash,
                passwordSalt = salt,
                avatarId = IndianPlayerAvatars.byId(avatarId).id,
                favoriteTeamName = "Royal Strikers",
                matchesPlayed = 0,
                matchesWon = 0,
                trophiesWon = 0,
                isGuest = false
            )
        }
        saveAccountToVault(freshOrUpgraded, setActive = true)
        return Result.success(freshOrUpgraded)
    }

    fun updateActiveProfile(
        newUsername: String,
        newAvatarId: String,
        newFavoriteTeam: String,
        newPassword: String? = null
    ): Result<AccountProfile> {
        val current = loadActiveProfile() ?: continueAsGuest(newUsername, newAvatarId)
        val cleanedUser = newUsername.trim().take(28)
        if (cleanedUser.length < 2) {
            return Result.failure(IllegalArgumentException("Username must be at least 2 characters"))
        }
        val norm = normalizeUsername(cleanedUser)
        val accounts = loadAllAccounts()
        val conflict = accounts.any {
            it.accountId != current.accountId && it.hasPassword && normalizeUsername(it.username) == norm
        }
        if (conflict) {
            return Result.failure(IllegalStateException("Username '$cleanedUser' is already taken by another account"))
        }

        val trimmedPwd = newPassword?.trim().orEmpty()
        val updated = if (trimmedPwd.isNotEmpty()) {
            if (trimmedPwd.length < 4) {
                return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))
            }
            val salt = generateSalt()
            val hash = hashPassword(trimmedPwd, salt)
            current.copy(
                username = cleanedUser,
                passwordHash = hash,
                passwordSalt = salt,
                avatarId = IndianPlayerAvatars.byId(newAvatarId).id,
                favoriteTeamName = newFavoriteTeam.trim().take(32).ifBlank { "Royal Strikers" },
                isGuest = false
            )
        } else {
            current.copy(
                username = cleanedUser,
                avatarId = IndianPlayerAvatars.byId(newAvatarId).id,
                favoriteTeamName = newFavoriteTeam.trim().take(32).ifBlank { "Royal Strikers" }
            )
        }

        saveAccountToVault(updated, setActive = true)
        return Result.success(updated)
    }

    fun recordMatchOutcome(won: Boolean, wonTrophy: Boolean = false): AccountProfile? {
        val current = loadActiveProfile() ?: return null
        val updated = current.copy(
            matchesPlayed = current.matchesPlayed + 1,
            matchesWon = current.matchesWon + if (won) 1 else 0,
            trophiesWon = current.trophiesWon + if (wonTrophy) 1 else 0
        )
        saveAccountToVault(updated, setActive = true)
        return updated
    }

    fun exportProfileJson(profile: AccountProfile): JSONObject = serializeAccount(profile)

    fun importProfileJson(
        obj: JSONObject,
        passwordForVerification: String = ""
    ): Result<AccountProfile> = runCatching {
        val restored = parseAccount(obj)
            ?: throw IllegalArgumentException("Invalid account data in backup")
        if (restored.hasPassword && passwordForVerification.isNotBlank()) {
            val candidateHash = hashPassword(passwordForVerification, restored.passwordSalt)
            if (!constantTimeEquals(candidateHash, restored.passwordHash)) {
                throw IllegalArgumentException("Password does not match this backup")
            }
        }
        saveAccountToVault(restored, setActive = true)
        restored
    }

    /**
     * Generates a compact, portable Account Backup Code (CB1-...) for backward compatibility.
     */
    fun exportBackupCode(profile: AccountProfile): String {
        val obj = serializeAccount(profile)
        val encoded = Base64.encodeToString(
            obj.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "CB1-$encoded"
    }

    fun importBackupCode(rawCode: String, passwordForVerification: String = ""): Result<AccountProfile> {
        val cleaned = rawCode.trim()
        if (!cleaned.startsWith("CB1-")) {
            return Result.failure(IllegalArgumentException("Backup code must start with CB1- or CB2-"))
        }
        return runCatching {
            val payload = cleaned.removePrefix("CB1-")
            val jsonBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val obj = JSONObject(String(jsonBytes, Charsets.UTF_8))
            importProfileJson(obj, passwordForVerification).getOrThrow()
        }
    }

    fun signOutActiveSession() {
        prefs.edit().remove(KEY_ACTIVE_ACCOUNT_ID).apply()
    }

    fun loadRecentRoomCodes(): List<String> {
        val raw = prefs.getString(KEY_RECENT_ROOMS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val code = arr.optString(i).trim()
                    if (code.matches(Regex("^[0-9]{4}$"))) add(code)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun rememberRoomCode(roomCode: String) {
        val cleaned = roomCode.trim()
        if (!cleaned.matches(Regex("^[0-9]{4}$"))) return
        val updated = (listOf(cleaned) + loadRecentRoomCodes().filterNot { it == cleaned }).take(6)
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENT_ROOMS, arr.toString()).apply()
    }

    private fun loadAllAccounts(): List<AccountProfile> {
        val raw = prefs.getString(KEY_ACCOUNTS_VAULT, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    parseAccount(arr.optJSONObject(i))?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAccountToVault(profile: AccountProfile, setActive: Boolean) {
        val accounts = loadAllAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.accountId == profile.accountId }
        if (idx >= 0) {
            accounts[idx] = profile
        } else {
            accounts.add(profile)
        }
        val arr = JSONArray()
        accounts.takeLast(12).forEach { arr.put(serializeAccount(it)) }
        val editor = prefs.edit()
            .putString(KEY_ACCOUNTS_VAULT, arr.toString())
            .putString(KEY_USERNAME, profile.username)
        if (setActive) {
            editor.putString(KEY_ACTIVE_ACCOUNT_ID, profile.accountId)
            editor.putString(KEY_LOCAL_UID, profile.accountId)
        }
        editor.apply()
    }

    private fun serializeAccount(p: AccountProfile): JSONObject = JSONObject().apply {
        put("accountId", p.accountId)
        put("username", p.username)
        put("passwordHash", p.passwordHash)
        put("passwordSalt", p.passwordSalt)
        put("avatarId", p.avatarId)
        put("favoriteTeamName", p.favoriteTeamName)
        put("matchesPlayed", p.matchesPlayed)
        put("matchesWon", p.matchesWon)
        put("trophiesWon", p.trophiesWon)
        put("isGuest", p.isGuest)
    }

    private fun parseAccount(o: JSONObject?): AccountProfile? {
        if (o == null) return null
        val id = o.optString("accountId").trim()
        val user = o.optString("username").trim()
        if (id.isBlank() || user.isBlank()) return null
        return AccountProfile(
            accountId = id,
            username = user,
            passwordHash = o.optString("passwordHash"),
            passwordSalt = o.optString("passwordSalt"),
            avatarId = o.optString("avatarId", IndianPlayerAvatars.DEFAULT_ID),
            favoriteTeamName = o.optString("favoriteTeamName", "Royal Strikers"),
            matchesPlayed = o.optInt("matchesPlayed", 0).coerceAtLeast(0),
            matchesWon = o.optInt("matchesWon", 0).coerceAtLeast(0),
            trophiesWon = o.optInt("trophiesWon", 0).coerceAtLeast(0),
            isGuest = o.optBoolean("isGuest", true)
        )
    }

    private fun normalizeUsername(raw: String): String = raw.trim().lowercase(Locale.US)

    private fun generateSalt(): String {
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String =
        sha256Hex("cricbid_v1::$salt::$password")

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    private companion object {
        const val PREFS_NAME = "cricbid_local_profile"
        const val KEY_USERNAME = "saved_username"
        const val KEY_LOCAL_UID = "local_manager_uid"
        const val KEY_ACTIVE_ACCOUNT_ID = "active_account_id"
        const val KEY_ACCOUNTS_VAULT = "accounts_vault_v1"
        const val KEY_RECENT_ROOMS = "recent_room_codes_v1"
    }
}
