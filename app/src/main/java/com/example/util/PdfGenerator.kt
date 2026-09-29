package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in pt
    private const val PAGE_HEIGHT = 842 // Standard A4 height in pt
    private const val MARGIN = 50

    /**
     * Generates a real formatted PDF document from text, title, and optional header styling
     */
    fun createDocumentPdf(
        title: String,
        content: String,
        category: String = "Word Document",
        watermark: String? = null,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(24, 90, 189) // Office Word Blue
            style = Paint.Style.FILL
        }
        // Top accent bar
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 8f, headerPaint)

        // Title Paint
        val titlePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Subtitle / category Paint
        val metaPaint = TextPaint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        // Content Paint
        val contentPaint = TextPaint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 12f
            isAntiAlias = true
        }

        var yPos = MARGIN + 10f
        canvas.drawText(title, MARGIN.toFloat(), yPos, titlePaint)
        yPos += 20f

        val dateStr = "OfficePro Suite • $category • Generated on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}"
        canvas.drawText(dateStr, MARGIN.toFloat(), yPos, metaPaint)
        yPos += 15f

        // Separator line
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN.toFloat(), yPos, (PAGE_WIDTH - MARGIN).toFloat(), yPos, linePaint)
        yPos += 25f

        // Watermark if specified
        if (!watermark.isNullOrBlank()) {
            val wmPaint = TextPaint().apply {
                color = Color.argb(30, 200, 20, 20)
                textSize = 64f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.save()
            canvas.rotate(-45f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
            val wmBounds = Rect()
            wmPaint.getTextBounds(watermark, 0, watermark.length, wmBounds)
            canvas.drawText(
                watermark,
                (PAGE_WIDTH / 2f) - (wmBounds.width() / 2f),
                (PAGE_HEIGHT / 2f) + (wmBounds.height() / 2f),
                wmPaint
            )
            canvas.restore()
        }

        // Render main text using StaticLayout for robust wrapping
        val contentWidth = PAGE_WIDTH - (MARGIN * 2)
        @Suppress("DEPRECATION")
        val staticLayout = StaticLayout(
            content,
            contentPaint,
            contentWidth,
            Layout.Alignment.ALIGN_NORMAL,
            1.3f,
            0.0f,
            false
        )

        canvas.save()
        canvas.translate(MARGIN.toFloat(), yPos)
        staticLayout.draw(canvas)
        canvas.restore()

        // Footer
        val footerPaint = TextPaint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Page 1 of 1 • Created with OfficePro Suite",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 25f,
            footerPaint
        )

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    /**
     * Generates a PDF containing an image (e.g. Scanned Document / OCR photo)
     */
    fun createImagePdf(bitmap: Bitmap, title: String, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerPaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(title, MARGIN.toFloat(), 40f, headerPaint)

        // Scale and draw bitmap inside margins
        val availableWidth = (PAGE_WIDTH - (MARGIN * 2)).toFloat()
        val availableHeight = (PAGE_HEIGHT - MARGIN - 60).toFloat()

        val scale = minOf(
            availableWidth / bitmap.width.toFloat(),
            availableHeight / bitmap.height.toFloat()
        )

        val destWidth = bitmap.width * scale
        val destHeight = bitmap.height * scale
        val left = MARGIN + (availableWidth - destWidth) / 2f
        val top = 60f + (availableHeight - destHeight) / 2f

        val destRect = Rect(left.toInt(), top.toInt(), (left + destWidth).toInt(), (top + destHeight).toInt())
        canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }

    /**
     * Render the first page of a PDF file to a Bitmap using native Android PdfRenderer
     */
    fun renderPdfPageToBitmap(file: File, pageIndex: Int = 0): Bitmap? {
        if (!file.exists()) return null
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
