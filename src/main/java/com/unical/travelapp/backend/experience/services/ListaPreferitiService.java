package com.unical.travelapp.backend.experience.services;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.repository.ItinerarioRepository;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.exeption.ItinerarioNonTrovato;
import com.unical.travelapp.backend.experience.exeption.ListaPreferitiNonTrovata;
import com.unical.travelapp.backend.experience.mapper.ListaPreferitiMapper;
import com.unical.travelapp.backend.experience.models.DTO.CondivisioneRequest;
import com.unical.travelapp.backend.experience.models.DTO.ListaPreferitiDTO;
import com.unical.travelapp.backend.experience.models.DTO.ListaPreferitiRequest;
import com.unical.travelapp.backend.experience.models.ListaPreferiti;
import com.unical.travelapp.backend.experience.models.VisibilitaListaPreferiti;
import com.unical.travelapp.backend.experience.repository.ListaPreferitiRepository;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ListaPreferitiService {

    private final UtenteService utenteService;
    private final UtenteRepository utenteRepository;
    private final ListaPreferitiRepository repo;
    private final ItinerarioRepository itinerarioRepository;
    private final ListaPreferitiMapper mapper;
    private final AuditLogger auditLogger;

    public ListaPreferitiService(UtenteService utenteService,
                                 UtenteRepository utenteRepository,
                                 ListaPreferitiRepository repo,
                                 ItinerarioRepository itinerarioRepository,
                                 ListaPreferitiMapper mapper,
                                 AuditLogger auditLogger) {
        this.utenteService = utenteService;
        this.utenteRepository = utenteRepository;
        this.repo = repo;
        this.itinerarioRepository = itinerarioRepository;
        this.mapper = mapper;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<ListaPreferitiDTO> getMieListe() {
        Utente utente = utenteCorrente();
        return mapper.toRiepiloghi(repo.findByUtenteOrderByIdDesc(utente), utente);
    }

    @Transactional(readOnly = true)
    public List<ListaPreferitiDTO> getListeCondiviseConMe() {
        Utente utente = utenteCorrente();
        return mapper.toRiepiloghi(repo.findCondiviseCon(utente), utente);
    }

    @Transactional(readOnly = true)
    public ListaPreferitiDTO getLista(Long listaId) {
        Utente utente = utenteCorrente();
        return mapper.toDettaglio(caricaLeggibile(listaId, utente), utente);
    }

    @Transactional
    public ListaPreferitiDTO creaLista(ListaPreferitiRequest request) {
        Utente utente = utenteCorrente();
        String nome = request.getNome().trim();
        vietaNomeDuplicato(utente, nome);

        ListaPreferiti lista = new ListaPreferiti();
        lista.setUtente(utente);
        lista.setNome(nome);
        lista.setVisibilita(request.visibilitaRichiesta());

        ListaPreferiti salvata = repo.save(lista);
        auditLogger.success("LISTA_PREFERITI_CREATA", "listaPreferiti", String.valueOf(salvata.getId()));
        return mapper.toDettaglio(salvata, utente);
    }

    @Transactional
    public ListaPreferitiDTO aggiornaLista(Long listaId, ListaPreferitiRequest request) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);

        String nome = request.getNome().trim();
        if (!lista.getNome().equalsIgnoreCase(nome)) {
            vietaNomeDuplicato(utente, nome);
        }
        lista.setNome(nome);

        VisibilitaListaPreferiti nuova = request.visibilitaRichiesta();
        if (nuova == VisibilitaListaPreferiti.PRIVATA && !lista.getDestinatari().isEmpty()) {
            lista.getDestinatari().clear();
            auditLogger.success("LISTA_PREFERITI_CONDIVISIONI_REVOCATE", "listaPreferiti", String.valueOf(listaId));
        }
        lista.setVisibilita(nuova);

        ListaPreferiti salvata = repo.save(lista);
        auditLogger.success("LISTA_PREFERITI_MODIFICATA", "listaPreferiti", String.valueOf(listaId));
        return mapper.toDettaglio(salvata, utente);
    }

    @Transactional
    public void eliminaLista(Long listaId) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);

        repo.delete(lista);
        auditLogger.success("LISTA_PREFERITI_ELIMINATA", "listaPreferiti", String.valueOf(listaId));
    }


    @Transactional
    public ListaPreferitiDTO aggiungiItinerario(Long listaId, Long itinerarioId) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);

        return mapper.toDettaglio(inserisci(lista, itinerarioId), utente);
    }

    @Transactional
    public ListaPreferitiDTO rimuoviItinerario(Long listaId, Long itinerarioId) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);
        Itinerario itinerario = caricaItinerario(itinerarioId);

        lista.getItinerari().removeIf(salvato -> Objects.equals(salvato.getId(), itinerario.getId()));
        return mapper.toDettaglio(repo.save(lista), utente);
    }

    @Transactional
    public ListaPreferitiDTO aggiungiAllaListaPredefinita(Long itinerarioId) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = repo
                .findFirstByUtenteAndNome(utente, ListaPreferiti.NOME_LISTA_PREDEFINITA)
                .orElseGet(() -> {
                    ListaPreferiti nuova = new ListaPreferiti();
                    nuova.setUtente(utente);
                    nuova.setNome(ListaPreferiti.NOME_LISTA_PREDEFINITA);
                    nuova.setVisibilita(VisibilitaListaPreferiti.PRIVATA);
                    return nuova;
                });

        return mapper.toDettaglio(inserisci(lista, itinerarioId), utente);
    }


    @Transactional
    public void rimuoviDaTutteLeMieListe(Long itinerarioId) {
        Utente utente = utenteCorrente();
        Itinerario itinerario = caricaItinerario(itinerarioId);

        for (ListaPreferiti lista : repo.findByUtenteOrderByIdDesc(utente)) {
            boolean rimosso = lista.getItinerari()
                    .removeIf(salvato -> Objects.equals(salvato.getId(), itinerario.getId()));
            if (rimosso) {
                repo.save(lista);
            }
        }
    }

    @Transactional
    public ListaPreferitiDTO condividiCon(Long listaId, CondivisioneRequest request) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);
        Utente destinatario = destinatarioRichiesto(request);

        if (Objects.equals(destinatario.getId(), utente.getId())) {
            throw new IllegalArgumentException("la lista è già tua: non serve condividerla con te stesso");
        }

        boolean giaPresente = lista.getDestinatari().stream()
                .anyMatch(d -> Objects.equals(d.getId(), destinatario.getId()));
        if (!giaPresente) {
            lista.getDestinatari().add(destinatario);
        }
        lista.setVisibilita(VisibilitaListaPreferiti.CONDIVISA);

        ListaPreferiti salvata = repo.save(lista);
        auditLogger.success("LISTA_PREFERITI_CONDIVISA", "listaPreferiti",
                listaId + " -> utente " + destinatario.getId());
        return mapper.toDettaglio(salvata, utente);
    }


    @Transactional
    public ListaPreferitiDTO revocaCondivisione(Long listaId, Long utenteId) {
        Utente utente = utenteCorrente();
        ListaPreferiti lista = caricaModificabile(listaId, utente);

        lista.getDestinatari().removeIf(destinatario -> Objects.equals(destinatario.getId(), utenteId));

        ListaPreferiti salvata = repo.save(lista);
        auditLogger.success("LISTA_PREFERITI_CONDIVISIONE_REVOCATA", "listaPreferiti",
                listaId + " -> utente " + utenteId);
        return mapper.toDettaglio(salvata, utente);
    }

    private Utente utenteCorrente() {
        Utente utente = utenteService.getUtenteSessione();
        if (utente == null) {

            throw new AccessDeniedException("utente della sessione non disponibile");
        }
        return utente;
    }


    private ListaPreferiti caricaLeggibile(Long listaId, Utente utente) {
        ListaPreferiti lista = repo.findById(listaId)
                .orElseThrow(() -> new ListaPreferitiNonTrovata("lista dei preferiti non trovata"));

        if (!lista.eLeggibileDa(utente)) {
            throw new ListaPreferitiNonTrovata("lista dei preferiti non trovata");
        }
        return lista;
    }

    private ListaPreferiti caricaModificabile(Long listaId, Utente utente) {
        ListaPreferiti lista = caricaLeggibile(listaId, utente);

        if (!lista.appartieneA(utente)) {
            throw new AccessDeniedException("la lista dei preferiti non è tua");
        }
        return lista;
    }

    private ListaPreferiti inserisci(ListaPreferiti lista, Long itinerarioId) {
        Itinerario itinerario = caricaItinerario(itinerarioId);

        boolean giaSalvato = lista.getItinerari().stream()
                .anyMatch(salvato -> Objects.equals(salvato.getId(), itinerario.getId()));
        if (!giaSalvato) {
            lista.getItinerari().add(itinerario);
        }
        return repo.save(lista);
    }

    private Itinerario caricaItinerario(Long itinerarioId) {
        return itinerarioRepository.findById(itinerarioId)
                .orElseThrow(() -> new ItinerarioNonTrovato("itinerario non trovato"));
    }

    private Utente destinatarioRichiesto(CondivisioneRequest request) {
        if (request.getUtenteId() != null) {
            return utenteRepository.findById(request.getUtenteId())
                    .orElseThrow(() -> new ListaPreferitiNonTrovata("utente da condividere non trovato"));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return utenteRepository.findByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new ListaPreferitiNonTrovata("utente da condividere non trovato"));
        }
        throw new IllegalArgumentException("indica l'utente con cui condividere: utenteId oppure email");
    }

    private void vietaNomeDuplicato(Utente utente, String nome) {
        if (repo.existsByUtenteAndNomeIgnoreCase(utente, nome)) {
            throw new IllegalArgumentException("hai già una lista con questo nome");
        }
    }
}
