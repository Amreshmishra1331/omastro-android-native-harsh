package android.print

/**
 * Subclassing shims for [PrintDocumentAdapter] callbacks.
 *
 * `PrintDocumentAdapter.LayoutResultCallback` and
 * `PrintDocumentAdapter.WriteResultCallback` have package-private
 * constructors, so app code in another package cannot extend them directly
 * (Kotlin: "Cannot access '<init>': it is package-private"). These shims live
 * in `android.print` (the same package) and expose lambda-based callbacks for
 * [com.omastro.app.ui.share.renderHtmlToPdfFile].
 *
 * These are app-defined classes in a framework package namespace; they do not
 * modify or shadow any framework class.
 */
class PdfLayoutCallback(
    private val onFinished: (PrintDocumentInfo, Boolean) -> Unit,
    private val onFailed: (CharSequence?) -> Unit
) : PrintDocumentAdapter.LayoutResultCallback() {
    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) =
        onFinished(info, changed)

    override fun onLayoutFailed(error: CharSequence?) = onFailed(error)

    override fun onLayoutCancelled() = onFailed("cancelled")
}

class PdfWriteCallback(
    private val onFinished: (Array<PageRange>) -> Unit,
    private val onFailed: (CharSequence?) -> Unit,
    private val onCancelled: () -> Unit
) : PrintDocumentAdapter.WriteResultCallback() {
    override fun onWriteFinished(pages: Array<PageRange>) = onFinished(pages)

    override fun onWriteFailed(error: CharSequence?) = onFailed(error)

    override fun onWriteCancelled() = onCancelled()
}
