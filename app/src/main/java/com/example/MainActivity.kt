package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compression.CompressionJobState
import com.example.ui.ChotoViewModel
import com.example.ui.components.ChotoToast
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PreviewAndCompressScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ChotoTheme
import com.example.ui.theme.ChotoThemeTokens
import kotlinx.serialization.Serializable

@Serializable
sealed interface ChotoRoute {
    @Serializable
    data object Home : ChotoRoute

    @Serializable
    data object Preview : ChotoRoute

    @Serializable
    data class Result(val fromHistory: Boolean = false) : ChotoRoute

    @Serializable
    data object History : ChotoRoute

    @Serializable
    data object Settings : ChotoRoute
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChotoTheme {
                ChotoApp()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChotoApp(
    viewModel: ChotoViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val navController = rememberNavController()
    val reducedMotion = ChotoThemeTokens.reducedMotion

    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedVideo by viewModel.selectedVideo.collectAsStateWithLifecycle()
    val isInspectingVideo by viewModel.isInspectingVideo.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    val hasEnoughSpace by viewModel.hasEnoughSpace.collectAsStateWithLifecycle()
    val jobState by viewModel.jobState.collectAsStateWithLifecycle()
    val interruptedJob by viewModel.interruptedJob.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResult.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val justSavedFeedback by viewModel.justSavedFeedback.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val errorShakeCount by viewModel.errorShakeCount.collectAsStateWithLifecycle()
    val pickError by viewModel.pickError.collectAsStateWithLifecycle()

    var showEmptyDeviceHint by remember { mutableStateOf(false) }

    var showNotificationPermCard by remember { mutableStateOf(false) }
    var notificationPermanentlyDenied by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showNotificationPermCard = false
            notificationPermanentlyDenied = false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
            val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            notificationPermanentlyDenied = !canAskAgain
            showNotificationPermCard = true
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            showEmptyDeviceHint = false
            viewModel.onVideoPicked(uri)
        } else {
            showEmptyDeviceHint = true
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            val folderName = docId?.substringAfterLast(':')?.substringAfterLast('/')
            viewModel.setCustomSaveFolder(treeUri, folderName)
        }
    }

