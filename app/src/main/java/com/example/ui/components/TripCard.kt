package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Importance
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingTrip
import com.example.data.model.Zone
import com.example.domain.RouteOptimizer
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GaugeLimit
import com.example.ui.theme.GaugeSafe
import com.example.ui.theme.GaugeWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TripCard(
    trip: ShoppingTrip,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onItemClick: (ShoppingItem) -> Unit,
    onItemLongClick: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoneColor = if (trip.zone == Zone.DIANA) AccentEmerald else AccentPurple
    val weightFraction = (trip.totalWeightKg / RouteOptimizer.MAX_WEIGHT_KG).toFloat().coerceIn(0f, 1f)
    val volumeFraction = (trip.totalVolumeLiters / RouteOptimizer.MAX_VOLUME_LITERS).toFloat().coerceIn(0f, 1f)

    val animatedWeight by animateFloatAsState(targetValue = weightFraction, label = "weight_progress")
    val animatedVolume by animateFloatAsState(targetValue = volumeFraction, label = "volume_progress")

    val weightColor = when {
        trip.totalWeightKg >= 8.0 -> GaugeLimit
        trip.totalWeightKg >= 6.5 -> GaugeWarning
        else -> GaugeSafe
    }

    val volumeColor = when {
        trip.totalVolumeLiters >= 10.0 -> GaugeLimit
        trip.totalVolumeLiters >= 8.0 -> GaugeWarning
        else -> AccentCyan
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trip_card_${trip.tripNumber}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: TURA [X] + Zona
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(zoneColor.copy(alpha = 0.2f))
                            .border(1.5.dp, zoneColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = zoneColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "TURA ${trip.tripNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = trip.zone.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = zoneColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Total items in trip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${trip.items.size} produse",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Capacity meters (Weight 8kg & Volume 10 Liters)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weight Gauge (8 kg max)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = weightColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Greutate",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f / 8 kg", trip.totalWeightKg),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = weightColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedWeight },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = weightColor,
                        trackColor = DarkOutline
                    )
                }

                // Volume Gauge (10 Liters max)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = volumeColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Volum",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f / 10 L", trip.totalVolumeLiters),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = volumeColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedVolume },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = volumeColor,
                        trackColor = DarkOutline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-location Store Groups
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                trip.subLocationGroups.forEach { group ->
                    SubLocationStoreSection(
                        storeName = group.subLocationDisplayName,
                        items = group.items,
                        onTogglePurchased = onTogglePurchased,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick,
                        onDeleteItem = onDeleteItem
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom of Trip: Total Estimated Trip Price
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceElevated.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Total estimat tură:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                Text(
                    text = String.format(Locale.US, "~%.1f lei", trip.totalEstimatedPriceRon),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

@Composable
fun SubLocationStoreSection(
    storeName: String,
    items: List<ShoppingItem>,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onItemClick: (ShoppingItem) -> Unit,
    onItemLongClick: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated.copy(alpha = 0.5f))
            .border(1.dp, DarkOutline.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${items.size} art.",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                SpaciousTripItemRow(
                    item = item,
                    onTogglePurchased = { onTogglePurchased(item) },
                    onItemClick = { onItemClick(item) },
                    onItemLongClick = { onItemLongClick(item) },
                    onDelete = { onDeleteItem(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpaciousTripItemRow(
    item: ShoppingItem,
    onTogglePurchased: () -> Unit,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("checkbox_${item.id}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentEmerald,
                    uncheckedColor = TextMuted,
                    checkmarkColor = Color.White
                )
            )

            // Product Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Large Name
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                )

                // Badges row: Quantity, Weight & Volume (Liters), Estimated Price, Importance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quantity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCyan.copy(alpha = 0.2f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.quantity,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Weight & Liters
                    Text(
                        text = "${item.weightKg} kg • ${item.volumeLiters} L",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    // Small estimated total price for item
                    Text(
                        text = String.format(Locale.US, "• ~%.1f lei", item.estimatedPriceRon),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFCD34D),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )

                    // Importance if not normal
                    if (item.importance != Importance.NORMAL) {
                        ImportanceBadge(importance = item.importance)
                    }
                }
            }

            // Quick Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Șterge",
                    tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ImportanceBadge(importance: Importance) {
    val (bgColor, textColor) = when (importance) {
        Importance.IMPORTANT -> Pair(Color(0x33EF4444), Color(0xFFFCA5A5))
        Importance.MODERAT -> Pair(Color(0x3338BDF8), Color(0xFF7DD3FC))
        Importance.NORMAL -> Pair(Color(0x2264748B), Color(0xFFCBD5E1))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = importance.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
