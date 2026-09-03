package com.unical.travelapp.backend.identity;

import com.unical.travelapp.backend.identity.entity.Ruolo;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.security.support.NessunLeak;
import com.unical.travelapp.backend.security.support.SecurityIntegrationTestBase;
import com.unical.travelapp.backend.security.support.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Foto profilo dell'utente autenticato")
class FotoProfiloFlussoTest extends SecurityIntegrationTestBase {

    @Value("${app.storage.immagini.path}")
    private String cartellaStorage;

    private Utente viaggiatore;

    @BeforeEach
    void datiDiPartenza() {
        viaggiatore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
    }


    @Test
    @DisplayName("l'utente carica la propria foto e la ritrova nel profilo")
    void utenteImpostaLaPropriaFoto() throws Exception {
        MvcResult caricamento = mockMvc.perform(impostaFoto(SUB_UTENTE_A)).andReturn();

        assertThat(caricamento.getResponse().getStatus()).isEqualTo(200);
        NessunLeak.verifica(caricamento);
        assertThat(immagineRepository.count()).isEqualTo(1);

        Long immagineId = immagineRepository.findAll().get(0).getId();
        assertThat(caricamento.getResponse().getContentAsString())
                .contains("/api/immagini/" + immagineId + "/contenuto");

        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoProfilo.id").value(immagineId))
                .andExpect(jsonPath("$.fotoProfilo.contentType").value("image/png"))
                .andExpect(jsonPath("$.fotoProfilo.proprietarioId").value(viaggiatore.getId()));
    }


    @Test
    @DisplayName("chi non e' autenticato non puo' impostare alcuna foto")
    void anonimoNonImpostaFoto() throws Exception {
        MvcResult risultato = mockMvc.perform(
                multipart("/api/utenti/me/foto-profilo").file(immaginePng()).with(comePut())).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(401);
        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("la seconda foto sostituisce la prima: riga e file precedenti spariscono")
    void laFotoNuovaSostituisceLaVecchia() throws Exception {
        mockMvc.perform(impostaFoto(SUB_UTENTE_A)).andExpect(status().isOk());

        Long primaId = immagineRepository.findAll().get(0).getId();
        Path primoFile = fileDellUnicaImmagine();

        mockMvc.perform(impostaFoto(SUB_UTENTE_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoProfilo.id").value(not(primaId.intValue())));

        assertThat(immagineRepository.count()).isEqualTo(1);
        assertThat(immagineRepository.existsById(primaId)).isFalse();
        assertThat(primoFile).doesNotExist();
    }


    @Test
    @DisplayName("un file che non e' un'immagine viene rifiutato e la foto precedente resta")
    void fileNonValidoNonToccaLaFotoEsistente() throws Exception {
        mockMvc.perform(impostaFoto(SUB_UTENTE_A)).andExpect(status().isOk());

        Long originaleId = immagineRepository.findAll().get(0).getId();
        Path originale = fileDellUnicaImmagine();

        MockMultipartFile falso = new MockMultipartFile(
                "file", "foto.png", "image/png", "questo non e' un PNG".getBytes());

        mockMvc.perform(multipart("/api/utenti/me/foto-profilo").file(falso)
                        .with(comePut())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isBadRequest());

        assertThat(immagineRepository.count()).isEqualTo(1);
        assertThat(immagineRepository.existsById(originaleId)).isTrue();
        assertThat(originale).exists();
    }


    @Test
    @DisplayName("l'utente rimuove la propria foto: riga e file spariscono")
    void utenteRimuoveLaPropriaFoto() throws Exception {
        mockMvc.perform(impostaFoto(SUB_UTENTE_A)).andExpect(status().isOk());

        Path fileSuDisco = fileDellUnicaImmagine();

        mockMvc.perform(delete("/api/utenti/me/foto-profilo")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNoContent());

        assertThat(immagineRepository.count()).isZero();
        assertThat(fileSuDisco).doesNotExist();

        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoProfilo").doesNotExist());
    }


    @Test
    @DisplayName("rimuovere una foto che non c'e' non e' un errore")
    void rimozioneSenzaFotoEIdempotente() throws Exception {
        mockMvc.perform(delete("/api/utenti/me/foto-profilo")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isNoContent());

        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("la foto di un utente non compare nel profilo di un altro")
    void laFotoRestaDelSuoProprietario() throws Exception {
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);

        mockMvc.perform(impostaFoto(SUB_UTENTE_A)).andExpect(status().isOk());

        mockMvc.perform(post("/api/utenti/me")
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoProfilo").doesNotExist());
    }


    private RequestBuilder impostaFoto(String keycloakSub) throws IOException {
        return multipart("/api/utenti/me/foto-profilo")
                .file(immaginePng())
                .with(comePut())
                .with(TestJwt.conRuoliRealm(keycloakSub, "VIAGGIATORE"));
    }

    private static RequestPostProcessor comePut() {
        return richiesta -> {
            richiesta.setMethod("PUT");
            return richiesta;
        };
    }

    private MockMultipartFile immaginePng() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB), "png", output);
        return new MockMultipartFile("file", "foto.png", "image/png", output.toByteArray());
    }

    private Path fileDellUnicaImmagine() {
        return Paths.get(cartellaStorage)
                .resolve(immagineRepository.findAll().get(0).getPercorsoRelativo())
                .normalize();
    }
}
