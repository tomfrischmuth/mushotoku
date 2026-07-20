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

private val NumberedPrefix = Regex("""^(\d{1,9})\. """)

/** Length of a leading "12. " marker, or 0 when the line does not start one. */
internal fun numberedPrefixLength(line: String): Int =
    NumberedPrefix.find(line)?.value?.length ?: 0

/** The number a "12. " marker carries, or null when there is none. */
internal fun numberedPrefixValue(line: String): Int? =
    NumberedPrefix.find(line)?.groupValues?.get(1)?.toIntOrNull()

/** The toolbar format the given line currently carries. */
internal fun activeToolbarFormat(line: String): String = when {
    line.startsWith("### ")     -> "h3"
    line.startsWith("## ")      -> "h2"
    line.startsWith("# ")       -> "h1"
    checkStateOf(line) != null  -> "check"
    line.startsWith("- ")       -> "dash"
    line.startsWith("* ")       -> "bullet"
    numberedPrefixLength(line) > 0 -> "number"
    else                        -> "text"
}

/**
 * Numbers the current line, continuing from the line above instead of always
 * restarting at one.
 */
internal fun applyNumberedPrefix(tfv: TextFieldValue): TextFieldValue {
    val text      = tfv.text
    val cursor    = tfv.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val prevEnd   = lineStart - 1
    val next = if (prevEnd <= 0) 1 else {
        val prevStart = text.lastIndexOf('\n', prevEnd - 1) + 1
        (numberedPrefixValue(text.substring(prevStart, prevEnd))?.plus(1)) ?: 1
    }
    return applyLinePrefix(tfv, "$next. ")
}

internal fun applyLinePrefix(tfv: TextFieldValue, prefix: String): TextFieldValue {
    val text = tfv.text
    val cursor = tfv.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val lineContent = text.substring(lineStart, lineEnd)
    val stripped = when {
        lineContent.startsWith("### ") -> lineContent.substring(4)
        lineContent.startsWith("## ")  -> lineContent.substring(3)
        lineContent.startsWith("# ")   -> lineContent.substring(2)
        lineContent.startsWith("> ")   -> lineContent.substring(2)
        checkStateOf(lineContent) != null -> lineContent.substring(ChecklistPrefixLength)
        lineContent.startsWith("- ")   -> lineContent.substring(2)
        lineContent.startsWith("* ")   -> lineContent.substring(2)
        numberedPrefixLength(lineContent) > 0 -> lineContent.substring(numberedPrefixLength(lineContent))
        else                           -> lineContent
    }
    val newLine = if (prefix.isEmpty()) stripped else "$prefix$stripped"
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val newCursor = (cursor + newLine.length - lineContent.length).coerceAtLeast(lineStart)
    return TextFieldValue(newText, TextRange(newCursor))
}

/**
 * Drops [insert] in at the cursor (replacing any selection) and pads it with
 * spaces where it would otherwise collide with surrounding words.
 */
internal fun insertAtCursor(tfv: TextFieldValue, insert: String): TextFieldValue {
    val text = tfv.text
    val start = tfv.selection.min.coerceIn(0, text.length)
    val end   = tfv.selection.max.coerceIn(0, text.length)
    val needsLeadingSpace  = start > 0 && text[start - 1] !in " \n\t"
    val needsTrailingSpace = end >= text.length || text[end] !in " \n\t"
    val piece = buildString {
        if (needsLeadingSpace) append(' ')
        append(insert)
        if (needsTrailingSpace) append(' ')
    }
    val newText = text.substring(0, start) + piece + text.substring(end)
    return TextFieldValue(newText, TextRange(start + piece.length))
}

private fun isWrappedWith(content: String, marker: String): Boolean {
    if (content.length < marker.length * 2 + 1) return false
    if (!content.startsWith(marker) || !content.endsWith(marker)) return false
    if (marker == "*" && (content.startsWith("**") || content.endsWith("**"))) return false
    return true
}

private fun hasWordContent(s: String) = s.any { it.isLetter() || it.isDigit() }

internal fun deleteWordBackward(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val sel  = tfv.selection
    if (!sel.collapsed) {
        val newText = text.substring(0, sel.min) + text.substring(sel.max)
        return TextFieldValue(newText, TextRange(sel.min))
    }
    val cursor = sel.start
    if (cursor == 0) return tfv
    var i = cursor - 1
    while (i >= 0 && text[i] == ' ') i--
    if (i >= 0 && text[i] == '\n') {
        i--
    } else {
        while (i >= 0 && text[i] != ' ' && text[i] != '\n') i--
    }
    val deleteFrom = i + 1
    val newText = text.substring(0, deleteFrom) + text.substring(cursor)
    return TextFieldValue(newText, TextRange(deleteFrom))
}

