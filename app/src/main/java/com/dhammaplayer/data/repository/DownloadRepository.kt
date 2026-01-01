package com.dhammaplayer.data.repository

import android.content.Context
import com.dhammaplayer.data.local.DownloadDao
import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.DownloadedTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val MAX_AUTO_CACHE_FILES = 15
    }

    private val downloadsDir: File
        get() = File(context.filesDir, "audio_downloads").also {
            if (!it.exists()) it.mkdirs()
        }

    private val autoCacheDir: File
        get() = File(context.filesDir, "audio_cache").also {
            if (!it.exists()) it.mkdirs()
        }

    fun getDownloadedTrackIds(): Flow<List<String>> = downloadDao.getDownloadedTrackIds()

    fun getAllDownloads(): Flow<List<DownloadedTrack>> = downloadDao.getAllDownloads()

    suspend fun isDownloaded(trackId: String): Boolean = downloadDao.isDownloaded(trackId)

    suspend fun getLocalFilePath(trackId: String): String? {
        val download = downloadDao.getDownload(trackId)
        return download?.filePath?.takeIf { File(it).exists() }
    }

    suspend fun downloadTrack(
        track: AudioTrack,
        onProgress: (Float) -> Unit = {},
        isManualDownload: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if already cached as auto-cache
            val existing = downloadDao.getDownload(track.id)
            if (existing != null && !existing.isManualDownload && isManualDownload) {
                // Convert auto-cache to manual download by moving file and updating record
                val fileName = "${track.id.hashCode()}.mp3"
                val sourceFile = File(existing.filePath)
                val targetFile = File(downloadsDir, fileName)

                // Move file from cache to downloads directory
                if (sourceFile.exists()) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()
                }

                // Update database record
                downloadDao.insertDownload(
                    existing.copy(
                        filePath = targetFile.absolutePath,
                        downloadedAt = System.currentTimeMillis(),
                        isManualDownload = true
                    )
                )

                return@withContext Result.success(targetFile.absolutePath)
            }

            // If already exists as manual download, just return it
            if (existing != null && existing.isManualDownload) {
                return@withContext Result.success(existing.filePath)
            }

            val request = Request.Builder()
                .url(track.audioUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Download failed: ${response.code}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val contentLength = body.contentLength()
            val fileName = "${track.id.hashCode()}.mp3"
            val targetDir = if (isManualDownload) downloadsDir else autoCacheDir
            val file = File(targetDir, fileName)

            FileOutputStream(file).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            onProgress(totalBytesRead.toFloat() / contentLength)
                        }
                    }
                }
            }

            downloadDao.insertDownload(
                DownloadedTrack(
                    trackId = track.id,
                    filePath = file.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                    isManualDownload = isManualDownload
                )
            )

            // If this is an auto-cache, manage the cache limit
            if (!isManualDownload) {
                manageAutoCacheLimit()
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeDownload(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val download = downloadDao.getDownload(trackId)
            download?.let {
                val file = File(it.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            downloadDao.deleteDownload(trackId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Auto-cache a track that's being streamed.
     * This is separate from manual downloads and is limited to MAX_AUTO_CACHE_FILES.
     */
    suspend fun autoCacheTrack(track: AudioTrack): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Check if already cached (manual or auto)
            val existing = downloadDao.getDownload(track.id)
            if (existing != null) {
                // Already cached, just update timestamp
                downloadDao.insertDownload(
                    existing.copy(downloadedAt = System.currentTimeMillis())
                )
                return@withContext Result.success(existing.filePath)
            }

            // Download as auto-cache
            downloadTrack(track, isManualDownload = false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Manage auto-cache limit by removing oldest files when over MAX_AUTO_CACHE_FILES.
     * Manual downloads are never removed by this method.
     */
    private suspend fun manageAutoCacheLimit() {
        val autoCachedFiles = downloadDao.getAutoCachedFiles()
        if (autoCachedFiles.size > MAX_AUTO_CACHE_FILES) {
            val filesToRemove = autoCachedFiles.take(autoCachedFiles.size - MAX_AUTO_CACHE_FILES)

            // Delete files from disk
            filesToRemove.forEach { download ->
                val file = File(download.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }

            // Delete from database
            downloadDao.deleteDownloads(filesToRemove.map { it.trackId })
        }
    }
}

