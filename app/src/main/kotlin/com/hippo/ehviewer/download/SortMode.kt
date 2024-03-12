package com.hippo.ehviewer.download

import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo

data class SortMode(val field: Field, val order: Order, val groupByDownloadLabel: Boolean = false) {
    val flag
        get() = groupByDownloadLabel.compareTo(false) shl 4 or (field.value shl 1) or order.value

    enum class Field(val value: Int) {
        GID(0),
        TITLE(1),
        TIME(2),
        LABEL(3),
        PAGES(4),
    }

    enum class Order(val value: Int) {
        ASC(0),
        DESC(1),
    }

    fun comparator(): Comparator<DownloadInfo> {
        var comparator = when (field) {
            Field.GID -> compareBy { it.gid }
            Field.TITLE, Field.LABEL -> compareBy { EhUtils.getSuitableTitle(it) }
            Field.TIME -> compareBy { it.time }
            Field.PAGES -> compareBy<DownloadInfo> { it.pages }.thenBy { EhUtils.getSuitableTitle(it) }
        }
        if (order == Order.DESC) {
            comparator = comparator.reversed()
        }
        if (groupByDownloadLabel) {
            comparator = compareBy<DownloadInfo> { it.label }.then(comparator)
        }
        return comparator
    }

    companion object {
        fun from(flag: Int): SortMode {
            val field = flag and 0xF shr 1
            val order = flag and 0x1
            val groupByDownloadLabel = flag shr 4 == 1
            require(field in Field.entries.indices && order in Order.entries.indices)
            return SortMode(Field.entries[field], Order.entries[order], groupByDownloadLabel)
        }
        val Default = SortMode(Field.TIME, Order.DESC)
        val All = listOf(
            SortMode(Field.TIME, Order.DESC),
            SortMode(Field.TIME, Order.ASC),
            SortMode(Field.GID, Order.DESC),
            SortMode(Field.GID, Order.ASC),
            SortMode(Field.TITLE, Order.ASC),
            SortMode(Field.TITLE, Order.DESC),
            SortMode(Field.PAGES, Order.ASC),
            SortMode(Field.PAGES, Order.DESC),
        )
    }
}
