@file:OptIn(ExperimentalCoroutinesApi::class)

package com.o3.vanguard.visionary.core.testkit.dispatcher

import com.o3.vanguard.visionary.core.common.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class TestAppDispatchers(
    override val io: CoroutineDispatcher = UnconfinedTestDispatcher(),
    override val default: CoroutineDispatcher = io,
    override val main: CoroutineDispatcher = io,
) : AppDispatchers

fun runViewModelTest(body: suspend TestScope.() -> Unit) {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        runTest(dispatcher) { body() }
    } finally {
        Dispatchers.resetMain()
    }
}
