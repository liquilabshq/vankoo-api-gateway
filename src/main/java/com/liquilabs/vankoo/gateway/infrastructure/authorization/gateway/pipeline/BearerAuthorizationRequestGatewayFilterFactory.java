package com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.pipeline;

import com.liquilabs.vankoo.gateway.infrastructure.tokens.jwt.BearerTokenService;
import com.liquilabs.vankoo.gateway.infrastructure.authorization.gateway.enrichers.AuthenticationRequestEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BearerAuthorizationRequestGatewayFilterFactory extends AbstractGatewayFilterFactory<BearerAuthorizationRequestGatewayFilterFactory.Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerAuthorizationRequestGatewayFilterFactory.class);
    private final BearerTokenService tokenService;

    public BearerAuthorizationRequestGatewayFilterFactory(BearerTokenService tokenService) {
        super(Config.class);
        this.tokenService = tokenService;
    }

    public static class Config {
        // Configuration parameters could be added here if needed in the YAML
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = tokenService.getBearerTokenFrom(exchange.getRequest());
            if (token == null) {
                LOGGER.warn("Missing or invalid Authorization header");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
            if (!tokenService.validateToken(token)) {
                LOGGER.error("Token validation failed");
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
            return tokenService.extractAuthentication(token)
                    .map(auth -> {
                        // Log de inyección para saber que to' va bien
                        LOGGER.info("Successfully authenticated user: {} - Injecting identity headers", auth.userId());
                        var enrichedRequest = AuthenticationRequestEnricher
                                .enrichWithAuthentication(exchange.getRequest(), auth);
                        return chain.filter(exchange.mutate().request(enrichedRequest).build());
                    })
                    .orElseGet(() -> {
                        LOGGER.error("Failed to extract claims from token");
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    // Rescatamos el metodo auxiliar onError para mantener el código limpio
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}