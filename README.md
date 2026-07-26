# OrderEcho

**餐厅通话录音管理** — a small, offline Android app for reviewing and safely retaining Huawei phone-call recordings.

## What it does

On the restaurant's Huawei BAC-AL00 running Android 8.0, the system phone app already records calls to:

```text
/storage/emulated/0/Sounds/Callrecord/
```

OrderEcho will scan those `.amr` files, show them grouped by month and date, play them inside the app, and remove recordings older than the selected retention period.

## What it does not do

- Record telephone calls
- Send files or data over the network
- Move or rename Huawei recordings
- Require root access

## Build and install

Use JDK 17 (Android Studio's bundled JDK is suitable), then build the debug APK:

```powershell
./gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on the Huawei BAC-AL00 with Android Studio or ADB, grant storage access, and verify the physical-device acceptance checks in `docs/HANDOFF.md`.

## Status

The first-release Android implementation is complete in this repository. Device acceptance remains required on the restaurant's Huawei BAC-AL00 because no Android device is connected to this development environment.

See [docs/HANDOFF.md](docs/HANDOFF.md) for the current goal and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the approved design.

