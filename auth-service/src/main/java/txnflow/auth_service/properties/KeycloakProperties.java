package txnflow.auth_service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret,
        String adminRealm,
        String adminClientId,
        String adminUsername,
        String adminPassword
) {
}