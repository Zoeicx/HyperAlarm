HyperAlarm to niekomercyjny moduł rozszerzający system, działający w oparciu o framework Xposed i przeznaczony dla Mi Clock. Zamienia alarmy Mi Clock w wyzwalacze skryptów root: po nadejściu godziny alarmu może zostać uruchomiony wskazany przez użytkownika skrypt zamiast dzwonka lub dodatkowo do niego. Przed dalszym korzystaniem należy przeczytać i zrozumieć niniejszą Umowę użytkownika HyperAlarm.

## 0. Wersja i data wejścia w życie

- 0.1. Wersja umowy: 1.0
- 0.2. Data wejścia w życie: 15 listopada 2026 r.
- 0.3. Niniejsza umowa jest dołączana do każdej kompilacji; najnowsza wersja jest dostępna w repozytorium projektu.

## 1. Postanowienia ogólne

- 1.1. HyperAlarm jest rozwijany i utrzymywany przez „Projekt HyperAlarm” (dalej „my”).
- 1.2. Moduł wymaga urządzenia z uzyskanymi uprawnieniami root oraz zainstalowanym frameworkiem Xposed / LSPosed. Brak któregokolwiek z tych warunków uniemożliwia korzystanie z funkcji.
- 1.3. Jedynym zakresem działania modułu jest Mi Clock (com.android.deskclock). Nie wstrzykujemy hooków do innych aplikacji.
- 1.4. Korzystając z modułu, użytkownik przyjmuje do wiadomości, że powoduje on uruchomienie dostarczonego przez użytkownika skryptu o ustawionej godzinie, a nie wyłącznie dzwonienie alarmu.
- 1.5. Użytkownik może w każdej chwili zaprzestać korzystania: odinstalować moduł lub dezaktywować go w menedżerze Xposed.

## 2. Licencja

- 2.1. Udzielamy użytkownikowi osobistej, niezbywalnej, niepodlegającej sublicencjonowaniu i niewyłącznej licencji na instalowanie i korzystanie z modułu.
- 2.2. Kod źródłowy jest udostępniany na warunkach GNU Affero General Public License v3.0 (AGPL-3.0). Użytkownik może go kopiować, modyfikować i redystrybuować w zakresie dozwolonym przez tę licencję.
- 2.3. Komercyjne preinstalowanie, sprzedaż w pakiecie lub dystrybucja wraz z urządzeniem wymagają naszej uprzedniej pisemnej zgody.
- 2.4. Części odziedziczone po HyperCeiler podlegają nadal ich pierwotnym warunkom licencyjnym; szczegóły w THIRD_PARTY_NOTICES.md w repozytorium.
- 2.5. Nazwa i ikona HyperAlarm nie są objęte licencją na kod źródłowy.

## 3. Możliwości i odpowiedzialność użytkownika

- 3.1. Skrypty są wykonywane jako root w głównej przestrzeni nazw montowania systemu, dlatego mają dostęp do katalogów danych innych aplikacji.
- 3.2. Użytkownik samodzielnie dostarcza, instaluje i utrzymuje skrypty. Nie ponosimy odpowiedzialności za działanie skryptów ani za jakiekolwiek jego skutki.
- 3.3. Zadanie ciche tłumi ekran alarmu; jeśli skrypt jest nieobecny, odrzucony lub nie można go ponownie zaplanować, moduł zachowuje normalny dźwięk alarmu.
- 3.4. Moduł nie jest niezawodnym sposobem przypominania. Nie należy go używać w sytuacjach, w których pominięty alarm może spowodować uszczerbek na zdrowiu lub znaczną szkodę majątkową, w tym w zadaniach medycznych, bezpieczeństwie pożarowym lub dyżurach.
- 3.5. Modyfikowanie działania systemu wiąże się z ryzykiem, w tym między innymi brakiem możliwości uruchomienia urządzenia, utratą danych i utratą gwarancji.

## 4. Przetwarzanie informacji

