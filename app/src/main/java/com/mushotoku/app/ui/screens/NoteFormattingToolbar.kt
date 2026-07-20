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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors

private val ToolbarAccent = Color(0xFF3D5AFE)

/** Material's minimum touch target; chips draw at least this wide so the gaps stay even. */
private val ChipMinWidth = 48.dp

/** Horizontal padding Material puts around a chip label. */
private val ChipLabelPadding = 16.dp

/** Widening the label instead of the chip keeps short labels centred. */
private val ChipLabelMinWidth = ChipMinWidth - ChipLabelPadding * 2

@Composable
internal fun FormattingToolbar(
    currentLine: String,
    canUndo: Boolean,
    onApplyPrefix: (String) -> Unit,
    onApplyInline: (String) -> Unit,
    onInsertTimestamp: (withDate: Boolean) -> Unit,
    onUndo: () -> Unit
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    val activeFormat = when {
        currentLine.startsWith("### ") -> "h3"
        currentLine.startsWith("## ")  -> "h2"
        currentLine.startsWith("# ")   -> "h1"
        else                           -> "text"
    }

    Surface(
        color           = colors.surface,
        shadowElevation = 4.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FormatChip("H1",   selected = activeFormat == "h1")   { onApplyPrefix("# ") }
                FormatChip("H2",   selected = activeFormat == "h2")   { onApplyPrefix("## ") }
                FormatChip("H3",   selected = activeFormat == "h3")   { onApplyPrefix("### ") }
                FormatChip("Text", selected = activeFormat == "text") { onApplyPrefix("") }

                ToolbarDivider(colors.divider)

                FormatChip("B", fontWeight = FontWeight.Bold)                { onApplyInline("**") }
                FormatChip("I", fontStyle  = FontStyle.Italic)               { onApplyInline("*") }

                ToolbarDivider(colors.divider)

                TimestampChip(
                    description  = strings.notesInsertTimestamp,
                    onClick      = { onInsertTimestamp(false) },
                    onLongClick  = { onInsertTimestamp(true) }
                )
            }

            IconButton(
                onClick  = soundClick(onUndo),
                enabled  = canUndo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = null,
                    tint               = if (canUndo) ToolbarAccent else colors.onSurfaceTertiary,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

/**
 * Chip-shaped button that inserts the current time; a long press inserts the
 * date as well. Built by hand because FilterChip has no long-press hook.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimestampChip(
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(ChipMinWidth)
            .border(1.dp, ToolbarAccent, shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication  = ripple(),
                onClick     = soundClick(onClick),
                onLongClick = soundClick(onLongClick)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Schedule,
            contentDescription = description,
            tint               = ToolbarAccent,
            modifier           = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ToolbarDivider(color: Color) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(20.dp)
            .background(color)
    )
}

@Composable
private fun FormatChip(
    label: String,
    selected: Boolean = false,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration? = null,
    fontFamily: FontFamily? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick  = soundClick(onClick),
        label    = {
            // A single-letter chip would stay narrower than the 48dp minimum
            // touch target and get padded with invisible space, widening the
            // gaps around it. Giving the label the minimum width instead grows
            // the chip itself and keeps the letter centred.
            Box(
                Modifier.widthIn(min = ChipLabelMinWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text           = label,
                    fontSize       = 13.sp,
                    fontWeight     = fontWeight,
                    fontStyle      = fontStyle,
                    textDecoration = textDecoration,
                    fontFamily     = fontFamily
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor         = Color.Transparent,
            labelColor             = ToolbarAccent,
            selectedContainerColor = ToolbarAccent,
            selectedLabelColor     = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled             = true,
            selected            = selected,
            borderColor         = ToolbarAccent,
            selectedBorderColor = Color.Transparent,
            borderWidth         = 1.dp
        )
    )
}
