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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.data.Task
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal val NoteAccent = Color(0xFF3D5AFE)

data class NoteEditorBarState(
    val title: String,
    val noteType: NoteType,
    val isEditing: Boolean,
    val onBack: () -> Unit,
    val onToggle: () -> Unit
)

@Composable
fun NotesScreen(
    notes: ImmutableList<Note>,
    contentPadding: PaddingValues,
    typeFilter: NoteType?,
    defaultNoteType: NoteType = NoteType.NOTE,
    createRequested: Boolean,
    onCreateConsumed: () -> Unit,
    onCreateNote: (String, String, NoteType, (Note) -> Unit) -> Unit,
    onUpdateNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onPinNote: (Note) -> Unit,
    confirmDeleteEnabled: Boolean = true,
    hapticEnabled: Boolean = true,
    onEditorActiveChange: (Boolean) -> Unit = {},
    onEditorBarState: ((NoteEditorBarState) -> Unit)? = null,
    selectedNoteIds: Set<Long> = emptySet(),
    onSelectionChange: (Set<Long>) -> Unit = {},
    linkedNoteToTaskMap: Map<Long, Task> = emptyMap(),
    onNavigateToTask: (Task) -> Unit = {},
    openNoteId: Long? = null,
    onOpenNoteConsumed: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    val isSelectionMode = selectedNoteIds.isNotEmpty()

    var pinnedCollapsed by remember { mutableStateOf(false) }
    var editingNote     by remember { mutableStateOf<Note?>(null) }
    var creatingNote    by remember { mutableStateOf(false) }

    val isEditorActive = editingNote != null || creatingNote
    LaunchedEffect(isEditorActive) { onEditorActiveChange(isEditorActive) }

    LaunchedEffect(createRequested) {
        if (createRequested) { creatingNote = true; onCreateConsumed() }
    }

    LaunchedEffect(openNoteId, notes) {
        if (openNoteId != null) {
            val target = notes.firstOrNull { it.id == openNoteId }
            if (target != null) {
                editingNote = target
                onOpenNoteConsumed()
            }
        }
    }

    val focusManager = LocalFocusManager.current
    var searchQuery   by remember { mutableStateOf("") }

    val (pinned, unpinned) = remember(notes, typeFilter, searchQuery) {
        val list = if (typeFilter == null) notes else notes.filter { it.type == typeFilter }
        val filtered = if (searchQuery.isBlank()) list
        else list.filter { note ->
            note.title.contains(searchQuery, ignoreCase = true) ||
            note.content.contains(searchQuery, ignoreCase = true)
        }
        if (searchQuery.isNotBlank()) {
            persistentListOf<Note>() to filtered.sortedByDescending { it.updatedAt }.toImmutableList()
        } else {
            val p  = filtered.filter { it.isPinned }.sortedByDescending { it.updatedAt }.toImmutableList()
            val up = filtered.filter { !it.isPinned }.sortedByDescending { it.updatedAt }.toImmutableList()
            p to up
        }
    }
    val total = pinned.size + unpinned.size

    if (editingNote != null || creatingNote) {
        val currentLinkedTask = editingNote?.id?.let { linkedNoteToTaskMap[it] }
        NoteEditor(
            note        = editingNote,
            defaultType = defaultNoteType,
            onCreate    = onCreateNote,
            onUpdate    = onUpdateNote,
            onDelete    = onDeleteNote,
            onClose     = { editingNote = null; creatingNote = false },
            bottomPad   = contentPadding.calculateBottomPadding(),
            topPad      = contentPadding.calculateTopPadding(),
            onBarState  = onEditorBarState,
            linkedTask  = currentLinkedTask,
            onNavigateToTask = { task ->
                editingNote = null
                creatingNote = false
                onNavigateToTask(task)
            },
            hapticEnabled = hapticEnabled
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(isSelectionMode) {
                detectTapGestures {
                    if (isSelectionMode) onSelectionChange(emptySet())
                    else focusManager.clearFocus()
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top   = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            item(key = "search") {
                NoteSearchBar(
                    query         = searchQuery,
                    hint          = strings.notesSearchHint,
                    onQueryChange = { searchQuery = it }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (pinned.isNotEmpty()) {
                item(key = "ph") {
                    SectionHeader(
                        label     = strings.notesPinnedSection,
                        collapsed = pinnedCollapsed,
                        accent    = NoteAccent,
                        onClick   = { pinnedCollapsed = !pinnedCollapsed }
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (!pinnedCollapsed) {
                    item(key = "pg") {
                        NoteGroupCard(
                            notes = pinned,
                            selectedNoteIds = selectedNoteIds,
                            linkedNoteIds = linkedNoteToTaskMap.keys,
                            onTap = { note ->
                                if (isSelectionMode) {
                                    val newIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    onSelectionChange(newIds)
                                } else editingNote = note
                            },
                            onLongPress = { note -> onSelectionChange(selectedNoteIds + note.id) },
                            onPin = onPinNote,
                            onDelete = onDeleteNote,
                            confirmDeleteEnabled = confirmDeleteEnabled
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                } else {
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            if (unpinned.isNotEmpty()) {
                if (pinned.isNotEmpty()) {
                    item(key = "uh") {
                        SectionHeader(label = strings.tabNotes, accent = NoteAccent)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                item(key = "ug") {
                    NoteGroupCard(
                        notes = unpinned,
                        selectedNoteIds = selectedNoteIds,
                        linkedNoteIds = linkedNoteToTaskMap.keys,
                        onTap = { note ->
                            if (isSelectionMode) {
                                val newIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id else selectedNoteIds + note.id
                                onSelectionChange(newIds)
                            } else editingNote = note
                        },
                        onLongPress = { note -> onSelectionChange(selectedNoteIds + note.id) },
                        onPin = onPinNote,
                        onDelete = onDeleteNote,
                        confirmDeleteEnabled = confirmDeleteEnabled
                    )
                }
            }

            if (total == 0) {
                item {
                    Box(
                        Modifier.fillParentMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = if (searchQuery.isNotBlank()) strings.notesNoResults else when (typeFilter) {
                                NoteType.ROUTINE -> strings.noRoutinesYet
                                NoteType.LIST    -> strings.noListsYet
                                else             -> strings.noNotesYet
                            },
                            color = colors.onSurfaceTertiary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    label: String,
    accent: Color,
    collapsed: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text          = label.uppercase(),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = colors.onSurfaceSecondary,
            letterSpacing = 0.8.sp
        )
        if (onClick != null) {
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = if (collapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                contentDescription = null,
                tint     = accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NoteSearchBar(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.Search,
            contentDescription = null,
            tint               = colors.onSurfaceTertiary,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.weight(1f),
            textStyle     = TextStyle(fontSize = 15.sp, color = colors.onSurface),
            cursorBrush   = SolidColor(NoteAccent),
            singleLine    = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(hint, fontSize = 15.sp, color = colors.onSurfaceTertiary)
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Default.Clear,
                contentDescription = null,
                tint               = colors.onSurfaceTertiary,
                modifier           = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}
