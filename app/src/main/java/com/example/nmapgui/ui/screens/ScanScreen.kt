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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nmapgui.NmapViewModel
import com.example.nmapgui.data.*
import com.example.nmapgui.ui.components.*
import com.example.nmapgui.ui.theme.Green400

@Composable
fun ScanScreen(vm: NmapViewModel) {
    val state by vm.uiState.collectAsState()
    val opts = state.options

    fun update(block: ScanOptions.() -> ScanOptions) = vm.updateOptions(opts.block())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        // ── Status bar ──────────────────────────────────────────────────────
        StatusBar(
            isRootAvailable = state.isRootAvailable,
            resolvedNmapPath = state.resolvedNmapPath,
        )

        // ── Target ──────────────────────────────────────────────────────────
        TargetInput(
            value = opts.target,
            onValueChange = { update { copy(target = it) } },
        )

        // ── Command preview ─────────────────────────────────────────────────
        CommandPreview(command = state.generatedCommand)

        // ── Scan type ───────────────────────────────────────────────────────
        FlagSection("Scan Type", initiallyExpanded = true, badge = ScanType.valueOf(opts.scanType).flag) {
            ScanTypeSelector(
                current = opts.scanType,
                isRoot = state.isRootAvailable,
                onSelect = { update { copy(scanType = it) } },
            )
            // Version intensity slider (only relevant for -sV/-A)
            if (opts.scanType == "VERSION_INTENSITY") {
                VersionIntensityRow(
                    value = opts.versionIntensity,
                    onValueChange = { update { copy(versionIntensity = it) } },
                )
            }
        }

        // ── Port specification ───────────────────────────────────────────────
        FlagSection("Port Specification", badge = PortSpec.valueOf(opts.portSpec).let {
            when (it) {
                PortSpec.ALL -> "-p-"
                PortSpec.FAST -> "-F"
                PortSpec.TOP_N -> "--top-ports"
                PortSpec.CUSTOM -> "-p"
                PortSpec.DEFAULT -> "default"
            }
        }) {
            PortSpecSelector(
                current = opts.portSpec,
                customPorts = opts.customPorts,
                topPortsN = opts.topPortsN,
                onSpecChange = { update { copy(portSpec = it) } },
                onCustomPortsChange = { update { copy(customPorts = it) } },
                onTopNChange = { update { copy(topPortsN = it) } },
            )
        }

        // ── Host Discovery ───────────────────────────────────────────────────
        FlagSection("Host Discovery") {
            SwitchFlagRow(
                label = "Skip host discovery",
                flag = "-Pn",
                checked = opts.skipHostDiscovery,
                infoTitle = "Skip Ping / Treat All Hosts as Online (-Pn)",
                infoDesc = "Never pings or probes the host to determine if it is alive before scanning. Proceeds directly to port scanning every target IP. Use when targets block all ICMP/ARP probes (common on firewalled systems, cloud VMs, or when scanning across the internet). Without -Pn, if the ping fails nmap skips the host entirely and reports 0 ports.",
                onCheckedChange = { update { copy(skipHostDiscovery = it) } },
            )
            SwitchFlagRow(
                label = "TCP SYN ping",
                flag = "-PS${opts.tcpSynPingPorts}",
                checked = opts.tcpSynPing,
                infoTitle = "TCP SYN Ping (-PS[portlist])",
                infoDesc = "Sends empty TCP SYN packets to the specified port(s) as a ping. If the target responds with SYN-ACK or RST, it is considered alive. More likely to penetrate firewalls than ICMP. Default port: 80. Comma-separate multiple ports: 80,443,8080. Requires root for the SYN variant; unprivileged users get connect().",
                onCheckedChange = { update { copy(tcpSynPing = it) } },
            )
            if (opts.tcpSynPing) {
                TextFlagRow(
                    label = "SYN ping ports",
                    flag = "-PS",
                    value = opts.tcpSynPingPorts,
                    placeholder = "80,443",
                    infoTitle = "SYN Ping Port List",
                    infoDesc = "Comma-separated list of TCP ports to probe. e.g. '22,80,443'. Default is 80.",
                    onValueChange = { update { copy(tcpSynPingPorts = it) } },
                )
            }
            SwitchFlagRow(
                label = "TCP ACK ping",
                flag = "-PA${opts.tcpAckPingPorts}",
                checked = opts.tcpAckPing,
                infoTitle = "TCP ACK Ping (-PA[portlist])",
                infoDesc = "Sends empty TCP ACK packets. Stateless firewalls that block SYN may allow ACK. A live host returns RST. Some stateful firewalls may block ACK to ports that weren't opened. Combining -PS and -PA covers both cases.",
                onCheckedChange = { update { copy(tcpAckPing = it) } },
            )
            if (opts.tcpAckPing) {
                TextFlagRow(
                    label = "ACK ping ports",
                    flag = "-PA",
                    value = opts.tcpAckPingPorts,
                    placeholder = "80",
                    infoTitle = "ACK Ping Port List",
                    infoDesc = "Comma-separated port list for ACK ping probes. Default: 80.",
                    onValueChange = { update { copy(tcpAckPingPorts = it) } },
                )
            }
            SwitchFlagRow(
                label = "UDP ping",
                flag = "-PU${opts.udpPingPorts}",
                checked = opts.udpPing,
                infoTitle = "UDP Ping (-PU[portlist])",
                infoDesc = "Sends UDP packets to specified ports. If the port is closed, ICMP port-unreachable is returned proving the host is alive. Open/filtered ports usually produce no response. Useful for targets that filter all TCP but allow UDP (e.g. DNS/SNMP servers). Default port: 40125 (intentionally obscure to provoke ICMP unreachable).",
                onCheckedChange = { update { copy(udpPing = it) } },
            )
            SwitchFlagRow(
                label = "ICMP echo ping",
                flag = "-PE",
                checked = opts.icmpEchoPing,
                infoTitle = "ICMP Echo Ping (-PE)",
                infoDesc = "Classic 'ping' — sends ICMP Echo Request type 8. Many firewalls block ICMP; if they do, the host may appear down when it's not. On a LAN this is reliable and fast. Requires root.",
                onCheckedChange = { update { copy(icmpEchoPing = it) } },
            )
            SwitchFlagRow(
                label = "ICMP timestamp ping",
                flag = "-PP",
                checked = opts.icmpTimestampPing,
                infoTitle = "ICMP Timestamp Ping (-PP)",
                infoDesc = "Sends ICMP timestamp request (type 13). Useful when firewalls block Echo Request (type 8) but accidentally allow timestamp requests. Less common than -PE.",
                onCheckedChange = { update { copy(icmpTimestampPing = it) } },
            )
            SwitchFlagRow(
                label = "ARP ping",
                flag = "-PR",
                checked = opts.arpPing,
                infoTitle = "ARP Ping (-PR)",
                infoDesc = "On a local network (same subnet), uses ARP (Address Resolution Protocol) to discover hosts. ARP cannot be blocked by firewalls at layer 3 — any IP host must respond. Fastest and most reliable method for LAN scanning. Automatically used when available; explicit -PR forces it even when other methods might be tried.",
                onCheckedChange = { update { copy(arpPing = it) } },
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            SwitchFlagRow(
                label = "No DNS resolution",
                flag = "-n",
                checked = opts.noDns,
                infoTitle = "No DNS Resolution (-n)",
                infoDesc = "Never does reverse DNS lookup for discovered IP addresses. Speeds up scans (each DNS lookup adds latency) and avoids DNS-based logging. Use when you already know the IPs and don't need hostnames, or when scanning a large network quickly.",
                onCheckedChange = { update { copy(noDns = it) } },
            )
            SwitchFlagRow(
                label = "Always resolve DNS",
                flag = "-R",
                checked = opts.alwaysResolveDns,
                infoTitle = "Always DNS Resolve (-R)",
                infoDesc = "Resolves DNS for every IP, including hosts marked as 'down' during host discovery. Default: only resolves live hosts. Use when you need hostnames for all targets regardless of status.",
                onCheckedChange = { update { copy(alwaysResolveDns = it) } },
            )
            TextFlagRow(
                label = "Custom DNS servers",
                flag = "--dns-servers",
                value = opts.dnsServers,
                placeholder = "8.8.8.8,1.1.1.1",
                infoTitle = "DNS Servers (--dns-servers)",
                infoDesc = "Comma-separated list of DNS resolvers to use instead of system default. Useful if system DNS is slow, broken, or you want to use a specific internal resolver for your target network.",
                onValueChange = { update { copy(dnsServers = it) } },
            )
        }

        // ── Detection ───────────────────────────────────────────────────────
        FlagSection("Detection & Fingerprinting") {
            SwitchFlagRow(
                label = "OS Detection",
                flag = "-O",
                checked = opts.osDetection,
                infoTitle = "OS Detection (-O)",
                infoDesc = "Uses TCP/IP stack fingerprinting to determine the operating system. Compares probe responses (ICMP, TCP, UDP behavior) against a database of ~2,600 OS fingerprints. Requires root. Works best when the target has at least one open port AND one closed port. Add --osscan-guess for more aggressive guessing when confidence is below 100%.",
                onCheckedChange = { update { copy(osDetection = it) } },
                rootRequired = true,
            )
            if (opts.osDetection) {
                SwitchFlagRow(
                    label = "Aggressive OS guess",
                    flag = "--osscan-guess",
                    checked = opts.osscanGuess,
                    infoTitle = "OS Scan Guess (--osscan-guess)",
                    infoDesc = "When nmap cannot find a perfect OS match (100% accuracy), it normally only reports matches above a certain threshold. --osscan-guess lowers that threshold and reports the best guess even when uncertain. Output will include an accuracy percentage like '(95% confidence)'.",
                    onCheckedChange = { update { copy(osscanGuess = it) } },
                )
                SwitchFlagRow(
                    label = "OS scan limit",
                    flag = "--osscan-limit",
                    checked = opts.osscanLimit,
                    infoTitle = "OS Scan Limit (--osscan-limit)",
                    infoDesc = "Only attempt OS fingerprinting against hosts that have at least one open AND one closed TCP port. Without this, nmap tries OS detection on all live hosts even when it has poor data, wasting time. Use to skip hopeless cases.",
                    onCheckedChange = { update { copy(osscanLimit = it) } },
                )
            }
        }

        // ── Timing ──────────────────────────────────────────────────────────
        FlagSection("Timing & Performance", badge = TimingTemplate.valueOf(opts.timingTemplate).flag) {
            TimingSelector(
                current = opts.timingTemplate,
                onSelect = { update { copy(timingTemplate = it) } },
            )
            TextFlagRow(
                label = "Max retries",
                flag = "--max-retries",
                value = opts.maxRetries,
                placeholder = "10 (default)",
                infoTitle = "Max Retransmissions (--max-retries N)",
                infoDesc = "Maximum number of times to re-probe a port before giving up and marking it filtered. Default: 10. Lower value (e.g. 2) dramatically speeds scans by not waiting on non-responsive ports, but risks false filtered results. Raise if you have a lossy network.",
                onValueChange = { update { copy(maxRetries = it) } },
            )
            TextFlagRow(
                label = "Min rate (pkts/sec)",
                flag = "--min-rate",
                value = opts.minRate,
                placeholder = "e.g. 1000",
                infoTitle = "Minimum Send Rate (--min-rate N)",
                infoDesc = "Force nmap to send at least N packets per second regardless of network conditions. Useful to guarantee fast scanning. Setting too high on slow networks causes packet loss → missed open ports. Nmap's adaptive timing normally chooses this automatically; use --min-rate only when you need a guaranteed floor.",
                onValueChange = { update { copy(minRate = it) } },
            )
            TextFlagRow(
                label = "Max rate (pkts/sec)",
                flag = "--max-rate",
                value = opts.maxRate,
                placeholder = "e.g. 500",
                infoTitle = "Maximum Send Rate (--max-rate N)",
                infoDesc = "Cap packet send rate at N per second. Use on fragile targets (printers, embedded devices, ICS/SCADA) to prevent overwhelming them, or when you have a bandwidth constraint and must not exceed a certain rate.",
                onValueChange = { update { copy(maxRate = it) } },
            )
            TextFlagRow(
                label = "Host timeout",
                flag = "--host-timeout",
                value = opts.hostTimeout,
                placeholder = "e.g. 30m, 1h",
                infoTitle = "Host Timeout (--host-timeout <time>)",
                infoDesc = "Give up on a host after this much time and move on. Format: 30s, 5m, 1h. Prevents any single slow host from blocking a large network scan. That host will be skipped in results.",
                onValueChange = { update { copy(hostTimeout = it) } },
            )
            TextFlagRow(
                label = "Min parallelism",
                flag = "--min-parallelism",
                value = opts.minParallelism,
                placeholder = "e.g. 10",
                infoTitle = "Minimum Parallelism (--min-parallelism N)",
                infoDesc = "Maintain at least N parallel probes outstanding at any time. Nmap normally starts conservatively. Setting a minimum floor speeds up sluggish adaptive timing on reliable networks.",
                onValueChange = { update { copy(minParallelism = it) } },
            )
            TextFlagRow(
                label = "Max parallelism",
                flag = "--max-parallelism",
                value = opts.maxParallelism,
                placeholder = "e.g. 1",
                infoTitle = "Maximum Parallelism (--max-parallelism N)",
                infoDesc = "Cap the number of simultaneous outstanding probes. Setting --max-parallelism 1 forces serial scanning (like -T0/T1 behavior). Use to reduce load on fragile targets or very slow networks.",
                onValueChange = { update { copy(maxParallelism = it) } },
            )
            TextFlagRow(
                label = "Scan delay",
                flag = "--scan-delay",
                value = opts.scanDelay,
                placeholder = "e.g. 500ms, 1s",
                infoTitle = "Scan Delay (--scan-delay <time>)",
                infoDesc = "Minimum delay between each probe to a single host. Format: 500ms, 1s, 0.5s. Useful for rate-limiting on hosts that have IDS/IPS triggered by rapid scanning, or to comply with site-specific rules.",
                onValueChange = { update { copy(scanDelay = it) } },
            )
        }

        // ── NSE Scripts ─────────────────────────────────────────────────────
        FlagSection("NSE Scripts") {
            SwitchFlagRow(
                label = "Default scripts",
                flag = "-sC",
                checked = opts.scriptScan,
                infoTitle = "Default Script Scan (-sC)",
                infoDesc = "Equivalent to --script=default. Runs scripts from the 'default' category which are considered safe, fast, and useful for most scans. Examples: banner grabbing, SSH host-key extraction, HTTP title, SSL certificate info, SMB security mode. Always safe to run on your own systems.",
                onCheckedChange = { update { copy(scriptScan = it) } },
            )
            TextFlagRow(
                label = "Scripts (comma-sep names or categories)",
                flag = "--script",
                value = opts.customScripts,
                placeholder = "vuln, smb-vuln-ms17-010, http-title",
                infoTitle = "NSE Script Selection (--script=<name|category>)",
                infoDesc = """Select specific scripts or categories.

CATEGORIES (safe to use):
• default — safe, common scripts (same as -sC)
• discovery — learn more about targets (whois, DNS, SNMP)
• safe — won't crash services or be overly intrusive
• version — used internally by -sV

USE WITH CAUTION:
• auth — test default credentials
• brute — brute-force logins (slow, noisy)
• vuln — check for known CVEs
• exploit — attempt exploitation (dangerous)
• intrusive — may crash services
• dos — denial-of-service (destructive!)
• fuzzer — send malformed packets

COMMON INDIVIDUAL SCRIPTS:
http-title, http-headers, http-methods
ssl-cert, ssl-enum-ciphers
ssh-hostkey, ssh-auth-methods
smb-vuln-ms17-010, smb-security-mode
dns-brute, ftp-anon, mysql-empty-password""",
                onValueChange = { update { copy(customScripts = it) } },
                singleLine = false,
            )
            TextFlagRow(
                label = "Script arguments",
                flag = "--script-args",
                value = opts.scriptArgs,
                placeholder = "user=admin,pass=admin123",
                infoTitle = "Script Arguments (--script-args key=value,...)",
                infoDesc = "Pass arguments to NSE scripts. Comma-separated key=value pairs. Examples:\nuser=admin,pass=secret → for auth/brute scripts\nhttps://example.com → for http scripts\nsmb.username=admin,smb.password=password → for SMB scripts\nCheck individual script docs: nmap --script-help <scriptname>",
                onValueChange = { update { copy(scriptArgs = it) } },
            )
        }

        // ── Output ──────────────────────────────────────────────────────────
        FlagSection("Output & Display") {
            VerbositySelector(
                current = opts.verbosity,
                onSelect = { update { copy(verbosity = it) } },
            )
            SwitchFlagRow(
                label = "Show reason",
                flag = "--reason",
                checked = opts.showReason,
                infoTitle = "Port State Reason (--reason)",
                infoDesc = "Adds a 'reason' column to port output explaining WHY each port was marked open, closed, or filtered. Examples: 'syn-ack' (open), 'rst' (closed), 'no-response' (filtered), 'admin-prohibited'. Essential for understanding firewall behavior.",
                onCheckedChange = { update { copy(showReason = it) } },
            )
            SwitchFlagRow(
                label = "Show open ports only",
                flag = "--open",
                checked = opts.openPortsOnly,
                infoTitle = "Open Ports Only (--open)",
                infoDesc = "Only show ports in 'open' or 'open|filtered' states. Hides closed and filtered ports from output. Makes large scan results dramatically more readable when most ports are closed.",
                onCheckedChange = { update { copy(openPortsOnly = it) } },
            )
            SwitchFlagRow(
                label = "Traceroute",
                flag = "--traceroute",
                checked = opts.traceroute,
                infoTitle = "Traceroute (--traceroute)",
                infoDesc = "Traces the hop path to each target after scanning, similar to 'traceroute' or 'tracert'. Shows intermediate routers between you and the target. Uses the port/protocol found most likely to reach the target. Note: -A includes --traceroute automatically.",
                onCheckedChange = { update { copy(traceroute = it) } },
            )
            SwitchFlagRow(
                label = "Packet trace",
                flag = "--packet-trace",
                checked = opts.packetTrace,
                infoTitle = "Packet Trace (--packet-trace)",
                infoDesc = "Shows all packets sent and received, similar to tcpdump. Extremely verbose — intended for debugging nmap behavior or understanding exactly what is sent. Output includes SENT/RCVD with packet details (flags, TTL, data). Use for troubleshooting only.",
                onCheckedChange = { update { copy(packetTrace = it) } },
            )
        }

        // ── Evasion ──────────────────────────────────────────────────────────
        FlagSection("Evasion & Spoofing") {
            InfoBanner(
                text = "⚠ Evasion flags require root. Spoofing source IPs (-S) means scan replies return to the spoofed address, not you — use with extreme care on networks you control."
            )
            SwitchFlagRow(
                label = "Fragment packets",
                flag = "-f",
                checked = opts.fragmentPackets,
                infoTitle = "Fragment Packets (-f)",
                infoDesc = "Splits TCP header across multiple IP fragments (8 bytes each). Makes it harder for packet filters, IDS, and deep packet inspection to reassemble and identify the probe. Some older firewalls can't handle fragments and pass them through. Add -f twice (-ff) for 16-byte fragments. Requires root.",
                onCheckedChange = { update { copy(fragmentPackets = it) } },
                rootRequired = true,
            )
            TextFlagRow(
                label = "Custom MTU (fragmentation)",
                flag = "--mtu",
                value = opts.mtu,
                placeholder = "8, 16, 24… (must be multiple of 8)",
                infoTitle = "Custom MTU (--mtu <size>)",
                infoDesc = "Custom offset for packet fragmentation. Must be a multiple of 8. Overrides -f. Smaller values = more fragments = harder for IDS to inspect, but more likely to be dropped by some routers. Requires root.",
                onValueChange = { update { copy(mtu = it) } },
            )
            TextFlagRow(
                label = "Decoy scan",
                flag = "-D",
                value = opts.decoys,
                placeholder = "RND:5  or  decoy1,ME,decoy2",
                infoTitle = "Decoy Scan (-D <decoy1>[,<decoy2>,...,ME])",
                infoDesc = "Makes the scan appear to originate from multiple source IPs simultaneously, burying your real IP in a crowd of decoys. 'ME' indicates your real IP's position in the list. 'RND:5' uses 5 random IPs. Target logs see all IPs but can't easily determine which is real. Decoys must be up or SYN scan response routing fails. Requires root.",
                onValueChange = { update { copy(decoys = it) } },
            )
            TextFlagRow(
                label = "Spoof source IP",
                flag = "-S",
                value = opts.sourceIp,
                placeholder = "192.168.1.100",
                infoTitle = "Source IP Spoof (-S <IP>)",
                infoDesc = "Forges the source IP address in outgoing packets. Requires root. Responses go to the spoofed IP (not you), so you cannot see results — usually used in conjunction with a sniffer on the network, or for blind attacks. Mainly useful for testing IDS/firewall behavior. Only use on networks you own.",
                onValueChange = { update { copy(sourceIp = it) } },
            )
            TextFlagRow(
                label = "Source port",
                flag = "--source-port",
                value = opts.sourcePort,
                placeholder = "53  or  20",
                infoTitle = "Source Port Spoofing (--source-port <port>)",
                infoDesc = "Use a specific source port number. Some firewalls trust traffic appearing to come from certain ports (e.g., port 53/DNS, port 20/FTP-data). If a firewall rule says 'allow established from port 53', spoofing source port 53 might bypass it on stateless firewalls. Does not work on stateful firewalls (they track connection state, not just port numbers).",
                onValueChange = { update { copy(sourcePort = it) } },
            )
            TextFlagRow(
                label = "Network interface",
                flag = "-e",
                value = opts.networkInterface,
                placeholder = "wlan0, eth0, tun0",
                infoTitle = "Network Interface (-e <iface>)",
                infoDesc = "Specify which network interface to send packets through. Nmap normally auto-detects; use this when you have multiple interfaces and need to control which one is used (e.g., VPN tun0 vs WiFi wlan0).",
                onValueChange = { update { copy(networkInterface = it) } },
            )
            TextFlagRow(
                label = "Spoof MAC address",
                flag = "--spoof-mac",
                value = opts.spoofMac,
                placeholder = "0  (random)  or  DE:AD:BE:EF:00:01",
                infoTitle = "MAC Address Spoof (--spoof-mac <MAC|0|vendor>)",
                infoDesc = "Only affects LAN scans (same broadcast domain). 0 = random MAC each run. Full MAC = 'DE:AD:BE:EF:00:01'. Vendor prefix = 'Apple', 'Cisco', '00:50:56' (VMware). Use to obscure your device identity from network logs, MAC-based access controls, or to impersonate a specific vendor.",
                onValueChange = { update { copy(spoofMac = it) } },
            )
            TextFlagRow(
                label = "Data length padding",
                flag = "--data-length",
                value = opts.dataLength,
                placeholder = "e.g. 25",
                infoTitle = "Random Data Padding (--data-length N)",
                infoDesc = "Appends N random bytes to probes. Some IDS/IPS systems signature-match on packet length or exact payload. Adding random padding makes packets look less like nmap scans. Doesn't fix the flags or timing patterns but obscures length-based signatures.",
                onValueChange = { update { copy(dataLength = it) } },
            )
            TextFlagRow(
                label = "IP TTL",
                flag = "--ttl",
                value = opts.ttl,
                placeholder = "e.g. 64, 128, 255",
                infoTitle = "IP Time-To-Live (--ttl <value>)",
                infoDesc = "Set the IP Time-To-Live field in packets. TTL determines how many router hops a packet can traverse before being dropped. Setting a low value limits how far probes travel (useful for LAN-only scanning). Some OS fingerprinting signatures include TTL, so setting an unusual TTL can confuse passive fingerprinters about your OS.",
                onValueChange = { update { copy(ttl = it) } },
            )
            SwitchFlagRow(
                label = "Randomize target order",
                flag = "--randomize-hosts",
                checked = opts.randomizeHosts,
                infoTitle = "Randomize Host Order (--randomize-hosts)",
                infoDesc = "Scans targets in random order rather than sequential. Spreads probes across the target network over time, which can be less obvious to IDS systems that detect sequential scanning patterns. Useful when scanning a /16 or larger and you want to avoid triggering sequential-scan alarms.",
                onCheckedChange = { update { copy(randomizeHosts = it) } },
            )
            SwitchFlagRow(
                label = "Bad checksum (--badsum)",
                flag = "--badsum",
                checked = opts.badSum,
                infoTitle = "Invalid Checksum (--badsum)",
                infoDesc = "Sends packets with deliberately invalid TCP/UDP/SCTP checksums. Any real TCP/IP stack will drop these. If you receive a response, it means a firewall/IDS is responding to the port probe without actually forwarding the packet to the real host — revealing the presence of a middlebox. Useful for detecting NAT devices, load balancers, or buggy firewalls.",
                onCheckedChange = { update { copy(badSum = it) } },
            )
        }

        // ── Extra args ───────────────────────────────────────────────────────
        FlagSection("Extra / Raw Arguments") {
            TextFlagRow(
                label = "Additional nmap arguments",
                flag = "",
                value = opts.extraArgs,
                placeholder = "--script-updatedb  --iflist  --version-all",
                infoTitle = "Extra Raw Arguments",
                infoDesc = "Any additional nmap flags not covered by the UI above. Appended verbatim to the end of the command. Space-separated. Useful for: --iflist (list interfaces/routes), --version-all (try every version probe), --script-updatedb (update NSE database), --privileged (assume root even when not), --unprivileged (assume non-root).",
                onValueChange = { update { copy(extraArgs = it) } },
                singleLine = false,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Run / Stop button ────────────────────────────────────────────────
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)) {
            if (state.isScanning) {
                Button(
                    onClick = { vm.stopScan() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Scan", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { vm.startScan() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = opts.target.isNotBlank() && state.resolvedNmapPath.isNotBlank(),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run Scan", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.resolvedNmapPath.isBlank()) {
            Text(
                "⚠ nmap not found. Install via Termux or set path in Settings.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // Error snackbar
        state.errorMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(4000)
                vm.clearError()
            }
            Text(
                msg,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun StatusBar(isRootAvailable: Boolean, resolvedNmapPath: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Root badge
        Surface(
            color = if (isRootAvailable)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (isRootAvailable) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isRootAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(
                    if (isRootAvailable) "Root" else "No Root",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRootAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        // Nmap path
        Text(
            if (resolvedNmapPath.isNotBlank()) "nmap: $resolvedNmapPath" else "nmap: NOT FOUND",
            style = MaterialTheme.typography.labelSmall,
            color = if (resolvedNmapPath.isNotBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 1,
        )
    }
}

@Composable
private fun TargetInput(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        label = { Text("Target") },
        placeholder = { Text("192.168.1.1  or  192.168.1.0/24  or  example.com") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            InfoIcon(
                title = "Target Specification",
                description = """Nmap accepts many target formats:

SINGLE HOST:
• 192.168.1.1 (IPv4)
• 2001:db8::1 (IPv6)
• example.com (hostname)

RANGES:
• 192.168.1.0/24 (CIDR — 256 hosts)
• 192.168.1.1-254 (dash range — 254 hosts)
• 192.168.1,2.1-10 (comma lists — 20 hosts)
• 10.*.1.1-10 (wildcard)

MULTIPLE TARGETS:
• Space-separate: 192.168.1.1 10.0.0.1
• -iL hosts.txt (load from file)
• --exclude 192.168.1.5 (exclude host)
• --excludefile excl.txt (exclude list)"""
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun ScanTypeSelector(
    current: String,
    isRoot: Boolean,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        ScanType.entries.forEach { type ->
            val enabled = !type.needsRoot || isRoot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = current == type.name,
                    onClick = { if (enabled) onSelect(type.name) },
                    enabled = enabled,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            type.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                        if (type.needsRoot && !isRoot) {
                            Text(
                                "NEEDS ROOT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 9.sp,
                            )
                        }
                        InfoIcon(title = type.label, description = type.info)
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionIntensityRow(value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Version intensity: $value", style = MaterialTheme.typography.bodyMedium)
            InfoIcon(
                title = "Version Intensity (--version-intensity 0-9)",
                description = """Controls how aggressively nmap probes for service/version info.

0 — Intensity 0 (lightest): Only sends probes marked 'rarity:0'. Fastest but misses obscure services.
2 — --version-light equivalent: Quick scan with common probes only.
7 — Default: Good balance of speed vs accuracy.
9 — --version-all equivalent: Tries every single probe. Slowest but most thorough.

Higher intensity increases: scan time, accuracy, and detectability. For most scans, the default (7) is appropriate."""
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..9f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0 (light)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("9 (all)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PortSpecSelector(
    current: String,
    customPorts: String,
    topPortsN: String,
    onSpecChange: (String) -> Unit,
    onCustomPortsChange: (String) -> Unit,
    onTopNChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        PortSpec.entries.forEach { spec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == spec.name, onClick = { onSpecChange(spec.name) })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(spec.label, style = MaterialTheme.typography.bodyMedium)
                    InfoIcon(title = spec.label, description = spec.info)
                }
            }
        }
        if (current == "CUSTOM") {
            OutlinedTextField(
                value = customPorts,
                onValueChange = onCustomPortsChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                label = { Text("Port expression (-p)") },
                placeholder = { Text("22,80,443,8080-8090,U:53") },
                singleLine = true,
            )
        }
        if (current == "TOP_N") {
            OutlinedTextField(
                value = topPortsN,
                onValueChange = onTopNChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                label = { Text("Top N ports") },
                placeholder = { Text("500") },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun TimingSelector(current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        TimingTemplate.entries.forEach { t ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == t.name, onClick = { onSelect(t.name) })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(t.label, style = MaterialTheme.typography.bodyMedium)
                    InfoIcon(title = t.label, description = t.info)
                }
            }
        }
    }
}

@Composable
private fun VerbositySelector(current: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Text("Verbosity", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
        VerbosityLevel.entries.forEach { v ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == v.name, onClick = { onSelect(v.name) })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(v.label, style = MaterialTheme.typography.bodyMedium)
                    InfoIcon(title = v.label, description = v.info)
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
