package com.example.domain

import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingPlanResult
import com.example.data.model.ShoppingTrip
import com.example.data.model.SubLocationGroup
import com.example.data.model.Zone
import java.util.Locale

object RouteOptimizer {

    const val MAX_WEIGHT_KG = 8.0
    const val MAX_VOLUME_LITERS = 10.0

    fun optimizeTrips(items: List<ShoppingItem>): ShoppingPlanResult {
        val activeItems = items.filter { !it.isPurchased }
        val purchasedItems = items.filter { it.isPurchased }

        if (activeItems.isEmpty()) {
            return ShoppingPlanResult(
                trips = emptyList(),
                allActiveItems = emptyList(),
                purchasedItems = purchasedItems,
                totalWeightKg = 0.0,
                totalVolumeLiters = 0.0,
                totalEstimatedPriceRon = 0.0,
                markdownReport = ""
            )
        }

        // Items with manual trip assignment
        val manualItems = activeItems.filter { it.manualTripNumber != null }
        val autoItems = activeItems.filter { it.manualTripNumber == null }

        // 1. Separate auto-items by Zone: Diana first, Hermes second
        val dianaItems = autoItems.filter { it.zone == Zone.DIANA }
            .sortedWith(compareBy({ it.importance.priorityOrder }, { it.subLocation }))
        val hermesItems = autoItems.filter { it.zone == Zone.HERMES }
            .sortedWith(compareBy({ it.importance.priorityOrder }, { it.subLocation }))

        val trips = mutableListOf<ShoppingTrip>()

        // Pack Diana trips (fill each trip up to 8kg and 10L)
        packZoneItems(dianaItems, Zone.DIANA, trips)

        // Pack Hermes trips
        packZoneItems(hermesItems, Zone.HERMES, trips)

        // If there are manual assignments, insert or merge them into the trips
        val finalTrips = mergeManualAssignments(trips, manualItems)

        val totalWeight = Math.round(activeItems.sumOf { it.weightKg } * 100.0) / 100.0
        val totalVolume = Math.round(activeItems.sumOf { it.volumeLiters } * 10.0) / 10.0
        val totalPrice = Math.round(activeItems.sumOf { it.estimatedPriceRon } * 100.0) / 100.0

        val markdownReport = generateMarkdownReport(activeItems, finalTrips)

        return ShoppingPlanResult(
            trips = finalTrips,
            allActiveItems = activeItems.sortedBy { it.importance.priorityOrder },
            purchasedItems = purchasedItems,
            totalWeightKg = totalWeight,
            totalVolumeLiters = totalVolume,
            totalEstimatedPriceRon = totalPrice,
            markdownReport = markdownReport
        )
    }

    private fun packZoneItems(
        zoneItems: List<ShoppingItem>,
        zone: Zone,
        tripsResult: MutableList<ShoppingTrip>
    ) {
        if (zoneItems.isEmpty()) return

        val remaining = zoneItems.toMutableList()

        while (remaining.isNotEmpty()) {
            val currentTripItems = mutableListOf<ShoppingItem>()
            var currentWeight = 0.0
            var currentVolume = 0.0

            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                val fitsWeight = (currentWeight + item.weightKg) <= MAX_WEIGHT_KG
                val fitsVolume = (currentVolume + item.volumeLiters) <= MAX_VOLUME_LITERS

                if (currentTripItems.isEmpty() || (fitsWeight && fitsVolume)) {
                    currentTripItems.add(item)
                    currentWeight = Math.round((currentWeight + item.weightKg) * 100.0) / 100.0
                    currentVolume = Math.round((currentVolume + item.volumeLiters) * 10.0) / 10.0
                    iterator.remove()
                }
            }

            if (currentTripItems.isNotEmpty()) {
                tripsResult.add(createTrip(tripsResult.size + 1, zone, currentTripItems))
            } else {
                val oversized = remaining.removeAt(0)
                tripsResult.add(createTrip(tripsResult.size + 1, zone, listOf(oversized)))
            }
        }
    }

    private fun mergeManualAssignments(
        autoTrips: List<ShoppingTrip>,
        manualItems: List<ShoppingItem>
    ): List<ShoppingTrip> {
        if (manualItems.isEmpty()) return autoTrips

        val tripsMap = autoTrips.associateBy { it.tripNumber }.toMutableMap()
        val manualByTrip = manualItems.groupBy { it.manualTripNumber ?: 1 }

        for ((tripNum, items) in manualByTrip) {
            val existing = tripsMap[tripNum]
            if (existing != null) {
                val combinedItems = existing.items + items
                tripsMap[tripNum] = createTrip(tripNum, existing.zone, combinedItems)
            } else {
                val zone = items.firstOrNull()?.zone ?: Zone.DIANA
                tripsMap[tripNum] = createTrip(tripNum, zone, items)
            }
        }

        return tripsMap.values.sortedBy { it.tripNumber }
    }

    private fun createTrip(
        tripNumber: Int,
        zone: Zone,
        items: List<ShoppingItem>
    ): ShoppingTrip {
        val groupedBySub = items
            .groupBy { it.subLocation }
            .map { (subKey, itemList) ->
                SubLocationGroup(
                    subLocationKey = subKey,
                    subLocationDisplayName = Zone.getSubLocationName(subKey),
                    items = itemList.sortedBy { it.importance.priorityOrder }
                )
            }
            .sortedBy { group ->
                group.items.minOfOrNull { it.importance.priorityOrder } ?: 3
            }

        val totalWeight = Math.round(items.sumOf { it.weightKg } * 100.0) / 100.0
        val totalVolume = Math.round(items.sumOf { it.volumeLiters } * 10.0) / 10.0
        val totalPrice = Math.round(items.sumOf { it.estimatedPriceRon } * 100.0) / 100.0

        return ShoppingTrip(
            tripNumber = tripNumber,
            zone = zone,
            items = items,
            totalWeightKg = totalWeight,
            totalVolumeLiters = totalVolume,
            totalEstimatedPriceRon = totalPrice,
            subLocationGroups = groupedBySub
        )
    }

    fun generateMarkdownReport(
        activeItems: List<ShoppingItem>,
        trips: List<ShoppingTrip>
    ): String {
        val sorted = activeItems.sortedBy { it.importance.priorityOrder }
        val sb = StringBuilder()
        sb.append("## Cumpărături\n")
        for (trip in trips) {
            sb.append("\n### TURA ${trip.tripNumber}: ${trip.zone.displayName} (${trip.totalWeightKg} kg / 8kg | ${trip.totalVolumeLiters} L / 10L | ~${trip.totalEstimatedPriceRon} lei)\n")
            for (group in trip.subLocationGroups) {
                for (item in group.items) {
                    sb.append("*   **${item.subLocationDisplayName}**: ${item.name} (${item.quantity}) - ~${item.estimatedPriceRon} lei\n")
                }
            }
        }
        return sb.toString()
    }
}
