package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MediRelayLightColors = lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = OnMedicalBluePrimary,
    primaryContainer = MedicalBlueContainer,
    onPrimaryContainer = OnMedicalBlueContainer,
    secondary = SteelBlueSecondary,
    onSecondary = OnSteelBlueSecondary,
    secondaryContainer = SteelBlueContainer,
    onSecondaryContainer = OnSteelBlueContainer,
    tertiary = CeruleanTertiary,
    onTertiary = OnCeruleanTertiary,
    tertiaryContainer = CeruleanContainer,
    onTertiaryContainer = OnCeruleanContainer,
    background = CanvasBackground,
    onBackground = OnCanvasText,
    surface = SurfaceCard,
    onSurface = OnCanvasText,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = MutedText,
    outline = OutlineSubtle,
    error = ErrorCrimson,
    errorContainer = ErrorContainerLight
)

private val MediRelayDarkColors = darkColorScheme(
    primary = DarkMedicalBluePrimary,
    onPrimary = DarkOnMedicalBluePrimary,
    primaryContainer = DarkMedicalBlueContainer,
    onPrimaryContainer = DarkOnMedicalBlueContainer,
    secondary = DarkSteelBlueSecondary,
    onSecondary = DarkOnMedicalBluePrimary,
    secondaryContainer = DarkSteelBlueContainer,
    onSecondaryContainer = DarkOnCanvasText,
    tertiary = DarkCeruleanTertiary,
    onTertiary = DarkOnMedicalBluePrimary,
    tertiaryContainer = DarkCeruleanContainer,
    onTertiaryContainer = DarkOnCanvasText,
    background = DarkCanvasBackground,
    onBackground = DarkOnCanvasText,
    surface = DarkSurfaceCard,
    onSurface = DarkOnCanvasText,
    surfaceVariant = DarkSurfaceSubtle,
    onSurfaceVariant = DarkMutedText,
    outline = DarkOutlineSubtle
)

val MediRelayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun MediRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MediRelayDarkColors else MediRelayLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MediRelayTypography,
        shapes = MediRelayShapes,
        content = content
    )
}
