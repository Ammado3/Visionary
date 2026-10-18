package com.o3.vanguard.visionary.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.o3.vanguard.visionary.core.database.entity.*
import kotlinx.coroutines.flow.Flow

data class CategoryCount(
    val category: String,
    val count: Int
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_item ORDER BY sortKey DESC")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE category = :category ORDER BY sortKey DESC")
    fun observeByCategory(category: String): Flow<List<MediaEntity>>

    @Query("SELECT category AS category, COUNT(*) AS count FROM media_item GROUP BY category")
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("SELECT * FROM media_item WHERE id = :id")
    fun observeById(id: Long): Flow<MediaEntity?>

    @Upsert suspend fun upsert(items: List<MediaEntity>)
    @Query("DELETE FROM media_item WHERE id IN (:ids)") suspend fun delete(ids: List<Long>)
}

@Dao
interface SearchDao {
    @Query("SELECT * FROM recent_search ORDER BY searchedAtMs DESC LIMIT 10")
    fun observeRecentSearches(): Flow<List<RecentSearchEntity>>

    @Upsert suspend fun upsertRecentSearch(entity: RecentSearchEntity)
}

@Dao
interface IndexJobDao {
    @Query("SELECT * FROM index_job WHERE state = 'PENDING'")
    fun observePendingJobs(): Flow<List<IndexJobEntity>>

    @Upsert suspend fun upsertJob(job: IndexJobEntity)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_item ORDER BY createdAtMs DESC")
    fun observeVaultItems(): Flow<List<VaultItemEntity>>

    @Upsert suspend fun upsertVaultItem(item: VaultItemEntity)
    @Query("DELETE FROM vault_item WHERE id = :id") suspend fun deleteVaultItem(id: String)
}
