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

package com.mushotoku.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.TextStyle

private const val RANGE  = 180
private const val CENTER = RANGE

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateSlider(
    selectedDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit,
    onTodayLongPress: (() -> Unit)? = null,
    scrollToSelectedTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope     = rememberCoroutineScope()

    val dates = remember(today) {
        (-RANGE..RANGE).map { today.plusDays(it.toLong()) }
    }

    val selectedIndex = remember(selectedDate, today) {
        dates.indexOfFirst { it == selectedDate }.takeIf { it >= 0 } ?: CENTER
    }

    LaunchedEffect(selectedIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val visibleCenter = visible.firstOrNull()?.index?.plus(visible.size / 2) ?: -1
        if (visibleCenter != selectedIndex) {
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }

    LaunchedEffect(scrollToSelectedTrigger) {
        if (scrollToSelectedTrigger > 0) {
            listState.animateScrollToItem((selectedIndex - 3).coerceAtLeast(0))
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem((CENTER - 3).coerceAtLeast(0))
    }

    val colors = LocalAppColors.current
    val locale = LocalAppStrings.current.locale

    LazyRow(
        state             = listState,
        modifier          = modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding    = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(dates.size) { index ->
            val date       = dates[index]
            val isSelected = date == selectedDate
            val isToday    = date == today

            val bgScale = remember { Animatable(if (isSelected) 1f else 0f) }
            LaunchedEffect(isSelected) {
                if (isSelected) {
                    if (bgScale.value < 0.9f) bgScale.snapTo(0.6f)
                    bgScale.animateTo(
                        1f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMedium
                        )
                    )
                } else {
                    bgScale.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
                }
            }

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        // ripple() (no sound) + soundClick on onClick: tap clicks,
                        // long-press stays silent on release.
                        indication = ripple(),
                        onClick    = soundClick { scope.launch { onDateSelected(date) } },
                        onLongClick = if (isToday && onTodayLongPress != null) onTodayLongPress else null
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .scale(bgScale.value)
                        .background(colors.accent)
                )

                Column(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        fontSize   = 10.sp,
                        color      = if (isSelected) Color.White else colors.onSurfaceSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = date.dayOfMonth.toString(),
                        fontSize   = 16.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) Color.White
                                     else if (isToday) colors.accent
                                     else colors.onSurface
                    )
                }
            }
        }
    }
}
