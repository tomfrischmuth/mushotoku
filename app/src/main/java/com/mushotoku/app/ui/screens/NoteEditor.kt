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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.mushotoku.app.ui.components.soundClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(FlowPreview::class)
@Composable
internal fun NoteEditor(
    note: Note?,
    defaultType: NoteType = NoteType.NOTE,
    onCreate: (String, String, NoteType, (Note) -> Unit) -> Unit,
    onUpdate: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onClose: () -> Unit,
    bottomPad: Dp,
    topPad: Dp = 0.dp,
    onBarState: ((NoteEditorBarState) -> Unit)? = null,
    linkedTask: Task? = null,
    onNavigateToTask: (Task) -> Unit = {},
    hapticEnabled: Boolean = true,
    knownTags: List<String> = emptyList()
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val isNew = note == null

    // A brand-new note has no row yet; once autosave creates it we track the
    // persisted note here and switch to update-mode. `creating` guards against a
    // second insert while the first is still in flight (avoids duplicates).
    var currentNote by remember(note?.id) { mutableStateOf(note) }
    var creating    by remember(note?.id) { mutableStateOf(false) }

    var isEditing by remember { mutableStateOf(isNew) }

    // Title and body are kept apart. They used to share one text, where an
    // empty first line promoted the first body line to title — which quietly
    // took a checklist item out of the list.
    var titleText by remember(note?.id) {
        mutableStateOf(note?.title.orEmpty().stripHeadingMarker())
    }
    var text by remember(note?.id) {
        val body = note?.content.orEmpty()
        mutableStateOf(TextFieldValue(body, TextRange(body.length)))
    }

    val canUndo by remember { derivedStateOf { text.selection.start > 0 || !text.selection.collapsed } }

    // The stamp just written, so tapping again rewrites it instead of adding a
    // second one. It only counts while the cursor still sits right behind it.
    var stampAnchor by remember(note?.id) { mutableStateOf<StampAnchor?>(null) }

    fun renderStamp(at: LocalDateTime, withDate: Boolean): String {
        val time = at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(strings.locale))
        if (!withDate) return time
        val date = at.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(strings.locale))
        return "$date $time"
    }

    /**
     * Timestamps are what turns a note into a diary. Tapping stamps the time;
     * tapping again swaps that same stamp between time and date, so the button
     * corrects itself rather than piling stamps up.
     */
    fun insertTimestamp(withDate: Boolean) {
        val anchor = stampAnchor
        // A long press always wants the date; a tap flips to the other form.
        val nextWithDate = if (withDate) true else anchor?.let { !it.withDate } ?: false
        val rewritten = anchor?.let { toggleStamp(text, it, renderStamp(it.at, nextWithDate)) }

        if (rewritten != null && anchor != null) {
            text = rewritten
            stampAnchor = anchor.copy(text = renderStamp(anchor.at, nextWithDate), withDate = nextWithDate)
            if (hapticEnabled && nextWithDate) context.performCheckHaptic()
            return
        }

        val now = LocalDateTime.now()
        val stamp = renderStamp(now, withDate)
        if (hapticEnabled && withDate) context.performCheckHaptic()
        val inserted = insertAtCursor(text, stamp)
        text = inserted
        val start = inserted.text.lastIndexOf(stamp, inserted.selection.start)
        stampAnchor = if (start < 0) null else StampAnchor(start, stamp, now, withDate)
    }

    fun undo() {
        val new = deleteWordBackward(text)
        if (new.text != text.text || new.selection != text.selection) text = new
    }

    fun persist(rawInput: String) {
        // An item left empty is not saved, so no stray box survives the note.
        val saveContent = dropEmptyCheckItems(rawInput)
        val saveTitle   = titleText.trim()
        if (saveTitle.isBlank() && saveContent.isBlank()) return

        val existing = currentNote
        if (existing == null) {
            if (creating) return
            creating = true
            onCreate(saveTitle, saveContent, defaultType) { created ->
                currentNote = created
                creating = false
            }
        } else {
            if (saveTitle == existing.title && saveContent == existing.content) return
            onUpdate(existing.copy(title = saveTitle, content = saveContent))
        }
    }

    fun saveAndClose() {
        val body     = dropEmptyCheckItems(text.text)
        val isEmpty  = titleText.isBlank() && body.isBlank()
        val existing = currentNote
        when {
            // An untitled note without a single line is not worth keeping.
            isEmpty && existing != null -> onDelete(existing)
            !isEmpty                    -> persist(text.text)
        }
        onClose()
    }

    fun toggleCheckbox(lineIndex: Int) {
        val lines = text.text.lines().toMutableList()
        if (lineIndex >= lines.size) return
        val line = lines[lineIndex]
        val state = checkStateOf(line) ?: return
        // Reaching green is the moment worth feeling.
        if (hapticEnabled && state.next() == CheckState.DONE) context.performCheckHaptic()
        lines[lineIndex] = cycleCheckLine(line)
        val newText = lines.joinToString("\n")
        text = TextFieldValue(newText, TextRange(text.selection.start.coerceIn(0, newText.length)))
    }

    BackHandler { saveAndClose() }

    // Debounced autosave for new and existing notes alike. For a new note the
    // first non-empty content promotes it to a real DB row (see persist()).
    LaunchedEffect(note?.id) {
        snapshotFlow { titleText to text.text }
            .drop(1)
            .debounce(700)
            .collect { (_, body) -> persist(body) }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditing) {
        if (isEditing) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        } else {
            keyboard?.hide()
            persist(text.text)
        }
    }

    val currentLine = remember(text.selection.start, text.text) {
        val cursor = text.selection.start.coerceIn(0, text.text.length)
        val lineStart = text.text.lastIndexOf('\n', cursor - 1) + 1
        val lineEnd = text.text.indexOf('\n', cursor).let { if (it == -1) text.text.length else it }
        text.text.substring(lineStart, lineEnd)
    }

    val noteTitle = titleText.trim()
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
                onApplyNumbered = { text = applyNumberedPrefix(text) },
                onInsertTimestamp = ::insertTimestamp,
                onUndo        = ::undo
            )
        }
        if (isEditing) {
            val prefix = tagPrefixAt(text.text, text.selection.start)
            val suggestions = remember(prefix, knownTags) {
                if (prefix == null) emptyList() else tagSuggestions(knownTags, prefix)
            }
            // Only once the tag is finished, so the offer does not flicker in
            // while it is still being typed.
            val removable = prefix == null && tagRangeAt(text.text, text.selection.start) != null
            TagSuggestionRow(
                suggestions = suggestions,
                showRemove  = removable,
                onPick      = { tag -> text = completeTag(text, tag) },
                onRemove    = { text = unmarkTag(text) }
            )
        }
        if (linkedTask != null) {
            AppointmentLinkChip(
                task     = linkedTask,
                onClick  = { persist(text.text); onNavigateToTask(linkedTask) }
            )
        }
        NoteTitleField(
            value    = titleText,
            onChange = { titleText = it },
            editable = isEditing
        )
        if (isEditing) {
            NoteMarkdownEditField(
                value          = text,
                onValueChange  = { new ->
                    val continued = if (new.text != text.text) autoContinueList(text, new) else new
                    text = lowercaseTags(continued)
                },
                focusRequester = focusRequester,
                modifier       = Modifier.weight(1f)
            )
        } else {
            NoteReadView(
                rawText          = text.text,
                modifier         = Modifier.weight(1f),
                onToggleCheckbox = ::toggleCheckbox,
                // Tapping the note opens the editor right where the finger landed.
                onTapText        = { rawOffset ->
                    text = text.copy(
                        selection = TextRange(rawOffset.coerceIn(0, text.text.length))
                    )
                    isEditing = true
                }
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
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        onTextLayout  = { layout = it },
        modifier      = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NoteBodyPaddingH)
            .padding(top = NoteBodyPaddingTop, bottom = NoteBodyPaddingBottom)
            .drawBehind {
                val result = layout ?: return@drawBehind
                drawTagPills(result, colors.surfaceVariant, NoteBodySize)
                drawCheckBoxes(result, checkStatesIn(value.text), NoteBodySize.toPx())
            }
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize   = NoteBodySize,
            color      = colors.onSurface,
            lineHeight = NoteBodyLineHeight
        ),
        cursorBrush          = SolidColor(NoteAccent),
        visualTransformation = MarkdownVisualTransformation(colors, cursorLine),
        decorationBox        = { inner ->
            if (value.text.isEmpty()) {
                Text(
                    text  = strings.notesContentHint,
                    style = TextStyle(fontSize = NoteBodySize, color = colors.onSurfaceTertiary, lineHeight = NoteBodyLineHeight)
                )
            }
            inner()
        }
    )
}

