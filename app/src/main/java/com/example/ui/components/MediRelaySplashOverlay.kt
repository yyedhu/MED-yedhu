package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val BRAND_WORDMARK = "MediRelay"

@Composable
fun MediRelaySplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emblemScale = remember { Animatable(0.55f) }
    val emblemAlpha = remember { Animatable(0f) }
    val letterProgress = remember { List(BRAND_WORDMARK.length) { Animatable(0f) } }
    val relayProgress = remember { Animatable(0f) }
    var showSubtitle by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_beacon")
    val pulseRadiusFraction by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_ring_radius"
    )

    LaunchedEffect(Unit) {
        launch {
            emblemAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        delay(140L)
        letterProgress.forEachIndexed { index, anim ->
            launch {
                delay(index * 55L)
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        delay(420L)
        showSubtitle = true
        relayProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 980, easing = FastOutSlowInEasing)
        )

        delay(280L)
        onFinished()
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val backdropBrush = remember(primary, tertiary) {
        Brush.verticalGradient(
            colors = listOf(
                primary,
                primary.copy(alpha = 0.94f),
                tertiary
            )
        )
    }

    val appName = stringResource(R.string.app_name)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backdropBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished
            )
            .testTag("splash_overlay")
    ) {
        // Ambient medical radar pulse rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            val maxRadius = size.minDimension * 0.44f

            drawCircle(
                color = onPrimary.copy(alpha = (1f - pulseRadiusFraction) * 0.18f),
                radius = maxRadius * pulseRadiusFraction,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            val secondaryFraction = (pulseRadiusFraction + 0.48f) % 1f
            drawCircle(
                color = primaryContainer.copy(alpha = (1f - secondaryFraction) * 0.14f),
                radius = maxRadius * secondaryFraction,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(horizontal = 32.dp)
        ) {
            // Animated Pharmacy Emblem
            Surface(
                color = onPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, onPrimary.copy(alpha = 0.34f)),
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                        alpha = emblemAlpha.value
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        color = onPrimary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.LocalPharmacy,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Staggered Letter-by-Letter Animated "MediRelay" App Name
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        contentDescription = appName
                    }
                    .testTag("splash_brand_title")
            ) {
                BRAND_WORDMARK.forEachIndexed { index, ch ->
                    val progress = letterProgress[index].value
                    val isRelayPart = index >= 4
                    Text(
                        text = ch.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 42.sp,
                            lineHeight = 48.sp,
                            letterSpacing = (-0.6).sp,
                            fontWeight = if (isRelayPart) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isRelayPart) primaryContainer else onPrimary,
                        modifier = Modifier.graphicsLayer {
                            alpha = progress.coerceIn(0f, 1f)
                            translationY = (1f - progress) * 28.dp.toPx()
                            val pop = 0.78f + (0.22f * progress)
                            scaleX = pop
                            scaleY = pop
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = MaterialTheme.typography.titleSmall,
                        color = onPrimary.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )

                    // Animated Relay Progress Track + Gliding Rider Beacon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.width(220.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 3.dp)
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                            ) {
                                val y = size.height / 2f
                                drawLine(
                                    color = onPrimary.copy(alpha = 0.22f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = size.height,
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = primaryContainer,
                                    start = Offset(0f, y),
                                    end = Offset(size.width * relayProgress.value, y),
                                    strokeWidth = size.height,
                                    cap = StrokeCap.Round
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        val travelPx = 200.dp.toPx() * relayProgress.value
                                        IntOffset(travelPx.roundToInt(), 0)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TwoWheeler,
                                    contentDescription = null,
                                    tint = primaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.splash_loading_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = onPrimary.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
