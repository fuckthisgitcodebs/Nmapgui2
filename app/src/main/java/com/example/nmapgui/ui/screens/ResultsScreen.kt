package com.example.nmapgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nmapgui.NmapViewModel
import com.example.nmapgui.data.HostResult
import com.example.nmapgui.data.PortResult
import com.example.nmapgui.data.ScanResult
import com.example.nmapgui.ui.theme.*

enum class ResultTab { PARSED, RAW }

@Composable
fun ResultsScreen(vm: NmapViewModel) {
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ResultTab.PARSED) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab bar
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == ResultTab.PARSED,
                onClick = { selectedTab = ResultTab.PARSED },
                text = { Text("Parsed") },
            )
            Tab(
                selected = selectedTab == ResultTab.RAW,
                onClick = { selectedTab = ResultTab.RAW },
                text = { Text("Live / Raw") },
            )
        }

        when (selectedTab) {
            ResultTab.PARSED -> ParsedResultsPane(result = state.scanResult, isScanning = state.isScanning)
            ResultTab.RAW -> RawOutputPane(lines = state.liveOutput, isScanning = state.isScanning)
        }
    }
}

@Composable
private fun ParsedResultsPane(result: ScanResult?, isScanning: Boolean) {
    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isScanning) {
                    CircularProgressIndicator(color = Green400)
                    Spacer(Modifier.height(8.dp))
                    Text("Scan in progress…", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No scan results yet. Run a scan on the Scan tab.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Stats bar
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Scan Complete",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (result.scanStats.isNotBlank()) {
                        Text(result.scanStats, style = MaterialTheme.typography.bodySmall)
                    }
                    if (result.startTime.isNotBlank()) {
                        Text("Started: ${result.startTime}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Hosts found: ${result.hosts.size}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (result.hosts.isEmpty()) {
            item {
                Text(
                    "No hosts returned in XML output. Check the Raw tab for full output.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(result.hosts) { host -> HostCard(host) }
    }
}

@Composable
private fun HostCard(host: HostResult) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Host header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint = if (host.state == "up") Green400 else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        host.address,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (host.state == "up") Green400 else MaterialTheme.colorScheme.error,
                    )
                    if (host.hostname.isNotBlank()) {
                        Text(host.hostname, style = MaterialTheme.typography.bodySmall, color = Cyan400)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(host.state)
                        if (host.distance.isNotBlank()) Text("${host.distance} hops", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            if (expanded) {
                HorizontalDivider()

                // OS matches
                if (host.osMatches.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("OS Detection", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        host.osMatches.take(3).forEach { os ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("• ${os.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("${os.accuracy}%", style = MaterialTheme.typography.bodySmall, color = Orange400)
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Ports table
                if (host.ports.isNotEmpty()) {
                    PortsTable(host.ports)
                } else {
                    Text(
                        "No ports reported (host may block all ports or ping-only scan was used).",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortsTable(ports: List<PortResult>) {
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        // Header row
        Row(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)) {
            TableCell("PORT", weight = 0.18f, header = true)
            TableCell("PROTO", weight = 0.12f, header = true)
            TableCell("STATE", weight = 0.15f, header = true)
            TableCell("SERVICE", weight = 0.25f, header = true)
            TableCell("VERSION", weight = 0.30f, header = true)
        }
        ports.forEach { port ->
            val stateColor = when (port.state.lowercase()) {
                "open" -> Green400
                "closed" -> MaterialTheme.colorScheme.error
                "filtered" -> Orange400
                "open|filtered" -> Yellow400
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    TableCell(port.portId, weight = 0.18f, color = MaterialTheme.colorScheme.primary)
                    TableCell(port.protocol, weight = 0.12f)
                    TableCell(port.state, weight = 0.15f, color = stateColor)
                    TableCell(port.service, weight = 0.25f)
                    val versionStr = listOf(port.product, port.version, port.extraInfo)
                        .filter { it.isNotBlank() }.joinToString(" ")
                    TableCell(versionStr, weight = 0.30f, muted = true)
                }
                // Show reason if present
                if (port.reason.isNotBlank()) {
                    Text(
                        "  reason: ${port.reason}",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
                // Scripts
                port.scripts.forEach { script ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(script.id, style = MaterialTheme.typography.labelSmall, color = Cyan400)
                            Text(script.output, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    muted: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        color = if (color != androidx.compose.ui.graphics.Color.Unspecified) color
        else if (muted) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        fontSize = if (header) 10.sp else 12.sp,
    )
}

@Composable
private fun StatusBadge(state: String) {
    val (bg, fg) = when (state.lowercase()) {
        "up" -> Green400.copy(alpha = 0.2f) to Green400
        "down" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f) to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(
            state.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
private fun RawOutputPane(lines: List<String>, isScanning: Boolean) {
    val scrollState = rememberScrollState()
    val clipboard = LocalClipboardManager.current
    val fullText = remember(lines) { lines.joinToString("\n") }

    // Auto-scroll to bottom
    LaunchedEffect(lines.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Green400)
                Spacer(Modifier.width(8.dp))
                Text("Scanning…", style = MaterialTheme.typography.bodySmall, color = Green400)
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text("${lines.size} lines", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { clipboard.setText(AnnotatedString(fullText)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy all", modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider()
        // Output
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            lines.forEach { line ->
                val color = when {
                    line.startsWith("$") -> Green400
                    line.contains("open") -> Green400
                    line.contains("filtered") -> Orange400
                    line.contains("closed") -> MaterialTheme.colorScheme.error
                    line.startsWith("WARNING") || line.startsWith("ERROR") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = color,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
