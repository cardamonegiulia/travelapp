package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagamenti")
@AllArgsConstructor
public class PagamentoController {

    private final PagamentoRepository pagamentoRepo;

    // Controller temporaneo usato solo per controllare i pagamenti durante lo sviluppo.
    // Al momento la logica reale del pagamento viene gestita dentro PrenotazioneController
    // tramite gli endpoint:
    // POST /api/prenotazioni/{id}/paga
    // POST /api/prenotazioni/{id}/annulla
    //
    // In futuro, se il pagamento diventerà un modulo autonomo, questo controller
    // andrà rifatto usando Service, DTO Response e Mapper.

}