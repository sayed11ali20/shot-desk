package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProjectStatus
import com.example.data.local.ProjectWithShots
import com.example.data.local.SessionPreferences
import com.example.data.local.ShotEntity
import com.example.data.local.UserSession
import com.example.data.remote.CloudSyncDataSource
import com.example.data.repository.CacheStats
import com.example.data.repository.ProductionRepository
import com.example.data.repository.SyncIndicatorState
import com.example.ui.i18n.stringsForLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val route: String, val label: String) {
    CREATE("create", "Create"),
    PLAN("plan", "Plan"),
    WORK("work", "Work"),
    SETTINGS("settings", "Settings"),
}

enum class PlanFilter(val label: String) {
    ALL("All"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShotDeskViewModel(
    private val repository: ProductionRepository,
    private val sessionPreferences: SessionPreferences,
    private val cloudSyncDataSource: CloudSyncDataSource,
    private val auth: FirebaseAuth,
) : ViewModel() {

    val session: StateFlow<UserSession> = sessionPreferences.sessionFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserSession(),
        )

    val firebaseUser: StateFlow<FirebaseUser?> = auth.authStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = auth.currentUser,
        )

    val activeOwnerId: StateFlow<String> = combine(session, firebaseUser) { sess, fbUser ->
        fbUser?.uid ?: sess.localUserId
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = auth.currentUser?.uid ?: "",
    )

