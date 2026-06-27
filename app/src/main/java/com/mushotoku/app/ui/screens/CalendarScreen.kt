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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Task
import com.mushotoku.app.holidays.Holiday
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CalendarScreen(
    currentMonth: YearMonth,
    appointments: ImmutableList<Task>,
    onMonthChange: (YearMonth) -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onAddAppointment: (title: String, time: String, date: LocalDate) -> Unit,
    onClose: () -> Unit,
    holidayOn: (LocalDate) -> Holiday? = { null },
    holidayName: (String) -> String = { it },
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    val accent  = colors.accent
    val today   = LocalDate.now()
    val appointmentDays = remember(appointments) { appointments.map { it.date }.toSet() }
    var selectedDate    by remember(currentMonth) { mutableStateOf<LocalDate?>(null) }
    var showAddDialog   by remember { mutableStateOf(false) }
    val selectedAppointments = remember(selectedDate, appointments) {
        selectedDate?.let { d -> appointments.filter { it.date == d.toEpochDay() }.sortedWith(compareBy { it.time }) }
            ?: emptyList()
    }

    val firstDay    = currentMonth.atDay(1)
    val offset      = firstDay.dayOfWeek.value - 1
    val daysInMonth = currentMonth.lengthOfMonth()
    val weeks       = (offset + daysInMonth + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.topBar)
                .statusBarsPadding()
                .padding(end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = soundClick(onClose)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
            }
            Text(strings.calendarTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, modifier = Modifier.weight(1f))
            IconButton(onClick = soundClick { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = soundClick { onMonthChange(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = colors.onSurface)
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", strings.locale)).replaceFirstChar { it.uppercase() },
                fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.onSurface,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            IconButton(onClick = soundClick { onMonthChange(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.onSurface)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 6.dp)) {
            strings.weekDays.forEach { label ->
                Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.onSurfaceTertiary)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            repeat(weeks) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val dayNum = week * 7 + col - offset + 1
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..daysInMonth) {
                                val date       = currentMonth.atDay(dayNum)
                                val isToday    = date == today
                                val isSelected = date == selectedDate
                                val hasAppts   = appointmentDays.contains(date.toEpochDay())
                                val isHoliday  = holidayOn(date) != null

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { selectedDate = if (selectedDate == date) null else date }
                                        .padding(vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                when {
                                                    isSelected -> accent
                                                    isToday    -> accent.copy(alpha = 0.12f)
                                                    else       -> Color.Transparent
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 15.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                isToday    -> accent
                                                else       -> colors.onSurface
                                            }
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (hasAppts) {
                                                Box(Modifier.size(5.dp).background(if (isSelected) Color.White else accent, CircleShape))
                                            }
                                            if (isHoliday) {
                                                Box(Modifier.size(5.dp).border(1.2.dp, if (isSelected) Color.White else accent, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedDate != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = soundClick { onNavigateToDate(selectedDate!!) }) {
                    Text(strings.goToDate, color = accent)
                }
            }
        }

        val selectedHoliday = selectedDate?.let { holidayOn(it) }
        if (selectedHoliday != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).border(1.5.dp, accent, CircleShape))
                Spacer(Modifier.width(14.dp))
                Text(
                    text = holidayName(selectedHoliday.nameKey),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
            }
        }

        if (selectedDate != null && selectedAppointments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colors.divider)
            val navigateDate = selectedDate!!
            val isPast        = navigateDate.isBefore(today)
            val isPastOrToday = !navigateDate.isAfter(today)
            val doneExpressions = remember(selectedAppointments) {
                val exprs = strings.appointmentDoneExpressions
                selectedAppointments.associate { it.id to exprs[kotlin.math.abs(it.id.hashCode()) % exprs.size] }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                selectedAppointments.forEach { task ->
                    val dimAlpha  = if (isPast) 0.4f else 1f
                    val showDoneExpr = task.isDone && isPastOrToday
                    val statusText = when {
                        showDoneExpr        -> doneExpressions[task.id] ?: strings.appointmentDoneExpressions[kotlin.math.abs(task.id.hashCode()) % strings.appointmentDoneExpressions.size]
                        task.time.isEmpty() -> strings.allDay
                        else                -> task.time
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onNavigateToDate(navigateDate) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(7.dp).background(accent.copy(alpha = dimAlpha), CircleShape))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = task.title,
                            fontSize = 15.sp,
                            color = colors.onSurface.copy(alpha = dimAlpha),
                            textDecoration = if (isPast) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = colors.onSurfaceSecondary.copy(alpha = dimAlpha),
                            fontWeight = if (showDoneExpr) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.onSurfaceTertiary.copy(alpha = dimAlpha), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCalendarAppointmentDialog(
            initialDate = selectedDate ?: today,
            onConfirm   = { title, time, date -> onAddAppointment(title, time, date) },
            onDismiss   = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddCalendarAppointmentDialog(
    initialDate: LocalDate,
    onConfirm: (title: String, time: String, date: LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val strings        = LocalAppStrings.current
    val context        = LocalContext.current
    val dateFmt        = DateTimeFormatter.ofPattern("d. MMMM yyyy", strings.locale)
    var title          by remember { mutableStateOf("") }
    var selectedDate   by remember { mutableStateOf(initialDate) }
    var selectedTime   by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val cal = remember { Calendar.getInstance() }
    val datePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day -> selectedDate = LocalDate.of(year, month + 1, day) },
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
        )
    }
    val timePicker = remember {
        TimePickerDialog(
            context,
            { _, h, m -> selectedTime = "%02d:%02d".format(h, m) },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        )
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dialogNewAppointment) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(strings.placeholderAppointment) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedButton(
                    onClick = soundClick { datePicker.updateDate(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth); datePicker.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate.format(dateFmt))
                }
                OutlinedButton(
                    onClick = soundClick { timePicker.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedTime.isEmpty()) strings.setTimeBtn else selectedTime)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick { if (title.isNotBlank()) { onConfirm(title, selectedTime, selectedDate); onDismiss() } },
                enabled = title.isNotBlank()
            ) { Text(strings.add) }
        },
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}
