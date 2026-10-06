package com.mbfreire.employee_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequestDTO(

        @NotBlank(message = "O username não pode estar vazio.")
        @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "O username pode conter apenas letras, números, ponto, hífen e underline."
        )
        String username

) {}