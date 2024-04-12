package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DownloadArtistDao {
    @Query("SELECT * FROM DOWNLOAD_ARTISTS")
    suspend fun list(): List<DownloadArtist>

    @Insert
    suspend fun insert(downloadArtist: DownloadArtist): Long

    @Insert
    suspend fun insert(downloadArtists: List<DownloadArtist>)

    @Query("DELETE FROM DOWNLOAD_ARTISTS WHERE GID = :gid")
    suspend fun deleteByKey(gid: Long)

    @Delete
    suspend fun delete(downloadArtist: DownloadArtist)

    @Delete
    suspend fun delete(downloadArtists: List<DownloadArtist>)
}
