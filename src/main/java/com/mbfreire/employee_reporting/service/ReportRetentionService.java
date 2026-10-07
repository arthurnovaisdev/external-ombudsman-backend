package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.ReportRetentionProperties;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.repository.ReportMessageRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportRetentionService {

    private final ReportRepository reportRepository;
    private final ReportMessageRepository reportMessageRepository;
    private final ReportRetentionProperties properties;

    @Transactional
    public PurgeBatchResult purgeNextBatch() {

        Instant now =
                Instant.now();

        Instant cutoff =
                now.minus(
                        properties.getRetentionDays(),
                        ChronoUnit.DAYS
                );

        List<Report> reports =
                reportRepository
                        .findForMessagePurge(
                                cutoff,
                                PageRequest.of(
                                        0,
                                        properties.getBatchSize()
                                )
                        );

        if (reports.isEmpty()) {

            return new PurgeBatchResult(
                    0,
                    0
            );
        }

        List<UUID> reportIds =
                reports.stream()
                        .map(
                                Report::getId
                        )
                        .toList();

        int deletedMessages =
                reportMessageRepository
                        .deleteByReportIdIn(
                                reportIds
                        );

        for (Report report : reports) {

            report.setMessagesPurgedAt(
                    now
            );
        }

        return new PurgeBatchResult(
                reports.size(),
                deletedMessages
        );
    }

    public int getBatchSize() {
        return properties.getBatchSize();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public record PurgeBatchResult(
            int reportsPurged,
            int messagesDeleted
    ) {
    }
}