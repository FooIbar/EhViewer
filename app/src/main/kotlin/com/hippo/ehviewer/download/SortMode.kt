package com.hippo.ehviewer.download

import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.dao.DownloadInfo

data class SortMode(val field: Field, val order: Order) {
    val flag
        get() = field.value shl 1 or order.value

    enum class Field(val value: Int) {
        GID(0),
        TITLE(1),
        TIME(2),
        LABEL(3),
    }

    enum class Order(val value: Int) {
        ASC(0),
        DESC(1),
    }

    fun comparator(): Comparator<DownloadInfo> {
        val comparator = when (field) {
            Field.GID -> compareBy { it.gid }
            Field.TITLE -> compareBy { EhUtils.getSuitableTitle(it) }
            Field.TIME -> compareBy { it.time }
            Field.LABEL -> compareBy<DownloadInfo> { it.label }.thenBy { EhUtils.getSuitableTitle(it) }
        }
        return if (order == Order.ASC) comparator else comparator.reversed()
    }

    companion object {
        fun from(flag: Int): SortMode {
            val field = flag shr 1
            val order = flag and 1
            require(field in Field.entries.indices && order in Order.entries.indices)
            return SortMode(Field.entries[field], Order.entries[order])
        }
        val Default = SortMode(Field.TIME, Order.DESC)
        val All = listOf(
            SortMode(Field.TIME, Order.DESC),
            SortMode(Field.TIME, Order.ASC),
            SortMode(Field.GID, Order.DESC),
            SortMode(Field.GID, Order.ASC),
            SortMode(Field.TITLE, Order.ASC),
            SortMode(Field.TITLE, Order.DESC),
            SortMode(Field.LABEL, Order.ASC),
            SortMode(Field.LABEL, Order.DESC),
        )
    }
}
