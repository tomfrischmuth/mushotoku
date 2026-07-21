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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import com.mushotoku.app.security.SecurityGate

/**
 * A launcher for one of the system's document dialogs that the lock gate does not
 * mistake for the user leaving.
 *
 * Picking a file runs in a separate system activity, so the app goes through
 * onStop and comes back to an immediate relock — in the middle of writing down a
 * recovery code, for instance, where the app lock is not even fully set up yet.
 * This is the same case [SecurityGate.authInProgress] already covers for the
 * biometric prompt.
 *
 * Returns the launch function rather than the launcher itself, so there is no
 * second way to open the dialog that skips the flag.
 */
@Composable
internal fun <I, O> rememberDocumentPicker(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): (I) -> Unit {
    val launcher = rememberLauncherForActivityResult(contract) { result ->
        SecurityGate.authInProgress = false
        onResult(result)
    }
    return { input ->
        SecurityGate.authInProgress = true
        launcher.launch(input)
    }
}
