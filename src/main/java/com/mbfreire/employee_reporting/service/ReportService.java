package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.config.AttachmentFeatureProperties;
import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.dto.response.*;
import com.mbfreire.employee_reporting.entity.*;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int PROTOCOL_RANDOM_LENGTH = 8;
    private static final int MAX_PROTOCOL_GENERATION_ATTEMPTS = 20;

    private static final int MAX_ATTACHMENTS_PER_REPORT = 5;
    private static final long MAX_TOTAL_ATTACHMENT_BYTES =
            25L * 1024 * 1024;

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;

    private final ObjectProvider<FileStorageService>
            fileStorageServiceProvider;

    private final AttachmentRepository attachmentRepository;
    private final AttachmentFeatureProperties attachmentFeatureProperties;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Transactional
    public ProtocolResponseDTO register(
            ReportRequestDTO dto,
            User owner
    ) {

        Category category =
                categoryRepository.findById(dto.categoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Categoria não encontrada."
                                )
                        );

        if (!category.isActive()) {
            throw new BusinessRuleException(
                    "A categoria selecionada não está disponível."
            );
        }

        Report report =
                Report.builder()
                        .protocol(generateUniqueProtocol())
                        .owner(owner)
                        .category(category)
                        .description(dto.description().trim())
                        .incidentDate(dto.incidentDate())
                        .incidentLocation(
                                normalizeOptionalText(
                                        dto.incidentLocation()
                                )
                        )
                        .build();

        reportRepository.save(report);

        return new ProtocolResponseDTO(
                report.getProtocol()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReportResponseDTO> findMine(
            User user,
            Pageable pageable
    ) {

        return reportRepository
                .findByOwnerId(
                        user.getId(),
                        pageable
                )
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ReportResponseDTO findMineDetail(
            String protocol,
            User user
    ) {

        Report report =
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        return toResponseDTO(report);
    }

    @Transactional(readOnly = true)
    public Page<ReportAdminSummaryResponseDTO> findAllAdmin(
            Pageable pageable
    ) {

        return reportRepository
                .findAll(pageable)
                .map(this::toAdminSummaryDTO);
    }

    @Transactional(readOnly = true)
    public ReportAdminResponseDTO findAdminDetail(
            String protocol
    ) {

        Report report =
                reportRepository
                        .findByProtocol(protocol)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        return toAdminResponseDTO(report);
    }

    @Transactional
    public ReportAdminResponseDTO close(
            String protocol,
            User loggedInAdmin
    ) {

        Report report =
                reportRepository
                        .findByProtocolForUpdate(protocol)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        if (report.getClosedAt() != null) {
            return toAdminResponseDTO(report);
        }

        report.setClosedAt(
                Instant.now()
        );

        reportRepository.save(report);

        AuditLog audit =
                AuditLog.builder()
                        .action("CLOSE_REPORT")
                        .report(report)
                        .adminUser(loggedInAdmin)
                        .build();

        auditLogRepository.save(audit);

        return toAdminResponseDTO(report);
    }

    @Transactional
    public void uploadAttachments(
            String protocol,
            User loggedInUser,
            List<MultipartFile> files
    ) {

        if (!attachmentFeatureProperties.isEnabled()) {
            throw new AttachmentUnavailableException(
                    "O envio de anexos está temporariamente indisponível."
            );
        }

        FileStorageService fileStorageService =
                requireFileStorageService();

        Report report =
                findReportForWrite(
                        protocol,
                        loggedInUser
                );

        if (report.getClosedAt() != null) {
            throw new BusinessRuleException(
                    "Não é possível enviar anexos para uma manifestação encerrada."
            );
        }

        if (files == null
                || files.isEmpty()) {

            throw new BusinessRuleException(
                    "Nenhum arquivo foi enviado."
            );
        }

        validateAttachmentQuota(
                report,
                files
        );

        List<String> storedFiles =
                new ArrayList<>();

        registerRollbackCleanup(
                storedFiles,
                fileStorageService
        );

        for (MultipartFile file : files) {

            FileStorageService.StoredFile storedFile =
                    fileStorageService.storeFile(file);

            storedFiles.add(
                    storedFile.storedFileName()
            );

            Attachment attachment =
                    Attachment.builder()
                            .report(report)
                            .originalFileName(
                                    file.getOriginalFilename()
                            )
                            .storedFileName(
                                    storedFile.storedFileName()
                            )
                            .contentType(
                                    file.getContentType()
                            )
                            .fileSize(
                                    file.getSize()
                            )
                            .build();

            attachmentRepository.save(
                    attachment
            );
        }

        attachmentRepository.flush();
    }


    @Transactional(readOnly = true)
    public AttachmentDownloadDTO downloadAttachment(
            String protocol,
            UUID attachmentId,
            User loggedInUser
    ) {

        if (!attachmentFeatureProperties.isEnabled()) {
            throw new AttachmentUnavailableException(
                    "A recuperação de anexos está temporariamente indisponível."
            );
        }

        findReportForRead(
                protocol,
                loggedInUser
        );

        Attachment attachment =
                attachmentRepository
                        .findByIdAndReportProtocol(
                                attachmentId,
                                protocol
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Anexo não encontrado."
                                )
                        );

        Resource resource =
                requireFileStorageService()
                        .loadFile(
                                attachment.getStoredFileName()
                        );

        return new AttachmentDownloadDTO(
                resource,
                attachment.getOriginalFileName(),
                attachment.getContentType()
        );
    }


    private Report findReportForRead(
            String protocol,
            User user
    ) {

        if (user.getRole() == Role.ADMIN) {

            return reportRepository
                    .findByProtocol(protocol)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Manifestação não encontrada."
                            )
                    );
        }

        return reportRepository
                .findByProtocolAndOwnerId(
                        protocol,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Manifestação não encontrada."
                        )
                );
    }


    private Report findReportForWrite(
            String protocol,
            User user
    ) {

        if (user.getRole() == Role.ADMIN) {

            return reportRepository
                    .findByProtocolForUpdate(protocol)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Manifestação não encontrada."
                            )
                    );
        }

        return reportRepository
                .findByProtocolAndOwnerIdForUpdate(
                        protocol,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Manifestação não encontrada."
                        )
                );
    }

    private void validateAttachmentQuota(
            Report report,
            List<MultipartFile> files
    ) {

        long existingCount =
                attachmentRepository
                        .countByReportId(
                                report.getId()
                        );

        if (existingCount
                + files.size()
                > MAX_ATTACHMENTS_PER_REPORT) {

            throw new BusinessRuleException(
                    "Cada manifestação pode possuir no máximo "
                            + MAX_ATTACHMENTS_PER_REPORT
                            + " anexos."
            );
        }

        Long storedBytesResult =
                attachmentRepository
                        .sumFileSizeByReportId(
                                report.getId()
                        );

        long existingBytes =
                storedBytesResult == null
                        ? 0L
                        : storedBytesResult;

        long incomingBytes = 0L;

        for (MultipartFile file : files) {

            if (file == null
                    || file.isEmpty()) {

                throw new BusinessRuleException(
                        "Um dos arquivos enviados está vazio."
                );
            }

            try {

                incomingBytes =
                        Math.addExact(
                                incomingBytes,
                                file.getSize()
                        );

            } catch (ArithmeticException e) {

                throw new BusinessRuleException(
                        "O tamanho total dos arquivos enviados é inválido."
                );
            }
        }

        long totalBytes;

        try {

            totalBytes =
                    Math.addExact(
                            existingBytes,
                            incomingBytes
                    );

        } catch (ArithmeticException e) {

            throw new BusinessRuleException(
                    "O tamanho total dos anexos é inválido."
            );
        }

        if (totalBytes
                > MAX_TOTAL_ATTACHMENT_BYTES) {

            throw new BusinessRuleException(
                    "Os anexos de uma manifestação não podem ultrapassar 25 MB no total."
            );
        }
    }


    private void registerRollbackCleanup(
            List<String> storedFiles,
            FileStorageService fileStorageService
    ) {

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {

            throw new IllegalStateException(
                    "A transação de upload não está ativa."
            );
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(
                                    int status
                            ) {

                                if (status
                                        == TransactionSynchronization
                                        .STATUS_ROLLED_BACK) {

                                    for (String storedFileName
                                            : storedFiles) {

                                        fileStorageService
                                                .deleteFile(
                                                        storedFileName
                                                );
                                    }
                                }
                            }
                        }
                );
    }


    private FileStorageService requireFileStorageService() {

        FileStorageService fileStorageService =
                fileStorageServiceProvider
                        .getIfAvailable();

        if (fileStorageService == null) {

            throw new AttachmentUnavailableException(
                    "O serviço de anexos está temporariamente indisponível."
            );
        }

        return fileStorageService;
    }

    private String generateUniqueProtocol() {

        int year =
                Year.now().getValue();

        for (int attempt = 0;
             attempt < MAX_PROTOCOL_GENERATION_ATTEMPTS;
             attempt++) {

            String protocol =
                    "DEN-"
                            + year
                            + "-"
                            + generateRandomCode(
                            PROTOCOL_RANDOM_LENGTH
                    );

            if (!reportRepository
                    .existsByProtocol(protocol)) {

                return protocol;
            }
        }

        throw new IllegalStateException(
                "Não foi possível gerar um protocolo único."
        );
    }


    private String generateRandomCode(
            int length
    ) {

        StringBuilder builder =
                new StringBuilder(length);

        for (int i = 0;
             i < length;
             i++) {

            int index =
                    secureRandom.nextInt(
                            CHARACTERS.length()
                    );

            builder.append(
                    CHARACTERS.charAt(index)
            );
        }

        return builder.toString();
    }


    private ReportResponseDTO toResponseDTO(
            Report report
    ) {

        return new ReportResponseDTO(
                report.getProtocol(),
                report.getCategory().getName(),
                report.getDescription(),
                report.getIncidentDate(),
                report.getIncidentLocation(),
                report.getCreatedAt(),
                report.getClosedAt(),
                report.getMessagesPurgedAt()
        );
    }


    private ReportAdminSummaryResponseDTO toAdminSummaryDTO(
            Report report
    ) {

        return new ReportAdminSummaryResponseDTO(
                report.getProtocol(),
                report.getCategory().getName(),
                report.getDescription(),
                report.getCreatedAt(),
                report.getClosedAt(),
                report.getOwner().getName(),
                report.getOwner().getUsername()
        );
    }


    private ReportAdminResponseDTO toAdminResponseDTO(
            Report report
    ) {

        List<AttachmentResponseDTO> attachments =
                attachmentRepository
                        .findByReportId(
                                report.getId()
                        )
                        .stream()
                        .map(attachment ->
                                new AttachmentResponseDTO(
                                        attachment.getId(),
                                        attachment.getOriginalFileName(),
                                        attachment.getContentType(),
                                        attachment.getFileSize(),
                                        attachment.getCreatedAt()
                                )
                        )
                        .toList();

        return new ReportAdminResponseDTO(
                report.getProtocol(),
                report.getCategory().getName(),
                report.getDescription(),
                report.getIncidentDate(),
                report.getIncidentLocation(),
                report.getCreatedAt(),
                report.getClosedAt(),
                report.getMessagesPurgedAt(),

                report.getOwner().getName(),
                report.getOwner().getUsername(),
                report.getOwner().getContactEmail(),

                attachments
        );
    }


    private String normalizeOptionalText(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }
}