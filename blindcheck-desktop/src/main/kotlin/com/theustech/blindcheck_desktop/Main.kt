package com.theustech.blindcheck_desktop

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val TRACKING_PKG = "com.theustech.blindcheck_tracking_app"
private const val ACTION_NEXT = "com.theustech.blindcheck.ACTION_NEXT"
private const val ACTION_PREVIOUS = "com.theustech.blindcheck.ACTION_PREVIOUS"
private const val ACTION_ACTIVATE = "com.theustech.blindcheck.ACTION_ACTIVATE"
private const val ACTION_BACK = "com.theustech.blindcheck.ACTION_BACK"
private const val ACTION_SCROLL_FORWARD = "com.theustech.blindcheck.ACTION_SCROLL_FORWARD"
private const val ACTION_SCROLL_BACKWARD = "com.theustech.blindcheck.ACTION_SCROLL_BACKWARD"

fun sendAdbBroadcast(action: String): Boolean {
    return try {
        val process = ProcessBuilder(
            "adb", "shell", "am", "broadcast", "-p", TRACKING_PKG, "-a", action
        )
            .redirectErrorStream(true)
            .start()
        process.waitFor(5, TimeUnit.SECONDS)
        process.exitValue() == 0
    } catch (e: Exception) {
        false
    }
}

fun checkAdbConnected(): Boolean {
    return try {
        val process = ProcessBuilder("adb", "devices")
            .redirectErrorStream(true)
            .start()
        process.waitFor(3, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText()
        output.lines().drop(1).any { it.contains("\tdevice") }
    } catch (e: Exception) {
        false
    }
}

@Composable
@Preview
fun RemoteControlApp() {
    val scope = rememberCoroutineScope()
    var deviceConnected by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        deviceConnected = checkAdbConnected()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "BlindCheck Remote",
                    style = MaterialTheme.typography.headlineMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val color = if (deviceConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                    Text(
                        text = if (deviceConnected) "Device connected" else "No device found",
                        color = color,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            deviceConnected = checkAdbConnected()
                        }
                    }) {
                        Text("Refresh")
                    }
                }

                HorizontalDivider()

                fun sendAction(action: String, label: String) {
                    scope.launch(Dispatchers.IO) {
                        val ok = sendAdbBroadcast(action)
                        statusMessage = if (ok) "Sent: $label" else "Failed: $label"
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { sendAction(ACTION_PREVIOUS, "Previous") },
                        enabled = deviceConnected
                    ) { Text("◀  Previous") }

                    Button(
                        onClick = { sendAction(ACTION_NEXT, "Next") },
                        enabled = deviceConnected
                    ) { Text("Next  ▶") }
                }

                Button(
                    onClick = { sendAction(ACTION_ACTIVATE, "Activate") },
                    enabled = deviceConnected,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) { Text("✓  Activate") }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { sendAction(ACTION_SCROLL_BACKWARD, "Scroll Backward") },
                        enabled = deviceConnected
                    ) { Text("↑  Scroll Back") }

                    OutlinedButton(
                        onClick = { sendAction(ACTION_SCROLL_FORWARD, "Scroll Forward") },
                        enabled = deviceConnected
                    ) { Text("↓  Scroll Forward") }
                }

                OutlinedButton(
                    onClick = { sendAction(ACTION_BACK, "Back") },
                    enabled = deviceConnected
                ) { Text("⬅  Back") }

                if (statusMessage.isNotEmpty()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
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
