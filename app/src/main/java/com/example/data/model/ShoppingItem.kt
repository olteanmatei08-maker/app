package com.example.data.model

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val quantity: String = "1 buc",
    val zone: Zone = Zone.DIANA,
    val subLocation: String = "mega",
    val weightKg: Double = 0.5,
    val volumeLiters: Double = 1.0, // In liters (backpack has 10L capacity)
    val estimatedPriceRon: Double = 10.0, // Estimated price in RON
    val importance: Importance = Importance.NORMAL,
    val manualTripNumber: Int? = null, // Allows user to assign/move to a specific trip
    val isPurchased: Boolean = false,
    val needsClarification: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val subLocationDisplayName: String
        get() = Zone.getSubLocationName(subLocation)
}
