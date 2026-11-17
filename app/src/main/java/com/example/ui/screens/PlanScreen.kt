package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectStatus
import com.example.data.local.ProjectWithShots
import com.example.ui.components.ExportHelper
import com.example.ui.components.ProductionShotTableFooter
import com.example.ui.components.ProductionShotTableHeaderRow
import com.example.ui.components.ProductionShotTableRow
import com.example.ui.components.computeShotTableMetrics
import com.example.ui.components.formatDuration
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PlanFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlanScreen(
    projects: List<ProjectWithShots>,
    searchQuery: String,
    activeFilter: PlanFilter,
    selectedDetailProjectId: String?,
    isDarkTheme: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (PlanFilter) -> Unit,
    onOpenDetail: (String?) -> Unit,
    onEditInCreate: (String) -> Unit,
    onStartShootingInWork: (String) -> Unit,
    onToggleProjectStatus: (projectId: String, currentStatus: String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onExportFeedback: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    val detailProject = projects.firstOrNull { it.project.id == selectedDetailProjectId }

    if (detailProject != null) {
        BackHandler {
            onOpenDetail(null)
        }
        ProjectDetailBreakdownView(
            projectWithShots = detailProject,
            isDarkTheme = isDarkTheme,
            onBack = { onOpenDetail(null) },
            onEditInCreate = { onEditInCreate(detailProject.project.id) },
            onStartShootingInWork = { onStartShootingInWork(detailProject.project.id) },
            onToggleStatus = { onToggleProjectStatus(detailProject.project.id, detailProject.project.status) },
            onDelete = { onDeleteProject(detailProject.project.id) },
            onExportFeedback = onExportFeedback,
        )
        return
    }

    val filteredProjects = remember(projects, searchQuery, activeFilter) {
        projects.filter { item ->
            val matchesFilter = when (activeFilter) {
                PlanFilter.ALL -> true
                PlanFilter.IN_PROGRESS -> item.project.status == ProjectStatus.IN_PROGRESS
                PlanFilter.COMPLETED -> item.project.status == ProjectStatus.COMPLETED
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.project.title.contains(q, ignoreCase = true) ||
                item.activeShots.any {
                    it.shotNumber.contains(q, ignoreCase = true) ||
                        it.dialogueScript.contains(q, ignoreCase = true) ||
                        it.visualDescription.contains(q, ignoreCase = true)
                }
            matchesFilter && matchesQuery
        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(s.searchPlaceholder) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_search_input"),
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    PlanFilter.entries.forEach { filter ->
                        val filterLabel = when (filter) {
                            PlanFilter.ALL -> s.filterAll
                            PlanFilter.IN_PROGRESS -> s.filterInProgress
                            PlanFilter.COMPLETED -> s.filterCompleted
                        }
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filterLabel) },
                            modifier = Modifier.testTag("plan_filter_${filter.name.lowercase()}"),
                        )
                    }
                }
            }

            if (filteredProjects.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MovieFilter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (projects.isEmpty()) s.emptyPlansTitle else s.emptyPlansMatchesTitle,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (projects.isEmpty()) s.emptyPlansHint else s.emptyPlansFilterHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredProjects,
                    key = { it.project.id },
                ) { item ->
                    ProjectPlanCard(
                        projectWithShots = item,
                        onClick = { onOpenDetail(item.project.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectPlanCard(
    projectWithShots: ProjectWithShots,
    onClick: () -> Unit,
) {
    val s = LocalAppStrings.current
    val project = projectWithShots.project
    val isCompleted = project.status == ProjectStatus.COMPLETED
    val dateFormatted = remember(project.createdAtMillis, s.isArabic) {
        val locale = if (s.isArabic) Locale("ar") else Locale.US
        SimpleDateFormat("MMM dd, yyyy", locale).format(Date(project.createdAtMillis))
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCompleted) FilmedEmerald.copy(alpha = 0.16f) else WarningAmber.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = if (isCompleted) s.statusCompleted else s.statusInProgress,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) FilmedEmerald else WarningAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = s.createdDateLabel(dateFormatted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = s.shotsCountPill(projectWithShots.activeShots.size),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDuration(projectWithShots.totalDurationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Text(
                    text = s.filmedCountLabel(projectWithShots.completedCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = FilmedEmerald,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectDetailBreakdownView(
    projectWithShots: ProjectWithShots,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onEditInCreate: () -> Unit,
    onStartShootingInWork: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onExportFeedback: (String) -> Unit,
) {
    val s = LocalAppStrings.current
    val context = LocalContext.current
    val project = projectWithShots.project
    val shots = projectWithShots.activeShots
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val tableHorizontalScroll = rememberScrollState()

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.deleteProjectTitle) },
            text = {
                Text(s.deleteProjectConfirmMsg(project.title, shots.size))
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(s.deleteButton)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(s.cancelButton)
                }
            },
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val containerMaxWidth = maxWidth.coerceAtMost(1280.dp)
        val tableMetrics = remember(containerMaxWidth) {
            computeShotTableMetrics(containerMaxWidth - 32.dp)
        }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = s.cdBack,
                            )
                        }
                        Column {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = s.shotsAndDurationSummary(
                                    shots.size,
                                    formatDuration(projectWithShots.totalDurationSeconds),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = s.deleteButton,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Button(
                        onClick = onStartShootingInWork,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.startShootingAction)
                    }

                    OutlinedButton(
                        onClick = onEditInCreate,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.editShotsAction)
                    }

                    OutlinedButton(
                        onClick = onToggleStatus,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (project.status == ProjectStatus.COMPLETED) s.markInProgressAction else s.markCompletedAction
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                ExportHelper.exportAndSharePdf(context, projectWithShots, s)
                            } catch (e: Exception) {
                                onExportFeedback("PDF export failed: ${e.localizedMessage}")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("export_pdf_button"),
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.exportPdfAction)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                ExportHelper.shareTextScript(context, projectWithShots, s)
                            } catch (e: Exception) {
                                onExportFeedback("Text export failed: ${e.localizedMessage}")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("export_text_button"),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.exportTextAction)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (shots.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = s.emptyBreakdownText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                stickyHeader(key = "plan_table_header") {
                    ProductionShotTableHeaderRow(
                        metrics = tableMetrics,
                        horizontalScrollState = tableHorizontalScroll,
                        interactiveMode = false,
                    )
                }

                itemsIndexed(
                    items = shots,
                    key = { _, shot -> shot.id },
                ) { index, shot ->
                    ProductionShotTableRow(
                        index = index,
                        shot = shot,
                        isDarkTheme = isDarkTheme,
                        metrics = tableMetrics,
                        horizontalScrollState = tableHorizontalScroll,
                        interactiveMode = false,
                    )
                }

                item(key = "plan_table_footer") {
                    ProductionShotTableFooter(
                        shots = shots,
                        metrics = tableMetrics,
                        horizontalScrollState = tableHorizontalScroll,
                        interactiveMode = false,
                    )
                }
            }
        }
    }
}
