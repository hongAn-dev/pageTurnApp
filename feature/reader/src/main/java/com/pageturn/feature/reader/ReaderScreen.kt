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
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
    val scrollState = rememberScrollState()

    var showAppearanceDialog by remember { mutableStateOf(false) }
    
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

    // Appearance settings dialog definition
    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Cài đặt giao diện đọc", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Font Size control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cỡ chữ", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.changeFontSize((settings.fontSizeSp - 2).coerceAtLeast(12)) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor)
                            ) {
                                Text("-", color = currentButtonContentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Text("${settings.fontSizeSp} sp", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = currentTextColor))
                            Button(
                                onClick = { viewModel.changeFontSize((settings.fontSizeSp + 2).coerceAtMost(30)) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentButtonContainerColor, contentColor = currentButtonContentColor)
                            ) {
                                Text("+", color = currentButtonContentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }

                    // Font Family control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phông chữ", style = MaterialTheme.typography.bodyMedium, color = currentTextColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.fontFamily == "serif",
                                onClick = { viewModel.changeFontFamily("serif") },
                                label = { Text("Serif") }
                            )
                            FilterChip(
                                selected = settings.fontFamily == "sans-serif",
                                onClick = { viewModel.changeFontFamily("sans-serif") },
                                label = { Text("Sans-Serif") }
                            )
                        }
                    }

                    // Theme control
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chủ đề nền", style = MaterialTheme.typography.bodyMedium, color = currentTextColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.changeReadingTheme("warm") },
                                colors = ButtonDefaults.buttonColors(containerColor = PtBackgroundWarm, contentColor = PtTextWarm),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ấm", color = PtTextWarm, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.changeReadingTheme("light") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sáng", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.changeReadingTheme("dark") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tối", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAppearanceDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = currentIconTint)
                ) {
                    Text("Đóng", color = currentIconTint, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

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
            title = { Text("Chia sẻ Quote Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom drawn Quote Card Mock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PtSurfaceWarm),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw luxury card borders / design frames
                            drawRect(
                                color = PtNavyPrimary,
                                topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                                size = Size(w - 24.dp.toPx(), h - 24.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            // Draw decorative corner markings
                            drawCircle(PtNavyPrimary, 4.dp.toPx(), Offset(16.dp.toPx(), 16.dp.toPx()))
                            drawCircle(PtNavyPrimary, 4.dp.toPx(), Offset(w - 16.dp.toPx(), 16.dp.toPx()))
                            drawCircle(PtNavyPrimary, 4.dp.toPx(), Offset(16.dp.toPx(), h - 16.dp.toPx()))
                            drawCircle(PtNavyPrimary, 4.dp.toPx(), Offset(w - 16.dp.toPx(), h - 16.dp.toPx()))
                        }

                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "“",
                                fontSize = 48.sp,
                                fontFamily = FontFamily.Serif,
                                color = PtNavyPrimary
                            )
                            Text(
                                text = selectedParagraphText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 4,
                                color = PtTextMain
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "— ${(uiState as? ReaderUiState.Success)?.bookTitle ?: "PageTurn"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = PtNavyPrimary
                                )
                            )
                        }
                    }

                    Text(
                        text = "Thiết kế thẻ trích dẫn Serif cổ điển.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
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
                    Text("Chia sẻ qua ứng dụng", color = currentButtonContentColor, fontWeight = FontWeight.Bold)
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
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "PageTurn",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 24.sp,
                                color = currentTitleColor
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = currentIconTint)
                    }
                },
                actions = {
                    // Font adjustment button (Tt) styled beautifully
                    IconButton(onClick = { showAppearanceDialog = true }) {
                        Text("Tt", style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = currentIconTint))
                    }
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentIconTint.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = currentIconTint)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = currentSurfaceColor)
            )
        },
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

                // Dynamic Pagination: PDF = 1 image per page, text = 5 paragraphs per page
                val paragraphs = state.chapter.content.split("\n\n")
                val isPdfContent = paragraphs.any { it.startsWith("[page_image:") && it.endsWith("]") }
                val paragraphsPerPage = if (isPdfContent) 1 else 5
                val pdfParagraphs = if (isPdfContent)
                    paragraphs.filter { it.startsWith("[page_image:") && it.endsWith("]") }
                else paragraphs
                val totalPages = if (isPdfContent)
                    pdfParagraphs.size
                else
                    (paragraphs.size + paragraphsPerPage - 1) / paragraphsPerPage

                // State to collapse/expand the bottom control bar
                var bottomBarExpanded by remember { mutableStateOf(true) }

                val initialParagraph = viewModel.initialParagraph
                val targetPage = if (isPdfContent) initialParagraph else initialParagraph / paragraphsPerPage
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

                Scaffold(
                    modifier = Modifier.padding(innerPadding),
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .background(currentSurfaceColor)
                                .fillMaxWidth()
                        ) {
                            // Always-visible: page info row + toggle button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp, start = 16.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trang ${pagerState.currentPage + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = currentTextSecondaryColor
                                )
                                // Collapse / Expand toggle arrow
                                IconButton(
                                    onClick = { bottomBarExpanded = !bottomBarExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (bottomBarExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = if (bottomBarExpanded) "Ẩn thanh điều khiển" else "Hiện thanh điều khiển",
                                        tint = currentIconTint
                                    )
                                }
                            }
                            // Progress indicator bar
                            LinearProgressIndicator(
                                progress = (pagerState.currentPage + 1).toFloat() / totalPages.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = currentIconTint,
                                trackColor = currentIconTint.copy(alpha = 0.2f)
                            )

                            // Collapsible control section
                            if (bottomBarExpanded) {
                                // Bottom control actions with proper library icons
                                val currentPageParagraphs = remember(pagerState.currentPage, if (isPdfContent) pdfParagraphs else paragraphs) {
                                    val sourceList = if (isPdfContent) pdfParagraphs else paragraphs
                                    val startIndex = pagerState.currentPage * paragraphsPerPage
                                    val endIndex = minOf(startIndex + paragraphsPerPage, sourceList.size)
                                    sourceList.subList(startIndex, endIndex)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous Page Arrow
                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage > 0) {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Previous page",
                                            tint = if (pagerState.currentPage > 0) currentIconTint else currentIconTint.copy(alpha = 0.3f)
                                        )
                                    }

                                    // Appearance (Settings icon)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { showAppearanceDialog = true }
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Appearance", tint = currentIconTint)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("GIAO DIỆN", style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, color = currentIconTint))
                                    }

                                    // TTS Audio Playback button
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            if (isTtsPlaying) {
                                                viewModel.stopSpeaking()
                                            } else {
                                                viewModel.speak(currentPageParagraphs.joinToString("\n\n"))
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isTtsPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                            contentDescription = "TTS",
                                            tint = currentIconTint
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("ĐỌC SÁCH", style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, color = currentIconTint))
                                    }

                                    // Share (Share icon)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            selectedParagraphText = currentPageParagraphs.firstOrNull() ?: ""
                                            selectedParagraphIndex = pagerState.currentPage * paragraphsPerPage
                                            showQuoteCardDialog = true
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = currentIconTint)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("CHIA SẺ", style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp, color = currentIconTint))
                                    }

                                    // Next Page Arrow
                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage < totalPages - 1) {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage < totalPages - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Next page",
                                            tint = if (pagerState.currentPage < totalPages - 1) currentIconTint else currentIconTint.copy(alpha = 0.3f)
                                        )
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
                    ) { page ->
                        val sourceList = if (isPdfContent) pdfParagraphs else paragraphs
                        val startIndex = page * paragraphsPerPage
                        val endIndex = minOf(startIndex + paragraphsPerPage, sourceList.size)
                        val visibleParagraphs = sourceList.subList(startIndex, endIndex)

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            if (page == 0) {
                                // Book Header Details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.bookTitle,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = currentTextSecondaryColor
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = state.chapter.title,
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, color = currentTitleColor, fontFamily = currentFontFamily)
                                        )
                                    }

                                    // Bookmark Flag
                                    BookmarkFlag(
                                        isBookmarked = isBookmarked,
                                        onClick = { viewModel.toggleBookmark(chapterNumber = state.chapter.chapterNumber, pageNumber = 42) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            SelectionContainer {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    visibleParagraphs.forEachIndexed { relativeIndex, paragraph ->
                                        val absoluteIndex = startIndex + relativeIndex
                                        val pHighlight = highlights.find { it.startOffset == absoluteIndex }
                                        val noteText = pHighlight?.noteText
                                        val hasNote = !noteText.isNullOrEmpty()

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
                                                            .fillMaxWidth()
                                                            .wrapContentHeight()
                                                            .clip(RoundedCornerShape(0.dp)) // Clip to prevent overflow into adjacent pages
                                                            .padding(vertical = 4.dp)
                                                    ) {
                                                        Image(
                                                            bitmap = bitmap,
                                                            contentDescription = "PDF Page",
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .wrapContentHeight()
                                                                .graphicsLayer(
                                                                    scaleX = scale,
                                                                    scaleY = scale,
                                                                    translationX = offset.x,
                                                                    translationY = offset.y,
                                                                    clip = true
                                                                )
                                                                .pointerInput(Unit) {
                                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                                                        offset = if (scale == 1f) Offset.Zero else offset + pan
                                                                    }
                                                                },
                                                            contentScale = ContentScale.FillWidth
                                                        )
                                                    }
                                                }
                                            }
                                        } else if (paragraph.startsWith("[image:") && paragraph.endsWith("]")) {
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
                                        
                                        // Show Illustration after 2nd paragraph of first page
                                        if (absoluteIndex == 1) {
                                            IllustrationCard()
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
