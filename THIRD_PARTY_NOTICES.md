# Third-party notices

HyperAlarm's own code is licensed under **AGPL-3.0** (`LICENSE`).

Code inherited from the HyperCeiler fork base keeps its original terms: the bulk of it is **MIT**
(`MIT_License.md`), and individual files carry an **AGPL-3.0** header of their own (for example
`library/libhook/src/main/java/com/hyperalarm/libhook/utils/api/DeviceTypeUtils.java`). Both are
compatible with this repository's AGPL-3.0, so the combined work is distributed under AGPL-3.0.

This file lists the third-party components that ship in or build the APK.

## Runtime dependencies

### libxposed api / service

- Used for: the modern Xposed entry point and the hook API inside the clock process
- Upstream: <https://github.com/libxposed/api>
- License: Apache License 2.0

### Gson

- Used for: the JSON payload of the bridge between the module app and the clock process
  (`AlarmItem` / `HookAlarmQuery`); its field names are the wire format, so it is a runtime
  dependency, not a test one
- Upstream: <https://github.com/google/gson>
- License: Apache License 2.0

### AndroidX / Room

- Used for: the app's UI plumbing and the log database (`LogRepository`)
- Upstream: <https://developer.android.com/jetpack/androidx>
- License: Apache License 2.0

### libsu

- Used for: the single root shell implementation (`com.hyperalarm.common.utils.RootShell`)
- Upstream: <https://github.com/topjohnwu/libsu>
- License: Apache License 2.0

### MIUIX (`fan.miuix:*`)

- Used for: every screen of the module (appcompat, preference, bottomsheet, slidingwidget,
  navigator, nestedheader, pickerwidget, ...)
- Upstream: <https://github.com/miuix-kotlin-multiplatform/miuix>
- License: as published by its authors upstream

### EzHookTool / HiddenApiBypass / HyperFocusApi / LunarCalendar / AppIconLoader

- Used for: hook helpers, hidden API access, focus-notification helpers, lunar calendar conversion
  and installed-app icons
- Upstream: see the coordinates in `gradle/libs.versions.toml`
- License: as published by their authors upstream

### LSParanoid

- Used for: string obfuscation of the hook module at build time only (not shipped as code)
- Upstream: <https://github.com/LSPosed/LSParanoid>
- License: Apache License 2.0

## Code adapted from other projects

### HyperCeiler (fork base)

Parts of `library/core`, `library/provision`, `library/common` and the MIUIX resources were adapted
from HyperCeiler.

- Upstream: <https://github.com/ReChronoRain/HyperCeiler>
- License: MIT, full text in `MIT_License.md`
- Changes: package/name adaptation, reduced to the single `com.android.deskclock` scope, alarms and
  scripts module, translations rewritten, unused hook rules and screens removed

### de.robv.android.xposed compatibility layer

`library/libhook/src/main/java/de/robv/android/xposed/` is a small compatibility shim written for
this project so the hook code can be compiled and reasoned about without the closed-source Xposed
bridge. It mirrors the well-known public API surface only.
