package com.unical.travelapp.backend.security.trasversali;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scansione del repository: nessun segreto deve essere versionato.
 *
 * <p>Una credenziale committata resta nella storia di git anche dopo essere stata
 * rimossa, quindi il controllo va fatto prima del commit, non dopo.
 */
class NessunSegretoCommittatoTest {

    private static final Path RADICE = Path.of(".");

    private static final List<String> CARTELLE_ESCLUSE = List.of(
            "target", ".git", ".idea", "node_modules", "build", ".gradle", "logs");

    private static final List<Pattern> SCHEMI_SOSPETTI = List.of(
            // 1) segreto assegnato a una stringa letterale fra virgolette (JSON, codice)
            Pattern.compile("(?i)(password|passwd|secret|client[-_]?secret|api[-_]?key|token)"
                    + "[\"']?\\s*[:=]\\s*\"(?!\\$\\{)(?!\\{\\{)([^\"\\s$<>{}]{8,})\""),
            // 2) segreto assegnato in un file di configurazione senza virgolette.
            //    La chiave deve stare a inizio riga (eventualmente con un prefisso puntato
            //    come spring.datasource.password): cosi' "String token = metodo(...)" in
            //    codice Java non viene scambiato per una credenziale.
            //    Il ';' e' fra i caratteri esclusi per lo stesso motivo: una riga che termina
            //    con ';' e' un'istruzione Java (es. "this.clientSecret = clientSecret;"), e un
            //    valore Java non quotato e' sempre un identificatore, mai una credenziale
            //    letterale. Le credenziali scritte fra virgolette nel codice restano
            //    intercettate dallo schema 1, che di virgolette ne pretende.
            Pattern.compile("(?i)^\\s*[\\w.\\-]*"
                    + "(password|passwd|secret|client[-_]?secret|api[-_]?key|token)"
                    + "\\s*[:=]\\s*(?!\\$\\{)(?!\\{\\{)([^\\s\"'<>${}();]{8,})\\s*$"),
            // token JWT letterale
            Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\."),
            // chiave privata PEM
            Pattern.compile("-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----"),
            // 3) segreto in un array JSON: "secret" : [ "valore" ]. E' la forma che usano gli
            //    export di Keycloak per ogni valore di configurazione, e sfuggiva agli schemi
            //    1 e 2 perche' dopo i due punti trovano una parentesi quadra invece di una
            //    virgoletta. E' cosi' che le chiavi del realm sono restate versionate.
            Pattern.compile("(?i)\"(password|passwd|secret|client[-_]?secret|api[-_]?key|token"
                    + "|private[-_]?key)\"\\s*:\\s*\\[\\s*\"(?!\\$\\{)([^\"\\s]{8,})\""),
            // 4) chiave privata RSA in base64 senza intestazione PEM: e' come Keycloak
            //    esporta le chiavi del realm, quindi lo schema sul PEM non la vede. I due
            //    frammenti sono la firma ASN.1 di una RSAPrivateKey (PKCS#1) e dell'involucro
            //    PKCS#8: comparire in un file versionato non ha usi legittimi.
            //    Il secondo e' spezzato in due pezzi perche' e' un letterale, e scritto per
            //    esteso lo scanner segnalerebbe questa riga stessa.
            Pattern.compile("MII[A-Za-z0-9+/]{2,12}IBAAKCAQ|QIBADANBgkqhkiG9w0" + "BAQEFAASC")
    );

    /** Valori che compaiono nei file ma non sono segreti reali. */
    private static final List<String> FALSI_POSITIVI = List.of(
            "password}", "password:-", "changeit", "esempio", "example",
            "placeholder", "your-", "xxxxx", "********");

    private List<Path> fileVersionati() throws IOException {
        try (Stream<Path> percorsi = Files.walk(RADICE)) {
            return percorsi
                    .filter(Files::isRegularFile)
                    .filter(percorso -> {
                        String normalizzato = percorso.toString().replace('\\', '/');
                        return CARTELLE_ESCLUSE.stream().noneMatch(
                                escluso -> normalizzato.contains("/" + escluso + "/")
                                        || normalizzato.startsWith("./" + escluso + "/"));
                    })
                    .filter(percorso -> {
                        String nome = percorso.getFileName().toString().toLowerCase(Locale.ROOT);
                        return nome.endsWith(".java") || nome.endsWith(".properties")
                                || nome.endsWith(".yml") || nome.endsWith(".yaml")
                                || nome.endsWith(".xml") || nome.endsWith(".json")
                                || nome.endsWith(".md") || nome.endsWith(".env")
                                || nome.endsWith(".sh") || nome.endsWith(".bat");
                    })
                    .toList();
        }
    }

    @Test
    void nessunFileVersionatoContieneCredenzialiInChiaro() throws IOException {
        List<String> sospetti = new ArrayList<>();

        for (Path file : fileVersionati()) {
            String contenuto;
            try {
                contenuto = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException ignorato) {
                continue; // file binario o non leggibile come testo
            }

            String[] righe = contenuto.split("\n");
            for (int i = 0; i < righe.length; i++) {
                String riga = righe[i];
                String minuscola = riga.toLowerCase(Locale.ROOT);

                if (FALSI_POSITIVI.stream().anyMatch(minuscola::contains)) {
                    continue;
                }
                for (Pattern schema : SCHEMI_SOSPETTI) {
                    if (schema.matcher(riga).find()) {
                        sospetti.add(file + ":" + (i + 1) + " -> " + riga.trim());
                    }
                }
            }
        }

        assertThat(sospetti)
                .as("possibili segreti versionati")
                .isEmpty();
    }

