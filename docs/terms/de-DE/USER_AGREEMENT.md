HyperAlarm ist ein nicht kommerzielles Systemerweiterungsmodul, das auf dem Xposed-Framework läuft und auf Mi Clock ausgerichtet ist. Es verwandelt Alarme von Mi Clock in Auslöser für Root-Skripte: Wenn ein Alarm ausgelöst wird, kann ein von Ihnen bereitgestelltes Skript ausgeführt werden, anstatt zu klingeln oder zusätzlich zum Klingeln. Bitte lesen und verstehen Sie diese HyperAlarm-Nutzungsvereinbarung, bevor Sie die Nutzung fortsetzen.

## 0. Version und Inkrafttreten

- 0.1. Version der Vereinbarung: 1.0
- 0.2. Datum des Inkrafttretens: 15. November 2026
- 0.3. Diese Vereinbarung wird mit jedem Build ausgeliefert; die aktuelle Fassung ist im Projekt-Repository verfügbar.

## 1. Allgemeines

- 1.1. HyperAlarm wird von der HyperAlarm-Projektgruppe (nachfolgend „wir“) entwickelt und gepflegt.
- 1.2. Das Modul setzt ein Gerät mit Root-Rechten und installiertem Xposed-/LSPosed-Framework voraus; fehlt eine der beiden Voraussetzungen, ist es nicht funktionsfähig.
- 1.3. Der einzige Geltungsbereich des Moduls ist Mi Clock (com.android.deskclock). Wir injizieren keine Hooks in andere Apps.
- 1.4. Mit der Nutzung des Moduls bestätigen Sie, dass Sie verstanden haben: Es bewirkt, dass bei einem Alarm zu der eingestellten Zeit ein von Ihnen bereitgestelltes Skript ausgeführt wird, anstatt nur zu klingeln.
- 1.5. Sie können die Nutzung jederzeit beenden, indem Sie das Modul deinstallieren oder es im Xposed-Manager deaktivieren.

## 2. Lizenzumfang

- 2.1. Wir gewähren Ihnen eine persönliche, nicht übertragbare, nicht unterlizenzierbare und nicht ausschließliche Lizenz zur Installation und Nutzung des Moduls.
- 2.2. Der Quellcode wird unter der GNU Affero General Public License v3.0 (AGPL-3.0) veröffentlicht. Sie dürfen ihn im Rahmen dieser Lizenz vervielfältigen, ändern und weiterverbreiten.
- 2.3. Kommerzielle Vorinstallation, Bündelung oder Vertrieb zusammen mit einem Gerät bedürfen unserer vorherigen schriftlichen Genehmigung.
- 2.4. Für von HyperCeiler übernommene Teile gelten weiterhin die ursprünglichen Lizenzbedingungen von HyperCeiler; siehe THIRD_PARTY_NOTICES.md im Repository.
- 2.5. Der Name und das Symbol von HyperAlarm sind nicht von der Quellcodelizenz umfasst.

## 3. Funktionen und Ihre Verantwortung

- 3.1. Skripte werden als root im Haupt-Mount-Namespace des Systems ausgeführt und können daher die Datenverzeichnisse anderer Apps sehen.
- 3.2. Sie stellen die Skripte selbst bereit, installieren und pflegen sie. Wir sind nicht für das Verhalten der Skripte oder jegliche daraus folgenden Konsequenzen verantwortlich.
- 3.3. Eine stille Aufgabe unterdrückt die Alarmoberfläche; wenn das Skript fehlt, die Ausführung abgelehnt wird oder der Alarm nicht neu geplant werden kann, behält das Modul den normalen Alarmton bei.
- 3.4. Dieses Modul ist kein zuverlässiges Erinnerungsmittel. Verwenden Sie es nicht für Fälle, in denen ein ausbleibender Alarm zu Personenschäden oder erheblichem Sachschaden führen kann, insbesondere nicht für medizinische Zwecke, Brandschutz oder Bereitschaftsdienste.
- 3.5. Die Änderung des Systemverhaltens birgt Risiken, einschließlich, aber nicht beschränkt auf ein Gerät, das nicht mehr startet, Datenverlust oder den Verlust der Garantie.

## 4. Umgang mit Informationen

