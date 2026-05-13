package txnflow.auth_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import txnflow.auth_service.dto.request.RegisterRequest;
import txnflow.auth_service.service.KeycloakUserService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final KeycloakUserService keycloakUserService;


    @GetMapping("/hello")
    public String hello() {
        return "Hello! from TxnFlow!";
    }


//    POST /api/v1/auth/register


    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        keycloakUserService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User registered successfully");
    }



//    POST /api/v1/auth/login



//    POST /api/v1/auth/refresh



//    POST /api/v1/auth/logout




//    GET  /api/v1/auth/me



}
