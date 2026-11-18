<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | **Deutsch** | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm ist ein Xposed-Modul, dessen **einziger Scope die Xiaomi-Uhr ist**. Es macht aus Weckern Auslöser für Shell-Skripte.

## Ziel-App

Nur die folgenden Versionen sind geprüft. Bei allem anderen selbst testen, ein APK zur Anpassung schicken oder auf diese Version aktualisieren bzw. zurückgehen:

- Xiaomi-Uhr `com.android.deskclock`
- Version `18.28.0`, Versionscode `1306002800`

## Installation

1. Root (KernelSU / Magisk) und LSPosed (Xposed API 101 oder neuer) bereithalten.
2. Das Modul installieren und HyperAlarm in LSPosed aktivieren.
3. Als Scope nur `com.android.deskclock` anhaken, keine weiteren Pakete.
4. Die Xiaomi-Uhr erzwungen beenden und den Prozess neu starten, sonst wird das Modul nicht aktiv.

## Bedienung

- Neuer Wecker: das `+` rechts in der oberen Aktionsleiste.
- Die Bezeichnung des Weckers ist der Skriptname und muss auf `.sh` enden:
  - `alert.sh`: läuft still zur Weckzeit, kein Klingeln, kein Wecker-Bildschirm
  - `@alert.sh`: führt das Skript aus und lässt Wecker-Bildschirm und Klingelton stehen
- Das `@` wird in der Liste ausgeblendet; der Schalter „Klingelton“ wechselt zwischen beiden Modi.
- „Jetzt ausführen“ testet ein Skript, ohne auf die Weckzeit zu warten.
- Schichtwecker lösen pro Schicht aus, berechnet aus Startdatum und Zyklus.

## Regeln für Skripte

Skripte liegen in `/data/adb/alarm_shell/scripts/`, der Name in der Bezeichnung muss zum Dateinamen passen.

```text
/data/adb/alarm_shell/scripts/      # Skriptverzeichnis
/data/adb/alarm_shell/password      # Sperrbildschirm-Passwort
/data/adb/alarm_shell/unlock_phone  # Standard-Entsperrskript
```

- Zeilenenden müssen LF sein. Eine Datei mit Windows-CRLF schlägt einfach fehl.
- Skripte laufen als root im Haupt-Namespace des Systems und sehen daher die Datenverzeichnisse anderer Apps.
- Jeder Lauf hinterlässt ein Log: sichtbar unter „Skript-Logs“ in der App, zusätzlich gespiegelt nach
  `/sdcard/Android/data/io.github.zoeicx.hyperalarm/files/script_logs/<Skriptname>/`.
- Vor einem stillen Wecker entsperrt das Modul den Bildschirm mit dem hinterlegten Passwort, falls eines gesetzt ist.

## Einstellungen

- **Uhr zeigt nur Wecker**: in der Liste der Xiaomi-Uhr bleiben echte Wecker, Skript-Aufgaben verschwinden.
- **Klingelnde Aufgaben ausblenden**: blendet auch Aufgaben mit Klingelton aus.
- Log-Level / App-Logs / Skript-Logs: bei Problemen zuerst das Level hochsetzen.
- **Sperrbildschirm-Passwort**: entsperrt vor stillen Aufgaben; leer lassen heißt unverändert.
- Neustart-Anleitung, Skriptverzeichnis kopieren, Konfiguration sichern und wiederherstellen.

## Bekannte Grenzen

- Nur auf Android 16 / HyperOS 3 / Xiaomi-Uhr 18.28.0 geprüft; andere Kombinationen auf eigene Gefahr.
- Der Schichtmodus hängt an Interna der Uhr und muss nach einem Uhr-Update erneut geprüft werden.
- Ohne root arbeitet das Modul nicht; die Einstellungsseite weist darauf hin.
- Skript-Logs werden in den externen Speicher gespiegelt und sind für root und adb shell lesbar. Keine Geheimnisse ins Log schreiben.

## Häufige Fragen

- **Aufgabe angelegt, die Uhr reagiert nicht**: prüfen, ob das Modul in LSPosed aktiv ist, der Scope nur die Xiaomi-Uhr umfasst und der Uhr-Prozess neu gestartet wurde.
- **Aufgabe fehlt in der Uhr-Liste**: „Uhr zeigt nur Wecker“ ist aktiv, einfach abschalten.
- **Zur Weckzeit passiert nichts**: die Skript-Logs in der App ansehen; ein fehlendes Skript oder ein Tippfehler in der Bezeichnung wird dort genannt.

## Lizenz

- Eigener Code dieses Repositories: **AGPL-3.0** (siehe `../LICENSE`)
- Fremdkomponenten und ihre Lizenzen: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Danksagung

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): der Ausgangspunkt des Projekts; ein Großteil der MIUIX-Oberfläche und der Einstellungen stammt von dort
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): UI-Komponenten im Xiaomi-Stil
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): Root- und Xposed-APIs
- Alle, die Issues erstellt und Logs geschickt haben
