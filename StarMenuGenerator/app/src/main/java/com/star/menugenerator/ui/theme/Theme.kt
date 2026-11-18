package com.star.menugenerator.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colours sampled from the official STAR logo and the supplied purple template.
val StarPurple = Color(0xFF57266F)
val StarDeep = Color(0xFF351448)
val StarYellow = Color(0xFFFFC928)
val StarPink = Color(0xFFE91E85)
val StarBg = Color(0xFFFAF8FC)
val StarLine = Color(0xFFEADCF2)
val StarMuted = Color(0xFF7A6A88)
val StarOk = Color(0xFF1B6E3C)
val StarWarn = Color(0xFF8A6100)

private val Scheme = lightColorScheme(
    primary = StarPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E6F7),
    onPrimaryContainer = StarDeep,
    secondary = StarYellow,
    onSecondary = StarDeep,
    tertiary = StarPink,
    background = StarBg,
    onBackground = Color(0xFF2A1836),
    surface = Color.White,
    onSurface = Color(0xFF2A1836),
    surfaceVariant = Color(0xFFF6EFFA),
    onSurfaceVariant = StarMuted,
    outline = Color(0xFFD2BFE0),
    outlineVariant = StarLine,
    error = Color(0xFFB3261E),
)

private val StarTypography = Typography().copy(
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
)

private val StarShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun StarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = StarTypography, shapes = StarShapes, content = content)
}
