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

package com.mushotoku.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.ui.components.AppTab
import com.mushotoku.app.ui.components.soundClick
import com.mushotoku.app.ui.components.BottomBar
import com.mushotoku.app.ui.components.DateSlider
import com.mushotoku.app.ui.components.NoteTypeFilter
import com.mushotoku.app.ui.screens.NoteEditorBarState
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.collections.immutable.ImmutableList

private val AccentBlue = Color(0xFF3D5AFE)

@Composable
internal fun TodayTopBar(
    modifier: Modifier,
    selectedDate: LocalDate,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    val today = LocalDate.now()
    val dayLabel = when (selectedDate) {
        today              -> strings.today
        today.plusDays(1)  -> strings.tomorrow
        today.minusDays(1) -> strings.yesterday
        else -> selectedDate.dayOfWeek.getDisplayName(
            java.time.format.TextStyle.FULL, strings.locale
        ).replaceFirstChar { it.uppercase() }
    }
    AppTopBar(
        modifier = modifier,
        title    = dayLabel,
        subtitle = selectedDate.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", strings.locale)),
        trailing = {
            IconButton(onClick = soundClick(onOpenCalendar)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = strings.calendarTitle, tint = AccentBlue)
            }
            IconButton(onClick = soundClick(onOpenSettings)) {
                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = AccentBlue)
            }
        }
    )
}

@Composable
internal fun NotesTopBar(
    modifier: Modifier,
    noteEditorActive: Boolean,
    editorBar: NoteEditorBarState?,
    selectedNoteIds: Set<Long>,
    notes: ImmutableList<Note>,
    noteTypeFilter: NoteType?,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    if (noteEditorActive && editorBar != null) {
        AppTopBar(
            modifier = modifier,
            title    = " ",
            subtitle = " ",
            leading  = {
                IconButton(onClick = soundClick(editorBar.onBack)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = AccentBlue)
                }
            },
            trailing = {
                TextButton(onClick = soundClick(editorBar.onToggle)) {
                    Text(
                        text       = if (editorBar.isEditing) strings.notesDone else strings.notesEdit,
                        color      = AccentBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    } else if (selectedNoteIds.isNotEmpty()) {
        AppTopBar(
            modifier = modifier,
            title    = strings.notesSelected(selectedNoteIds.size),
            leading  = {
                IconButton(onClick = soundClick(onClearSelection)) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = AccentBlue)
                }
            },
            trailing = {
                IconButton(onClick = soundClick(onDeleteSelected)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = strings.notesDeleteSelected, tint = AccentBlue)
                }
            }
        )
    } else {
        AppTopBar(
            modifier = modifier,
            title    = strings.tabNotes,
            subtitle = if (noteTypeFilter == null)
                strings.notesCountByType(
                    notes.count { it.type == NoteType.ROUTINE },
                    notes.count { it.type == NoteType.LIST },
                    notes.count { it.type == NoteType.NOTE }
                )
            else noteTypeFilter.let { filter ->
                strings.notesCountFor(filter, notes.count { it.type == filter })
            },
            trailing = {
                IconButton(onClick = soundClick(onOpenTrash)) {
                    Icon(Icons.Default.Delete, contentDescription = strings.trash, tint = AccentBlue)
                }
                IconButton(onClick = soundClick(onOpenSettings)) {
                    Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = AccentBlue)
                }
            }
        )
    }
}

@Composable
internal fun FinanceTopBar(
    modifier: Modifier,
    selectedDate: LocalDate,
    onOpenMonthlyOverview: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val strings = LocalAppStrings.current
    AppTopBar(
        modifier = modifier,
        title    = strings.tabFinance,
        subtitle = selectedDate.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", strings.locale)),
        trailing = {
            IconButton(onClick = soundClick(onOpenMonthlyOverview)) {
                Icon(Icons.Default.BarChart, contentDescription = strings.monthlyOverviewTitle, tint = AccentBlue)
            }
            IconButton(onClick = soundClick(onOpenBudget)) {
                Icon(Icons.Default.Savings, contentDescription = strings.budgetDialogTitle, tint = AccentBlue)
            }
            IconButton(onClick = soundClick(onOpenSettings)) {
                Icon(Icons.Default.Settings, contentDescription = strings.settings, tint = AccentBlue)
            }
        }
    )
}

@Composable
internal fun AppBottomBar(
    modifier: Modifier,
    currentTab: AppTab,
    selectedDate: LocalDate,
    noteTypeFilter: NoteType?,
    financeTabEnabled: Boolean,
    glassDividerColor: Color,
    onDateSelected: (LocalDate) -> Unit,
    onTodayLongPress: () -> Unit,
    onNoteTypeSelect: (NoteType?) -> Unit,
    onTabChange: (AppTab) -> Unit,
    dateScrollTrigger: Int = 0
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = glassDividerColor, thickness = 0.5.dp)
        if (currentTab == AppTab.TODAY || currentTab == AppTab.FINANCE) {
            DateSlider(
                selectedDate     = selectedDate,
                onDateSelected   = onDateSelected,
                onTodayLongPress = onTodayLongPress,
                scrollToSelectedTrigger = dateScrollTrigger
            )
        } else if (currentTab == AppTab.NOTES) {
            NoteTypeFilter(
                selected = noteTypeFilter,
                onSelect = onNoteTypeSelect
            )
        }
        BottomBar(
            currentTab  = currentTab,
            showFinance = financeTabEnabled,
            onTabChange = onTabChange
        )
    }
}

@Composable
private fun AppTopBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    titleIcon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start  = if (leading != null) 4.dp else 20.dp,
                end    = 8.dp,
                top    = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (titleIcon != null) {
                    titleIcon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (subtitle != null) {
                Text(
                    text     = subtitle,
                    fontSize = 13.sp,
                    color    = colors.onSurfaceSecondary,
                    modifier = if (onSubtitleClick != null)
                        Modifier.clickable(onClick = onSubtitleClick)
                    else Modifier
                )
            }
        }
        trailing?.invoke()
    }
}
