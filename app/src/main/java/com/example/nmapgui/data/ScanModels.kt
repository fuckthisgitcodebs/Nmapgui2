package com.example.nmapgui.data

import kotlinx.serialization.Serializable

// ── Scan configuration ──────────────────────────────────────────────────────

enum class ScanType(val flag: String, val label: String, val needsRoot: Boolean, val info: String) {
    PING_SCAN(
        "-sn", "Ping Scan (-sn)", false,
        "Disables port scanning entirely. Nmap only discovers which hosts are alive by sending ICMP echo, TCP SYN to port 443, TCP ACK to port 80, and ICMP timestamp requests. Use when you only need a quick host-alive map without port data. Fast and low-noise."
    ),
    TCP_SYN(
        "-sS", "TCP SYN / Stealth (-sS)", true,
        "Sends SYN, never completes the TCP three-way handshake. If target responds SYN-ACK → port is OPEN; RST → CLOSED; no response → FILTERED. Never creates a full connection so many logs miss it. Requires root/CAP_NET_RAW. Default when running as root."
    ),
    TCP_CONNECT(
        "-sT", "TCP Connect (-sT)", false,
        "Uses the OS connect() syscall to complete a full TCP handshake. Works without root. Slower than SYN scan and more likely to appear in target logs because each connection is fully established and then immediately closed."
    ),
    UDP(
        "-sU", "UDP Scan (-sU)", true,
        "Scans UDP ports. Sends UDP packet; ICMP port-unreachable → CLOSED; no response → OPEN|FILTERED. Very slow because rate-limiting applies. Combine with -sS for both protocols: 'nmap -sU -sS target'. Requires root."
    ),
    TCP_ACK(
        "-sA", "TCP ACK (-sA)", true,
        "Sends ACK packets. Does NOT determine open ports — maps firewall rulesets. Unfiltered: RST received (firewall lets ACK through). Filtered: no response or ICMP error. Use to find firewall rules, not open services."
    ),
    TCP_WINDOW(
        "-sW", "TCP Window (-sW)", true,
        "Like ACK scan but examines the TCP Window field in RST responses. On some systems, open ports return a positive Window value while closed ports return zero. Less reliable than SYN scan; OS-dependent."
    ),
    TCP_MAIMON(
        "-sM", "Maimon (-sM)", true,
        "Sends FIN/ACK probe. Named after Uriel Maimon. Per RFC 793 compliant systems drop packets for open ports and send RST for closed. Many modern systems send RST regardless, making it less useful than FIN/NULL/Xmas."
    ),
    TCP_FIN(
        "-sF", "FIN Scan (-sF)", true,
        "Sends a TCP FIN packet. Open ports (per RFC 793) drop the packet silently; closed ports reply with RST. Can bypass stateless firewalls and ACL rules that only look for SYN. Unreliable against Windows (always sends RST)."
    ),
    TCP_NULL(
        "-sN", "Null Scan (-sN)", true,
        "Sends TCP packet with NO flags set. Open ports drop it; closed ports reply RST. Same use-case as FIN/Xmas. Works on UNIX/Linux; Windows ignores RFC 793 and always responds RST so open/closed are indistinguishable."
    ),
    TCP_XMAS(
        "-sX", "Xmas Scan (-sX)", true,
        "Sets FIN + PSH + URG flags ('lit up like a Christmas tree'). Same open/closed logic as FIN/Null. Can slip past some firewalls. Fails to distinguish open from filtered on Windows, Cisco IOS, and other non-RFC-compliant stacks."
    ),
    IP_PROTOCOL(
        "-sO", "IP Protocol (-sO)", true,
        "Iterates through IP protocol numbers (TCP=6, UDP=17, ICMP=1, IGMP=2, etc.) rather than ports, determining which IP protocols the target supports. ICMP unreachable errors → protocol unsupported; no response → open/filtered."
    ),
    VERSION_INTENSITY(
        "-sV", "Version Detection (-sV)", false,
        "Probes open ports to fingerprint the running service name and version. Intensity 0–9 controls how many probes are tried: 0=lightest/fastest, 9=all probes (slowest, highest accuracy). Default intensity: 7. Pairs with -O for full fingerprinting."
    ),
    AGGRESSIVE(
        "-A", "Aggressive (-A)", false,
        "Shorthand for: -sV (version detection) + -O (OS detection) + -sC (default scripts) + --traceroute. Comprehensive but very noisy — will trigger IDS/IPS. Use only on networks/targets you own or have explicit permission to scan."
    ),
}

enum class PortSpec(val label: String, val info: String) {
    DEFAULT(
        "Default (top 1000)", "Nmap scans the 1,000 most commonly used ports as listed in nmap-services. Covers the vast majority of real-world services without the slowdown of a full 65535-port scan."
    ),
    ALL(
        "All ports (-p-)", "Scans all 65,535 TCP (or UDP) ports. Thorough but slow — use with -T4 or --min-rate to speed up. Catches non-standard service placements like SSH on port 2222 or HTTP on 8080."
    ),
    FAST(
        "Fast / Top-100 (-F)", "Scans only the top 100 ports from nmap-services. Approximately 10× faster than the default 1,000-port scan. Misses many services but useful for rapid triage."
    ),
    TOP_N(
        "Top N (--top-ports N)", "Scan exactly the N most frequently open ports according to Nmap's nmap-services frequency data. Set N below (e.g., 500 for a middle ground between -F and default)."
    ),
    CUSTOM(
        "Custom (-p ...)", "Full port expression syntax: single ports (22), ranges (1-1024), comma lists (22,80,443), protocol prefixes (T:80,U:53), or '-p-' for all. Mix freely: 'T:22-25,80,443,U:53,161'."
    ),
}

