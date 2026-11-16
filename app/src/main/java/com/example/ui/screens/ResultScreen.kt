package com.example.ui.screens

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompressionHistoryEntity
import com.example.ui.components.AnimatedCheckBadge
import com.example.ui.components.ChotoTextButton
import com.example.ui.components.InlineVideoPreviewCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SizeRow
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    result: CompressionHistoryEntity,
    isSaving: Boolean,
    justSavedFeedback: Boolean,
    showBackArrow: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPickAnother: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val view = LocalView.current

    val targetPercent = result.savedPercent
    val countAnim = remember(result.id) {
        Animatable(if (reducedMotion) targetPercent.toFloat() else 0f)
    }
    var showSubLabel by remember(result.id) { mutableStateOf(reducedMotion) }

    LaunchedEffect(result.id) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (!reducedMotion) {
            countAnim.animateTo(
                targetValue = targetPercent.toFloat(),
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            )
            showSubLabel = true
        }
    }

    val playableUri = remember(result.outputPath, result.savedUri) {
        val path = result.outputPath
        if (!path.isNullOrBlank() && File(path).exists()) {
            Uri.fromFile(File(path))
        } else if (!result.savedUri.isNullOrBlank()) {
            Uri.parse(result.savedUri)
        } else {
            null
        }
    }

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
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (showBackArrow) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("result_back_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = colors.textPrimary,
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    AnimatedCheckBadge()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.state_done),
                        style = textStyles.h1,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.accentSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            val pctText = BanglaFormatter.formatPercent(countAnim.value.roundToInt())
                            Text(
                                text = stringResource(R.string.label_percent_saved, pctText),
                                style = textStyles.display,
                                color = colors.accent,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("result_saved_percent"),
                            )
                            AnimatedVisibility(
                                visible = showSubLabel,
                                enter = if (reducedMotion) {
                                    fadeIn(tween(150))
                                } else {
                                    fadeIn(tween(220)) + slideInVertically(tween(220)) { 16 }
                                },
                            ) {
                                val savedBytes = (result.originalSizeBytes - result.compressedSizeBytes)
                                    .coerceAtLeast(0L)
                                Text(
                                    text = "${BanglaFormatter.formatSizeCompactMb(savedBytes)} ${stringResource(R.string.label_saved)}",
                                    style = textStyles.caption,
                                    color = colors.accent,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    SizeRow(
                        label = stringResource(R.string.label_before),
                        value = BanglaFormatter.formatSizeCompactMb(result.originalSizeBytes),
                    )
                    SizeRow(
                        label = stringResource(R.string.label_after),
                        value = BanglaFormatter.formatSizeCompactMb(result.compressedSizeBytes),
                        valueColor = colors.accent,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    InlineVideoPreviewCard(
                        videoUri = playableUri,
                        thumbnailPath = result.thumbnailPath,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = BanglaFormatter.middleEllipsize(result.originalName, 34),
                        style = textStyles.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 14.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(
                            if (justSavedFeedback) R.string.cta_saved else R.string.cta_save,
                        ),
                        onClick = onSave,
                        enabled = !isSaving && !justSavedFeedback && playableUri != null,
                        testTag = "save_result_button",
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SecondaryButton(
                            text = stringResource(R.string.cta_share),
                            onClick = onShare,
                            enabled = playableUri != null,
                            modifier = Modifier.weight(1f),
                            testTag = "share_result_button",
                        )
                        ChotoTextButton(
                            text = stringResource(R.string.cta_again),
                            onClick = onPickAnother,
                            modifier = Modifier.weight(1f),
                            testTag = "another_video_button",
                        )
                    }
                }
            }
        }
    }
}
