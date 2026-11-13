package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PharmacyCatalog
import com.example.model.CartItem
import com.example.model.CatalogSortOption
import com.example.model.MedicineCategory
import com.example.model.MedicineGroup
import com.example.model.MedicineOffering
import com.example.ui.components.DiscountPercentPill
import com.example.ui.components.MedicineThumbnail
import com.example.ui.components.PriceDisplay
import com.example.ui.components.QuantityStepper
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicinesCatalogScreen(
    searchQuery: String,
    selectedCategory: MedicineCategory,
    rxFreeOnly: Boolean = false,
    catalogSortOption: CatalogSortOption = CatalogSortOption.RELEVANCE,
    medicineGroups: List<MedicineGroup>,
    cartItems: List<CartItem>,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (MedicineCategory) -> Unit,
    onToggleRxFreeOnly: () -> Unit = {},
    onSelectCatalogSort: (CatalogSortOption) -> Unit = {},
    onAddToCart: (MedicineOffering) -> Unit,
    onIncrementCartItem: (CartItem) -> Unit,
    onDecrementCartItem: (CartItem) -> Unit,
    onOpenStore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeGroup by remember { mutableStateOf<MedicineGroup?>(null) }
    val cartLookup = remember(cartItems) {
        cartItems.associateBy { "${it.storeId}__${it.medicineId}" }
    }

    if (activeGroup != null) {
        val group = activeGroup!!
        ModalBottomSheet(
            onDismissRequest = { activeGroup = null }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MedicineThumbnail(
                        imageRes = group.imageRes,
                        contentDescription = group.name,
                        size = 76.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${group.genericName} • ${group.dosage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = group.manufacturer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                Text(
                    text = "Available at ${group.storeOfferings.size} Medical Store${if (group.storeOfferings.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium
                )

                group.storeOfferings.forEach { offering ->
                    val store = PharmacyCatalog.storeById(offering.storeId)
                    val cartItem = cartLookup["${offering.storeId}__${offering.id}"]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeGroup = null
                                        onOpenStore(offering.storeId)
                                    }
                            ) {
                                Text(
                                    text = offering.storeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${store?.etaMinutes ?: 25} min ETA • ${offering.stockCount} in stock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PriceDisplay(price = offering.price, mrp = offering.mrp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

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
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "All Medicines (${medicineGroups.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search medicine name or salt…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicines_catalog_search")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CatalogSortOption.entries, key = { "cat_sort_${it.name}" }) { option ->
                        FilterChip(
                            selected = catalogSortOption == option,
                            onClick = { onSelectCatalogSort(option) },
                            label = { Text(option.label) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = rxFreeOnly,
                            onClick = onToggleRxFreeOnly,
                            label = { Text("No Rx Needed") },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        items(
            items = medicineGroups,
            key = { it.id }
        ) { group ->
            val bestOffer = group.storeOfferings.first()
            val bestOfferCartItem = cartLookup["${bestOffer.storeId}__${bestOffer.id}"]

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable { activeGroup = group }
                    .testTag("medicine_grid_card_${group.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        Image(
                            painter = painterResource(id = group.imageRes),
                            contentDescription = group.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (group.maxDiscountPercent > 0) {
                            DiscountPercentPill(
                                discountPercent = group.maxDiscountPercent,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            )
                        }
                        if (group.requiresPrescription) {
                            StatusPill(
                                text = stringResource(R.string.rx_required_badge),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = group.dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${group.storeOfferings.size} store${if (group.storeOfferings.size == 1) "" else "s"} • From ${formatCurrency(group.lowestPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (bestOfferCartItem != null) {
                        QuantityStepper(
                            quantity = bestOfferCartItem.quantity.toInt(),
                            onIncrement = { onIncrementCartItem(bestOfferCartItem) },
                            onDecrement = { onDecrementCartItem(bestOfferCartItem) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Button(
                            onClick = { onAddToCart(bestOffer) },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("grid_add_med_${group.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add ${formatCurrency(bestOffer.price)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
