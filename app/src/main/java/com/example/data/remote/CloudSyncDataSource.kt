package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.data.local.ShotEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncDataSource(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    constructor(context: Context, auth: FirebaseAuth) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before accessing Cloud Firestore.")
    }

    suspend fun upsertUserProfile(fullName: String, nickname: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "fullName" to fullName.trim().take(80),
                        "nickname" to nickname.trim().take(40),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "fullName" to fullName.trim().take(80),
                        "nickname" to nickname.trim().take(40),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<UserProfileDto?> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            val dto = snap.toObject(UserProfileDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(dto)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertProject(project: ProjectEntity): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(project.id)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "title" to project.title.trim().take(120),
                        "status" to project.status,
                        "updatedAtMillis" to project.updatedAtMillis,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to project.title.trim().take(120),
                        "status" to project.status,
                        "createdAtMillis" to project.createdAtMillis,
                        "updatedAtMillis" to project.updatedAtMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val projRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            val shotsSnap = projRef.collection("shots").get().await()
            val batch = db.batch()
            for (shotDoc in shotsSnap.documents) {
                batch.delete(shotDoc.reference)
            }
            batch.delete(projRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, projRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertShot(shot: ShotEntity): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(shot.projectId)
            .collection("shots").document(shot.id)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "shotNumber" to shot.shotNumber.trim().take(40).ifBlank { "Shot" },
                        "sortOrder" to shot.sortOrder,
                        "shotType" to shot.shotType.trim().take(60).ifBlank { "Wide Shot" },
                        "dialogueScript" to shot.dialogueScript.take(5000),
                        "visualDescription" to shot.visualDescription.take(3000),
                        "vfxNotes" to shot.vfxNotes.take(2000),
                        "sfxMusicNotes" to shot.sfxMusicNotes.take(2000),
                        "directorsNotes" to shot.directorsNotes.take(2000),
                        "durationSeconds" to shot.durationSeconds.coerceIn(0, 86400),
                        "isCompleted" to shot.isCompleted,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "projectId" to shot.projectId,
                        "shotNumber" to shot.shotNumber.trim().take(40).ifBlank { "Shot" },
                        "sortOrder" to shot.sortOrder,
                        "shotType" to shot.shotType.trim().take(60).ifBlank { "Wide Shot" },
                        "dialogueScript" to shot.dialogueScript.take(5000),
                        "visualDescription" to shot.visualDescription.take(3000),
                        "vfxNotes" to shot.vfxNotes.take(2000),
                        "sfxMusicNotes" to shot.sfxMusicNotes.take(2000),
                        "directorsNotes" to shot.directorsNotes.take(2000),
                        "durationSeconds" to shot.durationSeconds.coerceIn(0, 86400),
                        "isCompleted" to shot.isCompleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteShot(projectId: String, shotId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("shots").document(shotId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllProjects(): Result<List<ProjectDto>> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("projects")
        return try {
            val snap = colRef.get().await()
            val items = snap.toObjects(ProjectDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchShotsForProject(projectId: String): Result<List<ShotDto>> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid)
            .collection("projects").document(projectId)
            .collection("shots")
        return try {
            val snap = colRef.get().await()
            val items = snap.toObjects(ShotDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    fun observeCloudProjects(): Flow<List<ProjectDto>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ProjectDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }
}
