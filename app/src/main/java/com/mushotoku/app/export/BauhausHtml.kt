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

object BauhausHtml {

    private const val ACCENT = "#E8A33D"
    private const val INK = "#1C1B19"
    private const val MUTED = "#6B6862"
    private const val HAIRLINE = "#E2DDD1"

    private val CSS = """
        @page { size: A4; margin: 20mm 18mm; }
        * { box-sizing: border-box; }
        html, body { margin: 0; padding: 0; }
        body {
            font-family: -apple-system, "Roboto", "Segoe UI", Helvetica, Arial, sans-serif;
            color: $INK;
            font-size: 11pt;
            line-height: 1.5;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
        }
        header.doc { margin-bottom: 26px; }
        header.doc h1 {
            font-size: 24pt;
            font-weight: 700;
            letter-spacing: 0.5px;
            margin: 0 0 6px 0;
            padding-bottom: 10px;
            border-bottom: 2px solid $ACCENT;
        }
        header.doc .meta { color: $MUTED; font-size: 9.5pt; margin: 0; }

        section.month { margin-top: 22px; }
        section.month:first-of-type { margin-top: 4px; }
        h2.month {
            font-size: 10pt;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1.5px;
            color: $INK;
            margin: 0 0 12px 0;
            padding-bottom: 5px;
            border-bottom: 1px solid $HAIRLINE;
        }
        h2.month::before {
            content: "";
            display: inline-block;
            width: 14px; height: 2px;
            background: $ACCENT;
            vertical-align: middle;
            margin-right: 8px;
        }

        .entry { padding: 9px 0; border-bottom: 1px solid $HAIRLINE; page-break-inside: avoid; }
        .entry:last-child { border-bottom: none; }
        .entry .when { color: $ACCENT; font-weight: 700; font-size: 10pt; }
        .entry .title { font-weight: 600; font-size: 11.5pt; margin-top: 2px; }
        .entry .body { color: $MUTED; font-size: 10.5pt; margin-top: 3px; white-space: pre-wrap; }

        .grat-list { margin: 4px 0 0 0; padding: 0; list-style: none; }
        .grat-list li { padding: 1px 0 1px 18px; position: relative; }
        .grat-list li::before {
            content: counter(grat) ".";
            counter-increment: grat;
            position: absolute; left: 0;
            color: $ACCENT; font-weight: 700; font-size: 9.5pt;
        }
        .grat-list { counter-reset: grat; }

        .mood {
            display: inline-block; vertical-align: middle;
            width: 11px; height: 11px; border-radius: 50%;
            margin-left: 8px;
        }
        .mood-label { color: $MUTED; font-size: 9pt; margin-left: 5px; vertical-align: middle; }

        .note-body { white-space: pre-wrap; font-size: 11pt; color: $INK; line-height: 1.6; }
        .empty { color: $MUTED; font-style: italic; margin-top: 24px; }

        .fin-group {
            display: flex; justify-content: space-between;
            margin-top: 10px; padding: 7px 0 3px;
            border-top: 1px solid $HAIRLINE;
            font-weight: 700; font-size: 11pt; color: $INK;
        }
        .fin-group .amt { color: $ACCENT; }
        .fin-cat {
            display: flex; justify-content: space-between;
            padding: 1px 0 1px 16px;
            color: $MUTED; font-size: 10.5pt;
        }
        .fin-recurring { color: $MUTED; font-size: 9.5pt; margin-top: 6px; }
        .fin-total {
            display: flex; justify-content: space-between;
            margin-top: 10px; padding-top: 8px;
            border-top: 2px solid $ACCENT;
            font-weight: 700; font-size: 12pt; color: $INK;
        }
    """.trimIndent()

    val MOOD_COLORS = listOf("#EF5350", "#FF8A65", "#FFCA28", "#9CCC65", "#42A5F5")

    fun escape(text: String): String = buildString(text.length) {
        for (ch in text) when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(ch)
        }
    }

    fun document(headerTitle: String, headerSubtitle: String, contentHtml: String): String = """
        <!DOCTYPE html>
        <html lang="de">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>$CSS</style>
        </head>
        <body>
        <header class="doc">
            <h1>${escape(headerTitle)}</h1>
            <p class="meta">${escape(headerSubtitle)}</p>
        </header>
        $contentHtml
        </body>
        </html>
    """.trimIndent()
}
