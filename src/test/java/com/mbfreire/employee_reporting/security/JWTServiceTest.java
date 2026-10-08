package com.mbfreire.employee_reporting.security;

import com.mbfreire.employee_reporting.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JWTServiceTest {

    private static final String SECRET =
            "01234567890123456789012345678901";

    private JWTService jwtService;

    private User user;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {

        jwtService =
                createJwtService(
                        SECRET,
                        60_000L
                );

        user =
                mock(User.class);

        userDetails =
                mock(UserDetailsImpl.class);

        when(
                userDetails.getUsername()
        ).thenReturn(
                "cliente.a"
        );

        doReturn(
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_CLIENT"
                        )
                )
        ).when(
                userDetails
        ).getAuthorities();

        when(
                userDetails.getUser()
        ).thenReturn(
                user
        );

        when(
                user.getTokenVersion()
        ).thenReturn(
                1L
        );
    }

    @Test
    void generatedTokenContainsUsername() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        String username =
                jwtService.extractUsername(
                        token
                );

        assertEquals(
                "cliente.a",
                username
        );
    }

    @Test
    void generatedTokenContainsTokenVersion() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        long tokenVersion =
                jwtService.extractTokenVersion(
                        token
                );

        assertEquals(
                1L,
                tokenVersion
        );
    }

    @Test
    void validTokenIsAccepted() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        userDetails
                );

        assertTrue(
                valid
        );
    }

    @Test
    void tokenVersionMismatchInvalidatesOldToken() {

        String oldToken =
                jwtService.generateToken(
                        userDetails
                );

        when(
                user.getTokenVersion()
        ).thenReturn(
                2L
        );

        boolean valid =
                jwtService.isTokenValid(
                        oldToken,
                        userDetails
                );

        assertFalse(
                valid
        );
    }

    @Test
    void tokenForAnotherUsernameIsInvalid() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        UserDetailsImpl anotherUser =
                mock(UserDetailsImpl.class);

        User anotherEntity =
                mock(User.class);

        when(
                anotherUser.getUsername()
        ).thenReturn(
                "cliente.b"
        );

        when(
                anotherUser.getUser()
        ).thenReturn(
                anotherEntity
        );

        when(
                anotherEntity.getTokenVersion()
        ).thenReturn(
                1L
        );

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        anotherUser
                );

        assertFalse(
                valid
        );
    }

    @Test
    void expiredTokenIsRejected() {

        JWTService expiredJwtService =
                createJwtService(
                        SECRET,
                        -1_000L
                );

        String expiredToken =
                expiredJwtService.generateToken(
                        userDetails
                );

        assertThrows(
                ExpiredJwtException.class,
                () ->
                        expiredJwtService.isTokenValid(
                                expiredToken,
                                userDetails
                        )
        );
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {

        JWTService anotherJwtService =
                createJwtService(
                        "abcdefghijklmnopqrstuvwxyz123456",
                        60_000L
                );

        String token =
                anotherJwtService.generateToken(
                        userDetails
                );

        assertThrows(
                JwtException.class,
                () ->
                        jwtService.extractUsername(
                                token
                        )
        );
    }

    @Test
    void genericUserDetailsCannotGenerateToken() {

        UserDetails genericUser =
                org.springframework.security.core.userdetails.User
                        .withUsername(
                                "cliente.a"
                        )
                        .password(
                                "password"
                        )
                        .roles(
                                "CLIENT"
                        )
                        .build();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        jwtService.generateToken(
                                genericUser
                        )
        );
    }

    @Test
    void genericUserDetailsCannotValidateToken() {

        String token =
                jwtService.generateToken(
                        userDetails
                );

        UserDetails genericUser =
                org.springframework.security.core.userdetails.User
                        .withUsername(
                                "cliente.a"
                        )
                        .password(
                                "password"
                        )
                        .roles(
                                "CLIENT"
                        )
                        .build();

        boolean valid =
                jwtService.isTokenValid(
                        token,
                        genericUser
                );

        assertFalse(
                valid
        );
    }

    private JWTService createJwtService(
            String secret,
            long expirationMs
    ) {

        JWTService service =
                new JWTService();

        ReflectionTestUtils.setField(
                service,
                "secret",
                secret
        );

        ReflectionTestUtils.setField(
                service,
                "expirationMs",
                expirationMs
        );

        return service;
    }
}