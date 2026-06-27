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

package com.mushotoku.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceSecondary: Color,
    val onSurfaceTertiary: Color,
    val accent: Color,
    val accentContainer: Color,
    val divider: Color,
    val topBar: Color,
    val bottomBar: Color,
)

val LightAppColors = AppColors(
    background          = Color(0xFFF5F5F5),
    surface             = Color.White,
    surfaceVariant      = Color(0xFFF8F9FA),
    onSurface           = Color(0xFF1A1A1A),
    onSurfaceSecondary  = Color(0xFF666666),
    onSurfaceTertiary   = Color(0xFFBBBBBB),
    accent              = Color(0xFF3D5AFE),
    accentContainer     = Color(0xFFE8EAFE),
    divider             = Color(0xFFEEEEEE),
    topBar              = Color.White,
    bottomBar           = Color.White,
)

val DarkAppColors = AppColors(
    background          = Color(0xFF0E0E0E),
    surface             = Color(0xFF1C1C1C),
    surfaceVariant      = Color(0xFF161616),
    onSurface           = Color(0xFFE8E8E8),
    onSurfaceSecondary  = Color(0xFF9E9E9E),
    onSurfaceTertiary   = Color(0xFF4A4A4A),
    accent              = Color(0xFF3D5AFE),
    accentContainer     = Color(0xFF151C3D),
    divider             = Color(0xFF2A2A2A),
    topBar              = Color(0xFF1C1C1C),
    bottomBar           = Color(0xFF1C1C1C),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
