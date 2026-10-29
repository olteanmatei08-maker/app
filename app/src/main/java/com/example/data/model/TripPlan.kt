package com.example.data.model

data class SubLocationGroup(
    val subLocationKey: String,
    val subLocationDisplayName: String,
    val items: List<ShoppingItem>
)

data class ShoppingTrip(
    val tripNumber: Int,
    val zone: Zone,
    val items: List<ShoppingItem>,
    val totalWeightKg: Double,
    val totalVolumeLiters: Double,
    val totalEstimatedPriceRon: Double,
    val subLocationGroups: List<SubLocationGroup>
) {
    val isWeightNearLimit: Boolean
        get() = totalWeightKg >= 7.0

    val isVolumeNearLimit: Boolean
        get() = totalVolumeLiters >= 8.5
}

data class ShoppingPlanResult(
    val trips: List<ShoppingTrip>,
    val allActiveItems: List<ShoppingItem>,
    val purchasedItems: List<ShoppingItem>,
    val totalWeightKg: Double,
    val totalVolumeLiters: Double,
    val totalEstimatedPriceRon: Double,
    val markdownReport: String
)
