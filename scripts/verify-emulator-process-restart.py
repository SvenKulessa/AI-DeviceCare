"""Exercise a force-stop and fresh launch of the Consumer debug APK on an AVD."""

import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from xml.etree import ElementTree

PACKAGE = "online.capitalai.devicecare"
ACTIVITY = f"{PACKAGE}/.MainActivity"
DUMP = "/data/local/tmp/devicecare-process-qa.xml"
SCAN = "Lokale Prüfung starten"
PROFILE = "Geräteprofil"
FOOTER = "Cloud, analytics, VPN, malware scans and paid billing are not active."


def adb(*args, check=True):
    return subprocess.run(
        ["adb", "-e", *args], check=check, capture_output=True, text=True, timeout=30,
    )


def view():
    adb("shell", "rm", "-f", DUMP)
    adb("shell", "uiautomator", "dump", DUMP)
    root = ElementTree.fromstring(adb("exec-out", "cat", DUMP).stdout)
    nodes = [node for node in root.iter("node") if node.get("package") == PACKAGE]
    if not nodes:
        raise RuntimeError("Consumer activity is not in the foreground")
    return nodes


def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.attrib["bounds"])))


def find_text(label, *, expect_no_results=False):
    for _ in range(12):
        nodes = view()
        if expect_no_results and any(
            node.get("text", "") == PROFILE or "Quelle:" in node.get("text", "")
            for node in nodes
        ):
            raise RuntimeError("Inspection evidence was restored without a new scan")
        for node in nodes:
            if node.get("text") == label:
                return node
        left, top, right, bottom = bounds(nodes[0])
        x = str((left + right) // 2)
        height = bottom - top
        adb("shell", "input", "swipe", x, str(top + 4 * height // 5), x, str(top + height // 3), "350")
    raise RuntimeError(f"Expected UI element was not reachable: {label}")


def pid():
    result = adb("shell", "pidof", PACKAGE, check=False)
    value = result.stdout.strip()
    if result.returncode == 1 and not value and not result.stderr:
        return None
    if result.returncode != 0 or not value.isdecimal():
        raise RuntimeError("Could not read the Consumer app process ID")
    return value


def launch():
    result = adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    if "Status: ok" not in result.stdout:
        raise RuntimeError("Consumer activity did not launch")


def verify(apk):
    if adb("shell", "getprop", "ro.kernel.qemu").stdout.strip() != "1":
        raise RuntimeError("Run only on a disposable Android emulator")
    if adb("shell", "getprop", "ro.build.version.sdk").stdout.strip() != "35":
        raise RuntimeError("Expected an Android 15 (API 35) emulator")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("install", "-r", str(apk))
    adb("shell", "am", "force-stop", PACKAGE)
    try:
        launch()
        left, top, right, bottom = bounds(find_text(SCAN))
        adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))
        find_text(PROFILE)
        previous = pid()
        if previous is None:
            raise RuntimeError("The process with the completed scan was not running")
        adb("shell", "am", "force-stop", PACKAGE)
        if pid() is not None:
            raise RuntimeError("The scanned process survived force-stop")

        launch()
        current = pid()
        if current is None or current == previous:
            raise RuntimeError("Expected a different app process after relaunch")
        find_text(SCAN, expect_no_results=True)
        find_text(FOOTER, expect_no_results=True)
    finally:
        adb("shell", "rm", "-f", DUMP)
    print(json.dumps({
        "test": "devicecare-process-restart",
        "api": 35,
        "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
        "profileSeenBeforeForceStop": True,
        "processStopped": True,
        "freshProcess": True,
        "scanButtonReachable": True,
        "noInspectionRestored": True,
    }))


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
