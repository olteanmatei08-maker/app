package com.example.data.model

enum class Importance(
    val priorityOrder: Int,
    val displayName: String,
    val badgeColorHex: Long
) {
    IMPORTANT(1, "important", 0xFFEF4444),
    MODERAT(2, "moderat", 0xFF38BDF8),
    NORMAL(3, "normal", 0xFF64748B);

    companion object {
        fun fromString(value: String): Importance {
            val normalized = value.lowercase().trim()
            return when {
                normalized.contains("urgent") || normalized.contains("important") ||
                        normalized.contains("critic") || normalized.contains("vital") -> IMPORTANT
                normalized.contains("moderat") || normalized.contains("mediu") ||
                        normalized.contains("destul") -> MODERAT
                else -> NORMAL
            }
        }
    }
}
