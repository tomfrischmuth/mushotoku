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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.strings.LocalAppStrings

enum class AppTab { TODAY, FINANCE, NOTES }

@Composable
fun BottomBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    showFinance: Boolean = true,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabButton(strings.tabToday,   Icons.Default.Today,    currentTab == AppTab.TODAY,   Modifier.weight(1f)) { onTabChange(AppTab.TODAY) }
            if (showFinance) {
                TabButton(strings.tabFinance, Icons.Default.Savings, currentTab == AppTab.FINANCE, Modifier.weight(1f)) { onTabChange(AppTab.FINANCE) }
            }
            TabButton(strings.tabNotes,   Icons.Default.EditNote, currentTab == AppTab.NOTES,   Modifier.weight(1f)) { onTabChange(AppTab.NOTES) }
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) Color(0xFF3D5AFE) else Color(0xFF9E9E9E)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
