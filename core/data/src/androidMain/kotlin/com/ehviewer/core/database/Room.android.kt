package com.ehviewer.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import okio.Path.Companion.toOkioPath
import splitties.init.appCtx

actual inline fun <reified T : RoomDatabase> roomDb(
    name: String,
    builder: RoomDatabase.Builder<T>.() -> Unit,
) = Room.databaseBuilder<T>(appCtx, name).setDriver(AndroidSQLiteDriver()).apply(builder).build()

actual fun getDatabasePath(name: String) = appCtx.getDatabasePath(name).toOkioPath()
