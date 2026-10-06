package com.mbfreire.employee_reporting.dto.response;

import java.time.Instant;

public record ReportAdminSummaryResponseDTO(
        String protocol,
        String category,
        String description,
        Instant createdAt,
        Instant closedAt,
        String ownerName,
        String ownerUsername
) {}