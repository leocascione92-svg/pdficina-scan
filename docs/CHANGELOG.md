# Changelog

## 0.1.2 — 2026-10-16 — CLOUD BUILD PREPARATION
- Baseline 0.1.1 mantenuta integralmente nelle funzioni.
- Incrementati versionCode a 3 e versionName a 0.1.2.
- Aggiunto `.github/workflows/compila-apk.yml`: build Android automatica via GitHub Actions Ubuntu 24.04, Gradle 8.11.1 e JDK 17.
- Test JVM 4/4 su matematica prospettica, verifica apksigner, checksum SHA-256.
- Workflow predisposto per caricare file APK direttamente in GitHub Releases (repo privato), senza ZIP da estrarre per l'utente.
- Nessun APK compilato nell'ambiente corrente; serve un repository e un'esecuzione GitHub Actions riuscita.

# Changelog

## 0.1.1 — 2026-10-16 — PRE-ALFA BUILD PREPARATION
- Aggiornato versionCode a 2 e versionName a 0.1.1.
- Nuovo script Windows di compilazione con download strumenti ufficiali Gradle/Android SDK, build e verifica firma.
- Generazione checksum SHA256 dell'APK se la compilazione riesce.
- ZIPMASTER non sostituisce o modifica il ramo desktop PDFicina.
- Nessun APK prodotto o certificato in questo ambiente; Android SDK e connettivita verso repository ufficiali non disponibili.

# Changelog

## 0.1.0 — 2026-10-16 — PRE-ALFA SORGENTE
- Primo progetto Android separato da PDFicina desktop.
- Fotografia con ACTION_IMAGE_CAPTURE e URI MediaStore pending/publish.
- Import multiplo con ACTION_OPEN_DOCUMENT e copia in memoria condivisa.
- Editor con quattro angoli trascinabili, warp prospettico, bilineare, filtri.
- Suggerimento bordi sperimentale, da verificare manualmente.
- Lista pagine con ordine, rimozione non distruttiva e sessione persistita.
- PDF A4 multipagina tramite PdfDocument in cartella Download/PDFicinaScan.
- Niente codice nativo ABI-specifico o permesso Internet.
- Test prospettiva matematica 4/4 superati su JVM locale.
- Build APK e test Android reali NON ESEGUITI: SDK Android mancante nell'ambiente.
