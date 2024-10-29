package com.hippo.ehviewer.gallery

import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path

private val progressScope = CoroutineScope(Dispatchers.IO)

abstract class PageLoader2(val gid: Long, var startPage: Int) : PageLoader() {
    init {
        if (startPage == -1) {
            progressScope.launch {
                startPage = EhDB.getReadProgress(gid)
            }
        }
    }

    override fun close() {
        super.close()
        if (gid != 0L) {
            progressScope.launch {
                EhDB.putReadProgress(gid, startPage)
            }
        }
    }

    protected abstract val title: String

    protected abstract fun getImageExtension(index: Int): String?

    fun getImageFilename(index: Int): String? = getImageExtension(index)?.let {
        FileUtils.sanitizeFilename("$title - ${index + 1}.${it.lowercase()}")
    }

    abstract fun save(index: Int, file: Path): Boolean
}
