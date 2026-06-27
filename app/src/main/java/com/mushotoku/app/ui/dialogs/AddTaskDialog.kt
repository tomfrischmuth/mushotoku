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

package com.mushotoku.app.ui.dialogs
import com.mushotoku.app.ui.components.soundClick

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.mushotoku.app.data.Note
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.data.Recurrence
import com.mushotoku.app.ui.components.GlassAlertDialog
import com.mushotoku.app.ui.strings.LocalAppStrings
import java.util.Calendar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private enum class NewItemType { TASK, APPOINTMENT, HABIT }

internal val LinkAccent = Color(0xFF3D5AFE)

@Composable
fun AddTaskDialog(
    notes: ImmutableList<Note> = persistentListOf(),
    linkedNoteIds: Set<Long> = emptySet(),
    onConfirm: (title: String, isAppointment: Boolean, time: String, linkedNoteId: Long?, newNoteTitle: String?) -> Unit,
    onAddHabit: (name: String, recurrence: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings        = LocalAppStrings.current
    val colors         = LocalAppColors.current
    var text          by remember { mutableStateOf("") }
    var itemType      by remember { mutableStateOf(NewItemType.TASK) }
    var selectedTime  by remember { mutableStateOf("") }
    var selectedRecurrence by remember { mutableStateOf(Recurrence.DAILY) }
    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
    val focusRequester  = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context         = LocalContext.current

    var linkedNoteId          by remember { mutableStateOf<Long?>(null) }
    var linkedNoteDisplayTitle by remember { mutableStateOf("") }
    var showNotePicker        by remember { mutableStateOf(false) }
    var showNewNoteField      by remember { mutableStateOf(false) }
    var newNoteTitle          by remember { mutableStateOf("") }

    val submit = {
        if (text.isNotBlank()) {
            when (itemType) {
                NewItemType.HABIT -> onAddHabit(text, selectedRecurrence)
                else -> onConfirm(
                    text,
                    itemType == NewItemType.APPOINTMENT,
                    selectedTime,
                    linkedNoteId,
                    if (showNewNoteField && newNoteTitle.isNotBlank()) newNoteTitle else null
                )
            }
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val cal = remember { Calendar.getInstance() }
    val timePicker = remember {
        TimePickerDialog(
            context,
            { _, h, m -> selectedTime = "%02d:%02d".format(h, m) },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )
    }

    if (showNotePicker) {
        NoteLinkPickerDialog(
            notes         = notes,
            linkedNoteIds = linkedNoteIds,
            onPick        = { id, title ->
                linkedNoteId = id
                linkedNoteDisplayTitle = title
                showNotePicker = false
                showNewNoteField = false
                newNoteTitle = ""
            },
            onDismiss     = { showNotePicker = false }
        )
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(when (itemType) {
                NewItemType.APPOINTMENT -> strings.dialogNewAppointment
                NewItemType.HABIT       -> strings.dialogNewHabit
                else                    -> strings.dialogNewTask
            })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accent,
                    selectedLabelColor     = Color.White,
                    containerColor         = colors.surfaceVariant,
                    labelColor             = colors.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = itemType == NewItemType.TASK,
                        onClick  = soundClick { itemType = NewItemType.TASK; selectedTime = ""; linkedNoteId = null; showNewNoteField = false; newNoteTitle = "" },
                        label    = { Text(strings.chipTask) },
                        colors   = chipColors
                    )
                    FilterChip(
                        selected = itemType == NewItemType.APPOINTMENT,
                        onClick  = soundClick { itemType = NewItemType.APPOINTMENT },
                        label    = { Text(strings.chipAppointment) },
                        colors   = chipColors
                    )
                    FilterChip(
                        selected = itemType == NewItemType.HABIT,
                        onClick  = soundClick { itemType = NewItemType.HABIT; selectedTime = ""; linkedNoteId = null; showNewNoteField = false; newNoteTitle = "" },
                        label    = { Text(strings.addHabit) },
                        colors   = chipColors
                    )
                }

                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    placeholder   = {
                        Text(when (itemType) {
                            NewItemType.APPOINTMENT -> strings.placeholderAppointment
                            NewItemType.HABIT       -> strings.habitNameHint
                            else                    -> strings.placeholderTask
                        })
                    },
                    modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine      = true,
                    shape           = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = if (itemType == NewItemType.TASK) ImeAction.Done else ImeAction.Next),
                    keyboardActions = KeyboardActions(onDone = { if (itemType == NewItemType.TASK) submit() })
                )

                if (itemType == NewItemType.APPOINTMENT) {
                    OutlinedButton(
                        onClick  = soundClick { timePicker.show() },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(50.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedTime.isEmpty()) strings.setTimeBtn else selectedTime)
                    }

                    AppointmentNoteLinkSection(
                        notes                  = notes,
                        linkedNoteId           = linkedNoteId,
                        linkedNoteDisplayTitle = linkedNoteDisplayTitle,
                        showNewNoteField       = showNewNoteField,
                        newNoteTitle           = newNoteTitle,
                        onShowPicker           = { showNotePicker = true },
                        onClearLinked          = { linkedNoteId = null; linkedNoteDisplayTitle = "" },
                        onStartNewNote         = { showNewNoteField = true; newNoteTitle = text },
                        onNewNoteTitleChange   = { newNoteTitle = it },
                        onCancelNewNote        = { showNewNoteField = false; newNoteTitle = "" }
                    )
                }

                if (itemType == NewItemType.HABIT) {
                    Box {
                        OutlinedButton(
                            onClick  = soundClick { recurrenceMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(50.dp)
                        ) {
                            Icon(Icons.Default.Repeat, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.recurrenceName(selectedRecurrence))
                        }
                        DropdownMenu(
                            expanded        = recurrenceMenuExpanded,
                            onDismissRequest = { recurrenceMenuExpanded = false }
                        ) {
                            Recurrence.ALL.forEach { rec ->
                                DropdownMenuItem(
                                    text    = { Text(strings.recurrenceName(rec)) },
                                    onClick = soundClick { selectedRecurrence = rec; recurrenceMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = soundClick { submit() },
                enabled = text.isNotBlank()
            ) { Text(strings.add) }
        },
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}
