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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteTagsTest {

    private fun note(title: String, content: String = "") =
        Note(id = 1, title = title, content = content)

    @Test fun `Tags werden aus Titel und Inhalt gelesen`() {
        assertEquals(listOf("job", "idee"), extractTags(note("# Notiz #job", "Text #idee")))
    }

    @Test fun `Ueberschriften sind keine Tags`() {
        assertEquals(emptyList<String>(), extractTags(note("# Titel", "## Zwei\n### Drei")))
    }

    @Test fun `Gross- und Kleinschreibung wird zusammengefasst`() {
        assertEquals(listOf("job"), extractTags(note("#Job und #JOB und #job")))
        assertTrue(noteHasTag(note("#Job"), "job"))
        assertTrue(noteHasTag(note("#job"), "JOB"))
    }

    @Test fun `Umlaute und Ziffern gehoeren zum Tag`() {
        assertEquals(listOf("büro", "w2", "to-do", "über_uns"),
            extractTags(note("#büro #w2 #to-do #über_uns")))
    }

    @Test fun `ein Doppelkreuz mitten im Wort zaehlt nicht`() {
        assertEquals(emptyList<String>(), extractTags(note("C#code", "Farbe #ff0000ff ist hex")).minus("ff0000ff"))
        assertEquals(emptyList<String>(), extractTags(note("Preis 5#", "nr#4")))
    }

    @Test fun `Satzzeichen beenden den Tag`() {
        assertEquals(listOf("job"), extractTags(note("Das ist #job.")))
        assertEquals(listOf("a", "b"), extractTags(note("#a, #b!")))
    }

    @Test fun `jeder Tag erscheint nur einmal und in Reihenfolge`() {
        assertEquals(listOf("b", "a"), extractTags(note("#b #a #b")))
    }

    @Test fun `die Leiste sammelt alle Tags alphabetisch`() {
        val notes = listOf(note("#zeta"), note("#alpha #zeta"), note("ohne Tag"))
        assertEquals(listOf("alpha", "zeta"), collectTags(notes))
    }

    @Test fun `Tag-Erkennung an einer Position`() {
        assertEquals(4, tagLengthAt("#job", 0))
        assertEquals(4, tagLengthAt("ein #job hier", 4))
        assertEquals(0, tagLengthAt("C#code", 1))
        assertEquals(0, tagLengthAt("# Titel", 0))
        assertEquals(0, tagLengthAt("#", 0))
        assertEquals(0, tagLengthAt("kein Doppelkreuz", 0))
        assertEquals(6, tagLengthAt("#to-do.", 0))
    }

    @Test fun `Tags werden beim Tippen klein geschrieben`() {
        val tfv = TextFieldValue("Text #Job und #ÜBER hier", TextRange(9))
        val out = lowercaseTags(tfv)
        assertEquals("Text #job und #über hier", out.text)
        assertEquals(TextRange(9), out.selection)
    }

    @Test fun `ausserhalb von Tags bleibt die Schreibweise`() {
        assertEquals("Grosses WORT", lowercaseTags(TextFieldValue("Grosses WORT")).text)
        assertEquals("C#Code", lowercaseTags(TextFieldValue("C#Code")).text)
    }

    @Test fun `der gerade getippte Tag wird erkannt`() {
        assertEquals("jo", tagPrefixAt("Text #jo", 8))
        assertEquals("jo", tagPrefixAt("#Jo", 3))
        assertEquals(null, tagPrefixAt("Text #jo", 6))
        assertEquals(null, tagPrefixAt("Text ohne", 9))
        assertEquals(null, tagPrefixAt("Text #job hier", 7))
    }

    @Test fun `Vorschlaege sind die Tags mit gleichem Anfang`() {
        val alle = listOf("job", "jobsuche", "journal", "idee")
        assertEquals(listOf("job", "jobsuche", "journal"), tagSuggestions(alle, "jo"))
        assertEquals(listOf("jobsuche"), tagSuggestions(alle, "job"))
        assertEquals(emptyList<String>(), tagSuggestions(alle, "xyz"))
        assertEquals(2, tagSuggestions(alle, "jo", limit = 2).size)
    }

    @Test fun `ein Vorschlag ersetzt den angefangenen Tag`() {
        val tfv = TextFieldValue("Text #jo", TextRange(8))
        val out = completeTag(tfv, "journal")
        assertEquals("Text #journal ", out.text)
        assertEquals(TextRange(14), out.selection)
    }

    @Test fun `ein Vorschlag mitten im Text laesst den Rest stehen`() {
        val out = completeTag(TextFieldValue("a #jo b", TextRange(5)), "job")
        assertEquals("a #job  b", out.text)
    }

    @Test fun `der Tag am Cursor wird gefunden`() {
        assertEquals(5..8, tagRangeAt("Text #job hier", 7))
        assertEquals(5..8, tagRangeAt("Text #job hier", 9))
        assertEquals(5..8, tagRangeAt("Text #job hier", 6))
        assertEquals(null, tagRangeAt("Text #job hier", 12))
        assertEquals(null, tagRangeAt("kein Tag", 4))
    }

    @Test fun `das Doppelkreuz bleibt stehen, der Tag verschwindet`() {
        val out = unmarkTag(TextFieldValue("Text #job hier", TextRange(9)))
        assertEquals("Text \\#job hier", out.text)
        assertEquals(TextRange(10), out.selection)
        // Nowhere in the app is it a tag any more.
        assertEquals(emptyList<String>(), extractTags(out.text))
        assertEquals(0, tagLengthAt(out.text, 6))
    }

    @Test fun `ein Backspace macht daraus wieder einen Tag`() {
        val unmarked = unmarkTag(TextFieldValue("#job", TextRange(4))).text
        assertEquals(listOf("job"), extractTags(unmarked.replace(TagEscape.toString(), "")))
    }

    @Test fun `ohne Tag am Cursor bleibt alles wie es ist`() {
        val tfv = TextFieldValue("kein Tag hier", TextRange(4))
        assertEquals(tfv.text, unmarkTag(tfv).text)
    }

    @Test fun `eine Zeile aus lauter Tags sagt nichts aus`() {
        assertTrue(isTagOnly("#job"))
        assertTrue(isTagOnly("#job #idee"))
        assertTrue(isTagOnly("  #job  "))
        assertFalse(isTagOnly("#job und Text"))
        assertFalse(isTagOnly("Text"))
        assertFalse(isTagOnly(""))
    }

    @Test fun `ein entwerteter Tag zaehlt nicht als Tag-Zeile`() {
        assertFalse(isTagOnly("\\#job"))
    }

    @Test fun `ein Tag laesst sich aus dem Text entfernen`() {
        val n = Note(id = 1, title = "# Notiz #job", content = "Text #job hier\nund #idee")
        val out = n.withoutTag("job")
        assertEquals("# Notiz", out.title)
        assertEquals("Text hier\nund #idee", out.content)
        assertEquals(listOf("idee"), extractTags(out))
    }

    @Test fun `beim Entfernen bleibt ein aehnlicher Tag stehen`() {
        val n = Note(id = 1, title = "T", content = "#job #jobsuche")
        assertEquals(listOf("jobsuche"), extractTags(n.withoutTag("job")))
    }

    @Test fun `ohne Tags bleibt die Leiste leer`() {
        assertEquals(emptyList<String>(), collectTags(listOf(note("nichts"), note("auch nichts"))))
        assertFalse(noteHasTag(note("nichts"), "job"))
    }
}
