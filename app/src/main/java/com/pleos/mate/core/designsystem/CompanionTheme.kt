package com.pleos.mate.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pleos.mate.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun CompanionTheme(content: @Composable () -> Unit) {
    val astaSans = remember {
        FontFamily(
            Font(R.font.asta_sans_regular, FontWeight.Normal),
            Font(R.font.asta_sans_bold, FontWeight.Bold)
        )
    }
    val colors = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFFFFDCA8), onPrimary = Color(0xFF382A20), primaryContainer = Color(0xFFB8A982),
        onPrimaryContainer = Color(0xFF292923),
        secondary = Color(0xFFD5CCB9), onSecondary = Color(0xFF382A20),
        secondaryContainer = Color(0xFF48564F), onSecondaryContainer = Color(0xFFFFFCF6),
        tertiary = Color(0xFFBEC9B9), onTertiary = Color(0xFF30382E),
        tertiaryContainer = Color(0xFF45594E), onTertiaryContainer = Color(0xFFF0F3EA),
        onBackground = Color(0xFFFFFCF6), surfaceTint = Color(0xFFFFD49B),
        surfaceContainerLowest = Color(0xFF202827), surfaceContainerLow = Color(0xFF2A3431),
        surfaceContainer = Color(0xFF353C3B), surfaceContainerHigh = Color(0xFF404C47),
        surfaceContainerHighest = Color(0xFF4B5952),
        background = Color(0xFF20352B), surface = Color(0xFF3C5144), onSurface = Color(0xFFFFFCF6),
        surfaceVariant = Color(0xFF353C3B), onSurfaceVariant = Color(0xFFDBDFD9),
        outline = Color(0xFFD5CCB9), outlineVariant = Color(0xFF78827B)
    ) else lightColorScheme(
        primary = Color(0xFF426D54), onPrimary = Color.White,
        primaryContainer = Color(0xFFD5E8D4), onPrimaryContainer = Color(0xFF163E29),
        secondary = Color(0xFF586B59), onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2EBDD), onSecondaryContainer = Color(0xFF263D2E),
        tertiary = Color(0xFF875D39), onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFE1BD), onTertiaryContainer = Color(0xFF503519),
        background = Color(0xFFF3F5EB), onBackground = Color(0xFF263C2E),
        surface = Color(0xFFFFFCF4), onSurface = Color(0xFF263C2E),
        surfaceVariant = Color(0xFFE3EBDD), onSurfaceVariant = Color(0xFF526457),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFFDF7),
        surfaceContainer = Color(0xFFF5F7EF), surfaceContainerHigh = Color(0xFFEDF2E8),
        surfaceContainerHighest = Color(0xFFE2EBDD),
        surfaceTint = Color(0xFF426D54), outline = Color(0xFF6C8070),
        outlineVariant = Color(0xFFCCD9CA)
    )
    val baseTypography = Typography()
    val typography = Typography(
        displayLarge = baseTypography.displayLarge.withFont(astaSans),
        displayMedium = baseTypography.displayMedium.withFont(astaSans),
        displaySmall = baseTypography.displaySmall.withFont(astaSans),
        headlineLarge = baseTypography.headlineLarge.copy(fontSize = 42.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold).withFont(astaSans),
        headlineMedium = baseTypography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold).withFont(astaSans),
        headlineSmall = baseTypography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp).withFont(astaSans),
        titleLarge = baseTypography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold).withFont(astaSans),
        titleMedium = baseTypography.titleMedium.copy(fontSize = 20.sp, lineHeight = 28.sp).withFont(astaSans),
        titleSmall = baseTypography.titleSmall.withFont(astaSans),
        bodyLarge = baseTypography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp).withFont(astaSans),
        bodyMedium = baseTypography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 28.sp).withFont(astaSans),
        bodySmall = baseTypography.bodySmall.copy(fontSize = 14.sp, lineHeight = 22.sp).withFont(astaSans),
        labelLarge = baseTypography.labelLarge.copy(fontSize = 18.sp, lineHeight = 26.sp).withFont(astaSans),
        labelMedium = baseTypography.labelMedium.copy(fontSize = 16.sp, lineHeight = 24.sp).withFont(astaSans),
        labelSmall = baseTypography.labelSmall.copy(fontSize = 14.sp, lineHeight = 22.sp).withFont(astaSans)
    )
    val shapes = Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(22.dp),
        large = RoundedCornerShape(26.dp),
        extraLarge = RoundedCornerShape(30.dp)
    )
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalRippleConfiguration provides androidx.compose.material3.RippleConfiguration(),
            androidx.compose.foundation.LocalIndication provides androidx.compose.material3.ripple(),
            LocalContentColor provides colors.onSurface,
            LocalTextStyle provides typography.bodyMedium,
            content = content
        )
    }
}

private fun TextStyle.withFont(fontFamily: FontFamily): TextStyle = copy(
    fontFamily = fontFamily,
    color = Color.Unspecified
)
