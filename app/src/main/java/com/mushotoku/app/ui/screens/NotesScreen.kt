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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
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
    /** True while the editor covers this screen; the list then freezes its insets. */
    editorActive: Boolean = false,
    onOpenNote: (Note) -> Unit = {},
    onDeleteTag: (tag: String, withNotes: Boolean) -> Unit = { _, _ -> },
    selectedNoteIds: Set<Long> = emptySet(),
    onSelectionChange: (Set<Long>) -> Unit = {},
    linkedNoteToTaskMap: Map<Long, Task> = emptyMap()
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    val isSelectionMode = selectedNoteIds.isNotEmpty()

    var pinnedCollapsed by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current
    var searchQuery   by remember { mutableStateOf("") }
    val searchFocus   = rememberSearchFocus()
    var selectedTag   by remember { mutableStateOf<String?>(null) }
    var quickFilter   by remember { mutableStateOf<NoteQuickFilter?>(null) }
    var tagToDelete   by remember { mutableStateOf<String?>(null) }

    // The bar offers the tags of the notes currently in view, so it never
    // suggests a tag that would lead to an empty screen.
    val tags = remember(notes) { collectTags(notes) }
    LaunchedEffect(tags) { if (selectedTag != null && selectedTag !in tags) selectedTag = null }

    // A chip only shows while there is something for it to find.
    val quickAvailable = remember(notes) {
        buildSet {
            if (notes.any { noteHasStamp(it) }) add(NoteQuickFilter.STAMP)
            if (notes.any { displayedNoteType(it) == NoteType.LIST }) add(NoteQuickFilter.LIST)
        }
    }
    LaunchedEffect(quickAvailable) { if (quickFilter !in quickAvailable) quickFilter = null }

    val (pinned, unpinned) = remember(notes, searchQuery, selectedTag, quickFilter) {
        val byTag = selectedTag?.let { tag -> notes.filter { noteHasTag(it, tag) } } ?: notes
        val list  = when (quickFilter) {
            NoteQuickFilter.STAMP -> byTag.filter { noteHasStamp(it) }
            NoteQuickFilter.LIST  -> byTag.filter { displayedNoteType(it) == NoteType.LIST }
            null                  -> byTag
        }
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

    // The insets the list was laid out with before the editor took over. While a
    // note is open the top bar is the editor's and reports different insets;
    // re-measuring the list to those would move it under the reader's feet.
    var listPadding by remember { mutableStateOf(contentPadding) }
    LaunchedEffect(contentPadding, editorActive) {
        if (!editorActive) listPadding = contentPadding
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
            } else onOpenNote(note)
        }
        LazyVerticalGrid(
            columns  = GridCells.Fixed(2),
            state    = gridState,
            modifier = Modifier.fillMaxSize().endSearchOnOutsideTap(searchFocus),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top   = listPadding.calculateTopPadding() + 16.dp,
                bottom = listPadding.calculateBottomPadding() + 16.dp
            ),
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
                        // Tapping the active tag again is the way back to all
                        // notes. Only ever one filter at a time, so the bar
                        // always says plainly what is on screen.
                        onSelect   = { tag ->
                            selectedTag = if (selectedTag == tag) null else tag
                            quickFilter = null
                        },
                        onLongPress = { tag -> tagToDelete = tag },
                        quick          = quickFilter,
                        quickAvailable = quickAvailable,
                        onQuick        = { filter ->
                            quickFilter = if (quickFilter == filter) null else filter
                            selectedTag = null
                        }
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
                            text  = if (searchQuery.isNotBlank() || selectedTag != null || quickFilter != null)
                                        strings.notesNoResults
                                    else strings.noNotesYet,
                            color = colors.onSurfaceTertiary,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }


    tagToDelete?.let { tag ->
        val affected = notes.count { noteHasTag(it, tag) }
        GlassAlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("#" + tag) },
            text  = { Text(strings.tagDeleteText(tag)) },
            confirmButton = {
                TextButton(onClick = soundClick { onDeleteTag(tag, false); tagToDelete = null }) {
                    Text(strings.tagDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { onDeleteTag(tag, true); tagToDelete = null }) {
                    Text(strings.tagDeleteWithNotes, color = Color(0xFFD32F2F))
                }
            }
        )
    }
    }
}

/**
 * What the icon chips in front of the tags stand for: the two ways of finding
 * a note that need no tag written into it.
 */
internal enum class NoteQuickFilter { STAMP, LIST }

/**
 * Tags found in the notes themselves, and in front of them the quick filters.
 * Deliberately no folders — a note says itself what it is.
 *
 * Only ever one of them holds at a time, tags included: two filters at once
 * would leave the bar unable to say what is on screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteTagBar(
    tags: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onLongPress: (String) -> Unit,
    quick: NoteQuickFilter? = null,
    quickAvailable: Set<NoteQuickFilter> = emptySet(),
    onQuick: (NoteQuickFilter) -> Unit = {}
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoteQuickFilter.entries.filter { it in quickAvailable }.forEach { filter ->
            val isSelected = filter == quick
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) NoteAccent else colors.surface)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(1.dp, colors.divider, RoundedCornerShape(50))
                    )
                    .clickable { onQuick(filter) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                // A blank label in the tags' own size makes these chips exactly
                // as tall as they are, whatever the icon measures.
                Text(" ", fontSize = 13.sp, maxLines = 1)
                Icon(
                    imageVector = when (filter) {
                        NoteQuickFilter.STAMP -> Icons.Default.Schedule
                        NoteQuickFilter.LIST  -> Icons.AutoMirrored.Filled.List
                    },
                    contentDescription = when (filter) {
                        NoteQuickFilter.STAMP -> strings.notesFilterTimestamp
                        NoteQuickFilter.LIST  -> strings.notesFilterLists
                    },
                    tint     = if (isSelected) Color.White else colors.onSurfaceSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
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
                    .combinedClickable(
                        onClick     = { onSelect(tag) },
                        onLongClick = { onLongPress(tag) }
                    )
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

/**
 * Where the search bar sits and whether it is being typed in — so a tap that
 * lands anywhere else can end the search.
 */
@Stable
internal class SearchFocus {
    var bounds  by mutableStateOf(Rect.Zero)
    var focused by mutableStateOf(false)
}

@Composable
internal fun rememberSearchFocus(): SearchFocus = remember { SearchFocus() }

/**
 * Ends the search on a press outside the bar. The press is only watched, never
 * consumed, so the note under the finger still gets its tap.
 */
internal fun Modifier.endSearchOnOutsideTap(search: SearchFocus): Modifier = composed {
    val focusManager = LocalFocusManager.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(search) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && search.focused) {
                        val point = event.changes.first().position + origin
                        if (!search.bounds.contains(point)) focusManager.clearFocus()
                    }
                }
            }
        }
}

@Composable
internal fun NoteSearchBar(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit,
    focus: SearchFocus? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { focus?.bounds = it.boundsInRoot() }
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
            modifier      = Modifier
                .weight(1f)
                .onFocusChanged { focus?.focused = it.isFocused },
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
