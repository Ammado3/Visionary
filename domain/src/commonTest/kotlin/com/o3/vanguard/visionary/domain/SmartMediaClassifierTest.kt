package com.o3.vanguard.visionary.domain

import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.usecase.SmartMediaClassifier
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization tests for [SmartMediaClassifier].
 */
class SmartMediaClassifierTest {

    private val classifier = SmartMediaClassifier()

    @Test
    fun classifier_receiptKeywords_returnsReceipt() {
        val category = classifier.classify(text = "Total amount due: $50.00", isScreenshot = false)
        assertEquals(MediaCategory.RECEIPT, category)
    }

    @Test
    fun classifier_documentKeywords_returnsDocument() {
        val category = classifier.classify(text = "Monthly Bank Statement Invoice", isScreenshot = false)
        assertEquals(MediaCategory.DOCUMENT, category)
    }

    @Test
    fun classifier_screenshotFlagWithoutKeywords_returnsScreenshot() {
        val category = classifier.classify(text = "Hello world", isScreenshot = true)
        assertEquals(MediaCategory.SCREENSHOT, category)
    }

    @Test
    fun classifier_ordinaryPhotoWithoutKeywordsOrScreenshot_returnsPhoto() {
        val category = classifier.classify(text = "Sunset at the beach", isScreenshot = false)
        assertEquals(MediaCategory.PHOTO, category)
    }
}
