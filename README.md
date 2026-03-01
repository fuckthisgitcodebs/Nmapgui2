# Nmap GUI for Android

A fully-featured Android GUI frontend for the [nmap](https://nmap.org) network scanner.

## Features

- **All major nmap scan types** with full explanations: TCP SYN, TCP Connect, UDP, ACK, FIN, Null, Xmas, Maimon, Window, IP Protocol, Ping, Version, Aggressive
- **Full flag coverage** — every option explained via inline info tooltips (tap ℹ)
- **Live output streaming** — see results as they appear
- **XML result parsing** — structured host/port/service/OS display
- **Scan history** — saved with timestamps, searchable, exportable
- **Root-aware** — root-only scan types are disabled when root is unavailable
- **Dark terminal theme** by default

## Requirements

- Android 8.0+ (API 26+)
- **Root access** recommended (enables SYN/UDP/ACK/OS-detection scans)
- **nmap binary** — must be installed separately

## Installing nmap

### Via Termux (recommended)
```bash
# Install Termux from F-Droid (not Play Store)
pkg install nmap
```
The app auto-detects nmap at `/data/data/com.termux/files/usr/bin/nmap`.

### Custom path
Set the path manually in **Settings → nmap binary path**.

## Building

### GitHub Actions (automatic)
Push to any branch — `.github/workflows/build.yml` builds the debug APK automatically.

### Local build
```bash
# Requires Java 17, Android SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Legal

Only scan networks and systems you own or have explicit written permission to scan.
Unauthorized port scanning is illegal in many jurisdictions.
