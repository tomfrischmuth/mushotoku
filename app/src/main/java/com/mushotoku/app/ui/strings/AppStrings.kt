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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mushotoku.app.R
import com.mushotoku.app.data.NoteType
import java.util.Locale

@Immutable
class AppStrings(private val ctx: Context) {

    val locale: Locale get() = ctx.resources.configuration.locales[0]

    private fun s(id: Int): String = ctx.getString(id)
    private fun s(id: Int, vararg args: Any?): String = ctx.getString(id, *args)
    private fun q(id: Int, n: Int): String = ctx.resources.getQuantityString(id, n, n)
    private fun arr(id: Int): List<String> = ctx.resources.getStringArray(id).toList()

    val tabToday get() = s(R.string.tab_today)
    val tabFinance get() = s(R.string.tab_finance)
    val tabNotes get() = s(R.string.tab_notes)
    val today get() = s(R.string.today)
    val tomorrow get() = s(R.string.tomorrow)
    val yesterday get() = s(R.string.yesterday)
    val allDay get() = s(R.string.all_day)
    val appointmentDoneExpressions: List<String> by lazy { arr(R.array.appointment_done_expressions) }
    val add get() = s(R.string.add)
    val back get() = s(R.string.back)
    val cancel get() = s(R.string.cancel)
    val delete get() = s(R.string.delete)
    val close get() = s(R.string.close)
    val export get() = s(R.string.export)
    val settings get() = s(R.string.settings)
    val menuDisplay get() = s(R.string.menu_display)
    val menuDisplaySubtitle get() = s(R.string.menu_display_subtitle)
    val menuFinance get() = s(R.string.menu_finance)
    val menuFinanceSubtitle get() = s(R.string.menu_finance_subtitle)
    val menuExport get() = s(R.string.menu_export)
    val menuExportSubtitle get() = s(R.string.menu_export_subtitle)
    val menuData get() = s(R.string.menu_data)
    val menuDataSubtitle get() = s(R.string.menu_data_subtitle)
    val menuLicenses get() = s(R.string.menu_licenses)
    val menuLicensesSubtitle get() = s(R.string.menu_licenses_subtitle)
    val sectionAppearance get() = s(R.string.section_appearance)
    val modeLabel get() = s(R.string.mode_label)
    val modeLight get() = s(R.string.mode_light)
    val modeDark get() = s(R.string.mode_dark)
    val modeSystem get() = s(R.string.mode_system)
    val fontSizeLabel get() = s(R.string.font_size_label)
    val fontSmall get() = s(R.string.font_small)
    val fontNormal get() = s(R.string.font_normal)
    val fontLarge get() = s(R.string.font_large)
    val languageLabel get() = s(R.string.language_label)
    val langAuto get() = s(R.string.lang_auto)
    val langGerman get() = s(R.string.lang_german)
    val langEnglish get() = s(R.string.lang_english)
    val langSpanish get() = s(R.string.lang_spanish)
    val langFrench get() = s(R.string.lang_french)
    val langItalian get() = s(R.string.lang_italian)
    val langPortuguesePt get() = s(R.string.lang_portuguese_pt)
    val langPortugueseBr get() = s(R.string.lang_portuguese_br)
    val langDutch get() = s(R.string.lang_dutch)
    val langPolish get() = s(R.string.lang_polish)
    val confirmDeleteLabel get() = s(R.string.confirm_delete_label)
    val hapticFeedbackLabel get() = s(R.string.haptic_feedback_label)
    val appIconLabel get() = s(R.string.app_icon_label)
    val iconOriginal get() = s(R.string.icon_original)
    val iconInverse get() = s(R.string.icon_inverse)
    val iconWarmEmber get() = s(R.string.icon_warm_ember)
    val iconBlue get() = s(R.string.icon_blue)
    val iconRainbow get() = s(R.string.icon_rainbow)
    val iconRetro get() = s(R.string.icon_retro)
    val iconMinimalist get() = s(R.string.icon_minimalist)
    val iconPlayful get() = s(R.string.icon_playful)
    val sectionFinanceTab get() = s(R.string.section_finance_tab)
    val showFinanceTab get() = s(R.string.show_finance_tab)
    val sectionCategories get() = s(R.string.section_categories)
    val recurringCostsLabel get() = s(R.string.recurring_costs_label)
    val perMonth get() = s(R.string.per_month)
    val addCategory get() = s(R.string.add_category)
    val resetCategories get() = s(R.string.reset_categories)
    val resetCategoriesDialogTitle get() = s(R.string.reset_categories_dialog_title)
    val resetCategoriesDialogText get() = s(R.string.reset_categories_dialog_text)
    val resetCategoriesConfirm get() = s(R.string.reset_categories_confirm)
    val deleteCategoryDialogTitle get() = s(R.string.delete_category_dialog_title)
    val deleteCategoryDialogText get() = s(R.string.delete_category_dialog_text)
    val categoryNameHint get() = s(R.string.category_name_hint)
    val categoryGroupHint get() = s(R.string.category_group_hint)
    val sectionData get() = s(R.string.section_data)
    val deleteFinanceData get() = s(R.string.delete_finance_data)
    val deleteFinanceDialogTitle get() = s(R.string.delete_finance_dialog_title)
    val deleteFinanceDialogText get() = s(R.string.delete_finance_dialog_text)
    val deleteAllTasks get() = s(R.string.delete_all_tasks)
    val deleteAllTasksDialogTitle get() = s(R.string.delete_all_tasks_dialog_title)
    val deleteAllTasksDialogText get() = s(R.string.delete_all_tasks_dialog_text)
    val deleteAllAppointments get() = s(R.string.delete_all_appointments)
    val deleteAllAppointmentsDialogTitle get() = s(R.string.delete_all_appointments_dialog_title)
    val deleteAllAppointmentsDialogText get() = s(R.string.delete_all_appointments_dialog_text)
    val deleteAllHabits get() = s(R.string.delete_all_habits)
    val deleteAllHabitsDialogTitle get() = s(R.string.delete_all_habits_dialog_title)
    val deleteAllHabitsDialogText get() = s(R.string.delete_all_habits_dialog_text)
    val deleteAllNotes get() = s(R.string.delete_all_notes)
    val deleteAllNotesDialogTitle get() = s(R.string.delete_all_notes_dialog_title)
    val deleteAllNotesDialogText get() = s(R.string.delete_all_notes_dialog_text)
    val currencyLabel get() = s(R.string.currency_label)
    val sectionSalary get() = s(R.string.section_salary)
    val salaryAmountLabel get() = s(R.string.salary_amount_label)
    val salaryDayLabel get() = s(R.string.salary_day_label)
    val salaryDayFirst get() = s(R.string.salary_day_first)
    val salaryDayFifteenth get() = s(R.string.salary_day_fifteenth)
    val budgetDialogTitle get() = s(R.string.budget_dialog_title)
    val budgetPeriodLabel get() = s(R.string.budget_period_label)
    val budgetSalaryRow get() = s(R.string.budget_salary_row)
    val budgetRecurringRow get() = s(R.string.budget_recurring_row)
    val budgetSpentRow get() = s(R.string.budget_spent_row)
    val budgetRemainingRow get() = s(R.string.budget_remaining_row)
    val budgetDaysLeftRow: (Int) -> String = { days -> q(R.plurals.budget_days_left, days) }
    val budgetPerDayRow get() = s(R.string.budget_per_day_row)
    val budgetNoSalary get() = s(R.string.budget_no_salary)
    val totalLabel get() = s(R.string.total_label)
    val addCustomAmountTitle get() = s(R.string.add_custom_amount_title)
    val oneTimeExpenseBtn get() = s(R.string.one_time_expense_btn)
    val oneTimeExpensesSection get() = s(R.string.one_time_expenses_section)
    val selectGroupHint get() = s(R.string.select_group_hint)
    val selectCategoryHint get() = s(R.string.select_category_hint)
    val monthlyOverviewTitle get() = s(R.string.monthly_overview_title)
    val currentMonthFallback get() = s(R.string.current_month_fallback)
    val noExpensesThisMonth get() = s(R.string.no_expenses_this_month)
    val inclRecurringCosts: (String) -> String = { amt -> s(R.string.incl_recurring_costs, amt) }
    val previousMonths get() = s(R.string.previous_months)
    val inDetailBtn get() = s(R.string.in_detail_btn)
    val savingsPotentialBtn get() = s(R.string.savings_potential_btn)
    val savingsPotentialTitle get() = s(R.string.savings_potential_title)
    val savingsPotentialSubtitle get() = s(R.string.savings_potential_subtitle)
    val savingsUpToPerYear: (String) -> String = { amt -> s(R.string.savings_up_to_per_year, amt) }
    val savingsRangePerYear: (String, String) -> String = { lo, hi -> s(R.string.savings_range_per_year, lo, hi) }
    val savingsBreakdownSection get() = s(R.string.savings_breakdown_section)
    val savingsConfidenceHint get() = s(R.string.savings_confidence_hint)
    val savingsNoData get() = s(R.string.savings_no_data)
    val detailedReportTitle get() = s(R.string.detailed_report_title)
    val createReportBtn get() = s(R.string.create_report_btn)
    val reportMonthLabel get() = s(R.string.report_month_label)
    val reportFrom1st get() = s(R.string.report_from_1st)
    val reportFrom15th get() = s(R.string.report_from_15th)
    val noDataForPeriod get() = s(R.string.no_data_for_period)
    val exportReport get() = s(R.string.export_report)
    val trash get() = s(R.string.trash)
    val trashEmpty get() = s(R.string.trash_empty)
    val trashDeleteAll get() = s(R.string.trash_delete_all)
    val trashDeleteAllDialogTitle get() = s(R.string.trash_delete_all_dialog_title)
    val trashDeleteAllDialogText get() = s(R.string.trash_delete_all_dialog_text)
    val trashRestore get() = s(R.string.trash_restore)
    val noNotesYet get() = s(R.string.no_notes_yet)
    val noRoutinesYet get() = s(R.string.no_routines_yet)
    val noListsYet get() = s(R.string.no_lists_yet)
    val deleteNoteDialogTitle get() = s(R.string.delete_note_dialog_title)
    val deleteNoteDialogText: (String) -> String = { title -> s(R.string.delete_note_dialog_text, title) }
    val addContentHint get() = s(R.string.add_content_hint)
    val noteTypeName: (NoteType) -> String = { type -> s(when (type) {
        NoteType.NOTE -> R.string.note_type_note
        NoteType.LIST -> R.string.note_type_list
        NoteType.ROUTINE -> R.string.note_type_routine
    }) }
    val noteTypeFilterName: (NoteType) -> String = { type -> s(when (type) {
        NoteType.NOTE -> R.string.note_type_filter_notes
        NoteType.LIST -> R.string.note_type_filter_lists
        NoteType.ROUTINE -> R.string.note_type_filter_routines
    }) }
    val notesCount: (Int) -> String = { n -> q(R.plurals.notes_count, n) }
    val notesCountFor: (NoteType, Int) -> String = { type, n -> when (type) {
        NoteType.ROUTINE -> q(R.plurals.notes_count_routine, n)
        NoteType.LIST -> q(R.plurals.notes_count_list, n)
        NoteType.NOTE -> q(R.plurals.notes_count_note, n)
    } }
    val notesCountByType: (Int, Int, Int) -> String = { routines, lists, notes ->
        listOfNotNull(
            if (routines > 0) q(R.plurals.notes_count_routine, routines) else null,
            if (lists > 0) q(R.plurals.notes_count_list, lists) else null,
            if (notes > 0) q(R.plurals.notes_count_note, notes) else null
        ).joinToString(" · ").ifEmpty { s(R.string.no_notes) }
    }
    val notesPinnedSection get() = s(R.string.notes_pinned_section)
    val notesFilterAll get() = s(R.string.notes_filter_all)
    val notesSearchHint get() = s(R.string.notes_search_hint)
    val notesContentHint get() = s(R.string.notes_content_hint)
    val notesNoResults get() = s(R.string.notes_no_results)
    val notesDone get() = s(R.string.notes_done)
    val notesEdit get() = s(R.string.notes_edit)
    val notesSelected: (Int) -> String = { n -> s(R.string.notes_selected, n) }
    val notesDeleteSelected get() = s(R.string.notes_delete_selected)
    val notesPinAction get() = s(R.string.notes_pin_action)
    val notesUnpinAction get() = s(R.string.notes_unpin_action)
    val notesChecklistHint get() = s(R.string.notes_checklist_hint)
    val dialogNewAppointment get() = s(R.string.dialog_new_appointment)
    val dialogNewTask get() = s(R.string.dialog_new_task)
    val chipTask get() = s(R.string.chip_task)
    val chipAppointment get() = s(R.string.chip_appointment)
    val placeholderAppointment get() = s(R.string.placeholder_appointment)
    val placeholderTask get() = s(R.string.placeholder_task)
    val setTimeBtn get() = s(R.string.set_time_btn)
    val setDateBtn get() = s(R.string.set_date_btn)
    val dialogNewNote get() = s(R.string.dialog_new_note)
    val placeholderTitle get() = s(R.string.placeholder_title)
    val placeholderContent get() = s(R.string.placeholder_content)
    val deleteTaskDialogTitle: (Boolean) -> String = { isAppt ->
        s(if (isAppt) R.string.delete_appointment_dialog_title else R.string.delete_task_dialog_title)
    }
    val deleteTaskDialogText: (String) -> String = { title -> s(R.string.delete_task_dialog_text, title) }
    val appointmentsSection get() = s(R.string.appointments_section)
    val tasksSection get() = s(R.string.tasks_section)
    val habitsSection get() = s(R.string.habits_section)
    val dialogNewHabit get() = s(R.string.dialog_new_habit)
    val addHabit get() = s(R.string.add_habit)
    val habitNameHint get() = s(R.string.habit_name_hint)
    val deleteHabitDialogTitle get() = s(R.string.delete_habit_dialog_title)
    val deleteHabitDialogText: (String) -> String = { name -> s(R.string.delete_habit_dialog_text, name) }
    val recurrenceLabel get() = s(R.string.recurrence_label)
    val recurrenceName: (String) -> String = { r -> when (r) {
        "DAILY" -> s(R.string.recurrence_daily)
        "EVERY_OTHER_DAY" -> s(R.string.recurrence_every_other_day)
        "WEEKLY" -> s(R.string.recurrence_weekly)
        "BIWEEKLY" -> s(R.string.recurrence_biweekly)
        "MONTHLY" -> s(R.string.recurrence_monthly)
        else -> r
    } }
    val streakHistoryTitle get() = s(R.string.streak_history_title)
    val calendarTitle get() = s(R.string.calendar_title)
    val goToDate get() = s(R.string.go_to_date)
    val weekDays: List<String> by lazy { arr(R.array.week_days) }
    val groupName: (String) -> String = { group ->
        groupResId(group)?.let { s(it) } ?: group
    }
    val categoryName: (String, String) -> String = { id, fallback ->
        categoryResId(id)?.let { s(it) } ?: fallback
    }
    val meditationTitle get() = s(R.string.meditation_title)
    val meditationTimerCard get() = s(R.string.meditation_timer_card)
    val meditationGratitudeCard get() = s(R.string.meditation_gratitude_card)
    val meditationMoodCard get() = s(R.string.meditation_mood_card)
    val meditationHighlightsWeek get() = s(R.string.meditation_highlights_week)
    val meditationHighlightsMonth get() = s(R.string.meditation_highlights_month)
    val meditationDurationLabel get() = s(R.string.meditation_duration_label)
    val meditationBellLabel get() = s(R.string.meditation_bell_label)
    val meditationBellNone get() = s(R.string.meditation_bell_none)
    val meditationStart get() = s(R.string.meditation_start)
    val meditationStop get() = s(R.string.meditation_stop)
    val meditationPause get() = s(R.string.meditation_pause)
    val meditationResume get() = s(R.string.meditation_resume)
    val meditationArchive get() = s(R.string.meditation_archive)
    val meditationArchiveEmpty get() = s(R.string.meditation_archive_empty)
    val meditationBreathing get() = s(R.string.meditation_breathing)
    val meditationSessionComplete get() = s(R.string.meditation_session_complete)
    val meditationGratitudeToday get() = s(R.string.meditation_gratitude_today)
    val meditationGratitudeHint: (Int) -> String = { n -> s(R.string.meditation_gratitude_hint, n) }
    val meditationGratitudeSave get() = s(R.string.meditation_gratitude_save)
    val meditationNoGratitudeToday get() = s(R.string.meditation_no_gratitude_today)
    val meditationMoodQuestion get() = s(R.string.meditation_mood_question)
    val meditationMoodEmoji: (Int) -> String = { m -> when (m) { 1 -> "😢"; 2 -> "😔"; 3 -> "😐"; 4 -> "😊"; else -> "😄" } }
    val meditationMinutes: (Int) -> String = { n -> q(R.plurals.meditation_minutes, n) }
    val meditationWeekDays: (Int) -> String = { n -> s(R.string.meditation_week_days, n) }
    val meditationMonthDays: (Int) -> String = { n -> s(R.string.meditation_month_days, n) }
    val meditationWeekEntries: (Int) -> String = { n -> q(R.plurals.meditation_week_entries, n) }
    val meditationAvgMood get() = s(R.string.meditation_avg_mood)
    val meditationStreak: (Int) -> String = { n -> q(R.plurals.meditation_streak, n) }
    val meditationBestStreak: (Int) -> String = { n -> q(R.plurals.meditation_best_streak, n) }
    val meditationMinutesLabel get() = s(R.string.meditation_minutes_label)
    val meditationJournalSingular get() = s(R.string.meditation_journal_singular)
    val meditationJournalPlural get() = s(R.string.meditation_journal_plural)
    val meditationMoodAll get() = s(R.string.meditation_mood_all)
    val meditationMoodMonth get() = s(R.string.meditation_mood_month)
    val meditationMoodWeek get() = s(R.string.meditation_mood_week)
    val deleteAllMindfulness get() = s(R.string.delete_all_mindfulness)
    val deleteAllMindfulnessDialogTitle get() = s(R.string.delete_all_mindfulness_dialog_title)
    val deleteAllMindfulnessDialogText get() = s(R.string.delete_all_mindfulness_dialog_text)
    val meditationHint get() = s(R.string.meditation_hint)
    val sleepLabCard get() = s(R.string.sleep_lab_card)
    val sleepLabCardSubtitle get() = s(R.string.sleep_lab_card_subtitle)
    val sleepLabTitle get() = s(R.string.sleep_lab_title)
    val sleepLabHint get() = s(R.string.sleep_lab_hint)
    val sleepLabProtectTitle get() = s(R.string.sleep_lab_protect_title)
    val sleepLabCaffeineTitle get() = s(R.string.sleep_lab_caffeine_title)
    val sleepLabActiveAtBed: (Int, String) -> String = { mg, time -> s(R.string.sleep_lab_active_at_bed, mg, time) }
    val sleepLabEnoughTitle get() = s(R.string.sleep_lab_enough_title)
    val sleepLabCutoff: (String) -> String = { time -> s(R.string.sleep_lab_cutoff, time) }
    val sleepLabNoMore get() = s(R.string.sleep_lab_no_more)
    val sleepLabRuheGeschuetzt get() = s(R.string.sleep_lab_ruhe_geschuetzt)
    val sleepLabAnchor get() = s(R.string.sleep_lab_anchor)
    val sleepLabNoCaffeine get() = s(R.string.sleep_lab_no_caffeine)
    val sleepLabAddCaffeine get() = s(R.string.sleep_lab_add_caffeine)
    val sleepLabPresetCoffee get() = s(R.string.sleep_lab_preset_coffee)
    val sleepLabPresetEspresso get() = s(R.string.sleep_lab_preset_espresso)
    val sleepLabPresetTea get() = s(R.string.sleep_lab_preset_tea)
    val sleepLabPresetEnergy get() = s(R.string.sleep_lab_preset_energy)
    val sleepLabPresetCola get() = s(R.string.sleep_lab_preset_cola)
    val sleepLabMg: (Int) -> String = { n -> s(R.string.sleep_lab_mg, n) }
    val sleepLabBedtimeLabel get() = s(R.string.sleep_lab_bedtime_label)
    val sleepLabMetabolism get() = s(R.string.sleep_lab_metabolism)
    val sleepLabMetabFast get() = s(R.string.sleep_lab_metab_fast)
    val sleepLabMetabNormal get() = s(R.string.sleep_lab_metab_normal)
    val sleepLabMetabSlow get() = s(R.string.sleep_lab_metab_slow)
    val additionalIncomeSection get() = s(R.string.additional_income_section)
    val addIncomeBtn get() = s(R.string.add_income_btn)
    val addIncomeTitle get() = s(R.string.add_income_title)
    val incomeLabelHint get() = s(R.string.income_label_hint)
    val budgetAdditionalIncomeRow get() = s(R.string.budget_additional_income_row)
    val noteLinkSection get() = s(R.string.note_link_section)
    val noteLinkExisting get() = s(R.string.note_link_existing)
    val noteLinkNew get() = s(R.string.note_link_new)
    val noteLinkTitleHint get() = s(R.string.note_link_title_hint)
    val noteLinkPickerTitle get() = s(R.string.note_link_picker_title)
    val noteLinkLinkedTo: (String) -> String = { title -> s(R.string.note_link_linked_to, title) }
    val noteLinkGoToAppointment get() = s(R.string.note_link_go_to_appointment)
    val menuCalendar get() = s(R.string.menu_calendar)
    val menuCalendarSubtitle get() = s(R.string.menu_calendar_subtitle)
    val menuSecurity get() = s(R.string.menu_security)
    val menuSecuritySubtitle get() = s(R.string.menu_security_subtitle)
    val taskDone get() = s(R.string.task_done)
    val taskMissed get() = s(R.string.task_missed)
    val sleepLabPersonalization get() = s(R.string.sleep_lab_personalization)
    val meditationSoundLabel get() = s(R.string.meditation_sound_label)
    val meditationSoundWarm get() = s(R.string.meditation_sound_warm)
    val meditationSoundBright get() = s(R.string.meditation_sound_bright)
}

