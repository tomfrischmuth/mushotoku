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

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDateTime

/** Where the last timestamp was written, and in which form. */
internal data class StampAnchor(
    val start: Int,
    val text: String,
    val at: LocalDateTime,
    val withDate: Boolean
)

/**
 * Rewrites the stamp the anchor points at, or returns null when it is no longer
 * untouched — then the button writes a fresh stamp instead.
 *
 * The cursor is allowed to sit one character past the stamp: inserting adds a
 * trailing space, and the cursor comes to rest behind it.
 */
internal fun toggleStamp(
    tfv: TextFieldValue,
    anchor: StampAnchor,
    newStamp: String
): TextFieldValue? {
    val end = anchor.start + anchor.text.length
    if (anchor.start < 0 || end > tfv.text.length) return null
    if (!tfv.text.regionMatches(anchor.start, anchor.text, 0, anchor.text.length)) return null

    val tail = tfv.selection.start - end
    if (tail !in 0..1) return null
    if (tail == 1 && tfv.text.getOrNull(end) != ' ') return null

    val newText = tfv.text.substring(0, anchor.start) + newStamp + tfv.text.substring(end)
    return TextFieldValue(newText, TextRange(anchor.start + newStamp.length + tail))
}
