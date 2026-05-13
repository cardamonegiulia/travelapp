package com.unical.travelapp.backend.booking.controllers;

import com.unical.travelapp.backend.booking.entity.Pagamento;
import com.unical.travelapp.backend.booking.repositories.PagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagamenti")
public class PagamentoController {

    @Autowired //INIEZIONE DAL BIN CONTAINER QUANDO NE AVRA BISOGNO
    private PagamentoRepository pagamentoRepo;

    // esempio
    @GetMapping
    public List<Pagamento> getAllPagamenti() {
        return pagamentoRepo.findAll();
    }
}
