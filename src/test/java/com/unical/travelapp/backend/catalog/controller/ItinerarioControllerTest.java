package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.ItinerarioDTO;
import com.unical.travelapp.backend.catalog.dto.ItinerarioRequestDTO;
import com.unical.travelapp.backend.catalog.entity.Itinerario;
import com.unical.travelapp.backend.catalog.mapper.ItinerarioMapper;
import com.unical.travelapp.backend.catalog.service.ItinerarioService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
import com.unical.travelapp.backend.experience.models.DTO.ValutazioneMediaDTO;
import com.unical.travelapp.backend.experience.services.RecensioneService;
import com.unical.travelapp.backend.identity.entity.Utente;
import com.unical.travelapp.backend.identity.service.UtenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItinerarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ItinerarioService itinerarioService;

    @Mock
    private ItinerarioMapper itinerarioMapper;

    @Mock
    private UtenteService utenteService;

    @Mock
    private AuditLogger auditLogger;

    // Il controller chiede le valutazioni per mostrare la media delle stelle sulle anteprime
    @Mock
    private RecensioneService recensioneService;

    @InjectMocks
    private ItinerarioController itinerarioController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(itinerarioController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/itinerari - Deve rispondere 200 OK con lista paginata")
    void testGetAllItinerari() throws Exception {
        Itinerario itinerario = new Itinerario();
        itinerario.setId(1L);
        itinerario.setTitolo("Tour Amalfi");

        ItinerarioDTO itinerarioDTO = new ItinerarioDTO();
        itinerarioDTO.setId(1L);
        itinerarioDTO.setTitolo("Tour Amalfi");

        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<Itinerario> page = new PageImpl<>(List.of(itinerario), pageRequest, 1);

        when(itinerarioService.getAllItinerari(any(Pageable.class))).thenReturn(page);
        when(recensioneService.getValutazioni(any())).thenReturn(Map.of());
        when(itinerarioMapper.toDTO(any(Itinerario.class), any())).thenReturn(itinerarioDTO);

        mockMvc.perform(get("/api/itinerari")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/itinerari/{id} - Deve rispondere 200 OK")
    void testGetItinerarioByIdSuccess() throws Exception {
        Itinerario itinerario = new Itinerario();
        itinerario.setId(1L);
        ItinerarioDTO dto = new ItinerarioDTO();
        dto.setId(1L);

        when(itinerarioService.getItinerarioById(eq(1L))).thenReturn(Optional.of(itinerario));
        when(recensioneService.getValutazione(eq(1L))).thenReturn(ValutazioneMediaDTO.NESSUNA);
        when(itinerarioMapper.toDTO(eq(itinerario), any())).thenReturn(dto);

        mockMvc.perform(get("/api/itinerari/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /api/itinerari - Creazione itinerario con salvataggio ed audit")
    void testCreateItinerarioSuccess() throws Exception {
        Itinerario entity = new Itinerario();
        entity.setId(10L);

        ItinerarioDTO responseDTO = new ItinerarioDTO();
        responseDTO.setId(10L);

        when(itinerarioMapper.fromRequest(any(ItinerarioRequestDTO.class))).thenReturn(entity);
        when(utenteService.getUtenteSessione()).thenReturn(new Utente());
        when(itinerarioService.saveItinerario(any(Itinerario.class))).thenReturn(entity);
        when(itinerarioMapper.toDTO(entity)).thenReturn(responseDTO);
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        String validPayload = """
            {
                "titolo": "Tour Amalfi",
                "descrizione": "Escursione guidata",
                "destinazionePrincipale": "Amalfi",
                "durataGiorni": 3,
                "maxPartecipanti": 15,
                "prezzoBase": 150.00
            }
            """;

        mockMvc.perform(post("/api/itinerari")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("PUT /api/itinerari/{id} - Aggiornamento itinerario riuscito (200 OK)")
    void testUpdateItinerarioSuccess() throws Exception {
        Itinerario updatedEntity = new Itinerario();
        updatedEntity.setId(1L);

        ItinerarioDTO responseDTO = new ItinerarioDTO();
        responseDTO.setId(1L);
        responseDTO.setTitolo("Tour Aggiornato");

        when(itinerarioMapper.fromRequest(any(ItinerarioRequestDTO.class))).thenReturn(updatedEntity);
        when(utenteService.getUtenteSessione()).thenReturn(new Utente());
        when(utenteService.isAdmin()).thenReturn(false);
        when(itinerarioService.updateItinerario(eq(1L), any(Itinerario.class), any(Utente.class), eq(false)))
                .thenReturn(updatedEntity);
        when(itinerarioMapper.toDTO(updatedEntity)).thenReturn(responseDTO);
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        String validPayload = """
            {
                "titolo": "Tour Aggiornato",
                "descrizione": "Nuova descrizione",
                "destinazionePrincipale": "Costiera Amalfitana",
                "durataGiorni": 4,
                "maxPartecipanti": 20,
                "prezzoBase": 200.00
            }
            """;

        mockMvc.perform(put("/api/itinerari/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/itinerari/{id} - Cancellazione itinerario riuscita (204 No Content)")
    void testDeleteItinerarioSuccess() throws Exception {
        Utente mockUtente = new Utente();
        when(utenteService.getUtenteSessione()).thenReturn(mockUtente);
        when(utenteService.isAdmin()).thenReturn(false);
        doNothing().when(itinerarioService).deleteItinerario(eq(1L), any(Utente.class), eq(false));
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/api/itinerari/1"))
                .andExpect(status().isNoContent());
    }
}