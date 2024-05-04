package com.hippo.ehviewer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SearchDao {
    @Query("DELETE FROM suggestions")
    suspend fun clear()

    @Query("DELETE FROM suggestions WHERE `query` = :query")
    suspend fun deleteQuery(query: String)

    @Query("SELECT DISTINCT `query` FROM suggestions WHERE `query` LIKE :prefix || '%' ORDER BY date DESC LIMIT :limit")
    suspend fun rawSuggestions(prefix: String, limit: Int): List<String>

    @Query("SELECT DISTINCT `query` FROM suggestions ORDER BY date DESC LIMIT :limit")
    suspend fun list(limit: Int): List<String>

    @Insert
    suspend fun insert(search: Search)
}
