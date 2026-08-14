package com.unical.travelapp.backend.security.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Mini server HTTP locale che pubblica la JWK set della chiave RSA generata a runtime.
 *
 * <p>Serve a collaudare il percorso di autenticazione COMPLETO senza toccare una riga di
 * configurazione di produzione: il {@code NimbusJwtDecoder} dell'applicazione scarica
 * davvero le chiavi, verifica davvero la firma e applica davvero i validator di issuer,
 * audience e scadenza. Sostituisce solo l'indirizzo da cui prendere le chiavi pubbliche,
 * che e' esattamente il ruolo di Keycloak.
 *
 * <p>Non contiene alcuna credenziale: la coppia di chiavi nasce e muore nella JVM di test.
 */
public final class ServerJwkDiProva {

    private static ServerJwkDiProva istanza;

    private final RsaTokenFactory idp = new RsaTokenFactory();
    private final HttpServer server;

    private ServerJwkDiProva() {
        try {
            String jwkSet = new JWKSet(new RSAKey.Builder(idp.chiavePubblica())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID("chiave-di-test")
                    .build()).toString();
            byte[] corpo = jwkSet.getBytes(StandardCharsets.UTF_8);

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/certs", scambio -> {
                scambio.getResponseHeaders().add("Content-Type", "application/json");
                scambio.sendResponseHeaders(200, corpo.length);
                try (OutputStream out = scambio.getResponseBody()) {
                    out.write(corpo);
                }
            });
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("impossibile avviare il server JWK di prova", e);
        }
    }

    public static synchronized ServerJwkDiProva istanza() {
        if (istanza == null) {
            istanza = new ServerJwkDiProva();
        }
        return istanza;
    }

    public RsaTokenFactory idp() {
        return idp;
    }

    public String jwkSetUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/certs";
    }
}
