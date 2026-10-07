package com.mbfreire.employee_reporting.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(
        prefix = "notifications.admin"
)
@Getter
@Setter
public class AdminNotificationProperties {

    private boolean enabled = false;
    private String email;

    @PostConstruct
    void validate() {

        if (enabled
                && (email == null
                || email.isBlank())) {

            throw new IllegalStateException(
                    "ADMIN_NOTIFICATION_EMAIL deve ser configurado "
                            + "quando as notificações administrativas estiverem habilitadas."
            );
        }
    }
}