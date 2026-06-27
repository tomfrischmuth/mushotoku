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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mushotoku.app.ui.theme.AppColors
import com.mushotoku.app.ui.theme.LocalAppColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

internal class GlassOverlayEntry(
    val onDismissRequest: () -> Unit,
    val content: @Composable BoxScope.() -> Unit
)

class GlassOverlayHostState {
    internal val entries = mutableStateListOf<GlassOverlayEntry>()
    internal fun push(entry: GlassOverlayEntry) { entries.add(entry) }
    internal fun remove(entry: GlassOverlayEntry) { entries.remove(entry) }
}

val LocalGlassOverlayHost = staticCompositionLocalOf<GlassOverlayHostState?> { null }

@Composable
fun rememberGlassOverlayHostState(): GlassOverlayHostState = remember { GlassOverlayHostState() }

@Composable
fun GlassOverlayHost(state: GlassOverlayHostState) {
    state.entries.forEachIndexed { index, entry ->
        Box(Modifier.fillMaxSize()) {
            BackHandler(enabled = index == state.entries.lastIndex) { entry.onDismissRequest() }
            entry.content(this)
        }
    }
}

private fun AppColors.isDark() =
    (background.red + background.green + background.blue) < 1.5f

private fun dialogScrimStyle(isDark: Boolean) = HazeStyle(
    blurRadius   = 20.dp,
    tints        = listOf(HazeTint(Color.Black.copy(alpha = if (isDark) 0.50f else 0.35f))),
    fallbackTint = HazeTint(Color.Black.copy(alpha = if (isDark) 0.50f else 0.35f))
)

private fun dialogGlassStyle(isDark: Boolean) = HazeStyle(
    blurRadius   = 14.dp,
    tints        = listOf(HazeTint(
        if (isDark) Color(0xFF0E0E0E).copy(alpha = 0.75f)
        else        Color.White.copy(alpha = 0.68f)
    )),
    fallbackTint = HazeTint(
        if (isDark) Color(0xFF1C1C1E)
        else        Color(0xFFF0F0F5)
    )
)

private val dialogBorderColor = Color.White.copy(alpha = 0.22f)

@Composable
private fun dialogSetup(onDismissRequest: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val host = LocalGlassOverlayHost.current
    if (host == null) {
        Box(Modifier.fillMaxSize(), content = content)
        return
    }
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val currentContent by rememberUpdatedState(content)
    val entry = remember {
        GlassOverlayEntry(
            onDismissRequest = { currentDismiss() },
            content          = { currentContent() }
        )
    }
    DisposableEffect(Unit) {
        host.push(entry)
        onDispose { host.remove(entry) }
    }
}

@Composable
private fun BlurScrim(hazeState: HazeState?, isDark: Boolean, onDismissRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) Modifier.hazeEffect(hazeState, dialogScrimStyle(isDark))
                else Modifier.background(Color.Black.copy(alpha = 0.40f))
            )
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                onDismissRequest()
            }
    )
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    confirmButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    val hazeState = LocalHazeState.current
    val colors    = LocalAppColors.current
    val isDark    = colors.isDark()
    val cardShape = RoundedCornerShape(28.dp)

    dialogSetup(onDismissRequest) {
        BlurScrim(hazeState, isDark, onDismissRequest)

        Column(
            modifier = modifier
                .align(Alignment.Center)
                .imePadding()
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .border(0.5.dp, dialogBorderColor, cardShape)
                .clip(cardShape)
                .then(
                    if (hazeState != null) Modifier.hazeEffect(hazeState, dialogGlassStyle(isDark))
                    else Modifier.background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF0F0F5))
                )
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                .padding(24.dp)
        ) {
            ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(color = colors.onSurface)) {
                title()
            }
            if (text != null) {
                Spacer(Modifier.height(16.dp))
                ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceSecondary)) {
                    text()
                }
            }
            if (confirmButton != null || dismissButton != null) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    if (dismissButton != null) Spacer(Modifier.width(8.dp))
                    confirmButton?.invoke()
                }
            }
        }
    }
}

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val hazeState = LocalHazeState.current
    val colors    = LocalAppColors.current
    val isDark    = colors.isDark()

    dialogSetup(onDismissRequest) {
        BlurScrim(hazeState, isDark, onDismissRequest)

        Box(
            modifier = modifier
                .align(Alignment.Center)
                .imePadding()
                .border(0.5.dp, dialogBorderColor, shape)
                .clip(shape)
                .then(
                    if (hazeState != null) Modifier.hazeEffect(hazeState, dialogGlassStyle(isDark))
                    else Modifier.background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF0F0F5))
                )
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
        ) {
            content()
        }
    }
}
