package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonValida;
import com.unical.travelapp.backend.experience.models.TipoImmagine;
import com.unical.travelapp.backend.experience.services.storage.ArchivioImmagini;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;


@Service
public class ImmagineStorageService {

    private static final DateTimeFormatter CARTELLA_MESE = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Pattern PERCORSO_VALIDO = Pattern.compile(
            "^\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z]{3}$");

    private final ArchivioImmagini archivio;
    private final long dimensioneMassimaByte;
    private final int latoMassimoPixel;

    public ImmagineStorageService(
            ArchivioImmagini archivio,
            @Value("${app.storage.immagini.max-size-byte}") long dimensioneMassimaByte,
            @Value("${app.storage.immagini.max-lato-pixel}") int latoMassimoPixel) {

        this.archivio = archivio;
        this.dimensioneMassimaByte = dimensioneMassimaByte;
        this.latoMassimoPixel = latoMassimoPixel;
    }


    public record ImmagineArchiviata(
            String percorsoRelativo,
            String contentType,
            long dimensioneByte,
            int larghezza,
            int altezza) {
    }


    public ImmagineArchiviata salva(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ImmagineNonValida("Nessun file caricato");
        }


        if (file.getSize() > dimensioneMassimaByte) {
            throw new ImmagineNonValida("Il file supera la dimensione massima consentita di "
                    + (dimensioneMassimaByte / 1024 / 1024) + " MB");
        }

        String estensioneDichiarata = estensione(file.getOriginalFilename());
        if (!TipoImmagine.estensioniAmmesse().contains(estensioneDichiarata)) {
            throw new ImmagineNonValida("Estensione non ammessa: sono accettati solo i file "
                    + TipoImmagine.estensioniAmmesse());
        }

        byte[] contenuto = leggiContenuto(file);


        byte[] intestazione = Arrays.copyOf(contenuto,
                Math.min(contenuto.length, TipoImmagine.BYTE_INTESTAZIONE));
        TipoImmagine tipo = TipoImmagine.daContenuto(intestazione)
                .orElseThrow(() -> new ImmagineNonValida(
                        "Il contenuto del file non e' un'immagine in un formato ammesso: "
                                + TipoImmagine.estensioniAmmesse()));
        if (!tipo.ammetteEstensione(estensioneDichiarata)) {
            throw new ImmagineNonValida("L'estensione del file non corrisponde al suo contenuto reale");
        }

        int[] dimensioni = dimensioniImmagine(contenuto);

        String nomeFile = UUID.randomUUID() + "." + tipo.getEstensioneCanonica();
        String percorsoRelativo = LocalDate.now().format(CARTELLA_MESE) + "/" + nomeFile;

        archivio.scrivi(percorsoRelativo, contenuto);

        return new ImmagineArchiviata(percorsoRelativo, tipo.getContentType(),
                contenuto.length, dimensioni[0], dimensioni[1]);
    }

    public Resource carica(String percorsoRelativo) {
        verificaFormatoPercorso(percorsoRelativo);
        return archivio.leggi(percorsoRelativo);
    }

    public void elimina(String percorsoRelativo) {
        verificaFormatoPercorso(percorsoRelativo);
        archivio.elimina(percorsoRelativo);
    }

    private byte[] leggiContenuto(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ArchiviazioneImmagineFallita("Lettura del file caricato fallita", e);
        }
    }

    private int[] dimensioniImmagine(byte[] contenuto) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(contenuto))) {

            Iterator<ImageReader> lettori = ImageIO.getImageReaders(input);
            if (!lettori.hasNext()) {
                throw new ImmagineNonValida("Il file non e' un'immagine leggibile");
            }

            ImageReader lettore = lettori.next();
            try {
                lettore.setInput(input);
                int larghezza = lettore.getWidth(0);
                int altezza = lettore.getHeight(0);

                if (larghezza <= 0 || altezza <= 0
                        || larghezza > latoMassimoPixel || altezza > latoMassimoPixel) {
                    throw new ImmagineNonValida("Dimensioni dell'immagine non ammesse: massimo "
                            + latoMassimoPixel + " pixel per lato");
                }

                return new int[]{larghezza, altezza};
            } finally {
                lettore.dispose();
            }

        } catch (IOException e) {
            throw new ImmagineNonValida("Il file non e' un'immagine leggibile");
        }
    }


    private void verificaFormatoPercorso(String percorsoRelativo) {
        if (percorsoRelativo == null || !PERCORSO_VALIDO.matcher(percorsoRelativo).matches()) {
            throw new ImmagineNonValida("Percorso dell'immagine non valido");
        }
    }


    private String estensione(String nomeOriginale) {
        String nome = StringUtils.getFilename(StringUtils.cleanPath(
                Optional.ofNullable(nomeOriginale).orElse("")));

        String estensione = StringUtils.getFilenameExtension(nome);
        if (estensione == null || estensione.isBlank()) {
            throw new ImmagineNonValida("Il nome del file non ha un'estensione");
        }

        return estensione.toLowerCase(Locale.ROOT);
    }
}
