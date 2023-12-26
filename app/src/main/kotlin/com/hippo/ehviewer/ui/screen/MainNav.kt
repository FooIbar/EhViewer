package com.hippo.ehviewer.ui.screen

import androidx.annotation.MainThread
import androidx.navigation.NavController
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.ui.NavGraphs
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction

private fun urlToDestination(url: String): Direction? {
    if (url.isEmpty()) return null
    val ret1 = GalleryListUrlParser.parse(url)
    if (ret1 != null) return GalleryListScreenDestination(ret1)
    val ret2 = GalleryDetailUrlParser.parse(url)
    if (ret2 != null) return GalleryDetailScreenDestination(TokenArgs(ret2.gid, ret2.token))
    val ret3 = GalleryPageUrlParser.parse(url)
    if (ret3 != null) return ProgressScreenDestination(ret3.gid, ret3.pToken, ret3.page)
    return null
}

@MainThread
fun DestinationsNavigator.navWithUrl(url: String): Boolean {
    val dest = urlToDestination(url) ?: return false
    navigate(dest)
    return true
}

@MainThread
fun DestinationsNavigator.popNavigate(direction: Direction) = navigate(direction) {
    popUpTo(NavGraphs.root.route)
}

@MainThread
fun NavController.navWithUrl(url: String): Boolean {
    val dest = urlToDestination(url) ?: return false
    navigate(dest)
    return true
}

@MainThread
fun NavController.navigate(direction: Direction) = navigate(direction.route)
