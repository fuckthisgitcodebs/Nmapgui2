package com.example.nmapgui

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nmapgui.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore by preferencesDataStore(name = "settings")
private val KEY_NMAP_PATH = stringPreferencesKey("nmap_path")
private val KEY_OUTPUT_DIR = stringPreferencesKey("output_dir")
private val KEY_DARK_THEME = stringPreferencesKey("dark_theme")

data class AppSettings(
    val nmapBinaryPath: String = "",
    val outputDir: String = "",
    val darkTheme: Boolean = true,
)

data class ScanUiState(
    val options: ScanOptions = ScanOptions(),
    val generatedCommand: String = "nmap",
    val isScanning: Boolean = false,
    val liveOutput: List<String> = emptyList(),
    val xmlBuffer: StringBuilder = StringBuilder(),
    val scanResult: ScanResult? = null,
    val errorMessage: String? = null,
    val isRootAvailable: Boolean = false,
    val resolvedNmapPath: String = "",
    val scanComplete: Boolean = false,
)

private val jsonCodec = Json { ignoreUnknownKeys = true; prettyPrint = true }

class NmapViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = com.example.nmapgui.data.ScanRepository(application)
    private val executor = NmapExecutor()
    private val xmlParser = NmapXmlParser()

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<ScanHistoryEntry>>(emptyList())
    val history: StateFlow<List<ScanHistoryEntry>> = _history.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private var scanJob: Job? = null

    init {
        checkRoot()
        loadHistory()
        viewModelScope.launch {
            application.dataStore.data.collect { prefs ->
                val path = prefs[KEY_NMAP_PATH] ?: ""
                val dir = prefs[KEY_OUTPUT_DIR] ?: ""
                val dark = prefs[KEY_DARK_THEME] != "false"
                _settings.value = AppSettings(path, dir, dark)
                resolveNmapPath(path)
            }
        }
    }

    private fun checkRoot() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val root = RootUtils.isRootAvailable()
            _uiState.update { it.copy(isRootAvailable = root) }
        }
    }

    private fun resolveNmapPath(custom: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val path = NmapExecutor.findNmap(custom) ?: ""
            _uiState.update { it.copy(resolvedNmapPath = path) }
        }
    }

    fun updateOptions(options: ScanOptions) {
        _uiState.update { it.copy(options = options, generatedCommand = buildCommand(options)) }
    }

    fun buildCommand(options: ScanOptions): String {
        val parts = mutableListOf<String>()
        val nmapPath = _uiState.value.resolvedNmapPath.ifBlank { "nmap" }
        parts.add(nmapPath)

        val scanType = ScanType.valueOf(options.scanType)
        if (scanType != ScanType.AGGRESSIVE) parts.add(scanType.flag)

        // Host discovery
        if (options.skipHostDiscovery) parts.add("-Pn")
        if (options.tcpSynPing) parts.add("-PS${options.tcpSynPingPorts}")
        if (options.tcpAckPing) parts.add("-PA${options.tcpAckPingPorts}")
        if (options.udpPing) parts.add("-PU${options.udpPingPorts}")
        if (options.icmpEchoPing) parts.add("-PE")
        if (options.icmpTimestampPing) parts.add("-PP")
        if (options.arpPing) parts.add("-PR")
        if (options.noDns) parts.add("-n")
        else if (options.alwaysResolveDns) parts.add("-R")
        if (options.dnsServers.isNotBlank()) parts.addAll(listOf("--dns-servers", options.dnsServers))

        // Port spec
        when (PortSpec.valueOf(options.portSpec)) {
            PortSpec.ALL -> parts.add("-p-")
            PortSpec.FAST -> parts.add("-F")
            PortSpec.TOP_N -> if (options.topPortsN.isNotBlank()) parts.addAll(listOf("--top-ports", options.topPortsN))
            PortSpec.CUSTOM -> if (options.customPorts.isNotBlank()) parts.addAll(listOf("-p", options.customPorts))
            PortSpec.DEFAULT -> {}
        }

        // Detection
        if (scanType == ScanType.VERSION_INTENSITY || scanType == ScanType.AGGRESSIVE) {
            if (scanType == ScanType.VERSION_INTENSITY) {
                parts.add("-sV")
                if (options.versionIntensity != 7) parts.addAll(listOf("--version-intensity", options.versionIntensity.toString()))
            }
        }
        if (options.osDetection && scanType != ScanType.AGGRESSIVE) parts.add("-O")
        if (options.osscanGuess) parts.add("--osscan-guess")
        if (options.osscanLimit) parts.add("--osscan-limit")
        if (scanType == ScanType.AGGRESSIVE) parts.add("-A")

        // Timing
        parts.add(TimingTemplate.valueOf(options.timingTemplate).flag)
        if (options.maxRetries.isNotBlank()) parts.addAll(listOf("--max-retries", options.maxRetries))
        if (options.minRate.isNotBlank()) parts.addAll(listOf("--min-rate", options.minRate))
        if (options.maxRate.isNotBlank()) parts.addAll(listOf("--max-rate", options.maxRate))
        if (options.hostTimeout.isNotBlank()) parts.addAll(listOf("--host-timeout", options.hostTimeout))
        if (options.minParallelism.isNotBlank()) parts.addAll(listOf("--min-parallelism", options.minParallelism))
        if (options.maxParallelism.isNotBlank()) parts.addAll(listOf("--max-parallelism", options.maxParallelism))
        if (options.minRttTimeout.isNotBlank()) parts.addAll(listOf("--min-rtt-timeout", options.minRttTimeout))
        if (options.maxRttTimeout.isNotBlank()) parts.addAll(listOf("--max-rtt-timeout", options.maxRttTimeout))
        if (options.scanDelay.isNotBlank()) parts.addAll(listOf("--scan-delay", options.scanDelay))

        // Scripts
        if (options.scriptScan && scanType != ScanType.AGGRESSIVE) parts.add("-sC")
        if (options.customScripts.isNotBlank()) parts.addAll(listOf("--script", options.customScripts))
        if (options.scriptArgs.isNotBlank()) parts.addAll(listOf("--script-args", options.scriptArgs))

        // Output
        when (VerbosityLevel.valueOf(options.verbosity)) {
            VerbosityLevel.V -> parts.add("-v")
            VerbosityLevel.VV -> parts.add("-vv")
            VerbosityLevel.NONE -> {}
        }
        if (options.showReason) parts.add("--reason")
        if (options.openPortsOnly) parts.add("--open")
        if (options.traceroute) parts.add("--traceroute")
        if (options.packetTrace) parts.add("--packet-trace")
        // Always add XML output to temp file for parsing
        parts.addAll(listOf("-oX", "/tmp/nmap_last_scan.xml"))

        // Evasion
        if (options.fragmentPackets) parts.add("-f")
        if (options.mtu.isNotBlank()) parts.addAll(listOf("--mtu", options.mtu))
        if (options.decoys.isNotBlank()) parts.addAll(listOf("-D", options.decoys))
        if (options.sourceIp.isNotBlank()) parts.addAll(listOf("-S", options.sourceIp))
        if (options.sourcePort.isNotBlank()) parts.addAll(listOf("--source-port", options.sourcePort))
        if (options.networkInterface.isNotBlank()) parts.addAll(listOf("-e", options.networkInterface))
        if (options.spoofMac.isNotBlank()) parts.addAll(listOf("--spoof-mac", options.spoofMac))
        if (options.dataLength.isNotBlank()) parts.addAll(listOf("--data-length", options.dataLength))
        if (options.ttl.isNotBlank()) parts.addAll(listOf("--ttl", options.ttl))
        if (options.randomizeHosts) parts.add("--randomize-hosts")
        if (options.badSum) parts.add("--badsum")

        // Extra
        if (options.extraArgs.isNotBlank()) parts.addAll(options.extraArgs.split(" ").filter { it.isNotBlank() })

        // Target last
        if (options.target.isNotBlank()) parts.add(options.target)

        return parts.joinToString(" ")
    }

    fun startScan() {
        val opts = _uiState.value.options
        if (opts.target.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Target is required.") }
            return
        }
        val nmapPath = _uiState.value.resolvedNmapPath
        if (nmapPath.isBlank()) {
            _uiState.update { it.copy(errorMessage = "nmap binary not found. Install via Termux or set path in Settings.") }
            return
        }

        val commandStr = buildCommand(opts)
        val commandParts = commandStr.split(" ").filter { it.isNotBlank() }

        _uiState.update {
            it.copy(
                isScanning = true,
                liveOutput = listOf("$ $commandStr", ""),
                xmlBuffer = StringBuilder(),
                scanResult = null,
                errorMessage = null,
                scanComplete = false,
            )
        }

        val channel = Channel<String>(Channel.UNLIMITED)

        scanJob = viewModelScope.launch {
            launch {
                executor.runScan(commandParts, channel)
                channel.close()
            }
            for (line in channel) {
                _uiState.update { state ->
                    state.copy(liveOutput = state.liveOutput + line)
                }
            }
            // Parse XML output
            val xmlFile = File("/tmp/nmap_last_scan.xml")
            val result = if (xmlFile.exists()) {
                val xml = xmlFile.readText()
                xmlParser.parse(xml)
            } else null

            val rawOutput = _uiState.value.liveOutput.joinToString("\n")
            if (result != null) {
                val entry = ScanHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    target = opts.target,
                    command = commandStr,
                    scanResult = result.copy(rawOutput = rawOutput),
                    rawOutput = rawOutput,
                )
                repo.addEntry(entry)
                loadHistory()
            }

            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanResult = result,
                    scanComplete = true,
                )
            }
        }
    }

    fun stopScan() {
        executor.stopScan()
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false, scanComplete = true) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearScanComplete() = _uiState.update { it.copy(scanComplete = false) }

    private fun loadHistory() {
        _history.value = repo.loadHistory()
    }

    fun deleteHistoryEntry(id: String) {
        repo.deleteEntry(id)
        loadHistory()
    }

    fun clearHistory() {
        repo.clearHistory()
        loadHistory()
    }

    fun saveSettings(nmapPath: String, outputDir: String, darkTheme: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[KEY_NMAP_PATH] = nmapPath
                prefs[KEY_OUTPUT_DIR] = outputDir
                prefs[KEY_DARK_THEME] = if (darkTheme) "true" else "false"
            }
        }
    }

    fun loadHistoryResult(entry: ScanHistoryEntry): ScanResult? = entry.scanResult

    fun exportScanToFile(rawOutput: String, context: Context): String {
        return try {
            val dir = _settings.value.outputDir.ifBlank { context.filesDir.path }
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "nmap_$ts.txt")
            file.writeText(rawOutput)
            file.absolutePath
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
