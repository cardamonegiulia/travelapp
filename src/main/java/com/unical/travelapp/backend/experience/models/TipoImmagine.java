package com.unical.travelapp.backend.experience.models;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Allow-list dei formati accettati in upload.
//
// L'estensione dichiarata dal client non e' una prova di nulla: un eseguibile rinominato
// "foto.jpg" ha estensione valida e Content-Type valido, perche' entrambi arrivano dal
// chiamante. L'unico dato attendibile e' il contenuto, quindi ogni formato porta con se'
// la propria firma binaria (magic number) e il tipo effettivo viene riconosciuto leggendo
// i primi byte del file.
public enum TipoImmagine {

    JPEG("image/jpeg", "jpg", List.of("jpg", "jpeg"),
            new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),

    PNG("image/png", "png", List.of("png"),
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    // numero di byte da leggere dall'inizio del file per poter riconoscere qualunque firma
    public static final int BYTE_INTESTAZIONE = 8;

    private final String contentType;
    private final String estensioneCanonica;
    private final List<String> estensioniAmmesse;
    private final byte[] firma;

    TipoImmagine(String contentType, String estensioneCanonica, List<String> estensioniAmmesse, byte[] firma) {
        this.contentType = contentType;
        this.estensioneCanonica = estensioneCanonica;
        this.estensioniAmmesse = estensioniAmmesse;
        this.firma = firma;
    }

    public String getContentType() {
        return contentType;
    }

    // estensione con cui il file viene effettivamente salvato: una sola per formato, cosi'
    // ".jpeg" e ".JPG" finiscono comunque su disco come ".jpg"
    public String getEstensioneCanonica() {
        return estensioneCanonica;
    }

    public boolean ammetteEstensione(String estensione) {
        return estensioniAmmesse.contains(estensione);
    }

    // riconosce il formato reale dai primi byte del file, ignorando nome ed estensione
    public static Optional<TipoImmagine> daContenuto(byte[] intestazione) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.firmaCorrisponde(intestazione))
                .findFirst();
    }

    public static List<String> estensioniAmmesse() {
        return Arrays.stream(values())
                .flatMap(tipo -> tipo.estensioniAmmesse.stream())
                .toList();
    }

    private boolean firmaCorrisponde(byte[] intestazione) {
        if (intestazione == null || intestazione.length < firma.length) {
            return false;
        }
        return Arrays.equals(intestazione, 0, firma.length, firma, 0, firma.length);
    }
}
