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
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class JournalPdfExporter(
    private val context: Context,
    private val db: AppDatabase,
    private val strings: ExportStrings,
    private val locale: Locale,
) : Exporter {

    override val category = ExportCategory.JOURNAL
    override fun supportedFormats() = listOf(ExportFormat.PDF)
    override fun mimeType(format: ExportFormat) = "application/pdf"
    override fun defaultBaseName() = strings.baseJournal

    override suspend fun hasData(): Boolean =
        db.gratitudeDao().getAllOnce().any { it.filledCount > 0 }

    override suspend fun write(
        out: OutputStream,
        format: ExportFormat,
        options: ExportOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        onProgress(0, 1)
        val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        val dateFmt = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", locale)
        val createdFmt = DateTimeFormatter.ofPattern("d. MMMM yyyy", locale)

        val entries = db.gratitudeDao().getAllOnce()
            .filter { it.filledCount > 0 }
            .sortedBy { it.date }

        val moodByDate = if (options.includeMood)
            db.moodDao().getAllOnce().associate { it.date to it.mood }
        else emptyMap()

        val byMonth = entries
            .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.date)) }
            .toSortedMap()

        val content = buildString {
            for ((month, items) in byMonth) {
                append("<section class=\"month\">")
                append("<h2 class=\"month\">")
                append(BauhausHtml.escape(month.format(monthFmt).replaceFirstChar { it.uppercase() }))
                append("</h2>")
                for (e in items) {
                    val date = LocalDate.ofEpochDay(e.date)
                    append("<div class=\"entry\">")
                    append("<div class=\"when\">")
                    append(BauhausHtml.escape(date.format(dateFmt).replaceFirstChar { it.uppercase() }))
                    if (options.includeMood) {
                        val mood = moodByDate[e.date]
                        if (mood != null) {
                            val idx = (mood - 1).coerceIn(0, 4)
                            append("<span class=\"mood\" style=\"background:${BauhausHtml.MOOD_COLORS[idx]}\"></span>")
                            append("<span class=\"mood-label\">")
                            append(BauhausHtml.escape(strings.moodLabels[idx]))
                            append("</span>")
                        }
                    }
                    append("</div>")
                    append("<ul class=\"grat-list\">")
                    for (item in listOf(e.entry1, e.entry2, e.entry3)) {
                        if (item.isNotBlank()) {
                            append("<li>").append(BauhausHtml.escape(item)).append("</li>")
                        }
                    }
                    append("</ul>")
                    append("</div>")
                }
                append("</section>")
            }
        }

        val today = LocalDate.now().format(createdFmt)
        val html = BauhausHtml.document(
            headerTitle = strings.journalTitle,
            headerSubtitle = "${strings.pdfCreatedOn} $today",
            contentHtml = content
        )

        out.use { HtmlToPdfRenderer(context).render(html, it) }
        onProgress(1, 1)
    }
}
