package txnflow.authservice.service;

import org.springframework.security.oauth2.jwt.Jwt;
import txnflow.authservice.dto.request.LoginRequest;
import txnflow.authservice.dto.request.LogoutRequest;
import txnflow.authservice.dto.request.RefreshTokenRequest;
import txnflow.authservice.dto.request.RegisterRequest;
import txnflow.authservice.dto.response.AuthUserResponse;
import txnflow.authservice.dto.response.TokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshTokenRequest request);
    void logout(LogoutRequest request);
    AuthUserResponse me(Jwt jwt);
}
