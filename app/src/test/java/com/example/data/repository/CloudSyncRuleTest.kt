package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectStatus
import com.example.data.local.ShotEntity
import com.example.data.remote.CloudSyncDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudSyncRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun owner_canCreateProjectAndShot_andToggleShotCompletion() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val cloud = CloudSyncDataSource(firestore, auth)
        val projId = "proj_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val shotId = "shot_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()

        val project = ProjectEntity(
            id = projId,
            ownerId = uid,
            title = "YouTube Ep. 1",
            status = ProjectStatus.IN_PROGRESS,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        val shot = ShotEntity(
            id = shotId,
            projectId = projId,
            ownerId = uid,
            shotNumber = "Scene 1 - Shot 1",
            sortOrder = 0,
            shotType = "Wide Shot",
            dialogueScript = "Welcome back to the channel!",
            visualDescription = "Slow dolly push toward desk.",
            vfxNotes = "35mm grain overlay",
            sfxMusicNotes = "Subtle synth bed",
            directorsNotes = "Check key light",
            durationSeconds = 15,
            isCompleted = false,
            createdAtMillis = now,
            updatedAtMillis = now,
        )

        val projResult = withTimeout(DEFAULT_TIMEOUT_MS) { cloud.upsertProject(project) }
        assertTrue(projResult.isSuccess)

        val shotResult = withTimeout(DEFAULT_TIMEOUT_MS) { cloud.upsertShot(shot) }
        assertTrue(shotResult.isSuccess)

        // Toggle shot completed and revert back for reshoot
        val toggleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            cloud.upsertShot(shot.copy(isCompleted = true))
        }
        assertTrue(toggleResult.isSuccess)

        val revertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            cloud.upsertShot(shot.copy(isCompleted = false))
        }
        assertTrue(revertResult.isSuccess)

        val projects = withTimeout(DEFAULT_TIMEOUT_MS) { cloud.fetchAllProjects().getOrThrow() }
        assertTrue(projects.any { it.id == projId && it.title == "YouTube Ep. 1" })

        val shots = withTimeout(DEFAULT_TIMEOUT_MS) { cloud.fetchShotsForProject(projId).getOrThrow() }
        assertTrue(shots.any { it.id == shotId && !it.isCompleted })
    }

    @Test
    fun unauthenticatedCaller_observeCloudProjects_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val cloud = CloudSyncDataSource(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                cloud.observeCloudProjects().first()
            }
        } catch (e: Throwable) {
            caught = e
        }
        assertTrue("Expected unauthenticated failure", caught != null)
        // Either fail-fast IllegalStateException or PERMISSION_DENIED from Firestore
        val firestoreEx = caught?.unwrapFirestoreException()
        if (firestoreEx != null) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        } else {
            assertTrue(caught is IllegalStateException)
        }
    }

    @Test
    fun crossUser_cannotReadAnotherDirectorsProject() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceCloud = CloudSyncDataSource(firestore, auth)
        val projId = "proj_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val now = System.currentTimeMillis()
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceCloud.upsertProject(
                ProjectEntity(
                    id = projId,
                    ownerId = aliceUid,
                    title = "Alice Confidential Script",
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
            ).getOrThrow()
        }

        // Switch to Bob and try reading Alice's document directly
        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("projects").document(projId)
                    .get()
                    .let { com.google.android.gms.tasks.Tasks.await(it) }
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Expected PERMISSION_DENIED when Bob reads Alice's project", denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_director@shotdesk.test"
        const val BOB_EMAIL = "bob_director@shotdesk.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
