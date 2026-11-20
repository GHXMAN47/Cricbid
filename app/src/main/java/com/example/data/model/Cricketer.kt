package com.example.data.model

import java.util.Locale

enum class PlayerRole(val shortLabel: String, val displayName: String) {
    BATTER("BAT", "Batter"),
    WICKETKEEPER("WK", "Wicketkeeper"),
    ALL_ROUNDER("AR", "All-Rounder"),
    SEAMER("PACE", "Seamer"),
    SPINNER("SPIN", "Spinner");

    val isSpecialistBowler: Boolean
        get() = this == SEAMER || this == SPINNER

    companion object {
        fun fromNameSafe(raw: String?): PlayerRole =
            entries.firstOrNull { it.name == raw }
                ?: when (raw) {
                    "BATSMAN" -> BATTER
                    "BOWLER" -> SEAMER
                    else -> BATTER
                }
    }
}

data class Cricketer(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val overseas: Boolean,
    val role: PlayerRole,
    val eraTag: String,
    val battingRating: Int,
    val bowlingRating: Int,
    val fieldingRating: Int,
    val basePriceLakhs: Long,
    val specialty: String
) {
    val overallRating: Int
        get() = when (role) {
            PlayerRole.BATTER -> ((battingRating * 0.70) + (fieldingRating * 0.20) + (bowlingRating * 0.10)).toInt()
            PlayerRole.SEAMER, PlayerRole.SPINNER -> ((bowlingRating * 0.70) + (fieldingRating * 0.20) + (battingRating * 0.10)).toInt()
            PlayerRole.ALL_ROUNDER -> ((battingRating * 0.42) + (bowlingRating * 0.42) + (fieldingRating * 0.16)).toInt()
            PlayerRole.WICKETKEEPER -> ((battingRating * 0.60) + (fieldingRating * 0.35) + (bowlingRating * 0.05)).toInt()
        }
}

object CurrencyFormatter {
    fun formatLakhs(lakhs: Long): String {
        return when {
            lakhs >= 100L -> {
                val crores = lakhs / 100.0
                if (lakhs % 100L == 0L) {
                    String.format(Locale.US, "₹%.0f Cr", crores)
                } else {
                    String.format(Locale.US, "₹%.2f Cr", crores)
                }
            }
            else -> "₹${lakhs} L"
        }
    }

    fun nextBidIncrementLakhs(currentBidLakhs: Long): Long = when {
        currentBidLakhs < 200L -> 25L   // +₹25L under ₹2 Cr
        currentBidLakhs < 500L -> 50L   // +₹50L from ₹2 Cr to ₹5 Cr
        currentBidLakhs < 1000L -> 75L  // +₹75L from ₹5 Cr to ₹10 Cr
        else -> 100L                    // +₹1 Cr above ₹10 Cr
    }
}
