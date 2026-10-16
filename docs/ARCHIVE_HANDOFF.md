# Archive handoff

Archivio verificato il 16/10/2026:
- Cartella centrale `X_CHATGPT/PDFICINA`.
- `PDFicina_ZIPMASTER.zip` contiene versione desktop Win32 0.6.0 PRE-ALFA datata 08/09/2026.
- Letti: README, HANDOFF, BUGS, STATE, NEXT, TEST, HISTORY, spec/BIBLE, porzione generale della ROADMAP.

Questa build Android costituisce un ramo progettuale SEPARATO (`it.pdficina.scan`). Non modifica alcun file di PDFicina desktop. La nomenclatura della build Android riparte da 0.1.0. Il presente ZIP è il pacchetto sorgente canonico iniziale per il ramo Android, autosufficiente per riprendere il progetto da una chat successiva.

Requisiti esterni necessari a creare un APK: Android SDK Platform 35, Android Gradle Plugin 8.9.2 e Gradle; strumenti NON inclusi nel ZIP per evitare redistribuzione degli SDK e aumentare la portabilità dell'archivio.


0.1.1 (2026-10-16): nuovo ZIPMASTER con script di build Windows. Prossimo passo: compilare con SDK 35, esaminare log e risolvere errori, firmare e testare APK Galaxy S24. Conservare 0.1.0 e relative specifiche.

Storico: history/PDFicinaScan_0.1.0_ZIPMASTER.zip incluso in ZIPMASTER 0.1.1.

0.1.2 (2026-10-16): pipeline di compilazione cloud pronta, attende repository GitHub. Avviare dopo il caricamento del workflow; correggere problemi da log CI. Storico 0.1.1 ZIPMASTER aggiunto a `history/`.
