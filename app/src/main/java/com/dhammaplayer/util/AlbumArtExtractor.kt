package com.dhammaplayer.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumArtExtractor @Inject constructor() {
    /**
     * Extracts album art from an audio file.
     * @param audioUri The URI or file path of the audio file
     * @return Bitmap of the album art, or null if not available
     */
    suspend fun extractAlbumArt(audioUri: String): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            // Check if it's a file path or URI
            if (audioUri.startsWith("/") || audioUri.startsWith("file://")) {
                val path = if (audioUri.startsWith("file://")) {
                    audioUri.toUri().path ?: return@withContext null
                } else {
                    audioUri
                }
                retriever.setDataSource(path)
            } else if (audioUri.startsWith("http://") || audioUri.startsWith("https://")) {
                retriever.setDataSource(audioUri, emptyMap())
            } else {
                return@withContext null
            }

            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Ignore release errors
            }
        }
    }
}

