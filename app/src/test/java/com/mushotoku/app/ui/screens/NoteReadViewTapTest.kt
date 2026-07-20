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

import com.mushotoku.app.ui.theme.DarkAppColors
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A tap in the read view has to land on the matching character of the source,
 * even though markers are hidden or drawn differently.
 */
class NoteReadViewTapTest {

    private fun view(raw: String) = buildReadView(raw, DarkAppColors)

    /** Source offset for the tap that landed on the first occurrence of [visible]. */
    private fun tapOn(raw: String, visible: String): Int {
        val v = view(raw)
        val at = v.text.text.indexOf(visible)
        check(at >= 0) { "\"$visible\" is not rendered in \"${v.text.text}\"" }
        return v.rawOffset(at)
    }

    @Test fun `hinter einer versteckten Ueberschrift stimmt die Stelle`() {
        val raw = "# Titel"
        assertEquals("Titel", view(raw).text.text)
        assertEquals(2, tapOn(raw, "T"))
        assertEquals(6, tapOn(raw, "l"))
    }

    @Test fun `hinter einem Kaestchen stimmt die Stelle`() {
        val raw = "# Titel\n- [ ] Aufgabe"
        assertEquals("Titel\n☐ Aufgabe", view(raw).text.text)
        assertEquals(14, tapOn(raw, "Aufgabe"))
        assertEquals(raw.indexOf("gabe"), tapOn(raw, "gabe"))
    }

    @Test fun `hinter ausgeblendeter Fett-Auszeichnung stimmt die Stelle`() {
        val raw = "- **fett** danach"
        assertEquals("- fett danach", view(raw).text.text)
        assertEquals(4, tapOn(raw, "fett"))
        assertEquals(raw.indexOf("danach"), tapOn(raw, "danach"))
    }

    @Test fun `ueber mehrere Zeilen hinweg bleibt die Zuordnung stabil`() {
        val raw = "# T\n- a\n* b\n2. c\n> d"
        val v = view(raw)
        assertEquals("T\n- a\n• b\n2. c\nd", v.text.text)
        listOf("a", "b", "c", "d").forEach { ch ->
            assertEquals(raw.indexOf(ch), v.rawOffset(v.text.text.indexOf(ch)))
        }
    }

    @Test fun `ein Tipp ans Ende landet am Textende`() {
        val raw = "# Titel"
        val v = view(raw)
        assertEquals(raw.length, v.rawOffset(v.text.text.length))
        assertEquals(raw.length, v.rawOffset(9999))
        assertEquals(2, v.rawOffset(-5))
    }

    @Test fun `leerer Text kippt nicht um`() {
        val v = view("")
        assertEquals("", v.text.text)
        assertEquals(0, v.rawOffset(0))
    }
}
