package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompressionHistoryEntity
import com.example.ui.components.ChotoTextButton
import com.example.ui.components.Dropzone
import com.example.ui.components.LocalThumbnailImage
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.ShakeContainer
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    recentHistory: List<CompressionHistoryEntity>,
    isInspectingVideo: Boolean,
    interruptedJob: CompressionHistoryEntity?,
    pickError: String?,
    errorShakeCount: Int,
    showEmptyDeviceHint: Boolean,
    showNotificationPermissionCard: Boolean,
    notificationPermanentlyDenied: Boolean,
    onPickVideoClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAllHistory: () -> Unit,
    onHistoryItemClick: (CompressionHistoryEntity) -> Unit,
    onDeleteHistoryItem: (CompressionHistoryEntity) -> Unit,
    onRetryInterruptedJob: () -> Unit,
    onDismissInterruptedJob: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismissPickError: () -> Unit,
    thumbnailModifierFor: @Composable (String?) -> Modifier = { Modifier },
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    var itemToDelete by remember { mutableStateOf<CompressionHistoryEntity?>(null) }

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
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp),
            ) {
                item(key = "header") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = textStyles.h1,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = textStyles.caption,
                                color = colors.textSecondary,
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = colors.textPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                if (interruptedJob != null) {
                    item(key = "interrupted_job") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = colors.danger,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.interrupted_title),
                                        style = textStyles.bodyStrong,
                                        color = colors.textPrimary,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.interrupted_body),
                                    style = textStyles.caption,
                                    color = colors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    PrimaryButton(
                                        text = stringResource(R.string.cta_retry),
                                        onClick = onRetryInterruptedJob,
                                        modifier = Modifier.weight(1f),
                                        testTag = "retry_interrupted_button",
                                    )
                                    ChotoTextButton(
                                        text = stringResource(R.string.cta_cancel),
                                        onClick = onDismissInterruptedJob,
                                        testTag = "dismiss_interrupted_button",
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "dropzone") {
                    ShakeContainer(trigger = errorShakeCount) {
                        Box(contentAlignment = Alignment.Center) {
                            Dropzone(
                                title = stringResource(R.string.cta_pick),
                                subtitle = if (showEmptyDeviceHint) {
                                    stringResource(R.string.hint_no_videos)
                                } else {
                                    stringResource(R.string.label_supported_formats)
                                },
                                onClick = {
                                    if (!isInspectingVideo) {
                                        onPickVideoClick()
                                    }
                                },
                            )
                            if (isInspectingVideo) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface.copy(alpha = 0.88f)),
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.accent,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (!pickError.isNullOrBlank()) {
                    item(key = "pick_error") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.dangerSoft,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = pickError,
                                    style = textStyles.body,
                                    color = colors.danger,
                                    modifier = Modifier.weight(1f),
                                )
                                ChotoTextButton(
                                    text = stringResource(R.string.cta_ok),
                                    onClick = onDismissPickError,
                                )
                            }
                        }
                    }
                }

                if (showNotificationPermissionCard) {
                    item(key = "perm_card") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.perm_title),
                                    style = textStyles.h2,
                                    color = colors.textPrimary,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.perm_body),
                                    style = textStyles.body,
                                    color = colors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SecondaryButton(
                                    text = stringResource(
                                        if (notificationPermanentlyDenied) {
                                            R.string.perm_go_settings
                                        } else {
                                            R.string.perm_allow
                                        },
                                    ),
                                    onClick = if (notificationPermanentlyDenied) {
                                        onOpenAppSettings
                                    } else {
                                        onRequestNotificationPermission
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "permission_action_button",
                                )
                            }
                        }
                    }
                }

                if (recentHistory.isNotEmpty()) {
                    item(key = "recent_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.label_recent),
                                style = textStyles.caption,
                                color = colors.textSecondary,
                            )
                            ChotoTextButton(
                                text = stringResource(R.string.cta_see_all),
                                onClick = onSeeAllHistory,
                                testTag = "see_all_history_button",
                            )
                        }
                    }

                    itemsIndexed(
                        items = recentHistory,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        StaggeredRow(index = index) {
                            RecentHistoryRow(
                                item = item,
                                thumbnailModifier = thumbnailModifierFor(item.thumbnailPath),
                                onClick = { onHistoryItemClick(item) },
                                onLongPress = { itemToDelete = item },
                            )
                        }
                    }
                }
            }
        }
    }

    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = colors.bg,
            title = {
                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    style = textStyles.h2,
                    color = colors.textPrimary,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_confirm_body),
                    style = textStyles.body,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                ChotoTextButton(
                    text = stringResource(R.string.cta_delete),
                    isDanger = true,
                    onClick = {
                        onDeleteHistoryItem(target)
                        itemToDelete = null
                    },
                )
            },
            dismissButton = {
                ChotoTextButton(
                    text = stringResource(R.string.cta_cancel),
                    onClick = { itemToDelete = null },
                )
            },
        )
    }
}

@Composable
private fun StaggeredRow(
    index: Int,
    content: @Composable () -> Unit,
) {
    val reducedMotion = ChotoThemeTokens.reducedMotion
    var visible by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(index * 40L)
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) {
            fadeIn(tween(150))
        } else {
            fadeIn(tween(220)) + slideInVertically(tween(220)) { 16 }
        },
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentHistoryRow(
    item: CompressionHistoryEntity,
    thumbnailModifier: Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .testTag("recent_item_${item.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            LocalThumbnailImage(
                thumbnailPath = item.thumbnailPath,
                contentDescription = stringResource(R.string.cd_video_thumbnail),
                modifier = thumbnailModifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BanglaFormatter.middleEllipsize(item.originalName, 26),
                    style = textStyles.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val beforeStr = BanglaFormatter.formatSizeCompactMb(item.originalSizeBytes)
                val afterStr = BanglaFormatter.formatSizeCompactMb(item.compressedSizeBytes)
                Text(
                    text = buildAnnotatedString {
                        append(beforeStr)
                        append(" ")
                        withStyle(SpanStyle(color = colors.accent)) {
                            append("→")
                        }
                        append(" ")
                        append(afterStr)
                    },
                    style = textStyles.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}
