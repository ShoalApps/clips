package io.github.tintloom.clips

/** Where a shared or opened short-video link should go. */
object ClipLinks {
    sealed interface Target
    data class YouTubeShort(val id: String) : Target
    /** Played through the site's official embed player. */
    data class Embed(val site: String, val embedUrl: String, val original: String) : Target
    /** A short link (vm.tiktok.com) that has to be followed to find the video. */
    data class Unresolved(val url: String) : Target

    private val youtube = Regex("""(?:youtube\.com/shorts/|youtu\.be/)([\w-]{11})""")
    private val tiktok = Regex("""tiktok\.com/(?:@[\w.-]+/video|embed/v2|player/v1|v)/(\d{8,})""")
    private val tiktokShort = Regex("""https?://(?:vm|vt)\.tiktok\.com/[\w]+/?""")
    private val instagram = Regex("""instagram\.com/(?:[\w.]+/)?(?:reel|reels|p)/([\w-]+)""")
    private val anyUrl = Regex("""https?://\S+""")

    /** The first supported link in [text] (a URL, or a share message containing one). */
    fun parse(text: String): Target? {
        val url = anyUrl.find(text)?.value ?: return null
        youtube.find(url)?.let { return YouTubeShort(it.groupValues[1]) }
        tiktok.find(url)?.let { return tiktokEmbed(it.groupValues[1], url) }
        if (tiktokShort.matches(url.trimEnd('/') + "/") || tiktokShort.containsMatchIn(url)) return Unresolved(url)
        instagram.find(url)?.let { return Embed("Instagram", "https://www.instagram.com/reel/${it.groupValues[1]}/embed/", url) }
        return null
    }

    fun tiktokEmbed(id: String, original: String) =
        Embed("TikTok", "https://www.tiktok.com/player/v1/$id?autoplay=1&loop=1&description=1&music_info=1", original)

    /** After following a short link's redirects. */
    fun fromResolved(finalUrl: String, original: String): Target? =
        tiktok.find(finalUrl)?.let { tiktokEmbed(it.groupValues[1], original) }
}
