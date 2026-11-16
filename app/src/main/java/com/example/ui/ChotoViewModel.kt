package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ChotoApplication
import com.example.R
import com.example.compression.CompressionCoordinator
import com.example.compression.CompressionJobState
import com.example.compression.VideoInspector
import com.example.data.CompressionHistoryEntity
import com.example.data.UserSettings
import com.example.model.BitrateOption
import com.example.model.CompressionConfig
import com.example.model.CompressionEstimate
import com.example.model.CompressionEstimator
import com.example.model.CompressionPreset
import com.example.model.FpsOption
import com.example.model.ResolutionOption
import com.example.model.VideoFormatOption
import com.example.model.VideoMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChotoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ChotoApplication
    private val historyRepo = app.historyRepository
    private val settingsRepo = app.settingsRepository

    val recentHistory: StateFlow<List<CompressionHistoryEntity>> = historyRepo.recentCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistory: StateFlow<List<CompressionHistoryEntity>> = historyRepo.allCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSettings: StateFlow<UserSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val jobState: StateFlow<CompressionJobState> = CompressionCoordinator.jobState

    private val _selectedVideo = MutableStateFlow<VideoMetadata?>(null)
    val selectedVideo: StateFlow<VideoMetadata?> = _selectedVideo.asStateFlow()

    private val _isInspectingVideo = MutableStateFlow(false)
    val isInspectingVideo: StateFlow<Boolean> = _isInspectingVideo.asStateFlow()

    private val _config = MutableStateFlow(CompressionConfig.forPreset(CompressionPreset.BALANCED))
    val config: StateFlow<CompressionConfig> = _config.asStateFlow()

    private val _availableBytes = MutableStateFlow(Long.MAX_VALUE)
    val availableBytes: StateFlow<Long> = _availableBytes.asStateFlow()

    val estimate: StateFlow<CompressionEstimate?> = combine(_selectedVideo, _config) { video, cfg ->
        video?.let { CompressionEstimator.estimate(it, cfg) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val hasEnoughSpace: StateFlow<Boolean> = combine(estimate, _availableBytes) { est, free ->
        if (est == null) true else free >= (est.estimatedBytes * 2L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _activeResult = MutableStateFlow<CompressionHistoryEntity?>(null)
    val activeResult: StateFlow<CompressionHistoryEntity?> = _activeResult.asStateFlow()

    private val _interruptedJob = MutableStateFlow<CompressionHistoryEntity?>(null)
    val interruptedJob: StateFlow<CompressionHistoryEntity?> = _interruptedJob.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _justSavedFeedback = MutableStateFlow(false)
    val justSavedFeedback: StateFlow<Boolean> = _justSavedFeedback.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _errorShakeCount = MutableStateFlow(0)
    val errorShakeCount: StateFlow<Int> = _errorShakeCount.asStateFlow()

    private val _pickError = MutableStateFlow<String?>(null)
    val pickError: StateFlow<String?> = _pickError.asStateFlow()

    private var recentlyDeleted: CompressionHistoryEntity? = null
    private var toastJob: Job? = null
    private val hevcSupported: Boolean by lazy { VideoInspector.isHevcSupported() }

    init {
        checkInterruptedJob()
        observeJobCompletion()
    }

    private fun checkInterruptedJob() {
        viewModelScope.launch {
            val inProgress = historyRepo.getLatestInProgress()
            if (inProgress != null && CompressionCoordinator.jobState.value is CompressionJobState.Idle) {
                _interruptedJob.value = inProgress
            }
        }
    }

    private fun observeJobCompletion() {
        viewModelScope.launch {
            CompressionCoordinator.jobState.collect { state ->
                when (state) {
                    is CompressionJobState.Completed -> {
                        val entity = historyRepo.getById(state.historyId)
                        _activeResult.value = entity
                        _justSavedFeedback.value = false
                    }
                    is CompressionJobState.Failed -> {
                        _errorShakeCount.value += 1
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _isInspectingVideo.value = true
            _pickError.value = null
            CompressionCoordinator.resetToIdle()
            val metadata = VideoInspector.inspectVideo(app, uri)
            if (metadata == null) {
                _isInspectingVideo.value = false
                _pickError.value = app.getString(R.string.err_generic)
                _errorShakeCount.value += 1
                return@launch
            }
            _availableBytes.value = VideoInspector.getAvailableBytes(app)
            val defaultPreset = userSettings.value.defaultPreset
            _config.value = CompressionConfig.forPreset(
                preset = defaultPreset,
                sourceSizeBytes = metadata.sizeBytes,
                hevcSupported = hevcSupported,
            )
            _selectedVideo.value = metadata
            _isInspectingVideo.value = false
        }
    }

    fun clearPickError() {
        _pickError.value = null
    }

    fun selectPreset(preset: CompressionPreset) {
        val video = _selectedVideo.value
        _config.value = CompressionConfig.forPreset(
            preset = preset,
            sourceSizeBytes = video?.sizeBytes ?: 0L,
            hevcSupported = hevcSupported,
        )
    }

    fun updateAdvancedConfig(
        resolution: ResolutionOption = _config.value.resolution,
        bitrate: BitrateOption = _config.value.bitrate,
        format: VideoFormatOption = _config.value.format,
        fps: FpsOption = _config.value.fps,
        keepAudio: Boolean = _config.value.keepAudio,
    ) {
        _config.value = CompressionConfig(
            preset = CompressionPreset.CUSTOM,
            resolution = resolution,
            bitrate = bitrate,
            format = format,
            fps = fps,
            keepAudio = keepAudio,
        )
    }

    fun resetCustomPreset() {
        selectPreset(userSettings.value.defaultPreset)
    }

    fun startCompression() {
        val video = _selectedVideo.value ?: return
        val cfg = _config.value
        val est = CompressionEstimator.estimate(video, cfg)
        val freeBytes = VideoInspector.getAvailableBytes(app)
        _availableBytes.value = freeBytes

        if (freeBytes < est.estimatedBytes * 2L || !est.willActuallyShrink) {
            _errorShakeCount.value += 1
            return
        }

        CompressionCoordinator.startCompression(
            context = app,
            metadata = video,
            config = cfg,
            estimate = est,
        )
    }

    fun cancelCompression() {
        CompressionCoordinator.cancelCompression(app)
    }

    fun retryCompression() {
        CompressionCoordinator.resetToIdle()
        startCompression()
    }

    fun resetForAnotherVideo() {
        CompressionCoordinator.resetToIdle()
        _selectedVideo.value = null
        _activeResult.value = null
        _justSavedFeedback.value = false
    }

    fun openHistoryItem(entity: CompressionHistoryEntity) {
        CompressionCoordinator.resetToIdle()
        _activeResult.value = entity
        _justSavedFeedback.value = entity.isSaved
    }

    fun saveCurrentResult() {
        val result = _activeResult.value ?: return
        val outputPath = result.outputPath ?: return
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            val settings = userSettings.value
            val savedUri = VideoInspector.saveOutputToDevice(
                context = app,
                sourcePath = outputPath,
                originalDisplayName = result.originalName,
                customFolderUriString = settings.customSaveFolderUri,
            )
            _isSaving.value = false
            if (savedUri != null) {
                val updated = result.copy(
                    isSaved = true,
                    savedUri = savedUri.toString(),
                )
                historyRepo.update(updated)
                _activeResult.value = updated
                _justSavedFeedback.value = true
                showToast(app.getString(R.string.toast_saved))
                delay(2000L)
                _justSavedFeedback.value = false
            } else {
                _errorShakeCount.value += 1
                showToast(app.getString(R.string.err_generic))
            }
        }
    }

    fun buildShareIntent(): Intent? {
        val result = _activeResult.value ?: return null
        val shareUri = VideoInspector.getShareableUri(app, result.outputPath, result.savedUri)
            ?: return null
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, app.getString(R.string.cta_share))
    }

    fun deleteHistoryItem(entity: CompressionHistoryEntity) {
        viewModelScope.launch {
            recentlyDeleted = entity
            historyRepo.deleteById(entity.id)
        }
    }

    fun undoDeleteHistory() {
        val item = recentlyDeleted ?: return
        recentlyDeleted = null
        viewModelScope.launch {
            historyRepo.restore(item)
        }
    }

    fun dismissInterruptedJob() {
        viewModelScope.launch {
            historyRepo.clearInProgress()
            _interruptedJob.value = null
        }
    }

    fun retryInterruptedJob(onReadyForPreview: () -> Unit) {
        val job = _interruptedJob.value ?: return
        viewModelScope.launch {
            historyRepo.clearInProgress()
            _interruptedJob.value = null
            val uri = runCatching { Uri.parse(job.originalUri) }.getOrNull()
            if (uri != null) {
                onVideoPicked(uri)
                onReadyForPreview()
            } else {
                showToast(app.getString(R.string.err_generic))
            }
        }
    }

    fun setDefaultPreset(preset: CompressionPreset) {
        viewModelScope.launch {
            settingsRepo.setDefaultPreset(preset)
        }
    }

    fun setCustomSaveFolder(uri: Uri?, name: String?) {
        viewModelScope.launch {
            if (uri != null) {
                runCatching {
                    app.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
            settingsRepo.setCustomSaveFolder(uri?.toString(), name)
        }
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _toastMessage.value = message
        toastJob = viewModelScope.launch {
            delay(3000L)
            _toastMessage.value = null
        }
    }
}
