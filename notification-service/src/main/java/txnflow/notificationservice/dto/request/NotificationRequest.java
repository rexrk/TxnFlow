package txnflow.notificationservice.dto.request;

import com.resend.services.emails.model.CreateEmailOptions;
import txnflow.notificationservice.enums.NotificationType;

import java.util.UUID;

public record NotificationRequest(
        UUID eventId,
        UUID userId,
        UUID referenceId,
        String recipient,
        NotificationType type,
        CreateEmailOptions email
) {}