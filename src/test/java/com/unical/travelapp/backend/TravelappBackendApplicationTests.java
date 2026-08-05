package com.unical.travelapp.backend;

import com.unical.travelapp.backend.security.support.TestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Il datasource arriva da TestDatabase (Testcontainers Postgres, o H2 come fallback):
// senza di esso il contesto non partirebbe perche' application.properties si aspetta
// le variabili d'ambiente DB_URL/DB_USERNAME/DB_PASSWORD.
@SpringBootTest
@ActiveProfiles("test")
class TravelappBackendApplicationTests {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestDatabase.applica(registry);
    }

    @Test
    void contextLoads() {
    }

}
