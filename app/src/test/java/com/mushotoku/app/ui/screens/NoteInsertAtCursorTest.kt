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
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteInsertAtCursorTest {

    private fun tfv(text: String, start: Int, end: Int = start) =
        TextFieldValue(text, TextRange(start, end))

    @Test fun `am Textende folgt ein Leerzeichen zum Weiterschreiben`() {
        val result = insertAtCursor(tfv("", 0), "09:25")
        assertEquals("09:25 ", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test fun `mitten im Wort wird beidseitig getrennt`() {
        val result = insertAtCursor(tfv("abcdef", 3), "09:25")
        assertEquals("abc 09:25 def", result.text)
        assertEquals(TextRange(10), result.selection)
    }

    @Test fun `vorhandene Leerzeichen werden nicht verdoppelt`() {
        val result = insertAtCursor(tfv("- x", 2, 3), "09:25")
        assertEquals("- 09:25 ", result.text)
    }

    @Test fun `am Zeilenanfang bleibt der Umbruch erhalten`() {
        val result = insertAtCursor(tfv("Titel\nZeile", 6), "09:25")
        assertEquals("Titel\n09:25 Zeile", result.text)
    }

    @Test fun `Auswahl wird ersetzt`() {
        val result = insertAtCursor(tfv("alt neu", 0, 3), "09:25")
        assertEquals("09:25 neu", result.text)
        assertEquals(TextRange(5), result.selection)
    }
}
