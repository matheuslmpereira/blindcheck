package com.theustech.blindcheck_desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

// ── Inter color scheme ────────────────────────────────────────────────────────

private val InterColorScheme = lightColorScheme(
    primary            = Color(0xFFFF6B00),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFFFDCC8),
    onPrimaryContainer = Color(0xFF3A0A00),
    secondary          = Color(0xFF7A5436),
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFFFDCC0),
    onSecondaryContainer = Color(0xFF2C1600),
    surface            = Color(0xFFFFFBFF),
    onSurface          = Color(0xFF201A17),
    surfaceVariant     = Color(0xFFF5DED5),
    onSurfaceVariant   = Color(0xFF52443D),
    outline            = Color(0xFF85746C),
    outlineVariant     = Color(0xFFD8C2BA),
    error              = Color(0xFFBA1A1A),
    onError            = Color.White,
    background         = Color(0xFFFFFBFF),
    onBackground       = Color(0xFF201A17),
)

// ── ADB resolution ────────────────────────────────────────────────────────────

private fun resolveAdb(): String {
    val sdkRoot = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: "${System.getProperty("user.home")}/Library/Android/sdk"
    val fromSdk = File("$sdkRoot/platform-tools/adb")
    if (fromSdk.canExecute()) return fromSdk.absolutePath
    return "adb"
}

private val ADB = resolveAdb()

// ── Broadcast constants ───────────────────────────────────────────────────────

private const val TRACKING_PKG       = "com.theustech.blindcheck_tracking_app"
private const val ANNOUNCE_TAG       = "BlindCheckAnnounce"
private const val ACTION_NEXT        = "com.theustech.blindcheck.ACTION_NEXT"
private const val ACTION_PREVIOUS    = "com.theustech.blindcheck.ACTION_PREVIOUS"
private const val ACTION_ACTIVATE    = "com.theustech.blindcheck.ACTION_ACTIVATE"
private const val ACTION_BACK        = "com.theustech.blindcheck.ACTION_BACK"
private const val ACTION_SCROLL_FWD  = "com.theustech.blindcheck.ACTION_SCROLL_FORWARD"
private const val ACTION_SCROLL_BACK = "com.theustech.blindcheck.ACTION_SCROLL_BACKWARD"
private const val ACTION_HOME        = "com.theustech.blindcheck.ACTION_HOME"
private const val ACTION_RECENTS     = "com.theustech.blindcheck.ACTION_RECENTS"
private const val ACTION_SWIPE_UP    = "com.theustech.blindcheck.ACTION_SWIPE_UP"
private const val ACTION_SWIPE_DOWN  = "com.theustech.blindcheck.ACTION_SWIPE_DOWN"

// ── ADB helpers ───────────────────────────────────────────────────────────────

data class AdbResult(val success: Boolean, val output: String)

private fun runAdb(vararg args: String): AdbResult = try {
    val process = ProcessBuilder(ADB, *args).redirectErrorStream(true).start()
    val timedOut = !process.waitFor(5, TimeUnit.SECONDS)
    val output = process.inputStream.bufferedReader().readText().trim()
    if (timedOut) process.destroyForcibly()
    AdbResult(success = !timedOut && process.exitValue() == 0, output = output)
} catch (e: Exception) {
    AdbResult(success = false, output = "Error: ${e.message}")
}

private fun sendBroadcast(action: String): AdbResult =
    runAdb("shell", "am", "broadcast", "-p", TRACKING_PKG, "-a", action)

private fun connectedDevice(): String? {
    val result = runAdb("devices")
    return result.output.lines().drop(1).firstOrNull { it.contains("\tdevice") }
        ?.substringBefore("\t")?.trim()
}

// Parses a logcat line and returns the message after the tag, or null.
// Logcat format: "MM-DD HH:MM:SS.mmm  PID  TID I BlindCheckAnnounce: <message>"
private fun parseAnnouncement(line: String): String? {
    val marker = "$ANNOUNCE_TAG: "
    val idx = line.indexOf(marker)
    return if (idx >= 0) line.substring(idx + marker.length).trim() else null
}

