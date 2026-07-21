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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

/**
 * Below this gap between two deletions the key is being held, not pressed again.
 * Android repeats at roughly 50 ms once the initial delay is over, while two
 * deliberate presses are far slower than this.
 */
private const val HeldKeyMillis = 250L

@OptIn(FlowPreview::class)
@Composable
internal fun NoteEditor(
    note: Note?,
    defaultType: NoteType = NoteType.NOTE,
    onCreate: (String, String, NoteType, (Note) -> Unit) -> Unit,
    onUpdate: (Note) -> Unit,
    /** Saves without moving the note to the top; used for a mere tick. */
    onUpdateQuiet: (Note) -> Unit = onUpdate,
    onDelete: (Note) -> Unit,
    onClose: () -> Unit,
    bottomPad: Dp,
    topPad: Dp = 0.dp,
    onBarState: ((NoteEditorBarState) -> Unit)? = null,
    linkedTask: Task? = null,
    onNavigateToTask: (Task) -> Unit = {},
    hapticEnabled: Boolean = true,
    knownTags: List<String> = emptyList(),
    startInTitle: Boolean = true,
    /** The note's colour, used for the editor's controls. */
    accent: Color = NoteAccent
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

    // Start of the line last written in. Deleting a whole item is meant for
    // tidying up a list, not for correcting the entry being typed, so the line
    // the writing is happening in is left out of it until the caret moves away.
    var typedLineStart by remember(note?.id) { mutableStateOf<Int?>(null) }

    // When the last deletion came in, to tell a held backspace from a pressed
    // one. Auto-repeat would otherwise clear item after item in one sweep.
    var lastDeleteAt by remember(note?.id) { mutableLongStateOf(0L) }

    // The stamp just written, so tapping again rewrites it instead of adding a
    // second one. It only counts while the cursor still sits right behind it.
    var stampAnchor by remember(note?.id) { mutableStateOf<StampAnchor?>(null) }

    // Set while the only change so far is a tick, so the note keeps its place
    // in the list. Any typing clears it again.
    var tickOnly by remember(note?.id) { mutableStateOf(false) }

    // Set when the editor was opened by tapping the title, so the caret starts
    // there rather than in the text — at the letter that was tapped.
    var editTitle  by remember(note?.id) { mutableStateOf(false) }
    var titleCaret by remember(note?.id) { mutableStateOf<Int?>(null) }

    fun renderStamp(at: LocalDateTime, withDate: Boolean): String {
        val time = at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(strings.locale))
        if (!withDate) return time
        val date = at.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(strings.locale))
        return date + StampSeparator + time
    }

    /**
     * Timestamps are what turns a note into a diary. The button runs through
     * the same three states a list marker does: time, date and time, then gone
     * again — so a stamp written by mistake needs no deleting by hand.
     */
    fun insertTimestamp(withDate: Boolean) {
        val anchor = stampAnchor
        // The third tap takes the stamp back out; a long press wants the date
        // whatever came before.
        if (!withDate && anchor != null && anchor.withDate) {
            val cleared = removeStamp(text, anchor)
            if (cleared != null) {
                text = cleared
                stampAnchor = null
                return
            }
        }
        // A long press always wants the date; a tap flips to the other form.
        val nextWithDate = if (withDate) true else anchor?.let { !it.withDate } ?: false

        if (anchor != null) {
            val stamp = renderStamp(anchor.at, nextWithDate)
            val rewritten = toggleStamp(text, anchor, stamp)
            if (rewritten != null) {
                text = rewritten
                stampAnchor = anchor.copy(text = stamp, withDate = nextWithDate)
                if (hapticEnabled && nextWithDate) context.performCheckHaptic()
                return
            }
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
            // Old notes still store their heading marker, so compare without it:
            // otherwise merely opening a note rewrites it and sorts it to the top.
            val unchanged = saveTitle == existing.title.stripHeadingMarker() &&
                saveContent == existing.content
            if (unchanged) return
            val saved = existing.copy(title = saveTitle, content = saveContent)
            if (tickOnly) onUpdateQuiet(saved) else onUpdate(saved)
            tickOnly = false
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
        tickOnly = true
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
    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditing) {
        if (isEditing) {
            // A brand-new note starts where the setting says; an existing one
            // always opens in the text, where the work is.
            val target = if (editTitle || (isNew && startInTitle)) titleFocusRequester else focusRequester
            try { target.requestFocus() } catch (_: Exception) {}
            editTitle = false
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
                accent        = accent,
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
        // Title and text share one scroll container: the title belongs to the
        // note, not to the chrome, so it scrolls away with the text.
        BoxWithConstraints(Modifier.weight(1f)) {
        val viewportHeight = maxHeight
        var headerPx by remember { mutableStateOf(0) }
        val headerHeight = with(LocalDensity.current) { headerPx.toDp() }
        Column(Modifier.verticalScroll(rememberScrollState())) {
        Box(Modifier.onSizeChanged { headerPx = it.height }) {
        NoteTitleField(
            value          = titleText,
            onChange       = { titleText = it },
            editable       = isEditing,
            focusRequester = titleFocusRequester,
            // Enter in the title carries on into the text, as one would expect
            // from a line that is followed by the note itself.
            onNext         = { try { focusRequester.requestFocus() } catch (_: Exception) {} },
            caret          = titleCaret,
            onCaretUsed    = { titleCaret = null },
            onTap          = { at -> titleCaret = at; editTitle = true; isEditing = true }
        )
        }
        // The text reaches at least to the bottom of the screen, so tapping the
        // empty space below the last line lands in the text and not nowhere.
        val bodyMinHeight = (viewportHeight - headerHeight).coerceAtLeast(0.dp)
        if (isEditing) {
            NoteMarkdownEditField(
                accent         = accent,
                value          = text,
                onValueChange  = { new ->
                    // A backspace at the end of an item clears its text in one go
                    // — but never in the line that is currently being written in,
                    // and never off a held key, which would run through the whole
                    // list item by item.
                    val caretLine = lineStartOf(new.text, new.selection.start)
                    when {
                        new.text.length > text.text.length -> typedLineStart = caretLine
                        typedLineStart != caretLine -> typedLineStart = null
                    }
                    val now = System.currentTimeMillis()
                    val heldDown = now - lastDeleteAt < HeldKeyMillis
                    if (new.text.length < text.text.length) lastDeleteAt = now

                    val cleared = deleteCheckItemText(text, new)
                        ?.takeIf { typedLineStart == null && !heldDown }

                    val unboxed = deleteCheckMarker(text, new)
                    val joined  = joinCheckItemUp(text, new)
                    if (new.text != text.text) tickOnly = false
                    val next = when {
                        cleared != null -> cleared
                        unboxed != null -> unboxed
                        joined != null -> joined
                        new.text != text.text -> lowercaseTags(autoContinueList(text, new))
                        else -> lowercaseTags(new)
                    }
                    // A tap in the middle of the markup would otherwise leave the
                    // caret inside the marker, where nothing can be written. The
                    // start of the line stays reachable: a backspace there joins
                    // the item to the line above.
                    text = if (next.selection.collapsed && next.text == text.text) {
                        val moved = clampOutOfCheckMarker(
                            next.text, next.selection.start, allowLineStart = true
                        )
                        if (moved == next.selection.start) next
                        else next.copy(selection = TextRange(moved))
                    } else next
                },
                focusRequester = focusRequester,
                modifier       = Modifier.fillMaxWidth().heightIn(min = bodyMinHeight)
            )
        } else {
            NoteReadView(
                rawText          = text.text,
                accent           = accent,
                modifier         = Modifier.fillMaxWidth().heightIn(min = bodyMinHeight),
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
    }
}

@Composable
private fun NoteMarkdownEditField(
    accent: Color,
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
        cursorBrush          = SolidColor(accent),
        visualTransformation = MarkdownVisualTransformation(colors, cursorLine, accent),
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
 * The note's title, on its own line above the body. Keeping it separate is what
 * stops the first body line from being pulled up into the title.
 */
@Composable
private fun NoteTitleField(
    value: String,
    onChange: (String) -> Unit,
    editable: Boolean,
    focusRequester: FocusRequester,
    onNext: () -> Unit,
    /** Where a tap in the read view landed, so the caret starts there. */
    caret: Int? = null,
    onCaretUsed: () -> Unit = {},
    onTap: (Int) -> Unit = {}
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
            // The field keeps its own selection; the editor only knows the text.
            var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
            if (field.text != value) field = field.copy(text = value)
            LaunchedEffect(caret) {
                val at = caret ?: return@LaunchedEffect
                field = field.copy(selection = TextRange(at.coerceIn(0, field.text.length)))
                onCaretUsed()
            }
            BasicTextField(
                value         = field,
                onValueChange = { typed ->
                    // The title wraps, so Enter would insert a line break; it is
                    // taken as the step into the text instead.
                    if (typed.text.contains('\n')) {
                        val cleaned = typed.text.replace("\n", "")
                        field = typed.copy(
                            text      = cleaned,
                            selection = TextRange(typed.selection.start.coerceIn(0, cleaned.length))
                        )
                        onChange(cleaned)
                        onNext()
                    } else {
                        field = typed
                        onChange(typed.text)
                    }
                },
                textStyle     = style,
                cursorBrush     = SolidColor(NoteAccent),
                modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester),
                decorationBox = { inner ->
                    if (field.text.isEmpty()) {
                        Text(strings.notesNoTitle, style = style.copy(color = colors.onSurfaceTertiary))
                    }
                    inner()
                }
            )
        } else {
            // Reading the title and wanting to change it is the same motion as
            // with the text below: the tap opens it, at the letter it landed on.
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                text  = value.ifBlank { strings.notesNoTitle },
                style = if (value.isBlank()) style.copy(color = colors.onSurfaceTertiary) else style,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(value) {
                        detectTapGestures { pos ->
                            val at = layout?.getOffsetForPosition(pos) ?: value.length
                            onTap(at.coerceIn(0, value.length))
                        }
                    }
            )
        }
    }
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
