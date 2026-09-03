package com.unical.travelapp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecurityChecksConfigTest {

    private ProdSecurityChecksConfig configConIssuer(String issuerUri) {
        ProdSecurityChecksConfig config = new ProdSecurityChecksConfig();
        ReflectionTestUtils.setField(config, "issuerUri", issuerUri);
        return config;
    }

    @Test
    void fallisceConIssuerHttp() {
        assertThatThrownBy(() -> configConIssuer("http://keycloak.interno:8090/realms/travelapp").verificaIssuerHttps())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void passaConIssuerHttps() {
        assertThatCode(() -> configConIssuer("https://auth.travelapp.com/realms/travelapp").verificaIssuerHttps())
                .doesNotThrowAnyException();
    }
}
