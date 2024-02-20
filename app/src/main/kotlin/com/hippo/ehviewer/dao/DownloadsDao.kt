package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadsDao {
    @Suppress("ktlint:standard:annotation")
    @Query("SELECT LABEL, COUNT(*) AS COUNT FROM DOWNLOADS LEFT JOIN DOWNLOAD_LABELS USING(LABEL) GROUP BY LABEL")
    fun count(): Flow<Map<@MapColumn("LABEL") String?, @MapColumn("COUNT") Int>>

    @Query("SELECT * FROM DOWNLOADS ORDER BY TIME")
    suspend fun list(): List<DownloadEntity>

    @Transaction
    @Query("SELECT * FROM DOWNLOADS LEFT JOIN DOWNLOAD_DIRNAME USING(GID) ORDER BY TIME DESC")
    suspend fun joinList(): List<DownloadInfo>

    @Update
    suspend fun update(downloadInfo: List<DownloadEntity>)

    @Insert
    suspend fun insert(downloadInfo: List<DownloadEntity>)

    @Upsert
    suspend fun upsert(t: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: DownloadEntity)

    @Delete
    suspend fun delete(downloadInfo: List<DownloadEntity>)
}
