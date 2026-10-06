package com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.pipeline;

import com.liquilabs.vankoo.gateway.domain.model.valueobjects.UserAuthentication;
import com.liquilabs.vankoo.gateway.infrastructure.problems.ProblemDetailWriter;
import com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.BearerTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test, no Spring context: the filter is applied to a mock exchange,
 * the token service is mocked and the chain only records what it receives.
 */
@ExtendWith(MockitoExtension.class)
class BearerAuthorizationRequestGatewayFilterFactoryTest {

    @Mock
    private BearerTokenService tokenService;

    private GatewayFilter filter;
    private ServerWebExchange forwarded;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        var factory = new BearerAuthorizationRequestGatewayFilterFactory(
                tokenService, new ProblemDetailWriter(JsonMapper.builder().build()));
        filter = factory.apply(new BearerAuthorizationRequestGatewayFilterFactory.Config());
        chain = exchange -> {
            forwarded = exchange;
            return Mono.empty();
        };
    }

    @Test
    void aValidTokenIsForwardedWithTheIdentityHeaders() {
        var exchange = exchangeWithAuthorization("Bearer good-token");
        when(tokenService.getBearerTokenFrom(any())).thenReturn("good-token");
        when(tokenService.validateToken("good-token")).thenReturn(true);
        when(tokenService.extractAuthentication("good-token")).thenReturn(Optional.of(
                new UserAuthentication("user-123", "carlos@vankoo.pe", List.of("ROLE_MYPE"))));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        var headers = forwarded.getRequest().getHeaders();
        assertEquals("user-123", headers.getFirst("X-User-Id"));
        assertEquals("carlos@vankoo.pe", headers.getFirst("X-User-Email"));
        assertEquals("ROLE_MYPE", headers.getFirst("X-User-Roles"));
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void aRequestWithoutTokenIsRejectedWithA401Problem() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/invoicing/api/v1/invoices"));
        when(tokenService.getBearerTokenFrom(any())).thenReturn(null);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertUnauthenticated(exchange);
        verify(tokenService, never()).validateToken(any());
    }

    @Test
    void anInvalidTokenIsRejectedWithA401Problem() {
        var exchange = exchangeWithAuthorization("Bearer forged-token");
        when(tokenService.getBearerTokenFrom(any())).thenReturn("forged-token");
        when(tokenService.validateToken("forged-token")).thenReturn(false);

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertUnauthenticated(exchange);
        verify(tokenService, never()).extractAuthentication(any());
    }

    @Test
    void aTokenWhoseClaimsCannotBeReadIsRejectedWithA401Problem() {
        var exchange = exchangeWithAuthorization("Bearer odd-token");
        when(tokenService.getBearerTokenFrom(any())).thenReturn("odd-token");
        when(tokenService.validateToken("odd-token")).thenReturn(true);
        when(tokenService.extractAuthentication("odd-token")).thenReturn(Optional.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertUnauthenticated(exchange);
    }

    private static MockServerWebExchange exchangeWithAuthorization(String header) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/invoicing/api/v1/invoices")
                .header(HttpHeaders.AUTHORIZATION, header));
    }

    private void assertUnauthenticated(MockServerWebExchange exchange) {
        var response = exchange.getResponse();
        assertNull(forwarded, "the request must not reach the downstream service");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("Bearer", response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE));
        StepVerifier.create(response.getBodyAsString())
                .assertNext(body -> assertEquals(true, body.contains("\"code\":\"unauthenticated\"")))
                .verifyComplete();
    }
}
