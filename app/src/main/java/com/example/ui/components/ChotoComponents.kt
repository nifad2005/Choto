package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.theme.ChotoThemeTokens
import com.example.util.BanglaFormatter
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "primary_button",
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "primary_btn_scale",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.action,
            contentColor = colors.actionText,
            disabledContainerColor = colors.surface2,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag),
    ) {
        Text(
            text = text,
            style = textStyles.button,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "secondary_button",
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "secondary_btn_scale",
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.surface2,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag),
    ) {
        Text(
            text = text,
            style = textStyles.button,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
fun ChotoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "text_button",
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isDanger) colors.danger else colors.textSecondary,
            disabledContentColor = colors.textTertiary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
    ) {
        Text(
            text = text,
            style = textStyles.button,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Dropzone(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "dropzone_pick_video",
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "dropzone_scale",
    )
    val cdPick = stringResource(R.string.cd_pick_video)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .drawBehind {
                val strokeWidthPx = 1.5.dp.toPx()
                val radiusPx = 16.dp.toPx()
                val pathEffect = if (isPressed) {
                    null
                } else {
                    PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                }
                drawRoundRect(
                    color = if (isPressed) colors.action else colors.border,
                    topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(width = strokeWidthPx, pathEffect = pathEffect),
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                role = Role.Button,
                onClickLabel = cdPick,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .testTag(testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = textStyles.bodyStrong,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = textStyles.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PresetPill(
    title: String,
    estimateLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "preset_pill",
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "preset_pill_scale",
    )

    val containerColor = if (selected) colors.bg else colors.surface
    val borderStroke = if (selected) {
        BorderStroke(2.dp, colors.action)
    } else {
        BorderStroke(1.dp, colors.border)
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(
                text = title,
                style = if (selected) textStyles.bodyStrong else textStyles.body,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (!estimateLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = estimateLabel,
                    style = textStyles.caption,
                    color = if (selected) colors.accent else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progressPercent: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val reducedMotion = ChotoThemeTokens.reducedMotion

    val targetSweep = (progressPercent.coerceIn(0, 100) / 100f) * 360f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 150)
        } else {
            spring(stiffness = 280f, dampingRatio = 0.80f)
        },
        label = "progress_ring_sweep",
    )

    val a11yText = stringResource(R.string.a11y_progress_announce, BanglaFormatter.bn(progressPercent))

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(160.dp)
            .semantics { contentDescription = a11yText },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 8.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = colors.surface2,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (animatedSweep > 0.5f) {
                drawArc(
                    color = colors.accent,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        AnimatedContent(
            targetState = progressPercent.coerceIn(0, 100),
            transitionSpec = {
                fadeIn(tween(durationMillis = 90)) togetherWith fadeOut(tween(durationMillis = 90))
            },
            label = "progress_digits",
        ) { pct ->
            Text(
                text = BanglaFormatter.formatPercent(pct),
                style = textStyles.display,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AnimatedCheckBadge(
    modifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val cdDone = stringResource(R.string.cd_done)

    val circleScale = remember { Animatable(if (reducedMotion) 1f else 0.6f) }
    val checkProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            circleScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(stiffness = 280f, dampingRatio = 0.72f),
            )
            checkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = circleScale.value
                scaleY = circleScale.value
            }
            .clip(CircleShape)
            .background(colors.accent)
            .semantics { contentDescription = cdDone },
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.18f, size.height * 0.52f)
                lineTo(size.width * 0.42f, size.height * 0.76f)
                lineTo(size.width * 0.84f, size.height * 0.28f)
            }
            val measure = PathMeasure()
            measure.setPath(path, false)
            val drawnPath = Path()
            measure.getSegment(0f, measure.length * checkProgress.value, drawnPath, true)

            drawPath(
                path = drawnPath,
                color = Color.White,
                style = Stroke(
                    width = 3.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
fun SizeRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ChotoThemeTokens.colors.textPrimary,
    showDivider: Boolean = true,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = textStyles.bodyStrong,
                color = valueColor,
            )
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = colors.border)
        }
    }
}

@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Surface(
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.defaultMinSize(minHeight = 24.dp),
    ) {
        Text(
            text = text,
            style = textStyles.caption,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun StatusChip(
    saved: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography

    Surface(
        shape = CircleShape,
        color = if (saved) colors.accentSoft else colors.surface2,
        modifier = modifier.defaultMinSize(minHeight = 24.dp),
    ) {
        Text(
            text = stringResource(if (saved) R.string.status_saved else R.string.status_unsaved),
            style = textStyles.caption,
            color = if (saved) colors.accent else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChotoSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colors.bg,
        contentColor = colors.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.36f),
        dragHandle = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.border),
                )
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = textStyles.h1,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            content()
        }
    }
}

@Composable
fun ChotoToast(
    message: String?,
    modifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val textStyles = ChotoThemeTokens.typography
    val isDark = colors.bg == Color(0xFF0B0B0D)
    val toastBg = if (isDark) Color(0xFFF5F5F7) else Color(0xFF111114)
    val toastFg = if (isDark) Color(0xFF111114) else Color(0xFFFFFFFF)

    androidx.compose.animation.AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = toastBg,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = message.orEmpty(),
                style = textStyles.body,
                color = toastFg,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun ShakeContainer(
    trigger: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reducedMotion = ChotoThemeTokens.reducedMotion
    val phase = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0 && !reducedMotion) {
            phase.snapTo(0f)
            phase.animateTo(1f, animationSpec = tween(durationMillis = 240))
        }
    }

    val shakeDp = if (trigger > 0 && !reducedMotion && phase.value > 0f && phase.value < 1f) {
        (sin(phase.value * Math.PI * 4.0) * 6.0 * (1.0 - phase.value)).toFloat()
    } else {
        0f
    }

    Box(
        modifier = modifier.offset { IntOffset(shakeDp.dp.roundToPx(), 0) },
    ) {
        content()
    }
}

@Composable
fun LocalThumbnailImage(
    thumbnailPath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = ChotoThemeTokens.colors
    val bitmap = remember(thumbnailPath) {
        if (!thumbnailPath.isNullOrBlank() && File(thumbnailPath).exists()) {
            runCatching { BitmapFactory.decodeFile(thumbnailPath)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(colors.surface2),
        ) {
            Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = contentDescription,
                tint = colors.textTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun InlineVideoPreviewCard(
    videoUri: Uri?,
    thumbnailPath: String?,
    modifier: Modifier = Modifier,
    thumbnailModifier: Modifier = Modifier,
) {
    val colors = ChotoThemeTokens.colors
    val cdPlay = stringResource(R.string.cd_play)
    val cdThumb = stringResource(R.string.cd_video_thumbnail)
    var isPlayingInline by remember(videoUri) { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface2)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
    ) {
        if (isPlayingInline && videoUri != null) {
            InlineExoPlayer(
                uri = videoUri,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LocalThumbnailImage(
                thumbnailPath = thumbnailPath,
                contentDescription = cdThumb,
                modifier = thumbnailModifier.fillMaxSize(),
            )
            if (videoUri != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = cdPlay,
                            onClick = { isPlayingInline = true },
                        )
                        .testTag("play_preview_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = cdPlay,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineExoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                useController = true
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) {
                view.player = player
            }
        },
        modifier = modifier,
    )
}
