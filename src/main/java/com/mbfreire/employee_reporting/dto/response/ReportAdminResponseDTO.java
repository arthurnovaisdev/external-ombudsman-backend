package com.mbfreire.employee_reporting.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReportAdminResponseDTO(
        String protocol,
        String category,
        String description,
        LocalDate incidentDate,
        String incidentLocation,
        Instant createdAt,
        Instant closedAt,
        Instant messagesPurgedAt,

        String ownerName,
        String ownerUsername,
        String ownerContactEmail,

        List<AttachmentResponseDTO> attachments
) {}