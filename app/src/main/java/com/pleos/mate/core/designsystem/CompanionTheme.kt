package com.pleos.mate.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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
    val colors = darkColorScheme(
        primary = Color(0xFFD3C29C), onPrimary = Color(0xFF34332F), primaryContainer = Color(0xFFB8A982),
        onPrimaryContainer = Color(0xFF292923),
        secondary = Color(0xFFD5CCB9), onSecondary = Color(0xFF34332F),
        secondaryContainer = Color(0xFF68605A), onSecondaryContainer = Color(0xFFFFFCF6),
        tertiary = Color(0xFFBEC9B9), onTertiary = Color(0xFF30382E),
        tertiaryContainer = Color(0xFF535E50), onTertiaryContainer = Color(0xFFF0F3EA),
        onBackground = Color(0xFFFFFCF6), surfaceTint = Color(0xFFD3C29C),
        surfaceContainerLowest = Color(0xFF373432), surfaceContainerLow = Color(0xFF45413E),
        surfaceContainer = Color(0xFF514D49), surfaceContainerHigh = Color(0xFF5B5652),
        surfaceContainerHighest = Color(0xFF65605B),
        background = Color(0xFF484542), surface = Color(0xFF514D49), onSurface = Color(0xFFFFFCF6),
        surfaceVariant = Color(0xFF514D49), onSurfaceVariant = Color(0xFFE6E0D9),
        outline = Color(0xFFD5CCB9), outlineVariant = Color(0xFF827A72)
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
            androidx.compose.material3.LocalRippleConfiguration provides null,
            androidx.compose.foundation.LocalIndication provides NoRippleIndication,
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

private object NoRippleIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource):
        androidx.compose.ui.node.DelegatableNode = object : Modifier.Node() {}
}
