package com.unical.travelapp.backend.experience.models;

import com.unical.travelapp.backend.common.audit.Auditable;
import com.unical.travelapp.backend.identity.entity.Utente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// Metadati di un'immagine caricata. Il file NON sta nel database: sul DB c'e' solo il
// riferimento testuale al file scritto sullo storage (vedi ImmagineStorageService), cosi'
// le query non trascinano megabyte di binario e lo storage puo' essere spostato su un
// servizio esterno senza toccare lo schema.
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "immagini")
public class Immagine extends Auditable {

    @Id
    @Column(name = "immagine_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID generato automaticamente", example = "123")
    private Long id;

    // percorso relativo alla cartella base dello storage, nella forma "aaaa/mm/<uuid>.jpg".
    // Non contiene mai il nome scelto dall'utente (vedi ImmagineStorageService).
    @Column(name = "percorso_relativo", nullable = false, unique = true, length = 200)
    @Schema(description = "percorso del file sullo storage, relativo alla cartella base",
            example = "2026/08/9f1c2c3e-1f2a-4c9b-8f0a-2b1d4e5f6a7b.jpg")
    private String percorsoRelativo;

    @Column(name = "content_type", nullable = false, length = 50)
    @Schema(description = "tipo MIME reale del file, ricavato dal contenuto", example = "image/jpeg")
    private String contentType;

    @Column(name = "dimensione_byte", nullable = false)
    @Schema(description = "dimensione del file in byte", example = "204800")
    private long dimensioneByte;

    @Column(nullable = false)
    @Schema(description = "larghezza in pixel", example = "1920")
    private int larghezza;

    @Column(nullable = false)
    @Schema(description = "altezza in pixel", example = "1080")
    private int altezza;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "proprietario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "utente che ha caricato l'immagine")
    private Utente proprietario;
}
