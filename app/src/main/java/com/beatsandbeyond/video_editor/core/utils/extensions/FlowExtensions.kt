package com.beatsandbeyond.video_editor.core.utils.extensions

import com.beatsandbeyond.video_editor.core.common.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Extension functions on [Flow] for common patterns in B&B Video Editor.
 */

/**
 * Maps a [Flow] of [AppResult.Success] values using [transform],
 * propagating [AppResult.Error] and [AppResult.Loading] unchanged.
 */
fun <T, R> Flow<AppResult<T>>.mapSuccess(
    transform: suspend (T) -> R,
): Flow<AppResult<R>> = map { result ->
    when (result) {
        is AppResult.Success -> AppResult.Success(transform(result.data))
        is AppResult.Error -> result
        is AppResult.Loading -> result
    }
}

/**
 * Catches exceptions thrown downstream and converts them to [AppResult.Error] emissions.
 * Use when wrapping flows that may throw unchecked exceptions.
 */
fun <T> Flow<AppResult<T>>.catchAsError(): Flow<AppResult<T>> =
    catch { e -> emit(AppResult.Error(e)) }
