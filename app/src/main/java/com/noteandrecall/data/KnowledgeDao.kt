package com.noteandrecall.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<KnowledgeItem>>

    @Query("SELECT * FROM knowledge_items ORDER BY recallCount ASC, createdAt DESC")
    fun getLeastRecalled(): Flow<List<KnowledgeItem>>

    @Query("SELECT * FROM knowledge_items WHERE id = :id")
    suspend fun getById(id: Long): KnowledgeItem?

    @Query("SELECT * FROM knowledge_items WHERE tags LIKE '%' || :tag || '%' ORDER BY recallCount ASC")
    fun getByTag(tag: String): Flow<List<KnowledgeItem>>

    @Insert
    suspend fun insert(item: KnowledgeItem): Long

    @Update
    suspend fun update(item: KnowledgeItem)

    @Delete
    suspend fun delete(item: KnowledgeItem)

    @Query("DELETE FROM knowledge_items")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<KnowledgeItem>)

    @Query("SELECT * FROM knowledge_items WHERE title = :title AND createdAt = :createdAt LIMIT 1")
    suspend fun findByTitleAndDate(title: String, createdAt: Long): KnowledgeItem?

    @Query("SELECT DISTINCT tags FROM knowledge_items")
    suspend fun getAllTagsStrings(): List<String>
}
