package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val nickname: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ProjectDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val status: String = "in_progress",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ShotDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val shotNumber: String = "",
    val sortOrder: Long = 0L,
    val shotType: String = "",
    val dialogueScript: String = "",
    val visualDescription: String = "",
    val vfxNotes: String = "",
    val sfxMusicNotes: String = "",
    val directorsNotes: String = "",
    val durationSeconds: Long = 0L,
    val isCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)
