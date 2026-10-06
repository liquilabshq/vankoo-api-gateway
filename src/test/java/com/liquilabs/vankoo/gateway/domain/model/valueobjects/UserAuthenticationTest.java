package com.liquilabs.vankoo.gateway.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAuthenticationTest {

    @Test
    void keepsTheIdentityTakenFromTheToken() {
        var authentication = new UserAuthentication("user-123", "sofia@vankoo.pe", List.of("ROLE_INVESTOR"));

        assertEquals("user-123", authentication.userId());
        assertEquals("sofia@vankoo.pe", authentication.email());
        assertEquals(List.of("ROLE_INVESTOR"), authentication.roles());
    }

    @Test
    void rejectsAMissingUserId() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserAuthentication(null, "sofia@vankoo.pe", List.of()));
    }

    @Test
    void rejectsAMissingEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new UserAuthentication("user-123", null, List.of()));
    }
}
