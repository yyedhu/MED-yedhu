package com.example.model

import androidx.compose.runtime.Immutable
import com.example.data.PharmacyCatalog
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class DeliveryPartner(
    val id: String,
    val name: String,
    val phone: String,
    val vehicleLabel: String,
    val rating: Double,
    val completedDeliveries: Int
)

@Immutable
data class TrackingCheckpoint(
    val title: String,
    val locationDetail: String,
    val isReached: Boolean,
    val isCurrent: Boolean
)

@Immutable
data class OrderTrackingSnapshot(
    val orderId: String,
    val status: OrderStatus,
    val storeName: String,
    val storeNeighborhood: String,
    val destinationArea: String,
    val currentLocationLabel: String,
    val transitCorridor: String,
    val statusHeadline: String,
    val customerNotificationText: String,
    val partner: DeliveryPartner,
    val progressFraction: Float,
    val remainingMinutes: Int,
    val remainingDistanceKm: Double,
    val partnerLatitude: Double,
    val partnerLongitude: Double,
    val storeLatitude: Double,
    val storeLongitude: Double,
    val destLatitude: Double,
    val destLongitude: Double,
    val formattedGpsCoordinates: String,
    val gpsStatusLabel: String,
    val hasRiderArrived: Boolean,
    val isSyncedFromDatabase: Boolean,
    val checkpoints: List<TrackingCheckpoint>
)

object DeliveryTrackerEngine {

    private val kochiPartners = listOf(
        DeliveryPartner(
            id = "rider_arjun",
            name = "Arjun Nair",
            phone = "+919847021456",
            vehicleLabel = "KL-07-CX-4821 • Electric Scooter",
            rating = 4.9,
            completedDeliveries = 1240
        ),
        DeliveryPartner(
            id = "rider_fathima",
            name = "Fathima Beevi",
            phone = "+919847155820",
            vehicleLabel = "KL-07-BY-1934 • Med-Express Bike",
            rating = 4.9,
            completedDeliveries = 980
        ),
        DeliveryPartner(
            id = "rider_vishnu",
            name = "Vishnu Menon",
            phone = "+919447288310",
            vehicleLabel = "KL-07-AZ-7742 • Cold-Chain Scooter",
            rating = 4.8,
            completedDeliveries = 1520
        ),
        DeliveryPartner(
            id = "rider_stephy",
            name = "Stephy Thomas",
            phone = "+919745064912",
            vehicleLabel = "KL-07-DW-3309 • Electric Scooter",
            rating = 4.9,
            completedDeliveries = 870
        ),
        DeliveryPartner(
            id = "rider_nikhil",
            name = "Nikhil Varghese",
            phone = "+919895243187",
            vehicleLabel = "KL-07-EV-9055 • Express Med-Bike",
            rating = 4.8,
            completedDeliveries = 1105
        )
    )

    fun storeCoordinates(storeId: String): Pair<Double, Double> = when (storeId) {
        "store_apollo_care" -> 9.9710 to 76.2830 // MG Road, Ernakulam
        "store_greencross_24x7" -> 10.0261 to 76.3125 // Edappally Junction
        "store_medplus_express" -> 10.0159 to 76.3419 // Kakkanad InfoPark Rd
        "store_citycare_apothecary" -> 9.9619 to 76.2990 // Panampilly Nagar
        "store_wellspring_health" -> 9.9949 to 76.2921 // Kaloor - Kadavanthra Rd
        else -> 9.9816 to 76.2999 // Central Kochi
    }

    fun assignedPartnerForOrder(orderId: String, storeId: String): DeliveryPartner {
        val seed = abs((orderId.ifBlank { storeId }).hashCode())
        return kochiPartners[seed % kochiPartners.size]
    }

    fun assignedPartnerForOrder(order: MedicineOrder): DeliveryPartner =
        assignedPartnerForOrder(order.id, order.storeId)

    fun extractDestinationZoneInfo(deliveryAddress: String): KochiZone {
        val lower = deliveryAddress.lowercase()
        return KochiServiceArea.supportedZones.firstOrNull { zone ->
            val shortName = zone.name.substringBefore("/").trim().lowercase()
            lower.contains(shortName) || lower.contains(zone.pinCode)
        } ?: KochiServiceArea.supportedZones.first()
    }

