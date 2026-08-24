package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.experience.models.Immagine;
import com.unical.travelapp.backend.experience.services.ImmagineService;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Foto profilo dell'utente in sessione.
 *
 * <p>Sta in un service a se' e non dentro {@link UtenteService} per una ragione precisa:
 * {@link ImmagineService} dipende gia' da {@code UtenteService} per sapere chi sta
 * caricando: aggiungere qui la dipendenza opposta creerebbe un ciclo, che Spring rifiuta
 * all'avvio. Da questa parte il grafo resta aciclico
 * ({@code FotoProfiloService -> ImmagineService -> UtenteService}).
 *
 * <p>Non c'e' nessun id nella firma dei metodi: il proprietario e' sempre l'utente del
 * token. Un parametro sarebbe una richiesta al client di dire chi e', cioe' esattamente
 * cio' che l'utente non deve poter decidere.
 */
@Service
public class FotoProfiloService {

    private final UtenteRepository utenteRepository;
    private final UtenteService utenteService;
    private final ImmagineService immagineService;
    private final UtenteMapper utenteMapper;

    public FotoProfiloService(UtenteRepository utenteRepository,
                              UtenteService utenteService,
                              ImmagineService immagineService,
                              UtenteMapper utenteMapper) {
        this.utenteRepository = utenteRepository;
        this.utenteService = utenteService;
        this.immagineService = immagineService;
        this.utenteMapper = utenteMapper;
    }

    /**
     * Sostituisce la foto profilo con quella appena caricata e restituisce il profilo
     * aggiornato, cosi' al client basta una sola chiamata per mostrare il nuovo avatar.
     *
     * <p>L'ordine delle operazioni e' la parte che conta. La foto vecchia viene cancellata
     * <b>dopo</b> che il nuovo riferimento e' stato scritto (e reso persistente con
     * {@code saveAndFlush}): all'inverso, un errore durante l'upload lascerebbe l'utente
     * senza alcuna foto, avendo buttato via quella che aveva. Il {@code flush} esplicito
     * serve perche' l'UPDATE della colonna deve arrivare al database prima della DELETE
     * della riga a cui punta, altrimenti si viola la chiave esterna.
     *
     * <p>Se la validazione del file fallisce ({@code ImmagineNonValida}) la transazione si
     * chiude senza aver toccato nulla: la foto precedente resta al suo posto.
     *
     * <p>Il timeout e' esplicito perche' quello predefinito — {@code
     * spring.transaction.default-timeout}, dieci secondi — e' tarato su transazioni che
     * parlano solo col database, mentre qui dentro c'e' anche la scrittura su object
     * storage, che passa per la rete: superati i dieci secondi, la {@code saveAndFlush}
     * successiva moriva con {@code TransactionTimedOutException} e l'utente vedeva un 500
     * dopo una lunga attesa, con il file gia' finito sullo storage. Il valore va tenuto
     * sopra il tempo di caricamento peggiore ragionevole, non largo a piacere: per tutta la
     * sua durata resta impegnata una connessione del pool.
     */
    @Transactional(timeoutString = "${app.storage.immagini.upload-timeout-secondi}")
    public UtenteResponseDto imposta(MultipartFile file) {
        Utente utente = utenteService.getUtenteSessione();
        Immagine precedente = utente.getFotoProfilo();

        utente.setFotoProfilo(immagineService.caricaEntita(file));
        Utente aggiornato = utenteRepository.saveAndFlush(utente);

        if (precedente != null) {
            // niente foto orfane: senza questa riga ogni cambio lascerebbe sullo storage il
            // file precedente, che nessuno puo' piu' raggiungere ne' cancellare
            immagineService.eliminaEntita(precedente);
        }
        return utenteMapper.toResponseDto(aggiornato);
    }

    /** Torna al segnaposto: toglie il riferimento e cancella riga e file della foto. */
    @Transactional
    public void rimuovi() {
        Utente utente = utenteService.getUtenteSessione();
        Immagine precedente = utente.getFotoProfilo();
        if (precedente == null) {
            // gia' senza foto: l'operazione e' idempotente, non e' un errore
            return;
        }

        utente.setFotoProfilo(null);
        utenteRepository.saveAndFlush(utente);
        immagineService.eliminaEntita(precedente);
    }
}
