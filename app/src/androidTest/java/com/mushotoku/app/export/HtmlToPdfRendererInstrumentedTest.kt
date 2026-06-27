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

package com.mushotoku.app.export

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class HtmlToPdfRendererInstrumentedTest {

    @Test fun renders_valid_pdf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val html = BauhausHtml.document(
            headerTitle = "Test",
            headerSubtitle = "Erstellt am heute",
            contentHtml = "<section class=\"month\"><h2 class=\"month\">Juni 2026</h2>" +
                "<div class=\"entry\"><div class=\"when\">Heute</div>" +
                "<div class=\"title\">Beispiel &amp; Emoji 🎉</div></div></section>"
        )
        val out = ByteArrayOutputStream()
        runBlocking { HtmlToPdfRenderer(ctx).render(html, out) }

        val bytes = out.toByteArray()
        assertTrue("PDF sollte nicht leer sein", bytes.size > 200)
        val magic = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue("Sollte mit %PDF- beginnen, war: $magic", magic == "%PDF-")
    }
}
