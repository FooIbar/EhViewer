package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT IFNULL(PAGE, 0) FROM PROGRESS WHERE GID = :gid")
    suspend fun getPage(gid: Long): Int

    @Query("SELECT IFNULL(PAGE, 0) FROM PROGRESS WHERE GID = :gid")
    fun getPageFlow(gid: Long): Flow<Int>

    @Query("SELECT * FROM PROGRESS")
    suspend fun list(): List<ProgressInfo>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(progress: List<ProgressInfo>)

    @Upsert
    suspend fun upsert(progress: ProgressInfo)

    @Query("DELETE FROM PROGRESS")
    suspend fun deleteAll()
}
