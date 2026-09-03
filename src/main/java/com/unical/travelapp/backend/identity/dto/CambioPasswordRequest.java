package com.unical.travelapp.backend.identity.dto;

import lombok.Data;
import lombok.ToString;

@Data
public class CambioPasswordRequest {

    @ToString.Exclude
    @PasswordSicura
    private String nuovaPassword;
}
