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

package com.mushotoku.app.ui

import androidx.compose.ui.graphics.Color

/**
 * The three states of the task light, shared so notes and tasks cannot drift
 * apart. Red means untouched, yellow started, green done.
 */
val StatusRed    = Color(0xFFE53935)
val StatusYellow = Color(0xFFFFB300)
val StatusGreen  = Color(0xFF43A047)
