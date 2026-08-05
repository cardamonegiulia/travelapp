package com.unical.travelapp.backend.security.support;

import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Controllo automatico anti-leak da applicare a OGNI risposta di errore prodotta dalla suite.
 *
 * <p>Fallisce se il body espone dettagli interni: nomi di classe/eccezione, package Java,
 * frammenti SQL o di vincoli DB, path del filesystem. Se qualcuno rimuovesse il
 * GlobalExceptionHandler e lasciasse trapelare l'errore grezzo di Spring, questi controlli
 * diventerebbero rossi.
 */
public final class NessunLeak {

    private static final List<String> FRAMMENTI_VIETATI = List.of(
            "exception",
            "at com.",
            "at org.",
            "com.unical.travelapp",
            "org.springframework",
            "org.hibernate",
            "jakarta.persistence",
            "java.lang.",
            "caused by",
            "stacktrace",
            "constraint",
            "psql",
            "jdbc",
            "hikari",
            "select ",
            "insert into",
            "update ",
            "delete from",
            "c:\\",
            "/home/",
            "/usr/",
            ".java:"
    );

    private NessunLeak() {
    }

    public static void verifica(MvcResult risultato) throws Exception {
        verifica(risultato.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    public static void verifica(String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        String minuscolo = body.toLowerCase(Locale.ROOT);
        for (String frammento : FRAMMENTI_VIETATI) {
            assertThat(minuscolo)
                    .as("il body di errore non deve esporre dettagli interni (\"%s\"): %s", frammento, body)
                    .doesNotContain(frammento);
        }
    }
}
