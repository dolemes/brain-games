package com.dolemes.braingames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

/** Cores de feedback da paleta Okabe-Ito (segura para daltonismo). Use sempre com ✓/✗, nunca sozinhas. */
val CorrectColor = Color(0xFF009E73)
val WrongColor = Color(0xFFD55E00)

// Contraste alto: texto principal acima de 7:1 sobre o fundo nos dois temas.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE8EAF2),
    onSurfaceVariant = Color(0xFF3A3D45),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF1F1F4),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF1F1F4),
    surfaceVariant = Color(0xFF2B2D33),
    onSurfaceVariant = Color(0xFFD5D7DF),
)

// Textos maiores que o padrão do Material: o público inclui idosos.
private val Base = Typography()
private val AppTypography = Typography(
    displayLarge = Base.displayLarge,
    headlineMedium = Base.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp),
    titleLarge = Base.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium = Base.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyLarge = Base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 18.sp, lineHeight = 24.sp),
)

/**
 * Tema do app. [textScale] (1,0 a 1,6, das Configurações) multiplica a escala de fonte do
 * sistema, então o jogador pode aumentar o texto só neste app.
 */
@Composable
fun BrainGamesTheme(
    textScale: Float = 1f,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val base = LocalDensity.current
    val scaled = Density(base.density, base.fontScale * textScale.coerceIn(1f, 1.6f))
    CompositionLocalProvider(LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
