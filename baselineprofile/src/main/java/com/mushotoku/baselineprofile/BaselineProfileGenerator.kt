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

package com.mushotoku.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Erzeugt das Baseline-Profil für com.mushotoku.app.
 *
 * Ausführen mit angeschlossenem Gerät/Emulator (API 36+):
 *   ./gradlew :app:generateReleaseBaselineProfile
 *
 * Das Ergebnis landet in app/src/release/generated/baselineProfiles/ und wird
 * beim Release-Build automatisch eingebettet (profileinstaller).
 *
 * Aktuell wird nur der Cold-Start-Pfad erfasst (Application-Init inkl. nativem
 * SQLCipher-Load, Compose-Setup, Lock-Gate, erstes Frame) – der mit Abstand
 * wertvollste Teil. Zum Erweitern auf Scroll-Journeys nach startActivityAndWait()
 * die jeweiligen Screens ansteuern (z. B. via device.findObject(...) + scroll).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.mushotoku.app",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()
    }
}
