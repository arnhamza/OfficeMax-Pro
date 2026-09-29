package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocType
import com.example.data.model.OfficeDocument
import com.example.ui.OfficeViewModel
import com.example.ui.theme.PowerPointOrange
import com.example.util.OfficeSlide
import com.example.util.SlideLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideEditorScreen(
    doc: OfficeDocument,
    viewModel: OfficeViewModel,
    onBack: () -> Unit
) {
    val isPresentationMode by viewModel.isPresentationMode.collectAsStateWithLifecycle()
    val slides by viewModel.slidesList.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentSlideIndex.collectAsStateWithLifecycle()

    BackHandler {
        if (isPresentationMode) {
            viewModel.togglePresentationMode(false)
        } else {
            onBack()
        }
    }

    if (slides.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading presentation...")
        }
        return
    }

    val currentSlide = slides.getOrElse(currentIndex) { slides.first() }

    // If Presentation Mode is active, show the clean full-screen keynote slideshow!
    if (isPresentationMode) {
        PresentationModeView(
            slides = slides,
            currentIndex = currentIndex,
            onSelectSlide = { viewModel.selectSlide(it) },
            onExit = { viewModel.togglePresentationMode(false) }
        )
        return
    }

    var title by remember(doc.id) { mutableStateOf(doc.title) }

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
                            .testTag("slide_title_input"),
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
                        modifier = Modifier.testTag("slide_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Fullscreen Presentation Mode Button
                    Button(
                        onClick = { viewModel.togglePresentationMode(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = PowerPointOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("btn_start_slideshow")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Present", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = { viewModel.convertDocument(doc.copy(title = title), DocType.PDF) },
                        modifier = Modifier.testTag("slide_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = Color(0xFFE81123)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.shareDocument(doc.copy(title = title)) },
                        modifier = Modifier.testTag("slide_share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
        ) {
            // Slide Canvas Card (16:9 aspect ratio presentation preview)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(16f / 10f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                SlideCanvasPreview(
                    slide = currentSlide,
                    slideNumber = currentIndex + 1,
                    totalSlides = slides.size
                )
            }

            // Slide Layout Picker & Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layout:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                SlideLayout.values().forEach { layout ->
                    AssistChip(
                        onClick = { viewModel.updateCurrentSlide(currentSlide.copy(layout = layout)) },
                        label = { Text(layout.label) },
                        border = if (currentSlide.layout == layout) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, PowerPointOrange)
                        } else null
                    )
                }

                IconButton(
                    onClick = { viewModel.deleteCurrentSlide() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Slide",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Editable Fields for current slide
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = currentSlide.title,
                    onValueChange = { viewModel.updateCurrentSlide(currentSlide.copy(title = it)) },
                    label = { Text("Slide Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("slide_edit_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerPointOrange
                    )
                )

                OutlinedTextField(
                    value = currentSlide.subtitle,
                    onValueChange = { viewModel.updateCurrentSlide(currentSlide.copy(subtitle = it)) },
                    label = { Text("Subtitle / Header Note") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PowerPointOrange
                    )
                )

                if (currentSlide.layout == SlideLayout.STAT_METRIC) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currentSlide.statNumber,
                            onValueChange = { viewModel.updateCurrentSlide(currentSlide.copy(statNumber = it)) },
                            label = { Text("Key Metric (e.g. +240%)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currentSlide.statLabel,
                            onValueChange = { viewModel.updateCurrentSlide(currentSlide.copy(statLabel = it)) },
                            label = { Text("Metric Description") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = currentSlide.bulletPoints.joinToString("\n"),
                        onValueChange = { newText ->
                            val list = newText.lines().filter { it.isNotBlank() }
                            viewModel.updateCurrentSlide(currentSlide.copy(bulletPoints = list))
                        },
                        label = { Text("Bullet Points (one per line)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PowerPointOrange
                        )
                    )
                }

                OutlinedTextField(
                    value = currentSlide.speakerNotes,
                    onValueChange = { viewModel.updateCurrentSlide(currentSlide.copy(speakerNotes = it)) },
                    label = { Text("Speaker Notes (Presenter View)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = true
                )
            }

            // Bottom Thumbnail Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Slides (${slides.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    FilledTonalButton(
                        onClick = { viewModel.addSlide() },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_add_slide")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Slide", fontSize = 11.sp)
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(slides) { idx, slide ->
                        val isSelected = idx == currentIndex
                        Card(
                            modifier = Modifier
                                .width(88.dp)
                                .height(56.dp)
                                .clickable { viewModel.selectSlide(idx) }
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) PowerPointOrange else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PowerPointOrange
                                )
                                Text(
                                    text = slide.title,
                                    fontSize = 9.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SlideCanvasPreview(
    slide: OfficeSlide,
    slideNumber: Int,
    totalSlides: Int,
    isFullScreen: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isFullScreen) 32.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Brand Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isFullScreen) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(PowerPointOrange)
            )
            Text(
                text = "Slide $slideNumber of $totalSlides",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = if (isFullScreen) 14.sp else 10.sp
            )
        }

        // Slide Content depending on layout
        when (slide.layout) {
            SlideLayout.TITLE_SLIDE -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = slide.title,
                        style = if (isFullScreen) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    if (slide.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = slide.subtitle,
                            style = if (isFullScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                            color = PowerPointOrange,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            SlideLayout.STAT_METRIC -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = slide.title,
                        style = if (isFullScreen) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = slide.statNumber.ifEmpty { "+100%" },
                        fontSize = if (isFullScreen) 64.sp else 36.sp,
                        fontWeight = FontWeight.Black,
                        color = PowerPointOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = slide.statLabel.ifEmpty { "Projected growth" },
                        style = if (isFullScreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            }
            SlideLayout.QUOTE -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = slide.title,
                        style = if (isFullScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                        color = PowerPointOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = slide.bulletPoints.firstOrNull() ?: "\"Inspiring keynote vision.\"",
                        style = if (isFullScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                // BULLET_LIST or TWO_COLUMN
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = slide.title,
                        style = if (isFullScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (slide.subtitle.isNotEmpty()) {
                        Text(
                            text = slide.subtitle,
                            style = if (isFullScreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                            color = PowerPointOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    slide.bulletPoints.take(if (isFullScreen) 5 else 3).forEach { pt ->
                        Row(
                            modifier = Modifier.padding(vertical = if (isFullScreen) 4.dp else 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "• ",
                                color = PowerPointOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isFullScreen) 18.sp else 12.sp
                            )
                            Text(
                                text = pt,
                                style = if (isFullScreen) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Slide Footer
        Text(
            text = "OfficePro Suite Keynote",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            fontSize = if (isFullScreen) 12.sp else 8.sp
        )
    }
}

@Composable
fun PresentationModeView(
    slides: List<OfficeSlide>,
    currentIndex: Int,
    onSelectSlide: (Int) -> Unit,
    onExit: () -> Unit
) {
    val slide = slides.getOrElse(currentIndex) { slides.first() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .testTag("presentation_mode_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(PowerPointOrange, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("PRESENTATION MODE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${currentIndex + 1} / ${slides.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onExit,
                    modifier = Modifier.testTag("btn_exit_slideshow")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Slideshow",
                        tint = Color.White
                    )
                }
            }

            // Big Centered Slide
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    SlideCanvasPreview(
                        slide = slide,
                        slideNumber = currentIndex + 1,
                        totalSlides = slides.size,
                        isFullScreen = true
                    )
                }
            }

            // Speaker Notes & Bottom Navigation Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(16.dp)
            ) {
                if (slide.speakerNotes.isNotEmpty()) {
                    Text(
                        text = "SPEAKER NOTES: ${slide.speakerNotes}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { if (currentIndex > 0) onSelectSlide(currentIndex - 1) },
                        enabled = currentIndex > 0,
                        modifier = Modifier.testTag("btn_slide_prev")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    FilledTonalButton(
                        onClick = { if (currentIndex < slides.size - 1) onSelectSlide(currentIndex + 1) },
                        enabled = currentIndex < slides.size - 1,
                        modifier = Modifier.testTag("btn_slide_next")
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
