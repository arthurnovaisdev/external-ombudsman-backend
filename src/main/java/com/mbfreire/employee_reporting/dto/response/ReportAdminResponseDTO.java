package com.mbfreire.employee_reporting.dto.response;

import com.mbfreire.employee_reporting.enums.ReportStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReportAdminResponseDTO(
        String protocol,
        String category,
        String description,
        ReportStatus status,
        LocalDate incidentDate,
        String incidentLocation,
        Instant createdAt,
        List<AttachmentResponseDTO> attachments
) {}
