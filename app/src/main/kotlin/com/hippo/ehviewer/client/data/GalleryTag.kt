package com.hippo.ehviewer.client.data

enum class VoteStatus(val display: String) {
    NONE(""),
    UP("+1"),
    DOWN("-1"),
}

enum class PowerStatus {
    SOLID,
    ACTIVE,
    WEAK,
}

data class GalleryTag(
    val text: String,
    val power: PowerStatus,
    val vote: VoteStatus,
)
