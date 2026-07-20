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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteClassificationTest {

    private fun note(content: String, type: NoteType = NoteType.NOTE) =
        Note(id = 1, title = "# Titel", content = content, type = type)

    @Test fun `eine Notiz mit Liste erscheint auch bei den Listen`() {
        listOf("- Milch", "* Milch", "1. Milch", "- [ ] Milch").forEach { line ->
            assertTrue(line, matchesTypeFilter(note(line), NoteType.LIST))
        }
    }

    @Test fun `eine Notiz ohne Liste erscheint dort nicht`() {
        assertFalse(matchesTypeFilter(note("nur Text"), NoteType.LIST))
        assertFalse(matchesTypeFilter(note("## Zwischentitel"), NoteType.LIST))
    }

    @Test fun `eine reine Liste bleibt aus den Notizen heraus`() {
        val pure = Note(id = 1, title = "# Einkauf", content = "## Montag\n- Milch\n- Brot",
                        type = NoteType.LIST)
        assertTrue(matchesTypeFilter(pure, NoteType.LIST))
        assertFalse(matchesTypeFilter(pure, NoteType.NOTE))
    }

    @Test fun `eine Liste mit echtem Text erscheint auch bei den Notizen`() {
        val mixed = Note(id = 1, title = "# Einkauf",
                         content = "## Montag\n- Milch\nDenk an den Bon.", type = NoteType.LIST)
        assertTrue(matchesTypeFilter(mixed, NoteType.LIST))
        assertTrue(matchesTypeFilter(mixed, NoteType.NOTE))
    }

    @Test fun `der eigene Typ zaehlt immer`() {
        val plain = Note(id = 1, title = "# Titel", content = "", type = NoteType.NOTE)
        assertTrue(matchesTypeFilter(plain, NoteType.NOTE))
        val routine = Note(id = 1, title = "# Ablauf", content = "- Aufstehen", type = NoteType.ROUTINE)
        assertTrue(matchesTypeFilter(routine, NoteType.ROUTINE))
        assertTrue("eine Routine mit Liste steht auch bei den Listen",
                   matchesTypeFilter(routine, NoteType.LIST))
    }

    @Test fun `ohne Filter erscheint alles`() {
        assertTrue(matchesTypeFilter(note("egal"), null))
    }

    @Test fun `der Titel allein ist noch kein Text`() {
        assertFalse(hasProse(Note(id = 1, title = "Titel ohne Raute", content = "- Milch")))
        assertFalse(hasProse(note("## Zwischentitel\n- Milch")))
        assertTrue(hasProse(note("## Zwischentitel\n- Milch\nund noch ein Satz")))
    }
}
