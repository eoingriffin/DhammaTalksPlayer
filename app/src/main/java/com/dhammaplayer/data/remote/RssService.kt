package com.dhammaplayer.data.remote

import com.dhammaplayer.data.model.AudioTrack
import com.dhammaplayer.data.model.TalkSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun fetchDhammaTalks(source: TalkSource): Result<List<AudioTrack>> =
        withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.rssUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch RSS feed: ${response.code}")
                )
            }

            val xmlText = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val tracks = parseRssFeed(xmlText, source)
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssFeed(xmlText: String, source: TalkSource): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlText))

        var eventType = parser.eventType
        var currentTrack: MutableMap<String, String>? = null
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    // For namespace-aware parsing, combine prefix and name
                    val prefix = parser.prefix
                    val name = parser.name
                    currentTag = if (prefix != null) "$prefix:$name" else name

                    when (name) {
                        "item" -> currentTrack = mutableMapOf()
                        "enclosure" -> {
                            currentTrack?.let { track ->
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrEmpty()) {
                                    track["audioUrl"] = url
                                }
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty() && currentTrack != null) {
                        when (currentTag) {
                            "title" -> currentTrack["title"] = text
                            "link" -> currentTrack["link"] = text
                            "pubDate" -> currentTrack["pubDate"] = text
                            "description" -> currentTrack["description"] = stripHtml(text)
                            "guid" -> currentTrack["guid"] = text
                            "itunes:duration", "duration" -> currentTrack["duration"] = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && currentTrack != null) {
                        val audioUrl = currentTrack["audioUrl"] ?: ""
                        if (audioUrl.isNotEmpty()) {
                            val id = currentTrack["guid"]?.trim() ?: audioUrl
                            val pubDateString = currentTrack["pubDate"] ?: ""

                            // Parse pubDate to timestamp for proper sorting
                            val pubDateTimestamp = try {
                                ZonedDateTime.parse(
                                    pubDateString,
                                    DateTimeFormatter.RFC_1123_DATE_TIME
                                )
                                    .toInstant()
                                    .toEpochMilli()
                            } catch (e: Exception) {
                                0L // Default timestamp for invalid dates
                            }

                            // Parse itunes:duration to milliseconds
                            val durationMillis = parseDuration(currentTrack["duration"])

                            tracks.add(
                                AudioTrack(
                                    id = id,
                                    title = currentTrack["title"] ?: "Untitled Talk",
                                    link = currentTrack["link"] ?: "",
                                    pubDate = pubDateString,
                                    pubDateTimestamp = pubDateTimestamp,
                                    audioUrl = audioUrl,
                                    description = currentTrack["description"] ?: "",
                                    duration = durationMillis,
                                    source = source.name
                                )
                            )
                        }
                        currentTrack = null
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        // Sort by pubDateTimestamp in descending order (newest first)
        tracks.sortByDescending { it.pubDateTimestamp }
        return tracks
    }

    private fun stripHtml(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }

    /**
     * Parses iTunes duration format to milliseconds.
     * The itunes:duration field is in seconds (as a plain number), not HH:MM:SS format.
     */
    private fun parseDuration(durationString: String?): Long? {
        if (durationString.isNullOrEmpty()) return null

        return try {
            // iTunes duration is in seconds, convert to milliseconds
            durationString.toLong() * 1000
        } catch (e: Exception) {
            null
        }
    }
}

