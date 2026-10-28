package io.github.shoalapps.clownfish.data

/** A zero or invalid limit keeps the existing best-available behavior. */
internal object ShortsQuality {
    fun maxHeight(settings: Settings, unmetered: Boolean): Int {
        val limit = if (unmetered) settings.shortsQualityUnmetered else settings.shortsQualityMetered
        return limit.takeIf { it > 0 } ?: Int.MAX_VALUE
    }
}
