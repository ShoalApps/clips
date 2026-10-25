@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package io.github.tintloom.clips

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.tintloom.clips.data.VideoItem
import io.github.tintloom.clips.ui.components.openExternal
import io.github.tintloom.clips.ui.screens.ShortsLaunch
import io.github.tintloom.clips.ui.screens.ShortsScreen
import io.github.tintloom.clips.ui.theme.TubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Screen {
    data object Feed : Screen
    data object Settings : Screen
    data class Embed(val target: ClipLinks.Embed) : Screen
}

class ClipsActivity : ComponentActivity() {
    private val incoming = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun linkFrom(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }
}

@Composable
private fun ClipsRoot(incoming: String?, onHandled: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Feed) }
    var pasting by remember { mutableStateOf(false) }
    var feedKey by remember { mutableStateOf(0) }

    /** Sends a link to the right place: YouTube Shorts into the feed, TikTok and Reels to their official players. */
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
                feedKey++
            }
            is ClipLinks.Embed -> screen = Screen.Embed(target)
            else -> io.github.tintloom.clips.ui.components.toast(context, "That link isn't a short video Tintloom Clips can play")
        }
    }

    LaunchedEffect(incoming) {
        if (incoming != null) { open(incoming); onHandled() }
    }
    BackHandler(enabled = screen != Screen.Feed) { screen = Screen.Feed }

    when (val s = screen) {
        Screen.Feed -> Box(Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.runtime.key(feedKey) {
                ShortsScreen(onChannel = { url -> openExternal(context, url) }, onExit = onExit)
            }
            Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                val colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = .35f), contentColor = Color.White)
                FilledTonalIconButton(onClick = { pasting = true }, colors = colors) { Icon(Icons.Rounded.AddLink, "Open a TikTok, Reels or Shorts link") }
                FilledTonalIconButton(onClick = { screen = Screen.Settings }, colors = colors) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        }
        Screen.Settings -> ClipsSettings(onBack = { screen = Screen.Feed })
        is Screen.Embed -> EmbedScreen(s.target, onBack = { screen = Screen.Feed })
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

/** A TikTok or Instagram video in the site's own official embed player. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbedScreen(target: ClipLinks.Embed, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(target.site) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target.original))) } }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, "Open in ${target.site}")
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding())) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        // The official players keep their session in cookies.
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        loadUrl(target.embedUrl)
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Text(
                "Played with ${target.site}'s official embed player.",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = .7f),
                modifier = Modifier.padding(12.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}

// ---------------- Settings ----------------

private class GroupBuilder {
    val rows = mutableListOf<@Composable (ListItemShapes) -> Unit>()
    fun row(content: @Composable (ListItemShapes) -> Unit) { rows += content }
}

private fun LazyListScope.group(title: String, build: GroupBuilder.() -> Unit) {
    val rows = GroupBuilder().apply(build).rows
    if (rows.isEmpty()) return
    item(key = "t-$title") {
        Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp))
    }
    rows.forEachIndexed { i, r ->
        item(key = "$title-$i") {
            Box(Modifier.padding(horizontal = 16.dp, vertical = ListItemDefaults.SegmentedGap / 2)) { r(ListItemDefaults.segmentedShapes(i, rows.size)) }
        }
    }
}

@Composable
private fun tileColors() = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    selectedContentColor = MaterialTheme.colorScheme.onSurface,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun Toggle(title: String, body: String?, checked: Boolean, shapes: ListItemShapes, onChange: (Boolean) -> Unit) {
    SegmentedListItem(
        checked = checked, onCheckedChange = onChange, shapes = shapes,
        supportingContent = body?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = tileColors(),
    ) { Text(title) }
}

@Composable
private fun Link(title: String, body: String?, shapes: ListItemShapes, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick, shapes = shapes,
        supportingContent = body?.let { { Text(it) } },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) },
        colors = tileColors(),
    ) { Text(title) }
}

@Composable
private fun Choices(title: String, labels: List<String>, selected: Int, shapes: ListItemShapes, onSelect: (Int) -> Unit) {
    Surface(shape = shapes.shape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, l ->
                    SegmentedButton(selected = selected == i, onClick = { onSelect(i) }, shape = SegmentedButtonDefaults.itemShape(i, labels.size)) { Text(l) }
                }
            }
        }
    }
}

@Composable
private fun ClipsSettings(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = AppGraph.store
    val s by store.settings.collectAsState()
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?" }
    var picking by remember { mutableStateOf<Triple<String, List<Int>, (Int) -> Unit>?>(null) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = pad.calculateTopPadding()).wrapContentWidth().widthIn(max = 680.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            group("Appearance") {
                row { sh -> Choices("Theme", listOf("Auto", "Light", "Dark"), s.theme, sh) { i -> store.updateSettings { it.copy(theme = i) } } }
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    row { sh -> Toggle("Dynamic colour", "Match colours to your wallpaper", s.dynamicColor, sh) { v -> store.updateSettings { it.copy(dynamicColor = v) } } }
                }
            }
            group("Feed") {
                val keys = listOf("youtube", "tiktok", "reels")
                row { sh -> Choices("Layout", listOf("YouTube", "TikTok", "Reels"), keys.indexOf(s.shortsLayout).coerceAtLeast(0), sh) { i -> store.updateSettings { it.copy(shortsLayout = keys[i]) } } }
                row { sh ->
                    Link("Limit per visit", if (s.shortsSessionLimit == 0) "No limit" else "${s.shortsSessionLimit} clips, then a break screen", sh) {
                        picking = Triple("Clips per visit", listOf(0, 5, 10, 20, 50)) { v -> store.updateSettings { it.copy(shortsSessionLimit = v) } }
                    }
                }
                row { sh ->
                    Link("Daily time limit", if (s.shortsDailyMinutes == 0) "No limit" else "${s.shortsDailyMinutes} minutes a day", sh) {
                        picking = Triple("Minutes per day", listOf(0, 10, 15, 30, 60, 90)) { v -> store.updateSettings { it.copy(shortsDailyMinutes = v) } }
                    }
                }
                row { sh -> Toggle("Return YouTube Dislike", "Show dislike counts", s.ryd, sh) { v -> store.updateSettings { it.copy(ryd = v) } } }
            }
            group("Updates") {
                row { sh -> Toggle("Check for updates", "Look for a new version on GitHub once a day", s.autoUpdate, sh) { v -> store.updateSettings { it.copy(autoUpdate = v) } } }
                row { sh -> Link("Check now", "Version $version", sh) { AppGraph.checkForUpdate(force = true) } }
            }
            group("About") {
                row { sh -> Link("Source code", "github.com/Tintloom/clips", sh) { openExternal(context, "https://github.com/Tintloom/clips") } }
                row { sh -> Link("Tintloom", "tintloom.github.io: all the apps and their credits", sh) { openExternal(context, "https://tintloom.github.io") } }
                row { sh ->
                    Link(
                        "GNU GPL v3 or later",
                        "Free software. Not affiliated with Google, YouTube, TikTok, ByteDance, Instagram or Meta.",
                        sh,
                    ) { openExternal(context, "https://www.gnu.org/licenses/gpl-3.0.html") }
                }
            }
        }
    }
    picking?.let { (title, options, onPick) ->
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { v ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = false, onClick = { onPick(v); picking = null })
                            Text(if (v == 0) "No limit" else "$v", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = null }) { Text("Close") } },
        )
    }
}
