package com.o3.vanguard.visionary.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

sealed interface AppError {
    data class Unsupported(val reason: String) : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Cancelled : AppError
    data class Io(val kind: String) : AppError
    data class Crypto(val kind: String) : AppError
    data class Unknown(val kind: String) : AppError
}

sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Ok)?.value

interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}

data class AppConfig(
    val versionName: String,
    val isDebug: Boolean,
    val useFakeData: Boolean,
)

val coreCommonModule = module {
    single<AppDispatchers> { DefaultAppDispatchers() }
}