    val projects: StateFlow<List<ProjectWithShots>> = activeOwnerId
        .flatMapLatest { ownerId ->
            if (ownerId.isBlank()) flowOf(emptyList()) else repository.observeProjects(ownerId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val syncIndicatorState: StateFlow<SyncIndicatorState> = activeOwnerId
        .flatMapLatest { ownerId ->
            if (ownerId.isBlank()) flowOf(SyncIndicatorState.SAVED_LOCALLY)
            else repository.observeSyncIndicatorState(ownerId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SyncIndicatorState.SAVED_LOCALLY,
        )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _selectedCreateProjectId = MutableStateFlow<String?>(null)
    val selectedCreateProjectId: StateFlow<String?> = _selectedCreateProjectId.asStateFlow()

    private val _editingShotId = MutableStateFlow<String?>(null)
    val editingShotId: StateFlow<String?> = _editingShotId.asStateFlow()

    private val _selectedWorkProjectId = MutableStateFlow<String?>(null)
    val selectedWorkProjectId: StateFlow<String?> = _selectedWorkProjectId.asStateFlow()

    private val _selectedDetailProjectId = MutableStateFlow<String?>(null)
    val selectedDetailProjectId: StateFlow<String?> = _selectedDetailProjectId.asStateFlow()

    private val _planSearchQuery = MutableStateFlow("")
    val planSearchQuery: StateFlow<String> = _planSearchQuery.asStateFlow()

    private val _planFilter = MutableStateFlow(PlanFilter.ALL)
    val planFilter: StateFlow<PlanFilter> = _planFilter.asStateFlow()

    private val _cacheStats = MutableStateFlow(
        CacheStats(
            totalBytes = 0L,
            pendingChangesCount = 0,
            isCloudLinked = auth.currentUser != null,
            isOnline = repository.isOnline.value,
            lastSyncMillis = 0L,
        )
    )
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            projects.collect { list ->
                if (_selectedCreateProjectId.value != null && list.none { it.project.id == _selectedCreateProjectId.value }) {
                    _selectedCreateProjectId.value = list.firstOrNull()?.project?.id
                } else if (_selectedCreateProjectId.value == null && list.isNotEmpty()) {
                    _selectedCreateProjectId.value = list.first().project.id
                }

                if (_selectedWorkProjectId.value != null && list.none { it.project.id == _selectedWorkProjectId.value }) {
                    _selectedWorkProjectId.value = list.firstOrNull()?.project?.id
                } else if (_selectedWorkProjectId.value == null && list.isNotEmpty()) {
                    _selectedWorkProjectId.value = list.first().project.id
                }

                if (_selectedDetailProjectId.value != null && list.none { it.project.id == _selectedDetailProjectId.value }) {
                    _selectedDetailProjectId.value = null
                }
                refreshCacheStats()
            }
        }
        viewModelScope.launch {
            combine(firebaseUser, repository.isOnline, session) { _, _, _ -> Unit }.collect {
                refreshCacheStats()
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun createLocalAccount(fullName: String, nickname: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            sessionPreferences.createLocalAccount(fullName, nickname, password)
        }
    }

    fun logInLocalAccount(nickname: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            val result = sessionPreferences.verifyAndLogInLocalAccount(nickname, password)
            if (result.isFailure) {
                _authError.value = result.exceptionOrNull()?.message ?: "Log in failed"
            }
        }
    }

    fun onGoogleSignInCompleted(user: FirebaseUser) {
        viewModelScope.launch {
            _authError.value = null
            val currentSession = session.value
            val previousLocalId = currentSession.localUserId
            sessionPreferences.activateGoogleSession(
                firebaseUid = user.uid,
                displayName = user.displayName,
                email = user.email,
            )
            if (previousLocalId.isNotBlank() && previousLocalId != user.uid) {
                repository.migrateLocalOfflineDataToFirebaseUid(previousLocalId, user.uid)
            }
            val cloudProfile = cloudSyncDataSource.getUserProfile().getOrNull()
            if (cloudProfile != null && currentSession.fullName.isBlank()) {
                sessionPreferences.updateProfile(
                    fullName = cloudProfile.fullName,
                    nickname = cloudProfile.nickname,
                    newPassword = null,
                )
            }
            val syncRes = repository.syncWithCloud()
            if (syncRes.isSuccess) {
                _snackbarMessage.value = "Cloud sync enabled"
            }
            refreshCacheStats()
        }
    }

    fun onGoogleSignInError(message: String) {
        _authError.value = message
        _snackbarMessage.value = message
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences.setDarkMode(enabled)
        }
    }

    fun setLanguageCode(code: String) {
        viewModelScope.launch {
            sessionPreferences.setLanguageCode(code)
        }
    }

    private fun currentStrings() = stringsForLanguage(session.value.languageCode)

    fun selectCreateProject(projectId: String) {
        _selectedCreateProjectId.value = projectId
        _editingShotId.value = null
    }

    fun startEditingShot(shot: ShotEntity) {
        _selectedCreateProjectId.value = shot.projectId
        _editingShotId.value = shot.id
    }

    fun cancelEditingShot() {
        _editingShotId.value = null
    }

    fun selectWorkProject(projectId: String) {
        _selectedWorkProjectId.value = projectId
    }

    fun openProjectDetail(projectId: String?) {
        _selectedDetailProjectId.value = projectId
    }

    fun updatePlanSearchQuery(query: String) {
        _planSearchQuery.value = query
    }

    fun updatePlanFilter(filter: PlanFilter) {
        _planFilter.value = filter
    }

    fun createProject(title: String, onCreated: (String) -> Unit = {}) {
        val ownerId = activeOwnerId.value
        if (ownerId.isBlank()) return
        viewModelScope.launch {
            val id = repository.createProject(ownerId, title)
            _selectedCreateProjectId.value = id
            _editingShotId.value = null
            _snackbarMessage.value = currentStrings().msgProjectCreated(title)
            onCreated(id)
        }
    }

    fun finishWorkAndSaveProject(
        projectId: String,
        pendingShotId: String?,
        pendingShotNumber: String,
        pendingShotType: String,
        pendingDialogue: String,
        pendingVisuals: String,
        pendingVfx: String,
        pendingSfx: String,
        pendingNotes: String,
        pendingDurationSeconds: Int,
        onFinished: (String) -> Unit,
    ) {
        val ownerId = activeOwnerId.value
        if (ownerId.isBlank()) return
        viewModelScope.launch {
            val hasTypedContent = pendingDialogue.isNotBlank() ||
                pendingVisuals.isNotBlank() ||
                pendingVfx.isNotBlank() ||
                pendingSfx.isNotBlank() ||
                pendingNotes.isNotBlank() ||
                pendingShotId != null

            if (hasTypedContent) {
                repository.saveShot(
                    ownerId = ownerId,
                    projectId = projectId,
                    shotId = pendingShotId,
                    shotNumber = pendingShotNumber,
                    shotType = pendingShotType,
                    dialogueScript = pendingDialogue,
                    visualDescription = pendingVisuals,
                    vfxNotes = pendingVfx,
                    sfxMusicNotes = pendingSfx,
                    directorsNotes = pendingNotes,
                    durationSeconds = pendingDurationSeconds,
                )
            }
            _editingShotId.value = null
            _selectedDetailProjectId.value = projectId
            _selectedWorkProjectId.value = projectId
            if (auth.currentUser != null && repository.isOnline.value) {
                repository.syncWithCloud()
            }
            _snackbarMessage.value = currentStrings().msgProjectFinalized
            onFinished(projectId)
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateProjectTitle(projectId, newTitle)
            _snackbarMessage.value = currentStrings().msgProjectFinalized
        }
    }

    fun toggleProjectStatus(projectId: String, currentStatus: String) {
        viewModelScope.launch {
            val next = if (currentStatus == ProjectStatus.COMPLETED) {
                ProjectStatus.IN_PROGRESS
            } else {
                ProjectStatus.COMPLETED
            }
            repository.setProjectStatus(projectId, next)
            val s = currentStrings()
            _snackbarMessage.value = if (next == ProjectStatus.COMPLETED) {
                s.statusCompleted
            } else {
                s.statusInProgress
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_selectedDetailProjectId.value == projectId) {
                _selectedDetailProjectId.value = null
            }
            _snackbarMessage.value = currentStrings().msgProjectDeleted
        }
    }

    fun saveShotAndContinue(
        projectId: String,
        shotId: String?,
        shotNumber: String,
        shotType: String,
        dialogueScript: String,
        visualDescription: String,
        vfxNotes: String,
        sfxMusicNotes: String,
        directorsNotes: String,
        durationSeconds: Int,
        onSaved: () -> Unit,
    ) {
        val ownerId = activeOwnerId.value
        if (ownerId.isBlank()) return
        viewModelScope.launch {
            repository.saveShot(
                ownerId = ownerId,
                projectId = projectId,
                shotId = shotId,
                shotNumber = shotNumber,
                shotType = shotType,
                dialogueScript = dialogueScript,
                visualDescription = visualDescription,
                vfxNotes = vfxNotes,
                sfxMusicNotes = sfxMusicNotes,
                directorsNotes = directorsNotes,
                durationSeconds = durationSeconds,
            )
            _editingShotId.value = null
            val s = currentStrings()
            _snackbarMessage.value = if (shotId != null) s.msgShotUpdated else s.msgShotSavedNext
            onSaved()
        }
    }

    fun duplicateShot(shot: ShotEntity) {
        viewModelScope.launch {
            repository.duplicateShot(shot)
            _snackbarMessage.value = currentStrings().msgShotDuplicated
        }
    }

    fun moveShot(projectId: String, shotId: String, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveShot(projectId, shotId, moveUp)
        }
    }

