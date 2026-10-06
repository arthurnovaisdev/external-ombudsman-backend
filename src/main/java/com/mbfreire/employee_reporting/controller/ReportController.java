package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ReportRequestDTO;
import com.mbfreire.employee_reporting.dto.response.AttachmentDownloadDTO;
import com.mbfreire.employee_reporting.dto.response.ProtocolResponseDTO;
import com.mbfreire.employee_reporting.dto.response.ReportAdminResponseDTO;
import com.mbfreire.employee_reporting.dto.response.ReportAdminSummaryResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private static final String PROTOCOL_REGEX =
            "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}";

    private final ReportService reportService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProtocolResponseDTO> register(
            @Valid @RequestBody ReportRequestDTO dto,
            @AuthenticationPrincipal UserDetailsImpl principal
    ) {

        ProtocolResponseDTO response =
                reportService.register(
                        dto,
                        principal.getUser()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> rejectMultipartRegistration() {

        throw new AttachmentUnavailableException(
                "Registre a manifestação primeiro e envie os anexos separadamente."
        );
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ReportResponseDTO>> findMine(
            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "A página não pode ser negativa."
            )
            int page,

            @RequestParam(defaultValue = "10")
            @Min(
                    value = 1,
                    message = "O tamanho da página deve ser no mínimo 1."
            )
            @Max(
                    value = 50,
                    message = "O tamanho da página deve ser no máximo 50."
            )
            int size,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        return ResponseEntity.ok(
                reportService.findMine(
                        principal.getUser(),
                        pageable
                )
        );
    }


    @GetMapping("/mine/{protocol}")
    public ResponseEntity<ReportResponseDTO> findMineDetail(
            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        return ResponseEntity.ok(
                reportService.findMineDetail(
                        protocol,
                        principal.getUser()
                )
        );
    }

    @PostMapping(
            value = "/mine/{protocol}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadAttachments(
            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @RequestParam(
                    value = "files",
                    required = false
            )
            List<MultipartFile> files,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        reportService.uploadAttachments(
                protocol,
                principal.getUser(),
                files
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }


    @GetMapping(
            "/mine/{protocol}/attachments/{attachmentId}"
    )
    public ResponseEntity<Resource> downloadOwnAttachment(
            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @PathVariable
            UUID attachmentId,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        AttachmentDownloadDTO attachment =
                reportService.downloadAttachment(
                        protocol,
                        attachmentId,
                        principal.getUser()
                );

        return buildAttachmentResponse(
                attachment
        );
    }
    @GetMapping("/admin")
    public ResponseEntity<Page<ReportAdminSummaryResponseDTO>>
    findAllAdmin(

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "A página não pode ser negativa."
            )
            int page,

            @RequestParam(defaultValue = "10")
            @Min(
                    value = 1,
                    message = "O tamanho da página deve ser no mínimo 1."
            )
            @Max(
                    value = 50,
                    message = "O tamanho da página deve ser no máximo 50."
            )
            int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        return ResponseEntity.ok(
                reportService.findAllAdmin(
                        pageable
                )
        );
    }


    @GetMapping("/admin/{protocol}")
    public ResponseEntity<ReportAdminResponseDTO>
    findAdminDetail(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol
    ) {

        return ResponseEntity.ok(
                reportService.findAdminDetail(
                        protocol
                )
        );
    }


    @PostMapping("/admin/{protocol}/close")
    public ResponseEntity<ReportAdminResponseDTO>
    close(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        return ResponseEntity.ok(
                reportService.close(
                        protocol,
                        principal.getUser()
                )
        );
    }


    @GetMapping(
            "/admin/{protocol}/attachments/{attachmentId}"
    )
    public ResponseEntity<Resource> downloadAdminAttachment(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @PathVariable
            UUID attachmentId,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        AttachmentDownloadDTO attachment =
                reportService.downloadAttachment(
                        protocol,
                        attachmentId,
                        principal.getUser()
                );

        return buildAttachmentResponse(
                attachment
        );
    }

    private ResponseEntity<Resource> buildAttachmentResponse(
            AttachmentDownloadDTO attachment
    ) {

        ContentDisposition contentDisposition =
                ContentDisposition
                        .attachment()
                        .filename(
                                attachment.originalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(
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
                .body(
                        attachment.resource()
                );
    }
}