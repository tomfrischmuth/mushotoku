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

import com.mushotoku.app.ui.StatusGreen
import com.mushotoku.app.ui.StatusRed
import com.mushotoku.app.ui.StatusYellow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteChecklistTest {

    @Test fun `jede Schreibweise wird erkannt`() {
        assertEquals(CheckState.OPEN,  checkStateOf("- [ ] offen"))
        assertEquals(CheckState.DOING, checkStateOf("- [/] laeuft"))
        assertEquals(CheckState.DONE,  checkStateOf("- [x] fertig"))
        assertEquals(CheckState.DONE,  checkStateOf("- [X] fertig"))
    }

    @Test fun `andere Zeilen sind keine Checkliste`() {
        assertNull(checkStateOf("- Strich"))
        assertNull(checkStateOf("* Punkt"))
        assertNull(checkStateOf("1. Nummer"))
        assertNull(checkStateOf("# Titel"))
        assertNull(checkStateOf(""))
        assertNull(checkStateOf("- [] ohne Leerzeichen"))
    }

    @Test fun `ein Klick geht einen Zustand weiter und dann rundherum`() {
        assertEquals("- [/] A", cycleCheckLine("- [ ] A"))
        assertEquals("- [x] A", cycleCheckLine("- [/] A"))
        assertEquals("- [ ] A", cycleCheckLine("- [x] A"))
        assertEquals("- [ ] A", cycleCheckLine("- [X] A"))
    }

    @Test fun `der Text der Zeile bleibt beim Wechsel erhalten`() {
        assertEquals("- [/] Milch **kaufen**", cycleCheckLine("- [ ] Milch **kaufen**"))
        assertEquals("- [/] ", cycleCheckLine("- [ ] "))
    }

    @Test fun `eine Zeile ohne Kaestchen bleibt unveraendert`() {
        assertEquals("nur Text", cycleCheckLine("nur Text"))
        assertEquals("- Strich", cycleCheckLine("- Strich"))
    }

    @Test fun `die Zustaende einer Notiz kommen der Reihe nach`() {
        val raw = "# Titel\n- [x] a\nText\n- [ ] b\n- [/] c"
        assertEquals(
            listOf(CheckState.DONE, CheckState.OPEN, CheckState.DOING),
            checkStatesIn(raw)
        )
        assertEquals(emptyList<CheckState>(), checkStatesIn("nur Text\n- Strich"))
    }

    @Test fun `leere Kaestchen werden beim Speichern entfernt`() {
        assertEquals("# Titel\n- [ ] Milch", dropEmptyCheckItems("# Titel\n- [ ] Milch\n- [ ] "))
        assertEquals("- [x] a\n- [/] b", dropEmptyCheckItems("- [x] a\n- [ ]   \n- [/] b"))
        assertEquals("", dropEmptyCheckItems("- [ ] "))
    }

    @Test fun `andere leere Zeilen bleiben erhalten`() {
        assertEquals("Text\n\nmehr", dropEmptyCheckItems("Text\n\nmehr"))
        assertEquals("- \n* \n1. ", dropEmptyCheckItems("- \n* \n1. "))
    }

    @Test fun `der Punkt zeigt den am wenigsten erledigten Zustand`() {
        assertEquals(CheckState.OPEN,  openCheckState("- [ ] a\n- [/] b\n- [x] c"))
        assertEquals(CheckState.OPEN,  openCheckState("- [x] a\n- [ ] b"))
        assertEquals(CheckState.DOING, openCheckState("- [/] a\n- [x] b"))
    }

    @Test fun `ohne offene Punkte gibt es keinen Punkt`() {
        assertNull(openCheckState("- [x] a\n- [x] b"))
        assertNull(openCheckState("nur Text\n- Strich"))
        assertNull(openCheckState(""))
    }

    @Test fun `die Farben sind die der Aufgaben-Ampel`() {
        assertEquals(StatusRed,    CheckState.OPEN.color)
        assertEquals(StatusYellow, CheckState.DOING.color)
        assertEquals(StatusGreen,  CheckState.DONE.color)
    }
}
