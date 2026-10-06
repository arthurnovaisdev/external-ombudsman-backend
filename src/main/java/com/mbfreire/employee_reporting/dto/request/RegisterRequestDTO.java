package com.mbfreire.employee_reporting.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(

        @NotBlank(message = "O nome não pode estar vazio.")
        @Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
        String name,

        @NotBlank(message = "O username não pode estar vazio.")
        @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "O username pode conter apenas letras, números, ponto, hífen e underline."
        )
        String username,

        @Email(message = "Informe um e-mail de contato válido.")
        @Size(max = 150, message = "O e-mail deve possuir no máximo 150 caracteres.")
        String contactEmail,

        @NotBlank(message = "A senha provisória não pode estar vazia.")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres.")
        String password

) {}