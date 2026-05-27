package txnflow.paymentadapterservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {
    private static final String APP_USER_ID = "app_user_id";

    public UUID getCurrentAppUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Invalid authentication");
        }

        String appUserId = jwtAuth.getToken().getClaimAsString(APP_USER_ID);

        if (appUserId == null || appUserId.isBlank()) {
            throw new IllegalStateException(APP_USER_ID + " claim missing");
        }

        return UUID.fromString(appUserId);
    }
}