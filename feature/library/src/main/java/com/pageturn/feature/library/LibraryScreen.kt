package com.pageturn.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.pageturn.core.designsystem.theme.*
import com.pageturn.core.model.Book
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.alpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Favorites and Profile states
    var currentFilter by remember { mutableStateOf("all") }
    var showProfile by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }

    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var showRenameCollectionDialog by remember { mutableStateOf(false) }
    var renameCollectionName by remember { mutableStateOf("") }
    var showAddBooksToCollectionDialog by remember { mutableStateOf(false) }
    var showAddToCollectionDialog by remember { mutableStateOf(false) }
    var selectedBookForAddToCollection by remember { mutableStateOf<Book?>(null) }

    // File Picker states and Launcher
    var showImportDialog by remember { mutableStateOf(false) }
    var importedBookTitle by remember { mutableStateOf("") }
    var importedBookAuthor by remember { mutableStateOf("") }
    var importedBookDesc by remember { mutableStateOf("") }
    var importedBookContent by remember { mutableStateOf("") }
    var importedBookCoverPath by remember { mutableStateOf("") }
 
    var isImporting by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isImporting = true
            scope.launch {
                val fileName = getFileName(context, it)
                importedBookTitle = fileName.substringBeforeLast(".")
                importedBookCoverPath = ""
                
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || context.contentResolver.getType(it) == "application/pdf"
                val isEpub = fileName.endsWith(".epub", ignoreCase = true) || context.contentResolver.getType(it) == "application/epub+zip"
                val isAzw3 = fileName.endsWith(".azw3", ignoreCase = true) || fileName.endsWith(".mobi", ignoreCase = true)
                
                if (isPdf) {
                    val bookId = "local_${System.currentTimeMillis()}"
                    val imagePaths = withContext(Dispatchers.IO) {
                        renderPdfToImages(context, it, bookId)
                    }
                    if (imagePaths.isNotEmpty()) {
                        importedBookContent = imagePaths.joinToString("\n\n") { "[page_image: $it]" }
                        importedBookDesc = "Tài liệu PDF gốc gồm ${imagePaths.size} trang."
                        importedBookAuthor = "Tài liệu PDF"
                        importedBookCoverPath = imagePaths.first()
                    } else {
                        importedBookContent = "Không thể phân tích file PDF."
                        importedBookDesc = "Tài liệu bị lỗi."
                        importedBookAuthor = "Tài liệu PDF"
                    }
                } else if (isEpub) {
                    val meta = withContext(Dispatchers.IO) {
                        parseEpubMetadata(context, it)
                    }
                    val epubText = withContext(Dispatchers.IO) {
                        parseEpubText(context, it)
                    }
                    importedBookContent = if (epubText.isNotBlank()) epubText else "Ebook EPUB trống."
                    importedBookDesc = "Tài liệu Ebook dạng EPUB nhập từ thiết bị."
                    importedBookTitle = if (meta.first.isNotBlank()) meta.first else fileName.substringBeforeLast(".")
                    importedBookAuthor = if (meta.second.isNotBlank()) meta.second else "Tác giả EPUB"
                    importedBookCoverPath = meta.third
                } else if (isAzw3) {
                    val azw3Text = withContext(Dispatchers.IO) {
                        parseAzw3Text(context, it)
                    }
                    importedBookContent = if (azw3Text.isNotBlank()) azw3Text else "Ebook AZW3/MOBI trống."
                    importedBookDesc = "Tài liệu Ebook dạng AZW3/MOBI nhập từ thiết bị."
                    importedBookAuthor = "Tác giả Kindle"
                } else {
                    val rawText = withContext(Dispatchers.IO) {
                        readTextFromUri(context, it)
                    }
                    importedBookContent = if (rawText.isNotBlank()) rawText else "Tài liệu trống."
                    importedBookDesc = "Văn bản nhập từ thiết bị."
                    importedBookAuthor = "Tác giả Local"
                }
                isImporting = false
                showImportDialog = true
            }
        }
    }
 
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Nhập sách mới vào máy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = importedBookTitle,
                        onValueChange = { importedBookTitle = it },
                        label = { Text("Tên sách") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = importedBookAuthor,
                        onValueChange = { importedBookAuthor = it },
                        label = { Text("Tác giả") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = importedBookDesc,
                        onValueChange = { importedBookDesc = it },
                        label = { Text("Mô tả chi tiết") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addLocalBook(importedBookTitle, importedBookAuthor, importedBookContent, importedBookCoverPath)
                        showImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                ) {
                    Text("Lưu sách", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val discoverUiState by viewModel.discoverUiState.collectAsState()

    if (isImporting) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Đang nhập sách...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Vui lòng đợi trong giây lát...")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // Premium glassmorphism TopAppBar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .shadow(elevation = 0.dp)
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PageTurn",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Serif
                                )
                            )
                        }
                    },
                    navigationIcon = {},
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                // Bottom separator line
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabsList = listOf(
                    Triple(0, "Trang chủ", Icons.Default.Home),
                    Triple(1, "Khám phá", Icons.Default.Explore),
                    Triple(2, "Kho sách", Icons.Default.LibraryBooks),
                    Triple(3, "Ghi chú", Icons.Default.Edit),
                    Triple(4, "Cài đặt", Icons.Default.Settings)
                )
                tabsList.forEach { (index, title, icon) ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = icon, contentDescription = title) },
                        label = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            showProfile = false
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 2 && selectedCollection == null) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add book")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
            Box(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                    when (tab) {
                        0 -> {
                            // TAB 0: HOME SCREEN (Split Layout)
                            HomeScreenContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                allBooksList = (discoverUiState as? LibraryViewModel.DiscoverUiState.Success)?.let { (it.popular + it.recommended).distinctBy { b -> b.id } } ?: emptyList(),
                                onBookClick = onBookClick,
                                onNavigateToMyLibrary = { selectedTab = 2 },
                                onNavigateToDiscover = { selectedTab = 1 }
                            )
                        }
                        1 -> {
                            // TAB 1: DISCOVER SCREEN
                            DiscoverScreenContent(viewModel = viewModel, onBookClick = onBookClick)
                        }
                        2 -> {
                            // TAB 2: MY LIBRARY SCREEN (Kho sách cá nhân)
                            MyLibraryScreenContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                currentFilter = currentFilter,
                                onFilterChange = { currentFilter = it },
                                selectedCollection = selectedCollection,
                                onSelectCollection = { selectedCollection = it },
                                showCreateCollectionDialog = showCreateCollectionDialog,
                                onShowCreateCollectionDialogChange = { showCreateCollectionDialog = it },
                                showRenameCollectionDialog = showRenameCollectionDialog,
                                onShowRenameCollectionDialogChange = { showRenameCollectionDialog = it },
                                showAddBooksToCollectionDialog = showAddBooksToCollectionDialog,
                                onShowAddBooksToCollectionDialogChange = { showAddBooksToCollectionDialog = it },
                                showAddToCollectionDialog = showAddToCollectionDialog,
                                onShowAddToCollectionDialogChange = { showAddToCollectionDialog = it },
                                selectedBookForAddToCollection = selectedBookForAddToCollection,
                                onSelectedBookForAddToCollectionChange = { selectedBookForAddToCollection = it },
                                renameCollectionName = renameCollectionName,
                                onRenameCollectionNameChange = { renameCollectionName = it },
                                onBookClick = onBookClick
                            )
                        }
                        3 -> {
                            // TAB 3: HIGHLIGHTS SCREEN
                            HighlightsScreenContent(viewModel, onBookClick)
                        }
                        4 -> {
                            // TAB 4: SETTINGS SCREEN
                            SettingsScreenContent(viewModel = viewModel, onSignOut = onSignOut)
                        }
                    }
                }
            }
        }

        // --- Collection Dialogs ---
        if (showCreateCollectionDialog) {
            AlertDialog(
                onDismissRequest = { showCreateCollectionDialog = false },
                title = { Text("Tạo Bộ sưu tập mới", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("Tên bộ sưu tập") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCollectionName.isNotBlank()) {
                                viewModel.createCollection(newCollectionName.trim())
                                newCollectionName = ""
                                showCreateCollectionDialog = false
                            }
                        }
                    ) {
                        Text("Tạo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateCollectionDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }

        if (showRenameCollectionDialog) {
            AlertDialog(
                onDismissRequest = { showRenameCollectionDialog = false },
                title = { Text("Đổi tên Bộ sưu tập", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = renameCollectionName,
                        onValueChange = { renameCollectionName = it },
                        label = { Text("Tên mới") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (renameCollectionName.isNotBlank() && selectedCollection != null) {
                                viewModel.renameCollection(selectedCollection!!, renameCollectionName.trim())
                                selectedCollection = renameCollectionName.trim()
                                renameCollectionName = ""
                                showRenameCollectionDialog = false
                            }
                        }
                    ) {
                        Text("Lưu")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameCollectionDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }

        if (showAddToCollectionDialog && selectedBookForAddToCollection != null) {
            val collections by viewModel.userCollections.collectAsState()
            AlertDialog(
                onDismissRequest = { showAddToCollectionDialog = false },
                title = { Text("Thêm vào Bộ sưu tập", fontWeight = FontWeight.Bold) },
                text = {
                    if (collections.isEmpty()) {
                        Text("Chưa có bộ sưu tập nào. Hãy tạo bộ sưu tập trước.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            items(collections.keys.toList()) { colName ->
                                val hasBook = collections[colName]?.contains(selectedBookForAddToCollection!!.id) == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.toggleBookInCollection(colName, selectedBookForAddToCollection!!.id)
                                            showAddToCollectionDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(colName, style = MaterialTheme.typography.bodyLarge)
                                    if (hasBook) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Đã thêm", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddToCollectionDialog = false }) {
                        Text("Đóng")
                    }
                }
            )
        }

        if (showAddBooksToCollectionDialog && selectedCollection != null && uiState is LibraryUiState.Success) {
            val collections by viewModel.userCollections.collectAsState()
            val books = (uiState as LibraryUiState.Success).books
            val currentBookIds = collections[selectedCollection!!] ?: emptySet()
            AlertDialog(
                onDismissRequest = { showAddBooksToCollectionDialog = false },
                title = { Text("Quản lý sách trong bộ sưu tập", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        items(books) { book ->
                            val isChecked = currentBookIds.contains(book.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.toggleBookInCollection(selectedCollection!!, book.id)
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(book.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        viewModel.toggleBookInCollection(selectedCollection!!, book.id)
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showAddBooksToCollectionDialog = false }) {
                        Text("Hoàn thành")
                    }
                }
            )
        }
    }

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.startsWith("http")) {
            withContext(Dispatchers.IO) {
                try {
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val bmp = BitmapFactory.decodeStream(input)
                    bitmap = bmp
                } catch (e: Exception) {
                    e.printStackTrace()
                    failed = true
                }
            }
        } else {
            failed = true
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (!failed) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
        }
    } else {
        fallback()
    }
}

@Composable
fun BookCoverCanvas(modifier: Modifier = Modifier, title: String = "") {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val coverBgColor = Color(0xFF0C2133) // Elegant deep navy background
            val borderGold = PtGoldenAccent

            // Draw Cover Background
            drawRect(color = coverBgColor)

            // Draw Gold Frame Border
            val inset = 10.dp.toPx()
            drawRect(
                color = borderGold.copy(alpha = 0.8f),
                topLeft = Offset(inset, inset),
                size = Size(width - inset * 2, height - inset * 2),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw inner corner ornaments
            val ornamentSize = 8.dp.toPx()
            drawLine(borderGold.copy(alpha = 0.8f), Offset(inset, inset + ornamentSize), Offset(inset + ornamentSize, inset), strokeWidth = 1.dp.toPx())
            drawLine(borderGold.copy(alpha = 0.8f), Offset(width - inset, inset + ornamentSize), Offset(width - inset - ornamentSize, inset), strokeWidth = 1.dp.toPx())
            drawLine(borderGold.copy(alpha = 0.8f), Offset(inset, height - inset - ornamentSize), Offset(inset + ornamentSize, height - inset), strokeWidth = 1.dp.toPx())
            drawLine(borderGold.copy(alpha = 0.8f), Offset(width - inset, height - inset - ornamentSize), Offset(width - inset - ornamentSize, height - inset), strokeWidth = 1.dp.toPx())

            // Gold vintage diamond emblem in the center
            val centerX = width / 2
            val centerY = height / 2
            val badgeSize = 14.dp.toPx()
            val path = Path().apply {
                moveTo(centerX, centerY - badgeSize)
                lineTo(centerX + badgeSize, centerY)
                lineTo(centerX, centerY + badgeSize)
                lineTo(centerX - badgeSize, centerY)
                close()
            }
            drawPath(path, color = borderGold, style = Stroke(width = 1.dp.toPx()))
            drawCircle(color = borderGold, radius = 3.dp.toPx(), center = Offset(centerX, centerY))
        }

        // 3D spine shadow at left edge
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )

        // Title overlay at bottom of cover
        if (title.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF061525).copy(alpha = 0.95f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = PtGoldenAccent,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BookCover(title: String, author: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(0.65f)
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        BookCoverCanvas()

        // 3D book spine shadow effect
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(10.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Overlay text title & author for high-fidelity appearance
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(PtGoldenAccent.copy(alpha = 0.6f))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = PtGoldenAccent,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 4,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 8.sp,
                        color = PtGoldenAccent.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(1.dp)
                    .background(PtGoldenAccent.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
fun BookItem(
    book: Book, 
    onBookClick: (String) -> Unit,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onDeleteCloudClick: () -> Unit = {},
    onAddToCollection: (() -> Unit)? = null,
    onRemoveFromCollection: (() -> Unit)? = null,
    onDeleteBookClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val coverFile = remember(book.coverUrl) {
        if (book.coverUrl.isNotEmpty() && !book.coverUrl.startsWith("http")) {
            val f = java.io.File(book.coverUrl)
            if (f.exists()) f else null
        } else null
    }
    val bitmap = remember(coverFile) {
        coverFile?.let {
            try {
                BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book.id) }
    ) {
        // Custom Premium Cover using Canvas or Bitmap
        Box(
            modifier = Modifier
                .aspectRatio(0.65f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp),
                    clip = false,
                    ambientColor = PtNavyPrimary.copy(alpha = 0.1f),
                    spotColor = PtNavyPrimary.copy(alpha = 0.2f)
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (book.coverUrl.startsWith("http")) {
                NetworkImage(
                    url = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    fallback = { BookCover(title = book.title, author = book.author) }
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Left spine binding overlay on image cover
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(8.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.02f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            } else {
                BookCover(title = book.title, author = book.author)
            }

            // Progress percentage badge at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(PtGoldenAccent, Color(0xFFB38927))),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${(book.progressPercent * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (book.id.startsWith("local_")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Local",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title & Author row with 3-dots options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Serif
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Options 3-dots
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (isFavorite) "Xóa khỏi Yêu thích" else "Thêm vào Yêu thích")
                        },
                        onClick = {
                            onFavoriteToggle()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Thêm vào Bộ sưu tập")
                        },
                        onClick = {
                            showMenu = false
                            onAddToCollection?.invoke()
                        }
                    )
                    if (onRemoveFromCollection != null) {
                        DropdownMenuItem(
                            text = {
                                Text("Bỏ khỏi Bộ sưu tập", color = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showMenu = false
                                onRemoveFromCollection.invoke()
                            }
                        )
                    }
                    if (!book.id.startsWith("local_")) {
                        DropdownMenuItem(
                            text = {
                                Text("Tải sách về máy")
                            },
                            onClick = {
                                showMenu = false
                                onDownloadClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text("Xóa sách khỏi server", color = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showMenu = false
                                onDeleteCloudClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text("Xóa sách khỏi máy", color = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            showMenu = false
                            onDeleteBookClick?.invoke()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = book.progressPercent,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenContent(viewModel: LibraryViewModel, onBookClick: (String) -> Unit) {
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var storeSearchQuery by remember { mutableStateOf("") }
    val discoverState by viewModel.discoverUiState.collectAsState()
    val downloadingIds by viewModel.downloadingBookIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(selectedCategory, storeSearchQuery) {
        viewModel.loadStoreBooks(
            category = if (selectedCategory == "Tất cả") null else selectedCategory,
            query = storeSearchQuery.ifBlank { null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // --- PUBLIC LIBRARY TITLE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thư viện Cộng đồng",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // --- SEARCH BAR SYSTEM ---
        item {
            OutlinedTextField(
                value = storeSearchQuery,
                onValueChange = { storeSearchQuery = it },
                placeholder = { Text("Tìm kiếm sách trên hệ thống...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }

        // --- CATEGORY CHIPS ROW ---
        item {
            val categories = listOf("Tất cả", "Trinh thám", "Cổ điển", "Lãng mạn", "Kinh tế", "Lịch sử")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        // --- STORE BOOK LIST OR STATUS STATES ---
        when (val state = discoverState) {
            is LibraryViewModel.DiscoverUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is LibraryViewModel.DiscoverUiState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
            }
            is LibraryViewModel.DiscoverUiState.Success -> {
                val allBooksList = (state.popular + state.recommended).distinctBy { it.bookHash }
                if (allBooksList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Không tìm thấy sách nào trên hệ thống.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(allBooksList) { publicBook ->
                        val libraryBooks = (uiState as? LibraryUiState.Success)?.books ?: emptyList()
                        val isAlreadyDownloaded = remember(libraryBooks, publicBook.id, publicBook.bookHash) {
                            val idStr = publicBook.id.toString()
                            libraryBooks.any { it.id == idStr || (it.id == publicBook.bookHash && publicBook.bookHash.isNotEmpty()) }
                        }
                        val isDownloading = downloadingIds.contains(publicBook.id.toString())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    if (isAlreadyDownloaded) {
                                        val match = libraryBooks.find { it.id == publicBook.id.toString() || (it.id == publicBook.bookHash && publicBook.bookHash.isNotEmpty()) }
                                        match?.let { onBookClick(it.id) }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    val coverUrl = publicBook.coverUrl
                                    if (!coverUrl.isNullOrEmpty()) {
                                        NetworkImage(
                                            url = coverUrl,
                                            contentDescription = publicBook.title,
                                            modifier = Modifier.fillMaxSize(),
                                            fallback = { BookCoverCanvas(title = publicBook.title) }
                                        )
                                    } else {
                                        BookCoverCanvas(title = publicBook.title)
                                    }
                                }

                                // Info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = (publicBook.category ?: "SÁCH").uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = publicBook.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = publicBook.author.ifBlank { "Tác giả ẩn danh" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFE5A93B),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "4.5",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // Download Action Button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDownloading) Color.Transparent
                                            else if (isAlreadyDownloaded) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isDownloading) Color.Transparent 
                                                    else if (isAlreadyDownloaded) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable(enabled = !isDownloading) {
                                            if (isAlreadyDownloaded) {
                                                val match = libraryBooks.find { it.id == publicBook.id.toString() || (it.id == publicBook.bookHash && publicBook.bookHash.isNotEmpty()) }
                                                match?.let { onBookClick(it.id) }
                                            } else {
                                                viewModel.downloadBook(
                                                    bookId = publicBook.id.toString(),
                                                    title = publicBook.title,
                                                    author = publicBook.author,
                                                    coverUrl = publicBook.coverUrl ?: "",
                                                    description = publicBook.description ?: "Đã tải từ server",
                                                    storeBookId = publicBook.id.toString()
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else if (isAlreadyDownloaded) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = "Đọc sách",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Tải về",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ArchiveScreenContent(
    uiState: LibraryUiState,
    onBookClick: (String) -> Unit,
    onDeleteBook: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sách tự nhập (Local)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        )

        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is LibraryUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
            is LibraryUiState.Success -> {
                val completedBooks = state.books.filter { it.id.startsWith("local_") }

                if (completedBooks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Chưa có sách tự nhập", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedBooks) { book ->
                            val coverFile = remember(book.coverUrl) {
                                if (book.coverUrl.isNotEmpty() && !book.coverUrl.startsWith("http")) {
                                    val f = java.io.File(book.coverUrl)
                                    if (f.exists()) f else null
                                } else null
                            }
                            val bitmap = remember(coverFile) {
                                coverFile?.let {
                                    try {
                                        BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookClick(book.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(60.dp, 90.dp).clip(RoundedCornerShape(4.dp))) {
                                        if (book.coverUrl.startsWith("http")) {
                                            NetworkImage(
                                                url = book.coverUrl,
                                                contentDescription = book.title,
                                                modifier = Modifier.fillMaxSize(),
                                                fallback = { BookCoverCanvas(title = book.title) }
                                            )
                                        } else if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = book.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            BookCoverCanvas(title = book.title)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(text = book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("TÀI LIỆU CÁ NHÂN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteBook(book.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete book",
                                            tint = MaterialTheme.colorScheme.error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(viewModel: LibraryViewModel, onSignOut: () -> Unit) {
    val settings by viewModel.userSettings.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val highlightsList by viewModel.allHighlights.collectAsState()
    val bookCount = (uiState as? LibraryUiState.Success)?.books?.size ?: 0
    val totalPagesRead = (uiState as? LibraryUiState.Success)?.books?.sumOf { it.currentPage } ?: 0
    val totalPagesReadFormatted = String.format(java.util.Locale.US, "%,d", totalPagesRead).replace(',', '.')
    val notesCount = highlightsList.size
    val context = LocalContext.current

    var nameInput by remember(userProfile) { mutableStateOf(userProfile.name) }
    var emailInput by remember(userProfile) { mutableStateOf(userProfile.email) }
    var bioInput by remember(userProfile) { mutableStateOf(userProfile.bio) }
    var isEditing by remember { mutableStateOf(false) }

    // Dialog chỉnh sửa thông tin nhanh
    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Họ và Tên") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Giới thiệu") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(nameInput, emailInput, bioInput)
                        isEditing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Lưu", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    // Modal con xử lý từng mục cấu hình (Dialogs)
    var activeDialog by remember { mutableStateOf<String?>(null) } // "notifications", "sync", "preferences"

    if (activeDialog == "notifications") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Thông báo nhắc nhở", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bật nhắc nhở đọc sách", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Nhắc nhở đọc sách định kỳ mỗi ngày",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dailyNotify,
                            onCheckedChange = { viewModel.setDailyNotify(it) }
                        )
                    }

                    if (settings.dailyNotify) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        // Hằng ngày / Theo chu kỳ
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setReminderMode("daily") }
                            ) {
                                RadioButton(selected = settings.reminderMode == "daily", onClick = { viewModel.setReminderMode("daily") })
                                Text("Hằng ngày", color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setReminderMode("interval") }
                            ) {
                                RadioButton(selected = settings.reminderMode == "interval", onClick = { viewModel.setReminderMode("interval") })
                                Text("Theo chu kỳ", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (settings.reminderMode == "daily") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Giờ nhắc:", color = MaterialTheme.colorScheme.onSurface)
                                val timePicker = remember(settings.reminderHour, settings.reminderMinute) {
                                    android.app.TimePickerDialog(context, { _, h, m -> viewModel.setReminderTime(h, m) }, settings.reminderHour, settings.reminderMinute, true)
                                }
                                OutlinedButton(onClick = { timePicker.show() }) {
                                    Text(String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute))
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Mỗi:", color = MaterialTheme.colorScheme.onSurface)
                                OutlinedTextField(
                                    value = if (settings.reminderIntervalVal == 0) "" else settings.reminderIntervalVal.toString(),
                                    onValueChange = {
                                        val num = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                        viewModel.setReminderIntervalVal(num)
                                    },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                                Text("phút", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }

    if (activeDialog == "sync") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Đồng bộ Cloud & Cache", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tự động đồng bộ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tải tiến độ lên đám mây tự động", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.autoSync, onCheckedChange = { viewModel.setAutoSync(it) })
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Đẩy lên", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { viewModel.pullFromCloud() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kéo về")
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bộ nhớ đệm Offline", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Đang dùng: $cacheSize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.clearCache()
                                android.widget.Toast.makeText(context, "Đã xóa sạch bộ nhớ cache!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Xóa Cache")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }

    if (activeDialog == "notes") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Xuất ghi chú & Highlights", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hệ thống hỗ trợ trích xuất toàn bộ các đoạn văn được highlight và ghi chú đã tạo sang các định dạng tài liệu phổ biến:", color = MaterialTheme.colorScheme.onSurface)
                    Button(
                        onClick = {
                            viewModel.exportHighlightsAsTxt { textContent ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "PageTurn TXT Export")
                                    putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Xuất ghi chú sang Text"))
                            }
                            activeDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Xuất sang Text (.txt)", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.exportHighlightsAsJson { jsonContent ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "PageTurn JSON Export")
                                    putExtra(android.content.Intent.EXTRA_TEXT, jsonContent)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Xuất ghi chú sang JSON"))
                            }
                            activeDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Xuất sang JSON (.json)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDialog = null }) {
                    Text("Đóng", color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }

    if (activeDialog == "preferences") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Tùy chọn đọc sách", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cỡ chữ mặc định
                    Text("Cỡ chữ mặc định trong trình đọc:", color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = settings.fontSizeSp.toFloat(),
                            onValueChange = { viewModel.setFontSize(it.toInt()) },
                            valueRange = 12f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${settings.fontSizeSp} sp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Font chữ
                    Text("Font chữ ưu tiên:", color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("serif" to "Serif", "sans-serif" to "Sans-Serif", "monospace" to "Monospace").forEach { (key, label) ->
                            val isSelected = settings.fontFamily == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.setFontFamily(key)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Màu nền đọc sách (warm, light, dark)
                    Text("Màu nền đọc sách mặc định:", color = MaterialTheme.colorScheme.onSurface)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("warm" to "Ấm áp", "light" to "Sáng", "dark" to "Tối").forEach { (themeKey, label) ->
                            val isSelected = settings.readingTheme == themeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.setReadingTheme(themeKey)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Hoàn tất", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- AVATAR & NAME HEADER ---
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                        )
                    )
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.name.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Serif
                    )
                )
            }
            // Edit Floating button on avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable { isEditing = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Name & Email
        Text(
            text = userProfile.name,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = viewModel.userEmail.ifBlank { userProfile.email.ifBlank { "anvo@gmail.com" } },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- STATS ROW (Books, Pages Read, Highlights) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$bookCount",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Sách", 
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = totalPagesReadFormatted,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Trang đã đọc", 
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$notesCount",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Đánh dấu", 
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SETTINGS SECTION TITLE ---
        Text(
            text = "Cài đặt",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            ),
            textAlign = TextAlign.Start
        )

        // --- SETTINGS ITEMS LIST ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val listItems = listOf(
                Triple("Thông báo", Icons.Default.Notifications, "notifications"),
                Triple("Xuất ghi chú", Icons.Default.Edit, "notes"),
                Triple("Đồng bộ thiết bị", Icons.Default.CloudSync, "sync"),
                Triple("Tùy chọn đọc", Icons.Default.Tune, "preferences")
            )

            listItems.forEachIndexed { index, (title, icon, actionKey) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeDialog = actionKey }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (index < listItems.size - 1) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SIGN OUT BUTTON (DARK BORDER & RED TEXT) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.signOut()
                    onSignOut()
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Sign Out",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Đăng xuất",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Book Title"
}

fun readTextFromUri(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: ""
    } catch (e: Exception) {
        ""
    }
}

fun generateLongPdfMockContent(title: String): String {
    val builder = StringBuilder()
    builder.append("TÀI LIỆU HỌC TẬP & NGHIÊN CỨU: $title\n\n")
    builder.append("Tài liệu này đã được trích xuất kỹ thuật số và chuẩn hóa định dạng hiển thị bởi bộ giải mã nâng cao của PageTurn. Dưới đây là nội dung chi tiết của tài liệu bao gồm các biểu đồ và hình vẽ minh họa.\n\n")
    builder.append("[image: diagram_overview]\n\n")
    builder.append("SƠ ĐỒ TỔNG QUAN TÀI LIỆU\n\n")
    
    for (i in 1..8) {
        builder.append("Chương $i: Phân Tích Chuyên Sâu Phần Thứ $i\n\n")
        builder.append("1. Giới thiệu tổng quan chương học:\n\n")
        builder.append("Chương này tập trung vào các khái niệm trọng tâm của phần số $i, phân tích phương pháp luận thực tế và định hướng phát triển tối ưu.\n\n")
        builder.append("[image: diagram_chapter_$i]\n\n")
        builder.append("BIỂU ĐỒ MINH HỌA TIẾN TRÌNH CHƯƠNG $i\n\n")
        builder.append("2. Nội dung lý thuyết cốt lõi:\n\n")
        builder.append("Hệ thống lý thuyết được xây dựng dựa trên dữ liệu thu thập thực tế. Chúng tôi khuyến nghị người đọc ghi chép, tô đậm (highlight) những từ khóa hoặc đoạn văn quan trọng để dễ dàng ôn tập và ghi nhớ sau này thông qua tính năng Lưu trữ của ứng dụng.\n\n")
        builder.append("Chúc các bạn gặt hái được nhiều kiến thức bổ ích từ chương này!\n\n")
    }
    return builder.toString()
}

@Composable
fun HighlightsScreenContent(viewModel: LibraryViewModel, onBookClick: (String) -> Unit) {
    val highlights by viewModel.allHighlights.collectAsState()
    val bookTitles by viewModel.bookTitles.collectAsState()

    var editingHighlight by remember { mutableStateOf<com.pageturn.core.model.Highlight?>(null) }
    var noteTextState by remember { mutableStateOf("") }
    var colorHexState by remember { mutableStateOf("#FFF176") }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog && editingHighlight != null) {
        val currentHighlight = editingHighlight!!
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Chi tiết Highlight & Ghi chú", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "\"${currentHighlight.selectedText ?: "Đoạn văn"}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(android.graphics.Color.parseColor(colorHexState)).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    )

                    OutlinedTextField(
                        value = noteTextState,
                        onValueChange = { noteTextState = it },
                        label = { Text("Nội dung ghi chú...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Màu đánh dấu:", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val highlightColors = listOf("#FFF176", "#A5D6A7", "#F48FB1", "#90CAF9")
                        highlightColors.forEach { colorStr ->
                            val colorValue = Color(android.graphics.Color.parseColor(colorStr))
                            val isSelected = colorHexState == colorStr
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
                                    .clickable { colorHexState = colorStr }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Buttons Row to prevent squeezing inside confirmButton container
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Hủy", color = Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Navigate to position button
                            TextButton(
                                onClick = {
                                    onBookClick("${currentHighlight.bookId}?chapter=${currentHighlight.chapterNumber}&paragraph=${currentHighlight.startOffset}")
                                    showEditDialog = false
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = PtNavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Đi đến vị trí", color = PtNavyPrimary, fontWeight = FontWeight.Bold)
                            }

                            // Save button
                            Button(
                                onClick = {
                                    viewModel.updateHighlight(
                                        id = currentHighlight.id,
                                        bookId = currentHighlight.bookId,
                                        chapterNumber = currentHighlight.chapterNumber,
                                        startOffset = currentHighlight.startOffset,
                                        colorHex = colorHexState,
                                        selectedText = currentHighlight.selectedText ?: "",
                                        noteText = noteTextState
                                    )
                                    showEditDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PtNavyPrimary, contentColor = Color.White)
                            ) {
                                Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = null
        )
    }

    var selectedBookId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val currentBookId = selectedBookId
        if (currentBookId == null) {
            Text(
                text = "Ghi chú & Highlight",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = PtTextNavy)
            )

            if (highlights.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chưa có highlight hay ghi chú nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PtTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val grouped = highlights.groupBy { it.bookId }
                    grouped.forEach { (bookId, bookHighlights) ->
                        val bookLabel = bookTitles[bookId] ?: when (bookId) {
                            "1" -> "Sherlock Holmes"
                            "2" -> "The Great Gatsby"
                            "3" -> "Pride and Prejudice"
                            "4" -> "Moby Dick"
                            else -> {
                                if (bookId.startsWith("local_")) "Tài liệu Local" else bookId
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedBookId = bookId },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = bookLabel,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = PtTextMain
                                            )
                                            Text(
                                                text = "${bookHighlights.size} ghi chú & highlight",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = PtTextSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = PtTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val bookHighlights = highlights.filter { it.bookId == currentBookId }
            val bookLabel = bookTitles[currentBookId] ?: when (currentBookId) {
                "1" -> "Sherlock Holmes"
                "2" -> "The Great Gatsby"
                "3" -> "Pride and Prejudice"
                "4" -> "Moby Dick"
                else -> {
                    if (currentBookId.startsWith("local_")) "Tài liệu Local" else currentBookId
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { selectedBookId = null }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bookLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = PtTextNavy),
                    maxLines = 1
                )
            }

            if (bookHighlights.isEmpty()) {
                LaunchedEffect(Unit) {
                    selectedBookId = null
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookHighlights) { highlight ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingHighlight = highlight
                                    noteTextState = highlight.noteText ?: ""
                                    colorHexState = highlight.colorHex
                                    showEditDialog = true
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chương ${highlight.chapterNumber}, Đoạn ${highlight.startOffset + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PtNavyPrimary)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(highlight.colorHex)))
                                    )
                                }

                                val textToDisplay = if (!highlight.selectedText.isNullOrBlank()) highlight.selectedText else "Đoạn văn được highlight"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "\"$textToDisplay\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = FontFamily.Serif
                                        ),
                                        color = PtTextMain
                                    )
                                }

                                if (!highlight.noteText.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("📝 ", fontSize = 14.sp)
                                        Text(
                                            text = "Ghi chú: ${highlight.noteText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PtTextSecondary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.deleteHighlight(highlight.id) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Xóa ghi nhớ", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

fun renderPdfToImages(context: android.content.Context, uri: Uri, bookId: String): List<String> {
    val imagePaths = mutableListOf<String>()
    var file: java.io.File? = null
    var inputFd: android.os.ParcelFileDescriptor? = null
    var renderer: android.graphics.pdf.PdfRenderer? = null
    try {
        file = java.io.File(context.filesDir, "${bookId}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        inputFd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = android.graphics.pdf.PdfRenderer(inputFd)
        val pageCount = renderer.pageCount
        
        val bookDir = java.io.File(context.filesDir, bookId)
        if (!bookDir.exists()) bookDir.mkdirs()
        
        for (i in 0 until pageCount) {
            val page = renderer.openPage(i)
            val scale = 1.2f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            
            val imageFile = java.io.File(bookDir, "page_${i}.jpg")
            imageFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            imagePaths.add(imageFile.absolutePath)
        }
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    } finally {
        try { renderer?.close() } catch(e: Exception) {}
        try { inputFd?.close() } catch(e: Exception) {}
        try { file?.delete() } catch(e: Exception) {}
    }
    return imagePaths
}

fun parseEpubText(context: android.content.Context, uri: Uri): String {
    val textBuilder = StringBuilder()
    var tempFile: java.io.File? = null
    try {
        tempFile = java.io.File.createTempFile("pageturn_import_", ".epub", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val zipFile = java.util.zip.ZipFile(tempFile)
        
        var opfPath = ""
        val containerEntry = zipFile.getEntry("META-INF/container.xml")
        if (containerEntry != null) {
            val containerContent = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val matcher = java.util.regex.Pattern.compile("<rootfile\\s+[^>]*full-path=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(containerContent)
            if (matcher.find()) {
                opfPath = matcher.group(1)
            }
        }
        
        if (opfPath.isEmpty()) {
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".opf", ignoreCase = true)) {
                    opfPath = entry.name
                    break
                }
            }
        }

        if (opfPath.isNotEmpty()) {
            val opfEntry = zipFile.getEntry(opfPath)
            if (opfEntry != null) {
                val opfContent = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                
                val manifestItems = mutableMapOf<String, String>()
                val itemMatcher = java.util.regex.Pattern.compile("<item\\s+[^>]*id=\"([^\"]+)\"[^>]*href=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                while (itemMatcher.find()) {
                    manifestItems[itemMatcher.group(1)] = itemMatcher.group(2)
                }
                
                val spineIds = mutableListOf<String>()
                val spineMatcher = java.util.regex.Pattern.compile("<itemref\\s+[^>]*idref=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                while (spineMatcher.find()) {
                    spineIds.add(spineMatcher.group(1))
                }
                
                val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                
                val readingPaths = spineIds.mapNotNull { id ->
                    manifestItems[id]?.let { href ->
                        val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                        if (decodedHref.startsWith("/")) decodedHref.drop(1) else "$opfDir$decodedHref"
                    }
                }
                
                for (path in readingPaths) {
                    val entry = zipFile.getEntry(path) ?: zipFile.getEntry(path.replace("\\", "/"))
                    if (entry != null) {
                        val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                        val plainText = stripHtml(htmlContent)
                        if (plainText.isNotBlank()) {
                            textBuilder.append(plainText).append("\n\n")
                        }
                    }
                }
            }
        }
        
        if (textBuilder.isEmpty()) {
            val htmlEntries = mutableListOf<java.util.zip.ZipEntry>()
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.lowercase()
                if (name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")) {
                    htmlEntries.add(entry)
                }
            }
            htmlEntries.sortBy { it.name }
            for (entry in htmlEntries) {
                val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                val plainText = stripHtml(htmlContent)
                if (plainText.isNotBlank()) {
                    textBuilder.append(plainText).append("\n\n")
                }
            }
        }
        
        zipFile.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try { tempFile?.delete() } catch (e: Exception) {}
    }
    return textBuilder.toString()
}

fun decompressPalmDoc(data: ByteArray): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < data.size) {
        val c = data[i].toInt() and 0xFF
        i++
        if (c in 1..8) {
            for (j in 0 until c) {
                if (i < data.size) {
                    out.write(data[i].toInt() and 0xFF)
                    i++
                }
            }
        } else if (c == 0) {
            out.write(0)
        } else if (c in 9..0x7f) {
            out.write(c)
        } else if (c in 0x80..0xbf) {
            if (i < data.size) {
                val c2 = data[i].toInt() and 0xFF
                i++
                val distance = (((c and 0x3F) shl 8) or c2) shr 3
                val length = (c2 and 0x07) + 3
                val outBytes = out.toByteArray()
                val start = outBytes.size - distance
                for (j in 0 until length) {
                    val idx = start + j
                    if (idx >= 0 && idx < outBytes.size) {
                        out.write(outBytes[idx].toInt() and 0xFF)
                    }
                }
            }
        } else if (c in 0xc0..0xff) {
            out.write(' '.code)
            out.write(c xor 0x80)
        }
    }
    return out.toByteArray()
}

fun parseAzw3Text(context: android.content.Context, uri: Uri): String {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val fileBytes = inputStream.use { it.readBytes() }
        
        if (fileBytes.size < 78) return ""
        
        val numRecords = ((fileBytes[76].toInt() and 0xFF) shl 8) or (fileBytes[77].toInt() and 0xFF)
        val recordOffsets = LongArray(numRecords)
        for (i in 0 until numRecords) {
            val offset = 78 + i * 8
            if (offset + 4 <= fileBytes.size) {
                val rOffset = ((fileBytes[offset].toLong() and 0xFF) shl 24) or
                              ((fileBytes[offset+1].toLong() and 0xFF) shl 16) or
                              ((fileBytes[offset+2].toLong() and 0xFF) shl 8) or
                              (fileBytes[offset+3].toLong() and 0xFF)
                recordOffsets[i] = rOffset
            }
        }
        
        if (numRecords < 2) return ""
        val headerOffset = recordOffsets[0].toInt()
        if (headerOffset + 10 > fileBytes.size) return ""
        
        val compression = ((fileBytes[headerOffset].toInt() and 0xFF) shl 8) or (fileBytes[headerOffset+1].toInt() and 0xFF)
        val textRecordCount = ((fileBytes[headerOffset + 8].toInt() and 0xFF) shl 8) or (fileBytes[headerOffset + 9].toInt() and 0xFF)
        
        val textBuilder = java.lang.StringBuilder()
        for (r in 1..minOf(textRecordCount, numRecords - 1)) {
            val start = recordOffsets[r].toInt()
            val end = if (r + 1 < numRecords) recordOffsets[r + 1].toInt() else fileBytes.size
            if (start in 0 until fileBytes.size && end in start..fileBytes.size) {
                val recordData = fileBytes.copyOfRange(start, end)
                val decompressed = when (compression) {
                    1 -> recordData
                    2 -> decompressPalmDoc(recordData)
                    else -> recordData
                }
                textBuilder.append(String(decompressed, Charsets.UTF_8))
            }
        }
        return stripHtml(textBuilder.toString())
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}

fun stripHtml(html: String): String {
    var text = html
    // Remove script and style blocks entirely
    text = text.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
    text = text.replace(Regex("<head>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")

    // Block-level tags: insert newline markers so paragraphs are preserved
    val blockTags = listOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "blockquote", "section", "article", "header", "footer", "main", "nav", "aside", "figure", "figcaption")
    for (tag in blockTags) {
        text = text.replace(Regex("</$tag>", RegexOption.IGNORE_CASE), "\n")
        text = text.replace(Regex("<$tag[^>]*>", RegexOption.IGNORE_CASE), "\n")
    }
    // Line break tags
    text = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")

    // Remove all remaining HTML tags
    text = text.replace(Regex("<[^>]*>"), "")

    // Decode common HTML entities
    text = text
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&ldquo;", "\u201C")
        .replace("&rdquo;", "\u201D")
        .replace("&lsquo;", "\u2018")
        .replace("&rsquo;", "\u2019")

    // Collapse multiple spaces/tabs on the same line into single space
    text = text.replace(Regex("[ \t]+"), " ")

    // Split into lines, trim each line
    val rawLines = text.split("\n")
    val paragraphs = mutableListOf<String>()
    val currentParagraph = StringBuilder()

    for (line in rawLines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            // Empty line = paragraph boundary
            val built = currentParagraph.toString().trim()
            if (built.isNotEmpty()) {
                if (!built.matches(Regex("\\d+"))) {
                    paragraphs.add(built)
                }
                currentParagraph.clear()
            }
        } else {
            // Skip standalone page numbers on a line
            if (trimmed.matches(Regex("\\d+"))) {
                continue
            }
            // Append to current paragraph with space
            if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
            currentParagraph.append(trimmed)
        }
    }
    // Flush final paragraph
    val built = currentParagraph.toString().trim()
    if (built.isNotEmpty() && !built.matches(Regex("\\d+"))) {
        paragraphs.add(built)
    }

    // Join paragraphs with double newline for reader to split on
    return paragraphs.joinToString("\n\n")
}

fun parseEpubMetadata(context: android.content.Context, uri: Uri): Triple<String, String, String> {
    var title = ""
    var author = ""
    var coverPath = ""
    var tempFile: java.io.File? = null
    try {
        tempFile = java.io.File.createTempFile("epub_meta_", ".epub", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val zipFile = java.util.zip.ZipFile(tempFile)
        var opfPath = ""
        val containerEntry = zipFile.getEntry("META-INF/container.xml")
        if (containerEntry != null) {
            val containerContent = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val matcher = java.util.regex.Pattern.compile("<rootfile\\s+[^>]*full-path=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(containerContent)
            if (matcher.find()) {
                opfPath = matcher.group(1)
            }
        }
        if (opfPath.isEmpty()) {
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".opf", ignoreCase = true)) {
                    opfPath = entry.name
                    break
                }
            }
        }

        if (opfPath.isNotEmpty()) {
            val opfEntry = zipFile.getEntry(opfPath)
            if (opfEntry != null) {
                val opfContent = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                // Parse Title
                val titleMatcher = java.util.regex.Pattern.compile("<dc:title[^>]*>([^<]+)</dc:title>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                if (titleMatcher.find()) {
                    title = titleMatcher.group(1).trim()
                }
                // Parse Author
                val authorMatcher = java.util.regex.Pattern.compile("<dc:creator[^>]*>([^<]+)</dc:creator>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                if (authorMatcher.find()) {
                    author = authorMatcher.group(1).trim()
                }

                // Find Cover Image ID from meta tag or item properties
                var coverId = ""
                val metaMatcher = java.util.regex.Pattern.compile("<meta\\s+[^>]*name=\"cover\"\\s+[^>]*content=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                if (metaMatcher.find()) {
                    coverId = metaMatcher.group(1)
                }

                val manifestItems = mutableMapOf<String, String>() // id -> href
                val itemMatcher = java.util.regex.Pattern.compile("<item\\s+[^>]*id=\"([^\"]+)\"[^>]*href=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                while (itemMatcher.find()) {
                    manifestItems[itemMatcher.group(1)] = itemMatcher.group(2)
                }

                var coverHref = ""
                if (coverId.isNotEmpty()) {
                    coverHref = manifestItems[coverId] ?: ""
                }
                if (coverHref.isEmpty()) {
                    // Fallback to searching item properties="cover-image"
                    val propMatcher = java.util.regex.Pattern.compile("<item\\s+[^>]*properties=\"cover-image\"[^>]*href=\"([^\"]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                    if (propMatcher.find()) {
                        coverHref = propMatcher.group(1)
                    }
                }
                if (coverHref.isEmpty()) {
                    // Fallback to searching id="cover" or id="cover-image"
                    coverHref = manifestItems["cover"] ?: manifestItems["cover-image"] ?: ""
                }
                if (coverHref.isEmpty()) {
                    // Fallback to searching href that contains "cover" and has image extension
                    for ((_, href) in manifestItems) {
                        if (href.lowercase().contains("cover") && (href.endsWith(".jpg") || href.endsWith(".jpeg") || href.endsWith(".png"))) {
                            coverHref = href
                            break
                        }
                    }
                }

                if (coverHref.isNotEmpty()) {
                    val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                    val decodedHref = java.net.URLDecoder.decode(coverHref, java.nio.charset.StandardCharsets.UTF_8.name())
                    val coverImgPathInsideZip = if (decodedHref.startsWith("/")) decodedHref.drop(1) else "$opfDir$decodedHref"
                    val zipEntry = zipFile.getEntry(coverImgPathInsideZip) ?: zipFile.getEntry(coverImgPathInsideZip.replace("\\", "/"))
                    if (zipEntry != null) {
                        val coversDir = java.io.File(context.filesDir, "covers")
                        if (!coversDir.exists()) coversDir.mkdirs()
                        val coverFile = java.io.File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                        zipFile.getInputStream(zipEntry).use { input ->
                            coverFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        coverPath = coverFile.absolutePath
                    }
                }
            }
        }
        zipFile.close()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try { tempFile?.delete() } catch(e: Exception) {}
    }
    return Triple(title, author, coverPath)
}

@Composable
fun HomeScreenContent(
    uiState: LibraryUiState,
    viewModel: LibraryViewModel,
    allBooksList: List<com.pageturn.core.network.api.PublicBookDto>,
    onBookClick: (String) -> Unit,
    onNavigateToMyLibrary: () -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    val readingBooks = remember(uiState) {
        (uiState as? LibraryUiState.Success)?.books?.filter { it.progressPercent > 0.0f } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nửa trên: Sách đang đọc
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sách đang đọc",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                TextButton(onClick = onNavigateToMyLibrary) {
                    Text("Tất cả >", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                }
            }
        }

        if (readingBooks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Chưa có sách đang đọc dở",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(readingBooks) { book ->
                        val coverFile = remember(book.coverUrl) {
                            if (book.coverUrl.isNotEmpty() && !book.coverUrl.startsWith("http")) {
                                val f = java.io.File(book.coverUrl)
                                if (f.exists()) f else null
                            } else null
                        }
                        val bitmap = remember(coverFile) {
                            coverFile?.let {
                                try {
                                    BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { onBookClick(book.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = book.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        NetworkImage(
                                            url = book.coverUrl,
                                            contentDescription = book.title,
                                            modifier = Modifier.fillMaxSize(),
                                            fallback = { BookCover(title = book.title, author = book.author) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = book.title,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = book.progressPercent,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${(book.progressPercent * 100).toInt()}% hoàn thành",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Nửa dưới: Sách trên server
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sách trên server",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                TextButton(onClick = onNavigateToDiscover) {
                    Text("Khám phá >", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                }
            }
        }

        if (allBooksList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            val previewServerBooks = allBooksList.take(6)
            items(previewServerBooks) { publicBook ->
                val downloadingIds by viewModel.downloadingBookIds.collectAsState()
                val isDownloading = downloadingIds.contains(publicBook.id.toString())
                val libraryBooks = (uiState as? LibraryUiState.Success)?.books ?: emptyList()
                val isAlreadyDownloaded = remember(libraryBooks, publicBook.id, publicBook.bookHash) {
                    val idStr = publicBook.id.toString()
                    libraryBooks.any { it.id == idStr || (it.id == publicBook.bookHash && publicBook.bookHash.isNotEmpty()) }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isAlreadyDownloaded) {
                                val match = libraryBooks.find { it.id == publicBook.id.toString() || (it.id == publicBook.bookHash && publicBook.bookHash.isNotEmpty()) }
                                match?.let { onBookClick(it.id) }
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(75.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            NetworkImage(
                                url = publicBook.coverUrl ?: "",
                                contentDescription = publicBook.title,
                                modifier = Modifier.fillMaxSize(),
                                fallback = { BookCover(title = publicBook.title, author = publicBook.author) }
                            )
                        }

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = publicBook.title,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = publicBook.author,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Action button / status
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDownloading) Color.Transparent
                                    else if (isAlreadyDownloaded) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                                .clickable {
                                    if (!isAlreadyDownloaded && !isDownloading) {
                                        viewModel.downloadBook(
                                            publicBook.id.toString(),
                                            publicBook.title,
                                            publicBook.author,
                                            publicBook.coverUrl ?: "",
                                            publicBook.description ?: ""
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (isAlreadyDownloaded) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Đã tải", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Tải xuống", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyLibraryScreenContent(
    uiState: LibraryUiState,
    viewModel: LibraryViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    selectedCollection: String?,
    onSelectCollection: (String?) -> Unit,
    showCreateCollectionDialog: Boolean,
    onShowCreateCollectionDialogChange: (Boolean) -> Unit,
    showRenameCollectionDialog: Boolean,
    onShowRenameCollectionDialogChange: (Boolean) -> Unit,
    showAddBooksToCollectionDialog: Boolean,
    onShowAddBooksToCollectionDialogChange: (Boolean) -> Unit,
    showAddToCollectionDialog: Boolean,
    onShowAddToCollectionDialogChange: (Boolean) -> Unit,
    selectedBookForAddToCollection: Book?,
    onSelectedBookForAddToCollectionChange: (Book?) -> Unit,
    renameCollectionName: String,
    onRenameCollectionNameChange: (String) -> Unit,
    onBookClick: (String) -> Unit
) {
    val favoriteBookIds by viewModel.favoriteBookIds.collectAsState()
    val collections by viewModel.userCollections.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Premium Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Tìm kiếm sách...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )

        // Premium Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            listOf(
                "all" to "Tất cả",
                "favorite" to "Yêu thích",
                "collections" to "Bộ sưu tập"
            ).forEach { (key, label) ->
                val isSelected = currentFilter == key
                val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            onFilterChange(key)
                            onSelectCollection(null)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = chipTextColor
                    )
                }
            }
        }

        // Collection details header
        if (currentFilter == "collections" && selectedCollection != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bộ sưu tập: $selectedCollection",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { onShowAddBooksToCollectionDialogChange(true) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm sách", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onRenameCollectionNameChange(selectedCollection); onShowRenameCollectionDialogChange(true) }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đổi tên", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onSelectCollection(null) }
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quay lại", fontSize = 12.sp)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        currentFilter == "favorite" -> "Danh sách yêu thích"
                        currentFilter == "collections" -> "Các bộ sưu tập"
                        else -> "Kho sách cá nhân"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Grid Content
        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is LibraryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is LibraryUiState.Success -> {
                if (currentFilter == "collections") {
                    if (selectedCollection == null) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Item 0: Create Collection Button
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(142.dp)
                                            .clickable { onShowCreateCollectionDialogChange(true) }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp, vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Tạo Bộ sưu tập",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Thêm mới",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            items(collections.keys.toList()) { colName ->
                                val bookIds = collections[colName] ?: emptySet()
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(142.dp).clickable { onSelectCollection(colName) }) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = colName,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${bookIds.size} cuốn sách",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCollection(colName) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show books in selected custom collection
                        val bookIds = collections[selectedCollection!!] ?: emptySet()
                        val collectionBooks = remember(state.books, bookIds) {
                            state.books.filter { bookIds.contains(it.id) }
                        }

                        if (collectionBooks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Bộ sưu tập trống", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(collectionBooks) { book ->
                                    BookItem(
                                        book = book,
                                        onBookClick = onBookClick,
                                        isFavorite = favoriteBookIds.contains(book.id),
                                        onFavoriteToggle = { viewModel.toggleBookFavorite(book.id) },
                                        onDownloadClick = { viewModel.downloadBook(book.id, book.title, book.author, book.coverUrl, book.description ?: "") },
                                        onDeleteCloudClick = { viewModel.deleteLocalBook(book.id) },
                                        onAddToCollection = { onSelectedBookForAddToCollectionChange(book); onShowAddToCollectionDialogChange(true) },
                                        onRemoveFromCollection = { viewModel.toggleBookInCollection(selectedCollection, book.id) },
                                        onDeleteBookClick = { viewModel.deleteLocalBook(book.id) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val filteredBooks = state.books.filter { book ->
                        (currentFilter == "all" || favoriteBookIds.contains(book.id)) &&
                        (book.title.contains(searchQuery, ignoreCase = true) ||
                         book.author.contains(searchQuery, ignoreCase = true))
                    }

                    if (filteredBooks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentFilter == "favorite") "Không có sách yêu thích nào" else "Không tìm thấy sách phù hợp",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredBooks) { book ->
                                BookItem(
                                    book = book,
                                    onBookClick = onBookClick,
                                    isFavorite = favoriteBookIds.contains(book.id),
                                    onFavoriteToggle = { viewModel.toggleBookFavorite(book.id) },
                                    onDownloadClick = { viewModel.downloadBook(book.id, book.title, book.author, book.coverUrl, book.description ?: "") },
                                    onDeleteCloudClick = { viewModel.deleteLocalBook(book.id) },
                                    onAddToCollection = { onSelectedBookForAddToCollectionChange(book); onShowAddToCollectionDialogChange(true) },
                                    onDeleteBookClick = { viewModel.deleteLocalBook(book.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


