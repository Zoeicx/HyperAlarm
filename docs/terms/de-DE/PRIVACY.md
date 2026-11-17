Diese Datenschutzerklärung erläutert, wie HyperAlarm (nachfolgend „das Modul“) mit Ihren Informationen umgeht. Das Ergebnis vorweg: Das Modul erhebt keine personenbezogenen Daten und lädt keine hoch; Ihre Protokolle und Einstellungen bleiben auf diesem Gerät. Die Einzelheiten folgen unten.

## 0. Version und Inkrafttreten

- 0.1. Version der Erklärung: 1.0
- 0.2. Datum des Inkrafttretens: 15. November 2026
- 0.3. Diese Erklärung wird mit jedem Build ausgeliefert.

## 1. Auf diesem Gerät gespeicherte Informationen

- 1.1. Skriptprotokolle: Zeitpunkt, Exit-Code, Standardausgabe und Standardfehler jeder Skriptausführung. Sie werden in der Datenbank des Moduls und in seinem externen Speicherverzeichnis gespeichert.
- 1.2. App-Protokolle: die Laufzeitprotokolle des Moduls selbst, die zur Fehlerdiagnose dienen.
- 1.3. Absturzaufzeichnungen: kurze Ausnahmezusammenfassungen für den Sicherheitsmodus, die im privaten Verzeichnis des Moduls gespeichert werden.
- 1.4. Einstellungen: Sprache, Protokollstufe, Listenfilter und Ähnliches, gespeichert in der Konfigurationsdatei des Moduls.
- 1.5. Sperrbildschirm-Passwort: wird nur in eine Datei geschrieben, wenn Sie es in den Einstellungen selbst eingeben; die Datei hat den Modus 600 und ist nur von root und diesem Modul lesbar; sie dient zum Entsperren des Bildschirms vor der Ausführung einer stillen Aufgabe.
- 1.6. Nichts davon verlässt Ihr Gerät, es sei denn, Sie exportieren, teilen oder melden es selbst in einem Eintrag im Projekt-Repository.

## 2. Informationen, die wir nicht erheben

- 2.1. Keine Gerätekennungen (IMEI, Android ID, Seriennummer usw.).
- 2.2. Keine Standortdaten.
- 2.3. Keine Kontakte, Nachrichten oder Anrufprotokolle.
- 2.4. Keine Kopien Ihrer Skripte.
- 2.5. Das Modul enthält keine Analyse-, Absturzberichts- oder Werbe-SDKs.

## 3. Die beiden Netzwerkzugriffe

- 3.1. Die Seite „Info“ ruft öffentliche GitHub-Schnittstellen ab, um das Profilbild des Autors und die Liste der Mitwirkenden zu lesen. Die Anfragen gehen nur an api.github.com und avatars.githubusercontent.com, enthalten den von GitHub geforderten Standard-User-Agent und keine Geräte- oder Benutzerkennung. GitHub sieht dadurch Ihre IP-Adresse.
- 3.2. Der Einrichtungsassistent beim ersten Start und die Einstellungsseite rufen den Text der Nutzungsvereinbarung und dieser Erklärung von GitHub ab, damit die Texte unabhängig von Versionsupdates aktualisiert werden können. Diese Anfragen enthalten ebenfalls nur Standard-Header. Wenn der Abruf fehlschlägt, wird die im Installationspaket enthaltene Kopie angezeigt oder ein Fehlerhinweis ausgegeben.
- 3.3. Darüber hinaus führt das Modul keine Netzwerkanfragen aus.

## 4. Weitergabe an Dritte

- 4.1. Wir geben keine Informationen an Dritte weiter, weil wir keine erheben.
- 4.2. Die oben genannten GitHub-Anfragen sind Ihre unmittelbare Interaktion mit GitHub und unterliegen der Datenschutzerklärung von GitHub.
- 4.3. Wenn Sie Protokolle selbst exportieren und teilen, wählen Sie den Empfänger; darin liegt keine Weitergabe durch uns.

## 5. Aufbewahrung und Löschung

- 5.1. Protokolle werden automatisch auf eine feste Obergrenze gekürzt; darüber hinausgehende älteste Einträge werden gelöscht.
- 5.2. Sie können die Protokolle in der App löschen oder die Daten des Moduls direkt entfernen.
- 5.3. Die Deinstallation des Moduls löscht alle Daten in seinen privaten Verzeichnissen.
- 5.4. Protokollkopien im externen Speicher müssen Sie vor der Deinstallation manuell löschen.

## 6. Ihre Rechte

- 6.1. Sie können jederzeit alle vom Modul gespeicherten Informationen einsehen, exportieren und löschen.
- 6.2. Sie können jede Berechtigung verweigern; die zugehörige Funktion ist dann nicht verfügbar.
- 6.3. Sie können das Sperrbildschirm-Passwort in den Einstellungen löschen; der Dateiinhalt wird geleert.

## 7. Minderjährige

- 7.1. Das Modul richtet sich an Nutzer mit gewissen Systemkenntnissen. Minderjährige sollten es unter Aufsicht einer erziehungsberechtigten Person verwenden.
- 7.2. Da es Root-Rechte und die Ausführung von Skripten betrifft, empfehlen wir Minderjährigen nicht, es eigenständig zu bedienen.

## 8. Änderungen dieser Erklärung

- 8.1. Diese Erklärung kann mit einer Veröffentlichung überarbeitet werden; die überarbeitete Fassung wird mit dem neuen Build ausgeliefert und im Repository veröffentlicht.
- 8.2. Wenn eine Änderung eine neue Art der Informationsverarbeitung einführt, weisen wir in den Versionshinweisen ausdrücklich darauf hin.

## 9. Kontakt

- 9.1. Bei Fragen zum Datenschutz melden Sie sich bitte über einen Eintrag im Projekt-Repository.
- 9.2. Der Quellcode des Moduls ist öffentlich und prüfbar; Sie können die obigen Aussagen selbst überprüfen.
