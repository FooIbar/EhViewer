package com.hippo.ehviewer.client.parser

import kotlinx.serialization.Serializable

@Serializable
data class VoteTagResult(
    val error: String?,
    val tagpane: String?,
)
