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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AppSettings
import com.mushotoku.app.ui.theme.LocalAppColors

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun NotificationsSection(
    settings: AppSettings,
    permissionGranted: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onSetLead: (Int) -> Unit,
) {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    val context = LocalContext.current
    val n = remember(strings.locale) { notificationStrings(context) }

    val leadOptions = listOf(
        0   to n.leadAtTime,
        5   to n.lead5Min,
        10  to n.lead10Min,
        15  to n.lead15Min,
        30  to n.lead30Min,
        60  to n.lead1Hour,
    )

    Column {
        Spacer(Modifier.height(16.dp))
        SectionLabel(n.section)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(n.enableLabel, fontSize = 15.sp, color = colors.onSurface)
                        Text(n.enableHint, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = soundCheck(onSetEnabled),
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.accent)
                    )
                }

                if (settings.notificationsEnabled) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = colors.divider)
                    Spacer(Modifier.height(12.dp))

                    Text(n.leadLabel, fontSize = 13.sp, color = colors.onSurfaceSecondary)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leadOptions.forEach { (minutes, label) ->
                            FilterChip(
                                selected = settings.notificationLeadMinutes == minutes,
                                onClick  = soundClick { onSetLead(minutes) },
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
                }
            }
        }

        if (settings.notificationsEnabled && !permissionGranted) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = n.permissionHint,
                color = Color(0xFFFFB74D),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
