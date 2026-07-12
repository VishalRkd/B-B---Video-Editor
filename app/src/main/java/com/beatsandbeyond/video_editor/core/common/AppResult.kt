package com.beatsandbeyond.video_editor.core.common

/**
 * A generic wrapper for operation results that propagates through all layers of the application.
 *
 * - [Success]: Operation completed successfully, [data] holds the result.
 * - [Error]: Operation failed, [exception] holds the cause, [message] provides a human-readable description.
 * - [Loading]: Operation is in progress (used primarily in UI state flows).
 *
 * Usage:
 * ```
 * val result: AppResult<List<MediaItem>> = repository.getMedia()
 * result
 *     .onSuccess { items -> adapter.submitList(items) }
 *     .onError { _, message -> showError(message) }
 * ```
 */
sealed class AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>()

    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage,
    ) : AppResult<Nothing>()

    data object Loading : AppResult<Nothing>()

    // ── Helpers ────────────────────────────────────────────────────────────

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    /** Returns data if [Success], null otherwise. */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns data if [Success], throws the exception if [Error], or throws IllegalStateException if [Loading]. */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> error("Result is still loading")
    }
}

/** Executes [action] if this is [AppResult.Success]. Returns the same result for chaining. */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Executes [action] if this is [AppResult.Error]. Returns the same result for chaining. */
inline fun <T> AppResult<T>.onError(action: (Throwable, String?) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(exception, message)
    return this
}

/** Executes [action] if this is [AppResult.Loading]. Returns the same result for chaining. */
inline fun <T> AppResult<T>.onLoading(action: () -> Unit): AppResult<T> {
    if (this is AppResult.Loading) action()
    return this
}

/** Transforms a [AppResult.Success] value using [transform], propagating [Error] and [Loading] unchanged. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
    is AppResult.Loading -> this
}
