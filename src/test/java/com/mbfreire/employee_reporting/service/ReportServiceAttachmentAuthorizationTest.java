package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AttachmentFeatureProperties;
import com.mbfreire.employee_reporting.entity.Attachment;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.event.AdminNotificationEvent;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.AttachmentRepository;
import com.mbfreire.employee_reporting.repository.AuditLogRepository;
import com.mbfreire.employee_reporting.repository.CategoryRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceAttachmentAuthorizationTest {

    private ReportRepository reportRepository;
    private CategoryRepository categoryRepository;
    private AuditLogRepository auditLogRepository;
    private AttachmentRepository attachmentRepository;

    private ObjectProvider<FileStorageService>
            fileStorageServiceProvider;

    private FileStorageService fileStorageService;

    private ApplicationEventPublisher eventPublisher;

    private ReportService reportService;

    private User clientA;
    private User clientB;
    private User admin;

    @BeforeEach
    void setUp() {

        reportRepository =
                mock(ReportRepository.class);

        categoryRepository =
                mock(CategoryRepository.class);

        auditLogRepository =
                mock(AuditLogRepository.class);

        attachmentRepository =
                mock(AttachmentRepository.class);

        fileStorageServiceProvider =
                mock(ObjectProvider.class);

        fileStorageService =
                mock(FileStorageService.class);

        eventPublisher =
                mock(ApplicationEventPublisher.class);

        AttachmentFeatureProperties properties =
                new AttachmentFeatureProperties();

        properties.setEnabled(
                true
        );

        when(
                fileStorageServiceProvider.getIfAvailable()
        ).thenReturn(
                fileStorageService
        );

        reportService =
                new ReportService(
                        reportRepository,
                        categoryRepository,
                        auditLogRepository,
                        fileStorageServiceProvider,
                        attachmentRepository,
                        properties,
                        eventPublisher
                );

        clientA =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Cliente A")
                        .username("cliente.a")
                        .role(Role.CLIENT)
                        .active(true)
                        .build();

        clientB =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Cliente B")
                        .username("cliente.b")
                        .role(Role.CLIENT)
                        .active(true)
                        .build();

        admin =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Administrador")
                        .username("admin")
                        .role(Role.ADMIN)
                        .active(true)
                        .build();
    }

    @Test
    void clientCanDownloadAttachmentFromOwnReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        UUID attachmentId =
                UUID.randomUUID();

        Report report =
                reportOwnedBy(
                        protocol,
                        clientA
                );

        Attachment attachment =
                Attachment.builder()
                        .id(attachmentId)
                        .report(report)
                        .originalFileName(
                                "evidencia.pdf"
                        )
                        .storedFileName(
                                "arquivo-interno.pdf"
                        )
                        .contentType(
                                "application/pdf"
                        )
                        .fileSize(
                                1024L
                        )
                        .build();

        Resource resource =
                mock(Resource.class);

        when(
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                clientA.getId()
                        )
        ).thenReturn(
                Optional.of(report)
        );

        when(
                attachmentRepository
                        .findByIdAndReportProtocol(
                                attachmentId,
                                protocol
                        )
        ).thenReturn(
                Optional.of(attachment)
        );

        when(
                fileStorageService.loadFile(
                        "arquivo-interno.pdf"
                )
        ).thenReturn(
                resource
        );

        var response =
                reportService.downloadAttachment(
                        protocol,
                        attachmentId,
                        clientA
                );

        assertSame(
                resource,
                response.resource()
        );

        assertEquals(
                "evidencia.pdf",
                response.originalFileName()
        );

        assertEquals(
                "application/pdf",
                response.contentType()
        );

        verify(
                reportRepository
        ).findByProtocolAndOwnerId(
                protocol,
                clientA.getId()
        );

        verify(
                reportRepository,
                never()
        ).findByProtocol(
                protocol
        );

        verify(
                fileStorageService
        ).loadFile(
                "arquivo-interno.pdf"
        );
    }

    @Test
    void clientCannotDownloadAttachmentFromAnotherClientsReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        UUID attachmentId =
                UUID.randomUUID();

        when(
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                clientB.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        var error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                reportService
                                        .downloadAttachment(
                                                protocol,
                                                attachmentId,
                                                clientB
                                        )
                );

        assertEquals(
                "Manifestação não encontrada.",
                error.getMessage()
        );

        verify(
                reportRepository
        ).findByProtocolAndOwnerId(
                protocol,
                clientB.getId()
        );

        verify(
                reportRepository,
                never()
        ).findByProtocol(
                protocol
        );

        verifyNoInteractions(
                attachmentRepository
        );

        verify(
                fileStorageServiceProvider,
                never()
        ).getIfAvailable();

        verifyNoInteractions(
                fileStorageService
        );
    }

    @Test
    void adminCanDownloadAttachmentFromAnyReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        UUID attachmentId =
                UUID.randomUUID();

        Report report =
                reportOwnedBy(
                        protocol,
                        clientB
                );

        Attachment attachment =
                Attachment.builder()
                        .id(attachmentId)
                        .report(report)
                        .originalFileName(
                                "documento.pdf"
                        )
                        .storedFileName(
                                "storage-documento.pdf"
                        )
                        .contentType(
                                "application/pdf"
                        )
                        .fileSize(
                                2048L
                        )
                        .build();

        Resource resource =
                mock(Resource.class);

        when(
                reportRepository.findByProtocol(
                        protocol
                )
        ).thenReturn(
                Optional.of(report)
        );

        when(
                attachmentRepository
                        .findByIdAndReportProtocol(
                                attachmentId,
                                protocol
                        )
        ).thenReturn(
                Optional.of(attachment)
        );

        when(
                fileStorageService.loadFile(
                        "storage-documento.pdf"
                )
        ).thenReturn(
                resource
        );

        var response =
                reportService.downloadAttachment(
                        protocol,
                        attachmentId,
                        admin
                );

        assertSame(
                resource,
                response.resource()
        );

        assertEquals(
                "documento.pdf",
                response.originalFileName()
        );

        verify(
                reportRepository
        ).findByProtocol(
                protocol
        );

        verify(
                fileStorageService
        ).loadFile(
                "storage-documento.pdf"
        );
    }

    @Test
    void clientCannotUploadAttachmentToAnotherClientsReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        MultipartFile file =
                mock(MultipartFile.class);

        when(
                reportRepository
                        .findByProtocolAndOwnerIdForUpdate(
                                protocol,
                                clientB.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        var error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                reportService
                                        .uploadAttachments(
                                                protocol,
                                                clientB,
                                                List.of(file)
                                        )
                );

        assertEquals(
                "Manifestação não encontrada.",
                error.getMessage()
        );

        verify(
                reportRepository
        ).findByProtocolAndOwnerIdForUpdate(
                protocol,
                clientB.getId()
        );

        verify(
                reportRepository,
                never()
        ).findByProtocolForUpdate(
                protocol
        );

        verifyNoInteractions(
                attachmentRepository
        );

        verify(
                fileStorageService,
                never()
        ).storeFile(
                any()
        );
    }

    @Test
    void clientCannotUploadAttachmentToClosedReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        MultipartFile file =
                mock(MultipartFile.class);

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientA)
                        .closedAt(
                                Instant.now()
                        )
                        .build();

        when(
                reportRepository
                        .findByProtocolAndOwnerIdForUpdate(
                                protocol,
                                clientA.getId()
                        )
        ).thenReturn(
                Optional.of(report)
        );

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                reportService
                                        .uploadAttachments(
                                                protocol,
                                                clientA,
                                                List.of(file)
                                        )
                );

        assertEquals(
                "Não é possível enviar anexos para uma manifestação encerrada.",
                error.getMessage()
        );

        verifyNoInteractions(
                attachmentRepository
        );

        verify(
                fileStorageService,
                never()
        ).storeFile(
                any()
        );
    }

    @Test
    void adminCannotUploadAttachmentToClosedReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        MultipartFile file =
                mock(MultipartFile.class);

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientB)
                        .closedAt(
                                Instant.now()
                        )
                        .build();

        when(
                reportRepository
                        .findByProtocolForUpdate(
                                protocol
                        )
        ).thenReturn(
                Optional.of(report)
        );

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () ->
                                reportService
                                        .uploadAttachments(
                                                protocol,
                                                admin,
                                                List.of(file)
                                        )
                );

        assertEquals(
                "Não é possível enviar anexos para uma manifestação encerrada.",
                error.getMessage()
        );

        verify(
                reportRepository
        ).findByProtocolForUpdate(
                protocol
        );

        verifyNoInteractions(
                attachmentRepository
        );

        verify(
                fileStorageService,
                never()
        ).storeFile(
                any()
        );
    }

    @Test
    void authorizedUserCannotDownloadAttachmentFromDifferentProtocol() {

        String protocol =
                "DEN-2026-ABCD2345";

        UUID attachmentId =
                UUID.randomUUID();

        Report report =
                reportOwnedBy(
                        protocol,
                        clientA
                );

        when(
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                clientA.getId()
                        )
        ).thenReturn(
                Optional.of(report)
        );

        when(
                attachmentRepository
                        .findByIdAndReportProtocol(
                                attachmentId,
                                protocol
                        )
        ).thenReturn(
                Optional.empty()
        );

        var error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                reportService
                                        .downloadAttachment(
                                                protocol,
                                                attachmentId,
                                                clientA
                                        )
                );

        assertEquals(
                "Anexo não encontrado.",
                error.getMessage()
        );

        verify(
                fileStorageServiceProvider,
                never()
        ).getIfAvailable();

        verifyNoInteractions(
                fileStorageService
        );
    }

    private Report reportOwnedBy(
            String protocol,
            User owner
    ) {

        return Report.builder()
                .id(UUID.randomUUID())
                .protocol(protocol)
                .owner(owner)
                .build();
    }
}