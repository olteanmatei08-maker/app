package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone

@Entity(tableName = "shopping_items")
data class ShoppingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val zoneId: String,
    val subLocation: String,
    val weightKg: Double,
    val volumeLiters: Double,
    val estimatedPriceRon: Double,
    val importanceName: String,
    val manualTripNumber: Int? = null,
    val isPurchased: Boolean,
    val needsClarification: Boolean = false,
    val createdAt: Long
) {
    fun toDomain(): ShoppingItem {
        return ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            zone = Zone.fromId(zoneId),
            subLocation = subLocation,
            weightKg = weightKg,
            volumeLiters = volumeLiters,
            estimatedPriceRon = estimatedPriceRon,
            importance = Importance.fromString(importanceName),
            manualTripNumber = manualTripNumber,
            isPurchased = isPurchased,
            needsClarification = needsClarification,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(item: ShoppingItem): ShoppingEntity {
            return ShoppingEntity(
                id = item.id,
                name = item.name,
                quantity = item.quantity,
                zoneId = item.zone.id,
                subLocation = item.subLocation,
                weightKg = item.weightKg,
                volumeLiters = item.volumeLiters,
                estimatedPriceRon = item.estimatedPriceRon,
                importanceName = item.importance.name,
                manualTripNumber = item.manualTripNumber,
                isPurchased = item.isPurchased,
                needsClarification = item.needsClarification,
                createdAt = item.createdAt
            )
        }
    }
}