enum class TimingTemplate(val flag: String, val label: String, val info: String) {
    T0(
        "-T0", "Paranoid (T0)",
        "5 minutes between probes. Completely serial — one probe at a time. Designed to evade almost any IDS/IPS. Extremely slow; a full scan of 1000 ports could take days. Use only for maximum stealth against monitored targets."
    ),
    T1(
        "-T1", "Sneaky (T1)",
        "15 seconds between probes. Serial. Evades most IDS systems. Still very slow but vastly quicker than T0. Suitable when stealth matters more than speed."
    ),
    T2(
        "-T2", "Polite (T2)",
        "0.4 seconds between probes. Reduces bandwidth impact on the target network. Use on fragile devices (printers, embedded systems) or when you want to be a courteous scanner."
    ),
    T3(
        "-T3", "Normal (T3)",
        "Default timing. Adaptive — adjusts based on network response times. Balances speed and reliability. No artificial delays; uses dynamic algorithms to determine optimal probe rate."
    ),
    T4(
        "-T4", "Aggressive (T4)",
        "Assumes a fast, reliable network (LAN or dedicated fiber). Cuts timeouts to 10ms, retransmit wait to 1250ms. Significantly faster than T3. Recommended for CTFs, lab environments, and trusted networks where speed matters."
    ),
    T5(
        "-T5", "Insane (T5)",
        "5ms timeout. Sacrifices accuracy for extreme speed. May miss open ports if the network or target is the slightest bit slow. Use only on localhost or extremely reliable LANs when time is critical and missed ports are acceptable."
    ),
}

enum class VerbosityLevel(val flag: String, val label: String, val info: String) {
    NONE("", "None", "Default output — shows final results only."),
    V("-v", "Verbose (-v)", "Shows open ports as discovered in real-time (don't wait for scan to finish). Also displays estimated scan completion time and extra status messages."),
    VV("-vv", "Very Verbose (-vv)", "Even more detail: shows all ports being probed, all packets sent/received summaries, script output as it arrives. Useful for deep debugging."),
}

@Serializable
data class ScanOptions(
    // Target
    val target: String = "",

    // Scan type
    val scanType: String = "TCP_SYN",

    // Host discovery
    val skipHostDiscovery: Boolean = false,
    val tcpSynPing: Boolean = false,
    val tcpSynPingPorts: String = "80,443",
    val tcpAckPing: Boolean = false,
    val tcpAckPingPorts: String = "80",
    val udpPing: Boolean = false,
    val udpPingPorts: String = "40125",
    val icmpEchoPing: Boolean = false,
    val icmpTimestampPing: Boolean = false,
    val arpPing: Boolean = false,
    val noDns: Boolean = false,
    val alwaysResolveDns: Boolean = false,
    val dnsServers: String = "",

    // Port spec
    val portSpec: String = "DEFAULT",
    val customPorts: String = "",
    val topPortsN: String = "500",

    // Detection
    val versionIntensity: Int = 7,
    val osDetection: Boolean = false,
    val osscanGuess: Boolean = false,
    val osscanLimit: Boolean = false,

    // Timing
    val timingTemplate: String = "T3",
    val maxRetries: String = "",
    val minRate: String = "",
    val maxRate: String = "",
    val hostTimeout: String = "",
    val minParallelism: String = "",
    val maxParallelism: String = "",
    val minRttTimeout: String = "",
    val maxRttTimeout: String = "",
    val scanDelay: String = "",

    // Scripts
    val scriptScan: Boolean = false,
    val customScripts: String = "",
    val scriptArgs: String = "",

    // Output
    val verbosity: String = "NONE",
    val showReason: Boolean = false,
    val openPortsOnly: Boolean = false,
    val traceroute: Boolean = false,
    val packetTrace: Boolean = false,

    // Evasion
    val fragmentPackets: Boolean = false,
    val mtu: String = "",
    val decoys: String = "",
    val sourceIp: String = "",
    val sourcePort: String = "",
    val networkInterface: String = "",
    val spoofMac: String = "",
    val dataLength: String = "",
    val ttl: String = "",
    val randomizeHosts: Boolean = false,
    val badSum: Boolean = false,

    // Extra
    val extraArgs: String = "",
)

// ── Scan results ─────────────────────────────────────────────────────────────

@Serializable
data class PortResult(
    val portId: String,
    val protocol: String,
    val state: String,
    val service: String = "",
    val version: String = "",
    val product: String = "",
    val extraInfo: String = "",
    val reason: String = "",
    val scripts: List<ScriptResult> = emptyList(),
)

@Serializable
data class ScriptResult(val id: String, val output: String)

@Serializable
data class OsMatch(val name: String, val accuracy: String)

@Serializable
data class HostResult(
    val address: String,
    val addressType: String = "ipv4",
    val hostname: String = "",
    val state: String = "up",
    val osMatches: List<OsMatch> = emptyList(),
    val ports: List<PortResult> = emptyList(),
    val distance: String = "",
    val traceroute: List<String> = emptyList(),
)

@Serializable
data class ScanResult(
    val hosts: List<HostResult> = emptyList(),
    val scanStats: String = "",
    val startTime: String = "",
    val elapsedTime: String = "",
    val rawOutput: String = "",
)

// ── History ──────────────────────────────────────────────────────────────────

@Serializable
data class ScanHistoryEntry(
    val id: String,
    val timestamp: Long,
    val target: String,
    val command: String,
    val scanResult: ScanResult?,
    val rawOutput: String,
)
