# NetGuard Mobile

An Android-first learning project for per-app network usage monitoring and basic traffic review.

## Current version: 0.1.0

### Included
- Kotlin + Jetpack Compose dark dashboard
- Today-level per-UID network totals using Android `NetworkStatsManager`
- Downloaded and uploaded byte totals
- App usage list and basic high-usage review threshold
- Usage Access settings shortcut
- GitHub Actions workflow to build a debug APK in the cloud

### Important Android limitations
- Android requires the user to grant Usage Access in system settings. The app does not silently grant this access.
- Network statistics are aggregated by UID and network type. They are not a packet-by-packet live feed.
- Some devices, Android builds, carriers, and emulator images may return incomplete or delayed mobile-data statistics.
- Shared UIDs, system services, and package-visibility restrictions can make app-name attribution incomplete.
- The Connections screen is a clearly marked placeholder. Domain-to-app mapping requires a separate consent-based local VPN/DNS observation implementation and will not be perfectly complete.
- The Alerts screen currently flags high data usage for review. High usage is not proof of malware.

## Build in the cloud
1. Create a GitHub repository and upload the contents of this project.
2. Open the repository's **Actions** tab.
3. Enable GitHub Actions if prompted.
4. Run the **Android build** workflow, or push a commit to `main`.
5. Open the completed workflow run and download the `NetGuardMobile-debug` artifact.
6. Extract the ZIP artifact and install `app-debug.apk` on your Android phone. You may need to allow installation from the browser or file manager you used.

## Local build
Requires JDK 17, Android SDK 35, and Gradle 8.9:
```bash
gradle :app:assembleDebug
```

## Permissions
`PACKAGE_USAGE_STATS` is special app access. Open the in-app Usage Access button and enable NetGuard Mobile in Android Settings. Then return to the app and refresh.

## License
MIT