    val launchVideoPicker = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm && !notificationPermanentlyDenied) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        videoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        )
    }

    // Navigate to Preview once a picked video finishes inspecting
    LaunchedEffect(selectedVideo?.uriString) {
        if (selectedVideo != null) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute?.contains("Preview") != true) {
                navController.navigate(ChotoRoute.Preview) {
                    launchSingleTop = true
                }
            }
        }
    }

    // Navigate to Result automatically when compression finishes
    LaunchedEffect(jobState) {
        if (jobState is CompressionJobState.Completed && activeResult != null) {
            navController.navigate(ChotoRoute.Result(fromHistory = false)) {
                popUpTo(ChotoRoute.Home) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = ChotoRoute.Home,
                enterTransition = { fadeIn(tween(if (reducedMotion) 150 else 220)) },
                exitTransition = { fadeOut(tween(if (reducedMotion) 150 else 220)) },
                popEnterTransition = { fadeIn(tween(if (reducedMotion) 150 else 220)) },
                popExitTransition = { fadeOut(tween(if (reducedMotion) 150 else 220)) },
            ) {
                composable<ChotoRoute.Home> {
                    HomeScreen(
                        recentHistory = recentHistory,
                        isInspectingVideo = isInspectingVideo,
                        interruptedJob = interruptedJob,
                        pickError = pickError,
                        errorShakeCount = errorShakeCount,
                        showEmptyDeviceHint = showEmptyDeviceHint,
                        showNotificationPermissionCard = showNotificationPermCard,
                        notificationPermanentlyDenied = notificationPermanentlyDenied,
                        onPickVideoClick = launchVideoPicker,
                        onOpenSettings = {
                            navController.navigate(ChotoRoute.Settings) { launchSingleTop = true }
                        },
                        onSeeAllHistory = {
                            navController.navigate(ChotoRoute.History) { launchSingleTop = true }
                        },
                        onHistoryItemClick = { item ->
                            viewModel.openHistoryItem(item)
                            navController.navigate(ChotoRoute.Result(fromHistory = true)) {
                                launchSingleTop = true
                            }
                        },
                        onDeleteHistoryItem = viewModel::deleteHistoryItem,
                        onRetryInterruptedJob = {
                            viewModel.retryInterruptedJob {
                                navController.navigate(ChotoRoute.Preview) { launchSingleTop = true }
                            }
                        },
                        onDismissInterruptedJob = viewModel::dismissInterruptedJob,
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenAppSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            context.startActivity(intent)
                        },
                        onDismissPickError = viewModel::clearPickError,
                        thumbnailModifierFor = { thumbPath ->
                            if (!reducedMotion && !thumbPath.isNullOrBlank()) {
                                Modifier.sharedElement(
                                    state = rememberSharedContentState(key = "thumb_$thumbPath"),
                                    animatedVisibilityScope = this@composable,
                                )
                            } else {
                                Modifier
                            }
                        },
                    )
                }

                composable<ChotoRoute.Preview> {
                    val video = selectedVideo
                    val est = estimate
                    if (video != null && est != null) {
                        val thumbMod = if (!reducedMotion && !video.thumbnailPath.isNullOrBlank()) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = "thumb_${video.thumbnailPath}"),
                                animatedVisibilityScope = this@composable,
                            )
                        } else {
                            Modifier
                        }
                        PreviewAndCompressScreen(
                            metadata = video,
                            config = config,
                            estimate = est,
                            hasEnoughSpace = hasEnoughSpace,
                            jobState = jobState,
                            errorShakeCount = errorShakeCount,
                            onBack = {
                                viewModel.resetForAnotherVideo()
                                navController.popBackStack(ChotoRoute.Home, inclusive = false)
                            },
                            onSelectPreset = viewModel::selectPreset,
                            onUpdateAdvancedConfig = viewModel::updateAdvancedConfig,
                            onResetCustomPreset = viewModel::resetCustomPreset,
                            onStartCompression = viewModel::startCompression,
                            onCancelCompression = viewModel::cancelCompression,
                            onRetryCompression = viewModel::retryCompression,
                            thumbnailModifier = thumbMod,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack(ChotoRoute.Home, inclusive = false)
                        }
                    }
                }

                composable<ChotoRoute.Result> { backStackEntry ->
                    val fromHistory = backStackEntry.arguments?.getBoolean("fromHistory") ?: false
                    val res = activeResult
                    if (res != null) {
                        ResultScreen(
                            result = res,
                            isSaving = isSaving,
                            justSavedFeedback = justSavedFeedback,
                            showBackArrow = fromHistory,
                            onBack = {
                                if (fromHistory) {
                                    navController.popBackStack()
                                } else {
                                    viewModel.resetForAnotherVideo()
                                    navController.popBackStack(ChotoRoute.Home, inclusive = false)
                                }
                            },
                            onSave = viewModel::saveCurrentResult,
                            onShare = {
                                val intent = viewModel.buildShareIntent()
                                if (intent != null) {
                                    context.startActivity(intent)
                                } else {
                                    viewModel.showToast(context.getString(R.string.err_generic))
                                }
                            },
                            onPickAnother = {
                                viewModel.resetForAnotherVideo()
                                navController.popBackStack(ChotoRoute.Home, inclusive = false)
                                launchVideoPicker()
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack(ChotoRoute.Home, inclusive = false)
                        }
                    }
                }

                composable<ChotoRoute.History> {
                    HistoryScreen(
                        allHistory = allHistory,
                        onBack = { navController.popBackStack() },
                        onPickVideo = {
                            navController.popBackStack(ChotoRoute.Home, inclusive = false)
                            launchVideoPicker()
                        },
                        onItemClick = { item ->
                            viewModel.openHistoryItem(item)
                            navController.navigate(ChotoRoute.Result(fromHistory = true)) {
                                launchSingleTop = true
                            }
                        },
                        onDeleteItem = viewModel::deleteHistoryItem,
                        onUndoDelete = viewModel::undoDeleteHistory,
                    )
                }

                composable<ChotoRoute.Settings> {
                    SettingsScreen(
                        settings = userSettings,
                        onBack = { navController.popBackStack() },
                        onSelectDefaultPreset = viewModel::setDefaultPreset,
                        onPickSaveFolder = { folderPickerLauncher.launch(null) },
                        onResetSaveFolder = { viewModel.setCustomSaveFolder(null, null) },
                    )
                }
            }
        }

        ChotoToast(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}
