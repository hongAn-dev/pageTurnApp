package com.pageturn.core.data.repository

import android.content.Context
import com.pageturn.core.common.network.Dispatcher
import com.pageturn.core.common.network.PtDispatchers
import com.pageturn.core.database.dao.BookDao
import com.pageturn.core.database.dao.BookmarkDao
import com.pageturn.core.database.dao.HighlightDao
import com.pageturn.core.database.model.BookEntity
import com.pageturn.core.database.model.BookmarkEntity
import com.pageturn.core.database.model.HighlightEntity
import com.pageturn.core.database.model.asEntity
import com.pageturn.core.database.model.asExternalModel
import com.pageturn.core.domain.repository.BookRepository
import com.pageturn.core.model.Book
import com.pageturn.core.model.Bookmark
import com.pageturn.core.model.Chapter
import com.pageturn.core.model.Highlight
import com.pageturn.core.network.PageTurnNetworkApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val bookmarkDao: BookmarkDao,
    private val highlightDao: HighlightDao,
    private val networkApi: PageTurnNetworkApi,
    @Dispatcher(PtDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context
) : BookRepository {

    override fun getRecentReads(): Flow<List<Book>> {
        return bookDao.getBooks()
            .map { entities -> entities.map(BookEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override fun getBookDetails(bookId: String): Flow<Book?> {
        return bookDao.getBook(bookId)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter = withContext(ioDispatcher) {
        val localBookDir = java.io.File(context.filesDir, bookId)
        val epubFile = java.io.File(localBookDir, "book.epub")
        val pdfFile = java.io.File(localBookDir, "book.pdf")

        // Check if PDF exists
        if (pdfFile.exists()) {
            val localBook = bookDao.getBookSnapshot(bookId)
            return@withContext Chapter(
                id = "${bookId}_pdf_$chapterNumber",
                bookId = bookId,
                title = localBook?.title ?: "Sách PDF",
                chapterNumber = chapterNumber,
                content = "[pdf_file: ${pdfFile.absolutePath}]",
                imageUrl = ""
            )
        }

        // Check if epub exists AND is a valid ZIP (magic bytes PK\x03\x04)
        val isValidEpub = epubFile.exists() && epubFile.length() > 4 && run {
            val header = ByteArray(4)
            epubFile.inputStream().use { it.read(header) }
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
        }

        if (epubFile.exists() && !isValidEpub) {
            // File hỏng (server trả về HTML/JSON) – xóa để tải lại sau
            epubFile.delete()
        }

        if (isValidEpub) {
            val localBook = bookDao.getBookSnapshot(bookId)
            val contentFile = java.io.File(localBookDir, "book_content.txt")
            val chapterContent = if (contentFile.exists()) {
                contentFile.readText()
            } else {
                val fullText = readAllContentFromLocalEpub(epubFile)
                try {
                    contentFile.writeText(fullText)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                fullText
            }
            return@withContext Chapter(
                id = "${bookId}_1",
                bookId = bookId,
                title = localBook?.title ?: "Sách Local",
                chapterNumber = 1,
                content = chapterContent,
                imageUrl = ""
            )
        } else if (bookId.startsWith("local_")) {
            val localBook = bookDao.getBookSnapshot(bookId)
            Chapter(
                id = "${bookId}_$chapterNumber",
                bookId = bookId,
                title = localBook?.title ?: "Sách Local",
                chapterNumber = chapterNumber,
                content = localBook?.description ?: "Không có nội dung.",
                imageUrl = ""
            )
        } else {
            networkApi.getChapter(bookId, chapterNumber)
        }
    }

    private fun readChapterFromLocalEpub(epubFile: java.io.File, chapterNumber: Int): String {
        try {
            android.util.Log.d("PageTurnEPUB", "Opening local EPUB: ${epubFile.absolutePath}, size: ${epubFile.length()} bytes")
            val zipFile = java.util.zip.ZipFile(epubFile)
            
            var opfPath = ""
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            android.util.Log.d("PageTurnEPUB", "container.xml entry found: ${containerEntry != null}")
            if (containerEntry != null) {
                val containerContent = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                val matcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?rootfile\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(containerContent)
                if (matcher.find()) {
                    val attrs = matcher.group(1) ?: ""
                    val pathMatcher = java.util.regex.Pattern.compile("full-path=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                    if (pathMatcher.find()) {
                        opfPath = pathMatcher.group(1) ?: ""
                    }
                }
            }
            android.util.Log.d("PageTurnEPUB", "opfPath from container.xml: '$opfPath'")
            
            if (opfPath.isEmpty()) {
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".opf", ignoreCase = true)) {
                        opfPath = entry.name
                        break
                    }
                }
                android.util.Log.d("PageTurnEPUB", "Fallback opfPath scan found: '$opfPath'")
            }
            
            val readingPaths = mutableListOf<String>()
            if (opfPath.isNotEmpty()) {
                val opfEntry = zipFile.getEntry(opfPath)
                android.util.Log.d("PageTurnEPUB", "OPF entry found: ${opfEntry != null}")
                if (opfEntry != null) {
                    val opfContent = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                    
                    val manifestItems = mutableMapOf<String, String>()
                    val itemMatcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?item\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                    while (itemMatcher.find()) {
                        val attrs = itemMatcher.group(1) ?: continue
                        val idMatcher = java.util.regex.Pattern.compile("id=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        val hrefMatcher = java.util.regex.Pattern.compile("href=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        if (idMatcher.find() && hrefMatcher.find()) {
                            val idVal = idMatcher.group(1)
                            val hrefVal = hrefMatcher.group(1)
                            if (idVal != null && hrefVal != null) {
                                manifestItems[idVal] = hrefVal
                            }
                        }
                    }
                    android.util.Log.d("PageTurnEPUB", "manifestItems parsed count: ${manifestItems.size}")
                    
                    val spineIds = mutableListOf<String>()
                    val spineMatcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?itemref\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                    while (spineMatcher.find()) {
                        val attrs = spineMatcher.group(1) ?: continue
                        val idrefMatcher = java.util.regex.Pattern.compile("idref=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        if (idrefMatcher.find()) {
                            val idrefVal = idrefMatcher.group(1)
                            if (idrefVal != null) {
                                spineIds.add(idrefVal)
                            }
                        }
                    }
                    android.util.Log.d("PageTurnEPUB", "spineIds parsed count: ${spineIds.size}")
                    
                    val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                    spineIds.forEach { id ->
                        manifestItems[id]?.let { href ->
                            val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                            val finalPath = if (decodedHref.startsWith("/")) decodedHref.drop(1) else "$opfDir$decodedHref"
                            readingPaths.add(finalPath)
                        }
                    }
                }
            }
            android.util.Log.d("PageTurnEPUB", "readingPaths size: ${readingPaths.size}")
            
            if (readingPaths.isEmpty()) {
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
                htmlEntries.forEach { readingPaths.add(it.name) }
                android.util.Log.d("PageTurnEPUB", "Fallback manual html entries found size: ${readingPaths.size}")
            }
            
            val targetIndex = chapterNumber - 1
            android.util.Log.d("PageTurnEPUB", "targetIndex: $targetIndex, chapterNumber: $chapterNumber")
            if (targetIndex in readingPaths.indices) {
                val path = readingPaths[targetIndex]
                val entry = zipFile.getEntry(path) ?: zipFile.getEntry(path.replace("\\", "/"))
                android.util.Log.d("PageTurnEPUB", "Loading path: '$path', entry found: ${entry != null}")
                if (entry != null) {
                    val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    zipFile.close()
                    val stripped = stripHtml(htmlContent)
                    android.util.Log.d("PageTurnEPUB", "htmlContent length: ${htmlContent.length}, stripped length: ${stripped.length}")
                    return if (stripped.trim().isEmpty()) {
                        "Trang giới thiệu / Chương hình ảnh"
                    } else {
                        stripped
                    }
                }
            }
            
            if (readingPaths.isNotEmpty()) {
                zipFile.close()
                throw NoSuchElementException("Đã đến chương cuối cùng của sách.")
            }
            
            // Fallback to concatenating all text if index out of bounds and readingPaths is empty
            val fallbackBuilder = java.lang.StringBuilder()
            for (path in readingPaths) {
                val entry = zipFile.getEntry(path) ?: zipFile.getEntry(path.replace("\\", "/"))
                if (entry != null) {
                    val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    fallbackBuilder.append(stripHtml(htmlContent)).append("\n\n")
                }
            }
            zipFile.close()
            val result = fallbackBuilder.toString()
            android.util.Log.d("PageTurnEPUB", "Fallback result length: ${result.length}")
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return "Lỗi đọc sách ngoại tuyến: ${e.message}"
        }
    }

    private fun readAllContentFromLocalEpub(epubFile: java.io.File): String {
        try {
            android.util.Log.d("PageTurnEPUB", "Extracting full EPUB content: ${epubFile.absolutePath}")
            val zipFile = java.util.zip.ZipFile(epubFile)
            
            var opfPath = ""
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                val containerContent = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                val matcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?rootfile\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(containerContent)
                if (matcher.find()) {
                    val attrs = matcher.group(1) ?: ""
                    val pathMatcher = java.util.regex.Pattern.compile("full-path=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                    if (pathMatcher.find()) {
                        opfPath = pathMatcher.group(1) ?: ""
                    }
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
            
            val readingPaths = mutableListOf<String>()
            if (opfPath.isNotEmpty()) {
                val opfEntry = zipFile.getEntry(opfPath)
                if (opfEntry != null) {
                    val opfContent = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                    
                    val manifestItems = mutableMapOf<String, String>()
                    val itemMatcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?item\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                    while (itemMatcher.find()) {
                        val attrs = itemMatcher.group(1) ?: continue
                        val idMatcher = java.util.regex.Pattern.compile("id=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        val hrefMatcher = java.util.regex.Pattern.compile("href=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        if (idMatcher.find() && hrefMatcher.find()) {
                            val idVal = idMatcher.group(1)
                            val hrefVal = hrefMatcher.group(1)
                            if (idVal != null && hrefVal != null) {
                                manifestItems[idVal] = hrefVal
                            }
                        }
                    }
                    
                    val spineIds = mutableListOf<String>()
                    val spineMatcher = java.util.regex.Pattern.compile("<(?:[a-zA-Z0-9_-]+:)?itemref\\s+([^>]*?)>", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(opfContent)
                    while (spineMatcher.find()) {
                        val attrs = spineMatcher.group(1) ?: continue
                        val idrefMatcher = java.util.regex.Pattern.compile("idref=[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(attrs)
                        if (idrefMatcher.find()) {
                            val idrefVal = idrefMatcher.group(1)
                            if (idrefVal != null) {
                                spineIds.add(idrefVal)
                            }
                        }
                    }
                    
                    val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
                    spineIds.forEach { id ->
                        manifestItems[id]?.let { href ->
                            val decodedHref = java.net.URLDecoder.decode(href, "UTF-8")
                            val finalPath = if (decodedHref.startsWith("/")) decodedHref.drop(1) else "$opfDir$decodedHref"
                            readingPaths.add(finalPath)
                        }
                    }
                }
            }
            
            if (readingPaths.isEmpty()) {
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
                htmlEntries.forEach { readingPaths.add(it.name) }
            }
            
            val fullBuilder = java.lang.StringBuilder()
            for (path in readingPaths) {
                val entry = zipFile.getEntry(path) ?: zipFile.getEntry(path.replace("\\", "/"))
                if (entry != null) {
                    val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    val stripped = stripHtml(htmlContent).trim()
                    if (stripped.isNotEmpty()) {
                        fullBuilder.append(stripped).append("\n\n")
                    }
                }
            }
            zipFile.close()
            return fullBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "Lỗi đọc sách ngoại tuyến: ${e.message}"
        }
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<head>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    override suspend fun updateReadingProgress(bookId: String, page: Int, progressPercent: Float) = withContext(ioDispatcher) {
        bookDao.updateBookProgress(bookId, page, progressPercent)
    }

    override suspend fun sync() = withContext(ioDispatcher) {
        val remoteBooks = networkApi.getRecentReads()
        bookDao.insertOrReplaceBooks(remoteBooks.map(Book::asEntity))
    }

    override fun getBookmarks(bookId: String): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId)
            .map { entities -> entities.map(BookmarkEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override suspend fun addBookmark(bookId: String, chapterNumber: Int, pageNumber: Int) = withContext(ioDispatcher) {
        val id = "${bookId}_${chapterNumber}_${pageNumber}"
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                pageNumber = pageNumber,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun removeBookmark(bookmarkId: String) = withContext(ioDispatcher) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    override fun isBookmarked(bookmarkId: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(bookmarkId)
            .flowOn(ioDispatcher)
    }

    override fun getHighlights(bookId: String, chapterNumber: Int): Flow<List<Highlight>> {
        return highlightDao.getHighlightsForChapter(bookId, chapterNumber)
            .map { entities -> entities.map(HighlightEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override fun getAllHighlights(): Flow<List<Highlight>> {
        return highlightDao.getAllHighlights()
            .map { entities -> entities.map(HighlightEntity::asExternalModel) }
            .flowOn(ioDispatcher)
    }

    override suspend fun addHighlight(
        bookId: String,
        chapterNumber: Int,
        startOffset: Int,
        endOffset: Int,
        colorHex: String,
        noteText: String?,
        selectedText: String?
    ) = withContext(ioDispatcher) {
        // Use a unique ID with timestamp so multiple highlights in same paragraph do not overwrite each other
        val id = "${bookId}_${chapterNumber}_${startOffset}_${endOffset}_${System.currentTimeMillis()}"
        highlightDao.insertHighlight(
            HighlightEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                startOffset = startOffset,
                endOffset = endOffset,
                colorHex = colorHex,
                noteText = noteText,
                timestamp = System.currentTimeMillis(),
                selectedText = selectedText
            )
        )
    }

    override suspend fun removeHighlight(highlightId: String) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightById(highlightId)
    }

    override suspend fun removeHighlightsByParagraph(bookId: String, chapterNumber: Int, startOffset: Int) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightsByParagraph(bookId, chapterNumber, startOffset)
    }

    override suspend fun removeHighlightByText(bookId: String, chapterNumber: Int, startOffset: Int, selectedText: String) = withContext(ioDispatcher) {
        highlightDao.deleteHighlightByText(bookId, chapterNumber, startOffset, selectedText)
    }

    override suspend fun addLocalBook(book: Book) = withContext(ioDispatcher) {
        bookDao.insertOrReplaceBooks(listOf(book.asEntity()))
    }

    override fun getBookmarkedBookIds(): Flow<List<String>> {
        return bookmarkDao.getBookmarkedBookIds()
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteLocalBook(bookId: String) = withContext(ioDispatcher) {
        bookDao.deleteBookById(bookId)
        bookmarkDao.deleteBookmarksByBookId(bookId)
        highlightDao.deleteHighlightsByBookId(bookId)
    }

    // --- Cloud sync helpers ---

    override suspend fun getAllBooksSnapshot(): List<Book> = withContext(ioDispatcher) {
        bookDao.getBooksSnapshot().map(BookEntity::asExternalModel)
    }

    override suspend fun getAllHighlightsSnapshot(): List<Highlight> = withContext(ioDispatcher) {
        highlightDao.getAllHighlightsSnapshot().map(HighlightEntity::asExternalModel)
    }

    override suspend fun updateProgressIfNewer(
        bookId: String, cloudPage: Int, cloudTotal: Int, cloudProgress: Float
    ) = withContext(ioDispatcher) {
        val local = bookDao.getBookSnapshot(bookId) ?: return@withContext
        if (cloudPage > local.currentPage) {
            bookDao.updateBookProgress(bookId, cloudPage, cloudProgress)
        }
    }

    override suspend fun upsertHighlight(
        id: String, bookId: String, chapterNumber: Int, startOffset: Int,
        colorHex: String, selectedText: String, noteText: String
    ) = withContext(ioDispatcher) {
        val existing = highlightDao.getHighlightById(id)
        highlightDao.insertHighlight(
            HighlightEntity(
                id = id,
                bookId = bookId,
                chapterNumber = chapterNumber,
                startOffset = startOffset,
                endOffset = existing?.endOffset ?: startOffset,
                colorHex = colorHex,
                selectedText = selectedText,
                noteText = noteText,
                timestamp = existing?.timestamp ?: System.currentTimeMillis()
            )
        )
    }

    override suspend fun downloadBook(bookId: String): ByteArray = withContext(ioDispatcher) {
        networkApi.downloadBook(bookId)
    }

    override suspend fun deleteCloudBook(bookId: String) = withContext(ioDispatcher) {
        networkApi.deleteCloudBook(bookId)
        bookDao.deleteBookById(bookId)
        bookmarkDao.deleteBookmarksByBookId(bookId)
        highlightDao.deleteHighlightsByBookId(bookId)
    }

    override suspend fun clearAllLocalData() = withContext(ioDispatcher) {
        bookDao.deleteAllBooks()
    }
}

