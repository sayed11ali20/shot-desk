package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ShotTypePresets
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.localizeShotType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlexibleShotTypeSelector(
    shotTypeValue: String,
    customShotTypes: List<String>,
    onShotTypeChange: (String) -> Unit,
    onAddCustomShotType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showInlineCustomInput by rememberSaveable { mutableStateOf(false) }
    var newCustomOptionText by rememberSaveable { mutableStateOf("") }

    val displayValue = if (shotTypeValue in ShotTypePresets.presets) {
        localizeShotType(shotTypeValue, s)
    } else {
        shotTypeValue
    }

    val allCustomOptions = remember(customShotTypes) {
        customShotTypes
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in ShotTypePresets.presets }
            .distinctBy { it.lowercase() }
    }

    val isCurrentValueNewCustom = shotTypeValue.trim().isNotEmpty() &&
        shotTypeValue.trim() !in ShotTypePresets.presets &&
        allCustomOptions.none { it.equals(shotTypeValue.trim(), ignoreCase = true) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = { typed ->
                    onShotTypeChange(typed)
                },
                readOnly = false,
                singleLine = true,
                label = { Text(s.shotTypeLabel) },
                placeholder = { Text(s.shotTypePlaceholder) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (displayValue.isNotEmpty()) {
                            IconButton(
                                onClick = { onShotTypeChange("") },
                                modifier = Modifier.testTag("clear_shot_type_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = s.cdDelete,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    }
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    .fillMaxWidth()
                    .testTag("shot_type_dropdown"),
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
            ) {
                ShotTypePresets.presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(localizeShotType(preset, s)) },
                        onClick = {
                            onShotTypeChange(preset)
                            dropdownExpanded = false
                        },
                    )
                }
                if (allCustomOptions.isNotEmpty()) {
                    allCustomOptions.forEach { custom ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = custom,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            },
                            onClick = {
                                onShotTypeChange(custom)
                                dropdownExpanded = false
                            },
                        )
                    }
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = s.addCustomOptionChip,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        showInlineCustomInput = true
                    },
                )
            }
        }

        // Quick preset + custom chips strip (never locks user out of typing freely)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            if (isCurrentValueNewCustom) {
                FilterChip(
                    selected = false,
                    onClick = { onAddCustomShotType(shotTypeValue.trim()) },
                    label = { Text(s.saveAsPresetChip) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = MaterialTheme.colorScheme.primary,
                        iconColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.testTag("pin_custom_shot_type_chip"),
                )
            }

            allCustomOptions.forEach { custom ->
                val isSelected = shotTypeValue.equals(custom, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onShotTypeChange(custom) },
                    label = { Text(custom) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }

            ShotTypePresets.presets.forEach { preset ->
                val localized = localizeShotType(preset, s)
                val isSelected = shotTypeValue == preset || shotTypeValue.equals(localized, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onShotTypeChange(preset) },
                    label = { Text(localized) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }

            FilterChip(
                selected = showInlineCustomInput,
                onClick = { showInlineCustomInput = !showInlineCustomInput },
                label = { Text(s.addCustomOptionChip) },
                modifier = Modifier.testTag("add_custom_shot_type_chip"),
            )
        }

        AnimatedVisibility(visible = showInlineCustomInput) {
            InlineCustomOptionCreator(
                value = newCustomOptionText,
                onValueChange = { newCustomOptionText = it },
                placeholder = s.customShotNamePlaceholder,
                addLabel = s.addChipAction,
                testTagPrefix = "custom_shot_type",
                onSubmit = {
                    val trimmed = newCustomOptionText.trim()
                    if (trimmed.isNotEmpty()) {
                        onAddCustomShotType(trimmed)
                        onShotTypeChange(trimmed)
                        newCustomOptionText = ""
                        showInlineCustomInput = false
                    }
                },
                onClose = {
                    newCustomOptionText = ""
                    showInlineCustomInput = false
                },
            )
        }
    }
}

