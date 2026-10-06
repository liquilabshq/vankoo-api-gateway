package com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.enrichers;

import com.liquilabs.vankoo.gateway.domain.model.valueobjects.UserAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationRequestEnricherTest {

    private final MockServerHttpRequest request = MockServerHttpRequest.get("/finance/api/v1/accounts").build();

    @Test
    void injectsTheIdentityHeadersForTheDownstreamService() {
        var auth = new UserAuthentication("user-123", "sofia@vankoo.pe", List.of("ROLE_INVESTOR", "ROLE_USER"));

        var headers = AuthenticationRequestEnricher.enrichWithAuthentication(request, auth).getHeaders();

        assertEquals("user-123", headers.getFirst("X-User-Id"));
        assertEquals("sofia@vankoo.pe", headers.getFirst("X-User-Email"));
        assertEquals("ROLE_INVESTOR,ROLE_USER", headers.getFirst("X-User-Roles"));
    }

    @Test
    void sendsAnEmptyRolesHeaderWhenTheTokenHasNoRoles() {
        var auth = new UserAuthentication("user-123", "sofia@vankoo.pe", List.of());

        var headers = AuthenticationRequestEnricher.enrichWithAuthentication(request, auth).getHeaders();

        assertEquals("", headers.getFirst("X-User-Roles"));
    }

    @Test
    void dropsBlankAndNullRoles() {
        var auth = new UserAuthentication("user-123", "sofia@vankoo.pe", Arrays.asList("ROLE_INVESTOR", " ", null));

        var headers = AuthenticationRequestEnricher.enrichWithAuthentication(request, auth).getHeaders();

        assertEquals("ROLE_INVESTOR", headers.getFirst("X-User-Roles"));
    }

    @Test
    void keepsTheOriginalPath() {
        var auth = new UserAuthentication("user-123", "sofia@vankoo.pe", List.of());

        var enriched = AuthenticationRequestEnricher.enrichWithAuthentication(request, auth);

        assertEquals("/finance/api/v1/accounts", enriched.getPath().value());
    }
}
