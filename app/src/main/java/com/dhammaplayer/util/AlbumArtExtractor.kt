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

    // Session cache for album art - cleared when app is killed
    private val cache = mutableMapOf<String, Bitmap?>()

    /**
     * Extracts album art from an audio file, using session cache.
     * Only extracts from local files to minimize network traffic.
     * For remote URLs, returns null unless the track is being played
     * (in which case the media controller will have already downloaded it).
     *
     * @param trackId The unique track ID for caching
     * @param audioUri The URI or file path of the audio file
     * @param isLocalFile Whether the audio file is stored locally
     * @return Bitmap of the album art, or null if not available
     */
    suspend fun extractAlbumArt(
        trackId: String,
        audioUri: String,
        isLocalFile: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        // Check cache first
        if (cache.containsKey(trackId)) {
            return@withContext cache[trackId]
        }

        // Only extract from local files to avoid network traffic
        if (!isLocalFile) {
            return@withContext null
        }

        val bitmap = extractFromUri(audioUri)
        cache[trackId] = bitmap
        bitmap
    }

    /**
     * Extracts album art for a track that is currently playing.
     * This is allowed to use network since the audio is already streaming.
     *
     * @param trackId The unique track ID for caching
     * @param audioUri The URI or file path of the audio file
     * @return Bitmap of the album art, or null if not available
     */
    suspend fun extractAlbumArtForPlayingTrack(
        trackId: String,
        audioUri: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        // Check cache first
        if (cache.containsKey(trackId)) {
            return@withContext cache[trackId]
        }

        val bitmap = extractFromUri(audioUri)
        cache[trackId] = bitmap
        bitmap
    }

    private fun extractFromUri(audioUri: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            // Check if it's a file path or URI
            if (audioUri.startsWith("/") || audioUri.startsWith("file://")) {
                val path = if (audioUri.startsWith("file://")) {
                    audioUri.toUri().path ?: return null
                } else {
                    audioUri
                }
                retriever.setDataSource(path)
            } else if (audioUri.startsWith("http://") || audioUri.startsWith("https://")) {
                retriever.setDataSource(audioUri, emptyMap())
            } else {
                return null
            }

            val embeddedPicture = retriever.embeddedPicture
            return if (embeddedPicture != null) {
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
            } else {
                null
            }
        } catch (_: Exception) {
            return null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Ignore release errors
            }
        }
    }

    /**
     * Clears the session cache. Called when needed to free memory.
     */
    fun clearCache() {
        cache.clear()
    }
}

