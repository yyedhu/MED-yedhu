package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.unit.Dp
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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circlePitchAlignment
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
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
private const val LAYER_PARTNER_PULSE = "kochi-partner-pulse-layer"
private const val LAYER_PARTNER_PIN = "kochi-partner-pin-layer"
private const val ICON_PARTNER_PIN = "kochi-partner-pin-icon"

data class RouteGeoBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

object RealTimeTrackingMath {
    fun interpolateCoordinate(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        fraction: Float
    ): Pair<Double, Double> {
        val t = fraction.coerceIn(0f, 1f).toDouble()
        return (startLat + (endLat - startLat) * t) to (startLon + (endLon - startLon) * t)
    }

    fun computeRouteBoundingBox(
        storeLat: Double,
        storeLon: Double,
        destLat: Double,
        destLon: Double,
        partnerLat: Double,
        partnerLon: Double,
        paddingDegrees: Double = 0.004
    ): RouteGeoBounds {
        val lats = listOf(storeLat, destLat, partnerLat)
        val lons = listOf(storeLon, destLon, partnerLon)
        return RouteGeoBounds(
            minLat = (lats.minOrNull() ?: 9.9312) - paddingDegrees,
            maxLat = (lats.maxOrNull() ?: 9.9312) + paddingDegrees,
            minLon = (lons.minOrNull() ?: 76.2673) - paddingDegrees,
            maxLon = (lons.maxOrNull() ?: 76.2673) + paddingDegrees
        )
    }
}

