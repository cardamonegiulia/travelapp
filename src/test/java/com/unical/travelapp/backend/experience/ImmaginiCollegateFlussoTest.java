package com.unical.travelapp.backend.experience;

import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.experience.models.Recensione;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Foto allegate a recensioni e itinerari: percorso completo attraverso la filter chain di
 * produzione (autenticazione, ruoli, ownership) e non solo la logica del service.
 */
@DisplayName("Immagini collegate a recensioni e itinerari")
class ImmaginiCollegateFlussoTest extends SecurityIntegrationTestBase {

    @Value("${app.storage.immagini.path}")
    private String cartellaStorage;

    private Utente organizzatore;
    private Itinerario itinerario;
    private Recensione recensione;

    @BeforeEach
    void datiDiPartenza() {
        organizzatore = utente(SUB_ORGANIZZATORE, Ruolo.ORGANIZZATORE);
        Utente autore = utente(SUB_UTENTE_A, Ruolo.VIAGGIATORE);
        utente(SUB_UTENTE_B, Ruolo.VIAGGIATORE);
        utente(SUB_ADMIN, Ruolo.ADMIN);

        itinerario = itinerario(organizzatore);
        recensione = recensione(autore, itinerario);
    }


    // --- recensioni -----------------------------------------------------------------------

    @Test
    @DisplayName("l'autore allega una foto alla propria recensione e la ritrova nella risposta")
    void autoreAllegaFotoAllaPropriaRecensione() throws Exception {
        MvcResult caricamento = mockMvc.perform(caricaSuRecensione(recensione.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(caricamento.getResponse().getStatus()).isEqualTo(201);
        NessunLeak.verifica(caricamento);
        assertThat(immagineRepository.count()).isEqualTo(1);

        // la foto compare sia nel sotto-elenco sia nel DTO della recensione
        mockMvc.perform(get("/api/recensioni/{id}/immagini", recensione.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contentType").value("image/png"));

        mockMvc.perform(get("/api/recensioni/{id}", recensione.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immagini.length()").value(1));
    }


    @Test
    @DisplayName("un altro viaggiatore non puo' allegare foto a una recensione non sua")
    void estraneoNonAllegaFotoAllaRecensioneAltrui() throws Exception {
        MvcResult risultato = mockMvc.perform(caricaSuRecensione(recensione.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_B, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
        NessunLeak.verifica(risultato);
        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("un file che non e' un'immagine viene rifiutato con 400 e non lascia nulla sul database")
    void fileNonImmagineRifiutato() throws Exception {
        MockMultipartFile falso = new MockMultipartFile("file", "innocua.png", "image/png",
                new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00});

        MvcResult risultato = mockMvc.perform(multipart("/api/recensioni/{id}/immagini", recensione.getId())
                .file(falso)
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(400);
        NessunLeak.verifica(risultato);
        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("cancellare la recensione cancella anche le sue foto, riga e file")
    void cancellareLaRecensioneCancellaLeFoto() throws Exception {
        mockMvc.perform(caricaSuRecensione(recensione.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isCreated());

        Path fileSuDisco = fileDellUnicaImmagine();
        assertThat(fileSuDisco).exists();

        mockMvc.perform(delete("/api/recensioni/{id}", recensione.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk());

        assertThat(immagineRepository.count()).isZero();
        assertThat(fileSuDisco).doesNotExist();
    }


    // --- itinerari ------------------------------------------------------------------------

    @Test
    @DisplayName("l'organizzatore proprietario allega una foto al proprio itinerario")
    void organizzatoreAllegaFotoAlProprioItinerario() throws Exception {
        MvcResult risultato = mockMvc.perform(caricaSuItinerario(itinerario.getId())
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(201);
        NessunLeak.verifica(risultato);

        mockMvc.perform(get("/api/itinerari/{id}", itinerario.getId())
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immagini.length()").value(1))
                .andExpect(jsonPath("$.immagini[0].url").value("/api/immagini/"
                        + immagineRepository.findAll().get(0).getId() + "/contenuto"));
    }


    @Test
    @DisplayName("un altro organizzatore riceve 404 sull'itinerario non suo, non 403")
    void organizzatoreEstraneoNonAllegaFoto() throws Exception {
        utente("sub-altro-organizzatore", Ruolo.ORGANIZZATORE);

        MvcResult risultato = mockMvc.perform(caricaSuItinerario(itinerario.getId())
                .with(TestJwt.conRuoliRealm("sub-altro-organizzatore", "ORGANIZZATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(404);
        NessunLeak.verifica(risultato);
        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("un viaggiatore non puo' allegare foto a un itinerario")
    void viaggiatoreNonAllegaFotoAgliItinerari() throws Exception {
        MvcResult risultato = mockMvc.perform(caricaSuItinerario(itinerario.getId())
                .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(403);
        assertThat(immagineRepository.count()).isZero();
    }


    @Test
    @DisplayName("una foto puo' essere rimossa solo dalla risorsa a cui appartiene davvero")
    void nonSiRimuoveUnaFotoDiUnAltraRisorsa() throws Exception {
        mockMvc.perform(caricaSuItinerario(itinerario.getId())
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isCreated());

        Long immagineId = immagineRepository.findAll().get(0).getId();

        // stessa immagine, ma indicata come se appartenesse alla recensione
        MvcResult risultato = mockMvc.perform(
                delete("/api/recensioni/{id}/immagini/{immagineId}", recensione.getId(), immagineId)
                        .with(TestJwt.conRuoliRealm(SUB_UTENTE_A, "VIAGGIATORE"))).andReturn();

        assertThat(risultato.getResponse().getStatus()).isEqualTo(404);
        assertThat(immagineRepository.count()).isEqualTo(1);
    }


    @Test
    @DisplayName("l'organizzatore rimuove una foto dal proprio itinerario: riga e file spariscono")
    void organizzatoreRimuoveLaPropriaFoto() throws Exception {
        mockMvc.perform(caricaSuItinerario(itinerario.getId())
                .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isCreated());

        Long immagineId = immagineRepository.findAll().get(0).getId();
        Path fileSuDisco = fileDellUnicaImmagine();

        mockMvc.perform(delete("/api/itinerari/{id}/immagini/{immagineId}", itinerario.getId(), immagineId)
                        .with(TestJwt.conRuoliRealm(SUB_ORGANIZZATORE, "ORGANIZZATORE")))
                .andExpect(status().isNoContent());

        assertThat(immagineRepository.count()).isZero();
        assertThat(fileSuDisco).doesNotExist();
    }


    // --- helper ---------------------------------------------------------------------------

    private MockMultipartHttpServletRequestBuilder caricaSuRecensione(Long recensioneId) throws IOException {
        return multipart("/api/recensioni/{id}/immagini", recensioneId).file(immaginePng());
    }

    private MockMultipartHttpServletRequestBuilder caricaSuItinerario(Long itinerarioId) throws IOException {
        return multipart("/api/itinerari/{id}/immagini", itinerarioId).file(immaginePng());
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
