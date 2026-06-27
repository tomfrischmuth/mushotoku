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

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.science.Metabolism
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.viewmodel.SleepCaffeineViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class CaffeinePreset(val labelKey: (AppStrings) -> String, val mg: Int)

private val CAFFEINE_PRESETS = listOf(
    CaffeinePreset({ it.sleepLabPresetCoffee }, 95),
    CaffeinePreset({ it.sleepLabPresetEspresso }, 63),
    CaffeinePreset({ it.sleepLabPresetTea }, 47),
    CaffeinePreset({ it.sleepLabPresetEnergy }, 105),
    CaffeinePreset({ it.sleepLabPresetCola }, 32),
)

private val METABOLISM_OPTIONS = listOf(
    Metabolism.FAST to { s: AppStrings -> s.sleepLabMetabFast },
    Metabolism.NORMAL to { s: AppStrings -> s.sleepLabMetabNormal },
    Metabolism.SLOW to { s: AppStrings -> s.sleepLabMetabSlow },
)

private fun bgDark(c: Color) = c.red + c.green + c.blue < 1.5f

@Composable
fun SleepCaffeineScreen(
    vm: SleepCaffeineViewModel,
    strings: AppStrings,
    onClose: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }

    val doses by vm.doses.collectAsStateWithLifecycle()
    val hint by vm.hint.collectAsStateWithLifecycle()
    val bedtime by vm.desiredBedtime.collectAsStateWithLifecycle()
    val metabolism by vm.metabolism.collectAsStateWithLifecycle()

    val isDark = bgDark(colors.background)
    val hazeState = rememberHazeState()

    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF0E1430), Color(0xFF0A1124)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE6ECF8), Color(0xFFEDF1FB), Color(0xFFE4ECF7)))
    }
    val glassStyle = if (isDark) HazeStyle(
        blurRadius = 22.dp,
        tints = listOf(HazeTint(Color(0xFF0C1226).copy(alpha = 0.60f))),
        fallbackTint = HazeTint(Color(0xFF161B33)),
    ) else HazeStyle(
        blurRadius = 22.dp,
        tints = listOf(HazeTint(Color.White.copy(alpha = 0.50f))),
        fallbackTint = HazeTint(Color(0xFFEFF2FB)),
    )
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f)
    val headerTint = if (isDark) Color.White.copy(alpha = 0.82f) else Color(0xFF24306A)
    val orbTop = if (isDark) Color(0xFF3949AB).copy(alpha = 0.18f) else Color(0xFF7986CB).copy(alpha = 0.20f)
    val orbBottom = if (isDark) Color(0xFF00838F).copy(alpha = 0.16f) else Color(0xFF4DD0E1).copy(alpha = 0.16f)

    Box(Modifier.fillMaxSize()) {

        Box(Modifier.fillMaxSize().background(bgGradient).hazeSource(hazeState)) {
            Box(
                Modifier.size(300.dp).absoluteOffset(x = 110.dp, y = (-60).dp)
                    .clip(CircleShape).background(orbTop).align(Alignment.TopEnd)
            )
            Box(
                Modifier.size(240.dp).absoluteOffset(x = (-70).dp, y = 60.dp)
                    .clip(CircleShape).background(orbBottom).align(Alignment.BottomStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = soundClick(onClose)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = headerTint)
                }
                Text(strings.sleepLabTitle, fontSize = 18.sp, fontWeight = FontWeight.Light, color = headerTint, letterSpacing = 0.6.sp)
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(hazeState, glassStyle, glassBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌙", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.sleepLabProtectTitle, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(14.dp))

                when {
                    hint.overThreshold -> {
                        Text(strings.sleepLabEnoughTitle, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.sleepLabActiveAtBed(hint.remainingAtBedMg, hint.bedtime.format(HM)),
                            color = colors.onSurfaceSecondary, fontSize = 13.sp,
                        )
                    }
                    hint.hasCaffeineToday -> {
                        Text(
                            strings.sleepLabActiveAtBed(hint.remainingAtBedMg, hint.bedtime.format(HM)),
                            color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    else -> {
                        Text(strings.sleepLabRuheGeschuetzt, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (!hint.overThreshold) {
                    Spacer(Modifier.height(6.dp))
                    val cutoff = hint.cutoffTime
                    val actionText = if (cutoff != null)
                        strings.sleepLabCutoff(cutoff.format(HM))
                    else
                        strings.sleepLabNoMore
                    Text(actionText, color = colors.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(hazeState, glassStyle, glassBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☕", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.sleepLabCaffeineTitle, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(12.dp))

                val todayStart = remember { LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli() }
                val todayDoses = doses.filter { it.timeMillis >= todayStart }.sortedBy { it.timeMillis }
                if (todayDoses.isEmpty()) {
                    Text(strings.sleepLabNoCaffeine, color = colors.onSurfaceTertiary, fontSize = 13.sp)
                } else {
                    todayDoses.forEach { dose ->
                        val doseTime = remember(dose.timeMillis) {
                            Instant.ofEpochMilli(dose.timeMillis).atZone(zone).toLocalTime()
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                doseTime.format(HM),
                                color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        showTimePicker(context, doseTime) { vm.updateDoseTime(dose, it) }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                            Text(
                                "· ${dose.source.ifBlank { strings.sleepLabCaffeineTitle }} · ${strings.sleepLabMg(dose.amountMg)}",
                                color = colors.onSurfaceSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = soundClick { vm.deleteDose(dose.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = colors.onSurfaceTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(strings.sleepLabAddCaffeine, color = colors.onSurfaceTertiary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                FlowRowPresets {
                    CAFFEINE_PRESETS.forEach { preset ->
                        val label = preset.labelKey(strings)
                        PresetChip(
                            text = "$label · ${preset.mg}",
                            accent = colors.accent,
                            isDark = isDark,
                            drinkable = preset.mg <= hint.budgetNowMg,
                            onClick = { vm.addDose(preset.mg, label) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(hazeState, glassStyle, glassBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(strings.sleepLabPersonalization, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(14.dp))

                Text(strings.sleepLabBedtimeLabel, color = colors.onSurfaceSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                TimeChip(
                    text = bedtime.format(HM),
                    accent = colors.accent,
                    onClick = { showTimePicker(context, bedtime) { vm.setDesiredBedtime(it) } },
                )

                Spacer(Modifier.height(16.dp))
                Text(strings.sleepLabMetabolism, color = colors.onSurfaceSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                FlowRowPresets {
                    METABOLISM_OPTIONS.forEach { (option, labelOf) ->
                        SelectableChip(
                            text = labelOf(strings),
                            selected = option == metabolism,
                            accent = colors.accent,
                            isDark = isDark,
                            onClick = { vm.setMetabolism(option) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun GlassCard(
    hazeState: HazeState,
    glassStyle: HazeStyle,
    glassBorder: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .border(0.5.dp, glassBorder, shape).clip(shape).hazeEffect(hazeState, glassStyle),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun TimeChip(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PresetChip(text: String, accent: Color, isDark: Boolean, drinkable: Boolean, onClick: () -> Unit) {
    val textColor = if (drinkable) accent else accent.copy(alpha = 0.35f)
    val borderColor = if (drinkable) accent.copy(alpha = 0.55f) else accent.copy(alpha = 0.18f)
    val bgColor = when {
        drinkable && isDark -> Color.White.copy(alpha = 0.07f)
        drinkable -> Color.Black.copy(alpha = 0.04f)
        isDark -> Color.White.copy(alpha = 0.03f)
        else -> Color.Black.copy(alpha = 0.02f)
    }
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SelectableChip(text: String, selected: Boolean, accent: Color, isDark: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected -> accent.copy(alpha = 0.18f)
        isDark -> Color.White.copy(alpha = 0.06f)
        else -> Color.Black.copy(alpha = 0.04f)
    }
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(if (selected) 1.dp else 0.5.dp, accent.copy(alpha = if (selected) 0.8f else 0.4f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = accent, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowPresets(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private val HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun showTimePicker(context: Context, initial: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)) }, initial.hour, initial.minute, true).show()
}
