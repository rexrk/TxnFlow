package txnflow.notificationservice.service;

import com.resend.core.exception.ResendException;
import txnflow.notificationservice.dto.request.NotificationRequest;

public interface EmailService {
    void send(NotificationRequest request) throws ResendException;
}
