package com.jerrywolff.androidpeoplesearch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A67), onPrimary = Color.White,
    secondary = Color(0xFF825500), tertiary = Color(0xFF9B4050),
    background = Color(0xFFF8FAF7), surface = Color(0xFFF8FAF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF58D9D3), secondary = Color(0xFFFFBA49), tertiary = Color(0xFFFFB1BD),
    background = Color(0xFF171C1B), surface = Color(0xFF171C1B),
)

@Composable
fun AndroidPeopleSearchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, typography = Typography(), content = content)
}
