package com.example.data.repository

import android.content.Context
import com.example.data.local.ProductionDao
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.ProjectWithShots
import com.example.data.local.SessionPreferences
import com.example.data.local.ShotDeskDatabase
import com.example.data.local.ShotEntity
import com.example.data.local.SyncStatus
import com.example.data.remote.CloudSyncDataSource
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

enum class SyncIndicatorState {
    CLOUD_SYNCED,
    SYNCING,
    SAVED_LOCALLY,
    SYNC_ERROR,
}

data class CacheStats(
    val totalBytes: Long,
    val pendingChangesCount: Int,
    val isCloudLinked: Boolean,
    val isOnline: Boolean,
    val lastSyncMillis: Long,
)

class ProductionRepository(
    private val context: Context,
    private val dao: ProductionDao,
    private val cloud: CloudSyncDataSource,
    private val sessionPreferences: SessionPreferences,
    private val networkMonitor: NetworkMonitor,
    private val auth: FirebaseAuth,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncFailed = MutableStateFlow(false)

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    init {
        repoScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online && auth.currentUser != null) {
                    syncWithCloud()
                }
            }
        }
    }

    fun observeProjects(ownerId: String): Flow<List<ProjectWithShots>> {
        return dao.observeProjectsWithShots(ownerId)
    }

    fun observeSyncIndicatorState(ownerId: String): Flow<SyncIndicatorState> {
        return combine(
            _isSyncing,
            _lastSyncFailed,
            networkMonitor.isOnline,
            dao.observePendingProjectCount(ownerId),
            dao.observePendingShotCount(ownerId),
        ) { syncing, failed, online, pendingProj, pendingShots ->
            val hasCloudUser = auth.currentUser != null
            val hasPending = (pendingProj + pendingShots) > 0
            when {
                syncing -> SyncIndicatorState.SYNCING
                failed -> SyncIndicatorState.SYNC_ERROR
                hasCloudUser && online && !hasPending -> SyncIndicatorState.CLOUD_SYNCED
                else -> SyncIndicatorState.SAVED_LOCALLY
            }
        }
    }

    suspend fun getPendingChangesCount(ownerId: String): Int {
        return dao.getPendingProjectCount(ownerId) + dao.getPendingShotCount(ownerId)
    }

    suspend fun getLocalCacheSizeBytes(): Long {
        val dbFile = context.getDatabasePath(ShotDeskDatabase.DATABASE_NAME)
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        val dsFile = File(context.filesDir, "datastore/shotdesk_session.preferences_pb")
        return listOf(dbFile, walFile, shmFile, dsFile)
            .filter { it.exists() }
            .sumOf { it.length() }
    }

    suspend fun createProject(ownerId: String, title: String): String {
        val cleanTitle = title.trim().ifBlank { "Untitled Production" }.take(120)
        val id = generateCleanId("proj")
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = id,
            ownerId = ownerId,
            title = cleanTitle,
            status = ProjectStatus.IN_PROGRESS,
            createdAtMillis = now,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
        )
        dao.upsertProject(project)
        triggerBackgroundSyncIfEligible()
        return id
    }

    suspend fun updateProjectTitle(projectId: String, newTitle: String) {
        val existing = dao.getProjectById(projectId) ?: return
        val cleanTitle = newTitle.trim().ifBlank { existing.title }.take(120)
        dao.upsertProject(
            existing.copy(
                title = cleanTitle,
                updatedAtMillis = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
            )
        )
        triggerBackgroundSyncIfEligible()
    }

    suspend fun setProjectStatus(projectId: String, status: String) {
        val existing = dao.getProjectById(projectId) ?: return
        dao.upsertProject(
            existing.copy(
                status = status,
                updatedAtMillis = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
            )
        )
        triggerBackgroundSyncIfEligible()
    }

    suspend fun deleteProject(projectId: String) {
        val existing = dao.getProjectById(projectId) ?: return
        val now = System.currentTimeMillis()
        if (auth.currentUser != null && networkMonitor.isOnline.value) {
            dao.upsertProject(existing.copy(syncStatus = SyncStatus.PENDING_DELETE.name, updatedAtMillis = now))
            val result = cloud.deleteProject(projectId)
            if (result.isSuccess) {
                dao.deleteProjectById(projectId)
            }
        } else if (existing.serverCreatedAtMillis == null && auth.currentUser == null) {
            dao.deleteProjectById(projectId)
        } else {
            dao.upsertProject(existing.copy(syncStatus = SyncStatus.PENDING_DELETE.name, updatedAtMillis = now))
        }
    }

    suspend fun saveShot(
        ownerId: String,
        projectId: String,
        shotId: String? = null,
        shotNumber: String,
        shotType: String,
        dialogueScript: String,
        visualDescription: String,
        vfxNotes: String,
        sfxMusicNotes: String,
        directorsNotes: String,
        durationSeconds: Int,
    ): String {
        val now = System.currentTimeMillis()
        val existingShots = dao.getShotsForProject(projectId)
        val existingShot = if (shotId != null) existingShots.firstOrNull { it.id == shotId } else null

        val resolvedId = existingShot?.id ?: generateCleanId("shot")
        val sortOrder = existingShot?.sortOrder
            ?: (existingShots.filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)

        val entity = ShotEntity(
            id = resolvedId,
            projectId = projectId,
            ownerId = ownerId,
            shotNumber = shotNumber.trim().ifBlank { "Scene 1 - Shot ${sortOrder + 1}" }.take(40),
            sortOrder = sortOrder,
            shotType = shotType.trim().ifBlank { "Wide Shot" }.take(60),
            dialogueScript = dialogueScript.take(5000),
            visualDescription = visualDescription.take(3000),
            vfxNotes = vfxNotes.take(2000),
            sfxMusicNotes = sfxMusicNotes.take(2000),
            directorsNotes = directorsNotes.take(2000),
            durationSeconds = durationSeconds.coerceIn(0, 86400),
            isCompleted = existingShot?.isCompleted ?: false,
            createdAtMillis = existingShot?.createdAtMillis ?: now,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
            serverCreatedAtMillis = existingShot?.serverCreatedAtMillis,
        )
        dao.upsertShot(entity)
        touchProjectUpdated(projectId, now)
        triggerBackgroundSyncIfEligible()
        return resolvedId
    }

    suspend fun duplicateShot(shot: ShotEntity) {
        val now = System.currentTimeMillis()
        val siblings = dao.getShotsForProject(shot.projectId)
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            .sortedBy { it.sortOrder }

        val insertOrder = shot.sortOrder + 1
        val shifted = siblings.mapNotNull { s ->
            if (s.sortOrder >= insertOrder) {
                s.copy(sortOrder = s.sortOrder + 1, updatedAtMillis = now, syncStatus = SyncStatus.PENDING_UPLOAD.name)
            } else {
                null
            }
        }
        if (shifted.isNotEmpty()) {
            dao.upsertShots(shifted)
        }

        val copyShot = shot.copy(
            id = generateCleanId("shot"),
            shotNumber = "${shot.shotNumber} (Copy)".take(40),
            sortOrder = insertOrder,
            isCompleted = false,
            createdAtMillis = now,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
            serverCreatedAtMillis = null,
        )
        dao.upsertShot(copyShot)
        touchProjectUpdated(shot.projectId, now)
        triggerBackgroundSyncIfEligible()
    }

    suspend fun moveShot(projectId: String, shotId: String, moveUp: Boolean) {
        val activeShots = dao.getShotsForProject(projectId)
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            .sortedWith(compareBy<ShotEntity> { it.sortOrder }.thenBy { it.createdAtMillis })

        val index = activeShots.indexOfFirst { it.id == shotId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in activeShots.indices) return

        val reordered = activeShots.toMutableList()
        val item = reordered.removeAt(index)
        reordered.add(targetIndex, item)

        val now = System.currentTimeMillis()
        val updated = reordered.mapIndexed { idx, s ->
            s.copy(
                sortOrder = idx,
                updatedAtMillis = now,
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
            )
        }
        dao.upsertShots(updated)
        touchProjectUpdated(projectId, now)
        triggerBackgroundSyncIfEligible()
    }

    suspend fun toggleShotCompleted(shot: ShotEntity) {
        val now = System.currentTimeMillis()
        val newCompleted = !shot.isCompleted
        dao.upsertShot(
            shot.copy(
                isCompleted = newCompleted,
                updatedAtMillis = now,
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
            )
        )
        // Automatically update parent project status when all shots are filmed or reverted
        val siblings = dao.getShotsForProject(shot.projectId)
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
        val allCompleted = siblings.isNotEmpty() && siblings.all {
            if (it.id == shot.id) newCompleted else it.isCompleted
        }
        val project = dao.getProjectById(shot.projectId)
        if (project != null) {
            val targetStatus = if (allCompleted) ProjectStatus.COMPLETED else ProjectStatus.IN_PROGRESS
            if (project.status != targetStatus) {
                dao.upsertProject(
                    project.copy(
                        status = targetStatus,
                        updatedAtMillis = now,
                        syncStatus = SyncStatus.PENDING_UPLOAD.name,
                    )
                )
            }
        }
        triggerBackgroundSyncIfEligible()
    }

    suspend fun deleteShot(shot: ShotEntity) {
        val now = System.currentTimeMillis()
        if (auth.currentUser != null && networkMonitor.isOnline.value) {
            dao.upsertShot(shot.copy(syncStatus = SyncStatus.PENDING_DELETE.name, updatedAtMillis = now))
            val result = cloud.deleteShot(shot.projectId, shot.id)
            if (result.isSuccess) {
                dao.deleteShotById(shot.id)
            }
        } else if (shot.serverCreatedAtMillis == null && auth.currentUser == null) {
            dao.deleteShotById(shot.id)
        } else {
            dao.upsertShot(shot.copy(syncStatus = SyncStatus.PENDING_DELETE.name, updatedAtMillis = now))
        }
        touchProjectUpdated(shot.projectId, now)
    }

    suspend fun migrateLocalOfflineDataToFirebaseUid(localOwnerId: String, firebaseUid: String) {
        if (localOwnerId.isBlank() || localOwnerId == firebaseUid) return
        dao.reassignProjectsOwner(localOwnerId, firebaseUid)
        dao.reassignShotsOwner(localOwnerId, firebaseUid)
    }

    suspend fun syncWithCloud(): Result<Unit> = syncMutex.withLock {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(IllegalStateException("Connect Google Sign-In to sync with Cloud Firestore."))
        }
        if (!networkMonitor.isOnline.value) {
            return Result.failure(IllegalStateException("Device is offline. Changes remain safely stored locally."))
        }

        _isSyncing.value = true
        _lastSyncFailed.value = false
        return try {
            val uid = user.uid
            val session = sessionPreferences.sessionFlow.first()
            if (session.localUserId.isNotBlank() && session.localUserId != uid) {
                migrateLocalOfflineDataToFirebaseUid(session.localUserId, uid)
            }

            if (session.fullName.isNotBlank() && session.nickname.isNotBlank()) {
                cloud.upsertUserProfile(session.fullName, session.nickname)
            }

            // 1. Push pending project deletions & uploads first
            val localProjects = dao.getAllProjectsForOwner(uid)
            for (proj in localProjects) {
                when (proj.syncStatus) {
                    SyncStatus.PENDING_DELETE.name -> {
                        val res = cloud.deleteProject(proj.id)
                        if (res.isSuccess) {
                            dao.deleteProjectById(proj.id)
                        } else {
                            throw res.exceptionOrNull() ?: IllegalStateException("Project delete failed")
                        }
                    }
                    SyncStatus.PENDING_UPLOAD.name -> {
                        val res = cloud.upsertProject(proj)
                        if (res.isSuccess) {
                            dao.upsertProject(
                                proj.copy(
                                    syncStatus = SyncStatus.SYNCED.name,
                                    serverCreatedAtMillis = proj.serverCreatedAtMillis ?: proj.createdAtMillis,
                                )
                            )
                        } else {
                            throw res.exceptionOrNull() ?: IllegalStateException("Project upload failed")
                        }
                    }
                }
            }

            // 2. Push pending shot deletions & uploads
            val localShots = dao.getAllShotsForOwner(uid)
            for (shot in localShots) {
                when (shot.syncStatus) {
                    SyncStatus.PENDING_DELETE.name -> {
                        val res = cloud.deleteShot(shot.projectId, shot.id)
                        if (res.isSuccess) {
                            dao.deleteShotById(shot.id)
                        } else {
                            throw res.exceptionOrNull() ?: IllegalStateException("Shot delete failed")
                        }
                    }
                    SyncStatus.PENDING_UPLOAD.name -> {
                        val res = cloud.upsertShot(shot)
                        if (res.isSuccess) {
                            dao.upsertShot(
                                shot.copy(
                                    syncStatus = SyncStatus.SYNCED.name,
                                    serverCreatedAtMillis = shot.serverCreatedAtMillis ?: shot.createdAtMillis,
                                )
                            )
                        } else {
                            throw res.exceptionOrNull() ?: IllegalStateException("Shot upload failed")
                        }
                    }
                }
            }

            // 3. Pull latest cloud state for projects & shots where local has no pending edits
            val remoteProjectsResult = cloud.fetchAllProjects()
            if (remoteProjectsResult.isSuccess) {
                val remoteProjects = remoteProjectsResult.getOrThrow()
                val currentLocalProjects = dao.getAllProjectsForOwner(uid).associateBy { it.id }

                for (remoteProj in remoteProjects) {
                    val localProj = currentLocalProjects[remoteProj.id]
                    if (localProj == null || localProj.syncStatus == SyncStatus.SYNCED.name) {
                        dao.upsertProject(
                            ProjectEntity(
                                id = remoteProj.id,
                                ownerId = uid,
                                title = remoteProj.title,
                                status = remoteProj.status,
                                createdAtMillis = remoteProj.createdAtMillis.takeIf { it > 0L }
                                    ?: remoteProj.createdAt?.toDate()?.time
                                    ?: System.currentTimeMillis(),
                                updatedAtMillis = remoteProj.updatedAtMillis.takeIf { it > 0L }
                                    ?: remoteProj.updatedAt?.toDate()?.time
                                    ?: System.currentTimeMillis(),
                                syncStatus = SyncStatus.SYNCED.name,
                                serverCreatedAtMillis = remoteProj.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            )
                        )
                    }

                    val remoteShotsResult = cloud.fetchShotsForProject(remoteProj.id)
                    if (remoteShotsResult.isSuccess) {
                        val remoteShots = remoteShotsResult.getOrThrow()
                        val currentLocalShots = dao.getShotsForProject(remoteProj.id).associateBy { it.id }
                        for (remoteShot in remoteShots) {
                            val localShot = currentLocalShots[remoteShot.id]
                            if (localShot == null || localShot.syncStatus == SyncStatus.SYNCED.name) {
                                dao.upsertShot(
                                    ShotEntity(
                                        id = remoteShot.id,
                                        projectId = remoteProj.id,
                                        ownerId = uid,
                                        shotNumber = remoteShot.shotNumber,
                                        sortOrder = remoteShot.sortOrder.toInt(),
                                        shotType = remoteShot.shotType,
                                        dialogueScript = remoteShot.dialogueScript,
                                        visualDescription = remoteShot.visualDescription,
                                        vfxNotes = remoteShot.vfxNotes,
                                        sfxMusicNotes = remoteShot.sfxMusicNotes,
                                        directorsNotes = remoteShot.directorsNotes,
                                        durationSeconds = remoteShot.durationSeconds.toInt(),
                                        isCompleted = remoteShot.isCompleted,
                                        createdAtMillis = remoteShot.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                        updatedAtMillis = remoteShot.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                                        syncStatus = SyncStatus.SYNCED.name,
                                        serverCreatedAtMillis = remoteShot.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                                    )
                                )
                            }
                        }
                    }
                }
            }

            sessionPreferences.updateLastSyncMillis(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _lastSyncFailed.value = true
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun clearLocalCache(ownerId: String, includeUnsynced: Boolean) {
        if (includeUnsynced) {
            dao.clearAllShotsForOwner(ownerId)
            dao.clearAllProjectsForOwner(ownerId)
        } else {
            dao.clearSyncedProjectsForOwner(ownerId)
        }
    }

    private suspend fun touchProjectUpdated(projectId: String, now: Long) {
        val proj = dao.getProjectById(projectId) ?: return
        dao.upsertProject(
            proj.copy(
                updatedAtMillis = now,
                syncStatus = SyncStatus.PENDING_UPLOAD.name,
            )
        )
    }

    private fun triggerBackgroundSyncIfEligible() {
        if (auth.currentUser != null && networkMonitor.isOnline.value) {
            repoScope.launch {
                syncWithCloud()
            }
        }
    }

    private fun generateCleanId(prefix: String): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(16)
        return "${prefix}_$token"
    }
}
