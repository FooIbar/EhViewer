package com.hippo.ehviewer.util

import com.hippo.files.SystemFileSystem
import okio.Path

infix fun Path.sendTo(file: Path) = SystemFileSystem.copy(this, file)
