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

/**
 * A tag is a "#" that opens a word, so it must not follow a letter (no
 * "C#code") and must be followed by one right away — which is what keeps the
 * markdown headings "# ", "## " and "### " out of the tag list.
 */
private val TagPattern = Regex("""(?<![\p{L}\p{N}_\\])#([\p{L}\p{N}_][\p{L}\p{N}_-]*)""")

private fun isTagWordChar(c: Char) = c.isLetterOrDigit() || c == '_'
private fun isTagBodyChar(c: Char) = isTagWordChar(c) || c == '-'

/**
 * Length of the tag starting at [at], or 0 if none starts there. Mirrors the
 * pattern above so the highlighting in the text and the bar always agree.
 */
internal fun tagLengthAt(text: String, at: Int): Int {
    if (text.getOrNull(at) != '#') return 0
    // A backslash in front is the markdown way of saying "just a hash".
    if (at > 0 && (isTagWordChar(text[at - 1]) || text[at - 1] == TagEscape)) return 0
    var end = at + 1
    if (end >= text.length || !isTagWordChar(text[end])) return 0
    while (end < text.length && isTagBodyChar(text[end])) end++
    return end - at
}

/**
 * Ranges of the tags in [text] that are finished and should be drawn as a
 * pill. A tag is still being typed while it ends the line the cursor is on;
 * pass a [cursorLine] of null when there is no cursor, as in the read view.
 */
internal fun boxedTagRanges(text: String, cursorLine: Int?): List<IntRange> {
    val result = mutableListOf<IntRange>()
    var i = 0
    var line = 0
    while (i < text.length) {
        if (text[i] == '\n') { line++; i++; continue }
        val len = tagLengthAt(text, i)
        if (len == 0) { i++; continue }
        val endsLine = i + len >= text.length || text[i + len] == '\n'
        if (!(endsLine && line == cursorLine)) result.add(i until i + len)
        i += len
    }
    return result
}

/** Titles used to be stored with their heading marker; old notes still carry it. */
internal fun String.stripHeadingMarker(): String =
    removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()

/** Tags are matched case-insensitively, so "#Job" and "#job" are one tag. */
internal fun normalizeTag(tag: String): String = tag.lowercase()

/**
 * Writes every tag in lower case as it is typed. Characters are lowered one by
 * one rather than through [String.lowercase], because a whole-string conversion
 * can change the length and would then shift the cursor.
 */
internal fun lowercaseTags(tfv: TextFieldValue): TextFieldValue {
    val text = tfv.text
    val out  = StringBuilder(text)
    var changed = false
    var i = 0
    while (i < text.length) {
        val len = tagLengthAt(text, i)
        if (len == 0) { i++; continue }
        for (j in i until i + len) {
            val lower = text[j].lowercaseChar()
            if (lower != text[j]) { out[j] = lower; changed = true }
        }
        i += len
    }
    return if (changed) tfv.copy(text = out.toString()) else tfv
}

/**
 * The tag being typed right at the cursor, without its "#", or null when the
 * cursor is not at the end of one.
 */
internal fun tagPrefixAt(text: String, cursor: Int): String? {
    if (cursor !in 0..text.length) return null
    // Only while the tag is still open at the cursor, not in the middle of one.
    if (cursor < text.length && isTagBodyChar(text[cursor])) return null
    var start = cursor
    while (start > 0 && isTagBodyChar(text[start - 1])) start--
    if (start == 0 || text[start - 1] != '#') return null
    val hash = start - 1
    if (hash > 0 && isTagWordChar(text[hash - 1])) return null
    if (start == cursor) return null
    return normalizeTag(text.substring(start, cursor))
}

/**
 * The finished tag the cursor sits in or right behind, or null when there is
 * none. Used to offer turning it back into ordinary text.
 */
internal fun tagRangeAt(text: String, cursor: Int): IntRange? {
    if (cursor !in 0..text.length) return null
    var start = cursor
    while (start > 0 && isTagBodyChar(text[start - 1])) start--
    if (start == 0 || text[start - 1] != '#') return null
    val hash = start - 1
    val len = tagLengthAt(text, hash)
    if (len == 0) return null
    return hash until hash + len
}

/**
 * Takes the tag status away while leaving the "#" in the text, by escaping it
 * the way markdown does. The backslash is hidden while rendering, so the note
 * still reads "#job", and exported files stay valid markdown.
 *
 * An invisible marker would have been simpler, but characters outside the font
 * pull in a fallback font and shift the line's metrics.
 */
internal const val TagEscape = '\\'

internal fun unmarkTag(tfv: TextFieldValue): TextFieldValue {
    val range = tagRangeAt(tfv.text, tfv.selection.start) ?: return tfv
    val hash = range.first
    val newText = tfv.text.substring(0, hash) + TagEscape + tfv.text.substring(hash)
    val cursor  = if (tfv.selection.start > hash) tfv.selection.start + 1 else tfv.selection.start
    return TextFieldValue(newText, TextRange(cursor))
}

/** Existing tags that continue [prefix], excluding the one already written out. */
internal fun tagSuggestions(allTags: List<String>, prefix: String, limit: Int = 4): List<String> =
    allTags.filter { it.startsWith(prefix) && it != prefix }.sorted().take(limit)

/** Replaces the tag at the cursor with [tag] and leaves a space behind it. */
internal fun completeTag(tfv: TextFieldValue, tag: String): TextFieldValue {
    val text   = tfv.text
    val cursor = tfv.selection.start.coerceIn(0, text.length)
    var start  = cursor
    while (start > 0 && isTagBodyChar(text[start - 1])) start--
    if (start == 0 || text[start - 1] != '#') return tfv
    val replacement = "$tag "
    val newText = text.substring(0, start) + replacement + text.substring(cursor)
    return TextFieldValue(newText, TextRange(start + replacement.length))
}

/** Every tag written anywhere in the note, in the order they first appear. */
internal fun extractTags(note: Note): List<String> =
    extractTags("${note.title}\n${note.content}")

internal fun extractTags(text: String): List<String> =
    TagPattern.findAll(text)
        .map { normalizeTag(it.groupValues[1]) }
        .distinct()
        .toList()

/** Whether nothing but tags is written here, so a preview would say nothing. */
internal fun isTagOnly(text: String): Boolean =
    text.isNotBlank() && TagPattern.replace(text, "").isBlank()

internal fun noteHasTag(note: Note, tag: String): Boolean =
    extractTags(note).contains(normalizeTag(tag))

/**
 * All tags across [notes], alphabetically so a tag keeps its place in the bar
 * instead of jumping around whenever a note is edited.
 */
internal fun collectTags(notes: List<Note>): List<String> =
    notes.flatMap { extractTags(it) }.distinct().sorted()
