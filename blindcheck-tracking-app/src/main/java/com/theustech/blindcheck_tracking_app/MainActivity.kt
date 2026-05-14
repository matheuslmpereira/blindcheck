package com.theustech.blindcheck_tracking_app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theustech.blindcheck_testing.model.A11yEventRecord
import com.theustech.blindcheck_tracking_app.data.AccessibilityEventType
import com.theustech.blindcheck_tracking_app.data.TrackingEventStore
import com.theustech.blindcheck_tracking_app.data.TrackingServiceStatus
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackingEventStreamScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var manualPackage by remember { mutableStateOf("") }
    var isPackageMenuExpanded by remember { mutableStateOf(false) }
    var isEventTypeMenuExpanded by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(TrackingEventStore.shared.isRecording) }
    var isTrackingServiceEnabled by remember { mutableStateOf(readTrackingServiceEnabled(context)) }
    var events by remember { mutableStateOf(TrackingEventStore.shared.snapshot()) }
    var observedPackages by remember { mutableStateOf(TrackingEventStore.shared.observedPackagesSnapshot()) }
    var targetPackages by remember { mutableStateOf(TrackingEventStore.shared.targetPackagesSnapshot()) }
    var targetEventTypes by remember { mutableStateOf(TrackingEventStore.shared.targetEventTypesSnapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            isRecording = TrackingEventStore.shared.isRecording
            isTrackingServiceEnabled = readTrackingServiceEnabled(context)
            events = TrackingEventStore.shared.snapshot()
            observedPackages = TrackingEventStore.shared.observedPackagesSnapshot()
            targetPackages = TrackingEventStore.shared.targetPackagesSnapshot()
            targetEventTypes = TrackingEventStore.shared.targetEventTypesSnapshot()
            delay(EVENT_REFRESH_MS)
        }
    }

    fun refresh() {
        events = TrackingEventStore.shared.snapshot()
        observedPackages = TrackingEventStore.shared.observedPackagesSnapshot()
        targetPackages = TrackingEventStore.shared.targetPackagesSnapshot()
        targetEventTypes = TrackingEventStore.shared.targetEventTypesSnapshot()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("BlindCheck") },
                actions = {
                    if (!isTrackingServiceEnabled) {
                        Text(
                            text = "Service off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(
                        text = if (isRecording) "● REC" else "○ Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRecording) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isTrackingServiceEnabled) {
                ServiceWarningBanner(
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }

            // Package filter row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = manualPackage,
                    onValueChange = { manualPackage = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Package filter") },
                    placeholder = { Text("com.example.app") },
                )
                Button(
                    enabled = manualPackage.isNotBlank(),
                    onClick = {
                        TrackingEventStore.shared.addTargetPackage(manualPackage)
                        manualPackage = ""
                        refresh()
                    },
                ) {
                    Text("Add")
                }
            }

            // Observed packages dropdown
            ExposedDropdownMenuBox(
                expanded = isPackageMenuExpanded,
                onExpandedChange = {
                    isPackageMenuExpanded = observedPackages.isNotEmpty() && !isPackageMenuExpanded
                },
            ) {
                OutlinedTextField(
                    value = if (observedPackages.isEmpty()) "No apps seen yet" else "Observed apps",
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    readOnly = true,
                    enabled = observedPackages.isNotEmpty(),
                    singleLine = true,
                    label = { Text("Filter from observed apps") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPackageMenuExpanded)
                    },
                )
                ExposedDropdownMenu(
                    expanded = isPackageMenuExpanded,
                    onDismissRequest = { isPackageMenuExpanded = false },
                ) {
                    observedPackages.forEach { pkg ->
                        DropdownMenuItem(
                            text = { Text(pkg) },
                            onClick = {
                                TrackingEventStore.shared.addTargetPackage(pkg)
                                isPackageMenuExpanded = false
                                refresh()
                            },
                        )
                    }
                }
            }

            // Event type dropdown
            ExposedDropdownMenuBox(
                expanded = isEventTypeMenuExpanded,
                onExpandedChange = { isEventTypeMenuExpanded = !isEventTypeMenuExpanded },
            ) {
                OutlinedTextField(
                    value = "Event types",
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    label = { Text("Filter by event type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEventTypeMenuExpanded)
                    },
                )
                ExposedDropdownMenu(
                    expanded = isEventTypeMenuExpanded,
                    onDismissRequest = { isEventTypeMenuExpanded = false },
                ) {
                    AccessibilityEventType.entries.forEach { eventType ->
                        DropdownMenuItem(
                            text = { Text(eventType.label) },
                            onClick = {
                                TrackingEventStore.shared.addTargetEventType(eventType)
                                isEventTypeMenuExpanded = false
                                refresh()
                            },
                        )
                    }
                }
            }

            // Active filter chips
            val hasFilters = targetPackages.isNotEmpty() || targetEventTypes.isNotEmpty()
            if (hasFilters) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Active filters" },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    targetPackages.forEach { pkg ->
                        InputChip(
                            selected = true,
                            onClick = {
                                TrackingEventStore.shared.removeTargetPackage(pkg)
                                refresh()
                            },
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = "$pkg filter. Double tap to remove."
                            },
                            label = { Text(pkg) },
                            trailingIcon = { Text("×") },
                        )
                    }
                    targetEventTypes.forEach { eventType ->
                        InputChip(
                            selected = true,
                            onClick = {
                                TrackingEventStore.shared.removeTargetEventType(eventType)
                                refresh()
                            },
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                contentDescription = "${eventType.label} filter. Double tap to remove."
                            },
                            label = { Text(eventType.label) },
                            trailingIcon = { Text("×") },
                        )
                    }
                }
                TextButton(
                    onClick = {
                        TrackingEventStore.shared.clearTargetPackages()
                        TrackingEventStore.shared.clearTargetEventTypes()
                        refresh()
                    },
                ) {
                    Text("Clear all filters")
                }
            }

            var newestFirst by remember { mutableStateOf(true) }

            HorizontalDivider()

            // Recording controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val nextRecording = !isRecording
                        isRecording = nextRecording
                        TrackingEventStore.shared.setRecording(nextRecording)
                        refresh()
                    },
                ) {
                    Text(if (isRecording) "Pause" else "Resume")
                }
                OutlinedButton(
                    onClick = {
                        TrackingEventStore.shared.clear()
                        refresh()
                    },
                ) {
                    Text("Reset")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${events.size} events · ${observedPackages.size} apps seen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                TextButton(onClick = { newestFirst = !newestFirst }) {
                    Text(
                        text = if (newestFirst) "Newest first" else "Oldest first",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            val sortedEvents = if (newestFirst) events.reversed() else events
            val listState = rememberLazyListState()
            LaunchedEffect(newestFirst) { listState.scrollToItem(0) }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sortedEvents, key = { event -> event.id }) { event ->
                    EventRecordRow(event = event)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ServiceWarningBanner(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "BlindCheck service is off",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "Enable BlindCheck (not just TalkBack) to capture accessibility events.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = "Open accessibility settings",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EventRecordRow(event: A11yEventRecord, modifier: Modifier = Modifier) {
    val eventTypeLabel = AccessibilityEventType.fromAndroidName(event.eventType)?.label
        ?: event.eventType
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${event.timestamp} · ${event.packageName.orEmpty()} · $eventTypeLabel",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (event.text.isNotEmpty()) {
            Text(
                text = event.text.joinToString(separator = " | "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!event.contentDescription.isNullOrBlank()) {
            Text(
                text = "Description: ${event.contentDescription}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TrackingEventStreamScreenPreview() {
    BlindchecktesteappTheme {
        TrackingEventStreamScreenPreviewContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingEventStreamScreenPreviewContent() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BlindCheck") },
                actions = {
                    Text(
                        text = "Service off",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "● REC",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ServiceWarningBanner(onOpenSettings = {})
            OutlinedTextField(
                value = "com.example.app",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Package filter") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Pause") }
                OutlinedButton(onClick = {}) { Text("Reset") }
            }
            Text(
                text = "0 events · 0 apps seen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun readTrackingServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        ?: return false
    return accessibilityManager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { service ->
            TrackingServiceStatus.isTrackingService(
                packageName = service.resolveInfo?.serviceInfo?.packageName,
                serviceClassName = service.resolveInfo?.serviceInfo?.name,
            )
        }
}

private const val EVENT_REFRESH_MS = 500L
