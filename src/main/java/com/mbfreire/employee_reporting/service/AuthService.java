package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.LoginRequestDTO;
import com.mbfreire.employee_reporting.dto.request.RegisterRequestDTO;
import com.mbfreire.employee_reporting.dto.response.LoginResponseDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.RateLimitExceededException;
import com.mbfreire.employee_reporting.repository.UserRepository;
import com.mbfreire.employee_reporting.security.JWTService;
import com.mbfreire.employee_reporting.security.RateLimitService;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {

        String username = normalizeUsername(dto.username());

        boolean allowed =
                rateLimitService.allowSensitiveIdentifier(
                        "login-account",
                        username,
                        10,
                        Duration.ofMinutes(15)
                );

        if (!allowed) {
            throw new RateLimitExceededException(
                    "Muitas tentativas de login. Aguarde antes de tentar novamente."
            );
        }

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                username,
                                dto.password()
                        )
                );

        if (!(authentication.getPrincipal()
                instanceof UserDetailsImpl userDetails)) {

            throw new IllegalStateException(
                    "Tipo de usuário inválido."
            );
        }

        User user = userDetails.getUser();

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponseDTO(
                token,
                user.getName(),
                user.getRole().name(),
                user.isPasswordChanged()
        );
    }

    @Transactional
    public void register(RegisterRequestDTO dto) {

        String username =
                normalizeUsername(dto.username());

        if (userRepository
                .findByUsername(username)
                .isPresent()) {

            throw new BusinessRuleException(
                    "Username já cadastrado no sistema."
            );
        }

        User user = User.builder()
                .name(dto.name().trim())
                .username(username)
                .contactEmail(
                        normalizeContactEmail(
                                dto.contactEmail()
                        )
                )
                .passwordHash(
                        passwordEncoder.encode(
                                dto.password()
                        )
                )
                .role(Role.CLIENT)
                .build();

        userRepository.save(user);
    }

    private String normalizeUsername(String username) {

        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeContactEmail(
            String contactEmail
    ) {

        if (contactEmail == null
                || contactEmail.isBlank()) {

            return null;
        }

        return contactEmail
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}