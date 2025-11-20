package com.hippo.ehviewer.client.exception

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.CborArray

@Serializable
sealed interface EhError {
    @CborArray
    @Serializable
    @SerialName("NoHits")
    data object NoHits : EhError

    @CborArray
    @Serializable
    @SerialName("NoWatched")
    data object NoWatched : EhError

    @CborArray
    @Serializable
    @SerialName("NeedLogin")
    data object NeedLogin : EhError

    @CborArray
    @Serializable
    @SerialName("NoHathClient")
    data object NoHathClient : EhError

    @CborArray
    @Serializable
    @SerialName("InsufficientFunds")
    data object InsufficientFunds : EhError

    @CborArray
    @Serializable
    @SerialName("GalleryUnavailable")
    data class GalleryUnavailable(val message: String) : EhError

    @CborArray
    @Serializable
    @SerialName("IpBanned")
    data class IpBanned(val message: String) : EhError

    @CborArray
    @Serializable
    @SerialName("Error")
    data class Error(val message: String) : EhError
}
