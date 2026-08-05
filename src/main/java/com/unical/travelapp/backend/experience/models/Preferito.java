package com.unical.travelapp.backend.experience.models;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.identity.entity.Utente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class Preferito extends Auditable {

    @Id
    @Column(name = "preferito_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "ID autogenerato della lista dei preferiti", example = "123")
    private long id;

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
    // inizializzata: su un Preferito appena creato la collection non deve essere null,
    // altrimenti il primo add() dell'utente solleva NullPointerException
    private List<Itinerario> itinerario = new ArrayList<>();

}
