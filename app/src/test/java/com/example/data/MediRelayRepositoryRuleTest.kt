package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.OrderStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class MediRelayRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAddress_authenticatedOwner_persistsAndStreams() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = MediRelayRepository(firestore, auth)

        val uniqueLabel = "Home-${UUID.randomUUID().toString().take(6)}"
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAddress(
                label = uniqueLabel,
                fullAddress = "42 MG Road, Indiranagar, Bengaluru",
                landmark = "Near Metro",
                contactPhone = "+91 9876543210",
                isDefault = true
            )
        }
        assertTrue("Expected saveAddress success, got ${result.exceptionOrNull()}", result.isSuccess)
        val addrId = result.getOrThrow()

        val streamed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAddresses(uid).first { list -> list.any { it.id == addrId } }
        }
        assertEquals(uniqueLabel, streamed.first { it.id == addrId }.label)
    }

    @Test
    fun addToCartAndPlaceOrder_validFlow_createsOrderAndClearsCart() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = MediRelayRepository(firestore, auth)
        val offering = PharmacyCatalog.offerings.first()

        val cartRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.addToCart(offering) }
        assertTrue("addToCart failed: ${cartRes.exceptionOrNull()}", cartRes.isSuccess)

        val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCartItems(uid).first { list -> list.any { it.medicineId == offering.id } }
        }
        val storeItems = cartItems.filter { it.storeId == offering.storeId }
        assertTrue(storeItems.isNotEmpty())

        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.placeOrder(
                storeId = offering.storeId,
                storeName = offering.storeName,
                storeItems = storeItems,
                deliveryFee = 15.0,
                deliveryAddress = "42 MG Road, Indiranagar, Bengaluru",
                hasPrescription = false,
                prescriptionFileName = "",
                estimatedMinutes = 20
            )
        }
        assertTrue("placeOrder failed: ${orderRes.exceptionOrNull()}", orderRes.isSuccess)
        val orderId = orderRes.getOrThrow()

        val fetchedOrder = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getOrderById(orderId).getOrThrow() }
        assertNotNull(fetchedOrder)
        assertEquals(OrderStatus.PLACED.wireValue, fetchedOrder!!.status)

        val advanceRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
        }
        assertTrue("updateOrderStatus failed: ${advanceRes.exceptionOrNull()}", advanceRes.isSuccess)
    }

    @Test
    fun getOrder_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MediRelayRepository(firestore, auth)
        val offering = PharmacyCatalog.offerings.first()

        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addToCart(offering).getOrThrow() }
        val aliceCart = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCartItems(aliceUid).first { it.isNotEmpty() }
        }
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.placeOrder(
                storeId = offering.storeId,
                storeName = offering.storeName,
                storeItems = aliceCart,
                deliveryFee = 15.0,
                deliveryAddress = "42 MG Road, Indiranagar",
                hasPrescription = false,
                prescriptionFileName = "",
                estimatedMinutes = 20
            ).getOrThrow()
        }

        // Switch to Bob and attempt direct read on Alice's order path
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("orders")
                    .document(orderId)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for cross-user order read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError!!.code)
        }
    }

    @Test
    fun observeOrders_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = MediRelayRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeOrders("unauthenticated_uid_probe").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated observeOrders")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError!!.code)
        }
    }

    @Test
    fun liveOrderTracking_ownerStreamsGpsUpdates_andCrossUserDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = MediRelayRepository(firestore, auth)
        val offering = PharmacyCatalog.offerings.first()

        withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.addToCart(offering).getOrThrow() }
        val cart = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCartItems(aliceUid).first { it.isNotEmpty() }
        }
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.placeOrder(
                storeId = offering.storeId,
                storeName = offering.storeName,
                storeItems = cart,
                deliveryFee = 15.0,
                deliveryAddress = "Home: 14B Skyline, Edappally, Kochi 682024",
                hasPrescription = false,
                prescriptionFileName = "",
                estimatedMinutes = 22
            ).getOrThrow()
        }

        val initialTrackingMap = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeLiveTrackings(aliceUid).first { it.containsKey(orderId) }
        }
        val initialTracking = initialTrackingMap.getValue(orderId)
        assertEquals(OrderStatus.PLACED.wireValue, initialTracking.status)
        assertTrue(initialTracking.storeLat > 9.0 && initialTracking.destLat > 9.0)

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateOrderTracking(
                orderId = orderId,
                status = OrderStatus.OUT_FOR_DELIVERY,
                progressFraction = 0.65,
                partnerLat = 10.0052,
                partnerLng = 76.3011
            )
        }
        assertTrue("updateOrderTracking failed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

        val updatedMap = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeLiveTrackings(aliceUid).first {
                it[orderId]?.status == OrderStatus.OUT_FOR_DELIVERY.wireValue
            }
        }
        assertEquals(10.0052, updatedMap.getValue(orderId).partnerLat, 0.0001)

        // Cross-user read on Alice's live tracking is rejected
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("tracking")
                    .document(orderId)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED for cross-user tracking read")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsError)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError!!.code)
        }
    }

    @Test
    fun favoriteStore_promoOrder_andPostDeliveryRating_persistAndEnforceRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = MediRelayRepository(firestore, auth)

        // Favorite toggle & stream
        val favOn = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.toggleFavoriteStore("store_apollo_care", currentlyFavorite = false)
        }
        assertTrue("toggleFavoriteStore failed: ${favOn.exceptionOrNull()}", favOn.isSuccess)
        val favIds = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeFavoriteStoreIds(aliceUid).first { "store_apollo_care" in it }
        }
        assertTrue("store_apollo_care" in favIds)

        // Add & place order with promo coupon, rider tip, payment method, and drop-off instructions
        val offering = PharmacyCatalog.offerings.first()
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.addBatchToCart(listOf(offering to 2L)).getOrThrow() }
        val cart = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCartItems(aliceUid).first { it.isNotEmpty() }
        }
        val orderId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.placeOrder(
                storeId = offering.storeId,
                storeName = offering.storeName,
                storeItems = cart,
                deliveryFee = 15.0,
                deliveryAddress = "Home: MG Road, Kochi 682016",
                hasPrescription = false,
                prescriptionFileName = "",
                estimatedMinutes = 18,
                discountAmount = 12.0,
                couponCode = "KOCHI20",
                tipAmount = 30.0,
                paymentMethod = "upi",
                deliveryInstructions = "Leave with security"
            ).getOrThrow()
        }

        val placedOrder = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getOrderById(orderId).getOrThrow() }
        assertNotNull(placedOrder)
        assertEquals("KOCHI20", placedOrder!!.couponCode)
        assertEquals(30.0, placedOrder.tipAmount, 0.01)
        assertEquals("Leave with security", placedOrder.deliveryInstructions)

        // Rating must fail before DELIVERED
        val prematureRating = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitOrderRating(orderId, 5, "Too early")
        }
        assertTrue("Expected premature rating to fail", prematureRating.isFailure)

        // Advance order through shop & rider lifecycle to DELIVERED
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateOrderStatus(orderId, OrderStatus.CONFIRMED).getOrThrow() }
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateOrderStatus(orderId, OrderStatus.PACKED).getOrThrow() }
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY).getOrThrow() }
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateOrderStatus(orderId, OrderStatus.DELIVERED).getOrThrow() }

        // Post-delivery rating succeeds
        val ratingRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitOrderRating(orderId, 5, "Tamper-proof sealed pack")
        }
        assertTrue("Expected rating submit success, got ${ratingRes.exceptionOrNull()}", ratingRes.isSuccess)
        val ratedOrder = withTimeout(DEFAULT_TIMEOUT_MS) { repo.getOrderById(orderId).getOrThrow() }
        assertEquals(5L, ratedOrder!!.rating)
        assertEquals("Tamper-proof sealed pack", ratedOrder.reviewComment)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_med@test.com"
        const val BOB_EMAIL = "bob_med@test.com"
        const val DEFAULT_TIMEOUT_MS = 10000L
        const val FLOW_TIMEOUT_MS = 10000L
    }
}
