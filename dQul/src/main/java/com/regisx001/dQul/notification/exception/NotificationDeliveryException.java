package com.regisx001.dQul.notification.exception;

import org.springframework.http.HttpStatus;

public class NotificationDeliveryException extends NotificationModuleException {

    public NotificationDeliveryException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "NOTIFICATION_DELIVERY_FAILED");
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_GATEWAY, "NOTIFICATION_DELIVERY_FAILED");
    }
}
