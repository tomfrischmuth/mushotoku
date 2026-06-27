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

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SwipeAnchor { Settled, RevealedStart, RevealedEnd, DismissedStart, DismissedEnd }

data class SwipeAction(
    val icon: ImageVector,
    val color: Color,
    val onButtonClick: (() -> Unit)? = null,
    val onAction: () -> Unit
)

@Composable
fun SwipeToReveal(
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    peekWidth: Dp = 72.dp,
    resetTrigger: Int = 0,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val peekPx = with(density) { peekWidth.toPx() }

    val state = remember { AnchoredDraggableState(initialValue = SwipeAnchor.Settled) }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.3f },
        animationSpec = tween(300)
    )

    LaunchedEffect(resetTrigger) {
        if (resetTrigger > 0 && !state.offset.isNaN()) state.animateTo(SwipeAnchor.Settled)
    }

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeAnchor.DismissedStart -> {
                startAction?.onAction?.invoke()
                state.animateTo(SwipeAnchor.Settled)
            }
            SwipeAnchor.DismissedEnd -> {
                endAction?.onAction?.invoke()
                state.animateTo(SwipeAnchor.Settled)
            }
            else -> Unit
        }
    }

    val offset by remember { derivedStateOf {
        val raw = state.offset
        if (raw.isNaN()) 0 else raw.roundToInt()
    } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { size ->
                state.updateAnchors(
                    DraggableAnchors {
                        SwipeAnchor.Settled at 0f
                        if (startAction != null) {
                            SwipeAnchor.RevealedStart at peekPx
                            SwipeAnchor.DismissedStart at size.width.toFloat()
                        }
                        if (endAction != null) {
                            SwipeAnchor.RevealedEnd at -peekPx
                            SwipeAnchor.DismissedEnd at -size.width.toFloat()
                        }
                    }
                )
            }
    ) {
        if (startAction != null && offset > 0) {
            Box(
                modifier = Modifier.matchParentSize().background(startAction.color),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(peekWidth)
                        .fillMaxHeight()
                        .clickable {
                            scope.launch {
                                (startAction.onButtonClick ?: startAction.onAction).invoke()
                                state.animateTo(SwipeAnchor.Settled)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(startAction.icon, contentDescription = null, tint = Color.White)
                }
            }
        }

        if (endAction != null && offset < 0) {
            Box(
                modifier = Modifier.matchParentSize().background(endAction.color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(peekWidth)
                        .fillMaxHeight()
                        .clickable {
                            scope.launch {
                                endAction.onAction()
                                state.animateTo(SwipeAnchor.Settled)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(endAction.icon, contentDescription = null, tint = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset, 0) }
                .fillMaxWidth()
                .anchoredDraggable(state, Orientation.Horizontal, flingBehavior = flingBehavior)
        ) {
            content()
            if (offset != 0) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { scope.launch { state.animateTo(SwipeAnchor.Settled) } }
                )
            }
        }
    }
}
