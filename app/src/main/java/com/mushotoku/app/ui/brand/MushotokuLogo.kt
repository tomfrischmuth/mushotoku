/*
 * Mushotoku — a privacy-focused, offline productivity app.
 * Copyright (C) 2026 Tom Frischmuth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mushotoku.app.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.cos
import kotlin.math.sin

object MushotokuBrand {
    val Dark = Color(0xFF1C1B19)
    val OffWhite = Color(0xFFF4F1E8)
    val Red = Color(0xFFC9453B)
    val Orange = Color(0xFFDA6B3D)
    val Amber = Color(0xFFE8A33D)
    val YellowGreen = Color(0xFFA3A648)
    val Green = Color(0xFF5DA855)
}

@Composable
fun MushotokuRing(
    modifier: Modifier = Modifier,
    ringColor: Color = MushotokuBrand.OffWhite,
    strokeFraction: Float = 0.226f,
) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val r = (size.minDimension / 2f) * 0.82f
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = r * strokeFraction
        drawEnso(center = center, radius = r, strokeWidth = stroke, ringColor = ringColor)
    }
}

@Composable
fun MushotokuWordmark(
    modifier: Modifier = Modifier,
    letterColor: Color = MushotokuBrand.OffWhite,
) {
    Canvas(modifier = modifier.aspectRatio(1960f / 440f)) {
        val s = size.width / 1960f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            val stroke = Stroke(
                width = 30f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            LetterPaths.forEach { d ->
                drawPath(
                    path = PathParser().parsePathString(d).toPath(),
                    color = letterColor,
                    style = stroke,
                )
            }
            drawEnso(Offset(1035f, 260f), radius = 75f, strokeWidth = 30f, ringColor = letterColor)
            drawEnso(Offset(1405f, 260f), radius = 75f, strokeWidth = 30f, ringColor = letterColor, mirrored = true)
        }
    }
}

private fun DrawScope.drawEnso(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    ringColor: Color,
    mirrored: Boolean = false,
) {
    val render: DrawScope.() -> Unit = {
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        val cap = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

        drawArc(
            color = ringColor,
            startAngle = -15f,
            sweepAngle = 199.3f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = cap,
        )

        val gradStart = pointOnCircle(center, radius, -29.0)
        val gradEnd = pointOnCircle(center, radius, 75.0)
        drawArc(
            brush = Brush.linearGradient(
                0.0f to MushotokuBrand.Red,
                0.25f to MushotokuBrand.Orange,
                0.5f to MushotokuBrand.Amber,
                0.75f to MushotokuBrand.YellowGreen,
                1.0f to MushotokuBrand.Green,
                start = gradStart,
                end = gradEnd,
            ),
            startAngle = -119f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = cap,
        )
    }
    if (mirrored) {
        withTransform({ scale(-1f, 1f, pivot = center) }) { render() }
    } else {
        render()
    }
}

private fun pointOnCircle(center: Offset, r: Float, clockwiseFromTopDeg: Double): Offset {
    val rad = Math.toRadians(clockwiseFromTopDeg)
    return Offset(
        x = center.x + (r * sin(rad)).toFloat(),
        y = center.y - (r * cos(rad)).toFloat(),
    )
}

private val LetterPaths = listOf(
    "M 80 350 L 80 225 A 55 55 0 0 1 190 225 L 190 350 M 190 225 A 55 55 0 0 1 300 225 L 300 350",
    "M 370 170 L 370 295 A 55 55 0 0 0 480 295 L 480 170",
    "M 654 192.5 A 45 45 0 1 0 615 260 A 45 45 0 1 1 576 327.5",
    "M 750 350 L 750 90 M 750 225 A 55 55 0 0 1 860 225 L 860 350",
    "M 1185 120 L 1185 330 Q 1185 350 1205 350 M 1140 170 L 1245 170",
    "M 1560 350 L 1560 90 M 1655 170 L 1575 250 M 1595 235 L 1665 350",
    "M 1740 170 L 1740 295 A 55 55 0 0 0 1850 295 L 1850 170",
)
