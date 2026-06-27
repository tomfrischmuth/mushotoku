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
        lineContent.startsWith("- [x] ") || lineContent.startsWith("- [X] ") -> lineContent.substring(6)
        lineContent.startsWith("- [ ] ") -> lineContent.substring(6)
        lineContent.startsWith("- ")   -> lineContent.substring(2)
        lineContent.startsWith("* ")   -> lineContent.substring(2)
        else                           -> lineContent
    }
    val newLine = if (prefix.isEmpty()) stripped else "$prefix$stripped"
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val newCursor = (cursor + newLine.length - lineContent.length).coerceAtLeast(lineStart)
    return TextFieldValue(newText, TextRange(newCursor))
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
        line.startsWith("- [x] ") || line.startsWith("- [X] ") -> 6
        line.startsWith("- [ ] ") -> 6
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
        prevLine.startsWith("- [x] ") || prevLine.startsWith("- [X] ") ->
            "- [ ] " to prevLine.substring(6)
        prevLine.startsWith("- [ ] ") ->
            "- [ ] " to prevLine.substring(6)
        prevLine.startsWith("- ") -> "- " to prevLine.substring(2)
        prevLine.startsWith("* ") -> "* " to prevLine.substring(2)
        else -> return new
    }
    if (content.isBlank()) return new
    val before = new.text.substring(0, insertPos + 1)
    val after  = new.text.substring(insertPos + 1)
    val result = before + prefix + after
    val cursor = insertPos + 1 + prefix.length
    return TextFieldValue(result, TextRange(cursor))
}
