package com.unical.travelapp.backend.security.support;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Genera coppie di chiavi RSA a runtime e firma token JWT reali con Nimbus.
 *
 * <p>Serve ai test del decoder e dei validator (audience, issuer, firma, scadenza), che il
 * post-processor {@code jwt()} non puo' coprire perche' scavalca il JwtDecoder.
 * Nessuna chiave e' committata nel repository e nessun token reale viene usato.
 */
public final class RsaTokenFactory {

    private final KeyPair coppiaChiavi;

    public RsaTokenFactory() {
        try {
            KeyPairGenerator generatore = KeyPairGenerator.getInstance("RSA");
            generatore.initialize(2048);
            this.coppiaChiavi = generatore.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA non disponibile nella JVM di test", e);
        }
    }

    public RSAPublicKey chiavePubblica() {
        return (RSAPublicKey) coppiaChiavi.getPublic();
    }

    /** Token valido: firmato con questa chiave, non scaduto, con issuer/audience indicati. */
    public String token(String issuer, List<String> audience, String subject, Map<String, Object> claimAggiuntivi) {
        return firma(issuer, audience, subject, Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(600), claimAggiuntivi, coppiaChiavi);
    }

    /** Token con scadenza nel passato. */
    public String tokenScaduto(String issuer, List<String> audience, String subject) {
        return firma(issuer, audience, subject, Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600), Map.of(), coppiaChiavi);
    }

    /** Token firmato con una chiave diversa da quella pubblicata: la firma non deve verificare. */
    public String tokenConFirmaSbagliata(String issuer, List<String> audience, String subject) {
        RsaTokenFactory altraIdentita = new RsaTokenFactory();
        return firma(issuer, audience, subject, Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(600), Map.of(), altraIdentita.coppiaChiavi);
    }

    private static String firma(String issuer, List<String> audience, String subject,
                                Instant emesso, Instant scadenza,
                                Map<String, Object> claimAggiuntivi, KeyPair chiavi) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .issueTime(Date.from(emesso))
                .expirationTime(Date.from(scadenza))
                .jwtID("jti-" + emesso.toEpochMilli());

        if (audience != null) {
            claims.audience(audience);
        }
        claimAggiuntivi.forEach(claims::claim);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(header, claims.build());
        try {
            jwt.sign(new RSASSASigner((RSAPrivateKey) chiavi.getPrivate()));
        } catch (Exception e) {
            throw new IllegalStateException("firma del token di test fallita", e);
        }
        return jwt.serialize();
    }
}
