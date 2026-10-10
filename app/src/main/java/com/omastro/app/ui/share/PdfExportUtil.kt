package com.omastro.app.ui.share

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders the server's official Kundali report HTML (fetched via
 * POST /wp-json/omastro/v1/pdf/export) into a real PDF file using the
 * WebView print framework — direct to file, no print dialog.
 *
 * The report content is 100% server-computed; the device only converts the
 * official HTML into PDF bytes. Must be called from a coroutine; the WebView
 * work runs on the main thread. Pass an Activity [context] for reliable
 * WebView initialisation. The HTML references remote Google fonts; the device
 * must be online for exact font fidelity, but the report renders regardless.
 */
@Suppress("DEPRECATION") // createPrintDocumentAdapter has no non-deprecated replacement
suspend fun renderHtmlToPdfFile(context: Context, html: String, filename: String): File =
    withContext(Dispatchers.Main) {
        val dir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val outFile = File(dir, filename.ifBlank { "Kundali-Report.pdf" })
        suspendCancellableCoroutine { cont ->
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = false
                allowFileAccess = false
                allowContentAccess = false
            }
            val widthPx = context.resources.displayMetrics.widthPixels
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    try {
                        view.measure(
                            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        view.layout(0, 0, widthPx, view.measuredHeight)
                        val attrs = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        val adapter = view.createPrintDocumentAdapter("Kundali Report")
                        val pfd = ParcelFileDescriptor.open(
                            outFile,
                            ParcelFileDescriptor.MODE_READ_WRITE or
                                ParcelFileDescriptor.MODE_CREATE or
                                ParcelFileDescriptor.MODE_TRUNCATE
                        )
                        adapter.onLayout(null, attrs, null,
                            object : PrintDocumentAdapter.LayoutResultCallback() {
                                override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                                    adapter.onWrite(
                                        arrayOf(PageRange.ALL_PAGES), pfd, CancellationSignal(),
                                        object : PrintDocumentAdapter.WriteResultCallback() {
                                            override fun onWriteFinished(pages: Array<PageRange>) {
                                                pfd.closeQuietly()
                                                webView.destroy()
                                                if (!cont.isCompleted) cont.resume(outFile)
                                            }
                                            override fun onWriteFailed(error: CharSequence?) {
                                                pfd.closeQuietly()
                                                webView.destroy()
                                                if (!cont.isCompleted) cont.resumeWithException(
                                                    IOException("PDF write failed: ${error ?: "unknown"}")
                                                )
                                            }
                                            override fun onWriteCancelled() {
                                                pfd.closeQuietly()
                                                webView.destroy()
                                                if (!cont.isCompleted) cont.resumeWithException(
                                                    CancellationException("PDF write cancelled")
                                                )
                                            }
                                        }
                                    )
                                }
                                override fun onLayoutFailed(error: CharSequence?) {
                                    pfd.closeQuietly()
                                    webView.destroy()
                                    if (!cont.isCompleted) cont.resumeWithException(
                                        IOException("PDF layout failed: ${error ?: "unknown"}")
                                    )
                                }
                            }, null)
                    } catch (e: Exception) {
                        try { webView.destroy() } catch (_: Exception) {}
                        if (!cont.isCompleted) cont.resumeWithException(e)
                    }
                }

                override fun onReceivedError(
                    view: WebView, request: WebResourceRequest, error: WebResourceError
                ) {
                    // Sub-resource errors (e.g. remote fonts) are non-fatal —
                    // the report page itself still renders and prints.
                }
            }
            webView.loadDataWithBaseURL("https://www.omastroai.com/", html, "text/html", "UTF-8", null)
            cont.invokeOnCancellation {
                try { webView.destroy() } catch (_: Exception) {}
            }
        }
    }

private fun ParcelFileDescriptor.closeQuietly() {
    try { close() } catch (_: Exception) {}
}