    @Test
    void loScannerRiconosceDavveroUnSegreto() {
        // senza questa verifica lo scanner potrebbe essere verde perche' non trova nulla,
        // non perche' non c'e' nulla da trovare
        List<String> esempiDiSegreto = List.of(
                "client_secret=teYdBrYper9ym5i2PxsJ3lqBWp7lPlG1",
                "spring.datasource.password=SuperSegreta123",
                "\"api_key\": \"abcdef1234567890\"",
                // composti a runtime: scritti per esteso farebbero scattare lo scanner su
                // questo stesso file
                "-----BEGIN " + "RSA PRIVATE KEY-----",
                // le due forme usate dagli export di Keycloak, che prima passavano indenni
                "\"secret\" : [ \"" + "9dxvmxzcHUM5ACEwotHp" + "\" ]",
                "\"privateKey\" : [ \"MIIEow" + "IBAAKCAQ" + "EAoV4NTe\" ]");

        for (String esempio : esempiDiSegreto) {
            assertThat(SCHEMI_SOSPETTI).anySatisfy(schema ->
                    assertThat(schema.matcher(esempio).find())
                            .as("lo scanner deve riconoscere: %s", esempio)
                            .isTrue());
        }

        // e non deve segnalare i riferimenti a variabili o le chiamate di metodo
        List<String> nonSegreti = List.of(
                "spring.datasource.password=${DB_PASSWORD}",
                "\"value\": \"{{client_secret}}\"",
                "String token = idp().token(ISSUER, audience);",
                // assegnazione Java di un valore iniettato: il segreto sta nell'ambiente,
                // non nel sorgente
                "        this.clientSecret = clientSecret;",
                "    private final String clientSecret;");

        // ...ma un segreto scritto fra virgolette nel codice Java deve continuare a essere
        // segnalato: l'esclusione sopra vale solo per i valori non quotati
        assertThat(SCHEMI_SOSPETTI).anySatisfy(schema ->
                assertThat(schema.matcher("String clientSecret = \"7f3Ka9dQm2Zx8Lp0\";").find())
                        .as("un segreto letterale in codice Java deve restare rilevabile")
                        .isTrue());

        for (String innocuo : nonSegreti) {
            assertThat(SCHEMI_SOSPETTI).allSatisfy(schema ->
                    assertThat(schema.matcher(innocuo).find())
                            .as("non deve essere segnalato: %s", innocuo)
                            .isFalse());
        }
    }

    @Test
    void leCredenzialiDelDatabaseArrivanoDaVariabiliDAmbiente() throws IOException {
        String configurazione = Files.readString(
                Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);

        assertThat(configurazione)
                .as("url, utente e password del database non devono essere scritti nel file")
                .contains("${DB_URL}")
                .contains("${DB_USERNAME}")
                .contains("${DB_PASSWORD}");
    }

    @Test
    void nessunTokenDiProvaEVersionato() throws IOException {
        // token.json e simili vanno tenuti fuori dal repository
        try (Stream<Path> percorsi = Files.walk(RADICE)) {
            List<Path> fileDiToken = percorsi
                    .filter(Files::isRegularFile)
                    .filter(percorso -> {
                        String normalizzato = percorso.toString().replace('\\', '/');
                        return !normalizzato.contains("/target/") && !normalizzato.contains("/.git/");
                    })
                    .filter(percorso -> {
                        String nome = percorso.getFileName().toString().toLowerCase(Locale.ROOT);
                        return nome.equals("token.json") || nome.equals("tokens.json")
                                || nome.equals(".env") || nome.endsWith(".pem") || nome.endsWith(".p12")
                                || nome.endsWith(".jks") || nome.endsWith(".keystore");
                    })
                    .toList();

            assertThat(fileDiToken)
                    .as("nessun file di token, chiavi o keystore nel repository")
                    .isEmpty();
        }
    }

    @Test
    void iTestNonContengonoTokenRealiMaSoloChiaviGenerateARuntime() throws IOException {
        try (Stream<Path> percorsi = Files.walk(Path.of("src/test"))) {
            List<String> conTokenLetterali = new ArrayList<>();
            for (Path file : percorsi.filter(Files::isRegularFile).toList()) {
                String contenuto = Files.readString(file, StandardCharsets.UTF_8);
                // il prefisso e' composto a runtime per non far scattare questo stesso
                // controllo (e gli scanner di segreti) sul file del test
                String prefissoJwt = "eyJ" + "hbGciOi";
                if (contenuto.contains(prefissoJwt)) {
                    conTokenLetterali.add(file.toString());
                }
            }
            assertThat(conTokenLetterali)
                    .as("i test devono firmare i token a runtime, non incorporarne di reali")
                    .isEmpty();
        }
    }
}
