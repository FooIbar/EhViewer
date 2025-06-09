package com.hippo.ehviewer.client.data

import kotlinx.serialization.Serializable

enum class VoteStatus(val display: String) {
    None(""),
    Up("+1"),
    Down("-1"),
}

enum class PowerStatus {
    Solid,
    Active,
    Weak,
}

@Serializable
data class GalleryTag(
    val text: String,
    val power: PowerStatus,
    val vote: VoteStatus,
)
