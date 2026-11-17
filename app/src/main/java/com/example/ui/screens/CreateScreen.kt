package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectWithShots
import com.example.data.local.ShotEntity
import com.example.data.local.ShotTypePresets
import com.example.ui.components.FlexibleDurationPresetRow
import com.example.ui.components.FlexibleShotTypeSelector
import com.example.ui.components.FlexibleTagSelectorRow
import com.example.ui.components.ShotTypeBadge
import com.example.ui.components.formatDuration
import com.example.ui.components.toggleTagInText
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    projects: List<ProjectWithShots>,
    selectedProjectId: String?,
    editingShotId: String?,
    onSelectProject: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onSaveShot: (
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
    ) -> Unit,
    onFinishWorkAndSaveProject: (
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
        onFinished: () -> Unit,
    ) -> Unit,
    onEditShot: (ShotEntity) -> Unit,
    onCancelEditShot: () -> Unit,
    onDuplicateShot: (ShotEntity) -> Unit,
    onMoveShot: (projectId: String, shotId: String, moveUp: Boolean) -> Unit,
    onDeleteShot: (ShotEntity) -> Unit,
) {
    val s = LocalAppStrings.current
    val activeProject = projects.firstOrNull { it.project.id == selectedProjectId } ?: projects.firstOrNull()
    val editingShot = activeProject?.activeShots?.firstOrNull { it.id == editingShotId }

    var showNewProjectDialog by rememberSaveable { mutableStateOf(false) }
    var newProjectTitle by rememberSaveable { mutableStateOf("") }

    val nextAutoShotNumber = remember(activeProject?.activeShots?.size, s.isArabic) {
        val nextIdx = (activeProject?.activeShots?.size ?: 0) + 1
        s.autoShotPrefix(nextIdx)
    }

    var shotNumber by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.shotNumber ?: nextAutoShotNumber)
    }
    var shotTypeValue by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.shotType ?: ShotTypePresets.presets.first())
    }
    var durationInput by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf((editingShot?.durationSeconds ?: 10).toString())
    }
    var dialogueScript by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.dialogueScript ?: "")
    }
    var visualDescription by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.visualDescription ?: "")
    }
    var vfxNotes by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.vfxNotes ?: "")
    }
    var sfxMusicNotes by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.sfxMusicNotes ?: "")
    }
    var directorsNotes by rememberSaveable(activeProject?.project?.id, editingShotId) {
        mutableStateOf(editingShot?.directorsNotes ?: "")
    }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

    // User-added custom reusable options during session + discovered from existing shots
    val sessionCustomShotTypes = remember { mutableStateListOf<String>() }
    val sessionCustomMovements = remember { mutableStateListOf<String>() }
    val sessionCustomAngles = remember { mutableStateListOf<String>() }
    val sessionCustomVfx = remember { mutableStateListOf<String>() }
    val sessionCustomSfx = remember { mutableStateListOf<String>() }
    val sessionCustomDurations = remember { mutableStateListOf<Int>() }

    val discoveredCustomShotTypes = remember(projects, sessionCustomShotTypes.size) {
        val fromShots = projects
            .flatMap { it.activeShots }
            .map { it.shotType.trim() }
            .filter { it.isNotEmpty() && it !in ShotTypePresets.presets }
        (sessionCustomShotTypes + fromShots).distinctBy { it.lowercase() }
    }

    LaunchedEffect(editingShot?.id, activeProject?.activeShots?.size, s.isArabic) {
        if (editingShot != null) {
            shotNumber = editingShot.shotNumber
            shotTypeValue = editingShot.shotType
            durationInput = editingShot.durationSeconds.toString()
            dialogueScript = editingShot.dialogueScript
            visualDescription = editingShot.visualDescription
            vfxNotes = editingShot.vfxNotes
            sfxMusicNotes = editingShot.sfxMusicNotes
            directorsNotes = editingShot.directorsNotes
        } else if (shotNumber.isBlank() || shotNumber.startsWith("Scene 1 - Shot") || shotNumber.startsWith("المشهد 1 - اللقطة")) {
            shotNumber = nextAutoShotNumber
        }
    }

    fun resetForNextShot(nextCount: Int) {
        shotNumber = s.autoShotPrefix(nextCount)
        shotTypeValue = ShotTypePresets.presets.first()
        durationInput = "10"
        dialogueScript = ""
        visualDescription = ""
        vfxNotes = ""
        sfxMusicNotes = ""
        directorsNotes = ""
        formError = null
    }

    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text(s.newProjectTitle) },
            text = {
                OutlinedTextField(
                    value = newProjectTitle,
                    onValueChange = { newProjectTitle = it },
                    label = { Text(s.projectTitleLabel) },
                    placeholder = { Text(s.projectTitlePlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_title_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newProjectTitle.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreateProject(trimmed)
                            newProjectTitle = ""
                            showNewProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project_button"),
                ) {
                    Text(s.createButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text(s.cancelButton)
                }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Project Selector & Creation Bar
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = s.activeVideoProject,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedButton(
                                onClick = { showNewProjectDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("new_project_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(s.newProject)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (projects.isEmpty()) {
                            var quickTitle by rememberSaveable { mutableStateOf("") }
                            Text(
                                text = s.firstProjectHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                OutlinedTextField(
                                    value = quickTitle,
                                    onValueChange = { quickTitle = it },
                                    label = { Text(s.projectNameLabel) },
                                    placeholder = { Text(s.projectTitlePlaceholder) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_project_name_input"),
                                )
                                Button(
                                    onClick = {
                                        if (quickTitle.trim().isNotEmpty()) {
                                            onCreateProject(quickTitle.trim())
                                            quickTitle = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("quick_project_create_button"),
                                ) {
                                    Text(s.startButton)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                projects.forEach { item ->
                                    val isSelected = item.project.id == activeProject?.project?.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectProject(item.project.id) },
                                        label = {
                                            Text(
                                                text = item.project.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                    )
                                }
                            }

                            if (activeProject != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = s.shotsPlannedCount(activeProject.activeShots.size),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = s.estRuntimeLabel(formatDuration(activeProject.totalDurationSeconds)),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = if (s.isArabic) MaterialTheme.typography.labelMedium.fontFamily else JetBrainsMonoFontFamily,
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shot Entry Form (Text & Planning Only)
            if (activeProject != null) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (editingShot != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.MovieCreation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (editingShot != null) s.editShotHeader else s.addShotHeader,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                if (editingShot != null) {
                                    TextButton(onClick = {
                                        onCancelEditShot()
                                        resetForNextShot(activeProject.activeShots.size + 1)
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(s.cancelEdit)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = shotNumber,
                                onValueChange = { shotNumber = it },
                                label = { Text(s.shotNumberLabel) },
                                placeholder = { Text(s.shotNumberPlaceholder) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_number_input"),
                            )

                            FlexibleShotTypeSelector(
                                shotTypeValue = shotTypeValue,
                                customShotTypes = discoveredCustomShotTypes,
                                onShotTypeChange = {
                                    shotTypeValue = it
                                    formError = null
                                },
                                onAddCustomShotType = { newCustom ->
                                    if (sessionCustomShotTypes.none { it.equals(newCustom, ignoreCase = true) }) {
                                        sessionCustomShotTypes.add(0, newCustom)
                                    }
                                },
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = durationInput,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() } && input.length <= 5) {
                                            durationInput = input
                                        }
                                    },
                                    label = { Text(s.durationLabel) },
                                    placeholder = { Text(s.durationPlaceholder) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shot_duration_input"),
                                )
                                FlexibleDurationPresetRow(
                                    durationInput = durationInput,
                                    customDurations = sessionCustomDurations,
                                    onSelectDuration = { sec -> durationInput = sec.toString() },
                                    onAddCustomDuration = { sec ->
                                        if (sec !in sessionCustomDurations) {
                                            sessionCustomDurations.add(sec)
                                        }
                                    },
                                )
                            }

                            OutlinedTextField(
                                value = dialogueScript,
                                onValueChange = { dialogueScript = it },
                                label = { Text(s.dialogueLabel) },
                                placeholder = { Text(s.dialoguePlaceholder) },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_dialogue_input"),
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            FlexibleTagSelectorRow(
                                label = s.cameraMovementLabel,
                                presets = s.cameraMovementPresets,
                                customOptions = sessionCustomMovements,
                                currentFieldText = visualDescription,
                                testTagPrefix = "camera_movement",
                                onToggleTag = { tag ->
                                    visualDescription = toggleTagInText(visualDescription, tag)
                                },
                                onAddCustomOption = { custom ->
                                    if (sessionCustomMovements.none { it.equals(custom, ignoreCase = true) }) {
                                        sessionCustomMovements.add(0, custom)
                                    }
                                },
                            )

                            FlexibleTagSelectorRow(
                                label = s.cameraAngleLabel,
                                presets = s.cameraAnglePresets,
                                customOptions = sessionCustomAngles,
                                currentFieldText = visualDescription,
                                testTagPrefix = "camera_angle",
                                onToggleTag = { tag ->
                                    visualDescription = toggleTagInText(visualDescription, tag)
                                },
                                onAddCustomOption = { custom ->
                                    if (sessionCustomAngles.none { it.equals(custom, ignoreCase = true) }) {
                                        sessionCustomAngles.add(0, custom)
                                    }
                                },
                            )

                            OutlinedTextField(
                                value = visualDescription,
                                onValueChange = { visualDescription = it },
                                label = { Text(s.visualDescLabel) },
                                placeholder = { Text(s.visualDescPlaceholder) },
                                minLines = 2,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_visual_input"),
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            FlexibleTagSelectorRow(
                                label = s.vfxPresetsLabel,
                                presets = s.vfxPresetChips,
                                customOptions = sessionCustomVfx,
                                currentFieldText = vfxNotes,
                                testTagPrefix = "vfx_tag",
                                onToggleTag = { tag ->
                                    vfxNotes = toggleTagInText(vfxNotes, tag)
                                },
                                onAddCustomOption = { custom ->
                                    if (sessionCustomVfx.none { it.equals(custom, ignoreCase = true) }) {
                                        sessionCustomVfx.add(0, custom)
                                    }
                                },
                            )

                            OutlinedTextField(
                                value = vfxNotes,
                                onValueChange = { vfxNotes = it },
                                label = { Text(s.vfxLabel) },
                                placeholder = { Text(s.vfxPlaceholder) },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_vfx_input"),
                            )

                            FlexibleTagSelectorRow(
                                label = s.sfxPresetsLabel,
                                presets = s.sfxPresetChips,
                                customOptions = sessionCustomSfx,
                                currentFieldText = sfxMusicNotes,
                                testTagPrefix = "sfx_tag",
                                onToggleTag = { tag ->
                                    sfxMusicNotes = toggleTagInText(sfxMusicNotes, tag)
                                },
                                onAddCustomOption = { custom ->
                                    if (sessionCustomSfx.none { it.equals(custom, ignoreCase = true) }) {
                                        sessionCustomSfx.add(0, custom)
                                    }
                                },
                            )

                            OutlinedTextField(
                                value = sfxMusicNotes,
                                onValueChange = { sfxMusicNotes = it },
                                label = { Text(s.sfxLabel) },
                                placeholder = { Text(s.sfxPlaceholder) },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_sfx_input"),
                            )

                            OutlinedTextField(
                                value = directorsNotes,
                                onValueChange = { directorsNotes = it },
                                label = { Text(s.directorsNotesLabel) },
                                placeholder = { Text(s.directorsNotesPlaceholder) },
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("shot_notes_input"),
                            )

                            if (!formError.isNullOrBlank()) {
                                Text(
                                    text = formError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            Button(
                                onClick = {
                                    val resolvedShotType = shotTypeValue.trim()
                                    if (resolvedShotType.isEmpty()) {
                                        formError = s.errCustomShotEmpty
                                        return@Button
                                    }
                                    if (resolvedShotType !in ShotTypePresets.presets &&
                                        sessionCustomShotTypes.none { it.equals(resolvedShotType, ignoreCase = true) }
                                    ) {
                                        sessionCustomShotTypes.add(0, resolvedShotType)
                                    }
                                    val resolvedDuration = (durationInput.toIntOrNull() ?: 0).coerceAtLeast(0)
                                    val currentCount = activeProject.activeShots.size
                                    onSaveShot(
                                        activeProject.project.id,
                                        editingShot?.id,
                                        shotNumber.trim().ifBlank { nextAutoShotNumber },
                                        resolvedShotType,
                                        dialogueScript.trim(),
                                        visualDescription.trim(),
                                        vfxNotes.trim(),
                                        sfxMusicNotes.trim(),
                                        directorsNotes.trim(),
                                        resolvedDuration,
                                    ) {
                                        val nextIndex = if (editingShot == null) currentCount + 2 else currentCount + 1
                                        resetForNextShot(nextIndex)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("save_shot_add_next_button"),
                            ) {
                                Text(
                                    text = if (editingShot != null) s.updateShot else s.saveShotAddNext,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                // Sequence Queue
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    ) {
                        Text(
                            text = s.shotSequenceTitle(activeProject.activeShots.size),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = s.totalDurationShort(formatDuration(activeProject.totalDurationSeconds)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (activeProject.activeShots.isEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = s.emptySequenceHint(activeProject.project.title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = activeProject.activeShots,
                        key = { _, shot -> shot.id },
                    ) { index, shot ->
                        CreateShotQueueCard(
                            shot = shot,
                            isFirst = index == 0,
                            isLast = index == activeProject.activeShots.lastIndex,
                            isEditing = shot.id == editingShotId,
                            onMoveUp = { onMoveShot(activeProject.project.id, shot.id, true) },
                            onMoveDown = { onMoveShot(activeProject.project.id, shot.id, false) },
                            onDuplicate = { onDuplicateShot(shot) },
                            onEdit = { onEditShot(shot) },
                            onDelete = { onDeleteShot(shot) },
                        )
                    }
                }

                // Prominent Finish Work & Save Project CTA at bottom of Create screen
                item {
                    Button(
                        onClick = {
                            val resolvedShotType = shotTypeValue.trim().ifBlank { ShotTypePresets.presets.first() }
                            val resolvedDuration = (durationInput.toIntOrNull() ?: 0).coerceAtLeast(0)
                            onFinishWorkAndSaveProject(
                                activeProject.project.id,
                                editingShot?.id,
                                shotNumber.trim().ifBlank { nextAutoShotNumber },
                                resolvedShotType,
                                dialogueScript.trim(),
                                visualDescription.trim(),
                                vfxNotes.trim(),
                                sfxMusicNotes.trim(),
                                directorsNotes.trim(),
                                resolvedDuration,
                            ) {
                                resetForNextShot(activeProject.activeShots.size + 1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FilmedEmerald,
                            contentColor = MaterialTheme.colorScheme.onTertiary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 12.dp)
                            .height(54.dp)
                            .testTag("finish_work_save_project_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.finishWorkSaveProject,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateShotQueueCard(
    shot: ShotEntity,
    isFirst: Boolean,
    isLast: Boolean,
    isEditing: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalAppStrings.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shot.shotNumber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ShotTypeBadge(shotType = shot.shotType)
                        Text(
                            text = "${shot.durationSeconds}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = s.cdMoveUp)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = s.cdMoveDown)
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = s.cdDuplicate)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = s.cdEdit)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = s.cdDelete,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (shot.visualDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shot.visualDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (shot.dialogueScript.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${shot.dialogueScript}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
