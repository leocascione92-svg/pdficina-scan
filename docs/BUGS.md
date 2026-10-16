# Problemi noti e regressioni da prevenire — 0.1.0

- Nessuna verifica di compilazione tramite Android Gradle Plugin: possibili errori API non ancora rilevati.
- Nessuna verifica runtime su Samsung Galaxy S24; verificare EXTRA_OUTPUT e URI MediaStore con IS_PENDING.
- Suggerimento bordi auto è una stima assiale e NON trova in modo affidabile angoli inclinati.
- La sessione si conserva finché esistono i dati dell'app, ma dopo disinstallazione rimangono soltanto i file MediaStore pubblicati (non l'indice della sessione).
- In import massivo l'operazione accoda foto una per volta e non ha annullamento.
- Foto ritratte con orientamenti EXIF rari (transpose/transverse) da verificare.
- In presenza di input corrotti/permessi revocati, il PDF può fallire; in tal caso l'output incompleto viene cancellato.
- Le immagini e i PDF già salvati NON sono cancellati dall'app quando si rimuove la pagina dalla sessione.
- Attività Editor ricreata durante rotazione perde il lavoro se conferma salvataggio in corso; bloccare rotazione o migrare a ViewModel/WorkManager in successiva versione.
- Nessun supporto a lingua diversa dall'italiano nell'UI attuale.

- v0.1.1: bootstrap compilazione PowerShell non testato su PC Windows reale; necessita JDK 17+, connettivita e accettazione licenze SDK.
- v0.1.1: non ancora disponibile APK; la build su Windows deve verificare eventuali errori Gradle e Android.

- v0.1.2: build cloud mai eseguita; verificare esito Android Gradle Plugin e pipeline release.
- v0.1.2: debug keystore GitHub Actions effimero; aggiornamenti firmati con chiavi diverse non installabili sopra senza disinstallazione. Usare keystore stabile nella release definitiva.
