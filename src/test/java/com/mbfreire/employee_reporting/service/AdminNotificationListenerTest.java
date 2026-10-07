package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AdminNotificationProperties;
import com.mbfreire.employee_reporting.event.AdminNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminNotificationListenerTest {

    private EmailService emailService;
    private AdminNotificationProperties properties;
    private AdminNotificationListener listener;

    @BeforeEach
    void setUp() {

        emailService =
                mock(EmailService.class);

        properties =
                new AdminNotificationProperties();

        properties.setEnabled(true);
        properties.setEmail(
                "admin@example.com"
        );

        listener =
                new AdminNotificationListener(
                        emailService,
                        properties
                );
    }

    @Test
    void sendsNotificationWhenReportIsCreated() {

        AdminNotificationEvent event =
                new AdminNotificationEvent(
                        AdminNotificationEvent.Type.REPORT_CREATED,
                        "DEN-2026-ABCD2345"
                );

        listener.handle(event);

        verify(
                emailService
        ).sendNewReportAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );
    }

    @Test
    void sendsNotificationWhenClientCreatesMessage() {

        AdminNotificationEvent event =
                new AdminNotificationEvent(
                        AdminNotificationEvent.Type.CLIENT_MESSAGE_CREATED,
                        "DEN-2026-ABCD2345"
                );

        listener.handle(event);

        verify(
                emailService
        ).sendNewClientMessageAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );
    }

    @Test
    void doesNotSendNotificationWhenFeatureIsDisabled() {

        properties.setEnabled(false);

        AdminNotificationEvent event =
                new AdminNotificationEvent(
                        AdminNotificationEvent.Type.REPORT_CREATED,
                        "DEN-2026-ABCD2345"
                );

        listener.handle(event);

        verifyNoInteractions(
                emailService
        );
    }

    @Test
    void brevoFailureDoesNotEscapeFromListenerForNewReport() {

        doThrow(
                new RuntimeException(
                        "Brevo indisponível"
                )
        ).when(
                emailService
        ).sendNewReportAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );

        AdminNotificationEvent event =
                new AdminNotificationEvent(
                        AdminNotificationEvent.Type.REPORT_CREATED,
                        "DEN-2026-ABCD2345"
                );

        assertDoesNotThrow(
                () ->
                        listener.handle(event)
        );

        verify(
                emailService
        ).sendNewReportAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );
    }

    @Test
    void brevoFailureDoesNotEscapeFromListenerForClientMessage() {

        doThrow(
                new RuntimeException(
                        "Brevo indisponível"
                )
        ).when(
                emailService
        ).sendNewClientMessageAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );

        AdminNotificationEvent event =
                new AdminNotificationEvent(
                        AdminNotificationEvent.Type.CLIENT_MESSAGE_CREATED,
                        "DEN-2026-ABCD2345"
                );

        assertDoesNotThrow(
                () ->
                        listener.handle(event)
        );

        verify(
                emailService
        ).sendNewClientMessageAdminNotification(
                "admin@example.com",
                "DEN-2026-ABCD2345"
        );
    }

    @Test
    void listenerIsConfiguredToRunAfterCommit()
            throws Exception {

        Method method =
                AdminNotificationListener.class
                        .getMethod(
                                "handle",
                                AdminNotificationEvent.class
                        );

        TransactionalEventListener annotation =
                method.getAnnotation(
                        TransactionalEventListener.class
                );

        assertNotNull(
                annotation
        );

        assertEquals(
                TransactionPhase.AFTER_COMMIT,
                annotation.phase()
        );
    }
}