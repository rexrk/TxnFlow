package txnflow.auth_service.dto.response;

public record AuthUserResponse(
        String userId,
        String email,
        String name
) {
}