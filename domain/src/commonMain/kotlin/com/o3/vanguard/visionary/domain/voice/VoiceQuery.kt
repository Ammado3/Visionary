package com.o3.vanguard.visionary.domain.voice

import com.o3.vanguard.visionary.domain.model.MediaCategory

data class VoiceQuery(
    val transcript: String,
    val keywords: List<String>,
    val categoryHints: Set<MediaCategory>,
) {
    val searchText: String get() = keywords.joinToString(" ")
}

object VoiceQueryParser {
    private val RECEIPT_WORDS = setOf("receipt", "receipts", "bill", "bills", "invoice", "invoices")
    private val SCREENSHOT_WORDS = setOf("screenshot", "screenshots")
    private val DOCUMENT_WORDS = setOf("document", "documents", "doc", "docs")
    private val SCAN_WORDS = setOf("scan", "scans", "scanned")
    private val VIDEO_WORDS = setOf("video", "videos")

    private val CATEGORY_MAP: Map<String, MediaCategory> = buildMap {
        RECEIPT_WORDS.forEach { put(it, MediaCategory.RECEIPT) }
        SCREENSHOT_WORDS.forEach { put(it, MediaCategory.SCREENSHOT) }
        DOCUMENT_WORDS.forEach { put(it, MediaCategory.DOCUMENT) }
        SCAN_WORDS.forEach { put(it, MediaCategory.SCAN) }
        VIDEO_WORDS.forEach { put(it, MediaCategory.VIDEO) }
    }

    private val FILLER_WORDS: Set<String> = setOf(
        "find", "show", "search", "get", "open", "look", "looking", "give", "me", "my", "i",
        "want", "need", "the", "a", "an", "for", "from", "of", "with", "about", "please",
        "can", "could", "you", "that", "this", "those", "some", "any", "all", "hey",
        "visionary", "photo", "photos", "picture", "pictures", "pic", "pics", "image", "images",
    )

    private val TRAILING_POSSESSIVE_REGEX = Regex("'s\\b")

    fun parse(transcript: String): VoiceQuery {
        val cleanedText = transcript
            .lowercase()
            .replace('’', '\'')
            .replace(TRAILING_POSSESSIVE_REGEX, "")
            .map { char -> if (char.isLetterOrDigit()) char else ' ' }
            .joinToString("")
            .replace(Regex("screen\\s+shot"), "screenshot")

        val tokens = cleanedText.split(Regex("\\s+")).filter { it.isNotEmpty() }

        val categoryHints = mutableSetOf<MediaCategory>()
        val keywords = mutableListOf<String>()

        for (token in tokens) {
            val category = CATEGORY_MAP[token]
            if (category != null) {
                categoryHints.add(category)
            } else if (token !in FILLER_WORDS) {
                val isSingleLetter = token.length == 1 && token[0].isLetter()
                if (!isSingleLetter && token !in keywords) {
                    keywords.add(token)
                }
            }
        }

        return VoiceQuery(
            transcript = transcript,
            keywords = keywords,
            categoryHints = categoryHints,
        )
    }
}
