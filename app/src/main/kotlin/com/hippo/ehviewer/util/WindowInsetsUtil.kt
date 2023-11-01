package com.hippo.ehviewer.util

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import dev.chrisbanes.insetter.applyInsetter
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun buildWindowInsets(builderAction: WindowInsetsCompat.Builder.() -> Unit): WindowInsetsCompat {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = if (isAtLeastQ) {
        WindowInsetsCompat.Builder()
    } else {
        // TODO: Report to google issue tracker
        // On API 20-28, WindowInsetsCompat.Builder() will copy the WindowInsets.CONSUMED static field,
        // which isConsumed() returns true, resulting in the insets not being dispatched
        runCatching {
            val constructor = android.view.WindowInsets::class.java.getConstructor(Rect::class.java)
            val platformInsets = constructor.newInstance(Rect())
            WindowInsetsCompat.Builder(WindowInsetsCompat.toWindowInsetsCompat(platformInsets))
        }.getOrElse {
            it.printStackTrace()
            WindowInsetsCompat.Builder()
        }
    }
    builder.builderAction()
    return builder.build()
}

@Composable
fun WindowInsetsCompat.Builder.set(type: Int, insets: WindowInsets): WindowInsetsCompat.Builder {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return setInsets(
        type,
        Insets.of(
            insets.getLeft(density, layoutDirection),
            insets.getTop(density),
            insets.getRight(density, layoutDirection),
            insets.getBottom(density),
        ),
    )
}

fun View.applyNavigationBarsPadding() {
    applyInsetter {
        type(navigationBars = true) {
            padding()
        }
    }
}
