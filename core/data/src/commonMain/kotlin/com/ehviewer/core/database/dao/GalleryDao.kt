package com.ehviewer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ehviewer.core.database.model.GalleryEntity

@Dao
interface GalleryDao {
    @Query("SELECT * FROM GALLERIES WHERE GID = :gid")
    suspend fun load(gid: Long): GalleryEntity?

    @Query("SELECT * FROM GALLERIES")
    suspend fun list(): List<GalleryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(galleryInfoList: List<GalleryEntity>)

    @Update
    suspend fun update(galleryInfo: GalleryEntity)

    @Update
    suspend fun update(galleryInfoList: List<GalleryEntity>)

    @Upsert
    suspend fun upsert(galleryInfo: GalleryEntity)

    @Delete
    suspend fun delete(galleryInfo: GalleryEntity)

    @Query("DELETE FROM GALLERIES WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)
}
