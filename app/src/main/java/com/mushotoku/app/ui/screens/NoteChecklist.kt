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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.times
import com.mushotoku.app.ui.StatusGreen
import com.mushotoku.app.ui.StatusRed
import com.mushotoku.app.ui.StatusYellow

/** The same three states the task light uses, written into the note itself. */
internal enum class CheckState(val marker: String, val color: Color) {
    OPEN("- [ ] ", StatusRed),
    DOING("- [/] ", StatusYellow),
    DONE("- [x] ", StatusGreen);

    /** One tap moves on to the next state and wraps around. */
    fun next(): CheckState = when (this) {
        OPEN  -> DOING
        DOING -> DONE
        DONE  -> OPEN
    }
}

internal const val ChecklistPrefixLength = 6

/**
 * Stands in for the box while laying out the text. An em space is used because
 * its width follows the font size, which is what reserves room for the box; it
 * is drawn transparent and the box is painted over it.
 */
internal const val CheckPlaceholder = ' '

/** Box side and check stroke, both relative to the body text. */
private const val BoxSizeFactor    = 1.35f
private const val BoxCornerFactor  = 0.28f
private const val CheckStrokeFactor = 0.13f

/** Optical centre of the line's letters, measured up from the baseline. */
private const val CentreAboveBaseline = 0.36f

internal fun checkStateOf(line: String): CheckState? = when {
    line.startsWith("- [x] ") || line.startsWith("- [X] ") -> CheckState.DONE
    line.startsWith("- [/] ")                              -> CheckState.DOING
    line.startsWith("- [ ] ")                              -> CheckState.OPEN
    else                                                   -> null
}

/** [line] with its box advanced one state, or unchanged if it is not a check line. */
internal fun cycleCheckLine(line: String): String {
    val state = checkStateOf(line) ?: return line
    return state.next().marker + line.substring(ChecklistPrefixLength)
}

/**
 * Drops check items that were never filled in. Without this an empty box is
 * left sitting in the note, and in the read view it even stays tappable.
 */
internal fun dropEmptyCheckItems(rawText: String): String =
    rawText.lines()
        .filterNot { checkStateOf(it) != null && it.substring(ChecklistPrefixLength).isBlank() }
        .joinToString("\n")

/** Start of the line [offset] sits in. */
internal fun lineStartOf(text: String, offset: Int): Int {
    val at = offset.coerceIn(0, text.length)
    return text.lastIndexOf('\n', (at - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
}

/**
 * [offset] moved past the marker of the line it sits in. A caret in the middle
 * of the markup is never what a tap was aiming for.
 *
 * With [allowLineStart] the very start of the line is left alone, so the caret
 * can be put in front of the box. There is nothing to write there, but it is
 * where a backspace has to land to join the item to the line above. The read
 * view has no caret to steer, so it keeps the stricter rule.
 */
internal fun clampOutOfCheckMarker(text: String, offset: Int, allowLineStart: Boolean = false): Int {
    val at = offset.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', (at - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val line = text.substring(lineStart, text.indexOf('\n', lineStart).let { if (it < 0) text.length else it })
    if (checkStateOf(line) == null) return at
    if (allowLineStart && at == lineStart) return at
    val afterMarker = lineStart + ChecklistPrefixLength
    return if (at < afterMarker) afterMarker.coerceAtMost(text.length) else at
}

/**
 * The line whose box was tapped, or null. Found by where the box is drawn
 * rather than by which character the finger came closest to: a tap that lands
 * just beside the box would otherwise slip past it into the text.
 */
internal fun checkBoxLineAt(
    layout: TextLayoutResult,
    position: Offset,
    bodyFontPx: Float,
    rawText: String
): Int? {
    val visible = layout.layoutInput.text.text
    val checkLines = rawText.lines().withIndex().filter { checkStateOf(it.value) != null }.map { it.index }
    var at = visible.indexOf(CheckPlaceholder)
    var i = 0
    while (at >= 0 && i < checkLines.size) {
        val box  = layout.getBoundingBox(at)
        val side = box.right - box.left
        if (side > 0f) {
            val line    = layout.getLineForOffset(at)
            val centerY = layout.getLineBaseline(line) - bodyFontPx * CentreAboveBaseline
            // Half a box of slack all round, and everything to the left of it:
            // there is nothing else out there to hit.
            val pad = side * 0.5f
            val hit = position.x <= box.right + pad &&
                position.y >= centerY - side / 2f - pad &&
                position.y <= centerY + side / 2f + pad
            if (hit) return checkLines[i]
        }
        i++
        at = visible.indexOf(CheckPlaceholder, at + 1)
    }
    return null
}

/** Every check state in [rawText], in the order the lines appear. */
internal fun checkStatesIn(rawText: String): List<CheckState> =
    rawText.lines().mapNotNull { checkStateOf(it) }

/**
 * The state of the least finished item, or null when nothing is left to do.
 * Red outranks yellow; a note whose items are all done needs no marker.
 */
internal fun openCheckState(rawText: String): CheckState? {
    val states = checkStatesIn(rawText)
    return when {
        states.contains(CheckState.OPEN)  -> CheckState.OPEN
        states.contains(CheckState.DOING) -> CheckState.DOING
        else                              -> null
    }
}

/** The placeholder is invisible; only the space it reserves matters. */
internal fun checkPlaceholderStyle(bodySize: TextUnit) =
    SpanStyle(color = Color.Transparent, fontSize = BoxSizeFactor * bodySize)

/**
 * Paints the task light over each placeholder. Drawing rather than composing is
 * forced by the editor: a text field cannot host composables, and both views
 * have to end up identical.
 */
internal fun DrawScope.drawCheckBoxes(
    layout: TextLayoutResult,
    states: List<CheckState>,
    bodyFontPx: Float
) {
    val text = layout.layoutInput.text.text
    var at = text.indexOf(CheckPlaceholder)
    var i = 0
    while (at >= 0 && i < states.size) {
        val box  = layout.getBoundingBox(at)
        val side = box.right - box.left
        if (side > 0f) {
            val line    = layout.getLineForOffset(at)
            val centerY = layout.getLineBaseline(line) - bodyFontPx * CentreAboveBaseline
            val topLeft = Offset(box.left, centerY - side / 2f)
            drawRoundRect(
                color        = states[i].color,
                topLeft      = topLeft,
                size         = Size(side, side),
                cornerRadius = CornerRadius(side * BoxCornerFactor)
            )
            if (states[i] == CheckState.DONE) drawCheckMark(topLeft, side)
        }
        i++
        at = text.indexOf(CheckPlaceholder, at + 1)
    }
}

private fun DrawScope.drawCheckMark(topLeft: Offset, side: Float) {
    val path = Path().apply {
        moveTo(topLeft.x + side * 0.27f, topLeft.y + side * 0.52f)
        lineTo(topLeft.x + side * 0.43f, topLeft.y + side * 0.69f)
        lineTo(topLeft.x + side * 0.74f, topLeft.y + side * 0.32f)
    }
    drawPath(
        path  = path,
        color = Color.White,
        style = Stroke(width = side * CheckStrokeFactor, cap = StrokeCap.Round)
    )
}
