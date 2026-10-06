package com.mbfreire.employee_reporting.dto.response;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String username,
        String contactEmail,
        String role,
        boolean active,
        boolean passwordChanged
) {}