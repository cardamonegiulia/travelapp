package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonValida;
import com.unical.travelapp.backend.experience.services.ImmagineStorageService.ImmagineArchiviata;
import com.unical.travelapp.backend.experience.services.storage.ArchivioFilesystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Test delle validazioni dell'upload: nessun contesto Spring, il service si costruisce a
// mano perche' prende solo tre valori di configurazione.
@DisplayName("ImmagineStorageService: validazioni e scrittura su storage")
class ImmagineStorageServiceTest {

    private static final long CINQUE_MB = 5L * 1024 * 1024;

    @TempDir
    Path cartellaStorage;

    private ImmagineStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new ImmagineStorageService(new ArchivioFilesystem(cartellaStorage.toString()), CINQUE_MB, 10000);
    }


    @Test
    @DisplayName("un PNG valido viene scritto sullo storage con nome generato dall'applicazione")
    void salvaImmagineValida() throws IOException {
        MultipartFile file = file("vacanza.png", "png", immagine(200, 100, "png"));

        ImmagineArchiviata archiviata = storage.salva(file);

        assertThat(archiviata.contentType()).isEqualTo("image/png");
        assertThat(archiviata.larghezza()).isEqualTo(200);
        assertThat(archiviata.altezza()).isEqualTo(100);
        // percorso "aaaa/mm/<uuid>.png": del nome scelto dall'utente non resta traccia
        assertThat(archiviata.percorsoRelativo())
                .matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png")
                .doesNotContain("vacanza");
        assertThat(cartellaStorage.resolve(archiviata.percorsoRelativo())).exists();
    }


    @Test
    @DisplayName("due upload dello stesso file producono due nomi diversi")
    void nomiSempreDiversi() throws IOException {
        byte[] contenuto = immagine(10, 10, "png");

        String primo = storage.salva(file("foto.png", "png", contenuto)).percorsoRelativo();
        String secondo = storage.salva(file("foto.png", "png", contenuto)).percorsoRelativo();

        assertThat(primo).isNotEqualTo(secondo);
    }


    @Test
    @DisplayName("un file vuoto viene rifiutato")
    void rifiutaFileVuoto() {
        MultipartFile file = file("foto.png", "png", new byte[0]);

        assertThatThrownBy(() -> storage.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("Nessun file");
    }


    @Test
    @DisplayName("un file oltre la dimensione massima viene rifiutato prima di toccare il disco")
    void rifiutaFileTroppoGrande() throws IOException {
        // service con limite di 100 byte: qualunque PNG valido lo supera
        ImmagineStorageService storageStretto =
                new ImmagineStorageService(new ArchivioFilesystem(cartellaStorage.toString()), 100, 10000);
        MultipartFile file = file("grande.png", "png", immagine(200, 200, "png"));

        assertThatThrownBy(() -> storageStretto.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("dimensione massima");

        assertThat(cartellaStorage).isEmptyDirectory();
    }


    @Test
    @DisplayName("un'estensione fuori dall'allow-list viene rifiutata")
    void rifiutaEstensioneNonAmmessa() throws IOException {
        // contenuto perfettamente valido, ma estensione non ammessa
        MultipartFile file = file("script.gif", "image/gif", immagine(10, 10, "png"));

        assertThatThrownBy(() -> storage.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("Estensione non ammessa");
    }


    @Test
    @DisplayName("un eseguibile rinominato .jpg viene rifiutato: conta il contenuto, non il nome")
    void rifiutaEseguibileMascherato() {
        // "MZ": firma di un eseguibile Windows. Estensione e Content-Type dichiarano
        // un'immagine, ma i byte no.
        byte[] eseguibile = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04};
        MultipartFile file = file("innocua.jpg", "image/jpeg", eseguibile);

        assertThatThrownBy(() -> storage.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("non e' un'immagine in un formato ammesso");

        assertThat(cartellaStorage).isEmptyDirectory();
    }


    @Test
    @DisplayName("un JPEG dichiarato come .png viene rifiutato per incoerenza fra estensione e contenuto")
    void rifiutaEstensioneIncoerenteColContenuto() throws IOException {
        MultipartFile file = file("foto.png", "image/png", immagine(10, 10, "jpg"));

        assertThatThrownBy(() -> storage.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("non corrisponde al suo contenuto reale");
    }


    @Test
    @DisplayName("un'immagine con lati oltre il limite viene rifiutata (decompression bomb)")
    void rifiutaImmagineTroppoGrandeInPixel() throws IOException {
        ImmagineStorageService storageStretto =
                new ImmagineStorageService(new ArchivioFilesystem(cartellaStorage.toString()), CINQUE_MB, 50);
        MultipartFile file = file("enorme.png", "image/png", immagine(100, 100, "png"));

        assertThatThrownBy(() -> storageStretto.salva(file))
                .isInstanceOf(ImmagineNonValida.class)
                .hasMessageContaining("pixel per lato");
    }


    @Test
    @DisplayName("un nome con path traversal non esce dalla cartella base")
    void neutralizzaPathTraversalNelNome() throws IOException {
        MultipartFile file = file("../../../evil.png", "image/png", immagine(10, 10, "png"));

        ImmagineArchiviata archiviata = storage.salva(file);

        Path scritto = cartellaStorage.resolve(archiviata.percorsoRelativo()).normalize();
        assertThat(scritto).exists();
        assertThat(scritto.startsWith(cartellaStorage)).isTrue();
        assertThat(archiviata.percorsoRelativo()).doesNotContain("..").doesNotContain("evil");
    }


    @Test
    @DisplayName("in lettura, un percorso con path traversal viene rifiutato")
    void rifiutaPercorsoManomessoInLettura() {
        assertThatThrownBy(() -> storage.carica("2026/08/../../../../etc/passwd"))
                .isInstanceOf(ImmagineNonValida.class);
    }


    @Test
    @DisplayName("carica() restituisce il file scritto, elimina() lo rimuove")
    void caricaEdElimina() throws IOException {
        ImmagineArchiviata archiviata = storage.salva(file("foto.jpg", "image/jpeg", immagine(20, 20, "jpg")));

        assertThat(storage.carica(archiviata.percorsoRelativo()).exists()).isTrue();

        storage.elimina(archiviata.percorsoRelativo());

        assertThat(Files.exists(cartellaStorage.resolve(archiviata.percorsoRelativo()))).isFalse();
        assertThatThrownBy(() -> storage.carica(archiviata.percorsoRelativo()))
                .isInstanceOf(ImmagineNonTrovata.class);
    }


    private MultipartFile file(String nome, String contentType, byte[] contenuto) {
        return new MockMultipartFile("file", nome, contentType, contenuto);
    }

    // immagine reale generata al volo: cosi' i byte hanno la firma e l'header corretti del
    // formato, senza dover versionare file binari di prova
    private byte[] immagine(int larghezza, int altezza, String formato) throws IOException {
        BufferedImage immagine = new BufferedImage(larghezza, altezza,
                "jpg".equals(formato) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(immagine, formato, output);
        return output.toByteArray();
    }
}
