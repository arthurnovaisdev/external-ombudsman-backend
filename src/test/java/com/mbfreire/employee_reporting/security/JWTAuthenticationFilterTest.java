package com.mbfreire.employee_reporting.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JWTAuthenticationFilterTest {

    private JWTService jwtService;
    private CustomUserDetailsService userDetailsService;
    private ObjectMapper objectMapper;

    private JWTAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {

        jwtService =
                mock(JWTService.class);

        userDetailsService =
                mock(CustomUserDetailsService.class);

        objectMapper =
                mock(ObjectMapper.class);

        filterChain =
                mock(FilterChain.class);

        filter =
                new JWTAuthenticationFilter(
                        jwtService,
                        userDetailsService,
                        objectMapper
                );

        when(
                objectMapper.writeValueAsString(
                        any()
                )
        ).thenReturn(
                "{}"
        );
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutAuthorizationHeaderContinuesNormally()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/reports/mine"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );
    }

    @Test
    void requestWithoutBearerPrefixContinuesNormally()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/reports/mine"
                );

        request.addHeader(
                "Authorization",
                "Basic abc123"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );
    }

    @Test
    void validTokenAuthenticatesUser()
            throws Exception {

        UserDetails userDetails =
                mock(UserDetails.class);

        when(
                jwtService.extractUsername(
                        "valid-token"
                )
        ).thenReturn(
                "cliente.a"
        );

        when(
                userDetailsService.loadUserByUsername(
                        "cliente.a"
                )
        ).thenReturn(
                userDetails
        );

        when(
                userDetails.isEnabled()
        ).thenReturn(
                true
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
                jwtService.isTokenValid(
                        "valid-token",
                        userDetails
                )
        ).thenReturn(
                true
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(
                authentication
        );

        assertTrue(
                authentication.isAuthenticated()
        );

        assertSame(
                userDetails,
                authentication.getPrincipal()
        );

        verify(
                filterChain
        ).doFilter(
                request,
                response
        );
    }

    @Test
    void disabledUserIsRejected()
            throws Exception {

        UserDetails userDetails =
                mock(UserDetails.class);

        when(
                jwtService.extractUsername(
                        "valid-token"
                )
        ).thenReturn(
                "cliente.a"
        );

        when(
                userDetailsService.loadUserByUsername(
                        "cliente.a"
                )
        ).thenReturn(
                userDetails
        );

        when(
                userDetails.isEnabled()
        ).thenReturn(
                false
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verify(
                jwtService,
                never()
        ).isTokenValid(
                any(),
                any()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void oldTokenVersionIsRejected()
            throws Exception {

        UserDetails userDetails =
                mock(UserDetails.class);

        when(
                jwtService.extractUsername(
                        "old-token"
                )
        ).thenReturn(
                "cliente.a"
        );

        when(
                userDetailsService.loadUserByUsername(
                        "cliente.a"
                )
        ).thenReturn(
                userDetails
        );

        when(
                userDetails.isEnabled()
        ).thenReturn(
                true
        );

        when(
                jwtService.isTokenValid(
                        "old-token",
                        userDetails
                )
        ).thenReturn(
                false
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "old-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void expiredTokenIsRejected()
            throws Exception {

        ExpiredJwtException exception =
                mock(ExpiredJwtException.class);

        when(
                jwtService.extractUsername(
                        "expired-token"
                )
        ).thenThrow(
                exception
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "expired-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void malformedTokenIsRejected()
            throws Exception {

        when(
                jwtService.extractUsername(
                        "malformed-token"
                )
        ).thenThrow(
                new IllegalArgumentException(
                        "Invalid token"
                )
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "malformed-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    @Test
    void nonexistentUserIsRejected()
            throws Exception {

        when(
                jwtService.extractUsername(
                        "valid-token"
                )
        ).thenReturn(
                "usuario.inexistente"
        );

        when(
                userDetailsService.loadUserByUsername(
                        "usuario.inexistente"
                )
        ).thenThrow(
                new UsernameNotFoundException(
                        "Usuário não encontrado."
                )
        );

        MockHttpServletRequest request =
                requestWithBearer(
                        "valid-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertEquals(
                401,
                response.getStatus()
        );

        verifyNoInteractions(
                filterChain
        );
    }

    private MockHttpServletRequest requestWithBearer(
            String token
    ) {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/api/reports/mine"
                );

        request.addHeader(
                "Authorization",
                "Bearer " + token
        );

        return request;
    }
}