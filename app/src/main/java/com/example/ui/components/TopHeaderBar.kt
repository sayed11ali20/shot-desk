package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.SyncIndicatorState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.FilmedEmeraldLight
import com.example.ui.theme.LogoHighlightNavy
import com.example.ui.theme.LogoIvoryHeaderEnd
import com.example.ui.theme.LogoIvoryHeaderStart
import com.example.ui.theme.LogoMidnightNavy
import com.example.ui.theme.LogoSlateBlue
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioGoldDark
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun TopHeaderBar(
    title: String,
    subtitle: String?,
    isDarkTheme: Boolean,
    syncState: SyncIndicatorState,
    pendingCount: Int,
    onSyncPillClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val goldAccent = if (isDarkTheme) StudioGold else StudioGoldDark
    val emeraldAccent = if (isDarkTheme) FilmedEmerald else FilmedEmeraldLight
    val amberAccent = if (isDarkTheme) WarningAmber else WarningAmberLight

    val pillBg by animateColorAsState(
        targetValue = when (syncState) {
            SyncIndicatorState.CLOUD_SYNCED -> emeraldAccent.copy(alpha = if (isDarkTheme) 0.16f else 0.12f)
            SyncIndicatorState.SYNCING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
            SyncIndicatorState.SAVED_LOCALLY -> if (isDarkTheme) {
                LogoHighlightNavy.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            }
            SyncIndicatorState.SYNC_ERROR -> amberAccent.copy(alpha = 0.16f)
        },
        label = "syncPillBg",
    )

    val pillContentColor = when (syncState) {
        SyncIndicatorState.CLOUD_SYNCED -> emeraldAccent
        SyncIndicatorState.SYNCING -> MaterialTheme.colorScheme.secondary
        SyncIndicatorState.SAVED_LOCALLY -> MaterialTheme.colorScheme.onSurfaceVariant
        SyncIndicatorState.SYNC_ERROR -> amberAccent
    }

    val syncLabel = when (syncState) {
        SyncIndicatorState.CLOUD_SYNCED -> s.syncCloudSynced
        SyncIndicatorState.SYNCING -> s.syncSyncing
        SyncIndicatorState.SAVED_LOCALLY -> if (pendingCount > 0) {
            s.savedLocallyCount(pendingCount)
        } else {
            s.syncSavedLocally
        }
        SyncIndicatorState.SYNC_ERROR -> s.syncPaused
    }

    val bgBrush = if (isDarkTheme) {
        Brush.horizontalGradient(
            colors = listOf(LogoMidnightNavy, LogoSlateBlue, LogoHighlightNavy),
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(LogoIvoryHeaderStart, LogoIvoryHeaderEnd, LogoIvoryHeaderStart),
        )
    }

    val bottomGoldRuleBrush = Brush.horizontalGradient(
        colors = listOf(
            goldAccent.copy(alpha = 0.2f),
            goldAccent.copy(alpha = 0.85f),
            goldAccent.copy(alpha = 0.35f),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(bgBrush)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Spotlight halo behind the custom studio logo on trailing edge
                val logoCenterX = if (isRtl) 38.dp.toPx() else w - 38.dp.toPx()
                val logoCenterY = h * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            goldAccent.copy(alpha = if (isDarkTheme) 0.20f else 0.16f),
                            Color.Transparent,
                        ),
                        center = Offset(logoCenterX, logoCenterY),
                        radius = 96.dp.toPx(),
                    ),
                    radius = 96.dp.toPx(),
                    center = Offset(logoCenterX, logoCenterY),
                )

                // Subtle artistic clapperboard diagonal stripes near the trailing emblem
                val stripeColor = goldAccent.copy(alpha = if (isDarkTheme) 0.07f else 0.06f)
                val stripeCount = 4
                val stripeWidth = 14.dp.toPx()
                val stripeGap = 24.dp.toPx()
                for (i in 0 until stripeCount) {
                    val baseOffset = (i * stripeGap)
                    val path = Path().apply {
                        if (!isRtl) {
                            val startX = w - 165.dp.toPx() + baseOffset
                            moveTo(startX, h)
                            lineTo(startX + stripeWidth, h)
                            lineTo(startX + stripeWidth + 32.dp.toPx(), 0f)
                            lineTo(startX + 32.dp.toPx(), 0f)
                            close()
                        } else {
                            val startX = 165.dp.toPx() - baseOffset
                            moveTo(startX, h)
                            lineTo(startX - stripeWidth, h)
                            lineTo(startX - stripeWidth - 32.dp.toPx(), 0f)
                            lineTo(startX - 32.dp.toPx(), 0f)
                            close()
                        }
                    }
                    drawPath(path = path, color = stripeColor)
                }

                // Subtle filmstrip perforation ticks along the bottom edge
                val sprocketW = 8.dp.toPx()
                val sprocketH = 3.dp.toPx()
                val sprocketStep = 18.dp.toPx()
                val sprocketColor = goldAccent.copy(alpha = if (isDarkTheme) 0.22f else 0.18f)
                var x = 12.dp.toPx()
                val yPos = h - 7.dp.toPx()
                while (x < w - 12.dp.toPx()) {
                    drawRoundRect(
                        color = sprocketColor,
                        topLeft = Offset(x, yPos),
                        size = Size(sprocketW, sprocketH),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
                    )
                    x += sprocketStep
                }
            },
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    // Editorial vertical gold film-gate accent bar
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(goldAccent, goldAccent.copy(alpha = 0.35f)),
                                ),
                            ),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = goldAccent.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = pillBg,
                        border = BorderStroke(1.dp, pillContentColor.copy(alpha = 0.42f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onSyncPillClick)
                            .testTag("header_sync_status_pill"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            when (syncState) {
                                SyncIndicatorState.SYNCING -> CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = pillContentColor,
                                )
                                SyncIndicatorState.CLOUD_SYNCED -> Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = s.cdSyncStatus,
                                    tint = pillContentColor,
                                    modifier = Modifier.size(14.dp),
                                )
                                SyncIndicatorState.SAVED_LOCALLY -> Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = s.cdSyncStatus,
                                    tint = pillContentColor,
                                    modifier = Modifier.size(14.dp),
                                )
                                SyncIndicatorState.SYNC_ERROR -> Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = s.cdSyncStatus,
                                    tint = pillContentColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = pillContentColor,
                                maxLines = 1,
                            )
                        }
                    }

                    // Custom studio emblem framed in a double gold studio ring
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, goldAccent.copy(alpha = 0.9f)),
                        shadowElevation = 6.dp,
                        color = LogoMidnightNavy,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("header_custom_logo"),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.custom_studio_logo),
                            contentDescription = s.cdStudioLogo,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            }

            // Signature metallic brushed-gold bottom rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(bottomGoldRuleBrush),
            )
        }
    }
}
