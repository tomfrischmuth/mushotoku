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
import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Habit
import com.mushotoku.app.data.HabitLog
import com.mushotoku.app.data.Recurrence
import com.mushotoku.app.data.isScheduledFor
import com.mushotoku.app.ui.components.SwipeAction
import com.mushotoku.app.ui.components.SwipeToReveal
import com.mushotoku.app.ui.theme.LocalAppColors
import androidx.compose.ui.platform.LocalFocusManager
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun HabitRow(
    habit: Habit,
    isDone: Boolean,
    streak: Int,
    habitLogs: ImmutableList<HabitLog>,
    selectedDate: LocalDate,
    onToggle: () -> Unit,
    onUpdate: (Habit) -> Unit,
    onDelete: () -> Unit,
    confirmDeleteEnabled: Boolean = true
) {
    val strings      = LocalAppStrings.current
    val colors       = LocalAppColors.current
    val accent       = Color(0xFF3D5AFE)
    val focusManager = LocalFocusManager.current

    var showDeleteDialog       by remember { mutableStateOf(false) }
    var showRecurrenceDialog   by remember { mutableStateOf(false) }
    var showHistoryDialog      by remember { mutableStateOf(false) }
    var editName               by remember(habit.id) { mutableStateOf(habit.name) }
    var isFocused              by remember { mutableStateOf(false) }
    var deleteResetTrigger     by remember { mutableIntStateOf(0) }
    var recurrenceResetTrigger by remember { mutableIntStateOf(0) }
    val prevDeleteShown         = remember { mutableStateOf(false) }
    val prevRecurrenceShown     = remember { mutableStateOf(false) }
    LaunchedEffect(showDeleteDialog) {
        if (prevDeleteShown.value && !showDeleteDialog) deleteResetTrigger++
        prevDeleteShown.value = showDeleteDialog
    }
    LaunchedEffect(showRecurrenceDialog) {
        if (prevRecurrenceShown.value && !showRecurrenceDialog) recurrenceResetTrigger++
        prevRecurrenceShown.value = showRecurrenceDialog
    }

    key(habit.recurrence) {
    SwipeToReveal(
        startAction  = SwipeAction(Icons.Default.Repeat, accent, onAction = { showRecurrenceDialog = true }),
        endAction    = SwipeAction(Icons.Default.Delete, Color(0xFFD32F2F), onAction = {
            if (confirmDeleteEnabled) showDeleteDialog = true else onDelete()
        }),
        resetTrigger = deleteResetTrigger + recurrenceResetTrigger,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .then(
                        if (isDone) Modifier.background(accent, RoundedCornerShape(6.dp))
                        else Modifier
                            .background(Color.Transparent, RoundedCornerShape(6.dp))
                            .border(2.dp, accent, RoundedCornerShape(6.dp))
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            BasicTextField(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                        if (!state.isFocused) {
                            if (editName.isBlank()) editName = habit.name
                            else if (editName != habit.name) onUpdate(habit.copy(name = editName))
                        }
                    },
                textStyle = TextStyle(fontSize = 16.sp, color = colors.onSurface),
                cursorBrush = SolidColor(if (isFocused) Color.Black else Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showHistoryDialog = true }
            ) {
                if (habit.recurrence == Recurrence.DAILY && streak > 0) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = streak.toString(),
                        fontSize = 12.sp,
                        color = Color(0xFFFF6D00)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = colors.onSurfaceSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = strings.recurrenceName(habit.recurrence),
                    fontSize = 12.sp,
                    color = colors.onSurfaceSecondary
                )
            }
        }
    }
    }

    if (showDeleteDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteHabitDialogTitle) },
            text  = { Text(strings.deleteHabitDialogText(habit.name)) },
            confirmButton = {
                TextButton(onClick = soundClick { showDeleteDialog = false; onDelete() }) {
                    Text(strings.delete, color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { showDeleteDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showRecurrenceDialog) {
        GlassAlertDialog(
            onDismissRequest = { showRecurrenceDialog = false },
            title = { Text(strings.recurrenceLabel) },
            text = {
                Column {
                    Recurrence.ALL.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdate(habit.copy(
                                        recurrence   = rec,
                                        createdAtDay = selectedDate.toEpochDay()
                                    ))
                                    showRecurrenceDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = habit.recurrence == rec,
                                onClick  = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(strings.recurrenceName(rec), fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = soundClick { showRecurrenceDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showHistoryDialog) {
        StreakHistoryDialog(
            habit     = habit,
            habitLogs = habitLogs,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
private fun StreakHistoryDialog(
    habit: Habit,
    habitLogs: ImmutableList<HabitLog>,
    onDismiss: () -> Unit
) {
    val strings  = LocalAppStrings.current
    val colors   = LocalAppColors.current
    val doneColor = Color(0xFF3D8AF7)
    val missColor = Color(0xFFD32F2F)

    val today   = LocalDate.now()
    val doneSet = habitLogs.mapTo(HashSet()) { it.date }

    val weeksBack  = 9
    val todayDow   = today.dayOfWeek.value
    val lastMonday = today.minusDays((todayDow - 1).toLong())
    val gridStart  = lastMonday.minusWeeks(weeksBack.toLong())
    val totalRows  = weeksBack + 1

    GlassDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 40.dp, vertical = 18.dp)) {

            Text(
                text  = strings.streakHistoryTitle,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.onSurfaceSecondary,
                modifier   = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text       = habit.name,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color      = colors.onSurface
            )

            Spacer(Modifier.height(14.dp))

            Row {
                strings.weekDays.forEach { label ->
                    Box(
                        modifier          = Modifier.size(26.dp).padding(bottom = 2.dp),
                        contentAlignment  = Alignment.Center
                    ) {
                        Text(
                            text     = label,
                            fontSize = 9.sp,
                            color    = colors.onSurfaceSecondary
                        )
                    }
                    Spacer(Modifier.width(3.dp))
                }
            }

            Spacer(Modifier.height(2.dp))

            val rowMonthLabels: List<String?> = run {
                val labels = mutableListOf<String?>()
                var lastMonth = -1
                for (week in 0 until totalRows) {
                    val firstDay = gridStart.plusDays((week * 7).toLong())
                    val m = firstDay.monthValue
                    if (m != lastMonth) {
                        labels += firstDay.month.getDisplayName(
                            java.time.format.TextStyle.SHORT, strings.locale
                        )
                        lastMonth = m
                    } else {
                        labels += null
                    }
                }
                labels
            }

            for (week in 0 until totalRows) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (dow in 0 until 7) {
                        val date     = gridStart.plusDays((week * 7 + dow).toLong())
                        val epochDay = date.toEpochDay()

                        val isFuture         = date.isAfter(today)
                        val isBeforeCreation = epochDay < habit.createdAtDay
                        val isScheduled      = !isFuture && !isBeforeCreation && habit.isScheduledFor(epochDay)
                        val isDone           = epochDay in doneSet

                        val dimAlpha  = if ((date.year * 12 + date.monthValue) % 2 == 0) 0.05f else 0.12f
                        val cellColor = when {
                            isScheduled && isDone  -> doneColor
                            isScheduled && !isDone -> missColor
                            else                   -> colors.onSurface.copy(alpha = dimAlpha)
                        }

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(cellColor, RoundedCornerShape(5.dp))
                        )
                        if (dow < 6) Spacer(Modifier.width(3.dp))
                    }

                    Box(
                        modifier = Modifier.layout { measurable, constraints ->
                            val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = 200))
                            layout(0, p.height) { p.place(8.dp.roundToPx(), 0) }
                        }
                    ) {
                        rowMonthLabels[week]?.let { label ->
                            Text(label, fontSize = 9.sp, color = colors.onSurfaceSecondary)
                        }
                    }
                }
                if (week < totalRows - 1) Spacer(Modifier.height(3.dp))
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(doneColor, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = strings.taskDone,
                    fontSize = 10.sp,
                    color    = colors.onSurfaceSecondary
                )
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(10.dp).background(missColor, RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(5.dp))
                Text(
                    text     = strings.taskMissed,
                    fontSize = 10.sp,
                    color    = colors.onSurfaceSecondary
                )
            }
        }
    }
}
