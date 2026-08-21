package com.unical.travelapp.backend.experience.services.storage;

import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import org.springframework.core.io.Resource;

/**
 * Dove finiscono fisicamente i byte delle immagini.
 *
 * Separa il *trasporto* (disco locale, object storage remoto) dalla *validazione*, che
 * resta per intero in {@link com.unical.travelapp.backend.experience.services.ImmagineStorageService}
 * e non va mai duplicata qui dentro: un'implementazione di questa interfaccia riceve byte
 * gia' controllati e un percorso gia' generato dall'applicazione.
 *
 * Il "percorso relativo" ha sempre la forma {@code aaaa/mm/<uuid>.<est>} ed e' l'unica
 * chiave conosciuta dal database: su filesystem diventa un path sotto la cartella base, su
 * object storage la chiave dell'oggetto nel bucket. Cambiando implementazione lo schema del
 * database non si tocca.
 */
public interface ArchivioImmagini {

    /**
     * Scrive un oggetto nuovo. Non deve mai sovrascrivere un percorso gia' esistente: un
     * UUID ripetuto e' praticamente impossibile, e se accadesse e' un errore da far
     * emergere, non un'immagine altrui da sostituire in silenzio.
     *
     * @throws ArchiviazioneImmagineFallita se la scrittura non riesce (500)
     */
    void scrivi(String percorsoRelativo, byte[] contenuto);

    /**
     * Contenuto dell'oggetto, pronto per lo streaming verso il client.
     *
     * @throws ImmagineNonTrovata se il file non c'e' piu' sullo storage (404)
     */
    Resource leggi(String percorsoRelativo);

    /** Rimuove l'oggetto. Se non c'e' piu', non e' un errore: l'esito voluto e' lo stesso. */
    void elimina(String percorsoRelativo);

    /** Nome leggibile dell'archivio, per i log di avvio. */
    String descrizione();
}
