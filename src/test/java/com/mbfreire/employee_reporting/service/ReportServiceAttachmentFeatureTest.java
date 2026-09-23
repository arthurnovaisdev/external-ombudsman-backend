package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AttachmentFeatureProperties;
import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.entity.Category;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.enums.ReportStatus;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.repository.AttachmentRepository;
import com.mbfreire.employee_reporting.repository.AuditLogRepository;
import com.mbfreire.employee_reporting.repository.CategoryRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import com.mbfreire.employee_reporting.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceAttachmentFeatureTest {

    private ReportRepository reportRepository;
    private CategoryRepository categoryRepository;
    private AttachmentRepository attachmentRepository;
    private ObjectProvider<FileStorageService> fileStorageServiceProvider;
    private PasswordEncoder passwordEncoder;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ReportRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        attachmentRepository = mock(AttachmentRepository.class);
        fileStorageServiceProvider = mock(ObjectProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);

        AttachmentFeatureProperties properties = new AttachmentFeatureProperties();
        properties.setEnabled(false);

        reportService = new ReportService(
                reportRepository,
                categoryRepository,
                mock(StatusHistoryRepository.class),
                mock(AuditLogRepository.class),
                fileStorageServiceProvider,
                attachmentRepository,
                passwordEncoder,
                properties
        );
    }

    @Test
    void registersReportWithoutAccessingAttachmentStorage() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .name("Conduta interna")
                .active(true)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(reportRepository.existsByProtocol(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-seguro");

        var response = reportService.register(new ReportRequestDTO(
                categoryId,
                "Relato de teste.",
                LocalDate.now(),
                null
        ));

        assertEquals(10, response.trackingCode().length());
        verifyNoInteractions(fileStorageServiceProvider, attachmentRepository);
    }

    @Test
    void consultsReportByProtocolWithoutAccessingAttachmentStorage() {
        Category category = Category.builder()
                .name("Conduta interna")
                .active(true)
                .build();
        Report report = Report.builder()
                .protocol("DEN-2026-ABCD2345")
                .accessCodeHash("hash-seguro")
                .category(category)
                .description("Relato de teste.")
                .status(ReportStatus.RECEIVED)
                .createdAt(Instant.parse("2026-09-23T12:00:00Z"))
                .build();

        when(reportRepository.findByProtocol(report.getProtocol())).thenReturn(Optional.of(report));
        when(passwordEncoder.matches("ABCD2345EF", "hash-seguro")).thenReturn(true);

        var response = reportService.consult(report.getProtocol(), "ABCD2345EF");

        assertEquals(report.getProtocol(), response.protocol());
        assertEquals("Conduta interna", response.category());
        verifyNoInteractions(fileStorageServiceProvider, attachmentRepository);
    }

    @Test
    void rejectsDirectUploadBeforeConsultingReportOrStorage() {
        var error = assertThrows(
                AttachmentUnavailableException.class,
                () -> reportService.uploadAttachments(
                        "DEN-2026-ABCD2345",
                        "ABCD2345EF",
                        List.of()
                )
        );

        assertEquals("O envio de anexos está temporariamente indisponível.", error.getMessage());
        verifyNoInteractions(reportRepository, attachmentRepository, fileStorageServiceProvider);
    }

    @Test
    void rejectsHistoricalDownloadWithoutAccessingSupabase() {
        assertThrows(
                AttachmentUnavailableException.class,
                () -> reportService.downloadAttachment(
                        "DEN-2026-ABCD2345",
                        UUID.randomUUID()
                )
        );

        verifyNoInteractions(attachmentRepository, fileStorageServiceProvider);
    }
}
