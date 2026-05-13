package txnflow.auth_service.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @JsonProperty("refresh_token")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}