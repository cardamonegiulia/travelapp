package com.unical.travelapp.backend.security.fase3_risorse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.ServerJwkDiProva;
import com.unical.travelapp.backend.security.support.TestDatabase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.servlet.multipart.max-file-size=64KB",
        "spring.servlet.multipart.max-request-size=128KB"
})
class UploadLimiteRealeSecurityTest {

    private static final int OLTRE_IL_LIMITE = 256 * 1024;
    private static final int ENTRO_IL_LIMITE = 16 * 1024;

    private static final String CONFINE = "----confine-di-prova-travelapp";

    @LocalServerPort private int porta;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void configurazione(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> ServerJwkDiProva.istanza().jwkSetUri());
    }

    private String tokenOrganizzatore() {
        return ServerJwkDiProva.istanza().idp().token(
                TestJwt.ISSUER, List.of(TestJwt.CLIENT_ID), "sub-uploader",
                Map.of("realm_access", Map.of("roles", List.of("ORGANIZZATORE"))));
    }

    private byte[] corpoMultipart(int dimensione) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String intestazione = "--" + CONFINE + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"grande.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String coda = "\r\n--" + CONFINE + "--\r\n";

        buffer.writeBytes(intestazione.getBytes(StandardCharsets.UTF_8));
        buffer.writeBytes(new byte[dimensione]);
        buffer.writeBytes(coda.getBytes(StandardCharsets.UTF_8));
        return buffer.toByteArray();
    }

    private HttpResponse<String> inviaUpload(int dimensione) throws Exception {
        HttpRequest richiesta = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + "/api/itinerari"))
                .header("Authorization", "Bearer " + tokenOrganizzatore())
                .header("Content-Type", "multipart/form-data; boundary=" + CONFINE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpoMultipart(dimensione)))
                .build();

        return HttpClient.newHttpClient().send(richiesta, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void unUploadOltreIlLimiteVieneRifiutatoCon413InFormatoProblemDetail() throws Exception {
        HttpResponse<String> risposta = inviaUpload(OLTRE_IL_LIMITE);

        assertThat(risposta.statusCode())
                .as("superare il limite di upload e' un errore del client (413), non del server")
                .isEqualTo(413);

        assertThat(risposta.headers().firstValue("Content-Type").orElse(""))
                .contains("application/problem+json");

        NessunLeak.verifica(risposta.body());

        JsonNode corpo = objectMapper.readTree(risposta.body());
        assertThat(corpo.get("status").asInt()).isEqualTo(413);
        assertThat(corpo.has("title")).isTrue();
        assertThat(corpo.has("detail")).isTrue();
        assertThat(corpo.has("traceId")).isTrue();
        assertThat(corpo.get("type").asText()).startsWith("urn:travelapp:problem:");
    }

    @Test
    void unUploadOltreIlLimiteRichiedeComunqueLAutenticazione() throws Exception {
        HttpRequest senzaToken = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + "/api/itinerari"))
                .header("Content-Type", "multipart/form-data; boundary=" + CONFINE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpoMultipart(OLTRE_IL_LIMITE)))
                .build();

        HttpResponse<String> risposta = HttpClient.newHttpClient()
                .send(senzaToken, HttpResponse.BodyHandlers.ofString());

        assertThat(risposta.statusCode())
                .as("il limite non deve diventare un modo per saltare l'autenticazione")
                .isEqualTo(401);
    }

    @Test
    void gliHeaderDiSicurezzaSonoPresentiAnchheSullaRispostaDiErrore() throws Exception {
        HttpResponse<String> risposta = inviaUpload(OLTRE_IL_LIMITE);

        assertThat(risposta.headers().firstValue("X-Frame-Options")).contains("DENY");
        assertThat(risposta.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    }

    @Test
    void nessunCookieDiSessioneVieneEmessoDaUnContenitoreReale() throws Exception {
        HttpResponse<String> risposta = inviaUpload(ENTRO_IL_LIMITE);

        assertThat(risposta.headers().allValues("Set-Cookie"))
                .as("API stateless: nessun JSESSIONID deve essere emesso")
                .noneSatisfy(cookie -> assertThat(cookie).containsIgnoringCase("JSESSIONID"));
    }
}
