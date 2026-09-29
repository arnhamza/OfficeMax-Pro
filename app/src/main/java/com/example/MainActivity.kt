package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.ui.ActiveToolScreen
import com.example.ui.MainTab
import com.example.ui.OfficeViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.screens.ApkExportScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.DocEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OcrScannerScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.SheetEditorScreen
import com.example.ui.screens.SlideEditorScreen
import com.example.ui.screens.TaskManagerScreen
import com.example.ui.theme.ExcelGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OfficeBluePrimary
import com.example.ui.theme.PdfCrimson
import com.example.ui.theme.PowerPointOrange
import com.example.ui.theme.ScannerPurple
import com.example.ui.theme.WordBlue

class MainActivity : ComponentActivity() {

    private val viewModel: OfficeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle opening external files directly from Android file managers
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            intent.data?.let { uri ->
                viewModel.importFileFromUri(uri)
            }
        }

        setContent {
            MyApplicationTheme {
                OfficeApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OfficeApp(viewModel: OfficeViewModel) {
    val activeDoc by viewModel.activeEditingDoc.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeToolScreen.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // 1. If a document is currently opened for editing
    if (activeDoc != null) {
        val doc = activeDoc!!
        when (doc.type) {
            DocType.DOC, DocType.TXT -> DocEditorScreen(
                doc = doc,
                viewModel = viewModel,
                onBack = { viewModel.closeActiveDocument() }
            )
            DocType.SHEET -> SheetEditorScreen(
                doc = doc,
                viewModel = viewModel,
                onBack = { viewModel.closeActiveDocument() }
            )
            DocType.SLIDE -> SlideEditorScreen(
                doc = doc,
                viewModel = viewModel,
                onBack = { viewModel.closeActiveDocument() }
            )
            DocType.PDF -> PdfViewerScreen(
                doc = doc,
                viewModel = viewModel,
                onBack = { viewModel.closeActiveDocument() }
            )
            DocType.SCAN -> OcrScannerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeActiveDocument() }
            )
        }
        return
    }

    // 2. If a specific tool screen is opened
    if (activeTool != null) {
        when (activeTool) {
            ActiveToolScreen.OCR_SCANNER -> OcrScannerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeTool() }
            )
            ActiveToolScreen.FORMAT_CONVERTER -> ConverterScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeTool() }
            )
            ActiveToolScreen.TASK_MANAGER -> TaskManagerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeTool() }
            )
            ActiveToolScreen.APK_EXPORT_CENTER -> ApkExportScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeTool() }
            )
            ActiveToolScreen.PDF_VIEWER_SIGNER -> {
                val dummyPdf = OfficeDocument(
                    title = "New Document.pdf",
                    type = DocType.PDF,
                    content = "Blank PDF document ready for digital signature and certification."
                )
                PdfViewerScreen(
                    doc = dummyPdf,
                    viewModel = viewModel,
                    onBack = { viewModel.closeTool() }
                )
            }
            null -> {}
        }
        return
    }

    // 3. Main Navigation Shell
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.HOME,
                    onClick = { viewModel.setTab(MainTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_home"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.DOCS,
                    onClick = { viewModel.setTab(MainTab.DOCS) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Docs", tint = WordBlue) },
                    label = { Text("Docs", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_docs"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SHEETS,
                    onClick = { viewModel.setTab(MainTab.SHEETS) },
                    icon = { Icon(Icons.Default.TableChart, contentDescription = "Sheets", tint = ExcelGreen) },
                    label = { Text("Sheets", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_sheets"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SLIDES,
                    onClick = { viewModel.setTab(MainTab.SLIDES) },
                    icon = { Icon(Icons.Default.Slideshow, contentDescription = "Slides", tint = PowerPointOrange) },
                    label = { Text("Slides", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_slides"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.TOOLS,
                    onClick = { viewModel.setTab(MainTab.TOOLS) },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools", tint = OfficeBluePrimary) },
                    label = { Text("Tools", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_tools"),
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MainTab.HOME -> HomeScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.DOCS -> CategoryTabScreen(
                type = DocType.DOC,
                title = "Word Documents",
                subtitle = "Rich text authoring, templates & reports",
                brandColor = WordBlue,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.SHEETS -> CategoryTabScreen(
                type = DocType.SHEET,
                title = "Excel Spreadsheets",
                subtitle = "Data tables, formulas (=SUM, =AVG) & charts",
                brandColor = ExcelGreen,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.SLIDES -> CategoryTabScreen(
                type = DocType.SLIDE,
                title = "PowerPoint Presentations",
                subtitle = "Slide decks, presenter notes & fullscreen slideshow",
                brandColor = PowerPointOrange,
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            MainTab.TOOLS -> ToolsHubScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun CategoryTabScreen(
    type: DocType,
    title: String,
    subtitle: String,
    brandColor: Color,
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val typeDocs = remember(documents, type) {
        documents.filter { it.type == type }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNewDocument(type) },
                containerColor = brandColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_create_${type.name.lowercase()}")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create New")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = type.containerColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = brandColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.createNewDocument(type) },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create ${type.displayName}", fontSize = 12.sp)
                    }
                }
            }

            // Documents List
            if (typeDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No ${type.displayName}s yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button to create a new file or open from device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(typeDocs, key = { it.id }) { doc ->
                        DocumentCard(
                            doc = doc,
                            onClick = { viewModel.openDocument(doc) },
                            onDelete = { viewModel.deleteDocument(doc) },
                            onShare = { viewModel.shareDocument(doc) },
                            onConvertToPdf = { viewModel.convertDocument(doc, DocType.PDF) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsHubScreen(
    viewModel: OfficeViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Office Productivity Toolkit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "OCR text extraction, all format converter, project task manager & APK export",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        ToolMenuCard(
            title = "OCR Document Scanner",
            subtitle = "Extract text, dates, phones and currency amounts from receipts or camera photos",
            icon = Icons.Default.CameraAlt,
            iconTint = ScannerPurple,
            containerColor = Color(0xFFF3EDFB),
            onClick = { viewModel.openTool(ActiveToolScreen.OCR_SCANNER) }
        )

        ToolMenuCard(
            title = "All-in-One Format Converter",
            subtitle = "Convert Docs to PDF, Spreadsheets to PDF tables, or Photos to PDF",
            icon = Icons.Default.SwapHoriz,
            iconTint = OfficeBluePrimary,
            containerColor = Color(0xFFE0F2FE),
            onClick = { viewModel.openTool(ActiveToolScreen.FORMAT_CONVERTER) }
        )

        ToolMenuCard(
            title = "Office Task Manager",
            subtitle = "Manage deadlines, review action items, and link tasks directly to documents",
            icon = Icons.Default.TaskAlt,
            iconTint = Color(0xFFB46500),
            containerColor = Color(0xFFFEF7E0),
            onClick = { viewModel.openTool(ActiveToolScreen.TASK_MANAGER) }
        )

        ToolMenuCard(
            title = "PDF Studio & Digital Signature",
            subtitle = "Certify documents, apply handwritten signatures and watermark stamps",
            icon = Icons.Default.PictureAsPdf,
            iconTint = PdfCrimson,
            containerColor = Color(0xFFFDE8E9),
            onClick = { viewModel.openTool(ActiveToolScreen.PDF_VIEWER_SIGNER) }
        )

        ToolMenuCard(
            title = "Direct APK & Backup Center",
            subtitle = "Instructions to download APK directly in AI Studio and export all files into a ZIP",
            icon = Icons.Default.Android,
            iconTint = Color(0xFFD97706),
            containerColor = Color(0xFFFEF3C7),
            onClick = { viewModel.openTool(ActiveToolScreen.APK_EXPORT_CENTER) }
        )
    }
}

@Composable
fun ToolMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tool_card_${title.lowercase().replace(" ", "_").take(15)}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
