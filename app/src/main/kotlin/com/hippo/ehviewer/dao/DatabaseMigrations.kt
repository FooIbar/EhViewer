package com.hippo.ehviewer.dao

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED

class Schema4to5 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "DOWNLOADS",
            "HISTORY",
            "LOCAL_FAVORITES",
        )
        val prefixToRemove = arrayOf(
            "https://ehgt.org/",
            "https://s.exhentai.org/t/",
            "https://exhentai.org/t/",
        )
        needMigrationTables.forEach { table ->
            prefixToRemove.forEach { prefix ->
                db.execSQL("UPDATE $table SET thumb = SUBSTR(thumb ,LENGTH('$prefix') + 1) WHERE thumb LIKE '$prefix%'")
            }
        }
    }
}

@DeleteTable(tableName = "BOOKMARKS")
class Schema6to7 : AutoMigrationSpec

@RenameColumn(tableName = "HISTORY", fromColumnName = "MODE", toColumnName = "FAVORITE_SLOT")
class Schema9to10 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE HISTORY SET FAVORITE_SLOT = FAVORITE_SLOT - 2")
    }
}

class Schema10to11 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "QUICK_SEARCH",
            "DOWNLOAD_LABELS",
            "DOWNLOADS",
        )
        needMigrationTables.forEach { table ->
            // TODO: Rewrite this with row_number() when min sdk is 30 (SQLite 3.28.0)
            db.execSQL("UPDATE $table SET POSITION = (SELECT COUNT(*) FROM $table T WHERE T.TIME < $table.TIME)")
        }
    }
}

@DeleteColumn(tableName = "QUICK_SEARCH", columnName = "TIME")
@DeleteColumn(tableName = "DOWNLOAD_LABELS", columnName = "TIME")
class Schema11to12 : AutoMigrationSpec

class Schema12to13 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        val needMigrationTables = arrayOf(
            "HISTORY",
            "DOWNLOADS",
            "LOCAL_FAVORITES",
        )
        needMigrationTables.forEachIndexed { index, table ->
            db.execSQL(
                "INSERT OR IGNORE INTO GALLERIES " +
                    "SELECT GID, TOKEN, TITLE, TITLE_JPN, THUMB, CATEGORY, POSTED, UPLOADER, RATING, SIMPLE_LANGUAGE, " +
                    "${if (index == 0) "FAVORITE_SLOT" else NOT_FAVORITED} FROM $table",
            )
        }
    }
}

@DeleteColumn(tableName = "HISTORY", columnName = "TOKEN")
@DeleteColumn(tableName = "HISTORY", columnName = "TITLE")
@DeleteColumn(tableName = "HISTORY", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "HISTORY", columnName = "THUMB")
@DeleteColumn(tableName = "HISTORY", columnName = "CATEGORY")
@DeleteColumn(tableName = "HISTORY", columnName = "POSTED")
@DeleteColumn(tableName = "HISTORY", columnName = "UPLOADER")
@DeleteColumn(tableName = "HISTORY", columnName = "RATING")
@DeleteColumn(tableName = "HISTORY", columnName = "SIMPLE_LANGUAGE")
@DeleteColumn(tableName = "HISTORY", columnName = "FAVORITE_SLOT")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TOKEN")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TITLE")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "THUMB")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "CATEGORY")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "POSTED")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "UPLOADER")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "RATING")
@DeleteColumn(tableName = "LOCAL_FAVORITES", columnName = "SIMPLE_LANGUAGE")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TOKEN")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TITLE")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "TITLE_JPN")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "THUMB")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "CATEGORY")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "POSTED")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "UPLOADER")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "RATING")
@DeleteColumn(tableName = "DOWNLOADS", columnName = "SIMPLE_LANGUAGE")
class Schema13to14 : AutoMigrationSpec

class Schema17to18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `_new_GALLERIES` (`GID` INTEGER NOT NULL, `TOKEN` TEXT NOT NULL, `TITLE` TEXT, `TITLE_JPN` TEXT, `THUMB` TEXT, `CATEGORY` INTEGER NOT NULL, `POSTED` TEXT, `UPLOADER` TEXT, `RATING` REAL NOT NULL, `SIMPLE_LANGUAGE` TEXT, `FAVORITE_SLOT` INTEGER NOT NULL, PRIMARY KEY(`GID`))")
        db.execSQL("INSERT INTO `_new_GALLERIES` (`GID`,`TOKEN`,`TITLE`,`TITLE_JPN`,`THUMB`,`CATEGORY`,`POSTED`,`UPLOADER`,`RATING`,`SIMPLE_LANGUAGE`,`FAVORITE_SLOT`) SELECT `GID`,`TOKEN`,`TITLE`,`TITLE_JPN`,`THUMB`,`CATEGORY`,`POSTED`,`UPLOADER`,`RATING`,`SIMPLE_LANGUAGE`,`FAVORITE_SLOT` FROM `GALLERIES` WHERE `TOKEN` IS NOT NULL")
        db.execSQL("DROP TABLE `GALLERIES`")
        db.execSQL("ALTER TABLE `_new_GALLERIES` RENAME TO `GALLERIES`")
    }
}

@RenameColumn(tableName = "DOWNLOADS", fromColumnName = "POSITION", toColumnName = "TIME")
class Schema18to19 : AutoMigrationSpec

class Schema20to21 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO GalleryFts(GalleryFts) VALUES('rebuild')")
    }
}

// Virtual tables can't be altered, so we use rename to recreate it
@RenameTable(fromTableName = "GalleryFts", toTableName = "GALLERIES_FTS")
class Schema21to22 : AutoMigrationSpec
