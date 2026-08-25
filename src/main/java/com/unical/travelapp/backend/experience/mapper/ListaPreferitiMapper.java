package com.unical.travelapp.backend.experience.mapper;

import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.experience.models.DTO.ListaPreferitiDTO;
import com.unical.travelapp.backend.experience.models.DTO.UtenteCondivisioneDTO;
import com.unical.travelapp.backend.experience.models.ListaPreferiti;
import com.unical.travelapp.backend.identity.entity.Utente;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListaPreferitiMapper {

    private final ItinerarioMapper itinerarioMapper;

    public ListaPreferitiMapper(ItinerarioMapper itinerarioMapper) {
        this.itinerarioMapper = itinerarioMapper;
    }

    /**
     * Riepilogo per gli elenchi: niente itinerari, solo il loro numero. Una schermata con
     * dieci liste non deve tirarsi dietro dieci gallerie di immagini.
     */
    public ListaPreferitiDTO toRiepilogo(ListaPreferiti lista, Utente richiedente) {
        return costruisci(lista, richiedente, false);
    }

    /** Dettaglio della singola lista: include gli itinerari salvati. */
    public ListaPreferitiDTO toDettaglio(ListaPreferiti lista, Utente richiedente) {
        return costruisci(lista, richiedente, true);
    }

    public List<ListaPreferitiDTO> toRiepiloghi(List<ListaPreferiti> liste, Utente richiedente) {
        return liste.stream().map(lista -> toRiepilogo(lista, richiedente)).toList();
    }

    public UtenteCondivisioneDTO toDestinatario(Utente utente) {
        if (utente == null) return null;

        UtenteCondivisioneDTO dto = new UtenteCondivisioneDTO();
        dto.setId(utente.getId());
        dto.setNome(utente.getNome());
        dto.setCognome(utente.getCognome());
        dto.setEmail(utente.getEmail());
        return dto;
    }

    private ListaPreferitiDTO costruisci(ListaPreferiti lista, Utente richiedente, boolean conItinerari) {
        if (lista == null) return null;

        boolean proprietaria = lista.appartieneA(richiedente);

        ListaPreferitiDTO dto = new ListaPreferitiDTO();
        dto.setId(lista.getId());
        dto.setNome(lista.getNome());
        dto.setVisibilita(lista.getVisibilita());
        dto.setProprietaria(proprietaria);
        dto.setNumeroItinerari(lista.getItinerari().size());

        Utente proprietario = lista.getUtente();
        if (proprietario != null) {
            dto.setProprietarioId(proprietario.getId());
            dto.setProprietarioNome(proprietario.getNome() + " " + proprietario.getCognome());
        }

        if (conItinerari) {
            dto.setItinerari(lista.getItinerari().stream().map(itinerarioMapper::toDTO).toList());
        }

        // Solo al proprietario: a un destinatario non interessa - e non spetta - sapere con
        // chi altro il proprietario ha condiviso la lista.
        if (proprietaria) {
            dto.setDestinatari(lista.getDestinatari().stream().map(this::toDestinatario).toList());
        }

        return dto;
    }
}
