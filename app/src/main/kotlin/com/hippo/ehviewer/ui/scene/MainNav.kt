package com.hippo.ehviewer.ui.scene

import androidx.annotation.MainThread
import androidx.navigation.NavController
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.ui.destinations.GalleryDetailScreenDestination
import com.hippo.ehviewer.ui.destinations.GalleryListScreenDestination
import com.hippo.ehviewer.ui.destinations.ProgressScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction

@MainThread
fun DestinationsNavigator.navWithUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    GalleryListUrlParser.parse(url)?.let {
        navigate(GalleryListScreenDestination(it))
        return true
    }

    GalleryDetailUrlParser.parse(url)?.apply {
        navigate(GalleryDetailScreenDestination(TokenArgs(gid, token)))
        return true
    }

    GalleryPageUrlParser.parse(url)?.apply {
        navigate(ProgressScreenDestination(gid, pToken, page))
        return true
    }
    return false
}

@MainThread
fun NavController.navWithUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    GalleryListUrlParser.parse(url)?.let {
        navigate(GalleryListScreenDestination(it))
        return true
    }

    GalleryDetailUrlParser.parse(url)?.apply {
        navigate(GalleryDetailScreenDestination(TokenArgs(gid, token)))
        return true
    }

    GalleryPageUrlParser.parse(url)?.apply {
        navigate(ProgressScreenDestination(gid, pToken, page))
        return true
    }
    return false
}

@MainThread
fun NavController.navigate(direction: Direction) = navigate(direction.route)
