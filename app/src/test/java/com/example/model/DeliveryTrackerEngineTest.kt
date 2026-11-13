package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryTrackerEngineTest {

    private fun sampleOrder(status: OrderStatus): MedicineOrder {
        return MedicineOrder(
            id = "ord_test_123",
            userId = "user_1",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            itemsSummary = "Dolo 650 Tablet x1",
            itemCount = 1L,
            subtotal = 32.0,
            deliveryFee = 15.0,
            totalAmount = 47.0,
            deliveryAddress = "Home: 14B Skyline, Edappally, Kochi, Kerala 682024",
            status = status.wireValue,
            estimatedMinutes = 20L
        )
    }

    @Test
    fun trackingSnapshot_extractsKochiDestinationPartnerAndLiveGps() {
        val midTransit = DeliveryTrackerEngine.buildTrackingSnapshot(
            order = sampleOrder(OrderStatus.OUT_FOR_DELIVERY),
            liveTransitFraction = 0.5f
        )
        val arrived = DeliveryTrackerEngine.buildTrackingSnapshot(
            order = sampleOrder(OrderStatus.OUT_FOR_DELIVERY),
            liveTransitFraction = 0.98f
        )

        assertEquals("Edappally", midTransit.destinationArea)
        assertEquals("MG Road, Ernakulam, Kochi", midTransit.storeNeighborhood)
        assertTrue(midTransit.partner.name.isNotBlank())
        assertTrue(midTransit.partner.vehicleLabel.contains("KL-07"))
        assertTrue(midTransit.formattedGpsCoordinates.contains("° N"))
        assertFalse(midTransit.hasRiderArrived)
        assertTrue(midTransit.partnerLatitude < arrived.partnerLatitude)
        assertTrue(arrived.hasRiderArrived)
        assertTrue(arrived.customerNotificationText.contains("Arrived"))
    }

    @Test
    fun trackingSnapshot_progressIncreasesThroughDeliveryStages() {
        val placed = DeliveryTrackerEngine.buildTrackingSnapshot(sampleOrder(OrderStatus.PLACED))
        val confirmed = DeliveryTrackerEngine.buildTrackingSnapshot(sampleOrder(OrderStatus.CONFIRMED))
        val packed = DeliveryTrackerEngine.buildTrackingSnapshot(sampleOrder(OrderStatus.PACKED))
        val enRoute = DeliveryTrackerEngine.buildTrackingSnapshot(
            order = sampleOrder(OrderStatus.OUT_FOR_DELIVERY),
            liveTransitFraction = 0.5f
        )
        val delivered = DeliveryTrackerEngine.buildTrackingSnapshot(sampleOrder(OrderStatus.DELIVERED))

        assertTrue(placed.progressFraction < confirmed.progressFraction)
        assertTrue(confirmed.progressFraction < packed.progressFraction)
        assertTrue(packed.progressFraction < enRoute.progressFraction)
        assertTrue(enRoute.progressFraction < delivered.progressFraction)
        assertTrue(packed.customerNotificationText.contains("Packed"))
        assertEquals(1.0f, delivered.progressFraction)
        assertEquals(0, delivered.remainingMinutes)
        assertTrue(delivered.checkpoints.all { it.isReached })
    }

    @Test
    fun trackingSnapshot_prefersFirestoreLiveTrackingCoordinates() {
        val firestoreTracking = LiveOrderTracking(
            id = "ord_test_123",
            userId = "user_1",
            orderId = "ord_test_123",
            storeId = "store_apollo_care",
            storeName = "Apollo Care Pharmacy",
            storeNeighborhood = "MG Road, Ernakulam, Kochi",
            destinationArea = "Edappally",
            transitCorridor = "NH-66 / Kochi Metro Corridor",
            partnerId = "rider_arjun",
            partnerName = "Arjun Nair",
            partnerPhone = "+919847021456",
            partnerVehicle = "KL-07-CX-4821 • Electric Scooter",
            status = OrderStatus.OUT_FOR_DELIVERY.wireValue,
            progressFraction = 0.72,
            storeLat = 9.9710,
            storeLng = 76.2830,
            destLat = 10.0261,
            destLng = 76.3125,
            partnerLat = 10.0107,
            partnerLng = 76.3042
        )

        val snapshot = DeliveryTrackerEngine.buildTrackingSnapshot(
            order = sampleOrder(OrderStatus.OUT_FOR_DELIVERY),
            liveTracking = firestoreTracking
        )

        assertTrue(snapshot.isSyncedFromDatabase)
        assertEquals(10.0107, snapshot.partnerLatitude, 0.0001)
        assertEquals(76.3042, snapshot.partnerLongitude, 0.0001)
        assertEquals(9.9710, snapshot.storeLatitude, 0.0001)
        assertEquals(10.0261, snapshot.destLatitude, 0.0001)
    }
}
