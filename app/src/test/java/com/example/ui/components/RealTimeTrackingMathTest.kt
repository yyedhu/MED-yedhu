package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTimeTrackingMathTest {

    @Test
    fun `interpolateCoordinate smoothly interpolates between two GPS points`() {
        val (midLat, midLon) = RealTimeTrackingMath.interpolateCoordinate(
            startLat = 9.9710,
            startLon = 76.2830,
            endLat = 10.0159,
            endLon = 76.3419,
            fraction = 0.5f
        )
        assertEquals(9.99345, midLat, 0.0001)
        assertEquals(76.31245, midLon, 0.0001)

        val (clampedLat, clampedLon) = RealTimeTrackingMath.interpolateCoordinate(
            startLat = 9.9710,
            startLon = 76.2830,
            endLat = 10.0159,
            endLon = 76.3419,
            fraction = 1.5f
        )
        assertEquals(10.0159, clampedLat, 0.00001)
        assertEquals(76.3419, clampedLon, 0.00001)
    }

    @Test
    fun `computeRouteBoundingBox includes padding even when points coincide`() {
        val bounds = RealTimeTrackingMath.computeRouteBoundingBox(
            storeLat = 9.9816,
            storeLon = 76.2999,
            destLat = 9.9816,
            destLon = 76.2999,
            partnerLat = 9.9816,
            partnerLon = 76.2999,
            paddingDegrees = 0.004
        )
        assertTrue(bounds.maxLat > bounds.minLat)
        assertTrue(bounds.maxLon > bounds.minLon)
        assertEquals(0.008, bounds.maxLat - bounds.minLat, 0.00001)
    }
}
