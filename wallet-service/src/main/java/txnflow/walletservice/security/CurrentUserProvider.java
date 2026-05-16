package txnflow.walletservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {

    public UUID getCurrentAppUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Invalid authentication");
        }

        String appUserId = jwtAuth.getToken().getClaimAsString("app_user_id");

        if (appUserId == null || appUserId.isBlank()) {
            throw new IllegalStateException("app_user_id claim missing");
        }

        return UUID.fromString(appUserId);
    }
}