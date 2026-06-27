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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.theme.AppColors

internal class MarkdownVisualTransformation(
    private val colors: AppColors,
    private val cursorLine: Int
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mb = MarkdownBuilder(text.text.length)
        renderMarkdown(text.text, colors, cursorLine, mb)
        return TransformedText(mb.sb.toAnnotatedString(), mb.buildMapping())
    }
}

private class MarkdownBuilder(sourceLen: Int) {
    val sb = AnnotatedString.Builder()
    private val srcToVis = IntArray(sourceLen + 1)
    private val visToSrc = ArrayList<Int>(sourceLen)
    private var srcIdx = 0
    private var visIdx = 0

    fun show(c: Char) {
        srcToVis[srcIdx] = visIdx; visToSrc.add(srcIdx)
        sb.append(c); srcIdx++; visIdx++
    }
    fun hide() { srcToVis[srcIdx] = visIdx; srcIdx++ }
    fun show(s: String) = s.forEach { show(it) }
    fun hide(n: Int) = repeat(n) { hide() }

    fun <R> styled(style: SpanStyle, block: () -> R): R {
        sb.pushStyle(style); val r = block(); sb.pop(); return r
    }

    fun buildMapping(): OffsetMapping {
        srcToVis[srcIdx] = visIdx; visToSrc.add(srcIdx)
        val sts = srcToVis.copyOf()
        val vts = visToSrc.toIntArray()
        return object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                sts[offset.coerceIn(0, sts.size - 1)]
            override fun transformedToOriginal(offset: Int) =
                vts[offset.coerceIn(0, vts.size - 1)]
        }
    }
}

private fun renderMarkdown(raw: String, colors: AppColors, activeLineIdx: Int, mb: MarkdownBuilder) {
    val muted  = colors.onSurfaceTertiary
    val subtle = colors.onSurfaceSecondary
    val accent = NoteAccent

    raw.lines().forEachIndexed { idx, line ->
        if (idx > 0) mb.show('\n')
        val show = idx == activeLineIdx

        when {
            line.startsWith("# ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("# ") } else mb.hide(2)
                mb.styled(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)) {
                    appendInline(line.substring(2), mb, show, colors)
                }
            }
            line.startsWith("## ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("## ") } else mb.hide(3)
                mb.styled(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)) {
                    appendInline(line.substring(3), mb, show, colors)
                }
            }
            line.startsWith("### ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("### ") } else mb.hide(4)
                mb.styled(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)) {
                    appendInline(line.substring(4), mb, show, colors)
                }
            }
            line.startsWith("> ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("> ") } else mb.hide(2)
                mb.styled(SpanStyle(color = subtle, fontStyle = FontStyle.Italic)) {
                    appendInline(line.substring(2), mb, show, colors)
                }
            }
            line.startsWith("- [x] ") || line.startsWith("- [X] ") -> {
                if (show) mb.styled(SpanStyle(color = accent)) { mb.show(line.substring(0, 6)) } else mb.hide(6)
                mb.styled(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough)) {
                    appendInline(line.substring(6), mb, show, colors)
                }
            }
            line.startsWith("- [ ] ") -> {
                if (show) mb.styled(SpanStyle(color = accent)) { mb.show(line.substring(0, 6)) } else mb.hide(6)
                appendInline(line.substring(6), mb, show, colors)
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                if (show) mb.styled(SpanStyle(color = accent)) { mb.show(line[0]); mb.show(' ') }
                else mb.hide(2)
                appendInline(line.substring(2), mb, show, colors)
            }
            line == "---" || line == "***" || line == "___" -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show(line) } else mb.hide(line.length)
            }
            else -> appendInline(line, mb, show, colors)
        }
    }
}

private fun appendInline(text: String, mb: MarkdownBuilder, showSyntax: Boolean, colors: AppColors) {
    val muted = colors.onSurfaceTertiary
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("***", i) -> {
                val close = text.indexOf("***", i + 3)
                if (close != -1) {
                    mb.styled(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show("***") } else mb.hide(3)
                        appendInline(text.substring(i + 3, close), mb, showSyntax, colors)
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show("***") } else mb.hide(3)
                    }
                    i = close + 3
                } else { mb.show(text[i]); i++ }
            }
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close != -1) {
                    mb.styled(SpanStyle(fontWeight = FontWeight.Bold)) {
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show("**") } else mb.hide(2)
                        appendInline(text.substring(i + 2, close), mb, showSyntax, colors)
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show("**") } else mb.hide(2)
                    }
                    i = close + 2
                } else { mb.show(text[i]); i++ }
            }
            text[i] == '*' -> {
                val close = text.indexOf('*', i + 1)
                if (close != -1) {
                    mb.styled(SpanStyle(fontStyle = FontStyle.Italic)) {
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show('*') } else mb.hide()
                        appendInline(text.substring(i + 1, close), mb, showSyntax, colors)
                        if (showSyntax) mb.styled(SpanStyle(color = muted)) { mb.show('*') } else mb.hide()
                    }
                    i = close + 1
                } else { mb.show(text[i]); i++ }
            }
            text[i] == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close != -1) {
                    if (showSyntax) mb.styled(SpanStyle(color = muted, fontFamily = FontFamily.Monospace, background = colors.surfaceVariant)) { mb.show('`') }
                    else mb.hide()
                    mb.styled(SpanStyle(fontFamily = FontFamily.Monospace, color = colors.onSurfaceSecondary, background = colors.surfaceVariant)) {
                        mb.show(text.substring(i + 1, close))
                    }
                    if (showSyntax) mb.styled(SpanStyle(color = muted, fontFamily = FontFamily.Monospace, background = colors.surfaceVariant)) { mb.show('`') }
                    else mb.hide()
                    i = close + 1
                } else { mb.show(text[i]); i++ }
            }
            else -> { mb.show(text[i]); i++ }
        }
    }
}
