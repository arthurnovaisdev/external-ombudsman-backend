package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.ReportRetentionProperties;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.repository.ReportMessageRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportRetentionServiceTest {

    private ReportRepository reportRepository;
    private ReportMessageRepository reportMessageRepository;
    private ReportRetentionProperties properties;

    private ReportRetentionService retentionService;

    @BeforeEach
    void setUp() {

        reportRepository =
                mock(ReportRepository.class);

        reportMessageRepository =
                mock(ReportMessageRepository.class);

        properties =
                new ReportRetentionProperties();

        properties.setEnabled(true);
        properties.setRetentionDays(180);
        properties.setBatchSize(100);

        retentionService =
                new ReportRetentionService(
                        reportRepository,
                        reportMessageRepository,
                        properties
                );
    }

    @Test
    void returnsEmptyResultWhenThereIsNothingToPurge() {

        when(
                reportRepository.findForMessagePurge(
                        any(Instant.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of()
        );

        var result =
                retentionService.purgeNextBatch();

        assertEquals(
                0,
                result.reportsPurged()
        );

        assertEquals(
                0,
                result.messagesDeleted()
        );

        verifyNoInteractions(
                reportMessageRepository
        );
    }

    @Test
    void purgesMessagesAndMarksReportsAsPurged() {

        Report firstReport =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(
                                "DEN-2026-ABCD2345"
                        )
                        .closedAt(
                                Instant.now()
                                        .minus(
                                                200,
                                                ChronoUnit.DAYS
                                        )
                        )
                        .build();

        Report secondReport =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(
                                "DEN-2026-EFGH5678"
                        )
                        .closedAt(
                                Instant.now()
                                        .minus(
                                                250,
                                                ChronoUnit.DAYS
                                        )
                        )
                        .build();

        when(
                reportRepository.findForMessagePurge(
                        any(Instant.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of(
                        firstReport,
                        secondReport
                )
        );

        when(
                reportMessageRepository
                        .deleteByReportIdIn(
                                any()
                        )
        ).thenReturn(5);

        var result =
                retentionService.purgeNextBatch();

        assertEquals(
                2,
                result.reportsPurged()
        );

        assertEquals(
                5,
                result.messagesDeleted()
        );

        assertNotNull(
                firstReport.getMessagesPurgedAt()
        );

        assertNotNull(
                secondReport.getMessagesPurgedAt()
        );

        verify(
                reportMessageRepository
        ).deleteByReportIdIn(
                argThat(ids ->
                        ids.size() == 2
                                && ids.contains(
                                firstReport.getId()
                        )
                                && ids.contains(
                                secondReport.getId()
                        )
                )
        );
    }

    @Test
    void usesConfiguredRetentionPeriodOf180Days() {

        when(
                reportRepository.findForMessagePurge(
                        any(Instant.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of()
        );

        Instant before =
                Instant.now()
                        .minus(
                                180,
                                ChronoUnit.DAYS
                        );

        retentionService.purgeNextBatch();

        Instant after =
                Instant.now()
                        .minus(
                                180,
                                ChronoUnit.DAYS
                        );

        ArgumentCaptor<Instant> cutoffCaptor =
                ArgumentCaptor.forClass(
                        Instant.class
                );

        verify(
                reportRepository
        ).findForMessagePurge(
                cutoffCaptor.capture(),
                any(Pageable.class)
        );

        Instant cutoff =
                cutoffCaptor.getValue();

        assertFalse(
                cutoff.isBefore(
                        before
                )
        );

        assertFalse(
                cutoff.isAfter(
                        after
                )
        );
    }

    @Test
    void usesConfiguredBatchSize() {

        properties.setBatchSize(25);

        when(
                reportRepository.findForMessagePurge(
                        any(Instant.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of()
        );

        retentionService.purgeNextBatch();

        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(
                reportRepository
        ).findForMessagePurge(
                any(Instant.class),
                pageableCaptor.capture()
        );

        assertEquals(
                25,
                pageableCaptor
                        .getValue()
                        .getPageSize()
        );

        assertEquals(
                0,
                pageableCaptor
                        .getValue()
                        .getPageNumber()
        );
    }

    @Test
    void repeatedExecutionDoesNotDeleteAgainWhenRepositoryReturnsNoMoreReports() {

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(
                                "DEN-2026-ABCD2345"
                        )
                        .closedAt(
                                Instant.now()
                                        .minus(
                                                200,
                                                ChronoUnit.DAYS
                                        )
                        )
                        .build();

        when(
                reportRepository.findForMessagePurge(
                        any(Instant.class),
                        any(Pageable.class)
                )
        ).thenReturn(
                List.of(report),
                List.of()
        );

        when(
                reportMessageRepository
                        .deleteByReportIdIn(
                                any()
                        )
        ).thenReturn(3);

        var firstExecution =
                retentionService.purgeNextBatch();

        var secondExecution =
                retentionService.purgeNextBatch();

        assertEquals(
                1,
                firstExecution.reportsPurged()
        );

        assertEquals(
                3,
                firstExecution.messagesDeleted()
        );

        assertEquals(
                0,
                secondExecution.reportsPurged()
        );

        assertEquals(
                0,
                secondExecution.messagesDeleted()
        );

        verify(
                reportMessageRepository,
                times(1)
        ).deleteByReportIdIn(
                any()
        );
    }

    @Test
    void exposesRetentionConfiguration() {

        assertTrue(
                retentionService.isEnabled()
        );

        assertEquals(
                100,
                retentionService.getBatchSize()
        );
    }
}