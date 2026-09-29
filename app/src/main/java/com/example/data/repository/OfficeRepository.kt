package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.OfficeDao
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.data.model.OfficeTask
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.util.FileManagerHelper
import com.example.util.OcrEngine
import com.example.util.SlidesEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class OfficeRepository(
    private val context: Context,
    private val dao: OfficeDao
) {

    val allDocuments: Flow<List<OfficeDocument>> = dao.getAllDocuments()
    val allTasks: Flow<List<OfficeTask>> = dao.getAllTasks()
    val favoriteDocuments: Flow<List<OfficeDocument>> = dao.getFavoriteDocuments()

    fun getDocumentsByType(type: DocType): Flow<List<OfficeDocument>> = dao.getDocumentsByType(type)

    fun getDocumentById(id: Long): Flow<OfficeDocument?> = dao.getDocumentById(id)

    suspend fun getDocumentByIdOnce(id: Long): OfficeDocument? = dao.getDocumentByIdOnce(id)

    suspend fun insertDocument(doc: OfficeDocument): Long = withContext(Dispatchers.IO) {
        val file = FileManagerHelper.saveDocumentToFile(context, doc)
        val updated = doc.copy(filePath = file.absolutePath, fileSizeBytes = file.length())
        dao.insertDocument(updated)
    }

    suspend fun updateDocument(doc: OfficeDocument) = withContext(Dispatchers.IO) {
        val file = FileManagerHelper.saveDocumentToFile(context, doc)
        val updated = doc.copy(
            filePath = file.absolutePath,
            fileSizeBytes = file.length(),
            lastModified = System.currentTimeMillis()
        )
        dao.updateDocument(updated)
    }

    suspend fun deleteDocument(doc: OfficeDocument) = withContext(Dispatchers.IO) {
        doc.filePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        }
        dao.deleteDocument(doc)
    }

    suspend fun toggleFavorite(doc: OfficeDocument) = withContext(Dispatchers.IO) {
        dao.updateDocument(doc.copy(isFavorite = !doc.isFavorite))
    }

    // Tasks
    suspend fun insertTask(task: OfficeTask): Long = withContext(Dispatchers.IO) {
        dao.insertTask(task)
    }

    suspend fun updateTask(task: OfficeTask) = withContext(Dispatchers.IO) {
        dao.updateTask(task)
    }

    suspend fun deleteTask(task: OfficeTask) = withContext(Dispatchers.IO) {
        dao.deleteTask(task)
    }

    /**
     * Seed initial rich office documents and tasks if database is brand new
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getDocumentCount().firstOrNull() ?: 0
        if (count == 0) {
            // 1. Word Document
            val docWord = OfficeDocument(
                title = "Q4 Enterprise Strategy & Roadmap",
                type = DocType.DOC,
                content = """
                    # EXECUTIVE BRIEFING: Q4 STRATEGIC INITIATIVES
                    
                    ## 1. Objectives & Overview
                    Our goal for the upcoming quarter is accelerating mobile document workflows, providing direct access to local storage, and integrating intelligent optical scanning for paperless digital operations.
                    
                    ## 2. Key Pillars
                    • Unified Suite Architecture: Seamless switching between Word Docs, Excel Spreadsheets, and Presentation Decks.
                    • High-Performance Conversion: Real-time generation of authentic A4 PDF documents with zero external cloud dependencies.
                    • On-Device OCR Scanner: Fast detection of dates, invoice numbers, monetary amounts, and tabular data.
                    • Direct File Manager: Native Android Storage Access Framework integration and fast ZIP backups.
                    
                    ## 3. Milestones & Deliverables
                    - Milestone 1: Core Document Engine stabilization (Complete)
                    - Milestone 2: Spreadsheet calculation formulas =SUM, =AVG, =MIN, =MAX (Complete)
                    - Milestone 3: Presentation Slides with Fullscreen Slideshow mode (Complete)
                    - Milestone 4: Direct Export & APK Packaging Center (Active)
                    
                    Prepared by: OfficePro Operations Team
                """.trimIndent(),
                categoryTag = "Strategy"
            )
            val doc1Id = insertDocument(docWord)

            // 2. Excel Spreadsheet
            val sampleSheetCsv = """
                Category,Q1 Actual,Q2 Actual,Q3 Target,Q4 Forecast,Total YTD
                Cloud Storage,12500,13200,14000,15500,=SUM(B2:E2)
                Office Equipment,4200,3800,4500,5000,=SUM(B3:E3)
                Team Software,8900,9100,9500,10200,=SUM(B4:E4)
                Professional Dev,3400,4200,5000,5500,=SUM(B5:E5)
                Marketing Media,15000,18500,21000,24000,=SUM(B6:E6)
                Total Budget,=SUM(B2:B6),=SUM(C2:C6),=SUM(D2:D6),=SUM(E2:E6),=SUM(F2:F6)
                Average Spend,=AVG(B2:B6),=AVG(C2:C6),=AVG(D2:D6),=AVG(E2:E6),=AVG(F2:F6)
            """.trimIndent()

            val docSheet = OfficeDocument(
                title = "Financial Forecast & Operations Budget",
                type = DocType.SHEET,
                content = sampleSheetCsv,
                categoryTag = "Finance"
            )
            val doc2Id = insertDocument(docSheet)

            // 3. PowerPoint Slides
            val docSlides = OfficeDocument(
                title = "OfficePro Suite Keynote Pitch Deck",
                type = DocType.SLIDE,
                content = SlidesEngine.serializeSlides(SlidesEngine.defaultPitchDeck()),
                categoryTag = "Pitch"
            )
            val doc3Id = insertDocument(docSlides)

            // 4. PDF Document
            val docPdf = OfficeDocument(
                title = "Enterprise Non-Disclosure & Agreement",
                type = DocType.PDF,
                content = OcrEngine.sampleContractOcr,
                categoryTag = "Legal"
            )
            val doc4Id = insertDocument(docPdf)

            // 5. Scanned OCR Invoice
            val docScan = OfficeDocument(
                title = "Metropolitan Office Hardware Receipt",
                type = DocType.SCAN,
                content = OcrEngine.sampleReceiptOcr,
                categoryTag = "Expenses"
            )
            insertDocument(docScan)

            // Seed Tasks
            insertTask(
                OfficeTask(
                    title = "Review Q4 Executive Briefing",
                    description = "Verify milestone checklist before company all-hands meeting",
                    priority = TaskPriority.HIGH,
                    status = TaskStatus.IN_PROGRESS,
                    linkedDocId = doc1Id,
                    linkedDocTitle = docWord.title
                )
            )
            insertTask(
                OfficeTask(
                    title = "Verify Budget Formula Totals",
                    description = "Ensure all =SUM and =AVG rows match accounting ledger",
                    priority = TaskPriority.HIGH,
                    status = TaskStatus.TODO,
                    linkedDocId = doc2Id,
                    linkedDocTitle = docSheet.title
                )
            )
            insertTask(
                OfficeTask(
                    title = "Rehearse PowerPoint Slideshow",
                    description = "Practice slide transitions and presenter notes for investor pitch",
                    priority = TaskPriority.MEDIUM,
                    status = TaskStatus.TODO,
                    linkedDocId = doc3Id,
                    linkedDocTitle = docSlides.title
                )
            )
            insertTask(
                OfficeTask(
                    title = "Countersign Enterprise NDA",
                    description = "Apply digital signature and export finalized PDF document",
                    priority = TaskPriority.LOW,
                    status = TaskStatus.REVIEW,
                    linkedDocId = doc4Id,
                    linkedDocTitle = docPdf.title
                )
            )
        }
    }

    companion object {
        fun create(context: Context): OfficeRepository {
            val db = AppDatabase.getInstance(context)
            return OfficeRepository(context, db.officeDao())
        }
    }
}
