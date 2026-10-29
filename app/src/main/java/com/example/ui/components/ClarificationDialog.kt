package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
fun ClarificationDialog(
    item: ShoppingItem,
    onConfirm: (ShoppingItem) -> Unit,
    onOpenFullEdit: (ShoppingItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStore by remember { mutableStateOf(item.subLocation) }
    var volumeLiters by remember { mutableDoubleStateOf(item.volumeLiters) }
    var priceRon by remember { mutableDoubleStateOf(item.estimatedPriceRon) }

    val availableStores = listOf(
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkOutline, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Confirmă detalii",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = AccentCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Store Selector Chips
                Text(
                    text = "Alege Magazinul:",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableStores.forEach { (subKey, label) ->
                        val isSelected = selectedStore == subKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStore = subKey },
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

                // Volume (Liters) Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Volum ghiozdan (Litri)",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f L (din 10L)", volumeLiters),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
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

                // Price (RON) Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Preț estimativ total",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f lei", priceRon),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
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

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Validare vs Editare Manuală
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val updated = item.copy(
                                subLocation = selectedStore,
                                volumeLiters = volumeLiters,
                                estimatedPriceRon = priceRon
                            )
                            onOpenFullEdit(updated)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline)
                    ) {
                        Text("Personalizează", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            val zone = if (selectedStore == "piata" || selectedStore == "beclean") Zone.HERMES else Zone.DIANA
                            val updated = item.copy(
                                subLocation = selectedStore,
                                zone = zone,
                                volumeLiters = volumeLiters,
                                estimatedPriceRon = priceRon,
                                needsClarification = false
                            )
                            onConfirm(updated)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentCyan,
                            contentColor = DarkSurface
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validare", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
