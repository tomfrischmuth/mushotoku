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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.ui.components.SwipeAction
import com.mushotoku.app.ui.components.SwipeToReveal
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList

private val AppBlue = Color(0xFF3D5AFE)
private val DeleteRed = Color(0xFFD32F2F)

@Composable
fun TrashScreen(
    notes: ImmutableList<Note>,
    onRestore: (Note) -> Unit,
    onPermanentDelete: (Note) -> Unit,
    onDeleteAll: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) { detectTapGestures { } }
            .navigationBarsPadding()
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
            Text(
                strings.trash,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (notes.isNotEmpty()) {
                TextButton(onClick = soundClick { showDeleteAllDialog = true }) {
                    Text(strings.trashDeleteAll, color = DeleteRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.trashEmpty, fontSize = 16.sp, color = colors.onSurfaceSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                    TrashNoteRow(
                        note = note,
                        onRestore = { onRestore(note) },
                        onPermanentDelete = { onPermanentDelete(note) }
                    )
                    if (index < notes.size - 1) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color     = colors.divider
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(strings.trashDeleteAllDialogTitle) },
            text  = { Text(strings.trashDeleteAllDialogText) },
            confirmButton = {
                TextButton(onClick = soundClick { showDeleteAllDialog = false; onDeleteAll() }) {
                    Text(strings.delete, color = DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { showDeleteAllDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun TrashNoteRow(
    note: Note,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val colors = LocalAppColors.current
    var resetTrigger by remember { mutableIntStateOf(0) }

    SwipeToReveal(
        startAction  = SwipeAction(Icons.Default.RestoreFromTrash, AppBlue, onAction = onRestore),
        endAction    = SwipeAction(Icons.Default.DeleteForever, DeleteRed, onAction = onPermanentDelete),
        resetTrigger = resetTrigger
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayTitle = note.title
                    .removePrefix("### ")
                    .removePrefix("## ")
                    .removePrefix("# ")
                    .ifBlank { "–" }
                Text(
                    text     = displayTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color    = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = note.content,
                        fontSize = 13.sp,
                        color    = colors.onSurfaceSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
