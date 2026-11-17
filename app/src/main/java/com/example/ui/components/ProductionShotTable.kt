package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.ShotEntity
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.FilmedEmeraldLight
import com.example.ui.theme.FilmedTintDark
import com.example.ui.theme.FilmedTintLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ViewfinderCyan
import com.example.ui.theme.ViewfinderCyanLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Immutable
data class ShotTableMetrics(
    val shotAndTypeWidth: Dp,
    val visualsWidth: Dp,
    val scriptWidth: Dp,
    val vfxSfxWidth: Dp,
    val durationStatusWidth: Dp,
    val totalWidth: Dp,
    val needsHorizontalScroll: Boolean,
)

private const val BASE_SHOT_TYPE = 172f
private const val BASE_VISUALS = 248f
private const val BASE_SCRIPT = 232f
private const val BASE_VFX_SFX = 216f
private const val BASE_DURATION_STATUS = 156f
private const val BASE_TOTAL_TABLE_DP =
    BASE_SHOT_TYPE + BASE_VISUALS + BASE_SCRIPT + BASE_VFX_SFX + BASE_DURATION_STATUS

fun computeShotTableMetrics(availableWidth: Dp): ShotTableMetrics {
    val safeAvailable = availableWidth.coerceAtLeast(300.dp)
    val minTableWidth = BASE_TOTAL_TABLE_DP.dp
    val needsScroll = safeAvailable < minTableWidth
    val effectiveTotal = if (needsScroll) minTableWidth else safeAvailable
    val scale = effectiveTotal.value / BASE_TOTAL_TABLE_DP

    val col1 = (BASE_SHOT_TYPE * scale).dp
    val col2 = (BASE_VISUALS * scale).dp
    val col3 = (BASE_SCRIPT * scale).dp
    val col4 = (BASE_VFX_SFX * scale).dp
    val col5 = (effectiveTotal - col1 - col2 - col3 - col4).coerceAtLeast(148.dp)

    return ShotTableMetrics(
        shotAndTypeWidth = col1,
        visualsWidth = col2,
        scriptWidth = col3,
        vfxSfxWidth = col4,
        durationStatusWidth = col5,
        totalWidth = col1 + col2 + col3 + col4 + col5,
        needsHorizontalScroll = needsScroll,
    )
}

