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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.ui.components.GlassAlertDialog
import com.mushotoku.app.ui.strings.LocalAppStrings

@Composable
fun AddNoteDialog(
    onConfirm: (String, String, NoteType) -> Unit,
    onDismiss: () -> Unit
) {
    val strings        = LocalAppStrings.current
    var title         by remember { mutableStateOf("") }
    var content       by remember { mutableStateOf("") }
    var selectedType  by remember { mutableStateOf(NoteType.NOTE) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dialogNewNote) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(strings.placeholderTitle) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(strings.placeholderContent) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick  = soundClick { selectedType = type },
                            label    = { Text(strings.noteTypeName(type)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = soundClick { if (title.isNotBlank()) { onConfirm(title, content, selectedType); onDismiss() } },
                enabled  = title.isNotBlank()
            ) { Text(strings.add) }
        },
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}
