package com.example.util

import android.content.Context
import android.graphics.Bitmap
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import java.io.File
import java.io.FileOutputStream

data class ConversionJob(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceTitle: String,
    val fromType: DocType,
    val toType: DocType,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val resultDoc: OfficeDocument? = null,
    val error: String? = null
)

object DocumentConverter {

    fun convertDocToPdf(context: Context, doc: OfficeDocument): OfficeDocument {
        val title = doc.title.removeSuffix(".docx").removeSuffix(".txt") + " (Converted)"
        val dir = FileManagerHelper.getDocsDirectory(context)
        val pdfFile = File(dir, "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

        FileOutputStream(pdfFile).use { fos ->
            PdfGenerator.createDocumentPdf(
                title = doc.title,
                content = doc.content,
                category = "Word to PDF Conversion",
                outputStream = fos
            )
        }

        return OfficeDocument(
            title = title,
            type = DocType.PDF,
            content = doc.content,
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            categoryTag = "Converted"
        )
    }

    fun convertSheetToPdf(context: Context, doc: OfficeDocument): OfficeDocument {
        val title = doc.title.removeSuffix(".xlsx").removeSuffix(".csv") + " (Spreadsheet Report)"
        val dir = FileManagerHelper.getDocsDirectory(context)
        val pdfFile = File(dir, "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

        // Format CSV into readable tabular summary
        val formattedTable = doc.content.lines().joinToString("\n") { line ->
            line.split(",").joinToString("   |   ") { it.trim() }
        }

        FileOutputStream(pdfFile).use { fos ->
            PdfGenerator.createDocumentPdf(
                title = doc.title,
                content = "SPREADSHEET DATA EXPORT\n\n$formattedTable",
                category = "Excel to PDF Table",
                outputStream = fos
            )
        }

        return OfficeDocument(
            title = title,
            type = DocType.PDF,
            content = formattedTable,
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            categoryTag = "Financial Report"
        )
    }

    fun convertSlidesToPdf(context: Context, doc: OfficeDocument): OfficeDocument {
        val title = doc.title.removeSuffix(".pptx") + " (Slides Handout)"
        val dir = FileManagerHelper.getDocsDirectory(context)
        val pdfFile = File(dir, "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

        val slides = SlidesEngine.parseSlides(doc.content)
        val sb = StringBuilder()
        slides.forEachIndexed { index, s ->
            sb.append("SLIDE ${index + 1}: ${s.title.uppercase()}\n")
            if (s.subtitle.isNotEmpty()) sb.append("Sub: ${s.subtitle}\n")
            if (s.statNumber.isNotEmpty()) sb.append("KPI: ${s.statNumber} - ${s.statLabel}\n")
            s.bulletPoints.forEach { b -> sb.append("  • $b\n") }
            if (s.speakerNotes.isNotEmpty()) sb.append("  [Speaker Notes: ${s.speakerNotes}]\n")
            sb.append("\n----------------------------------------\n\n")
        }

        FileOutputStream(pdfFile).use { fos ->
            PdfGenerator.createDocumentPdf(
                title = doc.title,
                content = sb.toString(),
                category = "PowerPoint Handout PDF",
                outputStream = fos
            )
        }

        return OfficeDocument(
            title = title,
            type = DocType.PDF,
            content = sb.toString(),
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            categoryTag = "Presentation"
        )
    }

    fun convertImageToPdf(context: Context, bitmap: Bitmap, title: String): OfficeDocument {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = FileManagerHelper.getDocsDirectory(context)
        val pdfFile = File(dir, "$safeTitle.pdf")

        FileOutputStream(pdfFile).use { fos ->
            PdfGenerator.createImagePdf(bitmap, title, fos)
        }

        return OfficeDocument(
            title = title,
            type = DocType.PDF,
            content = "Scanned photo document compiled into standard PDF",
            filePath = pdfFile.absolutePath,
            fileSizeBytes = pdfFile.length(),
            categoryTag = "Scanned PDF"
        )
    }

    fun convertOcrToDoc(text: String, title: String = "Digitized Document"): OfficeDocument {
        return OfficeDocument(
            title = title,
            type = DocType.DOC,
            content = text,
            categoryTag = "Digitized"
        )
    }

    fun convertOcrToSheet(csvText: String, title: String = "Scanned Spreadsheet"): OfficeDocument {
        return OfficeDocument(
            title = title,
            type = DocType.SHEET,
            content = csvText,
            categoryTag = "Scanned Data"
        )
    }
}
