package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.Zone
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditItemDialog(
    item: ShoppingItem?,
    totalTripsCount: Int = 3,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        quantity: String,
        zone: Zone,
        subLocation: String,
        weightKg: Double,
        volumeLiters: Double,
        estimatedPriceRon: Double,
        importance: Importance,
        manualTripNumber: Int?
    ) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity ?: "1 buc") }
    var selectedSubLocation by remember { mutableStateOf(item?.subLocation ?: "mega") }
    var weightKg by remember { mutableDoubleStateOf(item?.weightKg ?: 0.5) }
    var volumeLiters by remember { mutableDoubleStateOf(item?.volumeLiters ?: 1.0) }
    var priceRon by remember { mutableDoubleStateOf(item?.estimatedPriceRon ?: 10.0) }
    var selectedImportance by remember { mutableStateOf(item?.importance ?: Importance.NORMAL) }
    var selectedTripNumber by remember { mutableStateOf(item?.manualTripNumber) }

    val allStores = listOf(
        Pair("mega", "Mega Image"),
        Pair("moldovan", "Moldovan"),
        Pair("panemar", "Panemar"),
        Pair("carrefour", "Carrefour"),
        Pair("puiul de craiesti", "Puiul de Crăiești"),
        Pair("bonas", "Bonas"),
        Pair("beclean", "Beclean"),
        Pair("piata", "Piață"),
        Pair("farmacie", "Farmacie"),
        Pair("alt_magazin", "Alt magazin")
    )

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkOutline, RoundedCornerShape(20.dp))
                .testTag("edit_item_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (item != null) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (item != null) "Modificare Produs" else "Adăugare Produs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Închide", tint = TextMuted)
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Denumire Produs") },
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantitate (ex: 2 kg, 1 litru, 3 bucăți, 1 set)") },
                    modifier = Modifier.fillMaxWidth().testTag("item_quantity_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Magazin selector (including Alt magazin)
                Text(
                    text = "Magazin / Locație",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allStores.forEach { (subKey, label) ->
                        val isSelected = selectedSubLocation == subKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubLocation = subKey },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                                selectedLabelColor = AccentCyan
                            )
                        )
                    }
                }

                // Tura din care face parte (Manual trip assignment)
                Text(
                    text = "Alocare Tură",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTripNumber == null,
                        onClick = { selectedTripNumber = null },
                        label = { Text("Auto (Optimizat)", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                            selectedLabelColor = AccentCyan
                        )
                    )
                    val maxTrips = maxOf(3, totalTripsCount + 1)
                    for (t in 1..maxTrips) {
                        FilterChip(
                            selected = selectedTripNumber == t,
                            onClick = { selectedTripNumber = t },
                            label = { Text("Tura $t", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                                selectedLabelColor = AccentCyan
                            )
                        )
                    }
                }

                // Volume (Litri) Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Volum (Litri - Ghiozdan 10L)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                        Text(String.format(Locale.US, "%.1f L", volumeLiters), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { volumeLiters = Math.max(0.1, Math.round((volumeLiters - 0.5) * 10.0) / 10.0) },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Scade", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { volumeLiters = Math.min(10.0, Math.round((volumeLiters + 0.5) * 10.0) / 10.0) },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Crește", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Preț Estimativ Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Preț estimativ (Lei)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                        Text(String.format(Locale.US, "%.1f lei", priceRon), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { priceRon = Math.max(1.0, Math.round((priceRon - 2.0) * 10.0) / 10.0) },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Scade", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { priceRon = Math.round((priceRon + 2.0) * 10.0) / 10.0 },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Crește", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Greutate (kg) Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Greutate (kg)", style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                        Text(String.format(Locale.US, "%.2f kg", weightKg), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { weightKg = Math.max(0.1, Math.round((weightKg - 0.25) * 100.0) / 100.0) },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Scade", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { weightKg = Math.round((weightKg + 0.25) * 100.0) / 100.0 },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(DarkSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Crește", tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Importanță: Doar Normal, Moderat, Important
                Text(
                    text = "Importanță",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Importance.entries.forEach { imp ->
                        val isSelected = selectedImportance == imp
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedImportance = imp },
                            label = { Text(imp.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = Color(imp.badgeColorHex).copy(alpha = 0.25f),
                                selectedLabelColor = Color(imp.badgeColorHex)
                            )
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
                    ) {
                        Text("Anulează")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val zone = if (selectedSubLocation == "piata" || selectedSubLocation == "beclean") Zone.HERMES else Zone.DIANA
                                onSave(
                                    item?.id ?: 0L,
                                    name,
                                    quantity,
                                    zone,
                                    selectedSubLocation,
                                    weightKg,
                                    volumeLiters,
                                    priceRon,
                                    selectedImportance,
                                    selectedTripNumber
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = DarkSurface
                        ),
                        modifier = Modifier.testTag("save_item_button")
                    ) {
                        Text("Salvează", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
