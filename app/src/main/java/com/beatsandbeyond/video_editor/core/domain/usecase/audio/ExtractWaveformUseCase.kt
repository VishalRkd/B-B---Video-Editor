package com.beatsandbeyond.video_editor.core.domain.usecase.audio

import com.beatsandbeyond.video_editor.core.common.AppResult
import com.beatsandbeyond.video_editor.core.domain.engine.AudioEngine
import com.beatsandbeyond.video_editor.core.domain.model.Asset
import javax.inject.Inject

/**
 * Extracts the waveform amplitude data from an [Asset] using the audio engine.
 */
class ExtractWaveformUseCase @Inject constructor(
    private val audioEngine: AudioEngine,
) {
    suspend operator fun invoke(asset: Asset, samplesPerSecond: Int = 100): AppResult<FloatArray> {
        return audioEngine.extractWaveform(asset, samplesPerSecond)
    }
}
