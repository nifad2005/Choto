package com.example.compression

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodecList
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max

object VideoInspector {

    suspend fun inspectVideo(context: Context, uri: Uri): VideoMetadata? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (displayName, sizeFromCursor) = queryNameAndSize(resolver, uri)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val (width, height) = if (rotation == 90 || rotation == 270) {
                rawHeight to rawWidth
            } else {
                rawWidth to rawHeight
            }
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull() ?: 0L
            val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                ?.toFloatOrNull()?.toInt()?.coerceIn(15, 120) ?: 30
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

            val resolvedSize = if (sizeFromCursor > 0L) {
                sizeFromCursor
            } else {
                runCatching {
                    resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
                }.getOrDefault(0L)
            }

            if (durationMs <= 0L && resolvedSize <= 0L) {
                return@withContext null
            }

            val thumbPath = generateAndCacheThumbnail(context, retriever, uri.toString())

            VideoMetadata(
                uriString = uri.toString(),
                displayName = displayName.ifBlank { "video_${System.currentTimeMillis()}.mp4" },
                sizeBytes = max(0L, resolvedSize),
                durationMs = max(1000L, durationMs),
                width = width.coerceAtLeast(360),
                height = height.coerceAtLeast(360),
                bitrateBps = bitrate,
                fps = fps,
                hasAudio = hasAudio,
                thumbnailPath = thumbPath,
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun queryNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = ""
        var size = 0L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx).orEmpty()
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        if (name.isBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/') ?: "video.mp4"
        }
        return name to size
    }

    fun generateAndCacheThumbnail(
        context: Context,
        retriever: MediaMetadataRetriever,
        key: String,
    ): String? {
        return runCatching {
            val thumbDir = File(context.cacheDir, "thumbnails").apply { mkdirs() }
            val hash = sha256Short(key)
            val thumbFile = File(thumbDir, "$hash.jpg")
            if (thumbFile.exists() && thumbFile.length() > 0L) {
                return@runCatching thumbFile.absolutePath
            }
            val frame = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return@runCatching null

            val targetWidth = 320
            val scale = targetWidth.toFloat() / frame.width.coerceAtLeast(1)
            val targetHeight = (frame.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (frame.width > targetWidth) {
                Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
            } else {
                frame
            }
            FileOutputStream(thumbFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
            }
            if (scaled !== frame) scaled.recycle()
            frame.recycle()
            thumbFile.absolutePath
        }.getOrNull()
    }

    fun generateThumbnailForFile(context: Context, filePath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            generateAndCacheThumbnail(context, retriever, filePath)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun getAvailableBytes(context: Context): Long {
        return runCatching {
            val stat = StatFs(context.cacheDir.absolutePath)
            stat.availableBytes
        }.getOrDefault(Long.MAX_VALUE)
    }

    fun isHevcSupported(): Boolean {
        return runCatching {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            list.codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals("video/hevc", ignoreCase = true) }
            }
        }.getOrDefault(false)
    }

    suspend fun saveOutputToDevice(
        context: Context,
        sourcePath: String,
        originalDisplayName: String,
        customFolderUriString: String?,
    ): Uri? = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) return@withContext null

        val baseName = originalDisplayName.substringBeforeLast('.').ifBlank { "video" }
        val ext = sourceFile.extension.ifBlank { "mp4" }
        val outName = "${baseName}_choto.$ext"
        val mimeType = if (ext.equals("webm", ignoreCase = true)) "video/webm" else "video/mp4"

        if (!customFolderUriString.isNullOrBlank()) {
            val treeUri = Uri.parse(customFolderUriString)
            val savedViaSaf = runCatching {
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val targetUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocUri,
                    mimeType,
                    outName,
                ) ?: return@runCatching null

                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: return@runCatching null
                targetUri
            }.getOrNull()

            if (savedViaSaf != null) return@withContext savedViaSaf
        }

        return@withContext saveToMoviesChoto(context, sourceFile, outName, mimeType)
    }

    private fun saveToMoviesChoto(
        context: Context,
        sourceFile: File,
        outName: String,
        mimeType: String,
    ): Uri? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, outName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ছোট")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                } ?: throw IllegalStateException("Cannot open output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                uri
            } catch (_: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                null
            }
        } else {
            runCatching {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val chotoDir = File(moviesDir, "ছোট").apply { mkdirs() }
                val dest = File(chotoDir, outName)
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(dest).use { out -> input.copyTo(out) }
                }
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.DATA, dest.absolutePath)
                }
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: Uri.fromFile(dest)
            }.getOrNull()
        }
    }

    fun getShareableUri(context: Context, outputPath: String?, savedUri: String?): Uri? {
        if (!outputPath.isNullOrBlank()) {
            val file = File(outputPath)
            if (file.exists() && file.length() > 0L) {
                return runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrNull()
            }
        }
        if (!savedUri.isNullOrBlank()) {
            return Uri.parse(savedUri)
        }
        return null
    }

    fun cleanupOldTempFiles(context: Context, maxAgeMillis: Long = 24L * 60L * 60L * 1000L) {
        val now = System.currentTimeMillis()
        val compressedDir = File(context.cacheDir, "compressed")
        if (compressedDir.exists()) {
            compressedDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis) {
                    runCatching { file.delete() }
                }
            }
        }
        val thumbDir = File(context.cacheDir, "thumbnails")
        if (thumbDir.exists()) {
            thumbDir.listFiles()?.forEach { file ->
                if (now - file.lastModified() > maxAgeMillis * 7) {
                    runCatching { file.delete() }
                }
            }
        }
    }

    private fun sha256Short(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.take(8).joinToString("") { "%02x".format(it) }
    }
}
