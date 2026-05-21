#!/usr/bin/env python3
"""
StateLink PC Monitor Server v1.0
=================================
USB-connected PC stats monitor for the StateLink Android app.

This script runs on your Windows or Mac computer. It collects real-time
system stats (CPU, memory, disk, network, battery) and serves them on
localhost port 8765. The Android phone connects via ADB reverse tunnel
over a USB cable — no WiFi or internet needed.

HOW TO USE (one-time setup):
    1. Install Python 3.9+ from https://python.org
    2. Open a terminal / command prompt, run:
         pip install psutil
    3. Double-click this script, or run:
         python statelink_server.py

PACKAGING AS STANDALONE EXE/APP:
    pip install pyinstaller
    pyinstaller --onefile --console statelink_server.py
    (The exe/app will be in the "dist" folder)

For detailed zero-basics tutorial, see the TUTORIAL.md file.
"""

import http.server
import json
import sys
import os
import subprocess
import threading
import time
import platform

# ──────────────────────────────────────────────
#  Check for psutil
# ──────────────────────────────────────────────
try:
    import psutil
except ImportError:
    print("=" * 50)
    print("  ERROR: 'psutil' package is missing!")
    print()
    print("  Please open a Terminal/Command Prompt and run:")
    print("    pip install psutil")
    print()
    print("  Then try running this script again.")
    print("=" * 50)
    input("Press Enter to exit...")
    sys.exit(1)

# ──────────────────────────────────────────────
#  Global state
# ──────────────────────────────────────────────
current_stats = {}
stats_lock = threading.Lock()

# For network speed calculation
_net_prev_timestamp = time.time()
_net_prev_sent = 0
_net_prev_recv = 0


def format_speed(kbps):
    """Format KB/s into readable string."""
    if kbps < 0.1:
        return "0 KB/s"
    elif kbps < 1024:
        return f"{kbps:.1f} KB/s"
    else:
        return f"{kbps / 1024:.1f} MB/s"


def format_bytes_gb(num_bytes):
    """Convert bytes to GB with 1 decimal place."""
    return round(num_bytes / (1024**3), 1)


def get_disk_usage():
    """Get root disk usage, cross-platform."""
    if platform.system() == "Windows":
        return psutil.disk_usage("C:\\")
    else:
        return psutil.disk_usage("/")


def collect_network_speed():
    """Calculate upload/download speed in KB/s since last call."""
    global _net_prev_timestamp, _net_prev_sent, _net_prev_recv

    net = psutil.net_io_counters()
    now = time.time()
    elapsed = now - _net_prev_timestamp

    if elapsed <= 0:
        return (0.0, 0.0)

    up_speed = (net.bytes_sent - _net_prev_sent) / elapsed / 1024.0
    down_speed = (net.bytes_recv - _net_prev_recv) / elapsed / 1024.0

    _net_prev_timestamp = now
    _net_prev_sent = net.bytes_sent
    _net_prev_recv = net.bytes_recv

    # Guard against transient negative values (interface reset)
    if up_speed < 0:
        up_speed = 0.0
    if down_speed < 0:
        down_speed = 0.0

    return (up_speed, down_speed)


def collect_stats_loop():
    """Background thread: collect stats every second."""
    global current_stats

    while True:
        try:
            # ── CPU ──────────────────────────
            cpu = psutil.cpu_percent(interval=0.3)

            # ── Memory ───────────────────────
            mem = psutil.virtual_memory()

            # ── Disk ─────────────────────────
            disk = get_disk_usage()

            # ── Network speed ────────────────
            up_speed, down_speed = collect_network_speed()

            # ── Battery (laptops only) ───────
            battery = None
            try:
                battery = psutil.sensors_battery()
            except Exception:
                pass

            stats = {
                "status": "ok",
                "hostname": platform.node(),
                "platform": platform.system(),
                "cpu": round(cpu, 1),
                "cpu_cores": psutil.cpu_count(logical=True),
                "memory": round(mem.percent, 1),
                "memory_used_gb": format_bytes_gb(mem.used),
                "memory_total_gb": format_bytes_gb(mem.total),
                "disk": round(disk.percent, 1),
                "disk_used_gb": format_bytes_gb(disk.used),
                "disk_total_gb": format_bytes_gb(disk.total),
                "network_down_kbs": round(down_speed, 1),
                "network_up_kbs": round(up_speed, 1),
            }

            if battery is not None:
                stats["battery"] = round(battery.percent, 1)
                stats["battery_charging"] = battery.power_plugged
                if battery.secsleft > 0:
                    stats["battery_time_left"] = battery.secsleft
                else:
                    stats["battery_time_left"] = -1
            else:
                stats["battery"] = -1
                stats["battery_charging"] = False

            with stats_lock:
                current_stats = stats

        except Exception:
            # Keep serving the last known good stats
            pass

        time.sleep(1)


