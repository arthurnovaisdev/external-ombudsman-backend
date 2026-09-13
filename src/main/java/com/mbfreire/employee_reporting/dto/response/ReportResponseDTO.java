package com.mbfreire.employee_reporting.dto.response;

import com.mbfreire.employee_reporting.enums.ReportStatus;

import java.time.Instant;

public record ReportResponseDTO(
        String protocol,
        String category,
        String description,
        ReportStatus status,
        Instant createdAt
) {}
