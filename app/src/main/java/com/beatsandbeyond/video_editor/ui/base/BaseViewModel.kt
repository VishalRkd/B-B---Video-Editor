package com.beatsandbeyond.video_editor.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beatsandbeyond.video_editor.core.utils.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Abstract base class for all ViewModels in B&B Video Editor.
 *
 * Provides:
 * - A consistent coroutine exception handler that logs uncaught errors
 * - A [launchSafely] helper that wraps [viewModelScope.launch] with error handling
 * - A logging tag derived from the concrete ViewModel class name
 *
 * All ViewModels should use [launchSafely] instead of [viewModelScope.launch] directly
 * to ensure uncaught exceptions are always logged and surfaced to the UI.
 */
abstract class BaseViewModel : ViewModel() {

    protected val tag: String get() = this::class.java.simpleName

    /** Override in subclasses to handle uncaught coroutine exceptions. */
    protected open fun onCoroutineError(throwable: Throwable) {
        Logger.e(tag, "Uncaught coroutine error: ${throwable.message}", throwable)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onCoroutineError(throwable)
    }

    /**
     * Launches a coroutine in [viewModelScope] with the shared exception handler.
     * Prefer this over [viewModelScope.launch] to ensure consistent error handling.
     */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            block()
        }
    }
}
