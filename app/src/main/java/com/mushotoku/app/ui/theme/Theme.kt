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

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import com.mushotoku.app.ui.components.AppClickSoundIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

@Composable
fun MushotokuTheme(
    themeMode: String = "SYSTEM",
    fontScale: Float  = 1.0f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "DARK"  -> true
        "LIGHT" -> false
        else    -> systemDark
    }

    val appBlue = Color(0xFF3D5AFE)
    val appColors   = if (useDark) DarkAppColors else LightAppColors
    val m3Scheme    = if (useDark)
        darkColorScheme(primary = appBlue, onPrimary = Color.White, secondary = appBlue, onSecondary = Color.White)
    else
        lightColorScheme(primary = appBlue, onPrimary = Color.White, secondary = appBlue, onSecondary = Color.White)
    val baseDensity = LocalDensity.current
    val view        = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalDensity provides Density(baseDensity.density, fontScale)
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography  = Typography
        ) {
            // Must be INSIDE MaterialTheme: it re-provides LocalIndication (ripple),
            // so providing ours outside would be overridden.
            CompositionLocalProvider(LocalIndication provides AppClickSoundIndication) {
                content()
            }
        }
    }
}
