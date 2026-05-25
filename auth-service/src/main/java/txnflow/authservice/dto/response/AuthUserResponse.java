package txnflow.authservice.dto.response;

public record AuthUserResponse(
        String appUserId,
        String keycloakUserId,
        String email,
        String name
) {
}