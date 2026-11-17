<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | **English** | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm is an Xposed module whose **only scope is Mi Clock**. It turns Mi Clock alarms into triggers for shell scripts.

## Host app

Verified on the versions below only. For anything else, test it yourself, send an APK so it can be adapted, or update/downgrade to these:

- Mi Clock `com.android.deskclock`
- Version `18.28.0`, version code `1306002800`

## Install

1. Have root (KernelSU / Magisk) and LSPosed (Xposed API 101 or newer).
2. Install this module, then enable HyperAlarm in LSPosed.
3. Tick only `com.android.deskclock` as the scope. Do not add other packages.
4. Force stop Mi Clock and let its process start again, or the module will not be active.

## Usage

- New task: the `+` at the switch end of the top action bar.
- The alarm label is the script name and must end with `.sh`:
  - `alert.sh`: runs silently at the alarm time, no ringtone, no alarm screen
  - `@alert.sh`: runs the script and keeps the alarm screen and ringtone
- The `@` is hidden in the list; flip the "Ringtone" switch to move between the two modes.
- "Run now" lets you test a script without waiting for the alarm.
- Shift tasks fire per shift, computed from the start date and the cycle.

## Script rules

Scripts live in `/data/adb/alarm_shell/scripts/`, and the name in the label must match the file name.

```text
/data/adb/alarm_shell/scripts/      # script directory
/data/adb/alarm_shell/password      # lock screen password
/data/adb/alarm_shell/unlock_phone  # default unlock script
```

- Line endings must be LF. A Windows CRLF file simply fails.
- Scripts run as root inside the main system namespace, so they can see other apps' data directories.
- Every run leaves a log: the in-app "Script logs" page shows it, and it is mirrored to
  `/sdcard/Android/data/com.hyperalarm/files/script_logs/<script name>/`.
- Before a silent task runs, the module unlocks the screen with your lock screen password, if you set one.

## Settings

- **Clock shows alarms only**: the Mi Clock list keeps real alarms and hides script tasks.
- **Hide ringing tasks**: hides ringing tasks as well.
- Log levels / App logs / Script logs: raise the level first when something goes wrong.
- **Lock screen password**: used to unlock for silent tasks; leave it empty to keep the current one.
- Restart tutorial, copy the script directory, back up and restore the module configuration.

## Known limits

- Verified on Android 16 / HyperOS 3 / Mi Clock 18.28.0 only; other combinations are on you.
- Shift mode relies on Mi Clock internals and needs re-verification after a clock update.
- Without root the module cannot work; the settings page says so.
- Script logs are mirrored to external storage, readable by root and by adb shell. Do not echo secrets into a script.

## FAQ

- **A task does nothing at its time**: check that the module is enabled in LSPosed, that the scope is only Mi Clock, and that the clock process was restarted.
- **The task is missing from the clock list**: "Clock shows alarms only" is doing its job; turn it off.
- **Nothing ran at the alarm time**: read the in-app script log; a missing script or a misspelled label is reported there.

## License

- Code that belongs to this repository: **AGPL-3.0** (see `../LICENSE`)
- Third-party components and their licenses: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Credits

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): where this project started; most of the MIUIX UI and settings plumbing comes from it
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): Xiaomi-style UI components
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): root and Xposed APIs
- Everyone who filed issues and sent logs
