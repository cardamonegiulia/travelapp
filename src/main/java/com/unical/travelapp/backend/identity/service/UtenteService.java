package com.unical.travelapp.backend.identity.service;

import com.unical.travelapp.backend.identity.dto.UtenteDto;
import com.unical.travelapp.backend.identity.dto.UtenteResponseDto;
import com.unical.travelapp.backend.identity.dto.UtenteUpdateDto;
import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Tema;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.exception.UtenteGiaEsistenteException;
import com.unical.travelapp.backend.identity.exception.RiautenticazioneRichiestaException;
import com.unical.travelapp.backend.identity.exception.UtenteNonTrovatoException;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient;
import com.unical.travelapp.backend.identity.keycloak.KeycloakAdminClient.ProfiloKeycloak;
import com.unical.travelapp.backend.identity.mapper.UtenteMapper;
import com.unical.travelapp.backend.identity.repository.UtenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class UtenteService {

    private static final Logger log = LoggerFactory.getLogger(UtenteService.class);

    private final UtenteRepository utenteRepository;
    private final UtenteMapper utenteMapper;
    private final KeycloakAdminClient keycloakAdminClient;

    /**
     * Quanto puo' essere vecchia l'autenticazione perche' il cambio password sia ammesso.
     * Cinque minuti: abbastanza per compilare un form, troppo poco perche' un token
     * intercettato resti utile.
     */
    private final long etaMassimaAutenticazioneSecondi;

    public UtenteService(UtenteRepository utenteRepository,
                         UtenteMapper utenteMapper,
                         KeycloakAdminClient keycloakAdminClient,
                         @Value("${app.security.max-auth-age-seconds:300}") long etaMassimaAutenticazioneSecondi) {
        this.utenteRepository = utenteRepository;
        this.utenteMapper = utenteMapper;
        this.keycloakAdminClient = keycloakAdminClient;
        this.etaMassimaAutenticazioneSecondi = etaMassimaAutenticazioneSecondi;
    }

    public UtenteResponseDto salvaUtenteDatoDTO(UtenteDto dto) {
        if (utenteRepository.existsByEmail(dto.getEmail())) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + dto.getEmail()
            );
        }
        if (utenteRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con keycloakId: " + dto.getKeycloakId()
            );
        }
        return utenteMapper.toResponseDto(
                utenteRepository.save(utenteMapper.toEntity(dto))
        );
    }

    public Page<UtenteResponseDto> ottieniTutti(Pageable pageable) {
        return utenteRepository.findAll(pageable)
                .map(utenteMapper::toResponseDto);
    }

    public UtenteResponseDto ottieniPerId(Long id) {
        return utenteMapper.toResponseDto(
                utenteRepository.findById(id)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con id " + id + " non trovato"
                        ))
        );
    }

    public UtenteResponseDto ottieniPerKeycloakId(String keycloakId) {
        return utenteMapper.toResponseDto(
                utenteRepository.findByKeycloakId(keycloakId)
                        .orElseThrow(() -> new UtenteNonTrovatoException(
                                "Utente con keycloakId " + keycloakId + " non trovato"
                        ))
        );
    }

    public Utente salvaUtente(Utente utente) {
        return utenteRepository.save(utente);
    }
    /**
     * Aggiorna il profilo in locale e, per i campi che esistono in entrambe le fonti, anche su
     * Keycloak.
     *
     * <p>Senza la propagazione le due fonti divergevano in silenzio: l'email e' la chiave
     * unica locale ed e' anche l'indirizzo che l'IdP mette nel claim {@code email} dei token,
     * quindi dopo un cambio email il record locale e il token descrivevano due persone
     * diverse.
     *
     * <p>Il tema e' solo locale: se la richiesta cambia soltanto quello non si disturba
     * Keycloak. Quando invece c'e' da propagare, l'IdP viene aggiornato per primo e il
     * salvataggio locale segue; se quest'ultimo fallisce, una compensazione best-effort
     * riporta indietro il profilo remoto, come gia' fa la registrazione.
     */
    public UtenteResponseDto aggiornaUtente(Long id, UtenteUpdateDto dto) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));

        // stessa normalizzazione della registrazione: l'email e' chiave unica, non deve
        // duplicarsi per differenza di maiuscole
        String emailRichiesta = dto.getEmail() == null ? null : normalizzaEmail(dto.getEmail());

        // controllo email duplicata solo se è stata modificata
        if (emailRichiesta != null &&
                !utente.getEmail().equals(emailRichiesta) &&
                utenteRepository.existsByEmail(emailRichiesta)) {
            throw new UtenteGiaEsistenteException(
                    "Esiste già un utente con email: " + emailRichiesta
            );
        }

        ProfiloKeycloak precedente = profiloDi(utente);

        utenteMapper.updateEntity(utente, dto);
        if (emailRichiesta != null) {
            utente.setEmail(emailRichiesta);
        }

        ProfiloKeycloak aggiornato = profiloDi(utente);
        if (aggiornato.equals(precedente)) {
            return utenteMapper.toResponseDto(utenteRepository.save(utente));
        }

        String token = keycloakAdminClient.ottieniTokenAmministrativo();
        keycloakAdminClient.aggiornaProfilo(token, utente.getKeycloakId(), aggiornato);
        try {
            return utenteMapper.toResponseDto(utenteRepository.save(utente));
        } catch (RuntimeException e) {
            keycloakAdminClient.aggiornaProfiloSenzaPropagareErrori(token, utente.getKeycloakId(), precedente);
            throw e;
        }
    }

    private ProfiloKeycloak profiloDi(Utente utente) {
        return new ProfiloKeycloak(utente.getEmail(), utente.getNome(), utente.getCognome());
    }
    /**
     * Cancella l'utente su Keycloak <b>e</b> in locale.
     *
     * <p>L'ordine e' il punto centrale. Cancellando prima in locale, un errore dell'IdP
     * lascerebbe su Keycloak un account ancora capace di autenticarsi: al primo
     * {@code POST /api/utenti/me} il provisioning just-in-time ricreerebbe il record locale
     * e la cancellazione risulterebbe annullata, in silenzio. Togliendo prima l'identita' si
     * revoca l'accesso per primo: se poi fallisce la cancellazione locale resta una riga
     * orfana, visibile e ripulibile, di un utente che non puo' piu' entrare.
     *
     * <p>I token gia' emessi restano validi fino alla scadenza
     * ({@code accessTokenLifespan}, 5 minuti sul realm): la cancellazione impedisce di
     * ottenerne di nuovi, non invalida quelli in circolazione.
     */
    public void eliminaUtente(Long id) {
        Utente utente = utenteRepository.findById(id)
                .orElseThrow(() -> new UtenteNonTrovatoException(
                        "Utente con id " + id + " non trovato"
                ));

        keycloakAdminClient.eliminaUtente(
                keycloakAdminClient.ottieniTokenAmministrativo(), utente.getKeycloakId());

        utenteRepository.deleteById(id);
        log.info("Utente {} cancellato in locale e su Keycloak", id);
    }
    /**
     * Cambia la password dell'utente che ha inviato il token.
     *
     * <p>Il controllo che conta e' il primo: l'operazione richiede un'autenticazione recente,
     * non il solo possesso di un token valido. Un access token rubato resta spendibile fino
     * alla scadenza, e senza questa condizione basterebbe a impossessarsi dell'account in
     * modo definitivo — cambiata la password, il proprietario non rientra piu'.
     *
     * <p>La password attuale non viene chiesta: verificarla lato server significherebbe
     * riaccendere il password grant su un client Keycloak, cioe' il flusso che l'applicazione
     * ha smesso di usare (vedi {@code docs/login-android-setup.md}). Il claim
     * {@code auth_time} e' la prova equivalente, ed e' Keycloak a produrla.
     *
     * <p>Il client che riceve 401 deve rifare il login con {@code max_age}: rinnovare il
     * token col refresh non aiuta, perche' {@code auth_time} resta quello della sessione
     * originale.
     *
     * @return l'id locale dell'utente, per l'evento di audit
     */
    public Long cambiaPassword(Jwt jwt, String nuovaPassword) {
        Instant autenticatoIl = jwt.getClaimAsInstant("auth_time");
        if (autenticatoIl == null
                || autenticatoIl.isBefore(Instant.now().minusSeconds(etaMassimaAutenticazioneSecondi))) {
            // il claim assente e' trattato come "non fresca": un token che non dice quando
            // e' avvenuto il login non puo' dimostrare che sia avvenuto da poco
            throw new RiautenticazioneRichiestaException(etaMassimaAutenticazioneSecondi);
        }

        Utente utente = ottieniUtenteDaToken(jwt);

        String token = keycloakAdminClient.ottieniTokenAmministrativo();
        keycloakAdminClient.impostaPassword(token, utente.getKeycloakId(), nuovaPassword);
        keycloakAdminClient.terminaSessioniSenzaPropagareErrori(token, utente.getKeycloakId());

        log.info("Password cambiata per l'utente {}; sessioni terminate", utente.getId());
        return utente.getId();
    }

    // Il metodo centralizzato per ottenere utente
    public Utente ottieniUtenteDaToken(Jwt jwt) {
        String keycloakId = jwt.getSubject(); // Estrae il sotto (ID) dal token
        return utenteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UtenteNonTrovatoException("Utente loggato non trovato nel database locale"));
    }

    public Utente getUtenteSessione(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Utente utente = ottieniUtenteDaToken(jwt);

        return utente;
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // Se servisse ESCLUSIVAMENTE il numero ID (Long):
    public Long ottieniIdDaToken(Jwt jwt) {
        return ottieniUtenteDaToken(jwt).getId();
    }

    /**
     * Provisioning "just-in-time" del record locale a partire dal token.
     *
     * <p>Da quando esiste {@code POST /api/auth/registrazione} questo e' il percorso
     * secondario: serve agli account nati fuori dal flusso self-service (creati a mano in
     * console, o domani da un identity provider esterno). Per tutti gli altri e' un semplice
     * recupero per {@code keycloakId}.
     */
    public UtenteResponseDto sincronizzaUtente(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        // se l'utente esiste già nel DB lo restituisce, riallineandone il ruolo
        return utenteRepository.findByKeycloakId(keycloakId)
                .map(this::riallineaRuolo)
                .map(utenteMapper::toResponseDto)
                .orElseGet(() -> utenteMapper.toResponseDto(creaDaToken(keycloakId, jwt)));
    }

    /**
     * Riporta il ruolo locale su quello del token quando un amministratore lo cambia su
     * Keycloak.
     *
     * <p>Senza questo passaggio il campo resta al valore fissato alla creazione: i
     * {@code @PreAuthorize} continuerebbero ad applicare il ruolo vero (quello del token)
     * mentre {@code UtenteResponseDto.ruolo} letto dal frontend resterebbe quello vecchio,
     * per sempre. E' la stessa divergenza gia' corretta in {@link #creaDaToken}, che pero'
     * agiva solo sul primo accesso.
     *
     * <p>Quando il token non porta <b>alcun</b> ruolo applicativo il valore locale non
     * viene toccato: un client scope configurato male (ruoli non inclusi nei token, vedi
     * {@code docs/login-android-setup.md} passo 4) declasserebbe altrimenti ogni utente a
     * VIAGGIATORE al primo accesso, perdendo il dato originale in modo irreversibile.
     */
    private Utente riallineaRuolo(Utente utente) {
        return ruoloDalTokenCorrente()
                .filter(ruoloDelToken -> ruoloDelToken != utente.getRuolo())
                .map(ruoloDelToken -> {
                    log.info("Ruolo locale dell'utente {} riallineato da {} a {} (fonte: token)",
                            utente.getId(), utente.getRuolo(), ruoloDelToken);
                    utente.setRuolo(ruoloDelToken);
                    return utenteRepository.save(utente);
                })
                .orElse(utente);
    }

    private Utente creaDaToken(String keycloakId, Jwt jwt) {
        String email = normalizzaEmail(jwt.getClaimAsString("email"));

        // L'email e' chiave unica locale. Senza il claim veniva salvata "": il primo utente
        // se la prendeva in esclusiva e ogni successivo falliva sul vincolo di unicita' con
        // un 409 "conflitto sui dati" che non dice nulla a chi legge. Un token senza email
        // significa client Keycloak senza lo scope "email", cioe' un errore di
        // configurazione: va rifiutato subito e in modo esplicito.
        if (email.isEmpty()) {
            throw new IllegalArgumentException(
                    "Il token non contiene l'email necessaria a creare l'account locale");
        }
        if (utenteRepository.existsByEmail(email)) {
            throw new UtenteGiaEsistenteException("Esiste già un utente registrato con questa email");
        }

        Utente nuovo = new Utente();
        nuovo.setKeycloakId(keycloakId);
        nuovo.setNome(claimOppureVuoto(jwt, "given_name"));
        nuovo.setCognome(claimOppureVuoto(jwt, "family_name"));
        nuovo.setEmail(email);
        nuovo.setRuolo(ruoloDalTokenCorrente().orElse(Ruolo.VIAGGIATORE));
        nuovo.setTema(Tema.CHIARO);
        return utenteRepository.save(nuovo);
    }

    /**
     * Ruolo applicativo ricavato dalle authority gia' calcolate da
     * {@code KeycloakRoleConverter}, non da un default fisso: con {@code VIAGGIATORE}
     * cablato, un ORGANIZZATORE creato in console si ritrovava il ruolo sbagliato nella
     * colonna locale, quindi anche in {@code UtenteResponseDto.ruolo} letto dal frontend,
     * mentre i {@code @PreAuthorize} continuavano ad applicare quello vero del token.
     *
     * <p>Si leggono le authority e non i claim grezzi perche' sono l'unica fonte gia'
     * allineata all'autorizzazione: il converter unisce {@code realm_access} e
     * {@code resource_access}, una seconda lettura qui potrebbe divergere.
     *
     * <p>Vince il ruolo piu' alto. {@link Optional#empty()} quando il token non porta alcun
     * ruolo applicativo: e' un'informazione <b>assente</b>, non un VIAGGIATORE, e i due
     * chiamanti la trattano in modo diverso. Alla creazione si sceglie VIAGGIATORE (minimo
     * privilegio, non c'e' un valore precedente da preservare); al riallineamento si lascia
     * intatto il valore gia' presente.
     */
    private Optional<Ruolo> ruoloDalTokenCorrente() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return Stream.of(Ruolo.ADMIN, Ruolo.ORGANIZZATORE, Ruolo.VIAGGIATORE)
                .filter(ruolo -> authorities.stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + ruolo.name())))
                .findFirst();
    }

    /** Stessa normalizzazione della registrazione: l'email e' chiave unica, non va duplicata per differenza di maiuscole. */
    private String normalizzaEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String claimOppureVuoto(Jwt jwt, String claim) {
        String valore = jwt.getClaimAsString(claim);
        return valore != null ? valore : "";
    }
}