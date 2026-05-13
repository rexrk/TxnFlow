package txnflow.auth_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LogoutRequest(
        @JsonProperty("refresh_token")
        String refreshToken
) {
}