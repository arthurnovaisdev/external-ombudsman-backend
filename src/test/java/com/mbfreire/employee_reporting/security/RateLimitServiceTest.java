package com.mbfreire.employee_reporting.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {

        rateLimitService =
                new RateLimitService();
    }

    @Test
    void allowsRequestsUntilLimitIsReached() {

        String key =
                "login:ip:127.0.0.1";

        assertTrue(
                rateLimitService.allow(
                        key,
                        3,
                        Duration.ofMinutes(1)
                )
        );

        assertTrue(
                rateLimitService.allow(
                        key,
                        3,
                        Duration.ofMinutes(1)
                )
        );

        assertTrue(
                rateLimitService.allow(
                        key,
                        3,
                        Duration.ofMinutes(1)
                )
        );

        assertFalse(
                rateLimitService.allow(
                        key,
                        3,
                        Duration.ofMinutes(1)
                )
        );
    }

    @Test
    void differentKeysHaveIndependentCounters() {

        assertTrue(
                rateLimitService.allow(
                        "login:ip:10.0.0.1",
                        1,
                        Duration.ofMinutes(1)
                )
        );

        assertFalse(
                rateLimitService.allow(
                        "login:ip:10.0.0.1",
                        1,
                        Duration.ofMinutes(1)
                )
        );

        assertTrue(
                rateLimitService.allow(
                        "login:ip:10.0.0.2",
                        1,
                        Duration.ofMinutes(1)
                )
        );
    }

    @Test
    void counterResetsAfterWindowExpires()
            throws InterruptedException {

        String key =
                "test-window";

        Duration window =
                Duration.ofMillis(30);

        assertTrue(
                rateLimitService.allow(
                        key,
                        1,
                        window
                )
        );

        assertFalse(
                rateLimitService.allow(
                        key,
                        1,
                        window
                )
        );

        Thread.sleep(
                60
        );

        assertTrue(
                rateLimitService.allow(
                        key,
                        1,
                        window
                )
        );
    }

    @Test
    void sensitiveIdentifierUsesSameBucketAfterTrimming() {

        assertTrue(
                rateLimitService.allowSensitiveIdentifier(
                        "password-reset",
                        " cliente.a ",
                        1,
                        Duration.ofMinutes(15)
                )
        );

        assertFalse(
                rateLimitService.allowSensitiveIdentifier(
                        "password-reset",
                        "cliente.a",
                        1,
                        Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void differentSensitiveNamespacesHaveIndependentCounters() {

        String identifier =
                "cliente.a";

        assertTrue(
                rateLimitService.allowSensitiveIdentifier(
                        "login",
                        identifier,
                        1,
                        Duration.ofMinutes(15)
                )
        );

        assertFalse(
                rateLimitService.allowSensitiveIdentifier(
                        "login",
                        identifier,
                        1,
                        Duration.ofMinutes(15)
                )
        );

        assertTrue(
                rateLimitService.allowSensitiveIdentifier(
                        "forgot-password",
                        identifier,
                        1,
                        Duration.ofMinutes(15)
                )
        );
    }

    @Test
    void rejectsBlankKey() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService.allow(
                                "   ",
                                1,
                                Duration.ofMinutes(1)
                        )
        );
    }

    @Test
    void rejectsZeroRequestLimit() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService.allow(
                                "test",
                                0,
                                Duration.ofMinutes(1)
                        )
        );
    }

    @Test
    void rejectsNegativeRequestLimit() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService.allow(
                                "test",
                                -1,
                                Duration.ofMinutes(1)
                        )
        );
    }

    @Test
    void rejectsZeroWindow() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService.allow(
                                "test",
                                1,
                                Duration.ZERO
                        )
        );
    }

    @Test
    void rejectsNegativeWindow() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService.allow(
                                "test",
                                1,
                                Duration.ofSeconds(-1)
                        )
        );
    }

    @Test
    void rejectsBlankSensitiveNamespace() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService
                                .allowSensitiveIdentifier(
                                        " ",
                                        "cliente.a",
                                        1,
                                        Duration.ofMinutes(1)
                                )
        );
    }

    @Test
    void rejectsBlankSensitiveIdentifier() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        rateLimitService
                                .allowSensitiveIdentifier(
                                        "login",
                                        " ",
                                        1,
                                        Duration.ofMinutes(1)
                                )
        );
    }
}