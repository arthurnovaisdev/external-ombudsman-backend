package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.ReportMessageRequestDTO;
import com.mbfreire.employee_reporting.dto.response.ReportMessageResponseDTO;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.entity.ReportMessage;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.ReportMessageRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportMessageService {

    private final ReportRepository reportRepository;
    private final ReportMessageRepository reportMessageRepository;


    // CLIENT

    @Transactional(readOnly = true)
    public Page<ReportMessageResponseDTO> findMineMessages(
            String protocol,
            User client,
            Pageable pageable
    ) {

        requireClient(client);

        Report report =
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                client.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        return reportMessageRepository
                .findByReportIdOrderByCreatedAtAscIdAsc(
                        report.getId(),
                        pageable
                )
                .map(this::toResponseDTO);
    }


    @Transactional
    public ReportMessageResponseDTO sendMineMessage(
            String protocol,
            User client,
            ReportMessageRequestDTO dto
    ) {

        requireClient(client);

        Report report =
                reportRepository
                        .findByProtocolAndOwnerIdForUpdate(
                                protocol,
                                client.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        ensureReportIsOpen(report);

        return saveMessage(
                report,
                client,
                dto
        );
    }


    // ADMIN

    @Transactional(readOnly = true)
    public Page<ReportMessageResponseDTO> findAdminMessages(
            String protocol,
            User admin,
            Pageable pageable
    ) {

        requireAdmin(admin);

        Report report =
                reportRepository
                        .findByProtocol(protocol)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        return reportMessageRepository
                .findByReportIdOrderByCreatedAtAscIdAsc(
                        report.getId(),
                        pageable
                )
                .map(this::toResponseDTO);
    }


    @Transactional
    public ReportMessageResponseDTO sendAdminMessage(
            String protocol,
            User admin,
            ReportMessageRequestDTO dto
    ) {

        requireAdmin(admin);

        Report report =
                reportRepository
                        .findByProtocolForUpdate(protocol)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Manifestação não encontrada."
                                )
                        );

        ensureReportIsOpen(report);

        return saveMessage(
                report,
                admin,
                dto
        );
    }


    // MESSAGE

    private ReportMessageResponseDTO saveMessage(
            Report report,
            User author,
            ReportMessageRequestDTO dto
    ) {

        ReportMessage message =
                ReportMessage.builder()
                        .report(report)
                        .author(author)
                        .body(dto.body().trim())
                        .build();

        ReportMessage saved =
                reportMessageRepository
                        .saveAndFlush(message);

        return toResponseDTO(saved);
    }


    private void ensureReportIsOpen(
            Report report
    ) {

        if (report.getClosedAt() != null) {

            throw new BusinessRuleException(
                    "Não é possível enviar mensagens para uma manifestação encerrada."
            );
        }
    }


    // AUTHORIZATION

    private void requireClient(
            User user
    ) {

        if (user == null
                || user.getRole() != Role.CLIENT) {

            throw new AccessDeniedException(
                    "Acesso permitido apenas para clientes."
            );
        }
    }


    private void requireAdmin(
            User user
    ) {

        if (user == null
                || user.getRole() != Role.ADMIN) {

            throw new AccessDeniedException(
                    "Acesso permitido apenas para administradores."
            );
        }
    }


    // DTO

    private ReportMessageResponseDTO toResponseDTO(
            ReportMessage message
    ) {

        return new ReportMessageResponseDTO(
                message.getId(),
                message.getAuthor().getName(),
                message.getAuthor().getUsername(),
                message.getAuthor().getRole().name(),
                message.getBody(),
                message.getCreatedAt()
        );
    }
}