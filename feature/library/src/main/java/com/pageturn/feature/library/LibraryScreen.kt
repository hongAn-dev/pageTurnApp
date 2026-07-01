package com.pageturn.feature.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.pageturn.core.designsystem.theme.*
import com.pageturn.core.model.Book
import androidx.compose.ui.graphics.Path

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

    // File Picker states and Launcher
    var showImportDialog by remember { mutableStateOf(false) }
    var importedBookTitle by remember { mutableStateOf("") }
    var importedBookAuthor by remember { mutableStateOf("") }
    var importedBookDesc by remember { mutableStateOf("") }
    var importedBookContent by remember { mutableStateOf("") }
 
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
                    } else {
                        importedBookContent = "Không thể phân tích file PDF."
                        importedBookDesc = "Tài liệu bị lỗi."
                        importedBookAuthor = "Tài liệu PDF"
                    }
                } else if (isEpub) {
                    val epubText = withContext(Dispatchers.IO) {
                        parseEpubText(context, it)
                    }
                    importedBookContent = if (epubText.isNotBlank()) epubText else "Ebook EPUB trống."
                    importedBookDesc = "Tài liệu Ebook dạng EPUB nhập từ thiết bị."
                    importedBookAuthor = "Tác giả EPUB"
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
                        viewModel.addLocalBook(importedBookTitle, importedBookAuthor, importedBookContent)
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
                                showProfile = true
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
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "PageTurn",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        // Profile Icon
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showProfile = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.name.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
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
                if (showProfile) {
                    ProfileScreenContent(
                        viewModel = viewModel,
                        onBack = { showProfile = false },
                        onSignOut = {
                            showProfile = false
                            onSignOut()
                        }
                    )
                } else {
                    Crossfade(targetState = selectedTab, label = "tabTransition") { tab ->
                        when (tab) {
                            0 -> {
                                // TAB 0: LIBRARY MAIN VIEW
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    // Search Bar
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Tìm kiếm sách trong thư viện...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    // Filter Chips for All/Favorites
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        FilterChip(
                                            selected = currentFilter == "all",
                                            onClick = { currentFilter = "all" },
                                            label = { Text("Tất cả sách") }
                                        )
                                        FilterChip(
                                            selected = currentFilter == "favorite",
                                            onClick = { currentFilter = "favorite" },
                                            label = { Text("Yêu thích") }
                                        )
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
                                            text = if (currentFilter == "favorite") "Danh sách yêu thích" else "Sách đọc gần đây",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = "48 CUỐN SÁCH",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
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
                                                            onBookClick = onBookClick
                                                        )
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
                                SettingsScreenContent(viewModel)
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
}

@Composable
fun BookItem(
    book: Book, 
    onBookClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBookClick(book.id) }
    ) {
        // Custom Premium Cover using Canvas to draw
        Box(
            modifier = Modifier
                .aspectRatio(0.65f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .shadow(2.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            BookCoverCanvas(title = book.title)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
            maxLines = 1
        )
        // Author
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = book.progressPercent,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun BookCoverCanvas(title: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background Color depending on Book title
        val coverBgColor = when {
            title.contains("Sherlock", ignoreCase = true) -> Color(0xFF0F2537)
            title.contains("Gatsby", ignoreCase = true) -> Color(0xFF132F27)
            title.contains("Prejudice", ignoreCase = true) -> Color(0xFFE5DEC9)
            title.contains("Moby", ignoreCase = true) -> Color(0xFF1B2F38)
            else -> Color(0xFF5C3A21) // Leather brown for local/imported books
        }

        val borderGold = Color(0xFFD4AF37)

        // Draw Cover Background
        drawRect(color = coverBgColor)

        // Draw Gold Frame Border
        val inset = 12.dp.toPx()
        drawRect(
            color = borderGold,
            topLeft = Offset(inset, inset),
            size = Size(width - inset * 2, height - inset * 2),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw Internal Cover Details
        if (title.contains("Sherlock", ignoreCase = true)) {
            // Hat/Circle silhouette
            drawCircle(
                color = borderGold,
                radius = 28.dp.toPx(),
                center = Offset(width / 2, height / 2 - 20.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        } else if (title.contains("Gatsby", ignoreCase = true)) {
            // Art deco lines
            val midY = height / 2 - 20.dp.toPx()
            drawLine(borderGold, Offset(inset * 2, midY), Offset(width - inset * 2, midY), strokeWidth = 1.dp.toPx())
            drawLine(borderGold, Offset(width / 2, inset * 2), Offset(width / 2, height - inset * 2), strokeWidth = 1.dp.toPx())
        } else if (title.contains("Moby", ignoreCase = true)) {
            // Wave or moon
            drawCircle(
                color = Color(0xFFFFF9E6),
                radius = 18.dp.toPx(),
                center = Offset(width / 2, height / 2 - 25.dp.toPx())
            )
        } else {
            // Gold vintage diamond/shield emblem in the center for local books
            val centerX = width / 2
            val centerY = height / 2
            val badgeSize = 24.dp.toPx()
            val path = Path().apply {
                moveTo(centerX, centerY - badgeSize)
                lineTo(centerX + badgeSize, centerY)
                lineTo(centerX, centerY + badgeSize)
                lineTo(centerX - badgeSize, centerY)
                close()
            }
            drawPath(path, color = borderGold, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(color = borderGold, radius = 6.dp.toPx(), center = Offset(centerX, centerY))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreenContent() {
    var selectedCategory by remember { mutableStateOf("Tất cả") }

    val popularBooks = remember(selectedCategory) {
        when (selectedCategory) {
            "Trinh thám" -> listOf(
                "THE ADVENTURES OF SHERLOCK HOLMES" to "Arthur Conan Doyle",
                "Phía Sau Nghi Can X" to "Higashino Keigo",
                "Sự Im Lặng Của Bầy Cừu" to "Thomas Harris"
            )
            "Cổ điển" -> listOf(
                "THE GREAT GATSBY" to "F. Scott Fitzgerald",
                "PRIDE AND PREJUDICE" to "Jane Austen",
                "War and Peace" to "Leo Tolstoy"
            )
            "Lãng mạn" -> listOf(
                "Rừng Na Uy" to "Haruki Murakami",
                "Kiêu Hãnh Và Định Kiến" to "Jane Austen",
                "Romeo và Juliet" to "William Shakespeare"
            )
            "Kinh tế" -> listOf(
                "Nhà Giả Kim" to "Paulo Coelho",
                "Đắc Nhân Tâm" to "Dale Carnegie",
                "Cha Giàu Cha Nghèo" to "Robert Kiyosaki"
            )
            "Lịch sử" -> listOf(
                "Sapiens: Lược Sử Loài Người" to "Yuval Noah Harari",
                "Lịch Sử Văn Minh Thế Giới" to "Will Durant",
                "Đại Việt Sử Ký Toàn Thư" to "Ngô Sĩ Liên"
            )
            else -> listOf(
                "THE ADVENTURES OF SHERLOCK HOLMES" to "Arthur Conan Doyle",
                "THE GREAT GATSBY" to "F. Scott Fitzgerald",
                "PRIDE AND PREJUDICE" to "Jane Austen"
            )
        }
    }

    val recommendations = remember(selectedCategory) {
        when (selectedCategory) {
            "Trinh thám" -> listOf(
                "Đề thi đẫm máu" to "Lôi Mễ",
                "Mười người da đen nhỏ" to "Agatha Christie"
            )
            "Cổ điển" -> listOf(
                "Moby Dick" to "Herman Melville",
                "Những Người Khốn Khổ" to "Victor Hugo"
            )
            "Lãng mạn" -> listOf(
                "Trà hoa nữ" to "Alexandre Dumas con",
                "Cuốn theo chiều gió" to "Margaret Mitchell"
            )
            "Kinh tế" -> listOf(
                "Chiến Tranh Tiền Tệ" to "Song Hongbing",
                "Kinh tế học hài hước" to "Steven Levitt"
            )
            "Lịch sử" -> listOf(
                "Lược sử thời gian" to "Stephen Hawking",
                "Súng, vi trùng và thép" to "Jared Diamond"
            )
            else -> listOf(
                "Pride and Prejudice" to "Jane Austen",
                "Moby Dick" to "Herman Melville",
                "War and Peace" to "Leo Tolstoy"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Trending Header
        item {
            Text(
                text = "Khám phá Sách mới",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
        }

        // Horizontal Category Chips
        item {
            val categories = listOf("Tất cả", "Trinh thám", "Cổ điển", "Lãng mạn", "Kinh tế", "Lịch sử")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }

        // Carousel of popular books
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Đọc nhiều nhất tuần",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(popularBooks) { (title, author) ->
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .height(140.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    BookCoverCanvas(title = title)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 1, fontWeight = FontWeight.Bold)
                                Text(text = author, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Recommended Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Đề xuất cho bạn",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )

                // List style recommendations
                recommendations.forEach { (title, author) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {},
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(50.dp, 70.dp).clip(RoundedCornerShape(4.dp))) {
                                BookCoverCanvas(title = title)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
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
                                        BookCoverCanvas(title = book.title)
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

@Composable
fun SettingsScreenContent(viewModel: LibraryViewModel) {
    val settings by viewModel.userSettings.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Cài đặt Ứng dụng",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        )

        // --- Cloud Sync Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "☁️ Đồng bộ Cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Auto sync toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động đồng bộ")
                        Text(
                            "Đồng bộ tiến độ & highlights mỗi 6 giờ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoSync,
                        onCheckedChange = { viewModel.setAutoSync(it) }
                    )
                }

                // Sync status indicator
                when (val s = syncState) {
                    is LibraryViewModel.SyncState.Syncing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                "Đang đồng bộ...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is LibraryViewModel.SyncState.Done -> {
                        Text(
                            "✅ ${s.summary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is LibraryViewModel.SyncState.Error -> {
                        Text(
                            "❌ Lỗi: ${s.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }

                // Manual sync buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.weight(1f),
                        enabled = syncState !is LibraryViewModel.SyncState.Syncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đẩy lên", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { viewModel.pullFromCloud() },
                        modifier = Modifier.weight(1f),
                        enabled = syncState !is LibraryViewModel.SyncState.Syncing
                    ) {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kéo về", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Notification Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "🔔 Thông báo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nhắc nhở đọc sách")
                        Text(
                            if (settings.dailyNotify) "🟢 Đang bật — thông báo sẽ đến mỗi ngày"
                            else "Nhắc nhở đọc sách mỗi ngày",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.dailyNotify)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.dailyNotify,
                        onCheckedChange = {
                            viewModel.setDailyNotify(it)
                            android.widget.Toast.makeText(
                                context,
                                if (it) "✅ Đã bật! Thông báo sẽ đến sau ~10 giây để test" else "Đã tắt nhắc nhở",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }

        // --- Cache Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Bộ nhớ cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dung lượng tải offline")
                        Text("Đang sử dụng: $cacheSize", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            viewModel.clearCache()
                            android.widget.Toast.makeText(
                                context,
                                "Đã xóa sạch bộ nhớ cache thành công!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Text("Xóa Cache", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    
    var nameInput by remember(userProfile) { mutableStateOf(userProfile.name) }
    var emailInput by remember(userProfile) { mutableStateOf(userProfile.email) }
    var bioInput by remember(userProfile) { mutableStateOf(userProfile.bio) }
    
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopAppBar for Profile
        TopAppBar(
            title = { Text("Hồ sơ cá nhân", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
            },
            actions = {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            viewModel.updateUserProfile(nameInput, emailInput, bioInput)
                            isEditing = false
                        }
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    TextButton(onClick = { isEditing = true }) {
                        Text("Sửa", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nameInput.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Information fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
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
                        label = { Text("Giới thiệu bản thân") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileInfoItem(label = "Họ và Tên", value = userProfile.name)
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileInfoItem(label = "Email Tài khoản", value = viewModel.userEmail.ifBlank { userProfile.email.ifBlank { "Chưa cập nhật" } })
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileInfoItem(label = "Giới thiệu", value = userProfile.bio.ifBlank { "Chưa có giới thiệu" })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = {
                    viewModel.signOut()
                    onSignOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Đăng xuất")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất tài khoản", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = PtTextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = PtTextMain, fontWeight = FontWeight.Medium)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                items(highlights) { highlight ->
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
                                val bookLabel = bookTitles[highlight.bookId] ?: when (highlight.bookId) {
                                    "1" -> "Sherlock Holmes"
                                    "2" -> "The Great Gatsby"
                                    "3" -> "Pride and Prejudice"
                                    "4" -> "Moby Dick"
                                    else -> "Tài liệu Local"
                                }
                                Text(
                                    text = "$bookLabel - Chương ${highlight.chapterNumber}, Đoạn ${highlight.startOffset + 1}",
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
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xóa ghi nhớ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                paragraphs.add(built)
                currentParagraph.clear()
            }
        } else {
            // Append to current paragraph with space
            if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
            currentParagraph.append(trimmed)
        }
    }
    // Flush final paragraph
    val built = currentParagraph.toString().trim()
    if (built.isNotEmpty()) {
        paragraphs.add(built)
    }

    // Join paragraphs with double newline for reader to split on
    return paragraphs.joinToString("\n\n")
}

