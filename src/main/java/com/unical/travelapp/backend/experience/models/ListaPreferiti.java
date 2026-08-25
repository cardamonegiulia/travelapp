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

/**
 * Una lista di itinerari preferiti di un viaggiatore.
 *
 * <p>Un utente puo' averne quante ne vuole (prima ne esisteva una sola, implicita): ogni
 * lista ha un nome scelto da lui e una {@link VisibilitaListaPreferiti}. Se e' CONDIVISA,
 * {@link #destinatari} elenca gli utenti - uno per uno - che possono leggerla; scriverci
 * resta comunque riservato al proprietario.
 *
 * <p>La tabella resta {@code preferiti} e la join table degli itinerari resta
 * {@code itinerario_preferito}: cosi' i dati gia' presenti non vanno migrati.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "preferiti")
public class ListaPreferiti extends Auditable {

    /** Nome usato per la lista creata implicitamente quando si salva un itinerario "al volo". */
    public static final String NOME_LISTA_PREDEFINITA = "I miei preferiti";

    @Id
    @Column(name = "preferito_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "ID autogenerato della lista dei preferiti", example = "123")
    private long id;

    // Volutamente senza nullable=false: la colonna e' stata aggiunta a una tabella che in
    // ambiente di sviluppo puo' avere gia' righe, e ddl-auto=update non saprebbe come
    // riempirla. Il valore mancante e' normalizzato in lettura (vedi getNome()).
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
    // inizializzata: su una lista appena creata la collection non deve essere null,
    // altrimenti il primo add() dell'utente solleva NullPointerException
    private List<Itinerario> itinerari = new ArrayList<>();

    /**
     * Utenti - specifici, scelti dal proprietario - che possono leggere la lista quando e'
     * CONDIVISA. Vuota su una lista privata: l'invariante e' mantenuta dal service, che
     * svuota i destinatari quando la lista torna privata.
     */
    @ManyToMany
    @JoinTable(
            name = "preferiti_condivisione",
            joinColumns = @JoinColumn(name = "preferito_id"),
            inverseJoinColumns = @JoinColumn(name = "utente_id")
    )
    @Schema(description = "utenti con cui la lista e' condivisa")
    private List<Utente> destinatari = new ArrayList<>();

    /** Nome della lista, con il ripiego per le righe create prima che il campo esistesse. */
    public String getNome() {
        return nome == null || nome.isBlank() ? NOME_LISTA_PREDEFINITA : nome;
    }

    /** Visibilita' della lista: in mancanza di un valore la lista e' privata. */
    public VisibilitaListaPreferiti getVisibilita() {
        return visibilita == null ? VisibilitaListaPreferiti.PRIVATA : visibilita;
    }

    /** true se {@code utente} e' il proprietario della lista. */
    public boolean appartieneA(Utente altro) {
        return altro != null && utente != null && Objects.equals(utente.getId(), altro.getId());
    }

    /**
     * true se {@code utente} puo' LEGGERE la lista: il proprietario sempre, gli altri solo
     * se la lista e' condivisa ed e' stata condivisa proprio con loro.
     *
     * <p>Il confronto e' sull'id e non sull'oggetto: {@code destinatari} e' LAZY e i suoi
     * elementi possono essere proxy Hibernate, per i quali {@code equals} generato da
     * Lombok non e' affidabile.
     */
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
