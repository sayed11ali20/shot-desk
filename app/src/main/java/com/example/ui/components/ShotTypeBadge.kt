package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeShotType
import com.example.ui.theme.BadgeBRoll
import com.example.ui.theme.BadgeCloseUp
import com.example.ui.theme.BadgeCustom
import com.example.ui.theme.BadgeDrone
import com.example.ui.theme.BadgeExtremeCloseUp
import com.example.ui.theme.BadgeMedium
import com.example.ui.theme.BadgeOts
import com.example.ui.theme.BadgePov
import com.example.ui.theme.BadgeWide
import java.util.Locale

@Composable
fun ShotTypeBadge(
    shotType: String,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    val accentColor = shotTypeColor(shotType)
    val displayLabel = localizeShotType(shotType, s)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier,
    ) {
        Text(
            text = if (s.isArabic) displayLabel else displayLabel.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

fun shotTypeColor(shotType: String): Color {
    val normalized = shotType.trim().lowercase(Locale.US)
    return when {
        normalized == "wide shot" || normalized.contains("لقطة واسعة") -> BadgeWide
        normalized == "medium shot" || normalized.contains("لقطة متوسطة") -> BadgeMedium
        normalized == "close-up" || normalized.contains("لقطة قريبة (close") -> BadgeCloseUp
        normalized == "extreme close-up" || normalized.contains("لقطة قريبة جداً") -> BadgeExtremeCloseUp
        normalized == "over-the-shoulder" || normalized.contains("من فوق الكتف") -> BadgeOts
        normalized == "b-roll" || normalized.contains("لقطات مساعدة") -> BadgeBRoll
        normalized == "drone/aerial" || normalized.contains("تصوير جوي") -> BadgeDrone
        normalized == "pov" || normalized.contains("منظور الشخص الأول") -> BadgePov
        else -> BadgeCustom
    }
}

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
