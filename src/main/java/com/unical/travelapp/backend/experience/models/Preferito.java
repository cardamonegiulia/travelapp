package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.identity.entity.Utente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Data
@Entity
@Table(name = "preferiti")
public class Preferito {

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
    private List<Itinerario> itinerario;

}
