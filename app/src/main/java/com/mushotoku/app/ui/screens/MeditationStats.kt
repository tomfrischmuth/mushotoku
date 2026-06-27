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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.MoodEntry
import com.mushotoku.app.ui.strings.AppStrings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList

private enum class MoodPeriod { ALL, MONTH, WEEK }

@Composable
internal fun StatsRow(
    meditatedMinutes: Int,
    journalCount: Int,
    allMoods: ImmutableList<MoodEntry>,
    strings: AppStrings,
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    isDark: Boolean
) {
    var moodPeriod by remember { mutableStateOf(MoodPeriod.ALL) }

    val today   = LocalDate.now().toEpochDay()
    val avgMood = when (moodPeriod) {
        MoodPeriod.ALL   -> allMoods
        MoodPeriod.MONTH -> allMoods.filter { it.date >= today - 29 }
        MoodPeriod.WEEK  -> allMoods.filter { it.date >= today - 6 }
    }.let { list ->
        if (list.isNotEmpty()) list.map { it.mood }.average() else -1.0
    }

    val moodValue = if (avgMood < 0) "–" else {
        if (avgMood % 1.0 == 0.0) "${avgMood.toInt()}/5" else "%.1f/5".format(avgMood)
    }
    val moodLabel = when (moodPeriod) {
        MoodPeriod.ALL   -> strings.meditationMoodAll
        MoodPeriod.MONTH -> strings.meditationMoodMonth
        MoodPeriod.WEEK  -> strings.meditationMoodWeek
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            emoji       = "🧘",
            value       = if (meditatedMinutes > 0) "$meditatedMinutes" else "–",
            label       = strings.meditationMinutesLabel,
            hazeState   = hazeState,
            glassStyle  = glassStyle,
            glassBorder = glassBorder,
            isDark      = isDark,
            modifier    = Modifier.weight(1f)
        )
        StatChip(
            emoji       = "📖",
            value       = if (journalCount > 0) "$journalCount" else "–",
            label       = if (journalCount == 1) strings.meditationJournalSingular else strings.meditationJournalPlural,
            hazeState   = hazeState,
            glassStyle  = glassStyle,
            glassBorder = glassBorder,
            isDark      = isDark,
            modifier    = Modifier.weight(1f)
        )
        StatChip(
            emoji       = "😊",
            value       = moodValue,
            label       = moodLabel,
            hazeState   = hazeState,
            glassStyle  = glassStyle,
            glassBorder = glassBorder,
            isDark      = isDark,
            onClick     = { moodPeriod = when (moodPeriod) {
                MoodPeriod.ALL   -> MoodPeriod.MONTH
                MoodPeriod.MONTH -> MoodPeriod.WEEK
                MoodPeriod.WEEK  -> MoodPeriod.ALL
            }},
            modifier    = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    emoji: String,
    value: String,
    label: String,
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    isDark: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape      = RoundedCornerShape(18.dp)
    val valueColor = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF2B1A5C)
    val labelColor = if (isDark) Color.White.copy(alpha = 0.48f) else Color(0xFF7A6A9A)

    Column(
        modifier = modifier
            .border(0.5.dp, glassBorder, shape)
            .clip(shape)
            .hazeEffect(hazeState, glassStyle)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onClick() } else Modifier
            )
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(
            text       = value,
            fontSize   = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color      = valueColor,
            textAlign  = TextAlign.Center
        )
        Text(
            text      = label,
            fontSize  = 10.sp,
            color     = labelColor,
            textAlign = TextAlign.Center
        )
    }
}
