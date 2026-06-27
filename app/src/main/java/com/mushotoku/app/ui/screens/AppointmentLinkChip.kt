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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Task
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle

@Composable
internal fun AppointmentLinkChip(task: Task, onClick: () -> Unit) {
    val strings = LocalAppStrings.current
    val taskDate = LocalDate.ofEpochDay(task.date)
    val today    = LocalDate.now()
    val dateStr  = when (taskDate) {
        today                -> strings.today
        today.plusDays(1)    -> strings.tomorrow
        today.minusDays(1)   -> strings.yesterday
        else -> taskDate.dayOfWeek
            .getDisplayName(JavaTextStyle.SHORT, strings.locale)
            .replaceFirstChar { it.uppercase() } +
            ", " + taskDate.format(DateTimeFormatter.ofPattern("d. MMM", strings.locale))
    }
    val timeStr  = if (task.time.isNotEmpty()) " · ${task.time}" else ""
    val label    = "$dateStr$timeStr · ${task.title}"

    Surface(
        color    = NoteAccent.copy(alpha = 0.08f),
        shape    = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint               = NoteAccent,
                modifier           = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text     = label,
                fontSize = 13.sp,
                color    = NoteAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = strings.noteLinkGoToAppointment,
                tint               = NoteAccent,
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}
