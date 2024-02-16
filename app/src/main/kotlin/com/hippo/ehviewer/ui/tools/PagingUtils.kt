package com.hippo.ehviewer.ui.tools

import androidx.paging.PagingSource.LoadResult

inline fun <K : Any, V : Any, R : LoadResult.Page<K, V>, T> Result<T>.foldToLoadResult(
    onSuccess: (value: T) -> R,
) = fold(
    onSuccess = onSuccess,
    onFailure = { LoadResult.Error(it) },
)
