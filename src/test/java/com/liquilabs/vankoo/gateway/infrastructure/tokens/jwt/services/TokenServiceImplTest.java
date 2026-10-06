package com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceImplTest {

    private static final String SECRET = "a-test-signing-secret-of-at-least-32-bytes";
    private static final String OTHER_SECRET = "another-signing-secret-of-at-least-32-bytes";
    private static final long DAY = 86_400_000L;

    private TokenServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TokenServiceImpl();
        ReflectionTestUtils.setField(service, "secret", SECRET);
    }

    @Test
    void aTokenIssuedByIamIsValid() {
        assertTrue(service.validateToken(tokenSignedWith(SECRET, "user-123", "carlos@vankoo.pe", List.of("ROLE_MYPE"), DAY)));
    }

    @Test
    void aTokenSignedWithAnotherKeyIsInvalid() {
        assertFalse(service.validateToken(tokenSignedWith(OTHER_SECRET, "user-123", "carlos@vankoo.pe", List.of(), DAY)));
    }

    @Test
    void anExpiredTokenIsInvalid() {
        assertFalse(service.validateToken(tokenSignedWith(SECRET, "user-123", "carlos@vankoo.pe", List.of(), -DAY)));
    }

    @Test
    void aMalformedTokenIsInvalid() {
        assertFalse(service.validateToken("not-a-jwt"));
    }

    @Test
    void extractsUserIdEmailAndRolesFromTheClaims() {
        var token = tokenSignedWith(SECRET, "user-123", "carlos@vankoo.pe", List.of("ROLE_MYPE", "ROLE_USER"), DAY);

        var authentication = service.extractAuthentication(token).orElseThrow();

        assertEquals("user-123", authentication.userId());
        assertEquals("carlos@vankoo.pe", authentication.email());
        assertEquals(List.of("ROLE_MYPE", "ROLE_USER"), authentication.roles());
    }

    @Test
    void aTokenWithoutRolesYieldsAnEmptyRoleList() {
        var token = tokenSignedWith(SECRET, "user-123", "carlos@vankoo.pe", null, DAY);

        assertEquals(List.of(), service.extractAuthentication(token).orElseThrow().roles());
    }

    @Test
    void aTokenWithoutEmailYieldsNoAuthentication() {
        var token = tokenSignedWith(SECRET, "user-123", null, List.of("ROLE_MYPE"), DAY);

        assertTrue(service.extractAuthentication(token).isEmpty());
    }

    @Test
    void anInvalidTokenYieldsNoAuthentication() {
        assertTrue(service.extractAuthentication("not-a-jwt").isEmpty());
    }

    @Test
    void extractsTheBearerTokenFromTheAuthorizationHeader() {
        var request = MockServerHttpRequest.get("/investment/api/v1/auctions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi")
                .build();

        assertEquals("abc.def.ghi", service.getBearerTokenFrom(request));
    }

    @Test
    void returnsNullWhenTheHeaderIsMissingOrNotBearer() {
        var withoutHeader = MockServerHttpRequest.get("/investment/api/v1/auctions").build();
        var basicAuth = MockServerHttpRequest.get("/investment/api/v1/auctions")
                .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                .build();

        assertNull(service.getBearerTokenFrom(withoutHeader));
        assertNull(service.getBearerTokenFrom(basicAuth));
    }

    private static String tokenSignedWith(String secret, String subject, String email, List<String> roles, long ttlMillis) {
        var now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(new Date(now - DAY * 2))
                .expiration(new Date(now + ttlMillis))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
