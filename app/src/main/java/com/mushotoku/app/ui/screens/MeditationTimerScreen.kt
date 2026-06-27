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

import com.mushotoku.app.ui.strings.*

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mushotoku.app.R
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.service.MeditationService
import com.mushotoku.app.viewmodel.MeditationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DURATIONS = listOf(5, 10, 15, 20, 30)
private val INTERVALS = listOf(0, 1, 2, 5, 10)
private val SOUNDS = listOf(
    MeditationService.SOUND_BOWL,
    MeditationService.SOUND_WARM,
    MeditationService.SOUND_KEISU,
)
private fun soundLabel(sound: String, strings: AppStrings): String = when (sound) {
    MeditationService.SOUND_KEISU -> "Keisu"
    MeditationService.SOUND_WARM  -> strings.meditationSoundWarm
    else -> strings.meditationSoundBright
}
private fun soundPreviewRes(sound: String): Int = when (sound) {
    MeditationService.SOUND_KEISU -> R.raw.keisu
    MeditationService.SOUND_WARM  -> R.raw.bowl_warm
    else -> R.raw.singing_bowl
}
@Composable
fun MeditationTimerScreen(
    vm: MeditationViewModel,
    strings: AppStrings,
    onClose: () -> Unit
) {
    val colors       = LocalAppColors.current
    val timerState   by vm.timerState.collectAsStateWithLifecycle()
    val context      = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    var selectedDuration by remember { mutableIntStateOf(10) }
    var selectedInterval by remember { mutableIntStateOf(0) }
    var selectedSound by remember { mutableStateOf(MeditationService.SOUND_BOWL) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val scope = rememberCoroutineScope()
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    fun stopPreview() {
        previewJob?.cancel(); previewJob = null
        previewPlayer?.runCatching { if (isPlaying) stop(); release() }
        previewPlayer = null
    }

    fun previewSound(sound: String) {
        stopPreview()
        val mp = runCatching { MediaPlayer.create(context, soundPreviewRes(sound)) }.getOrNull() ?: return
        previewPlayer = mp
        mp.runCatching { setVolume(0.9f, 0.9f); start() }
        previewJob = scope.launch {
            delay(5000)
            repeat(12) { i ->
                val v = 0.9f * (11 - i) / 12f
                mp.runCatching { setVolume(v, v) }
                delay(50)
            }
            mp.runCatching { if (isPlaying) stop(); release() }
            if (previewPlayer === mp) previewPlayer = null
        }
    }

    DisposableEffect(Unit) { onDispose { stopPreview() } }

    fun launchSession() {
        stopPreview()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.startMeditation(selectedDuration, selectedInterval, selectedSound)
    }

    val isRunning  = timerState.isRunning
    val isPaused   = timerState.isPaused
    val hasSession = isRunning || isPaused

    val tips = if (strings.locale.language == "de") TIPS_DE else TIPS_EN
    var tipIndex by remember { mutableIntStateOf((0 until tips.size).random()) }
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                delay(TIP_INTERVAL_MS)
                tipIndex = (0 until tips.size).filter { it != tipIndex }.random()
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "breath")
    val breathScale by transition.animateFloat(
        initialValue = 0.80f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )
    val ringPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ), label = "ring"
    )
    val bellFlash by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bell"
    )
    val bellAlpha = if (timerState.bellRinging) bellFlash * 0.7f else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.topBar)
                .statusBarsPadding()
                .padding(end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = soundClick(onClose)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back,
                     tint = colors.onSurface)
            }
            Text(
                text = strings.meditationTimerCard,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }

        if (hasSession) {

            Spacer(Modifier.weight(1f))

            val ms  = timerState.remainingMs
            val min = ms / 60_000
            val sec = (ms % 60_000) / 1000
            Text(
                text = "%02d:%02d".format(min, sec),
                color = colors.onSurface,
                fontSize = 54.sp,
                fontWeight = FontWeight.Thin,
                letterSpacing = 4.sp
            )
            if (isPaused) {
                Text(
                    text = strings.meditationPause,
                    color = colors.onSurfaceSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            BreathingCircle(
                breathScale = breathScale,
                ringPhase   = ringPhase,
                bellAlpha   = bellAlpha,
                sizeDp      = 240
            )

            Spacer(Modifier.height(28.dp))

            Crossfade(
                targetState = tipIndex,
                animationSpec = tween(700),
                label = "tip"
            ) { idx ->
                Text(
                    text = tips[idx],
                    color = colors.onSurfaceSecondary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = soundClick { if (isPaused) vm.resumeMeditation() else vm.pauseMeditation() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(
                            listOf(colors.accent.copy(0.4f), colors.accent.copy(0.4f))
                        )
                    )
                ) {
                    Text(if (isPaused) strings.meditationResume else strings.meditationPause,
                         fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = soundClick { vm.stopMeditation(); onClose() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceSecondary),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(listOf(colors.divider, colors.divider))
                    )
                ) {
                    Text(strings.meditationStop, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

        } else {

            Spacer(Modifier.weight(0.5f))

            BreathingCircle(
                breathScale = breathScale,
                ringPhase   = ringPhase,
                bellAlpha   = 0f,
                sizeDp      = 160
            )

            Spacer(Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SettingSection(label = strings.meditationDurationLabel) {
                    TimerChipRow(
                        options = DURATIONS,
                        selected = selectedDuration,
                        label = { strings.meditationMinutes(it) },
                        onSelect = { selectedDuration = it }
                    )
                }
                SettingSection(label = strings.meditationBellLabel) {
                    TimerChipRow(
                        options = INTERVALS,
                        selected = selectedInterval,
                        label = { if (it == 0) strings.meditationBellNone else strings.meditationMinutes(it) },
                        onSelect = { selectedInterval = it }
                    )
                }
                SettingSection(label = strings.meditationSoundLabel) {
                    TimerChipRow(
                        options = SOUNDS,
                        selected = selectedSound,
                        label = { soundLabel(it, strings) },
                        onSelect = { selectedSound = it; previewSound(it) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = soundClick { launchSession() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(strings.meditationStart, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}