- 4.1. Dzienniki skryptów, dzienniki aplikacji, zapisy awarii, ustawienia i hasło blokady ekranu są przechowywane wyłącznie na urządzeniu użytkownika. Nie przesyłamy, nie zbieramy ani nie sprzedajemy ich.
- 4.2. Dostęp do sieci występuje dokładnie w dwóch miejscach: strona „O aplikacji” pobiera awatar autora i listę współtwórców z publicznych interfejsów GitHub; treść umów jest pobierana z GitHub. W obu przypadkach GitHub widzi wyłącznie adres IP użytkownika; identyfikatory urządzenia nie są przekazywane.
- 4.3. Szczegóły opisano w Polityce prywatności HyperAlarm opublikowanej równocześnie z niniejszą umową.

## 5. Uprawnienia, o które prosimy

- 5.1. Root: do wykonywania skryptów, odczytu i zapisu katalogu skryptów, kopiowania dzienników i zmiany ustawień globalnych. Jest niezbędny do działania podstawowych funkcji.
- 5.2. Lista zainstalowanych aplikacji: do wyświetlania aplikacji w selektorze zakresu.
- 5.3. Powiadomienia: do prezentowania uruchomionych i zakończonych skryptów oraz przyczyn niepowodzenia.
- 5.4. Sieć: wyłącznie w przypadkach opisanych w pkt 4.2.
- 5.5. Użytkownik może odmówić każdego z uprawnień; odpowiednia funkcja przestanie działać, a pozostałe funkcje będą działać nadal.

## 6. Zasady korzystania

- 6.1. Nie wolno usuwać informacji o prawach autorskich i licencji z modułu ani jego kopii.
- 6.2. Nie wolno wykorzystywać modułu do szkodzenia bezpieczeństwu sieci, naruszania prywatności innych osób ani popełniania innych czynów niezgodnych z prawem.
- 6.3. Nie wolno instalować ani używać modułu na urządzeniu innej osoby bez zgody właściciela urządzenia.
- 6.4. Nie wolno wykorzystywać modułu do omijania płatności, łamania zabezpieczeń oprogramowania ani obchodzenia kontroli dostępu.

## 7. Wyłączenie gwarancji i ograniczenie odpowiedzialności

- 7.1. Moduł jest udostępniany „tak jak jest”, bez jakichkolwiek gwarancji wyraźnych lub dorozumianych, w tym między innymi gwarancji przydatności handlowej, przydatności do określonego celu i nienaruszania praw.
- 7.2. Zweryfikowaliśmy działanie wyłącznie na Mi Clock 18.28.0. Po aktualizacji zegara lub systemu część funkcji może przestać działać lub zmienić zachowanie.
- 7.3. Problemy spowodowane działaniami użytkownika, treścią skryptów, środowiskiem root, frameworkiem Xposed lub aktualizacją systemu obciążają użytkownika.
- 7.4. W maksymalnym zakresie dozwolonym przez prawo nie ponosimy odpowiedzialności za szkody pośrednie, przypadkowe, szczególne lub karne wynikające z korzystania z modułu albo niemożności korzystania z niego.
- 7.5. Jeżeli którekolwiek postanowienie zostanie uznane za nieważne, pozostałe postanowienia pozostają w mocy.

## 8. Zmiany umowy i rozwiązanie

- 8.1. Możemy zmieniać niniejszą umowę wraz z wydaniem nowej wersji; zmieniony tekst jest dołączany do nowej kompilacji i publikowany w repozytorium.
- 8.2. Dalsze korzystanie po wejściu zmian w życie oznacza ich akceptację. W razie braku akceptacji należy zaprzestać korzystania i odinstalować moduł.
- 8.3. Użytkownik może w każdej chwili rozwiązać niniejszą umowę, odinstalowując moduł.

## 9. Prawo właściwe i rozstrzyganie sporów

- 9.1. Niniejsza umowa podlega prawu Chińskiej Republiki Ludowej.
- 9.2. Spory wynikające z niniejszej umowy strony rozstrzygają najpierw w drodze negocjacji w dobrej wierze; jeśli nie dojdzie do porozumienia, spór przekazuje się właściwemu sądowi miejsca naszej głównej działalności.

## 10. Kontakt

- 10.1. W sprawach dotyczących umowy należy zgłaszać issue w repozytorium projektu.
- 10.2. Jeżeli użytkownik uważa, że któreś postanowienie nie odpowiada faktycznemu działaniu modułu, powinien najpierw powiadomić nas w celu poprawienia, a nie uznawać je za wiążące.
