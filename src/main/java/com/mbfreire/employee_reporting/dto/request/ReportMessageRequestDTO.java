package com.mbfreire.employee_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportMessageRequestDTO(

        @NotBlank(message = "A mensagem não pode estar vazia.")
        @Size(
                max = 10000,
                message = "A mensagem deve possuir no máximo 10000 caracteres."
        )
        String body

) {}