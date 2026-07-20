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

import androidx.compose.ui.text.AnnotatedString
import com.mushotoku.app.ui.theme.DarkAppColors
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMarkdownTransformationTest {

    /** Renders [raw] with the cursor sitting on line [cursorLine]. */
    private fun render(raw: String, cursorLine: Int): String =
        MarkdownVisualTransformation(DarkAppColors, cursorLine)
            .filter(AnnotatedString(raw))
            .text.text

    @Test fun `Spiegelstriche bleiben Spiegelstriche`() {
        val raw = "# Einkauf\n- Milch\n- Butter"
        assertEquals("Einkauf\n- Milch\n- Butter", render(raw, cursorLine = 1))
        assertEquals("Einkauf\n- Milch\n- Butter", render(raw, cursorLine = 2))
        // Line 0 is the active one, so the heading keeps showing its marker.
        assertEquals("# Einkauf\n- Milch\n- Butter", render(raw, cursorLine = 0))
    }

    @Test fun `nur das Sternchen wird zum Punkt`() {
        assertEquals("• Milch", render("* Milch", cursorLine = 5))
        assertEquals("• Milch", render("* Milch", cursorLine = 0))
        assertEquals("- Milch\n• Butter", render("- Milch\n* Butter", cursorLine = 9))
    }

    @Test fun `Checkboxen werden als Kaestchen gezeichnet`() {
        val raw = "- [ ] offen\n- [x] erledigt"
        assertEquals("☐ offen\n☑ erledigt", render(raw, cursorLine = 0))
        assertEquals("☐ offen\n☑ erledigt", render(raw, cursorLine = 1))
        assertEquals("☑ erledigt", render("- [X] erledigt", cursorLine = 0))
    }

    @Test fun `Nummerierte Listen behalten ihre Zahl`() {
        val raw = "1. eins\n2. zwei\n10. zehn"
        assertEquals(raw, render(raw, cursorLine = 0))
        assertEquals(raw, render(raw, cursorLine = 9))
    }

    @Test fun `Cursor landet hinter dem Kaestchen`() {
        val raw = "- [ ] A"
        val t = MarkdownVisualTransformation(DarkAppColors, 0).filter(AnnotatedString(raw))
        assertEquals("☐ A", t.text.text)
        // The four hidden characters collapse onto the box, "A" keeps its place.
        assertEquals(2, t.offsetMapping.originalToTransformed(6))
        assertEquals(6, t.offsetMapping.transformedToOriginal(2))
    }

    @Test fun `Ueberschriften verstecken ihr Zeichen weiterhin`() {
        assertEquals("Titel", render("# Titel", cursorLine = 99))
        assertEquals("# Titel", render("# Titel", cursorLine = 0))
    }

    @Test fun `Zitate verstecken ihr Zeichen weiterhin`() {
        assertEquals("Zitat", render("> Zitat", cursorLine = 99))
    }

    @Test fun `Inline-Auszeichnung in einer Liste wird weiterhin versteckt`() {
        assertEquals("- fett", render("- **fett**", cursorLine = 99))
        assertEquals("- **fett**", render("- **fett**", cursorLine = 0))
    }

    @Test fun `Cursor-Positionen bleiben auf die Quelle abbildbar`() {
        val raw = "- Milch\n* Butter"
        val transformed = MarkdownVisualTransformation(DarkAppColors, 0)
            .filter(AnnotatedString(raw))
        // The bullet replaces the asterisk one for one, so both directions stay the identity.
        for (i in 0..raw.length) {
            assertEquals(i, transformed.offsetMapping.originalToTransformed(i))
            assertEquals(i, transformed.offsetMapping.transformedToOriginal(i))
        }
    }
}
