package com.unical.travelapp.backend.catalog.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
@Data
@AllArgsConstructor
public class AttivitaExtraDTO {
    private Long id;
    private String titolo;
    private BigDecimal prezzoExtra;
}
