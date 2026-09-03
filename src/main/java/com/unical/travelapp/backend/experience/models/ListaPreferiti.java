package com.unical.travelapp.backend.experience.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.identity.entity.Utente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "preferiti")
public class ListaPreferiti extends Auditable {

    public static final String NOME_LISTA_PREDEFINITA = "I miei preferiti";

    @Id
    @Column(name = "preferito_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "ID autogenerato della lista dei preferiti", example = "123")
    private long id;

    @Column(name = "nome", length = 80)
    @Schema(description = "Nome della lista scelto dal viaggiatore", example = "Viaggi d'estate")
    private String nome = NOME_LISTA_PREDEFINITA;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilita", length = 20)
    @Schema(description = "PRIVATA (solo il proprietario) oppure CONDIVISA (proprietario + destinatari)")
    private VisibilitaListaPreferiti visibilita = VisibilitaListaPreferiti.PRIVATA;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "utente_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "utente a cui appartiene la lista preferiti")
    private Utente utente;

    @ManyToMany
    @JoinTable(
            name = "itinerario_preferito",
            joinColumns = @JoinColumn(name = "preferito_id"),
            inverseJoinColumns = @JoinColumn(name = "itinerario_id")
    )
    @Schema(description = "lista degli itinerari presenti nei preferiti")
    private List<Itinerario> itinerari = new ArrayList<>();


    @ManyToMany
    @JoinTable(
            name = "preferiti_condivisione",
            joinColumns = @JoinColumn(name = "preferito_id"),
            inverseJoinColumns = @JoinColumn(name = "utente_id")
    )
    @Schema(description = "utenti con cui la lista e' condivisa")
    private List<Utente> destinatari = new ArrayList<>();


    public String getNome() {
        return nome == null || nome.isBlank() ? NOME_LISTA_PREDEFINITA : nome;
    }


    public VisibilitaListaPreferiti getVisibilita() {
        return visibilita == null ? VisibilitaListaPreferiti.PRIVATA : visibilita;
    }


    public boolean appartieneA(Utente altro) {
        return altro != null && utente != null && Objects.equals(utente.getId(), altro.getId());
    }


    public boolean eLeggibileDa(Utente altro) {
        if (appartieneA(altro)) {
            return true;
        }
        if (altro == null || getVisibilita() != VisibilitaListaPreferiti.CONDIVISA) {
            return false;
        }
        return destinatari.stream().anyMatch(d -> Objects.equals(d.getId(), altro.getId()));
    }
}
