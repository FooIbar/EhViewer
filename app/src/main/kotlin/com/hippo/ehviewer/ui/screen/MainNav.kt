package com.hippo.ehviewer.ui.screen

import androidx.annotation.MainThread
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import moe.tarsin.navigate

private fun urlToDestination(url: String): Direction? {
    if (url.isEmpty()) return null
    val ret1 = GalleryListUrlParser.parse(url)
    if (ret1 != null) return ret1.asDst()
    val ret2 = GalleryDetailUrlParser.parse(url)
    if (ret2 != null) return ret2.gid asDstWith ret2.token
    val ret3 = GalleryPageUrlParser.parse(url)
    if (ret3 != null) return ProgressScreenDestination(ret3.gid, ret3.pToken, ret3.page)
    return null
}

context(nav: DestinationsNavigator)
@MainThread
fun navWithUrl(url: String): Boolean {
    val dest = urlToDestination(url) ?: return false
    navigate(dest)
    return true
}

fun BaseGalleryInfo.asDst() = GalleryDetailScreenDestination(GalleryInfoArgs(this))

infix fun Long.asDstWith(token: String) = GalleryDetailScreenDestination(TokenArgs(this, token))

infix fun Long.asDstPageTo(page: Int) = this to page

infix fun Pair<Long, Int>.with(token: String) = GalleryDetailScreenDestination(TokenArgs(first, token, second))

fun ListUrlBuilder.asDst() = GalleryListScreenDestination(this)
