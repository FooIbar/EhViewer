package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.flow.Flow

interface RecordSnapshotScope<T : Any> {
    fun record(block: () -> T)
    fun transform(block: Flow<T>.() -> Flow<T>)
}

@Composable
fun <T : Any> delegateSnapshotUpdate(dsl: RecordSnapshotScope<T>.() -> Unit): State<T> {
    val state = remember(dsl) {
        object : RecordSnapshotScope<T> {
            lateinit var flow: Flow<T>
            lateinit var initial: T
            override fun record(block: () -> T) {
                initial = Snapshot.withoutReadObservation(block)
                flow = snapshotFlow(block)
            }
            override fun transform(block: Flow<T>.() -> Flow<T>) {
                flow = block(flow)
            }
        }.apply(dsl)
    }
    return with(state) { flow.collectAsState(initial) }
}
