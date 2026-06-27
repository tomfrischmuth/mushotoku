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
import com.mushotoku.app.ui.components.soundClick
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.ui.components.GlassAlertDialog
import com.mushotoku.app.ui.components.SwipeAction
import com.mushotoku.app.ui.components.SwipeToReveal
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun contentPreview(note: Note): String =
    note.content.lines()
        .firstOrNull { it.isNotBlank() }
        ?.let { line ->
            line
                .let { l ->
                    when {
                        l.startsWith("### ") -> l.substring(4)
                        l.startsWith("## ")  -> l.substring(3)
                        l.startsWith("# ")   -> l.substring(2)
                        l.startsWith("> ")   -> l.substring(2)
                        l.startsWith("- [x] ") || l.startsWith("- [X] ") -> l.substring(6)
                        l.startsWith("- [ ] ") -> l.substring(6)
                        l.startsWith("- ")   -> l.substring(2)
                        l.startsWith("* ")   -> l.substring(2)
                        l == "---" || l == "***" || l == "___" -> ""
                        else -> l
                    }
                }
                .replace(Regex("""\*\*\*(.*?)\*\*\*"""), "$1")
                .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
                .replace(Regex("""\*(.*?)\*"""), "$1")
                .replace(Regex("""`(.*?)`"""), "$1")
                .trim()
        }
        ?.takeIf { it.isNotBlank() }
        ?: ""

private fun formatStamp(ms: Long): String {
    if (ms <= 0L) return ""
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return if (dt.toLocalDate() == LocalDate.now())
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    else
        dt.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
}

@Composable
internal fun NoteGroupCard(
    notes: ImmutableList<Note>,
    selectedNoteIds: Set<Long>,
    linkedNoteIds: Set<Long> = emptySet(),
    onTap: (Note) -> Unit,
    onLongPress: (Note) -> Unit,
    onPin: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    confirmDeleteEnabled: Boolean = true
) {
    val colors = LocalAppColors.current
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column {
            notes.forEachIndexed { i, note ->
                SwipeableNoteRow(
                    note                 = note,
                    isSelectionMode      = selectedNoteIds.isNotEmpty(),
                    isSelected           = note.id in selectedNoteIds,
                    isLinked             = note.id in linkedNoteIds,
                    onTap                = { onTap(note) },
                    onLongPress          = { onLongPress(note) },
                    onPin                = { onPin(note) },
                    onDelete             = { onDelete(note) },
                    confirmDeleteEnabled = confirmDeleteEnabled
                )
                if (i < notes.size - 1) {
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

@Composable
private fun SwipeableNoteRow(
    note: Note,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLinked: Boolean = false,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    confirmDeleteEnabled: Boolean = true
) {
    val strings = LocalAppStrings.current
    var showDeleteDialog   by remember { mutableStateOf(false) }
    var deleteResetTrigger by remember { mutableIntStateOf(0) }
    val prevDialogShown     = remember { mutableStateOf(false) }

    LaunchedEffect(showDeleteDialog) {
        if (prevDialogShown.value && !showDeleteDialog) deleteResetTrigger++
        prevDialogShown.value = showDeleteDialog
    }

    if (isSelectionMode) {
        NoteListRow(
            note            = note,
            isSelectionMode = true,
            isSelected      = isSelected,
            isLinked        = isLinked,
            onClick         = onTap,
            onLongPress     = onLongPress
        )
    } else {
        SwipeToReveal(
            startAction  = SwipeAction(Icons.Default.PushPin, NoteAccent) { onPin() },
            endAction    = SwipeAction(Icons.Default.Delete, Color(0xFFD32F2F)) {
                if (confirmDeleteEnabled) showDeleteDialog = true else onDelete()
            },
            resetTrigger = deleteResetTrigger
        ) {
            NoteListRow(
                note            = note,
                isSelectionMode = false,
                isSelected      = false,
                isLinked        = isLinked,
                onClick         = onTap,
                onLongPress     = onLongPress
            )
        }
    }

    if (showDeleteDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text(strings.deleteNoteDialogTitle) },
            text    = { Text(strings.deleteNoteDialogText(note.title.removePrefix("### ").removePrefix("## ").removePrefix("# "))) },
            confirmButton = {
                TextButton(onClick = soundClick { showDeleteDialog = false; onDelete() }) {
                    Text(strings.delete, color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = soundClick { showDeleteDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun NoteListRow(
    note: Note,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isLinked: Boolean = false,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val colors = LocalAppColors.current
    val selectionBg by animateColorAsState(
        targetValue = if (isSelected) NoteAccent.copy(alpha = 0.10f) else colors.surface,
        animationSpec = tween(150),
        label = "selectionBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionBg)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                // ripple() (no sound) + soundClick on onClick: tap clicks,
                // long-press stays silent on release.
                indication = ripple(),
                onClick = soundClick(onClick),
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter   = expandHorizontally(tween(200)),
            exit    = shrinkHorizontally(tween(200))
        ) {
            Row {
                val circleColor by animateColorAsState(
                    targetValue = if (isSelected) NoteAccent else Color.Transparent,
                    animationSpec = tween(150),
                    label = "circleBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) NoteAccent else colors.onSurfaceTertiary,
                    animationSpec = tween(150),
                    label = "circleBorder"
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(1.5.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = NoteAccent, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text       = note.title.removePrefix("### ").removePrefix("## ").removePrefix("# "),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = when (note.type) {
                        NoteType.ROUTINE -> Icons.Default.Repeat
                        NoteType.LIST    -> Icons.AutoMirrored.Filled.List
                        NoteType.NOTE    -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint     = colors.onSurfaceTertiary,
                    modifier = Modifier.size(17.dp)
                )
                if (isLinked) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector        = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint               = NoteAccent,
                        modifier           = Modifier.size(14.dp)
                    )
                }
            }
            val preview = contentPreview(note)
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = preview,
                    fontSize = 13.sp,
                    color    = colors.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        val ms = if (note.updatedAt > 0) note.updatedAt else note.createdAt
        if (ms > 0) Text(formatStamp(ms), fontSize = 11.sp, color = colors.onSurfaceTertiary)
    }
}
