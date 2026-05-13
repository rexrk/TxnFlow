package txnflow.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import txnflow.auth_service.dto.request.LoginRequest;
import txnflow.auth_service.dto.request.LogoutRequest;
import txnflow.auth_service.dto.request.RefreshTokenRequest;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.dto.response.ApiResponse;
import txnflow.auth_service.dto.response.TokenResponse;
import txnflow.auth_service.service.KeycloakAuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;


    @GetMapping("/hello")
    public String hello() {
        return "Hello! from TxnFlow!";
    }


    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        keycloakAuthService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(keycloakAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(keycloakAuthService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout( @RequestBody LogoutRequest request) {
        keycloakAuthService.logout(request);
        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }


//    GET  /api/v1/auth/me


}
