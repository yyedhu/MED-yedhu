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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PharmacyCatalog
import com.example.model.CartItem
import com.example.model.CatalogSortOption
import com.example.model.MedicineOffering
import com.example.model.PromoCatalog
import com.example.ui.components.DiscountPercentPill
import com.example.ui.components.MedicineThumbnail
import com.example.ui.components.PriceDisplay
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StatusPill
import com.example.ui.components.StoreRatingBadge
import com.example.ui.components.formatCurrency

private enum class StoreMedicineFilter(val label: String) {
    ALL("All"),
    OTC_ONLY("No Rx Needed"),
    RX_ONLY("Rx Medicines")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    cartItems: List<CartItem>,
    isFavorite: Boolean = false,
    appliedCouponCode: String = "",
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onApplyCoupon: (String) -> Unit = {},
    onAddToCart: (MedicineOffering) -> Unit,
    onIncrementCartItem: (CartItem) -> Unit,
    onDecrementCartItem: (CartItem) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val store = remember(storeId) { PharmacyCatalog.storeById(storeId) }
    val allOfferings = remember(storeId) { PharmacyCatalog.offeringsForStore(storeId) }
    var storeQuery by remember { mutableStateOf("") }
    var rxFilter by remember { mutableStateOf(StoreMedicineFilter.ALL) }
    var sortOption by remember { mutableStateOf(CatalogSortOption.RELEVANCE) }

    val filteredOfferings = remember(allOfferings, storeQuery, rxFilter, sortOption) {
        val q = storeQuery.trim()
        val matched = allOfferings.filter { item ->
            val matchesQuery = q.isEmpty() ||
                item.name.contains(q, ignoreCase = true) ||
                item.genericName.contains(q, ignoreCase = true)
            val matchesRx = when (rxFilter) {
                StoreMedicineFilter.ALL -> true
                StoreMedicineFilter.OTC_ONLY -> !item.requiresPrescription
                StoreMedicineFilter.RX_ONLY -> item.requiresPrescription
            }
            matchesQuery && matchesRx
        }
        when (sortOption) {
            CatalogSortOption.RELEVANCE -> matched
            CatalogSortOption.PRICE_LOW_HIGH -> matched.sortedBy { it.price }
            CatalogSortOption.HIGHEST_DISCOUNT -> matched.sortedByDescending { it.discountPercent }
        }
    }

    val storeCartItems = remember(cartItems, storeId) { cartItems.filter { it.storeId == storeId } }
    val storeCartLookup = remember(storeCartItems) { storeCartItems.associateBy { it.medicineId } }
    val storeBagCount = remember(storeCartItems) { storeCartItems.sumOf { it.quantity }.toInt() }
    val storeBagSubtotal = remember(storeCartItems) { storeCartItems.sumOf { it.unitPrice * it.quantity } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = store?.name ?: "Medical Store",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("store_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to stores"
                        )
                    }
                },
                actions = {
                    if (store != null) {
                        IconButton(
                            onClick = { onToggleFavorite(store.id) },
                            modifier = Modifier.testTag("store_detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove favorite" else "Favorite store",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (storeBagCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "$storeBagCount item${if (storeBagCount == 1) "" else "s"} in bag",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${formatCurrency(storeBagSubtotal)} • ${store?.name ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onOpenCart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("view_bag_from_store_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Bag")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (store == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text("Medical store not found.")
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "store_hero_header") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Licensed pharmacy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "License ${store.licenseNumber} • ${store.neighborhood}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            StoreRatingBadge(rating = store.rating, reviewCount = store.reviewCount)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = "${store.etaMinutes} min ETA",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            StatusPill(
                                text = if (store.deliveryFee == 0.0) "Free Delivery" else "${formatCurrency(store.deliveryFee)} delivery",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            StatusPill(
                                text = "Min ${formatCurrency(store.minimumOrder)}",
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Store Offer Strip (Swiggy/Zomato Style)
            item(key = "store_offers_strip") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PromoCatalog.coupons, key = { it.code }) { coupon ->
                        val isApplied = appliedCouponCode.equals(coupon.code, ignoreCase = true)
                        Surface(
                            color = if (isApplied) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (isApplied) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isApplied) 1f else 0.35f)
                            ),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onApplyCoupon(coupon.code) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalOffer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = coupon.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isApplied) "Applied (${coupon.code})" else "Tap to apply ${coupon.code}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "store_search_box") {
                OutlinedTextField(
                    value = storeQuery,
                    onValueChange = { storeQuery = it },
                    placeholder = { Text("Search medicines in ${store.name}…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Rx / Sort Filter Row
            item(key = "store_filters_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StoreMedicineFilter.entries, key = { it.name }) { filter ->
                        FilterChip(
                            selected = rxFilter == filter,
                            onClick = { rxFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                    items(CatalogSortOption.entries, key = { "sort_${it.name}" }) { option ->
                        FilterChip(
                            selected = sortOption == option,
                            onClick = { sortOption = option },
                            label = { Text(option.label) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
            }

            items(
                items = filteredOfferings,
                key = { "${it.storeId}__${it.id}" },
                contentType = { "store_offering" }
            ) { offering ->
                val cartItem = storeCartLookup[offering.id]
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        MedicineThumbnail(
                            imageRes = offering.imageRes,
                            contentDescription = offering.name,
                            size = 72.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = offering.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (offering.requiresPrescription) {
                                    StatusPill(
                                        text = stringResource(R.string.rx_required_badge),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Text(
                                text = "${offering.genericName} • ${offering.dosage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = offering.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PriceDisplay(price = offering.price, mrp = offering.mrp)
                                DiscountPercentPill(discountPercent = offering.discountPercent)
                                Text(
                                    text = "${offering.stockCount} left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (cartItem != null) {
                            QuantityStepper(
                                quantity = cartItem.quantity.toInt(),
                                onIncrement = { onIncrementCartItem(cartItem) },
                                onDecrement = { onDecrementCartItem(cartItem) }
                            )
                        } else {
                            Button(
                                onClick = { onAddToCart(offering) },
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("add_med_${offering.id}")
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
}
