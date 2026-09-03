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

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.storage.immagini.tipo", havingValue = "s3")
    public ArchivioImmagini archivioS3(
            @Value("${app.storage.immagini.s3.endpoint:}") String endpoint,
            @Value("${app.storage.immagini.s3.bucket:}") String bucket,
            @Value("${app.storage.immagini.s3.access-key-id:}") String accessKeyId,
            @Value("${app.storage.immagini.s3.secret-access-key:}") String secretAccessKey,
            @Value("${app.storage.immagini.s3.region:auto}") String regione) {

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
