package com.mbfreire.employee_reporting.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(
        prefix = "reports.retention"
)
@Validated
@Getter
@Setter
public class ReportRetentionProperties {

    private boolean enabled = true;

    @Min(1)
    @Max(3650)
    private int retentionDays = 180;

    @Min(1)
    @Max(1000)
    private int batchSize = 100;
}