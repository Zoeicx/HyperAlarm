<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | [Italiano](README_it-IT.md) | **Polski** | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm to moduł Xposed, którego **jedynym zakresem jest Zegar Xiaomi**. Zamienia budziki zegara w wyzwalacze skryptów powłoki.

## Aplikacja docelowa

Sprawdzone tylko na poniższych wersjach. W innych przypadkach testuj sam, przyślij APK do dostosowania albo zaktualizuj/przywróć te wersje:

- Zegar Xiaomi `com.android.deskclock`
- Wersja `18.28.0`, kod wersji `1306002800`

## Instalacja

1. Przygotuj root (KernelSU / Magisk) oraz LSPosed (Xposed API 101 lub nowszy).
2. Zainstaluj moduł i włącz HyperAlarm w LSPosed.
3. W zakresie zaznacz tylko `com.android.deskclock`. Żadnych innych pakietów.
4. Wymuś zatrzymanie Zegara i pozwól procesowi wystartować od nowa, inaczej moduł nic nie zrobi.

## Użycie

- Nowe zadanie: `+` po prawej stronie górnego paska akcji.
- Etykieta budzika to nazwa skryptu i musi kończyć się na `.sh`:
  - `alert.sh`: uruchamia się cicho o wyznaczonej godzinie, bez dzwonka i bez ekranu budzika
  - `@alert.sh`: uruchamia skrypt i zostawia ekran budzika oraz dzwonek
- W liście `@` jest ukrywane; przełącznik „Dzwonek” przenosi zadanie między trybami.
- „Uruchom teraz” pozwala przetestować skrypt bez czekania na godzinę.
- Zadania zmianowe odpalają się na zmianę, wyliczaną z daty rozpoczęcia i cyklu.

## Zasady skryptów

Skrypty leżą w `/data/adb/alarm_shell/scripts/`, a nazwa w etykiecie musi zgadzać się z nazwą pliku.

```text
/data/adb/alarm_shell/scripts/      # katalog skryptów
/data/adb/alarm_shell/password      # hasło ekranu blokady
/data/adb/alarm_shell/unlock_phone  # domyślny skrypt odblokowania
```

- Konieczne są zakończenia linii LF. Plik z CRLF z Windows po prostu zawiedzie.
- Skrypty działają jako root w głównej przestrzeni nazw systemu, więc widzą katalogi danych innych aplikacji.
- Każde uruchomienie zostawia log: widać go w „Logach skryptów” w aplikacji, a kopia trafia do
  `/sdcard/Android/data/com.hyperalarm/files/script_logs/<nazwa skryptu>/`.
- Przed cichym zadaniem moduł odblokuje ekran Twoim hasłem, jeśli je ustawiono.

## Ustawienia

- **Zegar pokazuje tylko budziki**: w liście Zegara zostają prawdziwe budziki, zadania-skrypty znikają.
- **Ukryj zadania z dzwonkiem**: ukrywa także zadania z włączonym dzwonkiem.
- Poziom logów / Logi aplikacji / Logi skryptów: przy problemach najpierw podnieś poziom.
- **Hasło ekranu blokady**: służy do odblokowania przed cichymi zadaniami; puste pole nic nie zmienia.
- Przewodnik restartu, kopiowanie katalogu skryptów, kopia zapasowa i przywracanie konfiguracji.

## Znane ograniczenia

- Sprawdzone tylko na Android 16 / HyperOS 3 / Zegarze 18.28.0; inne zestawy na własne ryzyko.
- Tryb zmianowy opiera się na wewnętrznych mechanizmach Zegara, po aktualizacji zegara trzeba go sprawdzić ponownie.
- Bez roota moduł nie działa; strona ustawień o tym informuje.
- Logi skryptów są kopiowane do pamięci zewnętrznej i czytelne dla roota oraz adb shell. Nie wypisuj sekretów w logach.

## Częste pytania

- **Zadanie dodane, a zegar milczy**: sprawdź, czy moduł jest włączony w LSPosed, czy zakres to tylko Zegar i czy proces zegara został zrestartowany.
- **Zadania nie ma na liście zegara**: działa „Zegar pokazuje tylko budziki”, wystarczy to wyłączyć.
- **O wyznaczonej godzinie nic się nie stało**: zajrzyj w logi skryptów w aplikacji; brakujący skrypt lub literówka w etykiecie są tam podane.

## Licencja

- Kod własny tego repozytorium: **AGPL-3.0** (zob. `../LICENSE`)
- Komponenty firm trzecich i ich licencje: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Podziękowania

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): punkt wyjścia projektu; większość interfejsu MIUIX i ustawień pochodzi właśnie stamtąd
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): komponenty UI w stylu Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): API do roota i Xposed
- Wszyscy, którzy zgłaszali issue i przysyłali logi
