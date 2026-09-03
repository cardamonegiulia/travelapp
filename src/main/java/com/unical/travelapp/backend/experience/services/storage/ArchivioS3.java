package com.unical.travelapp.backend.experience.services.storage;

import com.unical.travelapp.backend.experience.exeption.ArchiviazioneImmagineFallita;
import com.unical.travelapp.backend.experience.exeption.ImmagineNonTrovata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;


public class ArchivioS3 implements ArchivioImmagini, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ArchivioS3.class);

    private final S3Client client;
    private final String bucket;
    private final String descrizioneEndpoint;

    public ArchivioS3(S3Client client, String bucket, String descrizioneEndpoint) {
        this.client = client;
        this.bucket = bucket;
        this.descrizioneEndpoint = descrizioneEndpoint;
    }

    @Override
    public void scrivi(String percorsoRelativo, byte[] contenuto) {
        if (esiste(percorsoRelativo)) {
            throw new ArchiviazioneImmagineFallita(
                    "Esiste gia' un oggetto con chiave " + percorsoRelativo);
        }

        try {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(percorsoRelativo)
                            .contentLength((long) contenuto.length)
                            .build(),
                    RequestBody.fromBytes(contenuto));

        } catch (S3Exception e) {
            log.error("Scrittura dell'immagine {} su object storage fallita", percorsoRelativo, e);
            throw new ArchiviazioneImmagineFallita("Scrittura del file sullo storage fallita", e);
        }
    }

    @Override
    public Resource leggi(String percorsoRelativo) {
        try {
            ResponseInputStream<GetObjectResponse> flusso = client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(percorsoRelativo)
                            .build());

            return new InputStreamResource(flusso);

        } catch (NoSuchKeyException e) {
            throw new ImmagineNonTrovata("File dell'immagine non presente sullo storage");
        } catch (S3Exception e) {
            log.error("Lettura dell'immagine {} da object storage fallita", percorsoRelativo, e);
            throw new ArchiviazioneImmagineFallita("Lettura del file dallo storage fallita", e);
        }
    }

    @Override
    public void elimina(String percorsoRelativo) {
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(percorsoRelativo)
                    .build());

        } catch (S3Exception e) {
            throw new ArchiviazioneImmagineFallita(
                    "Cancellazione del file " + percorsoRelativo + " fallita", e);
        }
    }

    @Override
    public String descrizione() {
        return "object storage S3 (bucket " + bucket + " su " + descrizioneEndpoint + ")";
    }

    @Override
    public void close() {
        client.close();
    }

    private boolean esiste(String chiave) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(chiave).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
