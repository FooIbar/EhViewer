package com.hippo.ehviewer.ui.tools

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadParams.Append
import androidx.paging.PagingSource.LoadParams.Prepend
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState

inline fun <K : Any, V : Any, R : LoadResult.Page<K, V>, T> Result<T>.foldToLoadResult(
    onSuccess: (value: T) -> R,
) = fold(
    onSuccess = onSuccess,
    onFailure = { LoadResult.Error(it) },
)

fun <Value : Any> PagingState<Int, Value>.getClippedRefreshKey() = when (val anchorPosition = anchorPosition) {
    null -> null
    else -> maxOf(0, anchorPosition - (config.initialLoadSize / 2))
}

fun getOffset(params: LoadParams<Int>, key: Int, itemCount: Int) = when (params) {
    is Prepend -> if (key < params.loadSize) 0 else key - params.loadSize
    is Append -> key
    is Refresh -> if (key >= itemCount) maxOf(0, itemCount - params.loadSize) else key
}

fun getLimit(params: LoadParams<Int>, key: Int) = when (params) {
    is Prepend -> if (key < params.loadSize) key else params.loadSize
    else -> params.loadSize
}
