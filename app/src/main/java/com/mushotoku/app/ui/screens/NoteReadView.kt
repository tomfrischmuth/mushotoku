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

private fun appendInlineReadMode(
    text: String,
    sb: AnnotatedString.Builder,
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
                    sb.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    sb.append(text.substring(i + 3, close))
                    sb.pop()
                    i = close + 3
                } else { sb.append(text[i]); i++ }
            }
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close != -1) {
                    sb.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    sb.append(text.substring(i + 2, close))
                    sb.pop()
                    i = close + 2
                } else { sb.append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val close = text.indexOf('*', i + 1)
                if (close != -1) {
                    sb.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    sb.append(text.substring(i + 1, close))
                    sb.pop()
                    i = close + 1
                } else { sb.append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close != -1) {
                    sb.pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = subtle, background = codeBackground))
                    sb.append(text.substring(i + 1, close))
                    sb.pop()
                    i = close + 1
                } else { sb.append(text[i]); i++ }
            }
            else -> { sb.append(text[i]); i++ }
        }
    }
}

private fun buildReadViewAnnotatedString(rawText: String, colors: AppColors): AnnotatedString {
    val sb     = AnnotatedString.Builder()
    val muted  = colors.onSurfaceTertiary
    val subtle = colors.onSurfaceSecondary
    val accent = NoteAccent
    rawText.lines().forEachIndexed { idx, line ->
        if (idx > 0) sb.append('\n')
        when {
            line.startsWith("# ") -> {
                sb.pushStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
                appendInlineReadMode(line.substring(2), sb, muted, subtle, colors.surfaceVariant)
                sb.pop()
            }
            line.startsWith("## ") -> {
                sb.pushStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                appendInlineReadMode(line.substring(3), sb, muted, subtle, colors.surfaceVariant)
                sb.pop()
            }
            line.startsWith("### ") -> {
                sb.pushStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
                appendInlineReadMode(line.substring(4), sb, muted, subtle, colors.surfaceVariant)
                sb.pop()
            }
            line.startsWith("> ") -> {
                sb.pushStyle(SpanStyle(color = subtle, fontStyle = FontStyle.Italic))
                appendInlineReadMode(line.substring(2), sb, muted, subtle, colors.surfaceVariant)
                sb.pop()
            }
            line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                val lineStart = sb.length
                sb.pushStyle(SpanStyle(color = accent)); sb.append("☑ "); sb.pop()
                sb.pushStyle(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough))
                appendInlineReadMode(line.substring(6), sb, muted, subtle, colors.surfaceVariant)
                sb.pop()
                sb.addStringAnnotation("checkbox", idx.toString(), lineStart, sb.length)
            }
            line.startsWith("- [ ] ") -> {
                val lineStart = sb.length
                sb.pushStyle(SpanStyle(color = accent)); sb.append("☐ "); sb.pop()
                appendInlineReadMode(line.substring(6), sb, muted, subtle, colors.surfaceVariant)
                sb.addStringAnnotation("checkbox", idx.toString(), lineStart, sb.length)
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                sb.pushStyle(SpanStyle(color = accent)); sb.append("• "); sb.pop()
                appendInlineReadMode(line.substring(2), sb, muted, subtle, colors.surfaceVariant)
            }
            line == "---" || line == "***" || line == "___" -> {
                sb.pushStyle(SpanStyle(color = muted)); sb.append("──────────────"); sb.pop()
            }
            else -> appendInlineReadMode(line, sb, muted, subtle, colors.surfaceVariant)
        }
    }
    return sb.toAnnotatedString()
}

@Composable
internal fun NoteReadView(
    rawText: String,
    modifier: Modifier = Modifier,
    onToggleCheckbox: ((lineIndex: Int) -> Unit)? = null
) {
    val colors      = LocalAppColors.current
    val scrollState = rememberScrollState()
    val annotated   = remember(rawText, colors) { buildReadViewAnnotatedString(rawText, colors) }

    if (onToggleCheckbox != null) {
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
                            annotated.getStringAnnotations("checkbox", offset, offset)
                                .firstOrNull()?.let { ann ->
                                    onToggleCheckbox(ann.item.toInt())
                                }
                        }
                    }
                },
            style        = TextStyle(fontSize = 16.sp, color = colors.onSurface, lineHeight = 28.sp),
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
                style = TextStyle(fontSize = 16.sp, color = colors.onSurface, lineHeight = 28.sp)
            )
        }
    }
}
