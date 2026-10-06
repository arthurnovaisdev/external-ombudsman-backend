package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.ForgotPasswordRequestDTO;
import com.mbfreire.employee_reporting.dto.request.ResetPasswordRequestDTO;
import com.mbfreire.employee_reporting.entity.PasswordResetToken;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.repository.PasswordResetTokenRepository;
import com.mbfreire.employee_reporting.repository.UserRepository;
import com.mbfreire.employee_reporting.security.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int RESET_TOKEN_BYTES = 32;

    private final RateLimitService rateLimitService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Transactional
    public void requestPasswordReset(
            ForgotPasswordRequestDTO dto
    ) {

        String username =
                normalizeUsername(dto.username());

        boolean accountAllowed =
                rateLimitService.allowSensitiveIdentifier(
                        "forgot-password-account",
                        username,
                        3,
                        Duration.ofMinutes(30)
                );

        if (!accountAllowed) {
            return;
        }

        tokenRepository.deleteExpiredOrUsed(
                LocalDateTime.now()
        );

        Optional<User> optionalUser =
                userRepository.findByUsername(username);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user =
                optionalUser.get();

        if (!user.isActive()) {
            return;
        }

        if (user.getContactEmail() == null
                || user.getContactEmail().isBlank()) {

            return;
        }

        tokenRepository.deleteAllByUserId(
                user.getId()
        );

        String rawToken =
                generateSecureToken();

        String tokenHash =
                hashToken(rawToken);

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .tokenHash(tokenHash)
                        .user(user)
                        .expiryDate(
                                LocalDateTime.now()
                                        .plusHours(1)
                        )
                        .used(false)
                        .build();

        tokenRepository.saveAndFlush(
                resetToken
        );

        emailService.sendPasswordResetEmail(
                user.getContactEmail(),
                user.getName(),
                rawToken
        );
    }

    @Transactional
    public void resetPassword(
            ResetPasswordRequestDTO dto
    ) {

        String tokenHash =
                hashToken(
                        dto.token()
                );

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(() ->
                                new BusinessRuleException(
                                        "Token inválido ou expirado."
                                )
                        );

        if (resetToken.isUsed()
                || resetToken.isExpired()) {

            throw new BusinessRuleException(
                    "Token inválido ou expirado."
            );
        }

        User user =
                resetToken.getUser();

        if (!user.isActive()) {

            throw new BusinessRuleException(
                    "Não foi possível redefinir a senha."
            );
        }

        if (passwordEncoder.matches(
                dto.newPassword(),
                user.getPasswordHash()
        )) {

            throw new BusinessRuleException(
                    "A nova senha não pode ser igual à senha atual."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        dto.newPassword()
                )
        );

        user.setPasswordChanged(true);

        user.incrementTokenVersion();

        userRepository.save(
                user
        );

        tokenRepository.deleteAllByUserId(
                user.getId()
        );
    }

    private String generateSecureToken() {

        byte[] randomBytes =
                new byte[RESET_TOKEN_BYTES];

        secureRandom.nextBytes(
                randomBytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        randomBytes
                );
    }

    private String hashToken(
            String rawToken
    ) {

        if (rawToken == null
                || rawToken.isBlank()) {

            throw new BusinessRuleException(
                    "Token inválido ou expirado."
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 não está disponível.",
                    e
            );
        }
    }

    private String normalizeUsername(String username) {

        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}