package com.hippo.unifile

abstract class CachingFile<T : UniFile>(override val parent: T?) : UniFile {
    private var cachePresent = false

    private val allChildren by lazy {
        cachePresent = true
        list() ?: mutableListOf()
    }

    protected abstract fun list(): MutableList<T>?

    protected fun popCacheIfPresent(file: T) {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.add(file)
            }
        }
    }

    protected fun evictCacheIfPresent(file: T) {
        if (cachePresent) {
            synchronized(allChildren) {
                allChildren.remove(file)
            }
        }
    }

    protected fun replaceCacheIfPresent(old: T, new: T) {
        if (cachePresent) {
            synchronized(allChildren) {
                val index = allChildren.indexOf(old)
                if (index != -1) {
                    allChildren[index] = new
                }
            }
        }
    }

    override fun listFiles() = synchronized(allChildren) {
        allChildren.toList()
    }

    override fun findFirst(filter: (String) -> Boolean) = synchronized(allChildren) {
        allChildren.firstOrNull { filter(it.name!!) }
    }

    override fun findFile(displayName: String) = findFirst { it.equals(displayName, true) }
}
