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

import com.mushotoku.app.ui.*
import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.viewmodel.MeditationViewModel
import com.mushotoku.app.viewmodel.SleepCaffeineViewModel
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private fun bgIsDark(r: Float, g: Float, b: Float) = r + g + b < 1.5f

/**
 * A warm orange for the whole mindfulness corner: it sets these screens apart
 * from the app's businesslike blue without needing a background of their own.
 */
private val MindfulAccentLight = Color(0xFFD97328)
private val MindfulAccentDark  = Color(0xFFE8974E)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeditationScreen(
    vm: MeditationViewModel,
    strings: AppStrings,
    onClose: () -> Unit
) {
    val baseColors = LocalAppColors.current
    val warm = if (bgIsDark(baseColors.background.red, baseColors.background.green, baseColors.background.blue))
        MindfulAccentDark else MindfulAccentLight
    val colors = remember(baseColors, warm) {
        baseColors.copy(accent = warm, accentContainer = warm.copy(alpha = 0.16f))
    }
    val focusManager   = LocalFocusManager.current
    val timerState            by vm.timerState.collectAsStateWithLifecycle()
    val todayGratitude        by vm.todayGratitude.collectAsStateWithLifecycle()
    val allGratitude          by vm.allGratitude.collectAsStateWithLifecycle()
    val todayMood             by vm.todayMood.collectAsStateWithLifecycle()
    val recentMoods           by vm.recentMoods.collectAsStateWithLifecycle()
    val allMoods              by vm.allMoods.collectAsStateWithLifecycle()
    val totalMeditatedMinutes by vm.totalMeditatedMinutes.collectAsStateWithLifecycle()

    var showTimer   by remember { mutableStateOf(timerState.isRunning || timerState.isPaused) }
    var showArchive by remember { mutableStateOf(false) }
    var showSleepLab by remember { mutableStateOf(false) }
    val sleepCaffeineVm: SleepCaffeineViewModel = viewModel()

    // Registered after the one in the scaffold, so it takes the gesture first:
    // back leaves the screen that is open, not the whole mindfulness view.
    BackHandler(enabled = showTimer || showArchive || showSleepLab) {
        when {
            showSleepLab -> showSleepLab = false
            showArchive  -> showArchive = false
            showTimer    -> showTimer = false
        }
    }

    LaunchedEffect(timerState.isRunning, timerState.isPaused) {
        if (timerState.isRunning || timerState.isPaused) showTimer = true
    }
    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    val isGerman   = strings.locale.language == "de"
    val todayQuote = remember { BuddhistQuotes.today(isGerman) }

    val isDark = bgIsDark(colors.background.red, colors.background.green, colors.background.blue)

    val hazeState = rememberHazeState()

    // The cards keep their soft look, but over the app's own background rather
    // than a colour scheme of their own.
    val glassStyle = HazeStyle(
        blurRadius   = 22.dp,
        tints        = listOf(HazeTint(colors.surface.copy(alpha = 0.86f))),
        fallbackTint = HazeTint(colors.surface)
    )

    val glassBorder = colors.divider.copy(alpha = if (isDark) 0.45f else 0.70f)
    val headerTint  = colors.onSurface

    // Writing in the journal should lift the whole card above the keyboard,
    // down to the middle of the gap before the next one — then all three lines
    // are in view, whichever one is being written.
    val gratitudeInView = remember { BringIntoViewRequester() }
    val scope           = rememberCoroutineScope()
    val density         = LocalDensity.current
    var gratitudeSize   by remember { mutableStateOf(IntSize.Zero) }

    // One row per day, up to three thoughts in each.
    val journalDays    = allGratitude.size
    val journalEntries = allGratitude.sumOf { it.filledCount }

    CompositionLocalProvider(LocalAppColors provides colors) {
    Box(Modifier.fillMaxSize()) {

        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .hazeSource(hazeState)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // The journal is written into, so the page has to make room for
                // the keyboard and keep the line being typed in view.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = soundClick(onClose)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = headerTint
                    )
                }
                Text(
                    text = strings.meditationTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = headerTint,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            MindSectionLabel(strings.meditationQuoteLabel, centered = true)
            Spacer(Modifier.height(10.dp))
            QuoteHero(quote = todayQuote, accent = colors.accent)

            Spacer(Modifier.height(28.dp))

            MindSectionLabel(strings.meditationSectionMeditation)
            Spacer(Modifier.height(8.dp))
            MindfulnessCard(
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                onClick     = { showTimer = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧘", fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.meditationTimerCard,
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(3.dp))
                        if (timerState.isRunning || timerState.isPaused) {
                            val ms = timerState.remainingMs
                            Text(
                                text = "%02d:%02d".format(ms / 60_000, (ms % 60_000) / 1000),
                                color = colors.accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                strings.meditationStart,
                                color = colors.onSurfaceSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            StatsRow(
                meditatedMinutes = totalMeditatedMinutes,
                journalEntries   = journalEntries,
                journalDays      = journalDays,
                allMoods         = allMoods,
                strings          = strings,
                hazeState        = hazeState,
                glassStyle       = glassStyle,
                glassBorder      = glassBorder,
                isDark           = isDark
            )

            Spacer(Modifier.height(24.dp))

            MindSectionLabel(strings.meditationSectionMood)
            Spacer(Modifier.height(8.dp))
            MoodCard(
                todayMood   = todayMood,
                recentMoods = recentMoods,
                strings     = strings,
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                onSelect    = { vm.saveMood(it) }
            )

            Spacer(Modifier.height(24.dp))

            MindSectionLabel(strings.meditationSectionGratitude)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .onSizeChanged { gratitudeSize = it }
                    .bringIntoViewRequester(gratitudeInView)
            ) {
            GratitudeCard(
                entry       = todayGratitude,
                strings     = strings,
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                isDark      = isDark,
                onSave      = { e1, e2, e3 -> vm.saveGratitude(e1, e2, e3) },
                onArchive   = { showArchive = true },
                onFieldFocused = {
                    scope.launch {
                        // Half of the 24 dp that separates this card from the next.
                        val below = with(density) { 12.dp.toPx() }
                        gratitudeInView.bringIntoView(
                            Rect(
                                left   = 0f,
                                top    = 0f,
                                right  = gratitudeSize.width.toFloat(),
                                bottom = gratitudeSize.height + below
                            )
                        )
                    }
                }
            )
            }

            Spacer(Modifier.height(24.dp))

            MindSectionLabel(strings.meditationSectionSleep)
            Spacer(Modifier.height(8.dp))
            MindfulnessCard(
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                onClick     = { showSleepLab = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            strings.sleepLabCard,
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            strings.sleepLabCardSubtitle,
                            color = colors.onSurfaceSecondary,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }

    if (showSleepLab) {
        SleepCaffeineScreen(vm = sleepCaffeineVm, strings = strings, onClose = { showSleepLab = false })
    }
    if (showTimer) {
        MeditationTimerScreen(vm = vm, strings = strings, onClose = { showTimer = false })
    }
    if (showArchive) {
        GratitudeArchiveScreen(
            entries  = allGratitude,
            strings  = strings,
            onDelete = { vm.deleteGratitude(it) },
            onClose  = { showArchive = false }
        )
    }
    }
}
