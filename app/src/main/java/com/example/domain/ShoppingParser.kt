package com.example.domain

import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone
import java.util.Locale

sealed class CommandAction {
    data class MarkPurchased(val query: String) : CommandAction()
    data class DeleteItem(val query: String) : CommandAction()
    data class AddItems(val items: List<ShoppingItem>) : CommandAction()
}

data class ExtractedQuantity(
    val displayQuantity: String,
    val multiplier: Double,
    val explicitWeightKg: Double?,
    val explicitVolumeLiters: Double?,
    val cleanedText: String
)

object ShoppingParser {

    fun parseCommand(input: String): CommandAction? {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        val checkPrefixes = listOf(
            "am cumparat ", "am cumpărat ",
            "bifeaza ", "bifează ",
            "cumparat ", "cumpărat ",
            "luat ", "am luat "
        )
        for (prefix in checkPrefixes) {
            if (lower.startsWith(prefix)) {
                val query = trimmed.substring(prefix.length).trim()
                if (query.isNotEmpty()) {
                    return CommandAction.MarkPurchased(query)
                }
            }
        }

        val deletePrefixes = listOf(
            "sterge ", "șterge ", "elimina ", "elimină ", "remove ", "delete "
        )
        for (prefix in deletePrefixes) {
            if (lower.startsWith(prefix)) {
                val query = trimmed.substring(prefix.length).trim()
                if (query.isNotEmpty()) {
                    return CommandAction.DeleteItem(query)
                }
            }
        }

        return null
    }

    fun parseRawTextList(rawText: String): List<ShoppingItem> {
        if (rawText.isBlank()) return emptyList()

        val rawLines = if (rawText.contains("\n")) {
            rawText.lines()
        } else {
            rawText.split(',', ';')
        }

        val result = mutableListOf<ShoppingItem>()
        for (line in rawLines) {
            val cleaned = cleanLine(line)
            if (cleaned.isNotBlank()) {
                val item = parseSingleItem(cleaned)
                result.add(item)
            }
        }
        return result
    }

    private fun cleanLine(line: String): String {
        return line.trim()
            .removePrefix("-")
            .removePrefix("*")
            .removePrefix("•")
            .replace(Regex("^\\d+[.)]\\s*"), "")
            .trim()
    }

