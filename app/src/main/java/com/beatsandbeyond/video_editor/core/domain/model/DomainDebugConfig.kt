package com.beatsandbeyond.video_editor.core.domain.model

/**
 * Global configuration to allow the pure domain layer to check configuration states
 * (like debug builds) set by the Android application module at startup.
 */
object DomainDebugConfig {
    /** If true, validation failures will throw exceptions rather than returning AppResult.Error. */
    var isDebug: Boolean = false
}
