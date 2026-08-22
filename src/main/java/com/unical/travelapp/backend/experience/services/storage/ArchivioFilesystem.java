package com.unical.travelapp.backend.experience.services.storage;

import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonValida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Immagini su disco, sotto una cartella base. E' il comportamento storico del progetto e
 * resta il default: non richiede alcun servizio esterno, quindi lo sviluppo locale e i test
 * funzionano senza credenziali.
 *
 * In produzione la cartella va su un volume dedicato, fuori dalla directory
 * dell'applicazione: cosi' un upload che riempie il disco non ferma il server.
 */
public class ArchivioFilesystem implements ArchivioImmagini {

    private static final Logger log = LoggerFactory.getLogger(ArchivioFilesystem.class);

    private final Path cartellaBase;

    public ArchivioFilesystem(String percorsoCartellaBase) {
        this.cartellaBase = Paths.get(percorsoCartellaBase).toAbsolutePath().normalize();
    }

    @Override
    public void scrivi(String percorsoRelativo, byte[] contenuto) {
        Path destinazione = risolvi(percorsoRelativo);
        try {
            Files.createDirectories(destinazione.getParent());
            // CREATE_NEW: se il nome esistesse gia' la scrittura fallisce invece di
            // sovrascrivere in silenzio un'altra immagine.
            Files.write(destinazione, contenuto, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("Scrittura dell'immagine {} fallita", percorsoRelativo, e);
            throw new ArchiviazioneImmagineFallita("Scrittura del file sullo storage fallita", e);
        }
    }

    @Override
    public Resource leggi(String percorsoRelativo) {
        Path file = risolvi(percorsoRelativo);

        if (!Files.isRegularFile(file)) {
            throw new ImmagineNonTrovata("File dell'immagine non presente sullo storage");
        }

        return new FileSystemResource(file);
    }

    @Override
    public void elimina(String percorsoRelativo) {
        Path file = risolvi(percorsoRelativo);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new ArchiviazioneImmagineFallita(
                    "Cancellazione del file " + percorsoRelativo + " fallita", e);
        }
    }

    @Override
    public String descrizione() {
        return "filesystem locale (" + cartellaBase + ")";
    }

    // Unico punto in cui un percorso relativo diventa assoluto. Il formato e' gia' stato
    // verificato da ImmagineStorageService; questo controllo di contenimento resta come
    // seconda difesa contro il path traversal, perche' e' l'ultima riga prima del disco.
    private Path risolvi(String percorsoRelativo) {
        Path risolto = cartellaBase.resolve(percorsoRelativo).normalize();
        if (!risolto.startsWith(cartellaBase)) {
            throw new ImmagineNonValida("Percorso dell'immagine non valido");
        }
        return risolto;
    }
}
