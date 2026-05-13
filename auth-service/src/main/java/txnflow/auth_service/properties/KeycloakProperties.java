package txnflow.auth_service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String adminRealm,
        String adminUsername,
        String adminPassword,
        String adminClientId,
        String clientSecret
) {
}