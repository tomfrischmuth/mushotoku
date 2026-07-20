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
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class NoteTimestampTest {

    private val at = LocalDateTime.of(2026, 7, 20, 9, 25)
    private fun anchor(start: Int, text: String) = StampAnchor(start, text, at, withDate = false)

    @Test fun `der Cursor darf auf dem angehaengten Leerzeichen stehen`() {
        // This is where inserting leaves it, and it is what broke the toggle.
        val tfv = TextFieldValue("Notiz 09:25 ", TextRange(12))
        val out = toggleStamp(tfv, anchor(6, "09:25"), "20.07.2026 09:25")
        assertEquals("Notiz 20.07.2026 09:25 ", out?.text)
        assertEquals(TextRange(23), out?.selection)
    }

    @Test fun `der Cursor direkt am Stempelende zaehlt auch`() {
        val tfv = TextFieldValue("Notiz 09:25", TextRange(11))
        val out = toggleStamp(tfv, anchor(6, "09:25"), "20.07.2026 09:25")
        assertEquals("Notiz 20.07.2026 09:25", out?.text)
        assertEquals(TextRange(22), out?.selection)
    }

    @Test fun `zurueck auf die Uhrzeit`() {
        val tfv = TextFieldValue("20.07.2026 09:25 ", TextRange(17))
        val out = toggleStamp(tfv, StampAnchor(0, "20.07.2026 09:25", at, true), "09:25")
        assertEquals("09:25 ", out?.text)
        assertEquals(TextRange(6), out?.selection)
    }

    @Test fun `weitergetippt gilt der Stempel nicht mehr`() {
        val tfv = TextFieldValue("Notiz 09:25 Uhr", TextRange(15))
        assertNull(toggleStamp(tfv, anchor(6, "09:25"), "20.07.2026 09:25"))
    }

    @Test fun `ein verschobener Cursor gilt nicht mehr`() {
        val tfv = TextFieldValue("Notiz 09:25 ", TextRange(3))
        assertNull(toggleStamp(tfv, anchor(6, "09:25"), "20.07.2026 09:25"))
    }

    @Test fun `ein veraenderter Stempel gilt nicht mehr`() {
        val tfv = TextFieldValue("Notiz 09:26 ", TextRange(12))
        assertNull(toggleStamp(tfv, anchor(6, "09:25"), "20.07.2026 09:25"))
    }

    @Test fun `ein Anker hinter dem Textende kippt nicht um`() {
        val tfv = TextFieldValue("kurz", TextRange(4))
        assertNull(toggleStamp(tfv, anchor(90, "09:25"), "20.07.2026 09:25"))
    }

    @Test fun `Text hinter dem Stempel bleibt stehen`() {
        val tfv = TextFieldValue("a 09:25 b", TextRange(7))
        val out = toggleStamp(tfv, anchor(2, "09:25"), "20.07.2026 09:25")
        assertEquals("a 20.07.2026 09:25 b", out?.text)
    }
}
