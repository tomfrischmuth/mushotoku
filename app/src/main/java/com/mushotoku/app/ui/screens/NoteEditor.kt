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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.data.Note
import com.mushotoku.app.data.NoteType
import com.mushotoku.app.data.Task
import com.mushotoku.app.ui.strings.LocalAppStrings
import com.mushotoku.app.ui.theme.LocalAppColors
import com.mushotoku.app.util.performCheckHaptic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(FlowPreview::class)
@Composable
internal fun NoteEditor(
    note: Note?,
    defaultType: NoteType = NoteType.NOTE,
    onAdd: (String, String, NoteType) -> Unit,
    onUpdate: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onClose: () -> Unit,
    bottomPad: Dp,
    topPad: Dp = 0.dp,
    onBarState: ((NoteEditorBarState) -> Unit)? = null,
    linkedTask: Task? = null,
    onNavigateToTask: (Task) -> Unit = {},
    hapticEnabled: Boolean = true
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val isNew = note == null

    var isEditing by remember { mutableStateOf(isNew) }

    val initialText = remember(note?.id) {
        if (note == null) {
            "# "
        } else {
            if (note.content.isNotEmpty()) "${note.title}\n${note.content}" else note.title
        }
    }
    var text by remember(note?.id) {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }

    val canUndo by remember { derivedStateOf { text.selection.start > 0 || !text.selection.collapsed } }

    fun undo() {
        val new = deleteWordBackward(text)
        if (new.text != text.text || new.selection != text.selection) text = new
    }

    fun extractParts(raw: String): Pair<String, String> {
        val nl = raw.indexOf('\n')
        val title   = if (nl >= 0) raw.substring(0, nl) else raw
        val content = if (nl >= 0) raw.substring(nl + 1) else ""
        return title to content
    }

    fun persist(raw: String) {
        val (rawTitle, content) = extractParts(raw)
        val titleStripped = rawTitle
            .removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()

        val saveTitle: String
        val saveContent: String
        if (titleStripped.isNotBlank()) {
            saveTitle   = rawTitle.trim()
            saveContent = content
        } else {
            val lines = content.lines()
            val idx   = lines.indexOfFirst { it.isNotBlank() }
            if (idx < 0) return
            saveTitle   = lines[idx].trim()
            saveContent = lines.drop(idx + 1).joinToString("\n")
        }

        if (isNew) onAdd(saveTitle, saveContent, defaultType)
        else {
            if (saveTitle == note!!.title && saveContent == note.content) return
            onUpdate(note.copy(title = saveTitle, content = saveContent))
        }
    }

    fun saveAndClose() {
        val (rawTitle, content) = extractParts(text.text)
        val titleStripped = rawTitle
            .removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()
        val effectiveTitle = titleStripped.ifBlank { content.lines().firstOrNull(String::isNotBlank) }
        when {
            effectiveTitle == null && !isNew -> onDelete(note!!)
            effectiveTitle != null           -> persist(text.text)
        }
        onClose()
    }

    fun toggleCheckbox(lineIndex: Int) {
        val lines = text.text.lines().toMutableList()
        if (lineIndex >= lines.size) return
        val line = lines[lineIndex]
        lines[lineIndex] = when {
            line.startsWith("- [ ] ") -> {
                if (hapticEnabled) context.performCheckHaptic()
                line.replaceFirst("- [ ] ", "- [x] ")
            }
            line.startsWith("- [x] ") || line.startsWith("- [X] ") ->
                line.replace(Regex("^- \\[[xX]\\] "), "- [ ] ")
            else -> return
        }
        val newText = lines.joinToString("\n")
        text = TextFieldValue(newText, TextRange(text.selection.start.coerceIn(0, newText.length)))
    }

    BackHandler { saveAndClose() }

    if (!isNew) {
        LaunchedEffect(note!!.id) {
            snapshotFlow { text.text }
                .drop(1)
                .debounce(700)
                .collect { persist(it) }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditing) {
        if (isEditing) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            keyboard?.hide()
            if (!isNew) persist(text.text)
        }
    }

    val currentLine = remember(text.selection.start, text.text) {
        val cursor = text.selection.start.coerceIn(0, text.text.length)
        val lineStart = text.text.lastIndexOf('\n', cursor - 1) + 1
        val lineEnd = text.text.indexOf('\n', cursor).let { if (it == -1) text.text.length else it }
        text.text.substring(lineStart, lineEnd)
    }

    val noteTitle = remember(text.text) {
        text.text.lines().firstOrNull().orEmpty()
            .removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()
    }
    SideEffect {
        onBarState?.invoke(NoteEditorBarState(
            title     = noteTitle,
            noteType  = note?.type ?: defaultType,
            isEditing = isEditing,
            onBack    = ::saveAndClose,
            onToggle  = { isEditing = !isEditing }
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = topPad)
            .then(if (isEditing) Modifier.imePadding() else Modifier)
    ) {
        if (isEditing) {
            FormattingToolbar(
                currentLine   = currentLine,
                canUndo       = canUndo,
                onApplyPrefix = { prefix -> text = applyLinePrefix(text, prefix) },
                onApplyInline = { marker -> text = applyInlineFormat(text, marker) },
                onUndo        = ::undo
            )
        }
        if (linkedTask != null) {
            AppointmentLinkChip(
                task     = linkedTask,
                onClick  = { persist(text.text); onNavigateToTask(linkedTask) }
            )
        }
        if (isEditing) {
            NoteMarkdownEditField(
                value          = text,
                onValueChange  = { new ->
                    text = if (new.text != text.text) autoContinueList(text, new) else new
                },
                focusRequester = focusRequester,
                modifier       = Modifier.weight(1f)
            )
        } else {
            NoteReadView(
                rawText          = text.text,
                modifier         = Modifier.weight(1f),
                onToggleCheckbox = ::toggleCheckbox
            )
        }

        Spacer(Modifier.height(bottomPad))
    }
}

@Composable
private fun NoteMarkdownEditField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    val cursorLine = remember(value.selection.start, value.text) {
        val pos = value.selection.start.coerceIn(0, value.text.length)
        value.text.substring(0, pos).count { it == '\n' }
    }
    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp)
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize   = 16.sp,
            color      = colors.onSurface,
            lineHeight = 28.sp
        ),
        cursorBrush          = SolidColor(NoteAccent),
        visualTransformation = MarkdownVisualTransformation(colors, cursorLine),
        decorationBox        = { inner ->
            if (value.text.isEmpty()) {
                Text(
                    text  = strings.notesContentHint,
                    style = TextStyle(fontSize = 16.sp, color = colors.onSurfaceTertiary, lineHeight = 28.sp)
                )
            }
            inner()
        }
    )
}
