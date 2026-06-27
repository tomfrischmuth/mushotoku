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
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MeditationScreen(
    vm: MeditationViewModel,
    strings: AppStrings,
    onClose: () -> Unit
) {
    val colors         = LocalAppColors.current
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

    LaunchedEffect(timerState.isRunning, timerState.isPaused) {
        if (timerState.isRunning || timerState.isPaused) showTimer = true
    }
    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    val isGerman   = strings.locale.language == "de"
    val todayQuote = remember { BuddhistQuotes.today(isGerman) }

    val isDark = bgIsDark(colors.background.red, colors.background.green, colors.background.blue)

    val hazeState = rememberHazeState()

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0C0B17), Color(0xFF0F1024), Color(0xFF0A0E1E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFEAE6F5), Color(0xFFEDF1FA), Color(0xFFE5ECF6)))
    }

    val glassStyle = if (isDark) HazeStyle(
        blurRadius   = 22.dp,
        tints        = listOf(HazeTint(Color(0xFF0D0B1E).copy(alpha = 0.60f))),
        fallbackTint = HazeTint(Color(0xFF1A1830))
    ) else HazeStyle(
        blurRadius   = 22.dp,
        tints        = listOf(HazeTint(Color.White.copy(alpha = 0.50f))),
        fallbackTint = HazeTint(Color(0xFFF2EFF8))
    )

    val glassBorder  = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f)
    val headerTint   = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF3A2668)
    val orbTop       = if (isDark) Color(0xFF5E35B1).copy(alpha = 0.16f) else Color(0xFF9575CD).copy(alpha = 0.20f)
    val orbBottom    = if (isDark) Color(0xFF1A237E).copy(alpha = 0.18f) else Color(0xFF3D5AFE).copy(alpha = 0.16f)

    val journalCount = allGratitude.size

    Box(Modifier.fillMaxSize()) {

        Box(
            Modifier
                .fillMaxSize()
                .background(bgGradient)
                .hazeSource(hazeState)
        ) {
            Box(
                Modifier
                    .size(300.dp)
                    .absoluteOffset(x = 110.dp, y = (-55).dp)
                    .clip(CircleShape)
                    .background(orbTop)
                    .align(Alignment.TopEnd)
            )
            Box(
                Modifier
                    .size(240.dp)
                    .absoluteOffset(x = (-70).dp, y = 55.dp)
                    .clip(CircleShape)
                    .background(orbBottom)
                    .align(Alignment.BottomStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Spacer(Modifier.height(40.dp))

            QuoteHero(quote = todayQuote, isDark = isDark)

            Spacer(Modifier.height(32.dp))

            StatsRow(
                meditatedMinutes = totalMeditatedMinutes,
                journalCount     = journalCount,
                allMoods         = allMoods,
                strings          = strings,
                hazeState        = hazeState,
                glassStyle       = glassStyle,
                glassBorder      = glassBorder,
                isDark           = isDark
            )

            Spacer(Modifier.height(16.dp))

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

            GratitudeCard(
                entry       = todayGratitude,
                strings     = strings,
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                isDark      = isDark,
                onSave      = { e1, e2, e3 -> vm.saveGratitude(e1, e2, e3) },
                onArchive   = { showArchive = true }
            )

            Spacer(Modifier.height(12.dp))

            MoodCard(
                todayMood   = todayMood,
                recentMoods = recentMoods,
                strings     = strings,
                hazeState   = hazeState,
                glassStyle  = glassStyle,
                glassBorder = glassBorder,
                onSelect    = { vm.saveMood(it) }
            )

            Spacer(Modifier.height(12.dp))

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
