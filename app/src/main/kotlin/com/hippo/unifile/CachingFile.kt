package com.hippo.unifile

abstract class CachingFile<T : UniFile> : UniFile {
    protected abstract val cachePresent: Boolean

    protected abstract val allChildren: MutableList<T>

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
}
