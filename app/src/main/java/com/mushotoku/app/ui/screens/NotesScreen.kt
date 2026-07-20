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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
    var selectedTag   by remember { mutableStateOf<String?>(null) }

    // The bar offers the tags of the notes currently in view, so it never
    // suggests a tag that would lead to an empty screen.
    val tags = remember(notes, typeFilter) {
        collectTags(notes.filter { matchesTypeFilter(it, typeFilter) })
    }
    LaunchedEffect(tags) { if (selectedTag != null && selectedTag !in tags) selectedTag = null }

    val (pinned, unpinned) = remember(notes, typeFilter, searchQuery, selectedTag) {
        val byType = notes.filter { matchesTypeFilter(it, typeFilter) }
        val list = selectedTag?.let { tag -> byType.filter { noteHasTag(it, tag) } } ?: byType
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
            hapticEnabled = hapticEnabled,
            // Every tag in the app, not just the ones the filter shows.
            knownTags     = remember(notes) { collectTags(notes) }
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
        val onTapNote: (Note) -> Unit = { note ->
            if (isSelectionMode) {
                val newIds = if (note.id in selectedNoteIds) selectedNoteIds - note.id
                             else selectedNoteIds + note.id
                onSelectionChange(newIds)
            } else editingNote = note
        }
        LazyVerticalGrid(
            columns  = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top   = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                NoteSearchBar(
                    query         = searchQuery,
                    hint          = strings.notesSearchHint,
                    onQueryChange = { searchQuery = it }
                )
            }
            if (tags.isNotEmpty()) {
                item(key = "tags", span = { GridItemSpan(maxLineSpan) }) {
                    NoteTagBar(
                        tags     = tags,
                        selected = selectedTag,
                        // Tapping the active tag again is the way back to all notes.
                        onSelect = { tag -> selectedTag = if (selectedTag == tag) null else tag }
                    )
                }
            }

            if (pinned.isNotEmpty()) {
                item(key = "ph", span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        label     = strings.notesPinnedSection,
                        collapsed = pinnedCollapsed,
                        accent    = NoteAccent,
                        onClick   = { pinnedCollapsed = !pinnedCollapsed }
                    )
                }
                if (!pinnedCollapsed) {
                    items(
                        items = pinned,
                        key   = { "p${it.id}" },
                        span  = { GridItemSpan(maxLineSpan) }
                    ) { note ->
                        NoteCard(
                            note         = note,
                            isSelected   = note.id in selectedNoteIds,
                            isLinked     = note.id in linkedNoteToTaskMap.keys,
                            previewLines = 1,
                            onTap        = { onTapNote(note) },
                            onLongPress  = { onSelectionChange(selectedNoteIds + note.id) }
                        )
                    }
                }
            }

            if (unpinned.isNotEmpty()) {
                if (pinned.isNotEmpty()) {
                    item(key = "uh", span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(label = strings.tabNotes, accent = NoteAccent)
                    }
                }
                items(unpinned, key = { it.id }) { note ->
                    NoteCard(
                        note         = note,
                        isSelected   = note.id in selectedNoteIds,
                        isLinked     = note.id in linkedNoteToTaskMap.keys,
                        previewLines = 2,
                        onTap        = { onTapNote(note) },
                        onLongPress  = { onSelectionChange(selectedNoteIds + note.id) }
                    )
                }
            }

            if (total == 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = if (searchQuery.isNotBlank() || selectedTag != null) strings.notesNoResults
                                    else when (typeFilter) {
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

/**
 * Tags found in the notes themselves. Deliberately tags only — the note types
 * keep their own switch in the bottom bar.
 */
@Composable
private fun NoteTagBar(
    tags: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val isSelected = tag == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) NoteAccent else colors.surface)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(1.dp, colors.divider, RoundedCornerShape(50))
                    )
                    // LocalIndication already sounds the tap; soundClick would double it.
                    .clickable { onSelect(tag) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text       = "#$tag",
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color      = if (isSelected) Color.White else colors.onSurfaceSecondary,
                    maxLines   = 1
                )
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
