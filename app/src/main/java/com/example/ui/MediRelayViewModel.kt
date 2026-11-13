package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediRelayRepository
import com.example.data.PharmacyCatalog
import com.example.model.CartItem
import com.example.model.CatalogSortOption
import com.example.model.DeliveryAddress
import com.example.model.DeliveryTrackerEngine
import com.example.model.KochiServiceArea
import com.example.model.LiveOrderTracking
import com.example.model.MedicalStore
import com.example.model.MedicineCategory
import com.example.model.MedicineGroup
import com.example.model.MedicineOffering
import com.example.model.MedicineOrder
import com.example.model.OrderPayloadCodec
import com.example.model.OrderStatus
import com.example.model.PaymentMethodOption
import com.example.model.PromoCatalog
import com.example.model.StoreSortOption
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrderAlertEvent(
    val id: String,
    val orderId: String,
    val title: String,
    val message: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MediRelayViewModel(
    private val repository: MediRelayRepository,
    val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(MedicineCategory.ALL)
    val selectedCategory: StateFlow<MedicineCategory> = _selectedCategory.asStateFlow()

    private val _open24hOnly = MutableStateFlow(false)
    val open24hOnly: StateFlow<Boolean> = _open24hOnly.asStateFlow()

    private val _freeDeliveryOnly = MutableStateFlow(false)
    val freeDeliveryOnly: StateFlow<Boolean> = _freeDeliveryOnly.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _rxFreeOnly = MutableStateFlow(false)
    val rxFreeOnly: StateFlow<Boolean> = _rxFreeOnly.asStateFlow()

    private val _storeSortOption = MutableStateFlow(StoreSortOption.RELEVANCE)
    val storeSortOption: StateFlow<StoreSortOption> = _storeSortOption.asStateFlow()

    private val _catalogSortOption = MutableStateFlow(CatalogSortOption.RELEVANCE)
    val catalogSortOption: StateFlow<CatalogSortOption> = _catalogSortOption.asStateFlow()

    private val _appliedCouponCode = MutableStateFlow("")
    val appliedCouponCode: StateFlow<String> = _appliedCouponCode.asStateFlow()

    private val _riderTipAmount = MutableStateFlow(0)
    val riderTipAmount: StateFlow<Int> = _riderTipAmount.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethodOption.UPI)
    val selectedPaymentMethod: StateFlow<PaymentMethodOption> = _selectedPaymentMethod.asStateFlow()

    private val _selectedInstructions = MutableStateFlow<Set<String>>(emptySet())
    val selectedInstructions: StateFlow<Set<String>> = _selectedInstructions.asStateFlow()

    private val _customDeliveryNote = MutableStateFlow("")
    val customDeliveryNote: StateFlow<String> = _customDeliveryNote.asStateFlow()

    private val _prescriptionUri = MutableStateFlow<String?>(null)
    val prescriptionUri: StateFlow<String?> = _prescriptionUri.asStateFlow()

    private val _prescriptionLabel = MutableStateFlow("")
    val prescriptionLabel: StateFlow<String> = _prescriptionLabel.asStateFlow()

    private val _selectedAddressId = MutableStateFlow<String?>(null)
    val selectedAddressId: StateFlow<String?> = _selectedAddressId.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _orderAlertEvent = MutableStateFlow<OrderAlertEvent?>(null)
    val orderAlertEvent: StateFlow<OrderAlertEvent?> = _orderAlertEvent.asStateFlow()

    private val _liveTransitProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val liveTransitProgress: StateFlow<Map<String, Float>> = _liveTransitProgress.asStateFlow()

    private val fulfillmentJobs = mutableMapOf<String, Job>()
    private val notifiedMilestones = mutableSetOf<String>()

    val cartState: StateFlow<UiState<List<CartItem>>> = repository.observeCartItems(currentUserId)
        .map<List<CartItem>, UiState<List<CartItem>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreOrThrowCancellation(error)
            Log.w(TAG, "Error observing cart items", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load cart"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val addressesState: StateFlow<UiState<List<DeliveryAddress>>> = repository.observeAddresses(currentUserId)
        .map<List<DeliveryAddress>, UiState<List<DeliveryAddress>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreOrThrowCancellation(error)
            Log.w(TAG, "Error observing addresses", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load addresses"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val ordersState: StateFlow<UiState<List<MedicineOrder>>> = repository.observeOrders(currentUserId)
        .map<List<MedicineOrder>, UiState<List<MedicineOrder>>> { UiState.Success(it) }
        .catch { error ->
            val rootError = unwrapFirestoreOrThrowCancellation(error)
            Log.w(TAG, "Error observing orders", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load orders"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val liveTrackingsState: StateFlow<Map<String, LiveOrderTracking>> = repository.observeLiveTrackings(currentUserId)
        .catch { error ->
            val rootError = unwrapFirestoreOrThrowCancellation(error)
            Log.w(TAG, "Error observing live order trackings", rootError)
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val favoriteStoreIdsState: StateFlow<Set<String>> = repository.observeFavoriteStoreIds(currentUserId)
        .catch { error ->
            val rootError = unwrapFirestoreOrThrowCancellation(error)
            Log.w(TAG, "Error observing favorite store ids", rootError)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    init {
        viewModelScope.launch {
            ordersState.collect { state ->
                if (state is UiState.Success) {
                    syncActiveOrderTracking(state.data)
                }
            }
        }
    }

    private data class StoreFilterParams(
        val query: String,
        val only24h: Boolean,
        val freeDeliveryOnly: Boolean,
        val favoritesOnly: Boolean,
        val sortOption: StoreSortOption,
        val favoriteIds: Set<String>
    )

    val filteredStores: StateFlow<List<MedicalStore>> = combine(
        combine(_searchQuery, _open24hOnly, _freeDeliveryOnly, _favoritesOnly) { q, o24, free, fav ->
            StoreFilterParams(q, o24, free, fav, StoreSortOption.RELEVANCE, emptySet())
        },
        _storeSortOption,
        favoriteStoreIdsState
    ) { params, sort, favIds ->
        params.copy(sortOption = sort, favoriteIds = favIds)
    }.map { params ->
        val matched = PharmacyCatalog.stores.filter { store ->
            val matches24h = !params.only24h || store.isOpen24Hours
            val matchesFree = !params.freeDeliveryOnly || store.deliveryFee == 0.0
            val matchesFav = !params.favoritesOnly || store.id in params.favoriteIds
            val matchesQuery = PharmacyCatalog.matchesStoreQuery(store, params.query)
            matches24h && matchesFree && matchesFav && matchesQuery
        }
        when (params.sortOption) {
            StoreSortOption.RELEVANCE -> matched
            StoreSortOption.FASTEST -> matched.sortedBy { it.etaMinutes }
            StoreSortOption.TOP_RATED -> matched.sortedWith(
                compareByDescending<MedicalStore> { it.rating }.thenByDescending { it.reviewCount }
            )
            StoreSortOption.LOW_FEE -> matched.sortedBy { it.deliveryFee }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PharmacyCatalog.stores
    )

    val filteredOfferings: StateFlow<List<MedicineOffering>> = combine(
        _searchQuery,
        _selectedCategory,
        _open24hOnly,
        _rxFreeOnly,
        _catalogSortOption
    ) { query, category, only24h, rxFreeOnly, sortOption ->
        val base = PharmacyCatalog.searchOfferings(query, category).filter { item ->
            (!only24h || PharmacyCatalog.isStoreOpen24h(item.storeId)) &&
                (!rxFreeOnly || !item.requiresPrescription)
        }
        when (sortOption) {
            CatalogSortOption.RELEVANCE -> base
            CatalogSortOption.PRICE_LOW_HIGH -> base.sortedBy { it.price }
            CatalogSortOption.HIGHEST_DISCOUNT -> base.sortedByDescending { it.discountPercent }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PharmacyCatalog.offerings
    )

    val filteredMedicineGroups: StateFlow<List<MedicineGroup>> = combine(
        _searchQuery,
        _selectedCategory,
        _open24hOnly,
        _rxFreeOnly,
        _catalogSortOption
    ) { query, category, only24h, rxFreeOnly, sortOption ->
        val groups = PharmacyCatalog.distinctMedicines(query, category).mapNotNull { group ->
            if (rxFreeOnly && group.requiresPrescription) return@mapNotNull null
            if (!only24h) group
            else {
                val openOffers = group.storeOfferings.filter { PharmacyCatalog.isStoreOpen24h(it.storeId) }
                if (openOffers.isEmpty()) null else group.copy(storeOfferings = openOffers)
            }
        }
        when (sortOption) {
            CatalogSortOption.RELEVANCE -> groups
            CatalogSortOption.PRICE_LOW_HIGH -> groups.sortedBy { it.lowestPrice }
            CatalogSortOption.HIGHEST_DISCOUNT -> groups.sortedByDescending { it.maxDiscountPercent }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = PharmacyCatalog.distinctMedicines()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: MedicineCategory) {
        _selectedCategory.value = category
    }

    fun toggleOpen24hOnly() {
        _open24hOnly.value = !_open24hOnly.value
    }

    fun toggleFreeDeliveryOnly() {
        _freeDeliveryOnly.value = !_freeDeliveryOnly.value
    }

    fun toggleFavoritesOnly() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun toggleRxFreeOnly() {
        _rxFreeOnly.value = !_rxFreeOnly.value
    }

    fun setStoreSortOption(option: StoreSortOption) {
        _storeSortOption.value = option
    }

    fun setCatalogSortOption(option: CatalogSortOption) {
        _catalogSortOption.value = option
    }

    fun applyCoupon(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isEmpty()) {
            clearCoupon()
            return
        }
        val coupon = PromoCatalog.findByCode(code)
        if (coupon == null) {
            _feedbackMessage.value = "Invalid coupon code $code. Try KOCHI20, MEDFREE, or CARE50."
            return
        }
        _appliedCouponCode.value = coupon.code
        _feedbackMessage.value = "${coupon.code} selected (${coupon.title})."
    }

    fun clearCoupon() {
        _appliedCouponCode.value = ""
    }

    fun selectRiderTip(amount: Int) {
        _riderTipAmount.value = amount.coerceIn(0, 500)
    }

    fun selectPaymentMethod(method: PaymentMethodOption) {
        _selectedPaymentMethod.value = method
    }

    fun toggleDeliveryInstruction(instruction: String) {
        val current = _selectedInstructions.value
        _selectedInstructions.value = if (instruction in current) current - instruction else current + instruction
    }

    fun updateCustomDeliveryNote(note: String) {
        if (note.length <= 100) {
            _customDeliveryNote.value = note
        }
    }

    fun toggleFavoriteStore(storeId: String) {
        val isFav = storeId in favoriteStoreIdsState.value
        viewModelScope.launch {
            val res = repository.toggleFavoriteStore(storeId, isFav)
            res.onSuccess { nowFav ->
                val storeName = PharmacyCatalog.storeById(storeId)?.name ?: "Store"
                _feedbackMessage.value = if (nowFav) {
                    "Saved $storeName to Favorites"
                } else {
                    "Removed $storeName from Favorites"
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not update favorite store."
            }
        }
    }

    fun selectAddress(addressId: String) {
        _selectedAddressId.value = addressId
    }

    fun attachPrescription(uriString: String, fileName: String) {
        _prescriptionUri.value = uriString
        _prescriptionLabel.value = fileName.ifBlank { "rx_upload.jpg" }
        _feedbackMessage.value = "Prescription attached to your order bag."
    }

    fun clearPrescription() {
        _prescriptionUri.value = null
        _prescriptionLabel.value = ""
    }

    fun consumeFeedback() {
        _feedbackMessage.value = null
    }

    fun consumeOrderAlert() {
        _orderAlertEvent.value = null
    }

    fun addToCart(offering: MedicineOffering) {
        viewModelScope.launch {
            val result = repository.addToCart(offering)
            if (result.isSuccess) {
                _feedbackMessage.value = "Added ${offering.name} (${offering.storeName})"
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Could not add item to cart."
            }
        }
    }

    fun incrementCartItem(item: CartItem) {
        viewModelScope.launch {
            val res = repository.updateCartQuantity(item.id, item.quantity + 1L)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update quantity."
            }
        }
    }

    fun decrementCartItem(item: CartItem) {
        viewModelScope.launch {
            val res = repository.updateCartQuantity(item.id, item.quantity - 1L)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update quantity."
            }
        }
    }

    fun clearStoreCart(storeItems: List<CartItem>) {
        viewModelScope.launch {
            val res = repository.clearCartForStore(storeItems)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to clear store bag."
            }
        }
    }

    fun saveAddress(
        label: String,
        fullAddress: String,
        landmark: String,
        contactPhone: String,
        isDefault: Boolean,
        existingId: String? = null
    ) {
        if (!KochiServiceArea.isKochiAddress(fullAddress, landmark)) {
            _feedbackMessage.value = "Only Kochi addresses (PIN 682xxx) are eligible for MediRelay delivery."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.saveAddress(
                label = label,
                fullAddress = fullAddress,
                landmark = landmark,
                contactPhone = contactPhone,
                isDefault = isDefault,
                existingId = existingId
            )
            _isSubmitting.value = false
            res.onSuccess { newId ->
                _selectedAddressId.value = newId
                _feedbackMessage.value = "Kochi delivery address saved."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to save address."
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            val res = repository.deleteAddress(addressId)
            if (res.isSuccess) {
                if (_selectedAddressId.value == addressId) {
                    _selectedAddressId.value = null
                }
                _feedbackMessage.value = "Address removed."
            } else {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not delete address."
            }
        }
    }

    fun placeStoreOrder(
        storeId: String,
        storeItems: List<CartItem>,
        deliveryAddress: DeliveryAddress?,
        onOrderPlaced: (String) -> Unit
    ) {
        if (storeItems.isEmpty()) return
        if (deliveryAddress == null) {
            _feedbackMessage.value = "Please select or add a Kochi delivery address first."
            return
        }
        if (!KochiServiceArea.isKochiAddress(deliveryAddress)) {
            _feedbackMessage.value = "Purchase unavailable: MediRelay delivers exclusively within Kochi (PIN 682xxx)."
            return
        }
        val needsRx = storeItems.any { it.requiresPrescription }
        val hasRx = _prescriptionUri.value != null
        if (needsRx && !hasRx) {
            _feedbackMessage.value = "One or more medicines require a prescription. Tap Attach Rx to add one."
            return
        }

        val store = PharmacyCatalog.storeById(storeId)
        val storeName = store?.name ?: storeItems.first().storeName
        val baseDeliveryFee = store?.deliveryFee ?: 15.0
        val subtotal = storeItems.sumOf { it.unitPrice * it.quantity }
        val promo = PromoCatalog.calculate(
            couponCode = _appliedCouponCode.value,
            subtotal = subtotal,
            baseDeliveryFee = baseDeliveryFee
        )
        val effectiveCoupon = if (promo.isApplied) promo.coupon?.code.orEmpty() else ""
        val effectiveDiscount = if (promo.isApplied) promo.discountAmount else 0.0
        val effectiveDeliveryFee = promo.effectiveDeliveryFee
        val tipAmount = _riderTipAmount.value.toDouble()
        val paymentMethod = _selectedPaymentMethod.value.wireValue
        val combinedInstructions = buildList {
            addAll(_selectedInstructions.value)
            val custom = _customDeliveryNote.value.trim()
            if (custom.isNotEmpty()) add(custom)
        }.joinToString(" • ").take(160)

        val eta = store?.etaMinutes ?: 30
        val formattedAddress = buildString {
            append(deliveryAddress.label)
            append(": ")
            append(deliveryAddress.fullAddress)
            if (deliveryAddress.landmark.isNotBlank()) {
                append(" (")
                append(deliveryAddress.landmark)
                append(")")
            }
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.placeOrder(
                storeId = storeId,
                storeName = storeName,
                storeItems = storeItems,
                deliveryFee = effectiveDeliveryFee,
                deliveryAddress = formattedAddress,
                hasPrescription = hasRx,
                prescriptionFileName = _prescriptionLabel.value,
                estimatedMinutes = eta,
                discountAmount = effectiveDiscount,
                couponCode = effectiveCoupon,
                tipAmount = tipAmount,
                paymentMethod = paymentMethod,
                deliveryInstructions = combinedInstructions
            )
            _isSubmitting.value = false
            res.onSuccess { orderId ->
                _prescriptionUri.value = null
                _prescriptionLabel.value = ""
                _appliedCouponCode.value = ""
                _riderTipAmount.value = 0
                _selectedInstructions.value = emptySet()
                _customDeliveryNote.value = ""
                _feedbackMessage.value = "Order placed with $storeName!"
                onOrderPlaced(orderId)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Failed to place order."
            }
        }
    }

    fun reorderPastOrder(order: MedicineOrder, onComplete: () -> Unit) {
        val decodedPairs = OrderPayloadCodec.decode(order.itemsPayload)
        val resolvedOfferings: List<Pair<MedicineOffering, Long>> = if (decodedPairs.isNotEmpty()) {
            decodedPairs.mapNotNull { (medId, qty) ->
                PharmacyCatalog.offeringFor(order.storeId, medId)?.let { it to qty }
            }
        } else {
            val storeOfferings = PharmacyCatalog.offeringsForStore(order.storeId)
            order.itemsSummary.split(",")
                .mapNotNull { segment ->
                    val namePart = segment.substringBefore(" x").trim()
                    val qty = segment.substringAfterLast(" x", "1").trim().toLongOrNull() ?: 1L
                    storeOfferings.firstOrNull {
                        it.name.equals(namePart, ignoreCase = true)
                    }?.let { it to qty }
                }
        }
        if (resolvedOfferings.isEmpty()) {
            _feedbackMessage.value = "Items from this past order are currently unavailable at ${order.storeName}."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.addBatchToCart(resolvedOfferings)
            _isSubmitting.value = false
            res.onSuccess { count ->
                _feedbackMessage.value = "Added $count medicine${if (count == 1) "" else "s"} from ${order.storeName} to bag"
                onComplete()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not reorder items."
            }
        }
    }

    fun submitOrderRating(orderId: String, rating: Int, reviewComment: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitOrderRating(orderId, rating, reviewComment)
            _isSubmitting.value = false
            res.onSuccess {
                _feedbackMessage.value = "Thanks for rating your Kochi delivery ($rating★)!"
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not save your rating."
            }
        }
    }

    fun cancelOrder(order: MedicineOrder) {
        val current = OrderStatus.fromWire(order.status)
        if (current != OrderStatus.PLACED) {
            _feedbackMessage.value = "Cannot cancel: ${order.storeName} has already started preparing your order."
            return
        }
        fulfillmentJobs.remove(order.id)?.cancel()
        val (storeLat, storeLng) = DeliveryTrackerEngine.storeCoordinates(order.storeId)
        viewModelScope.launch {
            val res = repository.updateOrderStatus(order.id, OrderStatus.CANCELLED)
            if (res.isSuccess) {
                repository.updateOrderTracking(
                    orderId = order.id,
                    status = OrderStatus.CANCELLED,
                    progressFraction = 0.0,
                    partnerLat = storeLat,
                    partnerLng = storeLng
                )
                _feedbackMessage.value = "Order cancelled."
            } else {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not cancel order."
            }
        }
    }

    private fun syncActiveOrderTracking(orders: List<MedicineOrder>) {
        val activeIds = orders
            .filter { !OrderStatus.fromWire(it.status).isTerminal }
            .map { it.id }
            .toSet()

        // Cancel background jobs for orders that became terminal
        val toRemove = fulfillmentJobs.keys.filter { it !in activeIds }
        toRemove.forEach { id ->
            fulfillmentJobs.remove(id)?.cancel()
            _liveTransitProgress.value = _liveTransitProgress.value - id
        }

        orders.filter { it.id in activeIds }.forEach { order ->
            if (fulfillmentJobs[order.id]?.isActive != true) {
                fulfillmentJobs[order.id] = viewModelScope.launch {
                    runShopAndRiderFulfillment(order)
                }
            }
        }
    }

    private suspend fun runShopAndRiderFulfillment(initialOrder: MedicineOrder) {
        val orderId = initialOrder.id
        var currentStatus = OrderStatus.fromWire(initialOrder.status)
        val partner = DeliveryTrackerEngine.assignedPartnerForOrder(initialOrder)
        val destinationArea = DeliveryTrackerEngine.extractDestinationZone(initialOrder.deliveryAddress)
        val destZone = DeliveryTrackerEngine.extractDestinationZoneInfo(initialOrder.deliveryAddress)
        val (storeLat, storeLng) = DeliveryTrackerEngine.storeCoordinates(initialOrder.storeId)
        val destLat = destZone.latitude
        val destLng = destZone.longitude
        val corridor = DeliveryTrackerEngine.resolveCorridorForOrder(
            storeId = initialOrder.storeId,
            deliveryAddress = initialOrder.deliveryAddress
        )

        repository.ensureOrderTracking(initialOrder)

        // 1. Medical shop confirms the order
        if (currentStatus == OrderStatus.PLACED) {
            delay(4500L)
            if (repository.updateOrderStatus(orderId, OrderStatus.CONFIRMED).isFailure) return
            repository.updateOrderTracking(
                orderId = orderId,
                status = OrderStatus.CONFIRMED,
                progressFraction = 0.0,
                partnerLat = storeLat,
                partnerLng = storeLng,
                transitCorridor = corridor
            )
            currentStatus = OrderStatus.CONFIRMED
        }

        // 2. Medical shop packs & seals the medicines -> Notify customer!
        if (currentStatus == OrderStatus.CONFIRMED) {
            delay(4500L)
            if (repository.updateOrderStatus(orderId, OrderStatus.PACKED).isFailure) return
            repository.updateOrderTracking(
                orderId = orderId,
                status = OrderStatus.PACKED,
                progressFraction = 0.0,
                partnerLat = storeLat,
                partnerLng = storeLng,
                transitCorridor = corridor
            )
            currentStatus = OrderStatus.PACKED
            emitCustomerMilestoneAlert(
                milestoneKey = "${orderId}_packed",
                orderId = orderId,
                title = "Packed by ${initialOrder.storeName}",
                message = "Your medicines are packed & sealed. ${partner.name} is picking them up."
            )
        }

        // 3. Delivery partner picks up item -> Start continuous Firestore GPS updates for OpenFreeMap!
        if (currentStatus == OrderStatus.PACKED) {
            delay(4000L)
            if (repository.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY).isFailure) return
            val (startLat, startLng) = DeliveryTrackerEngine.interpolateRoutePosition(
                storeLat = storeLat,
                storeLng = storeLng,
                destLat = destLat,
                destLng = destLng,
                fraction = 0.05f
            )
            repository.updateOrderTracking(
                orderId = orderId,
                status = OrderStatus.OUT_FOR_DELIVERY,
                progressFraction = 0.05,
                partnerLat = startLat,
                partnerLng = startLng,
                transitCorridor = corridor
            )
            currentStatus = OrderStatus.OUT_FOR_DELIVERY
            emitCustomerMilestoneAlert(
                milestoneKey = "${orderId}_pickup",
                orderId = orderId,
                title = "Rider Picked Up • GPS Live",
                message = "${partner.name} picked up your order from ${initialOrder.storeName} and is heading to $destinationArea."
            )
        }

        // 4. Continuously write delivery partner GPS coordinates to Firestore until arrival
        if (currentStatus == OrderStatus.OUT_FOR_DELIVERY) {
            val steps = 20
            val savedFraction = liveTrackingsState.value[orderId]?.progressFraction?.toFloat()
                ?: _liveTransitProgress.value[orderId]
                ?: 0.05f
            val startStep = (savedFraction * steps).toInt().coerceIn(1, steps)
            for (step in startStep..steps) {
                val fraction = (step.toFloat() / steps).coerceIn(0f, 1f)
                val (lat, lng) = DeliveryTrackerEngine.interpolateRoutePosition(
                    storeLat = storeLat,
                    storeLng = storeLng,
                    destLat = destLat,
                    destLng = destLng,
                    fraction = fraction
                )
                _liveTransitProgress.value = _liveTransitProgress.value + (orderId to fraction)
                repository.updateOrderTracking(
                    orderId = orderId,
                    status = OrderStatus.OUT_FOR_DELIVERY,
                    progressFraction = fraction.toDouble(),
                    partnerLat = lat,
                    partnerLng = lng,
                    transitCorridor = corridor
                )
                if (fraction >= 0.96f) {
                    emitCustomerMilestoneAlert(
                        milestoneKey = "${orderId}_arrived",
                        orderId = orderId,
                        title = "Rider Arrived at Your Location!",
                        message = "${partner.name} has arrived at $destinationArea, Kochi with your medicine order."
                    )
                }
                delay(900L)
            }
            delay(1800L)
            if (repository.updateOrderStatus(orderId, OrderStatus.DELIVERED).isSuccess) {
                repository.updateOrderTracking(
                    orderId = orderId,
                    status = OrderStatus.DELIVERED,
                    progressFraction = 1.0,
                    partnerLat = destLat,
                    partnerLng = destLng,
                    transitCorridor = corridor
                )
            }
            _liveTransitProgress.value = _liveTransitProgress.value - orderId
        }
    }

    private fun emitCustomerMilestoneAlert(
        milestoneKey: String,
        orderId: String,
        title: String,
        message: String
    ) {
        if (!notifiedMilestones.add(milestoneKey)) return
        _orderAlertEvent.value = OrderAlertEvent(
            id = milestoneKey,
            orderId = orderId,
            title = title,
            message = message
        )
    }

    override fun onCleared() {
        super.onCleared()
        fulfillmentJobs.values.forEach { it.cancel() }
        fulfillmentJobs.clear()
    }

    private fun unwrapFirestoreOrThrowCancellation(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "MediRelayVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
