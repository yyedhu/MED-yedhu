package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.min
import kotlin.math.roundToInt

enum class MedicineCategory(val label: String) {
    ALL("All"),
    PAIN_FEVER("Pain & Fever"),
    CARDIAC_DIABETES("Cardiac & Diabetes"),
    ANTIBIOTICS_RX("Antibiotics & Rx"),
    VITAMINS("Vitamins"),
    ALLERGY_COLD("Allergy & Cold"),
    FIRST_AID("First Aid")
}

enum class StoreSortOption(val label: String) {
    RELEVANCE("Relevance"),
    FASTEST("Fastest Delivery"),
    TOP_RATED("Rating 4.8+"),
    LOW_FEE("Lowest Fee")
}

enum class CatalogSortOption(val label: String) {
    RELEVANCE("Popular"),
    PRICE_LOW_HIGH("Price: Low-High"),
    HIGHEST_DISCOUNT("Max Savings")
}

@Immutable
enum class PaymentMethodOption(
    val wireValue: String,
    val displayTitle: String,
    val subtitle: String
) {
    UPI("upi", "Google Pay / UPI", "Instant verified UPI checkout"),
    CARD("card", "Credit / Debit Card", "Visa, Mastercard, RuPay"),
    COD("cod", "Pay on Delivery", "Cash or UPI QR at your doorstep");

    companion object {
        fun fromWire(value: String): PaymentMethodOption =
            entries.firstOrNull { it.wireValue == value } ?: UPI
    }
}

@Immutable
data class PromoCoupon(
    val code: String,
    val title: String,
    val subtitle: String,
    val minSubtotal: Double,
    val badgeText: String
)

@Immutable
data class PromoCalculation(
    val coupon: PromoCoupon?,
    val discountAmount: Double,
    val effectiveDeliveryFee: Double,
    val isApplied: Boolean,
    val statusMessage: String
)

object PromoCatalog {
    val coupons: List<PromoCoupon> = listOf(
        PromoCoupon(
            code = "KOCHI20",
            title = "20% OFF up to ₹80",
            subtitle = "Valid on medicine orders ₹149 & above across Kochi",
            minSubtotal = 149.0,
            badgeText = "20% OFF"
        ),
        PromoCoupon(
            code = "MEDFREE",
            title = "Free Kochi Express Delivery",
            subtitle = "Waives courier delivery fee on orders ₹99 & above",
            minSubtotal = 99.0,
            badgeText = "FREE DELIVERY"
        ),
        PromoCoupon(
            code = "CARE50",
            title = "Flat ₹50 OFF",
            subtitle = "Chronic & wellness savings on orders ₹249 & above",
            minSubtotal = 249.0,
            badgeText = "FLAT ₹50"
        )
    )

    fun findByCode(rawCode: String): PromoCoupon? {
        val normalized = rawCode.trim().uppercase()
        return coupons.firstOrNull { it.code == normalized }
    }

    fun calculate(
        couponCode: String,
        subtotal: Double,
        baseDeliveryFee: Double
    ): PromoCalculation {
        val normalized = couponCode.trim().uppercase()
        if (normalized.isEmpty()) {
            return PromoCalculation(
                coupon = null,
                discountAmount = 0.0,
                effectiveDeliveryFee = baseDeliveryFee,
                isApplied = false,
                statusMessage = ""
            )
        }
        val coupon = findByCode(normalized) ?: return PromoCalculation(
            coupon = null,
            discountAmount = 0.0,
            effectiveDeliveryFee = baseDeliveryFee,
            isApplied = false,
            statusMessage = "Coupon $normalized is not valid for Kochi medical orders."
        )
        if (subtotal < coupon.minSubtotal) {
            val needed = (coupon.minSubtotal - subtotal).coerceAtLeast(1.0).roundToInt()
            return PromoCalculation(
                coupon = coupon,
                discountAmount = 0.0,
                effectiveDeliveryFee = baseDeliveryFee,
                isApplied = false,
                statusMessage = "Add ₹$needed more to unlock ${coupon.code}"
            )
        }
        return when (coupon.code) {
            "KOCHI20" -> {
                val rawDiscount = (subtotal * 0.20).roundToInt().toDouble()
                val capped = min(rawDiscount, 80.0).coerceAtMost(subtotal)
                PromoCalculation(
                    coupon = coupon,
                    discountAmount = capped,
                    effectiveDeliveryFee = baseDeliveryFee,
                    isApplied = capped > 0.0,
                    statusMessage = "Saved ₹${capped.toInt()} with ${coupon.code}"
                )
            }
            "MEDFREE" -> {
                val waived = baseDeliveryFee
                PromoCalculation(
                    coupon = coupon,
                    discountAmount = 0.0,
                    effectiveDeliveryFee = 0.0,
                    isApplied = true,
                    statusMessage = if (waived > 0.0) {
                        "Delivery fee ₹${waived.toInt()} waived with MEDFREE"
                    } else {
                        "MEDFREE applied — Free Kochi delivery active"
                    }
                )
            }
            "CARE50" -> {
                val capped = min(50.0, subtotal)
                PromoCalculation(
                    coupon = coupon,
                    discountAmount = capped,
                    effectiveDeliveryFee = baseDeliveryFee,
                    isApplied = capped > 0.0,
                    statusMessage = "Flat ₹${capped.toInt()} OFF applied with CARE50"
                )
            }
            else -> PromoCalculation(null, 0.0, baseDeliveryFee, false, "")
        }
    }
}

