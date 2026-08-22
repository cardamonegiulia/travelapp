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

/**
 * Validazione delle immagini caricate e accesso allo storage.
 *
 * Qui sta tutta la logica di sicurezza dell'upload; il resto dell'applicazione riceve solo
 * un percorso gia' validato. La classe non conosce JPA ne' l'utente in sessione:
 * l'orchestrazione (chi carica, cosa finisce sul database) e' in {@link ImmagineService}.
 *
 * *Dove* finiscono i byte non la riguarda: e' un {@link ArchivioImmagini} iniettato — disco
 * locale o object storage esterno, scelto da {@code app.storage.immagini.tipo}. Le
 * validazioni restano identiche nei due casi, ed e' la ragione per cui stanno qui e non
 * nelle implementazioni dell'archivio.
 *
 * Ordine dei controlli, dal piu' economico al piu' costoso:
 * 1. file presente e non vuoto;
 * 2. dimensione entro il limite (difesa DoS: senza, si satura il disco del server);
 * 3. estensione dichiarata nell'allow-list;
 * 4. tipo reale ricavato dai byte del file, che deve coincidere con l'estensione;
 * 5. contenuto decodificabile come immagine e di dimensioni sensate;
 * 6. nome di destinazione generato dall'applicazione (UUID), mai quello del client.
 */
@Service
public class ImmagineStorageService {

    private static final DateTimeFormatter CARTELLA_MESE = DateTimeFormatter.ofPattern("yyyy/MM");

    // Forma esatta dei percorsi generati da questa classe: "aaaa/mm/<uuid>.<est>".
    // Vale anche in lettura, sui percorsi che arrivano dal database: se una riga venisse
    // manomessa, il percorso non passerebbe di qui.
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

    /** Esito dell'archiviazione: quanto serve al chiamante per creare la riga sul database. */
    public record ImmagineArchiviata(
            String percorsoRelativo,
            String contentType,
            long dimensioneByte,
            int larghezza,
            int altezza) {
    }

    /**
     * Valida il file e lo scrive sullo storage con un nome nuovo e casuale.
     *
     * @throws ImmagineNonValida se uno dei controlli sul file fallisce (400)
     * @throws ArchiviazioneImmagineFallita se la scrittura non riesce (500)
     */
    public ImmagineArchiviata salva(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ImmagineNonValida("Nessun file caricato");
        }

        // Per primo il controllo piu' economico: chiude la porta agli upload fatti solo per
        // riempire il disco. Il limite e' applicato anche da Spring
        // (spring.servlet.multipart.max-file-size); qui e' ridondante di proposito, perche' il
        // service deve reggere anche se quella property viene allargata.
        if (file.getSize() > dimensioneMassimaByte) {
            throw new ImmagineNonValida("Il file supera la dimensione massima consentita di "
                    + (dimensioneMassimaByte / 1024 / 1024) + " MB");
        }

        // Allow-list, non deny-list: si elencano i formati ammessi, non quelli vietati. Un
        // elenco di estensioni "pericolose" e' sempre incompleto per definizione.
        String estensioneDichiarata = estensione(file.getOriginalFilename());
        if (!TipoImmagine.estensioniAmmesse().contains(estensioneDichiarata)) {
            throw new ImmagineNonValida("Estensione non ammessa: sono accettati solo i file "
                    + TipoImmagine.estensioniAmmesse());
        }

        byte[] contenuto = leggiContenuto(file);

        // Il tipo reale si ricava dai byte, non dall'estensione ne' dal Content-Type: sono
        // entrambi campi che scrive il client.
        byte[] intestazione = Arrays.copyOf(contenuto,
                Math.min(contenuto.length, TipoImmagine.BYTE_INTESTAZIONE));
        TipoImmagine tipo = TipoImmagine.daContenuto(intestazione)
                .orElseThrow(() -> new ImmagineNonValida(
                        "Il contenuto del file non e' un'immagine in un formato ammesso: "
                                + TipoImmagine.estensioniAmmesse()));

        // Estensione ammessa e coerente col contenuto reale: un .png che dentro e' altro non
        // e' quasi mai una svista dell'utente.
        if (!tipo.ammetteEstensione(estensioneDichiarata)) {
            throw new ImmagineNonValida("L'estensione del file non corrisponde al suo contenuto reale");
        }

        int[] dimensioni = dimensioniImmagine(contenuto);

        // Nome generato dall'applicazione: UUID piu' estensione canonica, quindi solo cifre
        // esadecimali, trattini e un punto. Il nome scelto dal client non arriva mai al
        // filesystem, percio' non c'e' modo di farci passare "../" o caratteri speciali.
        String nomeFile = UUID.randomUUID() + "." + tipo.getEstensioneCanonica();
        String percorsoRelativo = LocalDate.now().format(CARTELLA_MESE) + "/" + nomeFile;

        archivio.scrivi(percorsoRelativo, contenuto);

        return new ImmagineArchiviata(percorsoRelativo, tipo.getContentType(),
                contenuto.length, dimensioni[0], dimensioni[1]);
    }

    /** Restituisce il file come Resource, per lo streaming verso il client. */
    public Resource carica(String percorsoRelativo) {
        verificaFormatoPercorso(percorsoRelativo);
        return archivio.leggi(percorsoRelativo);
    }

    /** Rimuove il file dallo storage; se non c'e' piu', non e' un errore. */
    public void elimina(String percorsoRelativo) {
        verificaFormatoPercorso(percorsoRelativo);
        archivio.elimina(percorsoRelativo);
    }

    private byte[] leggiContenuto(MultipartFile file) {
        try {
            // Il file e' gia' limitato a pochi MB dal controllo precedente, quindi tenerlo in
            // memoria e' accettabile e permette di ispezionarlo prima di scriverlo: niente
            // tocca il disco finche' non ha superato tutte le validazioni.
            return file.getBytes();
        } catch (IOException e) {
            throw new ArchiviazioneImmagineFallita("Lettura del file caricato fallita", e);
        }
    }

    // Ultimo controllo sul contenuto: deve essere decodificabile come immagine. Legge solo
    // l'header tramite ImageReader, senza decodificare i pixel, e rifiuta i lati assurdi: un
    // PNG di pochi KB puo' dichiarare 50000x50000 pixel e far esplodere la memoria di
    // chiunque provi ad aprirlo (decompression bomb).
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

    // Prima difesa contro il path traversal, valida per qualunque archivio: la forma deve
    // corrispondere esattamente a quella generata da salva(), quindi un "../.." non arriva
    // mai all'implementazione, che sia un percorso su disco o la chiave di un oggetto
    // remoto. Vale anche sui valori che arrivano dal database: se una riga venisse
    // manomessa, non passerebbe di qui. L'archivio su filesystem aggiunge poi il proprio
    // controllo di contenimento, come seconda difesa.
    private void verificaFormatoPercorso(String percorsoRelativo) {
        if (percorsoRelativo == null || !PERCORSO_VALIDO.matcher(percorsoRelativo).matches()) {
            throw new ImmagineNonValida("Percorso dell'immagine non valido");
        }
    }

    // Estensione ricavata dal nome originale, usata solo come dichiarazione da confrontare
    // col contenuto reale. StringUtils.getFilename scarta l'eventuale percorso contenuto nel
    // nome inviato dal client (alcuni browser mandano il path completo).
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
