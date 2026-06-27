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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.AppDatabase
import com.mushotoku.app.export.CalendarPdfExporter
import com.mushotoku.app.export.ExportCategory
import com.mushotoku.app.export.ExportFormat
import com.mushotoku.app.export.ExportOptions
import com.mushotoku.app.export.Exporter
import com.mushotoku.app.export.FinanceReportPdfExporter
import com.mushotoku.app.export.JournalPdfExporter
import com.mushotoku.app.export.NotesZipExporter
import com.mushotoku.app.export.extension
import com.mushotoku.app.viewmodel.ExportViewModel
import com.mushotoku.app.ui.theme.LocalAppColors
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ExportSection() {
    val strings = LocalAppStrings.current
    val colors  = LocalAppColors.current
    val context = LocalContext.current
    val currency = LocalAppCurrency.current
    val es      = exportStrings(context)
    val scope   = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val vm: ExportViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    val exporters = remember(strings.locale, currency) {
        val db = AppDatabase.getInstance(context)
        val s  = exportStrings(context)
        listOf(
            CalendarPdfExporter(context, db, s, strings.locale),
            JournalPdfExporter(context, db, s, strings.locale),
            NotesZipExporter(context, db, s, strings.locale),
            FinanceReportPdfExporter(context, db, s, strings, currency, strings.locale),
        )
    }
    fun exporterFor(category: ExportCategory) = exporters.first { it.category == category }

    var pending by remember { mutableStateOf<Triple<Exporter, ExportFormat, ExportOptions>?>(null) }
    var dialogExporter by remember { mutableStateOf<Exporter?>(null) }
    var includeMood by remember { mutableStateOf(false) }

    val onSafResult: (android.net.Uri?) -> Unit = { uri ->
        val p = pending
        pending = null
        if (uri != null && p != null) vm.export(p.first, p.second, p.third, uri)
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { onSafResult(it) }
    val zipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { onSafResult(it) }

    fun startExport(exporter: Exporter, format: ExportFormat, options: ExportOptions) {
        dialogExporter = null
        pending = Triple(exporter, format, options)
        val name = "${exporter.defaultBaseName()}_${java.time.LocalDate.now()}.${format.extension()}"
        if (format == ExportFormat.PDF) pdfLauncher.launch(name) else zipLauncher.launch(name)
    }

    fun onCategoryTap(category: ExportCategory) {
        val exporter = exporterFor(category)
        scope.launch {
            val has = withContext(Dispatchers.IO) { exporter.hasData() }
            if (!has) {
                snackbar.showSnackbar(es.emptyData)
                return@launch
            }
            if (exporter.supportedFormats() == listOf(ExportFormat.PDF) && category != ExportCategory.JOURNAL) {
                startExport(exporter, ExportFormat.PDF, ExportOptions())
            } else {
                includeMood = false
                dialogExporter = exporter
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ExportViewModel.UiState.Success -> {
                val result = snackbar.showSnackbar(
                    message = es.successPhrases.random(),
                    actionLabel = es.shareAction,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = s.mimeType
                        putExtra(Intent.EXTRA_STREAM, s.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, es.shareAction))
                }
                vm.consume()
            }
            is ExportViewModel.UiState.Error -> {
                snackbar.showSnackbar(s.message?.let { "${es.errorGeneric} $it" } ?: es.errorGeneric)
                vm.consume()
            }
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(es.sectionTitle)
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ExportCategoryRow(es.calendarTitle, es.calendarDesc) { onCategoryTap(ExportCategory.KALENDER) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                    ExportCategoryRow(es.journalTitle, es.journalDesc) { onCategoryTap(ExportCategory.JOURNAL) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                    ExportCategoryRow(es.notesTitle, es.notesDesc) { onCategoryTap(ExportCategory.NOTIZEN) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = colors.divider)
                    ExportCategoryRow(es.financeTitle, es.financeDesc) { onCategoryTap(ExportCategory.FINANZBERICHT) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    val de = dialogExporter
    if (de != null) {
        ExportFormatDialog(
            exporter = de,
            es = es,
            includeMood = includeMood,
            onIncludeMoodChange = { includeMood = it },
            onPickFormat = { fmt -> startExport(de, fmt, ExportOptions(includeMood = includeMood)) },
            onDismiss = { dialogExporter = null }
        )
    }

    (uiState as? ExportViewModel.UiState.Running)?.let { running ->
        val text = if (running.isNotes && running.total > 0)
            "${es.progressNotesPrefix} ${running.current} / ${running.total}"
        else es.progressGeneric
        GlassAlertDialog(
            onDismissRequest = { vm.cancel() },
            title = { Text(text) },
            dismissButton = {
                TextButton(onClick = soundClick { vm.cancel() }) { Text(es.cancelAction) }
            }
        )
    }
}

@Composable
private fun ExportCategoryRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = colors.onSurface)
            Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.onSurfaceTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ExportFormatDialog(
    exporter: Exporter,
    es: ExportStrings,
    includeMood: Boolean,
    onIncludeMoodChange: (Boolean) -> Unit,
    onPickFormat: (ExportFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val formats = exporter.supportedFormats()
    val singlePdf = formats == listOf(ExportFormat.PDF)
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(es.formatDialogTitle) },
        text = {
            Column {
                if (exporter.category == ExportCategory.JOURNAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(es.includeMoodLabel, color = colors.onSurface, modifier = Modifier.weight(1f))
                        Switch(checked = includeMood, onCheckedChange = soundCheck(onIncludeMoodChange))
                    }
                }
                if (!singlePdf) {
                    formats.forEach { fmt ->
                        Text(
                            text = when (fmt) {
                                ExportFormat.PDF -> es.formatPdf
                                ExportFormat.ZIP_TXT -> es.formatZipTxt
                                ExportFormat.ZIP_PDF -> es.formatZipPdf
                            },
                            color = colors.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPickFormat(fmt) }
                                .padding(vertical = 14.dp)
                        )
                    }
                }
            }
        },
        confirmButton = if (singlePdf) {
            { TextButton(onClick = soundClick { onPickFormat(ExportFormat.PDF) }) { Text(es.exportAction) } }
        } else null,
        dismissButton = { TextButton(onClick = soundClick(onDismiss)) { Text(es.cancelAction) } }
    )
}
