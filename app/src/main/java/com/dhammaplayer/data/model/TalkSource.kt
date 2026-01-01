package com.dhammaplayer.data.model

enum class TalkSource(val displayName: String, val rssUrl: String) {
    EVENING("Evening Talks", "https://www.dhammatalks.org/rss/evening.xml"),
    MORNING("Morning Talks", "https://www.dhammatalks.org/rss/morning.xml")
}

