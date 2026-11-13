package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PharmacyCatalog
import com.example.model.CartItem
import com.example.model.DeliveryAddress
import com.example.model.KochiServiceArea
import com.example.model.MedicineOffering
import com.example.model.PaymentMethodOption
import com.example.model.PromoCatalog
import com.example.ui.components.AddAddressDialog
import com.example.ui.components.BillLineRow
import com.example.ui.components.CouponSelectorCard
import com.example.ui.components.CouponsBottomSheet
import com.example.ui.components.DeliveryInstructionsAndTipCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MedicineThumbnail
import com.example.ui.components.PaymentMethodSelectorCard
import com.example.ui.components.QuantityStepper
import com.example.ui.components.RecommendedAddOnsRow
import com.example.ui.components.SavingsHighlightBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatCurrency

@Composable
fun CartCheckoutScreen(
    cartItems: List<CartItem>,
    addresses: List<DeliveryAddress>,
    selectedAddressId: String?,
    userPhoneNumber: String? = null,
    prescriptionUri: String?,
    prescriptionLabel: String,
    appliedCouponCode: String = "",
    selectedTip: Int = 0,
    selectedPaymentMethod: PaymentMethodOption = PaymentMethodOption.UPI,
    selectedInstructions: Set<String> = emptySet(),
    customDeliveryNote: String = "",
    isSubmitting: Boolean,
    onSelectAddress: (String) -> Unit,
    onSaveAddress: (String, String, String, String, Boolean) -> Unit,
    onAttachPrescription: (String, String) -> Unit,
    onClearPrescription: () -> Unit,
    onApplyCoupon: (String) -> Unit = {},
    onClearCoupon: () -> Unit = {},
    onSelectTip: (Int) -> Unit = {},
    onSelectPaymentMethod: (PaymentMethodOption) -> Unit = {},
    onToggleInstruction: (String) -> Unit = {},
    onCustomDeliveryNoteChange: (String) -> Unit = {},
    onAddRecommendedItem: (MedicineOffering) -> Unit = {},
    onIncrementCartItem: (CartItem) -> Unit,
    onDecrementCartItem: (CartItem) -> Unit,
    onClearStoreCart: (List<CartItem>) -> Unit,
    onPlaceStoreOrder: (String, List<CartItem>, DeliveryAddress?) -> Unit,
    onBrowseStores: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showCouponsSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val label = uri.lastPathSegment?.substringAfterLast('/') ?: "prescription_photo.jpg"
                onAttachPrescription(uri.toString(), label)
            }
        }
    )

    val activeAddress = remember(addresses, selectedAddressId) {
        addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
    }
    val isAddressInKochi = KochiServiceArea.isKochiAddress(activeAddress)

    if (showAddAddressDialog) {
        AddAddressDialog(
            defaultPhone = userPhoneNumber.orEmpty(),
            onDismiss = { showAddAddressDialog = false },
            onSave = { label, fullAddress, landmark, phone, isDefault ->
                onSaveAddress(label, fullAddress, landmark, phone, isDefault)
                showAddAddressDialog = false
            }
        )
    }

    if (cartItems.isEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                EmptyStatePanel(
                    title = stringResource(R.string.empty_cart_title),
                    subtitle = stringResource(R.string.empty_cart_subtitle),
                    actionLabel = "Browse Medical Stores",
                    onAction = onBrowseStores
                )
            }
        }
        return
    }

    val groupedByStore = remember(cartItems) { cartItems.groupBy { it.storeId } }
    val anyRxRequired = remember(cartItems) { cartItems.any { it.requiresPrescription } }
    val maxBagSubtotal = remember(groupedByStore) {
        groupedByStore.values.maxOfOrNull { items -> items.sumOf { it.unitPrice * it.quantity } } ?: 0.0
    }
    val firstStoreId = remember(groupedByStore) { groupedByStore.keys.firstOrNull().orEmpty() }
    val recommendedAddOns = remember(cartItems, firstStoreId) {
        val inCartIds = cartItems.map { it.medicineId }.toSet()
        PharmacyCatalog.recommendedAddOns(firstStoreId, inCartIds)
    }
    val previewPromo = remember(appliedCouponCode, maxBagSubtotal, firstStoreId) {
        val baseFee = PharmacyCatalog.storeById(firstStoreId)?.deliveryFee ?: 15.0
        if (appliedCouponCode.isBlank()) null
        else PromoCatalog.calculate(appliedCouponCode, maxBagSubtotal, baseFee)
    }

    if (showCouponsSheet) {
        CouponsBottomSheet(
            appliedCouponCode = appliedCouponCode,
            maxBagSubtotal = maxBagSubtotal,
            onApplyCoupon = onApplyCoupon,
            onDismiss = { showCouponsSheet = false }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Prescription Upload Card (Zero-Permission Android Photo Picker)
        item(key = "rx_upload_card") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (anyRxRequired && prescriptionUri == null)
                        MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (prescriptionUri != null) {
                            AsyncImage(
                                model = prescriptionUri,
                                contentDescription = "Selected prescription photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_prescription_guide),
                                contentDescription = "Prescription upload guide",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Doctor Prescription",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (anyRxRequired) {
                                    StatusPill(
                                        text = "Required",
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (prescriptionUri != null)
                                    "Attached: $prescriptionLabel"
                                else
                                    "Select a clear prescription photo using the system Photo Picker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (prescriptionUri == null)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = if (prescriptionUri == null)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("checkout_attach_rx_button")
                        ) {
                            Icon(
                                imageVector = if (prescriptionUri != null) Icons.Filled.CheckCircle else Icons.Filled.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (prescriptionUri != null) "Change Rx Photo" else "Attach Rx Photo")
                        }

                        if (prescriptionUri != null) {
                            TextButton(
                                onClick = onClearPrescription,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }

        // Swiggy / Zomato Recommended OTC Add-ons
        if (recommendedAddOns.isNotEmpty()) {
            item(key = "recommended_otc_addons") {
                RecommendedAddOnsRow(
                    recommendations = recommendedAddOns,
                    onAddToCart = onAddRecommendedItem
                )
            }
        }

        // Swiggy / Zomato Coupon Selector Card
        item(key = "coupon_selector_card") {
            CouponSelectorCard(
                appliedCouponCode = appliedCouponCode,
                promoCalculation = previewPromo,
                onOpenCouponSheet = { showCouponsSheet = true },
                onClearCoupon = onClearCoupon
            )
        }

        // Delivery Instructions & Tip Your Delivery Partner
        item(key = "instructions_and_tip_card") {
            DeliveryInstructionsAndTipCard(
                selectedInstructions = selectedInstructions,
                customNote = customDeliveryNote,
                selectedTip = selectedTip,
                onToggleInstruction = onToggleInstruction,
                onCustomNoteChange = onCustomDeliveryNoteChange,
                onSelectTip = onSelectTip
            )
        }

        // Payment Method Selector
        item(key = "payment_method_card") {
            PaymentMethodSelectorCard(
                selectedMethod = selectedPaymentMethod,
                onSelectMethod = onSelectPaymentMethod
            )
        }

        // Delivery Address Selection
        item(key = "address_selection_card") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Deliver To",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        TextButton(
                            onClick = { showAddAddressDialog = true },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("checkout_add_address_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add New")
                        }
                    }

                    if (addresses.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = "Kochi Only (PIN 682xxx): Add a Kochi delivery address to complete your medicine purchase.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        addresses.forEach { addr ->
                            val isSelected = activeAddress?.id == addr.id
                            val addrInKochi = KochiServiceArea.isKochiAddress(addr)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onSelectAddress(addr.id) }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectAddress(addr.id) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${addr.label} • ${addr.contactPhone}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        StatusPill(
                                            text = if (addrInKochi) "Kochi Eligible" else "Outside Kochi",
                                            containerColor = if (addrInKochi)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.errorContainer,
                                            contentColor = if (addrInKochi)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = addr.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (!isAddressInKochi) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Service Restricted to Kochi Only: Your selected address is outside Kochi. Please select or add a Kochi address (PIN 682xxx) to purchase.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Per-Medical-Store Order Carts with Detailed Swiggy/Zomato Bill Breakdown
        groupedByStore.forEach { (storeId, storeItems) ->
            item(key = "store_bag_$storeId") {
                val store = PharmacyCatalog.storeById(storeId)
                val storeName = store?.name ?: storeItems.first().storeName
                val baseDeliveryFee = store?.deliveryFee ?: 15.0
                val etaMinutes = store?.etaMinutes ?: 30
                val subtotal = storeItems.sumOf { it.unitPrice * it.quantity }
                val mrpTotal = storeItems.sumOf { item ->
                    val unitMrp = PharmacyCatalog.mrpForMedicine(item.medicineId) ?: item.unitPrice
                    unitMrp * item.quantity
                }
                val mrpSavings = (mrpTotal - subtotal).coerceAtLeast(0.0)
                val promo = PromoCatalog.calculate(
                    couponCode = appliedCouponCode,
                    subtotal = subtotal,
                    baseDeliveryFee = baseDeliveryFee
                )
                val effectiveDeliveryFee = promo.effectiveDeliveryFee
                val feeWaivedSavings = (baseDeliveryFee - effectiveDeliveryFee).coerceAtLeast(0.0)
                val couponSavings = if (promo.isApplied) promo.discountAmount else 0.0
                val tipAmount = selectedTip.toDouble()
                val totalPayable = (subtotal - couponSavings + effectiveDeliveryFee + tipAmount).coerceAtLeast(0.0)
                val totalSavings = mrpSavings + couponSavings + feeWaivedSavings

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalPharmacy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = storeName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Est. delivery in $etaMinutes mins • ${selectedPaymentMethod.displayTitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onClearStoreCart(storeItems) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Clear store bag",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(10.dp))

                        storeItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                MedicineThumbnail(
                                    imageRes = PharmacyCatalog.imageForMedicine(item.medicineId),
                                    contentDescription = item.medicineName,
                                    size = 52.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.medicineName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.dosage} • ${formatCurrency(item.unitPrice)} each",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.requiresPrescription) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        StatusPill(
                                            text = stringResource(R.string.rx_required_badge),
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                QuantityStepper(
                                    quantity = item.quantity.toInt(),
                                    onIncrement = { onIncrementCartItem(item) },
                                    onDecrement = { onDecrementCartItem(item) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Spacer(modifier = Modifier.height(10.dp))

                        SavingsHighlightBanner(totalSavings = totalSavings)
                        if (totalSavings > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        BillLineRow(label = "Medicines Subtotal", value = formatCurrency(subtotal))
                        if (couponSavings > 0.0) {
                            BillLineRow(
                                label = "Promo Coupon (${promo.coupon?.code})",
                                value = "-${formatCurrency(couponSavings)}",
                                valueColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        BillLineRow(
                            label = "Courier Delivery Fee",
                            value = if (effectiveDeliveryFee == 0.0) "FREE" else formatCurrency(effectiveDeliveryFee),
                            valueColor = if (effectiveDeliveryFee == 0.0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            strikethroughValue = if (feeWaivedSavings > 0.0) formatCurrency(baseDeliveryFee) else null
                        )
                        if (tipAmount > 0.0) {
                            BillLineRow(
                                label = "Delivery Partner Tip",
                                value = formatCurrency(tipAmount)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Total Payable",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatCurrency(totalPayable),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MaterialTheme.typography.labelMedium.fontFamily
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onPlaceStoreOrder(storeId, storeItems, activeAddress) },
                            enabled = !isSubmitting && isAddressInKochi,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("place_order_button_$storeId")
                        ) {
                            val buttonLabel = when {
                                isSubmitting -> "Submitting Order…"
                                activeAddress == null -> "Add Kochi Address to Order"
                                !isAddressInKochi -> "Kochi Delivery Only — Change Address"
                                else -> "${stringResource(R.string.place_order)} • ${formatCurrency(totalPayable)}"
                            }
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
