# PDFicina Scan — Master Prompt Android

Progetto Android distinto da PDFicina desktop. Non usare il codice o asset proprietari di CamScanner; replicare i flussi con codice proprio o componenti open source verificati. Obiettivo: scanner documentale gratuito, aperto e offline-first, personalizzabile, ottimizzato per Samsung Galaxy S24.

Principi vincolanti: le funzioni già approvate sono preservate ad ogni build, l'originale è immutabile, ogni elaborazione produce una nuova versione, file e PDF in archivio utente indipendente dai dati privati dell'app, niente cloud obbligatorio, niente account e niente pubblicità. Tutte le nuove funzionalità devono avere test ripetibili e changelog. Non dichiarare superati test su dispositivi mai testati.

ROADMAP:
0.1.x — acquisizione e PDF, crop manuale + aiuto bordi, filtri, ordinamento, storage robusto.
0.2.x — rilevamento quadrilatero robusto tramite OpenCV o implementazione equivalente con licenze verificate, OCR offline italiano/inglese (Tesseract Android), PDF ricercabili.
0.3.x — archivio documenti/categorie/ricerca, batch, retro/fronte ID, backup e restore ZIP verificati su USB-C con checksum.
0.4.x — firme e annotazioni, ottimizzazione dimensione PDF, protezione, importazione PDF, condivisione avanzata.
0.5+ — elaborazione ombre e deformazioni, output professionale, PDFicina desktop interoperability.
1.0 — test di regressione Android 10-16, test fisici S24, pacchetto APK firmato, test restore da SSD, audit sicurezza e accessibilità.
