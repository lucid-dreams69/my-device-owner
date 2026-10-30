# My Device Owner

A minimal Android **device-owner (DPC) app** for learning Android Enterprise
QR-code enrollment on a personal phone. Built for a Nothing Phone 2; works on
any phone running Android 12 or later.

Once enrolled, the app shows:

- **Status**: whether it is the device owner, plus model, Android version and security patch
- **Enrollment log**: each enrollment callback, the time it arrived, and any extras from the QR code
- **Try a policy**: disable the camera, block screenshots, set a lock screen message, lock the screen now
- **Remove device owner**: clears all of the above and turns it back into a normal, uninstallable app

## Enroll with the QR code

> **A factory reset erases everything on the phone.** Back up first. Have your
> Google account password ready, because Factory Reset Protection asks for it
> after the reset.

1. Factory reset the phone: **Settings → System → Reset options → Erase all data**.
2. On the first welcome screen, **tap 6 times** on an empty spot. A QR scanner
   opens. It may ask you to connect to Wi-Fi first.
3. Scan `enrollment-qr.png` (attached to the
   [latest release](../../releases/latest)).
4. The phone downloads this app and says it will be managed by it ("This
   device belongs to your organization"). Accept and finish setup as usual.
   You can sign in to your Google account during setup or later.
5. Open **My Device Owner**. It should say **Device owner: active**.

All the phone's preinstalled apps stay enabled
(`PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED`).

## Undo it

Open the app, tap **Remove device owner**, then uninstall it like any other
app. No second factory reset is needed.

## Try it without a factory reset (adb)

Android allows this only while the phone has **no accounts** (remove Google and
any other accounts temporarily):

```
adb install my-device-owner.apk
adb shell dpm set-device-owner io.github.luciddreams69.deviceowner/.AdminReceiver
```

## Build it yourself

Requirements: Android SDK (platform 35, build-tools 35) and JDK 17–23.

1. Create a signing key and a `keystore.properties` file next to `settings.gradle`:

   ```
   keytool -genkeypair -keystore release.jks -storetype PKCS12 -alias deviceowner -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=My Device Owner"
   ```

   ```properties
   storeFile=release.jks
   storePassword=...
   keyAlias=deviceowner
   keyPassword=...
   ```

   Keep `release.jks` safe and private. Updates must be signed with the same key,
   or the enrolled phone rejects them and the QR code checksum no longer matches.

2. Build: `gradlew assembleRelease` (output: `app/build/outputs/apk/release/app-release.apk`)
3. Make the QR code (`pip install segno`):

   ```
   python tools/make_qr.py --apk app/build/outputs/apk/release/app-release.apk --url https://github.com/<you>/<repo>/releases/latest/download/my-device-owner.apk
   ```

   Add `--wifi-ssid` and `--wifi-password` to put Wi-Fi details in the code.

## How enrollment works

The QR code is a small JSON object. It holds the admin component name, the APK
download URL, and the SHA-256 checksum of the APK's signing certificate. On
Android 12+ the setup wizard then calls the app in this order:

1. `GET_PROVISIONING_MODE`: `ProvisioningModeActivity` answers "fully managed device"
2. The app becomes device owner (`DEVICE_ADMIN_ENABLED` and `AdminReceiver`)
3. `ADMIN_POLICY_COMPLIANCE`: `PolicyComplianceActivity` is where a real company
   app would apply its starting policies

The enrollment log screen shows the order these actually arrived on your phone.
