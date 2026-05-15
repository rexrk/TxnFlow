package txnflow.auth_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import txnflow.auth_service.dto.request.LoginRequest;
import txnflow.auth_service.dto.request.LogoutRequest;
import txnflow.auth_service.dto.request.RefreshTokenRequest;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.dto.response.ApiResponse;
import txnflow.auth_service.dto.response.AuthUserResponse;
import txnflow.auth_service.dto.response.TokenResponse;
import txnflow.auth_service.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/public/hello")
    public String hello() {
        return "Hello! from TxnFlow!";
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(authService.me(jwt));
    }


}
