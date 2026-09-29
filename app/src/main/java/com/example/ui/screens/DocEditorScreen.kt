package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.ui.OfficeViewModel
import com.example.ui.theme.WordBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScreen(
    doc: OfficeDocument,
    viewModel: OfficeViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var title by remember(doc.id) { mutableStateOf(doc.title) }
    var content by remember(doc.id) { mutableStateOf(doc.content) }

    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(content) { content.length }
    val readingTimeMin = remember(wordCount) {
        maxOf(1, (wordCount / 200))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("doc_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("doc_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.convertDocument(doc.copy(title = title, content = content), DocType.PDF) },
                        modifier = Modifier.testTag("doc_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = Color(0xFFE81123)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareDocument(doc.copy(title = title, content = content)) },
                        modifier = Modifier.testTag("doc_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.saveActiveDocument(title, content) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("doc_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            // Formatting Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Word Brand Pill
                Box(
                    modifier = Modifier
                        .background(WordBlue, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("DOCX", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { content = "$content\n\n**Bold Text**" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { content = "$content\n\n*Italic Text*" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { content = "$content\n\n# Main Heading\n" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Title, contentDescription = "Heading", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { content = "$content\n• Bullet item 1\n• Bullet item 2\n" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { content = "$content\n1. Numbered item 1\n2. Numbered item 2\n" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { content = "$content\n> Quote or callout note\n" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.FormatQuote, contentDescription = "Quote", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Templates
                AssistChip(
                    onClick = {
                        content = """
                            # MEETING MINUTES
                            Date: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                            Attendees: Lead Architect, Product Manager, QA Engineer
                            
                            ## 1. Agenda
                            • Sprint deliverables & platform integration
                            • Document scanner performance benchmarks
                            
                            ## 2. Action Items
                            1. Export release APK build for device QA
                            2. Review offline formula calculation engine
                        """.trimIndent()
                    },
                    label = { Text("Meeting Notes") }
                )
                AssistChip(
                    onClick = {
                        content = """
                            # PROJECT PROPOSAL & CHARTER
                            Project: Office Suite Mobile
                            Owner: Engineering Operations
                            
                            ## Executive Summary
                            Deliver a native Android Office Suite with real file manager access, full document editing, and PDF tools.
                            
                            ## Scope
                            • Offline-first Room storage
                            • Full conversion between formats
                            • OCR scanning with ML Kit heuristics
                        """.trimIndent()
                    },
                    label = { Text("Proposal") }
                )
            }

            // Word / Character Count Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount words   |   $charCount characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "~$readingTimeMin min read",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )
            }

            // A4 Paper Document Canvas View
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .testTag("doc_content_input"),
                    placeholder = { Text("Start typing your document or insert a template...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        color = Color(0xFF1E293B)
                    )
                )
            }
        }
    }
}
