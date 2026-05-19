package com.theustech.blindcheck_desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

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

private const val TRACKING_PKG        = "com.theustech.blindcheck_tracking_app"
private const val ACTION_NEXT         = "com.theustech.blindcheck.ACTION_NEXT"
private const val ACTION_PREVIOUS     = "com.theustech.blindcheck.ACTION_PREVIOUS"
private const val ACTION_ACTIVATE     = "com.theustech.blindcheck.ACTION_ACTIVATE"
private const val ACTION_BACK         = "com.theustech.blindcheck.ACTION_BACK"
private const val ACTION_SCROLL_FWD   = "com.theustech.blindcheck.ACTION_SCROLL_FORWARD"
private const val ACTION_SCROLL_BACK  = "com.theustech.blindcheck.ACTION_SCROLL_BACKWARD"
private const val ACTION_HOME         = "com.theustech.blindcheck.ACTION_HOME"
private const val ACTION_RECENTS      = "com.theustech.blindcheck.ACTION_RECENTS"
private const val ACTION_SWIPE_UP     = "com.theustech.blindcheck.ACTION_SWIPE_UP"
private const val ACTION_SWIPE_DOWN   = "com.theustech.blindcheck.ACTION_SWIPE_DOWN"

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

// ── Button components ─────────────────────────────────────────────────────────

@Composable
private fun NavButton(
    symbol: String,
    label: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (enabled) cs.secondaryContainer else cs.surfaceVariant,
        contentColor = if (enabled) cs.onSecondaryContainer else cs.onSurfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, cs.outline.copy(alpha = if (enabled) 1f else 0.3f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(symbol, fontSize = (size.value * 0.28f).sp, textAlign = TextAlign.Center)
            if (size > 52.dp) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (size.value * 0.14f).sp,
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
            Text("OK", fontSize = (size.value * 0.24f).sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SecondaryButton(
    symbol: String,
    label: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = cs.surface,
        contentColor = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(symbol, fontSize = (size.value * 0.28f).sp, textAlign = TextAlign.Center)
            if (size > 52.dp) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (size.value * 0.14f).sp,
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
private fun DPadCross(
    buttonSize: Dp,
    gap: Dp,
    enabled: Boolean,
    onSend: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        // Row 1:  [ empty ]  [ scroll fwd ]  [ empty ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.size(buttonSize))
            NavButton("▲", "Scroll", buttonSize, enabled) { onSend(ACTION_SCROLL_FWD, "Scroll Forward") }
            Spacer(Modifier.size(buttonSize))
        }
        // Row 2:  [ prev ]  [ activate ]  [ next ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            NavButton("◀", "Prev", buttonSize, enabled) { onSend(ACTION_PREVIOUS, "Previous") }
            ActivateButton(buttonSize, enabled) { onSend(ACTION_ACTIVATE, "Activate") }
            NavButton("▶", "Next", buttonSize, enabled) { onSend(ACTION_NEXT, "Next") }
        }
        // Row 3:  [ empty ]  [ scroll back ]  [ empty ]
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.size(buttonSize))
            NavButton("▼", "Scroll", buttonSize, enabled) { onSend(ACTION_SCROLL_BACK, "Scroll Backward") }
            Spacer(Modifier.size(buttonSize))
        }
    }
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
        // Button size scales with window height; clamped to a sensible range.
        val buttonSize: Dp = (maxHeight.value * 0.105f).coerceIn(36f, 86f).dp
        val gap = 4.dp
        val panelWidth = buttonSize * 3 + gap * 2 + 16.dp
        val enabled = device != null

        Column(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Device status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (checking) {
                    CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.5.dp)
                } else {
                    val color = if (device != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Text(
                        text = device ?: "No device",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
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
                    Text("↺", style = MaterialTheme.typography.labelSmall)
                }
            }

            HorizontalDivider()

            Spacer(Modifier.weight(1f))

            // D-pad cross
            DPadCross(buttonSize, gap, enabled, onSend)

            Spacer(Modifier.height(4.dp))

            // System row: Back · Home · Recents
            Row(
                modifier = Modifier.width(buttonSize * 3 + gap * 2),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                SecondaryButton("⬅", "Back",    buttonSize, enabled) { onSend(ACTION_BACK,    "Back") }
                SecondaryButton("⌂", "Home",    buttonSize, enabled) { onSend(ACTION_HOME,    "Home") }
                SecondaryButton("▣", "Recents", buttonSize, enabled) { onSend(ACTION_RECENTS, "Recents") }
            }

            // Swipe row: Swipe Up · Swipe Down · (spacer)
            Row(
                modifier = Modifier.width(buttonSize * 3 + gap * 2),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                SecondaryButton("↑", "Swipe↑", buttonSize, enabled) { onSend(ACTION_SWIPE_UP,   "Swipe Up") }
                SecondaryButton("↓", "Swipe↓", buttonSize, enabled) { onSend(ACTION_SWIPE_DOWN, "Swipe Down") }
                Spacer(Modifier.size(buttonSize))
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Log panel ─────────────────────────────────────────────────────────────────

@Composable
private fun LogPanel(entries: List<String>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.padding(start = 0.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)) {
        Text(
            "Log",
            style = MaterialTheme.typography.labelSmall,
            color = cs.outline,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        val scrollState = rememberScrollState()
        LaunchedEffect(entries.size) { if (entries.isNotEmpty()) scrollState.scrollTo(0) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                .padding(8.dp)
                .verticalScroll(scrollState),
        ) {
            if (entries.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodySmall, color = cs.outline, fontFamily = FontFamily.Monospace)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    entries.forEach { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (entry.startsWith("✗")) cs.error else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── App root ──────────────────────────────────────────────────────────────────

@Composable
@Preview
fun RemoteControlApp() {
    val scope = rememberCoroutineScope()
    var device by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    val logEntries = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        logEntries.add(0, line)
        if (logEntries.size > 200) logEntries.removeAt(logEntries.lastIndex)
    }

    fun refreshDevice() {
        scope.launch {
            checking = true
            device = withContext(Dispatchers.IO) { connectedDevice() }
            checking = false
            appendLog(if (device != null) "● Device: $device" else "✗ No device — is ADB running?")
        }
    }

    fun send(action: String, label: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { sendBroadcast(action) }
            appendLog(if (result.success) "✓ $label" else "✗ $label — ${result.output}")
        }
    }

    LaunchedEffect(Unit) { refreshDevice() }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                RemotePanel(
                    device = device,
                    checking = checking,
                    onRefresh = { refreshDevice() },
                    onSend = { action, label -> send(action, label) },
                )
                VerticalDivider()
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
