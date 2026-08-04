package com.unical.travelapp.backend.identity.entity;
import com.unical.travelapp.backend.common.audit.Auditable;
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
}