// ── Button components ─────────────────────────────────────────────────────────

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val iconSize = (size.value * 0.42f).dp

    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) cs.primaryContainer else cs.surfaceVariant,
        contentColor = if (enabled) cs.onPrimaryContainer else cs.onSurfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, if (enabled) cs.primary.copy(alpha = 0.25f) else cs.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(iconSize))
            if (size > 52.dp) {
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    fontSize = (size.value * 0.13f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ActivateButton(size: Dp, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val iconSize = (size.value * 0.38f).dp

    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) cs.primary else cs.surfaceVariant,
        contentColor = if (enabled) cs.onPrimary else cs.onSurfaceVariant.copy(alpha = 0.38f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.TouchApp, contentDescription = "Activate", modifier = Modifier.size(iconSize))
            if (size > 52.dp) {
                Spacer(Modifier.height(2.dp))
                Text("OK", fontSize = (size.value * 0.15f).sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SystemButton(
    icon: ImageVector,
    label: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val iconSize = (size.value * 0.4f).dp

    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = cs.surface,
        contentColor = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(iconSize))
            if (size > 52.dp) {
                Spacer(Modifier.height(2.dp))
                Text(
                    label,
                    fontSize = (size.value * 0.13f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── D-pad cross ───────────────────────────────────────────────────────────────

@Composable
private fun DPadCross(buttonSize: Dp, gap: Dp, enabled: Boolean, onSend: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        // Row 1: [ · ] [ scroll fwd ] [ · ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.size(buttonSize))
            NavButton(Icons.Rounded.KeyboardArrowUp, "Scroll", buttonSize, enabled) {
                onSend(ACTION_SCROLL_FWD, "Scroll Forward")
            }
            Spacer(Modifier.size(buttonSize))
        }
        // Row 2: [ prev ] [ activate ] [ next ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Prev", buttonSize, enabled) {
                onSend(ACTION_PREVIOUS, "Previous")
            }
            ActivateButton(buttonSize, enabled) { onSend(ACTION_ACTIVATE, "Activate") }
            NavButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next", buttonSize, enabled) {
                onSend(ACTION_NEXT, "Next")
            }
        }
        // Row 3: [ · ] [ scroll back ] [ · ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.size(buttonSize))
            NavButton(Icons.Rounded.KeyboardArrowDown, "Scroll", buttonSize, enabled) {
                onSend(ACTION_SCROLL_BACK, "Scroll Backward")
            }
            Spacer(Modifier.size(buttonSize))
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

// ── Remote panel ──────────────────────────────────────────────────────────────

@Composable
private fun RemotePanel(
    device: String?,
    checking: Boolean,
    onRefresh: () -> Unit,
    onSend: (String, String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
        val buttonSize: Dp = (maxHeight.value * 0.105f).coerceIn(36f, 86f).dp
        val gap = 4.dp
        val rowWidth = buttonSize * 3 + gap * 2
        val panelWidth = rowWidth + 24.dp
        val enabled = device != null

        Column(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Device status ────────────────────────────────────────────────
            Row(
                modifier = Modifier.width(rowWidth),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    val dotColor = if (device != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error
                    Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                    Text(
                        text = device ?: "No device",
                        style = MaterialTheme.typography.bodySmall,
                        color = dotColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    onClick = onRefresh,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.width(rowWidth), color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.weight(1f))

            // ── Navigation D-pad ─────────────────────────────────────────────
            SectionLabel("Navegação")
            DPadCross(buttonSize, gap, enabled, onSend)

            Spacer(Modifier.height(4.dp))

            // ── Swipe ────────────────────────────────────────────────────────
            SectionLabel("Swipe")
            Row(
                modifier = Modifier.width(rowWidth),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                NavButton(Icons.Rounded.SwipeUp, "Swipe ↑", buttonSize, enabled) {
                    onSend(ACTION_SWIPE_UP, "Swipe Up")
                }
                NavButton(Icons.Rounded.SwipeDown, "Swipe ↓", buttonSize, enabled) {
                    onSend(ACTION_SWIPE_DOWN, "Swipe Down")
                }
                Spacer(Modifier.size(buttonSize))
            }

            Spacer(Modifier.weight(1f))

            // ── System (bottom) ──────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.width(rowWidth), color = MaterialTheme.colorScheme.outlineVariant)
            SectionLabel("Sistema")
            Row(
                modifier = Modifier.width(rowWidth),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                SystemButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back",    buttonSize, enabled) { onSend(ACTION_BACK,    "Back") }
                SystemButton(Icons.Rounded.Home,                   "Home",    buttonSize, enabled) { onSend(ACTION_HOME,    "Home") }
                SystemButton(Icons.Rounded.Apps,                   "Recents", buttonSize, enabled) { onSend(ACTION_RECENTS, "Recents") }
            }
        }
    }
}

// ── Log panel ─────────────────────────────────────────────────────────────────

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val cs = MaterialTheme.colorScheme
    when (entry) {
        is LogEntry.Announce -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (entry.isWindow) cs.primaryContainer.copy(alpha = 0.5f)
                        else cs.primaryContainer.copy(alpha = 0.25f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (entry.isWindow) Icons.Rounded.Smartphone else Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = cs.primary,
                )
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = cs.onPrimaryContainer,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
            }
        }
        is LogEntry.Action -> {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (entry.success) "✓" else "✗",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.success) cs.primary else cs.error,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        is LogEntry.System -> {
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodySmall,
                color = cs.outline,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun LogPanel(entries: List<LogEntry>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.padding(start = 0.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Text("Anúncios", style = MaterialTheme.typography.labelSmall, color = cs.outline)
            Spacer(Modifier.weight(1f))
            // Legend
            Icon(Icons.Rounded.Smartphone, null, Modifier.size(10.dp), tint = cs.primary)
            Text("tela", style = MaterialTheme.typography.labelSmall, color = cs.outline)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.VolumeUp, null, Modifier.size(10.dp), tint = cs.primary)
            Text("anúncio", style = MaterialTheme.typography.labelSmall, color = cs.outline)
        }
        val scrollState = rememberScrollState()
        LaunchedEffect(entries.size) { if (entries.isNotEmpty()) scrollState.scrollTo(0) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .verticalScroll(scrollState),
        ) {
            if (entries.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodySmall, color = cs.outline, fontFamily = FontFamily.Monospace)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    entries.forEach { LogEntryRow(it) }
                }
            }
        }
    }
}

// ── App root ──────────────────────────────────────────────────────────────────

sealed interface LogEntry {
    data class Action(val text: String, val success: Boolean) : LogEntry
    data class Announce(val text: String, val isWindow: Boolean) : LogEntry
    data class System(val text: String) : LogEntry
}

@Composable
@Preview
fun RemoteControlApp() {
    val scope = rememberCoroutineScope()
    var device by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    val logEntries = remember { mutableStateListOf<LogEntry>() }

    fun appendLog(entry: LogEntry) {
        logEntries.add(0, entry)
        if (logEntries.size > 200) logEntries.removeAt(logEntries.lastIndex)
    }

    fun refreshDevice() {
        scope.launch {
            checking = true
            device = withContext(Dispatchers.IO) { connectedDevice() }
            checking = false
            appendLog(LogEntry.System(
                if (device != null) "● Device: $device" else "✗ No device — is ADB running?"
            ))
        }
    }

    fun send(action: String, label: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { sendBroadcast(action) }
            appendLog(LogEntry.Action(label, result.success))
        }
    }

    // Poll logcat for accessibility announcements emitted by the tracking service.
    LaunchedEffect(device) {
        if (device == null) return@LaunchedEffect
        // Bootstrap: mark current logcat position so we only show new announcements.
        var lastSeen = withContext(Dispatchers.IO) {
            runAdb("logcat", "-d", "-s", "$ANNOUNCE_TAG:I").output
                .lines().lastOrNull { ANNOUNCE_TAG in it } ?: ""
        }
        while (true) {
            delay(500)
            val result = withContext(Dispatchers.IO) {
                runAdb("logcat", "-d", "-s", "$ANNOUNCE_TAG:I")
            }
            val newLines = result.output.lines()
                .filter { ANNOUNCE_TAG in it && it > lastSeen }
            newLines.forEach { line ->
                val msg = parseAnnouncement(line) ?: return@forEach
                val isWindow = msg.startsWith("Janela: ")
                appendLog(LogEntry.Announce(msg, isWindow))
            }
            if (newLines.isNotEmpty()) lastSeen = newLines.last()
        }
    }

    LaunchedEffect(Unit) { refreshDevice() }

    MaterialTheme(colorScheme = InterColorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                RemotePanel(
                    device = device,
                    checking = checking,
                    onRefresh = { refreshDevice() },
                    onSend = { action, label -> send(action, label) },
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LogPanel(
                    entries = logEntries,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BlindCheck Remote",
        state = WindowState(width = 760.dp, height = 580.dp),
    ) {
        RemoteControlApp()
    }
}