- 4.1. Skriptprotokolle, App-Protokolle, Absturzaufzeichnungen, Einstellungen und das Sperrbildschirm-Passwort verbleiben ausschließlich auf Ihrem Gerät. Wir laden sie nicht hoch, erheben sie nicht und verkaufen sie nicht.
- 4.2. Das Modul greift an genau zwei Stellen auf das Netzwerk zu: Die Seite „Info“ ruft öffentliche GitHub-Schnittstellen ab, um das Profilbild des Autors und die Liste der Mitwirkenden zu laden; der Vertragstext wird von GitHub abgerufen. Beide Zugriffe offenbaren GitHub lediglich Ihre IP-Adresse und enthalten keine Gerätekennung.
- 4.3. Einzelheiten finden Sie in der gleichzeitig veröffentlichten HyperAlarm-Datenschutzerklärung.

## 5. Erforderliche Berechtigungen

- 5.1. Root: zum Ausführen von Skripten, Lesen und Schreiben des Skriptverzeichnisses, Spiegeln von Protokollen und Ändern globaler Einstellungen. Dies ist für die Kernfunktionen erforderlich.
- 5.2. Liste der installierten Apps: um Apps in der Auswahl des Geltungsbereichs anzuzeigen.
- 5.3. Benachrichtigungen: um laufende und abgeschlossene Skriptausführungen sowie die Gründe für Fehler anzuzeigen.
- 5.4. Netzwerk: nur in den unter 4.2 beschriebenen Fällen.
- 5.5. Sie können jede dieser Berechtigungen verweigern; die zugehörige Funktion ist dann nicht verfügbar, die übrigen Funktionen bleiben jedoch unberührt.

## 6. Zulässige Nutzung

- 6.1. Sie dürfen Urheberrechts- und Lizenzhinweise nicht aus dem Modul oder seinen Kopien entfernen.
- 6.2. Sie dürfen das Modul nicht dazu verwenden, die Netzwerksicherheit zu beeinträchtigen, die Privatsphäre anderer zu verletzen oder gegen Gesetze zu verstoßen.
- 6.3. Sie dürfen das Modul ohne Zustimmung des Geräteeigentümers nicht auf einem fremden Gerät installieren oder nutzen.
- 6.4. Sie dürfen das Modul nicht verwenden, um Zahlungen zu umgehen, Software zu knacken oder Zugriffskontrollen zu umgehen.

## 7. Haftungsausschluss und Haftungsbeschränkung

- 7.1. Das Modul wird in seinem aktuellen Zustand bereitgestellt, ohne jegliche ausdrückliche oder stillschweigende Gewährleistung, einschließlich, aber nicht beschränkt auf Marktgängigkeit, Eignung für einen bestimmten Zweck und Nichtverletzung von Rechten Dritter.
- 7.2. Wir haben die Funktionsfähigkeit nur mit Mi Clock 18.28.0 geprüft. Nach einem Update der Uhr oder des Systems können Teile des Moduls ausfallen oder sich anders verhalten.
- 7.3. Probleme, die durch Ihr Handeln, Ihre Skripte, Ihre Root-Umgebung, das Xposed-Framework oder ein Systemupdate entstehen, sind von Ihnen zu tragen.
- 7.4. Im gesetzlich zulässigen Umfang haften wir nicht für mittelbare, beiläufige, besondere oder Strafschäden, die aus der Nutzung oder der Unmöglichkeit der Nutzung dieses Moduls entstehen.
- 7.5. Sollte eine Bestimmung für unwirksam erklärt werden, bleiben die übrigen Bestimmungen wirksam.

## 8. Änderungen und Beendigung

- 8.1. Wir können diese Vereinbarung mit einer Veröffentlichung überarbeiten; die überarbeitete Fassung wird mit dem neuen Build ausgeliefert und im Repository veröffentlicht.
- 8.2. Wenn Sie das Modul nach Inkrafttreten einer Änderung weiter nutzen, gilt dies als Zustimmung. Wenn Sie nicht einverstanden sind, beenden Sie die Nutzung und deinstallieren Sie das Modul.
- 8.3. Sie können diese Vereinbarung jederzeit beenden, indem Sie das Modul deinstallieren.

## 9. Anwendbares Recht und Streitbeilegung

- 9.1. Diese Vereinbarung unterliegt dem Recht der Volksrepublik China.
- 9.2. Streitigkeiten aus dieser Vereinbarung sind zunächst gütlich zu verhandeln; kommt keine Einigung zustande, sind sie dem zuständigen Volksgericht an unserem Hauptgeschäftssitz zur Entscheidung vorzulegen.

## 10. Kontakt

- 10.1. Bitte melden Sie Fragen zu dieser Vereinbarung über einen Eintrag im Projekt-Repository.
- 10.2. Wenn Sie der Ansicht sind, dass eine Bestimmung nicht dem tatsächlichen Verhalten entspricht, teilen Sie uns dies bitte mit, damit wir sie korrigieren, anstatt sie als verbindlich zu behandeln.
