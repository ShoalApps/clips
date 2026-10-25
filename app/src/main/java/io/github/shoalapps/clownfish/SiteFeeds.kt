package io.github.shoalapps.clownfish

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * TikTok and Instagram Reels as their own official mobile websites, signed in with your own
 * accounts through their own login pages. Clips shows the sites as a browser would: it doesn't
 * scrape them or use their private APIs.
 */
enum class Site(
    val key: String,
    val title: String,
    val feedUrl: String,
    val loginUrl: String,
    val home: String,
    /** Shown behind the page; Instagram's pages don't paint their own background. */
    val backdrop: Int,
    private val hosts: List<String>,
) {
    TIKTOK("tiktok", "TikTok", "https://www.tiktok.com/foryou", "https://www.tiktok.com/login", "https://www.tiktok.com", android.graphics.Color.BLACK, listOf("tiktok.com", "tiktokv.com", "tiktokcdn.com", "ttwstatic.com", "byteoversea.com", "ibytedtos.com")),
    REELS("reels", "Reels", "https://www.instagram.com/reels/", "https://www.instagram.com/accounts/login/", "https://www.instagram.com", android.graphics.Color.WHITE, listOf("instagram.com", "cdninstagram.com", "fbcdn.net", "facebook.com"));

    /** Pages that belong in the tab; anything else (help pages, other apps) opens outside. */
    fun owns(host: String) = hosts.any { host == it || host.endsWith(".$it") }

    /** Both sites keep their login in a "sessionid" cookie. */
    val signedIn: Boolean get() = CookieManager.getInstance().getCookie(home)?.split(';')?.any { it.trim().startsWith("sessionid=") && it.trim().length > "sessionid=".length } == true

    /** Signs out by deleting this site's cookies (only this site's). */
    fun signOut() {
        val cm = CookieManager.getInstance()
        val domain = Uri.parse(home).host!!.removePrefix("www.")
        cm.getCookie(home)?.split(';')?.map { it.substringBefore('=').trim() }?.filter { it.isNotEmpty() }?.forEach { name ->
            for (d in listOf(domain, ".$domain", "www.$domain")) cm.setCookie(home, "$name=; Max-Age=0; Path=/; Domain=$d")
            cm.setCookie(home, "$name=; Max-Age=0; Path=/")
        }
        cm.flush()
        SiteViews.reload(this)
    }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/**
 * One WebView per site for as long as the app is open, so switching tabs keeps your place.
 * Only the visible one plays.
 */
object SiteViews {
    private val views = mutableMapOf<Site, WebView>()

    /**
     * The sites serve their full versions to Chrome, so present as Chrome rather than an in-app
     * browser: mobile Chrome on phones, desktop Chrome on wide screens, where their mobile layouts
     * (made for a phone held upright) break.
     */
    internal fun chromeAgent(webViewAgent: String, wide: Boolean): String {
        val mobile = webViewAgent.replace("; wv)", ")").replace(Regex(" Version/\\d+\\.\\d+"), "")
        if (!wide) return mobile
        val chrome = Regex("Chrome/([\\d.]+)").find(webViewAgent)?.groupValues?.get(1) ?: "130.0.0.0"
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chrome Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun get(context: Context, site: Site, url: String = site.feedUrl): WebView = views.getOrPut(site) {
        // Debug builds can be inspected from a computer (chrome://inspect); release builds can't.
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
        WebView(context).apply {
            setBackgroundColor(site.backdrop)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            val wide = context.resources.configuration.screenWidthDp >= 900
            settings.userAgentString = chromeAgent(settings.userAgentString, wide)
            settings.useWideViewPort = wide
            settings.loadWithOverviewMode = wide
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // The site-tidying script, before the sites' own scripts run (or on page load on old WebViews).
            val tidy = context.assets.open("sites/tidy.js").bufferedReader().use { it.readText() }
            val early = androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)
            if (early) androidx.webkit.WebViewCompat.addDocumentStartJavaScript(this, tidy, setOf(site.home, site.home.replace("www.", "m.")))
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    if (!early) view.evaluateJavascript(tidy, null)
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "https" && site.owns(uri.host.orEmpty())) return false
                    // App links (tiktok://, intent://) and other sites open outside.
                    runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    return true
                }

                private var wasSignedIn = site.signedIn

                override fun onPageFinished(view: WebView, url: String?) {
                    CookieManager.getInstance().flush()
                    // Both sites land on their home page after logging in; take you to the videos instead.
                    val now = site.signedIn
                    if (now && !wasSignedIn && url?.startsWith(site.feedUrl) != true) view.loadUrl(site.feedUrl)
                    wasSignedIn = now
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }
    }

    fun open(context: Context, site: Site, url: String) {
        val view = views[site]
        if (view == null) get(context, site, url) else view.loadUrl(url)
    }

    fun reload(site: Site) {
        views[site]?.loadUrl(site.feedUrl)
    }

    /** Pauses a site's videos and timers while it's off screen. */
    fun pause(site: Site) {
        views[site]?.apply {
            evaluateJavascript("document.querySelectorAll('video').forEach(v=>v.pause())", null)
            onPause()
        }
    }

    fun resume(site: Site) {
        views[site]?.onResume()
    }

    fun destroyAll() {
        views.values.forEach { (it.parent as? ViewGroup)?.removeView(it); it.destroy() }
        views.clear()
    }
}

/** A site's feed, full screen. Back goes back within the site first. */
@Composable
fun SiteFeed(site: Site, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = remember(site) { SiteViews.get(context, site) }
    var canGoBack by remember { mutableStateOf(false) }
    BackHandler(enabled = canGoBack) { view.goBack() }
    DisposableEffect(site) {
        SiteViews.resume(site)
        // WebView doesn't announce history changes, so check twice a second while visible.
        val ticker = object : Runnable {
            override fun run() { canGoBack = view.canGoBack(); view.postDelayed(this, 500) }
        }
        view.post(ticker)
        onDispose {
            view.removeCallbacks(ticker)
            SiteViews.pause(site)
            (view.parent as? ViewGroup)?.removeView(view)
        }
    }
    AndroidView(factory = { (view.parent as? ViewGroup)?.removeView(view); view }, modifier = modifier.fillMaxSize())
}
