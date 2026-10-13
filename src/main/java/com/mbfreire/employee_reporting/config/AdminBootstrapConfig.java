package com.mbfreire.employee_reporting.config;

import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.repository.UserRepository;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
@Profile("bootstrap-admin")
@Slf4j
public class AdminBootstrapConfig {

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Bean
    CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Validator validator,
            @Value("${admin.bootstrap.username}")
            String adminUsername,
            @Value("${admin.bootstrap.password}")
            String adminPassword,
            @Value("${admin.bootstrap.contact-email:}")
            String adminContactEmail
    ) {
        return args -> {
            String normalizedUsername = normalizeUsername(adminUsername);
            String contactEmail = normalizeContactEmail(
                    adminContactEmail,
                    validator
            );

            validateUsername(normalizedUsername);
            validatePassword(adminPassword);

            var existingUser = userRepository.findByUsername(
                    normalizedUsername
            );

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                if (user.getRole() != Role.ADMIN) {
                    throw new IllegalStateException(
                            "O username configurado para bootstrap "
                                    + "já pertence a um usuário que não é ADMIN."
                    );
                }

                if (contactEmail != null
                        && (user.getContactEmail() == null
                        || user.getContactEmail().isBlank())) {
                    user.setContactEmail(contactEmail);
                    userRepository.save(user);

                    log.info(
                            "E-mail de contato do administrador inicial atualizado."
                    );
                } else {
                    log.info(
                            "Bootstrap administrativo ignorado: "
                                    + "a conta administrativa já existe."
                    );
                }

                return;
            }

            User admin = User.builder()
                    .name("Administrador")
                    .username(normalizedUsername)
                    .contactEmail(contactEmail)
                    .passwordHash(
                            passwordEncoder.encode(adminPassword)
                    )
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);

            log.info(
                    "Conta administrativa inicial criada com sucesso."
            );
        };
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }

        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeContactEmail(
            String contactEmail,
            Validator validator
    ) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return null;
        }

        String normalized = contactEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!validator.validate(
                new ContactEmailCandidate(normalized)
        ).isEmpty()) {
            throw new IllegalStateException(
                    "ADMIN_CONTACT_EMAIL deve ser um e-mail válido "
                            + "com no máximo 150 caracteres."
            );
        }

        return normalized;
    }

    private void validateUsername(String username) {
        if (!username.matches("^[a-z0-9._-]{3,50}$")) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME deve possuir entre 3 e 50 "
                            + "caracteres e conter apenas letras, "
                            + "números, ponto, hífen ou underline."
            );
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD não foi configurada."
            );
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD deve possuir pelo menos "
                            + MIN_PASSWORD_LENGTH
                            + " caracteres."
            );
        }

        String normalized = password
                .trim()
                .toLowerCase(Locale.ROOT);

        if (normalized.equals("changeme123")
                || normalized.equals("password")
                || normalized.equals("admin123")
                || normalized.equals("123456")) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD é muito previsível."
            );
        }
    }

    private record ContactEmailCandidate(
            @Email
            @Size(max = 150)
            String value
    ) {}
}