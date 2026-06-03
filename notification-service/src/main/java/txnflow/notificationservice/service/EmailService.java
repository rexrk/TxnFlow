package txnflow.notificationservice.service;

import txnflow.notificationservice.dto.request.NotificationRequest;

public interface EmailService {
    void send(NotificationRequest request);
}
