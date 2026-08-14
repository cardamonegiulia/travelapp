package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.identity.dto.RegistrazioneRequest;
import com.unical.travelapp.backend.identity.dto.RuoloRegistrabile;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.RuoloRealmNonConfiguratoException;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.NuovoUtenteKeycloak;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.RuoloRealm;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Registrazione self-service: crea l'utente su Keycloak, gli assegna il ruolo realm scelto e
 * salva il record locale con lo stesso ruolo.
 *
 * <p>L'ordine dei passi non e' casuale, ed e' il punto centrale di questa classe:
 * <ol>
 *   <li>controllo email nel DB locale, per non creare su Keycloak un utente che sarebbe
 *       comunque rifiutato dopo;</li>
 *   <li>verifica che il ruolo realm <b>esista</b> prima di creare l'utente: e' l'unico ordine
 *       che rende impossibile ritrovarsi con un account senza ruolo;</li>
 *   <li>creazione su Keycloak;</li>
 *   <li>assegnazione del ruolo realm;</li>
 *   <li>salvataggio del record locale.</li>
 * </ol>
 * Dal passo 3 in poi qualsiasi errore innesca la compensazione: l'utente Keycloak appena
 * creato viene rimosso, cosi' non restano account a meta' che l'utente non potrebbe ne'
 * usare ne' ricreare (la seconda registrazione fallirebbe con 409 da Keycloak).
 *
 * <p>Non e' annotato {@code @Transactional} di proposito: l'unica scrittura sul DB e' un
 * singolo {@code save()}, che ha gia' la sua transazione. Una transazione piu' ampia
 * sposterebbe soltanto il commit dopo la fine del metodo, cioe' fuori dalla portata della
 * compensazione, peggiorando la situazione invece di migliorarla.
 */
@Service
public class RegistrazioneService {

    private static final Logger log = LoggerFactory.getLogger(RegistrazioneService.class);

    /**
     * Difesa in profondita'. Il tipo {@link RuoloRegistrabile} gia' esclude ADMIN, ma questa
     * lista fa in modo che aggiungere domani un valore all'enum non lo renda automaticamente
     * auto-assegnabile da un endpoint pubblico: serve una decisione esplicita anche qui.
     */
    private static final Set<String> RUOLI_AUTO_ASSEGNABILI = Set.of("VIAGGIATORE", "ORGANIZZATORE");

    private final UtenteRepository utenteRepository;
    private final UtenteMapper utenteMapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final AuditLogger auditLogger;

    public RegistrazioneService(UtenteRepository utenteRepository,
                                UtenteMapper utenteMapper,
                                KeycloakAdminClient keycloakAdminClient,
                                AuditLogger auditLogger) {
        this.utenteRepository = utenteRepository;
        this.utenteMapper = utenteMapper;
        this.keycloakAdminClient = keycloakAdminClient;
        this.auditLogger = auditLogger;
    }

    public UtenteResponseDto registra(RegistrazioneRequest richiesta) {
        String email = normalizzaEmail(richiesta.getEmail());
        RuoloRegistrabile ruoloScelto = richiesta.getRuolo();
        String nomeRuoloRealm = ruoloScelto.nomeRuoloRealm();

        if (!RUOLI_AUTO_ASSEGNABILI.contains(nomeRuoloRealm)) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, "ruolo non auto-assegnabile: " + nomeRuoloRealm);
            throw new IllegalArgumentException("Il ruolo richiesto non è assegnabile in fase di registrazione");
        }

        if (utenteRepository.existsByEmail(email)) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, "email già registrata");
            throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
        }

        String token = keycloakAdminClient.ottieniTokenAmministrativo();

        RuoloRealm ruoloRealm = keycloakAdminClient.trovaRuoloRealm(token, nomeRuoloRealm)
                .orElseThrow(() -> {
                    log.error("Ruolo realm '{}' assente su Keycloak: crearlo in Realm roles prima di "
                            + "abilitare la registrazione", nomeRuoloRealm);
                    auditLogger.failure("REGISTRAZIONE", "Utente", email,
                            "ruolo realm non configurato: " + nomeRuoloRealm);
                    return new RuoloRealmNonConfiguratoException(nomeRuoloRealm);
                });

        String keycloakId = keycloakAdminClient.creaUtente(token, new NuovoUtenteKeycloak(
                email,
                email,
                richiesta.getNome().trim(),
                richiesta.getCognome().trim(),
                richiesta.getPassword()));

        try {
            keycloakAdminClient.assegnaRuoloRealm(token, keycloakId, ruoloRealm);

            Utente utente = new Utente();
            utente.setKeycloakId(keycloakId);
            utente.setNome(richiesta.getNome().trim());
            utente.setCognome(richiesta.getCognome().trim());
            utente.setEmail(email);
            // stesso ruolo assegnato su Keycloak: il campo locale non e' piu' un default fisso
            utente.setRuolo(ruoloScelto.toRuolo());
            utente.setTema(Tema.CHIARO);

            Utente salvato = utenteRepository.save(utente);
            auditLogger.success("REGISTRAZIONE", "Utente", String.valueOf(salvato.getId()));
            return utenteMapper.toResponseDto(salvato);

        } catch (RuntimeException e) {
            auditLogger.failure("REGISTRAZIONE", "Utente", email, e.getClass().getSimpleName());
            keycloakAdminClient.eliminaUtenteSenzaPropagareErrori(token, keycloakId);
            throw e;
        }
    }

    /**
     * L'email e' sia username Keycloak sia chiave unica locale: normalizzarla evita che
     * {@code Mario@Example.com} e {@code mario@example.com} diventino due account distinti
     * in locale ma lo stesso username su Keycloak (che li normalizza per conto suo).
     */
    private String normalizzaEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
