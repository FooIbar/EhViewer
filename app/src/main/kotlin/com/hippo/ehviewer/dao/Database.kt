package com.hippo.ehviewer.dao

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hippo.ehviewer.client.data.BaseGalleryInfo

@Database(
    entities = [
        BaseGalleryInfo::class, DownloadLabel::class, DownloadEntity::class, DownloadDirname::class,
        Filter::class, HistoryInfo::class, LocalFavoriteInfo::class, ProgressInfo::class, QuickSearch::class,
        GalleryEntityFts::class,
    ],
    version = 22,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 4, to = 5, spec = Schema4to5::class),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7, spec = Schema6to7::class),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = Schema9to10::class),
        AutoMigration(from = 10, to = 11, spec = Schema10to11::class),
        AutoMigration(from = 11, to = 12, spec = Schema11to12::class),
        AutoMigration(from = 12, to = 13, spec = Schema12to13::class),
        AutoMigration(from = 13, to = 14, spec = Schema13to14::class),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 18, to = 19, spec = Schema18to19::class),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21, spec = Schema20to21::class),
        AutoMigration(from = 21, to = 22, spec = Schema21to22::class),
    ],
)
@TypeConverters(FilterModeConverter::class)
abstract class EhDatabase : RoomDatabase() {
    abstract fun galleryDao(): GalleryDao
    abstract fun downloadDirnameDao(): DownloadDirnameDao
    abstract fun downloadLabelDao(): DownloadLabelDao
    abstract fun downloadsDao(): DownloadsDao
    abstract fun filterDao(): FilterDao
    abstract fun historyDao(): HistoryDao
    abstract fun localFavoritesDao(): LocalFavoritesDao
    abstract fun progressDao(): ProgressDao
    abstract fun quickSearchDao(): QuickSearchDao
}

@Database(
    entities = [Search::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class SearchDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao
}
