package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> asyncState(
    record: () -> T,
    transform: Flow<T>.() -> Flow<T> = { this },
) = remember(transform, record) {
    Snapshot.withoutReadObservation(record) to transform(snapshotFlow(record))
}.let { (initial, flow) ->
    flow.collectAsState(initial)
}
