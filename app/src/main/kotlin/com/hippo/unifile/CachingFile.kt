package com.hippo.unifile

abstract class CachingFile<T : UniFile>(override val parent: T?) : UniFile {
    private var cachePresent = false

    private val allChildren by lazy {
        cachePresent = true
        list()?.associateBy { it.name!! } as? HashMap ?: hashMapOf()
    }

    protected abstract fun list(): MutableList<T>?

    protected fun popCacheIfPresent(file: T) {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren[file.name!!.lowercase()] = file
            }
        }
    }

    protected fun evictCacheIfPresent(name: String) {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.remove(name.lowercase())
            }
        }
    }

    override fun listFiles() = synchronized(allChildren) {
        allChildren.values.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.entries.firstOrNull { (name, _) -> filter(name) }?.value
    }

    override fun findFile(displayName: String) = allChildren[displayName.lowercase()]

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
