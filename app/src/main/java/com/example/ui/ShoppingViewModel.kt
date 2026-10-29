package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShoppingDatabase
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingPlanResult
import com.example.data.model.Zone
import com.example.data.repository.ShoppingRepository
import com.example.domain.CommandAction
import com.example.domain.RouteOptimizer
import com.example.domain.ShoppingParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalUiState(
    val quickCommandText: String = "",
    val isRawListDialogOpen: Boolean = false,
    val isAddItemDialogOpen: Boolean = false,
    val editingItem: ShoppingItem? = null,
    val clarifyingItem: ShoppingItem? = null,
    val currentTripIndex: Int = 0,
    val snackbarMessage: String? = null
)

data class ShoppingUiState(
    val planResult: ShoppingPlanResult = ShoppingPlanResult(
        trips = emptyList(),
        allActiveItems = emptyList(),
        purchasedItems = emptyList(),
        totalWeightKg = 0.0,
        totalVolumeLiters = 0.0,
        totalEstimatedPriceRon = 0.0,
        markdownReport = ""
    ),
    val allItems: List<ShoppingItem> = emptyList(),
    val quickCommandText: String = "",
    val isRawListDialogOpen: Boolean = false,
    val isAddItemDialogOpen: Boolean = false,
    val editingItem: ShoppingItem? = null,
    val clarifyingItem: ShoppingItem? = null,
    val currentTripIndex: Int = 0,
    val snackbarMessage: String? = null
)

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShoppingRepository
    private val localState = MutableStateFlow(LocalUiState())

    val uiState: StateFlow<ShoppingUiState>

    init {
        val database = ShoppingDatabase.getInstance(application)
        val backup = com.example.data.local.ShoppingStorageBackup(application)
        repository = ShoppingRepository(database.shoppingDao(), backup)

        viewModelScope.launch {
            repository.checkAndRestoreBackupIfNeeded()
        }

        // Starts completely empty or restores from internal backup
        uiState = combine(repository.allItems, localState) { rawItems, local ->
            val plan = RouteOptimizer.optimizeTrips(rawItems)

            // Clamp trip index
            val safeTripIndex = if (plan.trips.isEmpty()) 0 else local.currentTripIndex.coerceIn(0, plan.trips.size - 1)

            ShoppingUiState(
                planResult = plan,
                allItems = rawItems,
                quickCommandText = local.quickCommandText,
                isRawListDialogOpen = local.isRawListDialogOpen,
                isAddItemDialogOpen = local.isAddItemDialogOpen,
                editingItem = local.editingItem,
                clarifyingItem = local.clarifyingItem,
                currentTripIndex = safeTripIndex,
                snackbarMessage = local.snackbarMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ShoppingUiState()
        )
    }

    fun onQuickCommandChange(text: String) {
        localState.update { it.copy(quickCommandText = text) }
    }

    fun onTripIndexChange(index: Int) {
        localState.update { it.copy(currentTripIndex = index) }
    }

    fun onQuickCommandSubmit() {
        val input = localState.value.quickCommandText.trim()
        if (input.isBlank()) return

        val command = ShoppingParser.parseCommand(input)
        if (command != null) {
            when (command) {
                is CommandAction.MarkPurchased -> {
                    viewModelScope.launch {
                        val count = repository.markPurchasedByQuery(command.query)
                        if (count > 0) {
                            showSnackbar("Bifat cumpărat: \"${command.query}\"")
                        } else {
                            showSnackbar("Nu s-a găsit: \"${command.query}\"")
                        }
                    }
                }
                is CommandAction.DeleteItem -> {
                    viewModelScope.launch {
                        val count = repository.deleteByQuery(command.query)
                        if (count > 0) {
                            showSnackbar("Eliminat din listă: \"${command.query}\"")
                        } else {
                            showSnackbar("Nu s-a găsit: \"${command.query}\"")
                        }
                    }
                }
                is CommandAction.AddItems -> {
                    viewModelScope.launch {
                        repository.addItems(command.items)
                        showSnackbar("Adăugat ${command.items.size} produse")
                    }
                }
            }
        } else {
            val newItem = ShoppingParser.parseSingleItem(input)
            viewModelScope.launch {
                val insertedId = repository.addItem(newItem)
                val itemWithId = newItem.copy(id = insertedId)

                // If uncertain about store/quantity/price, pop up simple clarification dialog
                if (newItem.needsClarification) {
                    localState.update { it.copy(clarifyingItem = itemWithId) }
                } else {
                    showSnackbar("Adăugat: ${newItem.name} (${newItem.quantity})")
                }
            }
        }

        localState.update { it.copy(quickCommandText = "") }
    }

    fun onConfirmClarification(updatedItem: ShoppingItem) {
        viewModelScope.launch {
            repository.updateItem(updatedItem)
            showSnackbar("Confirmat: ${updatedItem.name} (${updatedItem.subLocationDisplayName})")
        }
        localState.update { it.copy(clarifyingItem = null) }
    }

    fun dismissClarification() {
        localState.update { it.copy(clarifyingItem = null) }
    }

    fun onTogglePurchased(item: ShoppingItem) {
        viewModelScope.launch {
            repository.togglePurchased(item.id, !item.isPurchased)
            val action = if (!item.isPurchased) "Cumpărat" else "Reactivat în plan"
            showSnackbar("$action: ${item.name}")
        }
    }

    fun onDeleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            showSnackbar("Șters: ${item.name}")
        }
    }

    fun onAddBatchRawText(rawText: String) {
        val parsed = ShoppingParser.parseRawTextList(rawText)
        if (parsed.isNotEmpty()) {
            viewModelScope.launch {
                repository.addItems(parsed)
                showSnackbar("Adăugat cu succes ${parsed.size} produse în plan!")
            }
        }
        localState.update { it.copy(isRawListDialogOpen = false) }
    }

    fun onSaveItem(
        id: Long,
        name: String,
        quantity: String,
        zone: Zone,
        subLocation: String,
        weightKg: Double,
        volumeLiters: Double,
        priceRon: Double,
        importance: Importance,
        manualTripNumber: Int?
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.addItem(
                    ShoppingItem(
                        name = name.trim(),
                        quantity = quantity.trim(),
                        zone = zone,
                        subLocation = subLocation,
                        weightKg = weightKg,
                        volumeLiters = volumeLiters,
                        estimatedPriceRon = priceRon,
                        importance = importance,
                        manualTripNumber = manualTripNumber
                    )
                )
                showSnackbar("Adăugat: $name")
            } else {
                repository.updateItem(
                    ShoppingItem(
                        id = id,
                        name = name.trim(),
                        quantity = quantity.trim(),
                        zone = zone,
                        subLocation = subLocation,
                        weightKg = weightKg,
                        volumeLiters = volumeLiters,
                        estimatedPriceRon = priceRon,
                        importance = importance,
                        manualTripNumber = manualTripNumber
                    )
                )
                showSnackbar("Actualizat: $name")
            }
        }
        localState.update { it.copy(editingItem = null, isAddItemDialogOpen = false, clarifyingItem = null) }
    }

    fun openRawListDialog() {
        localState.update { it.copy(isRawListDialogOpen = true) }
    }

    fun closeRawListDialog() {
        localState.update { it.copy(isRawListDialogOpen = false) }
    }

    fun openAddItemDialog() {
        localState.update { it.copy(editingItem = null, isAddItemDialogOpen = true) }
    }

    fun openEditItemDialog(item: ShoppingItem) {
        localState.update { it.copy(editingItem = item, isAddItemDialogOpen = true) }
    }

    fun closeEditItemDialog() {
        localState.update { it.copy(editingItem = null, isAddItemDialogOpen = false) }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            repository.clearAll()
            showSnackbar("Toate produsele au fost șterse")
        }
    }

    fun clearSnackbar() {
        localState.update { it.copy(snackbarMessage = null) }
    }

    private fun showSnackbar(message: String) {
        localState.update { it.copy(snackbarMessage = message) }
    }
}
