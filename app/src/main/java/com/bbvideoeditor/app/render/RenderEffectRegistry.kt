package com.bbvideoeditor.app.render

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Effects
import com.bbvideoeditor.core.Clip
import com.bbvideoeditor.core.EffectSpec
import com.bbvideoeditor.core.MediaReference

/** One extension point shared by preview and export. Each call must return fresh audio processors. */
@UnstableApi
fun interface RenderEffectFactory {
    fun create(spec: EffectSpec, clip: Clip, media: MediaReference): Effects
}

@UnstableApi
class RenderEffectRegistry(private val factories: Map<String, RenderEffectFactory> = emptyMap()) {
    fun create(spec: EffectSpec, clip: Clip, media: MediaReference): Effects =
        factories[spec.type]?.create(spec, clip, media)
            ?: throw UnsupportedRenderFeature("The effect '${spec.type}' is not installed.")
}

class UnsupportedRenderFeature(message: String) : IllegalArgumentException(message)
