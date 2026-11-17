HyperAlarm è un modulo non commerciale di potenziamento del sistema che funziona sul framework Xposed ed è destinato a Mi Clock. Trasforma le sveglie di Mi Clock in attivatori di script root: quando scatta una sveglia, uno script da Lei fornito può essere eseguito al posto della suoneria, oppure in aggiunta a essa. Legga e comprenda il presente Accordo per l'utente di HyperAlarm prima di continuare a utilizzare il modulo.

## 0. Versione e data di entrata in vigore

- 0.1. Versione dell'accordo: 1.0
- 0.2. Data di entrata in vigore: 15 novembre 2026
- 0.3. Il presente accordo viene distribuito con ogni compilazione; la versione più recente è disponibile nel repository del progetto.

## 1. Disposizioni generali

- 1.1. HyperAlarm è sviluppato e mantenuto dal Progetto HyperAlarm (di seguito, «noi»).
- 1.2. Il modulo richiede un dispositivo con permessi root e il framework Xposed / LSPosed installato; in assenza di una delle due condizioni, non funziona.
- 1.3. L'unico ambito di applicazione del modulo è Mi Clock (com.android.deskclock). Non iniettiamo hook in altre applicazioni.
- 1.4. Utilizzando il modulo, Lei riconosce che esso fa eseguire alle sveglie uno script da Lei fornito all'orario impostato, anziché limitarsi a far suonare la sveglia.
- 1.5. Può smettere di utilizzarlo in qualsiasi momento disinstallando il modulo o disattivandolo nel gestore Xposed.

## 2. Ambito della licenza

- 2.1. Le concediamo una licenza personale, non trasferibile, non sublicenziabile e non esclusiva per installare e utilizzare il modulo.
- 2.2. Il codice sorgente è pubblicato sotto la GNU Affero General Public License v3.0 (AGPL-3.0). Può copiarlo, modificarlo e ridistribuirlo alle condizioni di tale licenza.
- 2.3. La preinstallazione commerciale, la vendita abbinata o la distribuzione insieme a un dispositivo richiedono la nostra preventiva autorizzazione scritta.
- 2.4. Le parti ereditate da HyperCeiler restano soggette alla loro licenza originale; si veda THIRD_PARTY_NOTICES.md nel repository.
- 2.5. Il nome e l'icona di HyperAlarm non sono inclusi nella licenza del codice sorgente.

## 3. Funzionalità e Sua responsabilità

- 3.1. Gli script vengono eseguiti come root nel namespace di mount principale del sistema e possono quindi vedere le directory dei dati di altre applicazioni.
- 3.2. Gli script sono forniti, installati e mantenuti da Lei. Non siamo responsabili del comportamento degli script né di alcuna conseguenza della loro esecuzione.
- 3.3. Un'attività silenziosa sopprime l'interfaccia della sveglia; se lo script è assente, viene rifiutato o la sveglia non può essere riprogrammata, il modulo mantiene il normale suono della sveglia.
- 3.4. Questo modulo non è un mezzo di promemoria affidabile. Non lo utilizzi in situazioni in cui una sveglia mancata possa causare lesioni personali o perdite patrimoniali rilevanti, inclusi, a titolo esemplificativo, ambito medico, sicurezza antincendio o servizi di reperibilità.
- 3.5. Modificare il comportamento del sistema comporta rischi, inclusi, a titolo non esaustivo, l'impossibilità di avviare il dispositivo, la perdita di dati o la decadenza della garanzia.

## 4. Trattamento delle informazioni

- 4.1. I registri degli script, i registri dell'applicazione, le registrazioni degli arresti anomali, le impostazioni e la password di blocco schermo restano esclusivamente sul Suo dispositivo. Non li carichiamo, non li raccogliamo e non li vendiamo.
- 4.2. Il modulo accede alla rete in esattamente due punti: la pagina «Informazioni» interroga interfacce pubbliche di GitHub per leggere l'avatar dell'autore e l'elenco dei contributori; il testo dell'accordo viene scaricato da GitHub. Entrambi gli accessi rivelano a GitHub solo il Suo indirizzo IP e non contengono alcun identificatore del dispositivo.
- 4.3. I dettagli sono riportati nell'Informativa sulla privacy di HyperAlarm pubblicata insieme al presente accordo.

## 5. Autorizzazioni richieste

- 5.1. Root: per eseguire script, leggere e scrivere la directory degli script, sincronizzare i registri e modificare impostazioni globali. È necessaria per le funzioni principali.
- 5.2. Elenco delle applicazioni installate: per mostrare le applicazioni nel selettore dell'ambito.
- 5.3. Notifiche: per mostrare l'esecuzione degli script in corso e i risultati, nonché i motivi degli errori.
- 5.4. Rete: solo nei due casi descritti al punto 4.2.
- 5.5. Può rifiutare qualsiasi autorizzazione; la funzione corrispondente non sarà disponibile, ma le altre non saranno interessate.

## 6. Norme di utilizzo

- 6.1. Non deve rimuovere le informazioni sul diritto d'autore e sulla licenza dal modulo o dalle sue copie.
- 6.2. Non deve utilizzare il modulo per compromettere la sicurezza delle reti, violare la privacy altrui o compiere altri atti contrari alla legge.
- 6.3. Non deve installare o utilizzare il modulo su un dispositivo altrui senza il consenso del proprietario.
- 6.4. Non deve utilizzare il modulo per aggirare pagamenti, violare la protezione del software o eludere controlli di accesso.

## 7. Esclusione e limitazione di responsabilità

- 7.1. Il modulo è fornito «così com'è», senza garanzie di alcun tipo, esplicite o implicite, incluse, a titolo non esaustivo, quelle di commerciabilità, idoneità a uno scopo particolare e non violazione di diritti di terzi.
- 7.2. Abbiamo verificato il funzionamento solo su Mi Clock 18.28.0. Dopo un aggiornamento dell'orologio o del sistema, alcune funzioni potrebbero non funzionare più o comportarsi diversamente.
- 7.3. I problemi causati dalle Sue azioni, dal contenuto dei Suoi script, dal Suo ambiente root, dal framework Xposed o da un aggiornamento del sistema sono a Suo carico.
- 7.4. Nella misura massima consentita dalla legge, non siamo responsabili per danni indiretti, incidentali, speciali o punitivi derivanti dall'uso o dall'impossibilità di utilizzare questo modulo.
- 7.5. Se una clausola è dichiarata invalida, le altre clausole restano in vigore.

## 8. Modifiche e cessazione

- 8.1. Possiamo modificare il presente accordo in occasione di un aggiornamento di versione; il testo modificato viene distribuito con la nuova compilazione e pubblicato nel repository.
- 8.2. Se continua a utilizzare il modulo dopo l'entrata in vigore di una modifica, ciò costituisce accettazione; in caso contrario, smetta di utilizzarlo e disinstalli il modulo.
- 8.3. Può porre fine al presente accordo in qualsiasi momento disinstallando il modulo.

## 9. Legge applicabile e controversie

- 9.1. Il presente accordo è disciplinato dalle leggi della Repubblica Popolare Cinese.
- 9.2. Le controversie derivanti dal presente accordo devono essere risolte innanzitutto mediante trattativa bonaria; in caso di esito negativo, sono sottoposte al tribunale popolare competente della nostra sede principale.

## 10. Contatti

- 10.1. Per qualsiasi questione relativa al presente accordo, apra una segnalazione nel repository del progetto.
- 10.2. Se ritiene che una clausola non corrisponda al comportamento effettivo, La preghiamo di segnalarcelo affinché possiamo correggerla, anziché considerarla vincolante.
