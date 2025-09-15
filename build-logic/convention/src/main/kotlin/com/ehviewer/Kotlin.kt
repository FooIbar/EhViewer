package com.ehviewer

import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

// https://kotlinlang.org/docs/gradle-compiler-options.html#all-compiler-options
internal fun KotlinCommonCompilerOptions.configureKotlin() {
    progressiveMode = true
    allWarningsAsErrors = true
    optIn.addAll(
        "kotlin.ExperimentalStdlibApi",
        "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        "kotlin.contracts.ExperimentalContracts",
        "kotlin.time.ExperimentalTime",
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
    )
    freeCompilerArgs.addAll(
        "-Xcontext-parameters",
        "-Xwhen-expressions=indy",
        "-Xannotation-default-target=param-property",
    )
}

internal fun KotlinCommonCompilerOptions.configureKotlinCompose() {
    optIn.addAll(
        "androidx.compose.animation.ExperimentalAnimationApi",
        "androidx.compose.animation.ExperimentalSharedTransitionApi",
        "androidx.compose.foundation.ExperimentalFoundationApi",
        "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        "androidx.compose.material3.ExperimentalMaterial3Api",
        "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
        "androidx.compose.runtime.ExperimentalComposeRuntimeApi",
        "androidx.compose.ui.ExperimentalComposeUiApi",
    )
}
