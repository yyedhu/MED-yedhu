package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CartItem
import com.example.model.DeliveryAddress
import com.example.model.DeliveryTrackerEngine
import com.example.model.LiveOrderTracking
import com.example.model.MedicineOffering
import com.example.model.MedicineOrder
import com.example.model.OrderPayloadCodec
import com.example.model.OrderStatus
import com.example.model.PaymentMethodOption
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MediRelayRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing MediRelay.")
    }

    fun observeCartItems(userId: String): Flow<List<CartItem>> {
        val path = "users/$userId/cartItems"
        return db.collection("users")
            .document(userId)
            .collection("cartItems")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CartItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.medicineName }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeAddresses(userId: String): Flow<List<DeliveryAddress>> {
        val path = "users/$userId/addresses"
        return db.collection("users")
            .document(userId)
            .collection("addresses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DeliveryAddress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.isDefault }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeOrders(userId: String): Flow<List<MedicineOrder>> {
        val path = "users/$userId/orders"
        return db.collection("users")
            .document(userId)
            .collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MedicineOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeLiveTrackings(userId: String): Flow<Map<String, LiveOrderTracking>> {
        val path = "users/$userId/tracking"
        return db.collection("users")
            .document(userId)
            .collection("tracking")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LiveOrderTracking::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.orderId.ifBlank { it.id } }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeFavoriteStoreIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/favorites"
        return db.collection("users")
            .document(userId)
            .collection("favorites")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.getString("storeId") ?: it.id }.toSet()
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    suspend fun toggleFavoriteStore(storeId: String, currentlyFavorite: Boolean): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val favRef = db.collection("users")
            .document(uid)
            .collection("favorites")
            .document(storeId)
        return try {
            if (currentlyFavorite) {
                favRef.delete().await()
                Result.success(false)
            } else {
                favRef.set(
                    mapOf(
                        "userId" to uid,
                        "storeId" to storeId,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, favRef.path)
            Result.failure(e)
        }
    }

    suspend fun addToCart(offering: MedicineOffering): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = "${offering.storeId}__${offering.id}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("cartItems")
            .document(docId)

        return try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val currentQty = existingSnap.getLong("quantity") ?: 1L
                val nextQty = (currentQty + 1L).coerceAtMost(50L)
                docRef.update(
                    mapOf(
                        "quantity" to nextQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "storeId" to offering.storeId,
                    "storeName" to offering.storeName,
                    "medicineId" to offering.id,
                    "medicineName" to offering.name,
                    "dosage" to offering.dosage,
                    "unitPrice" to offering.price,
                    "quantity" to 1L,
                    "requiresPrescription" to offering.requiresPrescription,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("cartItems")
            .document(cartItemId)

        return try {
            if (newQuantity <= 0L) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity.coerceAtMost(50L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun clearCartForStore(items: List<CartItem>): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (items.isEmpty()) return Result.success(Unit)
        return try {
            val batch = db.batch()
            items.forEach { item ->
                if (item.id.isNotBlank()) {
                    val ref = db.collection("users")
                        .document(uid)
                        .collection("cartItems")
                        .document(item.id)
                    batch.delete(ref)
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/cartItems")
            Result.failure(e)
        }
    }

    suspend fun addBatchToCart(items: List<Pair<MedicineOffering, Long>>): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (items.isEmpty()) return Result.success(0)
        return try {
            val cartCol = db.collection("users").document(uid).collection("cartItems")
            val batch = db.batch()
            var count = 0
            for ((offering, qty) in items) {
                val docId = "${offering.storeId}__${offering.id}"
                val docRef = cartCol.document(docId)
                val existing = docRef.get().await()
                if (existing.exists()) {
                    val currentQty = existing.getLong("quantity") ?: 0L
                    val nextQty = (currentQty + qty).coerceIn(1L, 50L)
                    batch.update(
                        docRef,
                        mapOf(
                            "quantity" to nextQty,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "storeId" to offering.storeId,
                            "storeName" to offering.storeName,
                            "medicineId" to offering.id,
                            "medicineName" to offering.name,
                            "dosage" to offering.dosage,
                            "unitPrice" to offering.price,
                            "quantity" to qty.coerceIn(1L, 50L),
                            "requiresPrescription" to offering.requiresPrescription,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                count++
            }
            batch.commit().await()
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/cartItems")
            Result.failure(e)
        }
    }

    suspend fun saveAddress(
        label: String,
        fullAddress: String,
        landmark: String,
        contactPhone: String,
        isDefault: Boolean,
        existingId: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docId = existingId?.takeIf { it.isNotBlank() } ?: "addr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("addresses")
            .document(docId)

        return try {
            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "label" to label.trim(),
                    "fullAddress" to fullAddress.trim(),
                    "landmark" to landmark.trim(),
                    "contactPhone" to contactPhone.trim(),
                    "isDefault" to isDefault,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "label" to label.trim(),
                    "fullAddress" to fullAddress.trim(),
                    "landmark" to landmark.trim(),
                    "contactPhone" to contactPhone.trim(),
                    "isDefault" to isDefault,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users")
            .document(uid)
            .collection("addresses")
            .document(addressId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        storeId: String,
        storeName: String,
        storeItems: List<CartItem>,
        deliveryFee: Double,
        deliveryAddress: String,
        hasPrescription: Boolean,
        prescriptionFileName: String,
        estimatedMinutes: Int,
        discountAmount: Double = 0.0,
        couponCode: String = "",
        tipAmount: Double = 0.0,
        paymentMethod: String = PaymentMethodOption.UPI.wireValue,
        deliveryInstructions: String = ""
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (storeItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cannot place an empty store order."))
        }
        val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val orderRef = db.collection("users")
            .document(uid)
            .collection("orders")
            .document(orderId)

        val subtotal = storeItems.sumOf { it.unitPrice * it.quantity }
        val sanitizedDiscount = discountAmount.coerceIn(0.0, subtotal)
        val sanitizedTip = tipAmount.coerceIn(0.0, 2000.0)
        val totalAmount = (subtotal - sanitizedDiscount + deliveryFee + sanitizedTip).coerceAtLeast(0.0)
        val itemCount = storeItems.sumOf { it.quantity }.coerceIn(1L, 100L)
        val itemsSummary = storeItems.joinToString(", ") { "${it.medicineName} x${it.quantity}" }.take(390)
        val itemsPayload = OrderPayloadCodec.encode(storeItems)
        val sanitizedPayment = PaymentMethodOption.fromWire(paymentMethod).wireValue

        val payload = mapOf(
            "userId" to uid,
            "storeId" to storeId,
            "storeName" to storeName,
            "itemsSummary" to itemsSummary,
            "itemsPayload" to itemsPayload,
            "itemCount" to itemCount,
            "subtotal" to subtotal,
            "deliveryFee" to deliveryFee,
            "discountAmount" to sanitizedDiscount,
            "couponCode" to couponCode.trim().uppercase().take(32),
            "tipAmount" to sanitizedTip,
            "totalAmount" to totalAmount,
            "paymentMethod" to sanitizedPayment,
            "deliveryAddress" to deliveryAddress.trim().take(260),
            "deliveryInstructions" to deliveryInstructions.trim().take(160),
            "status" to OrderStatus.PLACED.wireValue,
            "hasPrescription" to hasPrescription,
            "prescriptionFileName" to prescriptionFileName.trim().take(160),
            "estimatedMinutes" to estimatedMinutes.coerceIn(5, 240).toLong(),
            "rating" to 0L,
            "reviewComment" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val trackingRef = db.collection("users")
            .document(uid)
            .collection("tracking")
            .document(orderId)
        val trackingPayload = buildTrackingCreatePayload(
            uid = uid,
            orderId = orderId,
            storeId = storeId,
            storeName = storeName,
            deliveryAddress = deliveryAddress,
            status = OrderStatus.PLACED
        )

        return try {
            val batch = db.batch()
            batch.set(orderRef, payload)
            batch.set(trackingRef, trackingPayload)
            storeItems.forEach { item ->
                if (item.id.isNotBlank()) {
                    val cartRef = db.collection("users")
                        .document(uid)
                        .collection("cartItems")
                        .document(item.id)
                    batch.delete(cartRef)
                }
            }
            batch.commit().await()
            Result.success(orderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun getOrderById(orderId: String): Result<MedicineOrder?> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val orderRef = db.collection("users")
            .document(uid)
            .collection("orders")
            .document(orderId)
        return try {
            val snap = orderRef.get().await()
            Result.success(
                snap.toObject(
                    MedicineOrder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, nextStatus: OrderStatus): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val orderRef = db.collection("users")
            .document(uid)
            .collection("orders")
            .document(orderId)

        return try {
            orderRef.update(
                mapOf(
                    "status" to nextStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitOrderRating(
        orderId: String,
        rating: Int,
        reviewComment: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val orderRef = db.collection("users")
            .document(uid)
            .collection("orders")
            .document(orderId)
        return try {
            orderRef.update(
                mapOf(
                    "rating" to rating.coerceIn(1, 5).toLong(),
                    "reviewComment" to reviewComment.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureOrderTracking(order: MedicineOrder): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val trackRef = db.collection("users")
            .document(uid)
            .collection("tracking")
            .document(order.id)
        return try {
            val existing = trackRef.get().await()
            if (!existing.exists()) {
                val payload = buildTrackingCreatePayload(
                    uid = uid,
                    orderId = order.id,
                    storeId = order.storeId,
                    storeName = order.storeName,
                    deliveryAddress = order.deliveryAddress,
                    status = OrderStatus.fromWire(order.status)
                )
                trackRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, trackRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderTracking(
        orderId: String,
        status: OrderStatus,
        progressFraction: Double,
        partnerLat: Double,
        partnerLng: Double,
        transitCorridor: String? = null
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val trackRef = db.collection("users")
            .document(uid)
            .collection("tracking")
            .document(orderId)

        val updates = mutableMapOf<String, Any>(
            "status" to status.wireValue,
            "progressFraction" to progressFraction.coerceIn(0.0, 1.0),
            "partnerLat" to partnerLat.coerceIn(-90.0, 90.0),
            "partnerLng" to partnerLng.coerceIn(-180.0, 180.0),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!transitCorridor.isNullOrBlank()) {
            updates["transitCorridor"] = transitCorridor.take(120)
        }

        return try {
            trackRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, trackRef.path)
            Result.failure(e)
        }
    }

    private fun buildTrackingCreatePayload(
        uid: String,
        orderId: String,
        storeId: String,
        storeName: String,
        deliveryAddress: String,
        status: OrderStatus
    ): Map<String, Any> {
        val store = PharmacyCatalog.storeById(storeId)
        val storeNeighborhood = (store?.neighborhood ?: "Ernakulam, Kochi").take(100)
        val destZone = DeliveryTrackerEngine.extractDestinationZoneInfo(deliveryAddress)
        val destinationArea = DeliveryTrackerEngine.extractDestinationZone(deliveryAddress).take(100)
        val corridor = DeliveryTrackerEngine.resolveCorridorForOrder(storeId, deliveryAddress).take(120)
        val partner = DeliveryTrackerEngine.assignedPartnerForOrder(orderId, storeId)
        val (storeLat, storeLng) = DeliveryTrackerEngine.storeCoordinates(storeId)
        val initialFraction = when (status) {
            OrderStatus.DELIVERED -> 1.0
            OrderStatus.OUT_FOR_DELIVERY -> 0.1
            else -> 0.0
        }
        val (partnerLat, partnerLng) = DeliveryTrackerEngine.interpolateRoutePosition(
            storeLat = storeLat,
            storeLng = storeLng,
            destLat = destZone.latitude,
            destLng = destZone.longitude,
            fraction = initialFraction.toFloat()
        )
        return mapOf(
            "userId" to uid,
            "orderId" to orderId,
            "storeId" to storeId,
            "storeName" to storeName.take(80),
            "storeNeighborhood" to storeNeighborhood,
            "destinationArea" to destinationArea,
            "transitCorridor" to corridor,
            "partnerId" to partner.id,
            "partnerName" to partner.name.take(80),
            "partnerPhone" to partner.phone.take(20),
            "partnerVehicle" to partner.vehicleLabel.take(80),
            "status" to status.wireValue,
            "progressFraction" to initialFraction,
            "storeLat" to storeLat,
            "storeLng" to storeLng,
            "destLat" to destZone.latitude,
            "destLng" to destZone.longitude,
            "partnerLat" to partnerLat,
            "partnerLng" to partnerLng,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