# ──────────────────────────────────────────────
#  HTTP request handler
# ──────────────────────────────────────────────
class StatsHandler(http.server.BaseHTTPRequestHandler):
    """Handles HTTP GET requests for /stats endpoint."""

    def do_GET(self):
        if self.path == "/stats":
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()

            with stats_lock:
                payload = json.dumps(current_stats, ensure_ascii=False)

            self.wfile.write(payload.encode("utf-8"))

        elif self.path == "/ping":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(b'{"pong":true}')

        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Suppress default HTTP request logging to keep the console clean
        pass


# ──────────────────────────────────────────────
#  ADB reverse tunnel setup
# ──────────────────────────────────────────────
def find_adb():
    """Try to locate the adb executable."""
    # First try: check PATH
    for path in os.environ.get("PATH", "").split(os.pathsep):
        candidate = os.path.join(path, "adb")
        if os.path.isfile(candidate) or os.path.isfile(candidate + ".exe"):
            return "adb"  # it's on PATH

    # Second try: common Android SDK locations
    home = os.path.expanduser("~")
    common_paths = [
        os.path.join(home, "AppData", "Local", "Android", "Sdk", "platform-tools", "adb.exe"),  # Windows
        os.path.join(home, "Android", "Sdk", "platform-tools", "adb"),                          # Linux
        os.path.join(home, "Library", "Android", "sdk", "platform-tools", "adb"),               # Mac
    ]
    for p in common_paths:
        if os.path.isfile(p):
            return p

    return None


def setup_adb_reverse():
    """Configure ADB reverse port forwarding so the phone can reach the PC."""
    print("─" * 46)
    print("  USB Setup")
    print("─" * 46)

    adb_path = find_adb()
    if not adb_path:
        print()
        print("  WARNING: ADB not found on this computer.")
        print()
        print("  If you are running the server on the SAME computer")
        print("  that has Android Studio, the app can still connect")
        print("  via Android Studio's built-in ADB.")
        print()
        print("  Otherwise, download Platform Tools:")
        print("  https://developer.android.com/studio/releases/platform-tools")
        print()
        return

    try:
        # Check for connected devices
        result = subprocess.run(
            [adb_path, "devices"],
            capture_output=True,
            text=True,
            timeout=10,
        )
        lines = result.stdout.strip().split("\n")
        # Filter out header and empty lines
        device_lines = [
            l for l in lines[1:]
            if l.strip() and "\tdevice" in l
        ]

        if not device_lines:
            print()
            print("  No Android device detected via USB.")
            print()
            print("  Please check:")
            print("  1. Phone is connected via USB cable")
            print("  2. USB Debugging is ON (Developer Options)")
            print("  3. You tapped 'Allow' on the phone's USB")
            print("     debugging authorization popup")
            print()
            return

        print(f"  Found device(s): {len(device_lines)}")
        for d in device_lines:
            print(f"    {d.split()[0]}")

        # Remove any existing reverse mapping first
        subprocess.run(
            [adb_path, "reverse", "--remove", "tcp:8765"],
            capture_output=True,
            timeout=5,
        )

        # Create reverse tunnel: phone port 8765 -> PC port 8765
        result = subprocess.run(
            [adb_path, "reverse", "tcp:8765", "tcp:8765"],
            capture_output=True,
            text=True,
            timeout=10,
        )

        if result.returncode == 0:
            print("  ADB reverse tunnel: phone:8765 -> PC:8765  [OK]")
        else:
            err = result.stderr.strip()
            print(f"  Failed to create reverse tunnel: {err}")

    except subprocess.TimeoutExpired:
        print("  ADB command timed out. Check your USB connection.")
    except Exception as e:
        print(f"  ADB error: {e}")


# ──────────────────────────────────────────────
#  Main entry point
# ──────────────────────────────────────────────
def main():
    print()
    print("╔══════════════════════════════════════════╗")
    print("║      StateLink PC Monitor Server v1.0    ║")
    print("║        USB Data-Link for Android         ║")
    print("╚══════════════════════════════════════════╝")
    print()
    print(f"  Hostname : {platform.node()}")
    print(f"  OS       : {platform.system()} {platform.release()}")
    print(f"  Python   : {sys.version_info.major}.{sys.version_info.minor}")
    print()

    # ── Start background stats collector ──
    collector = threading.Thread(target=collect_stats_loop, daemon=True)
    collector.start()
    time.sleep(0.5)  # Wait for the first stats sample

    # ── Set up ADB reverse tunnel ──
    setup_adb_reverse()

    # ── Start HTTP server ──
    print()
    print("─" * 46)
    print(f"  Server listening on port 8765")
    print(f"  Phone endpoint: http://localhost:8765/stats")
    print()
    print("  Keep this window open while using the app.")
    print("  Press Ctrl + C to stop the server.")
    print("─" * 46)
    print()

    server = http.server.HTTPServer(("0.0.0.0", 8765), StatsHandler)

    def shutdown():
        print()
        print("Shutting down...")
        # Clean up ADB reverse
        adb_path = find_adb()
        if adb_path:
            subprocess.run(
                [adb_path, "reverse", "--remove", "tcp:8765"],
                capture_output=True,
                timeout=5,
            )
        server.shutdown()
        print("Goodbye!")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        shutdown()


if __name__ == "__main__":
    main()
