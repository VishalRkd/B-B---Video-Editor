package com.bbvideoeditor.app.diagnostics

import android.os.Trace
import android.os.Build

/** Stable, opt-in system-trace slices. The disabled path creates no events or strings. */
internal inline fun <T> engineTrace(name: String, block: () -> T): T {
    if (Build.VERSION.SDK_INT < 29 || !Trace.isEnabled()) return block()
    Trace.beginSection(name)
    return try { block() } finally { Trace.endSection() }
}

// Paired at the call site with endEngineTrace, including Compose function boundaries.
@android.annotation.SuppressLint("UnclosedTrace")
internal fun beginEngineTrace(name: String): Boolean = (Build.VERSION.SDK_INT >= 29 && Trace.isEnabled()).also {
    if (it) Trace.beginSection(name)
}

internal fun endEngineTrace(enabled: Boolean) {
    if (enabled) Trace.endSection()
}

internal fun engineTraceCounter(name: String, value: Long) {
    if (Build.VERSION.SDK_INT >= 29 && Trace.isEnabled()) Trace.setCounter(name, value)
}
