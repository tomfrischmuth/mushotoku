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

import android.content.Context
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.ui.strings.ExportStrings
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.coroutineContext

class NotesZipExporter(
    private val context: Context,
    private val db: AppDatabase,
    private val strings: ExportStrings,
    private val locale: Locale,
) : Exporter {

    override val category = ExportCategory.NOTIZEN
    override fun supportedFormats() = listOf(ExportFormat.ZIP_TXT, ExportFormat.ZIP_PDF)
    override fun mimeType(format: ExportFormat) = "application/zip"
    override fun defaultBaseName() = strings.baseNotes

    override suspend fun hasData(): Boolean = db.noteDao().getAllOnce().isNotEmpty()

    override suspend fun write(
        out: OutputStream,
        format: ExportFormat,
        options: ExportOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        val notes = db.noteDao().getAllOnce().sortedBy { it.createdAt }
        val total = notes.size
        val taken = mutableSetOf<String>()
        val asPdf = format == ExportFormat.ZIP_PDF
        val ext = if (asPdf) "pdf" else "txt"
        val renderer = if (asPdf) HtmlToPdfRenderer(context) else null
        val createdFmt = DateTimeFormatter.ofPattern("d. MMMM yyyy", locale)

        onProgress(0, total)
        ZipBuilder(out).use { zip ->
            notes.forEachIndexed { index, note ->
                coroutineContext.ensureActive()
                val cleanTitle = stripMarkdownHeading(note.title)
                val base = ExportFilenameSanitizer.sanitize(cleanTitle, strings.pdfNoteFallback + "_", index + 1)
                val fileName = ExportFilenameSanitizer.uniqueFileName(base, ext, taken)

                if (asPdf) {
                    val subtitle = if (note.createdAt > 0L)
                        "${strings.pdfCreatedOn} ${Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(createdFmt)}"
                    else ""
                    val html = BauhausHtml.document(
                        headerTitle = cleanTitle.ifBlank { base },
                        headerSubtitle = subtitle,
                        contentHtml = "<div class=\"note-body\">${BauhausHtml.escape(note.content)}</div>"
                    )
                    val bytes = ByteArrayOutputStream().also { renderer!!.render(html, it) }.toByteArray()
                    zip.addEntry(fileName, bytes)
                } else {
                    zip.addTextEntry(fileName, note.content)
                }
                onProgress(index + 1, total)
            }
        }
    }

    private fun stripMarkdownHeading(raw: String): String =
        raw.trim()
            .removePrefix("### ")
            .removePrefix("## ")
            .removePrefix("# ")
            .trim()
}