@Composable
fun FlexibleTagSelectorRow(
    label: String,
    presets: List<String>,
    customOptions: List<String>,
    currentFieldText: String,
    testTagPrefix: String,
    onToggleTag: (String) -> Unit,
    onAddCustomOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    var showCustomCreator by rememberSaveable { mutableStateOf(false) }
    var customInput by rememberSaveable { mutableStateOf("") }

    val combinedOptions = remember(presets, customOptions) {
        (customOptions + presets)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            combinedOptions.forEach { option ->
                val isActive = containsTagToken(currentFieldText, option)
                FilterChip(
                    selected = isActive,
                    onClick = { onToggleTag(option) },
                    label = { Text(option) },
                    leadingIcon = if (isActive) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }

            FilterChip(
                selected = showCustomCreator,
                onClick = { showCustomCreator = !showCustomCreator },
                label = { Text(s.addCustomOptionChip) },
                modifier = Modifier.testTag("${testTagPrefix}_add_custom_chip"),
            )
        }

        AnimatedVisibility(visible = showCustomCreator) {
            InlineCustomOptionCreator(
                value = customInput,
                onValueChange = { customInput = it },
                placeholder = s.addCustomOptionPlaceholder,
                addLabel = s.addChipAction,
                testTagPrefix = testTagPrefix,
                onSubmit = {
                    val trimmed = customInput.trim()
                    if (trimmed.isNotEmpty()) {
                        onAddCustomOption(trimmed)
                        if (!containsTagToken(currentFieldText, trimmed)) {
                            onToggleTag(trimmed)
                        }
                        customInput = ""
                        showCustomCreator = false
                    }
                },
                onClose = {
                    customInput = ""
                    showCustomCreator = false
                },
            )
        }
    }
}

@Composable
fun FlexibleDurationPresetRow(
    durationInput: String,
    customDurations: List<Int>,
    onSelectDuration: (Int) -> Unit,
    onAddCustomDuration: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalAppStrings.current
    var showCustomInput by rememberSaveable { mutableStateOf(false) }
    var customSecText by rememberSaveable { mutableStateOf("") }

    val allPresets = remember(customDurations) {
        (listOf(5, 10, 15, 30, 60) + customDurations)
            .filter { it > 0 }
            .distinct()
            .sorted()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            allPresets.forEach { sec ->
                FilterChip(
                    selected = durationInput.toIntOrNull() == sec,
                    onClick = { onSelectDuration(sec) },
                    label = { Text("${sec}s") },
                )
            }

            FilterChip(
                selected = showCustomInput,
                onClick = { showCustomInput = !showCustomInput },
                label = { Text(s.addCustomOptionChip) },
                modifier = Modifier.testTag("duration_add_custom_chip"),
            )
        }

        AnimatedVisibility(visible = showCustomInput) {
            InlineCustomOptionCreator(
                value = customSecText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 5) {
                        customSecText = input
                    }
                },
                placeholder = s.durationPlaceholder,
                addLabel = s.addChipAction,
                keyboardType = KeyboardType.Number,
                testTagPrefix = "custom_duration",
                onSubmit = {
                    val parsed = customSecText.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onAddCustomDuration(parsed)
                        onSelectDuration(parsed)
                        customSecText = ""
                        showCustomInput = false
                    }
                },
                onClose = {
                    customSecText = ""
                    showCustomInput = false
                },
            )
        }
    }
}

@Composable
private fun InlineCustomOptionCreator(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    addLabel: String,
    testTagPrefix: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_custom_input"),
        )
        Button(
            onClick = onSubmit,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
                .height(50.dp)
                .testTag("${testTagPrefix}_custom_add_button"),
        ) {
            Text(addLabel)
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
            )
        }
    }
}

fun toggleTagInText(currentText: String, tag: String): String {
    val trimmedTag = tag.trim()
    if (trimmedTag.isEmpty()) return currentText

    val existingParts = currentText
        .split("•", ",", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val matchingIndex = existingParts.indexOfFirst { it.equals(trimmedTag, ignoreCase = true) }
    return if (matchingIndex >= 0) {
        val remaining = existingParts.toMutableList().apply { removeAt(matchingIndex) }
        remaining.joinToString(" • ")
    } else {
        if (currentText.isBlank()) {
            trimmedTag
        } else {
            val trimmedCurrent = currentText.trimEnd()
            if (trimmedCurrent.endsWith("•") || trimmedCurrent.endsWith(",")) {
                "$trimmedCurrent $trimmedTag"
            } else {
                "$trimmedCurrent • $trimmedTag"
            }
        }
    }
}

private fun containsTagToken(text: String, tag: String): Boolean {
    if (text.isBlank()) return false
    return text
        .split("•", ",", "\n")
        .any { it.trim().equals(tag.trim(), ignoreCase = true) }
}
