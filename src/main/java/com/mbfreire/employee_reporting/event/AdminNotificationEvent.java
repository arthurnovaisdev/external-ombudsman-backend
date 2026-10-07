package com.mbfreire.employee_reporting.event;

public record AdminNotificationEvent(
        Type type,
        String protocol
) {

    public enum Type {
        REPORT_CREATED,
        CLIENT_MESSAGE_CREATED
    }
}