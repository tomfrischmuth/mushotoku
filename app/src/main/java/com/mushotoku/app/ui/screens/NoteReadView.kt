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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.theme.AppColors
import com.mushotoku.app.ui.theme.LocalAppColors

/**
 * Collects the rendered text and, alongside it, which source character every
 * rendered character came from — that is what lets a tap in the read view land
 * on the right spot once the editor opens.
 */
private class ReadViewBuilder {
    val sb = AnnotatedString.Builder()
    private val visToRaw = ArrayList<Int>()

    val length get() = sb.length

    /** Appends [text], whose characters map one for one starting at [rawStart]. */
    fun append(text: String, rawStart: Int) {
        text.forEachIndexed { i, c -> sb.append(c); visToRaw.add(rawStart + i) }
    }

    /** Appends decoration that has no counterpart in the source, all of it pointing at [rawAt]. */
    fun substitute(text: String, rawAt: Int) {
        text.forEach { sb.append(it); visToRaw.add(rawAt) }
    }

    fun <R> styled(style: SpanStyle, block: () -> R): R {
        sb.pushStyle(style); val r = block(); sb.pop(); return r
    }

    /** One entry per rendered character plus a final one for the end of the text. */
    fun mapping(rawLength: Int): IntArray = (visToRaw + rawLength).toIntArray()
}

private fun appendInlineReadMode(
    text: String,
    rawStart: Int,
    b: ReadViewBuilder,
    muted: Color,
    subtle: Color,
    codeBackground: Color
) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("***", i) -> {
                val close = text.indexOf("***", i + 3)
                if (close != -1) {
                    b.styled(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        b.append(text.substring(i + 3, close), rawStart + i + 3)
                    }
                    i = close + 3
                } else { b.append(text[i].toString(), rawStart + i); i++ }
            }
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close != -1) {
                    b.styled(SpanStyle(fontWeight = FontWeight.Bold)) {
                        b.append(text.substring(i + 2, close), rawStart + i + 2)
                    }
                    i = close + 2
                } else { b.append(text[i].toString(), rawStart + i); i++ }
            }
            text[i] == '*' -> {
                val close = text.indexOf('*', i + 1)
                if (close != -1) {
                    b.styled(SpanStyle(fontStyle = FontStyle.Italic)) {
                        b.append(text.substring(i + 1, close), rawStart + i + 1)
                    }
                    i = close + 1
                } else { b.append(text[i].toString(), rawStart + i); i++ }
            }
            text[i] == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close != -1) {
                    b.styled(SpanStyle(fontFamily = FontFamily.Monospace, color = subtle, background = codeBackground)) {
                        b.append(text.substring(i + 1, close), rawStart + i + 1)
                    }
                    i = close + 1
                } else { b.append(text[i].toString(), rawStart + i); i++ }
            }
            else -> { b.append(text[i].toString(), rawStart + i); i++ }
        }
    }
}

/** The rendered note plus the map from rendered offset back to source offset. */
internal class ReadView(val text: AnnotatedString, val visToRaw: IntArray) {
    /** Source offset for a tap that landed at [visibleOffset]. */
    fun rawOffset(visibleOffset: Int): Int = visToRaw[visibleOffset.coerceIn(0, visToRaw.size - 1)]
}

internal fun buildReadView(rawText: String, colors: AppColors): ReadView {
    val b      = ReadViewBuilder()
    val muted  = colors.onSurfaceTertiary
    val subtle = colors.onSurfaceSecondary
    val accent = NoteAccent
    var lineStart = 0
    rawText.lines().forEachIndexed { idx, line ->
        if (idx > 0) b.append("\n", lineStart - 1)
        val body = { prefix: Int, style: SpanStyle? ->
            val render = {
                appendInlineReadMode(
                    line.substring(prefix), lineStart + prefix, b, muted, subtle, colors.surfaceVariant
                )
            }
            if (style == null) render() else b.styled(style) { render() }
        }
        when {
            line.startsWith("# ")   -> body(2, SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
            line.startsWith("## ")  -> body(3, SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
            line.startsWith("### ") -> body(4, SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
            line.startsWith("> ")   -> body(2, SpanStyle(color = subtle, fontStyle = FontStyle.Italic))
            line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                val from = b.length
                b.styled(CheckboxStyle) { b.substitute("$CheckedBox ", lineStart) }
                body(6, SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough))
                b.sb.addStringAnnotation("checkbox", idx.toString(), from, b.length)
            }
            line.startsWith("- [ ] ") -> {
                val from = b.length
                b.styled(CheckboxStyle) { b.substitute("$EmptyBox ", lineStart) }
                body(6, null)
                b.sb.addStringAnnotation("checkbox", idx.toString(), from, b.length)
            }
            line.startsWith("- ") -> {
                b.styled(DashStyle) { b.append("- ", lineStart) }
                body(2, null)
            }
            line.startsWith("* ") -> {
                b.styled(BulletStyle) { b.substitute("$Bullet ", lineStart) }
                body(2, null)
            }
            numberedPrefixLength(line) > 0 -> {
                val n = numberedPrefixLength(line)
                b.styled(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                    b.append(line.substring(0, n), lineStart)
                }
                body(n, null)
            }
            line == "---" || line == "***" || line == "___" ->
                b.styled(SpanStyle(color = muted)) { b.substitute("──────────────", lineStart) }
            else -> body(0, null)
        }
        lineStart += line.length + 1
    }
    return ReadView(b.sb.toAnnotatedString(), b.mapping(rawText.length))
}

@Composable
internal fun NoteReadView(
    rawText: String,
    modifier: Modifier = Modifier,
    onToggleCheckbox: ((lineIndex: Int) -> Unit)? = null,
    onTapText: ((rawOffset: Int) -> Unit)? = null
) {
    val colors      = LocalAppColors.current
    val scrollState = rememberScrollState()
    val readView    = remember(rawText, colors) { buildReadView(rawText, colors) }
    val annotated   = readView.text

    if (onToggleCheckbox != null || onTapText != null) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text     = annotated,
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 16.dp)
                .pointerInput(annotated) {
                    detectTapGestures { tapOffset ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(tapOffset)
                            val checkbox = annotated
                                .getStringAnnotations("checkbox", offset, offset).firstOrNull()
                            when {
                                // Ticking off an item wins over opening the editor.
                                checkbox != null && onToggleCheckbox != null ->
                                    onToggleCheckbox(checkbox.item.toInt())
                                onTapText != null -> onTapText(readView.rawOffset(offset))
                            }
                        }
                    }
                },
            style        = TextStyle(fontSize = NoteBodySize, color = colors.onSurface, lineHeight = NoteBodyLineHeight),
            onTextLayout = { layoutResult = it }
        )
    } else {
        SelectionContainer {
            Text(
                text     = annotated,
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 16.dp),
                style = TextStyle(fontSize = NoteBodySize, color = colors.onSurface, lineHeight = NoteBodyLineHeight)
            )
        }
    }
}
