#!/usr/bin/env python3
"""Make the Android Enterprise enrollment QR code for My Device Owner.

During setup the phone downloads the APK from --url and installs it only if its
signing certificate matches the checksum in the QR code. New versions signed
with the same key keep working with the same QR code.

    pip install segno
    python tools/make_qr.py --apk dist/my-device-owner.apk \
        --url https://github.com/<user>/<repo>/releases/latest/download/my-device-owner.apk

Optional: --wifi-ssid NAME --wifi-password PASS (otherwise setup asks you to connect).
"""
import argparse
import base64
import json
import os
import re
import subprocess
from datetime import date
from pathlib import Path

import segno

PACKAGE = "io.github.luciddreams69.deviceowner"
COMPONENT = f"{PACKAGE}/{PACKAGE}.AdminReceiver"
EXTRA = "android.app.extra."


def find_apksigner():
    sdk = (os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
           or os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk"))
    build_tools = Path(sdk, "build-tools")
    name = "apksigner.bat" if os.name == "nt" else "apksigner"
    if build_tools.is_dir():
        for version in sorted(build_tools.iterdir(), reverse=True):  # newest first
            if (version / name).exists():
                return str(version / name)
    raise SystemExit(f"apksigner not found in {build_tools}; set ANDROID_HOME to your Android SDK")


def signature_checksum(apk):
    """URL-safe base64 (no padding) of the SHA-256 of the APK's signing certificate."""
    out = subprocess.run([find_apksigner(), "verify", "--print-certs", apk],
                         capture_output=True, text=True, check=True).stdout
    match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})", out)
    if not match:
        raise SystemExit(f"Could not read the signing certificate of {apk}")
    return base64.urlsafe_b64encode(bytes.fromhex(match.group(1))).decode().rstrip("=")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True, help="signed release APK")
    parser.add_argument("--url", required=True, help="direct HTTPS download link for that APK")
    parser.add_argument("--out", default="dist/enrollment-qr.png", help="PNG to write")
    parser.add_argument("--wifi-ssid")
    parser.add_argument("--wifi-password")
    args = parser.parse_args()

    if not args.url.startswith("https://"):
        raise SystemExit("--url must be an https:// link")

    payload = {
        EXTRA + "PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": COMPONENT,
        EXTRA + "PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": args.url,
        EXTRA + "PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": signature_checksum(args.apk),
        # Keep every preinstalled app (camera, gallery, ...) instead of the bare enterprise set.
        EXTRA + "PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": True,
        # Handed to the app during enrollment; it appears in the app's enrollment log.
        EXTRA + "PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
            "enrolled_via": "qr",
            "qr_made": date.today().isoformat(),
        },
    }
    if args.wifi_ssid:
        payload[EXTRA + "PROVISIONING_WIFI_SSID"] = args.wifi_ssid
        payload[EXTRA + "PROVISIONING_WIFI_SECURITY_TYPE"] = "WPA" if args.wifi_password else "NONE"
        if args.wifi_password:
            payload[EXTRA + "PROVISIONING_WIFI_PASSWORD"] = args.wifi_password

    qr = segno.make(json.dumps(payload, separators=(",", ":")), error="m")
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    qr.save(args.out, scale=8, border=4)

    shown = dict(payload)
    if EXTRA + "PROVISIONING_WIFI_PASSWORD" in shown:
        shown[EXTRA + "PROVISIONING_WIFI_PASSWORD"] = "********"
    print(json.dumps(shown, indent=2))
    print(f"\nWrote {args.out} (QR version {qr.version}, {qr.symbol_size(scale=1, border=0)[0]} modules)")


if __name__ == "__main__":
    main()
