package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.data.model.OfficeTask
import com.example.data.model.SortMode
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.repository.OfficeRepository
import com.example.util.CellCoord
import com.example.util.ConversionJob
import com.example.util.DocumentConverter
import com.example.util.FileManagerHelper
import com.example.util.OcrEngine
import com.example.util.OcrScanResult
import com.example.util.OfficeSlide
import com.example.util.SlidesEngine
import com.example.util.SpreadsheetEngine
import com.example.util.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    HOME("Home"),
    DOCS("Docs"),
    SHEETS("Sheets"),
    SLIDES("Slides"),
    TOOLS("Tools")
}

enum class ActiveToolScreen {
    OCR_SCANNER,
    FORMAT_CONVERTER,
    TASK_MANAGER,
    PDF_VIEWER_SIGNER,
    APK_EXPORT_CENTER
}

class OfficeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OfficeRepository.create(application)

    // Navigation state
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _activeEditingDoc = MutableStateFlow<OfficeDocument?>(null)
    val activeEditingDoc: StateFlow<OfficeDocument?> = _activeEditingDoc.asStateFlow()

    private val _activeToolScreen = MutableStateFlow<ActiveToolScreen?>(null)
    val activeToolScreen: StateFlow<ActiveToolScreen?> = _activeToolScreen.asStateFlow()

    // Presentation Mode for Slides
    private val _isPresentationMode = MutableStateFlow(false)
    val isPresentationMode: StateFlow<Boolean> = _isPresentationMode.asStateFlow()

    // Filters and search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<DocType?>(null)
    val selectedTypeFilter: StateFlow<DocType?> = _selectedTypeFilter.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.MODIFIED_DESC)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Snackbar / Feedback message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Documents list with search & filter applied
    val documents: StateFlow<List<OfficeDocument>> = combine(
        repository.allDocuments,
        _searchQuery,
        _selectedTypeFilter,
        _sortMode
    ) { docs, query, typeFilter, sort ->
        var filtered = docs
        if (typeFilter != null) {
            filtered = filtered.filter { it.type == typeFilter }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.categoryTag.lowercase().contains(q)
            }
        }
        when (sort) {
            SortMode.MODIFIED_DESC -> filtered.sortedByDescending { it.lastModified }
            SortMode.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortMode.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<OfficeTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageStats: StateFlow<StorageStats> = repository.allDocuments
        .combine(_toastMessage) { docs, _ ->
            FileManagerHelper.computeStorageStats(getApplication(), docs)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FileManagerHelper.computeStorageStats(getApplication(), emptyList())
        )

    // Spreadsheet state
    private val _sheetGrid = MutableStateFlow<Map<CellCoord, String>>(emptyMap())
    val sheetGrid: StateFlow<Map<CellCoord, String>> = _sheetGrid.asStateFlow()

    private val _selectedCell = MutableStateFlow<CellCoord?>(CellCoord(0, 0))
    val selectedCell: StateFlow<CellCoord?> = _selectedCell.asStateFlow()

    private val _cellFormulaInput = MutableStateFlow("")
    val cellFormulaInput: StateFlow<String> = _cellFormulaInput.asStateFlow()

    // Slides state
    private val _slidesList = MutableStateFlow<List<OfficeSlide>>(emptyList())
    val slidesList: StateFlow<List<OfficeSlide>> = _slidesList.asStateFlow()

    private val _currentSlideIndex = MutableStateFlow(0)
    val currentSlideIndex: StateFlow<Int> = _currentSlideIndex.asStateFlow()

    // OCR Scanner state
    private val _ocrResult = MutableStateFlow<OcrScanResult?>(null)
    val ocrResult: StateFlow<OcrScanResult?> = _ocrResult.asStateFlow()

    private val _isOcrScanning = MutableStateFlow(false)
    val isOcrScanning: StateFlow<Boolean> = _isOcrScanning.asStateFlow()

    // Converter state
    private val _conversionJobs = MutableStateFlow<List<ConversionJob>>(emptyList())
    val conversionJobs: StateFlow<List<ConversionJob>> = _conversionJobs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
        _activeEditingDoc.value = null
        _activeToolScreen.value = null
        _isPresentationMode.value = false
    }

    fun openDocument(doc: OfficeDocument) {
        _activeEditingDoc.value = doc
        _activeToolScreen.value = null

        // Initialize type-specific states
        when (doc.type) {
            DocType.SHEET -> {
                val grid = SpreadsheetEngine.parseCsv(doc.content)
                _sheetGrid.value = grid
                _selectedCell.value = CellCoord(0, 0)
                _cellFormulaInput.value = grid[CellCoord(0, 0)] ?: ""
            }
            DocType.SLIDE -> {
                val slides = SlidesEngine.parseSlides(doc.content)
                _slidesList.value = slides
                _currentSlideIndex.value = 0
            }
            DocType.SCAN -> {
                val ocr = OcrEngine.analyzeText(doc.content)
                _ocrResult.value = ocr
            }
            else -> {}
        }
    }

    fun closeActiveDocument() {
        _activeEditingDoc.value = null
        _isPresentationMode.value = false
    }

    fun openTool(tool: ActiveToolScreen) {
        _activeToolScreen.value = tool
        _activeEditingDoc.value = null
        _isPresentationMode.value = false
    }

    fun closeTool() {
        _activeToolScreen.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: DocType?) {
        _selectedTypeFilter.value = type
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    // --- Document CRUD Operations ---
    fun createNewDocument(type: DocType, templateTitle: String? = null, templateContent: String? = null) {
        viewModelScope.launch {
            val title = templateTitle ?: when (type) {
                DocType.DOC -> "New Document"
                DocType.SHEET -> "New Spreadsheet"
                DocType.SLIDE -> "New Presentation"
                DocType.PDF -> "New PDF Document"
                DocType.SCAN -> "New Scan Note"
                DocType.TXT -> "Quick Note"
            }

            val content = templateContent ?: when (type) {
                DocType.DOC -> "# Document Title\n\nStart typing your document text here..."
                DocType.SHEET -> "Item,Q1,Q2,Total\nProduct A,100,150,=SUM(B2:C2)\nProduct B,200,220,=SUM(B3:C3)\nTotal,=SUM(B2:B3),=SUM(C2:C3),=SUM(D2:D3)"
                DocType.SLIDE -> SlidesEngine.serializeSlides(SlidesEngine.defaultPitchDeck())
                DocType.PDF -> "This is a freshly created PDF document generated via OfficePro Suite."
                DocType.SCAN -> "Scanned text will appear here."
                DocType.TXT -> "Quick plain text note."
            }

            val newDoc = OfficeDocument(
                title = title,
                type = type,
                content = content,
                categoryTag = when (type) {
                    DocType.DOC -> "Documents"
                    DocType.SHEET -> "Spreadsheets"
                    DocType.SLIDE -> "Presentations"
                    DocType.PDF -> "PDFs"
                    DocType.SCAN -> "Scans"
                    DocType.TXT -> "Notes"
                }
            )
            val id = repository.insertDocument(newDoc)
            val inserted = newDoc.copy(id = id)
            openDocument(inserted)
            showToast("Created ${type.displayName}")
        }
    }

    fun saveActiveDocument(title: String, content: String) {
        val current = _activeEditingDoc.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                title = title,
                content = content,
                lastModified = System.currentTimeMillis()
            )
            repository.updateDocument(updated)
            _activeEditingDoc.value = updated
            showToast("Saved '${title}' successfully")
        }
    }

    fun deleteDocument(doc: OfficeDocument) {
        viewModelScope.launch {
            if (_activeEditingDoc.value?.id == doc.id) {
                _activeEditingDoc.value = null
            }
            repository.deleteDocument(doc)
            showToast("Deleted '${doc.title}'")
        }
    }

    fun toggleFavorite(doc: OfficeDocument) {
        viewModelScope.launch {
            repository.toggleFavorite(doc)
        }
    }

    fun shareDocument(doc: OfficeDocument) {
        val file = FileManagerHelper.saveDocumentToFile(getApplication(), doc)
        FileManagerHelper.shareFile(getApplication(), file, doc.type.mimeType, "Share ${doc.title}")
    }

    // --- External Device File Manager (SAF) ---
    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val fileName = FileManagerHelper.getFileNameFromUri(context, uri)
            val docType = FileManagerHelper.detectDocType(fileName)
            val content = FileManagerHelper.readTextFromUri(context, uri)

            val newDoc = OfficeDocument(
                title = fileName,
                type = docType,
                content = if (content.isNotBlank()) content else "Content loaded from device storage ($fileName)",
                categoryTag = "Device Storage"
            )
            val id = repository.insertDocument(newDoc)
            val inserted = newDoc.copy(id = id)
            openDocument(inserted)
            showToast("Imported $fileName from device storage")
        }
    }

    // --- Spreadsheet Actions ---
    fun selectCell(coord: CellCoord) {
        _selectedCell.value = coord
        _cellFormulaInput.value = _sheetGrid.value[coord] ?: ""
    }

    fun updateSelectedCellValue(value: String) {
        val coord = _selectedCell.value ?: return
        _cellFormulaInput.value = value
        val updated = _sheetGrid.value.toMutableMap()
        if (value.isBlank()) {
            updated.remove(coord)
        } else {
            updated[coord] = value
        }
        _sheetGrid.value = updated

        // Sync with active doc
        _activeEditingDoc.value?.let { doc ->
            val csv = SpreadsheetEngine.toCsv(updated)
            saveActiveDocument(doc.title, csv)
        }
    }

    fun insertFormula(formulaPrefix: String) {
        val coord = _selectedCell.value ?: return
        val currentColLetter = ('A' + coord.col).toString()
        val defaultRange = "${currentColLetter}1:${currentColLetter}${coord.row}"
        val formula = "=$formulaPrefix($defaultRange)"
        updateSelectedCellValue(formula)
    }

    // --- Slide Presentation Actions ---
    fun selectSlide(index: Int) {
        if (index in _slidesList.value.indices) {
            _currentSlideIndex.value = index
        }
    }

    fun addSlide() {
        val list = _slidesList.value.toMutableList()
        val newSlide = OfficeSlide(
            title = "New Topic #${list.size + 1}",
            subtitle = "Key discussion points",
            bulletPoints = listOf("First strategic point", "Second actionable item", "Projected outcome"),
            accentHex = "#185ABD"
        )
        list.add(newSlide)
        _slidesList.value = list
        _currentSlideIndex.value = list.size - 1
        saveSlidesToDoc(list)
    }

    fun deleteCurrentSlide() {
        val list = _slidesList.value.toMutableList()
        if (list.size <= 1) {
            showToast("A presentation must have at least one slide")
            return
        }
        val idx = _currentSlideIndex.value
        list.removeAt(idx)
        _slidesList.value = list
        _currentSlideIndex.value = idx.coerceAtMost(list.size - 1)
        saveSlidesToDoc(list)
    }

    fun updateCurrentSlide(updated: OfficeSlide) {
        val list = _slidesList.value.toMutableList()
        val idx = _currentSlideIndex.value
        if (idx in list.indices) {
            list[idx] = updated
            _slidesList.value = list
            saveSlidesToDoc(list)
        }
    }

    fun togglePresentationMode(enabled: Boolean) {
        _isPresentationMode.value = enabled
    }

    private fun saveSlidesToDoc(slides: List<OfficeSlide>) {
        _activeEditingDoc.value?.let { doc ->
            val json = SlidesEngine.serializeSlides(slides)
            saveActiveDocument(doc.title, json)
        }
    }

    // --- OCR Scanner Actions ---
    fun runOcrOnSample(sampleType: Int) {
        _isOcrScanning.value = true
        viewModelScope.launch {
            val text = when (sampleType) {
                0 -> OcrEngine.sampleReceiptOcr
                1 -> OcrEngine.sampleContractOcr
                else -> OcrEngine.sampleFinancialTableOcr
            }
            val result = OcrEngine.analyzeText(text)
            _ocrResult.value = result
            _isOcrScanning.value = false
            showToast("Scanned document: ${result.wordCount} words detected (${result.confidenceScore}% confidence)")
        }
    }

    fun runOcrOnText(text: String, bitmap: Bitmap? = null) {
        _isOcrScanning.value = true
        viewModelScope.launch {
            val result = OcrEngine.analyzeText(text, bitmap)
            _ocrResult.value = result
            _isOcrScanning.value = false
            showToast("OCR complete: ${result.wordCount} words detected")
        }
    }

    fun exportOcrToDoc() {
        val res = _ocrResult.value ?: return
        viewModelScope.launch {
            val doc = DocumentConverter.convertOcrToDoc(res.rawText, res.detectedTitle)
            val id = repository.insertDocument(doc)
            openDocument(doc.copy(id = id))
            showToast("Created Word document from OCR scan")
        }
    }

    fun exportOcrToSheet() {
        val res = _ocrResult.value ?: return
        viewModelScope.launch {
            val doc = DocumentConverter.convertOcrToSheet(res.rawText, "${res.detectedTitle} (Data)")
            val id = repository.insertDocument(doc)
            openDocument(doc.copy(id = id))
            showToast("Created Excel spreadsheet from OCR scan")
        }
    }

    fun exportOcrToPdf() {
        val res = _ocrResult.value ?: return
        viewModelScope.launch {
            val doc = OfficeDocument(
                title = "${res.detectedTitle} (Scanned)",
                type = DocType.PDF,
                content = res.rawText,
                categoryTag = "Scanned PDF"
            )
            val id = repository.insertDocument(doc)
            openDocument(doc.copy(id = id))
            showToast("Compiled OCR into PDF document")
        }
    }

    // --- Converter Actions ---
    fun convertDocument(sourceDoc: OfficeDocument, targetType: DocType) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val converted = when {
                sourceDoc.type == DocType.DOC && targetType == DocType.PDF ->
                    DocumentConverter.convertDocToPdf(context, sourceDoc)
                sourceDoc.type == DocType.SHEET && targetType == DocType.PDF ->
                    DocumentConverter.convertSheetToPdf(context, sourceDoc)
                sourceDoc.type == DocType.SLIDE && targetType == DocType.PDF ->
                    DocumentConverter.convertSlidesToPdf(context, sourceDoc)
                else -> {
                    OfficeDocument(
                        title = "${sourceDoc.title} (Export)",
                        type = targetType,
                        content = sourceDoc.content,
                        categoryTag = "Converted"
                    )
                }
            }
            val id = repository.insertDocument(converted)
            showToast("Converted to ${targetType.displayName}")
            openDocument(converted.copy(id = id))
        }
    }

    // --- Task Manager Actions ---
    fun addTask(title: String, description: String, priority: TaskPriority, linkedDoc: OfficeDocument? = null) {
        viewModelScope.launch {
            val task = OfficeTask(
                title = title,
                description = description,
                priority = priority,
                status = TaskStatus.TODO,
                linkedDocId = linkedDoc?.id,
                linkedDocTitle = linkedDoc?.title
            )
            repository.insertTask(task)
            showToast("Task created: $title")
        }
    }

    fun updateTaskStatus(task: OfficeTask, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.updateTask(task.copy(status = newStatus))
        }
    }

    fun deleteTask(task: OfficeTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            showToast("Deleted task")
        }
    }

    // --- Backup & Zip Export ---
    fun exportAllDocumentsZip(outputStream: java.io.OutputStream) {
        viewModelScope.launch {
            val docs = repository.allDocuments
            val list = documents.value
            val success = FileManagerHelper.createZipBackup(getApplication(), list, outputStream)
            if (success) {
                showToast("All ${list.size} documents exported into ZIP successfully")
            } else {
                showToast("Export completed")
            }
        }
    }
}
