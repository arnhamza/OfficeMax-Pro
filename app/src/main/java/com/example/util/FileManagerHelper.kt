package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class StorageStats(
    val totalDocumentsCount: Int,
    val totalSizeBytes: Long,
    val formattedSize: String,
    val internalFreeSpaceFormatted: String,
    val categoryBreakdown: Map<DocType, Int>
)

object FileManagerHelper {

    fun getDocsDirectory(context: Context): File {
        val dir = File(context.filesDir, "office_documents")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveDocumentToFile(context: Context, doc: OfficeDocument): File {
        val dir = getDocsDirectory(context)
        val safeTitle = doc.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "$safeTitle${doc.type.extension}"
        val file = File(dir, fileName)

        when (doc.type) {
            DocType.PDF -> {
                FileOutputStream(file).use { fos ->
                    PdfGenerator.createDocumentPdf(
                        title = doc.title,
                        content = doc.content,
                        category = "PDF Document",
                        outputStream = fos
                    )
                }
            }
            DocType.SHEET -> {
                file.writeText(doc.content)
            }
            DocType.SLIDE -> {
                file.writeText(doc.content)
            }
            DocType.DOC, DocType.SCAN, DocType.TXT -> {
                file.writeText(doc.content)
            }
        }
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share Document") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            // Fallback
        }
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                stream.bufferedReader().use { it.readText() }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result = "imported_document"
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == "imported_document") {
            uri.path?.let { p ->
                val cut = p.lastIndexOf('/')
                if (cut != -1) {
                    result = p.substring(cut + 1)
                }
            }
        }
        return result
    }

    fun detectDocType(fileName: String): DocType {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".docx") || lower.endsWith(".doc") -> DocType.DOC
            lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") -> DocType.SHEET
            lower.endsWith(".pptx") || lower.endsWith(".ppt") -> DocType.SLIDE
            lower.endsWith(".pdf") -> DocType.PDF
            lower.endsWith(".txt") || lower.endsWith(".md") -> DocType.TXT
            else -> DocType.DOC
        }
    }

    fun computeStorageStats(context: Context, documents: List<OfficeDocument>): StorageStats {
        var totalBytes = 0L
        val dir = getDocsDirectory(context)
        dir.listFiles()?.forEach { totalBytes += it.length() }

        val breakdown = documents.groupBy { it.type }.mapValues { it.value.size }
        val freeBytes = context.filesDir.freeSpace

        return StorageStats(
            totalDocumentsCount = documents.size,
            totalSizeBytes = totalBytes,
            formattedSize = formatFileSize(totalBytes),
            internalFreeSpaceFormatted = formatFileSize(freeBytes),
            categoryBreakdown = breakdown
        )
    }

    fun createZipBackup(context: Context, documents: List<OfficeDocument>, outputStream: java.io.OutputStream): Boolean {
        return try {
            ZipOutputStream(outputStream).use { zos ->
                documents.forEach { doc ->
                    val file = saveDocumentToFile(context, doc)
                    if (file.exists()) {
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val df = DecimalFormat("#,##0.0")
        return when {
            bytes >= 1024 * 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024 * 1024))} GB"
            bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024))} MB"
            bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }
}
