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

import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType

private fun isHeading(line: String) =
    line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")

private fun isListLine(line: String) =
    checkStateOf(line) != null ||
        line.startsWith("- ") || line.startsWith("* ") ||
        numberedPrefixLength(line) > 0

/** Title and content as one text, the way both are written in the editor. */
private fun Note.lines(): List<String> =
    if (content.isEmpty()) listOf(title) else "$title\n$content".lines()

/** Whether the note writes out a list of any kind. */
internal fun containsList(note: Note): Boolean = note.lines().any { isListLine(it) }

/**
 * Whether anything is written beyond headings and list items. The first line is
 * skipped because it is the title, and a title plus a subheading is not yet
 * what makes a note a note.
 */
internal fun hasProse(note: Note): Boolean =
    note.lines().drop(1).any { it.isNotBlank() && !isHeading(it) && !isListLine(it) }

/**
 * Which tab a note shows up under. Its stored type always counts, and its
 * content can add to it: a note holding a list also belongs with the lists, and
 * a list that carries real text belongs with the notes as well.
 */
internal fun matchesTypeFilter(note: Note, filter: NoteType?): Boolean = when (filter) {
    null              -> true
    NoteType.LIST     -> note.type == NoteType.LIST || containsList(note)
    NoteType.NOTE     -> note.type == NoteType.NOTE || (containsList(note) && hasProse(note))
    NoteType.ROUTINE  -> note.type == NoteType.ROUTINE
}
