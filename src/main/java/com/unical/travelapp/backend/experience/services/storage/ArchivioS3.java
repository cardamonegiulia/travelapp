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

/**
 * Immagini su object storage compatibile S3 — in particolare Cloudflare R2, ma la stessa
 * classe vale per MinIO, Backblaze B2 o S3 di AWS: cambia solo l'endpoint configurato.
 *
 * Il bucket va tenuto **privato**. I file continuano a essere serviti dall'endpoint
 * autenticato {@code GET /api/immagini/{id}/contenuto}: il backend legge da qui e
 * ristreamma al client, cosi' il controllo di accesso resta dove sta oggi e nessun URL
 * pubblico permanente circola per le immagini degli utenti. Un bucket pubblico
 * renderebbe ogni foto profilo leggibile da chiunque ne indovini la chiave.
 *
 * Il "percorso relativo" del database diventa la chiave dell'oggetto, senza trasformazioni:
 * lo schema non cambia passando da filesystem a R2.
 */
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
        // Diversamente dal filesystem, dove CREATE_NEW e' atomico, qui la verifica di
        // non-esistenza e la scrittura sono due chiamate distinte: fra le due esiste in
        // teoria una finestra di sovrascrittura. E' accettabile perche' la chiave contiene
        // un UUID generato al momento, mai un dato del client, quindi una collisione
        // richiederebbe un evento di probabilita' trascurabile e non provocabile da fuori.
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

            // InputStreamResource e non ByteArrayResource: il contenuto passa al client
            // mentre arriva, senza essere accumulato in memoria dal server. Si puo' leggere
            // una volta sola, ed e' esattamente l'uso che ne fa il controller.
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
            // DELETE su S3 e' idempotente: una chiave inesistente non e' un errore, che e'
            // il comportamento richiesto dall'interfaccia.
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
