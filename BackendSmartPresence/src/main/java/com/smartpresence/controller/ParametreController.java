package com.smartpresence.controller;

import com.smartpresence.dto.request.ParametreEtablissementRequest;
import com.smartpresence.dto.response.ApiResponse;
import com.smartpresence.dto.response.ParametreEtablissementResponse;
import com.smartpresence.service.ParametreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/parametres")
@RequiredArgsConstructor
@Tag(name = "Paramètres", description = "Gestion des paramètres de l'établissement")
public class ParametreController {

    private final ParametreService parametreService;

    @Operation(summary = "Récupérer les paramètres")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISEUR')")
    public ResponseEntity<ApiResponse<ParametreEtablissementResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success(parametreService.get(), "Paramètres récupérés"));
    }

    @Operation(summary = "Mettre à jour les paramètres")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ParametreEtablissementResponse>> update(@Valid @RequestBody ParametreEtablissementRequest request) {
        return ResponseEntity.ok(ApiResponse.success(parametreService.update(request), "Paramètres mis à jour avec succès"));
    }
}
