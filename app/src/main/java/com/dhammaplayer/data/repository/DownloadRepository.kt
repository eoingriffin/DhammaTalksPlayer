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
    private val downloadsDir: File
        get() = File(context.filesDir, "audio_downloads").also {
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
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
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
            val file = File(downloadsDir, fileName)

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
                    downloadedAt = System.currentTimeMillis()
                )
            )

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
}

