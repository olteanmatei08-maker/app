package com.example.data.model

enum class Zone(
    val id: String,
    val displayName: String,
    val priority: Int,
    val allowedSubLocations: List<String>
) {
    DIANA(
        id = "diana",
        displayName = "Zona Diana",
        priority = 1,
        allowedSubLocations = listOf("mega", "carrefour", "farmacie", "panemar", "moldovan", "puiul de craiesti", "bonas", "alt_magazin")
    ),
    HERMES(
        id = "hermes",
        displayName = "Zona Hermes",
        priority = 2,
        allowedSubLocations = listOf("piata", "beclean", "farmacie", "panemar", "alt_magazin")
    );

    companion object {
        fun fromId(id: String): Zone {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DIANA
        }

        fun getSubLocationName(subKey: String): String {
            val normalized = subKey.lowercase().trim()
            return when {
                normalized.contains("alt") -> "Alt magazin"
                normalized.contains("mega") -> "Mega Image"
                normalized.contains("carrefour") -> "Carrefour"
                normalized.contains("puiul de craiesti") || normalized.contains("craiesti") -> "Puiul de Crăiești"
                normalized.contains("moldovan") -> "Moldovan"
                normalized.contains("bonas") -> "Bonas"
                normalized.contains("beclean") -> "Beclean"
                normalized.contains("piata") || normalized.contains("piață") -> "Piață"
                normalized.contains("panemar") -> "Panemar"
                normalized.contains("farmacie") -> "Farmacie"
                else -> subKey.replaceFirstChar { it.uppercase() }
            }
        }
    }
}
