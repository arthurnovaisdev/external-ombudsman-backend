package com.mbfreire.employee_reporting.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ReportMessageResponseDTO(
        UUID id,
        String authorName,
        String authorUsername,
        String authorRole,
        String body,
        Instant createdAt
) {}