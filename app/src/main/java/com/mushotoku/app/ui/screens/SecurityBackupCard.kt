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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.viewmodel.BackupViewModel
import com.mushotoku.app.viewmodel.ExportOutcome
import com.mushotoku.app.viewmodel.ImportOutcome
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun BackupCard(s: SecurityStrings) {
    val context = LocalContext.current
    val vm: BackupViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showExportPassword by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            exportUri = uri
            showExportPassword = true
        }
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importUri = uri
            showImportDialog = true
        }
    }

    SecCard {
        SecClickRow(
            title = s.createBackup,
            subtitle = s.createBackupHint,
            onClick = { resultMsg = null; createLauncher.launch(defaultBackupName()) },
        )
    }
    Spacer(Modifier.height(10.dp))
    SecCard {
        SecClickRow(
            title = s.restoreBackup,
            subtitle = s.restoreBackupHint,
            onClick = { resultMsg = null; openLauncher.launch(arrayOf("*/*")) },
        )
    }

    if (working) {
        Spacer(Modifier.height(10.dp))
        Text("…", color = colors.onSurfaceSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
    }
    resultMsg?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = colors.onSurfaceSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
    }

    if (showExportPassword) {
        NewPassphraseDialog(
            title = s.backupPasswordTitle,
            warning = s.backupPasswordWarning,
            newLabel = s.backupPassword,
            confirmLabel = s.confirmBackupPassword,
            s = s,
            onConfirm = { pw ->
                showExportPassword = false
                val uri = exportUri
                if (uri == null) {
                    pw.fill(' ')
                } else {
                    working = true
                    scope.launch {
                        val outcome = vm.export(context, uri, pw)
                        working = false
                        resultMsg = if (outcome is ExportOutcome.Success) s.backupExported
                        else (outcome as ExportOutcome.Error).message ?: s.backupExportFailed
                    }
                }
            },
            onDismiss = { showExportPassword = false },
        )
    }

    if (showImportDialog) {
        ImportPasswordModeDialog(
            s = s,
            onConfirm = { pw, replace ->
                showImportDialog = false
                val uri = importUri
                if (uri == null) {
                    pw.fill(' ')
                } else {
                    working = true
                    scope.launch {
                        val outcome = vm.import(context, uri, pw, replace)
                        working = false
                        resultMsg = when (outcome) {
                            ImportOutcome.Success -> s.backupImported
                            ImportOutcome.WrongPassword -> s.wrongBackupPassword
                            ImportOutcome.Corrupt -> s.corruptBackup
                            is ImportOutcome.Incompatible -> s.incompatibleBackup
                            is ImportOutcome.Error -> outcome.message ?: s.backupExportFailed
                        }
                    }
                }
            },
            onDismiss = { showImportDialog = false },
        )
    }
}

@Composable
private fun ImportPasswordModeDialog(
    s: SecurityStrings,
    onConfirm: (CharArray, replace: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var p by remember { mutableStateOf("") }
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.importModeTitle, color = colors.onSurface) },
        text = {
            Column {
                Text(s.importModeMessage, color = colors.onSurfaceSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                PassphraseField(p, { p = it }, s.enterBackupPassword)
            }
        },
        confirmButton = {
            TextButton(
                enabled = p.isNotEmpty(),
                onClick = soundClick { onConfirm(p.toCharArray(), true) },
            ) { Text(s.replaceData, color = if (p.isNotEmpty()) Color(0xFFFF6B6B) else colors.onSurfaceTertiary) }
        },
        dismissButton = {
            Row {
                TextButton(
                    enabled = p.isNotEmpty(),
                    onClick = soundClick { onConfirm(p.toCharArray(), false) },
                ) { Text(s.mergeData, color = if (p.isNotEmpty()) colors.accent else colors.onSurfaceTertiary) }
                TextButton(onClick = soundClick(onDismiss)) { Text(s.cancel, color = colors.onSurfaceSecondary) }
            }
        },
    )
}

private fun defaultBackupName(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    return "mushotoku-backup-$date.appbackup"
}
