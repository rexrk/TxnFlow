package txnflow.auth_service.dto.response;

public record AuthUserResponse(
        String appUserId,
        String keycloakUserId,
        String email,
        String name
) {
}