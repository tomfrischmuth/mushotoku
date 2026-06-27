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
import com.mushotoku.app.ui.components.*
import com.mushotoku.app.ui.*

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AppSettings
import java.text.Collator
import com.mushotoku.app.ui.theme.LocalAppColors

private val FONT_SCALE_VALUES = listOf(0.85f, 1.0f, 1.15f)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun DarstellungSection(
    settings: AppSettings,
    onSetThemeMode: (String) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetConfirmDelete: (Boolean) -> Unit,
    onSetHaptic: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    val fontLabels = listOf(strings.fontSmall, strings.fontNormal, strings.fontLarge)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        SectionLabel(strings.sectionAppearance)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(strings.modeLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LIGHT" to strings.modeLight, "DARK" to strings.modeDark, "SYSTEM" to strings.modeSystem).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick  = soundClick { onSetThemeMode(mode) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor     = Color.White,
                                containerColor         = colors.surfaceVariant,
                                labelColor             = colors.onSurface
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(Modifier.height(12.dp))

                Text(strings.fontSizeLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FONT_SCALE_VALUES.zip(fontLabels).forEach { (scale, label) ->
                        FilterChip(
                            selected = settings.fontScale == scale,
                            onClick  = soundClick { onSetFontScale(scale) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor     = Color.White,
                                containerColor         = colors.surfaceVariant,
                                labelColor             = colors.onSurface
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(Modifier.height(12.dp))

                Text(strings.languageLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val langCollator = Collator.getInstance(strings.locale)
                    val sortedLanguages = listOf(
                        "de" to strings.langGerman,
                        "en" to strings.langEnglish,
                        "es" to strings.langSpanish,
                        "fr" to strings.langFrench,
                        "it" to strings.langItalian,
                        "pt-PT" to strings.langPortuguesePt,
                        "pt-BR" to strings.langPortugueseBr,
                        "nl" to strings.langDutch,
                        "pl" to strings.langPolish,
                    ).sortedWith(compareBy(langCollator) { it.second })
                    (listOf("AUTO" to strings.langAuto) + sortedLanguages).forEach { (lang, label) ->
                        FilterChip(
                            selected = settings.language == lang,
                            onClick  = soundClick { onSetLanguage(lang) },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent,
                                selectedLabelColor     = Color.White,
                                containerColor         = colors.surfaceVariant,
                                labelColor             = colors.onSurface
                            )
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.divider)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.confirmDeleteLabel, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.confirmDeleteEnabled,
                        onCheckedChange = soundCheck(onSetConfirmDelete),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.divider)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.hapticFeedbackLabel, fontSize = 15.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = soundCheck(onSetHaptic),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        AppIconSection()

        Spacer(Modifier.height(10.dp))
        Text(
            text = strings.meditationHint,
            color = colors.onSurfaceSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}
