package com.mbfreire.employee_reporting.service;

import com.mbfreire.employee_reporting.dto.request.ReportMessageRequestDTO;
import com.mbfreire.employee_reporting.entity.Category;
import com.mbfreire.employee_reporting.entity.Report;
import com.mbfreire.employee_reporting.entity.ReportMessage;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.event.AdminNotificationEvent;
import com.mbfreire.employee_reporting.exception.BusinessRuleException;
import com.mbfreire.employee_reporting.exception.ResourceNotFoundException;
import com.mbfreire.employee_reporting.repository.ReportMessageRepository;
import com.mbfreire.employee_reporting.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportMessageServiceTest {

    private ReportRepository reportRepository;
    private ReportMessageRepository reportMessageRepository;
    private ApplicationEventPublisher eventPublisher;

    private ReportMessageService reportMessageService;

    private User clientA;
    private User clientB;
    private User admin;

    @BeforeEach
    void setUp() {

        reportRepository =
                mock(ReportRepository.class);

        reportMessageRepository =
                mock(ReportMessageRepository.class);

        eventPublisher =
                mock(ApplicationEventPublisher.class);

        reportMessageService =
                new ReportMessageService(
                        reportRepository,
                        reportMessageRepository,
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
    void clientCannotReadMessagesFromAnotherClientsReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        when(
                reportRepository
                        .findByProtocolAndOwnerId(
                                protocol,
                                clientA.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        var error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                reportMessageService
                                        .findMineMessages(
                                                protocol,
                                                clientA,
                                                PageRequest.of(
                                                        0,
                                                        20
                                                )
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
                clientA.getId()
        );

        verify(
                reportRepository,
                never()
        ).findByProtocol(
                protocol
        );

        verifyNoInteractions(
                reportMessageRepository,
                eventPublisher
        );
    }

    @Test
    void clientCannotSendMessageToAnotherClientsReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        when(
                reportRepository
                        .findByProtocolAndOwnerIdForUpdate(
                                protocol,
                                clientA.getId()
                        )
        ).thenReturn(
                Optional.empty()
        );

        ReportMessageRequestDTO dto =
                new ReportMessageRequestDTO(
                        "Tentativa de mensagem."
                );

        var error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                reportMessageService
                                        .sendMineMessage(
                                                protocol,
                                                clientA,
                                                dto
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
                clientA.getId()
        );

        verify(
                reportRepository,
                never()
        ).findByProtocolForUpdate(
                protocol
        );

        verifyNoInteractions(
                reportMessageRepository,
                eventPublisher
        );
    }

    @Test
    void ownerCanReadOwnMessages() {

        String protocol =
                "DEN-2026-ABCD2345";

        Category category =
                Category.builder()
                        .id(UUID.randomUUID())
                        .name("Conduta")
                        .active(true)
                        .build();

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientA)
                        .category(category)
                        .description(
                                "Manifestação de teste."
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-10-06T15:00:00Z"
                                )
                        )
                        .build();

        ReportMessage firstMessage =
                ReportMessage.builder()
                        .id(UUID.randomUUID())
                        .report(report)
                        .author(clientA)
                        .body(
                                "Mensagem do cliente."
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-10-06T15:10:00Z"
                                )
                        )
                        .build();

        ReportMessage secondMessage =
                ReportMessage.builder()
                        .id(UUID.randomUUID())
                        .report(report)
                        .author(admin)
                        .body(
                                "Resposta administrativa."
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-10-06T15:20:00Z"
                                )
                        )
                        .build();

        var pageable =
                PageRequest.of(
                        0,
                        20
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
                reportMessageRepository
                        .findByReportIdOrderByCreatedAtAscIdAsc(
                                report.getId(),
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(
                                firstMessage,
                                secondMessage
                        ),
                        pageable,
                        2
                )
        );

        var response =
                reportMessageService
                        .findMineMessages(
                                protocol,
                                clientA,
                                pageable
                        );

        assertEquals(
                2,
                response.getTotalElements()
        );

        assertEquals(
                "Mensagem do cliente.",
                response.getContent()
                        .get(0)
                        .body()
        );

        assertEquals(
                "CLIENT",
                response.getContent()
                        .get(0)
                        .authorRole()
        );

        assertEquals(
                "Resposta administrativa.",
                response.getContent()
                        .get(1)
                        .body()
        );

        assertEquals(
                "ADMIN",
                response.getContent()
                        .get(1)
                        .authorRole()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void cannotSendMessageToClosedReport() {

        String protocol =
                "DEN-2026-ABCD2345";

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
                                reportMessageService
                                        .sendMineMessage(
                                                protocol,
                                                clientA,
                                                new ReportMessageRequestDTO(
                                                        "Nova mensagem."
                                                )
                                        )
                );

        assertEquals(
                "Não é possível enviar mensagens para uma manifestação encerrada.",
                error.getMessage()
        );

        verify(
                reportMessageRepository,
                never()
        ).saveAndFlush(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void adminCannotSendMessageToClosedReport() {

        String protocol =
                "DEN-2026-ABCD2345";

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
                                reportMessageService
                                        .sendAdminMessage(
                                                protocol,
                                                admin,
                                                new ReportMessageRequestDTO(
                                                        "Resposta administrativa."
                                                )
                                        )
                );

        assertEquals(
                "Não é possível enviar mensagens para uma manifestação encerrada.",
                error.getMessage()
        );

        verify(
                reportMessageRepository,
                never()
        ).saveAndFlush(
                any()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void adminCanReadMessagesFromAnyReport() {

        String protocol =
                "DEN-2026-ABCD2345";

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientB)
                        .build();

        ReportMessage message =
                ReportMessage.builder()
                        .id(UUID.randomUUID())
                        .report(report)
                        .author(clientB)
                        .body(
                                "Mensagem do Cliente B."
                        )
                        .createdAt(
                                Instant.parse(
                                        "2026-10-06T15:00:00Z"
                                )
                        )
                        .build();

        var pageable =
                PageRequest.of(
                        0,
                        20
                );

        when(
                reportRepository
                        .findByProtocol(protocol)
        ).thenReturn(
                Optional.of(report)
        );

        when(
                reportMessageRepository
                        .findByReportIdOrderByCreatedAtAscIdAsc(
                                report.getId(),
                                pageable
                        )
        ).thenReturn(
                new PageImpl<>(
                        List.of(message),
                        pageable,
                        1
                )
        );

        var response =
                reportMessageService
                        .findAdminMessages(
                                protocol,
                                admin,
                                pageable
                        );

        assertEquals(
                1,
                response.getTotalElements()
        );

        assertEquals(
                "cliente.b",
                response.getContent()
                        .getFirst()
                        .authorUsername()
        );

        verifyNoInteractions(
                eventPublisher
        );
    }

    @Test
    void clientCannotUseAdminMessageMethods() {

        assertThrows(
                AccessDeniedException.class,
                () ->
                        reportMessageService
                                .findAdminMessages(
                                        "DEN-2026-ABCD2345",
                                        clientA,
                                        PageRequest.of(
                                                0,
                                                20
                                        )
                                )
        );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        reportMessageService
                                .sendAdminMessage(
                                        "DEN-2026-ABCD2345",
                                        clientA,
                                        new ReportMessageRequestDTO(
                                                "Tentativa indevida."
                                        )
                                )
        );

        verifyNoInteractions(
                reportRepository,
                reportMessageRepository,
                eventPublisher
        );
    }

    @Test
    void adminCannotUseClientMessageMethods() {

        assertThrows(
                AccessDeniedException.class,
                () ->
                        reportMessageService
                                .findMineMessages(
                                        "DEN-2026-ABCD2345",
                                        admin,
                                        PageRequest.of(
                                                0,
                                                20
                                        )
                                )
        );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        reportMessageService
                                .sendMineMessage(
                                        "DEN-2026-ABCD2345",
                                        admin,
                                        new ReportMessageRequestDTO(
                                                "Tentativa indevida."
                                        )
                                )
        );

        verifyNoInteractions(
                reportRepository,
                reportMessageRepository,
                eventPublisher
        );
    }

    @Test
    void clientMessageUsesAuthenticatedClientAsAuthor() {

        String protocol =
                "DEN-2026-ABCD2345";

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientA)
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

        when(
                reportMessageRepository
                        .saveAndFlush(
                                any(ReportMessage.class)
                        )
        ).thenAnswer(invocation -> {

            ReportMessage message =
                    invocation.getArgument(0);

            message.setId(
                    UUID.randomUUID()
            );

            message.setCreatedAt(
                    Instant.parse(
                            "2026-10-06T18:00:00Z"
                    )
            );

            return message;
        });

        var response =
                reportMessageService
                        .sendMineMessage(
                                protocol,
                                clientA,
                                new ReportMessageRequestDTO(
                                        "  Minha mensagem.  "
                                )
                        );

        assertEquals(
                "cliente.a",
                response.authorUsername()
        );

        assertEquals(
                "CLIENT",
                response.authorRole()
        );

        assertEquals(
                "Minha mensagem.",
                response.body()
        );

        verify(
                reportMessageRepository
        ).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(
                        message ->
                                message.getAuthor()
                                        .getId()
                                        .equals(clientA.getId())
                                        && message.getReport()
                                        .getId()
                                        .equals(report.getId())
                                        && message.getBody()
                                        .equals("Minha mensagem.")
                )
        );

        ArgumentCaptor<AdminNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(
                        AdminNotificationEvent.class
                );

        verify(
                eventPublisher
        ).publishEvent(
                eventCaptor.capture()
        );

        AdminNotificationEvent event =
                eventCaptor.getValue();

        assertEquals(
                AdminNotificationEvent.Type.CLIENT_MESSAGE_CREATED,
                event.type()
        );

        assertEquals(
                protocol,
                event.protocol()
        );
    }

    @Test
    void adminMessageUsesAuthenticatedAdminAsAuthor() {

        String protocol =
                "DEN-2026-ABCD2345";

        Report report =
                Report.builder()
                        .id(UUID.randomUUID())
                        .protocol(protocol)
                        .owner(clientB)
                        .build();

        when(
                reportRepository
                        .findByProtocolForUpdate(
                                protocol
                        )
        ).thenReturn(
                Optional.of(report)
        );

        when(
                reportMessageRepository
                        .saveAndFlush(
                                any(ReportMessage.class)
                        )
        ).thenAnswer(invocation -> {

            ReportMessage message =
                    invocation.getArgument(0);

            message.setId(
                    UUID.randomUUID()
            );

            message.setCreatedAt(
                    Instant.parse(
                            "2026-10-06T18:05:00Z"
                    )
            );

            return message;
        });

        var response =
                reportMessageService
                        .sendAdminMessage(
                                protocol,
                                admin,
                                new ReportMessageRequestDTO(
                                        "Resposta administrativa."
                                )
                        );

        assertEquals(
                "admin",
                response.authorUsername()
        );

        assertEquals(
                "ADMIN",
                response.authorRole()
        );

        verify(
                reportMessageRepository
        ).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(
                        message ->
                                message.getAuthor()
                                        .getId()
                                        .equals(admin.getId())
                                        && message.getReport()
                                        .getId()
                                        .equals(report.getId())
                )
        );

        verifyNoInteractions(
                eventPublisher
        );
    }
}