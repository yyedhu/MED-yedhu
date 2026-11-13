package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.DeliveryTrackerEngine
import com.example.model.LiveOrderTracking
import com.example.model.MedicineOrder
import com.example.model.OrderStatus
import com.example.model.OrderTrackingSnapshot
import com.example.model.PaymentMethodOption
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveDeliveryTrackingSheet(
    order: MedicineOrder,
    liveTransitFraction: Float?,
    liveTracking: LiveOrderTracking? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tracking = DeliveryTrackerEngine.buildTrackingSnapshot(
        order = order,
        liveTransitFraction = liveTransitFraction,
        liveTracking = liveTracking
    )
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusPill(
                        text = if (tracking.status.isTerminal) tracking.status.displayLabel.uppercase()
                        else "LIVE KOCHI GPS TRACKING",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            tracking.status == OrderStatus.DELIVERED -> "Delivered to ${tracking.destinationArea}"
                            tracking.status == OrderStatus.CANCELLED -> "Order Cancelled"
                            tracking.hasRiderArrived -> "Rider Arrived at ${tracking.destinationArea}!"
                            else -> "Arriving in ${tracking.remainingMinutes} mins"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!tracking.status.isTerminal) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (tracking.hasRiderArrived) "HERE" else "${tracking.remainingMinutes}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (tracking.hasRiderArrived) "ARRIVED" else "MIN ETA",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Customer Notification Banner (Shop Packed / Rider Picked Up / Rider Arrived)
            Surface(
                color = if (tracking.hasRiderArrived || tracking.status == OrderStatus.PACKED)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tracking.customerNotificationText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tracking.statusHeadline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive Free OpenStreetMap + Firestore Live GPS Tracking
            KochiDeliveryRouteMap(
                tracking = tracking,
                deliveryAddress = order.deliveryAddress
            )

            // Live Delivery Partner GPS Telemetry Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = "Partner GPS coordinates",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${tracking.gpsStatusLabel} • ${tracking.formattedGpsCoordinates}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tracking.currentLocationLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!tracking.status.isTerminal && !tracking.hasRiderArrived) {
                        Text(
                            text = "${tracking.remainingDistanceKm} km",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Delivery Partner Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.TwoWheeler,
                                contentDescription = "Delivery partner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tracking.partner.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${tracking.partner.rating}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        Text(
                            text = tracking.partner.vehicleLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Kochi Medical Courier • ${tracking.partner.completedDeliveries}+ deliveries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val dialIntent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${tracking.partner.phone}")
                            )
                            context.startActivity(dialIntent)
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("call_partner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call delivery partner",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }
                }
            }

            // Live Checkpoint Timeline (Read-only for customers — updated by medical shop & rider GPS)
            Text(
                text = "Fulfillment & Rider Timeline",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tracking.checkpoints.forEachIndexed { index, checkpoint ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            checkpoint.isCurrent -> MaterialTheme.colorScheme.primary
                                            checkpoint.isReached -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            ) {
                                if (checkpoint.isReached) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = if (checkpoint.isCurrent)
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            if (index < tracking.checkpoints.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(22.dp)
                                        .background(
                                            if (checkpoint.isReached) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = checkpoint.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (checkpoint.isReached) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = checkpoint.locationDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (checkpoint.isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Items: ${order.itemsSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val paymentLabel = PaymentMethodOption.fromWire(order.paymentMethod).displayTitle
                val billMeta = buildList {
                    add("Paid via $paymentLabel (${formatCurrency(order.totalAmount)})")
                    if (order.couponCode.isNotBlank() && order.discountAmount > 0.0) {
                        add("Coupon ${order.couponCode} (-${formatCurrency(order.discountAmount)})")
                    }
                    if (order.tipAmount > 0.0) {
                        add("Rider Tip ${formatCurrency(order.tipAmount)}")
                    }
                }.joinToString(" • ")
                Text(
                    text = billMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (order.deliveryInstructions.isNotBlank()) {
                    Text(
                        text = "Drop-off note: ${order.deliveryInstructions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private const val OPENFREEMAP_LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE_PLANNED_ROUTE = "kochi-planned-route-source"
private const val LAYER_PLANNED_ROUTE = "kochi-planned-route-layer"
private const val SOURCE_TRAVELED_ROUTE = "kochi-traveled-route-source"
private const val LAYER_TRAVELED_ROUTE = "kochi-traveled-route-layer"
private const val SOURCE_STORE_PIN = "kochi-store-pin-source"
private const val LAYER_STORE_PIN = "kochi-store-pin-layer"
private const val ICON_STORE_PIN = "kochi-store-pin-icon"
private const val SOURCE_DEST_PIN = "kochi-dest-pin-source"
private const val LAYER_DEST_PIN = "kochi-dest-pin-layer"
private const val ICON_DEST_PIN = "kochi-dest-pin-icon"
private const val SOURCE_PARTNER_PIN = "kochi-partner-pin-source"
private const val LAYER_PARTNER_PIN = "kochi-partner-pin-layer"
private const val ICON_PARTNER_PIN = "kochi-partner-pin-icon"

@Composable
fun KochiDeliveryRouteMap(
    tracking: OrderTrackingSnapshot,
    deliveryAddress: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline

    val primaryArgb = primaryColor.toArgb()
    val plannedRouteArgb = primaryColor.copy(alpha = 0.38f).toArgb()
    val storePinColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
    val destPinColorArgb = tertiaryColor.toArgb()

    var recenterTrigger by remember { mutableIntStateOf(0) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    val mapView = remember(context) {
        val appCtx = context.applicationContext
        MapLibre.getInstance(appCtx)
        MapView(appCtx).apply {
            onCreate(null)
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.uiSettings.apply {
                isCompassEnabled = true
                isAttributionEnabled = false
                isLogoEnabled = false
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(tracking.partnerLatitude, tracking.partnerLongitude))
                .zoom(13.5)
                .build()

            map.setStyle(OPENFREEMAP_LIBERTY_STYLE_URL) { style ->
                setupOpenFreeMapLayers(
                    style = style,
                    context = context.applicationContext,
                    plannedRouteArgb = plannedRouteArgb,
                    primaryArgb = primaryArgb,
                    storePinColorArgb = storePinColorArgb,
                    destPinColorArgb = destPinColorArgb
                )
                updateOpenFreeMapSources(style = style, tracking = tracking)
                loadedStyle = style
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(loadedStyle, tracking) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (style.isFullyLoaded) {
            updateOpenFreeMapSources(style = style, tracking = tracking)
        }
    }

    LaunchedEffect(mapLibreMap, tracking.orderId, recenterTrigger) {
        val map = mapLibreMap ?: return@LaunchedEffect
        fitKochiRouteBounds(map = map, tracking = tracking)
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("kochi_openfreemap")
                )

                // Top-left live OpenFreeMap + Firestore GPS pill
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = MaterialTheme.shapes.small,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "OpenFreeMap • ${if (tracking.isSyncedFromDatabase) "Firestore GPS Live" else "Kochi GPS"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Top-right Recenter map button
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    IconButton(
                        onClick = { recenterTrigger++ },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("recenter_openfreemap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Recenter map on Kochi route",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom-left OpenFreeMap attribution badge
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "© OpenFreeMap • OpenStreetMap",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Origin & Destination footer strip
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalPharmacy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tracking.storeNeighborhood,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tracking.destinationArea,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun setupOpenFreeMapLayers(
    style: Style,
    context: Context,
    plannedRouteArgb: Int,
    primaryArgb: Int,
    storePinColorArgb: Int,
    destPinColorArgb: Int
) {
    style.addImage(
        ICON_STORE_PIN,
        createMapPinBitmap(context, storePinColorArgb, "Rx", sizeDp = 30f, highlightHalo = false)
    )
    style.addImage(
        ICON_DEST_PIN,
        createMapPinBitmap(context, destPinColorArgb, "H", sizeDp = 30f, highlightHalo = false)
    )
    style.addImage(
        ICON_PARTNER_PIN,
        createMapPinBitmap(context, primaryArgb, "▶", sizeDp = 34f, highlightHalo = true)
    )

    style.addSource(GeoJsonSource(SOURCE_PLANNED_ROUTE))
    style.addLayer(
        LineLayer(LAYER_PLANNED_ROUTE, SOURCE_PLANNED_ROUTE).withProperties(
            lineColor(plannedRouteArgb),
            lineWidth(5.5f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        )
    )

    style.addSource(GeoJsonSource(SOURCE_TRAVELED_ROUTE))
    style.addLayer(
        LineLayer(LAYER_TRAVELED_ROUTE, SOURCE_TRAVELED_ROUTE).withProperties(
            lineColor(primaryArgb),
            lineWidth(7.5f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        )
    )

    style.addSource(GeoJsonSource(SOURCE_STORE_PIN))
    style.addLayer(
        SymbolLayer(LAYER_STORE_PIN, SOURCE_STORE_PIN).withProperties(
            iconImage(ICON_STORE_PIN),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
    )

    style.addSource(GeoJsonSource(SOURCE_DEST_PIN))
    style.addLayer(
        SymbolLayer(LAYER_DEST_PIN, SOURCE_DEST_PIN).withProperties(
            iconImage(ICON_DEST_PIN),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
    )

    style.addSource(GeoJsonSource(SOURCE_PARTNER_PIN))
    style.addLayer(
        SymbolLayer(LAYER_PARTNER_PIN, SOURCE_PARTNER_PIN).withProperties(
            iconImage(ICON_PARTNER_PIN),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
    )
}

private fun updateOpenFreeMapSources(
    style: Style,
    tracking: OrderTrackingSnapshot
) {
    val storePoint = Point.fromLngLat(tracking.storeLongitude, tracking.storeLatitude)
    val destPoint = Point.fromLngLat(tracking.destLongitude, tracking.destLatitude)
    val partnerPoint = Point.fromLngLat(tracking.partnerLongitude, tracking.partnerLatitude)

    style.getSourceAs<GeoJsonSource>(SOURCE_PLANNED_ROUTE)?.setGeoJson(
        Feature.fromGeometry(LineString.fromLngLats(listOf(storePoint, destPoint)))
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_TRAVELED_ROUTE)?.setGeoJson(
        if (tracking.status != OrderStatus.CANCELLED) {
            FeatureCollection.fromFeatures(
                arrayOf(Feature.fromGeometry(LineString.fromLngLats(listOf(storePoint, partnerPoint))))
            )
        } else {
            FeatureCollection.fromFeatures(emptyArray())
        }
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_STORE_PIN)?.setGeoJson(
        Feature.fromGeometry(storePoint)
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_DEST_PIN)?.setGeoJson(
        Feature.fromGeometry(destPoint)
    )
    style.getSourceAs<GeoJsonSource>(SOURCE_PARTNER_PIN)?.setGeoJson(
        Feature.fromGeometry(partnerPoint)
    )
}

private fun fitKochiRouteBounds(
    map: MapLibreMap,
    tracking: OrderTrackingSnapshot
) {
    val lats = listOf(tracking.storeLatitude, tracking.destLatitude, tracking.partnerLatitude)
    val lons = listOf(tracking.storeLongitude, tracking.destLongitude, tracking.partnerLongitude)
    val minLat = (lats.minOrNull() ?: 9.9312) - 0.004
    val maxLat = (lats.maxOrNull() ?: 9.9312) + 0.004
    val minLon = (lons.minOrNull() ?: 76.2673) - 0.004
    val maxLon = (lons.maxOrNull() ?: 76.2673) + 0.004

    val bounds = LatLngBounds.Builder()
        .include(LatLng(minLat, minLon))
        .include(LatLng(maxLat, maxLon))
        .build()

    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 96), 450)
}

private fun createMapPinBitmap(
    context: Context,
    fillColorArgb: Int,
    badgeLabel: String,
    sizeDp: Float,
    highlightHalo: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = max((sizeDp * density).toInt(), 36)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    if (highlightHalo) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(
                64,
                AndroidColor.red(fillColorArgb),
                AndroidColor.green(fillColorArgb),
                AndroidColor.blue(fillColorArgb)
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, sizePx * 0.48f, haloPaint)
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.36f, borderPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColorArgb
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.30f, fillPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = sizePx * 0.24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(badgeLabel, cx, textY, textPaint)

    return bitmap
}

