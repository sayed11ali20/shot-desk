package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductionDao {

    @Transaction
    @Query(
        "SELECT * FROM projects " +
            "WHERE ownerId = :ownerId AND syncStatus != 'PENDING_DELETE' " +
            "ORDER BY updatedAtMillis DESC"
    )
    fun observeProjectsWithShots(ownerId: String): Flow<List<ProjectWithShots>>

    @Query("SELECT * FROM projects WHERE ownerId = :ownerId")
    suspend fun getAllProjectsForOwner(ownerId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM shots WHERE projectId = :projectId ORDER BY sortOrder ASC, createdAtMillis ASC")
    suspend fun getShotsForProject(projectId: String): List<ShotEntity>

    @Query("SELECT * FROM shots WHERE ownerId = :ownerId")
    suspend fun getAllShotsForOwner(ownerId: String): List<ShotEntity>

    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :ownerId AND syncStatus != 'SYNCED'")
    fun observePendingProjectCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM shots WHERE ownerId = :ownerId AND syncStatus != 'SYNCED'")
    fun observePendingShotCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM projects WHERE ownerId = :ownerId AND syncStatus != 'SYNCED'")
    suspend fun getPendingProjectCount(ownerId: String): Int

    @Query("SELECT COUNT(*) FROM shots WHERE ownerId = :ownerId AND syncStatus != 'SYNCED'")
    suspend fun getPendingShotCount(ownerId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShot(shot: ShotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShots(shots: List<ShotEntity>)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("DELETE FROM shots WHERE id = :shotId")
    suspend fun deleteShotById(shotId: String)

    @Query("DELETE FROM shots WHERE projectId = :projectId")
    suspend fun deleteShotsByProjectId(projectId: String)

    @Query("UPDATE projects SET ownerId = :newOwnerId, syncStatus = 'PENDING_UPLOAD' WHERE ownerId = :oldOwnerId")
    suspend fun reassignProjectsOwner(oldOwnerId: String, newOwnerId: String)

    @Query("UPDATE shots SET ownerId = :newOwnerId, syncStatus = 'PENDING_UPLOAD' WHERE ownerId = :oldOwnerId")
    suspend fun reassignShotsOwner(oldOwnerId: String, newOwnerId: String)

    @Query("DELETE FROM projects WHERE ownerId = :ownerId AND syncStatus = 'SYNCED'")
    suspend fun clearSyncedProjectsForOwner(ownerId: String)

    @Query("DELETE FROM projects WHERE ownerId = :ownerId")
    suspend fun clearAllProjectsForOwner(ownerId: String)

    @Query("DELETE FROM shots WHERE ownerId = :ownerId")
    suspend fun clearAllShotsForOwner(ownerId: String)
}