internal fun applyInlineFormat(tfv: TextFieldValue, marker: String): TextFieldValue {
    val text = tfv.text
    val sel  = tfv.selection

    if (!sel.collapsed) {
        val s = sel.min; val e = sel.max
        val selected = text.substring(s, e)
        val ml = marker.length
        val surroundedByMarker = s >= ml && e + ml <= text.length &&
            text.substring(s - ml, s) == marker &&
            text.substring(e, e + ml) == marker &&
            (ml > 1 || ((s < 2 || text[s - 2] != '*') && (e + 1 >= text.length || text[e + 1] != '*')))
        return when {
            isWrappedWith(selected, marker) -> {
                val inner   = selected.substring(ml, selected.length - ml)
                val newText = text.substring(0, s) + inner + text.substring(e)
                TextFieldValue(newText, TextRange(s, s + inner.length))
            }
            surroundedByMarker -> {
                val newText = text.substring(0, s - ml) + selected + text.substring(e + ml)
                TextFieldValue(newText, TextRange(s - ml, s - ml + selected.length))
            }
            !hasWordContent(selected) -> tfv
            else -> {
                val newText = text.substring(0, s) + marker + selected + marker + text.substring(e)
                TextFieldValue(newText, TextRange(s + ml, e + ml))
            }
        }
    }

    val cursor    = sel.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val lineEnd   = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
    val line      = text.substring(lineStart, lineEnd)
    val contentOffset = when {
        line.startsWith("### ") -> 4
        line.startsWith("## ")  -> 3
        line.startsWith("# ")   -> 2
        line.startsWith("> ")   -> 2
        checkStateOf(line) != null -> ChecklistPrefixLength
        line.startsWith("- ")   -> 2
        line.startsWith("* ")   -> 2
        else                    -> 0
    }
    val contentStart = lineStart + contentOffset
    val content      = text.substring(contentStart, lineEnd)

    return if (content.isEmpty()) {
        tfv
    } else if (isWrappedWith(content, marker)) {
        val inner   = content.substring(marker.length, content.length - marker.length)
        val newText = text.substring(0, contentStart) + inner + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(contentStart + inner.length))
    } else {
        if (!hasWordContent(content)) return tfv
        val newText = text.substring(0, contentStart) + marker + content + marker + text.substring(lineEnd)
        TextFieldValue(newText, TextRange(contentStart + marker.length + content.length))
    }
}

internal fun autoContinueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val insertPos = new.selection.start - 1
    if (new.text.length != old.text.length + 1 || insertPos < 0 || new.text[insertPos] != '\n')
        return new
    val lineStart = old.text.lastIndexOf('\n', old.selection.start - 1) + 1
    val prevLine  = old.text.substring(lineStart, old.selection.start)
    val (prefix, content) = when {
        // A new item always starts open, whatever state the one above reached.
        checkStateOf(prevLine) != null ->
            CheckState.OPEN.marker to prevLine.substring(ChecklistPrefixLength)
        prevLine.startsWith("- ") -> "- " to prevLine.substring(2)
        prevLine.startsWith("* ") -> "* " to prevLine.substring(2)
        numberedPrefixLength(prevLine) > 0 -> {
            val n = numberedPrefixValue(prevLine)!!
            "${n + 1}. " to prevLine.substring(numberedPrefixLength(prevLine))
        }
        else -> return new
    }
    // Enter on an item that was never filled in ends the list: the marker the
    // previous Enter added disappears again and no new line is opened.
    if (content.isBlank()) {
        val cleared = old.text.removeRange(lineStart, old.selection.start)
        return TextFieldValue(cleared, TextRange(lineStart))
    }
    val before = new.text.substring(0, insertPos + 1)
    val after  = new.text.substring(insertPos + 1)
    val result = before + prefix + after
    val cursor = insertPos + 1 + prefix.length
    return TextFieldValue(result, TextRange(cursor))
}

/**
 * A backspace anywhere in a check marker takes the whole box away in one go.
 * Deleting a single character would leave "- [ ]", which is no longer a check
 * line — the raw markdown would suddenly show and want five more presses.
 *
 * Returns null when the edit was something else.
 */
internal fun deleteCheckMarker(old: TextFieldValue, new: TextFieldValue): TextFieldValue? {
    if (new.text.length != old.text.length - 1) return null
    val at = new.selection.start
    if (at < 0 || at > old.text.length) return null

    val lineStart = old.text.lastIndexOf('\n', (at - 1).coerceAtLeast(0))
        .let { if (it < 0) 0 else it + 1 }
    val lineEnd = old.text.indexOf('\n', lineStart).let { if (it < 0) old.text.length else it }
    if (checkStateOf(old.text.substring(lineStart, lineEnd)) == null) return null
    if (at < lineStart || at >= lineStart + ChecklistPrefixLength) return null

    val newText = old.text.removeRange(lineStart, lineStart + ChecklistPrefixLength)
    return TextFieldValue(newText, TextRange(lineStart))
}
