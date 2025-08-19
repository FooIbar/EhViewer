package com.hippo.ehviewer.util

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ehviewer.core.i18n.R
import com.ehviewer.core.util.withUIContext
import java.io.File
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Fuck off the silly Android launcher and callback :)

private val atomicInteger = AtomicInt(0)

context(ctx: Context)
private val lifecycle: Lifecycle
    get() {
        var context: Context? = ctx
        while (true) {
            when (context) {
                is LifecycleOwner -> return context.lifecycle
                !is ContextWrapper -> error("This should never happen!")
                else -> context = context.baseContext
            }
        }
    }

context(_: Context)
suspend fun <I, O> awaitActivityResult(contract: ActivityResultContract<I, O>, input: I): O {
    val key = "activity_rq#${atomicInteger.fetchAndIncrement()}"
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
            val activity = findActivity<ComponentActivity>()
            launcher = activity.activityResultRegistry.register(key, contract) {
                launcher?.unregister()
                lifecycle.removeObserver(observer)
                cont.resume(it)
            }.apply { launch(input) }
        }
    }
}

context(ctx: Context)
suspend fun requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(ctx, key) == PackageManager.PERMISSION_GRANTED) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}

context(_: Context)
suspend fun pickVisualMedia(type: VisualMediaType): Uri? = awaitActivityResult(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest(mediaType = type))

@RequiresApi(Build.VERSION_CODES.O)
context(ctx: Context)
suspend fun requestInstallPermission(): Boolean = with(ctx) {
    if (packageManager.canRequestPackageInstalls()) return true
    val granted = requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    if (!granted) {
        awaitActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:$packageName".toUri(),
            ),
        )
        requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    }
    return packageManager.canRequestPackageInstalls()
}

context(ctx: Context)
suspend fun installPackage(file: File) = with(ctx) {
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
