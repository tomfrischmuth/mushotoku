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
import com.mushotoku.app.R
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.holidays.DefaultHolidayProvider
import com.mushotoku.app.holidays.HolidayDefaults
import com.mushotoku.app.holidays.HolidayNames
import com.mushotoku.app.holidays.localizedFor
import com.mushotoku.app.ui.strings.ExportStrings
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalendarPdfExporter(
    private val context: Context,
    private val db: AppDatabase,
    private val strings: ExportStrings,
    private val locale: Locale,
) : Exporter {

    override val category = ExportCategory.KALENDER
    override fun supportedFormats() = listOf(ExportFormat.PDF)
    override fun mimeType(format: ExportFormat) = "application/pdf"
    override fun defaultBaseName() = strings.baseCalendar

    override suspend fun hasData(): Boolean =
        db.taskDao().getAllTasks().any { it.isAppointment }

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

        val appointments = db.taskDao().getAllTasks()
            .filter { it.isAppointment }
            .sortedWith(compareBy({ it.date }, { it.time }, { it.id }))

        val byMonth = appointments
            .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.date)) }
            .toSortedMap()

        val content = buildString {
            for ((month, items) in byMonth) {
                append("<section class=\"month\">")
                append("<h2 class=\"month\">")
                append(BauhausHtml.escape(month.format(monthFmt).replaceFirstChar { it.uppercase() }))
                append("</h2>")
                for (t in items) {
                    val date = LocalDate.ofEpochDay(t.date)
                    val timeStr = if (t.time.isNotBlank()) t.time else strings.pdfAllDay
                    append("<div class=\"entry\">")
                    append("<div class=\"when\">")
                    append(BauhausHtml.escape(date.format(dateFmt).replaceFirstChar { it.uppercase() }))
                    append(" · ").append(BauhausHtml.escape(timeStr))
                    append("</div>")
                    append("<div class=\"title\">").append(BauhausHtml.escape(t.title)).append("</div>")
                    append("</div>")
                }
                append("</section>")
            }
            if (appointments.isNotEmpty()) {
                append(holidaySectionHtml(appointments.first().date, appointments.last().date, monthFmt, dateFmt))
            }
        }

        val today = LocalDate.now().format(createdFmt)
        val html = BauhausHtml.document(
            headerTitle = strings.calendarTitle,
            headerSubtitle = "${strings.pdfCreatedOn} $today",
            contentHtml = content
        )

        out.use { HtmlToPdfRenderer(context).render(html, it) }
        onProgress(1, 1)
    }

    private suspend fun holidaySectionHtml(
        minEpochDay: Long,
        maxEpochDay: Long,
        monthFmt: DateTimeFormatter,
        dateFmt: DateTimeFormatter,
    ): String {
        val settings = db.appSettingsDao().getOnce() ?: return ""
        if (!settings.showHolidays || !settings.includeHolidaysInExport) return ""

        val region = HolidayDefaults.resolveRegion(context, settings.holidayCountry, settings.holidayRegion)
        val yMin = LocalDate.ofEpochDay(minEpochDay).year
        val yMax = LocalDate.ofEpochDay(maxEpochDay).year
        val holidays = DefaultHolidayProvider().holidays(region, yMin..yMax)
        if (holidays.isEmpty()) return ""

        val localizedCtx = context.localizedFor(locale)
        val sectionTitle = localizedCtx.getString(R.string.holiday_pdf_section)
        val byMonth = holidays.groupBy { YearMonth.from(it.date) }.toSortedMap()

        return buildString {
            append("<h1 style=\"font-size:15pt;font-weight:800;letter-spacing:.04em;text-transform:uppercase;margin-top:34px;padding-top:16px;border-top:2px solid currentColor;\">")
            append(BauhausHtml.escape(sectionTitle))
            append("</h1>")
            for ((month, items) in byMonth) {
                append("<section class=\"month\">")
                append("<h2 class=\"month\">")
                append(BauhausHtml.escape(month.format(monthFmt).replaceFirstChar { it.uppercase() }))
                append("</h2>")
                for (hol in items) {
                    append("<div class=\"entry\">")
                    append("<div class=\"when\">")
                    append(BauhausHtml.escape(hol.date.format(dateFmt).replaceFirstChar { it.uppercase() }))
                    append("</div>")
                    append("<div class=\"title\">")
                    append(BauhausHtml.escape(HolidayNames.resolve(localizedCtx, hol.nameKey)))
                    append("</div>")
                    append("</div>")
                }
                append("</section>")
            }
        }
    }
}
