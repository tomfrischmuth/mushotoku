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

package com.mushotoku.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.PathParser
import com.mushotoku.app.R
import com.mushotoku.app.ui.brand.LetterPaths
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the recovery code as a one-page, print-ready PDF carrying the Mushotoku
 * wordmark, so the user can save or print it instead of sharing raw text.
 */
object RecoveryCodePdf {

    // A4 at 72 dpi.
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 56f

    private const val DARK = 0xFF1C1B19.toInt()
    private const val GRAY = 0xFF6B6B6B.toInt()
    private const val BORDER = 0xFFD8D3C4.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    // Wordmark enso gradient (Red → Orange → Amber → YellowGreen → Green).
    private val ENSO_COLORS = intArrayOf(
        0xFFC9453B.toInt(), 0xFFDA6B3D.toInt(), 0xFFE8A33D.toInt(), 0xFFA3A648.toInt(), 0xFF5DA855.toInt(),
    )
    private val ENSO_STOPS = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)

    fun create(context: Context, code: String): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        canvas.drawColor(WHITE)

        val centerX = PAGE_W / 2f

        // Wordmark
        val wmWidth = 240f
        drawWordmark(canvas, left = centerX - wmWidth / 2f, top = 96f, width = wmWidth)

        // Title
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DARK; textSize = 20f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(context.getString(R.string.recovery_pdf_title), centerX, 214f, title)

        // Code box
        val boxLeft = 80f
        val boxRight = PAGE_W - 80f
        val boxTop = 266f
        val boxBottom = 322f
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.2f; color = BORDER
        }
        canvas.drawRoundRect(RectF(boxLeft, boxTop, boxRight, boxBottom), 10f, 10f, box)

        val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DARK; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        var codeSize = 20f
        codePaint.textSize = codeSize
        while (codePaint.measureText(code) > (boxRight - boxLeft - 28f) && codeSize > 10f) {
            codeSize -= 1f
            codePaint.textSize = codeSize
        }
        val codeBaseline = (boxTop + boxBottom) / 2f - (codePaint.descent() + codePaint.ascent()) / 2f
        canvas.drawText(code, centerX, codeBaseline, codePaint)

        // Body text
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = GRAY; textSize = 11f }
        val bodyText = context.getString(R.string.recovery_pdf_body)
        val bodyWidth = (PAGE_W - 2 * MARGIN).toInt()
        val layout = StaticLayout.Builder
            .obtain(bodyText, 0, bodyText.length, body, bodyWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .build()
        canvas.save()
        canvas.translate(MARGIN, 360f)
        layout.draw(canvas)
        canvas.restore()

        doc.finishPage(page)

        val file = File(context.cacheDir, "Mushotoku-Recovery-Code.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawWordmark(canvas: Canvas, left: Float, top: Float, width: Float) {
        val s = width / 1960f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(s, s)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 30f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = DARK
        }
        LetterPaths.forEach { canvas.drawPath(PathParser.createPathFromPathData(it), stroke) }
        drawEnso(canvas, 1035f, 260f, 75f, 30f, mirrored = false)
        drawEnso(canvas, 1405f, 260f, 75f, 30f, mirrored = true)
        canvas.restore()
    }

    private fun drawEnso(canvas: Canvas, cx: Float, cy: Float, r: Float, sw: Float, mirrored: Boolean) {
        canvas.save()
        if (mirrored) canvas.scale(-1f, 1f, cx, cy)
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = sw; strokeCap = Paint.Cap.BUTT; color = DARK
        }
        canvas.drawArc(oval, -15f, 199.3f, false, ring)
        val (sx, sy) = pointOnCircle(cx, cy, r, -29.0)
        val (ex, ey) = pointOnCircle(cx, cy, r, 75.0)
        val grad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = sw; strokeCap = Paint.Cap.BUTT
            shader = LinearGradient(sx, sy, ex, ey, ENSO_COLORS, ENSO_STOPS, Shader.TileMode.CLAMP)
        }
        canvas.drawArc(oval, -119f, 104f, false, grad)
        canvas.restore()
    }

    private fun pointOnCircle(cx: Float, cy: Float, r: Float, deg: Double): Pair<Float, Float> {
        val rad = Math.toRadians(deg)
        return (cx + (r * sin(rad)).toFloat()) to (cy - (r * cos(rad)).toFloat())
    }
}
