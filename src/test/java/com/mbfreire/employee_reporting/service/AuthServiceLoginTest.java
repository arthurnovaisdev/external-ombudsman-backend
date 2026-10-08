package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.LoginRequestDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.RateLimitExceededException;
import com.mbfreire.employee_reporting.repository.UserRepository;
import com.mbfreire.employee_reporting.security.JWTService;
import com.mbfreire.employee_reporting.security.RateLimitService;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceLoginTest {

    private RateLimitService rateLimitService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JWTService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        rateLimitService =
                mock(RateLimitService.class);

        userRepository =
                mock(UserRepository.class);

        passwordEncoder =
                mock(PasswordEncoder.class);

        authenticationManager =
                mock(AuthenticationManager.class);

        jwtService =
                mock(JWTService.class);

        authService =
                new AuthService(
                        rateLimitService,
                        userRepository,
                        passwordEncoder,
                        authenticationManager,
                        jwtService
                );
    }

    @Test
    void loginNormalizesUsernameBeforeAuthentication() {

        LoginRequestDTO dto =
                new LoginRequestDTO(
                        "  CLIENTE.TESTE  ",
                        "Senha123!"
                );

        User user =
                User.builder()
                        .name("Cliente Teste")
                        .username("cliente.teste")
                        .role(Role.CLIENT)
                        .active(true)
                        .passwordChanged(true)
                        .build();

        UserDetailsImpl userDetails =
                mock(UserDetailsImpl.class);

        Authentication authentication =
                mock(Authentication.class);

        when(
                rateLimitService.allowSensitiveIdentifier(
                        "login-account",
                        "cliente.teste",
                        10,
                        Duration.ofMinutes(15)
                )
        ).thenReturn(
                true
        );

        when(
                authenticationManager.authenticate(
                        org.mockito.ArgumentMatchers.any(
                                UsernamePasswordAuthenticationToken.class
                        )
                )
        ).thenReturn(
                authentication
        );

        when(
                authentication.getPrincipal()
        ).thenReturn(
                userDetails
        );

        when(
                userDetails.getUser()
        ).thenReturn(
                user
        );

        when(
                jwtService.generateToken(
                        userDetails
                )
        ).thenReturn(
                "jwt-token"
        );

        authService.login(
                dto
        );

        verify(
                rateLimitService
        ).allowSensitiveIdentifier(
                "login-account",
                "cliente.teste",
                10,
                Duration.ofMinutes(15)
        );

        verify(
                authenticationManager
        ).authenticate(
                argThat(authenticationToken ->
                        authenticationToken
                                .getPrincipal()
                                .equals("cliente.teste")
                                &&
                                authenticationToken
                                        .getCredentials()
                                        .equals("Senha123!")
                )
        );

        verify(
                jwtService
        ).generateToken(
                userDetails
        );
    }

    @Test
    void accountRateLimitBlocksAuthentication() {

        LoginRequestDTO dto =
                new LoginRequestDTO(
                        "CLIENTE.TESTE",
                        "Senha123!"
                );

        when(
                rateLimitService.allowSensitiveIdentifier(
                        "login-account",
                        "cliente.teste",
                        10,
                        Duration.ofMinutes(15)
                )
        ).thenReturn(
                false
        );

        assertThrows(
                RateLimitExceededException.class,
                () ->
                        authService.login(
                                dto
                        )
        );

        verify(
                authenticationManager,
                never()
        ).authenticate(
                org.mockito.ArgumentMatchers.any()
        );

        verify(
                jwtService,
                never()
        ).generateToken(
                org.mockito.ArgumentMatchers.any()
        );
    }
}