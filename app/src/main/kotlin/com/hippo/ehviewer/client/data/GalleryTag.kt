package com.hippo.ehviewer.client.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class VoteStatus {
    NONE,
    UP,
    DOWN,
}

@Parcelize
data class GalleryTag(
    val text: String,
    val weak: Boolean,
    val vote: VoteStatus,
) : Parcelable
