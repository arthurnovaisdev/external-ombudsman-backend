package com.mbfreire.employee_reporting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    private RestTemplate restTemplate;
    private EmailService emailService;

    @BeforeEach
    void setUp() {

        restTemplate =
                mock(RestTemplate.class);

        emailService =
                new EmailService(
                        restTemplate
                );

        ReflectionTestUtils.setField(
                emailService,
                "apiUrl",
                "https://api.brevo.com/v3/smtp/email"
        );

        ReflectionTestUtils.setField(
                emailService,
                "apiKey",
                "test-api-key"
        );

        ReflectionTestUtils.setField(
                emailService,
                "senderEmail",
                "no-reply@mbfreire.com.br"
        );

        ReflectionTestUtils.setField(
                emailService,
                "senderName",
                "Ouvidoria MBFREIRE"
        );

        ReflectionTestUtils.setField(
                emailService,
                "frontendBaseUrl",
                "https://ouvidoria.example.com"
        );
    }

    @Test
    void newReportNotificationContainsOnlySafeInformation() {

        emailService
                .sendNewReportAdminNotification(
                        "admin@mbfreire.com.br",
                        "DEN-2026-ABCD2345"
                );

        ArgumentCaptor<HttpEntity> requestCaptor =
                ArgumentCaptor.forClass(
                        HttpEntity.class
                );

        verify(
                restTemplate
        ).postForEntity(
                eq(
                        "https://api.brevo.com/v3/smtp/email"
                ),
                requestCaptor.capture(),
                eq(String.class)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                (Map<String, Object>)
                        requestCaptor
                                .getValue()
                                .getBody();

        assertNotNull(
                body
        );

        assertEquals(
                "Nova manifestação - DEN-2026-ABCD2345",
                body.get("subject")
        );

        String htmlContent =
                (String) body.get(
                        "htmlContent"
                );

        String textContent =
                (String) body.get(
                        "textContent"
                );

        assertNotNull(
                htmlContent
        );

        assertNotNull(
                textContent
        );

        assertEquals(
                true,
                htmlContent.contains(
                        "DEN-2026-ABCD2345"
                )
        );

        assertEquals(
                true,
                textContent.contains(
                        "DEN-2026-ABCD2345"
                )
        );

        /*
         * O e-mail administrativo deve conter somente
         * informações mínimas.
         */
        assertFalse(
                htmlContent.contains(
                        "Descrição confidencial"
                )
        );

        assertFalse(
                textContent.contains(
                        "Descrição confidencial"
                )
        );

        assertSenderAndRecipient(
                body
        );
    }

    @Test
    void newClientMessageNotificationContainsOnlySafeInformation() {

        emailService
                .sendNewClientMessageAdminNotification(
                        "admin@mbfreire.com.br",
                        "DEN-2026-ABCD2345"
                );

        ArgumentCaptor<HttpEntity> requestCaptor =
                ArgumentCaptor.forClass(
                        HttpEntity.class
                );

        verify(
                restTemplate
        ).postForEntity(
                eq(
                        "https://api.brevo.com/v3/smtp/email"
                ),
                requestCaptor.capture(),
                eq(String.class)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                (Map<String, Object>)
                        requestCaptor
                                .getValue()
                                .getBody();

        assertNotNull(
                body
        );

        assertEquals(
                "Nova mensagem - DEN-2026-ABCD2345",
                body.get("subject")
        );

        String htmlContent =
                (String) body.get(
                        "htmlContent"
                );

        String textContent =
                (String) body.get(
                        "textContent"
                );

        assertNotNull(
                htmlContent
        );

        assertNotNull(
                textContent
        );

        assertEquals(
                true,
                htmlContent.contains(
                        "DEN-2026-ABCD2345"
                )
        );

        assertEquals(
                true,
                textContent.contains(
                        "DEN-2026-ABCD2345"
                )
        );

        /*
         * O corpo real da conversa nunca deve
         * ser copiado para o e-mail.
         */
        assertFalse(
                htmlContent.contains(
                        "Mensagem confidencial do cliente"
                )
        );

        assertFalse(
                textContent.contains(
                        "Mensagem confidencial do cliente"
                )
        );

        assertSenderAndRecipient(
                body
        );
    }

    @Test
    void usesMbfreirePublicNameAsSender() {

        emailService
                .sendNewReportAdminNotification(
                        "admin@mbfreire.com.br",
                        "DEN-2026-ABCD2345"
                );

        ArgumentCaptor<HttpEntity> requestCaptor =
                ArgumentCaptor.forClass(
                        HttpEntity.class
                );

        verify(
                restTemplate
        ).postForEntity(
                eq(
                        "https://api.brevo.com/v3/smtp/email"
                ),
                requestCaptor.capture(),
                eq(String.class)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body =
                (Map<String, Object>)
                        requestCaptor
                                .getValue()
                                .getBody();

        @SuppressWarnings("unchecked")
        Map<String, Object> sender =
                (Map<String, Object>)
                        body.get("sender");

        assertEquals(
                "Ouvidoria MBFREIRE",
                sender.get("name")
        );

        assertEquals(
                "no-reply@mbfreire.com.br",
                sender.get("email")
        );
    }

    @SuppressWarnings("unchecked")
    private void assertSenderAndRecipient(
            Map<String, Object> body
    ) {

        Map<String, Object> sender =
                (Map<String, Object>)
                        body.get("sender");

        assertEquals(
                "Ouvidoria MBFREIRE",
                sender.get("name")
        );

        assertEquals(
                "no-reply@mbfreire.com.br",
                sender.get("email")
        );

        List<Map<String, Object>> recipients =
                (List<Map<String, Object>>)
                        body.get("to");

        assertEquals(
                1,
                recipients.size()
        );

        assertEquals(
                "admin@mbfreire.com.br",
                recipients.getFirst()
                        .get("email")
        );

        assertEquals(
                "Administrador",
                recipients.getFirst()
                        .get("name")
        );
    }
}