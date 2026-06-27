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
import android.os.CancellationSignal
import android.print.PdfPrint
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HtmlToPdfRenderer(private val context: Context) {

    suspend fun render(html: String, out: OutputStream) {
        val tmp = File.createTempFile("export_", ".pdf", context.cacheDir)
        try {
            renderToFile(html, tmp)
            FileInputStream(tmp).use { it.copyTo(out) }
        } finally {
            tmp.delete()
        }
    }

    private suspend fun renderToFile(html: String, file: File) = withContext(Dispatchers.Main) {
        val webView = WebView(context)
        val signal = CancellationSignal()
        try {
            suspendCancellableCoroutine { cont ->
                var printed = false
                cont.invokeOnCancellation { signal.cancel() }
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        if (printed) return
                        printed = true
                        try {
                            printToFile(view, file, signal, cont)
                        } catch (t: Throwable) {
                            if (cont.isActive) cont.resumeWithException(t)
                        }
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        } finally {
            webView.destroy()
        }
    }

    private fun printToFile(
        view: WebView,
        file: File,
        signal: CancellationSignal,
        cont: CancellableContinuation<Unit>
    ) {
        val adapter = view.createPrintDocumentAdapter("mushotoku_export")
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        PdfPrint(attributes).print(adapter, file, signal, object : PdfPrint.Callback {
            override fun onFinished() {
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onFailed(error: CharSequence?) {
                if (cont.isActive) cont.resumeWithException(IOException("PDF-Erzeugung fehlgeschlagen: $error"))
            }
            override fun onCancelled() {
                if (cont.isActive) cont.cancel()
            }
        })
    }
}
