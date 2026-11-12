package com.o3.vanguard.visionary.core.common

import com.o3.vanguard.visionary.core.testkit.assert.assertErr
import com.o3.vanguard.visionary.core.testkit.assert.assertOk
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonPrimitivesTest {

    @Test
    fun floatArrayToByteArray_roundTrip_preservesValues() {
        val original = floatArrayOf(0.0f, 1.234f, -56.789f, Float.MAX_VALUE, Float.MIN_VALUE)
        val byteArray = original.toByteArray()
        val restored = byteArray.toFloatArray()

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            assertEquals(original[i], restored[i])
        }
    }

    @Test
    fun appResult_map_transformsOkValueAndPreservesError() {
        val okResult: AppResult<Int> = AppResult.Ok(21)
        val mappedOk = okResult.map { it * 2 }
        assertEquals(42, mappedOk.assertOk())

        val errResult: AppResult<Int> = AppResult.Err(AppError.NotFound)
        val mappedErr = errResult.map { it * 2 }
        assertEquals(AppError.NotFound, mappedErr.assertErr())
    }
}
