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

package com.mushotoku.app.ui.strings

import android.content.Context
import com.mushotoku.app.R

class ExportStrings(
    val sectionTitle: String,
    val calendarTitle: String,
    val calendarDesc: String,
    val journalTitle: String,
    val journalDesc: String,
    val notesTitle: String,
    val notesDesc: String,
    val financeTitle: String,
    val financeDesc: String,
    val formatDialogTitle: String,
    val formatPdf: String,
    val formatZipTxt: String,
    val formatZipPdf: String,
    val includeMoodLabel: String,
    val exportAction: String,
    val cancelAction: String,
    val progressGeneric: String,
    val progressNotesPrefix: String,
    val successPhrases: List<String>,
    val shareAction: String,
    val errorGeneric: String,
    val emptyData: String,
    val baseCalendar: String,
    val baseJournal: String,
    val baseNotes: String,
    val baseFinance: String,
    val pdfCreatedOn: String,
    val pdfAllDay: String,
    val pdfNoteFallback: String,
    val moodLabels: List<String>,
    val financeReportHeading: String,
    val financeMonthTotal: String,
    val financeInclRecurring: String,
)

fun exportStrings(ctx: Context): ExportStrings = ExportStrings(
    sectionTitle = ctx.getString(R.string.export_section_title),
    calendarTitle = ctx.getString(R.string.export_calendar_title),
    calendarDesc = ctx.getString(R.string.export_calendar_desc),
    journalTitle = ctx.getString(R.string.export_journal_title),
    journalDesc = ctx.getString(R.string.export_journal_desc),
    notesTitle = ctx.getString(R.string.export_notes_title),
    notesDesc = ctx.getString(R.string.export_notes_desc),
    financeTitle = ctx.getString(R.string.export_finance_title),
    financeDesc = ctx.getString(R.string.export_finance_desc),
    formatDialogTitle = ctx.getString(R.string.export_format_dialog_title),
    formatPdf = ctx.getString(R.string.export_format_pdf),
    formatZipTxt = ctx.getString(R.string.export_format_zip_txt),
    formatZipPdf = ctx.getString(R.string.export_format_zip_pdf),
    includeMoodLabel = ctx.getString(R.string.export_include_mood_label),
    exportAction = ctx.getString(R.string.export_action),
    cancelAction = ctx.getString(R.string.export_cancel_action),
    progressGeneric = ctx.getString(R.string.export_progress_generic),
    progressNotesPrefix = ctx.getString(R.string.export_progress_notes_prefix),
    successPhrases = ctx.resources.getStringArray(R.array.export_success_phrases).toList(),
    shareAction = ctx.getString(R.string.export_share_action),
    errorGeneric = ctx.getString(R.string.export_error_generic),
    emptyData = ctx.getString(R.string.export_empty_data),
    baseCalendar = ctx.getString(R.string.export_base_calendar),
    baseJournal = ctx.getString(R.string.export_base_journal),
    baseNotes = ctx.getString(R.string.export_base_notes),
    baseFinance = ctx.getString(R.string.export_base_finance),
    pdfCreatedOn = ctx.getString(R.string.export_pdf_created_on),
    pdfAllDay = ctx.getString(R.string.export_pdf_all_day),
    pdfNoteFallback = ctx.getString(R.string.export_pdf_note_fallback),
    moodLabels = ctx.resources.getStringArray(R.array.export_mood_labels).toList(),
    financeReportHeading = ctx.getString(R.string.export_finance_report_heading),
    financeMonthTotal = ctx.getString(R.string.export_finance_month_total),
    financeInclRecurring = ctx.getString(R.string.export_finance_incl_recurring),
)
