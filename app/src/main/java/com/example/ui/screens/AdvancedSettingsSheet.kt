package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BitrateOption
import com.example.model.CompressionConfig
import com.example.model.FpsOption
import com.example.model.ResolutionOption
import com.example.model.VideoFormatOption
import com.example.ui.components.ChotoSheet
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter

@Composable
fun AdvancedSettingsSheet(
    config: CompressionConfig,
    onUpdateConfig: (
        resolution: ResolutionOption,
        bitrate: BitrateOption,
        format: VideoFormatOption,
        fps: FpsOption,
        keepAudio: Boolean,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    ChotoSheet(
        title = stringResource(R.string.adv_title),
        onDismiss = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            // Resolution
            SheetSectionLabel(stringResource(R.string.adv_resolution))
            SegmentedOptionRow(
                options = listOf(
                    ResolutionOption.AUTO to stringResource(R.string.adv_opt_auto),
                    ResolutionOption.P1080 to stringResource(R.string.adv_opt_1080p),
                    ResolutionOption.P720 to stringResource(R.string.adv_opt_720p),
                    ResolutionOption.P480 to stringResource(R.string.adv_opt_480p),
                ),
                selected = config.resolution,
                onSelect = { res ->
                    onUpdateConfig(res, config.bitrate, config.format, config.fps, config.keepAudio)
                },
            )

            // Bitrate
            SheetSectionLabel(stringResource(R.string.adv_bitrate))
            SegmentedOptionRow(
                options = listOf(
                    BitrateOption.AUTO to stringResource(R.string.adv_opt_auto),
                    BitrateOption.LOW to stringResource(R.string.adv_opt_low),
                    BitrateOption.MEDIUM to stringResource(R.string.adv_opt_medium),
                    BitrateOption.HIGH to stringResource(R.string.adv_opt_high),
                ),
                selected = config.bitrate,
                onSelect = { br ->
                    onUpdateConfig(config.resolution, br, config.format, config.fps, config.keepAudio)
                },
            )

            // Format
            SheetSectionLabel(stringResource(R.string.adv_format))
            SegmentedOptionRow(
                options = listOf(
                    VideoFormatOption.MP4_H264 to stringResource(R.string.adv_opt_h264),
                    VideoFormatOption.MP4_HEVC to stringResource(R.string.adv_opt_hevc),
                    VideoFormatOption.WEBM to stringResource(R.string.adv_opt_webm),
                ),
                selected = config.format,
                onSelect = { fmt ->
                    onUpdateConfig(config.resolution, config.bitrate, fmt, config.fps, config.keepAudio)
                },
            )

            // Frame Rate Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SheetSectionLabel(stringResource(R.string.adv_fps))
                FpsStepper(
                    current = config.fps,
                    onChange = { fps ->
                        onUpdateConfig(config.resolution, config.bitrate, config.format, fps, config.keepAudio)
                    },
                )
            }

            // Audio Toggle + Segmented
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SheetSectionLabel(stringResource(R.string.adv_audio))
                    Switch(
                        checked = config.keepAudio,
                        onCheckedChange = { keep ->
                            onUpdateConfig(config.resolution, config.bitrate, config.format, config.fps, keep)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.actionText,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surface2,
                        ),
                        modifier = Modifier.testTag("audio_switch"),
                    )
                }
                SegmentedOptionRow(
                    options = listOf(
                        true to stringResource(R.string.adv_audio_keep),
                        false to stringResource(R.string.adv_audio_remove),
                    ),
                    selected = config.keepAudio,
                    onSelect = { keep ->
                        onUpdateConfig(config.resolution, config.bitrate, config.format, config.fps, keep)
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(
            text = stringResource(R.string.cta_ok),
            onClick = onDismiss,
            testTag = "advanced_ok_button",
        )
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = ChotoThemeTokens.typography.bodyStrong,
        color = ChotoThemeTokens.colors.textPrimary,
    )
}

@Composable
private fun <T> SegmentedOptionRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) colors.bg else colors.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) colors.action else colors.border,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = label,
                    style = if (isSelected) textStyles.bodyStrong else textStyles.body,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun FpsStepper(
    current: FpsOption,
    onChange: (FpsOption) -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val entries = FpsOption.entries
    val currentIndex = entries.indexOf(current).coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (currentIndex > 0) onChange(entries[currentIndex - 1])
                },
                enabled = currentIndex > 0,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = if (currentIndex > 0) colors.textPrimary else colors.textTertiary,
                )
            }
            Text(
                text = BanglaFormatter.bn(current.fps),
                style = textStyles.bodyStrong,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(
                onClick = {
                    if (currentIndex < entries.lastIndex) onChange(entries[currentIndex + 1])
                },
                enabled = currentIndex < entries.lastIndex,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = if (currentIndex < entries.lastIndex) colors.textPrimary else colors.textTertiary,
                )
            }
        }
    }
}