private fun groupResId(group: String): Int? = when (group) {
    "Wohnen" -> R.string.group_wohnen
    "Lebensmittel" -> R.string.group_lebensmittel
    "Essen & Trinken" -> R.string.group_essen_trinken
    "Transport" -> R.string.group_transport
    "Gesundheit & Körper" -> R.string.group_gesundheit_koerper
    "Kleidung & Accessoires" -> R.string.group_kleidung_accessoires
    "Freizeit" -> R.string.group_freizeit
    "Sport" -> R.string.group_sport
    "Reisen" -> R.string.group_reisen
    "Digitales" -> R.string.group_digitales
    "Bildung" -> R.string.group_bildung
    "Soziales" -> R.string.group_soziales
    "Haustiere" -> R.string.group_haustiere
    "Finanzen & Vorsorge" -> R.string.group_finanzen_vorsorge
    "Familie & Kinder" -> R.string.group_familie_kinder
    "Beruf & Büro" -> R.string.group_beruf_buero
    "Sonstiges" -> R.string.group_sonstiges
    else -> null
}

private fun categoryResId(id: String): Int? = when (id) {
    "miete" -> R.string.category_miete
    "nebenkosten" -> R.string.category_nebenkosten
    "strom" -> R.string.category_strom
    "wasser" -> R.string.category_wasser
    "heizung" -> R.string.category_heizung
    "internet" -> R.string.category_internet
    "reparaturen" -> R.string.category_reparaturen
    "haushalt" -> R.string.category_haushalt
    "gartenarbeit" -> R.string.category_gartenarbeit
    "supermarkt" -> R.string.category_supermarkt
    "wochenmarkt" -> R.string.category_wochenmarkt
    "bio_laden" -> R.string.category_bio_laden
    "getraenke" -> R.string.category_getraenke
    "snacks" -> R.string.category_snacks
    "restaurant" -> R.string.category_restaurant
    "cafe" -> R.string.category_cafe
    "takeaway" -> R.string.category_takeaway
    "lieferdienst" -> R.string.category_lieferdienst
    "kantine" -> R.string.category_kantine
    "bar" -> R.string.category_bar
    "fahrzeugleasing" -> R.string.category_fahrzeugleasing
    "tanken" -> R.string.category_tanken
    "parkgebuehren" -> R.string.category_parkgebuehren
    "kfz_versicherung" -> R.string.category_kfz_versicherung
    "kfz_steuer" -> R.string.category_kfz_steuer
    "werkstatt" -> R.string.category_werkstatt
    "oepnv" -> R.string.category_oepnv
    "fahrdienste" -> R.string.category_fahrdienste
    "fahrrad" -> R.string.category_fahrrad
    "escooter" -> R.string.category_escooter
    "medikamente" -> R.string.category_medikamente
    "arztbesuch" -> R.string.category_arztbesuch
    "zahnarzt" -> R.string.category_zahnarzt
    "krankenkasse" -> R.string.category_krankenkasse
    "brille" -> R.string.category_brille
    "koerperpflege" -> R.string.category_koerperpflege
    "kosmetik" -> R.string.category_kosmetik
    "friseur" -> R.string.category_friseur
    "wellness" -> R.string.category_wellness
    "kleidung" -> R.string.category_kleidung
    "schuhe" -> R.string.category_schuhe
    "taschen" -> R.string.category_taschen
    "accessoires" -> R.string.category_accessoires
    "sportkleidung" -> R.string.category_sportkleidung
    "reinigung" -> R.string.category_reinigung
    "events" -> R.string.category_events
    "ausstellungen" -> R.string.category_ausstellungen
    "sportveranstalt" -> R.string.category_sportveranstalt
    "lesestoff" -> R.string.category_lesestoff
    "kulturveranstalt" -> R.string.category_kulturveranstalt
    "fitnessstudio" -> R.string.category_fitnessstudio
    "sportkurse" -> R.string.category_sportkurse
    "sportverein" -> R.string.category_sportverein
    "sportausruestung" -> R.string.category_sportausruestung
    "schwimmbad" -> R.string.category_schwimmbad
    "outdoor_sport" -> R.string.category_outdoor_sport
    "indoor_sport" -> R.string.category_indoor_sport
    "fluege" -> R.string.category_fluege
    "unterkunft" -> R.string.category_unterkunft
    "mietwagen" -> R.string.category_mietwagen
    "reiseversicherung" -> R.string.category_reiseversicherung
    "aktivitaeten" -> R.string.category_aktivitaeten
    "souvenirs" -> R.string.category_souvenirs
    "reisegepaeck" -> R.string.category_reisegepaeck
    "smartphone" -> R.string.category_smartphone
    "computer" -> R.string.category_computer
    "mobilfunk" -> R.string.category_mobilfunk
    "software" -> R.string.category_software
    "abonnements" -> R.string.category_abonnements
    "streaming" -> R.string.category_streaming
    "gaming" -> R.string.category_gaming
    "fachliteratur" -> R.string.category_fachliteratur
    "schule_uni" -> R.string.category_schule_uni
    "seminare" -> R.string.category_seminare
    "sprachen" -> R.string.category_sprachen
    "online_kurse" -> R.string.category_online_kurse
    "spenden" -> R.string.category_spenden
    "geschenke" -> R.string.category_geschenke
    "vereinsbeitraege" -> R.string.category_vereinsbeitraege
    "hochzeiten" -> R.string.category_hochzeiten
    "tierfutter" -> R.string.category_tierfutter
    "tierarzt" -> R.string.category_tierarzt
    "tier_zubehoer" -> R.string.category_tier_zubehoer
    "tierpension" -> R.string.category_tierpension
    "tierversicherung" -> R.string.category_tierversicherung
    "hundesteuer" -> R.string.category_hundesteuer
    "krankenversicherung" -> R.string.category_krankenversicherung
    "lebensversicherung" -> R.string.category_lebensversicherung
    "haftpflicht" -> R.string.category_haftpflicht
    "altersvorsorge" -> R.string.category_altersvorsorge
    "investitionen" -> R.string.category_investitionen
    "sparbetrag" -> R.string.category_sparbetrag
    "kredit" -> R.string.category_kredit
    "steuernachzahlung" -> R.string.category_steuernachzahlung
    "steuerberater" -> R.string.category_steuerberater
    "kinderbetreuung" -> R.string.category_kinderbetreuung
    "schulbedarf" -> R.string.category_schulbedarf
    "essensgeld" -> R.string.category_essensgeld
    "nachhilfe" -> R.string.category_nachhilfe
    "vereine_kinder" -> R.string.category_vereine_kinder
    "spielzeug" -> R.string.category_spielzeug
    "freizeitaktivit" -> R.string.category_freizeitaktivit
    "kinderkleidung" -> R.string.category_kinderkleidung
    "windeln" -> R.string.category_windeln
    "babynahrung" -> R.string.category_babynahrung
    "babymoebel" -> R.string.category_babymoebel
    "babysitter" -> R.string.category_babysitter
    "taschengeld" -> R.string.category_taschengeld
    "unterhalt" -> R.string.category_unterhalt
    "arbeitsmaterial" -> R.string.category_arbeitsmaterial
    "homeoffice" -> R.string.category_homeoffice
    "berufskleidung" -> R.string.category_berufskleidung
    "weiterbildung" -> R.string.category_weiterbildung
    "bussgelder" -> R.string.category_bussgelder
    "rechtsanwalt" -> R.string.category_rechtsanwalt
    "sonstiges" -> R.string.category_sonstiges
    else -> null
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { error("LocalAppStrings not provided") }
