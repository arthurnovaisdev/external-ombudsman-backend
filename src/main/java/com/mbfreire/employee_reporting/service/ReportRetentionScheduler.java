package com.mbfreire.employee_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportRetentionScheduler {

    private final ReportRetentionService reportRetentionService;

    @EventListener(
            ApplicationReadyEvent.class
    )
    public void purgeOnStartup() {

        runCleanup(
                "startup"
        );
    }

    @Scheduled(
            cron = "${reports.retention.cron:0 0 3 * * *}",
            zone = "UTC"
    )
    public void purgeScheduled() {

        runCleanup(
                "scheduled"
        );
    }

    private void runCleanup(
            String trigger
    ) {

        if (!reportRetentionService.isEnabled()) {
            return;
        }

        try {

            int totalReports =
                    0;

            int totalMessages =
                    0;

            while (true) {

                ReportRetentionService.PurgeBatchResult result =
                        reportRetentionService
                                .purgeNextBatch();

                totalReports +=
                        result.reportsPurged();

                totalMessages +=
                        result.messagesDeleted();

                if (result.reportsPurged()
                        < reportRetentionService
                        .getBatchSize()) {

                    break;
                }
            }

            if (totalReports > 0) {

                log.info(
                        "Retenção de mensagens concluída. "
                                + "Trigger: {}, manifestações: {}, mensagens removidas: {}.",
                        trigger,
                        totalReports,
                        totalMessages
                );
            }

        } catch (Exception exception) {

            log.error(
                    "Falha durante a retenção automática "
                            + "de mensagens. Trigger: {}.",
                    trigger,
                    exception
            );
        }
    }
}