    fun extractDestinationZone(deliveryAddress: String): String {
        val lower = deliveryAddress.lowercase()
        val matched = KochiServiceArea.supportedZones.firstOrNull { zone ->
            val shortName = zone.name.substringBefore("/").trim().lowercase()
            lower.contains(shortName) || lower.contains(zone.pinCode)
        }
        return matched?.name ?: "Kochi Doorstep"
    }

    fun interpolateRoutePosition(
        storeLat: Double,
        storeLng: Double,
        destLat: Double,
        destLng: Double,
        fraction: Float
    ): Pair<Double, Double> {
        val t = fraction.coerceIn(0f, 1f).toDouble()
        val lat = storeLat + (destLat - storeLat) * t
        val lng = storeLng + (destLng - storeLng) * t
        return lat to lng
    }

    fun resolveCorridorForOrder(storeId: String, deliveryAddress: String): String {
        val storeNeighborhood = PharmacyCatalog.storeById(storeId)?.neighborhood ?: "Ernakulam, Kochi"
        val destinationArea = extractDestinationZone(deliveryAddress)
        return resolveKochiCorridor(storeNeighborhood, destinationArea)
    }

    fun buildTrackingSnapshot(
        order: MedicineOrder,
        liveTransitFraction: Float? = null,
        liveTracking: LiveOrderTracking? = null
    ): OrderTrackingSnapshot {
        val store = PharmacyCatalog.storeById(order.storeId)
        val storeNeighborhood = liveTracking?.storeNeighborhood?.takeIf { it.isNotBlank() }
            ?: store?.neighborhood
            ?: "Ernakulam, Kochi"
        val destZone = extractDestinationZoneInfo(order.deliveryAddress)
        val destinationArea = liveTracking?.destinationArea?.takeIf { it.isNotBlank() }
            ?: extractDestinationZone(order.deliveryAddress)
        val fallbackPartner = assignedPartnerForOrder(order)
        val partner = if (liveTracking != null && liveTracking.partnerName.isNotBlank()) {
            fallbackPartner.copy(
                id = liveTracking.partnerId.ifBlank { fallbackPartner.id },
                name = liveTracking.partnerName,
                phone = liveTracking.partnerPhone.ifBlank { fallbackPartner.phone },
                vehicleLabel = liveTracking.partnerVehicle.ifBlank { fallbackPartner.vehicleLabel }
            )
        } else {
            fallbackPartner
        }

        val status = OrderStatus.fromWire(order.status)
        val transitCorridor = liveTracking?.transitCorridor?.takeIf { it.isNotBlank() }
            ?: resolveKochiCorridor(storeNeighborhood, destinationArea)

        val (defaultStoreLat, defaultStoreLng) = storeCoordinates(order.storeId)
        val storeLat = liveTracking?.storeLat?.takeIf { it != 0.0 } ?: defaultStoreLat
        val storeLng = liveTracking?.storeLng?.takeIf { it != 0.0 } ?: defaultStoreLng
        val destLat = liveTracking?.destLat?.takeIf { it != 0.0 } ?: destZone.latitude
        val destLng = liveTracking?.destLng?.takeIf { it != 0.0 } ?: destZone.longitude

        val totalRouteKm = haversineKm(storeLat, storeLng, destLat, destLng)
            .coerceAtLeast(store?.distanceKm ?: 1.8)
        val totalEtaMinutes = order.estimatedMinutes.toInt().coerceAtLeast(12)

        val transitFraction = (
            liveTracking?.progressFraction?.toFloat()
                ?: liveTransitFraction
                ?: 0.55f
            ).coerceIn(0f, 1f)
        val hasArrived = status == OrderStatus.DELIVERED ||
            (status == OrderStatus.OUT_FOR_DELIVERY && transitFraction >= 0.96f)

        val routeLerp = when (status) {
            OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED -> 0.0f
            OrderStatus.OUT_FOR_DELIVERY -> transitFraction
            OrderStatus.DELIVERED -> 1.0f
            OrderStatus.CANCELLED -> 0.0f
        }

        val (fallbackLat, fallbackLng) = interpolateRoutePosition(
            storeLat = storeLat,
            storeLng = storeLng,
            destLat = destLat,
            destLng = destLng,
            fraction = routeLerp
        )
        val partnerLat = if (liveTracking != null && liveTracking.partnerLat != 0.0) {
            liveTracking.partnerLat
        } else {
            fallbackLat
        }
        val partnerLng = if (liveTracking != null && liveTracking.partnerLng != 0.0) {
            liveTracking.partnerLng
        } else {
            fallbackLng
        }
        val formattedGps = String.format(Locale.US, "%.4f° N, %.4f° E", partnerLat, partnerLng)

        val progress = when (status) {
            OrderStatus.PLACED -> 0.14f
            OrderStatus.CONFIRMED -> 0.34f
            OrderStatus.PACKED -> 0.54f
            OrderStatus.OUT_FOR_DELIVERY -> 0.56f + (0.42f * transitFraction)
            OrderStatus.DELIVERED -> 1.0f
            OrderStatus.CANCELLED -> 0.0f
        }

        val remainingMinutes = when (status) {
            OrderStatus.PLACED -> totalEtaMinutes
            OrderStatus.CONFIRMED -> (totalEtaMinutes * 0.80).roundToInt().coerceAtLeast(8)
            OrderStatus.PACKED -> (totalEtaMinutes * 0.60).roundToInt().coerceAtLeast(6)
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived) 0 else ((totalEtaMinutes * 0.55) * (1f - transitFraction)).roundToInt().coerceAtLeast(1)
            OrderStatus.DELIVERED, OrderStatus.CANCELLED -> 0
        }

