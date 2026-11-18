package com.star.menugenerator.render

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.star.menugenerator.domain.Block
import com.star.menugenerator.domain.MenuSettings
import com.star.menugenerator.domain.TemplateSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class ExportKind(val mime: String, val ext: String) {
    PNG("image/png", "png"),
    PDF("application/pdf", "pdf"),
}

/** [savedUri] is the copy in Downloads/STAR Menus (null on Android 8/9, where only sharing is offered). */
data class SavedFile(val name: String, val file: File, val shareUri: Uri, val savedUri: Uri?)

data class ExportResult(val kind: ExportKind, val files: List<SavedFile>) {
    val savedToDownloads: Boolean get() = files.isNotEmpty() && files.all { it.savedUri != null }
}

/**
 * Writes the rendered menu as high-resolution PNG (2x, 2108 x 2984 px per page) or a PDF (A4 page,
 * real vector text over the template artwork). Files are created in the app cache for sharing /
 * printing and copied to Downloads/STAR Menus on Android 10+ so they are visible in the Files app.
 */
class Exporter(private val context: Context, private val renderer: MenuRenderer) {

    private fun exportDir(): File = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun clearOld() {
        exportDir().listFiles()?.forEach { it.delete() }
    }

    fun baseName(title: String): String {
        val cleaned = title.trim().replace(Regex("[^\\p{L}\\p{N}\\p{M}]+"), "-").trim('-').take(50)
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return "STAR-menu-" + (if (cleaned.isEmpty()) "menu" else cleaned) + "-" + stamp
    }

    suspend fun exportPng(
        spec: TemplateSpec, settings: MenuSettings, pages: List<List<Block>>, base: String,
        saveCopy: Boolean = true, onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): ExportResult = withContext(Dispatchers.Default) {
        clearOld()
        val files = ArrayList<SavedFile>()
        pages.forEachIndexed { i, blocks ->
            onProgress(i + 1, pages.size)
            val name = if (pages.size > 1) "$base-page${i + 1}.png" else "$base.png"
            val file = File(exportDir(), name)
            val bmp = renderer.renderBitmap(spec, settings, blocks, i, pages.size, 2f)
            try {
                FileOutputStream(file).use { out ->
                    if (!bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)) {
                        throw IllegalStateException("Could not write the PNG file")
                    }
                }
            } finally {
                bmp.recycle()
            }
            files += toSaved(file, ExportKind.PNG, saveCopy)
        }
        ExportResult(ExportKind.PNG, files)
    }

    suspend fun exportPdf(
        spec: TemplateSpec, settings: MenuSettings, pages: List<List<Block>>, base: String,
        saveCopy: Boolean = true, onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): ExportResult = withContext(Dispatchers.Default) {
        clearOld()
        val file = File(exportDir(), "$base.pdf")
        val pageW = 595 // A4 in PostScript points
        val pageH = (pageW.toFloat() * spec.h / spec.w).roundToInt()
        val doc = PdfDocument()
        try {
            pages.forEachIndexed { i, blocks ->
                onProgress(i + 1, pages.size)
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create())
                val canvas = page.canvas
                canvas.scale(pageW.toFloat() / spec.w, pageH.toFloat() / spec.h)
                renderer.drawPage(canvas, spec, settings, blocks, i, pages.size)
                doc.finishPage(page)
            }
            FileOutputStream(file).use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
        ExportResult(ExportKind.PDF, listOf(toSaved(file, ExportKind.PDF, saveCopy)))
    }

    private fun toSaved(file: File, kind: ExportKind, saveCopy: Boolean): SavedFile {
        val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val saved = if (saveCopy) copyToDownloads(file, kind.mime) else null
        return SavedFile(file.name, file, shareUri, saved)
    }

    private fun copyToDownloads(file: File, mime: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        var uri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/STAR Menus")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            val out = resolver.openOutputStream(uri) ?: throw IllegalStateException("No output stream")
            out.use { o -> FileInputStream(file).use { it.copyTo(o) } }
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            return uri
        } catch (e: Exception) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            return null
        }
    }

    // ---- share & print ----

    fun share(context: Context, result: ExportResult) {
        val uris = ArrayList(result.files.map { it.shareUri })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = result.kind.mime
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = result.kind.mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "Share menu").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun print(context: Context, pdf: File, jobName: String) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        manager.print(jobName, PdfFilePrintAdapter(pdf, jobName), null)
    }
}

/** Sends an already-written PDF file to the Android print dialog. */
private class PdfFilePrintAdapter(private val file: File, private val jobName: String) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.LayoutResultCallback?, extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?, destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.WriteResultCallback?,
    ) {
        try {
            FileInputStream(file).use { input ->
                FileOutputStream(destination!!.fileDescriptor).use { output -> input.copyTo(output) }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        }
    }
}
