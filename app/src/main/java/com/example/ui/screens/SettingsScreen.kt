package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSession
import com.example.data.repository.CacheStats
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.FilmedEmerald
import com.example.ui.theme.WarningAmber
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    session: UserSession,
    firebaseUser: FirebaseUser?,
    cacheStats: CacheStats,
    onSelectLanguage: (String) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onForceSyncNow: () -> Unit,
    onClearLocalCache: (includeUnsynced: Boolean) -> Unit,
    onUpdateProfile: (fullName: String, nickname: String, newPassword: String?) -> Unit,
    onGoogleSignInSuccess: (FirebaseUser) -> Unit,
    onGoogleSignInError: (String) -> Unit,
    onLogOut: () -> Unit,
) {
    val s = LocalAppStrings.current
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showClearCacheDialog by rememberSaveable { mutableStateOf(false) }

    if (showEditProfileDialog) {
        var fullNameInput by rememberSaveable { mutableStateOf(session.fullName) }
        var nicknameInput by rememberSaveable { mutableStateOf(session.nickname) }
        var newPasswordInput by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(s.editProfileTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text(s.authFullName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        label = { Text(s.authNickname) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text(s.newPasswordOptional) },
                        placeholder = { Text(s.keepCurrentPasswordHint) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullNameInput.trim().isNotEmpty() && nicknameInput.trim().isNotEmpty()) {
                            onUpdateProfile(
                                fullNameInput.trim(),
                                nicknameInput.trim(),
                                newPasswordInput.takeIf { it.isNotBlank() },
                            )
                            showEditProfileDialog = false
                        }
                    },
                ) {
                    Text(s.saveChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(s.cancelButton)
                }
            },
        )
    }

    if (showClearCacheDialog) {
        val hasUnsynced = cacheStats.pendingChangesCount > 0
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    imageVector = if (hasUnsynced) Icons.Rounded.WarningAmber else Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = if (hasUnsynced) WarningAmber else MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(if (hasUnsynced) s.clearCacheWarningTitle else s.clearCacheTitle)
            },
            text = {
                Text(
                    if (hasUnsynced) s.clearCacheWarningMsg(cacheStats.pendingChangesCount) else s.clearCacheMsg
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        onClearLocalCache(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(if (hasUnsynced) s.eraseEverything else s.clearCacheConfirm)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasUnsynced) {
                        TextButton(
                            onClick = {
                                showClearCacheDialog = false
                                onClearLocalCache(false)
                            },
                        ) {
                            Text(s.syncedOnly)
                        }
                    }
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text(s.cancelButton)
                    }
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
            // Language Switcher Section
            item {
                SettingsSectionCard(title = s.sectionLanguage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.languageTitle,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = s.languageSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val isEnglish = session.languageCode != AppLanguage.ARABIC.code
                        FilterChip(
                            selected = isEnglish,
                            onClick = { onSelectLanguage(AppLanguage.ENGLISH.code) },
                            label = {
                                Text(
                                    text = s.languageEnglish,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            leadingIcon = if (isEnglish) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lang_english"),
                        )

                        FilterChip(
                            selected = !isEnglish,
                            onClick = { onSelectLanguage(AppLanguage.ARABIC.code) },
                            label = {
                                Text(
                                    text = s.languageArabic,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            leadingIcon = if (!isEnglish) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_lang_arabic"),
                        )
                    }
                }
            }

            // Appearance Section
            item {
                SettingsSectionCard(title = s.sectionAppearance) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = if (session.isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = s.darkModeTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = if (session.isDarkMode) s.darkModeOnSubtitle else s.darkModeOffSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = session.isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            modifier = Modifier.testTag("settings_dark_mode_switch"),
                        )
                    }
                }
            }

            // Storage & Sync Management Section
            item {
                val formattedSize = remember(cacheStats.totalBytes) {
                    formatByteSize(cacheStats.totalBytes)
                }
                val lastSyncLabel = remember(cacheStats.lastSyncMillis, s.isArabic) {
                    if (cacheStats.lastSyncMillis <= 0L) {
                        s.neverSynced
                    } else {
                        val locale = if (s.isArabic) Locale("ar") else Locale.US
                        SimpleDateFormat("MMM dd, HH:mm", locale).format(Date(cacheStats.lastSyncMillis))
                    }
                }

                SettingsSectionCard(title = s.sectionStorageSync) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(s.localCacheSizeLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = formattedSize,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = if (cacheStats.isCloudLinked && cacheStats.isOnline) {
                                    Icons.Rounded.CloudDone
                                } else {
                                    Icons.Rounded.CloudOff
                                },
                                contentDescription = null,
                                tint = if (cacheStats.isCloudLinked && cacheStats.isOnline) FilmedEmerald else WarningAmber,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(s.cloudSyncStatusLabel, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = s.lastSyncedLabel(lastSyncLabel, cacheStats.pendingChangesCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !cacheStats.isCloudLinked -> s.statusLocalOnly
                                !cacheStats.isOnline -> s.statusOffline
                                cacheStats.pendingChangesCount > 0 -> s.statusPending
                                else -> s.statusSynced
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (cacheStats.isCloudLinked && cacheStats.isOnline && cacheStats.pendingChangesCount == 0) {
                                FilmedEmerald
                            } else {
                                WarningAmber
                            },
                        )
                    }

                    if (!cacheStats.isCloudLinked) {
                        GoogleSignInButton(
                            label = s.connectGoogleSync,
                            onAuthSuccess = onGoogleSignInSuccess,
                            onAuthError = onGoogleSignInError,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = onForceSyncNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("force_sync_now_button"),
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.forceSyncNow)
                        }

                        OutlinedButton(
                            onClick = { showClearCacheDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_local_cache_button"),
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.clearCacheButton)
                        }
                    }
                }
            }

            // Account Section
            item {
                SettingsSectionCard(title = s.sectionAccount) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.fullName.ifBlank { firebaseUser?.displayName ?: "Director" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "@${session.nickname.ifBlank { "director" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (firebaseUser?.email != null) {
                                Text(
                                    text = s.cloudAccountPrefix(firebaseUser.email!!),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_button"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.editProfileButton)
                        }

                        Button(
                            onClick = onLogOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("log_out_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.logOutButton)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

private fun formatByteSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
