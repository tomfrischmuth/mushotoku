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

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.os.PersistableBundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mushotoku.app.R
import com.mushotoku.app.export.RecoveryCodePdf
import com.mushotoku.app.ui.components.rememberDocumentPicker
import com.mushotoku.app.ui.components.soundCheck
import com.mushotoku.app.ui.components.soundClick
import com.mushotoku.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Suggested in the save dialog; the user can rename it there. */
private const val PDF_FILE_NAME = "Mushotoku-Recovery-Code.pdf"

/**
 * One-time display of a freshly generated recovery code. The user must actively
 * confirm they have saved it before continuing; the code is never persisted in
 * readable form, so this is the only moment it is shown.
 */
@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val colors = LocalAppColors.current
    val s = remember(context) { recoveryStrings(context) }
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }
    val savePdf = rememberDocumentPicker(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { scope.launch { writeRecoveryPdf(context, code, it) } } }

    Dialog(
        onDismissRequest = { /* must be acknowledged */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.height(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = s.title,
                color = colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = s.message,
                color = colors.onSurfaceSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = code,
                color = colors.onSurface,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(vertical = 20.dp, horizontal = 12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = soundClick {
                    scope.launch { clipboard.setClipEntry(sensitiveClip(s.title, code).toClipEntry()) }
                }) {
                    Text(s.copy)
                }
                OutlinedButton(onClick = soundClick { savePdf(PDF_FILE_NAME) }) {
                    Text(s.savePdf)
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(checked = saved, onCheckedChange = soundCheck { saved = it })
                Spacer(Modifier.width(8.dp))
                Text(s.savedCheck, color = colors.onSurface, fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = soundClick(onDone),
                enabled = saved,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ),
            ) { Text(s.continueLabel) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Writes the sheet to the folder the user picked. Sharing the file instead would
 * only offer whatever app happens to accept a PDF — on a plain Android that is
 * the print service and Bluetooth, with no way to simply keep the file.
 */
private suspend fun writeRecoveryPdf(context: Context, code: String, target: Uri) {
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(target)?.use {
            RecoveryCodePdf.writeTo(context, code, it)
        }
    }
}

/**
 * The recovery code opens the whole database, so the clip is marked sensitive:
 * Android then keeps it out of the paste preview and out of clipboard history
 * instead of putting it on screen for everyone in the room to read.
 */
private fun sensitiveClip(label: String, code: String): ClipData =
    ClipData.newPlainText(label, code).apply {
        description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }

private class RecoveryStrings(
    val title: String,
    val message: String,
    val copy: String,
    val savePdf: String,
    val savedCheck: String,
    val continueLabel: String,
)

private fun recoveryStrings(ctx: Context) = RecoveryStrings(
    title = ctx.getString(R.string.recovery_setup_title),
    message = ctx.getString(R.string.recovery_setup_message),
    copy = ctx.getString(R.string.recovery_copy),
    savePdf = ctx.getString(R.string.recovery_save_pdf),
    savedCheck = ctx.getString(R.string.recovery_saved_check),
    continueLabel = ctx.getString(R.string.recovery_continue),
)
