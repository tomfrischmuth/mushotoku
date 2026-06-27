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

internal class HolidayStrings(
    val menu: String,
    val menuSubtitle: String,
    val section: String,
    val showLabel: String,
    val showHint: String,
    val countryLabel: String,
    val regionLabel: String,
    val exportLabel: String,
    val exportHint: String,
    val pdfSectionTitle: String,
)

internal fun holidayStrings(ctx: Context): HolidayStrings = HolidayStrings(
    menu = ctx.getString(R.string.holiday_settings_menu),
    menuSubtitle = ctx.getString(R.string.holiday_settings_menu_subtitle),
    section = ctx.getString(R.string.holiday_settings_section),
    showLabel = ctx.getString(R.string.holiday_settings_show_label),
    showHint = ctx.getString(R.string.holiday_settings_show_hint),
    countryLabel = ctx.getString(R.string.holiday_settings_country_label),
    regionLabel = ctx.getString(R.string.holiday_settings_region_label),
    exportLabel = ctx.getString(R.string.holiday_settings_export_label),
    exportHint = ctx.getString(R.string.holiday_settings_export_hint),
    pdfSectionTitle = ctx.getString(R.string.holiday_pdf_section),
)
