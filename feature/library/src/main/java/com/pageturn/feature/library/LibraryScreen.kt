package com.pageturn.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                selectedTab = 3
                                showProfile = false
                                scope.launch { drawerState.close() }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.name.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userProfile.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                // Drawer items
                val menuItems = listOf(
                    Triple(0, "Thư viện", Icons.Default.LibraryBooks),
                    Triple(1, "Khám phá", Icons.Default.Explore),
                    Triple(2, "Lưu trữ", Icons.Default.Archive),
                    Triple(4, "Ghi chú & Highlight", Icons.Default.Edit),
                    Triple(3, "Cài đặt", Icons.Default.Settings)
                )

                menuItems.forEach { (index, title, icon) ->
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = icon, contentDescription = title) },
                        label = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            showProfile = false
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
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
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
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
            floatingActionButton = {
                if (selectedTab == 0 && !showProfile) {
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
                                // TAB 0: LIBRARY MAIN VIEW
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // Premium Search Bar
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
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
                                                        currentFilter = key
                                                        selectedCollection = null
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

                                    // Recent Reads header details
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
                                                currentFilter == "collections" && selectedCollection != null -> "Bộ sưu tập: $selectedCollection"
                                                currentFilter == "collections" -> "Các bộ sưu tập"
                                                else -> "Sách đọc gần đây"
                                            },
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (currentFilter == "collections" && selectedCollection != null) {
                                            TextButton(
                                                onClick = { selectedCollection = null }
                                            ) {
                                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Quay lại")
                                            }
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
                                            val favoriteBookIds by viewModel.favoriteBookIds.collectAsState()
                                            
                                            if (currentFilter == "collections") {
                                                if (selectedCollection == null) {
                                                    // Group books by author
                                                    val collectionsMap = remember(state.books) {
                                                        state.books.groupBy { it.author.ifBlank { "Khác" } }
                                                    }
                                                    
                                                    if (collectionsMap.isEmpty()) {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            Text("Không có bộ sưu tập nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    } else {
                                                        LazyVerticalGrid(
                                                            columns = GridCells.Fixed(2),
                                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                            verticalArrangement = Arrangement.spacedBy(20.dp),
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            items(collectionsMap.keys.toList()) { author ->
                                                                val booksInCollection = collectionsMap[author] ?: emptyList()
                                                                Card(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable { selectedCollection = author },
                                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                                ) {
                                                                    Column(
                                                                        modifier = Modifier.padding(16.dp),
                                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Folder,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(64.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.height(8.dp))
                                                                        Text(
                                                                            text = author,
                                                                            fontWeight = FontWeight.Bold,
                                                                            maxLines = 1,
                                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                                            style = MaterialTheme.typography.bodyLarge
                                                                        )
                                                                        Text(
                                                                            text = "${booksInCollection.size} cuốn sách",
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    // Show books in selected author collection
                                                    val collectionBooks = remember(state.books, selectedCollection) {
                                                        state.books.filter { it.author.ifBlank { "Khác" } == selectedCollection }
                                                    }
                                                    
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
                                                                onFavoriteToggle = { viewModel.toggleBookFavorite(book.id) }
                                                            )
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
                                                                onFavoriteToggle = { viewModel.toggleBookFavorite(book.id) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // TAB 1: DISCOVER TAB (GORGEOUS UI/UX)
                                DiscoverScreenContent()
                            }
                            2 -> {
                                // TAB 2: ARCHIVE TAB
                                ArchiveScreenContent(uiState, onBookClick, onDeleteBook = { viewModel.deleteLocalBook(it) })
                            }
                            3 -> {
                                // TAB 3: SETTINGS TAB
                                SettingsScreenContent(viewModel = viewModel, onSignOut = onSignOut)
                            }
                            4 -> {
                                // TAB 4: HIGHLIGHTS & NOTES TAB
                                HighlightsScreenContent(viewModel, onBookClick)
                            }
                        }
                    }
                }
            }
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
    onFavoriteToggle: () -> Unit
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
                            Text("Thêm vào Collection")
                        },
                        onClick = {
                            showMenu = false
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
fun DiscoverScreenContent() {
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    // Dữ liệu sách thật với ảnh bìa thật trực tuyến chất lượng cao
    val popularBooks = remember(selectedCategory) {
        when (selectedCategory) {
            "Trinh thám" -> listOf(
                Triple("THE ADVENTURES OF SHERLOCK HOLMES", "Arthur Conan Doyle", "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg"),
                Triple("Phía Sau Nghi Can X", "Higashino Keigo", "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=300&auto=format&fit=crop"),
                Triple("Sự Im Lặng Của Bầy Cừu", "Thomas Harris", "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=300&auto=format&fit=crop")
            )
            "Cổ điển" -> listOf(
                Triple("THE GREAT GATSBY", "F. Scott Fitzgerald", "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg"),
                Triple("PRIDE AND PREJUDICE", "Jane Austen", "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"),
                Triple("War and Peace", "Leo Tolstoy", "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.medium.jpg")
            )
            "Lãng mạn" -> listOf(
                Triple("Rừng Na Uy", "Haruki Murakami", "https://images.unsplash.com/photo-1474932430478-367db26836c1?q=80&w=300&auto=format&fit=crop"),
                Triple("Romeo và Juliet", "William Shakespeare", "https://www.gutenberg.org/cache/epub/1513/pg1513.cover.medium.jpg"),
                Triple("Wuthering Heights", "Emily Brontë", "https://www.gutenberg.org/cache/epub/768/pg768.cover.medium.jpg")
            )
            "Kinh tế" -> listOf(
                Triple("Nhà Giả Kim", "Paulo Coelho", "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=300&auto=format&fit=crop"),
                Triple("Đắc Nhân Tâm", "Dale Carnegie", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?q=80&w=300&auto=format&fit=crop"),
                Triple("Think and Grow Rich", "Napoleon Hill", "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?q=80&w=300&auto=format&fit=crop")
            )
            "Lịch sử" -> listOf(
                Triple("Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=300&auto=format&fit=crop"),
                Triple("Lịch Sử Văn Minh Thế Giới", "Will Durant", "https://images.unsplash.com/photo-1463320305624-91d179a990aa6?q=80&w=300&auto=format&fit=crop"),
                Triple("The History of Herodotus", "Herodotus", "https://www.gutenberg.org/cache/epub/2707/pg2707.cover.medium.jpg")
            )
            else -> listOf(
                Triple("THE ADVENTURES OF SHERLOCK HOLMES", "Arthur Conan Doyle", "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg"),
                Triple("THE GREAT GATSBY", "F. Scott Fitzgerald", "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg"),
                Triple("PRIDE AND PREJUDICE", "Jane Austen", "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg")
            )
        }
    }

    val recommendations = remember(selectedCategory) {
        when (selectedCategory) {
            "Trinh thám" -> listOf(
                Triple("Đề thi đẫm máu", "Lôi Mễ", "https://images.unsplash.com/photo-1509021436665-8f07dbf5bf1d?q=80&w=300&auto=format&fit=crop"),
                Triple("Mười người da đen nhỏ", "Agatha Christie", "https://images.unsplash.com/photo-1531988042231-d39a9cc12a9a?q=80&w=300&auto=format&fit=crop")
            )
            "Cổ điển" -> listOf(
                Triple("Moby Dick", "Herman Melville", "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg"),
                Triple("Những Người Khốn Khổ", "Victor Hugo", "https://images.unsplash.com/photo-1541963463532-d68292c34b19?q=80&w=300&auto=format&fit=crop")
            )
            "Lãng mạn" -> listOf(
                Triple("Trà hoa nữ", "Alexandre Dumas con", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300&auto=format&fit=crop"),
                Triple("Cuốn theo chiều gió", "Margaret Mitchell", "https://images.unsplash.com/photo-1476275466078-4007374efbbe?q=80&w=300&auto=format&fit=crop")
            )
            "Kinh tế" -> listOf(
                Triple("Chiến Tranh Tiền Tệ", "Song Hongbing", "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=300&auto=format&fit=crop"),
                Triple("Kinh tế học hài hước", "Steven Levitt", "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=300&auto=format&fit=crop")
            )
            "Lịch sử" -> listOf(
                Triple("Lược sử thời gian", "Stephen Hawking", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=300&auto=format&fit=crop"),
                Triple("Súng, vi trùng và thép", "Jared Diamond", "https://images.unsplash.com/photo-1447069387593-a5de0862481e?q=80&w=300&auto=format&fit=crop")
            )
            else -> listOf(
                Triple("Pride and Prejudice", "Jane Austen", "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"),
                Triple("Moby Dick", "Herman Melville", "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg"),
                Triple("War and Peace", "Leo Tolstoy", "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.medium.jpg")
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Section header + Categories
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Thể loại",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                val categories = listOf("Tất cả", "Trinh thám", "Cổ điển", "Lãng mạn", "Kinh tế", "Lịch sử")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        // Popular Books Row
        item {
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đọc nhiều nhất tuần",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Lấy tối đa 6 sách cho khu vực Popular
                val popularSixBooks = remember(popularBooks, recommendations) {
                    val list = mutableListOf<Triple<String, String, String>>()
                    list.addAll(popularBooks)
                    list.addAll(recommendations)
                    list.distinctBy { it.first }.take(6)
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                ) {
                    items(popularSixBooks) { (title, author, coverUrl) ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clickable {},
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(175.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(10.dp))
                            ) {
                                NetworkImage(
                                    url = coverUrl,
                                    contentDescription = title,
                                    modifier = Modifier.fillMaxSize(),
                                    fallback = { BookCoverCanvas(title = title) }
                                )
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // All Books Section (Tất cả sách dạng Grid flexible)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tất cả sách",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                // Tổng hợp toàn bộ các cuốn sách trong hệ thống mà không giới hạn số lượng
                val allBooksList = remember(selectedCategory) {
                    if (selectedCategory == "Tất cả") {
                        // Nếu chọn Tất cả, gộp hết sách từ toàn bộ các category để hiển thị đầy đủ
                        val list = mutableListOf<Triple<String, String, String>>()
                        listOf("Trinh thám", "Cổ điển", "Lãng mạn", "Kinh tế", "Lịch sử").forEach { cat ->
                            when (cat) {
                                "Trinh thám" -> {
                                    list.add(Triple("THE ADVENTURES OF SHERLOCK HOLMES", "Arthur Conan Doyle", "https://www.gutenberg.org/cache/epub/1661/pg1661.cover.medium.jpg"))
                                    list.add(Triple("Phía Sau Nghi Can X", "Higashino Keigo", "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Sự Im Lặng Của Bầy Cừu", "Thomas Harris", "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Đề thi đẫm máu", "Lôi Mễ", "https://images.unsplash.com/photo-1509021436665-8f07dbf5bf1d?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Mười người da đen nhỏ", "Agatha Christie", "https://images.unsplash.com/photo-1531988042231-d39a9cc12a9a?q=80&w=300&auto=format&fit=crop"))
                                }
                                "Cổ điển" -> {
                                    list.add(Triple("THE GREAT GATSBY", "F. Scott Fitzgerald", "https://www.gutenberg.org/cache/epub/64317/pg64317.cover.medium.jpg"))
                                    list.add(Triple("PRIDE AND PREJUDICE", "Jane Austen", "https://www.gutenberg.org/cache/epub/1342/pg1342.cover.medium.jpg"))
                                    list.add(Triple("War and Peace", "Leo Tolstoy", "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.medium.jpg"))
                                    list.add(Triple("Moby Dick", "Herman Melville", "https://www.gutenberg.org/cache/epub/2701/pg2701.cover.medium.jpg"))
                                    list.add(Triple("Những Người Khốn Khổ", "Victor Hugo", "https://images.unsplash.com/photo-1541963463532-d68292c34b19?q=80&w=300&auto=format&fit=crop"))
                                }
                                "Lãng mạn" -> {
                                    list.add(Triple("Rừng Na Uy", "Haruki Murakami", "https://images.unsplash.com/photo-1474932430478-367db26836c1?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Romeo và Juliet", "William Shakespeare", "https://www.gutenberg.org/cache/epub/1513/pg1513.cover.medium.jpg"))
                                    list.add(Triple("Wuthering Heights", "Emily Brontë", "https://www.gutenberg.org/cache/epub/768/pg768.cover.medium.jpg"))
                                    list.add(Triple("Trà hoa nữ", "Alexandre Dumas con", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Cuốn theo chiều gió", "Margaret Mitchell", "https://images.unsplash.com/photo-1476275466078-4007374efbbe?q=80&w=300&auto=format&fit=crop"))
                                }
                                "Kinh tế" -> {
                                    list.add(Triple("Nhà Giả Kim", "Paulo Coelho", "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Đắc Nhân Tâm", "Dale Carnegie", "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Think and Grow Rich", "Napoleon Hill", "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Chiến Tranh Tiền Tệ", "Song Hongbing", "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Kinh tế học hài hước", "Steven Levitt", "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=300&auto=format&fit=crop"))
                                }
                                "Lịch sử" -> {
                                    list.add(Triple("Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Lịch Sử Văn Minh Thế Giới", "Will Durant", "https://images.unsplash.com/photo-1463320305624-91d179a990aa6?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("The History of Herodotus", "Herodotus", "https://www.gutenberg.org/cache/epub/2707/pg2707.cover.medium.jpg"))
                                    list.add(Triple("Lược sử thời gian", "Stephen Hawking", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=300&auto=format&fit=crop"))
                                    list.add(Triple("Súng, vi trùng và thép", "Jared Diamond", "https://images.unsplash.com/photo-1447069387593-a5de0862481e?q=80&w=300&auto=format&fit=crop"))
                                }
                            }
                        }
                        list.distinctBy { it.first }
                    } else {
                        val list = mutableListOf<Triple<String, String, String>>()
                        list.addAll(popularBooks)
                        list.addAll(recommendations)
                        list.distinctBy { it.first }
                    }
                }

                // Grid tự thích ứng chiều rộng cột tối thiểu 100.dp để tự động sắp xếp 2-4 cột tùy cỡ máy
                val chunkedBooks = remember(allBooksList) {
                    allBooksList.chunked(3) // Tạm thời gom nhóm thành các hàng để render hợp lệ trong LazyColumn
                }

                // Vẽ Layout dạng lưới thủ công hoặc lặp dòng với Row thích ứng vì đang nằm trong LazyColumn
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Chúng ta sử dụng Flexbox-like flow Row để tự thích ứng linh hoạt hơn
                    // Hoặc đơn giản là dùng các Row với cân bằng trọng số (Weight)
                    chunkedBooks.forEach { rowBooks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowBooks.forEach { (title, author, coverUrl) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {},
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(0.7f)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                                    ) {
                                        NetworkImage(
                                            url = coverUrl,
                                            contentDescription = title,
                                            modifier = Modifier.fillMaxSize(),
                                            fallback = { BookCoverCanvas(title = title) }
                                        )
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif
                                        ),
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Thêm các khoảng trống giả lập nếu hàng cuối cùng không đủ 3 sách để giữ cột thẳng hàng
                            repeat(3 - rowBooks.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing
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
    val bookCount = (uiState as? LibraryUiState.Success)?.books?.size ?: 0
    val context = LocalContext.current

    var nameInput by remember(userProfile) { mutableStateOf(userProfile.name) }
    var emailInput by remember(userProfile) { mutableStateOf(userProfile.email) }
    var bioInput by remember(userProfile) { mutableStateOf(userProfile.bio) }
    var isEditing by remember { mutableStateOf(false) }

    // Dialog chỉnh sửa thông tin nhanh
    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = { Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.Bold) },
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
                Button(onClick = {
                    viewModel.updateUserProfile(nameInput, emailInput, bioInput)
                    isEditing = false
                }) {
                    Text("Lưu", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Modal con xử lý từng mục cấu hình (Dialogs)
    var activeDialog by remember { mutableStateOf<String?>(null) } // "notifications", "goals", "notes", "sync", "preferences"

    if (activeDialog == "notifications") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Thông báo nhắc nhở", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bật nhắc nhở đọc sách", fontWeight = FontWeight.Bold)
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
                        Divider()
                        // Hằng ngày / Theo chu kỳ
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setReminderMode("daily") }
                            ) {
                                RadioButton(selected = settings.reminderMode == "daily", onClick = { viewModel.setReminderMode("daily") })
                                Text("Hằng ngày")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.setReminderMode("interval") }
                            ) {
                                RadioButton(selected = settings.reminderMode == "interval", onClick = { viewModel.setReminderMode("interval") })
                                Text("Theo chu kỳ")
                            }
                        }

                        if (settings.reminderMode == "daily") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Giờ nhắc:")
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
                                Text("Mỗi:")
                                OutlinedTextField(
                                    value = if (settings.reminderIntervalVal == 0) "" else settings.reminderIntervalVal.toString(),
                                    onValueChange = {
                                        val num = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                        viewModel.setReminderIntervalVal(num)
                                    },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                                Text("phút")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeDialog = null }) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }

    if (activeDialog == "goals") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Mục tiêu đọc sách", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Đặt mục tiêu số phút đọc sách mỗi ngày để hình thành thói quen tốt:")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var goalMinutes by remember { mutableStateOf(30) }
                        Slider(
                            value = goalMinutes.toFloat(),
                            onValueChange = { goalMinutes = it.toInt() },
                            valueRange = 5f..120f,
                            steps = 23,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$goalMinutes phút", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeDialog = null }) {
                    Text("Đồng ý", color = Color.White)
                }
            }
        )
    }

    if (activeDialog == "sync") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Đồng bộ Cloud & Cache", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tự động đồng bộ", fontWeight = FontWeight.Bold)
                            Text("Tải tiến độ lên đám mây tự động", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.autoSync, onCheckedChange = { viewModel.setAutoSync(it) })
                    }
                    
                    Divider()
                    
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

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bộ nhớ đệm Offline", fontWeight = FontWeight.Bold)
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
                Button(onClick = { activeDialog = null }) {
                    Text("Đóng", color = Color.White)
                }
            }
        )
    }

    if (activeDialog == "notes") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Xuất ghi chú & Highlights", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hệ thống hỗ trợ trích xuất toàn bộ các đoạn văn được highlight và ghi chú đã tạo sang các định dạng tài liệu phổ biến:")
                    Button(
                        onClick = {
                            android.widget.Toast.makeText(context, "Đang chuẩn bị tệp tin TXT trích xuất...", android.widget.Toast.LENGTH_SHORT).show()
                            activeDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Xuất sang Text (.txt)", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Đang chuẩn bị tệp tin JSON...", android.widget.Toast.LENGTH_SHORT).show()
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
                    Text("Đóng")
                }
            }
        )
    }

    if (activeDialog == "preferences") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Tùy chọn đọc sách", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cỡ chữ mặc định
                    Text("Cỡ chữ mặc định trong trình đọc:")
                    var fontSizeVal by remember { mutableStateOf(18f) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Slider(
                            value = fontSizeVal,
                            onValueChange = { fontSizeVal = it },
                            valueRange = 12f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${fontSizeVal.toInt()} sp", fontWeight = FontWeight.Bold)
                    }

                    Divider()

                    // Font chữ
                    Text("Font chữ ưu tiên:")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Serif", "Sans-Serif", "Monospace").forEach { f ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        android.widget.Toast.makeText(context, "Đã chọn font $f làm mặc định", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(f, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeDialog = null }) {
                    Text("Hoàn tất", color = Color.White)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0F0F0F)) // Dark background tương tự ảnh mẫu
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
                            colors = listOf(PtGoldGradientStart, PtGoldGradientEnd)
                        )
                    )
                    .border(3.dp, Color(0xFFE5A93B), CircleShape), // Viền vàng như ảnh mẫu
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.name.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
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
                    .background(Color(0xFFE5A93B))
                    .border(1.5.dp, Color(0xFF0F0F0F), CircleShape)
                    .clickable { isEditing = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color(0xFF0F0F0F),
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
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = viewModel.userEmail.ifBlank { userProfile.email.ifBlank { "anvo@gmail.com" } },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Gray
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
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(text = "Books", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "3,241",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(text = "Pages Read", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "47",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Text(text = "Highlights", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- READING STREAK CARD ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1713)) // Màu nâu tối ấm áp cho Streak card
                .border(1.dp, Color(0xFFE5A93B).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Reading Streak",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "14",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5A93B)
                    )
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("day streak", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                    Text("Last 14 days", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Custom Visual Indicator for Streak days
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(7) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (idx < 5) Color(0xFFE5A93B) // Ngày đã đạt streak
                                    else Color(0xFFE5A93B).copy(alpha = 0.2f) // Ngày chưa đạt
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SETTINGS SECTION TITLE ---
        Text(
            text = "Settings",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
            textAlign = TextAlign.Start
        )

        // --- 5 SETTINGS ITEMS LIST ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E1E)),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            val listItems = listOf(
                Triple("Notifications", Icons.Default.Notifications, "notifications"),
                Triple("Reading Goals", Icons.Default.TrendingUp, "goals"),
                Triple("Export Notes", Icons.Default.Edit, "notes"),
                Triple("Device Sync", Icons.Default.CloudSync, "sync"),
                Triple("Preferences", Icons.Default.Tune, "preferences")
            )

            listItems.forEach { (title, icon, actionKey) ->
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
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Kẻ đường ngăn cách nhẹ trừ mục cuối cùng
                if (actionKey != "preferences") {
                    Divider(color = Color(0xFF2C2C2C), thickness = 0.5.dp)
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
                .border(1.dp, Color(0xFFD32F2F).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                    tint = Color(0xFFEF5350)
                )
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFEF5350),
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


