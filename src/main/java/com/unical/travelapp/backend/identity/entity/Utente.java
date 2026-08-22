package com.unical.travelapp.backend.identity.entity;
import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.experience.models.Immagine;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.unical.travelapp.backend.identity.entity.Ruolo;
@Entity
@Table(name = "utenti")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Utente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING) // Salva il nome del ruolo come stringa nel DB
    private Ruolo ruolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tema")
    private Tema tema = Tema.CHIARO;

    // Foto del profilo: una sola per utente, quindi un riferimento singolo e non una
    // galleria come su Itinerario. La chiave esterna sta qui perche' e' l'utente ad avere
    // "al piu' una foto": cosi' sostituirla e' un UPDATE di una colonna, e l'unicita' e'
    // garantita dallo schema invece che da codice applicativo.
    //
    // Nessun cascade: l'immagine ha un ciclo di vita proprio (file sullo storage + riga),
    // gestito da ImmagineService. Cancellarla insieme all'utente lascerebbe il file orfano
    // sul disco, perche' JPA non sa nulla del filesystem.
    @OneToOne
    @JoinColumn(name = "foto_profilo_id")
    private Immagine fotoProfilo;
}