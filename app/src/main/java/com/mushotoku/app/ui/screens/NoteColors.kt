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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mushotoku.app.ui.theme.LocalAppColors

/**
 * The colours a note can be given. Stored as the index, not as a colour value,
 * so the same note can be tinted differently in the light and the dark theme.
 * Index 0 is "no colour" and keeps the ordinary card surface.
 */
private val NoteHues = listOf(
    Color(0xFFE53935), // red
    Color(0xFFFB8C00), // orange
    Color(0xFFFDD835), // yellow
    Color(0xFF43A047), // green
    Color(0xFF1E88E5), // blue
    Color(0xFF8E24AA), // purple
)

internal val NoteColorCount = NoteHues.size + 1

/** Same rule the mindfulness screen uses to tell the themes apart. */
internal fun Color.isDarkSurface(): Boolean = red + green + blue < 1.5f

/** The full-strength hue, for the circles in the picker. */
internal fun noteHue(index: Int): Color? = NoteHues.getOrNull(index - 1)

/**
 * Card background for a note. The hue is heavily muted: it has to read as a
 * tinted card, not as a coloured block, and the title must stay legible on it
 * in both themes.
 */
internal fun noteCardColor(index: Int, surface: Color, isDark: Boolean): Color {
    val hue = noteHue(index) ?: return surface
    // Light mode needs the larger share: the same tint over white reads much
    // paler than over a dark surface.
    return hue.copy(alpha = if (isDark) 0.34f else 0.30f).compositeOver(surface)
}

/**
 * Colour for the editor's controls: the same hue as the card, but at full
 * strength — as a thin outline or a letter it has to carry, where the card can
 * rely on its whole area. On a dark surface it is lifted towards white, which
 * dark backgrounds need to keep a colour legible.
 */
internal fun noteAccentColor(index: Int, isDark: Boolean): Color? {
    val hue = noteHue(index) ?: return null
    return if (isDark) hue.mixWith(Color.White, 0.35f) else hue
}

private fun Color.mixWith(other: Color, amount: Float): Color = Color(
    red   = red * (1 - amount) + other.red * amount,
    green = green * (1 - amount) + other.green * amount,
    blue  = blue * (1 - amount) + other.blue * amount,
    alpha = 1f
)

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red   = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue  = blue * a + background.blue * (1 - a),
        alpha = 1f
    )
}

/**
 * Colours for the selected notes, as circles. Sits above the search so it lands
 * in view the moment a selection starts.
 */
@Composable
internal fun NoteColorPicker(
    current: Int?,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        val isDark = colors.background.isDarkSurface()
        repeat(NoteColorCount) { index ->
            val selected = current == index
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    // Exactly the card's own colour, so the circle is a preview
                    // of the note rather than of the palette.
                    .background(noteCardColor(index, colors.surface, isDark))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) colors.onSurface else colors.divider,
                        shape = CircleShape
                    )
                    .clickable { onPick(index) },
                contentAlignment = Alignment.Center
            ) {
                // The first circle is the way back to no colour at all.
                if (index == 0) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        tint = colors.onSurfaceSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
