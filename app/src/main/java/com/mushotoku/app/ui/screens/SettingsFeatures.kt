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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.ui.components.soundCheck
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors

/**
 * The parts of the app that can be left out entirely. Mindfulness carries the
 * way to reach it with it: hidden behind a long press, it would otherwise be
 * switched on with no sign of where it went.
 */
@Composable
internal fun FeaturesSection(
    settings: AppSettings,
    onSetFinanceEnabled: (Boolean) -> Unit,
    onSetMindfulnessEnabled: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        SectionLabel(strings.sectionMindfulness)
        FeatureCard {
            FeatureRow(
                label    = strings.showMindfulness,
                subtitle = strings.featureMindfulnessDescription,
                checked  = settings.mindfulnessEnabled,
                onCheckedChange = onSetMindfulnessEnabled
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(strings.sectionFinanceTab)
        FeatureCard {
            FeatureRow(
                label    = strings.showFinanceTab,
                subtitle = strings.featureFinanceDescription,
                checked  = settings.financeTabEnabled,
                onCheckedChange = onSetFinanceEnabled
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content   = content
    )
}

@Composable
private fun FeatureRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = colors.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = colors.onSurfaceSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = soundCheck(onCheckedChange),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
        )
    }
}
