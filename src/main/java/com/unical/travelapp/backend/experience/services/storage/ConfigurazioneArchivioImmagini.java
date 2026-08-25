package com.unical.travelapp.backend.experience.services.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Sceglie dove finiscono le immagini, in base a {@code app.storage.immagini.tipo}:
 * {@code filesystem} (default) oppure {@code s3}.
 *
 * Il default resta il disco locale di proposito: i test e chi clona il repo devono poter
 * lavorare senza credenziali di un servizio esterno. L'object storage si attiva
 * esplicitamente, e in quel caso le credenziali sono obbligatorie e verificate all'avvio.
 */
@Configuration
public class ConfigurazioneArchivioImmagini {

    private static final Logger log = LoggerFactory.getLogger(ConfigurazioneArchivioImmagini.class);

    @Bean
    @ConditionalOnProperty(name = "app.storage.immagini.tipo", havingValue = "filesystem", matchIfMissing = true)
    public ArchivioImmagini archivioFilesystem(
            @Value("${app.storage.immagini.path}") String percorsoCartellaBase) {

        ArchivioImmagini archivio = new ArchivioFilesystem(percorsoCartellaBase);
        log.info("Immagini archiviate su {}", archivio.descrizione());
        return archivio;
    }

    /**
     * Client S3 puntato su un endpoint personalizzato.
     *
     * Due impostazioni non sono negoziabili con Cloudflare R2:
     * <ul>
     *   <li>{@code region} vale sempre {@code auto} — R2 non ha regioni, ma il protocollo
     *       S3 richiede che il campo sia valorizzato per calcolare la firma;</li>
     *   <li>{@code pathStyleAccessEnabled} attivo: l'indirizzamento virtual-hosted
     *       ({@code bucket.endpoint}) non e' quello che R2 si aspetta sull'endpoint S3.</li>
     * </ul>
     *
     * Il bean e' chiuso da Spring allo shutdown ({@code close} e' il destroyMethod dedotto
     * da {@link AutoCloseable}), cosi' il pool di connessioni non resta appeso.
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.immagini.tipo", havingValue = "s3")
    public ArchivioImmagini archivioS3(
            @Value("${app.storage.immagini.s3.endpoint:}") String endpoint,
            @Value("${app.storage.immagini.s3.bucket:}") String bucket,
            @Value("${app.storage.immagini.s3.access-key-id:}") String accessKeyId,
            @Value("${app.storage.immagini.s3.secret-access-key:}") String secretAccessKey,
            @Value("${app.storage.immagini.s3.region:auto}") String regione) {

        // Fail-fast e non degrado silenzioso: con lo storage esterno attivo ma mal
        // configurato, *ogni* upload e ogni lettura fallirebbero a runtime. Meglio non
        // partire, con un messaggio che dice quale valore manca.
        richiedi(endpoint, "app.storage.immagini.s3.endpoint");
        richiedi(bucket, "app.storage.immagini.s3.bucket");
        richiedi(accessKeyId, "app.storage.immagini.s3.access-key-id");
        richiedi(secretAccessKey, "app.storage.immagini.s3.secret-access-key");
        verificaEndpointSenzaBucket(endpoint, bucket);

        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(regione))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        ArchivioImmagini archivio = new ArchivioS3(client, bucket, endpoint);
        log.info("Immagini archiviate su {}", archivio.descrizione());
        return archivio;
    }

    /**
     * L'endpoint deve essere la sola radice del servizio, senza il bucket.
     *
     * Vale la pena controllarlo perche' la dashboard di Cloudflare R2 mostra, sotto "S3
     * API", proprio l'URL completo di bucket, ed e' quello che viene naturale copiare. Con
     * l'indirizzamento path-style il bucket viene aggiunto dal client: se e' gia' presente
     * nell'endpoint finisce due volte, e gli oggetti vengono scritti sotto un prefisso in
     * piu' ({@code travelapp-photo/2026/08/...} invece di {@code 2026/08/...}).
     *
     * Il guaio e' che non si manifesta come errore: scrittura, lettura e cancellazione
     * usano tutte lo stesso prefisso sbagliato e sembrano funzionare. A non tornare sono le
     * chiavi salvate sul database, che non corrispondono a quelle reali nel bucket.
     */
    private static void verificaEndpointSenzaBucket(String endpoint, String bucket) {
        String normalizzato = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;

        if (normalizzato.endsWith("/" + bucket)) {
            throw new IllegalStateException(
                    "app.storage.immagini.s3.endpoint non deve contenere il nome del bucket:"
                            + " il client lo aggiunge da se'. Valore attuale: " + endpoint
                            + " — va usato: " + normalizzato.substring(0, normalizzato.length() - bucket.length() - 1));
        }
    }

    private static void richiedi(String valore, String nomeProperty) {
        if (valore == null || valore.isBlank()) {
            throw new IllegalStateException(
                    "app.storage.immagini.tipo=s3 richiede " + nomeProperty
                            + ", che non e' valorizzata. Impostare la variabile d'ambiente corrispondente"
                            + " oppure riportare app.storage.immagini.tipo a 'filesystem'.");
        }
    }
}
