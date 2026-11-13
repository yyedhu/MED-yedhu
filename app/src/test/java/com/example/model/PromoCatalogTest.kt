package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoCatalogTest {

    @Test
    fun kochi20_appliesTwentyPercentCappedAtEighty() {
        val eligible = PromoCatalog.calculate(
            couponCode = "kochi20",
            subtotal = 200.0,
            baseDeliveryFee = 15.0
        )
        assertTrue(eligible.isApplied)
        assertEquals(40.0, eligible.discountAmount, 0.01)
        assertEquals(15.0, eligible.effectiveDeliveryFee, 0.01)

        val capped = PromoCatalog.calculate(
            couponCode = "KOCHI20",
            subtotal = 1000.0,
            baseDeliveryFee = 15.0
        )
        assertTrue(capped.isApplied)
        assertEquals(80.0, capped.discountAmount, 0.01)
    }

    @Test
    fun medfree_waivesDeliveryFeeAboveMinimum() {
        val tooLow = PromoCatalog.calculate(
            couponCode = "MEDFREE",
            subtotal = 50.0,
            baseDeliveryFee = 20.0
        )
        assertFalse(tooLow.isApplied)
        assertEquals(20.0, tooLow.effectiveDeliveryFee, 0.01)

        val valid = PromoCatalog.calculate(
            couponCode = "MEDFREE",
            subtotal = 120.0,
            baseDeliveryFee = 20.0
        )
        assertTrue(valid.isApplied)
        assertEquals(0.0, valid.effectiveDeliveryFee, 0.01)
    }

    @Test
    fun orderPayloadCodec_roundTripsCorrectly() {
        val items = listOf(
            CartItem(medicineId = "med_dolo_650", quantity = 2L),
            CartItem(medicineId = "med_neurobion_forte", quantity = 1L)
        )
        val encoded = OrderPayloadCodec.encode(items)
        val decoded = OrderPayloadCodec.decode(encoded)
        assertEquals(listOf("med_dolo_650" to 2L, "med_neurobion_forte" to 1L), decoded)
    }
}
