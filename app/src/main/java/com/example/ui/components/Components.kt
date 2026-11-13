package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.DeliveryAddress
import com.example.model.KochiServiceArea
import com.example.model.OrderStatus
import java.util.Locale

@Composable
fun MedicineThumbnail(
    @DrawableRes imageRes: Int,
    contentDescription: String,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
        )
    }
}

fun formatCurrency(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        "₹${amount.toInt()}"
    } else {
        "₹${String.format(Locale.US, "%.2f", amount)}"
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DiscountPercentPill(
    discountPercent: Int,
    modifier: Modifier = Modifier
) {
    if (discountPercent <= 0) return
    StatusPill(
        text = "$discountPercent% OFF",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun StoreRatingBadge(
    rating: Double,
    reviewCount: Int? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", rating),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Rating",
                modifier = Modifier.size(11.dp)
            )
            if (reviewCount != null) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "($reviewCount)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease quantity",
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase quantity",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PriceDisplay(
    price: Double,
    mrp: Double? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Text(
            text = formatCurrency(price),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (mrp != null && mrp > price) {
            Text(
                text = formatCurrency(mrp),
                style = MaterialTheme.typography.labelSmall.copy(
                    textDecoration = TextDecoration.LineThrough
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderStatusStepper(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    if (currentStatus == OrderStatus.CANCELLED) {
        StatusPill(
            text = "Order Cancelled",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            modifier = modifier
        )
        return
    }

    val steps = listOf(
        OrderStatus.PLACED to "Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PACKED to "Packed",
        OrderStatus.OUT_FOR_DELIVERY to "Rider",
        OrderStatus.DELIVERED to "Delivered"
    )
    val currentIndex = steps.indexOfFirst { it.first == currentStatus }.coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, (_, label) ->
            val reached = index <= currentIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (index == 0) Color.Transparent
                                else if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        if (reached) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = label,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (index == steps.lastIndex) Color.Transparent
                                else if (index < currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocalPharmacy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun AddAddressDialog(
    existingAddress: DeliveryAddress? = null,
    defaultPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (label: String, fullAddress: String, landmark: String, contactPhone: String, isDefault: Boolean) -> Unit
) {
    val zones = KochiServiceArea.supportedZones
    val initialZone = remember(existingAddress) {
        val fullLower = existingAddress?.fullAddress?.lowercase().orEmpty()
        zones.firstOrNull { fullLower.contains(it.name.lowercase()) || fullLower.contains(it.pinCode) }
            ?: zones.first()
    }
    var selectedZone by remember { mutableStateOf(initialZone) }
    var label by remember { mutableStateOf(existingAddress?.label?.ifBlank { "Home" } ?: "Home") }
    var streetAddress by remember {
        mutableStateOf(
            existingAddress?.fullAddress?.substringBefore(",")?.trim().orEmpty()
        )
    }
    var cityLocality by remember { mutableStateOf("${initialZone.name}, Kochi") }
    var pinCode by remember {
        val matchPin = Regex("682\\d{3}").find(existingAddress?.fullAddress.orEmpty())?.value
        mutableStateOf(matchPin ?: initialZone.pinCode)
    }
    var landmark by remember { mutableStateOf(existingAddress?.landmark.orEmpty()) }
    var phone by remember {
        mutableStateOf(existingAddress?.contactPhone?.takeIf { it.isNotBlank() } ?: defaultPhone)
    }
    var isDefault by remember { mutableStateOf(existingAddress?.isDefault ?: true) }

    val composedFullAddress = remember(streetAddress, cityLocality, pinCode) {
        listOf(
            streetAddress.trim(),
            cityLocality.trim(),
            "Kerala ${pinCode.trim()}"
        ).filter { it.isNotBlank() }.joinToString(", ")
    }

    val pinValid = KochiServiceArea.isKochiPinCode(pinCode)
    val addressInKochi = KochiServiceArea.isKochiAddress(composedFullAddress, landmark)
    val showOutsideKochiError = (pinCode.trim().length == 6 && !pinValid) ||
        (cityLocality.trim().length >= 3 && !addressInKochi)

    val isValid = label.trim().isNotEmpty() &&
        streetAddress.trim().length >= 4 &&
        pinValid &&
        addressInKochi &&
        phone.trim().length >= 7

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingAddress == null) "Add Kochi Delivery Address" else "Edit Kochi Address",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Kochi Only: Purchases are restricted to Kochi addresses (PIN 682xxx).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    text = "Select Kochi Locality",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(zones) { zone ->
                        FilterChip(
                            selected = selectedZone == zone,
                            onClick = {
                                selectedZone = zone
                                cityLocality = "${zone.name}, Kochi"
                                pinCode = zone.pinCode
                            },
                            label = { Text(zone.name) },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 40) label = it },
                    label = { Text("Label (Home, Work, Family)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_label_input")
                )
                OutlinedTextField(
                    value = streetAddress,
                    onValueChange = { if (it.length <= 140) streetAddress = it },
                    label = { Text("House / Flat / Street") },
                    minLines = 2,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_full_input")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = cityLocality,
                        onValueChange = { if (it.length <= 60) cityLocality = it },
                        label = { Text("Kochi Area / City") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.length <= 6) pinCode = digits
                        },
                        label = { Text("PIN (682xxx)") },
                        singleLine = true,
                        isError = pinCode.length == 6 && !pinValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("address_pin_input")
                    )
                }

                if (showOutsideKochiError) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Delivery unavailable outside Kochi. Only Kochi PIN codes (682xxx) can purchase.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { if (it.length <= 120) landmark = it },
                    label = { Text("Landmark (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 20) phone = it },
                    label = { Text("Contact Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_phone_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Set as default delivery address",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(label, composedFullAddress, landmark, phone, isDefault)
                },
                enabled = isValid,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_address_button")
            ) {
                Text("Save Kochi Address")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
