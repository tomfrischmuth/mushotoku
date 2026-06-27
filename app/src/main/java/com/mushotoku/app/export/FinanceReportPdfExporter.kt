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
import com.mushotoku.app.ui.AppCurrency
import com.mushotoku.app.ui.strings.AppStrings
import com.mushotoku.app.ui.strings.ExportStrings
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class FinanceReportPdfExporter(
    private val context: Context,
    private val db: AppDatabase,
    private val strings: ExportStrings,
    private val appStrings: AppStrings,
    private val currency: AppCurrency,
    private val locale: Locale,
) : Exporter {

    override val category = ExportCategory.FINANZBERICHT
    override fun supportedFormats() = listOf(ExportFormat.PDF)
    override fun mimeType(format: ExportFormat) = "application/pdf"
    override fun defaultBaseName() = strings.baseFinance

    override suspend fun hasData(): Boolean =
        db.expenseDao().getAll().isNotEmpty() ||
            db.recurringCostHistoryDao().getAll().first().isNotEmpty()

    override suspend fun write(
        out: OutputStream,
        format: ExportFormat,
        options: ExportOptions,
        onProgress: (Int, Int) -> Unit
    ) {
        onProgress(0, 1)
        val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        val createdFmt = DateTimeFormatter.ofPattern("d. MMMM yyyy", locale)

        val expenses = db.expenseDao().getAll()
        val categories = db.categoryDao().getAllCategories().first()
        val history = db.recurringCostHistoryDao().getAll().first()
        val catById = categories.associateBy { it.id }

        val months = buildSet {
            expenses.forEach { val d = LocalDate.ofEpochDay(it.date); add(YearMonth.of(d.year, d.monthValue)) }
            add(YearMonth.now())
        }.sortedDescending()

        val sections = months.mapNotNull { month ->
            val from = month.atDay(1)
            val to = month.atEndOfMonth()
            val monthStr = "%04d-%02d".format(month.year, month.monthValue)

            val byCategory = mutableMapOf<String, Double>()
            expenses
                .filter { val d = LocalDate.ofEpochDay(it.date); !d.isBefore(from) && !d.isAfter(to) }
                .forEach { byCategory[it.category] = (byCategory[it.category] ?: 0.0) + it.amount }
            val recurring = history
                .filter { it.startMonth <= monthStr && (it.endMonth == null || it.endMonth >= monthStr) }
            recurring.forEach { byCategory[it.categoryId] = (byCategory[it.categoryId] ?: 0.0) + it.amount }

            val groups = byCategory.entries
                .filter { it.value > 0.0 }
                .groupBy { (id, _) -> catById[id]?.group ?: "Sonstiges" }
                .map { (group, entries) ->
                    FinanceReportPdf.Group(
                        name = appStrings.groupName(group),
                        total = entries.sumOf { it.value },
                        items = entries.sortedByDescending { it.value }.map { (id, amt) ->
                            (catById[id]?.let { appStrings.categoryName(it.id, it.name) } ?: id) to amt
                        }
                    )
                }
                .sortedByDescending { it.total }

            if (groups.isEmpty()) null
            else FinanceReportPdf.Section(
                heading = month.format(monthFmt).replaceFirstChar { it.uppercase() },
                groups = groups,
                recurring = recurring.sumOf { it.amount },
                total = groups.sumOf { it.total }
            )
        }

        val today = LocalDate.now().format(createdFmt)
        val html = FinanceReportPdf.html(
            title = strings.financeReportHeading,
            subtitle = "${strings.pdfCreatedOn} $today",
            sections = sections,
            currency = currency,
            monthTotalLabel = strings.financeMonthTotal,
            inclRecurringTemplate = strings.financeInclRecurring,
        )

        out.use { HtmlToPdfRenderer(context).render(html, it) }
        onProgress(1, 1)
    }
}
