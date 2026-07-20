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

    // --- Das Kästchen um den Zeitstempel ---

    private fun stampAt(text: String, at: Int) =
        if (stampLengthAt(text, at) == 0) null else text.substring(at, at + stampLengthAt(text, at))

    @Test fun `eine blosse Uhrzeit bekommt ein Kaestchen`() {
        assertEquals("09:25", stampAt("Notiz 09:25 weiter", 6))
    }

    @Test fun `Datum Punkt Uhrzeit gehoeren in ein Kaestchen`() {
        assertEquals("20.07.2026 \u00b7 09:25", stampAt("a 20.07.2026 \u00b7 09:25 b", 2))
    }

    @Test fun `auch ein Datum aus Worten zaehlt dazu`() {
        assertEquals("Jul 20, 2026 \u00b7 9:25 AM", stampAt("Jul 20, 2026 \u00b7 9:25 AM", 0))
    }

    @Test fun `mitten im Wort faengt kein Stempel an`() {
        assertEquals(0, stampLengthAt("Raum1 09:25", 4))
    }

    @Test fun `ein Doppelpunkt im Text ist kein Stempel`() {
        assertEquals(0, stampLengthAt("Notiz: weiter", 5))
    }

    @Test fun `eine angehaengte Zahl gehoert nicht zum Stempel`() {
        assertEquals(0, stampLengthAt("09:250", 0))
    }

    @Test fun `ohne Punkt bleibt das Datum aussen vor`() {
        assertEquals("09:25", stampAt("20.07.2026 09:25", 11))
        assertEquals(0, stampLengthAt("20.07.2026 09:25", 0))
    }

    @Test fun `das Wort vor dem Datum bleibt aussen vor`() {
        assertEquals(0, stampLengthAt("test Jul 20, 2026 \u00b7 5:31 PM", 0))
        assertEquals("Jul 20, 2026 \u00b7 5:31 PM", stampAt("test Jul 20, 2026 \u00b7 5:31 PM", 5))
    }
}
