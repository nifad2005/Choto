package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserSettings
import com.example.model.CompressionPreset
import com.example.ui.components.ChotoSheet
import com.example.ui.components.ChotoTextButton
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onBack: () -> Unit,
    onSelectDefaultPreset: (CompressionPreset) -> Unit,
    onPickSaveFolder: () -> Unit,
    onResetSaveFolder: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    var showDefaultPresetSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.textPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.cta_settings),
                        style = textStyles.h1,
                        color = colors.textPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val presetLabel = when (settings.defaultPreset) {
                    CompressionPreset.BEST -> stringResource(R.string.preset_best)
                    CompressionPreset.BALANCED -> stringResource(R.string.preset_balanced)
                    CompressionPreset.SMALLEST -> stringResource(R.string.preset_smallest)
                    CompressionPreset.CUSTOM -> stringResource(R.string.preset_balanced)
                }

                SettingsRow(
                    label = stringResource(R.string.settings_default_quality),
                    value = presetLabel,
                    showChevron = true,
                    onClick = { showDefaultPresetSheet = true },
                    testTag = "setting_default_quality",
                )

                SettingsRow(
                    label = stringResource(R.string.settings_save_folder),
                    value = settings.customSaveFolderName ?: stringResource(R.string.settings_default_folder),
                    showChevron = true,
                    onClick = onPickSaveFolder,
                    testTag = "setting_save_folder",
                )

                if (settings.customSaveFolderUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        ChotoTextButton(
                            text = stringResource(R.string.cta_reset),
                            onClick = onResetSaveFolder,
                        )
                    }
                }

                SettingsRow(
                    label = stringResource(R.string.settings_language),
                    value = stringResource(R.string.settings_language_bn),
                    showChevron = false,
                    onClick = null,
                    testTag = "setting_language",
                )

                SettingsRow(
                    label = stringResource(R.string.settings_about),
                    value = null,
                    showChevron = true,
                    onClick = { showAboutSheet = true },
                    testTag = "setting_about",
                )

                SettingsRow(
                    label = stringResource(R.string.settings_privacy),
                    value = null,
                    showChevron = true,
                    onClick = { showPrivacySheet = true },
                    testTag = "setting_privacy",
                )

                SettingsRow(
                    label = stringResource(R.string.settings_version),
                    value = BanglaFormatter.bn("1.0"),
                    showChevron = false,
                    onClick = null,
                    showDivider = false,
                    testTag = "setting_version",
                )
            }
        }
    }

    if (showDefaultPresetSheet) {
        ChotoSheet(
            title = stringResource(R.string.settings_default_quality),
            onDismiss = { showDefaultPresetSheet = false },
        ) {
            val presets = listOf(
                Triple(CompressionPreset.BEST, R.string.preset_best, R.string.preset_best_sub),
                Triple(CompressionPreset.BALANCED, R.string.preset_balanced, R.string.preset_balanced_sub),
                Triple(CompressionPreset.SMALLEST, R.string.preset_smallest, R.string.preset_smallest_sub),
            )
            presets.forEach { (preset, titleRes, subRes) ->
                val selected = settings.defaultPreset == preset
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .clickable {
                            onSelectDefaultPreset(preset)
                            showDefaultPresetSheet = false
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(titleRes),
                            style = textStyles.bodyStrong,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(subRes),
                            style = textStyles.caption,
                            color = colors.textSecondary,
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAboutSheet) {
        ChotoSheet(
            title = stringResource(R.string.settings_about),
            onDismiss = { showAboutSheet = false },
        ) {
            Text(
                text = stringResource(R.string.about_body),
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(R.string.cta_ok),
                onClick = { showAboutSheet = false },
            )
        }
    }

    if (showPrivacySheet) {
        ChotoSheet(
            title = stringResource(R.string.settings_privacy),
            onDismiss = { showPrivacySheet = false },
        ) {
            Text(
                text = stringResource(R.string.privacy_body),
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(R.string.cta_ok),
                onClick = { showPrivacySheet = false },
            )
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String?,
    showChevron: Boolean,
    onClick: (() -> Unit)?,
    showDivider: Boolean = true,
    testTag: String,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp)
            .testTag(testTag),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = label,
                style = textStyles.body,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = textStyles.caption,
                        color = colors.textTertiary,
                    )
                }
                if (showChevron) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = colors.border)
        }
    }
}
