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

package com.mushotoku.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mushotoku.app.data.*
import com.mushotoku.app.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val db   = AppDatabase.getInstance(app)
    private val repo = AppRepository(db, db.taskDao(), db.noteDao(), db.expenseDao(), db.categoryDao(), db.appSettingsDao(), db.habitDao(), db.recurringCostHistoryDao(), db.additionalIncomeDao())

    val settings: StateFlow<AppSettings> = repo.getSettings()
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    /**
     * The persisted language, or null until the real value has loaded. Unlike
     * [settings] (whose placeholder initial value is the "AUTO" default), this
     * never emits a transient default — so applying the per-app locale from it
     * won't briefly reset the app to the system language on startup.
     */
    val appliedLanguage: StateFlow<String?> = repo.getSettings()
        .map { (it ?: AppSettings()).language }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFinanceTabEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(financeTabEnabled = enabled))
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(themeMode = mode))
    }

    fun setFontScale(scale: Float) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(fontScale = scale))
    }

    fun setLanguage(lang: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(language = lang))
    }

    fun setSalary(amount: Double) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(salary = amount))
    }

    fun setSalaryDay(day: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(salaryDay = day))
    }

    fun setConfirmDeleteEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(confirmDeleteEnabled = enabled))
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(hapticFeedbackEnabled = enabled))
    }

    fun setCurrency(code: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(currency = code))
    }

    fun setAppLockTimeout(seconds: Int) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(appLockTimeoutSeconds = seconds))
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(notificationsEnabled = enabled))
    }

    fun setNotificationLeadMinutes(minutes: Int) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(notificationLeadMinutes = minutes))
    }

    fun setBlockScreenshots(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(blockScreenshots = enabled))
    }

    fun setShowHolidays(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(showHolidays = enabled))
    }

    fun setHolidayCountry(iso: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(holidayCountry = iso, holidayRegion = ""))
    }

    fun setHolidayRegion(subdivisionIso: String) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(holidayRegion = subdivisionIso))
    }

    fun setIncludeHolidaysInExport(enabled: Boolean) = viewModelScope.launch {
        repo.updateSettings(settings.value.copy(includeHolidaysInExport = enabled))
    }
}
