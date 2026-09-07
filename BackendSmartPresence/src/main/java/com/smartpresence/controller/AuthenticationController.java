package com.smartpresence.controller;

import com.smartpresence.dto.request.LoginRequest;
import com.smartpresence.dto.request.RefreshTokenRequest;
import com.smartpresence.dto.request.UserRegistrationRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.AuthResponse;
import com.smartpresence.dto.response.UserResponse;
import com.smartpresence.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentification utilisateur (JWT)")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Se connecter", description = "Valide les identifiants et émet un JWT")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Connexion réussie"));
    }

    @Operation(summary = "Créer un compte utilisateur",
            description = "Réservé aux administrateurs. L'inscription libre est volontairement "
                    + "fermée : elle permettrait à un tiers de s'attribuer le rôle ADMIN.")
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Enregistrement réussi"));
    }

    @Operation(summary = "Rafraîchir le token", description = "Émet un nouveau JWT si le refresh token est valide")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authenticationService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token rafraîchi"));
    }
}
