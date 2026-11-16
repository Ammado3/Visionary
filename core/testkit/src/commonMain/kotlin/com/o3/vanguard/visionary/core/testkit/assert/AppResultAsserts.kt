package com.o3.vanguard.visionary.core.testkit.assert

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import kotlin.test.fail

fun <T> AppResult<T>.assertOk(): T = when (this) {
    is AppResult.Ok -> value
    is AppResult.Err -> fail("Expected AppResult.Ok but was AppResult.Err($error)")
}

fun AppResult<*>.assertErr(): AppError = when (this) {
    is AppResult.Err -> error
    is AppResult.Ok -> fail("Expected AppResult.Err but was AppResult.Ok($value)")
}
