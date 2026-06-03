package txnflow.paymentadapterservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {
    private static final String APP_USER_ID = "app_user_id";
    private static final String EMAIL_CLAIM = "preferred_username";

    public UUID getCurrentAppUserId() {
        String appUserId = getJwtAuthentication()
                .getToken()
                .getClaimAsString(APP_USER_ID);

        if (appUserId == null || appUserId.isBlank()) {
            throw new IllegalStateException(APP_USER_ID + " claim missing");
        }

        return UUID.fromString(appUserId);
    }

    public String getCurrentUserEmail() {
        String email = getJwtAuthentication()
                .getToken()
                .getClaimAsString(EMAIL_CLAIM);

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("email claim missing");
        }

        return email;
    }

    private JwtAuthenticationToken getJwtAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Invalid authentication");
        }

        return jwtAuth;
    }
}