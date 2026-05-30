package com.example.saleslogdevice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FieldLog ブランドカラー。アイコンと同じ「ネイビー × ティール」を基調にする。
 * primary=ネイビー（主操作・信頼感）、secondary=ティール（訪問・アクセント）、
 * tertiary=グリーン（完了）。
 */
private val BrandNavy = Color(0xFF13386B)
private val BrandTeal = Color(0xFF0E9E90)
private val BrandTealBright = Color(0xFF19C3B2)

private val LightColors = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E5FA),
    onPrimaryContainer = Color(0xFF0A2547),
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBF1EB),
    onSecondaryContainer = Color(0xFF04332D),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC7EBC9),
    onTertiaryContainer = Color(0xFF0A3D10),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF161A1F),
    surface = Color.White,
    onSurface = Color(0xFF161A1F),
    surfaceVariant = Color(0xFFE7ECF2),
    onSurfaceVariant = Color(0xFF55606D),
    outline = Color(0xFFBCC5D0),
    outlineVariant = Color(0xFFD9E0E8),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EC3FF),
    onPrimary = Color(0xFF062B55),
    primaryContainer = Color(0xFF1B416E),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = BrandTealBright,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF0B5249),
    onSecondaryContainer = Color(0xFFA8F3E8),
    tertiary = Color(0xFF8FD98F),
    onTertiary = Color(0xFF0A3D10),
    tertiaryContainer = Color(0xFF1C4D20),
    onTertiaryContainer = Color(0xFFC7EBC9),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE3E6EB),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE3E6EB),
    surfaceVariant = Color(0xFF2A323C),
    onSurfaceVariant = Color(0xFFB9C2CD),
    outline = Color(0xFF5A6470),
    outlineVariant = Color(0xFF3A434E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
        bodyLarge = bodyLarge.copy(lineHeight = 22.sp),
    )
}

@Composable
fun SalesLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
