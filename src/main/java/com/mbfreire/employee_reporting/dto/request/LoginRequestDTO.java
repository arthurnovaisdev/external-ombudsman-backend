package com.mbfreire.employee_reporting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "O username não pode estar vazio.")
        @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "O username pode conter apenas letras, números, ponto, hífen e underline."
        )
        String username,

        @NotBlank(message = "A senha não pode estar vazia.")
        @Size(max = 100, message = "A senha deve ter no máximo 100 caracteres.")
        String password

) {}