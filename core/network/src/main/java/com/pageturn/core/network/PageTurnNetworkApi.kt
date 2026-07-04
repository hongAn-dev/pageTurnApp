package com.pageturn.core.network

import com.pageturn.core.model.Book
import com.pageturn.core.model.Chapter

interface PageTurnNetworkApi {
    suspend fun getRecentReads(): List<Book>
    suspend fun getBookDetails(bookId: String): Book
    suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter
}

class FakePageTurnNetworkApi : PageTurnNetworkApi {
    private val books = listOf(
        Book(
            id = "1",
            title = "Sherlock Holmes",
            author = "Arthur Conan Doyle",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/b/b9/Adventures_of_sherlock_holmes.jpg",
            progressPercent = 0.85f,
            totalPages = 350,
            currentPage = 42,
            description = "The Adventures of Sherlock Holmes is a collection of twelve short stories by Arthur Conan Doyle."
        ),
        Book(
            id = "2",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7a/The_Great_Gatsby_Cover_1925_Retouched.jpg",
            progressPercent = 0.50f,
            totalPages = 180,
            currentPage = 90,
            description = "The novel chronicles an era that Fitzgerald himself dubbed the 'Jazz Age'."
        ),
        Book(
            id = "3",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/1/17/PrideAndPrejudiceTitlePage.jpg",
            progressPercent = 0.95f,
            totalPages = 400,
            currentPage = 380,
            description = "It follows the turbulent relationship between Elizabeth Bennet and Fitzwilliam Darcy."
        ),
        Book(
            id = "4",
            title = "Moby Dick",
            author = "Herman Melville",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/3/3f/Moby-Dick_FE_title_page.jpg",
            progressPercent = 0.12f,
            totalPages = 600,
            currentPage = 72,
            description = "The sailor Ishmael's narrative of the obsessive quest of Ahab, captain of the whaling quest."
        ),
        Book(
            id = "5",
            title = "War and Peace",
            author = "Leo Tolstoy",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/a/af/Leo_Tolstoy_-_War_and_Peace_-_first_edition%2C_1869.jpg",
            progressPercent = 0.05f,
            totalPages = 1200,
            currentPage = 60,
            description = "Epic novel by Leo Tolstoy, chronicling the French invasion of Russia."
        )
    )

    override suspend fun getRecentReads(): List<Book> = books

    override suspend fun getBookDetails(bookId: String): Book {
        return books.first { it.id == bookId }
    }

    override suspend fun getChapter(bookId: String, chapterNumber: Int): Chapter {
        val book = getBookDetails(bookId)
        return Chapter(
            id = "${bookId}_$chapterNumber",
            bookId = bookId,
            title = "A Scandal in Bohemia",
            chapterNumber = chapterNumber,
            content = """
                To Sherlock Holmes she is always the woman. I have seldom heard him mention her under any other name. In his eyes she eclipses and predominates the whole of her sex. It was not that he felt any emotion akin to love for Irene Adler. All emotions, and that one particularly, were abhorrent to his cold, precise but admirably balanced mind. He was, I take it, the most perfect reasoning and observing machine that the world has seen, but as a lover he would have placed himself in a false position.

                He never spoke of the softer passions, save with a gibe and a sneer. They were admirable things for the observer—excellent for drawing the veil from men's motives and actions. But for the trained reasoner to admit such intrusions into his own delicate and finely adjusted temperament was to introduce a distracting factor which might throw a doubt upon all his mental results. Grit in a sensitive instrument, or a crack in one of his own high-power lenses, would not be more disturbing than a strong emotion in a nature such as his.
                
                And yet there was but one woman to him, and that woman was the late Irene Adler, of dubious and questionable memory. I had seen little of Holmes lately. My marriage had drifted us away from each other. My own complete happiness, and the home-centred interests which rise up around the man who first finds himself master of his own establishment, were sufficient to absorb all my attention.

                Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street, buried among his old books, and alternating from week to week between cocaine and ambition, the drowsiness of the drug, and the fierce energy of his own keen nature.
            """.trimIndent(),
            imageUrl = "https://example.com/images/baker_street.jpg"
        )
    }
}
