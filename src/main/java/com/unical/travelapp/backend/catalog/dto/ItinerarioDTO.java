package com.unical.travelapp.backend.catalog.dto;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Data
public class ItinerarioDTO {
    private Long id;
    private Long organizzatoreId;
    private String titolo;
    private String descrizione;
    private String destinazionePrincipale;
    private BigDecimal prezzoBase;
    private Integer durataGiorni;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private LocalDate dataLimitePrenotazione;
    private Integer maxPartecipanti;
    private String stato;
    private Double mediaVoti;
    private long numeroRecensioni;
    private boolean dateDisponibili;
    private List<GiornoProgrammaDTO> programma = new ArrayList<>();
    private List<ImmagineResponse> immagini = new ArrayList<>();
}
