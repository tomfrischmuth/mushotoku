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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.ui.components.GlassAlertDialog
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun NoteLinkPickerDialog(
    notes: ImmutableList<Note>,
    linkedNoteIds: Set<Long>,
    onPick: (noteId: Long, displayTitle: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.noteLinkPickerTitle) },
        text = {
            Box(Modifier.heightIn(max = 320.dp)) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    notes.forEach { note ->
                        val noteTitle = note.title
                            .removePrefix("### ").removePrefix("## ").removePrefix("# ")
                        val alreadyLinked = note.id in linkedNoteIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !alreadyLinked) { onPick(note.id, noteTitle) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                tint = if (alreadyLinked) colors.onSurfaceTertiary else LinkAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                noteTitle,
                                fontSize = 15.sp,
                                color = if (alreadyLinked) colors.onSurfaceTertiary else colors.onSurface
                            )
                        }
                        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = soundClick(onDismiss)) { Text(strings.cancel) }
        }
    )
}

@Composable
internal fun ColumnScope.AppointmentNoteLinkSection(
    notes: ImmutableList<Note>,
    linkedNoteId: Long?,
    linkedNoteDisplayTitle: String,
    showNewNoteField: Boolean,
    newNoteTitle: String,
    onShowPicker: () -> Unit,
    onClearLinked: () -> Unit,
    onStartNewNote: () -> Unit,
    onNewNoteTitleChange: (String) -> Unit,
    onCancelNewNote: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
    Text(
        text = strings.noteLinkSection,
        fontSize = 12.sp,
        color = colors.onSurfaceTertiary
    )

    when {
        linkedNoteId != null -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = LinkAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(linkedNoteDisplayTitle, modifier = Modifier.weight(1f), fontSize = 14.sp, color = colors.onSurface)
                IconButton(onClick = soundClick(onClearLinked), modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = colors.onSurfaceTertiary, modifier = Modifier.size(16.dp))
                }
            }
        }
        showNewNoteField -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newNoteTitle,
                    onValueChange = onNewNoteTitleChange,
                    placeholder = { Text(strings.noteLinkTitleHint) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = soundClick(onCancelNewNote), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = colors.onSurfaceTertiary, modifier = Modifier.size(16.dp))
                }
            }
        }
        else -> {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = soundClick { if (notes.isNotEmpty()) onShowPicker() },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(50.dp),
                    enabled  = notes.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.noteLinkExisting, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick  = soundClick(onStartNewNote),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.noteLinkNew, fontSize = 13.sp)
                }
            }
        }
    }
}
