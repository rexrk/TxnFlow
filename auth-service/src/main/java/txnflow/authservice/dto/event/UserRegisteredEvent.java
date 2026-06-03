package txnflow.authservice.dto.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        UUID keycloakUserId,
        String email
) {}