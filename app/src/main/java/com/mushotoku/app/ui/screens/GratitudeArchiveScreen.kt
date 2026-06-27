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

import com.mushotoku.app.ui.strings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.GratitudeEntry
import kotlinx.collections.immutable.ImmutableList
import com.mushotoku.app.ui.components.SwipeAction
import com.mushotoku.app.ui.components.SwipeToReveal
import com.mushotoku.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun GratitudeArchiveScreen(
    entries: ImmutableList<GratitudeEntry>,
    strings: AppStrings,
    onDelete: (Long) -> Unit,
    onClose: () -> Unit
) {
    val colors       = LocalAppColors.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusManager.clearFocus(force = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
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
                text = strings.meditationArchive,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = strings.meditationArchiveEmpty,
                    color = colors.onSurfaceSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.date }) { entry ->
                    SwipeToReveal(
                        endAction = SwipeAction(Icons.Default.Delete, Color(0xFFD32F2F)) {
                            onDelete(entry.date)
                        }
                    ) {
                        ArchiveEntryCard(entry = entry, strings = strings)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveEntryCard(entry: GratitudeEntry, strings: AppStrings) {
    val colors = LocalAppColors.current
    val date = LocalDate.ofEpochDay(entry.date)
    val formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", strings.locale)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🙏", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = date.format(formatter),
                    color = colors.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                val filled = entry.filledCount
                Row {
                    repeat(3) { i ->
                        Text(
                            text = if (i < filled) "✦" else "✧",
                            color = if (i < filled) colors.accent else colors.onSurfaceTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (entry.filledCount > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(Modifier.height(10.dp))
                listOf(entry.entry1, entry.entry2, entry.entry3)
                    .filterNot { it.isBlank() }
                    .forEachIndexed { idx, text ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                text = text,
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
            }
        }
    }
}
