package txnflow.auth_service.service;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import txnflow.auth_service.dto.request.LoginRequest;
import txnflow.auth_service.dto.request.RefreshTokenRequest;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.dto.response.TokenResponse;
import txnflow.auth_service.exception.UserAlreadyExistsException;
import txnflow.auth_service.properties.KeycloakProperties;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private final Keycloak keycloak;
    private final KeycloakProperties props;
    private final RestClient restClient;

    public void register(RegisterRequest request) {
        log.info("User registration request {}", request.email());
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.email());
        user.setEmail(request.email());

        String[] names = splitName(request.name());
        user.setFirstName(names[0]);
        user.setLastName(names[1]);

        user.setEnabled(true);

        user.setEmailVerified(true);  // [Fix] Temporary email set

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        Response response = keycloak
                .realm(props.realm())
                .users()
                .create(user);

        if (response.getStatus() == 409) {
            log.info("User {} already exists", request.email());
            response.close();
            throw new UserAlreadyExistsException("User already exists");
        }

        if (response.getStatus() != 201) {
            int status = response.getStatus();
            response.close();
            throw new RuntimeException("Failed to create user. Status: " + status);
        }

        response.close();
    }

    private String[] splitName(String name) {
        String cleaned = name == null ? "" : name.trim().replaceAll("\\s+", " ");

        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        String[] parts = cleaned.split(" ", 2);

        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "-";

        return new String[]{firstName, lastName};
    }

    public TokenResponse login(LoginRequest request) {
        String tokenUrl = props.serverUrl()
                + "/realms/"
                + props.realm()
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        form.add(OAuth2Constants.CLIENT_ID, props.clientId());
        form.add(OAuth2Constants.CLIENT_SECRET, props.clientSecret());
        form.add(OAuth2Constants.USERNAME, request.email());
        form.add(OAuth2Constants.PASSWORD, request.password());
        form.add(OAuth2Constants.SCOPE, "openid profile email");

        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        String tokenUrl = props.serverUrl()
                + "/realms/"
                + props.realm()
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.add(OAuth2Constants.CLIENT_ID, props.clientId());
        form.add(OAuth2Constants.CLIENT_SECRET, props.clientSecret());
        form.add(OAuth2Constants.REFRESH_TOKEN, request.refreshToken());

        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
    }
}