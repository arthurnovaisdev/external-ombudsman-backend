package com.mbfreire.employee_reporting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ReportRetentionSchedulerTest {

    private ReportRetentionService retentionService;
    private ReportRetentionScheduler scheduler;

    @BeforeEach
    void setUp() {

        retentionService =
                mock(ReportRetentionService.class);

        scheduler =
                new ReportRetentionScheduler(
                        retentionService
                );
    }

    @Test
    void doesNothingWhenRetentionIsDisabled() {

        when(
                retentionService.isEnabled()
        ).thenReturn(false);

        scheduler.purgeOnStartup();

        verify(
                retentionService
        ).isEnabled();

        verify(
                retentionService,
                never()
        ).purgeNextBatch();
    }

    @Test
    void processesMultipleBatchesUntilLastIncompleteBatch() {

        when(
                retentionService.isEnabled()
        ).thenReturn(true);

        when(
                retentionService.getBatchSize()
        ).thenReturn(100);

        when(
                retentionService.purgeNextBatch()
        ).thenReturn(
                new ReportRetentionService.PurgeBatchResult(
                        100,
                        250
                ),
                new ReportRetentionService.PurgeBatchResult(
                        100,
                        180
                ),
                new ReportRetentionService.PurgeBatchResult(
                        15,
                        30
                )
        );

        scheduler.purgeOnStartup();

        verify(
                retentionService,
                times(3)
        ).purgeNextBatch();
    }

    @Test
    void stopsImmediatelyWhenThereIsNothingToPurge() {

        when(
                retentionService.isEnabled()
        ).thenReturn(true);

        when(
                retentionService.getBatchSize()
        ).thenReturn(100);

        when(
                retentionService.purgeNextBatch()
        ).thenReturn(
                new ReportRetentionService.PurgeBatchResult(
                        0,
                        0
                )
        );

        scheduler.purgeOnStartup();

        verify(
                retentionService,
                times(1)
        ).purgeNextBatch();
    }

    @Test
    void retentionFailureDoesNotEscapeFromScheduler() {

        when(
                retentionService.isEnabled()
        ).thenReturn(true);

        when(
                retentionService.purgeNextBatch()
        ).thenThrow(
                new RuntimeException(
                        "Falha simulada no banco"
                )
        );

        assertDoesNotThrow(
                () ->
                        scheduler.purgeOnStartup()
        );
    }

    @Test
    void scheduledExecutionUsesSameCleanupFlow() {

        when(
                retentionService.isEnabled()
        ).thenReturn(true);

        when(
                retentionService.getBatchSize()
        ).thenReturn(100);

        when(
                retentionService.purgeNextBatch()
        ).thenReturn(
                new ReportRetentionService.PurgeBatchResult(
                        0,
                        0
                )
        );

        scheduler.purgeScheduled();

        verify(
                retentionService
        ).purgeNextBatch();
    }
}