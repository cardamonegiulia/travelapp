package com.unical.travelapp.backend.experience.services.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Selezione dell'archivio: nessun contesto Spring, la configurazione si costruisce a mano
// perche' i suoi metodi prendono solo valori di configurazione.
@DisplayName("ConfigurazioneArchivioImmagini: scelta dello storage e controlli all'avvio")
class ConfigurazioneArchivioImmaginiTest {

    private final ConfigurazioneArchivioImmagini configurazione = new ConfigurazioneArchivioImmagini();

    @TempDir
    Path cartella;

    @Test
    @DisplayName("il tipo filesystem produce un archivio su disco")
    void archivioSuDisco() {
        ArchivioImmagini archivio = configurazione.archivioFilesystem(cartella.toString());

        assertThat(archivio).isInstanceOf(ArchivioFilesystem.class);
        assertThat(archivio.descrizione()).contains("filesystem locale");
    }

    @Test
    @DisplayName("con tipo s3 e credenziali complete si ottiene un archivio su object storage")
    void archivioSuObjectStorage() {
        ArchivioImmagini archivio = configurazione.archivioS3(
                "https://esempio.r2.cloudflarestorage.com", "immagini-travelapp",
                "chiave-di-prova", "segreto-di-prova", "auto");

        assertThat(archivio).isInstanceOf(ArchivioS3.class);
        // il bucket compare nella descrizione, la chiave segreta no
        assertThat(archivio.descrizione())
                .contains("immagini-travelapp")
                .doesNotContain("segreto-di-prova");
    }

    /*
     * I quattro casi che seguono sono il motivo per cui esiste questa classe: con lo storage
     * esterno attivo ma mal configurato, *ogni* upload e ogni lettura fallirebbero a runtime.
     * Il messaggio deve dire quale property manca, altrimenti resta da indovinare.
     */

    @Test
    @DisplayName("con tipo s3 e endpoint mancante l'applicazione non parte")
    void endpointObbligatorio() {
        assertThatThrownBy(() -> configurazione.archivioS3(
                "  ", "bucket", "chiave", "segreto", "auto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.storage.immagini.s3.endpoint");
    }

    @Test
    @DisplayName("con tipo s3 e bucket mancante l'applicazione non parte")
    void bucketObbligatorio() {
        assertThatThrownBy(() -> configurazione.archivioS3(
                "https://esempio.r2.cloudflarestorage.com", "", "chiave", "segreto", "auto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.storage.immagini.s3.bucket");
    }

    @Test
    @DisplayName("con tipo s3 e access key mancante l'applicazione non parte")
    void accessKeyObbligatoria() {
        assertThatThrownBy(() -> configurazione.archivioS3(
                "https://esempio.r2.cloudflarestorage.com", "bucket", null, "segreto", "auto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.storage.immagini.s3.access-key-id");
    }

    @Test
    @DisplayName("un endpoint che contiene gia' il bucket viene rifiutato all'avvio")
    void endpointConBucketRifiutato() {
        // e' l'URL che la dashboard R2 mostra sotto "S3 API": senza questo controllo gli
        // oggetti finirebbero sotto un prefisso in piu', senza che niente segnali l'errore
        assertThatThrownBy(() -> configurazione.archivioS3(
                "https://esempio.r2.cloudflarestorage.com/immagini-travelapp",
                "immagini-travelapp", "chiave", "segreto", "auto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non deve contenere il nome del bucket")
                .hasMessageContaining("https://esempio.r2.cloudflarestorage.com");
    }

    @Test
    @DisplayName("con tipo s3 e secret mancante l'applicazione non parte")
    void secretObbligatorio() {
        assertThatThrownBy(() -> configurazione.archivioS3(
                "https://esempio.r2.cloudflarestorage.com", "bucket", "chiave", null, "auto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.storage.immagini.s3.secret-access-key");
    }
}
