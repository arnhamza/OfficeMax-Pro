package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.ExcelGreenContainer
import com.example.ui.theme.PdfCrimson
import com.example.ui.theme.PdfCrimsonContainer
import com.example.ui.theme.PowerPointOrange
import com.example.ui.theme.PowerPointOrangeContainer
import com.example.ui.theme.ScannerPurple
import com.example.ui.theme.ScannerPurpleContainer
import com.example.ui.theme.WordBlue
import com.example.ui.theme.WordBlueContainer

enum class DocType(
    val displayName: String,
    val extension: String,
    val iconLetter: String,
    val primaryColor: Color,
    val containerColor: Color,
    val mimeType: String
) {
    DOC(
        displayName = "Word Document",
        extension = ".docx",
        iconLetter = "W",
        primaryColor = WordBlue,
        containerColor = WordBlueContainer,
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    SHEET(
        displayName = "Excel Spreadsheet",
        extension = ".xlsx",
        iconLetter = "X",
        primaryColor = ExcelGreen,
        containerColor = ExcelGreenContainer,
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    SLIDE(
        displayName = "PowerPoint Presentation",
        extension = ".pptx",
        iconLetter = "P",
        primaryColor = PowerPointOrange,
        containerColor = PowerPointOrangeContainer,
        mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    ),
    PDF(
        displayName = "PDF Document",
        extension = ".pdf",
        iconLetter = "PDF",
        primaryColor = PdfCrimson,
        containerColor = PdfCrimsonContainer,
        mimeType = "application/pdf"
    ),
    SCAN(
        displayName = "OCR Scanned Doc",
        extension = ".txt",
        iconLetter = "OCR",
        primaryColor = ScannerPurple,
        containerColor = ScannerPurpleContainer,
        mimeType = "text/plain"
    ),
    TXT(
        displayName = "Plain Text",
        extension = ".txt",
        iconLetter = "TXT",
        primaryColor = Color(0xFF5F6368),
        containerColor = Color(0xFFF1F3F4),
        mimeType = "text/plain"
    )
}

enum class TaskPriority(val label: String, val color: Color) {
    HIGH("High Priority", Color(0xFFD93025)),
    MEDIUM("Medium", Color(0xFFF29900)),
    LOW("Normal", Color(0xFF1E8E3E))
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    REVIEW("In Review"),
    COMPLETED("Completed")
}

enum class SortMode(val title: String) {
    MODIFIED_DESC("Recently Modified"),
    NAME_ASC("Name (A-Z)"),
    SIZE_DESC("File Size (Largest)")
}