@Composable
fun RealTimeOrderTrackingCard(
    order: MedicineOrder,
    liveTransitFraction: Float?,
    liveTracking: LiveOrderTracking? = null,
    isMapPaused: Boolean = false,
    onOpenFullDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracking = remember(order, liveTransitFraction, liveTracking) {
        DeliveryTrackerEngine.buildTrackingSnapshot(
            order = order,
            liveTransitFraction = liveTransitFraction,
            liveTracking = liveTracking
        )
    }

    val pulseTransition = rememberInfiniteTransition(label = "liveDotPulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_order_tracking_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Status + ETA Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (tracking.status.isTerminal) 1f else dotAlpha
                                    )
                                )
                        )
                        StatusPill(
                            text = if (tracking.hasRiderArrived) "RIDER ARRIVED" else "REAL-TIME GPS TRACKING",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            tracking.status == OrderStatus.DELIVERED -> "Delivered to ${tracking.destinationArea}"
                            tracking.hasRiderArrived -> "${tracking.partner.name} arrived at ${tracking.destinationArea}!"
                            tracking.status == OrderStatus.OUT_FOR_DELIVERY -> "Arriving in ${tracking.remainingMinutes} mins"
                            else -> "${tracking.storeName} • ${tracking.status.displayLabel}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tracking.currentLocationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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

            // Milestone Notification Strip
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = tracking.customerNotificationText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Embedded Real-Time MapLibre Map (OpenFreeMap)
            RealTimeOrderTrackingMap(
                tracking = tracking,
                mapHeight = 224.dp,
                isPaused = isMapPaused
            )

            // Compact Fulfillment Progress
            OrderStatusStepper(currentStatus = tracking.status)

            // Delivery Person Telemetry + Actions
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.TwoWheeler,
                                    contentDescription = "Delivery person",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
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
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(11.dp)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tracking.gpsStatusLabel} • ${tracking.formattedGpsCoordinates}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val dialIntent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${tracking.partner.phone}")
                                )
                                context.startActivity(dialIntent)
                            },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("realtime_call_partner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call delivery partner",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Partner", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = onOpenFullDetails,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 48.dp)
                                .testTag("open_full_tracking_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInFull,
                                contentDescription = "Open full tracking sheet",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Timeline", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeOrderTrackingMap(
    tracking: OrderTrackingSnapshot,
    mapHeight: Dp = 256.dp,
    isPaused: Boolean = false,
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
    var followRiderMode by remember(tracking.orderId) { mutableStateOf(false) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    // 64-bit smooth coordinate interpolation so rider glides between Firestore GPS updates
    var displayedPartnerLat by remember(tracking.orderId) { mutableDoubleStateOf(tracking.partnerLatitude) }
    var displayedPartnerLon by remember(tracking.orderId) { mutableDoubleStateOf(tracking.partnerLongitude) }
    val glideAnim = remember(tracking.orderId) { Animatable(1f) }

    LaunchedEffect(tracking.partnerLatitude, tracking.partnerLongitude) {
        val startLat = displayedPartnerLat
        val startLon = displayedPartnerLon
        val targetLat = tracking.partnerLatitude
        val targetLon = tracking.partnerLongitude
        if (startLat == targetLat && startLon == targetLon) return@LaunchedEffect

        glideAnim.snapTo(0f)
        glideAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        ) {
            val (lat, lon) = RealTimeTrackingMath.interpolateCoordinate(
                startLat = startLat,
                startLon = startLon,
                endLat = targetLat,
                endLon = targetLon,
                fraction = value
            )
            displayedPartnerLat = lat
            displayedPartnerLon = lon
        }
    }

    // Pulsing GPS radar ring around the delivery partner marker
    val pulseTransition = rememberInfiniteTransition(label = "partnerMapPulse")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "partnerRadarProgress"
    )

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
                updateOpenFreeMapSources(
                    style = style,
                    tracking = tracking,
                    partnerLat = displayedPartnerLat,
                    partnerLon = displayedPartnerLon
                )
                loadedStyle = style
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!isPaused) mapView.onStart()
                Lifecycle.Event.ON_RESUME -> if (!isPaused) mapView.onResume()
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

    LaunchedEffect(isPaused, mapView) {
        if (isPaused) {
            mapView.onPause()
        } else {
            mapView.onStart()
            mapView.onResume()
        }
    }

    LaunchedEffect(loadedStyle, tracking, displayedPartnerLat, displayedPartnerLon) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (style.isFullyLoaded) {
            updateOpenFreeMapSources(
                style = style,
                tracking = tracking,
                partnerLat = displayedPartnerLat,
                partnerLon = displayedPartnerLon
            )
        }
    }

    LaunchedEffect(loadedStyle, pulseProgress, tracking.status, tracking.hasRiderArrived) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (!style.isFullyLoaded) return@LaunchedEffect
        val showPulse = !tracking.status.isTerminal && !tracking.hasRiderArrived
        style.getLayerAs<CircleLayer>(LAYER_PARTNER_PULSE)?.setProperties(
            circleRadius(14f + 16f * pulseProgress),
            circleOpacity(if (showPulse) (1f - pulseProgress) * 0.42f else 0f)
        )
    }

    LaunchedEffect(
        mapLibreMap,
        tracking.orderId,
        followRiderMode,
        recenterTrigger,
        tracking.partnerLatitude,
        tracking.partnerLongitude
    ) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (followRiderMode) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(tracking.partnerLatitude, tracking.partnerLongitude),
                    15.2
                ),
                600
            )
        } else {
            fitKochiRouteBounds(map = map, tracking = tracking)
        }
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
                    .height(mapHeight)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "OpenFreeMap • ${if (tracking.isSyncedFromDatabase) "Firestore GPS" else "Kochi Live"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Top-right Follow Rider toggle + Recenter controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Surface(
                        color = if (followRiderMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        contentColor = if (followRiderMode) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { followRiderMode = !followRiderMode }
                            .testTag("follow_rider_toggle")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TwoWheeler,
                                contentDescription = "Toggle follow delivery partner camera",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (followRiderMode) "Following" else "Follow Rider",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shape = CircleShape,
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = {
                                followRiderMode = false
                                recenterTrigger++
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("recenter_openfreemap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Fit full Kochi route on map",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom-left OpenStreetMap / OpenFreeMap attribution badge
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

                // Bottom-right Compact Pin Legend
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Rx Store • ▶ Rider • H You",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                    text = if (!tracking.status.isTerminal && !tracking.hasRiderArrived) {
                        "→ ${tracking.remainingDistanceKm} km →"
                    } else {
                        "→"
                    },
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
        CircleLayer(LAYER_PARTNER_PULSE, SOURCE_PARTNER_PIN).withProperties(
            circleColor(primaryArgb),
            circleRadius(18f),
            circleOpacity(0.32f),
            circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP)
        )
    )
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
    tracking: OrderTrackingSnapshot,
    partnerLat: Double,
    partnerLon: Double
) {
    val storePoint = Point.fromLngLat(tracking.storeLongitude, tracking.storeLatitude)
    val destPoint = Point.fromLngLat(tracking.destLongitude, tracking.destLatitude)
    val partnerPoint = Point.fromLngLat(partnerLon, partnerLat)

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
    val box = RealTimeTrackingMath.computeRouteBoundingBox(
        storeLat = tracking.storeLatitude,
        storeLon = tracking.storeLongitude,
        destLat = tracking.destLatitude,
        destLon = tracking.destLongitude,
        partnerLat = tracking.partnerLatitude,
        partnerLon = tracking.partnerLongitude
    )
    val bounds = LatLngBounds.Builder()
        .include(LatLng(box.minLat, box.minLon))
        .include(LatLng(box.maxLat, box.maxLon))
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
