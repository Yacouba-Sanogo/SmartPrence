package com.smartpresence.controller;

import com.smartpresence.dto.request.UserUpdateRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.UserResponse;
import com.smartpresence.security.CustomUserDetails;
import com.smartpresence.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @Operation(summary = "Lister tous les utilisateurs")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.findAll(), "Utilisateurs récupérés"));
    }

    @Operation(summary = "Trouver un utilisateur par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.findById(id)));
    }

    @Operation(summary = "Récupérer mon profil", description = "Récupère les informations de l'utilisateur connecté")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                utilisateurService.findById(userDetails.getId()), "Profil récupéré"));
    }

    @Operation(summary = "Modifier un utilisateur")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(utilisateurService.update(id, request), "Utilisateur mis à jour"));
    }

    @Operation(summary = "Activer un utilisateur")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable UUID id) {
        utilisateurService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur activé"));
    }

    @Operation(summary = "Désactiver un utilisateur")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        utilisateurService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur désactivé"));
    }
}
