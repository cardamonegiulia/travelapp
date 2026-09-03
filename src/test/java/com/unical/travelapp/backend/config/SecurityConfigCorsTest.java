package com.unical.travelapp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void laConfigurazioneCorsUsaUnAllowListEsplicitaSenzaCredenziali() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", List.of("https://app.travelapp.com"));

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/itinerari");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration.getAllowedOrigins()).containsExactly("https://app.travelapp.com");
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
