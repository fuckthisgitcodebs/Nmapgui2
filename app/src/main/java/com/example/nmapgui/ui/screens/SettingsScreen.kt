package com.example.nmapgui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nmapgui.NmapExecutor
import com.example.nmapgui.NmapViewModel
import com.example.nmapgui.ui.components.InfoIcon
import com.example.nmapgui.ui.theme.Green400

@Composable
fun SettingsScreen(vm: NmapViewModel) {
    val settings by vm.settings.collectAsState()
    val state by vm.uiState.collectAsState()

    var nmapPath by remember(settings.nmapBinaryPath) { mutableStateOf(settings.nmapBinaryPath) }
    var outputDir by remember(settings.outputDir) { mutableStateOf(settings.outputDir) }
    var darkTheme by remember(settings.darkTheme) { mutableStateOf(settings.darkTheme) }
    var saved by remember { mutableStateOf(false) }

    fun save() {
        vm.saveSettings(nmapPath, outputDir, darkTheme)
        saved = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        // Nmap binary
        SettingsCard(title = "Nmap Binary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("nmap binary path", style = MaterialTheme.typography.bodyMedium)
                    InfoIcon(
                        title = "nmap Binary Path",
                        description = """Path to the nmap executable on this device.

Leave blank to auto-detect from these locations (in order):
• /data/data/com.termux/files/usr/bin/nmap  ← Termux install
• /data/adb/magisk/usr/bin/nmap  ← Magisk module
• /system/xbin/nmap
• /system/bin/nmap
• /usr/bin/nmap

To install nmap via Termux:
1. Install Termux from F-Droid (not Play Store)
2. Run: pkg install nmap
3. Leave this field blank (auto-detected)

Current detected: ${state.resolvedNmapPath.ifBlank { "NOT FOUND" }}"""
                    )
                }
                OutlinedTextField(
                    value = nmapPath,
                    onValueChange = { nmapPath = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Leave blank to auto-detect") },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
                    singleLine = true,
                )
                // Auto-detect result
                val detected = NmapExecutor.findNmap(nmapPath)
                Surface(
                    color = if (detected != null)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            if (detected != null) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (detected != null) Green400 else MaterialTheme.colorScheme.error,
                        )
                        Text(
                            if (detected != null) "Found: $detected" else "nmap not found on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (detected != null) Green400 else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (detected == null) {
                    Text(
                        "Install nmap: open Termux → pkg install nmap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Output directory
        SettingsCard(title = "Output") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Save directory", style = MaterialTheme.typography.bodyMedium)
                    InfoIcon(
                        title = "Scan Output Directory",
                        description = "Directory where exported scan files are saved. Leave blank to use the app's internal files directory. Tap 'Export' on a scan result to save to this directory. Example: /sdcard/nmap_scans"
                    )
                }
                OutlinedTextField(
                    value = outputDir,
                    onValueChange = { outputDir = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Leave blank for app internal storage") },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    singleLine = true,
                )
            }
        }

        // Appearance
        SettingsCard(title = "Appearance") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Dark theme", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
            }
        }

        // Root info
        SettingsCard(title = "Root Status") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (state.isRootAvailable) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (state.isRootAvailable) Green400 else MaterialTheme.colorScheme.error,
                )
                Text(
                    if (state.isRootAvailable) "Root access available — all scan types enabled." else "No root — TCP SYN/UDP/Idle/ACK/FIN/Null/Xmas scans disabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.isRootAvailable) Green400 else MaterialTheme.colorScheme.error,
                )
                InfoIcon(
                    title = "Root Access",
                    description = """Many of nmap's most powerful scan types require root (CAP_NET_RAW) to craft raw packets:

REQUIRES ROOT:
• -sS TCP SYN (stealth) — most common scan type
• -sU UDP scan
• -sA TCP ACK (firewall mapping)
• -sW, -sM, -sF, -sN, -sX — exotic TCP scans
• -sO IP protocol scan
• -O OS detection
• -f packet fragmentation
• -D decoy scan, -S spoof source IP

WORKS WITHOUT ROOT:
• -sT TCP Connect (slow but functional)
• -sn Ping scan (limited)
• -sV version detection
• -sC / NSE scripts
• -A aggressive (uses -sT instead of -sS)

To get root: your device must be rooted with Magisk or similar."""
                )
            }
        }

        // Save button
        Button(
            onClick = { save() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Settings")
        }

        if (saved) {
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); saved = false }
            Text("Settings saved.", color = Green400, style = MaterialTheme.typography.bodySmall)
        }

        // About
        SettingsCard(title = "About") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nmap GUI for Android", style = MaterialTheme.typography.titleMedium)
                Text("A full-featured frontend for the nmap network scanner.", style = MaterialTheme.typography.bodySmall)
                Text("nmap by Gordon Lyon (Fyodor) — https://nmap.org", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("This app does not include nmap. Install separately via Termux.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("⚠ Only scan networks and systems you own or have explicit permission to scan. Unauthorized scanning is illegal in many jurisdictions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            content()
        }
    }
}
