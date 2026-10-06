package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.dto.response.ProtocolResponseDTO;
import com.mbfreire.employee_reporting.entity.User;
import com.mbfreire.employee_reporting.enums.Role;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.exception.GlobalExceptionHandler;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import com.mbfreire.employee_reporting.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerAttachmentFeatureTest {

    private ReportService reportService;
    private MockMvc mockMvc;

    private User client;
    private UserDetailsImpl principal;

    @BeforeEach
    void setUp() {

        reportService =
                mock(ReportService.class);

        principal =
                mock(UserDetailsImpl.class);

        client =
                User.builder()
                        .id(UUID.randomUUID())
                        .name("Cliente de Teste")
                        .username("cliente.teste")
                        .role(Role.CLIENT)
                        .active(true)
                        .build();

        when(
                principal.getUser()
        ).thenReturn(client);

        HandlerMethodArgumentResolver
                authenticationPrincipalResolver =
                new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(
                            MethodParameter parameter
                    ) {

                        return UserDetailsImpl.class
                                .isAssignableFrom(
                                        parameter
                                                .getParameterType()
                                );
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory
                    ) {

                        return principal;
                    }
                };

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(
                                new ReportController(
                                        reportService
                                )
                        )
                        .setControllerAdvice(
                                new GlobalExceptionHandler()
                        )
                        .setCustomArgumentResolvers(
                                authenticationPrincipalResolver
                        )
                        .build();
    }

    @Test
    void keepsJsonRegistrationContractWithoutAttachments()
            throws Exception {

        when(
                reportService.register(
                        any(ReportRequestDTO.class),
                        eq(client)
                )
        ).thenReturn(
                new ProtocolResponseDTO(
                        "DEN-2026-ABCD2345"
                )
        );

        mockMvc.perform(
                        post("/api/reports")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "categoryId": "550e8400-e29b-41d4-a716-446655440000",
                                          "description": "Relato de teste.",
                                          "incidentDate": null,
                                          "incidentLocation": null
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.protocol")
                                .value(
                                        "DEN-2026-ABCD2345"
                                )
                )
                .andExpect(
                        jsonPath("$.trackingCode")
                                .doesNotExist()
                );

        verify(
                reportService
        ).register(
                any(ReportRequestDTO.class),
                eq(client)
        );
    }

    @Test
    void rejectsMultipartRegistration()
            throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "files",
                        "evidencia.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "%PDF-fixture".getBytes()
                );

        mockMvc.perform(
                        multipart("/api/reports")
                                .file(file)
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "Registre a manifestação primeiro e envie os anexos separadamente."
                                )
                );
    }

    @Test
    void rejectsOwnReportUploadWhenAttachmentsAreDisabled()
            throws Exception {

        doThrow(
                new AttachmentUnavailableException(
                        "O envio de anexos está temporariamente indisponível."
                )
        ).when(
                reportService
        ).uploadAttachments(
                anyString(),
                eq(client),
                anyList()
        );

        MockMultipartFile file =
                new MockMultipartFile(
                        "files",
                        "evidencia.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "%PDF-fixture".getBytes()
                );

        mockMvc.perform(
                        multipart(
                                "/api/reports/mine/DEN-2026-ABCD2345/attachments"
                        )
                                .file(file)
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "O envio de anexos está temporariamente indisponível."
                                )
                );
    }

    @Test
    void rejectsOwnReportUploadWithoutFilesWhenAttachmentsAreDisabled()
            throws Exception {

        doThrow(
                new AttachmentUnavailableException(
                        "O envio de anexos está temporariamente indisponível."
                )
        ).when(
                reportService
        ).uploadAttachments(
                eq("DEN-2026-ABCD2345"),
                eq(client),
                isNull()
        );

        mockMvc.perform(
                        multipart(
                                "/api/reports/mine/DEN-2026-ABCD2345/attachments"
                        )
                )
                .andExpect(
                        status().isServiceUnavailable()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(503)
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value(
                                        "O envio de anexos está temporariamente indisponível."
                                )
                );
    }
}