/**
 * Offers the existing tags that continue what is being typed. It only appears
 * while a tag is open at the cursor, so carrying on writing a new tag of one's
 * own is never interrupted.
 */
@Composable
private fun TagSuggestionRow(
    suggestions: List<String>,
    showRemove: Boolean,
    onPick: (String) -> Unit,
    onRemove: () -> Unit
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    AnimatedVisibility(visible = suggestions.isNotEmpty() || showRemove) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = NoteBodyPaddingH, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showRemove) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.surfaceVariant)
                        // LocalIndication already sounds the tap; soundClick would double it.
                        .clickable(onClick = onRemove)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text     = "✕  ${strings.notesTagRemove}",
                        fontSize = 13.sp,
                        color    = colors.onSurfaceSecondary,
                        maxLines = 1
                    )
                }
            }
            suggestions.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.surfaceVariant)
                        .clickable { onPick(tag) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text     = "#$tag",
                        fontSize = 13.sp,
                        color    = colors.onSurfaceSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * The note's title, on its own line above the body. Keeping it separate is what
 * stops the first body line from being pulled up into the title.
 */
@Composable
private fun NoteTitleField(
    value: String,
    onChange: (String) -> Unit,
    editable: Boolean
) {
    val colors  = LocalAppColors.current
    val strings = LocalAppStrings.current
    val style = TextStyle(
        fontSize   = 27.sp,
        fontWeight = FontWeight.Bold,
        color      = colors.onSurface
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = NoteBodyPaddingH)
            .padding(top = NoteBodyPaddingTop, bottom = 4.dp)
    ) {
        if (editable) {
            BasicTextField(
                value         = value,
                onValueChange = { onChange(it.replace("\n", "")) },
                textStyle     = style,
                singleLine    = true,
                cursorBrush   = SolidColor(NoteAccent),
                modifier      = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(strings.notesNoTitle, style = style.copy(color = colors.onSurfaceTertiary))
                    }
                    inner()
                }
            )
        } else {
            Text(
                text  = value.ifBlank { strings.notesNoTitle },
                style = if (value.isBlank()) style.copy(color = colors.onSurfaceTertiary) else style
            )
        }
    }
}
