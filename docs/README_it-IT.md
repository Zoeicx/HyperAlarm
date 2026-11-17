<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | [Español](README_es-ES.md) | **Italiano** | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm è un modulo Xposed il cui **unico ambito è l'Orologio di Xiaomi**. Trasforma le sveglie dell'orologio in trigger per script di shell.

## App ospite

Solo le versioni qui sotto sono verificate. Per le altre provalo tu, manda un APK per l'adattamento oppure aggiorna/retrocedi a queste:

- Orologio Xiaomi `com.android.deskclock`
- Versione `18.28.0`, codice versione `1306002800`

## Installazione

1. Servono root (KernelSU / Magisk) e LSPosed (Xposed API 101 o superiore).
2. Installa il modulo e attiva HyperAlarm in LSPosed.
3. Nell'ambito spunta solo `com.android.deskclock`. Nessun altro pacchetto.
4. Forza l'arresto dell'Orologio e lascia ripartire il processo, altrimenti il modulo resta fermo.

## Uso

- Nuova attività: il `+` a destra nella barra delle azioni in alto.
- L'etichetta della sveglia è il nome dello script e deve finire con `.sh`:
  - `alert.sh`: parte in silenzio all'ora, senza suoneria né schermata della sveglia
  - `@alert.sh`: esegue lo script e mantiene schermata e suoneria
- La `@` è nascosta nell'elenco; con l'interruttore «Suoneria» passi da un modo all'altro.
- «Esegui ora» prova uno script senza aspettare l'ora.
- Le attività a turni scattano per turno, calcolato da data di inizio e ciclo.

## Regole per gli script

Gli script stanno in `/data/adb/alarm_shell/scripts/` e il nome nell'etichetta deve combaciare con quello del file.

```text
/data/adb/alarm_shell/scripts/      # cartella degli script
/data/adb/alarm_shell/password      # password della schermata di blocco
/data/adb/alarm_shell/unlock_phone  # script di sblocco predefinito
```

- Le righe devono finire in LF. Un file con CRLF di Windows semplicemente fallisce.
- Gli script girano come root nel namespace principale del sistema, quindi vedono le cartelle dati delle altre app.
- Ogni esecuzione lascia un log: si legge in «Log degli script» nell'app ed è copiato anche in
  `/sdcard/Android/data/com.hyperalarm/files/script_logs/<nome script>/`.
- Prima di un'attività silenziosa il modulo sblocca lo schermo con la password, se l'hai impostata.

## Impostazioni

- **L'orologio mostra solo le sveglie**: nell'elenco dell'Orologio restano le sveglie vere, le attività script spariscono.
- **Nascondi le attività con suoneria**: nasconde anche quelle con la suoneria attiva.
- Livello dei log / Log dell'app / Log degli script: alza il livello prima di indagare un problema.
- **Password della schermata di blocco**: serve per sbloccare prima delle attività silenziose; vuoto significa non modificarla.
- Guida al riavvio, copia della cartella degli script, backup e ripristino della configurazione.

## Limiti noti

- Verificato solo su Android 16 / HyperOS 3 / Orologio 18.28.0; altre combinazioni sono a tuo rischio.
- La modalità a turni dipende dalle viscere dell'Orologio e va riverificata dopo un aggiornamento dell'orologio.
- Senza root il modulo non funziona; la pagina delle impostazioni lo dice.
- I log degli script vengono copiati nella memoria esterna e sono leggibili da root e da adb shell. Non stampare segreti nei log.

## Domande frequenti

- **Ho creato un'attività ma l'orologio non reagisce**: controlla che il modulo sia attivo in LSPosed, che l'ambito sia solo l'Orologio e di aver riavviato il processo dell'orologio.
- **L'attività non compare nell'elenco dell'orologio**: sta funzionando «L'orologio mostra solo le sveglie», disattivala.
- **All'ora stabilita non è successo nulla**: guarda i log degli script nell'app; script mancante o etichetta sbagliata vengono segnalati lì.

## Licenza

- Codice proprio di questo repository: **AGPL-3.0** (vedi `../LICENSE`)
- Componenti di terze parti e relative licenze: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Ringraziamenti

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): il punto di partenza del progetto; buona parte dell'interfaccia MIUIX e delle impostazioni arriva da lì
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): componenti UI in stile Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): API per root e Xposed
- Tutti quelli che hanno aperto issue e mandato log
