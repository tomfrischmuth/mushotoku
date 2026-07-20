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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mushotoku.app.ui.theme.AppColors

/** Body text metrics, shared by the editor and the read view so both stay in step. */
internal val NoteBodySize       = 18.sp
internal val NoteBodyLineHeight = 31.sp

/** Same insets in both modes, otherwise the text shifts when switching. */
internal val NoteBodyPaddingH      = 24.dp
internal val NoteBodyPaddingTop    = 20.dp
internal val NoteBodyPaddingBottom = 16.dp

/**
 * Heading sizes, shared for the same reason. H3 sits clearly above the 17sp
 * body; at its former 18sp only the weight told them apart.
 */
internal val Heading1Style = SpanStyle(fontSize = 27.sp, fontWeight = FontWeight.Bold)
internal val Heading2Style = SpanStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold)
internal val Heading3Style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

/**
 * Sits either side of a tag in the rendering only, never in the note itself.
 * The pill is drawn wider than its letters, so without it the neighbouring text
 * would come to rest on the pill's edge.
 *
 * An ordinary space, widened by letter spacing: a wider space character would
 * be missing from the font and drag in a fallback, which shifts the whole
 * line's metrics.
 */
internal const val TagSpacer = ' '
internal val TagSpacerStyle = SpanStyle(letterSpacing = 5.sp)

/** Marks the tag ranges that get a pill, for the drawing pass to pick up. */
internal const val TagPillAnnotation = "tagPill"
internal const val StampPillAnnotation = "stampPill"

internal const val Bullet     = '•'  // •
internal const val EmptyBox   = '☐'  // ☐

/** The bullet is drawn larger than the body text so it reads as a symbol, not a letter. */
internal fun bulletStyle(accent: Color) =
    SpanStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
internal fun dashStyle(accent: Color) =
    SpanStyle(color = accent, fontWeight = FontWeight.Bold)
internal fun numberStyle(accent: Color) =
    SpanStyle(color = accent, fontWeight = FontWeight.Bold)

/**
 * Tags are greyed here; the pill of a finished tag is drawn behind the text,
 * since a span style could only paint a square background.
 */
internal fun tagStyle(colors: AppColors) = SpanStyle(color = colors.onSurfaceSecondary)

/** A stamp reads as a marker, not as prose, so it stays as quiet as a tag. */
internal fun stampStyle(colors: AppColors) = SpanStyle(color = colors.onSurfaceSecondary)

internal class MarkdownVisualTransformation(
    private val colors: AppColors,
    private val cursorLine: Int,
    /** Colour for the list markers; the check boxes keep the task light. */
    private val accent: Color = NoteAccent
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val mb = MarkdownBuilder(text.text.length)
        renderMarkdown(text.text, colors, cursorLine, accent, mb)
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

    /**
     * Adds a character that has no counterpart in the source. It maps to the
     * position it precedes, so the cursor never lands inside it.
     */
    fun insert(c: Char) { visToSrc.add(srcIdx); sb.append(c); visIdx++ }

    val visibleIndex get() = visIdx

    fun annotate(tag: String, from: Int, to: Int) = sb.addStringAnnotation(tag, "", from, to)

    /** Draws [visible] in place of the next source character, one for one. */
    fun substitute(visible: Char) {
        srcToVis[srcIdx] = visIdx; visToSrc.add(srcIdx)
        sb.append(visible); srcIdx++; visIdx++
    }

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

private fun renderMarkdown(
    raw: String,
    colors: AppColors,
    activeLineIdx: Int,
    accent: Color,
    mb: MarkdownBuilder
) {
    val muted  = colors.onSurfaceTertiary
    val subtle = colors.onSurfaceSecondary

    raw.lines().forEachIndexed { idx, line ->
        if (idx > 0) mb.show('\n')
        val show = idx == activeLineIdx

        when {
            line.startsWith("# ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("# ") } else mb.hide(2)
                mb.styled(Heading1Style) {
                    appendInline(line.substring(2), mb, show, colors)
                }
            }
            line.startsWith("## ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("## ") } else mb.hide(3)
                mb.styled(Heading2Style) {
                    appendInline(line.substring(3), mb, show, colors)
                }
            }
            line.startsWith("### ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("### ") } else mb.hide(4)
                mb.styled(Heading3Style) {
                    appendInline(line.substring(4), mb, show, colors)
                }
            }
            line.startsWith("> ") -> {
                if (show) mb.styled(SpanStyle(color = muted)) { mb.show("> ") } else mb.hide(2)
                mb.styled(SpanStyle(color = subtle, fontStyle = FontStyle.Italic)) {
                    appendInline(line.substring(2), mb, show, colors)
                }
            }
            // List markers stay visible on every line: hiding them once the
            // cursor moves away would make the list itself disappear. They are
            // drawn as real bullets and boxes rather than their raw syntax.
            checkStateOf(line) != null -> {
                val state = checkStateOf(line)!!
                mb.styled(checkPlaceholderStyle(NoteBodySize)) {
                    mb.substitute(CheckPlaceholder); mb.hide(4); mb.show(' ')
                }
                if (state == CheckState.DONE) {
                    mb.styled(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough)) {
                        appendInline(line.substring(ChecklistPrefixLength), mb, show, colors)
                    }
                } else {
                    appendInline(line.substring(ChecklistPrefixLength), mb, show, colors)
                }
            }
            // A dash stays a dash: it is its own kind of list, not a bullet.
            line.startsWith("- ") -> {
                mb.styled(dashStyle(accent)) { mb.show('-'); mb.show(' ') }
                appendInline(line.substring(2), mb, show, colors)
            }
            line.startsWith("* ") -> {
                mb.styled(bulletStyle(accent)) { mb.substitute(Bullet); mb.show(' ') }
                appendInline(line.substring(2), mb, show, colors)
            }
            numberedPrefixLength(line) > 0 -> {
                val n = numberedPrefixLength(line)
                mb.styled(numberStyle(accent)) { mb.show(line.substring(0, n)) }
                appendInline(line.substring(n), mb, show, colors)
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
            // An escaped hash is ordinary text: the backslash disappears and
            // the "#" stays behind.
            text.startsWith("$TagEscape#", i) -> {
                mb.hide(); mb.show('#')
                i += 2
            }
            stampLengthAt(text, i) > 0 -> {
                val len = stampLengthAt(text, i)
                mb.styled(TagSpacerStyle) { mb.insert(TagSpacer) }
                val from = mb.visibleIndex
                mb.styled(stampStyle(colors)) { mb.show(text.substring(i, i + len)) }
                mb.annotate(StampPillAnnotation, from, mb.visibleIndex)
                mb.styled(TagSpacerStyle) { mb.insert(TagSpacer) }
                i += len
            }
            tagLengthAt(text, i) > 0 -> {
                val len = tagLengthAt(text, i)
                // Spaced only where a pill is actually drawn, so the text does
                // not jump sideways the moment a tag is finished.
                val boxed = !(i + len >= text.length && showSyntax)
                if (boxed) mb.styled(TagSpacerStyle) { mb.insert(TagSpacer) }
                val from = mb.visibleIndex
                mb.styled(tagStyle(colors)) { mb.show(text.substring(i, i + len)) }
                if (boxed) {
                    mb.annotate(TagPillAnnotation, from, mb.visibleIndex)
                    mb.styled(TagSpacerStyle) { mb.insert(TagSpacer) }
                }
                i += len
            }
            else -> { mb.show(text[i]); i++ }
        }
    }
}
