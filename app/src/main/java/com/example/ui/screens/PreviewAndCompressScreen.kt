package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.compression.CompressionJobState
import com.example.model.BitrateOption
import com.example.model.CompressionConfig
import com.example.model.CompressionEstimate
import com.example.model.CompressionEstimator
import com.example.model.CompressionPreset
import com.example.model.FpsOption
import com.example.model.ResolutionOption
import com.example.model.VideoFormatOption
import com.example.model.VideoMetadata
import com.example.ui.components.ChotoSheet
import com.example.ui.components.ChotoTextButton
import com.example.ui.components.InlineVideoPreviewCard
import com.example.ui.components.PresetPill
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ProgressRing
import com.example.ui.components.ShakeContainer
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter
import kotlin.math.roundToInt

@Composable
fun PreviewAndCompressScreen(
    metadata: VideoMetadata,
    config: CompressionConfig,
    estimate: CompressionEstimate,
    hasEnoughSpace: Boolean,
    jobState: CompressionJobState,
    errorShakeCount: Int,
    onBack: () -> Unit,
    onSelectPreset: (CompressionPreset) -> Unit,
    onUpdateAdvancedConfig: (
        ResolutionOption,
        BitrateOption,
        VideoFormatOption,
        FpsOption,
        Boolean,
    ) -> Unit,
    onResetCustomPreset: () -> Unit,
    onStartCompression: () -> Unit,
    onCancelCompression: () -> Unit,
    onRetryCompression: () -> Unit,
    thumbnailModifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val view = LocalView.current
    val context = LocalContext.current

    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showCancelConfirmSheet by remember { mutableStateOf(false) }

    val isRunning = jobState is CompressionJobState.Running

    // Keep screen on & lock portrait only during active compression
    DisposableEffect(isRunning) {
        val activity = context as? Activity
        if (isRunning && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (activity != null) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // TalkBack milestone announcements (25, 50, 75, 100%)
    var lastAnnouncedMilestone by remember { mutableIntStateOf(0) }
    LaunchedEffect(jobState) {
        if (jobState is CompressionJobState.Running) {
            val milestone = when {
                jobState.progressPercent >= 75 && lastAnnouncedMilestone < 75 -> 75
                jobState.progressPercent >= 50 && lastAnnouncedMilestone < 50 -> 50
                jobState.progressPercent >= 25 && lastAnnouncedMilestone < 25 -> 25
                else -> 0
            }
            if (milestone > 0) {
                lastAnnouncedMilestone = milestone
                val announcement = context.getString(
                    R.string.a11y_progress_announce,
                    BanglaFormatter.bn(milestone),
                )
                view.announceForAccessibility(announcement)
            }
        } else {
            lastAnnouncedMilestone = 0
        }
    }

    BackHandler {
        if (isRunning) {
            showCancelConfirmSheet = true
        } else {
            onBack()
        }
    }

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
            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    } else {
                        (fadeIn(tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260))) togetherWith
                            (fadeOut(tween(220)) + scaleOut(targetScale = 0.96f, animationSpec = tween(220)))
                    }
                },
                label = "preview_to_compress_morph",
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp),
            ) { compressing ->
                if (compressing && jobState is CompressionJobState.Running) {
                    CompressingContent(
                        runningState = jobState,
                        onRequestCancel = { showCancelConfirmSheet = true },
                    )
                } else {
                    PreviewContent(
                        metadata = metadata,
                        config = config,
                        estimate = estimate,
                        hasEnoughSpace = hasEnoughSpace,
                        failedState = jobState as? CompressionJobState.Failed,
                        errorShakeCount = errorShakeCount,
                        thumbnailModifier = thumbnailModifier,
                        onBack = onBack,
                        onSelectPreset = { preset ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onSelectPreset(preset)
                        },
                        onResetCustom = onResetCustomPreset,
                        onOpenAdvanced = { showAdvancedSheet = true },
                        onStartCompression = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onStartCompression()
                        },
                        onRetryCompression = onRetryCompression,
                    )
                }
            }
        }
    }

    if (showAdvancedSheet) {
        AdvancedSettingsSheet(
            config = config,
            onUpdateConfig = onUpdateAdvancedConfig,
            onDismiss = { showAdvancedSheet = false },
        )
    }

    if (showCancelConfirmSheet) {
        ChotoSheet(
            title = stringResource(R.string.cancel_confirm_title),
            onDismiss = { showCancelConfirmSheet = false },
        ) {
            Text(
                text = stringResource(R.string.cancel_confirm_body),
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = { showCancelConfirmSheet = false },
                testTag = "continue_compression_button",
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChotoTextButton(
                text = stringResource(R.string.action_cancel_confirm),
                isDanger = true,
                onClick = {
                    showCancelConfirmSheet = false
                    onCancelCompression()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_cancel_button",
            )
        }
    }
}

@Composable
private fun PreviewContent(
    metadata: VideoMetadata,
    config: CompressionConfig,
    estimate: CompressionEstimate,
    hasEnoughSpace: Boolean,
    failedState: CompressionJobState.Failed?,
    errorShakeCount: Int,
    thumbnailModifier: Modifier,
    onBack: () -> Unit,
    onSelectPreset: (CompressionPreset) -> Unit,
    onResetCustom: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onStartCompression: () -> Unit,
    onRetryCompression: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion

    val bestEst = remember(metadata) {
        CompressionEstimator.estimate(
            metadata,
            CompressionConfig.forPreset(CompressionPreset.BEST, metadata.sizeBytes),
        )
    }
    val balancedEst = remember(metadata) {
        CompressionEstimator.estimate(
            metadata,
            CompressionConfig.forPreset(CompressionPreset.BALANCED, metadata.sizeBytes),
        )
    }
    val smallestEst = remember(metadata) {
        CompressionEstimator.estimate(
            metadata,
            CompressionConfig.forPreset(CompressionPreset.SMALLEST, metadata.sizeBytes),
        )
    }

    // Animate estimated KB/MB value smoothly when preset changes
    val targetKb = (estimate.estimatedBytes / 1024L).toInt().coerceAtLeast(1)
    val animatedKb by animateIntAsState(
        targetValue = targetKb,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 220),
        label = "animated_estimated_size",
    )

    val canCompress = hasEnoughSpace && estimate.willActuallyShrink
    val showNoChangeBlock = !estimate.willActuallyShrink || (failedState?.isNoChange == true)
    val showGenericError = failedState != null && !failedState.isNoChange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        // Scrollable top section
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("preview_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = colors.textPrimary,
                    )
                }
            }

            InlineVideoPreviewCard(
                videoUri = Uri.parse(metadata.uriString),
                thumbnailPath = metadata.thumbnailPath,
                thumbnailModifier = thumbnailModifier,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = BanglaFormatter.middleEllipsize(metadata.displayName, 34),
                style = textStyles.bodyStrong,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.label_preview_meta,
                    BanglaFormatter.formatSizeCompactMb(metadata.sizeBytes),
                    BanglaFormatter.formatDuration(metadata.durationMs),
                    metadata.resolutionLabel,
                ),
                style = textStyles.caption,
                color = colors.textSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.label_quality),
                    style = textStyles.h2,
                    color = colors.textPrimary,
                )
                if (config.preset == CompressionPreset.CUSTOM) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.preset_custom),
                            style = textStyles.caption,
                            color = colors.accent,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ChotoTextButton(
                            text = stringResource(R.string.cta_reset),
                            onClick = onResetCustom,
                            testTag = "reset_custom_preset_button",
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 2 pills top row, 1 pill below matching spec wireframe
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PresetPill(
                    title = stringResource(R.string.preset_best),
                    estimateLabel = BanglaFormatter.formatSizeCompactMb(bestEst.estimatedBytes),
                    selected = config.preset == CompressionPreset.BEST,
                    onClick = { onSelectPreset(CompressionPreset.BEST) },
                    modifier = Modifier.weight(1f),
                    testTag = "preset_best",
                )
                PresetPill(
                    title = stringResource(R.string.preset_balanced),
                    estimateLabel = BanglaFormatter.formatSizeCompactMb(balancedEst.estimatedBytes),
                    selected = config.preset == CompressionPreset.BALANCED,
                    onClick = { onSelectPreset(CompressionPreset.BALANCED) },
                    modifier = Modifier.weight(1f),
                    testTag = "preset_balanced",
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            PresetPill(
                title = stringResource(R.string.preset_smallest),
                estimateLabel = BanglaFormatter.formatSizeCompactMb(smallestEst.estimatedBytes),
                selected = config.preset == CompressionPreset.SMALLEST,
                onClick = { onSelectPreset(CompressionPreset.SMALLEST) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "preset_smallest",
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.label_estimated),
                    style = textStyles.body,
                    color = colors.textSecondary,
                )
                Text(
                    text = BanglaFormatter.formatSizeCompactMb(animatedKb.toLong() * 1024L),
                    style = textStyles.bodyStrong,
                    color = if (estimate.willActuallyShrink) colors.textPrimary else colors.danger,
                    modifier = Modifier.testTag("estimated_size_value"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = onOpenAdvanced,
                shape = RoundedCornerShape(12.dp),
                color = colors.bg,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advanced_settings_button"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.adv_title),
                        style = textStyles.body,
                        color = colors.textSecondary,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                }
            }

            if (metadata.isLongVideo) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.hint_long_video),
                        style = textStyles.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            if (!hasEnoughSpace) {
                Spacer(modifier = Modifier.height(8.dp))
                ShakeContainer(trigger = errorShakeCount) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.dangerSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.err_no_space),
                                style = textStyles.bodyStrong,
                                color = colors.danger,
                            )
                            Text(
                                text = stringResource(
                                    R.string.err_no_space_detail,
                                    BanglaFormatter.formatSizeSentenceMb(estimate.estimatedBytes * 2L),
                                ),
                                style = textStyles.caption,
                                color = colors.danger,
                            )
                        }
                    }
                }
            }

            if (showGenericError) {
                Spacer(modifier = Modifier.height(8.dp))
                ShakeContainer(trigger = errorShakeCount) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.dangerSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.err_generic),
                                style = textStyles.body,
                                color = colors.danger,
                                modifier = Modifier.weight(1f),
                            )
                            ChotoTextButton(
                                text = stringResource(R.string.cta_retry),
                                isDanger = true,
                                onClick = onRetryCompression,
                                testTag = "retry_error_button",
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sticky bottom CTA
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            if (showNoChangeBlock) {
                ShakeContainer(trigger = errorShakeCount) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.dangerSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.err_no_change),
                                style = textStyles.bodyStrong,
                                color = colors.danger,
                                textAlign = TextAlign.Center,
                            )
                            if (config.preset != CompressionPreset.SMALLEST) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.err_no_change_hint),
                                    style = textStyles.caption,
                                    color = colors.danger,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            } else {
                PrimaryButton(
                    text = stringResource(R.string.cta_compress),
                    onClick = onStartCompression,
                    enabled = canCompress,
                    testTag = "compress_button",
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (metadata.isSmallFile) {
                    stringResource(R.string.hint_already_small)
                } else {
                    stringResource(R.string.label_original_kept)
                },
                style = textStyles.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompressingContent(
    runningState: CompressionJobState.Running,
    onRequestCancel: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        ProgressRing(progressPercent = runningState.progressPercent)

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(
                if (runningState.isFinishing) R.string.state_finishing else R.string.state_compressing,
            ),
            style = textStyles.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = BanglaFormatter.middleEllipsize(runningState.fileName, 28),
            style = textStyles.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (runningState.remainingSeconds != null && runningState.remainingSeconds > 10L) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = BanglaFormatter.formatRemainingTime(runningState.remainingSeconds),
                style = textStyles.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        ChotoTextButton(
            text = stringResource(R.string.cta_cancel),
            isDanger = true,
            onClick = onRequestCancel,
            testTag = "cancel_compression_button",
        )
    }
}
