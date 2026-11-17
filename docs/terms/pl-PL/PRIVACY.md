Niniejsza polityka wyjaśnia, w jaki sposób HyperAlarm („moduł”) przetwarza informacje użytkownika. Krótko: moduł niczego nie zbiera i niczego nie przesyła. Dzienniki i ustawienia pozostają na tym urządzeniu. Szczegóły poniżej.

## 0. Wersja i data wejścia w życie

- 0.1. Wersja polityki: 1.0
- 0.2. Data wejścia w życie: 15 listopada 2026 r.
- 0.3. Niniejsza polityka jest dołączana do każdej kompilacji.

## 1. Co jest przechowywane na urządzeniu

- 1.1. Dzienniki skryptów: czas rozpoczęcia, kod zakończenia, standardowe wyjście i standardowe błędy każdego uruchomienia skryptu. Są przechowywane w bazie danych modułu oraz w katalogu w pamięci zewnętrznej.
- 1.2. Dzienniki aplikacji: własny dziennik działania modułu, używany do diagnostyki.
- 1.3. Zapisy awarii: krótkie opisy wyjątków używane przez tryb awaryjny, przechowywane w prywatnym katalogu modułu.
- 1.4. Ustawienia: język, poziom rejestrowania, filtry list i tym podobne, przechowywane w pliku preferencji modułu.
- 1.5. Hasło blokady ekranu: jest zapisywane w pliku tylko wtedy, gdy użytkownik sam je wprowadzi w ustawieniach; uprawnienia 600, dostępne tylko dla root i tego modułu; służy do odblokowania ekranu przed uruchomieniem zadania cichego.
- 1.6. Żadna z powyższych informacji nie opuszcza urządzenia, chyba że użytkownik sam je wyeksportuje, udostępni lub opublikuje w issue.

## 2. Czego nie zbieramy

- 2.1. Identyfikatorów urządzenia (IMEI, Android ID, numer seryjny itp.).
- 2.2. Danych o lokalizacji.
- 2.3. Kontaktów, wiadomości ani historii połączeń.
- 2.4. Treści skryptów użytkownika.
- 2.5. Moduł nie zawiera analityki, raportowania awarii ani reklamowych SDK.

## 3. Dwa żądania sieciowe

- 3.1. Strona „O aplikacji” wywołuje publiczne interfejsy GitHub, aby odczytać awatar autora i listę współtwórców. Żądania są wysyłane wyłącznie do api.github.com i avatars.githubusercontent.com, zawierają standardowy User-Agent wymagany przez GitHub i nie zawierają identyfikatorów urządzenia ani użytkownika. GitHub widzi zatem adres IP użytkownika.
- 3.2. Kreator konfiguracji i strona ustawień pobierają z GitHub treść umowy użytkownika i niniejszej polityki, aby dokumenty można było aktualizować niezależnie od wersji. Te żądania również zawierają wyłącznie standardowe nagłówki. Jeśli pobieranie się nie powiedzie, wyświetlana jest kopia z pakietu instalacyjnego albo komunikat o błędzie.
- 3.3. Poza tym moduł nie wykonuje żadnych żądań sieciowych.

## 4. Udostępnianie stronom trzecim

- 4.1. Niczego nie udostępniamy, ponieważ niczego nie zbieramy.
- 4.2. Wspomniane żądania do GitHub są bezpośrednią interakcją użytkownika z GitHub i podlegają własnej polityce prywatności GitHub.
- 4.3. Jeśli użytkownik sam wyeksportuje i udostępni dzienniki, odbiorcę wybiera użytkownik; nie stanowi to ujawnienia z naszej strony.

## 5. Przechowywanie i usuwanie

- 5.1. Dzienniki są automatycznie przycinane do stałego limitu; po jego przekroczeniu usuwane są najstarsze wpisy.
- 5.2. Dzienniki można wyczyścić w aplikacji albo bezpośrednio usunąć dane modułu.
- 5.3. Odinstalowanie modułu usuwa również wszystkie dane z jego prywatnych katalogów.
- 5.4. Kopie dzienników odwzorowane w pamięci zewnętrznej należy usunąć ręcznie przed odinstalowaniem modułu.

## 6. Prawa użytkownika

- 6.1. Użytkownik może w dowolnym momencie przeglądać, eksportować i usuwać wszystkie informacje przechowywane przez moduł.
- 6.2. Użytkownik może odmówić dowolnego uprawnienia; odpowiednia funkcja przestanie działać.
- 6.3. Hasło blokady ekranu można wyczyścić w ustawieniach; zawartość pliku zostanie opróżniona.

## 7. Osoby niepełnoletnie

- 7.1. Moduł jest przeznaczony dla użytkowników posiadających pewną wiedzę o systemie. Osoby niepełnoletnie powinny korzystać z niego pod opieką opiekuna.
- 7.2. Ponieważ wiąże się z root i wykonywaniem skryptów, nie zalecamy samodzielnego korzystania przez osoby niepełnoletnie.

## 8. Zmiany polityki

- 8.1. Polityka może być zmieniana wraz z wydaniem nowej wersji; zmieniony tekst jest dołączany do nowej kompilacji i publikowany w repozytorium.
- 8.2. Jeśli zmiana wprowadza nowe sposoby przetwarzania informacji, wyraźnie informujemy o tym w informacjach o wydaniu.

## 9. Kontakt

- 9.1. W sprawach dotyczących prywatności należy zgłaszać issue w repozytorium projektu.
- 9.2. Kod źródłowy modułu jest publiczny i podlega audytowi, więc użytkownik może samodzielnie zweryfikować powyższe stwierdzenia.
