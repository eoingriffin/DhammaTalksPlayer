package com.dhammaplayer.data.remote

import com.dhammaplayer.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val RSS_FEED_URL = "https://www.dhammatalks.org/rss/evening.xml"
    }

    suspend fun fetchDhammaTalks(): Result<List<AudioTrack>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RSS_FEED_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch RSS feed: ${response.code}")
                )
            }

            val xmlText = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            val tracks = parseRssFeed(xmlText)
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssFeed(xmlText: String): List<AudioTrack> {
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
                    currentTag = parser.name
                    when (currentTag) {
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
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && currentTrack != null) {
                        val audioUrl = currentTrack["audioUrl"] ?: ""
                        if (audioUrl.isNotEmpty()) {
                            val id = currentTrack["guid"]?.trim() ?: audioUrl
                            tracks.add(
                                AudioTrack(
                                    id = id,
                                    title = currentTrack["title"] ?: "Untitled Talk",
                                    link = currentTrack["link"] ?: "",
                                    pubDate = currentTrack["pubDate"] ?: "",
                                    audioUrl = audioUrl,
                                    description = currentTrack["description"] ?: ""
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

        return tracks
    }

    private fun stripHtml(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }
}

