package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PharmacyCatalog
import com.example.model.CartItem
import com.example.model.DeliveryAddress
import com.example.model.DeliveryTrackerEngine
import com.example.model.KochiServiceArea
import com.example.model.LiveOrderTracking
import com.example.model.MedicalStore
import com.example.model.MedicineCategory
import com.example.model.MedicineOffering
import com.example.model.MedicineOrder
import com.example.model.OrderStatus
import com.example.model.PromoCatalog
import com.example.model.PromoCoupon
import com.example.model.StoreSortOption
import com.example.ui.components.DiscountPercentPill
import com.example.ui.components.MedicineThumbnail
import com.example.ui.components.PriceDisplay
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StatusPill
import com.example.ui.components.StoreRatingBadge
import com.example.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresHomeScreen(
    searchQuery: String,
    selectedCategory: MedicineCategory,
    open24hOnly: Boolean,
    freeDeliveryOnly: Boolean = false,
    favoritesOnly: Boolean = false,
    rxFreeOnly: Boolean = false,
    storeSortOption: StoreSortOption = StoreSortOption.RELEVANCE,
    favoriteStoreIds: Set<String> = emptySet(),
    appliedCouponCode: String = "",
    stores: List<MedicalStore>,
    offerings: List<MedicineOffering>,
    cartItems: List<CartItem>,
    pastOrders: List<MedicineOrder> = emptyList(),
    activeAddress: DeliveryAddress?,
    activeOrder: MedicineOrder? = null,
    liveTransitFraction: Float? = null,
    liveTracking: LiveOrderTracking? = null,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (MedicineCategory) -> Unit,
    onToggleOpen24h: () -> Unit,
    onToggleFreeDelivery: () -> Unit = {},
    onToggleFavoritesOnly: () -> Unit = {},
    onToggleRxFreeOnly: () -> Unit = {},
    onSelectStoreSort: (StoreSortOption) -> Unit = {},
    onToggleFavoriteStore: (String) -> Unit = {},
    onApplyCoupon: (String) -> Unit = {},
    onReorderOrder: (MedicineOrder) -> Unit = {},
    onStoreClick: (String) -> Unit,
    onViewAllMedicines: () -> Unit,
    onTrackOrder: (String) -> Unit = {},
    onAddToCart: (MedicineOffering) -> Unit,
    onIncrementCartItem: (CartItem) -> Unit,
    onDecrementCartItem: (CartItem) -> Unit,
    onManageAddresses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cartLookup = remember(cartItems) {
        cartItems.associateBy { "${it.storeId}__${it.medicineId}" }
    }
    val cartCountByStore = remember(cartItems) {
        cartItems.groupBy { it.storeId }.mapValues { (_, items) -> items.sumOf { it.quantity }.toInt() }
    }
    val reorderCandidates = remember(pastOrders) {
        pastOrders.filter { OrderStatus.fromWire(it.status) == OrderStatus.DELIVERED }.take(5)
    }
    var showSortSheet by remember { mutableStateOf(false) }

    if (showSortSheet) {
        ModalBottomSheet(onDismissRequest = { showSortSheet = false }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = "Sort Kochi Medical Stores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                StoreSortOption.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                onSelectStoreSort(option)
                                showSortSheet = false
                            }
                            .padding(vertical = 8.dp)
                            .testTag("store_sort_${option.name.lowercase()}")
                    ) {
                        RadioButton(
                            selected = storeSortOption == option,
                            onClick = {
                                onSelectStoreSort(option)
                                showSortSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val isSearching = searchQuery.isNotBlank()

    Column(modifier = modifier.fillMaxSize()) {
        // Pinned Pharmacy Search Header (Search by Medical Store Name or Kochi Location)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_search_header")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.store_search_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search pharmacies by name or location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("clear_store_search_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear store search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_search_input")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PharmacyCatalog.kochiQuickLocations, key = { "loc_$it" }) { area ->
                        val selected = searchQuery.trim().equals(area, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSearchChange(if (selected) "" else area)
                            },
                            label = { Text(area) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("location_chip_${area.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Active Delivery Partner Live Tracking Banner
            if (!isSearching && activeOrder != null) {
                item(key = "active_order_banner") {
                    val tracking = remember(activeOrder, liveTransitFraction, liveTracking) {
                        DeliveryTrackerEngine.buildTrackingSnapshot(
                            order = activeOrder,
                            liveTransitFraction = liveTransitFraction,
                            liveTracking = liveTracking
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackOrder(activeOrder.id) }
                            .testTag("active_order_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.TwoWheeler,
                                        contentDescription = "Track delivery partner",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tracking.customerNotificationText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tracking.partner.name} • GPS ${tracking.formattedGpsCoordinates}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPill(
                                text = if (tracking.hasRiderArrived) "Arrived" else "${tracking.remainingMinutes}m GPS",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Kochi Location Pill Header
            if (!isSearching) {
                item(key = "kochi_location_header") {
            val isAddressInKochi = KochiServiceArea.isKochiAddress(activeAddress)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (activeAddress != null && !isAddressInKochi)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onManageAddresses)
                    .testTag("location_selector_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Kochi delivery location",
                        tint = if (activeAddress != null && !isAddressInKochi)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAddress?.label?.let { "$it • Kochi" } ?: "Kochi Delivery Zone (PIN 682xxx)",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeAddress?.fullAddress ?: "Kochi customers only — Tap to add your Kochi address",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeAddress != null && !isAddressInKochi)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = when {
                            activeAddress == null -> "Kochi Only"
                            isAddressInKochi -> "Kochi Eligible"
                            else -> "Outside Kochi"
                        },
                        containerColor = when {
                            activeAddress == null -> MaterialTheme.colorScheme.primaryContainer
                            isAddressInKochi -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = when {
                            activeAddress == null -> MaterialTheme.colorScheme.onPrimaryContainer
                            isAddressInKochi -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
                }
            }

        // Swiggy / Zomato style Kochi Offers Strip
        if (!isSearching) {
            item(key = "promo_offers_carousel") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kochi Deals & Coupons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PromoCatalog.coupons, key = { it.code }) { coupon ->
                            PromoDealBannerCard(
                                coupon = coupon,
                                isSelected = appliedCouponCode.equals(coupon.code, ignoreCase = true),
                                onClick = { onApplyCoupon(coupon.code) }
                            )
                        }
                    }
                }
            }

            // Swiggy / Zomato Quick Reorder Strip (for returning customers)
            if (reorderCandidates.isNotEmpty()) {
                item(key = "quick_reorder_strip") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Order Again from Kochi Stores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reorderCandidates, key = { "reorder_${it.id}" }) { pastOrder ->
                                QuickReorderCard(
                                    order = pastOrder,
                                    onReorder = { onReorderOrder(pastOrder) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swiggy / Zomato Sort & Quick Filter Bar
        item(key = "sort_and_quick_filters") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = storeSortOption != StoreSortOption.RELEVANCE,
                        onClick = { showSortSheet = true },
                        label = { Text("Sort: ${storeSortOption.label}") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_sort_chip")
                    )
                }
                item {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = onToggleFavoritesOnly,
                        label = { Text("Favorites") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_favorites")
                    )
                }
                item {
                    FilterChip(
                        selected = freeDeliveryOnly,
                        onClick = onToggleFreeDelivery,
                        label = { Text("Free Delivery") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_free_delivery")
                    )
                }
                item {
                    FilterChip(
                        selected = open24hOnly,
                        onClick = onToggleOpen24h,
                        label = { Text("24x7 Open") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_open_24h")
                    )
                }
                item {
                    FilterChip(
                        selected = rxFreeOnly,
                        onClick = onToggleRxFreeOnly,
                        label = { Text("No Rx Needed") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_rx_free")
                    )
                }
            }
        }

        // Kochi Medical Stores Feed Cards (Swiggy/Zomato Restaurant Card Style)
        item(key = "stores_section_header") {
            Text(
                text = "Kochi Medical Stores (${stores.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (stores.isEmpty()) {
            item(key = "stores_empty") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (isSearching) {
                                "No Kochi medical stores match \"${searchQuery.trim()}\". Try another store name or Kochi location."
                            } else {
                                "No Kochi medical stores match these filters. Try clearing Favorites or Free Delivery filters."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSearching) {
                            OutlinedButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_store_search_button")
                            ) {
                                Text("Clear Search")
                            }
                        }
                    }
                }
            }
        } else {
            items(
                items = stores,
                key = { "store_feed_${it.id}" },
                contentType = { "store_card" }
            ) { store ->
                MedicalStoreFeedCard(
                    store = store,
                    isFavorite = store.id in favoriteStoreIds,
                    cartItemCount = cartCountByStore[store.id] ?: 0,
                    onToggleFavorite = { onToggleFavoriteStore(store.id) },
                    onClick = { onStoreClick(store.id) }
                )
            }
        }

        // Category Filter Chips for Medicine Search
        item(key = "medicine_category_chips") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MedicineCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category.label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }

        // Always-Visible All Medicines Section (Names + Images + Store Price Comparison)
        item(key = "all_medicines_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Compare All Medicines (${offerings.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Names, images & prices across Kochi medical stores",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onViewAllMedicines,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Grid View")
                }
            }
        }

        if (offerings.isEmpty()) {
            item(key = "offerings_empty") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No matching medicines found across nearby medical stores. Try a generic name like Paracetamol or Cetirizine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(
                items = offerings,
                key = { "${it.storeId}__${it.id}" },
                contentType = { "medicine_card" }
            ) { offering ->
                val existingCartItem = cartLookup["${offering.storeId}__${offering.id}"]
                val store = PharmacyCatalog.storeById(offering.storeId)
                MedicineComparisonCard(
                    offering = offering,
                    etaMinutes = store?.etaMinutes ?: 25,
                    cartItem = existingCartItem,
                    onAdd = { onAddToCart(offering) },
                    onIncrement = { existingCartItem?.let(onIncrementCartItem) },
                    onDecrement = { existingCartItem?.let(onDecrementCartItem) },
                    onOpenStore = { onStoreClick(offering.storeId) }
                )
            }
        }
        }
    }
}

@Composable
private fun PromoDealBannerCard(
    coupon: PromoCoupon,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected)
            MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .width(228.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("home_promo_${coupon.code}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusPill(
                    text = coupon.badgeText,
                    containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimary
                )
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.LocalOffer,
                    contentDescription = coupon.code,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = coupon.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isSelected) "Applied to Checkout • ${coupon.code}" else "Tap to use ${coupon.code}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickReorderCard(
    order: MedicineOrder,
    onReorder: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.width(244.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.storeName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = order.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(order.totalAmount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onReorder,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("home_reorder_${order.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reorder", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MedicalStoreFeedCard(
    store: MedicalStore,
    isFavorite: Boolean,
    cartItemCount: Int,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("store_card_${store.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.LocalPharmacy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.verified_pharmacy_badge),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "${store.neighborhood} • ${store.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("favorite_store_${store.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StoreRatingBadge(rating = store.rating, reviewCount = store.reviewCount)
                StatusPill(
                    text = "${store.etaMinutes} mins",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatusPill(
                    text = if (store.deliveryFee == 0.0) "Free Delivery" else "${formatCurrency(store.deliveryFee)} fee",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (store.isOpen24Hours) {
                    StatusPill(
                        text = stringResource(R.string.open_24h_badge),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                if (cartItemCount > 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    StatusPill(
                        text = "$cartItemCount in bag",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (store.promoHeadline.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = store.promoHeadline,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineComparisonCard(
    offering: MedicineOffering,
    etaMinutes: Int,
    cartItem: CartItem?,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onOpenStore: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            MedicineThumbnail(
                imageRes = offering.imageRes,
                contentDescription = offering.name,
                size = 80.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = offering.storeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenStore)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$etaMinutes min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = offering.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${offering.genericName} • ${offering.dosage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PriceDisplay(price = offering.price, mrp = offering.mrp)
                            DiscountPercentPill(discountPercent = offering.discountPercent)
                        }
                        if (offering.requiresPrescription) {
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusPill(
                                text = stringResource(R.string.rx_required_badge),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (cartItem != null) {
                        QuantityStepper(
                            quantity = cartItem.quantity.toInt(),
                            onIncrement = onIncrement,
                            onDecrement = onDecrement
                        )
                    } else {
                        Button(
                            onClick = onAdd,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
