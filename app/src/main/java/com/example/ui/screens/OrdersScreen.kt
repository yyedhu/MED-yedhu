package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PharmacyCatalog
import com.example.model.DeliveryInstructionPresets
import com.example.model.DeliveryTrackerEngine
import com.example.model.LiveOrderTracking
import com.example.model.MedicineOrder
import com.example.model.OrderStatus
import com.example.model.PaymentMethodOption
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.LiveDeliveryTrackingSheet
import com.example.ui.components.OrderStatusStepper
import com.example.ui.components.RealTimeOrderTrackingCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OrderDateFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)
}

private fun formatOrderTimestamp(date: Date): String =
    OrderDateFormat.get()?.format(date) ?: "Just now"

@Composable
fun OrdersScreen(
    orders: List<MedicineOrder>,
    liveTransitProgress: Map<String, Float> = emptyMap(),
    liveTrackings: Map<String, LiveOrderTracking> = emptyMap(),
    trackedOrderId: String? = null,
    onConsumeTrackedOrderId: () -> Unit = {},
    onCancelOrder: (MedicineOrder) -> Unit,
    onReorderOrder: (MedicineOrder) -> Unit = {},
    onSubmitRating: (String, Int, String) -> Unit = { _, _, _ -> },
    onBrowseStores: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTrackingOrderId by remember { mutableStateOf<String?>(null) }
    var ratingOrder by remember { mutableStateOf<MedicineOrder?>(null) }
    var helpOrder by remember { mutableStateOf<MedicineOrder?>(null) }

    LaunchedEffect(trackedOrderId, orders) {
        if (!trackedOrderId.isNullOrBlank() && orders.any { it.id == trackedOrderId }) {
            activeTrackingOrderId = trackedOrderId
            onConsumeTrackedOrderId()
        }
    }

    val activeTrackingOrder = orders.firstOrNull { it.id == activeTrackingOrderId }
    if (activeTrackingOrder != null) {
        LiveDeliveryTrackingSheet(
            order = activeTrackingOrder,
            liveTransitFraction = liveTransitProgress[activeTrackingOrder.id],
            liveTracking = liveTrackings[activeTrackingOrder.id],
            onDismiss = { activeTrackingOrderId = null }
        )
    }

    ratingOrder?.let { targetOrder ->
        RateOrderBottomSheet(
            order = targetOrder,
            onSubmit = { stars, comment ->
                onSubmitRating(targetOrder.id, stars, comment)
                ratingOrder = null
            },
            onDismiss = { ratingOrder = null }
        )
    }

    helpOrder?.let { targetOrder ->
        OrderHelpBottomSheet(
            order = targetOrder,
            onDismiss = { helpOrder = null }
        )
    }

    if (orders.isEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                EmptyStatePanel(
                    title = stringResource(R.string.empty_orders_title),
                    subtitle = stringResource(R.string.empty_orders_subtitle),
                    actionLabel = "Order Medicines",
                    onAction = onBrowseStores
                )
            }
        }
        return
    }

    val activeLiveOrder = remember(orders) {
        orders.firstOrNull { !OrderStatus.fromWire(it.status).isTerminal }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "orders_header", contentType = "section_header") {
            Text(
                text = "Your Medicine Orders (${orders.size})",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (activeLiveOrder != null) {
            item(key = "realtime_tracking_hero_${activeLiveOrder.id}", contentType = "tracking_hero") {
                RealTimeOrderTrackingCard(
                    order = activeLiveOrder,
                    liveTransitFraction = liveTransitProgress[activeLiveOrder.id],
                    liveTracking = liveTrackings[activeLiveOrder.id],
                    isMapPaused = activeTrackingOrder != null,
                    onOpenFullDetails = { activeTrackingOrderId = activeLiveOrder.id }
                )
            }
        }

        items(
            items = orders,
            key = { it.id },
            contentType = { "order_card" }
        ) { order ->
            OrderCard(
                order = order,
                liveTransitFraction = liveTransitProgress[order.id],
                liveTracking = liveTrackings[order.id],
                onTrackOrder = { activeTrackingOrderId = order.id },
                onCancelOrder = { onCancelOrder(order) },
                onReorderOrder = { onReorderOrder(order) },
                onRateOrder = { ratingOrder = order },
                onHelpClick = { helpOrder = order }
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: MedicineOrder,
    liveTransitFraction: Float?,
    liveTracking: LiveOrderTracking? = null,
    onTrackOrder: () -> Unit,
    onCancelOrder: () -> Unit,
    onReorderOrder: () -> Unit,
    onRateOrder: () -> Unit,
    onHelpClick: () -> Unit
) {
    val tracking = remember(order, liveTransitFraction, liveTracking) {
        DeliveryTrackerEngine.buildTrackingSnapshot(
            order = order,
            liveTransitFraction = liveTransitFraction,
            liveTracking = liveTracking
        )
    }
    val status = tracking.status
    val canCancel = status == OrderStatus.PLACED
    val formattedDate = remember(order.createdAt) {
        order.createdAt?.toDate()?.let(::formatOrderTimestamp) ?: "Just now"
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$formattedDate • ${order.itemCount} item${if (order.itemCount == 1L) "" else "s"} • ${PaymentMethodOption.fromWire(order.paymentMethod).displayTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(order.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = MaterialTheme.typography.labelMedium.fontFamily
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = onHelpClick,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Order support",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Help", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = status.displayLabel,
                    containerColor = when (status) {
                        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
                        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = when (status) {
                        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimaryContainer
                        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                if (order.hasPrescription) {
                    StatusPill(
                        text = "Rx Verified",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (order.couponCode.isNotBlank() && order.discountAmount > 0.0) {
                    StatusPill(
                        text = "${order.couponCode} -${formatCurrency(order.discountAmount)}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                if (!status.isTerminal) {
                    StatusPill(
                        text = if (tracking.hasRiderArrived) "Rider Arrived" else "${tracking.remainingMinutes}m ETA",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OrderStatusStepper(currentStatus = status)

            if (status != OrderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Order status alert",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tracking.customerNotificationText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GpsFixed,
                                contentDescription = "Live GPS coordinates",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "GPS: ${tracking.formattedGpsCoordinates} • ${tracking.currentLocationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TwoWheeler,
                                contentDescription = "Assigned delivery partner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Partner: ${tracking.partner.name} • ${tracking.partner.vehicleLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = order.itemsSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order.rating > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Your rating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                append("You rated ${order.rating}/5")
                                if (order.reviewComment.isNotBlank()) {
                                    append(" • ")
                                    append(order.reviewComment)
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (status != OrderStatus.CANCELLED) {
                    Button(
                        onClick = onTrackOrder,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("track_order_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (status == OrderStatus.DELIVERED) "Route & GPS"
                            else "Track GPS Live"
                        )
                    }
                }

                if (status.isTerminal) {
                    OutlinedButton(
                        onClick = onReorderOrder,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reorder_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reorder")
                    }
                }

                if (status == OrderStatus.DELIVERED && order.rating == 0L) {
                    OutlinedButton(
                        onClick = onRateOrder,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("rate_order_button_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rate")
                    }
                }

                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancelOrder,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("cancel_order_button_${order.id}")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RateOrderBottomSheet(
    order: MedicineOrder,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(5) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Rate Your Delivery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "How was your medicine order from ${order.storeName}?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { selectedStars = star },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rating_star_$star")
                    ) {
                        Icon(
                            imageVector = if (star <= selectedStars) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = "$star stars",
                            tint = if (star <= selectedStars) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DeliveryInstructionPresets.ratingCompliments.forEach { tag ->
                    FilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                        },
                        label = { Text(tag) }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("Share feedback (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val combinedComment = buildList {
                        addAll(selectedTags)
                        val trimmed = note.trim()
                        if (trimmed.isNotEmpty()) add(trimmed)
                    }.joinToString(" • ").take(300)
                    onSubmit(selectedStars, combinedComment)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("submit_rating_button")
            ) {
                Text("Submit $selectedStars★ Rating")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderHelpBottomSheet(
    order: MedicineOrder,
    onDismiss: () -> Unit
) {
    val store = remember(order.storeId) { PharmacyCatalog.storeById(order.storeId) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Order Verification & Support",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "${order.storeName} • Licensed Kochi Pharmacy",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Drug License: ${store?.licenseNumber ?: "KL-EKM-VERIFIED"} • Batch sealed at store",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Text(
                text = "Order Reference: ${order.id}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• Pharmacist verification ensures tamper-proof sealed medicine bags.\n• Delivery is strictly tracked via live GPS inside Kochi (PIN 682xxx).\n• Tap Track GPS Live on any active order to view route coordinates or call your assigned rider directly.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Got It")
            }
        }
    }
}
