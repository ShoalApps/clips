@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.swordfishapps.clips

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.swordfishapps.clips.data.VideoItem
import io.github.swordfishapps.clips.ui.components.openExternal
import io.github.swordfishapps.clips.ui.screens.ShortsLaunch
import io.github.swordfishapps.clips.ui.screens.ShortsScreen
import io.github.swordfishapps.clips.ui.theme.TubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Screen {
    data object Feed : Screen
    data object Settings : Screen
    data object YouTubeSignIn : Screen
    data object ChannelPicker : Screen
    data object Developer : Screen
    data object About : Screen
}

class ClipsActivity : ComponentActivity() {
    private val incoming = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataMover.offerUninstall(this)
        enableEdgeToEdge()
        incoming.value = linkFrom(intent)
        AppGraph.checkForUpdate()
        setContent {
            val settings by AppGraph.store.settings.collectAsState()
            TubeTheme(settings) { ClipsRoot(incoming.value, onHandled = { incoming.value = null }, onExit = ::finish) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming.value = linkFrom(intent)
    }

    override fun onDestroy() {
        if (isFinishing) SiteViews.destroyAll()
        super.onDestroy()
    }

    private fun linkFrom(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }
}

@Composable
private fun ClipsRoot(incoming: String?, onHandled: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val s by AppGraph.store.settings.collectAsState()
    var screen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var pasting by remember { mutableStateOf(false) }
    var feedKey by remember { mutableStateOf(0) }
    val tabs = buildList {
        if (!s.shortsDisabled) add("shorts")
        if (s.showTikTok) add("tiktok")
        if (s.showReels) add("reels")
    }.ifEmpty { listOf("shorts") }
    var tab by remember { mutableStateOf(s.startTab) }
    if (tab !in tabs) tab = tabs.first()

    /** Opens a site in its tab, at a link or its login page. */
    fun showSite(site: Site, url: String) {
        SiteViews.open(context, site, url)
        screen = Screen.Feed
        tab = site.key
    }

    /** Sends a link to the right place: YouTube Shorts into the Shorts feed, TikTok and Reels into their tabs. */
    suspend fun open(text: String) {
        var target = ClipLinks.parse(text)
        if (target is ClipLinks.Unresolved) {
            target = runCatching {
                withContext(Dispatchers.IO) {
                    AppGraph.http.newCall(okhttp3.Request.Builder().url(target.url).head().build()).execute().use { it.request.url.toString() }
                }
            }.getOrNull()?.let { ClipLinks.fromResolved(it, text) }
        }
        when (target) {
            is ClipLinks.YouTubeShort -> {
                ShortsLaunch.start = VideoItem(
                    url = "https://www.youtube.com/shorts/${target.id}", id = target.id, title = "", channelName = "",
                    thumbnail = "https://i.ytimg.com/vi/${target.id}/hqdefault.jpg", isShort = true,
                )
                screen = Screen.Feed
                tab = "shorts"
                feedKey++
            }
            is ClipLinks.Embed -> showSite(if (target.site == "TikTok") Site.TIKTOK else Site.REELS, target.original)
            else -> io.github.swordfishapps.clips.ui.components.toast(context, "That link isn't a short video Swordfish Clips can play")
        }
    }

    LaunchedEffect(incoming) {
        if (incoming != null) { open(incoming); onHandled() }
    }
    BackHandler(enabled = screen != Screen.Feed) {
        screen = when (screen) { Screen.About, Screen.Developer, Screen.YouTubeSignIn, Screen.ChannelPicker -> Screen.Settings; else -> Screen.Feed }
    }

    when (screen) {
        Screen.Feed -> Box(Modifier.fillMaxSize().background(Color.Black)) {
            when (tab) {
                "shorts" -> androidx.compose.runtime.key(feedKey) {
                    ShortsScreen(onChannel = { url -> openExternal(context, url) }, onExit = onExit)
                }
                else -> SiteFeed(Site.of(tab)!!, Modifier.statusBarsPadding().padding(top = if (tabs.size > 1) 52.dp else 0.dp))
            }
            if (tabs.size > 1) SourceTabs(tabs, tab, Modifier.align(Alignment.TopCenter).statusBarsPadding()) { tab = it }
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                val colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = .35f), contentColor = Color.White)
                FilledTonalIconButton(onClick = { pasting = true }, colors = colors) { Icon(Icons.Rounded.AddLink, "Open a TikTok, Reels or Shorts link") }
                FilledTonalIconButton(onClick = { screen = Screen.Settings }, colors = colors) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        }
        Screen.Settings -> io.github.swordfishapps.clips.ui.screens.ClipsSettingsScreen(
            onBack = { screen = Screen.Feed },
            onYouTubeSignIn = { screen = Screen.YouTubeSignIn },
            onSwitchChannel = { screen = Screen.ChannelPicker },
            onSiteSignIn = { site -> showSite(site, site.loginUrl) },
            onDeveloper = { screen = Screen.Developer },
            onAbout = { screen = Screen.About },
        )
        Screen.YouTubeSignIn -> io.github.swordfishapps.clips.ui.screens.LoginScreen(onDone = { screen = Screen.Settings }, onBack = { screen = Screen.Settings })
        Screen.ChannelPicker -> io.github.swordfishapps.clips.ui.screens.ChannelPickerScreen(onDone = { screen = Screen.Settings }, onBack = { screen = Screen.Settings })
        Screen.Developer -> io.github.swordfishapps.clips.ui.screens.DeveloperSettingsScreen(onBack = { screen = Screen.Settings })
        Screen.About -> io.github.swordfishapps.clips.ui.screens.AboutScreen(onBack = { screen = Screen.Settings })
    }

    if (pasting) {
        var text by remember { mutableStateOf("") }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { pasting = false },
            title = { Text("Open a link") },
            text = {
                Column {
                    Text("Paste a YouTube Shorts, TikTok or Instagram Reels link.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, placeholder = { Text("https://…") })
                }
            },
            confirmButton = { TextButton(onClick = { pasting = false; val t = text; scope.launch { open(t) } }, enabled = text.isNotBlank()) { Text("Open") } },
            dismissButton = { TextButton(onClick = { pasting = false }) { Text("Cancel") } },
        )
    }
}

/** Shorts · TikTok · Reels along the top, like the sites' own "Following · For You" switch. */
@Composable
private fun SourceTabs(tabs: List<String>, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val names = mapOf("shorts" to "Shorts", "tiktok" to "TikTok", "reels" to "Reels")
    Row(modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (t in tabs) {
            val on = t == selected
            Column(
                Modifier.clip(RoundedCornerShape(50)).clickable { onSelect(t) }.padding(horizontal = 14.dp, vertical = 6.dp)
                    .semantics { this.selected = on; role = Role.Tab },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    names.getValue(t),
                    color = if (on) Color.White else Color.White.copy(alpha = .65f),
                    style = MaterialTheme.typography.titleMedium.copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .6f), blurRadius = 6f)),
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
                Box(Modifier.padding(top = 4.dp).size(width = 22.dp, height = 3.dp).clip(RoundedCornerShape(50)).background(if (on) Color.White else Color.Transparent))
            }
        }
    }
}
