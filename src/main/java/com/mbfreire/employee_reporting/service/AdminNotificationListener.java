package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AdminNotificationProperties;
import com.mbfreire.employee_reporting.event.AdminNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationListener {

    private final EmailService emailService;
    private final AdminNotificationProperties properties;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            AdminNotificationEvent event
    ) {

        if (!properties.isEnabled()) {
            return;
        }

        try {

            switch (event.type()) {

                case REPORT_CREATED ->
                        emailService
                                .sendNewReportAdminNotification(
                                        properties.getEmail(),
                                        event.protocol()
                                );

                case CLIENT_MESSAGE_CREATED ->
                        emailService
                                .sendNewClientMessageAdminNotification(
                                        properties.getEmail(),
                                        event.protocol()
                                );
            }

        } catch (Exception exception) {

            log.error(
                    "Falha ao enviar notificação administrativa "
                            + "do tipo {} para o protocolo {}.",
                    event.type(),
                    event.protocol(),
                    exception
            );
        }
    }
}