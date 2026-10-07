package com.mbfreire.employee_reporting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;


    // PASSWORD RESET

    public void sendPasswordResetEmail(
            String toEmail,
            String toName,
            String resetToken
    ) {

        String resetLink =
                buildPasswordResetLink(
                        resetToken
                );

        String safeName =
                HtmlUtils.htmlEscape(
                        toName
                );

        String safeResetLink =
                HtmlUtils.htmlEscape(
                        resetLink
                );

        String htmlContent =
                """
                <html>
                    <body>
                        <h2>Olá, %s.</h2>

                        <p>
                            Recebemos uma solicitação para redefinir
                            a senha da sua conta na Ouvidoria MBFREIRE.
                        </p>

                        <p>
                            Clique no link abaixo para criar
                            uma nova senha:
                        </p>

                        <p>
                            <a href="%s">
                                <b>Redefinir minha senha</b>
                            </a>
                        </p>

                        <p>
                            Este link possui validade limitada
                            e só pode ser utilizado para uma
                            redefinição de senha.
                        </p>

                        <br>

                        <p>
                            Se você não fez esta solicitação,
                            apenas ignore este e-mail.
                        </p>
                    </body>
                </html>
                """
                        .formatted(
                                safeName,
                                safeResetLink
                        );

        String textContent =
                """
                Olá, %s.

                Recebemos uma solicitação para redefinir a senha
                da sua conta na Ouvidoria MBFREIRE.

                Acesse o link abaixo para criar uma nova senha:

                %s

                Se você não fez esta solicitação, ignore este e-mail.
                """
                        .formatted(
                                toName,
                                resetLink
                        );

        sendEmail(
                toEmail,
                toName,
                "Redefinição de Senha - Ouvidoria MBFREIRE",
                htmlContent,
                textContent
        );
    }


    // ADMIN - NEW REPORT

    public void sendNewReportAdminNotification(
            String toEmail,
            String protocol
    ) {

        String safeProtocol =
                HtmlUtils.htmlEscape(
                        protocol
                );

        String htmlContent =
                """
                <html>
                    <body>
                        <h2>Nova manifestação recebida</h2>

                        <p>
                            Uma nova manifestação foi registrada
                            na Ouvidoria MBFREIRE.
                        </p>

                        <p>
                            Protocolo:
                            <b>%s</b>
                        </p>

                        <p>
                            Acesse o painel administrativo
                            para consultar os detalhes.
                        </p>
                    </body>
                </html>
                """
                        .formatted(
                                safeProtocol
                        );

        String textContent =
                """
                Nova manifestação recebida.

                Uma nova manifestação foi registrada
                na Ouvidoria MBFREIRE.

                Protocolo: %s

                Acesse o painel administrativo
                para consultar os detalhes.
                """
                        .formatted(
                                protocol
                        );

        sendEmail(
                toEmail,
                "Administrador",
                "Nova manifestação - " + protocol,
                htmlContent,
                textContent
        );
    }


    // ADMIN - NEW CLIENT MESSAGE

    public void sendNewClientMessageAdminNotification(
            String toEmail,
            String protocol
    ) {

        String safeProtocol =
                HtmlUtils.htmlEscape(
                        protocol
                );

        String htmlContent =
                """
                <html>
                    <body>
                        <h2>Nova mensagem em uma manifestação</h2>

                        <p>
                            Um cliente enviou uma nova mensagem
                            na Ouvidoria MBFREIRE.
                        </p>

                        <p>
                            Protocolo:
                            <b>%s</b>
                        </p>

                        <p>
                            Acesse o painel administrativo
                            para consultar a conversa.
                        </p>
                    </body>
                </html>
                """
                        .formatted(
                                safeProtocol
                        );

        String textContent =
                """
                Nova mensagem em uma manifestação.

                Um cliente enviou uma nova mensagem
                na Ouvidoria MBFREIRE.

                Protocolo: %s

                Acesse o painel administrativo
                para consultar a conversa.
                """
                        .formatted(
                                protocol
                        );

        sendEmail(
                toEmail,
                "Administrador",
                "Nova mensagem - " + protocol,
                htmlContent,
                textContent
        );
    }


    // BREVO

    private void sendEmail(
            String toEmail,
            String toName,
            String subject,
            String htmlContent,
            String textContent
    ) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "api-key",
                apiKey
        );

        headers.set(
                "accept",
                "application/json"
        );

        Map<String, Object> body =
                Map.of(
                        "sender",
                        Map.of(
                                "name",
                                senderName,
                                "email",
                                senderEmail
                        ),

                        "to",
                        List.of(
                                Map.of(
                                        "email",
                                        toEmail,
                                        "name",
                                        toName
                                )
                        ),

                        "subject",
                        subject,

                        "htmlContent",
                        htmlContent,

                        "textContent",
                        textContent
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        restTemplate.postForEntity(
                apiUrl,
                request,
                String.class
        );
    }


    private String buildPasswordResetLink(
            String resetToken
    ) {

        if (frontendBaseUrl == null
                || frontendBaseUrl.isBlank()) {

            throw new IllegalStateException(
                    "A URL do frontend não foi configurada."
            );
        }

        return UriComponentsBuilder
                .fromUriString(
                        frontendBaseUrl
                )
                .path(
                        "/reset-password"
                )
                .queryParam(
                        "token",
                        resetToken
                )
                .build()
                .encode()
                .toUriString();
    }
}