package com.mbfreire.employee_reporting.controller;

import com.mbfreire.employee_reporting.dto.request.ReportMessageRequestDTO;
import com.mbfreire.employee_reporting.dto.response.ReportMessageResponseDTO;
import com.mbfreire.employee_reporting.security.UserDetailsImpl;
import com.mbfreire.employee_reporting.service.ReportMessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Validated
public class ReportMessageController {

    private static final String PROTOCOL_REGEX =
            "DEN-\\d{4}-[A-HJ-NP-Z2-9]{8}";

    private final ReportMessageService reportMessageService;


    // CLIENT

    @GetMapping("/mine/{protocol}/messages")
    public ResponseEntity<Page<ReportMessageResponseDTO>>
    findMineMessages(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "A página não pode ser negativa."
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "O tamanho da página deve ser no mínimo 1."
            )
            @Max(
                    value = 100,
                    message = "O tamanho da página deve ser no máximo 100."
            )
            int size,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return ResponseEntity.ok(
                reportMessageService
                        .findMineMessages(
                                protocol,
                                principal.getUser(),
                                pageable
                        )
        );
    }


    @PostMapping("/mine/{protocol}/messages")
    public ResponseEntity<ReportMessageResponseDTO>
    sendMineMessage(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @Valid
            @RequestBody
            ReportMessageRequestDTO dto,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        ReportMessageResponseDTO response =
                reportMessageService
                        .sendMineMessage(
                                protocol,
                                principal.getUser(),
                                dto
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ADMIN

    @GetMapping("/admin/{protocol}/messages")
    public ResponseEntity<Page<ReportMessageResponseDTO>>
    findAdminMessages(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "A página não pode ser negativa."
            )
            int page,

            @RequestParam(defaultValue = "20")
            @Min(
                    value = 1,
                    message = "O tamanho da página deve ser no mínimo 1."
            )
            @Max(
                    value = 100,
                    message = "O tamanho da página deve ser no máximo 100."
            )
            int size,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        return ResponseEntity.ok(
                reportMessageService
                        .findAdminMessages(
                                protocol,
                                principal.getUser(),
                                pageable
                        )
        );
    }


    @PostMapping("/admin/{protocol}/messages")
    public ResponseEntity<ReportMessageResponseDTO>
    sendAdminMessage(

            @PathVariable
            @Pattern(
                    regexp = PROTOCOL_REGEX,
                    message = "O protocolo informado é inválido."
            )
            String protocol,

            @Valid
            @RequestBody
            ReportMessageRequestDTO dto,

            @AuthenticationPrincipal
            UserDetailsImpl principal
    ) {

        ReportMessageResponseDTO response =
                reportMessageService
                        .sendAdminMessage(
                                protocol,
                                principal.getUser(),
                                dto
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}