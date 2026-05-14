package com.theustech.blindcheck_tracking_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theustech.blindcheck_testing.model.A11yEventRecord
import com.theustech.blindcheck_tracking_app.data.TrackingEventStore
import com.theustech.blindcheck_tracking_app.ui.theme.BlindchecktesteappTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlindchecktesteappTheme {
                TrackingEventStreamScreen()
            }
        }
    }
}

@Composable
fun TrackingEventStreamScreen(modifier: Modifier = Modifier) {
    var targetPackage by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(TrackingEventStore.shared.isRecording) }
    var events by remember { mutableStateOf(TrackingEventStore.shared.snapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            isRecording = TrackingEventStore.shared.isRecording
            events = TrackingEventStore.shared.snapshot()
            delay(EVENT_REFRESH_MS)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "BlindCheck event stream",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = targetPackage,
                onValueChange = { value ->
                    targetPackage = value
                    TrackingEventStore.shared.setTargetPackage(value.trim().ifEmpty { null })
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Target package") },
                placeholder = { Text("com.example.app") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val nextRecording = !isRecording
                        isRecording = nextRecording
                        TrackingEventStore.shared.setRecording(nextRecording)
                        events = TrackingEventStore.shared.snapshot()
                    },
                ) {
                    Text(if (isRecording) "Stop recording" else "Start recording")
                }

                Button(
                    onClick = {
                        TrackingEventStore.shared.clear()
                        events = TrackingEventStore.shared.snapshot()
                    },
                ) {
                    Text("Clear")
                }
            }

            Text(
                text = "Events: ${events.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events, key = { event -> event.id }) { event ->
                    EventRecordRow(event = event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EventRecordRow(event: A11yEventRecord, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${event.timestamp} | ${event.packageName.orEmpty()} | ${event.eventType}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Text: ${event.text.joinToString(separator = " | ").ifBlank { "-" }}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Description: ${event.contentDescription.orEmpty().ifBlank { "-" }}",
            style = MaterialTheme.typography.bodySmall,
        )
        event.sourceNode?.let { node ->
            val states = buildList {
                if (node.focused) add("focused")
                if (node.clickable) add("clickable")
                if (node.editable) add("editable")
                if (!node.enabled) add("disabled")
            }.joinToString(", ").ifBlank { "enabled" }
            Text(
                text = "State: $states",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingEventStreamScreenPreview() {
    BlindchecktesteappTheme {
        TrackingEventStreamContentPreview()
    }
}

@Composable
private fun TrackingEventStreamContentPreview() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "BlindCheck event stream",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = "com.example.app",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Target package") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) {
                    Text("Start recording")
                }
                Button(onClick = {}) {
                    Text("Clear")
                }
            }
            Text(
                text = "Events: 0",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private const val EVENT_REFRESH_MS = 500L
