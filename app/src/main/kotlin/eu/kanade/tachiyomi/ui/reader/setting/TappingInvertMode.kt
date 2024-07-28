package eu.kanade.tachiyomi.ui.reader.setting

enum class TappingInvertMode(
    val shouldInvertHorizontal: Boolean = false,
    val shouldInvertVertical: Boolean = false,
) {
    NONE,
    HORIZONTAL(shouldInvertHorizontal = true),
    VERTICAL(shouldInvertVertical = true),
    BOTH(shouldInvertHorizontal = true, shouldInvertVertical = true),
}
