package com.unical.travelapp.backend.security.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public final class TestDatabase {

    private static final String IMMAGINE = "postgres:16-alpine";

    private static final boolean DOCKER_DISPONIBILE = rilevaDocker();

    private static volatile PostgreSQLContainer<?> container;

    private TestDatabase() {
    }

    public static void applica(DynamicPropertyRegistry registry) {
        if (DOCKER_DISPONIBILE) {
            PostgreSQLContainer<?> postgres = avvia();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.jpa.properties.hibernate.dialect",
                    () -> "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            registry.add("spring.datasource.url",
                    () -> "jdbc:h2:mem:travelapp;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.jpa.properties.hibernate.dialect",
                    () -> "org.hibernate.dialect.H2Dialect");
        }
    }

    public static boolean postgresReale() {
        return DOCKER_DISPONIBILE;
    }

    private static PostgreSQLContainer<?> avvia() {
        if (container == null) {
            synchronized (TestDatabase.class) {
                if (container == null) {
                    PostgreSQLContainer<?> nuovo = new PostgreSQLContainer<>(IMMAGINE)
                            .withDatabaseName("travelapp_test")
                            .withUsername("travelapp_test")
                            .withPassword("travelapp_test");
                    nuovo.start();
                    container = nuovo;
                }
            }
        }
        return container;
    }

    private static boolean rilevaDocker() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
