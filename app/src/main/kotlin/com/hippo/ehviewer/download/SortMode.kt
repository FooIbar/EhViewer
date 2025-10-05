package com.hippo.ehviewer.download

import com.ehviewer.core.database.model.DownloadInfo
import com.hippo.ehviewer.client.EhUtils

data class SortMode(val field: Field, val order: Order, val groupByDownloadLabel: Boolean = false) {
    val flag
        get() = groupByDownloadLabel.compareTo(false) shl 4 or (field.value shl 1) or order.value

    enum class Field(val value: Int) {
        GID(0),
        TITLE(1),
        TIME(2),
        PAGES(4),
    }

    enum class Order(val value: Int) {
        ASC(0),
        DESC(1),
    }

    fun comparator(): Comparator<DownloadInfo> {
        var comparator = when (field) {
            Field.GID -> compareBy { it.gid }
            Field.TITLE -> compareBy { EhUtils.getSuitableTitle(it) }
            Field.TIME -> compareBy { it.time }
            Field.PAGES -> compareBy<DownloadInfo> { it.pages }
        }
        if (order == Order.DESC) {
            comparator = comparator.reversed()
        }
        if (field == Field.PAGES) {
            comparator = comparator.thenByDescending { it.time }
        }
        if (groupByDownloadLabel) {
            comparator = compareBy<DownloadInfo> { it.label }.then(comparator)
        }
        return comparator
    }

    companion object {
        fun from(flag: Int): SortMode {
            val fieldValue = flag and 0xF shr 1
            val orderValue = flag and 0x1
            val groupByDownloadLabel = flag shr 4 == 1
            val field = Field.entries.find { it.value == fieldValue } ?: Default.field
            val order = Order.entries.find { it.value == orderValue } ?: Default.order
            return SortMode(field, order, groupByDownloadLabel)
        }
        val Default = SortMode(Field.TIME, Order.DESC)
        val All = listOf(
            Default,
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
