package txnflow.authservice.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import txnflow.authservice.properties.KeycloakProperties;

@Configuration
public class KeycloakConfig {

    @Bean
    public Keycloak keycloak(KeycloakProperties props) {
        return KeycloakBuilder.builder()
                .serverUrl(props.serverUrl())
                .realm(props.adminRealm())
                .username(props.adminUsername())
                .password(props.adminPassword())
                .clientId(props.adminClientId())
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }
}