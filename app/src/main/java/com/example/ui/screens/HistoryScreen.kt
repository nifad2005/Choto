package com.example.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompressionHistoryEntity
import com.example.ui.components.ChotoTextButton
import com.example.ui.components.LocalThumbnailImage
import com.example.ui.components.StatusChip
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    allHistory: List<CompressionHistoryEntity>,
    onBack: () -> Unit,
    onPickVideo: () -> Unit,
    onItemClick: (CompressionHistoryEntity) -> Unit,
    onDeleteItem: (CompressionHistoryEntity) -> Unit,
    onUndoDelete: () -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val grouped = remember(allHistory) {
        groupHistoryByDay(allHistory)
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = colors.bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    .widthIn(max = 480.dp),
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
                            .testTag("history_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.textPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.history_title),
                        style = textStyles.h1,
                        color = colors.textPrimary,
                    )
                }

                if (allHistory.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = stringResource(R.string.cd_history_empty),
                                tint = colors.textTertiary,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_history),
                                style = textStyles.body,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ChotoTextButton(
                                text = stringResource(R.string.cta_pick),
                                onClick = onPickVideo,
                                testTag = "empty_history_pick_button",
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        grouped.forEach { (sectionLabelRes, itemsInGroup) ->
                            item(key = "header_$sectionLabelRes") {
                                Text(
                                    text = stringResource(sectionLabelRes),
                                    style = textStyles.caption,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                )
                            }
                            items(
                                items = itemsInGroup,
                                key = { it.id },
                            ) { entry ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            onDeleteItem(entry)
                                            scope.launch {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                val res = snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.snackbar_deleted),
                                                    actionLabel = context.getString(R.string.cta_undo),
                                                    duration = SnackbarDuration.Short,
                                                )
                                                if (res == SnackbarResult.ActionPerformed) {
                                                    onUndoDelete()
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        Box(
                                            contentAlignment = Alignment.CenterEnd,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(colors.dangerSoft)
                                                .padding(horizontal = 20.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = stringResource(R.string.cta_delete),
                                                tint = colors.danger,
                                            )
                                        }
                                    },
                                ) {
                                    HistoryEntryCard(
                                        entry = entry,
                                        onClick = { onItemClick(entry) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: CompressionHistoryEntity,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick)
            .testTag("history_item_${entry.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            LocalThumbnailImage(
                thumbnailPath = entry.thumbnailPath,
                contentDescription = stringResource(R.string.cd_video_thumbnail),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BanglaFormatter.middleEllipsize(entry.originalName, 24),
                    style = textStyles.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val beforeStr = BanglaFormatter.formatSizeCompactMb(entry.originalSizeBytes)
                val afterStr = BanglaFormatter.formatSizeCompactMb(entry.compressedSizeBytes)
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
            Spacer(modifier = Modifier.width(8.dp))
            StatusChip(saved = entry.isSaved)
        }
    }
}

private fun groupHistoryByDay(
    items: List<CompressionHistoryEntity>,
): List<Pair<Int, List<CompressionHistoryEntity>>> {
    if (items.isEmpty()) return emptyList()
    val today = mutableListOf<CompressionHistoryEntity>()
    val yesterday = mutableListOf<CompressionHistoryEntity>()
    val older = mutableListOf<CompressionHistoryEntity>()

    val cal = Calendar.getInstance()
    val todayYear = cal.get(Calendar.YEAR)
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)

    items.forEach { item ->
        when {
            DateUtils.isToday(item.createdAtMillis) -> today.add(item)
            else -> {
                val itemCal = Calendar.getInstance().apply { timeInMillis = item.createdAtMillis }
                val isYesterday = itemCal.get(Calendar.YEAR) == todayYear &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == todayDay - 1
                if (isYesterday) yesterday.add(item) else older.add(item)
            }
        }
    }

    return buildList {
        if (today.isNotEmpty()) add(R.string.history_today to today)
        if (yesterday.isNotEmpty()) add(R.string.history_yesterday to yesterday)
        if (older.isNotEmpty()) add(R.string.history_older to older)
    }
}
