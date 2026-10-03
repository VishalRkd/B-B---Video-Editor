package com.videoeditorpro.android.domain.model

enum class FilterPreset(val displayName: String) {
    NONE("Original"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    BW("B&W"),
    VIVID("Vivid"),
    CINEMATIC("Cinema")
}

/**
 * Encapsulates color adjustments and shader filter presets for video rendering.
 */
data class FilterItem(
    val preset: FilterPreset = FilterPreset.NONE,
    val brightness: Int = 0,   // -100 to 100
    val contrast: Int = 0,     // -100 to 100
    val saturation: Int = 0    // -100 to 100
) {
    val isDefault: Boolean
        get() = preset == FilterPreset.NONE && brightness == 0 && contrast == 0 && saturation == 0

    companion object {
        val NONE = FilterItem()
    }
}
