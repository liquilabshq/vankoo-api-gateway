package com.liquilabs.vankoo.gateway.infrastructure.problems;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayProblemTest {

    @Test
    void unauthenticatedMapsToA401ProblemDetail() {
        var detail = GatewayProblem.UNAUTHENTICATED.toProblemDetail(URI.create("/iam/api/v1/users/me"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), detail.getStatus());
        assertEquals(URI.create("https://docs.vankoo.dev/errors/unauthenticated"), detail.getType());
        assertEquals("Unauthenticated", detail.getTitle());
        assertEquals(URI.create("/iam/api/v1/users/me"), detail.getInstance());
        assertEquals("unauthenticated", detail.getProperties().get(GatewayProblem.CODE_PROPERTY));
    }
}
