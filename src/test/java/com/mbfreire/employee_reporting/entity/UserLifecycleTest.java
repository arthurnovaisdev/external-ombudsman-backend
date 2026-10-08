package com.mbfreire.employee_reporting.entity;

import com.mbfreire.employee_reporting.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserLifecycleTest {

    @Test
    void prePersistInitializesSecurityDefaults() {

        User user =
                User.builder()
                        .name("Cliente Teste")
                        .username("cliente.teste")
                        .passwordHash("hash")
                        .role(Role.CLIENT)
                        .build();

        user.onCreate();

        assertTrue(
                user.isActive()
        );

        assertFalse(
                user.isPasswordChanged()
        );

        assertEquals(
                0L,
                user.getTokenVersion()
        );

        assertNotNull(
                user.getCreatedAt()
        );
    }
}