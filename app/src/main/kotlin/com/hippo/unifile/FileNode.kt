package com.hippo.unifile

abstract class FileNode<T : UniFile>(override val parent: T?) : UniFile {
    override fun findFirst(filter: (String) -> Boolean) = listFiles().firstOrNull { filter(it.name!!) }

    override fun findFile(displayName: String) = listFiles().firstOrNull { it.name!!.equals(displayName, true) }

    override fun ensureDir(): Boolean {
        if (isDirectory) return true
        if (isFile) return false

        val parent = parent ?: return false
        if (!parent.ensureDir()) return false
        return parent.createDirectory(name!!) != null
    }

    override fun ensureFile(): Boolean {
        if (isDirectory) return false
        if (isFile) return true

        val parent = parent ?: return false
        if (!parent.ensureDir()) return false
        return parent.createFile(name!!) != null
    }
}
