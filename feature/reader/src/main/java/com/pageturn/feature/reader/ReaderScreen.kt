package com.pageturn.feature.reader

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pageturn.core.designsystem.theme.*
import com.pageturn.core.model.Highlight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.Bitmap
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        }
        onDispose {
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
        }
    }


    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val scrollState = rememberScrollState()

    var showControlMenu by remember { mutableStateOf(false) }
    // Paragraph selection / actions states
    var selectedParagraphIndex by remember { mutableStateOf(-1) }
    var selectedParagraphText by remember { mutableStateOf("") }
    var showParagraphActions by remember { mutableStateOf(false) }
    var showQuoteCardDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }
    var customHighlightText by remember { mutableStateOf("") }
    
    var showColorPickerSheet by remember { mutableStateOf(false) }
    var activeSelectedText by remember { mutableStateOf("") }
    var activeParagraphIndex by remember { mutableStateOf(-1) }
    var activeSelectedStartChar by remember { mutableStateOf(0) }
    var activeNoteColor by remember { mutableStateOf("#FFF176") }

    LaunchedEffect(selectedParagraphText) {
        // Do NOT auto-fill the highlight text field - user should select/type what they want
        customHighlightText = ""
    }

    // Dynamic background colors according to theme setting
    val currentBgColor = when (settings.readingTheme) {
        "dark" -> Color(0xFF121212)
        "light" -> Color.White
        else -> PtBackgroundWarm // "warm"
    }
    
    val currentSurfaceColor = when (settings.readingTheme) {
        "dark" -> Color(0xFF1E1E1E)
        "light" -> Color.White
        else -> PtSurfaceWarm
    }

    val currentTextColor = when (settings.readingTheme) {
        "dark" -> Color(0xFFE0E0E0)
        "light" -> PtTextMain
        else -> PtTextWarm
    }

    val currentTextSecondaryColor = when (settings.readingTheme) {
        "dark" -> Color(0xFF8A8A93)
        "light" -> PtTextSecondary
        else -> PtTextSecondary
    }

    val currentTitleColor = when (settings.readingTheme) {
        "dark" -> Color.White
        "light" -> PtTextNavy
        else -> PtTextNavy
    }

    // Dynamic Button Colors to fix "black buttons" issue in Dark Theme
    val currentButtonContainerColor = when (settings.readingTheme) {
        "dark" -> Color(0xFF33333F)
        "light" -> PtNavyPrimary
        else -> PtNavyPrimary
    }

    val currentButtonContentColor = when (settings.readingTheme) {
        "dark" -> Color.White
        "light" -> Color.White
        else -> Color.White
    }

    val currentIconTint = when (settings.readingTheme) {
        "dark" -> Color(0xFFD0D0D8)
        "light" -> PtNavyPrimary
        else -> PtNavyPrimary
    }

    val currentFontFamily = if (settings.fontFamily == "sans-serif") FontFamily.SansSerif else FontFamily.Serif


    // Paragraph actions menu sheet
    if (showParagraphActions) {
        ModalBottomSheet(
            onDismissRequest = { showParagraphActions = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = currentSurfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lựa chọn đoạn văn để lưu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = currentTitleColor
                )

                OutlinedTextField(
                    value = customHighlightText,
                    onValueChange = { customHighlightText = it },
                    label = { Text("Nhập đoạn/từ muốn Highlight") },
                    placeholder = { Text("Gõ hoặc dán đoạn văn bạn muốn đánh dấu...", color = currentTextSecondaryColor) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTextColor,
                        unfocusedTextColor = currentTextColor
                    )
                )

                // Quick-fill button
                TextButton(
                    onClick = { customHighlightText = selectedParagraphText },
                    colors = ButtonDefaults.textButtonColors(contentColor = currentIconTint)
                ) {
                    Icon(imageVector = Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dùng toàn bộ đoạn", fontSize = 12.sp)
                }

                Divider(color = currentIconTint.copy(alpha = 0.2f))

                // Highlight Color selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Màu Highlight", style = MaterialTheme.typography.labelMedium, color = currentTextSecondaryColor)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Yellow highlight
                        Box(
                            modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFF176))
                                        .clickable {
                                            viewModel.addHighlight(
                                                chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                                startOffset = selectedParagraphIndex,
                                                endOffset = selectedParagraphIndex,
                                                colorHex = "#FFF176",
                                                selectedText = customHighlightText
                                            )
                                            showParagraphActions = false
                                        }
                        )
                        // Green highlight
                        Box(
                            modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFA5D6A7))
                                        .clickable {
                                            viewModel.addHighlight(
                                                chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                                startOffset = selectedParagraphIndex,
                                                endOffset = selectedParagraphIndex,
                                                colorHex = "#A5D6A7",
                                                selectedText = customHighlightText
                                            )
                                            showParagraphActions = false
                                        }
                        )
                        // Pink highlight
                        Box(
                            modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF48FB1))
                                        .clickable {
                                            viewModel.addHighlight(
                                                chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                                startOffset = selectedParagraphIndex,
                                                endOffset = selectedParagraphIndex,
                                                colorHex = "#F48FB1",
                                                selectedText = customHighlightText
                                            )
                                            showParagraphActions = false
                                        }
                        )
                        // Blue highlight
                        Box(
                            modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF90CAF9))
                                        .clickable {
                                            viewModel.addHighlight(
                                                chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                                startOffset = selectedParagraphIndex,
                                                endOffset = selectedParagraphIndex,
                                                colorHex = "#90CAF9",
                                                selectedText = customHighlightText
                                            )
                                            showParagraphActions = false
                                        }
                        )
                        
                        // Clear highlight action
                        TextButton(
                            onClick = {
                                viewModel.removeHighlightForParagraph(
                                    chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                    paragraphIndex = selectedParagraphIndex
                                )
                                showParagraphActions = false
                            }
                        ) {
                            Text("Xóa Màu", color = Color.Red)
                        }
                    }
                }

                Divider(color = currentIconTint.copy(alpha = 0.2f))

                // Other Contextual Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            noteInputText = ""
                            showAddNoteDialog = true
                            showParagraphActions = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentButtonContentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ghi chú", color = currentButtonContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showQuoteCardDialog = true
                            showParagraphActions = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentButtonContentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quote Card", color = currentButtonContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.speak(selectedParagraphText)
                            showParagraphActions = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentButtonContentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đọc đoạn", color = currentButtonContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add note dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Thêm Ghi chú cho đoạn văn", color = currentTextColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = noteInputText,
                        onValueChange = { noteInputText = it },
                        label = { Text("Nội dung ghi chú...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTextColor,
                            unfocusedTextColor = currentTextColor
                        )
                    )
                    
                    Text("Màu đánh dấu:", style = MaterialTheme.typography.bodyMedium, color = currentTextColor)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val noteColors = listOf("#FFF176", "#A5D6A7", "#F48FB1", "#90CAF9")
                        noteColors.forEach { colorStr ->
                            val colorValue = Color(android.graphics.Color.parseColor(colorStr))
                            val isSelected = activeNoteColor == colorStr
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) PtNavyPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { activeNoteColor = colorStr }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addHighlight(
                            chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                            startOffset = selectedParagraphIndex,
                            endOffset = activeSelectedStartChar,
                            colorHex = activeNoteColor,
                            noteText = noteInputText,
                            selectedText = customHighlightText
                        )
                        showAddNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor)
                ) {
                    Text("Lưu", color = currentButtonContentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddNoteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = currentIconTint)
                ) {
                    Text("Hủy", color = currentIconTint, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showColorPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorPickerSheet = false },
            containerColor = currentSurfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đánh dấu đoạn văn đã chọn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = currentTitleColor
                )
                
                Text(
                    text = "\"$activeSelectedText\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = currentTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTextColor.copy(alpha = 0.05f))
                        .padding(8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Yellow bubble
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF176))
                            .clickable {
                                viewModel.addHighlight(
                                    chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                    startOffset = activeParagraphIndex,
                                    endOffset = activeSelectedStartChar,
                                    colorHex = "#FFF176",
                                    selectedText = activeSelectedText
                                )
                                showColorPickerSheet = false
                            }
                    )
                    // Green bubble
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA5D6A7))
                            .clickable {
                                viewModel.addHighlight(
                                    chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                    startOffset = activeParagraphIndex,
                                    endOffset = activeSelectedStartChar,
                                    colorHex = "#A5D6A7",
                                    selectedText = activeSelectedText
                                )
                                showColorPickerSheet = false
                            }
                    )
                    // Pink bubble
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF48FB1))
                            .clickable {
                                viewModel.addHighlight(
                                    chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                    startOffset = activeParagraphIndex,
                                    endOffset = activeSelectedStartChar,
                                    colorHex = "#F48FB1",
                                    selectedText = activeSelectedText
                                )
                                showColorPickerSheet = false
                            }
                    )
                    // Blue bubble
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF90CAF9))
                            .clickable {
                                viewModel.addHighlight(
                                    chapterNumber = (uiState as? ReaderUiState.Success)?.chapter?.chapterNumber ?: 1,
                                    startOffset = activeParagraphIndex,
                                    endOffset = activeSelectedStartChar,
                                    colorHex = "#90CAF9",
                                    selectedText = activeSelectedText
                                )
                                showColorPickerSheet = false
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            noteInputText = ""
                            customHighlightText = activeSelectedText
                            selectedParagraphIndex = activeParagraphIndex
                            // Keep activeSelectedStartChar and activeNoteColor preserved
                            showAddNoteDialog = true
                            showColorPickerSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentButtonContentColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ghi chú", fontSize = 12.sp, color = currentButtonContentColor)
                    }

                    OutlinedButton(
                        onClick = { showColorPickerSheet = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = currentTextColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Premium Quote Card Drawing Dialog
    if (showQuoteCardDialog) {
        AlertDialog(
            onDismissRequest = { showQuoteCardDialog = false },
            title = {
                Text(
                    "Quote Card",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Premium Quote Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        // Background gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF061A2B),
                                            Color(0xFF0A3759)
                                        )
                                    )
                                )
                        )

                        // Decorative elements drawn on Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Large subtle circle top-right
                            drawCircle(
                                color = Color.White.copy(alpha = 0.04f),
                                radius = 120.dp.toPx(),
                                center = Offset(w * 0.85f, h * 0.15f)
                            )
                            // Gold horizontal line at top & bottom
                            drawLine(
                                color = PtGoldenAccent.copy(alpha = 0.6f),
                                start = Offset(24.dp.toPx(), 20.dp.toPx()),
                                end = Offset(w - 24.dp.toPx(), 20.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = PtGoldenAccent.copy(alpha = 0.6f),
                                start = Offset(24.dp.toPx(), h - 20.dp.toPx()),
                                end = Offset(w - 24.dp.toPx(), h - 20.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                            // Corner dots
                            drawCircle(PtGoldenAccent, 3.dp.toPx(), Offset(24.dp.toPx(), 20.dp.toPx()))
                            drawCircle(PtGoldenAccent, 3.dp.toPx(), Offset(w - 24.dp.toPx(), 20.dp.toPx()))
                            drawCircle(PtGoldenAccent, 3.dp.toPx(), Offset(24.dp.toPx(), h - 20.dp.toPx()))
                            drawCircle(PtGoldenAccent, 3.dp.toPx(), Offset(w - 24.dp.toPx(), h - 20.dp.toPx()))
                        }

                        // Content overlay
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 28.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "“",
                                fontSize = 40.sp,
                                fontFamily = FontFamily.Serif,
                                color = PtGoldenAccent,
                                lineHeight = 20.sp,
                                modifier = Modifier.offset(y = 8.dp)
                            )
                            Text(
                                text = selectedParagraphText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                ),
                                maxLines = 5,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(1.dp)
                                    .background(PtGoldenAccent.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "— ${(uiState as? ReaderUiState.Success)?.bookTitle ?: "PageTurn"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = PtGoldenAccent
                                )
                            )
                        }
                    }

                    Text(
                        text = "Thiết kế thẻ trích dẫn cao cấp.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareMsg = "\"$selectedParagraphText\"\n— Trích từ sách \"${(uiState as? ReaderUiState.Success)?.bookTitle ?: "PageTurn"}\""
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareMsg)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ trích dẫn"))
                        showQuoteCardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = currentButtonContentColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chia sẻ", color = currentButtonContentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQuoteCardDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = currentIconTint)
                ) {
                    Text("Đóng", color = currentIconTint, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = currentBgColor
    ) { innerPadding ->
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = currentIconTint)
                }
            }
            is ReaderUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
            is ReaderUiState.Success -> {
                val isBookmarked by viewModel.isBookmarked(chapterNumber = state.chapter.chapterNumber, pageNumber = 42).collectAsState(initial = false)
                val highlights by viewModel.getHighlights(state.chapter.chapterNumber).collectAsState(initial = emptyList())

                // Dynamic Pagination: PDF = 1 image per page, text = character budget pagination
                val paragraphs = state.chapter.content.split("\n\n")
                val isPdfContent = paragraphs.any { it.startsWith("[page_image:") && it.endsWith("]") }
                val pdfParagraphs = if (isPdfContent)
                    paragraphs.filter { it.startsWith("[page_image:") && it.endsWith("]") }
                else emptyList()

                val paginatedTextPages = remember(state.chapter.content, settings.fontSizeSp) {
                    if (isPdfContent) emptyList() else paginateText(state.chapter.content, settings.fontSizeSp)
                }

                val totalPages = if (isPdfContent) pdfParagraphs.size else paginatedTextPages.size

                val initialParagraph = viewModel.initialParagraph
                val targetPage = if (isPdfContent) {
                    initialParagraph
                } else {
                    val pageIdx = paginatedTextPages.indexOfFirst { pageList ->
                        pageList.any { it.absoluteIndex == initialParagraph }
                    }
                    if (pageIdx != -1) pageIdx else 0
                }
                val pagerState = rememberPagerState(initialPage = targetPage.coerceIn(0, totalPages - 1)) { totalPages }

                var isFirstLoad by remember { mutableStateOf(true) }
                // Reset page when a new chapter loads
                LaunchedEffect(state.chapter.id) {
                    if (isFirstLoad) {
                        isFirstLoad = false
                        pagerState.scrollToPage(targetPage.coerceIn(0, totalPages - 1))
                    } else {
                        pagerState.scrollToPage(0)
                    }
                }

                // Sync current page changes back to progress
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.updateProgress(pagerState.currentPage + 1, totalPages)
                }

                var lastScaleTime by remember { mutableStateOf(0L) }
                Scaffold(
                    modifier = Modifier.padding(innerPadding),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(currentBgColor)
                                .statusBarsPadding()
                                .height(44.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = currentIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.bookTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = currentTitleColor
                                ),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            // Nút di chuyển trang nhanh
                            var showGoToPageDialog by remember { mutableStateOf(false) }
                            var pageInputText by remember { mutableStateOf("") }

                            if (showGoToPageDialog) {
                                AlertDialog(
                                    onDismissRequest = { showGoToPageDialog = false },
                                    title = { Text("Đi tới trang", color = currentTextColor, fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column {
                                            Text(
                                                "Nhập số trang từ 1 đến $totalPages:",
                                                color = currentTextSecondaryColor,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            OutlinedTextField(
                                                value = pageInputText,
                                                onValueChange = { input ->
                                                    // Chỉ cho phép nhập số dương
                                                    val filtered = input.filter { char -> char.isDigit() }
                                                    pageInputText = filtered
                                                },
                                                placeholder = { Text("Ví dụ: 45") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                                ),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = currentTextColor,
                                                    unfocusedTextColor = currentTextColor,
                                                    focusedBorderColor = currentIconTint,
                                                    unfocusedBorderColor = currentTextColor.copy(alpha = 0.5f)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                val enteredNum = pageInputText.toIntOrNull()
                                                if (enteredNum != null) {
                                                    // Chuẩn hóa validate: lớn hơn max thì về max, nhỏ hơn hoặc bằng 0 thì về 1
                                                    val pageNumber = when {
                                                        enteredNum > totalPages -> totalPages
                                                        enteredNum <= 0 -> 1
                                                        else -> enteredNum
                                                    }
                                                    scope.launch {
                                                        pagerState.scrollToPage(pageNumber - 1)
                                                    }
                                                }
                                                showGoToPageDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = currentButtonContainerColor,
                                                contentColor = currentButtonContentColor
                                            )
                                        ) {
                                            Text("Đi tới", color = currentButtonContentColor, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showGoToPageDialog = false }) {
                                            Text("Hủy", color = currentIconTint)
                                        }
                                    }
                                )
                            }

                            IconButton(
                                onClick = {
                                    pageInputText = (pagerState.currentPage + 1).toString()
                                    showGoToPageDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FindInPage,
                                    contentDescription = "Go to page",
                                    tint = currentIconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .background(currentSurfaceColor)
                                .fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                progress = if (totalPages > 0) (pagerState.currentPage + 1).toFloat() / totalPages.toFloat() else 0f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = currentIconTint,
                                trackColor = currentIconTint.copy(alpha = 0.2f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trang ${pagerState.currentPage + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = currentTextSecondaryColor
                                )

                                IconButton(
                                    onClick = { showControlMenu = !showControlMenu },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showControlMenu) Icons.Default.KeyboardArrowDown else Icons.Default.Tune,
                                        contentDescription = "Menu",
                                        tint = currentIconTint
                                    )
                                }
                            }

                            if (showControlMenu) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    currentSurfaceColor,
                                                    currentSurfaceColor
                                                )
                                            )
                                        )
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    // Section: Font Size
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Cỡ chữ",
                                            color = currentTextSecondaryColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(24.dp))
                                                .border(1.dp, currentIconTint.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.changeFontSize((settings.fontSizeSp - 1).coerceAtLeast(12)) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = currentIconTint, modifier = Modifier.size(18.dp))
                                            }
                                            Text(
                                                "${settings.fontSizeSp}sp",
                                                color = currentTextColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                            IconButton(
                                                onClick = { viewModel.changeFontSize((settings.fontSizeSp + 1).coerceAtMost(30)) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = currentIconTint, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    // Section: Font Family
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Phông chữ",
                                            color = currentTextSecondaryColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("serif" to "Serif", "sans-serif" to "Sans").forEach { (key, label) ->
                                                val isSelected = settings.fontFamily == key
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(
                                                            if (isSelected) currentIconTint
                                                            else currentIconTint.copy(alpha = 0.08f)
                                                        )
                                                        .clickable { viewModel.changeFontFamily(key) }
                                                        .padding(horizontal = 16.dp, vertical = 7.dp)
                                                ) {
                                                    Text(
                                                        label,
                                                        color = if (isSelected) currentBgColor else currentTextColor,
                                                        fontFamily = if (key == "serif") FontFamily.Serif else FontFamily.SansSerif,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Section: Reading Theme
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Nền đọc",
                                            color = currentTextSecondaryColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Warm theme button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(PtBackgroundWarm)
                                                    .border(
                                                        width = if (settings.readingTheme == "warm") 2.dp else 1.dp,
                                                        color = if (settings.readingTheme == "warm") PtNavyPrimary else PtDividerWarm,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.changeReadingTheme("warm") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (settings.readingTheme == "warm") {
                                                    Text("✓", color = PtNavyPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            // Light theme button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .border(
                                                        width = if (settings.readingTheme == "light") 2.dp else 1.dp,
                                                        color = if (settings.readingTheme == "light") PtNavyPrimary else Color(0xFFDDDDDD),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.changeReadingTheme("light") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (settings.readingTheme == "light") {
                                                    Text("✓", color = PtNavyPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            // Dark theme button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1A1A2E))
                                                    .border(
                                                        width = if (settings.readingTheme == "dark") 2.dp else 1.dp,
                                                        color = if (settings.readingTheme == "dark") Color(0xFF5399D6) else Color(0xFF333344),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.changeReadingTheme("dark") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (settings.readingTheme == "dark") {
                                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = currentBgColor
                ) { subPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(subPadding)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -15) {
                                        showControlMenu = true
                                    } else if (dragAmount > 15) {
                                        showControlMenu = false
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                var totalZoom = 1f
                                detectTransformGestures { _, _, zoom, _ ->
                                    totalZoom *= zoom
                                    val now = System.currentTimeMillis()
                                    if (now - lastScaleTime > 150) {
                                        if (totalZoom > 1.05f) {
                                            viewModel.changeFontSize((settings.fontSizeSp + 1).coerceAtMost(30))
                                            totalZoom = 1f
                                            lastScaleTime = now
                                        } else if (totalZoom < 0.95f) {
                                            viewModel.changeFontSize((settings.fontSizeSp - 1).coerceAtLeast(12))
                                            totalZoom = 1f
                                            lastScaleTime = now
                                        }
                                    }
                                }
                            }
                            .clickable {
                                showControlMenu = !showControlMenu
                            }
                    ) { page ->
                        val paddingModifier = if (isPdfContent) {
                            Modifier.padding(0.dp)
                        } else {
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 48.dp)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(paddingModifier)
                                .clickable { showControlMenu = !showControlMenu }
                        ) {
                            if (isPdfContent) {
                                val paragraph = pdfParagraphs.getOrNull(page) ?: ""
                                if (paragraph.startsWith("[page_image:") && paragraph.endsWith("]")) {
                                    val imagePath = paragraph.substringAfter("[page_image: ").substringBefore("]")
                                    val file = java.io.File(imagePath)
                                    if (file.exists()) {
                                        val bitmap = remember(imagePath) {
                                            try {
                                                val raw = BitmapFactory.decodeFile(imagePath)
                                                if (raw != null) {
                                                    val cropped = cropWhiteBorders(raw)
                                                    cropped.asImageBitmap()
                                                } else {
                                                    null
                                                }
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                        if (bitmap != null) {
                                            var scale by remember { mutableStateOf(1f) }
                                            var offset by remember { mutableStateOf(Offset.Zero) }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(0.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = bitmap,
                                                    contentDescription = "PDF Page",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            translationX = offset.x,
                                                            translationY = offset.y,
                                                            clip = true
                                                        )
                                                        .pointerInput(Unit) {
                                                            awaitEachGesture {
                                                                val down = awaitFirstDown()
                                                                do {
                                                                    val event = awaitPointerEvent()
                                                                    val canceled = event.changes.any { it.isConsumed }
                                                                    if (!canceled) {
                                                                        val zoomChange = event.calculateZoom()
                                                                        val panChange = event.calculatePan()
                                                                        
                                                                        scale = (scale * zoomChange).coerceIn(1f, 4f)
                                                                        if (scale > 1f) {
                                                                            offset = offset + panChange
                                                                            event.changes.forEach { it.consume() }
                                                                        } else {
                                                                            offset = Offset.Zero
                                                                        }
                                                                    }
                                                                } while (!canceled && event.changes.any { it.pressed })
                                                            }
                                                        },
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                SelectionContainer {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        val pageParagraphs = paginatedTextPages.getOrNull(page) ?: emptyList()
                                        pageParagraphs.forEach { pageParagraph ->
                                            val absoluteIndex = pageParagraph.absoluteIndex
                                            val paragraph = pageParagraph.text
                                            val pHighlight = highlights.find { it.startOffset == absoluteIndex }

                                            if (paragraph.startsWith("[image:") && paragraph.endsWith("]")) {
                                                val imageName = paragraph.substringAfter("[image:").substringBefore("]")
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(150.dp)
                                                        .padding(vertical = 12.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(currentIconTint.copy(alpha = 0.08f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val w = size.width
                                                        val h = size.height
                                                        drawRect(
                                                            color = currentIconTint.copy(alpha = 0.15f),
                                                            style = Stroke(width = 1.dp.toPx())
                                                        )
                                                        drawCircle(
                                                            color = currentIconTint.copy(alpha = 0.4f),
                                                            radius = 28.dp.toPx(),
                                                            center = Offset(w / 2, h / 2),
                                                            style = Stroke(width = 2.dp.toPx())
                                                        )
                                                        drawLine(
                                                            color = currentIconTint.copy(alpha = 0.3f),
                                                            start = Offset(30.dp.toPx(), h / 2),
                                                            end = Offset(w - 30.dp.toPx(), h / 2),
                                                            strokeWidth = 2.dp.toPx()
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "📊 HÌNH ẢNH / BIỂU ĐỒ TÀI LIỆU",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = currentTextColor)
                                                        )
                                                        Text(
                                                            text = imageName.uppercase().replace("_", " "),
                                                            style = MaterialTheme.typography.labelSmall.copy(color = currentTextSecondaryColor)
                                                        )
                                                    }
                                                }
                                            } else {
                                                val annotatedText = remember(paragraph, highlights) {
                                                    buildAnnotatedString {
                                                        append(paragraph)
                                                        val paragraphHighlights = highlights.filter { it.startOffset == absoluteIndex }
                                                        for (highlight in paragraphHighlights) {
                                                            val sel = highlight.selectedText
                                                            if (!sel.isNullOrEmpty()) {
                                                                val startIdx = highlight.endOffset
                                                                val endIdx = startIdx + sel.length
                                                                if (startIdx >= 0 && endIdx <= paragraph.length && paragraph.substring(startIdx, endIdx) == sel) {
                                                                    addStyle(
                                                                        style = SpanStyle(background = Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.3f)),
                                                                        start = startIdx,
                                                                        end = endIdx
                                                                    )
                                                                } else {
                                                                    // Fallback to highlighting all matching words if index is out of bounds or text differs
                                                                    var fallbackIdx = paragraph.indexOf(sel)
                                                                    while (fallbackIdx != -1) {
                                                                        addStyle(
                                                                            style = SpanStyle(background = Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.3f)),
                                                                            start = fallbackIdx,
                                                                            end = fallbackIdx + sel.length
                                                                        )
                                                                        fallbackIdx = paragraph.indexOf(sel, fallbackIdx + 1)
                                                                    }
                                                                }
                                                            } else {
                                                                addStyle(
                                                                    style = SpanStyle(background = Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.3f)),
                                                                    start = 0,
                                                                    end = paragraph.length
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                var textFieldValue by remember(annotatedText) {
                                                    mutableStateOf(TextFieldValue(annotatedText))
                                                }

                                                LaunchedEffect(showColorPickerSheet) {
                                                    if (!showColorPickerSheet && textFieldValue.selection.length > 0) {
                                                        textFieldValue = textFieldValue.copy(selection = androidx.compose.ui.text.TextRange.Zero)
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        BasicTextField(
                                                            value = textFieldValue,
                                                            onValueChange = { newValue ->
                                                                textFieldValue = newValue
                                                                val sel = newValue.selection
                                                                if (sel.length > 0) {
                                                                    activeSelectedText = newValue.annotatedString.text.substring(sel.start, sel.end)
                                                                    activeParagraphIndex = absoluteIndex
                                                                    activeSelectedStartChar = minOf(sel.start, sel.end)
                                                                    showColorPickerSheet = true
                                                                }
                                                            },
                                                            readOnly = true,
                                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                                fontSize = settings.fontSizeSp.sp,
                                                                fontFamily = currentFontFamily,
                                                                color = currentTextColor
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }


                            Spacer(modifier = Modifier.weight(1f))

                            // Footer Page Info badge: only show for PDF content (text already has bottom bar)
                            if (isPdfContent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(currentSurfaceColor)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${page + 1} / $totalPages",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = currentTitleColor)
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
}

@Composable
fun BookmarkFlag(
    isBookmarked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(width = 24.dp, height = 36.dp)
            .clickable(onClick = onClick)
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2, size.height - 8.dp.toPx())
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = path,
            color = if (isBookmarked) PtNavyPrimary else PtDividerWarm
        )
    }
}

@Composable
fun IllustrationCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(PtSurfaceWarm)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High fidelity custom Canvas drawing representing Baker Street desk elements
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFEFECE6))
        ) {
            val w = size.width
            val h = size.height

            // Table lines / wood texture
            drawLine(Color(0xFFD3CFC9), Offset(0f, h - 30.dp.toPx()), Offset(w, h - 30.dp.toPx()), strokeWidth = 1.dp.toPx())

            // Magnifying Glass
            drawCircle(
                color = Color(0xFF1F1F24),
                radius = 20.dp.toPx(),
                center = Offset(w / 3 - 10.dp.toPx(), h / 2),
                style = Stroke(width = 3.dp.toPx())
            )
            drawLine(
                color = Color(0xFF1F1F24),
                start = Offset(w / 3, h / 2 + 15.dp.toPx()),
                end = Offset(w / 3 + 25.dp.toPx(), h / 2 + 40.dp.toPx()),
                strokeWidth = 6.dp.toPx()
            )

            // Tablet/E-Reader
            drawRect(
                color = Color(0xFF2C2C35),
                topLeft = Offset(w * 0.55f, h * 0.25f),
                size = Size(70.dp.toPx(), 90.dp.toPx())
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(w * 0.55f + 6.dp.toPx(), h * 0.25f + 6.dp.toPx()),
                size = Size(70.dp.toPx() - 12.dp.toPx(), 90.dp.toPx() - 12.dp.toPx())
            )
            // Text lines on e-reader
            for (i in 0..6) {
                drawLine(
                    color = Color.LightGray,
                    start = Offset(w * 0.55f + 10.dp.toPx(), h * 0.25f + 12.dp.toPx() + (i * 8.dp.toPx())),
                    end = Offset(w * 0.55f + 50.dp.toPx(), h * 0.25f + 12.dp.toPx() + (i * 8.dp.toPx())),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Spectacles / Glasses
            drawCircle(Color(0xFF333333), 10.dp.toPx(), Offset(w * 0.42f, h * 0.4f), style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(Color(0xFF333333), 10.dp.toPx(), Offset(w * 0.42f + 25.dp.toPx(), h * 0.4f), style = Stroke(width = 1.5.dp.toPx()))
            drawLine(Color(0xFF333333), Offset(w * 0.42f + 10.dp.toPx(), h * 0.4f), Offset(w * 0.42f + 15.dp.toPx(), h * 0.4f), strokeWidth = 1.5.dp.toPx())
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Caption
        Text(
            text = "Fig 3.1 — The instruments of observation at 221B Baker Street.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

fun cropWhiteBorders(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    
    var minX = width
    var minY = height
    var maxX = 0
    var maxY = 0
    
    val pixels = IntArray(width)
    for (y in 0 until height) {
        bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
        for (x in 0 until width) {
            val color = pixels[x]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            if (r < 240 || g < 240 || b < 240) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    
    if (maxX <= minX || maxY <= minY) {
        return bitmap
    }
    
    val padding = 8
    val cropX = maxOf(0, minX - padding)
    val cropY = maxOf(0, minY - padding)
    val cropWidth = minOf(width - cropX, (maxX - minX) + padding * 2)
    val cropHeight = minOf(height - cropY, (maxY - minY) + padding * 2)
    
    return try {
        android.graphics.Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    } catch (e: Exception) {
        bitmap
    }
}

data class PageParagraph(
    val absoluteIndex: Int,
    val text: String
)

fun paginateText(content: String, fontSizeSp: Int): List<List<PageParagraph>> {
    val pages = mutableListOf<List<PageParagraph>>()
    // Non-linear budget scaling with font size to guarantee no overflow at large sizes
    val budget = ((10 * 1000) / (fontSizeSp - 3)).coerceIn(250, 900)
    
    val rawParagraphs = content.split("\n\n")
    val paragraphs = if (rawParagraphs.firstOrNull()?.trim()?.toIntOrNull() != null) {
        rawParagraphs.drop(1)
    } else {
        rawParagraphs
    }
    
    var currentPage = mutableListOf<PageParagraph>()
    var currentPageLength = 0
    
    paragraphs.forEachIndexed { index, para ->
        val trimmedPara = para.trim()
        if (trimmedPara.isEmpty()) return@forEachIndexed
        
        // If the paragraph fits fully on the current page, add it
        if (currentPageLength + trimmedPara.length <= budget) {
            currentPage.add(PageParagraph(index, trimmedPara))
            currentPageLength += trimmedPara.length
        } else {
            // Otherwise, split the paragraph into sentences and distribute them
            val sentences = trimmedPara.split(Regex("(?<=\\.)\\s+"))
            var sentenceChunk = java.lang.StringBuilder()
            
            for (sentence in sentences) {
                val sentenceTrimmed = sentence.trim()
                if (sentenceTrimmed.isEmpty()) continue
                
                // Check if adding this sentence to the current chunk exceeds the budget
                if (currentPageLength + sentenceChunk.length + sentenceTrimmed.length > budget) {
                    // Push the current chunk if it exists
                    if (sentenceChunk.isNotEmpty()) {
                        currentPage.add(PageParagraph(index, sentenceChunk.toString().trim()))
                    }
                    // Push page
                    if (currentPage.isNotEmpty()) {
                        pages.add(currentPage)
                    }
                    currentPage = mutableListOf()
                    currentPageLength = 0
                    sentenceChunk = java.lang.StringBuilder()
                }
                
                if (sentenceChunk.isNotEmpty()) {
                    sentenceChunk.append(" ")
                }
                sentenceChunk.append(sentenceTrimmed)
            }
            
            if (sentenceChunk.isNotEmpty()) {
                currentPage.add(PageParagraph(index, sentenceChunk.toString().trim()))
                currentPageLength += sentenceChunk.length
            }
        }
    }
    
    if (currentPage.isNotEmpty()) {
        pages.add(currentPage)
    }
    
    return pages
}
