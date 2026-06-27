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

package com.mushotoku.app.ui.screens
import com.mushotoku.app.ui.components.*
import com.mushotoku.app.ui.*

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mushotoku.app.data.AppSettings

@Composable
internal fun KalenderSection(
    settings: AppSettings,
    notificationsPermissionGranted: Boolean,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetNotificationLead: (Int) -> Unit,
    onSetShowHolidays: (Boolean) -> Unit,
    onSetHolidayCountry: (String) -> Unit,
    onSetHolidayRegion: (String) -> Unit,
    onSetIncludeHolidaysInExport: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        NotificationsSection(
            settings = settings,
            permissionGranted = notificationsPermissionGranted,
            onSetEnabled = onSetNotificationsEnabled,
            onSetLead = onSetNotificationLead,
        )
        HolidaysSection(
            settings = settings,
            onSetShowHolidays = onSetShowHolidays,
            onSetCountry = onSetHolidayCountry,
            onSetRegion = onSetHolidayRegion,
            onSetIncludeInExport = onSetIncludeHolidaysInExport,
        )
    }
}
