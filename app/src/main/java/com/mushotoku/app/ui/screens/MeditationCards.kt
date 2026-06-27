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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.GratitudeEntry
import com.mushotoku.app.data.MoodEntry
import com.mushotoku.app.ui.strings.AppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal val MOOD_COLORS = listOf(
    Color(0xFFE07070), Color(0xFFD4956B),
    Color(0xFFD4C66B), Color(0xFF7FC87A), Color(0xFF7AB8D4)
)

@Composable
internal fun QuoteHero(quote: String, isDark: Boolean) {
    val textColor  = if (isDark) Color.White.copy(alpha = 0.88f)         else Color(0xFF2B1A5C)
    val markColor  = if (isDark) Color(0xFF9B7BD7).copy(alpha = 0.40f)   else Color(0xFF7C5CBF).copy(alpha = 0.32f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text      = "❝",
            fontSize  = 56.sp,
            color     = markColor,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text        = quote,
            fontSize    = 19.sp,
            fontStyle   = FontStyle.Italic,
            color       = textColor,
            textAlign   = TextAlign.Center,
            lineHeight  = 29.sp,
            letterSpacing = 0.2.sp,
            modifier    = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun MindfulnessCard(
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(0.5.dp, glassBorder, shape)
            .clip(shape)
            .hazeEffect(hazeState, glassStyle)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onClick() } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
internal fun GratitudeCard(
    entry: GratitudeEntry?,
    strings: AppStrings,
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    isDark: Boolean,
    onSave: (String, String, String) -> Unit,
    onArchive: () -> Unit
) {
    val colors = LocalAppColors.current
    var e1 by remember(entry?.date) { mutableStateOf(entry?.entry1 ?: "") }
    var e2 by remember(entry?.date) { mutableStateOf(entry?.entry2 ?: "") }
    var e3 by remember(entry?.date) { mutableStateOf(entry?.entry3 ?: "") }
    LaunchedEffect(entry) {
        e1 = entry?.entry1 ?: ""; e2 = entry?.entry2 ?: ""; e3 = entry?.entry3 ?: ""
    }

    MindfulnessCard(hazeState = hazeState, glassStyle = glassStyle, glassBorder = glassBorder) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🙏", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                strings.meditationGratitudeCard,
                color      = colors.onSurface,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f)
            )
            Text(
                text     = "${strings.meditationArchive} ›",
                color    = colors.accent,
                fontSize = 12.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onArchive() }
            )
        }

        Spacer(Modifier.height(6.dp))
        Row {
            val filled = entry?.filledCount ?: 0
            repeat(3) { i ->
                Text(
                    text     = if (i < filled) "✦" else "✧",
                    color    = if (i < filled) colors.accent else colors.onSurfaceTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.divider.copy(alpha = 0.6f))
        Spacer(Modifier.height(14.dp))

        listOf(
            Triple(1, e1) { v: String -> e1 = v },
            Triple(2, e2) { v: String -> e2 = v },
            Triple(3, e3) { v: String -> e3 = v },
        ).forEach { (n, value, setter) ->
            GratitudeField(
                value         = value,
                hint          = strings.meditationGratitudeHint(n),
                onValueChange = setter,
                isDark        = isDark
            )
            if (n < 3) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick  = soundClick { onSave(e1, e2, e3) },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
            Text(strings.meditationGratitudeSave, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GratitudeField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    val colors      = LocalAppColors.current
    var isFocused   by remember { mutableStateOf(false) }
    val fieldBg     = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)
    val fieldBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
    val shape       = RoundedCornerShape(10.dp)

    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        textStyle     = TextStyle(color = colors.onSurface, fontSize = 14.sp),
        cursorBrush   = SolidColor(colors.accent),
        modifier      = Modifier
            .fillMaxWidth()
            .border(0.5.dp, fieldBorder, shape)
            .clip(shape)
            .background(fieldBg)
            .onFocusChanged { isFocused = it.isFocused },
        maxLines      = 3,
        decorationBox = { innerTextField ->
            Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (value.isEmpty() && !isFocused) {
                    Text(hint, color = colors.onSurfaceTertiary, fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}

@Composable
internal fun MoodCard(
    todayMood: MoodEntry?,
    recentMoods: ImmutableList<MoodEntry>,
    strings: AppStrings,
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    onSelect: (Int) -> Unit
) {
    val colors = LocalAppColors.current
    MindfulnessCard(hazeState = hazeState, glassStyle = glassStyle, glassBorder = glassBorder) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("☀️", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                strings.meditationMoodCard,
                color      = colors.onSurface,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(strings.meditationMoodQuestion, color = colors.onSurfaceSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (1..5).forEach { level ->
                val isSel = todayMood?.mood == level
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSel) MOOD_COLORS[level - 1].copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onSelect(level) }
                ) {
                    Text(strings.meditationMoodEmoji(level), fontSize = 26.sp)
                }
            }
        }

        if (recentMoods.size >= 3) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            MoodChart(moods = recentMoods.take(14).reversed().toImmutableList(), accentColor = colors.accent)
        }
    }
}

@Composable
private fun MoodChart(moods: ImmutableList<MoodEntry>, accentColor: Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        if (moods.size < 2) return@Canvas
        val stepX  = size.width / (moods.size - 1).toFloat()
        val yRange = size.height * 0.78f
        val yBase  = size.height * 0.88f
        fun moodY(m: Int) = yBase - ((m - 1) / 4f) * yRange

        val path = Path()
        moods.forEachIndexed { i, e ->
            val x = i * stepX; val y = moodY(e.mood)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = accentColor.copy(alpha = 0.40f),
                 style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        moods.forEachIndexed { i, e ->
            drawCircle(
                color  = MOOD_COLORS[(e.mood - 1).coerceIn(0, 4)].copy(alpha = 0.90f),
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(i * stepX, moodY(e.mood))
            )
        }
    }
}
