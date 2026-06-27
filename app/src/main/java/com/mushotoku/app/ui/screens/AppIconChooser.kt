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

package com.mushotoku.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.icon.AppIcon
import com.mushotoku.app.icon.IconSwitcher
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppIconSection() {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val colors = LocalAppColors.current

    var selected by remember { mutableStateOf(IconSwitcher.current(context)) }

    SectionLabel(strings.appIconLabel)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            maxItemsInEachRow = 4,
        ) {
            AppIcon.entries.forEach { icon ->
                IconTile(
                    icon = icon,
                    label = labelFor(icon, strings),
                    isSelected = icon == selected,
                    accent = colors.accent,
                    labelColor = colors.onSurface,
                    onClick = {
                        selected = icon
                        IconSwitcher.apply(context, icon)
                    },
                )
            }
        }
    }
}

private fun labelFor(icon: AppIcon, strings: com.mushotoku.app.ui.strings.AppStrings): String =
    when (icon) {
        AppIcon.ORIGINAL -> strings.iconOriginal
        AppIcon.INVERSE -> strings.iconInverse
        AppIcon.WARM_EMBER -> strings.iconWarmEmber
        AppIcon.BLUE -> strings.iconBlue
        AppIcon.RAINBOW -> strings.iconRainbow
        AppIcon.RETRO -> strings.iconRetro
        AppIcon.MINIMALIST -> strings.iconMinimalist
        AppIcon.PLAYFUL -> strings.iconPlayful
    }

@Composable
private fun IconTile(
    icon: AppIcon,
    label: String,
    isSelected: Boolean,
    accent: Color,
    labelColor: Color,
    onClick: () -> Unit,
) {
    val spec = previewSpec(icon)
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(spec.background)
                .then(
                    if (isSelected) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val s = size.minDimension / 108f
                withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                    drawVariant(icon, spec)
                }
            }
        }
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) accent else labelColor,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

private data class PreviewSpec(val background: Color, val ring: Color)

private fun previewSpec(icon: AppIcon): PreviewSpec = when (icon) {
    AppIcon.INVERSE -> PreviewSpec(Color(0xFFF4F1E8), Color(0xFF1C1B19))
    AppIcon.RETRO -> PreviewSpec(Color(0xFF241E16), Color(0xFFECE0C6))
    else -> PreviewSpec(Color(0xFF1C1B19), Color(0xFFF4F1E8))
}

private fun DrawScope.drawVariant(icon: AppIcon, spec: PreviewSpec) {
    val center = Offset(54f, 54f)
    val r = 23f
    val topLeft = Offset(center.x - r, center.y - r)
    val arcSize = Size(r * 2f, r * 2f)

    fun arc(brush: Brush, start: Float, sweep: Float, w: Float, cap: StrokeCap) =
        drawArc(brush, start, sweep, false, topLeft, arcSize, style = Stroke(w, cap = cap))

    fun arc(color: Color, start: Float, sweep: Float, w: Float, cap: StrokeCap) =
        drawArc(color, start, sweep, false, topLeft, arcSize, style = Stroke(w, cap = cap))

    val gStart = pointOnCircle(center, r, -29.0)
    val gEnd = pointOnCircle(center, r, 75.0)

    when (icon) {
        AppIcon.MINIMALIST -> {
            arc(spec.ring, -119f, 303.3f, 3.5f, StrokeCap.Butt)
        }
        AppIcon.PLAYFUL -> {
            val seg = listOf(
                Triple(-119f, 38f, Color(0xFFE0413A)),
                Triple(-69f, 38f, Color(0xFFE8893B)),
                Triple(-19f, 38f, Color(0xFFECC93E)),
                Triple(31f, 38f, Color(0xFF5DA855)),
                Triple(81f, 38f, Color(0xFF3D7FC4)),
                Triple(131f, 38f, Color(0xFFB0508F)),
            )
            seg.forEach { (st, sw, c) -> arc(c, st, sw, 4.9f, StrokeCap.Round) }
        }
        else -> {
            arc(spec.ring, -15f, 199.3f, 5.2f, StrokeCap.Butt)
            arc(gradientFor(icon, gStart, gEnd), -119f, 104f, 5.2f, StrokeCap.Butt)
        }
    }
}

private fun gradientFor(icon: AppIcon, start: Offset, end: Offset): Brush = when (icon) {
    AppIcon.WARM_EMBER -> Brush.linearGradient(
        0f to Color(0xFFEBB44C), 0.35f to Color(0xFFE09850),
        0.7f to Color(0xFFD06A40), 1f to Color(0xFFBC4A38), start = start, end = end,
    )
    AppIcon.BLUE -> Brush.linearGradient(
        0f to Color(0xFF7CC7F2), 0.5f to Color(0xFF4A93D8), 1f to Color(0xFF2A4F94),
        start = start, end = end,
    )
    AppIcon.RAINBOW -> Brush.linearGradient(
        0f to Color(0xFFE0413A), 0.2f to Color(0xFFE8893B), 0.4f to Color(0xFFECC93E),
        0.6f to Color(0xFF5DA855), 0.8f to Color(0xFF3D7FC4), 1f to Color(0xFF7B4FB0),
        start = start, end = end,
    )
    AppIcon.RETRO -> Brush.linearGradient(
        0f to Color(0xFFD9743A), 0.33f to Color(0xFFE3B84A),
        0.66f to Color(0xFF7E9A4F), 1f to Color(0xFF3A8A8A), start = start, end = end,
    )
    else -> Brush.linearGradient(
        0f to Color(0xFFC9453B), 0.25f to Color(0xFFDA6B3D), 0.5f to Color(0xFFE8A33D),
        0.75f to Color(0xFFA3A648), 1f to Color(0xFF5DA855), start = start, end = end,
    )
}

private fun pointOnCircle(center: Offset, r: Float, clockwiseFromTopDeg: Double): Offset {
    val rad = Math.toRadians(clockwiseFromTopDeg)
    return Offset(
        x = center.x + (r * sin(rad)).toFloat(),
        y = center.y - (r * cos(rad)).toFloat(),
    )
}
