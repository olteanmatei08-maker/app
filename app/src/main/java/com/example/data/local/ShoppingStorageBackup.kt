package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Robust secondary offline persistent backup stored in internal files directory
 * and SharedPreferences. Guarantees that shopping items are never lost even if
 * the app is killed, restarted, device is rebooted, or database schema resets.
 */
class ShoppingStorageBackup(private val context: Context) {

    private val backupFile = File(context.filesDir, "cluj_shopping_backup.json")
    private val prefs = context.getSharedPreferences("cluj_shopping_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "ShoppingStorageBackup"
        private const val PREFS_KEY_BACKUP = "shopping_items_backup_json"
    }

    @Synchronized
    fun saveItems(items: List<ShoppingItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("zoneId", item.zone.id)
                    put("subLocation", item.subLocation)
                    put("weightKg", item.weightKg)
                    put("volumeLiters", item.volumeLiters)
                    put("estimatedPriceRon", item.estimatedPriceRon)
                    put("importanceName", item.importance.name)
                    if (item.manualTripNumber != null) {
                        put("manualTripNumber", item.manualTripNumber)
                    } else {
                        put("manualTripNumber", JSONObject.NULL)
                    }
                    put("isPurchased", item.isPurchased)
                    put("needsClarification", item.needsClarification)
                    put("createdAt", item.createdAt)
                }
                jsonArray.put(obj)
            }
            val jsonString = jsonArray.toString()

            // 1. Write to internal storage file atomically
            val tempFile = File(context.filesDir, "cluj_shopping_backup.tmp")
            tempFile.writeText(jsonString)
            tempFile.renameTo(backupFile)

            // 2. Also save to SharedPreferences for redundant persistence
            prefs.edit().putString(PREFS_KEY_BACKUP, jsonString).apply()

            Log.d(TAG, "Successfully persisted ${items.size} items to internal storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup items", e)
        }
    }

    @Synchronized
    fun loadItems(): List<ShoppingItem> {
        try {
            // First try internal private file
            var jsonString: String? = null
            if (backupFile.exists()) {
                jsonString = backupFile.readText()
            }

            // Fallback to SharedPreferences if file is missing/empty
            if (jsonString.isNullOrBlank()) {
                jsonString = prefs.getString(PREFS_KEY_BACKUP, null)
            }

            if (jsonString.isNullOrBlank()) {
                return emptyList()
            }

            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ShoppingItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val manualTrip = if (obj.isNull("manualTripNumber")) null else obj.optInt("manualTripNumber")
                list.add(
                    ShoppingItem(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        quantity = obj.getString("quantity"),
                        zone = Zone.fromId(obj.getString("zoneId")),
                        subLocation = obj.getString("subLocation"),
                        weightKg = obj.optDouble("weightKg", 0.5),
                        volumeLiters = obj.optDouble("volumeLiters", 0.5),
                        estimatedPriceRon = obj.optDouble("estimatedPriceRon", 0.0),
                        importance = Importance.fromString(obj.optString("importanceName", "NORMAL")),
                        manualTripNumber = manualTrip,
                        isPurchased = obj.optBoolean("isPurchased", false),
                        needsClarification = obj.optBoolean("needsClarification", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            Log.d(TAG, "Restored ${list.size} items from internal storage backup")
            return list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load backup items", e)
            return emptyList()
        }
    }
}
