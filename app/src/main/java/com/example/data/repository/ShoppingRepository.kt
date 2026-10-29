package com.example.data.repository

import com.example.data.local.ShoppingDao
import com.example.data.local.ShoppingEntity
import com.example.data.local.ShoppingStorageBackup
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class ShoppingRepository(
    private val dao: ShoppingDao,
    private val backup: ShoppingStorageBackup? = null
) {

    val allItems: Flow<List<ShoppingItem>> = dao.getAllItemsFlow()
        .map { entities -> entities.map { it.toDomain() } }
        .onEach { items ->
            // Keep internal disk backup strictly synchronized whenever items change
            backup?.saveItems(items)
        }

    suspend fun checkAndRestoreBackupIfNeeded() {
        try {
            val currentInDb = dao.getAllItems()
            if (currentInDb.isEmpty() && backup != null) {
                val backupItems = backup.loadItems()
                if (backupItems.isNotEmpty()) {
                    dao.insertItems(backupItems.map { ShoppingEntity.fromDomain(it) })
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun syncBackup() {
        try {
            val all = dao.getAllItems().map { it.toDomain() }
            backup?.saveItems(all)
        } catch (_: Exception) {
        }
    }

    suspend fun addItem(item: ShoppingItem): Long {
        val id = dao.insertItem(ShoppingEntity.fromDomain(item))
        syncBackup()
        return id
    }

    suspend fun addItems(items: List<ShoppingItem>) {
        dao.insertItems(items.map { ShoppingEntity.fromDomain(it) })
        syncBackup()
    }

    suspend fun updateItem(item: ShoppingItem) {
        dao.updateItem(ShoppingEntity.fromDomain(item))
        syncBackup()
    }

    suspend fun deleteItem(item: ShoppingItem) {
        dao.deleteItem(ShoppingEntity.fromDomain(item))
        syncBackup()
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
        syncBackup()
    }

    suspend fun togglePurchased(id: Long, isPurchased: Boolean) {
        dao.setPurchased(id, isPurchased)
        syncBackup()
    }

    suspend fun markPurchasedByQuery(query: String): Int {
        val count = dao.markPurchasedByQuery(query)
        syncBackup()
        return count
    }

    suspend fun deleteByQuery(query: String): Int {
        val count = dao.deleteByQuery(query)
        syncBackup()
        return count
    }

    suspend fun clearAll() {
        dao.clearAll()
        backup?.saveItems(emptyList())
    }

    suspend fun loadSampleData() {
        val sampleList = listOf(
            ShoppingItem(
                name = "Lapte",
                quantity = "2 L",
                zone = Zone.DIANA,
                subLocation = "mega",
                weightKg = 2.0,
                volumeLiters = 2.0,
                estimatedPriceRon = 8.0,
                importance = Importance.NORMAL
            ),
            ShoppingItem(
                name = "Piept de pui",
                quantity = "1 kg",
                zone = Zone.DIANA,
                subLocation = "puiul de craiesti",
                weightKg = 1.0,
                volumeLiters = 1.2,
                estimatedPriceRon = 26.0,
                importance = Importance.IMPORTANT
            ),
            ShoppingItem(
                name = "Pâine rustică",
                quantity = "1 buc",
                zone = Zone.DIANA,
                subLocation = "panemar",
                weightKg = 0.5,
                volumeLiters = 1.5,
                estimatedPriceRon = 13.0,
                importance = Importance.NORMAL
            )
        )
        dao.insertItems(sampleList.map { ShoppingEntity.fromDomain(it) })
        syncBackup()
    }
}
