package com.ehviewer.core.database

import androidx.room.RoomDatabase
import okio.Path

expect inline fun <reified T : RoomDatabase> roomDb(
    name: String,
    builder: RoomDatabase.Builder<T>.() -> Unit = {},
): T

expect fun getDatabasePath(name: String): Path
