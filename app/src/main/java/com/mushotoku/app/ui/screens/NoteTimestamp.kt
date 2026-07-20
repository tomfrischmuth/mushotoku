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
import com.mushotoku.app.data.Note
import java.time.LocalDateTime

/** The dot that joins date and time, and the marker the box is found by. */
internal const val StampSeparator = " · "

private val TimePattern = Regex("""\d{1,2}:\d{2}(?::\d{2})?(?:[   ]?[AaPp]\.?[Mm]\.?)?""")

private fun isStampWordChar(c: Char) = c.isLetterOrDigit()

/** Characters a localized medium date is built from — "Jul 20, 2026", "20.07.2026". */
private fun isDateChar(c: Char) = c.isLetterOrDigit() || c in ".,-/ "

/**
 * Length of the timestamp starting at [at], or 0 if none does. Only what the
 * stamp button writes is meant to be found: a time, on its own or behind a date
 * and the separating dot.
 */
internal fun stampLengthAt(text: String, at: Int): Int {
    if (at > 0 && isStampWordChar(text[at - 1])) return 0

    fun timeLengthAt(from: Int): Int {
        val m = TimePattern.matchAt(text, from) ?: return 0
        val end = from + m.value.length
        if (end < text.length && isStampWordChar(text[end])) return 0
        return m.value.length
    }

    // "20.07.2026 · 14:32" — the dot is what tells a written date apart from
    // any other words that happen to stand in front of a time.
    val dot = text.indexOf(StampSeparator, at)
    if (dot > at && dot - at <= 20) {
        val date = text.substring(at, dot)
        // A date is three words at most ("Jul 20, 2026"), so the words in front
        // of it stay outside the box.
        val words = date.split(' ')
        if (date.any { it.isDigit() } && date.all { isDateChar(it) } &&
            words.size <= 3 && words.none { it.isEmpty() }
        ) {
            val timeLen = timeLengthAt(dot + StampSeparator.length)
            if (timeLen > 0) return dot + StampSeparator.length + timeLen - at
        }
    }
    return timeLengthAt(at)
}

/** Whether a timestamp stands anywhere in the note. */
internal fun noteHasStamp(note: Note): Boolean =
    textHasStamp(note.title) || textHasStamp(note.content)

private fun textHasStamp(text: String): Boolean {
    var i = 0
    while (i < text.length) {
        val len = stampLengthAt(text, i)
        if (len > 0) return true
        i++
    }
    return false
}

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
