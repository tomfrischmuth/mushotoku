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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.ui.components.soundClick
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(16.dp)

/** Strips the markdown so a line can be shown as plain preview text. */
private fun stripMarkdown(line: String): String =
    when {
        line.startsWith("### ") -> line.substring(4)
        line.startsWith("## ")  -> line.substring(3)
        line.startsWith("# ")   -> line.substring(2)
        line.startsWith("> ")   -> line.substring(2)
        checkStateOf(line) != null -> line.substring(ChecklistPrefixLength)
        line.startsWith("- ")   -> line.substring(2)
        line.startsWith("* ")   -> line.substring(2)
        line == "---" || line == "***" || line == "___" -> ""
        else -> line
    }
        .replace(Regex("""\*\*\*(.*?)\*\*\*"""), "$1")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""\*(.*?)\*"""), "$1")
        .replace(Regex("""`(.*?)`"""), "$1")
        // An escaped hash reads as a plain "#" everywhere else, here too.
        .replace("$TagEscape#", "#")
        .trim()

/**
 * First line worth showing. A line holding nothing but tags is skipped: it
 * would fill the preview without saying anything about the note.
 */
private fun contentPreview(note: Note): String =
    note.content.lines()
        .map { stripMarkdown(it) }
        .firstOrNull { it.isNotBlank() && !isTagOnly(it) }
        ?: ""

private fun formatStamp(ms: Long): String {
    if (ms <= 0L) return ""
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return if (dt.toLocalDate() == LocalDate.now())
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    else
        dt.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
}

/**
 * One note as a card. [previewLines] is fixed rather than a maximum so every
 * card in a grid row ends up the same height.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteCard(
    note: Note,
    isSelected: Boolean,
    isLinked: Boolean,
    previewLines: Int,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val colors  = LocalAppColors.current
    val preview = remember(note.content, note.title) { contentPreview(note) }
    val strings = LocalAppStrings.current
    val title   = remember(note.title) { note.title.stripHeadingMarker() }
    val openState = remember(note.title, note.content) {
        openCheckState(note.title + "\n" + note.content)
    }
    val tinted = noteCardColor(note.color, colors.surface, colors.background.isDarkSurface())
    val footnote = noteCardFootnoteColor(tinted)
    val background by animateColorAsState(
        targetValue = if (isSelected) NoteAccent.copy(alpha = 0.12f) else tinted,
        label = "noteCardBackground"
    )

    Card(
        modifier = modifier
            .fillMaxHeight()
            .then(if (isSelected) Modifier.border(1.5.dp, NoteAccent, CardShape) else Modifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication  = ripple(),
                onClick     = soundClick(onTap),
                onLongClick = onLongPress
            ),
        shape     = CardShape,
        colors    = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = NoteAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text       = title.ifBlank { strings.notesNoTitle },
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (title.isBlank()) colors.onSurfaceTertiary else colors.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    // Fills the row so the dot is pushed to the right edge.
                    modifier   = Modifier.weight(1f)
                )
                // What is still open in this note's checklist, in the colour of
                // the least finished item.
                if (openState != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(openState.color)
                    )
                }
            }

            // Rendered even when empty: the reserved lines are what keeps both
            // cards of a grid row the same height.
            Spacer(Modifier.height(4.dp))
            Text(
                text     = preview,
                fontSize = 13.sp,
                color    = colors.onSurfaceSecondary,
                minLines = previewLines,
                maxLines = previewLines,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (displayedNoteType(note) == NoteType.LIST)
                            Icons.AutoMirrored.Filled.List else Icons.Default.Description,
                        contentDescription = null,
                        tint = footnote,
                        modifier = Modifier.size(15.dp)
                    )
                    if (isLinked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = NoteAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (isSelected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = NoteAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text     = formatStamp(if (note.updatedAt > 0) note.updatedAt else note.createdAt),
                    fontSize = 11.sp,
                    color    = footnote
                )
            }
        }
    }
}