    fun deleteShot(shot: ShotEntity) {
        viewModelScope.launch {
            if (_editingShotId.value == shot.id) {
                _editingShotId.value = null
            }
            repository.deleteShot(shot)
            _snackbarMessage.value = currentStrings().msgShotRemoved
        }
    }

    fun toggleShotCompleted(shot: ShotEntity) {
        viewModelScope.launch {
            repository.toggleShotCompleted(shot)
        }
    }

    fun forceSyncNow() {
        viewModelScope.launch {
            val result = repository.syncWithCloud()
            if (result.isSuccess) {
                _snackbarMessage.value = currentStrings().msgCloudSyncComplete
            } else {
                val msg = result.exceptionOrNull()?.message ?: currentStrings().syncPaused
                Log.w("ShotDeskVM", "Force sync notice: $msg")
                _snackbarMessage.value = msg
            }
            refreshCacheStats()
        }
    }

    fun clearLocalCache(includeUnsynced: Boolean) {
        val ownerId = activeOwnerId.value
        if (ownerId.isBlank()) return
        viewModelScope.launch {
            repository.clearLocalCache(ownerId, includeUnsynced)
            refreshCacheStats()
            val s = currentStrings()
            _snackbarMessage.value = if (includeUnsynced) {
                s.msgAllCacheCleared
            } else {
                s.msgSyncedCacheCleared
            }
        }
    }

    fun updateProfile(fullName: String, nickname: String, newPassword: String?) {
        viewModelScope.launch {
            sessionPreferences.updateProfile(fullName, nickname, newPassword)
            if (auth.currentUser != null && repository.isOnline.value) {
                cloudSyncDataSource.upsertUserProfile(fullName, nickname)
            }
            _snackbarMessage.value = currentStrings().msgProfileUpdated
        }
    }

    fun logOutLocalSession() {
        viewModelScope.launch {
            sessionPreferences.logOutKeepCredentials()
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            val ownerId = activeOwnerId.value
            val bytes = repository.getLocalCacheSizeBytes()
            val pending = if (ownerId.isBlank()) 0 else repository.getPendingChangesCount(ownerId)
            _cacheStats.value = CacheStats(
                totalBytes = bytes,
                pendingChangesCount = pending,
                isCloudLinked = auth.currentUser != null,
                isOnline = repository.isOnline.value,
                lastSyncMillis = session.value.lastSyncMillis,
            )
        }
    }
}
