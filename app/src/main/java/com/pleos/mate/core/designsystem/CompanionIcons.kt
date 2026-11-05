package com.pleos.mate.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap

internal enum class Glyph { Home, Missions, Collection, Settings, Tools, Companion, ChevronRight, Check }

@Composable
internal fun CompanionGlyphIcon(glyph: Glyph, modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.085f
        fun line(start: Offset, end: Offset) = drawLine(tint, start, end, stroke, cap = StrokeCap.Round)
        when (glyph) {
            Glyph.Home -> {
                line(Offset(size.width * .16f, size.height * .48f), Offset(size.width * .5f, size.height * .18f))
                line(Offset(size.width * .5f, size.height * .18f), Offset(size.width * .84f, size.height * .48f))
                line(Offset(size.width * .25f, size.height * .44f), Offset(size.width * .25f, size.height * .84f))
                line(Offset(size.width * .75f, size.height * .44f), Offset(size.width * .75f, size.height * .84f))
                line(Offset(size.width * .25f, size.height * .84f), Offset(size.width * .75f, size.height * .84f))
                line(Offset(size.width * .45f, size.height * .84f), Offset(size.width * .45f, size.height * .62f))
                line(Offset(size.width * .45f, size.height * .62f), Offset(size.width * .58f, size.height * .62f))
                line(Offset(size.width * .58f, size.height * .62f), Offset(size.width * .58f, size.height * .84f))
            }
            Glyph.Missions -> {
                drawRoundRect(tint, Offset(size.width * .2f, size.height * .18f), androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .68f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                line(Offset(size.width * .38f, size.height * .18f), Offset(size.width * .38f, size.height * .1f))
                line(Offset(size.width * .38f, size.height * .1f), Offset(size.width * .62f, size.height * .1f))
                line(Offset(size.width * .36f, size.height * .54f), Offset(size.width * .47f, size.height * .65f))
                line(Offset(size.width * .47f, size.height * .65f), Offset(size.width * .67f, size.height * .42f))
            }
            Glyph.Collection -> {
                val left = Path().apply {
                    moveTo(size.width * .5f, size.height * .22f)
                    cubicTo(size.width * .34f, size.height * .12f, size.width * .18f, size.height * .18f, size.width * .16f, size.height * .24f)
                    lineTo(size.width * .16f, size.height * .8f)
                    cubicTo(size.width * .3f, size.height * .72f, size.width * .4f, size.height * .76f, size.width * .5f, size.height * .84f)
                }
                val right = Path().apply {
                    moveTo(size.width * .5f, size.height * .22f)
                    cubicTo(size.width * .66f, size.height * .12f, size.width * .82f, size.height * .18f, size.width * .84f, size.height * .24f)
                    lineTo(size.width * .84f, size.height * .8f)
                    cubicTo(size.width * .7f, size.height * .72f, size.width * .6f, size.height * .76f, size.width * .5f, size.height * .84f)
                }
                drawPath(left, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                drawPath(right, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
                line(Offset(size.width * .5f, size.height * .22f), Offset(size.width * .5f, size.height * .84f))
            }
            Glyph.Settings -> {
                drawCircle(tint, radius = size.minDimension * .14f, center = Offset(size.width * .5f, size.height * .5f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawCircle(tint, radius = size.minDimension * .34f, center = Offset(size.width * .5f, size.height * .5f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                for (index in 0..7) {
                    val angle = index * (Math.PI / 4).toFloat()
                    val inner = size.minDimension * .39f
                    val outer = size.minDimension * .48f
                    line(Offset(size.width * .5f + kotlin.math.cos(angle) * inner, size.height * .5f + kotlin.math.sin(angle) * inner), Offset(size.width * .5f + kotlin.math.cos(angle) * outer, size.height * .5f + kotlin.math.sin(angle) * outer))
                }
            }
            Glyph.Tools -> {
                line(Offset(size.width * .18f, size.height * .78f), Offset(size.width * .72f, size.height * .24f))
                line(Offset(size.width * .62f, size.height * .18f), Offset(size.width * .82f, size.height * .38f))
                drawCircle(tint, size.minDimension * .12f, Offset(size.width * .22f, size.height * .78f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            }
            Glyph.Companion -> {
                drawCircle(tint, size.minDimension * .17f, Offset(size.width * .5f, size.height * .32f))
                drawArc(tint, 200f, 140f, false, Offset(size.width * .2f, size.height * .38f), androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .48f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
            }
            Glyph.ChevronRight -> {
                line(Offset(size.width * .35f, size.height * .18f), Offset(size.width * .68f, size.height * .5f))
                line(Offset(size.width * .68f, size.height * .5f), Offset(size.width * .35f, size.height * .82f))
            }
            Glyph.Check -> {
                line(Offset(size.width * .2f, size.height * .53f), Offset(size.width * .43f, size.height * .75f))
                line(Offset(size.width * .43f, size.height * .75f), Offset(size.width * .82f, size.height * .25f))
            }
        }
    }
}

