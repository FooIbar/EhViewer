package com.hippo.ehviewer.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import arrow.atomic.value
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.MainActivity
import eu.kanade.tachiyomi.util.lang.withUIContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Fuck off the silly Android launcher and callback :)

private var id by AtomicInteger()::value

suspend fun <I, O> MainActivity.awaitActivityResult(contract: ActivityResultContract<I, O>, input: I): O {
    val key = "activity_rq#${++id}"
    var launcher: ActivityResultLauncher<I>? = null
    var observer: LifecycleEventObserver? = null
    observer = LifecycleEventObserver { _, event ->
        if (Lifecycle.Event.ON_DESTROY == event) {
            launcher?.unregister()
            if (observer != null) {
                lifecycle.removeObserver(observer!!)
            }
        }
    }
    return withUIContext {
        lifecycle.addObserver(observer)
        suspendCoroutine { cont ->
            // No cancellation support here since we cannot cancel a launched Intent
            launcher = activityResultRegistry.register(key, contract) {
                launcher?.unregister()
                lifecycle.removeObserver(observer)
                cont.resume(it)
            }.apply { launch(input) }
        }
    }
}

suspend fun MainActivity.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, key) == PackageManager.PERMISSION_GRANTED) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}

suspend fun MainActivity.pickVisualMedia(type: VisualMediaType): Uri? = awaitActivityResult(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest(mediaType = type))

@RequiresApi(Build.VERSION_CODES.O)
suspend fun MainActivity.requestInstallPermission(): Boolean {
    if (packageManager.canRequestPackageInstalls()) return true
    val granted = requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    if (!granted) {
        awaitActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName"),
            ),
        )
        requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    }
    return packageManager.canRequestPackageInstalls()
}

suspend fun MainActivity.installPackage(file: File) {
    val canInstall = !isAtLeastO || requestInstallPermission()
    check(canInstall) { getString(R.string.permission_denied) }
    val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(contentUri, "application/vnd.android.package-archive")
    }
    startActivity(intent)
}
