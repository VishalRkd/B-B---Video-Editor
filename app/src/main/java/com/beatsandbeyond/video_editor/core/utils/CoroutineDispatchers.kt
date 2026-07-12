package com.beatsandbeyond.video_editor.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides injectable [CoroutineDispatcher] instances used throughout the app.
 *
 * Injecting dispatchers (instead of referencing [Dispatchers] directly) makes
 * every coroutine-based class unit-testable by substituting test dispatchers.
 *
 * Usage in production:
 * ```
 * class MyRepository @Inject constructor(private val dispatchers: CoroutineDispatchers) {
 *     suspend fun doWork() = withContext(dispatchers.io) { ... }
 * }
 * ```
 *
 * Usage in tests:
 * ```
 * val testDispatchers = CoroutineDispatchers(
 *     main = UnconfinedTestDispatcher(),
 *     io = UnconfinedTestDispatcher(),
 *     default = UnconfinedTestDispatcher(),
 * )
 * ```
 */
data class CoroutineDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val unconfined: CoroutineDispatcher = Dispatchers.Unconfined,
)
