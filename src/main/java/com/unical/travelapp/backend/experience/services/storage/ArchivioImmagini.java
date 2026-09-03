package com.unical.travelapp.backend.experience.services.storage;

import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import org.springframework.core.io.Resource;

public interface ArchivioImmagini {

    void scrivi(String percorsoRelativo, byte[] contenuto);

    Resource leggi(String percorsoRelativo);

    void elimina(String percorsoRelativo);

    String descrizione();
}