        val remainingKm = when (status) {
            OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PACKED -> totalRouteKm
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived) 0.0 else (totalRouteKm * (1f - transitFraction)).coerceAtLeast(0.1)
            OrderStatus.DELIVERED, OrderStatus.CANCELLED -> 0.0
        }

        val currentLocationLabel = when (status) {
            OrderStatus.PLACED -> "$storeNeighborhood (Awaiting Shop)"
            OrderStatus.CONFIRMED -> "$storeNeighborhood (Being Verified by Medical Shop)"
            OrderStatus.PACKED -> "$storeNeighborhood (Packed • Ready for Rider Pickup)"
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived)
                "$destinationArea, Kochi (Rider Arrived at Location)"
            else
                "$transitCorridor → $destinationArea"
            OrderStatus.DELIVERED -> "$destinationArea, Kochi (Delivered)"
            OrderStatus.CANCELLED -> "Order Cancelled"
        }

        val statusHeadline = when (status) {
            OrderStatus.PLACED -> "Order sent to ${order.storeName}. Waiting for medical shop confirmation."
            OrderStatus.CONFIRMED -> "Medical shop confirmed your order and is packing your medicines."
            OrderStatus.PACKED -> "Medical shop packed & sealed your order. ${partner.name} is picking it up."
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived)
                "${partner.name} has arrived at your $destinationArea location!"
            else
                "${partner.name} picked up your order and is heading to $destinationArea via $transitCorridor."
            OrderStatus.DELIVERED -> "Delivered safely at $destinationArea by ${partner.name}."
            OrderStatus.CANCELLED -> "This medicine order was cancelled."
        }

        val customerNotificationText = when (status) {
            OrderStatus.PLACED -> "Order Placed • Awaiting Medical Shop Confirmation"
            OrderStatus.CONFIRMED -> "Medical Shop Confirmed • Preparing Medicines"
            OrderStatus.PACKED -> "Notified: Packed & Sealed by ${order.storeName}"
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived)
                "Notified: Delivery Partner Arrived at Your Kochi Location!"
            else
                "GPS Live: ${partner.name} Picked Up Item & Is On The Way"
            OrderStatus.DELIVERED -> "Delivered at $destinationArea, Kochi"
            OrderStatus.CANCELLED -> "Order Cancelled"
        }

        val gpsStatusLabel = when (status) {
            OrderStatus.PLACED, OrderStatus.CONFIRMED -> "Rider Assigned • At Dispatch Zone"
            OrderStatus.PACKED -> "Rider At Pharmacy Pickup Point"
            OrderStatus.OUT_FOR_DELIVERY -> if (hasArrived) "GPS Arrived • At Doorstep" else "GPS Live • En Route (26 km/h)"
            OrderStatus.DELIVERED -> "GPS Completed • Delivered"
            OrderStatus.CANCELLED -> "GPS Inactive"
        }

        val checkpoints = listOf(
            TrackingCheckpoint(
                title = "Order Placed by Customer",
                locationDetail = "Submitted to ${order.storeName}, $storeNeighborhood",
                isReached = status != OrderStatus.CANCELLED,
                isCurrent = status == OrderStatus.PLACED
            ),
            TrackingCheckpoint(
                title = "Medical Shop Confirmed",
                locationDetail = "Pharmacist verified stock at ${order.storeName}",
                isReached = status in listOf(
                    OrderStatus.CONFIRMED,
                    OrderStatus.PACKED,
                    OrderStatus.OUT_FOR_DELIVERY,
                    OrderStatus.DELIVERED
                ),
                isCurrent = status == OrderStatus.CONFIRMED
            ),
            TrackingCheckpoint(
                title = "Packed by Medical Shop",
                locationDetail = "Sealed tamper-proof pouch • Customer notified",
                isReached = status in listOf(
                    OrderStatus.PACKED,
                    OrderStatus.OUT_FOR_DELIVERY,
                    OrderStatus.DELIVERED
                ),
                isCurrent = status == OrderStatus.PACKED
            ),
            TrackingCheckpoint(
                title = "Rider Picked Up • Live GPS En Route",
                locationDetail = "${partner.name} ($formattedGps) on $transitCorridor",
                isReached = status in listOf(
                    OrderStatus.OUT_FOR_DELIVERY,
                    OrderStatus.DELIVERED
                ),
                isCurrent = status == OrderStatus.OUT_FOR_DELIVERY && !hasArrived
            ),
            TrackingCheckpoint(
                title = "Rider Arrived & Delivered",
                locationDetail = "$destinationArea, Kochi • Customer notified on arrival",
                isReached = status == OrderStatus.DELIVERED || hasArrived,
                isCurrent = status == OrderStatus.DELIVERED || hasArrived
            )
        )

        return OrderTrackingSnapshot(
            orderId = order.id,
            status = status,
            storeName = order.storeName,
            storeNeighborhood = storeNeighborhood,
            destinationArea = destinationArea,
            currentLocationLabel = currentLocationLabel,
            transitCorridor = transitCorridor,
            statusHeadline = statusHeadline,
            customerNotificationText = customerNotificationText,
            partner = partner,
            progressFraction = progress,
            remainingMinutes = remainingMinutes,
            remainingDistanceKm = (remainingKm * 10).roundToInt() / 10.0,
            partnerLatitude = partnerLat,
            partnerLongitude = partnerLng,
            storeLatitude = storeLat,
            storeLongitude = storeLng,
            destLatitude = destLat,
            destLongitude = destLng,
            formattedGpsCoordinates = formattedGps,
            gpsStatusLabel = gpsStatusLabel,
            hasRiderArrived = hasArrived,
            isSyncedFromDatabase = liveTracking != null,
            checkpoints = checkpoints
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun resolveKochiCorridor(storeNeighborhood: String, destinationArea: String): String {
        val dest = destinationArea.lowercase()
        val store = storeNeighborhood.lowercase()
        return when {
            dest.contains("kakkanad") || store.contains("kakkanad") -> "Seaport-Airport Rd / Palarivattom"
            dest.contains("edappally") || dest.contains("aluva") || dest.contains("kalamassery") -> "NH-66 / Kochi Metro Corridor"
            dest.contains("fort kochi") -> "Marine Drive – Thoppumpady Bridge"
            dest.contains("vyttila") || dest.contains("thrippunithura") -> "SA Road – Vyttila Mobility Hub"
            else -> "Banerji Rd – Kaloor Junction"
        }
    }
}
