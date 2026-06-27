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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NoteTypeFilter(
    selected: NoteType?,
    onSelect: (NoteType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    val scope   = rememberCoroutineScope()
    val density = LocalDensity.current

    val options     = remember { listOf<NoteType?>(null, NoteType.ROUTINE, NoteType.LIST, NoteType.NOTE) }
    val pillHeight  = 36.dp

    val pillOffsetX    = remember { Animatable(0f) }
    val pillScale      = remember { Animatable(1f) }
    val currentSelected = rememberUpdatedState(selected)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val containerPx = with(density) { maxWidth.toPx() }
        val itemPx      = containerPx / options.size
        val itemDp      = maxWidth / options.size

        LaunchedEffect(itemPx) {
            pillOffsetX.snapTo(options.indexOf(selected).coerceAtLeast(0) * itemPx)
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffsetX.value.roundToInt(), 0) }
                .size(width = itemDp, height = pillHeight)
                .scale(pillScale.value)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accent)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillHeight)
                .pointerInput(itemPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { _ ->
                            scope.launch {
                                pillScale.animateTo(
                                    1.12f,
                                    spring(stiffness = Spring.StiffnessHigh)
                                )
                            }
                        },
                        onHorizontalDrag = { change, delta ->
                            change.consume()
                            val newX = (pillOffsetX.value + delta)
                                .coerceIn(0f, (options.lastIndex) * itemPx)
                            scope.launch { pillOffsetX.snapTo(newX) }
                        },
                        onDragEnd = {
                            val nearestIdx = (pillOffsetX.value / itemPx)
                                .roundToInt().coerceIn(0, options.lastIndex)
                            scope.launch {
                                launch {
                                    pillScale.animateTo(
                                        1f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness    = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                pillOffsetX.animateTo(
                                    nearestIdx * itemPx,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            onSelect(options[nearestIdx])
                        },
                        onDragCancel = {
                            val snapIdx = options.indexOf(currentSelected.value).coerceAtLeast(0)
                            scope.launch {
                                launch {
                                    pillScale.animateTo(
                                        1f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                                pillOffsetX.animateTo(
                                    snapIdx * itemPx,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    )
                }
        ) {
            options.forEach { type ->
                val isSelected = type == selected
                val label = if (type == null) strings.notesFilterAll else strings.noteTypeFilterName(type)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val idx = options.indexOf(type).coerceAtLeast(0)
                            scope.launch {
                                pillOffsetX.animateTo(
                                    idx * itemPx,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness    = Spring.StiffnessMedium
                                    )
                                )
                            }
                            onSelect(type)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isSelected) Color.White else colors.onSurfaceSecondary
                    )
                }
            }
        }
    }
}
