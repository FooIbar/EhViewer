package com.ehviewer.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ehviewer.core.database.model.GalleryEntity
import com.ehviewer.core.database.model.LocalFavoriteInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalFavoritesDao {
    @Query("SELECT COUNT(*) FROM LOCAL_FAVORITES")
    fun count(): Flow<Int>

    @Query("SELECT LOCAL_FAVORITES.* FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID) ORDER BY TIME")
    suspend fun list(): List<LocalFavoriteInfo>

    @Query("SELECT GALLERIES.* FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID) ORDER BY TIME DESC")
    fun joinListLazy(): PagingSource<Int, GalleryEntity>

    @Query(
        """SELECT GALLERIES.* FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID)
        JOIN GALLERIES_FTS ON GALLERIES.rowid = docid WHERE GALLERIES_FTS MATCH :title ORDER BY TIME DESC""",
    )
    fun joinListLazy(title: String): PagingSource<Int, GalleryEntity>

    @Query("SELECT GALLERIES.* FROM LOCAL_FAVORITES JOIN GALLERIES USING(GID) ORDER BY RANDOM() LIMIT 1")
    suspend fun random(): GalleryEntity?

    @Query("SELECT EXISTS(SELECT * FROM LOCAL_FAVORITES WHERE GID = :gid)")
    suspend fun contains(gid: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(localFavorites: List<LocalFavoriteInfo>)

    @Upsert
    suspend fun upsert(t: LocalFavoriteInfo)

    @Query("DELETE FROM LOCAL_FAVORITES WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)
}
