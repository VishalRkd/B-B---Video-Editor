package com.bbvideoeditor.app.editor

import androidx.compose.runtime.staticCompositionLocalOf

/** Optional Main-confined counters for rendering contracts. Production retains no counter object,
 * events or history. Tests opt in without changing the document or transport subscriptions. */
internal class TimelineRenderCounters {
    var timelineCommits = 0L
    var trackCommits = 0L
    var clipCommits = 0L
    var thumbnailCommits = 0L
    var thumbnailDraws = 0L
    var waveformCommits = 0L
    var waveformGeometry = 0L
    var waveformDraws = 0L
    var rulerDraws = 0L
    var flingStarts = 0L
    var flingFrames = 0L
    var lastReleaseVelocity = 0f
}

internal val LocalTimelineRenderCounters = staticCompositionLocalOf<TimelineRenderCounters?> { null }
