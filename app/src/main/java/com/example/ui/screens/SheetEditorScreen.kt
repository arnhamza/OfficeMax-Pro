package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.ui.OfficeViewModel
import com.example.ui.theme.ExcelGreen
import com.example.util.CellCoord
import com.example.util.SpreadsheetEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetEditorScreen(
    doc: OfficeDocument,
    viewModel: OfficeViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var title by remember(doc.id) { mutableStateOf(doc.title) }
    val grid by viewModel.sheetGrid.collectAsStateWithLifecycle()
    val selectedCell by viewModel.selectedCell.collectAsStateWithLifecycle()
    val formulaInput by viewModel.cellFormulaInput.collectAsStateWithLifecycle()

    var showChartSheet by remember { mutableStateOf(false) }
    var rowCount by remember { mutableIntStateOf(20) }
    var colCount by remember { mutableIntStateOf(6) }

    val evaluatedGrid = remember(grid) {
        SpreadsheetEngine.evaluateGrid(grid)
    }

    val horizontalScroll = rememberScrollState()

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
                            .testTag("sheet_title_input"),
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
                        modifier = Modifier.testTag("sheet_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showChartSheet = true },
                        modifier = Modifier.testTag("sheet_chart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "View Chart",
                            tint = ExcelGreen
                        )
                    }
                    IconButton(
                        onClick = { viewModel.convertDocument(doc.copy(title = title), DocType.PDF) },
                        modifier = Modifier.testTag("sheet_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = Color(0xFFE81123)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareDocument(doc.copy(title = title)) },
                        modifier = Modifier.testTag("sheet_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val csv = SpreadsheetEngine.toCsv(grid, rowCount, colCount)
                            viewModel.saveActiveDocument(title, csv)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("sheet_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Formula Bar (fx)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active cell badge (e.g. A1, B2)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(36.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCell?.toRef() ?: "A1",
                        fontWeight = FontWeight.Bold,
                        color = ExcelGreen,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.Functions,
                    contentDescription = "Formula fx",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedTextField(
                    value = formulaInput,
                    onValueChange = { viewModel.updateSelectedCellValue(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("formula_input_field"),
                    placeholder = { Text("Enter text, number or formula e.g. =SUM(B1:B5)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ExcelGreen,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
            }

            // Quick Formula Toolbar Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { viewModel.insertFormula("SUM") },
                    label = { Text("=SUM(..)") },
                    modifier = Modifier.testTag("btn_formula_sum")
                )
                AssistChip(
                    onClick = { viewModel.insertFormula("AVG") },
                    label = { Text("=AVG(..)") },
                    modifier = Modifier.testTag("btn_formula_avg")
                )
                AssistChip(
                    onClick = { viewModel.insertFormula("COUNT") },
                    label = { Text("=COUNT(..)") }
                )
                AssistChip(
                    onClick = { viewModel.insertFormula("MAX") },
                    label = { Text("=MAX(..)") }
                )
                AssistChip(
                    onClick = { viewModel.insertFormula("MIN") },
                    label = { Text("=MIN(..)") }
                )
                AssistChip(
                    onClick = { rowCount += 5 },
                    label = { Text("+5 Rows") }
                )
                AssistChip(
                    onClick = { if (colCount < 12) colCount += 1 },
                    label = { Text("+1 Col") }
                )
            }

            // 2D SpreadSheet Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScroll)
            ) {
                Column {
                    // Column Headers (Corner + A, B, C, D...)
                    Row(
                        modifier = Modifier.background(Color(0xFFE2E8F0))
                    ) {
                        // Corner empty box
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(32.dp)
                                .border(0.5.dp, Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }

                        for (c in 0 until colCount) {
                            val colLetter = SpreadsheetEngine.columnHeader(c)
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(32.dp)
                                    .border(0.5.dp, Color(0xFFCBD5E1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = colLetter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }

                    // Grid Rows
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rowCount) { r ->
                            Row {
                                // Row Number Header
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(38.dp)
                                        .background(Color(0xFFF1F5F9))
                                        .border(0.5.dp, Color(0xFFCBD5E1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${r + 1}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                for (c in 0 until colCount) {
                                    val coord = CellCoord(r, c)
                                    val isSelected = selectedCell == coord
                                    val displayVal = evaluatedGrid[coord] ?: ""

                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(38.dp)
                                            .background(if (isSelected) Color(0xFFE6F4EA) else Color.White)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) ExcelGreen else Color(0xFFE2E8F0)
                                            )
                                            .clickable { viewModel.selectCell(coord) }
                                            .padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = displayVal,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (r == 0 || displayVal.startsWith("Total") || displayVal.startsWith("Average")) FontWeight.Bold else FontWeight.Normal,
                                            color = if (displayVal.startsWith("#ERROR")) Color.Red else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Chart Preview Bottom Sheet
    if (showChartSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showChartSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Spreadsheet Data Visualization",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExcelGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dynamic bar chart generated from your spreadsheet rows",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Extract numeric values from first numeric column
                val chartData = remember(evaluatedGrid) {
                    val list = mutableListOf<Pair<String, Float>>()
                    for (r in 1 until 10) {
                        val label = evaluatedGrid[CellCoord(r, 0)] ?: "Row ${r + 1}"
                        val valStr = evaluatedGrid[CellCoord(r, 1)] ?: "0"
                        val num = valStr.replace("$", "").replace(",", "").toFloatOrNull() ?: 0f
                        if (num > 0f) {
                            list.add(label to num)
                        }
                    }
                    if (list.isEmpty()) {
                        listOf("Q1 Actual" to 12500f, "Q2 Actual" to 13200f, "Q3 Target" to 14000f, "Q4 Forecast" to 15500f)
                    } else list
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    val maxVal = remember(chartData) {
                        chartData.maxOfOrNull { it.second } ?: 100f
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val barWidth = (size.width / (chartData.size * 1.6f)).coerceIn(24f, 60f)
                        val spacing = size.width / chartData.size

                        chartData.forEachIndexed { i, pair ->
                            val barHeight = (pair.second / maxVal) * (size.height - 40f)
                            val x = (i * spacing) + (spacing - barWidth) / 2f
                            val y = size.height - barHeight - 20f

                            // Draw Bar
                            drawRoundRect(
                                color = ExcelGreen,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Legend
                chartData.forEach { (lbl, v) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(lbl, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF334155))
                        Text(
                            "$${"%,.0f".format(v)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExcelGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
