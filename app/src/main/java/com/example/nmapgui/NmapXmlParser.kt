package com.example.nmapgui

import com.example.nmapgui.data.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class NmapXmlParser {

    fun parse(xml: String): ScanResult {
        if (xml.isBlank()) return ScanResult()
        return try {
            parseInternal(xml)
        } catch (e: Exception) {
            ScanResult(rawOutput = xml, scanStats = "Parse error: ${e.message}")
        }
    }

    private fun parseInternal(xml: String): ScanResult {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val hosts = mutableListOf<HostResult>()
        var startTime = ""
        var elapsedTime = ""
        var scanStats = ""

        // Mutable state during parse
        var currentAddress = ""
        var currentAddressType = "ipv4"
        var currentHostname = ""
        var currentState = ""
        var currentOsMatches = mutableListOf<OsMatch>()
        var currentPorts = mutableListOf<PortResult>()
        var currentDistance = ""
        var inHost = false

        var currentPortId = ""
        var currentProto = ""
        var currentPortState = ""
        var currentService = ""
        var currentVersion = ""
        var currentProduct = ""
        var currentExtraInfo = ""
        var currentReason = ""
        var currentScripts = mutableListOf<ScriptResult>()
        var inPort = false

        var inScript = false
        var scriptId = ""
        var scriptOutput = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "nmaprun" -> {
                        startTime = parser.getAttributeValue(null, "startstr") ?: ""
                    }
                    "runstats" -> {}
                    "finished" -> {
                        elapsedTime = parser.getAttributeValue(null, "elapsed") ?: ""
                        val summary = parser.getAttributeValue(null, "summary") ?: ""
                        scanStats = "Elapsed: ${elapsedTime}s. $summary"
                    }
                    "hosts" -> {
                        val up = parser.getAttributeValue(null, "up") ?: "?"
                        val down = parser.getAttributeValue(null, "down") ?: "?"
                        val total = parser.getAttributeValue(null, "total") ?: "?"
                        scanStats += " Hosts: $up up, $down down, $total total."
                    }
                    "host" -> {
                        inHost = true
                        currentAddress = ""
                        currentAddressType = "ipv4"
                        currentHostname = ""
                        currentState = "up"
                        currentOsMatches = mutableListOf()
                        currentPorts = mutableListOf()
                        currentDistance = ""
                    }
                    "address" -> if (inHost) {
                        val addrtype = parser.getAttributeValue(null, "addrtype") ?: "ipv4"
                        if (addrtype in listOf("ipv4", "ipv6")) {
                            currentAddress = parser.getAttributeValue(null, "addr") ?: ""
                            currentAddressType = addrtype
                        }
                    }
                    "hostname" -> if (inHost) {
                        if ((parser.getAttributeValue(null, "type") ?: "") != "PTR"
                            || currentHostname.isEmpty()) {
                            currentHostname = parser.getAttributeValue(null, "name") ?: ""
                        }
                    }
                    "status" -> if (inHost) {
                        currentState = parser.getAttributeValue(null, "state") ?: "up"
                    }
                    "port" -> if (inHost) {
                        inPort = true
                        currentPortId = parser.getAttributeValue(null, "portid") ?: ""
                        currentProto = parser.getAttributeValue(null, "protocol") ?: "tcp"
                        currentPortState = ""
                        currentService = ""
                        currentVersion = ""
                        currentProduct = ""
                        currentExtraInfo = ""
                        currentReason = ""
                        currentScripts = mutableListOf()
                    }
                    "state" -> if (inPort) {
                        currentPortState = parser.getAttributeValue(null, "state") ?: ""
                        currentReason = parser.getAttributeValue(null, "reason") ?: ""
                    }
                    "service" -> if (inPort) {
                        currentService = parser.getAttributeValue(null, "name") ?: ""
                        currentProduct = parser.getAttributeValue(null, "product") ?: ""
                        currentVersion = parser.getAttributeValue(null, "version") ?: ""
                        currentExtraInfo = parser.getAttributeValue(null, "extrainfo") ?: ""
                    }
                    "script" -> if (inPort) {
                        inScript = true
                        scriptId = parser.getAttributeValue(null, "id") ?: ""
                        scriptOutput = parser.getAttributeValue(null, "output") ?: ""
                    }
                    "osmatch" -> if (inHost) {
                        currentOsMatches.add(
                            OsMatch(
                                name = parser.getAttributeValue(null, "name") ?: "",
                                accuracy = parser.getAttributeValue(null, "accuracy") ?: ""
                            )
                        )
                    }
                    "distance" -> if (inHost) {
                        currentDistance = parser.getAttributeValue(null, "value") ?: ""
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "port" -> if (inPort) {
                        currentPorts.add(
                            PortResult(
                                portId = currentPortId,
                                protocol = currentProto,
                                state = currentPortState,
                                service = currentService,
                                product = currentProduct,
                                version = currentVersion,
                                extraInfo = currentExtraInfo,
                                reason = currentReason,
                                scripts = currentScripts.toList(),
                            )
                        )
                        inPort = false
                    }
                    "script" -> if (inScript) {
                        currentScripts.add(ScriptResult(scriptId, scriptOutput))
                        inScript = false
                    }
                    "host" -> if (inHost) {
                        hosts.add(
                            HostResult(
                                address = currentAddress,
                                addressType = currentAddressType,
                                hostname = currentHostname,
                                state = currentState,
                                osMatches = currentOsMatches.toList(),
                                ports = currentPorts.toList(),
                                distance = currentDistance,
                            )
                        )
                        inHost = false
                    }
                }
            }
            event = parser.next()
        }

        return ScanResult(
            hosts = hosts,
            scanStats = scanStats.trim(),
            startTime = startTime,
            elapsedTime = elapsedTime,
            rawOutput = xml,
        )
    }
}
