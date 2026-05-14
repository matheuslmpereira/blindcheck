package com.theustech.blindcheck_tracking_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingEventStreamScreen(modifier: Modifier = Modifier) {
    var manualPackage by remember { mutableStateOf("") }
    var isPackageMenuExpanded by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(TrackingEventStore.shared.isRecording) }
    var events by remember { mutableStateOf(TrackingEventStore.shared.snapshot()) }
    var observedPackages by remember { mutableStateOf(TrackingEventStore.shared.observedPackagesSnapshot()) }
    var targetPackages by remember { mutableStateOf(TrackingEventStore.shared.targetPackagesSnapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            isRecording = TrackingEventStore.shared.isRecording
            events = TrackingEventStore.shared.snapshot()
            observedPackages = TrackingEventStore.shared.observedPackagesSnapshot()
            targetPackages = TrackingEventStore.shared.targetPackagesSnapshot()
            delay(EVENT_REFRESH_MS)
        }
    }

    fun addPackageFilter(packageName: String) {
        TrackingEventStore.shared.addTargetPackage(packageName)
        manualPackage = ""
        refreshTrackingState(
            onEventsChanged = { events = it },
            onObservedPackagesChanged = { observedPackages = it },
            onTargetPackagesChanged = { targetPackages = it },
        )
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
                value = manualPackage,
                onValueChange = { value -> manualPackage = value },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Add capture filter") },
                placeholder = { Text("com.example.app") },
            )

            ExposedDropdownMenuBox(
                expanded = isPackageMenuExpanded,
                onExpandedChange = {
                    isPackageMenuExpanded = observedPackages.isNotEmpty() && !isPackageMenuExpanded
                },
            ) {
                OutlinedTextField(
                    value = if (observedPackages.isEmpty()) "No packages observed yet" else "Observed packages",
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    readOnly = true,
                    enabled = observedPackages.isNotEmpty(),
                    singleLine = true,
                    label = { Text("Capture from observed packages") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPackageMenuExpanded)
                    },
                )
                ExposedDropdownMenu(
                    expanded = isPackageMenuExpanded,
                    onDismissRequest = { isPackageMenuExpanded = false },
                ) {
                    observedPackages.forEach { packageName ->
                        DropdownMenuItem(
                            text = { Text(packageName) },
                            onClick = {
                                addPackageFilter(packageName)
                                isPackageMenuExpanded = false
                            },
                        )
                    }
                }
            }

            ActivePackageFilters(
                targetPackages = targetPackages,
                onRemove = { packageName ->
                    TrackingEventStore.shared.removeTargetPackage(packageName)
                    refreshTrackingState(
                        onEventsChanged = { events = it },
                        onObservedPackagesChanged = { observedPackages = it },
                        onTargetPackagesChanged = { targetPackages = it },
                    )
                },
                onClear = {
                    TrackingEventStore.shared.clearTargetPackages()
                    refreshTrackingState(
                        onEventsChanged = { events = it },
                        onObservedPackagesChanged = { observedPackages = it },
                        onTargetPackagesChanged = { targetPackages = it },
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = manualPackage.isNotBlank(),
                    onClick = { addPackageFilter(manualPackage) },
                ) {
                    Text("Add filter")
                }

                Button(
                    onClick = {
                        val nextRecording = !isRecording
                        isRecording = nextRecording
                        TrackingEventStore.shared.setRecording(nextRecording)
                        refreshTrackingState(
                            onEventsChanged = { events = it },
                            onObservedPackagesChanged = { observedPackages = it },
                            onTargetPackagesChanged = { targetPackages = it },
                        )
                    },
                ) {
                    Text(if (isRecording) "Stop recording" else "Start recording")
                }

                Button(
                    onClick = {
                        TrackingEventStore.shared.clear()
                        refreshTrackingState(
                            onEventsChanged = { events = it },
                            onObservedPackagesChanged = { observedPackages = it },
                            onTargetPackagesChanged = { targetPackages = it },
                        )
                    },
                ) {
                    Text("Clear all")
                }
            }

            Text(
                text = "Captured events: ${events.size} | Apps observed: ${observedPackages.size}",
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
private fun ActivePackageFilters(
    targetPackages: List<String>,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (targetPackages.isEmpty()) {
        Text(
            text = "No capture filter active",
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        targetPackages.forEach { packageName ->
            InputChip(
                selected = true,
                onClick = { onRemove(packageName) },
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$packageName filter. Double tap to remove."
                },
                label = { Text(packageName) },
                trailingIcon = { Text("Remove") },
            )
        }
        Button(onClick = onClear) {
            Text("Clear filters")
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
                label = { Text("Add capture filter") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) {
                    Text("Add filter")
                }
                Button(onClick = {}) {
                    Text("Start recording")
                }
                Button(onClick = {}) {
                    Text("Clear all")
                }
            }
            Text(
                text = "Captured events: 0",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun refreshTrackingState(
    onEventsChanged: (List<A11yEventRecord>) -> Unit,
    onObservedPackagesChanged: (List<String>) -> Unit,
    onTargetPackagesChanged: (List<String>) -> Unit,
) {
    onEventsChanged(TrackingEventStore.shared.snapshot())
    onObservedPackagesChanged(TrackingEventStore.shared.observedPackagesSnapshot())
    onTargetPackagesChanged(TrackingEventStore.shared.targetPackagesSnapshot())
}

private const val EVENT_REFRESH_MS = 500L
