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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

private val PillPaddingH = 9.dp

/**
 * Height as a multiple of the font size, and how far above the baseline the
 * letters have their optical centre.
 *
 * Both are measured from the baseline rather than the line box: the line box is
 * taller than the letters and its extra space is distributed proportionally to
 * the font's ascent and descent, so its middle sits well above the middle of the
 * word.
 *
 * The centre sits half a cap height above the baseline. Reserving room for
 * descenders instead would leave the pill hanging low under the many tags that
 * have none, which is more noticeable than the tighter fit under a "j".
 */
private const val PillHeightFactor    = 1.5f
private const val PillCentreAboveBase = 0.36f

/**
 * Draws the rounded, filled pill behind every finished tag. A span style could
 * only paint a rectangle, so the shape has to be drawn from the text layout.
 */
internal fun DrawScope.drawTagPills(
    layout: TextLayoutResult,
    fill: Color,
    fontSize: TextUnit
) {
    // The renderer marked the tags it wants a pill for; re-detecting them here
    // could not tell an escaped hash from a real tag.
    val annotated = layout.layoutInput.text
    val ranges = annotated
        .getStringAnnotations(TagPillAnnotation, 0, annotated.length)
        .map { it.start until it.end }
    val padH   = PillPaddingH.toPx()
    val fontPx = fontSize.toPx()
    val height = fontPx * PillHeightFactor
    ranges.forEach { range ->
        if (range.last >= layout.layoutInput.text.length) return@forEach
        val line = layout.getLineForOffset(range.first)
        // A tag broken across two lines would need two pills; leave it plain.
        if (line != layout.getLineForOffset(range.last)) return@forEach

        val start = layout.getBoundingBox(range.first)
        val end   = layout.getBoundingBox(range.last)
        val width = (end.right + padH) - (start.left - padH)
        if (width <= 0f) return@forEach

        val centerY = layout.getLineBaseline(line) - fontPx * PillCentreAboveBase
        val topLeft = Offset(start.left - padH, centerY - height / 2f)
        drawRoundRect(
            color        = fill,
            topLeft      = topLeft,
            size         = Size(width, height),
            cornerRadius = CornerRadius(height / 2f)
        )
    }
}
