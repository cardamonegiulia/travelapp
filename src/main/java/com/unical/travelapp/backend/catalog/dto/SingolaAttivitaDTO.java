package com.unical.travelapp.backend.catalog.dto;
import com.unical.travelapp.backend.experience.models.DTO.ImmagineResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
public class SingolaAttivitaDTO {
    private Long id;
    private Long organizzatoreId;
    private String titolo;
    private String descrizione;
    private String luogo;
    private BigDecimal prezzo;
    private Integer durataMinuti;
    private Integer maxPartecipanti;
    private List<ImmagineResponse> immagini = new ArrayList<>();
    public SingolaAttivitaDTO() {
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getOrganizzatoreId() {
        return organizzatoreId;
    }
    public void setOrganizzatoreId(Long organizzatoreId) {
        this.organizzatoreId = organizzatoreId;
    }
    public String getTitolo() {
        return titolo;
    }
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    public String getDescrizione() {
        return descrizione;
    }
    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
    public String getLuogo() {
        return luogo;
    }
    public void setLuogo(String luogo) {
        this.luogo = luogo;
    }
    public BigDecimal getPrezzo() {
        return prezzo;
    }
    public void setPrezzo(BigDecimal prezzo) {
        this.prezzo = prezzo;
    }
    public Integer getDurataMinuti() {
        return durataMinuti;
    }
    public void setDurataMinuti(Integer durataMinuti) {
        this.durataMinuti = durataMinuti;
    }
    public Integer getMaxPartecipanti() {
        return maxPartecipanti;
    }
    public void setMaxPartecipanti(Integer maxPartecipanti) {
        this.maxPartecipanti = maxPartecipanti;
    }
    public List<ImmagineResponse> getImmagini() {
        return immagini;
    }
    public void setImmagini(List<ImmagineResponse> immagini) {
        this.immagini = (immagini != null) ? immagini : new ArrayList<>();
    }
}
