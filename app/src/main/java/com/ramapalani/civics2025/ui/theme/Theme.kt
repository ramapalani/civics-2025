package com.ramapalani.civics2025.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1B365D)
val Cream = Color(0xFFF6F1E8)
val Gold = Color(0xFFC5A46E)
val FlagRed = Color(0xFFB22234)
val DeepGreen = Color(0xFF2E5A3C)

private val scheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Navy,
    tertiary = FlagRed,
    background = Cream,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8E0D2),
    error = FlagRed,
)

@Composable
fun CivicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
