package com.example.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KochiServiceAreaTest {

    @Test
    fun kochiPinCodes_areAccepted() {
        assertTrue(KochiServiceArea.isKochiPinCode("682016"))
        assertTrue(KochiServiceArea.isKochiPinCode("682024"))
        assertTrue(KochiServiceArea.isKochiPinCode("682030"))
        assertTrue(KochiServiceArea.isKochiPinCode("682001"))
    }

    @Test
    fun nonKochiPinCodes_areRejected() {
        assertFalse(KochiServiceArea.isKochiPinCode("560038"))
        assertFalse(KochiServiceArea.isKochiPinCode("695001"))
        assertFalse(KochiServiceArea.isKochiPinCode("673001"))
        assertFalse(KochiServiceArea.isKochiPinCode("68201"))
    }

    @Test
    fun kochiAddresses_areEligibleForPurchase() {
        assertTrue(
            KochiServiceArea.isKochiAddress(
                DeliveryAddress(
                    label = "Home",
                    fullAddress = "14B Skyline, Edappally, Kochi, Kerala 682024",
                    landmark = "Near Lulu Mall"
                )
            )
        )
        assertTrue(
            KochiServiceArea.isKochiAddress(
                DeliveryAddress(
                    label = "Office",
                    fullAddress = "InfoPark Phase 2, Kakkanad, Ernakulam",
                    landmark = ""
                )
            )
        )
    }

    @Test
    fun outsideKochiAddresses_areBlockedFromPurchase() {
        assertFalse(
            KochiServiceArea.isKochiAddress(
                DeliveryAddress(
                    label = "Bengaluru",
                    fullAddress = "42 MG Road, Indiranagar, Bengaluru 560038",
                    landmark = "Near Metro"
                )
            )
        )
        assertFalse(
            KochiServiceArea.isKochiAddress(
                DeliveryAddress(
                    label = "Trivandrum",
                    fullAddress = "Technopark, Thiruvananthapuram, Kerala 695581",
                    landmark = ""
                )
            )
        )
        assertFalse(
            KochiServiceArea.isKochiAddress(
                DeliveryAddress(
                    label = "Spoofed City With Outside PIN",
                    fullAddress = "MG Road, Kochi, Kerala 695001",
                    landmark = ""
                )
            )
        )
        assertFalse(KochiServiceArea.isKochiAddress(null))
    }
}
