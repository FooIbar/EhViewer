package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> asyncState(
    produce: () -> T,
    transform: Flow<T>.() -> Flow<T> = { this },
) = remember(transform, produce) {
    Snapshot.withoutReadObservation(produce) to transform(snapshotFlow(produce))
}.let { (initial, flow) ->
    flow.collectAsState(initial)
}
