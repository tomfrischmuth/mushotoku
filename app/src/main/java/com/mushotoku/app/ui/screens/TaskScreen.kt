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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import java.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mushotoku.app.data.Habit
import com.mushotoku.app.data.HabitLog
import com.mushotoku.app.data.Task
import com.mushotoku.app.ui.components.TaskItem
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun TaskScreen(
    tasks: ImmutableList<Task>,
    habits: ImmutableList<Habit>,
    habitCompletions: ImmutableSet<String>,
    habitStreaks: ImmutableMap<String, Int> = persistentMapOf(),
    allHabitLogs: ImmutableList<HabitLog> = persistentListOf(),
    selectedDate: LocalDate = LocalDate.now(),
    holidayNames: List<String> = emptyList(),
    contentPadding: PaddingValues,
    onStatusClick: (Task) -> Unit,
    onTitleSave: (Task, String) -> Unit,
    onMoveToTomorrow: (Task) -> Unit,
    onReschedule: (Task, LocalDate, String) -> Unit,
    onDelete: (Task) -> Unit,
    onReorder: (List<Task>) -> Unit,
    onToggleHabit: (Habit) -> Unit,
    onUpdateHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    confirmDeleteEnabled: Boolean = true,
    onOpenLinkedNote: (Task) -> Unit = {}
) {
    val strings      = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current
    val colors       = LocalAppColors.current

    var rescheduleTask      by remember { mutableStateOf<Task?>(null) }
    var pendingReschedule   by remember { mutableStateOf<Pair<Task, LocalDate>?>(null) }
    var swipeResetTrigger   by remember { mutableIntStateOf(0) }
    var lastRescheduledId   by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(rescheduleTask) {
        if (rescheduleTask == null) swipeResetTrigger++
        else lastRescheduledId = rescheduleTask?.id
    }

    rescheduleTask?.let { task ->
        DisposableEffect(task.id) {
            val d = LocalDate.ofEpochDay(task.date)
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    pendingReschedule = task to LocalDate.of(year, month + 1, day)
                    rescheduleTask = null
                },
                d.year, d.monthValue - 1, d.dayOfMonth
            )
            dialog.setOnDismissListener { rescheduleTask = null }
            dialog.show()
            onDispose { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    pendingReschedule?.let { (task, date) ->
        if (!task.isAppointment) {
            LaunchedEffect(task.id) {
                onReschedule(task, date, task.time)
                pendingReschedule = null
            }
            return@let
        }
        DisposableEffect(task.id) {
            val cal = Calendar.getInstance()
            if (task.time.isNotEmpty()) {
                val parts = task.time.split(":")
                cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                cal.set(Calendar.MINUTE, parts[1].toInt())
            }
            var confirmed = false
            val dialog = TimePickerDialog(
                context,
                { _, hour, minute ->
                    confirmed = true
                    onReschedule(task, date, "%02d:%02d".format(hour, minute))
                    pendingReschedule = null
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            )
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, strings.allDay) { _, _ ->
                confirmed = true
                onReschedule(task, date, "")
                pendingReschedule = null
            }
            dialog.setOnDismissListener {
                if (!confirmed) {
                    onReschedule(task, date, task.time)
                    pendingReschedule = null
                }
            }
            dialog.show()
            onDispose { if (dialog.isShowing) dialog.dismiss() }
        }
    }

    val appointments = remember(tasks) {
        tasks.filter { it.isAppointment }.sortedWith(compareBy { it.time })
    }
    val taskItems    = remember(tasks) {
        tasks.filter { !it.isAppointment }.sortedBy { it.sortOrder }.toMutableStateList()
    }
    var draggingId   by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY  by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
    ) {
        Spacer(Modifier.height(contentPadding.calculateTopPadding() + 12.dp))
        if (appointments.isNotEmpty() || holidayNames.isNotEmpty()) {
            Text(
                text = strings.appointmentsSection.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        holidayNames.forEach { name ->
            HolidayItem(name = name)
        }
        appointments.forEach { task ->
            key(task.id) {
                TaskItem(
                    task                 = task,
                    swipeResetTrigger    = if (task.id == lastRescheduledId) swipeResetTrigger else 0,
                    onStatusClick        = { onStatusClick(task) },
                    onTitleSave          = { onTitleSave(task, it) },
                    onMoveToTomorrow     = { rescheduleTask = task },
                    onDelete             = { onDelete(task) },
                    confirmDeleteEnabled = confirmDeleteEnabled,
                    onOpenLinkedNote     = if (task.linkedNoteId != null) { { onOpenLinkedNote(task) } } else null
                )
            }
        }

        if (taskItems.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.tasksSection.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        taskItems.forEachIndexed { _, task ->
            val isDragging = draggingId == task.id
            key(task.id) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY    = if (isDragging) dragOffsetY else 0f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .onSizeChanged { itemHeightPx = it.height }
                ) {
                    TaskItem(
                        task                 = task,
                        swipeResetTrigger    = if (task.id == lastRescheduledId) swipeResetTrigger else 0,
                        onStatusClick        = { onStatusClick(task) },
                        onTitleSave          = { onTitleSave(task, it) },
                        onMoveToTomorrow     = { onMoveToTomorrow(task) },
                        onPickDate           = { rescheduleTask = task },
                        onDelete             = { onDelete(task) },
                        confirmDeleteEnabled = confirmDeleteEnabled,
                        isDraggable      = taskItems.size > 1,
                        modifier = if (taskItems.size > 1) Modifier.pointerInput(task.id) {
                            detectDragGestures(
                                onDragStart = { draggingId = task.id; dragOffsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    if (itemHeightPx > 0) {
                                        val cur = taskItems.indexOfFirst { it.id == task.id }
                                        if (cur != -1) {
                                            val target = (cur + (dragOffsetY / itemHeightPx).roundToInt())
                                                .coerceIn(0, taskItems.size - 1)
                                            if (target != cur) {
                                                taskItems.add(target, taskItems.removeAt(cur))
                                                dragOffsetY -= (target - cur) * itemHeightPx
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    onReorder(taskItems.toList())
                                    draggingId = null; dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    taskItems.clear()
                                    taskItems.addAll(tasks.filter { !it.isAppointment }.sortedBy { it.sortOrder })
                                    draggingId = null; dragOffsetY = 0f
                                }
                            )
                        } else Modifier
                    )
                }
            }
        }

        if (habits.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.habitsSection.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        habits.forEach { habit ->
            key(habit.id) {
                HabitRow(
                    habit                = habit,
                    isDone               = habit.id in habitCompletions,
                    streak               = habitStreaks[habit.id] ?: 0,
                    habitLogs            = allHabitLogs.filter { it.habitId == habit.id }.toImmutableList(),
                    selectedDate         = selectedDate,
                    onToggle             = { onToggleHabit(habit) },
                    onUpdate             = { onUpdateHabit(it) },
                    onDelete             = { onDeleteHabit(habit) },
                    confirmDeleteEnabled = confirmDeleteEnabled
                )
            }
        }

        Spacer(modifier = Modifier.fillMaxWidth().heightIn(min = contentPadding.calculateBottomPadding() + 32.dp))
    }
}

@Composable
private fun HolidayItem(name: String) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(colors.accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = name,
            fontSize = 16.sp,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = strings.allDay,
            fontSize = 13.sp,
            color = colors.onSurfaceSecondary
        )
    }
}
