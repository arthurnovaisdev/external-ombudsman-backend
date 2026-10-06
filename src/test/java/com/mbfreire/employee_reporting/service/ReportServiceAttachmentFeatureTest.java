package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AttachmentFeatureProperties;
import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.entity.Category;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.repository.AttachmentRepository;
import com.mbfreire.employee_reporting.repository.AuditLogRepository;
import com.mbfreire.employee_reporting.repository.CategoryRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceAttachmentFeatureTest {

    private ReportRepository reportRepository;
    private CategoryRepository categoryRepository;
    private AttachmentRepository attachmentRepository;
    private ObjectProvider<FileStorageService> fileStorageServiceProvider;

    private ReportService reportService;
    private User client;

    @BeforeEach
    void setUp() {

        reportRepository =
                mock(ReportRepository.class);

        categoryRepository =
                mock(CategoryRepository.class);

        attachmentRepository =
                mock(AttachmentRepository.class);

        fileStorageServiceProvider =
                mock(ObjectProvider.class);

        AttachmentFeatureProperties properties =
                new AttachmentFeatureProperties();

        properties.setEnabled(false);

        reportService =
                new ReportService(
                        reportRepository,
                        categoryRepository,
                        mock(AuditLogRepository.class),
                        fileStorageServiceProvider,
                        attachmentRepository,
                        properties
                );

        client =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Cliente de Teste")
                        .username("cliente.teste")
                        .role(Role.CLIENT)
                        .active(true)
                        .build();
    }

    @Test
    void registersReportWithoutAccessingAttachmentStorage() {

        UUID categoryId =
                UUID.randomUUID();

        Category category =
                Category.builder()
                        .id(categoryId)
                        .name("Conduta")
                        .active(true)
                        .build();

        when(
                categoryRepository.findById(categoryId)
        ).thenReturn(
                Optional.of(category)
        );

        when(
                reportRepository.existsByProtocol(
                        anyString()
                )
        ).thenReturn(false);

        var response =
                reportService.register(
                        new ReportRequestDTO(
                                categoryId,
                                "Relato de teste.",
                                LocalDate.now(),
                                null
                        ),
                        client
                );

        assertNotNull(
                response.protocol()
        );

        assertTrue(
                response.protocol().matches(
                        "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}"
                )
        );

        verifyNoInteractions(
                fileStorageServiceProvider,
                attachmentRepository
        );
    }

    @Test
    void findsOwnReportWithoutAccessingAttachmentStorage() {

        Category category =
                Category.builder()
                        .name("Conduta")
                        .active(true)
                        .build();

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(
                                "DEN-2026-ABCD2345"
                        )
                        .owner(client)
                        .category(category)
                        .description(
                                "Relato de teste."
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-09-23T12:00:00Z"
                                )
                        )
                        .build();

        when(
                reportRepository
                        .findByProtocolAndOwnerId(
                                report.getProtocol(),
                                client.getId()
                        )
        ).thenReturn(
                Optional.of(report)
        );

        var response =
                reportService.findMineDetail(
                        report.getProtocol(),
                        client
                );

        assertEquals(
                report.getProtocol(),
                response.protocol()
        );

        assertEquals(
                "Conduta",
                response.category()
        );

        verifyNoInteractions(
                fileStorageServiceProvider,
                attachmentRepository
        );
    }

    @Test
    void rejectsUploadWhenAttachmentsAreDisabled() {

        var error =
                assertThrows(
                        AttachmentUnavailableException.class,
                        () ->
                                reportService
                                        .uploadAttachments(
                                                "DEN-2026-ABCD2345",
                                                client,
                                                List.of()
                                        )
                );

        assertEquals(
                "O envio de anexos está temporariamente indisponível.",
                error.getMessage()
        );

        verifyNoInteractions(
                reportRepository,
                attachmentRepository,
                fileStorageServiceProvider
        );
    }

    @Test
    void rejectsDownloadWhenAttachmentsAreDisabled() {

        var error =
                assertThrows(
                        AttachmentUnavailableException.class,
                        () ->
                                reportService
                                        .downloadAttachment(
                                                "DEN-2026-ABCD2345",
                                                UUID.randomUUID(),
                                                client
                                        )
                );

        assertEquals(
                "A recuperação de anexos está temporariamente indisponível.",
                error.getMessage()
        );

        verifyNoInteractions(
                reportRepository,
                attachmentRepository,
                fileStorageServiceProvider
        );
    }
}