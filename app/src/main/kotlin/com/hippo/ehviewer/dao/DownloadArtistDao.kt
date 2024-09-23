package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadArtistDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(downloadArtists: List<DownloadArtist>)

    @Query("DELETE FROM DOWNLOAD_ARTISTS WHERE GID = :gid")
    suspend fun deleteByGid(gid: Long)
}
