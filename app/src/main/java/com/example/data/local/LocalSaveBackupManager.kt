package com.example.data.local

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class RestoredBackupSummary(
    val profile: AccountProfile,
    val careerPlayerName: String? = null,
    val careerLevel: Int? = null,
    val hallOfFameCount: Int = 0,
    val trophiesCount: Int = 0
) {
    fun toUserMessage(): String {
        val parts = mutableListOf("Account '${profile.username}'")
        if (!careerPlayerName.isNullOrBlank()) {
            val lvText = careerLevel?.let { " (Lv.$it)" }.orEmpty()
            parts.add("Career: $careerPlayerName$lvText")
        }
        if (hallOfFameCount > 0) parts.add("$hallOfFameCount HOF")
        if (trophiesCount > 0) parts.add("$trophiesCount Trophies")
        return "Restored ${parts.joinToString(" • ")}!"
    }
}

/**
 * Full Save Export & Import Manager for CricBid:
 * - Bundles Account Profile + Stats + Player Career Mode + Hall of Fame + Trophy Cabinet
 *   into either a compact copyable text Backup Code (`CB2-...`, GZIP + Base64) OR a readable `.json` file.
 * - Backward-compatible with legacy `CB1-...` account-only codes.
 * - Crash-safe with strict size bounds and try-catch validation.
 */
class LocalSaveBackupManager(
    private val profileStore: LocalProfileStore,
    private val careerStore: LocalCareerStore,
    private val trophyCabinetStore: LocalTrophyCabinetStore
) {
    constructor(context: Context) : this(
        profileStore = LocalProfileStore(context),
        careerStore = LocalCareerStore(context),
        trophyCabinetStore = LocalTrophyCabinetStore(context)
    )

    fun buildBackupSummaryLine(profile: AccountProfile): String {
        val career = careerStore.loadActiveCareer()
        val hof = careerStore.loadHallOfFame()
        val cabinet = trophyCabinetStore.loadCabinet()
        val items = mutableListOf("Account (${profile.matchesWon}W/${profile.matchesPlayed}M)")
        if (career != null) {
            items.add("Career: ${career.name} Lv.${career.level}")
        }
        if (hof.isNotEmpty()) {
            items.add("${hof.size} HOF")
        }
        if (cabinet.trophies.isNotEmpty()) {
            items.add("${cabinet.trophies.size} Trophies")
        }
        return items.joinToString(" • ")
    }

    fun exportFullBackupJson(profile: AccountProfile): String {
        val root = JSONObject().apply {
            put("app", "CricBid")
            put("version", 2)
            put("exportedAtMs", System.currentTimeMillis())
            put("profile", profileStore.exportProfileJson(profile))
            careerStore.exportActiveCareerJson()?.let { put("activeCareer", it) }
            put("hallOfFame", careerStore.exportHallOfFameJson())
            put("trophyCabinet", trophyCabinetStore.exportCabinetJson())
        }
        return root.toString(2)
    }

    fun exportFullBackupCode(profile: AccountProfile): String {
        val compactJson = JSONObject().apply {
            put("app", "CricBid")
            put("version", 2)
            put("exportedAtMs", System.currentTimeMillis())
            put("profile", profileStore.exportProfileJson(profile))
            careerStore.exportActiveCareerJson()?.let { put("activeCareer", it) }
            put("hallOfFame", careerStore.exportHallOfFameJson())
            put("trophyCabinet", trophyCabinetStore.exportCabinetJson())
        }.toString()

        val compressed = gzipCompress(compactJson.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(
            compressed,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        return "$PREFIX_V2$encoded"
    }

    /**
     * Imports any valid CricBid backup:
     * - `CB2-...` full compressed code (Account + Career + HOF + Trophies)
     * - Raw JSON `{ ... }` (from exported `.json` file or pasted text)
     * - `CB1-...` legacy account-only code
     */
    fun importBackup(
        rawInput: String,
        passwordHint: String = ""
    ): Result<RestoredBackupSummary> {
        val cleaned = rawInput.trim()
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Paste a backup code (CB2- / CB1-) or select a backup JSON file."))
        }
        if (cleaned.length > MAX_INPUT_CHARS) {
            return Result.failure(IllegalArgumentException("Backup payload is too large or invalid."))
        }
        return runCatching {
            when {
                cleaned.startsWith(PREFIX_V2) -> {
                    val payload = cleaned.removePrefix(PREFIX_V2).replace("\\s+".toRegex(), "")
                    val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
                    val jsonString = String(gzipDecompress(decoded), Charsets.UTF_8)
                    importFromFullJsonObject(JSONObject(jsonString), passwordHint)
                }
                cleaned.startsWith(PREFIX_V1) -> {
                    val profile = profileStore.importBackupCode(cleaned, passwordHint).getOrThrow()
                    RestoredBackupSummary(profile = profile)
                }
                cleaned.startsWith("{") -> {
                    importFromFullJsonObject(JSONObject(cleaned), passwordHint)
                }
                else -> {
                    throw IllegalArgumentException("Unrecognized backup format. Expected a CB2-... code, CB1-... code, or CricBid .json file.")
                }
            }
        }
    }

    private fun importFromFullJsonObject(
        root: JSONObject,
        passwordHint: String
    ): RestoredBackupSummary {
        val profileObj = root.optJSONObject("profile")
            ?: if (root.has("accountId") && root.has("username")) root else null
            ?: throw IllegalArgumentException("Backup file is missing CricBid account data.")

        val restoredProfile = profileStore.importProfileJson(profileObj, passwordHint).getOrThrow()
        val restoredCareer = careerStore.importActiveCareerJson(root.optJSONObject("activeCareer"))
        val restoredHof = careerStore.importHallOfFameJson(root.optJSONArray("hallOfFame"))
        val restoredCabinet = trophyCabinetStore.importCabinetJson(root.optJSONObject("trophyCabinet"))

        return RestoredBackupSummary(
            profile = restoredProfile,
            careerPlayerName = restoredCareer?.name,
            careerLevel = restoredCareer?.level,
            hallOfFameCount = restoredHof.size,
            trophiesCount = restoredCabinet.trophies.size
        )
    }

    private fun gzipCompress(input: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(input) }
        return baos.toByteArray()
    }

    private fun gzipDecompress(compressed: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(compressed)).use { gzip ->
            val buf = ByteArray(2048)
            var total = 0
            while (true) {
                val read = gzip.read(buf)
                if (read <= 0) break
                total += read
                if (total > MAX_DECOMPRESSED_BYTES) {
                    throw IllegalArgumentException("Backup data exceeds safety limit.")
                }
                baos.write(buf, 0, read)
            }
        }
        return baos.toByteArray()
    }

    companion object {
        const val PREFIX_V2 = "CB2-"
        const val PREFIX_V1 = "CB1-"
        private const val MAX_INPUT_CHARS = 512_000
        private const val MAX_DECOMPRESSED_BYTES = 1_024_000
    }
}
