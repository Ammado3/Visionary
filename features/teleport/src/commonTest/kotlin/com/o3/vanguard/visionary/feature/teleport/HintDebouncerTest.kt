package com.o3.vanguard.visionary.feature.teleport

import com.o3.vanguard.visionary.feature.teleport.presentation.ui.portal.HintDebouncer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HintDebouncerTest {

    @Test
    fun debouncer_ignoresCandidateShorterThanIgnoreThreshold() {
        val debouncer = HintDebouncer(minDisplayMs = 400L, ignoreShorterThanMs = 250L)
        assertNull(debouncer.displayedHint)

        val (hint, updated) = debouncer.processHintUpdate(
            candidate = "Bring hand closer",
            nowMs = 1100L,
            lastChangeMs = 0L,
            candidateStartMs = 1000L,
        )

        assertFalse(updated)
        assertNull(hint)
    }

    @Test
    fun debouncer_acceptsCandidateWhenDurationMeetsThreshold() {
        val debouncer = HintDebouncer(minDisplayMs = 400L, ignoreShorterThanMs = 250L)

        val (hint, updated) = debouncer.processHintUpdate(
            candidate = "Bring hand closer",
            nowMs = 1300L,
            lastChangeMs = 0L,
            candidateStartMs = 1000L,
        )

        assertTrue(updated)
        assertEquals("Bring hand closer", hint)
        assertEquals("Bring hand closer", debouncer.displayedHint)
    }

    @Test
    fun debouncer_preventsFlickerByEnforcingMinDisplayTime() {
        val debouncer = HintDebouncer(minDisplayMs = 400L, ignoreShorterThanMs = 250L)

        debouncer.processHintUpdate("Bring hand closer", nowMs = 1300L, lastChangeMs = 0L, candidateStartMs = 1000L)
        assertEquals("Bring hand closer", debouncer.displayedHint)

        val (hint, updated) = debouncer.processHintUpdate(
            candidate = "Move hand back",
            nowMs = 1500L,
            lastChangeMs = 1300L,
            candidateStartMs = 1200L,
        )

        assertFalse(updated)
        assertEquals("Bring hand closer", hint)

        val (hintAfterMin, updatedAfterMin) = debouncer.processHintUpdate(
            candidate = "Move hand back",
            nowMs = 1800L,
            lastChangeMs = 1300L,
            candidateStartMs = 1200L,
        )

        assertTrue(updatedAfterMin)
        assertEquals("Move hand back", hintAfterMin)
    }
}
