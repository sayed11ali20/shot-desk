package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
}

object ProjectStatus {
    const val IN_PROGRESS = "in_progress"
    const val COMPLETED = "completed"
}

object ShotTypePresets {
    val presets = listOf(
        "Wide Shot",
        "Medium Shot",
        "Close-Up",
        "Extreme Close-Up",
        "Over-the-Shoulder",
        "B-Roll",
        "Drone/Aerial",
        "POV",
    )
    const val CUSTOM_OPTION = "Custom / Write your own"
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val status: String = ProjectStatus.IN_PROGRESS,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val serverCreatedAtMillis: Long? = null,
)

@Entity(
    tableName = "shots",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("ownerId")],
)
data class ShotEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val ownerId: String,
    val shotNumber: String,
    val sortOrder: Int,
    val shotType: String,
    val dialogueScript: String = "",
    val visualDescription: String = "",
    val vfxNotes: String = "",
    val sfxMusicNotes: String = "",
    val directorsNotes: String = "",
    val durationSeconds: Int = 5,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING_UPLOAD.name,
    val serverCreatedAtMillis: Long? = null,
)

data class ProjectWithShots(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId",
    )
    val rawShots: List<ShotEntity>,
) {
    val activeShots: List<ShotEntity>
        get() = rawShots
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            .sortedWith(compareBy<ShotEntity> { it.sortOrder }.thenBy { it.createdAtMillis })

    val totalDurationSeconds: Int
        get() = activeShots.sumOf { it.durationSeconds.coerceAtLeast(0) }

    val completedCount: Int
        get() = activeShots.count { it.isCompleted }
}
