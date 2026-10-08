package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.ChangePasswordRequestDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSecurityTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {

        userRepository =
                mock(UserRepository.class);

        passwordEncoder =
                mock(PasswordEncoder.class);

        userService =
                new UserService(
                        userRepository,
                        passwordEncoder
                );
    }

    @Test
    void changePasswordIncrementsTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getPasswordHash()
        ).thenReturn(
                "old-password-hash"
        );

        when(
                passwordEncoder.matches(
                        "CurrentPassword123!",
                        "old-password-hash"
                )
        ).thenReturn(
                true
        );

        when(
                passwordEncoder.matches(
                        "NewPassword123!",
                        "old-password-hash"
                )
        ).thenReturn(
                false
        );

        when(
                passwordEncoder.encode(
                        "NewPassword123!"
                )
        ).thenReturn(
                "new-password-hash"
        );

        ChangePasswordRequestDTO dto =
                new ChangePasswordRequestDTO(
                        "CurrentPassword123!",
                        "NewPassword123!"
                );

        userService.changePassword(
                userId,
                dto
        );

        verify(
                user
        ).setPasswordHash(
                "new-password-hash"
        );

        verify(
                user
        ).setPasswordChanged(
                true
        );

        verify(
                user
        ).incrementTokenVersion();

        verify(
                userRepository
        ).save(
                user
        );
    }

    @Test
    void incorrectCurrentPasswordDoesNotIncrementTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getPasswordHash()
        ).thenReturn(
                "current-password-hash"
        );

        when(
                passwordEncoder.matches(
                        "WrongPassword123!",
                        "current-password-hash"
                )
        ).thenReturn(
                false
        );

        ChangePasswordRequestDTO dto =
                new ChangePasswordRequestDTO(
                        "WrongPassword123!",
                        "NewPassword123!"
                );

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                userService.changePassword(
                                        userId,
                                        dto
                                )
                );

        assertEquals(
                "A senha atual está incorreta.",
                error.getMessage()
        );

        verify(
                user,
                never()
        ).incrementTokenVersion();

        verify(
                userRepository,
                never()
        ).save(
                user
        );
    }

    @Test
    void samePasswordDoesNotIncrementTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.getPasswordHash()
        ).thenReturn(
                "current-password-hash"
        );

        when(
                passwordEncoder.matches(
                        "CurrentPassword123!",
                        "current-password-hash"
                )
        ).thenReturn(
                true
        );

        ChangePasswordRequestDTO dto =
                new ChangePasswordRequestDTO(
                        "CurrentPassword123!",
                        "CurrentPassword123!"
                );

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                userService.changePassword(
                                        userId,
                                        dto
                                )
                );

        assertEquals(
                "A nova senha não pode ser igual à senha atual.",
                error.getMessage()
        );

        verify(
                user,
                never()
        ).incrementTokenVersion();

        verify(
                userRepository,
                never()
        ).save(
                user
        );
    }

    @Test
    void deactivatingUserIncrementsTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        UUID adminId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.isActive()
        ).thenReturn(
                true
        );

        userService.setActiveStatus(
                userId,
                false,
                adminId
        );

        verify(
                user
        ).setActive(
                false
        );

        verify(
                user
        ).incrementTokenVersion();

        verify(
                userRepository
        ).save(
                user
        );
    }

    @Test
    void reactivatingUserIncrementsTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        UUID adminId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.isActive()
        ).thenReturn(
                false
        );

        userService.setActiveStatus(
                userId,
                true,
                adminId
        );

        verify(
                user
        ).setActive(
                true
        );

        verify(
                user
        ).incrementTokenVersion();

        verify(
                userRepository
        ).save(
                user
        );
    }

    @Test
    void unchangedActiveStatusDoesNotIncrementTokenVersion() {

        UUID userId =
                UUID.randomUUID();

        UUID adminId =
                UUID.randomUUID();

        User user =
                mock(User.class);

        when(
                userRepository.findById(userId)
        ).thenReturn(
                Optional.of(user)
        );

        when(
                user.isActive()
        ).thenReturn(
                true
        );

        userService.setActiveStatus(
                userId,
                true,
                adminId
        );

        verify(
                user,
                never()
        ).incrementTokenVersion();

        verify(
                userRepository,
                never()
        ).save(
                user
        );
    }

    @Test
    void adminCannotDeactivateOwnAccount() {

        UUID adminId =
                UUID.randomUUID();

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                userService.setActiveStatus(
                                        adminId,
                                        false,
                                        adminId
                                )
                );

        assertEquals(
                "Você não pode desativar a própria conta.",
                error.getMessage()
        );

        verify(
                userRepository,
                never()
        ).findById(
                adminId
        );
    }
}