package com.o3.vanguard.visionary.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.o3.vanguard.visionary.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_item ORDER BY sortKey DESC")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE category = :category ORDER BY sortKey DESC")
    fun observeByCategory(category: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE albumId = :albumId ORDER BY sortKey DESC")
    fun observeByAlbum(albumId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE isFavorite = 1 ORDER BY sortKey DESC")
    fun observeFavorites(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_item WHERE id = :id")
    fun observeById(id: Long): Flow<MediaEntity?>

    @Query("SELECT * FROM media_item")
    suspend fun getAll(): List<MediaEntity>

    @Query("SELECT * FROM media_text WHERE mediaId = :mediaId")
    suspend fun getText(mediaId: Long): MediaTextEntity?

    @Query("SELECT * FROM media_text WHERE mediaId IN (:mediaIds)")
    suspend fun getTexts(mediaIds: List<Long>): List<MediaTextEntity>

    @Query("SELECT * FROM media_text WHERE mediaId = :mediaId")
    fun observeText(mediaId: Long): Flow<MediaTextEntity?>

    @Query("""
        SELECT media_item.* FROM media_item
        LEFT JOIN media_text ON media_text.mediaId = media_item.id
        WHERE (:category = 'ALL' OR media_item.category = :category)
          AND (
            LOWER(media_item.ref) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(media_text.fullText, '')) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(COALESCE(media_text.labelsText, '')) LIKE '%' || LOWER(:query) || '%'
          )
        ORDER BY media_item.sortKey DESC
        LIMIT 100
    """)
    suspend fun search(query: String, category: String): List<MediaEntity>

    @Query("""
        SELECT media_item.* FROM media_item
        LEFT JOIN media_text ON media_text.mediaId = media_item.id
        WHERE LOWER(COALESCE(media_text.labelsText, '')) LIKE LOWER(:query)
           OR LOWER(COALESCE(media_text.fullText, '')) LIKE LOWER(:query)
           OR LOWER(media_item.ref) LIKE LOWER(:query)
    """)
    suspend fun searchByLabelsExact(query: String): List<MediaEntity>

    @Query("""
        SELECT media_item.* FROM media_item
        LEFT JOIN media_text ON media_text.mediaId = media_item.id
        WHERE media_text.embedding IS NOT NULL
    """)
    suspend fun getImagesWithEmbeddings(): List<MediaEntity>

    @Query("SELECT * FROM media_text")
    suspend fun getAllTextEntities(): List<MediaTextEntity>

    @Query("DELETE FROM media_item WHERE ref NOT IN (:existingUris)")
    suspend fun deleteNonExistingImages(existingUris: List<String>)

    @Query("DELETE FROM media_item WHERE ref = :ref")
    suspend fun deleteByRef(ref: String)

    @Query("SELECT * FROM media_item WHERE id = :id")
    suspend fun getById(id: Long): MediaEntity?

    @Query("DELETE FROM media_text WHERE mediaId = :id")
    suspend fun deleteText(id: Long)

    @Upsert suspend fun upsert(items: List<MediaEntity>)
    @Upsert suspend fun upsertText(items: List<MediaTextEntity>)
    @Query("DELETE FROM media_item WHERE id IN (:ids)") suspend fun delete(ids: List<Long>)

    @Query("UPDATE media_item SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
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

    @Query("SELECT * FROM vault_item WHERE id = :id")
    suspend fun getVaultItem(id: String): VaultItemEntity?

    @Upsert suspend fun upsertVaultItem(item: VaultItemEntity)
    @Query("DELETE FROM vault_item WHERE id = :id") suspend fun deleteVaultItem(id: String)
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_item ORDER BY deletedAtMs DESC")
    fun observeTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_item WHERE id = :id")
    suspend fun getTrashItem(id: Long): TrashItemEntity?

    @Upsert suspend fun insertTrashItem(item: TrashItemEntity)

    @Query("DELETE FROM trash_item WHERE id = :id") suspend fun deleteTrashItem(id: Long)
}

@Dao
interface ExclusionDao {
    @Query("SELECT ref FROM index_exclusion")
    suspend fun getAllExclusions(): List<String>

    @Upsert suspend fun upsertExclusion(exclusion: IndexExclusionEntity)

    @Query("DELETE FROM index_exclusion WHERE ref = :ref")
    suspend fun deleteExclusion(ref: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)
}
