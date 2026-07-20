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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
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
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = rememberSearchFocus()
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var quickFilter by remember { mutableStateOf<NoteQuickFilter?>(null) }
    var tagAction   by remember { mutableStateOf<String?>(null) }
    var confirm     by remember { mutableStateOf<TrashConfirm?>(null) }
    val selectionMode = selectedIds.isNotEmpty()
    val selected = notes.filter { it.id in selectedIds }

    val tags = remember(notes) { collectTags(notes) }
    val quickAvailable = remember(notes) {
        buildSet {
            if (notes.any { noteHasStamp(it) }) add(NoteQuickFilter.STAMP)
            if (notes.any { displayedNoteType(it) == NoteType.LIST }) add(NoteQuickFilter.LIST)
        }
    }
    val shown = remember(notes, searchQuery, selectedTag, quickFilter) {
        val byTag  = selectedTag?.let { tag -> notes.filter { noteHasTag(it, tag) } } ?: notes
        val byKind = when (quickFilter) {
            NoteQuickFilter.STAMP -> byTag.filter { noteHasStamp(it) }
            NoteQuickFilter.LIST  -> byTag.filter { displayedNoteType(it) == NoteType.LIST }
            null                  -> byTag
        }
        if (searchQuery.isBlank()) byKind
        else byKind.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    // Nothing to select or filter by once the bin empties under the screen.
    LaunchedEffect(notes) { selectedIds = selectedIds.filter { id -> notes.any { it.id == id } }.toSet() }
    LaunchedEffect(tags)  { if (selectedTag != null && selectedTag !in tags) selectedTag = null }
    LaunchedEffect(quickAvailable) { if (quickFilter !in quickAvailable) quickFilter = null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            // Keeps taps from reaching the notes lying behind this screen, but
            // only in the final pass: a tap detector here would race the chips
            // and cards for the same tap and swallow theirs.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Final).changes.forEach { it.consume() }
                    }
                }
            }
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
            IconButton(onClick = soundClick { if (selectionMode) selectedIds = emptySet() else onClose() }) {
                Icon(
                    if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = colors.onSurface
                )
            }
            Text(
                if (selectionMode) strings.notesSelected(selectedIds.size) else strings.trash,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selectionMode) {
                IconButton(onClick = soundClick {
                    confirm = TrashConfirm(selected, restore = true)
                }) {
                    Icon(Icons.Default.RestoreFromTrash, contentDescription = strings.trashRestore, tint = AppBlue)
                }
                IconButton(onClick = soundClick {
                    confirm = TrashConfirm(selected, restore = false)
                }) {
                    Icon(Icons.Default.DeleteForever, contentDescription = strings.delete, tint = DeleteRed)
                }
            } else if (notes.isNotEmpty()) {
                TextButton(onClick = soundClick { selectedIds = notes.map { it.id }.toSet() }) {
                    Text(strings.selectAll, color = AppBlue, fontWeight = FontWeight.SemiBold)
                }
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
            LazyVerticalGrid(
                columns  = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().endSearchOnOutsideTap(searchFocus),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                    NoteSearchBar(
                        query         = searchQuery,
                        hint          = strings.notesSearchHint,
                        onQueryChange = { searchQuery = it },
                        focus         = searchFocus
                    )
                }
                if (tags.isNotEmpty() || quickAvailable.isNotEmpty()) {
                    item(key = "tags", span = { GridItemSpan(maxLineSpan) }) {
                        NoteTagBar(
                            tags       = tags,
                            selected   = selectedTag,
                            onSelect   = { tag ->
                                selectedTag = if (selectedTag == tag) null else tag
                                quickFilter = null
                            },
                            onLongPress = { tag -> tagAction = tag },
                            quick          = quickFilter,
                            quickAvailable = quickAvailable,
                            onQuick        = { filter ->
                                quickFilter = if (quickFilter == filter) null else filter
                                selectedTag = null
                            }
                        )
                    }
                }
                if (shown.isEmpty()) {
                    item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.notesNoResults, fontSize = 16.sp, color = colors.onSurfaceSecondary)
                        }
                    }
                }
                items(shown, key = { it.id }) { note ->
                    NoteCard(
                        note         = note,
                        isSelected   = note.id in selectedIds,
                        isLinked     = false,
                        previewLines = 2,
                        // Without a selection a tap does nothing here: a note in
                        // the bin is not open for reading, only for restoring.
                        onTap        = {
                            if (selectionMode) {
                                selectedIds = if (note.id in selectedIds) selectedIds - note.id
                                              else selectedIds + note.id
                            }
                        },
                        onLongPress  = { selectedIds = selectedIds + note.id }
                    )
                }
            }
        }
    }

    // Long press on a tag acts on every note in the bin carrying it — the way
    // to empty or rescue a whole topic without picking the notes one by one.
    tagAction?.let { tag ->
        val affected = notes.filter { noteHasTag(it, tag) }
        GlassAlertDialog(
            onDismissRequest = { tagAction = null },
            title = { Text("#$tag") },
            text  = { Text(strings.trashTagText(affected.size)) },
            confirmButton = {
                TextButton(onClick = soundClick {
                    tagAction = null; confirm = TrashConfirm(affected, restore = true)
                }) {
                    Text(strings.trashRestoreAll, color = AppBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick {
                    tagAction = null; confirm = TrashConfirm(affected, restore = false)
                }) {
                    Text(strings.delete, color = DeleteRed)
                }
            }
        )
    }

    // Restoring and deleting always ask first, however they were triggered.
    confirm?.let { action ->
        GlassAlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (action.restore) strings.trashRestoreConfirmTitle else strings.trashDeleteConfirmTitle) },
            text  = {
                Text(
                    if (action.restore) strings.trashRestoreConfirmText(action.notes.size)
                    else strings.trashDeleteConfirmText(action.notes.size)
                )
            },
            confirmButton = {
                TextButton(onClick = soundClick {
                    action.notes.forEach(if (action.restore) onRestore else onPermanentDelete)
                    selectedIds = emptySet()
                    confirm = null
                }) {
                    Text(
                        if (action.restore) strings.trashRestore else strings.delete,
                        color = if (action.restore) AppBlue else DeleteRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { confirm = null }) { Text(strings.cancel) }
            }
        )
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

/** A pending restore or permanent delete, waiting for its confirmation. */
private data class TrashConfirm(val notes: List<Note>, val restore: Boolean)
