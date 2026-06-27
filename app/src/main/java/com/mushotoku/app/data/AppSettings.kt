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

package com.mushotoku.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val financeTabEnabled: Boolean = true,
    val themeMode: String = "DARK",
    val fontScale: Float  = 1.0f,
    val language: String  = "AUTO",
    val salary: Double    = 0.0,
    val salaryDay: String = "FIRST",
    val confirmDeleteEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val totalMeditatedMinutes: Int = 0,
    val desiredBedtimeMinutes: Int = 23 * 60,
    val caffeineMetabolism: String = "NORMAL",
    val currency: String = "EUR",
    val appLockTimeoutSeconds: Int = 0,
    val notificationsEnabled: Boolean = true,
    val notificationLeadMinutes: Int = 15,
    val blockScreenshots: Boolean = false,
    val showHolidays: Boolean = false,
    val holidayCountry: String = "",
    val holidayRegion: String = "",
    val includeHolidaysInExport: Boolean = true,
)