object DeliveryInstructionPresets {
    val chips: List<String> = listOf(
        "Avoid ringing bell",
        "Leave with security",
        "Call on arrival",
        "Handle cold-chain with care"
    )

    val tipOptions: List<Int> = listOf(0, 15, 30, 50)

    val ratingCompliments: List<String> = listOf(
        "Tamper-proof sealed pack",
        "Fast Kochi rider",
        "Polite handover",
        "Accurate medicines"
    )
}

object OrderPayloadCodec {
    fun encode(items: List<CartItem>): String =
        items.joinToString(";") { "${it.medicineId}:${it.quantity.coerceIn(1L, 50L)}" }
            .take(600)

    fun decode(payload: String): List<Pair<String, Long>> {
        if (payload.isBlank()) return emptyList()
        return payload.split(";")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2) null
                else {
                    val medId = parts[0].trim()
                    val qty = parts[1].trim().toLongOrNull()?.coerceIn(1L, 50L)
                    if (medId.isNotEmpty() && qty != null) medId to qty else null
                }
            }
    }
}

@Immutable
data class MedicalStore(
    val id: String,
    val name: String,
    val neighborhood: String,
    val distanceKm: Double,
    val etaMinutes: Int,
    val rating: Double,
    val reviewCount: Int,
    val isOpen24Hours: Boolean,
    val licenseNumber: String,
    val deliveryFee: Double,
    val minimumOrder: Double,
    val specialties: List<String>,
    val accentTag: String,
    val promoHeadline: String = ""
)

@Immutable
data class MedicineOffering(
    val id: String,
    val storeId: String,
    val storeName: String,
    val name: String,
    val genericName: String,
    val dosage: String,
    val category: MedicineCategory,
    val price: Double,
    val mrp: Double,
    val stockCount: Int,
    val requiresPrescription: Boolean,
    val manufacturer: String,
    val description: String,
    @DrawableRes val imageRes: Int
) {
    val discountPercent: Int
        get() = if (mrp > price && mrp > 0.0) (((mrp - price) / mrp) * 100.0).roundToInt() else 0
}

@Immutable
data class MedicineGroup(
    val id: String,
    val name: String,
    val genericName: String,
    val dosage: String,
    val category: MedicineCategory,
    val requiresPrescription: Boolean,
    val manufacturer: String,
    val description: String,
    @DrawableRes val imageRes: Int,
    val storeOfferings: List<MedicineOffering>
) {
    val lowestPrice: Double get() = storeOfferings.minOf { it.price }
    val mrp: Double get() = storeOfferings.maxOf { it.mrp }
    val maxDiscountPercent: Int get() = storeOfferings.maxOfOrNull { it.discountPercent } ?: 0
}

@IgnoreExtraProperties
data class DeliveryAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val fullAddress: String = "",
    val landmark: String = "",
    val contactPhone: String = "",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val medicineId: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val requiresPrescription: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MedicineOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val itemsSummary: String = "",
    val itemsPayload: String = "",
    val itemCount: Long = 0L,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val discountAmount: Double = 0.0,
    val couponCode: String = "",
    val tipAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = PaymentMethodOption.UPI.wireValue,
    val deliveryAddress: String = "",
    val deliveryInstructions: String = "",
    val status: String = OrderStatus.PLACED.wireValue,
    val hasPrescription: Boolean = false,
    val prescriptionFileName: String = "",
    val estimatedMinutes: Long = 30L,
    val rating: Long = 0L,
    val reviewComment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LiveOrderTracking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeNeighborhood: String = "",
    val destinationArea: String = "",
    val transitCorridor: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerPhone: String = "",
    val partnerVehicle: String = "",
    val status: String = OrderStatus.PLACED.wireValue,
    val progressFraction: Double = 0.0,
    val storeLat: Double = 0.0,
    val storeLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val partnerLat: Double = 0.0,
    val partnerLng: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OrderStatus(val wireValue: String, val displayLabel: String) {
    PLACED("placed", "Order Placed"),
    CONFIRMED("confirmed", "Store Confirmed"),
    PACKED("packed", "Sealed & Packed"),
    OUT_FOR_DELIVERY("out_for_delivery", "Rider En Route"),
    DELIVERED("delivered", "Delivered"),
    CANCELLED("cancelled", "Cancelled");

    fun nextProgressStep(): OrderStatus? = when (this) {
        PLACED -> CONFIRMED
        CONFIRMED -> PACKED
        PACKED -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> DELIVERED
        DELIVERED, CANCELLED -> null
    }

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED

    companion object {
        fun fromWire(value: String): OrderStatus =
            entries.firstOrNull { it.wireValue == value } ?: PLACED
    }
}
