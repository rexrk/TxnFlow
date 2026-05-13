package txnflow.auth_service.service;

import txnflow.auth_service.dto.request.LoginRequest;
import txnflow.auth_service.dto.request.LogoutRequest;
import txnflow.auth_service.dto.request.RefreshTokenRequest;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.dto.response.TokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshTokenRequest request);
    void logout(LogoutRequest request);

}
