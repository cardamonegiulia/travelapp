package com.unical.travelapp.backend.catalog.controller;

import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaDTO;
import com.unical.travelapp.backend.catalog.dto.SingolaAttivitaRequestDTO;
import com.unical.travelapp.backend.catalog.entity.SingolaAttivita;
import com.unical.travelapp.backend.catalog.mapper.SingolaAttivitaMapper;
import com.unical.travelapp.backend.catalog.service.SingolaAttivitaService;
import com.unical.travelapp.backend.common.audit.AuditLogger;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
class SingolaAttivitaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SingolaAttivitaService attivitaService;

    @Mock
    private SingolaAttivitaMapper attivitaMapper;

    @Mock
    private UtenteService utenteService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private SingolaAttivitaController singolaAttivitaController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(singolaAttivitaController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/attivita - Restituisce 200 OK e lista paginata")
    void testGetAllAttivita() throws Exception {
        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setId(1L);

        SingolaAttivitaDTO dto = new SingolaAttivitaDTO();
        dto.setId(1L);

        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<SingolaAttivita> page = new PageImpl<>(List.of(attivita), pageRequest, 1);

        when(attivitaService.getAllAttivita(any(Pageable.class))).thenReturn(page);
        when(attivitaMapper.toDTO(any(SingolaAttivita.class))).thenReturn(dto);

        mockMvc.perform(get("/api/attivita")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/attivita/{id} - Restituisce 200 OK")
    void testGetAttivitaByIdSuccess() throws Exception {
        SingolaAttivita attivita = new SingolaAttivita();
        attivita.setId(1L);

        SingolaAttivitaDTO dto = new SingolaAttivitaDTO();
        dto.setId(1L);

        when(attivitaService.getAttivitaById(eq(1L))).thenReturn(Optional.of(attivita));
        when(attivitaMapper.toDTO(attivita)).thenReturn(dto);

        mockMvc.perform(get("/api/attivita/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("POST /api/attivita/con-sessioni - Crea attività con date e sessioni ricorrenti")
    void testCreateAttivitaConSessioni() throws Exception {
        SingolaAttivita entity = new SingolaAttivita();
        entity.setId(5L);

        SingolaAttivitaDTO responseDTO = new SingolaAttivitaDTO();
        responseDTO.setId(5L);

        when(attivitaMapper.fromRequest(any(SingolaAttivitaRequestDTO.class))).thenReturn(entity);
        when(utenteService.getUtenteSessione()).thenReturn(new Utente());
        when(attivitaService.saveAttivitaConSessioni(any(SingolaAttivita.class), any(LocalDate.class), any(LocalDate.class), anyList()))
                .thenReturn(entity);
        when(attivitaMapper.toDTO(entity)).thenReturn(responseDTO);
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        String validPayload = """
            {
                "titolo": "Snorkeling Guidato",
                "descrizione": "Escursione marina con attrezzatura inclusa",
                "luogo": "Tropea",
                "prezzo": 45.00,
                "durataMinuti": 120,
                "maxPartecipanti": 10
            }
            """;

        mockMvc.perform(post("/api/attivita/con-sessioni")
                        .param("inizio", "2026-09-01")
                        .param("fine", "2026-09-30")
                        .param("giorni", "1", "3", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L));
    }

    @Test
    @DisplayName("PUT /api/attivita/{id} - Aggiornamento attività riuscito (200 OK)")
    void testUpdateAttivitaSuccess() throws Exception {
        SingolaAttivita updatedEntity = new SingolaAttivita();
        updatedEntity.setId(1L);

        SingolaAttivitaDTO responseDTO = new SingolaAttivitaDTO();
        responseDTO.setId(1L);
        responseDTO.setTitolo("Snorkeling Aggiornato");

        when(attivitaMapper.fromRequest(any(SingolaAttivitaRequestDTO.class))).thenReturn(updatedEntity);
        when(utenteService.getUtenteSessione()).thenReturn(new Utente());
        when(utenteService.isAdmin()).thenReturn(false);
        when(attivitaService.updateAttivita(eq(1L), any(SingolaAttivita.class), any(Utente.class), eq(false)))
                .thenReturn(updatedEntity);
        when(attivitaMapper.toDTO(updatedEntity)).thenReturn(responseDTO);
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        String validPayload = """
            {
                "titolo": "Snorkeling Aggiornato",
                "descrizione": "Nuova descrizione con guida esperta",
                "luogo": "Capo Vaticano",
                "prezzo": 55.00,
                "durataMinuti": 150,
                "maxPartecipanti": 12
            }
            """;

        mockMvc.perform(put("/api/attivita/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/attivita/{id} - Cancellazione attività (204 No Content)")
    void testDeleteAttivitaSuccess() throws Exception {
        Utente mockUtente = new Utente();
        when(utenteService.getUtenteSessione()).thenReturn(mockUtente);
        when(utenteService.isAdmin()).thenReturn(false);
        doNothing().when(attivitaService).deleteAttivita(eq(1L), any(Utente.class), eq(false));
        doNothing().when(auditLogger).success(anyString(), anyString(), anyString());

        mockMvc.perform(delete("/api/attivita/1"))
                .andExpect(status().isNoContent());
    }
}