    fun parseSingleItem(rawInput: String): ShoppingItem {
        var text = rawInput.trim()
        val lowerForAnalysis = text.lowercase(Locale.ROOT)

        // 1. Detect & Strip Importance (Normal default, Moderat, Important)
        val importance: Importance = when {
            lowerForAnalysis.contains("urgent") || lowerForAnalysis.contains("important") ||
                    lowerForAnalysis.contains("critic") || lowerForAnalysis.contains("vital") ||
                    lowerForAnalysis.contains("foarte important") || lowerForAnalysis.contains("ff important") -> Importance.IMPORTANT
            lowerForAnalysis.contains("moderat") || lowerForAnalysis.contains("mediu") ||
                    lowerForAnalysis.contains("destul") -> Importance.MODERAT
            else -> Importance.NORMAL
        }

        text = text.replace(
            Regex("(?i)\\b(extrem de important|foarte important|ff important|foarte urgent|urgent|important|moderat|mediu|normal)\\b"),
            ""
        ).trim()

        // 2. Extract & Strip Multi-unit Quantity or plain leading number (e.g. "4 lapte" -> qty "4 buc", name "lapte")
        val extractedQty = extractAndStripQuantity(text)
        var cleanName = extractedQty.cleanedText

        // 3. Automatically add Romanian diacritics for common grocery items
        cleanName = autoCorrectDiacritics(cleanName)

        // 4. Detect Zone, Sub-location and whether clarification is recommended
        val (zone, subLocation, isConfident) = detectZoneAndSubLocation(cleanName, lowerForAnalysis)

        // Clean out explicit store names from product title
        cleanName = cleanStoreNamesFromName(cleanName)
        if (cleanName.isBlank()) {
            cleanName = text.trim()
        }

        // Capitalize title
        cleanName = cleanName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        // 5. Calculate Weight, Volume in Liters (10L capacity), and Estimated Price
        val (finalWeight, finalVolume, finalPrice) = calculateMetrics(
            name = cleanName,
            rawLower = lowerForAnalysis,
            subLocation = subLocation,
            extracted = extractedQty
        )

        return ShoppingItem(
            name = cleanName,
            quantity = extractedQty.displayQuantity,
            zone = zone,
            subLocation = subLocation,
            weightKg = finalWeight,
            volumeLiters = finalVolume,
            estimatedPriceRon = finalPrice,
            importance = importance,
            isPurchased = false,
            needsClarification = !isConfident,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Extracts quantity with multi-unit support or bare leading numbers ("4 lapte" -> qty "4 buc", text "lapte").
     */
    fun extractAndStripQuantity(text: String): ExtractedQuantity {
        var working = text.trim()

        // 1. Bare leading number: "4 lapte", "2 carnat", "6 oua"
        val bareLeadingNumMatch = Regex("^(\\d+)\\s+(.+)$").find(working)
        if (bareLeadingNumMatch != null) {
            val count = bareLeadingNumMatch.groupValues[1].toIntOrNull() ?: 1
            val remainder = bareLeadingNumMatch.groupValues[2].trim()

            // Check if the remainder starts with a unit word
            val unitCheck = Regex("^(kg|kilograme?|kile|g|grame?|l|litri|litru|buc|bucati|bucăți|bucata|bucată|set|seturi|pachet|pachete|cutie|cutii|bax|baxuri|sticla|sticle|carton|cartoane|punga|pungi)\\b", RegexOption.IGNORE_CASE).find(remainder)
            if (unitCheck == null) {
                // It was a bare number like "4 lapte" or "2 carnat"!
                return ExtractedQuantity(
                    displayQuantity = "$count buc",
                    multiplier = count.toDouble(),
                    explicitWeightKg = null,
                    explicitVolumeLiters = null,
                    cleanedText = cleanPunctuation(remainder)
                )
            }
        }

        // 2. Weight in kg: "2 kg", "1.5kg", "2,5 kg", "1 kilogram", "2 kile"
        val kgPattern = Regex("(?i)(?:^|\\s+)(\\d+(?:[.,]\\d+)?)\\s*(?:kg|kilograme?|kile)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val kgMatch = kgPattern.find(working)
        if (kgMatch != null) {
            val amountStr = kgMatch.groupValues[1].replace(',', '.')
            val amount = amountStr.toDoubleOrNull() ?: 1.0
            val cleaned = working.removeRange(kgMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = "$amountStr kg",
                multiplier = amount,
                explicitWeightKg = amount,
                explicitVolumeLiters = Math.round(amount * 1.1 * 10.0) / 10.0,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 3. Weight in grams: "500g", "250 grame", "100 g"
        val gPattern = Regex("(?i)(?:^|\\s+)(\\d+)\\s*(?:g|grame?)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val gMatch = gPattern.find(working)
        if (gMatch != null) {
            val amount = gMatch.groupValues[1].toDoubleOrNull() ?: 500.0
            val kgVal = Math.round((amount / 1000.0) * 100.0) / 100.0
            val cleaned = working.removeRange(gMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = "${amount.toInt()}g",
                multiplier = kgVal,
                explicitWeightKg = kgVal,
                explicitVolumeLiters = Math.max(0.1, Math.round(kgVal * 1.0 * 10.0) / 10.0),
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 4. Volume in liters: "1 litru", "2 litri", "1.5L", "2l"
        val lPattern = Regex("(?i)(?:^|\\s+)(\\d+(?:[.,]\\d+)?)\\s*(?:l|litri|litru)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val lMatch = lPattern.find(working)
        if (lMatch != null) {
            val amountStr = lMatch.groupValues[1].replace(',', '.')
            val amount = amountStr.toDoubleOrNull() ?: 1.0
            val cleaned = working.removeRange(lMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = if (amount == 1.0) "1 litru" else "$amountStr litri",
                multiplier = amount,
                explicitWeightKg = amount * 1.0,
                explicitVolumeLiters = amount,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 5. Sets: "1 set", "2 seturi"
        val setPattern = Regex("(?i)(?:^|\\s+)(\\d+)\\s*(?:set|seturi)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val setMatch = setPattern.find(working)
        if (setMatch != null) {
            val count = setMatch.groupValues[1].toIntOrNull() ?: 1
            val cleaned = working.removeRange(setMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = if (count == 1) "1 set" else "$count seturi",
                multiplier = count.toDouble(),
                explicitWeightKg = count * 0.4,
                explicitVolumeLiters = count * 1.0,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 6. Bucăți: "3 bucăți", "2 bucati", "1 bucată", "3 buc"
        val bucPattern = Regex("(?i)(?:^|\\s+)(\\d+)\\s*(?:bucăți|bucati|bucată|bucata|buc)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val bucMatch = bucPattern.find(working)
        if (bucMatch != null) {
            val count = bucMatch.groupValues[1].toIntOrNull() ?: 1
            val cleaned = working.removeRange(bucMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = if (count == 1) "1 buc" else "$count bucăți",
                multiplier = count.toDouble(),
                explicitWeightKg = null,
                explicitVolumeLiters = null,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 7. Packs/cartons/bax/bottles:
        val packPattern = Regex("(?i)(?:^|\\s+)(\\d+)\\s*(?:pachete|pachet|cutii|cutie|baxuri|bax|sticle|sticla|cartoane|carton|pungi|punga|legături|legaturi|legătură|legatura)(?:\\s+|$)", setOf(RegexOption.IGNORE_CASE))
        val packMatch = packPattern.find(working)
        if (packMatch != null) {
            val count = packMatch.groupValues[1].toIntOrNull() ?: 1
            val unitWord = packMatch.value.trim()
            val cleaned = working.removeRange(packMatch.range).trim()
            return ExtractedQuantity(
                displayQuantity = unitWord,
                multiplier = count.toDouble(),
                explicitWeightKg = null,
                explicitVolumeLiters = null,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        // 8. Multipliers "2x", "x2"
        val xLeading = Regex("(?i)^(\\d+)[xX]\\s*").find(working)
        if (xLeading != null) {
            val count = xLeading.groupValues[1].toIntOrNull() ?: 1
            val cleaned = working.removeRange(xLeading.range).trim()
            return ExtractedQuantity(
                displayQuantity = "$count buc",
                multiplier = count.toDouble(),
                explicitWeightKg = null,
                explicitVolumeLiters = null,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        val xTrailing = Regex("(?i)\\s*[xX]\\s*(\\d+)$").find(working)
        if (xTrailing != null) {
            val count = xTrailing.groupValues[1].toIntOrNull() ?: 1
            val cleaned = working.removeRange(xTrailing.range).trim()
            return ExtractedQuantity(
                displayQuantity = "$count buc",
                multiplier = count.toDouble(),
                explicitWeightKg = null,
                explicitVolumeLiters = null,
                cleanedText = cleanPunctuation(cleaned)
            )
        }

        return ExtractedQuantity(
            displayQuantity = "1 buc",
            multiplier = 1.0,
            explicitWeightKg = null,
            explicitVolumeLiters = null,
            cleanedText = cleanPunctuation(working)
        )
    }

    private fun cleanPunctuation(text: String): String {
        return text
            .replace(Regex("(?i)^(de|cu)\\s+"), "")
            .replace(Regex("[-,;:]+$"), "")
            .replace(Regex("^[-,;:]+"), "")
            .trim()
    }

    /**
     * Automatic Romanian diacritics insertion for common grocery items typed without diacritics.
     */
    fun autoCorrectDiacritics(input: String): String {
        var res = input
        val map = listOf(
            Regex("(?i)\\bcarnat\\b") to "cârnaț",
            Regex("(?i)\\bcarnati\\b") to "cârnați",
            Regex("(?i)\\bpaine\\b") to "pâine",
            Regex("(?i)\\boua\\b") to "ouă",
            Regex("(?i)\\brosii\\b") to "roșii",
            Regex("(?i)\\bsmantana\\b") to "smântână",
            Regex("(?i)\\bfaina\\b") to "făină",
            Regex("(?i)\\bceapa\\b") to "ceapă",
            Regex("(?i)\\bverdeata\\b") to "verdeață",
            Regex("(?i)\\bpatrunjel\\b") to "pătrunjel",
            Regex("(?i)\\bcastraveti\\b") to "castraveți",
            Regex("(?i)\\bmuschiulet\\b") to "mușchiuleț",
            Regex("(?i)\\bcostita\\b") to "costiță",
            Regex("(?i)\\bsunca\\b") to "șuncă",
            Regex("(?i)\\bbiscuiti\\b") to "biscuiți",
            Regex("(?i)\\bservetele\\b") to "șervețele",
            Regex("(?i)\\bhartie igienica\\b") to "hârtie igienică",
            Regex("(?i)\\bplacinta\\b") to "plăcintă",
            Regex("(?i)\\bbranza\\b") to "brânză"
        )
        for ((pattern, replacement) in map) {
            res = res.replace(pattern, replacement)
        }
        return res
    }

    /**
     * Store Assignment based on Cluj specifications:
     * - Chicken -> Puiul de Crăiești
     * - Non-chicken meat -> Moldovan
     * - Bread:
     *   * "paine domneasca", "paine boiereasca", "paine bunica", "paine bunu" -> Mega Image
     *   * other bread/panificatie ("paine", "taraneasca", "artizanala", "bagheta", "covrig") -> Panemar
     * - Eggs -> Carrefour
     * - Dairy:
     *   * "cas bonas", "telemea bonas" -> Bonas
     *   * "unt de casa", "iaurt de casa", "cas beclean" -> Beclean
     *   * general milk, smântână 20%, caș feliat -> Mega Image
     * - Market produce -> Piață (Hermes)
     * - Majority groceries (biscuiți, struguri, șervețele, fulgi de porumb, hârtie, etc.) -> Mega Image
     * - "alt magazin" / "alt loc" -> Alt magazin
     */
    fun detectZoneAndSubLocation(cleanName: String, rawLower: String): Triple<Zone, String, Boolean> {
        val lower = "${cleanName.lowercase(Locale.ROOT)} $rawLower"

        // Explicit "Alt magazin" / "Alt loc"
        if (lower.contains("alt magazin") || lower.contains("alt loc") || lower.contains("altundeva")) {
            return Triple(Zone.DIANA, "alt_magazin", true)
        }

        // Direct store keyword mentions
        if (lower.contains("beclean")) return Triple(Zone.HERMES, "beclean", true)
        if (lower.contains("piata") || lower.contains("piață") || lower.contains("piata hermes")) return Triple(Zone.HERMES, "piata", true)
        if (lower.contains("puiul de craiesti") || lower.contains("craiesti") || lower.contains("crăiești")) return Triple(Zone.DIANA, "puiul de craiesti", true)
        if (lower.contains("moldovan")) return Triple(Zone.DIANA, "moldovan", true)
        if (lower.contains("panemar")) return Triple(Zone.DIANA, "panemar", true)
        if (lower.contains("bonas")) return Triple(Zone.DIANA, "bonas", true)
        if (lower.contains("carrefour")) return Triple(Zone.DIANA, "carrefour", true)
        if (lower.contains("mega") || lower.contains("mega image")) return Triple(Zone.DIANA, "mega", true)
        if (lower.contains("farmacie")) return Triple(Zone.DIANA, "farmacie", true)

        // 1. Bread Rules:
        // "paine domneasca", "paine boiereasca", "paine bunu", "paine bunica" -> Mega Image
        if (lower.contains("domneasca") || lower.contains("boiereasca") || lower.contains("bunu") || lower.contains("bunica")) {
            return Triple(Zone.DIANA, "mega", true)
        }
        // Panemar bakery:
        if (lower.contains("paine") || lower.contains("pâine") || lower.contains("taraneasca") ||
            lower.contains("țărănească") || lower.contains("artizanala") || lower.contains("bagheta") ||
            lower.contains("baghetă") || lower.contains("covrig") || lower.contains("croissant") ||
            lower.contains("chifla") || lower.contains("chifle") || lower.contains("cozonac") ||
            lower.contains("corn") || lower.contains("placinta") || lower.contains("plăcintă")
        ) {
            return Triple(Zone.DIANA, "panemar", true)
        }

        // 2. Chicken meat -> Puiul de Crăiești
        if (lower.contains("pui") || lower.contains("piept de pui") || lower.contains("piept pui") ||
            lower.contains("pulpe") || lower.contains("aripi") || lower.contains("ciocanele") ||
            lower.contains("ficatei") || lower.contains("spate pui")
        ) {
            return Triple(Zone.DIANA, "puiul de craiesti", true)
        }

        // 3. Non-chicken meat -> Moldovan
        if (lower.contains("carnat") || lower.contains("cârnaț") || lower.contains("carnati") || lower.contains("cârnați") ||
            lower.contains("carne") || lower.contains("porc") || lower.contains("vita") || lower.contains("vită") ||
            lower.contains("mici") || lower.contains("salam") || lower.contains("sunca") || lower.contains("șuncă") ||
            lower.contains("muschiulet") || lower.contains("mușchiuleț") || lower.contains("ceafa") || lower.contains("ceafă") ||
            lower.contains("costita") || lower.contains("costiță") || lower.contains("mezeluri") ||
            lower.contains("slaninuta") || lower.contains("bacon") || lower.contains("kaizer") ||
            lower.contains("cotlet") || lower.contains("crenvursti")
        ) {
            return Triple(Zone.DIANA, "moldovan", true)
        }

        // 4. Eggs -> Carrefour
        if (lower.contains("ou") || lower.contains("oua") || lower.contains("ouă") || lower.contains("carton oua")) {
            return Triple(Zone.DIANA, "carrefour", true)
        }

        // 5. Traditional Dairy -> Beclean (Hermes)
        if (lower.contains("unt de casa") || lower.contains("unt de casă") || lower.contains("iaurt de casa") ||
            lower.contains("iaurt de casă") || lower.contains("cas beclean") || lower.contains("branza beclean")
        ) {
            return Triple(Zone.HERMES, "beclean", true)
        }

        // 6. Bonas Dairy -> Bonas (Diana)
        if (lower.contains("cas bonas") || lower.contains("telemea bonas") || lower.contains("lapte bonas")) {
            return Triple(Zone.DIANA, "bonas", true)
        }

        // 7. Market Vegetables -> Piață (Hermes)
        if (lower.contains("rosii") || lower.contains("roșii") || lower.contains("castraveti") || lower.contains("castraveți") ||
            lower.contains("ardei") || lower.contains("ceapa") || lower.contains("ceapă") || lower.contains("usturoi") ||
            lower.contains("cartofi") || lower.contains("verdeata") || lower.contains("verdeață") ||
            lower.contains("patrunjel") || lower.contains("pătrunjel") || lower.contains("marar") || lower.contains("legume piata")
        ) {
            return Triple(Zone.HERMES, "piata", true)
        }

        // 8. Pharmacy
        if (lower.contains("medicament") || lower.contains("paracetamol") || lower.contains("aspirina") ||
            lower.contains("nurofen") || lower.contains("vitamine") || lower.contains("pansament")
        ) {
            return Triple(Zone.DIANA, "farmacie", true)
        }

        // 9. Mega Image standard grocery items
        if (lower.contains("biscuiti") || lower.contains("biscuiți") || lower.contains("struguri") ||
            lower.contains("servetele") || lower.contains("șervețele") || lower.contains("fulgi") ||
            lower.contains("porumb") || lower.contains("hartie") || lower.contains("hârtie") ||
            lower.contains("detergent") || lower.contains("apa") || lower.contains("apă") ||
            lower.contains("lapte") || lower.contains("smantana") || lower.contains("smântână") ||
            lower.contains("ulei") || lower.contains("zahar") || lower.contains("faina")
        ) {
            return Triple(Zone.DIANA, "mega", true)
        }

        // Unrecognized / generic item -> Flag for clarification so user gets instant clean confirmation!
        return Triple(Zone.DIANA, "mega", false)
    }

    private fun cleanStoreNamesFromName(name: String): String {
        return name
            .replace(Regex("(?i)\\b(mega image|mega|carrefour|panemar|moldovan|puiul de craiesti|craiesti|crăiești|bonas|beclean|piata hermes|piata|piață|farmacie|alt magazin|alt loc|diana|hermes)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun calculateMetrics(
        name: String,
        rawLower: String,
        subLocation: String,
        extracted: ExtractedQuantity
    ): Triple<Double, Double, Double> {
        val lower = "$name $rawLower".lowercase(Locale.ROOT)
        val mult = extracted.multiplier

        // Unit Price Estimation (lei / RON) - accurate real Cluj prices
        val unitPrice = when {
            // Dairy - Milk is strictly 4.0 lei
            lower.contains("lapte") -> 4.0
            lower.contains("iaurt") -> 2.5
            lower.contains("smantana") || lower.contains("smântână") -> 5.5
            lower.contains("unt") -> 11.0
            lower.contains("cas") || lower.contains("caș") || lower.contains("telemea") -> 14.0

            // Bread / Bakery - Artisan/Panemar bread is 13.0 lei
            subLocation == "panemar" && (lower.contains("covrig") || lower.contains("chifla")) -> 2.5
            subLocation == "panemar" || lower.contains("taraneasca") || lower.contains("artizanala") || lower.contains("rustica") -> 13.0
            lower.contains("paine") || lower.contains("pâine") -> {
                if (lower.contains("domneasca") || lower.contains("boiereasca")) 6.0 else 13.0
            }

            // Vegetables & Fruits - Potatoes are strictly 5.0 lei / kg
            lower.contains("cartof") || lower.contains("cartofi") -> 5.0
            lower.contains("morcov") || lower.contains("ceapa") || lower.contains("ceapă") -> 4.0
            lower.contains("mere") -> 5.0
            lower.contains("banane") -> 6.5
            lower.contains("castraveti") || lower.contains("castraveți") -> 6.0
            lower.contains("rosii") || lower.contains("roșii") -> 8.0
            lower.contains("struguri") -> 12.0
            lower.contains("verdeata") || lower.contains("patrunjel") -> 2.0

            // Meat & Poultry (Moldovan & Puiul de Crăiești)
            lower.contains("piept de pui") || lower.contains("piept") -> 26.0
            lower.contains("pulpe") -> 16.0
            lower.contains("pui") -> 20.0
            lower.contains("carnat") || lower.contains("cârnaț") -> 22.0
            lower.contains("mici") -> 18.0
            lower.contains("porc") || lower.contains("ceafa") || lower.contains("cotlet") -> 28.0
            lower.contains("vita") || lower.contains("vită") || lower.contains("muschiulet") -> 38.0
            lower.contains("salam") || lower.contains("sunca") || lower.contains("șuncă") || lower.contains("mezeluri") -> 9.0

            // Eggs (Carrefour / Mega Image)
            lower.contains("ou") || lower.contains("oua") || lower.contains("ouă") -> 12.0

            // Household & Pantry (Mega Image / Carrefour)
            lower.contains("bax") -> 18.0
            lower.contains("apa") || lower.contains("apă") -> 3.5
            lower.contains("ulei") -> 7.0
            lower.contains("zahar") || lower.contains("zahăr") -> 4.0
            lower.contains("faina") || lower.contains("făină") -> 3.5
            lower.contains("biscuiti") || lower.contains("biscuiți") -> 4.5
            lower.contains("ciocolata") || lower.contains("ciocolată") -> 5.5
            lower.contains("fulgi") -> 7.0
            lower.contains("servetele") || lower.contains("șervețele") -> 5.0
            lower.contains("hartie") || lower.contains("hârtie") -> 18.0
            lower.contains("detergent") -> 35.0

            else -> 6.0
        }

        // Unit Weight Estimation (kg)
        val unitWeight = when {
            extracted.explicitWeightKg != null -> extracted.explicitWeightKg / mult
            lower.contains("hartie") || lower.contains("hârtie") -> 0.8
            lower.contains("detergent") -> 2.5
            lower.contains("bax") -> 6.0
            lower.contains("iaurt") -> 0.15
            lower.contains("unt") -> 0.2
            lower.contains("paine") || lower.contains("pâine") -> 0.5
            lower.contains("ou") || lower.contains("oua") || lower.contains("ouă") -> 0.6
            lower.contains("lapte") || lower.contains("suc") || lower.contains("ulei") -> 1.0
            lower.contains("cartof") || lower.contains("mere") || lower.contains("banane") -> 1.0
            lower.contains("fulgi") || lower.contains("servetele") -> 0.3
            else -> 0.5
        }

        // Unit Volume in Liters (L) - for 10L backpack
        val unitVolumeLiters = when {
            extracted.explicitVolumeLiters != null -> extracted.explicitVolumeLiters / mult
            lower.contains("hartie") || lower.contains("hârtie") -> 4.0
            lower.contains("bax") -> 6.0
            lower.contains("detergent") -> 3.5
            lower.contains("paine") || lower.contains("pâine") -> 1.5
            lower.contains("fulgi") -> 1.8
            lower.contains("ou") || lower.contains("oua") || lower.contains("ouă") -> 1.2
            lower.contains("lapte") || lower.contains("suc") || lower.contains("ulei") -> 1.0
            lower.contains("cartof") || lower.contains("mere") || lower.contains("rosii") || lower.contains("roșii") -> 1.0
            lower.contains("struguri") || lower.contains("banane") -> 1.2
            lower.contains("iaurt") || lower.contains("unt") -> 0.25
            lower.contains("servetele") -> 1.0
            else -> 0.8
        }

        val totalWeight = Math.round((unitWeight * mult) * 100.0) / 100.0
        val totalVolume = Math.round((unitVolumeLiters * mult) * 10.0) / 10.0
        val totalPrice = Math.round((unitPrice * mult) * 100.0) / 100.0

        return Triple(totalWeight, totalVolume, totalPrice)
    }
}
