package com.pageturn.core.data.repository

import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal object EpubTextExtractor {
    fun readAllContent(epubFile: File): String {
        return try {
            ZipFile(epubFile).use { zipFile ->
                val readingPaths = zipFile.resolveReadingPaths()
                buildString {
                    readingPaths.forEach { path ->
                        val entry = zipFile.getEntry(path) ?: zipFile.getEntry(path.replace("\\", "/"))
                        if (entry != null) {
                            val htmlContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                            val stripped = stripHtml(htmlContent).trim()
                            if (stripped.isNotEmpty()) {
                                append(stripped).append("\n\n")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "Lỗi đọc sách ngoại tuyến: ${e.message}"
        }
    }

    private fun ZipFile.resolveReadingPaths(): List<String> {
        val opfPath = findOpfPath()
        if (opfPath.isNotEmpty()) {
            val paths = readSpinePaths(opfPath)
            if (paths.isNotEmpty()) return paths
        }

        return entries().asSequence()
            .filter { it.isHtmlEntry() }
            .map { it.name }
            .sorted()
            .toList()
    }

    private fun ZipFile.findOpfPath(): String {
        val containerEntry = getEntry("META-INF/container.xml")
        if (containerEntry != null) {
            val containerContent = getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val rootfileMatcher = ROOTFILE_PATTERN.matcher(containerContent)
            if (rootfileMatcher.find()) {
                val attrs = rootfileMatcher.group(1).orEmpty()
                val pathMatcher = FULL_PATH_PATTERN.matcher(attrs)
                if (pathMatcher.find()) {
                    return pathMatcher.group(1).orEmpty()
                }
            }
        }

        return entries().asSequence()
            .firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }
            ?.name
            .orEmpty()
    }

    private fun ZipFile.readSpinePaths(opfPath: String): List<String> {
        val opfEntry = getEntry(opfPath) ?: return emptyList()
        val opfContent = getInputStream(opfEntry).bufferedReader().use { it.readText() }

        val manifestItems = mutableMapOf<String, String>()
        val itemMatcher = ITEM_PATTERN.matcher(opfContent)
        while (itemMatcher.find()) {
            val attrs = itemMatcher.group(1).orEmpty()
            val idMatcher = ID_PATTERN.matcher(attrs)
            val hrefMatcher = HREF_PATTERN.matcher(attrs)
            if (idMatcher.find() && hrefMatcher.find()) {
                manifestItems[idMatcher.group(1).orEmpty()] = hrefMatcher.group(1).orEmpty()
            }
        }

        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        val spinePaths = mutableListOf<String>()
        val spineMatcher = ITEMREF_PATTERN.matcher(opfContent)
        while (spineMatcher.find()) {
            val attrs = spineMatcher.group(1).orEmpty()
            val idrefMatcher = IDREF_PATTERN.matcher(attrs)
            if (idrefMatcher.find()) {
                val href = manifestItems[idrefMatcher.group(1).orEmpty()] ?: continue
                val decodedHref = URLDecoder.decode(href, "UTF-8")
                spinePaths += if (decodedHref.startsWith("/")) decodedHref.drop(1) else "${opfDir}${decodedHref}"
            }
        }
        return spinePaths
    }

    private fun ZipEntry.isHtmlEntry(): Boolean {
        val name = name.lowercase()
        return name.endsWith(".html") || name.endsWith(".xhtml") || name.endsWith(".htm")
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

    private val ROOTFILE_PATTERN = Regex(
        "<(?:[a-zA-Z0-9_-]+:)?rootfile\\s+([^>]*?)>",
        RegexOption.IGNORE_CASE
    ).toPattern()
    private val FULL_PATH_PATTERN = Regex("full-path=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).toPattern()
    private val ITEM_PATTERN = Regex("<(?:[a-zA-Z0-9_-]+:)?item\\s+([^>]*?)>", RegexOption.IGNORE_CASE).toPattern()
    private val ITEMREF_PATTERN = Regex("<(?:[a-zA-Z0-9_-]+:)?itemref\\s+([^>]*?)>", RegexOption.IGNORE_CASE).toPattern()
    private val ID_PATTERN = Regex("id=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).toPattern()
    private val IDREF_PATTERN = Regex("idref=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).toPattern()
    private val HREF_PATTERN = Regex("href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).toPattern()
}