@Composable
fun ProductionShotTableHeaderRow(
    metrics: ShotTableMetrics,
    horizontalScrollState: ScrollState,
    interactiveMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val borderColor = MaterialTheme.colorScheme.outline
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
            .testTag("production_shot_table_header"),
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.width(metrics.totalWidth),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    HeaderCell(
                        title = s.colShotAndType,
                        width = metrics.shotAndTypeWidth,
                    )
                    VerticalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    HeaderCell(
                        title = s.colVisualsAndAction,
                        width = metrics.visualsWidth,
                    )
                    VerticalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    HeaderCell(
                        title = s.colScriptDialogue,
                        width = metrics.scriptWidth,
                    )
                    VerticalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    HeaderCell(
                        title = s.colVfxAndSfx,
                        width = metrics.vfxSfxWidth,
                    )
                    VerticalDivider(
                        thickness = 1.dp,
                        color = dividerColor,
                        modifier = Modifier.fillMaxHeight(),
                    )
                    HeaderCell(
                        title = if (interactiveMode) s.colDurationAndStatus else s.colDuration,
                        width = metrics.durationStatusWidth,
                    )
                }
                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(
    title: String,
    width: Dp,
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ProductionShotTableRow(
    index: Int,
    shot: ShotEntity,
    isDarkTheme: Boolean,
    metrics: ShotTableMetrics,
    horizontalScrollState: ScrollState,
    interactiveMode: Boolean,
    onToggleStatus: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val baseRowBg = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val completedBg = if (isDarkTheme) FilmedTintDark else FilmedTintLight
    val isMarkedCompleted = interactiveMode && shot.isCompleted

    val rowBg by animateColorAsState(
        targetValue = if (isMarkedCompleted) completedBg else baseRowBg,
        label = "shotTableRowBg",
    )

    val emeraldAccent = if (isDarkTheme) FilmedEmerald else FilmedEmeraldLight
    val cyanAccent = if (isDarkTheme) ViewfinderCyan else ViewfinderCyanLight
    val amberAccent = if (isDarkTheme) WarningAmber else WarningAmberLight
    val outerBorderColor = MaterialTheme.colorScheme.outline
    val innerGridColor = MaterialTheme.colorScheme.outlineVariant
    val startStripeColor by animateColorAsState(
        targetValue = if (isMarkedCompleted) emeraldAccent else outerBorderColor,
        label = "shotRowStartStripe",
    )

    val rowClickableModifier = if (interactiveMode && onToggleStatus != null) {
        Modifier.clickable(onClick = onToggleStatus)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState),
    ) {
        Column(
            modifier = Modifier
                .width(metrics.totalWidth)
                .background(rowBg)
                .then(rowClickableModifier)
                .testTag(if (interactiveMode) "work_shot_card_${shot.id}" else "plan_shot_row_${shot.id}"),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                // Start border / filmed indicator stripe
                Box(
                    modifier = Modifier
                        .width(if (isMarkedCompleted) 4.dp else 1.dp)
                        .fillMaxHeight()
                        .background(startStripeColor),
                )

                // Column 1: Shot # & Type
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .width(metrics.shotAndTypeWidth - if (isMarkedCompleted) 4.dp else 1.dp)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = if (s.isArabic) MaterialTheme.typography.labelSmall.fontFamily else JetBrainsMonoFontFamily,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = shot.shotNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                textDecoration = if (isMarkedCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = if (isMarkedCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    ShotTypeBadge(shotType = shot.shotType)
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = innerGridColor,
                    modifier = Modifier.fillMaxHeight(),
                )

                // Column 2: Visual Description & Camera Action (+ Director's Notes if prepared)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .width(metrics.visualsWidth)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (shot.visualDescription.isNotBlank()) {
                        Text(
                            text = shot.visualDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }

                    if (shot.directorsNotes.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(top = 2.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(13.dp),
                            )
                            Text(
                                text = "${s.notesPrefixShort}: ${shot.directorsNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = innerGridColor,
                    modifier = Modifier.fillMaxHeight(),
                )

                // Column 3: Spoken Script / Dialogue
                Box(
                    modifier = Modifier
                        .width(metrics.scriptWidth)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (shot.dialogueScript.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = shot.dialogueScript,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = if (s.isArabic) FontStyle.Normal else FontStyle.Italic,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = innerGridColor,
                    modifier = Modifier.fillMaxHeight(),
                )

                // Column 4: VFX & SFX Notes
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .width(metrics.vfxSfxWidth)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (shot.vfxNotes.isBlank() && shot.sfxMusicNotes.isBlank()) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    } else {
                        if (shot.vfxNotes.isNotBlank()) {
                            ProductionNoteTag(
                                icon = Icons.Rounded.Tune,
                                label = s.vfxPrefixShort,
                                text = shot.vfxNotes,
                                accentColor = cyanAccent,
                            )
                        }
                        if (shot.sfxMusicNotes.isNotBlank()) {
                            ProductionNoteTag(
                                icon = Icons.Rounded.MusicNote,
                                label = s.sfxPrefixShort,
                                text = shot.sfxMusicNotes,
                                accentColor = amberAccent,
                            )
                        }
                    }
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = innerGridColor,
                    modifier = Modifier.fillMaxHeight(),
                )

                // Column 5: Duration (Plan) or Duration & Interactive Status Toggle (Work)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .width(metrics.durationStatusWidth - 1.dp)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${shot.durationSeconds}s",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = if (s.isArabic) MaterialTheme.typography.labelMedium.fontFamily else JetBrainsMonoFontFamily,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (interactiveMode && onToggleStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (shot.isCompleted) {
                                emeraldAccent.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (shot.isCompleted) emeraldAccent else MaterialTheme.colorScheme.outline,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onToggleStatus)
                                .testTag("work_shot_checkbox_${shot.id}"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    imageVector = if (shot.isCompleted) {
                                        Icons.Rounded.CheckBox
                                    } else {
                                        Icons.Rounded.CheckBoxOutlineBlank
                                    },
                                    contentDescription = s.cdToggleShotComplete,
                                    tint = if (shot.isCompleted) emeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = if (shot.isCompleted) s.filmedBadge else s.statusPendingBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (shot.isCompleted) emeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // End vertical table border
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(outerBorderColor),
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = innerGridColor,
            )
        }
    }
}

@Composable
private fun ProductionNoteTag(
    icon: ImageVector,
    label: String,
    text: String,
    accentColor: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun ProductionShotTableFooter(
    shots: List<ShotEntity>,
    metrics: ShotTableMetrics,
    horizontalScrollState: ScrollState,
    interactiveMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val totalCount = shots.size
    val filmedCount = shots.count { it.isCompleted }
    val totalDuration = shots.sumOf { it.durationSeconds.coerceAtLeast(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState),
    ) {
        Surface(
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.width(metrics.totalWidth),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = if (interactiveMode && filmedCount == totalCount && totalCount > 0) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.Movie
                    },
                    contentDescription = null,
                    tint = if (interactiveMode && filmedCount == totalCount && totalCount > 0) {
                        FilmedEmerald
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (interactiveMode) {
                        s.tableSummaryTotal(filmedCount, totalCount, formatDuration(totalDuration))
                    } else {
                        s.tableSummaryPlan(totalCount, formatDuration(totalDuration))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
