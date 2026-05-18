package com.theustech.blindcheck_desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
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
    return "adb" // fall back to PATH
}

private val ADB = resolveAdb()

// ── Broadcast constants ───────────────────────────────────────────────────────

private const val TRACKING_PKG   = "com.theustech.blindcheck_tracking_app"
private const val ACTION_NEXT    = "com.theustech.blindcheck.ACTION_NEXT"
private const val ACTION_PREVIOUS = "com.theustech.blindcheck.ACTION_PREVIOUS"
private const val ACTION_ACTIVATE = "com.theustech.blindcheck.ACTION_ACTIVATE"
private const val ACTION_BACK    = "com.theustech.blindcheck.ACTION_BACK"
private const val ACTION_SCROLL_FORWARD  = "com.theustech.blindcheck.ACTION_SCROLL_FORWARD"
private const val ACTION_SCROLL_BACKWARD = "com.theustech.blindcheck.ACTION_SCROLL_BACKWARD"
private const val ACTION_HOME     = "com.theustech.blindcheck.ACTION_HOME"
private const val ACTION_RECENTS  = "com.theustech.blindcheck.ACTION_RECENTS"
private const val ACTION_SWIPE_UP   = "com.theustech.blindcheck.ACTION_SWIPE_UP"
private const val ACTION_SWIPE_DOWN = "com.theustech.blindcheck.ACTION_SWIPE_DOWN"

// ── ADB helpers ───────────────────────────────────────────────────────────────

data class AdbResult(val success: Boolean, val output: String)

private fun runAdb(vararg args: String): AdbResult {
    return try {
        val process = ProcessBuilder(ADB, *args)
            .redirectErrorStream(true)
            .start()
        val timedOut = !process.waitFor(5, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText().trim()
        if (timedOut) process.destroyForcibly()
        AdbResult(success = !timedOut && process.exitValue() == 0, output = output)
    } catch (e: Exception) {
        AdbResult(success = false, output = "Error: ${e.message}")
    }
}

private fun sendBroadcast(action: String): AdbResult =
    runAdb("shell", "am", "broadcast", "-p", TRACKING_PKG, "-a", action)

private fun connectedDevice(): String? {
    val result = runAdb("devices")
    val line = result.output.lines().drop(1).firstOrNull { it.contains("\tdevice") }
    return line?.substringBefore("\t")?.trim()
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
@Preview
fun RemoteControlApp() {
    val scope = rememberCoroutineScope()
    var device by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    var log by remember { mutableStateOf("") }

    fun appendLog(line: String) { log = "• $line\n$log" }

    fun refreshDevice() {
        scope.launch {
            checking = true
            device = withContext(Dispatchers.IO) { connectedDevice() }
            checking = false
            appendLog(if (device != null) "Device: $device" else "No device found — is ADB running? ($ADB)")
        }
    }

    fun send(action: String, label: String) {
        scope.launch {
            appendLog("Sending $label…")
            val result = withContext(Dispatchers.IO) { sendBroadcast(action) }
            appendLog(if (result.success) "OK [$label]  ${result.output}" else "FAIL [$label]  ${result.output}")
        }
    }

    LaunchedEffect(Unit) { refreshDevice() }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("BlindCheck Remote", style = MaterialTheme.typography.headlineMedium)

                // Device status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (checking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        val color = if (device != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        Text(
                            text = device ?: "No device",
                            color = color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = { refreshDevice() }) { Text("Refresh") }
                }

                HorizontalDivider()

                val connected = device != null

                // Navigation row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { send(ACTION_PREVIOUS, "Previous") }, enabled = connected) {
                        Text("◀  Previous")
                    }
                    Button(onClick = { send(ACTION_NEXT, "Next") }, enabled = connected) {
                        Text("Next  ▶")
                    }
                }

                // Activate
                Button(
                    onClick = { send(ACTION_ACTIVATE, "Activate") },
                    enabled = connected,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) { Text("✓  Activate") }

                // Scroll row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { send(ACTION_SCROLL_BACKWARD, "Scroll Backward") }, enabled = connected) {
                        Text("↑  Scroll Back")
                    }
                    OutlinedButton(onClick = { send(ACTION_SCROLL_FORWARD, "Scroll Forward") }, enabled = connected) {
                        Text("↓  Scroll Forward")
                    }
                }

                // System navigation row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { send(ACTION_BACK, "Back") }, enabled = connected) {
                        Text("⬅  Back")
                    }
                    OutlinedButton(onClick = { send(ACTION_HOME, "Home") }, enabled = connected) {
                        Text("⌂  Home")
                    }
                    OutlinedButton(onClick = { send(ACTION_RECENTS, "Recents") }, enabled = connected) {
                        Text("▣  Recents")
                    }
                    OutlinedButton(onClick = { send(ACTION_SWIPE_UP, "Swipe Up") }, enabled = connected) {
                        Text("↑  Swipe Up")
                    }
                    OutlinedButton(onClick = { send(ACTION_SWIPE_DOWN, "Swipe Down") }, enabled = connected) {
                        Text("↓  Swipe Down")
                    }
                }

                HorizontalDivider()

                // Log output
                Text("Log", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = log.ifEmpty { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BlindCheck Remote") {
        RemoteControlApp()
    }
}
