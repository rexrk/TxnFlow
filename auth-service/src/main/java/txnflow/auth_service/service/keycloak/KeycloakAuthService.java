package txnflow.auth_service.service.keycloak;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import txnflow.auth_service.constant.RoleConstants;
import txnflow.auth_service.dto.request.LoginRequest;
import txnflow.auth_service.dto.request.LogoutRequest;
import txnflow.auth_service.dto.request.RefreshTokenRequest;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.dto.response.AuthUserResponse;
import txnflow.auth_service.dto.response.TokenResponse;
import txnflow.auth_service.entity.AppUser;
import txnflow.auth_service.exception.InvalidCredentialsException;
import txnflow.auth_service.exception.InvalidRefreshTokenException;
import txnflow.auth_service.exception.UserAlreadyExistsException;
import txnflow.auth_service.properties.KeycloakProperties;
import txnflow.auth_service.repository.AppUserRepository;
import txnflow.auth_service.service.AuthService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAuthService implements AuthService {

    private final Keycloak keycloak;
    private final KeycloakProperties props;
    private final RestClient restClient;
    private final AppUserRepository appUserRepository;

    @Override
    public void register(RegisterRequest request) {
        log.info("User registration request {}", request.email());

        Response response = null;
        String keycloakUserId = null;

        try {
            UserRepresentation user = buildKeycloakUser(request);

            response = keycloak
                    .realm(props.realm())
                    .users()
                    .create(user);

            if (response.getStatus() == 409) {
                throw new UserAlreadyExistsException("User already exists");
            }

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
            }

            keycloakUserId = response.getLocation()
                    .getPath()
                    .replaceAll(".*/([^/]+)$", "$1");

            AppUser appUser = appUserRepository.save(
                    AppUser.builder()
                            .keycloakUserId(keycloakUserId)
                            .email(request.email())
                            .name(request.name())
                            .role(RoleConstants.USER)
                            .build()
            );

            updateKeycloakAppUserId(keycloakUserId, appUser.getId().toString());

            assignUserRole(keycloakUserId);

        } catch (UserAlreadyExistsException ex) {
            throw ex;

        } catch (Exception ex) {
            if (keycloakUserId != null) {
                try (Response ignored = keycloak
                        .realm(props.realm())
                        .users()
                        .delete(keycloakUserId)) {

                    log.info("Rolled back Keycloak user {}", keycloakUserId);
                }
            }

            throw new RuntimeException("Registration failed", ex);

        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private UserRepresentation buildKeycloakUser(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.email());
        user.setEmail(request.email());

        String[] names = splitName(request.name());
        user.setFirstName(names[0]);
        user.setLastName(names[1]);

        user.setEnabled(true);
        user.setEmailVerified(true);                                        // [Fix] Temporary email set

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        return user;
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

    private void updateKeycloakAppUserId(String keycloakUserId, String appUserId) {
        UserResource userResource = keycloak
                .realm(props.realm())
                .users()
                .get(keycloakUserId);

        UserRepresentation user = userResource.toRepresentation();

        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put("app_user_id", List.of(appUserId));

        user.setAttributes(attributes);
        userResource.update(user);
    }

    private void assignUserRole(String keycloakUserId) {
        RoleRepresentation userRole = keycloak
                .realm(props.realm())
                .roles()
                .get(RoleConstants.USER)
                .toRepresentation();

        keycloak
                .realm(props.realm())
                .users()
                .get(keycloakUserId)
                .roles()
                .realmLevel()
                .add(List.of(userRole));
    }

    @Override
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

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

        } catch (HttpClientErrorException.BadRequest ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
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

        try {
            return restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

        } catch (HttpClientErrorException.BadRequest ex) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }
    }

    @Override
    public void logout(LogoutRequest request) {

        String logoutUrl = props.serverUrl()
                + "/realms/"
                + props.realm()
                + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add(OAuth2Constants.CLIENT_ID, props.clientId());
        form.add(OAuth2Constants.CLIENT_SECRET, props.clientSecret());
        form.add(OAuth2Constants.REFRESH_TOKEN, request.refreshToken());

        restClient.post()
                .uri(logoutUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public AuthUserResponse me(Jwt jwt) {
        return new AuthUserResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name")
        );
    }
}