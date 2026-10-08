package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.RegisterRequestDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.repository.UserRepository;
import com.mbfreire.employee_reporting.security.JWTService;
import com.mbfreire.employee_reporting.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceRegistrationTest {

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
    void registerNormalizesUsernameAndCreatesClient() {

        RegisterRequestDTO dto =
                new RegisterRequestDTO(
                        "  Cliente de Teste  ",
                        "  Cliente.Teste  ",
                        "CLIENTE@EXAMPLE.COM",
                        "Senha123!"
                );

        when(
                userRepository.findByUsername(
                        "cliente.teste"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                passwordEncoder.encode(
                        "Senha123!"
                )
        ).thenReturn(
                "senha-hash"
        );

        authService.register(
                dto
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(
                userRepository
        ).save(
                userCaptor.capture()
        );

        User savedUser =
                userCaptor.getValue();

        assertEquals(
                "Cliente de Teste",
                savedUser.getName()
        );

        assertEquals(
                "cliente.teste",
                savedUser.getUsername()
        );

        assertEquals(
                "cliente@example.com",
                savedUser.getContactEmail()
        );

        assertEquals(
                "senha-hash",
                savedUser.getPasswordHash()
        );

        assertEquals(
                Role.CLIENT,
                savedUser.getRole()
        );

        verify(
                userRepository
        ).findByUsername(
                "cliente.teste"
        );

        verify(
                passwordEncoder
        ).encode(
                "Senha123!"
        );
    }

    @Test
    void duplicateUsernameIsRejectedAfterNormalization() {

        RegisterRequestDTO dto =
                new RegisterRequestDTO(
                        "Cliente",
                        "  CLIENTE.TESTE  ",
                        "cliente@example.com",
                        "Senha123!"
                );

        User existingUser =
                mock(User.class);

        when(
                userRepository.findByUsername(
                        "cliente.teste"
                )
        ).thenReturn(
                Optional.of(existingUser)
        );

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                authService.register(
                                        dto
                                )
                );

        assertEquals(
                "Username já cadastrado no sistema.",
                error.getMessage()
        );

        verify(
                passwordEncoder,
                never()
        ).encode(
                "Senha123!"
        );

        verify(
                userRepository,
                never()
        ).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void blankContactEmailIsStoredAsNull() {

        RegisterRequestDTO dto =
                new RegisterRequestDTO(
                        "Cliente Teste",
                        "cliente.teste",
                        "   ",
                        "Senha123!"
                );

        when(
                userRepository.findByUsername(
                        "cliente.teste"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                passwordEncoder.encode(
                        "Senha123!"
                )
        ).thenReturn(
                "senha-hash"
        );

        authService.register(
                dto
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(
                userRepository
        ).save(
                userCaptor.capture()
        );

        assertNull(
                userCaptor
                        .getValue()
                        .getContactEmail()
        );
    }

    @Test
    void contactEmailIsTrimmedAndLowercased() {

        RegisterRequestDTO dto =
                new RegisterRequestDTO(
                        "Cliente Teste",
                        "cliente.teste",
                        "  Cliente.Teste@Example.COM  ",
                        "Senha123!"
                );

        when(
                userRepository.findByUsername(
                        "cliente.teste"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                passwordEncoder.encode(
                        "Senha123!"
                )
        ).thenReturn(
                "senha-hash"
        );

        authService.register(
                dto
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(
                userRepository
        ).save(
                userCaptor.capture()
        );

        assertEquals(
                "cliente.teste@example.com",
                userCaptor
                        .getValue()
                        .getContactEmail()
        );
    }

    @Test
    void rawPasswordIsNeverStored() {

        RegisterRequestDTO dto =
                new RegisterRequestDTO(
                        "Cliente Teste",
                        "cliente.teste",
                        null,
                        "SenhaSecreta123!"
                );

        when(
                userRepository.findByUsername(
                        "cliente.teste"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                passwordEncoder.encode(
                        "SenhaSecreta123!"
                )
        ).thenReturn(
                "$2a$hash-seguro"
        );

        authService.register(
                dto
        );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        verify(
                userRepository
        ).save(
                userCaptor.capture()
        );

        assertEquals(
                "$2a$hash-seguro",
                userCaptor
                        .getValue()
                        .getPasswordHash()
        );
    }
}