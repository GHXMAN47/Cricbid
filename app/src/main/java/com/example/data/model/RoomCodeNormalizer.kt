package com.example.data.model

import java.util.Locale

/**
 * Normalizes and validates user-entered multiplayer room codes across UI, ViewModel,
 * and local/cloud repositories so whitespace, pasted '#', or casing never break joins.
 */
object RoomCodeNormalizer {
    fun normalize(raw: String): String =
        raw.trim()
            .removePrefix("#")
            .replace(Regex("\\s+"), "")
            .uppercase(Locale.US)

    fun isValid(normalizedCode: String): Boolean =
        normalizedCode.matches(Regex("^[0-9]{4}$"))
}
