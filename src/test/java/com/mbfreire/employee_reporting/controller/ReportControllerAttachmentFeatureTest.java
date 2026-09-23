package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.dto.response.ProtocolResponseDTO;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.exception.GlobalExceptionHandler;
import com.mbfreire.employee_reporting.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportControllerAttachmentFeatureTest {

    private ReportService reportService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void keepsJsonRegistrationContractWithoutAttachments() throws Exception {
        when(reportService.register(any(ReportRequestDTO.class)))
                .thenReturn(new ProtocolResponseDTO(
                        "DEN-2026-ABCD2345",
                        "ABCD2345EF"
                ));

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "550e8400-e29b-41d4-a716-446655440000",
                                  "description": "Relato de teste.",
                                  "incidentDate": null,
                                  "incidentLocation": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").value("DEN-2026-ABCD2345"))
                .andExpect(jsonPath("$.trackingCode").value("ABCD2345EF"));
    }

    @Test
    void rejectsMultipartRegistrationWithClearServiceUnavailableResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "evidencia.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-fixture".getBytes()
        );

        mockMvc.perform(multipart("/api/reports").file(file))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.erro").value(
                        "O envio de anexos está temporariamente indisponível. Registre a denúncia sem arquivos."
                ));
    }

    @Test
    void rejectsLegacyDirectUploadWithClearServiceUnavailableResponse() throws Exception {
        doThrow(new AttachmentUnavailableException(
                "O envio de anexos está temporariamente indisponível."
        )).when(reportService).uploadAttachments(anyString(), anyString(), anyList());

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "evidencia.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-fixture".getBytes()
        );

        mockMvc.perform(multipart("/api/reports/DEN-2026-ABCD2345/attachments")
                        .file(file)
                        .param("trackingCode", "ABCD2345EF"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.erro").value(
                        "O envio de anexos está temporariamente indisponível."
                ));
    }

    @Test
    void rejectsLegacyUploadEvenWhenMultipartFieldsAreMissing() throws Exception {
        doThrow(new AttachmentUnavailableException(
                "O envio de anexos está temporariamente indisponível."
        )).when(reportService).uploadAttachments(
                eq("DEN-2026-ABCD2345"),
                isNull(),
                isNull()
        );

        mockMvc.perform(multipart("/api/reports/DEN-2026-ABCD2345/attachments"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value(
                        "O envio de anexos está temporariamente indisponível."
                ));
    }
}
