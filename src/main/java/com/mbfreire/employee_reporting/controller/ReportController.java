package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.dto.request.ReportStatusUpdateRequestDTO;
import com.mbfreire.employee_reporting.dto.response.AttachmentDownloadDTO;
import com.mbfreire.employee_reporting.dto.response.ProtocolResponseDTO;
import com.mbfreire.employee_reporting.dto.response.ReportAdminResponseDTO;
import com.mbfreire.employee_reporting.dto.response.ReportResponseDTO;
import com.mbfreire.employee_reporting.exception.AttachmentUnavailableException;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import com.mbfreire.employee_reporting.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final ReportService reportService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProtocolResponseDTO> register(@Valid @RequestBody ReportRequestDTO dto) {
        ProtocolResponseDTO response = reportService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> rejectMultipartRegistration() {
        throw new AttachmentUnavailableException(
                "O envio de anexos está temporariamente indisponível. Registre a denúncia sem arquivos."
        );
    }

    @PostMapping(value = "/{protocol}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadAttachments(
            @PathVariable @Pattern (regexp = "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}", message = "O protocolo informado é inválido.") String protocol,
            @RequestParam(value = "trackingCode", required = false) @Pattern(regexp = "[A-HJ-NP-Z2-9]{10}", message = "O código de rastreio informado é inválido.") String trackingCode,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
            ) {
        reportService.uploadAttachments(protocol, trackingCode, files);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/consult")
    public ResponseEntity<ReportResponseDTO> consult(
            @RequestParam @Pattern(regexp = "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}", message = "O protocolo informado é inválido.") String protocol,
            @RequestParam @Pattern(regexp = "[A-HJ-NP-Z2-9]{10}", message = "O código de acesso informado é inválido.") String code
    ) {
        return ResponseEntity.ok(reportService.consult(protocol, code));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<ReportResponseDTO>> listAll(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página não pode ser negativa") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O tamanho da página deve ser no mínimo 1.") @Max(value = 50, message = "O tamanho da página deve ser no máximo 50.") int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ReportResponseDTO> reports = reportService.findAll(pageable);

        return ResponseEntity.ok(reports);
    }

    @GetMapping("/admin/{protocol}")
    public ResponseEntity<ReportAdminResponseDTO> findAdminDetail(@PathVariable @Pattern(regexp = "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}", message = "O protocolo informado é inválido.") String protocol) {
        ReportAdminResponseDTO response = reportService.findAdminDetail(protocol);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/{protocol}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable @Pattern(regexp = "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}", message = "O protocolo informado é inválido.") String protocol, @PathVariable UUID attachmentId) {
        AttachmentDownloadDTO attachment = reportService.downloadAttachment(protocol, attachmentId);

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(
                        attachment.originalFileName(),
                        StandardCharsets.UTF_8
                ).build();

        return ResponseEntity.ok().
                contentType(
                MediaType.parseMediaType(
                        attachment.contentType()
                )
        )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .cacheControl(
                        CacheControl.noStore()
                )
                .body(attachment.resource());
    }

    @PatchMapping("/admin/{protocol}/status")
    public ResponseEntity<ReportResponseDTO> updateStatus(
            @PathVariable @Pattern(regexp = "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}", message = "O protocolo informado é inválido.") String protocol,
            @Valid @RequestBody ReportStatusUpdateRequestDTO dto,
            @AuthenticationPrincipal UserDetailsImpl principal
            ) {
        ReportResponseDTO response = reportService.updateStatus(protocol, dto, principal.getUser());
        return ResponseEntity.ok(response);
    }
}
