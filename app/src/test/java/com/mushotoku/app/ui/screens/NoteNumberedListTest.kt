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

class NoteNumberedListTest {

    /** Simulates pressing Enter at the end of [text]. */
    private fun pressEnter(text: String): String {
        val old = TextFieldValue(text, TextRange(text.length))
        val new = TextFieldValue(text + "\n", TextRange(text.length + 1))
        return autoContinueList(old, new).text
    }

    @Test fun `Enter setzt die Nummerierung fort`() {
        assertEquals("1. eins\n2. ", pressEnter("1. eins"))
    }

    @Test fun `die Zahl zaehlt weiter statt bei eins zu beginnen`() {
        assertEquals("1. a\n2. b\n3. ", pressEnter("1. a\n2. b"))
        assertEquals("9. neun\n10. ", pressEnter("9. neun"))
        assertEquals("99. x\n100. ", pressEnter("99. x"))
    }

    @Test fun `Enter auf einem leeren Punkt beendet die Liste`() {
        // The marker the previous Enter inserted is taken back, no line is added.
        assertEquals("1. eins\n", pressEnter("1. eins\n2. "))
        assertEquals("- a\n", pressEnter("- a\n- "))
        assertEquals("- [ ] a\n", pressEnter("- [ ] a\n- [ ] "))
        assertEquals("", pressEnter("- "))
    }

    @Test fun `nach dem Beenden steht der Cursor auf der leeren Zeile`() {
        val text = "- a\n- "
        val old = TextFieldValue(text, TextRange(text.length))
        val new = TextFieldValue("$text\n", TextRange(text.length + 1))
        val result = autoContinueList(old, new)
        assertEquals("- a\n", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test fun `Aufzaehlungen und Checklisten laufen weiterhin weiter`() {
        assertEquals("- a\n- ", pressEnter("- a"))
        assertEquals("- [ ] a\n- [ ] ", pressEnter("- [ ] a"))
        assertEquals("- [x] a\n- [ ] ", pressEnter("- [x] a"))
    }

    @Test fun `Fliesstext bleibt unangetastet`() {
        assertEquals("nur Text\n", pressEnter("nur Text"))
        assertEquals("1.kein Punkt\n", pressEnter("1.kein Punkt"))
        assertEquals("1 . kein Punkt\n", pressEnter("1 . kein Punkt"))
    }

    @Test fun `Formatwechsel entfernt die Nummer`() {
        val tfv = TextFieldValue("12. Punkt", TextRange(9))
        assertEquals("Punkt", applyLinePrefix(tfv, "").text)
        assertEquals("- Punkt", applyLinePrefix(tfv, "- ").text)
        assertEquals("# Punkt", applyLinePrefix(tfv, "# ").text)
    }

    @Test fun `der Nummern-Knopf zaehlt von der Zeile darueber weiter`() {
        val text = "1. eins\n2. zwei\ndrei"
        val tfv  = TextFieldValue(text, TextRange(text.length))
        assertEquals("1. eins\n2. zwei\n3. drei", applyNumberedPrefix(tfv).text)
    }

    @Test fun `ohne Nummer darueber beginnt der Knopf bei eins`() {
        assertEquals("1. Punkt", applyNumberedPrefix(TextFieldValue("Punkt", TextRange(5))).text)
        assertEquals("- a\n1. b", applyNumberedPrefix(TextFieldValue("- a\nb", TextRange(5))).text)
    }

    @Test fun `der Nummern-Knopf ersetzt ein vorhandenes Praefix`() {
        val text = "1. eins\n- zwei"
        val tfv  = TextFieldValue(text, TextRange(text.length))
        assertEquals("1. eins\n2. zwei", applyNumberedPrefix(tfv).text)
    }

    @Test fun `die Leiste erkennt jede Listenart`() {
        assertEquals("dash",   activeToolbarFormat("- Milch"))
        assertEquals("bullet", activeToolbarFormat("* Milch"))
        assertEquals("number", activeToolbarFormat("3. Milch"))
        assertEquals("check",  activeToolbarFormat("- [ ] Milch"))
        assertEquals("check",  activeToolbarFormat("- [x] Milch"))
        assertEquals("h1",     activeToolbarFormat("# Titel"))
        assertEquals("h2",     activeToolbarFormat("## Titel"))
        assertEquals("h3",     activeToolbarFormat("### Titel"))
        assertEquals("text",   activeToolbarFormat("Milch"))
        assertEquals("text",   activeToolbarFormat(""))
    }

    @Test fun `Praefix-Erkennung greift nur bei echten Nummern`() {
        assertEquals(3, numberedPrefixLength("1. x"))
        assertEquals(4, numberedPrefixLength("42. x"))
        assertEquals(0, numberedPrefixLength("1.x"))
        assertEquals(0, numberedPrefixLength("- 1. x"))
        assertEquals(42, numberedPrefixValue("42. x"))
        assertEquals(null, numberedPrefixValue("x"))
    }
}
