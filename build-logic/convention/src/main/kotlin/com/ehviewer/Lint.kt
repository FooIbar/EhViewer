package com.ehviewer

import com.android.build.api.dsl.Lint

internal fun Lint.configureLint() {
    checkReleaseBuilds = false
    disable += setOf("MissingTranslation", "MissingQuantity")
    error += setOf("InlinedApi")
}
