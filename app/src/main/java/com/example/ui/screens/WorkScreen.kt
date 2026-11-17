package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProjectWithShots
import com.example.data.local.ShotEntity
import com.example.ui.components.ProductionShotTableFooter
import com.example.ui.components.ProductionShotTableHeaderRow
import com.example.ui.components.ProductionShotTableRow
import com.example.ui.components.computeShotTableMetrics
import com.example.ui.components.formatDuration
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkScreen(
    projects: List<ProjectWithShots>,
    selectedProjectId: String?,
    isDarkTheme: Boolean,
    onSelectProject: (String) -> Unit,
    onToggleShotCompleted: (ShotEntity) -> Unit,
) {
    val s = LocalAppStrings.current
    val activeProject = projects.firstOrNull { it.project.id == selectedProjectId } ?: projects.firstOrNull()
    val tableHorizontalScroll = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (projects.isEmpty() || activeProject == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = s.workEmptyTitle,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s.workEmptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@BoxWithConstraints
        }

        val containerMaxWidth = maxWidth.coerceAtMost(1280.dp)
        val tableMetrics = remember(containerMaxWidth) {
            computeShotTableMetrics(containerMaxWidth - 32.dp)
        }

        val shots = activeProject.activeShots
        val filmedCount = shots.count { it.isCompleted }
        val totalCount = shots.size
        val percent = if (totalCount == 0) 0 else ((filmedCount * 100f) / totalCount).toInt()
        val progressFraction = if (totalCount == 0) 0f else filmedCount.toFloat() / totalCount.toFloat()
        val remainingDuration = shots.filter { !it.isCompleted }.sumOf { it.durationSeconds.coerceAtLeast(0) }

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1280.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    projects.forEach { item ->
                        val selected = item.project.id == activeProject.project.id
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectProject(item.project.id) },
                            label = {
                                Text(
                                    text = item.project.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("work_progress_card"),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = s.workProgressText(filmedCount, totalCount, percent),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.workRemainingLeft(formatDuration(remainingDuration)),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = if (s.isArabic) MaterialTheme.typography.labelSmall.fontFamily else JetBrainsMonoFontFamily,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = if (percent == 100 && totalCount > 0) FilmedEmerald else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50)),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = s.workTapHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (shots.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = s.workNoShotsInProject(activeProject.project.title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                stickyHeader(key = "work_table_header") {
                    ProductionShotTableHeaderRow(
                        metrics = tableMetrics,
                        horizontalScrollState = tableHorizontalScroll,
                        interactiveMode = true,
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
                        interactiveMode = true,
                        onToggleStatus = { onToggleShotCompleted(shot) },
                    )
                }

                item(key = "work_table_footer") {
                    ProductionShotTableFooter(
                        shots = shots,
                        metrics = tableMetrics,
                        horizontalScrollState = tableHorizontalScroll,
                        interactiveMode = true,
                    )
                }
            }
        }
    }
}
